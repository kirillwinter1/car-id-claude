import 'package:app/controllers/auth_controller.dart';
import 'package:app/controllers/public_offer_controller.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/widgets/app_bars/title_app_bar.dart';
import 'package:app/widgets/buttons/solid_button.dart';
import 'package:app/widgets/custom_loading_indicator.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class RulesScreen extends StatelessWidget {
  final bool withAcceptButton;
  const RulesScreen({this.withAcceptButton = true, super.key});

  // Контент белого контейнера
  Widget _whiteBoxContent(PublicOfferController controller) {
    // Пока идет загрузка
    if (controller.isLoaded == false) {
      return const Center(child: CustomLoadingIndicator());
    }

    // При таймауте загрузки
    if (controller.isError) {
      return Center(
        child: Padding(
          padding: EdgeInsets.all(SC.s16),
          child: Text(controller.offerText!, textAlign: TextAlign.center),
        ),
      );
    }

    // При успешной загрузке
    return CustomScrollView(
      physics: const BouncingScrollPhysics(),
      slivers: [
        SliverToBoxAdapter(
            child: Padding(
          padding: EdgeInsets.symmetric(vertical: SC.s16, horizontal: SC.s16),
          child: Text(controller.offerText!, style: TextStyles.defaultRegular),
        )),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return GetBuilder<PublicOfferController>(
      init: PublicOfferController(),
      builder: (controller) {
        return Scaffold(
          backgroundColor: UiColors.blueF,
          appBar: const TitleAppBar(
            backButtonColorOn: true,
            title: 'Соглашение',
            backgroundColor: UiColors.blueF,
          ),
          body: SafeArea(
            child: Column(
              children: [
                ///
                /// Белый контейнер с текстом
                ///
                Expanded(
                    child: Container(
                  margin: EdgeInsets.fromLTRB(SC.s16, SC.s8, SC.s16, SC.s8),
                  width: double.infinity,
                  clipBehavior: Clip.hardEdge,
                  decoration: BoxDecoration(
                      color: UiColors.white,
                      borderRadius: BorderRadius.circular(SC.s24)),
                  child: _whiteBoxContent(controller),
                )),

                ///
                /// Текст над кнопкой "Принять"
                ///
                if (withAcceptButton)
                  Padding(
                    padding:
                        EdgeInsets.fromLTRB(SC.s16, SC.s12, SC.s16, SC.s16),
                    child: RichText(
                        textAlign: TextAlign.center,
                        text: TextSpan(
                            text: 'Нажимая кнопку ',
                            style: TextStyles.rulesTooltip,
                            children: [
                              TextSpan(
                                  text: '«Принять»',
                                  style: TextStyles.rulesTooltip
                                      .copyWith(fontWeight: FontWeight.w500)),
                              const TextSpan(
                                text:
                                    ' Вы соглашаетесь с правилами работы сервиса',
                              ),
                            ])),
                  ),

                ///
                /// Кнопка "Принять"
                ///
                if (withAcceptButton)
                  SolidButton(
                    title: 'Принять',
                    onPressed: controller.isLoaded &&
                            controller.isError == false
                        ? () {
                            if (Get.isRegistered<AuthController>()) {
                              Get.find<AuthController>().isAcceptedRules = true;
                            }
                            Get.back();
                          }
                        : null,
                  ),

                //
                SizedBox(height: SC.s16)
              ],
            ),
          ),
        );
      },
    );
  }
}
