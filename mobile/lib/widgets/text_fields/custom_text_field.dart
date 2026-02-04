import 'package:app/utils/res/svg_assets.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_svg/svg.dart';

class CustomTextField extends StatelessWidget {
  final TextEditingController controller;
  final FocusNode focusNode;
  final int? maxLength;
  final double? height;
  final List<TextInputFormatter>? inputFormatters;
  final TextInputType? keyboardType;
  final TextStyle? textStyle;
  final TextCapitalization textCapitalization;
  final bool enabled;
  final bool autofocus;
  final bool isValid;
  final Color? enabledBorderColor;
  final Color? disabledBorderColor;
  final Color? errorColor;
  final EdgeInsetsGeometry? innerPadding;
  final EdgeInsetsGeometry? contentPadding;
  final bool enableClearButton;
  final bool showClearButtonOnlyWhenFocused;
  final bool customBorder;
  final Widget? prefix;
  final int maxLines;
  final String? hintText;
  final TextStyle? hintStyle;

  const CustomTextField({
    required this.controller,
    required this.focusNode,
    this.maxLength,
    this.height,
    this.inputFormatters,
    this.keyboardType,
    this.textStyle,
    this.textCapitalization = TextCapitalization.none,
    this.enabled = true,
    this.autofocus = false,
    this.isValid = true,
    this.enabledBorderColor,
    this.disabledBorderColor,
    this.errorColor,
    this.innerPadding,
    this.contentPadding,
    this.enableClearButton = false,
    this.showClearButtonOnlyWhenFocused = true,
    this.customBorder = false,
    this.prefix,
    this.maxLines = 1,
    this.hintText,
    this.hintStyle,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    List<TextInputFormatter> formatters = inputFormatters ?? [];
    if (maxLength != null) {
      formatters.add(LengthLimitingTextInputFormatter(maxLength));
    }

    final bool showClearButton = enableClearButton &&
        (showClearButtonOnlyWhenFocused ? focusNode.hasFocus : true);

    return Container(
      padding: innerPadding ?? EdgeInsets.fromLTRB(SC.s12, 0, 0, 0),
      height: height ?? SC.s(46),
      alignment: Alignment.centerLeft,
      decoration: BoxDecoration(
        color: enabled
            ? Colors.white
            : disabledBorderColor ?? const Color(0xFF100A1C).withOpacity(0.05),
        borderRadius: BorderRadius.circular(SC.s4),
        border: focusNode.hasFocus
            ? Border.all(
                color: enabledBorderColor ?? UiColors.blueStroke, width: 2)
            : isValid
                ? Border.all(color: UiColors.greyBlue)
                : Border.all(color: errorColor ?? UiColors.redError, width: 2),
      ),
      child: TextFormField(
        controller: controller,
        focusNode: focusNode,
        onTap: () {},
        onChanged: (value) {},
        onFieldSubmitted: (value) {},
        inputFormatters: formatters.isNotEmpty ? formatters : null,
        style: textStyle ?? TextStyles.authTextField,
        autofocus: autofocus,
        keyboardType: keyboardType,
        showCursor: true,
        cursorColor: Colors.black,
        cursorHeight: SC.s19,
        cursorRadius: const Radius.circular(0),
        scrollPadding: EdgeInsets.zero,
        textCapitalization: textCapitalization,
        enabled: enabled,
        autocorrect: false,
        enableSuggestions: false,
        expands: false,
        maxLines: maxLines,
        scrollPhysics: const BouncingScrollPhysics(),
        decoration: InputDecoration(
          contentPadding: contentPadding ?? EdgeInsets.zero,
          enabledBorder: customBorder == false
              ? InputBorder.none
              : const OutlineInputBorder(
                  borderSide: BorderSide(color: Colors.transparent)),
          focusedBorder: customBorder == false
              ? InputBorder.none
              : const OutlineInputBorder(
                  borderSide: BorderSide(color: Colors.transparent)),
          disabledBorder: customBorder == false
              ? InputBorder.none
              : const OutlineInputBorder(
                  borderSide: BorderSide(color: Colors.transparent)),
          border: customBorder == false
              ? InputBorder.none
              : const OutlineInputBorder(
                  borderSide: BorderSide(color: Colors.transparent)),
          hintText: hintText,
          hintStyle: hintStyle ??
              TextStyles.regular16
                  .copyWith(color: UiColors.greyBlueA, height: SC.s22 / SC.s16),
          suffixIconConstraints: enableClearButton
              ? BoxConstraints(minHeight: 0, minWidth: 0, maxHeight: SC.s16)
              : null,
          suffixIcon: showClearButton
              ? (controller.text.isNotEmpty
                  ? IconButton(
                      padding: EdgeInsets.zero,
                      highlightColor: Colors.transparent,
                      splashColor: Colors.transparent,
                      onPressed: controller.clear,
                      icon: SvgPicture.asset(SvgAssets.check,
                          height: SC.s16,
                          width: SC.s16,
                          color: const Color(0xFF100A1C)),
                    )
                  : null)
              : null,
          prefixIcon: prefix,
          prefixIconConstraints: const BoxConstraints(),
          isDense: true,
        ),
      ),
    );
  }
}
