// This is a basic Flutter integration test.
//
// Since integration tests run in a full Flutter application, they can interact
// with the host side of a plugin implementation, unlike Dart unit tests.
//
// For more information about Flutter integration tests, please see
// https://flutter.dev/to/integration-testing

import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'package:sysmondax_widgets/sysmondax_widgets.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('updateCompanyBalance cihazda hatasız çalışır ve veri okunabilir',
      (WidgetTester tester) async {
    await SysmondaxWidgets.setSelectedCompanyId('integration-test-company');
    await SysmondaxWidgets.updateCompanyBalance(
      companyName: 'Test A.Ş.',
      debitText: '100,00',
      creditText: '50,00',
      cashText: '25,00',
      currencySymbol: '₺',
    );

    final companyId = await SysmondaxWidgets.getSelectedCompanyId();
    expect(companyId, 'integration-test-company');
  });
}
