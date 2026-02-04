import 'package:app/controllers/auth_controller.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/widgets/text_fields/custom_text_field.dart';
import 'package:flutter/material.dart';

class PhoneLoginWidget extends StatelessWidget {
  final AuthController ctrl;
  const PhoneLoginWidget(this.ctrl, {super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Ваш номер',
          style: TextStyles.authHeader,
        ),
        SizedBox(height: SC.s4),
        Text(
          'Для добавления метки введите номер телефона',
          style: TextStyles.authDescription,
        ),
        SizedBox(height: SC.s16),
        CustomTextField(
          controller: ctrl.phoneController,
          focusNode: ctrl.phoneInputFocusNode,
          inputFormatters: [ctrl.phoneMaskFormatter],
          autofocus: true,
          keyboardType: TextInputType.phone,
          prefix: Text('+7 ', style: TextStyles.authTextField),
          isValid: ctrl.isPhoneValid,
        ),

        // Error
        ctrl.isPhoneValid
            ? SizedBox(height: SC.s28)
            : Padding(
                padding: EdgeInsets.only(top: SC.s(4.5), bottom: SC.s(8.5)),
                child: Text(
                  'Ошибка при вводе телефона',
                  style: TextStyles.authInputError,
                ),
              ),
      ],
    );
  }
}
