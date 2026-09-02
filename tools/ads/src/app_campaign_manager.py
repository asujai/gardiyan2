import datetime
from typing import Dict, Any, List, Optional
from dataclasses import dataclass, field

@dataclass
class TargetLocation:
    name: str
    criterion_id: str

@dataclass
class CampaignBudgetPlan:
    daily_amount: float
    daily_amount_currency: str = "TRY"
    target_cpi: Optional[float] = None

@dataclass
class AppCampaignPlan:
    name: str
    language: str
    language_code: str
    language_constant_id: str
    target_locations: List[TargetLocation]
    budget: CampaignBudgetPlan
    headlines: List[str]
    descriptions: List[str]
    id: Optional[str] = None
    app_id: str = "com.gardiyan.app"
    app_store: str = "GOOGLE_APP_STORE"

    def __post_init__(self):
        # Hedef konumları dönüştür
        if self.target_locations and isinstance(self.target_locations[0], dict):
            self.target_locations = [TargetLocation(**loc) for loc in self.target_locations]
        
        # Bütçeyi dönüştür
        if isinstance(self.budget, dict):
            self.budget = CampaignBudgetPlan(**self.budget)

        # Doğrulamalar
        if not (1 <= len(self.headlines) <= 5):
            raise ValueError("En az 1, en fazla 5 başlık belirtilmelidir.")
        for idx, h in enumerate(self.headlines, 1):
            if len(h) > 30:
                raise ValueError(f"Başlık {idx} ('{h}') {len(h)} karakter! Google Ads limiti maksimum 30 karakterdir.")

        if not (1 <= len(self.descriptions) <= 5):
            raise ValueError("En az 1, en fazla 5 açıklama belirtilmelidir.")
        for idx, d in enumerate(self.descriptions, 1):
            if len(d) > 90:
                raise ValueError(f"Açıklama {idx} ('{d}') {len(d)} karakter! Google Ads limiti maksimum 90 karakterdir.")


