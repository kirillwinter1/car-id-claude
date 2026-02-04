import 'package:app/controllers/auth_controller.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';
import 'package:pin_code_fields/pin_code_fields.dart';

class CodeVerifyWidget extends StatelessWidget {
  final AuthController ctrl;
  const CodeVerifyWidget(this.ctrl, {super.key});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Вам поступит звонок',
          style: TextStyles.authHeader,
        ),
        SizedBox(height: SC.s4),
        RichText(
            text: TextSpan(
          text: 'Введите последние 4 цифры номера, с которого Вам позвонят на ',
          style: TextStyles.authDescription,
          children: [
            TextSpan(
                text: '+7 ${ctrl.phoneMaskFormatter.getMaskedText()}',
                style: TextStyles.authDescription
                    .copyWith(fontWeight: FontWeight.w500))
          ],
        )),

        SizedBox(height: SC.s16),
        PinCodeTextField(
          enableActiveFill: true,
          autovalidateMode: AutovalidateMode.disabled,
          controller: ctrl.codeController,
          focusNode: ctrl.codeInputFocusNode,
          autoDisposeControllers: false,
          mainAxisAlignment: MainAxisAlignment.center,
          showCursor: false,
          cursorColor: UiColors.black,
          cursorHeight: 22,
          autoFocus: true,
          appContext: context,
          separatorBuilder: (context, index) => SizedBox(width: SC.s8),
          length: 4,
          onChanged: (_) {},
          keyboardType: TextInputType.number,
          textStyle: TextStyles.authTextField,
          pinTheme: PinTheme(
              fieldOuterPadding: EdgeInsets.zero,
              borderRadius: BorderRadius.all(Radius.circular(SC.s4)),
              fieldHeight: SC.s(46),
              fieldWidth: SC.s(35),
              activeFillColor: UiColors.white, // уже введено
              selectedFillColor: UiColors.white, // в фокусе
              inactiveFillColor: UiColors.white, // еще не введено
              activeBorderWidth: ctrl.isCodeValid ? SC.s1 : SC.s2,
              selectedBorderWidth: SC.s2,
              inactiveBorderWidth: ctrl.isCodeValid ? SC.s1 : SC.s2,
              activeColor: ctrl.isCodeValid
                  ? UiColors.greyBlue
                  : UiColors.redError, // уже введено
              selectedColor: ctrl.isCodeValid
                  ? UiColors.blueStroke
                  : UiColors.redError, // в фокусе
              inactiveColor: ctrl.isCodeValid
                  ? UiColors.greyBlue
                  : UiColors.redError, // еще не введено
              shape: PinCodeFieldShape.box),
        ),

        // Error
        ctrl.isCodeValid
            ? SizedBox(height: SC.s28)
            : Align(
                alignment: Alignment.center,
                child: Padding(
                  padding: EdgeInsets.only(top: SC.s(4.5), bottom: SC.s(8.5)),
                  child: Text(
                    'Неверный код',
                    style: TextStyles.authInputError,
                  ),
                ),
              ),
      ],
    );
  }
}
