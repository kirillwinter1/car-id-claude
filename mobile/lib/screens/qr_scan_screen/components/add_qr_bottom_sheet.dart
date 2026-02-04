import 'package:app/controllers/qr_scan_controller.dart';
import 'package:app/widgets/text_fields/custom_text_field.dart';
import 'package:flutter/cupertino.dart';
import 'package:get/get.dart';

import '../../../utils/scale_config.dart';
import '../../../widgets/buttons/solid_button.dart';

class AddQRBottomSheet extends StatelessWidget {
  final QRScanController controller;

  const AddQRBottomSheet({
    required this.controller,
    Key? key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return GetBuilder<QRScanController>(builder: (controller) {
      return Padding(
        padding: EdgeInsets.fromLTRB(SC.s16, 0.0, SC.s16, SC.s16),
        child: Column(
          children: [
            CustomTextField(
              controller: controller.nameTextController,
              focusNode: controller.nameFocus,
              isValid: controller.nameTextValid,
              enabledBorderColor: const Color(0xFFE86C4A),
              textCapitalization: TextCapitalization.sentences,
            ),
            SizedBox(height: SC.s16),
            SolidButton(
              title: 'Сохранить',
              onPressed: controller.buttonActive
                  ? () => controller.onSaveButtonTap()
                  : null,
              padding: EdgeInsets.only(top: SC.s12),
            )
          ],
        ),
      );
    });
  }
}
