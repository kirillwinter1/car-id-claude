import 'package:app/controllers/notifications_controller.dart';
import 'package:app/screens/notifications_screen/components/empty_tile.dart';
import 'package:app/screens/notifications_screen/components/notification_tile.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/widgets/page_view/dynamic_height_page_view.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class NotificationsMainWidget extends StatelessWidget {
  const NotificationsMainWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return GetBuilder<NotificationsController>(builder: (ctrl) {
      return Padding(
        padding: EdgeInsets.only(bottom: SC.s28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: EdgeInsets.fromLTRB(SC.s24, 0, SC.s16, SC.s12),
              child: Text(
                'Уведомления',
                style: TextStyles.regular22.copyWith(color: UiColors.blackA_94),
              ),
            ),

            ///
            /// Если нет непрочитанных уведомлений
            ///
            if (ctrl.unreadNotifications.isEmpty)
              Padding(
                padding: EdgeInsets.fromLTRB(SC.s16, 0, SC.s16, SC.s12),
                child: const EmptyTile(text: 'Нет непрочитанных уведомлений'),
              ),

            ///
            /// Slider
            ///
            if (ctrl.unreadNotifications.isNotEmpty)
              DynamicHeightPageView(List.generate(
                  ctrl.unreadNotifications.length,
                  (i) => NotificationTile(ctrl.unreadNotifications[i]))),
          ],
        ),
      );
    });
  }
}
