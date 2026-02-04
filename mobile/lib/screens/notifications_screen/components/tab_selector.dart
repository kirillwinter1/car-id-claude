import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class TabSelector extends StatelessWidget {
  final double? height;
  final int selectedIndex;
  final VoidCallback? onFirstTap;
  final VoidCallback? onSecondTap;

  const TabSelector({
    this.selectedIndex = 0,
    this.height,
    this.onFirstTap,
    this.onSecondTap,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    final double finalHeight = height ?? SC.s(40);
    final double borderWidth = SC.s(0.5);
    final double horizontalMargin = SC.s16;
    final double innerPadding = SC.s4;

    final double innerContainerHeight =
        finalHeight - borderWidth * 2 - innerPadding * 2;
    final double innerContainerWidth = (Get.width -
            horizontalMargin * 2 -
            borderWidth * 2 -
            innerPadding * 2) /
        2;

    final TextStyle defaultStyle = TextStyles.regular16.copyWith(height: 1.25);

    const Duration duration = Duration(milliseconds: 200);

    return Container(
      height: finalHeight,
      margin:
          EdgeInsets.fromLTRB(horizontalMargin, SC.s10, horizontalMargin, 0),
      padding: EdgeInsets.all(innerPadding),
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border.all(color: UiColors.orange, width: borderWidth),
        borderRadius: BorderRadius.circular(SC.s8),
      ),
      child: Stack(children: [
        AnimatedPositioned(
          left: selectedIndex == 0 ? 0 : (Get.width - SC.s(42)) / 2,
          duration: duration,
          child: Container(
            height: innerContainerHeight,
            width: innerContainerWidth,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(SC.s4),
              color: UiColors.orange,
            ),
          ),
        ),
        Row(
          children: [
            GestureDetector(
              onTap: onFirstTap,
              child: Container(
                height: innerContainerHeight,
                width: innerContainerWidth,
                color: Colors.transparent,
                child: Center(
                    child: AnimatedDefaultTextStyle(
                  duration: duration,
                  style: selectedIndex == 0
                      ? defaultStyle.copyWith(color: UiColors.white)
                      : defaultStyle.copyWith(color: UiColors.blackF_60),
                  child: const Text('Входящие', textScaleFactor: 1),
                )),
              ),
            ),
            GestureDetector(
              onTap: onSecondTap,
              child: Container(
                height: innerContainerHeight,
                width: innerContainerWidth,
                color: Colors.transparent,
                child: Center(
                    child: AnimatedDefaultTextStyle(
                  duration: duration,
                  style: selectedIndex == 0
                      ? defaultStyle.copyWith(color: UiColors.blackF_60)
                      : defaultStyle.copyWith(color: UiColors.white),
                  child: const Text('Исходящие', textScaleFactor: 1),
                )),
              ),
            ),
          ],
        ),
      ]),
    );
  }
}
