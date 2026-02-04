class CarNotificationResponse {
  final int page;
  final int size;
  final int count;
  final int unreadCount;
  final List<CarNotification> notifications;

  const CarNotificationResponse({
    required this.page,
    required this.size,
    required this.count,
    required this.unreadCount,
    required this.notifications,
  });

  factory CarNotificationResponse.fromJson(Map<String, dynamic> json) {
    List<CarNotification> notifications = [];
    if (json['notifications'] is List) {
      for (var element in (json['notifications'] as List)) {
        try {
          final CarNotification temp = CarNotification.fromJson(element);
          notifications.add(temp);
        } catch (e) {
          print('error CarNotification.fromJson: $e');
        }
      }
    }

    return CarNotificationResponse(
      page: json['page'] is int ? json['page'] : 0,
      size: json['size'] is int ? json['size'] : 0,
      count: json['count'] is int ? json['count'] : 0,
      unreadCount: json['unread_count'] is int ? json['unread_count'] : 0,
      notifications: notifications,
    );
  }

  @override
  String toString() {
    return 'CarNotificationResponse: {count: $count, unread: $unreadCount, notifications length: ${notifications.length}}';
  }
}

class CarNotification {
  final String id;
  final String qrId;
  final String qrName;
  final int reasonId;
  final String? text;
  final DateTime? time;
  final bool isRead;

  const CarNotification({
    required this.id,
    required this.qrId,
    required this.qrName,
    required this.reasonId,
    this.text,
    this.time,
    required this.isRead,
  });

  factory CarNotification.fromJson(Map<String, dynamic> json) {
    bool isRead = true;
    if (json['status'] is String && json['status'] == 'UNREAD') {
      isRead = false;
    }

    return CarNotification(
      id: json['notification_id'] is String ? json['notification_id'] : '',
      qrId: json['qr_id'] is String ? json['qr_id'] : '',
      qrName: json['qr_name'] is String ? json['qr_name'] : '',
      reasonId: json['reason_id'] is int ? json['reason_id'] : 0,
      text: json['text'] is String ? json['text'] : null,
      time: json['time'] is String ? DateTime.tryParse(json['time']) : null,
      isRead: isRead,
    );
  }

  @override
  String toString() {
    return 'CarNotification: {id: $id, qrId: $qrId, reasonId: $reasonId, text: $text, time: $time, isRead: $isRead}';
  }
}
