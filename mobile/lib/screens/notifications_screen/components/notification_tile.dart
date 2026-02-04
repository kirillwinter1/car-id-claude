import 'package:app/controllers/notifications_controller.dart';
import 'package:app/models/car_notification.dart';
import 'package:app/models/report_event.dart';
import 'package:app/screens/status_screen/status_screen.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:intl/intl.dart';

class NotificationTile extends StatelessWidget {
  final CarNotification notification;
  final bool isOutgoing;
  NotificationTile(this.notification, {this.isOutgoing = false, super.key});

  final NotificationsController controller = Get.find();

  String _getReasonById(int id) {
    List<ReportEvent>? events = controller.events;

    ReportEvent? foundEvent =
        events?.firstWhereOrNull((element) => element.id == id);

    return foundEvent != null ? foundEvent.description : '';
  }

  @override
  Widget build(BuildContext context) {
    const Color unreadColor = UiColors.white;
    const Color readColor = Color(0xFFF5F5F5);
    final List<BoxShadow> unreadShadow = [
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
    ];
    final List<BoxShadow> readShadow = [];

    return Container(
      padding: EdgeInsets.all(SC.s20),
      decoration: BoxDecoration(
          color: isOutgoing
              ? unreadColor
              : notification.isRead
                  ? readColor
                  : unreadColor,
          borderRadius: BorderRadius.circular(SC.s20),
          boxShadow: isOutgoing
              ? unreadShadow
              : notification.isRead
                  ? readShadow
                  : unreadShadow),
      child: isOutgoing ? _outgoingContent() : _incomingContent(),
    );
  }

  Widget _incomingContent() {
    return Column(
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  ///
                  /// Заголовок
                  ///
                  Text(
                    notification.qrName,
                    style: TextStyles.medium14.copyWith(
                        color: UiColors.blackC_80,
                        height: SC.s18 / SC.s14,
                        letterSpacing: 0.25),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  SizedBox(height: SC.s4),

                  ///
                  /// Описание
                  ///
                  Text(
                    notification.text ?? _getReasonById(notification.reasonId),
                    style:
                        TextStyles.light16.copyWith(color: UiColors.blackA_94),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            SizedBox(width: SC.s4),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                ///
                /// Дата
                ///
                if (notification.time != null)
                  Text(
                    DateFormat('dd.MM.yyyy').format(notification.time!),
                    style:
                        TextStyles.light14.copyWith(color: UiColors.blackD_70),
                  ),

                ///
                /// Время
                ///
                if (notification.time != null)
                  Text(DateFormat('HH:mm').format(notification.time!),
                      style: TextStyles.light14
                          .copyWith(color: UiColors.blackD_70)),
              ],
            ),
          ],
        ),

        ///
        /// Отметить прочитанным
        ///
        if (notification.isRead == false)
          Padding(
            padding: EdgeInsets.only(top: SC.s20),
            child: Align(
              alignment: Alignment.centerRight,
              child: GestureDetector(
                onTap: () => controller.markAsRead(notification.id),
                child: Text(
                  'Отметить прочитанным',
                  style: TextStyles.medium14
                      .copyWith(color: UiColors.blackA_94, letterSpacing: 0.1),
                ),
              ),
            ),
          ),
      ],
    );
  }

  Widget _outgoingContent() {
    return Column(
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  ///
                  /// Описание
                  ///
                  Text(
                    notification.text ?? _getReasonById(notification.reasonId),
                    style: TextStyles.medium14.copyWith(
                        color: UiColors.blackC_80,
                        height: SC.s18 / SC.s14,
                        letterSpacing: 0.25),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
            SizedBox(width: SC.s4),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                ///
                /// Дата
                ///
                if (notification.time != null)
                  Text(
                    DateFormat('dd.MM.yyyy').format(notification.time!),
                    style:
                        TextStyles.light14.copyWith(color: UiColors.blackD_70),
                  ),

                ///
                /// Время
                ///
                if (notification.time != null)
                  Text(DateFormat('HH:mm').format(notification.time!),
                      style: TextStyles.light14
                          .copyWith(color: UiColors.blackD_70)),
              ],
            ),
          ],
        ),

        ///
        /// Посмотреть статус
        ///
        Padding(
          padding: EdgeInsets.only(top: SC.s10),
          child: Align(
            alignment: Alignment.centerRight,
            child: GestureDetector(
              onTap: () {
                Get.to(() => StatusScreen(notification));
              },
              child: Text(
                'Посмотреть статус',
                style: TextStyles.medium14
                    .copyWith(color: UiColors.blackA_94, letterSpacing: 0.1),
              ),
            ),
          ),
        ),
      ],
    );
  }
}
