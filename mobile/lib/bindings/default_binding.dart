import 'package:get/get.dart';

import '../controllers/home_controller.dart';
import '../controllers/splash_controller.dart';

class DefaultBinding implements Bindings {
  @override
  void dependencies() {
    Get.lazyPut<SplashController>(() {
      return SplashController();
    });
    Get.lazyPut<HomeController>(() {
      return HomeController();
    });
  }
}
