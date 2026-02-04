import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';

class MainActionTile extends StatelessWidget {
  final String title;
  final String description;
  final String iconPath;
  final VoidCallback? onClick;
  final EdgeInsetsGeometry? padding;
  final EdgeInsetsGeometry? innerPadding;
  final TextStyle? titleStyle;
  final TextStyle? descriptionStyle;
  final Color? backgroundColor;
  final Color? iconColor;
  final Color? iconBackgroundColor;
  final BoxBorder? border;

  const MainActionTile(
      {required this.title,
      required this.description,
      required this.iconPath,
      this.onClick,
      this.padding,
      this.innerPadding,
      this.titleStyle,
      this.descriptionStyle,
      this.backgroundColor,
      this.iconColor,
      this.iconBackgroundColor,
      this.border,
      super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: padding ?? EdgeInsets.symmetric(horizontal: SC.s16),
      child: GestureDetector(
        onTap: onClick,
        child: Container(
          decoration: BoxDecoration(
            color: backgroundColor ?? UiColors.blueF,
            borderRadius: BorderRadius.circular(SC.s24),
            border: border,
          ),
          padding: innerPadding ??
              EdgeInsets.fromLTRB(SC.s20, SC.s17, SC.s20, SC.s20),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                height: SC.s(60),
                width: SC.s(60),
                padding: EdgeInsets.all(SC.s14),
                margin: EdgeInsets.only(top: SC.s3),
                decoration: BoxDecoration(
                    color: iconBackgroundColor ?? UiColors.greyBlue,
                    borderRadius: BorderRadius.circular(SC.s12)),
                child: SvgPicture.asset(iconPath, color: iconColor),
              ),
              SizedBox(width: SC.s16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: titleStyle ??
                          TextStyles.bold14.copyWith(color: UiColors.blackC_80),
                    ),
                    SizedBox(height: SC.s3),
                    Text(
                      description,
                      style: descriptionStyle ??
                          TextStyles.regular12
                              .copyWith(color: UiColors.blackF_60),
                    ),
                  ],
                ),
              )
            ],
          ),
        ),
      ),
    );
  }
}
