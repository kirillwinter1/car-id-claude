import 'package:app/controllers/splash_controller.dart';
import 'package:app/utils/res/svg_assets.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';
import 'package:get/get.dart';

class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return GetBuilder<SplashController>(builder: (controller) {
      return Scaffold(
        body: Stack(
          alignment: Alignment.center,
          children: [
            Center(
              child: SvgPicture.asset(SvgAssets.logoSplash),
            ),
            Positioned(
                bottom: 0,
                child: Obx(() => Padding(
                      padding: EdgeInsets.only(bottom: SC.s(52)),
                      child: Text(
                        'Версия: ${controller.currentAppVersion.value}',
                        style: TextStyle(
                          fontSize: SC.s12,
                          fontWeight: FontWeight.w300,
                          color: const Color(0xFF0B2228).withOpacity(0.86),
                          fontFamily: 'Roboto',
                        ),
                      ),
                    )))
          ],
        ),
      );
    });
  }
}
