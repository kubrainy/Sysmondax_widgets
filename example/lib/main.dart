import 'package:flutter/material.dart';
import 'package:sysmondax_widgets/sysmondax_widgets.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _status = 'Widget verisi yazılıyor...';

  @override
  void initState() {
    super.initState();
    _writeSampleWidgetData();
  }

  Future<void> _writeSampleWidgetData() async {
    await SysmondaxWidgets.setSelectedCompanyId('demo-company');

    await SysmondaxWidgets.updateCompanyBalance(
      companyName: 'Demo Ticaret A.Ş.',
      debitText: '1.234,56',
      creditText: '789,00',
      cashText: '2.000,00',
      currencySymbol: '₺',
    );

    await SysmondaxWidgets.updateDueInvoices(
      companyName: 'Demo Ticaret A.Ş.',
      invoices: const [
        DueInvoiceItem(name: 'ABC Ltd.', amountText: '1.200,00 ₺', dueDateText: '05.10.2026'),
      ],
    );

    await SysmondaxWidgets.updateRecentTransactions(
      companyName: 'Demo Ticaret A.Ş.',
      transactions: const [
        RecentTransactionItem(
          name: 'ABC Ltd.',
          amountText: '1.200,00 ₺',
          dateText: '28.09.2026',
          statusName: 'Gönderildi',
          direction: 20,
        ),
      ],
    );

    if (!mounted) return;
    setState(() {
      _status = 'Widget verisi yazıldı. Ana ekrana widget ekleyip kontrol edin.';
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('sysmondax_widgets example')),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Text(_status, textAlign: TextAlign.center),
          ),
        ),
      ),
    );
  }
}
