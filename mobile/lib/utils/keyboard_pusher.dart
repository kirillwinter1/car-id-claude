import 'package:app/utils/scale_config.dart';
import 'package:flutter/cupertino.dart';
import 'package:get/get.dart';

import '../controllers/app_controller.dart';
import 'gms_hms.dart';

class KeyboardPusher extends StatelessWidget {
  final AppController appController = Get.find();

  KeyboardPusher({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (GetPlatform.isIOS) {
      return Obx(
        () => AnimatedContainer(
          curve: Curves.easeOutCubic,
          duration: const Duration(milliseconds: 300),
          height: appController.keyboardHeight.value > 0
              ? appController.keyboardHeight.value + SC.s8
              : SC.bottomPadding,
        ),
      );
    } else {
      if (GmsHms.isHmsAvailable) {
        return AnimatedContainer(
          curve: Curves.easeOut,
          duration: const Duration(milliseconds: 150),
          height: Get.mediaQuery.viewInsets.bottom > SC.initialBottomPadding
              ? SC.s8 + Get.mediaQuery.viewInsets.bottom
              : SC.bottomPadding,
        );
      } else {
        return Container(
          height: Get.mediaQuery.viewInsets.bottom > SC.initialBottomPadding
              ? SC.s8 + Get.mediaQuery.viewInsets.bottom
              : SC.bottomPadding,
        );
      }
    }
  }
}
