import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';

class SolidButton extends StatelessWidget {
  final String title;
  final TextStyle? titleStyle;
  final VoidCallback? onPressed;
  final Color backgroundColor;
  final Color disabledColor;
  final double? height;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? innerPadding;
  final Widget? widget;

  const SolidButton({
    required this.title,
    this.titleStyle,
    this.onPressed,
    this.height,
    this.backgroundColor = UiColors.orange,
    this.disabledColor = UiColors.greyLight,
    this.padding,
    this.innerPadding,
    this.widget,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: padding ?? EdgeInsets.symmetric(horizontal: SC.s16),
      child: MaterialButton(
        onPressed: onPressed,
        color: backgroundColor,
        disabledColor: disabledColor,
        highlightColor: backgroundColor.withOpacity(0.5),
        elevation: 0,
        minWidth: double.infinity,
        shape:
            RoundedRectangleBorder(borderRadius: BorderRadius.circular(SC.s18)),
        height: height ?? SC.s(64),
        padding: innerPadding,
        child: widget ??
            Text(
              title,
              style: titleStyle ?? TextStyles.buttonTextStyle,
            ),
      ),
    );
  }
}
