// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter_test/flutter_test.dart';

import 'package:sysmondax_widgets_example/main.dart';

void main() {
  testWidgets('Widget verisi yazma akışı UI\'da yansıyor', (WidgetTester tester) async {
    // Build our app and let the async widget-data write settle.
    // Not running on Android, so the underlying platform calls no-op.
    await tester.pumpWidget(const MyApp());
    await tester.pumpAndSettle();

    expect(find.textContaining('Widget verisi yazıldı'), findsOneWidget);
  });
}
