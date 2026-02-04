// ignore_for_file: unnecessary_getters_setters, unnecessary_cast, avoid_print

import 'package:app/controllers/app_event.dart';
import 'package:app/controllers/push_notification_controller.dart';
import 'package:app/models/marketplaces.dart';
import 'package:app/models/user.dart';
import 'package:app/repository/MarketplaceLinksRepository.dart';
import 'package:app/repository/UserRepository.dart';
import 'package:app/utils/const.dart';
import 'package:app/utils/routes.dart';
import 'package:app/utils/utils.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:get/get.dart';
import 'package:get_storage/get_storage.dart';
import 'package:keyboard_utils/keyboard_utils.dart';
import 'package:keyboard_utils/keyboard_listener.dart' as keyboard_listener;
import 'package:share/share.dart';

import '../models/report_event.dart';

/// Главный контроллер приложения, хранит текущего юзера и токен,
/// содержит методы восттановлению/удалению/логауту юзера,
/// а также параметры из Firebase (ссылки на сторы)
class AppController extends GetxController {
  final storage = GetStorage();
  final UserRepository userRepository = Get.put(UserRepository());

  List<ReportEvent> reportEvents = [];

  // Пользователь
  final Rx<User?> _user = (null as User?).obs;
  User? get user => _user.value;
  set user(User? user) {
    _user.value = user;
  }

  // Токен
  String? _token;
  String? get token => _token;
  set token(String? tokenValue) {
    Future.delayed(const Duration(milliseconds: 1000),
        () => Get.find<PushNotificationController>().updateFCMToken());
    _token = tokenValue;
  }

  bool get isAuthorized => _token != null && _token!.isNotEmpty;

  // Ссылки на маркетплейсы
  Marketplaces? marketplaces;
  final marketplacesRepo = Get.put(MarketplaceLinksRepository());

  KeyboardUtils keyboardUtils = KeyboardUtils();
  int? _idKeyboardListener;
  final keyboardHeight = 0.0.obs;

  @override
  void onInit() {
    super.onInit();

    // Init Firebase params
    _loadInitialSettings();
    paramsStream = appParamsRef.doc('params').snapshots();
    paramsStream.listen((event) => _proceedSnapshot(event));

    // загружаем ссылки на маркетплейсы с бэка
    loadMarketplaces();

    /// Init keyboard listener
    _idKeyboardListener = keyboardUtils.add(
      listener: keyboard_listener.KeyboardListener(willHideKeyboard: () {
        keyboardHeight.value = keyboardUtils.keyboardHeight;

        print(keyboardHeight.value.toString());
      }, willShowKeyboard: (double keyboardHeight) {
        this.keyboardHeight.value = keyboardHeight;
        print('show ${this.keyboardHeight.value.toString()}');
      }),
    );
  }

  ///
  /// Метод восстанавливает пользователя по сохраненному токену
  ///
  Future<void> restoreUser() async {
    String? tempToken = storage.read(KEY_USER_TOKEN);
    print("============ STORAGE TOKEN ===============");
    print("token: $tempToken");
    if (tempToken != null && tempToken.isNotEmpty) {
      try {
        var tm = DateTime.now().millisecondsSinceEpoch;
        user = await userRepository.getUserByToken(tempToken);
        update();
        print(
            '************* getUserByToken ${DateTime.now().millisecondsSinceEpoch - tm}');
        if (user != null) {
          token = tempToken;
        }
      } catch (error) {
        print("restoreUser error: $error");
      }
    }
  }

  ///
  /// Метод удаления аккаунта
  ///
  void deleteAccount() async {
    if (user != null && token != null) {
      Utils.showLoader();
      bool result = await userRepository.deleteUser(token!);
      Utils.hideLoader();

      if (result) {
        logOut();
      } else {
        Utils.showSnackBar(
            title: 'Произошла ошибка при удалении, попробуйте позже');
      }
    }
  }

  ///
  /// Метод логаута
  ///
  Future<void> logOut() async {
    if (user != null && token != null) {
      final bool result = await userRepository.logout(token!);

      if (result) {
        storage.erase();
        user = null;
        token = null;
        Get.offAllNamed(CustomRouter.PAGENAME_SPLASH);

        // хук на логаут юзера
        Get.find<AppEvent>().onUserLogout();

        update();
      } else {
        Utils.showSnackBar(
            title: 'Произошла ошибка при выходе из аккаунта, попробуйте позже');
      }
    }
  }

  ///
  /// Метод открывает страницу Car ID в соответствующем магазине приложений
  ///
  void openStoreLink() {
    switch (DEST_STORE) {
      case DestStore.apple:
        Utils.openUrl(linkIos.value);
      case DestStore.google:
        Utils.openUrl(linkAndroid.value);
      case DestStore.rustore:
        Utils.openUrl(linkRustore.value);
    }
  }

  ///
  /// Метод по нажатию на "Поделиться" на главном экране
  ///
  void onSharePressed() {
    switch (DEST_STORE) {
      case DestStore.apple:
        if (linkIos.value.isEmpty) return;
        Share.share(linkIos.value);
      case DestStore.google:
        if (linkAndroid.value.isEmpty) return;
        Share.share(linkAndroid.value);
      case DestStore.rustore:
        if (linkRustore.value.isEmpty) return;
        Share.share(linkRustore.value);
    }
  }

  ///
  /// Метод загружает ссылки на маркетплейсы с бэка
  ///
  void loadMarketplaces() async {
    marketplaces = await marketplacesRepo.fetchMarketplaceLinks().timeout(
          const Duration(seconds: TIMEOUT_DURATION_SECONDS),
          onTimeout: () => null,
        );
    update();
  }

  //////////////////////////////////////////////////
  /// Блок работы с фаербейзом для разных флагов ///
  //////////////////////////////////////////////////
  final appParamsRef = FirebaseFirestore.instance.collection('app_params');
  late Stream paramsStream;

  // линк в App Store
  final linkIos = ''.obs;
  // линк в Google Play
  final linkAndroid = ''.obs;
  // линк в Rustore
  final linkRustore = ''.obs;

  Future<void> _loadInitialSettings() async =>
      _proceedSnapshot(await appParamsRef.doc('params').get());

  void _proceedSnapshot(DocumentSnapshot<Map<String, dynamic>> snapshot) {
    if (snapshot.data() != null) {
      linkIos.value = snapshot.data()!['ios_link'];
      linkAndroid.value = snapshot.data()!['android_link'];
      linkRustore.value = snapshot.data()!['rustore_link'];
    }
  }
}
