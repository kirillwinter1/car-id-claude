import 'package:app/utils/res/svg_assets.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';

class CustomCheckbox extends StatelessWidget {
  final bool isSelected;
  final EdgeInsetsGeometry? margin;
  final double? size;

  const CustomCheckbox(
      {this.isSelected = false, this.margin, this.size, super.key});

  @override
  Widget build(BuildContext context) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 150),
      width: size ?? SC.s22,
      height: size ?? SC.s22,
      margin: margin ?? EdgeInsets.zero,
      decoration: isSelected
          ? BoxDecoration(
              borderRadius: BorderRadius.all(Radius.circular(SC.s2)),
              color: UiColors.orange,
              border: Border.all(color: UiColors.orange, width: SC.s2),
            )
          : BoxDecoration(
              borderRadius: BorderRadius.all(Radius.circular(SC.s2)),
              color: Colors.white,
              border:
                  Border.all(color: UiColors.greyNormalStroke, width: SC.s2),
            ),
      child: Center(
        child:
            isSelected ? SvgPicture.asset(SvgAssets.check) : const SizedBox(),
      ),
    );
  }
}
