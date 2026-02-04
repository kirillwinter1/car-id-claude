import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';

class CustomSwitch extends StatelessWidget {
  final bool value;
  final ValueChanged<bool> onChanged;
  final Color? activeColor;
  final Color? inactiveColor;
  final double? width;
  final double? height;

  const CustomSwitch({
    required this.value,
    required this.onChanged,
    this.activeColor = UiColors.orange,
    this.inactiveColor = UiColors.greyNormalStroke,
    this.width,
    this.height,
    super.key,
  });

  static const Duration duration = Duration(milliseconds: 200);

  @override
  Widget build(BuildContext context) {
    final double finalWidth = width ?? SC.s(52);
    final double finalHeight = height ?? SC.s(32);
    final double knobSize = finalHeight - SC.s8;

    return GestureDetector(
      onTap: () => onChanged(value),
      child: AnimatedContainer(
        duration: duration,
        curve: Curves.decelerate,
        height: finalHeight,
        width: finalWidth,
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(100),
          color: value ? activeColor : inactiveColor,
        ),
        child: AnimatedAlign(
          alignment: value ? Alignment.centerRight : Alignment.centerLeft,
          duration: duration,
          curve: Curves.decelerate,
          child: Padding(
            padding: EdgeInsets.symmetric(horizontal: SC.s4),
            child: Container(
              height: knobSize,
              width: knobSize,
              decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(100)),
            ),
          ),
        ),
      ),
    );
  }
}
