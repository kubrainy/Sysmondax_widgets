/// Vadesi yaklaşan bir fatura satırı. Tüm metin alanları ev sahibi
/// uygulama tarafından zaten biçimlendirilmiş halde verilmeli (para birimi,
/// ondalık basamak, tarih formatı gibi kararlar burada değil, çağıran tarafta
/// alınır) — bu paket sadece gösterir, biçimlendirmez.
class DueInvoiceItem {
  final String name;
  final String amountText;
  final String dueDateText;

  const DueInvoiceItem({
    required this.name,
    required this.amountText,
    required this.dueDateText,
  });
}

/// "Son İşlemler" widget'ında bir satır.
///
/// [statusName], native tarafta (`RecentTransactionsWidgetProvider.statusStyle`)
/// rozet rengini seçmek için birebir eşleşen şu değerlerden biri olmalı:
/// "Taslak", "İçe Aktarma Bekleniyor", "Gönderildi", "Reddedildi". Başka bir
/// değer gelirse rozet nötr (gri) renkte gösterilir — hata vermez ama
/// istenen rengi göstermez.
///
/// [direction] 10 = gelen, 20 = giden.
class RecentTransactionItem {
  final String name;
  final String amountText;
  final String dateText;
  final String statusName;
  final int direction;

  const RecentTransactionItem({
    required this.name,
    required this.amountText,
    required this.dateText,
    required this.statusName,
    required this.direction,
  });
}
