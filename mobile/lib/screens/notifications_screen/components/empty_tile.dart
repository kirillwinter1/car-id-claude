import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';

class EmptyTile extends StatelessWidget {
  final String text;
  const EmptyTile({this.text = '', super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
        width: double.infinity,
        alignment: Alignment.center,
        padding: EdgeInsets.symmetric(vertical: SC.s16),
        decoration: BoxDecoration(
            color: UiColors.white,
            borderRadius: BorderRadius.circular(SC.s20),
            boxShadow: [
              BoxShadow(
                offset: Offset(0, SC.s1),
                blurRadius: SC.s4,
                color: UiColors.black.withOpacity(0.16),
              ),
              BoxShadow(
                offset: Offset(0, SC.s1),
                blurRadius: SC.s6,
                color: UiColors.black.withOpacity(0.08),
              ),
            ]),
        child: Text(
          text,
          style: TextStyles.light14.copyWith(
              color: UiColors.blackF_60,
              height: SC.s22 / SC.s14,
              letterSpacing: 0.2),
        ));
  }
}
