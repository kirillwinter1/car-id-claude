import 'package:app/utils/res/svg_assets.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/widgets/app_bar_alert_widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';
import 'package:get/get.dart';

class TitleAppBar extends StatelessWidget implements PreferredSizeWidget {
  final bool automaticallyImplyLeading;
  final List<Widget>? actions;
  final EdgeInsetsGeometry? actionsItemPadding;
  final String? title;
  final Widget? leading;
  final double? preferredHeight;
  final VoidCallback? onBackTap;
  final Color? backgroundColor;
  final bool backButtonColorOn;
  final bool showAppBarAlert;
  const TitleAppBar({
    this.automaticallyImplyLeading = true,
    this.title,
    this.leading,
    this.preferredHeight,
    this.onBackTap,
    this.actions,
    this.actionsItemPadding,
    this.backgroundColor,
    this.backButtonColorOn = false,
    this.showAppBarAlert = false,
    super.key,
  });

  @override
  Size get preferredSize =>
      Size.fromHeight(SC.s(56 + (showAppBarAlert ? 50 : 0)));

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Column(
        children: [
          //
          if (showAppBarAlert) const AppBarAlertWidget(),

          AppBar(
            title: Text(
              title ?? '',
              style: TextStyles.appBarTitle,
            ),
            backgroundColor: backgroundColor,
            automaticallyImplyLeading: false,
            toolbarHeight:
                preferredSize.height - SC.s(showAppBarAlert ? 50 : 0),
            titleSpacing: 0,
            centerTitle: false,
            leadingWidth: SC.s(78),
            leading: automaticallyImplyLeading
                ? Padding(
                    padding: EdgeInsets.fromLTRB(SC.s20, SC.s8, SC.s18, SC.s8),
                    child: GestureDetector(
                      onTap: onBackTap ?? () => Get.back(),
                      child: Container(
                        padding:
                            EdgeInsets.fromLTRB(SC.s10, SC.s10, SC.s10, SC.s9),
                        decoration: BoxDecoration(
                            color: backButtonColorOn
                                ? UiColors.blueJ
                                : Colors.transparent,
                            borderRadius:
                                const BorderRadius.all(Radius.circular(100))),
                        child: SvgPicture.asset(
                          SvgAssets.arrowLeft,
                          color: UiColors.blackC_80,
                        ),
                      ),
                    ),
                  )
                : leading,
            actions: actions != null && actions!.isNotEmpty
                ? actions!
                    .map((e) => Padding(
                          padding: actionsItemPadding ??
                              EdgeInsets.fromLTRB(0, SC.s16, SC.s16, SC.s16),
                          child: e,
                        ))
                    .toList()
                : null,
          ),
        ],
      ),
    );
  }
}
