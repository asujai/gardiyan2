from typing import List, Dict, Any, Optional

class AdsReporter:
    """
    Google Ads Performans Raporlama ve Takip Sınıfı
    """

    def __init__(self, ads_client_provider):
        self.provider = ads_client_provider

    def get_campaign_summaries(self, customer_id: Optional[str] = None) -> List[Dict[str, Any]]:
        """
        Tüm kampanyaların genel durumunu listeler.
        """
        client = self.provider.get_client()
        cid = customer_id or self.provider.get_customer_id()
        if not cid:
            raise ValueError("Customer ID bulunamadı.")

        ga_service = client.get_service("GoogleAdsService")
        query = """
            SELECT
                campaign.id,
                campaign.name,
                campaign.status,
                campaign.advertising_channel_type,
                campaign.advertising_channel_sub_type,
                campaign_budget.amount_micros,
                metrics.impressions,
                metrics.clicks,
                metrics.conversions,
                metrics.cost_micros
            FROM campaign
            ORDER BY campaign.id DESC
        """

        response = ga_service.search(customer_id=cid, query=query)
        results = []
        for row in response:
            cost = row.metrics.cost_micros / 1_000_000
            conversions = row.metrics.conversions
            cpi = (cost / conversions) if conversions > 0 else 0.0
            daily_budget = row.campaign_budget.amount_micros / 1_000_000

            results.append({
                "id": str(row.campaign.id),
                "name": row.campaign.name,
                "status": row.campaign.status.name,
                "channel": row.campaign.advertising_channel_type.name,
                "daily_budget": daily_budget,
                "impressions": row.metrics.impressions,
                "clicks": row.metrics.clicks,
                "installs": conversions,
                "cost": cost,
                "cpi": cpi
            })
        return results

    def format_campaign_table(self, campaigns: List[Dict[str, Any]]) -> str:
        """
        Kampanyaları okunabilir bir tablo olarak formatlar.
        """
        if not campaigns:
            return "Henüz oluşturulmuş bir kampanya bulunamadı."

        table_data = []
        for c in campaigns:
            table_data.append([
                c["id"],
                c["name"],
                c["status"],
                f"{c['daily_budget']:.2f}",
                str(c['impressions']),
                str(c['clicks']),
                str(c['installs']),
                f"{c['cost']:.2f}",
                f"{c['cpi']:.2f}"
            ])

        headers = [
            "Kampanya ID", "Kampanya Adı", "Durum", "Bütçe/Gün",
            "Gösterim", "Tıklama", "Yükleme (Install)", "Harcama", "Gerç. CPI"
        ]

        try:
            from tabulate import tabulate
            return tabulate(table_data, headers=headers, tablefmt="github")
        except ImportError:
            # Fallback text formatter
            col_widths = [len(h) for h in headers]
            for row in table_data:
                for idx, cell in enumerate(row):
                    col_widths[idx] = max(col_widths[idx], len(str(cell)))

            header_line = " | ".join([h.ljust(col_widths[i]) for i, h in enumerate(headers)])
            separator_line = "-+-".join(["-" * col_widths[i] for i in range(len(headers))])
            rows_str = "\n".join([
                " | ".join([str(cell).ljust(col_widths[i]) for i, cell in enumerate(row)])
                for row in table_data
            ])
            return f"{header_line}\n{separator_line}\n{rows_str}"
