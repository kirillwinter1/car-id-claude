import 'package:app/controllers/auth_controller.dart';
import 'package:app/screens/auth/components/code_verify_widget.dart';
import 'package:app/screens/auth/components/phone_login_widget.dart';
import 'package:app/screens/auth/components/rules_screen.dart';
import 'package:app/utils/const.dart';
import 'package:app/utils/res/svg_assets.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/utils/utils.dart';
import 'package:app/widgets/buttons/solid_button.dart';
import 'package:app/widgets/custom_checkbox.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';
import 'package:get/get.dart';

class AuthScreen extends StatelessWidget {
  const AuthScreen({super.key});

  @override
  Widget build(BuildContext context) {
    const Duration animationDuration = Duration(milliseconds: 500);

    return GetBuilder<AuthController>(builder: (ctrl) {
      return Scaffold(
        body: Column(
          children: [
            //
            // Синий контейнер
            //
            Container(
              clipBehavior: Clip.hardEdge,
              padding: EdgeInsets.symmetric(horizontal: SC.s24),
              margin: EdgeInsets.only(bottom: SC.s28),
              width: double.infinity,
              decoration: BoxDecoration(
                  color: UiColors.blueF,
                  borderRadius: BorderRadius.only(
                    bottomLeft: Radius.circular(SC.s(28)),
                    bottomRight: Radius.circular(SC.s(28)),
                  )),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Stack(
                    children: [
                      ///
                      /// Лого
                      ///
                      Align(
                        alignment: Alignment.center,
                        child: Padding(
                          padding: EdgeInsets.only(
                            top: Get.mediaQuery.viewPadding.top + SC.s(36),
                            bottom: SC.s(38),
                          ),
                          child: SvgPicture.asset(SvgAssets.logo),
                        ),
                      ),

                      ///
                      /// Кнопка Назад
                      ///
                      if (ctrl.currentPage == 1)
                        Align(
                          alignment: Alignment.centerLeft,
                          child: Padding(
                            padding: EdgeInsets.only(
                              top: Get.mediaQuery.viewPadding.top + SC.s(40),
                              bottom: SC.s(38),
                            ),
                            child: GestureDetector(
                              onTap: () => ctrl.toPreviousPage(),
                              child: Container(
                                padding: EdgeInsets.fromLTRB(
                                    SC.s10, SC.s10, SC.s10, SC.s9),
                                decoration: const BoxDecoration(
                                    color: UiColors.blueJ,
                                    borderRadius:
                                        BorderRadius.all(Radius.circular(100))),
                                child: SvgPicture.asset(
                                  SvgAssets.arrowLeft,
                                  color: UiColors.blackC_80,
                                ),
                              ),
                            ),
                          ),
                        ),
                    ],
                  ),

                  ///
                  /// Заголовки и поля ввода телефона/смс-кода
                  ///
                  AnimatedCrossFade(
                    firstChild: PhoneLoginWidget(ctrl),
                    secondChild: CodeVerifyWidget(ctrl),
                    crossFadeState: ctrl.currentPage == 0
                        ? CrossFadeState.showFirst
                        : CrossFadeState.showSecond,
                    duration: animationDuration,
                  ),
                ],
              ),
            ),

            ///
            /// Чекбокс с правилами сервиса
            ///
            AnimatedCrossFade(
              firstChild: Padding(
                padding: EdgeInsets.fromLTRB(SC.s16, 0, SC.s24, SC.s20),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    GestureDetector(
                      onTap: () => ctrl.toggleIsAcceptedRules(),
                      child: CustomCheckbox(
                        isSelected: ctrl.isAcceptedRules,
                        margin: EdgeInsets.all(SC.s9),
                      ),
                    ),
                    SizedBox(width: SC.s8),
                    Expanded(
                        child: Padding(
                      padding: EdgeInsets.only(top: SC.s6),
                      child: RichText(
                          text: TextSpan(
                        text: 'Соглашаюсь с ',
                        style: TextStyles.authCheckboxText,
                        children: [
                          TextSpan(
                              text: 'Публичной офертой',
                              style: TextStyles.authCheckboxText
                                  .copyWith(color: UiColors.orange),
                              recognizer: TapGestureRecognizer()
                                ..onTap = () {
                                  // Get.to(() => const RulesScreen());
                                  Utils.openUrl(PUBLIC_OFFER_URL);
                                }),
                          const TextSpan(text: ', '),
                          TextSpan(
                              text:
                                  'Политикой конфиденциальности и обработки персональных данных',
                              style: TextStyles.authCheckboxText
                                  .copyWith(color: UiColors.orange),
                              recognizer: TapGestureRecognizer()
                                ..onTap = () {
                                  // Get.to(() => const RulesScreen());
                                  Utils.openUrl(PRIVACY_POLICY_URL);
                                }),
                          const TextSpan(text: '.'),
                        ],
                      )),
                    ))
                  ],
                ),
              ),
              secondChild: const SizedBox(),
              crossFadeState: ctrl.currentPage == 0
                  ? CrossFadeState.showFirst
                  : CrossFadeState.showSecond,
              duration: animationDuration,
            ),

            ///
            /// Кнопки "Получить код" и "Получить код повторно"
            ///
            AnimatedCrossFade(
              firstChild: SolidButton(
                title: 'Получить код',
                onPressed: ctrl.isPhonePageCompleted == false ||
                        (ctrl.isTimerForCurrentPhone && ctrl.secoundsLeft > 0)
                    ? null
                    : () {
                        ctrl.registerPhone();
                      },
              ),
              secondChild: SolidButton(
                title: 'Получить код повторно',
                onPressed: ctrl.secoundsLeft < 1
                    ? () {
                        ctrl.registerPhone();
                      }
                    : null,
              ),
              crossFadeState: ctrl.currentPage == 0
                  ? CrossFadeState.showFirst
                  : CrossFadeState.showSecond,
              duration: animationDuration,
            ),

            ///
            /// Таймер повторной отправки
            ///
            if (ctrl.isTimerForCurrentPhone && ctrl.secoundsLeft > 0)
              Padding(
                padding: EdgeInsets.fromLTRB(SC.s16, SC.s12, SC.s16, 0),
                child: RichText(
                    textAlign: TextAlign.center,
                    text: TextSpan(
                        text: 'Получить код повторно можно через ',
                        style: TextStyles.authCodeTooltip,
                        children: [
                          TextSpan(
                              text: ctrl.getTime(),
                              style: TextStyles.authCodeTooltip
                                  .copyWith(fontWeight: FontWeight.w500)),
                        ])),
              ),
          ],
        ),
      );
    });
  }
}
