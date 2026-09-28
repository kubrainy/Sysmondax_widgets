# sysmondax_widgets

Sysmondax'ın Android ana ekran widget'ları (Şirket Bakiyesi, Vadesi Yaklaşan
Ödemeler, Son İşlemler, Yeni Fatura Oluştur, Yeni İrsaliye Oluştur) için
native (Kotlin/RemoteViews) implementasyon + ince bir Dart API. Ağ/API
çağrısı yapmaz, sadece kendisine verilen veriyi ekrana basar — veriyi
çekmek (auth dahil) ev sahibi uygulamaya ait.

## Kurulum

```yaml
dependencies:
  sysmondax_widgets:
    git:
      url: https://github.com/kubrainy/Sysmondax_widgets.git
      ref: main   # ya da sabit bir tag/commit
```

`flutter pub get` yeterli, native kayıtlar Gradle merge ile otomatik gelir.
`minSdk` en az 24 olmalı.

## Dart API

```dart
import 'package:sysmondax_widgets/sysmondax_widgets.dart';

// Aktif şirket değiştiğinde:
await SysmondaxWidgets.setSelectedCompanyId(company.id);

// Verinizi çektikten sonra — tüm metinler SİZİN biçimlendirdiğiniz halde:
await SysmondaxWidgets.updateDueInvoices(
  companyName: company.name,
  invoices: [
    DueInvoiceItem(name: '...', amountText: '1.234,56 ₺', dueDateText: '05.10.2026'),
    // en fazla `dueInvoicesMaxItems` (6) kullanılır
  ],
);

await SysmondaxWidgets.updateCompanyBalance(
  companyName: company.name,
  debitText: '...', creditText: '...', cashText: '...', currencySymbol: '₺',
);

await SysmondaxWidgets.updateRecentTransactions(
  companyName: company.name,
  transactions: [
    RecentTransactionItem(
      name: '...', amountText: '...', dateText: '...',
      statusName: '...',   // bkz. aşağıdaki UYARI
      direction: 20,        // 10 = gelen, 20 = giden
    ),
  ],
);

// Çıkışta / oturum sonunda eski verinin görünmemesi için:
await SysmondaxWidgets.clear();
```

> **UYARI:** `statusName` rozet rengini şu **birebir** eşleşmeyle seçer:
> `"Taslak"`, `"İçe Aktarma Bekleniyor"`, `"Gönderildi"`, `"Reddedildi"`.
> Başka değer hata vermez ama nötr (gri) gösterilir.

## Arka plan yenileme ve tıklama yönlendirmesi

`home_widget`'ın arka plan callback'i tek bir top-level fonksiyon bekler ve
her tetiklendiğinde taze bir isolate'te çalışır — gerçek servislerinizi
doğrudan bu fonksiyon içinde çağırın.

```dart
@pragma('vm:entry-point')
Future<void> widgetBackgroundDispatcher(Uri? uri) async {
  WidgetsFlutterBinding.ensureInitialized();
  if (!SysmondaxWidgetUris.isRefresh(uri?.path)) return;

  final companyId = await SysmondaxWidgets.getSelectedCompanyId();
  if (companyId == null) return;
  try {
    // Kendi gerçek servisleriniz burada, sonra ilgili update*() çağrısı.
  } catch (_) {
    // Ağ yok / oturum yenilenemedi: widget eski veriyi göstermeye devam etsin.
  }
}

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  SysmondaxWidgets.registerBackgroundCallback(widgetBackgroundDispatcher);
  runApp(const YourApp());
}
```

Kısayol widget'larına tıklanınca uygulama aynı URI şemasıyla açılır —
`SysmondaxWidgets.bindLaunches` soğuk başlangıcı ve arka plandan öne gelmeyi
tek callback'te yakalar:

```dart
@override
void initState() {
  super.initState();
  SysmondaxWidgets.bindLaunches((uri) {
    if (!SysmondaxWidgetUris.isNavigation(uri.path)) return;
    switch (uri.path) {
      case SysmondaxWidgetUris.openNewInvoiceScreen:
        // Fatura oluşturma ekranınızı açın
      case SysmondaxWidgetUris.openNewDispatchScreen:
        // İrsaliye oluşturma ekranınızı açın
      case SysmondaxWidgetUris.openIncomingInvoicesScreen:
        // Gelen faturalar ekranınızı açın
      case SysmondaxWidgetUris.openOutgoingInvoicesScreen:
        // Giden faturalar ekranınızı açın
    }
  });
}
```
