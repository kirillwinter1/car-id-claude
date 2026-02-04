import 'package:app/screens/feedback_screen.dart/feedback_screen.dart';
import 'package:app/utils/const.dart';
import 'package:app/utils/res/svg_assets.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/utils/utils.dart';
import 'package:app/widgets/app_bars/title_app_bar.dart';
import 'package:app/widgets/buttons/solid_button.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:get/get.dart';

class SupportScreen extends StatelessWidget {
  const SupportScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: const TitleAppBar(title: 'Связаться с нами'),
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ///
            /// Связаться с поддержкой
            ///
            _supportButton(
                title: 'В Telegram',
                onTap: () {
                  Utils.openUrl(TELEGRAM_SUPPORT_URL);
                },
                icon: SvgAssets.telegram,
                padding: EdgeInsets.fromLTRB(SC.s16, SC.s12, SC.s16, SC.s16),
                iconInnerPadding:
                    EdgeInsets.fromLTRB(SC.s6, SC.s10, SC.s10, SC.s10)),

            ///
            /// Оставить отзыв
            ///
            _supportButton(
                title: 'По почте',
                onTap: () {
                  Get.to(() => const FeedbackScreen());
                },
                icon: SvgAssets.mail),
          ],
        ),
      ),
    );
  }

  Widget _supportButton({
    required String title,
    required VoidCallback onTap,
    required String icon,
    EdgeInsetsGeometry? iconInnerPadding,
    EdgeInsetsGeometry? padding,
  }) {
    return SolidButton(
      title: '',
      onPressed: onTap,
      height: SC.s(80),
      padding: padding,
      innerPadding: EdgeInsets.symmetric(horizontal: SC.s24),
      backgroundColor: UiColors.blueF,
      widget: Row(
        children: [
          Container(
            width: SC.s(40),
            height: SC.s(40),
            decoration: BoxDecoration(
              color: UiColors.blueD,
              borderRadius: BorderRadius.circular(SC.s(40)),
            ),
            padding: iconInnerPadding ?? EdgeInsets.all(SC.s8),
            margin: EdgeInsets.only(right: SC.s16),
            child: SvgPicture.asset(icon),
          ),
          Text(
            title,
            style: TextStyles.regular17,
          ),
        ],
      ),
    );
  }
}
