import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';

class LightButton extends StatelessWidget {
  final String title;
  final TextStyle? titleStyle;
  final VoidCallback? onPressed;
  final Color mainColor;
  final Color disabledColor;
  final double? height;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? contentPadding;
  final double? minWidth;
  final double? borderWidth;
  final double? borderRadius;

  const LightButton({
    required this.title,
    this.titleStyle,
    this.onPressed,
    this.height,
    this.mainColor = UiColors.orange,
    this.disabledColor = UiColors.greyLight,
    this.padding,
    this.contentPadding,
    this.minWidth = double.infinity,
    this.borderWidth,
    this.borderRadius,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    final bool isEnabled = onPressed != null;

    return Padding(
      padding: padding ?? EdgeInsets.symmetric(horizontal: SC.s16),
      child: MaterialButton(
        onPressed: onPressed,
        color: UiColors.white,
        disabledColor: UiColors.white,
        highlightColor: mainColor.withOpacity(0.7),
        splashColor: mainColor.withOpacity(0.7),
        elevation: 0,
        minWidth: minWidth,
        shape: RoundedRectangleBorder(
            side: BorderSide(
                color: isEnabled ? mainColor : disabledColor,
                width: borderWidth ?? SC.s2),
            borderRadius: BorderRadius.circular(borderRadius ?? SC.s18)),
        height: height ?? SC.s(64),
        padding: contentPadding,
        child: Text(
          title,
          style: titleStyle?.copyWith(
                  color: isEnabled ? mainColor : disabledColor) ??
              TextStyles.buttonTextStyle
                  .copyWith(color: isEnabled ? mainColor : disabledColor),
        ),
      ),
    );
  }
}
