import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';

class MenuTextButton extends StatelessWidget {
  final String title;
  final VoidCallback? onClick;
  final bool enabled;
  const MenuTextButton({
    required this.title,
    this.onClick,
    this.enabled = true,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: enabled ? onClick : null,
      child: Container(
        width: double.infinity,
        color: Colors.transparent,
        margin: EdgeInsets.only(left: SC.s24, right: SC.s(40)),
        padding: EdgeInsets.symmetric(vertical: SC.s12),
        child: Text(
          title,
          style: TextStyles.regular14.copyWith(
              color: UiColors.blackC_80.withOpacity(enabled ? 1 : 0.2)),
        ),
      ),
    );
  }
}
