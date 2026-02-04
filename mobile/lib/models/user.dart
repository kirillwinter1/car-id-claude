class User {
  final int? id;
  final String phoneNumber;
  // String? userName;
  // String? city;
  // String? carModel;
  // String? carNumber;
  // String? telegramChatId;

  User({
    required this.phoneNumber,
    this.id,
    // this.userName,
    // this.city,
    // this.carModel,
    // this.carNumber,
    // this.telegramChatId,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'] is int ? json['id'] : null,
      phoneNumber: json['phone_number'],
      // userName: json['userName'] is String ? json['userName'] : null,
      // city: json['city'] is String ? json['city'] : null,
      // carModel: json['carModel'] is String ? json['carModel'] : null,
      // carNumber: json['carNumber'] is String ? json['carNumber'] : null,
      // telegramChatId:
      //     json['telegramChatId'] is String ? json['telegramChatId'] : null,
    );
  }

  @override
  String toString() {
    return 'User: {id: $id, phoneNumber: $phoneNumber}';
  }
}
