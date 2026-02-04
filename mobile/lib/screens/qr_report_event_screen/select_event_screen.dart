import 'package:app/controllers/qr_report_event_controller.dart';
import 'package:app/screens/qr_report_event_screen/components/report_event_widget.dart';
import 'package:app/widgets/buttons/solid_button.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../utils/scale_config.dart';
import '../../widgets/app_bars/title_app_bar.dart';

class SelectEventScreen extends StatelessWidget {
  const SelectEventScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return GetBuilder<QRReportEventController>(
      init: QRReportEventController(),
      builder: (controller) {
        return Scaffold(
          resizeToAvoidBottomInset: true,
          appBar: TitleAppBar(
            title: 'Сообщить о событии',
            actionsItemPadding: EdgeInsets.fromLTRB(0, SC.s8, SC.s20, SC.s8),
            actions: const [],
          ),
          body: SingleChildScrollView(
            child: Padding(
              padding: EdgeInsets.symmetric(horizontal: SC.s16),
              child: Column(
                children: controller.events
                    .map(
                      (e) => ReportEventWidget(
                          event: e,
                          isSelected: e == controller.selectedEvent,
                          onTap: () => controller.onEventTap(e)),
                    )
                    .toList(),
              ),
            ),
          ),
          bottomNavigationBar: Container(
            width: double.infinity,
            padding: EdgeInsets.symmetric(vertical: SC.s16),
            child: SolidButton(
              title: 'Сообщить',
              onPressed: controller.selectedEvent != null
                  ? () => controller.onReportButtonTap()
                  : null,
            ),
          ),
        );
      },
    );
  }
}
