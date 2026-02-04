// import 'package:firebase_in_app_messaging/firebase_in_app_messaging.dart';
// import 'package:flutterfire_installations/flutterfire_installations.dart';
import 'package:firebase_analytics/firebase_analytics.dart';
import 'package:firebase_analytics/observer.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:flutter/cupertino.dart';
// import 'package:flutter_hms_gms_availability/flutter_hms_gms_availability.dart';
import 'package:get/get.dart';

// import '../../ui/screen/base/base_screen_controller.dart';
// import '../const.dart';
// import '../model/enum/android_build_type.dart';
import 'const.dart';
// import 'app_router_observer.dart';

/// Класс отвечющий за работу отдельно GMS и HMS
/// Google Mobile Services
/// Huawei Mobile Services

class GmsHms {
  static FirebaseAnalytics? analytics;
  static FirebaseAnalyticsObserver? observer;

  static bool isGmsAvailable = false;
  static bool isHmsAvailable = false;

  static Future<void> init() async {
    /*
    if (GetPlatform.isIOS) {
      isGmsAvailable = true;
      isHmsAvailable = false;
    } else {
      isGmsAvailable = await FlutterHmsGmsAvailability.isGmsAvailable;
      isHmsAvailable = await FlutterHmsGmsAvailability.isHmsAvailable;
    }

     */

    if (GetPlatform.isIOS) {
      isGmsAvailable = true;
      isHmsAvailable = false;
    } else {
      isGmsAvailable = ANDROID_BUILD_DEST == AndroidBuildType.google;
      isHmsAvailable = ANDROID_BUILD_DEST == AndroidBuildType.huawei;
    }

    print('GMS Available: $isGmsAvailable');
    print('HMS Available: $isHmsAvailable');

    if (isGmsAvailable) {
      await Firebase.initializeApp();
      FlutterError.onError = FirebaseCrashlytics.instance.recordFlutterError;
      if (BUILD_4_TEST) {
        FirebaseCrashlytics.instance.setUserIdentifier('TEST_BUILD');
      }
      analytics = FirebaseAnalytics.instance;
      observer = FirebaseAnalyticsObserver(analytics: analytics!);
    }
  }

  static List<NavigatorObserver> getNavigatorObservers() {
    // List<NavigatorObserver> res = [Get.find<BaseScreenController>().observer];
    // if (observer != null) res.add(observer!);
    // return res;
    return [];
    // return observer != null ? [observer! /*, AppRouteObserver()*/] : [];
  }
}

class Logger {
  static void log(
    List<String>? logs,
    String reason, {
    dynamic exception,
    StackTrace? stack,
  }) {
    if (!GmsHms.isGmsAvailable) return;

    if (logs != null) {
      for (var l in logs) {
        FirebaseCrashlytics.instance.log(l);
      }
    }

    FirebaseCrashlytics.instance.recordError(
      exception ?? Exception(reason),
      stack ?? StackTrace.current,
      reason: reason,
    );
  }

  /*
  static void logPurchase(Order order) {
    if (!GmsHms.isGmsAvailable || BUILD_4_TEST) return;

    var params = {
      'transaction_id': order.id,
      'currency': 'RUB',
      'value': order.totalCost,
      'tax': 0.0,
      'shipping': order.shippingCost ?? 0.0,
      'coupon': 'n/a',
      'items': order.items
          .map((e) => {
                'item_name': e.name,
                'item_category': 'n/a',
                'item_brand': 'n/a',
                'item_id': e.id,
                'price': e.price,
                'quantity': e.quantity,
              })
          .toList(),
    };

    GmsHms.analytics!.logEvent(
      name: 'purchase',
      parameters: params,
    );
  }

   */

  static void logEvent(String eventName) {
    if (!GmsHms.isGmsAvailable) return;
    print('Log event $eventName');
    GmsHms.analytics!.logEvent(name: eventName, parameters: {});
  }

  static void logBannerEvent({
    required String eventName,
    required String bannerId,
  }) {
    if (!GmsHms.isGmsAvailable) return;
    print('Log banner event - "${eventName}_$bannerId"');
    GmsHms.analytics!.logEvent(name: '${eventName}_$bannerId');
  }
}