class AppCampaignManager:
    """
    Google Ads App Campaigns (Universal App Campaigns - UAC) Yönetim Sınıfı
    """

    def __init__(self, ads_client_provider):
        self.provider = ads_client_provider

    def generate_plan_summary(self, plan: AppCampaignPlan) -> str:
        """
        Kullanıcı onayı için detaylı kampanya özeti metni (Markdown) üretir.
        """
        locations_str = ", ".join([f"{loc.name} (ID: {loc.criterion_id})" for loc in plan.target_locations])
        
        headlines_list = "\n".join([f"    {i+1}. {h} ({len(h)}/30 kar.)" for i, h in enumerate(plan.headlines)])
        descriptions_list = "\n".join([f"    {i+1}. {d} ({len(d)}/90 kar.)" for i, d in enumerate(plan.descriptions)])
        
        monthly_budget = plan.budget.daily_amount * 30.4
        cpi_str = f"{plan.budget.target_cpi} {plan.budget.daily_amount_currency}" if plan.budget.target_cpi else "Otomatik (Maksimum Yükleme)"
        
        summary = f"""
================================================================================
                    GOOGLE ADS UYGULAMA KAMPANYA PLANI (UAC)
================================================================================
• Kampanya Adı       : {plan.name}
• Hedef Uygulama     : {plan.app_id} (Google Play Store)
• Hedef Dil          : {plan.language} (ID: {plan.language_constant_id})
• Hedef Konumlar     : {locations_str}
• Günlük Bütçe       : {plan.budget.daily_amount:.2f} {plan.budget.daily_amount_currency} (Aylık Tahmini: ~{monthly_budget:.2f} {plan.budget.daily_amount_currency})
• Hedef CPI (tCPI)   : {cpi_str}

[REKLAM BAŞLIKLARI (Max 30 Karakter)]
{headlines_list}

[REKLAM AÇIKLAMALARI (Max 90 Karakter)]
{descriptions_list}
================================================================================
"""
        return summary

    def create_campaign(self, plan: AppCampaignPlan, customer_id: Optional[str] = None) -> Dict[str, Any]:
        """
        Google Ads API üzerinden App Campaign (UAC) oluşturur.
        """
        client = self.provider.get_client()
        cid = customer_id or self.provider.get_customer_id()
        if not cid:
            raise ValueError("Customer ID bulunamadı.")

        campaign_budget_service = client.get_service("CampaignBudgetService")
        campaign_service = client.get_service("CampaignService")
        ad_group_service = client.get_service("AdGroupService")
        ad_group_ad_service = client.get_service("AdGroupAdService")
        campaign_criterion_service = client.get_service("CampaignCriterionService")

        # 1. Bütçe Oluştur (Budget)
        budget_operation = client.get_type("CampaignBudgetOperation")
        campaign_budget = budget_operation.create
        campaign_budget.name = f"Limitra App Budget - {datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}"
        campaign_budget.amount_micros = int(plan.budget.daily_amount * 1_000_000)
        campaign_budget.delivery_method = client.enums.BudgetDeliveryMethodEnum.STANDARD
        campaign_budget.explicitly_shared = False

        budget_response = campaign_budget_service.mutate_campaign_budgets(
            customer_id=cid, operations=[budget_operation]
        )
        budget_resource_name = budget_response.results[0].resource_name

        # 2. Kampanya Oluştur (App Campaign for Installs)
        campaign_operation = client.get_type("CampaignOperation")
        campaign = campaign_operation.create
        campaign.name = plan.name
        campaign.campaign_budget = budget_resource_name
        campaign.status = client.enums.CampaignStatusEnum.PAUSED
        campaign.advertising_channel_type = client.enums.AdvertisingChannelTypeEnum.MULTI_CHANNEL
        campaign.advertising_channel_sub_type = client.enums.AdvertisingChannelSubTypeEnum.APP_CAMPAIGN

        # App Ayarları
        campaign.app_campaign_setting.app_id = plan.app_id
        campaign.app_campaign_setting.app_store = client.enums.AppCampaignAppStoreEnum.GOOGLE_APP_STORE
        
        if plan.budget.target_cpi:
            campaign.app_campaign_setting.bidding_strategy_goal_type = (
                client.enums.AppCampaignBiddingStrategyGoalTypeEnum.OPTIMIZE_INSTALLS_TARGET_INSTALL_COST
            )
            campaign.target_cpi_micros = int(plan.budget.target_cpi * 1_000_000)
        else:
            campaign.app_campaign_setting.bidding_strategy_goal_type = (
                client.enums.AppCampaignBiddingStrategyGoalTypeEnum.OPTIMIZE_INSTALLS_WITHOUT_TARGET_INSTALL_COST
            )

        campaign_response = campaign_service.mutate_campaigns(
            customer_id=cid, operations=[campaign_operation]
        )
        campaign_resource_name = campaign_response.results[0].resource_name

        # 3. Ad Group Oluştur
        ad_group_operation = client.get_type("AdGroupOperation")
        ad_group = ad_group_operation.create
        ad_group.name = f"{plan.name} - Ad Group"
        ad_group.campaign = campaign_resource_name
        ad_group.status = client.enums.AdGroupStatusEnum.ENABLED
        ad_group.type_ = client.enums.AdGroupTypeEnum.SEARCH_STANDARD

        ad_group_response = ad_group_service.mutate_ad_groups(
            customer_id=cid, operations=[ad_group_operation]
        )
        ad_group_resource_name = ad_group_response.results[0].resource_name

        # 4. App Ad (Varlıklar: Başlıklar & Açıklamalar)
        ad_group_ad_operation = client.get_type("AdGroupAdOperation")
        ad_group_ad = ad_group_ad_operation.create
        ad_group_ad.ad_group = ad_group_resource_name
        ad_group_ad.status = client.enums.AdGroupAdStatusEnum.ENABLED

        app_ad = ad_group_ad.ad.app_ad
        for h in plan.headlines:
            text_asset = client.get_type("AdTextAsset")
            text_asset.text = h
            app_ad.headlines.append(text_asset)

        for d in plan.descriptions:
            text_asset = client.get_type("AdTextAsset")
            text_asset.text = d
            app_ad.descriptions.append(text_asset)

        ad_group_ad_service.mutate_ad_group_ads(
            customer_id=cid, operations=[ad_group_ad_operation]
        )

        # 5. Konum ve Dil Kriterlerini Ekle
        criteria_operations = []
        # Konumlar
        for loc in plan.target_locations:
            crit_op = client.get_type("CampaignCriterionOperation")
            crit = crit_op.create
            crit.campaign = campaign_resource_name
            crit.location.geo_target_constant = f"geoTargetConstants/{loc.criterion_id}"
            criteria_operations.append(crit_op)

        # Dil
        crit_op_lang = client.get_type("CampaignCriterionOperation")
        crit_lang = crit_op_lang.create
        crit_lang.campaign = campaign_resource_name
        crit_lang.language.language_constant = f"languageConstants/{plan.language_constant_id}"
        criteria_operations.append(crit_op_lang)

        campaign_criterion_service.mutate_campaign_criteria(
            customer_id=cid, operations=criteria_operations
        )

        return {
            "success": True,
            "campaign_resource_name": campaign_resource_name,
            "campaign_id": campaign_resource_name.split("/")[-1],
            "status": "PAUSED (Onayınızla etkinleştirilebilir)"
        }

    def set_campaign_status(self, campaign_id: str, status: str, customer_id: Optional[str] = None) -> bool:
        """
        Kampanyanın durumunu ENABLED veya PAUSED yapar.
        """
        client = self.provider.get_client()
        cid = customer_id or self.provider.get_customer_id()
        campaign_service = client.get_service("CampaignService")

        campaign_operation = client.get_type("CampaignOperation")
        campaign = campaign_operation.update
        campaign.resource_name = campaign_service.campaign_path(cid, campaign_id)

        if status.upper() == "ENABLED":
            campaign.status = client.enums.CampaignStatusEnum.ENABLED
        elif status.upper() == "PAUSED":
            campaign.status = client.enums.CampaignStatusEnum.PAUSED
        elif status.upper() == "REMOVED":
            campaign.status = client.enums.CampaignStatusEnum.REMOVED
        else:
            raise ValueError(f"Geçersiz durum: {status}. ENABLED, PAUSED veya REMOVED olmalıdır.")

        client.copy_from(
            campaign_operation.update_mask,
            client.get_type("FieldMask")({"paths": ["status"]})
        )

        campaign_service.mutate_campaigns(customer_id=cid, operations=[campaign_operation])
        return True
