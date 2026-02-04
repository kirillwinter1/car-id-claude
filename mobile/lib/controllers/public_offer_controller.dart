import 'package:app/repository/PublicOfferRepository.dart';
import 'package:app/utils/const.dart';
import 'package:get/get.dart';

/// Контроллер, отвечающий за загрузку оферты и сошлашения с бэка
class PublicOfferController extends GetxController {
  final PublicOfferRepository repository = Get.put(PublicOfferRepository());

  // текст ошибки в случае неудачной загрузки
  static const String errorText =
      'Произошла ошибка при загрузке,\nпопробуйте позже.';

  // текст для отображения
  String? offerText;

  bool get isLoaded => offerText != null && offerText!.isNotEmpty;
  bool get isError => offerText == errorText;

  @override
  void onInit() async {
    super.onInit();

    offerText = await repository.fetchPublicOffer().timeout(
          const Duration(seconds: TIMEOUT_DURATION_SECONDS),
          onTimeout: () => errorText,
        );

    update();
  }
}
