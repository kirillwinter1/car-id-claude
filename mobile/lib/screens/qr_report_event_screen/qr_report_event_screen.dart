import 'package:app/controllers/qr_report_event_controller.dart';
import 'package:app/screens/qr_report_event_screen/components/qr_report_scanner_widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';
import 'package:get/get.dart';

import '../../utils/res/svg_assets.dart';
import '../../utils/scale_config.dart';
import '../../widgets/app_bars/title_app_bar.dart';
import '../../widgets/buttons/torch_button.dart';

class QRReportEventScreen extends StatelessWidget {
  const QRReportEventScreen({super.key});

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
          body: controller.accessGranted
              ? Stack(
                  children: [
                    QRReportScannerWidget(controller),
                    Positioned.fill(
                      child: Center(
                        child: SvgPicture.asset(SvgAssets.scannerAim),
                      ),
                    ),
                    Positioned(
                      bottom: SC.s(42.0),
                      left: Get.width * 0.5 - SC.s22,
                      child: TorchButton(
                        isOn: controller.torchIsOn,
                        onTap: () => controller.onTorchTap(),
                      ),
                    ),
                  ],
                )
              : const SizedBox(),
        );
      },
    );
  }
}
