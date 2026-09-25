import 'package:home_widget/home_widget.dart';

import 'src/models.dart';
import 'src/widget_updaters.dart';

export 'src/models.dart';
export 'src/widget_updaters.dart' show dueInvoicesMaxItems, recentTransactionsMaxItems;

/// Sysmondax ana ekran widget'larının Dart tarafı: SharedPreferences'a veri
/// yazıp native RemoteViews'i günceller. Bu paket kendi başına hiçbir ağ
/// çağrısı yapmaz — veriyi nereden/nasıl çekeceğine (auth, API) ev sahibi
/// uygulama karar verir, bu sınıfa sadece hazır veriyi verir.
///
/// Entegrasyon sözleşmesi için bkz. README.md.
class SysmondaxWidgets {
  SysmondaxWidgets._();

  /// Widget'ların hangi şirkete ait veri gösterdiğini işaretlemek için
  /// kullanılan ortak anahtar. Widget tıklamaları (örn. "Gelen Faturaları
  /// Gör") ve arka plan yenileme akışı, hangi şirket için ekran/veri
  /// açacağını bu anahtardan okur.
  static const String selectedCompanyIdKey = 'selected_company_id';

  /// Kullanıcı uygulama içinde aktif şirketi değiştirdiğinde (veya ilk
  /// girişte) çağrılmalı — widget'ların ve widget tıklama yönlendirmelerinin
  /// doğru şirketi bilmesi için gereklidir.
  static Future<void> setSelectedCompanyId(String companyId) {
    return HomeWidget.saveWidgetData<String>(selectedCompanyIdKey, companyId);
  }

  /// Arka planda çalışan yenileme kodunun (bkz. README > "Arka plan
  /// yenileme") hangi şirket için veri çekeceğini öğrenmek için kullanılır.
  static Future<String?> getSelectedCompanyId() {
    return HomeWidget.getWidgetData<String>(selectedCompanyIdKey);
  }

  /// "Vadesi Yaklaşan Ödemeler" widget'ını verilen veriyle günceller.
  static Future<void> updateDueInvoices({
    required String companyName,
    required List<DueInvoiceItem> invoices,
  }) {
    return DueInvoicesWidget.update(companyName: companyName, invoices: invoices);
  }

  /// "Şirket Bakiyesi" widget'ını verilen veriyle günceller. Alanlar zaten
  /// biçimlendirilmiş metin olarak beklenir (örn. debitText: "1.234,56").
  static Future<void> updateCompanyBalance({
    required String companyName,
    required String debitText,
    required String creditText,
    required String cashText,
    required String currencySymbol,
  }) {
    return CompanyBalanceWidget.update(
      companyName: companyName,
      debitText: debitText,
      creditText: creditText,
      cashText: cashText,
      currencySymbol: currencySymbol,
    );
  }

  /// "Son İşlemler" widget'ını verilen veriyle günceller.
  static Future<void> updateRecentTransactions({
    required String companyName,
    required List<RecentTransactionItem> transactions,
  }) {
    return RecentTransactionsWidget.update(
      companyName: companyName,
      transactions: transactions,
    );
  }
}

/// Native tarafın `HomeWidgetBackgroundIntent` ile Dart'a gönderdiği,
/// `app://widget/...` biçimindeki URI'lerin path kısımları. Ev sahibi
/// uygulama kendi `HomeWidget.registerInteractivityCallback` fonksiyonunda
/// bu sabitlere göre switch yapmalı — bkz. README.md > "Arka plan yenileme
/// ve tıklama yönlendirmesi kurulumu".
abstract final class SysmondaxWidgetUris {
  /// Arka plan yenileme tetikleyicileri (WorkManager tarafından periyodik
  /// olarak veya sistem widget'ı yenilemek istediğinde gönderilir).
  static const String dueInvoicesRefresh = '/dueinvoices';
  static const String companyBalanceRefresh = '/companybalance';
  static const String recentTransactionsRefresh = '/recenttransactions';

  /// Kısayol widget'larına tıklanınca gönderilen, uygulamanın belirli bir
  /// ekranı açması gereken URI'ler.
  static const String openNewInvoiceScreen = '/newinvoice';
  static const String openNewDispatchScreen = '/newdispatch';
  static const String openIncomingInvoicesScreen = '/incominginvoices';
  static const String openOutgoingInvoicesScreen = '/outgoinginvoices';
}
