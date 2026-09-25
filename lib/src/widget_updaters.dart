import 'package:home_widget/home_widget.dart';

import 'models.dart';

/// Native tarafta due_invoice_widget.xml'deki en fazla satır sayısıyla
/// (due_item1..6_row) eşleşmeli — değiştirilecekse ikisi birlikte
/// güncellenmeli.
const int dueInvoicesMaxItems = 6;

/// Native tarafta son_islemler_widget.xml'deki en fazla satır sayısıyla
/// (son_item1..6_row) eşleşmeli.
const int recentTransactionsMaxItems = 6;

class DueInvoicesWidget {
  // Bu sınıf artık ev sahibi uygulamanın kendi paketinde değil, bu plugin'in
  // paketinde yaşıyor — bu yüzden `androidName` (paket adı ev sahibi
  // uygulamanınkiyle varsayılır) yerine tam nitelikli `qualifiedAndroidName`
  // kullanılmalı, yoksa home_widget yanlış (var olmayan) sınıfı arar.
  static const String _providerName =
      'io.github.kubrainy.sysmondax_widgets.DueInvoicesWidgetProvider';

  static Future<void> update({
    required String companyName,
    required List<DueInvoiceItem> invoices,
  }) async {
    await HomeWidget.saveWidgetData<String>('due_company_name', companyName);

    for (var i = 0; i < dueInvoicesMaxItems; i++) {
      final item = i < invoices.length ? invoices[i] : null;
      await HomeWidget.saveWidgetData<String>(
        'due_item${i + 1}_name',
        item?.name ?? '',
      );
      await HomeWidget.saveWidgetData<String>(
        'due_item${i + 1}_amount',
        item?.amountText ?? '',
      );
      await HomeWidget.saveWidgetData<String>(
        'due_item${i + 1}_date',
        item?.dueDateText ?? '',
      );
    }

    await HomeWidget.updateWidget(qualifiedAndroidName: _providerName);
  }
}

class CompanyBalanceWidget {
  static const String _providerName =
      'io.github.kubrainy.sysmondax_widgets.CompanyBalanceWidgetProvider';

  static Future<void> update({
    required String companyName,
    required String debitText,
    required String creditText,
    required String cashText,
    required String currencySymbol,
  }) async {
    await HomeWidget.saveWidgetData<String>('balance_company_name', companyName);
    await HomeWidget.saveWidgetData<String>('balance_debit', debitText);
    await HomeWidget.saveWidgetData<String>('balance_credit', creditText);
    await HomeWidget.saveWidgetData<String>('balance_cash', cashText);
    await HomeWidget.saveWidgetData<String>(
      'balance_currency_symbol',
      currencySymbol,
    );

    await HomeWidget.updateWidget(qualifiedAndroidName: _providerName);
  }
}

class RecentTransactionsWidget {
  static const String _providerName =
      'io.github.kubrainy.sysmondax_widgets.RecentTransactionsWidgetProvider';

  static Future<void> update({
    required String companyName,
    required List<RecentTransactionItem> transactions,
  }) async {
    await HomeWidget.saveWidgetData<String>('son_company_name', companyName);

    for (var i = 0; i < recentTransactionsMaxItems; i++) {
      final item = i < transactions.length ? transactions[i] : null;
      await HomeWidget.saveWidgetData<String>(
        'son_item${i + 1}_name',
        item?.name ?? '',
      );
      await HomeWidget.saveWidgetData<String>(
        'son_item${i + 1}_amount',
        item?.amountText ?? '',
      );
      await HomeWidget.saveWidgetData<String>(
        'son_item${i + 1}_date',
        item?.dateText ?? '',
      );
      await HomeWidget.saveWidgetData<String>(
        'son_item${i + 1}_status',
        item?.statusName ?? '',
      );
      await HomeWidget.saveWidgetData<String>(
        'son_item${i + 1}_direction',
        (item?.direction ?? 10).toString(),
      );
    }

    await HomeWidget.updateWidget(qualifiedAndroidName: _providerName);
  }
}
