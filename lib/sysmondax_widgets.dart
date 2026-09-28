import 'dart:async';
import 'dart:io';

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

  // Abonelik tutulmazsa tıklama dinleyicisi toplanabilir.
  // ignore: unused_field
  static StreamSubscription<Uri?>? _launches;
  static bool _launchesBound = false;

  static bool get isSupported => Platform.isAndroid;

  /// WorkManager'ın uyandırdığı arka plan callback'ini kaydeder.
  /// [callback] top-level olmalı ve `@pragma('vm:entry-point')` taşımalı.
  static Future<void> registerBackgroundCallback(
    Future<void> Function(Uri?) callback,
  ) async {
    if (!isSupported) return;
    await HomeWidget.registerInteractivityCallback(callback);
  }

  /// Widget tıklamasıyla açılan URI'leri verir. Android dışında no-op.
  static Future<void> bindLaunches(void Function(Uri uri) onLaunch) async {
    if (!isSupported || _launchesBound) return;
    _launchesBound = true;

    final initial = await HomeWidget.initiallyLaunchedFromHomeWidget();
    if (initial != null) onLaunch(initial);

    _launches = HomeWidget.widgetClicked.listen((uri) {
      if (uri != null) onLaunch(uri);
    });
  }

  /// Widget verisini boşaltır. Çıkışta çağrılmalı.
  static Future<void> clear() async {
    if (!isSupported) return;
    await updateCompanyBalance(
      companyName: '',
      debitText: '',
      creditText: '',
      cashText: '',
      currencySymbol: '',
    );
    await updateDueInvoices(companyName: '', invoices: const []);
    await updateRecentTransactions(companyName: '', transactions: const []);
  }

  /// Widget'ların hangi şirkete ait veri gösterdiğini işaretlemek için
  /// kullanılan ortak anahtar. Widget tıklamaları (örn. "Gelen Faturaları
  /// Gör") ve arka plan yenileme akışı, hangi şirket için ekran/veri
  /// açacağını bu anahtardan okur.
  static const String selectedCompanyIdKey = 'selected_company_id';

  /// Kullanıcı uygulama içinde aktif şirketi değiştirdiğinde (veya ilk
  /// girişte) çağrılmalı — widget'ların ve widget tıklama yönlendirmelerinin
  /// doğru şirketi bilmesi için gereklidir.
  static Future<void> setSelectedCompanyId(String companyId) async {
    if (!isSupported || companyId.isEmpty) return;
    await HomeWidget.saveWidgetData<String>(selectedCompanyIdKey, companyId);
  }

  /// Arka planda çalışan yenileme kodunun (bkz. README > "Arka plan
  /// yenileme") hangi şirket için veri çekeceğini öğrenmek için kullanılır.
  static Future<String?> getSelectedCompanyId() async {
    if (!isSupported) return null;
    return HomeWidget.getWidgetData<String>(selectedCompanyIdKey);
  }

  /// "Vadesi Yaklaşan Ödemeler" widget'ını verilen veriyle günceller.
  static Future<void> updateDueInvoices({
    required String companyName,
    required List<DueInvoiceItem> invoices,
  }) async {
    if (!isSupported) return;
    await DueInvoicesWidget.update(companyName: companyName, invoices: invoices);
  }

  /// "Şirket Bakiyesi" widget'ını verilen veriyle günceller. Alanlar zaten
  /// biçimlendirilmiş metin olarak beklenir (örn. debitText: "1.234,56").
  static Future<void> updateCompanyBalance({
    required String companyName,
    required String debitText,
    required String creditText,
    required String cashText,
    required String currencySymbol,
  }) async {
    if (!isSupported) return;
    await CompanyBalanceWidget.update(
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
  }) async {
    if (!isSupported) return;
    await RecentTransactionsWidget.update(
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
  static const String openNewInvoiceScreen = '/outgoing-invoice-form';
  static const String openNewDispatchScreen = '/outgoing-despatch-form-screen';
  static const String openIncomingInvoicesScreen = '/incoming-invoice-list';
  static const String openOutgoingInvoicesScreen = '/outgoing-invoice-list';

  static const Set<String> refreshPaths = {
    dueInvoicesRefresh,
    companyBalanceRefresh,
    recentTransactionsRefresh,
  };

  static const Set<String> navigationPaths = {
    openNewInvoiceScreen,
    openNewDispatchScreen,
    openIncomingInvoicesScreen,
    openOutgoingInvoicesScreen,
  };

  static bool isRefresh(String? path) => path != null && refreshPaths.contains(path);

  static bool isNavigation(String? path) =>
      path != null && navigationPaths.contains(path);
}
