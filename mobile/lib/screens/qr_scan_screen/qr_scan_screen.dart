import 'package:app/controllers/qr_scan_controller.dart';
import 'package:app/screens/qr_scan_screen/components/scanner_widget.dart';
import 'package:app/widgets/buttons/torch_button.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';
import 'package:get/get.dart';

import '../../utils/res/svg_assets.dart';
import '../../utils/scale_config.dart';
import '../../widgets/app_bars/title_app_bar.dart';

class QrScanScreen extends StatelessWidget {
  const QrScanScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    bool openFromQRMarksScreen = Get.arguments ?? false;

    return GetBuilder<QRScanController>(
      init: QRScanController(openFromQRMarkScreen: openFromQRMarksScreen),
      builder: (controller) {
        return Scaffold(
          resizeToAvoidBottomInset: false,
          appBar: TitleAppBar(
            title: 'Добавление метки',
            actionsItemPadding: EdgeInsets.fromLTRB(0, SC.s8, SC.s20, SC.s8),
            actions: const [],
          ),
          body: controller.accessGranted
              ? Stack(
                  children: [
                    ScannerWidget(controller),
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
