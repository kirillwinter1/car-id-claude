import 'package:app/controllers/notifications_controller.dart';
import 'package:app/utils/res/svg_assets.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/widgets/app_bar_alert_widget.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';
import 'package:get/get.dart';

class MainAppBar extends StatelessWidget implements PreferredSizeWidget {
  final VoidCallback onMenuTap;
  final VoidCallback onBellTap;
  final VoidCallback onShareTap;
  final bool showAppBarAlert;
  const MainAppBar({
    required this.onMenuTap,
    required this.onBellTap,
    required this.onShareTap,
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
            backgroundColor: UiColors.white,
            automaticallyImplyLeading: false,
            toolbarHeight:
                preferredSize.height - SC.s(showAppBarAlert ? 50 : 0),
            titleSpacing: 0,
            centerTitle: false,
            leadingWidth: SC.s(64),

            ///
            /// Кнопка меню
            ///
            leading: Padding(
              padding: EdgeInsets.fromLTRB(SC.s16, SC.s4, 0, SC.s4),
              child: GestureDetector(
                onTap: onMenuTap,
                child: Container(
                  padding: EdgeInsets.all(SC.s12),
                  color: Colors.transparent,
                  child: SvgPicture.asset(
                    SvgAssets.menu,
                    color: UiColors.blackA_94,
                  ),
                ),
              ),
            ),
            actions: [
              ///
              /// Колокольчик
              ///
              GetBuilder<NotificationsController>(builder: (ctrl) {
                return Padding(
                  padding: EdgeInsets.fromLTRB(0, SC.s4, 0, SC.s4),
                  child: GestureDetector(
                    onTap: onBellTap,
                    child: Stack(
                      alignment: Alignment.center,
                      children: [
                        Container(
                          padding: EdgeInsets.all(SC.s12),
                          color: Colors.transparent,
                          child: SvgPicture.asset(
                            SvgAssets.bell,
                            color: UiColors.blackA_94,
                          ),
                        ),

                        /// Счетчик непрочитанных уведомлений
                        if (ctrl.unreadCount > 0)
                          Positioned(
                            right: SC.s4,
                            top: SC.s4,
                            child: Container(
                              height: SC.s16,
                              alignment: Alignment.center,
                              padding:
                                  EdgeInsets.symmetric(horizontal: SC.s(4.5)),
                              decoration: BoxDecoration(
                                  color: UiColors.redError,
                                  borderRadius: BorderRadius.circular(50)),
                              child: Text(
                                '${ctrl.unreadCount}',
                                style: TextStyles.medium11.copyWith(
                                  color: UiColors.white,
                                  letterSpacing: 0.5,
                                ),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                );
              }),

              ///
              /// Кнопка "Поделиться"
              ///
              Padding(
                padding: EdgeInsets.fromLTRB(0, SC.s4, SC.s16, SC.s4),
                child: GestureDetector(
                  onTap: onShareTap,
                  child: Container(
                    padding: EdgeInsets.all(SC.s12),
                    color: Colors.transparent,
                    child: SvgPicture.asset(
                      SvgAssets.share,
                      color: UiColors.blackA_94,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
