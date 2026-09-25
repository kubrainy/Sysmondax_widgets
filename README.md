# sysmondax_widgets

Sysmondax'ın Android ana ekran widget'ları (Şirket Bakiyesi, Vadesi Yaklaşan
Ödemeler, Son İşlemler, Yeni Fatura Oluştur, Yeni İrsaliye Oluştur) için
native (Kotlin/RemoteViews) implementasyon + ince bir Dart API.

## Bu paket ne yapar, ne yapmaz

- **Yapar:** RemoteViews render'lama, WorkManager ile periyodik yenileme
  tetikleme, widget yeniden boyutlandırma/konfigürasyon ekranları, widget
  tıklamalarını ev sahibi uygulamanın kendi ana ekranına yönlendirme.
- **Yapmaz:** Hiçbir ağ/API çağrısı yapmaz, auth/token bilgisi tutmaz. Veriyi
  nereden çekeceğinize (kendi `CompanyService`'iniz, kendi auth'unuz) siz
  karar verirsiniz; bu paket sadece size verilen veriyi ekrana basar.

Bunun nedeni: widget'ların arka planda (uygulama kapalıyken bile) veri
göstermesi gerekiyor, ama bu paket sizin gerçek backend/auth entegrasyonunuzu
bilemez — o yüzden "veri çekme" sorumluluğu tamamen sizde, "veriyi gösterme"
sorumluluğu bu pakette.

## Kurulum

```yaml
dependencies:
  sysmondax_widgets:
    git:
      url: https://github.com/kubrainy/Sysmondax_widgets.git
      ref: main   # ya da sabit bir tag/commit
```

`flutter pub get` sonrası native tarafta ekstra bir şey yapmanıza gerek yok:
widget receiver/activity kayıtları ve kaynaklar (layout, drawable, string,
color) Gradle manifest/resource merge ile uygulamanıza otomatik dahil olur.

**Not:** `minSdk`'niz en az 24 olmalı (bu paketin native modülünün
`minSdk`'si 24).

**Kaynak adı çakışması:** Bazı drawable isimleri jenerik (`ic_wallet`,
`ic_eye`, `ic_clock`, `ic_plus`, `ic_dispatch`, `ic_invoice`). Uygulamanızda
aynı isimde kaynaklar varsa (app modülünüz merge sırasında öncelikli olur,
build hata vermez ama yanlış ikon görünebilir), çakışan isimleri kontrol
edin.

## Dart API

```dart
import 'package:sysmondax_widgets/sysmondax_widgets.dart';
```

### Aktif şirket

Widget'lar ve widget tıklama yönlendirmeleri, verinin hangi şirkete ait
olduğunu bu anahtardan bilir. Kullanıcı uygulama içinde şirket
değiştirdiğinde (veya ilk girişte) çağırın:

```dart
await SysmondaxWidgets.setSelectedCompanyId(company.id);
```

### Widget'ları güncelleme

Kendi verinizi çektikten sonra (ekran açıldığında, arka plan job'ınızda,
vs.) ilgili fonksiyonu çağırın — tüm metin alanları **sizin tarafınızdan
biçimlendirilmiş** olarak beklenir (para birimi, ondalık, tarih formatı gibi
kararlar sizde):

```dart
await SysmondaxWidgets.updateDueInvoices(
  companyName: company.name,
  invoices: [
    DueInvoiceItem(
      name: invoice.actName ?? '',
      amountText: '${invoice.remTotal.toStringAsFixed(2)} ${invoice.currencySymbol}',
      dueDateText: formattedDate,
    ),
    // en fazla `dueInvoicesMaxItems` (6) adet kullanılır, fazlası yok sayılır
  ],
);

await SysmondaxWidgets.updateCompanyBalance(
  companyName: company.name,
  debitText: balance.totalDebt.toStringAsFixed(2),
  creditText: balance.totalReceivable.toStringAsFixed(2),
  cashText: balance.cashAccountRem.toStringAsFixed(2),
  currencySymbol: balance.currencySymbol,
);

await SysmondaxWidgets.updateRecentTransactions(
  companyName: company.name,
  transactions: [
    RecentTransactionItem(
      name: tx.actName ?? '',
      amountText: '${tx.total.toStringAsFixed(2)} ${tx.currencySymbol}',
      dateText: formattedDate,
      statusName: tx.statusName ?? '',   // bkz. aşağıdaki UYARI
      direction: tx.direction,           // 10 = gelen, 20 = giden
    ),
    // en fazla `recentTransactionsMaxItems` (6) adet kullanılır
  ],
);
```

