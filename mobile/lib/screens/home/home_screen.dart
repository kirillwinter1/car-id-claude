import 'package:app/controllers/app_controller.dart';
import 'package:app/controllers/notifications_controller.dart';
import 'package:app/controllers/qrmarks_controller.dart';
import 'package:app/screens/home/components/main_action_tile.dart';
import 'package:app/screens/home/components/marketplace_button.dart';
import 'package:app/screens/home/components/menu_content.dart';
import 'package:app/screens/home/components/notifications_main_widget.dart';
import 'package:app/utils/const.dart';
import 'package:app/utils/res/svg_assets.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/routes.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/utils/utils.dart';
import 'package:app/widgets/app_bars/main_app_bar.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen>
    with SingleTickerProviderStateMixin {
  final double screenWidth = Get.size.width;

  late AnimationController animationController;

  late double menuWidth = screenWidth * 0.8;
  double menuHeight = Get.size.height -
      Get.mediaQuery.viewPadding.top -
      Get.mediaQuery.viewPadding.bottom;

  @override
  void initState() {
    super.initState();

    // Состояние меню регулируется контроллером анимации
    // (0 - открыто; -screenWidth - закрыто)
    animationController = AnimationController(
      lowerBound: -screenWidth,
      upperBound: 0,
      duration: const Duration(milliseconds: 300),
      vsync: this,
    )..addListener(() => setState(() {}));
  }

  @override
  void dispose() {
    animationController.dispose();
    super.dispose();
  }

  void _openMenu({double? from}) {
    animationController.forward(from: from ?? -screenWidth);
  }

  void _closeMenu({double from = 0}) {
    animationController.reverse(from: from);
  }

  void _closeWithoutAnimation() {
    animationController.value = -screenWidth;
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        Scaffold(
          appBar: MainAppBar(
            onMenuTap: () => _openMenu(),
            onBellTap: () {
              Get.toNamed(CustomRouter.PAGENAME_NOTIFICATIONS);
              Get.find<NotificationsController>().setCurrentTab(0);
            },
            onShareTap: () => Get.find<AppController>().onSharePressed(),
          ),
          body: RefreshIndicator(
            color: UiColors.orange,
            strokeWidth: 1.5,
            onRefresh: () async {
              Get.find<NotificationsController>().loadAllUnread();
              Get.find<AppController>().loadMarketplaces();
            },
            child: CustomScrollView(
              physics: const BouncingScrollPhysics(
                  parent: AlwaysScrollableScrollPhysics()),
              slivers: [
                SliverToBoxAdapter(child: SizedBox(height: SC.s12)),

                ///
                /// Слайдер уведомлений
                ///
                const SliverToBoxAdapter(child: NotificationsMainWidget()),

                ///
                /// Добавить метку и Протестировать
                ///
                GetBuilder<QRMarksController>(builder: (controller) {
                  return SliverToBoxAdapter(
                    child: Column(
                      children: [
                        /// Добавить метку
                        MainActionTile(
                          onClick: () {
                            controller.hasAnyMark
                                ? Get.toNamed(CustomRouter.PAGENAME_QRMARKS)
                                : Get.toNamed(CustomRouter.PAGENAME_QR_SCAN);
                          },
                          title: controller.hasAnyMark
                              ? 'Мои метки'
                              : 'Добавить метку',
                          description: controller.hasAnyMark
                              ? 'Просмотр текущих и добавление новых меток'
                              : 'Соедините метку стикера со своим номером телефона для получения уведомлений',
                          iconPath: SvgAssets.qr,
                          titleStyle: TextStyles.bold14
                              .copyWith(color: UiColors.white94),
                          descriptionStyle: TextStyles.regular12.copyWith(
                              color: UiColors.white70, letterSpacing: 0.25),
                          backgroundColor: UiColors.orangeDark,
                          iconColor: UiColors.white94,
                          iconBackgroundColor: UiColors.orangeExtraDark,
                        ),

                        /// Протестировать
                        if (controller.hasAnyMark == false)
                          MainActionTile(
                            onClick: () {
                              Utils.openUrl(TELEGRAM_BOT_URL);
                            },
                            title: 'Протестировать',
                            description:
                                'Временная метка будет активна 24 часа',
                            iconPath: SvgAssets.hours24,
                            titleStyle: TextStyles.bold14
                                .copyWith(color: UiColors.orange),
                            descriptionStyle: TextStyles.regular12.copyWith(
                                color: UiColors.orangeDark,
                                letterSpacing: 0.25),
                            backgroundColor: UiColors.white,
                            iconColor: UiColors.white,
                            iconBackgroundColor: UiColors.orangeDark,
                            border: Border.all(
                              color: UiColors.orange,
                              width: SC.s2,
                            ),
                            padding:
                                EdgeInsets.fromLTRB(SC.s16, SC.s12, SC.s16, 0),
                            innerPadding: EdgeInsets.fromLTRB(
                                SC.s18, SC.s15, SC.s18, SC.s18),
                          ),
                      ],
                    ),
                  );
                }),

                SliverToBoxAdapter(child: SizedBox(height: SC.s12)),

                ///
                /// Сообщить о событии
                ///
                SliverToBoxAdapter(
                  child: MainActionTile(
                    onClick: () =>
                        Get.toNamed(CustomRouter.PAGENAME_QR_ADD_EVENT),
                    title: 'Сообщить о событии',
                    description:
                        'Передайте информацию о проблеме с их автомобилем владельцам метки',
                    iconPath: SvgAssets.warning,
                    titleStyle:
                        TextStyles.bold14.copyWith(color: UiColors.blackC_80),
                    descriptionStyle: TextStyles.regular12.copyWith(
                        color: UiColors.blackF_60, letterSpacing: 0.25),
                    backgroundColor: UiColors.blueD,
                    iconColor: UiColors.blackC_80,
                    iconBackgroundColor: UiColors.blueCDark,
                  ),
                ),

                SliverToBoxAdapter(child: SizedBox(height: SC.s12)),

                ///
                /// Настроить уведомления
                ///
                SliverToBoxAdapter(
                  child: MainActionTile(
                    onClick: () => Get.toNamed(
                        CustomRouter.PAGENAME_NOTIFICATION_SETTINGS),
                    title: 'Настроить уведомления',
                    description:
                        'Звонок робота, Telegram, Push\u2011уведомления',
                    iconPath: SvgAssets.bell,
                    titleStyle:
                        TextStyles.bold14.copyWith(color: UiColors.blackC_80),
                    descriptionStyle: TextStyles.regular12.copyWith(
                        color: UiColors.blackF_60, letterSpacing: 0.25),
                    backgroundColor: const Color(0xFFEFEFEF),
                    iconColor: UiColors.blackC_80,
                    iconBackgroundColor: const Color(0xFFDEDEDE),
                  ),
                ),

                ///
                /// Заказать стикер
                ///
                GetBuilder<AppController>(
                  builder: (appCtrl) {
                    // если блок выключен или нет ссылок -> скрываем
                    if (appCtrl.marketplaces == null ||
                        appCtrl.marketplaces!.enabled == false ||
                        appCtrl.marketplaces!.hasAnyLink == false) {
                      return const SliverToBoxAdapter(child: SizedBox());
                    }

                    return SliverToBoxAdapter(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Padding(
                            padding: EdgeInsets.fromLTRB(
                                SC.s16, SC.s28, SC.s16, SC.s10),
                            child: Text(
                              'Заказать стикер',
                              style: TextStyles.regular16,
                            ),
                          ),
                          Container(
                            margin: EdgeInsets.symmetric(horizontal: SC.s16),
                            padding: EdgeInsets.symmetric(
                                horizontal: SC.s24, vertical: SC.s18),
                            decoration: BoxDecoration(
                              color: UiColors.orangeLightLight,
                              borderRadius: BorderRadius.circular(100),
                            ),
                            child: Row(
                              children: [
                                MarketplaceButton(
                                    marketplace: Marketplace.ozon,
                                    url: appCtrl.marketplaces?.ozonLink),
                                MarketplaceButton(
                                    marketplace: Marketplace.wb,
                                    url: appCtrl.marketplaces?.wbLink),
                              ],
                            ),
                          ),
                        ],
                      ),
                    );
                  },
                ),

                //
                SliverToBoxAdapter(
                  child: SizedBox(
                      height: Get.mediaQuery.viewPadding.bottom != 0
                          ? Get.mediaQuery.viewPadding.bottom + SC.s12
                          : SC.s12),
                ),
              ],
            ),
          ),
        ),

        ///
        /// Затеменение под меню
        ///
        if (animationController.value != -screenWidth)
          Positioned.fill(
              child: Container(
            color: UiColors.white.withOpacity(Utils.getValueFromNewRange(
                originalValue: animationController.value,
                maxNewRange: 0.7,
                minNewRange: 0,
                maxOriginalRange: 0,
                minOriginalRange: -screenWidth)),
          )),

        ///
        /// Menu
        ///
        Positioned(
          left: animationController.value,
          top: Get.mediaQuery.viewPadding.top,
          child: GestureDetector(
            onPanUpdate: (details) {
              if (animationController.value + details.delta.dx <= 0) {
                animationController.value += details.delta.dx;
              }
            },
            onPanEnd: (details) {
              if (animationController.value > -Get.width * 0.1) {
                _openMenu(from: animationController.value);
              } else {
                _closeMenu(from: animationController.value);
              }
            },
            child: Material(
              color: Colors.transparent,
              child: Row(
                children: [
                  ///
                  /// Основной контент меню
                  ///
                  MenuContent(
                    width: menuWidth,
                    height: menuHeight,
                    closeMethod: _closeWithoutAnimation,
                  ),

                  ///
                  /// Правая прозрачная область
                  ///
                  GestureDetector(
                    onTap: () => _closeMenu(),
                    child: Container(
                      height: menuHeight,
                      width: Get.width - menuWidth,
                      color: Colors.transparent,
                    ),
                  )
                ],
              ),
            ),
          ),
        )
      ],
    );
  }
}
