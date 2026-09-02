/**
 * ==============================================================================
 * LIMITRA: APPBLOCK - GOOGLE ADS OTOMATİK KAMPANYA OLUŞTURMA SCRIPTI (GÜNCEL)
 * Web Sitesi: https://limitra.online/
 * Uygulama: com.gardiyan.app (Google Play Store)
 * ==============================================================================
 */

const CONFIG = {
  // Uygulama ve Kampanya Bilgileri
  appId: 'com.gardiyan.app',
  campaignName: '[UAC] Limitra: AppBlock - TR Ekran Suresi ve Odaklanma',
  
  // Bütçe ve Teklif Ayarları (TL)
  dailyBudgetAmount: 100.00,       // Günlük Bütçe (TL)
  targetCpi: 2.50,                 // Hedef Yükleme Başı Maliyet (tCPI - TL)
  
  // Hedefleme
  targetCountry: 'Turkey',         // Hedef Ülke
  targetLanguage: 'Turkish',       // Hedef Dil

  // Reklam Başlıkları (Maksimum 30 Karakter)
  headlines: [
    'Limitra: AppBlock',           // 17 kar.
    'Kararını Şimdi Ver',          // 18 kar.
    'Ekran Süreni Kontrol Et',     // 23 kar.
    '21 Gün Disiplin Zinciri',     // 23 kar.
    'Odaklan ve Zamanını Koru'      // 24 kar.
  ],

  // Reklam Açıklamaları (Maksimum 90 Karakter)
  descriptions: [
    'Sınırını zihnin sakinken belirle. Dikkatin dağıldığında Limitra kararına sadık tutsun.', // 87 kar.
    'Ne zaman durman gerektiğini biliyorsun. Limitra ile iradeye değil sisteme güven.',       // 83 kar.
    'Sosyal medyada kaybolmayı bırak. 21 günlük disiplin zinciriyle hedeflerine odaklan.',     // 84 kar.
    'Süre dolduğunda uygulama anında kilitlenir. Verilerin tamamen cihazında ve gizli kalır.', // 88 kar.
    'Daha az pazarlık, daha çok hayat. Limitra App Block\'u Google Play\'den şimdi indirin.'  // 85 kar.
  ]
};

function main() {
  Logger.log('====================================================');
  Logger.log('  LIMITRA: APPBLOCK - GOOGLE ADS KAMPANYA KURULUMU  ');
  Logger.log('====================================================');
  Logger.log('Hedef Uygulama: ' + CONFIG.appId);
  Logger.log('Kampanya Adı: ' + CONFIG.campaignName);
  Logger.log('Günlük Bütçe: ' + CONFIG.dailyBudgetAmount + ' TL');
  Logger.log('Hedef CPI: ' + CONFIG.targetCpi + ' TL');
  Logger.log('----------------------------------------------------');

  const columns = [
    'Action',
    'Campaign',
    'Budget',
    'Campaign type',
    'App ID',
    'App store',
    'Campaign status',
    'Bidding strategy type',
    'Target CPI',
    'Ad Group',
    'Headline 1',
    'Headline 2',
    'Headline 3',
    'Headline 4',
    'Headline 5',
    'Description 1',
    'Description 2',
    'Description 3',
    'Description 4',
    'Description 5',
    'Location',
    'Language'
  ];

  Logger.log('[1/2] Toplu yükleme tablosu oluşturuluyor...');
  const upload = AdsApp.bulkUploads().newCsvUpload(columns, {
    moneyInMicros: false
  });

  upload.append({
    'Action': 'Add',
    'Campaign': CONFIG.campaignName,
    'Budget': CONFIG.dailyBudgetAmount,
    'Campaign type': 'App',
    'App ID': CONFIG.appId,
    'App store': 'Google Play',
    'Campaign status': 'Paused', // Güvenlik için duraklatılmış başlar
    'Bidding strategy type': 'Target CPI',
    'Target CPI': CONFIG.targetCpi,
    'Ad Group': CONFIG.campaignName + ' - Reklam Grubu',
    'Headline 1': CONFIG.headlines[0] || '',
    'Headline 2': CONFIG.headlines[1] || '',
    'Headline 3': CONFIG.headlines[2] || '',
    'Headline 4': CONFIG.headlines[3] || '',
    'Headline 5': CONFIG.headlines[4] || '',
    'Description 1': CONFIG.descriptions[0] || '',
    'Description 2': CONFIG.descriptions[1] || '',
    'Description 3': CONFIG.descriptions[2] || '',
    'Description 4': CONFIG.descriptions[3] || '',
    'Description 5': CONFIG.descriptions[4] || '',
    'Location': CONFIG.targetCountry,
    'Language': CONFIG.targetLanguage
  });

  Logger.log('[2/2] Kampanya hesaba aktarılıyor...');
  upload.forCampaignManagement();
  upload.apply();

  Logger.log('====================================================');
  Logger.log(' [✓] Kampanya başarıyla hesaba gönderildi!');
  Logger.log(' Not: Kampanya "Duraklatıldı (Paused)" olarak açıldı.');
  Logger.log(' Sol menüdeki "Kampanyalar" sekmesinden kontrol edebilirsiniz.');
  Logger.log('====================================================');
}
