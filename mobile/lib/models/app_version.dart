import 'package:app/utils/const.dart';

class AppVersion {
  final int appleCurrent;
  final int appleMin;
  final int googleCurrent;
  final int googleMin;
  final int rustoreCurrent;
  final int rustoreMin;

  AppVersion({
    required this.appleCurrent,
    required this.appleMin,
    required this.googleCurrent,
    required this.googleMin,
    required this.rustoreCurrent,
    required this.rustoreMin,
  });

  int get minimal {
    switch (DEST_STORE) {
      case DestStore.google:
        return googleMin;
      case DestStore.apple:
        return appleMin;
      case DestStore.rustore:
        return rustoreMin;
    }
  }

  int get current {
    switch (DEST_STORE) {
      case DestStore.google:
        return googleCurrent;
      case DestStore.apple:
        return appleCurrent;
      case DestStore.rustore:
        return rustoreMin;
    }
  }

  factory AppVersion.fromJson(Map<String, dynamic> json) {
    return AppVersion(
      appleCurrent: json['apple_current'] is String
          ? int.tryParse(json['apple_current']) ?? 1
          : 1,
      appleMin: json['apple_min'] is String
          ? int.tryParse(json['apple_min']) ?? 1
          : 1,
      googleCurrent: json['google_current'] is String
          ? int.tryParse(json['google_current']) ?? 1
          : 1,
      googleMin: json['google_min'] is String
          ? int.tryParse(json['google_min']) ?? 1
          : 1,
      rustoreCurrent: json['rustore_current'] is String
          ? int.tryParse(json['rustore_current']) ?? 1
          : 1,
      rustoreMin: json['rustore_min'] is String
          ? int.tryParse(json['rustore_min']) ?? 1
          : 1,
    );
  }

  @override
  String toString() {
    return 'AppVersion: {appleCurrent: $appleCurrent, appleMin: $appleMin,}';
  }
}
