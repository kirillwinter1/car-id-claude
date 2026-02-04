import 'package:app/controllers/notifications_controller.dart';
import 'package:app/screens/notifications_screen/components/empty_tile.dart';
import 'package:app/screens/notifications_screen/components/notification_tile.dart';
import 'package:app/screens/notifications_screen/components/tab_selector.dart';
import 'package:app/utils/res/svg_assets.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/routes.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/utils/utils.dart';
import 'package:app/widgets/app_bars/title_app_bar.dart';
import 'package:app/widgets/custom_progress_indicator.dart';
import 'package:app/widgets/stateful_wrapper.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';
import 'package:get/get.dart';

class NotificationsScreen extends StatelessWidget {
  const NotificationsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return StatefulWrapper(
      onInit: () {
        Get.find<NotificationsController>().loadAllNotifications();
        Get.find<NotificationsController>().loadAllOutgoingNotifications();
      },
      child: Scaffold(
          appBar: TitleAppBar(
            title: 'Уведомления',
            actionsItemPadding: EdgeInsets.fromLTRB(0, SC.s8, SC.s20, SC.s8),
            onBackTap: () {
              if (Navigator.of(context).canPop()) {
                Get.back();
              } else {
                Get.offAllNamed(CustomRouter.PAGENAME_HOME);
              }
            },
            actions: [
              GestureDetector(
                onTap: () {
                  Utils.showInfoDialog(
                    infoText:
                        'В исходящих вы увидите ваши обращения к другим пользователям, отправленные только через мобильное приложение',
                  );
                },
                child: Container(
                  width: SC.s(40),
                  height: SC.s(40),
                  decoration: BoxDecoration(
                    color: UiColors.orangeLightLight,
                    borderRadius: BorderRadius.circular(50),
                  ),
                  padding: EdgeInsets.all(SC.s10),
                  child: SvgPicture.asset(SvgAssets.info),
                ),
              )
            ],
          ),
          body: GetBuilder<NotificationsController>(builder: (ctrl) {
            return Column(
              children: [
                Padding(
                  padding: EdgeInsets.only(bottom: SC.s10),
                  child: TabSelector(
                    selectedIndex: ctrl.currentTab,
                    onFirstTap: () => ctrl.setCurrentTab(0),
                    onSecondTap: () => ctrl.setCurrentTab(1),
                  ),
                ),
                Expanded(
                  child:
                      ctrl.currentTab == 0 ? _incoming(ctrl) : _outgoing(ctrl),
                )
              ],
            );
          })),
    );
  }

  Widget _incoming(NotificationsController ctrl) {
    return ctrl.isLoading
        ? const CustomProgressIndicator()
        : RefreshIndicator(
            color: UiColors.orange,
            strokeWidth: 1.5,
            onRefresh: () async {
              ctrl.loadAllNotifications();
            },
            child: CupertinoScrollbar(
              child: CustomScrollView(
                physics: const BouncingScrollPhysics(
                    parent: AlwaysScrollableScrollPhysics()),
                slivers: [
                  ///
                  /// Нет уведомлений
                  ///
                  if (ctrl.allNotifications.isEmpty)
                    SliverToBoxAdapter(
                        child: Padding(
                      padding: EdgeInsets.symmetric(
                          horizontal: SC.s16, vertical: SC.s12),
                      child: const EmptyTile(text: 'У вас нет уведомлений'),
                    )),

                  ///
                  /// Уведомления
                  ///
                  if (ctrl.allNotifications.isNotEmpty)
                    SliverList(
                        delegate: SliverChildBuilderDelegate(
                      (_, i) {
                        final bool isFirst = i == 0;
                        final bool isLast =
                            i == ctrl.allNotifications.length - 1;

                        return Padding(
                          padding: EdgeInsets.fromLTRB(
                              SC.s16,
                              isFirst ? SC.s12 : 0,
                              SC.s16,
                              isLast ? SC.s26 : SC.s16),
                          child: NotificationTile(ctrl.allNotifications[i]),
                        );
                      },
                      childCount: ctrl.allNotifications.length,
                    )),
                ],
              ),
            ),
          );
  }

  Widget _outgoing(NotificationsController ctrl) {
    return ctrl.isLoadingOutgoing
        ? const CustomProgressIndicator()
        : RefreshIndicator(
            color: UiColors.orange,
            strokeWidth: 1.5,
            onRefresh: () async {
              ctrl.loadAllOutgoingNotifications();
            },
            child: CupertinoScrollbar(
              child: CustomScrollView(
                physics: const BouncingScrollPhysics(
                    parent: AlwaysScrollableScrollPhysics()),
                slivers: [
                  ///
                  /// Нет уведомлений
                  ///
                  if (ctrl.allOutgoingNotifications.isEmpty)
                    SliverToBoxAdapter(
                        child: Padding(
                      padding: EdgeInsets.symmetric(
                          horizontal: SC.s16, vertical: SC.s12),
                      child: const EmptyTile(
                          text: 'У вас нет исходящих уведомлений'),
                    )),

                  ///
                  /// Уведомления
                  ///
                  if (ctrl.allOutgoingNotifications.isNotEmpty)
                    SliverList(
                        delegate: SliverChildBuilderDelegate(
                      (_, i) {
                        final bool isFirst = i == 0;
                        final bool isLast =
                            i == ctrl.allOutgoingNotifications.length - 1;

                        return Padding(
                          padding: EdgeInsets.fromLTRB(
                              SC.s16,
                              isFirst ? SC.s12 : 0,
                              SC.s16,
                              isLast ? SC.s26 : SC.s16),
                          child: NotificationTile(
                            ctrl.allOutgoingNotifications[i],
                            isOutgoing: true,
                          ),
                        );
                      },
                      childCount: ctrl.allOutgoingNotifications.length,
                    )),
                ],
              ),
            ),
          );
  }
}
