import 'package:app/controllers/feedback_controller.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/widgets/app_bars/title_app_bar.dart';
import 'package:app/widgets/buttons/solid_button.dart';
import 'package:app/widgets/text_fields/custom_text_field.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class FeedbackScreen extends StatelessWidget {
  const FeedbackScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return GetBuilder<FeedbackController>(
        init: FeedbackController(),
        builder: (ctrl) {
          return Scaffold(
            appBar: const TitleAppBar(title: 'Оставить отзыв'),
            body: SafeArea(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  ///
                  /// Email
                  ///
                  Padding(
                    padding: EdgeInsets.fromLTRB(SC.s16, SC.s8, SC.s16, SC.s5),
                    child: CustomTextField(
                      controller: ctrl.emailController,
                      focusNode: ctrl.emailFocus,
                      autofocus: true,
                      keyboardType: TextInputType.emailAddress,
                      hintText: 'Электронная почта',
                      textStyle:
                          TextStyles.regular16.copyWith(color: UiColors.black),
                      isValid: ctrl.showEmailError == false,
                    ),
                  ),

                  /// Error
                  AnimatedCrossFade(
                    firstChild: Padding(
                      padding: EdgeInsets.only(left: SC.s20, bottom: SC.s5),
                      child: ctrl.showEmailError
                          ? Text(
                              'Ошибка при вводе',
                              style: TextStyles.medium13
                                  .copyWith(color: UiColors.redError),
                            )
                          : const SizedBox(),
                    ),
                    secondChild: SizedBox(height: SC.s20),
                    crossFadeState: ctrl.showEmailError
                        ? CrossFadeState.showFirst
                        : CrossFadeState.showSecond,
                    duration: const Duration(milliseconds: 200),
                  ),

                  ///
                  /// Feedback
                  ///
                  Padding(
                    padding: EdgeInsets.fromLTRB(SC.s16, 0, SC.s16, SC.s24),
                    child: CustomTextField(
                      controller: ctrl.feedbackTextController,
                      focusNode: ctrl.feedbackTextFocus,
                      height: SC.s(140),
                      maxLines: 5,
                      keyboardType: TextInputType.multiline,
                      textCapitalization: TextCapitalization.sentences,
                      hintText: 'Текст сообщения',
                      textStyle:
                          TextStyles.regular16.copyWith(color: UiColors.black),
                    ),
                  ),
                  const Expanded(child: SizedBox()),

                  ///
                  /// Button
                  ///
                  SolidButton(
                    title: 'Отправить',
                    onPressed:
                        ctrl.isFormReady ? () => ctrl.sendFeedback() : null,
                  ),
                  SizedBox(height: SC.s20),
                ],
              ),
            ),
          );
        });
  }
}
