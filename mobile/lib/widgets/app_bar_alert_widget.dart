import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';

class AppBarAlertWidget extends StatelessWidget {
  final String alertText;
  final Color textColor;
  final Color backgroundColor;

  const AppBarAlertWidget({
    this.alertText = 'Временные неполадки на сервере',
    this.textColor = UiColors.white,
    this.backgroundColor = UiColors.redError,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return Hero(
      tag: 'alert',
      child: Material(
        color: Colors.transparent,
        child: Container(
          height: SC.s(50),
          width: double.infinity,
          color: backgroundColor,
          alignment: Alignment.center,
          child: Text(
            alertText,
            style: TextStyles.regular14.copyWith(color: textColor),
          ),
        ),
      ),
    );
  }
}