> **UYARI — `statusName` birebir eşleşmeli:** Native taraf rozet rengini
> (mor/yeşil/turuncu) `statusName` değerine göre şu **birebir** string
> eşleşmesiyle seçiyor: `"Taslak"`, `"İçe Aktarma Bekleniyor"`,
> `"Gönderildi"`, `"Reddedildi"`. Başka bir değer hata vermez ama nötr
> (gri) renkte gösterilir. Kendi API'nizin durum adları farklıysa, bu
> paketin fonksiyonuna vermeden önce siz eşleyin.

## Arka plan yenileme ve tıklama yönlendirmesi kurulumu

Bu, ev sahibi uygulamada yazmanız gereken **tek** entegrasyon parçası.
`home_widget`'ın arka plan callback mekanizması uygulama başına tek bir
top-level fonksiyon bekler ve bu fonksiyon her tetiklendiğinde ayrı, taze bir
Dart isolate'inde çalışır — yani önceden "hazırlanmış" bir servis nesnesini
hatırlayamaz, kendi gerçek servislerinizi (auth dahil) doğrudan bu fonksiyon
içinde çağırmanız gerekir.

`main.dart`'ınızda:

```dart
import 'package:home_widget/home_widget.dart';
import 'package:sysmondax_widgets/sysmondax_widgets.dart';

@pragma('vm:entry-point')
Future<void> widgetBackgroundDispatcher(Uri? uri) async {
  WidgetsFlutterBinding.ensureInitialized();

  switch (uri?.path) {
    case SysmondaxWidgetUris.dueInvoicesRefresh:
      await _refreshDueInvoices();
      break;
    case SysmondaxWidgetUris.companyBalanceRefresh:
      await _refreshCompanyBalance();
      break;
    case SysmondaxWidgetUris.recentTransactionsRefresh:
      await _refreshRecentTransactions();
      break;
  }
}

Future<void> _refreshDueInvoices() async {
  final companyId = await SysmondaxWidgets.getSelectedCompanyId();
  if (companyId == null) return;
  try {
    // Kendi gerçek servisleriniz burada:
    final company = await YourCompanyService().getCompany(companyId);
    final invoices = await YourInvoiceService().getUpcomingDueInvoices(companyId);
    await SysmondaxWidgets.updateDueInvoices(
      companyName: company.name,
      invoices: invoices.map(/* ... */).toList(),
    );
  } catch (_) {
    // Ağ yok / oturum yenilenemedi: widget eski veriyi göstermeye devam
    // etsin, sessizce vazgeç.
  }
}

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  HomeWidget.registerInteractivityCallback(widgetBackgroundDispatcher);
  runApp(const YourApp());
}
```

Kısayol widget'larına (Yeni Fatura Oluştur, Yeni İrsaliye Oluştur, Gelen/
Giden Faturaları Gör) tıklanınca uygulama aynı URI şemasıyla açılır; bunu
uygulamanızın launch/deep-link akışında (`HomeWidget.initiallyLaunchedFromHomeWidget()`
ve `HomeWidget.widgetClicked` stream'i) yakalayıp ilgili ekranı açmalısınız:

```dart
void _handleWidgetUri(Uri? uri) {
  switch (uri?.path) {
    case SysmondaxWidgetUris.openNewInvoiceScreen:
      // Fatura oluşturma ekranınızı açın
    case SysmondaxWidgetUris.openNewDispatchScreen:
      // İrsaliye oluşturma ekranınızı açın
    case SysmondaxWidgetUris.openIncomingInvoicesScreen:
      // Gelen faturalar ekranınızı açın
    case SysmondaxWidgetUris.openOutgoingInvoicesScreen:
      // Giden faturalar ekranınızı açın
  }
}
```

## Bilinen sınırlamalar / henüz doğrulanmamış noktalar

- Bu paket bir prototipten (widget_try) çıkarıldı; native taraf (RemoteViews,
  WorkManager, config ekranları) gerçek cihazda test edildi. Bu repodaki
  veri-çekme örnek kodu **yoktur** — o kısım sizin gerçek servislerinizle
  bu paketin API'sine bağlanmanızla oluşacak, henüz uçtan uca sizin
  backend'inizle test edilmedi.
- Kısayol widget'larının (Yeni Fatura/İrsaliye) "Gelen/Giden Faturaları Gör"
  seçimi widget'a özel `SharedPreferences` dosyasında (`new_invoice_widget_prefs`,
  `new_dispatch_widget_prefs`) tutulur; uygulama verisiyle ilgisi yoktur.
