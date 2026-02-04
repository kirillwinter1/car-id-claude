import 'package:app/controllers/app_controller.dart';
import 'package:app/controllers/auth_controller.dart';
import 'package:app/controllers/notifications_controller.dart';
import 'package:app/screens/home/components/menu_button.dart';
import 'package:app/screens/home/components/menu_text_button.dart';
import 'package:app/screens/support_screen/support_screen.dart';
import 'package:app/utils/const.dart';
import 'package:app/utils/res/svg_assets.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/routes.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/utils/utils.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:get/get.dart';

class MenuContent extends StatelessWidget {
  final double width;
  final double height;
  final Function closeMethod;
  MenuContent({
    required this.width,
    required this.height,
    required this.closeMethod,
    super.key,
  }) {
    appController = Get.find<AppController>();
    authController = Get.find<AuthController>();
  }

  late final AppController appController;
  late final AuthController authController;

  void closeMenu() {
    Future.delayed(const Duration(milliseconds: 300), () => closeMethod());
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: height - (GetPlatform.isAndroid ? SC.s20 : 0),
      width: width,
      margin: GetPlatform.isAndroid
          ? EdgeInsets.symmetric(vertical: SC.s10)
          : EdgeInsets.zero,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.only(
          topRight: Radius.circular(SC.s20),
          bottomRight: Radius.circular(SC.s20),
        ),
        boxShadow: [
          BoxShadow(
            offset: Offset(SC.s1, SC.s2),
            blurRadius: SC.s5,
            color: const Color(0xFF1F0F0B).withOpacity(0.16),
          ),
          BoxShadow(
            offset: Offset(SC.s1, SC.s4),
            blurRadius: SC.s10,
            color: const Color(0xFF1F0F0B).withOpacity(0.16),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ///
          /// Лого
          ///
          Padding(
            padding: EdgeInsets.fromLTRB(SC.s24, SC.s(32), SC.s16, SC.s24),
            child: SvgPicture.asset(
              SvgAssets.logo,
              height: SC.s(34),
            ),
          ),

          ///
          /// Синий контейнер
          ///
          Container(
            width: double.infinity,
            decoration: BoxDecoration(
                color: UiColors.blueJ,
                borderRadius: BorderRadius.only(
                  topRight: Radius.circular(SC.s20),
                  bottomRight: Radius.circular(SC.s20),
                )),
            padding: EdgeInsets.symmetric(horizontal: SC.s24, vertical: SC.s20),
            margin: EdgeInsets.only(right: SC.s16, bottom: SC.s20),
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              GetBuilder<NotificationsController>(builder: (ctrl) {
                return MenuButton(
                  onClick: () {
                    Get.toNamed(CustomRouter.PAGENAME_NOTIFICATIONS);
                    ctrl.setCurrentTab(0);
                    closeMenu();
                  },
                  title: 'Уведомления',
                  iconPath: SvgAssets.bell,
                  counterValue: ctrl.unreadCount,
                );
              }),
              MenuButton(
                onClick: () {
                  Get.toNamed(CustomRouter.PAGENAME_QRMARKS);
                  closeMenu();
                },
                title: 'Метки',
                iconPath: SvgAssets.qr,
              ),
              MenuButton(
                onClick: () {
                  Get.toNamed(CustomRouter.PAGENAME_QR_ADD_EVENT);
                  closeMenu();
                },
                title: 'Сообщить о событии',
                iconPath: SvgAssets.warning,
              ),
            ]),
          ),

          ///
          /// Остальные пункты меню
          ///
          // MenuTextButton(
          //   onClick: () {
          //     appController.openStoreLink();
          //   },
          //   title: 'Оценить приложение',
          // ),
          MenuTextButton(
              onClick: () {
                Get.to(() => const SupportScreen());
                closeMenu();
              },
              title: 'Связаться с нами'),
          MenuTextButton(
              onClick: () {
                // Get.to(() => const RulesScreen(withAcceptButton: false));
                Utils.openUrl(PUBLIC_OFFER_URL);
                closeMenu();
              },
              title: 'Публичная оферта'),
          MenuTextButton(
              onClick: () {
                Utils.openUrl(PRIVACY_POLICY_URL);
                closeMenu();
              },
              title: 'Политика конфиденциальности'),
          MenuTextButton(
              onClick: () async {
                final bool result = await Utils.showOptionsDialog(
                  title: 'Выйти из аккаунта?',
                  buttonTextConfirm: 'Выйти',
                  buttonTextCancel: 'Отмена',
                );

                if (result) {
                  appController.logOut();
                  closeMenu();
                }
              },
              title: 'Выйти'),
          MenuTextButton(
              onClick: () async {
                final bool result = await Utils.showOptionsDialog(
                  title: 'Удаление аккаунта',
                  subtitle:
                      'Все связанные с телефоном метки будут удалены и непригодны для дальнейшего использования',
                  buttonTextConfirm: 'Удалить',
                  buttonTextCancel: 'Отмена',
                );

                if (result) appController.deleteAccount();
              },
              title: 'Удалить аккаунт'),
          const Expanded(child: SizedBox()),

          ///
          /// Номер
          ///
          if (appController.isAuthorized)
            Padding(
              padding: EdgeInsets.only(left: SC.s24, bottom: SC.s(32)),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Номер аккаунта:',
                    style:
                        TextStyles.medium14.copyWith(color: UiColors.blackD_70),
                  ),
                  Text(
                    '+7 ${authController.phoneMaskFormatter.maskText(appController.user!.phoneNumber.substring(1))}',
                    style: TextStyles.regular14
                        .copyWith(color: UiColors.blackD_70),
                  ),
                ],
              ),
            )
        ],
      ),
    );
  }
}
