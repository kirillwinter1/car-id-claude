import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';

class MenuButton extends StatelessWidget {
  final String title;
  final String iconPath;
  final VoidCallback? onClick;
  final int counterValue;
  const MenuButton({
    required this.title,
    required this.iconPath,
    this.onClick,
    this.counterValue = 0,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onClick,
      child: Container(
        color: Colors.transparent,
        padding: EdgeInsets.symmetric(vertical: SC.s12),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              children: [
                SvgPicture.asset(
                  iconPath,
                  color: UiColors.blackBOnBlue,
                  height: SC.s24,
                  width: SC.s24,
                ),
                Padding(
                  padding: EdgeInsets.only(left: SC.s16, top: SC.s1),
                  child: Text(
                    title,
                    style: TextStyles.medium14
                        .copyWith(color: UiColors.blackBOnBlue),
                  ),
                ),
              ],
            ),
            if (counterValue > 0)
              Container(
                height: SC.s20,
                alignment: Alignment.center,
                padding: EdgeInsets.symmetric(horizontal: SC.s6),
                decoration: BoxDecoration(
                    color: UiColors.redError,
                    borderRadius: BorderRadius.circular(50)),
                child: Text(
                  counterValue.toString(),
                  style: TextStyles.medium13.copyWith(
                    color: UiColors.white,
                    letterSpacing: 0.5,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
