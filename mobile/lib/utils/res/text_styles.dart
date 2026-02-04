import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';

class TextStyles {
  static TextStyle defaultRegular = TextStyle(
    fontSize: SC.s14,
    color: UiColors.blackA_94,
    fontWeight: FontWeight.w400,
    fontFamily: 'Roboto',
  );

  static TextStyle authHeader = TextStyle(
    fontSize: SC.s19,
    color: UiColors.blackA_94,
    fontWeight: FontWeight.w800,
    letterSpacing: 0.15,
    fontFamily: 'Roboto',
  );

  static TextStyle authDescription = TextStyle(
    fontSize: SC.s14,
    color: UiColors.blackD_70,
    fontWeight: FontWeight.w400,
    fontFamily: 'Roboto',
  );

  static TextStyle authTextField = TextStyle(
    fontSize: SC.s19,
    color: UiColors.black,
    fontWeight: FontWeight.w500,
    height: SC.s22 / SC.s19,
    fontFamily: 'Roboto',
  );

  static TextStyle authInputError = TextStyle(
    fontSize: SC.s13,
    color: UiColors.redError,
    fontWeight: FontWeight.w500,
    height: SC.s15 / SC.s13,
    fontFamily: 'Roboto',
  );

  static TextStyle authCheckboxText = TextStyle(
    fontSize: SC.s12,
    color: const Color(0xFF262626),
    fontWeight: FontWeight.w500,
    height: SC.s16 / SC.s12,
    fontFamily: 'Roboto',
  );

  static TextStyle authCodeTooltip = TextStyle(
    fontSize: SC.s14,
    color: UiColors.blackJ_42,
    fontWeight: FontWeight.w400,
    fontFamily: 'Roboto',
  );

  static TextStyle rulesTooltip = authCodeTooltip;

  static TextStyle buttonTextStyle = TextStyle(
    fontSize: SC.s17,
    color: Colors.white,
    fontWeight: FontWeight.w500,
    fontFamily: 'Roboto',
  );

  static TextStyle appBarTitle = TextStyle(
    fontSize: SC.s19,
    color: UiColors.blackA_94,
    fontWeight: FontWeight.w500,
    height: SC.s22 / SC.s19,
    letterSpacing: 0.2,
    fontFamily: 'Roboto',
  );

  static TextStyle bold14 = TextStyle(
    fontSize: SC.s14,
    fontWeight: FontWeight.w700,
    height: SC.s26 / SC.s14,
    fontFamily: 'Roboto',
  );

  static TextStyle medium11 = TextStyle(
    fontSize: SC.s11,
    fontWeight: FontWeight.w500,
    fontFamily: 'Roboto',
  );

  static TextStyle medium13 = TextStyle(
    fontSize: SC.s13,
    fontWeight: FontWeight.w500,
    height: SC.s15 / SC.s13,
    fontFamily: 'Roboto',
  );

  static TextStyle medium14 = TextStyle(
    fontSize: SC.s14,
    fontWeight: FontWeight.w500,
    height: SC.s22 / SC.s14,
    fontFamily: 'Roboto',
  );

  static TextStyle medium16 = TextStyle(
    fontSize: SC.s16,
    fontWeight: FontWeight.w500,
    height: SC.s22 / SC.s16,
    fontFamily: 'Roboto',
  );

  static TextStyle medium19 = TextStyle(
    fontSize: SC.s19,
    fontWeight: FontWeight.w500,
    height: SC.s26 / SC.s19,
    fontFamily: 'Roboto',
  );

  static TextStyle regular12 = TextStyle(
    fontSize: SC.s12,
    fontWeight: FontWeight.w400,
    height: SC.s18 / SC.s12,
    fontFamily: 'Roboto',
  );

  static TextStyle regular14 = TextStyle(
    fontSize: SC.s14,
    fontWeight: FontWeight.w400,
    height: SC.s22 / SC.s14,
    fontFamily: 'Roboto',
  );

  static TextStyle regular16 = TextStyle(
    fontSize: SC.s16,
    fontWeight: FontWeight.w400,
    height: SC.s24 / SC.s16,
    fontFamily: 'Roboto',
  );

  static TextStyle regular17 = TextStyle(
    fontSize: SC.s17,
    fontWeight: FontWeight.w400,
    fontFamily: 'Roboto',
  );

  static TextStyle regular22 = TextStyle(
    fontSize: SC.s22,
    fontWeight: FontWeight.w400,
    fontFamily: 'Roboto',
  );

  static TextStyle light14 = TextStyle(
    fontSize: SC.s14,
    height: SC.s20 / SC.s14,
    fontWeight: FontWeight.w300,
    fontFamily: 'Roboto',
  );

  static TextStyle light16 = TextStyle(
    fontSize: SC.s16,
    height: SC.s22 / SC.s16,
    fontWeight: FontWeight.w300,
    fontFamily: 'Roboto',
  );
}
