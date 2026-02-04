import 'package:app/models/car_notification.dart';
import 'package:app/models/report_event.dart';
import 'package:app/repository/NotificationsRepository.dart';
import 'package:app/repository/QRReportEventRepository.dart';
import 'package:app/utils/utils.dart';
import 'package:get/get.dart';

/// Контроллер, отвечающий за все уведомления внутри приложения, содержит
/// методы загрузки непрочитанных/всех уведомлений, метод отметки прочитанным
class NotificationsController extends GetxController {
  final NotificationsRepository repository = Get.put(NotificationsRepository());
  final QRReportEventRepository eventRepository =
      Get.put(QRReportEventRepository());

  // вкладки Входящие/Исходящие
  // 0 = Входящие; 1 = Исходящие
  int currentTab = 0;

  // активные события с бэка (report.get_all_reasons) для расшифровки уведомлений
  List<ReportEvent>? events;
  bool eventsLoaded = false;

  // все уведомления
  List<CarNotification> allNotifications = [];
  // непрочитанные уведомления
  List<CarNotification> unreadNotifications = [];

  // все исходящие уведомления
  List<CarNotification> allOutgoingNotifications = [];

  // текущий статус загрузки входящих
  bool isLoading = false;
  // текущий статус загрузки исходящих
  bool isLoadingOutgoing = false;

  // счетчики все/непрочитанные
  int? total;
  int unreadCount = 0;

  // пагинация
  int pageAll = 0;
  int pageUnread = 0;
  int pageAllOutgoing = 0;

  static const int perPage = 20;

  ///
  /// Метод загружает постранично все непрочитанные уведомления
  ///
  void loadAllUnread() async {
    loadEvents();

    // сбрасываем на нулевую страницу
    if (pageUnread != 0) pageUnread = 0;

    // временный лист загруженных уведомлений
    List<CarNotification> tempList = [];

    // цикличная постраничная загрузка непрочитанных уведомлений
    do {
      // print('ITERATION Unread /// page: $pageUnread');
      final CarNotificationResponse? response = await repository
          .fetchUnreadNotifications(page: pageUnread, perPage: perPage);

      if (response != null) {
        unreadCount = response.count;
        tempList.addAll(response.notifications);
        pageUnread++;
      }

      // print(
      //     'CONDITION Unread /// ${pageUnread * perPage < unreadCount} /// page: $pageUnread, unreadCount: $unreadCount');
    } while (pageUnread * perPage < unreadCount);

    // сохраняем загруженные уведомления для отображения в UI
    unreadNotifications = tempList;

    // добавляем в список всех уведомлений новые непрочитанные,
    // чтобы лишний раз не дергать метод загрузки всех уведомлений
    // (кейс: когда пришел пуш а у юзера уже открыт экран всех уведомлений)
    List<CarNotification> tempUnread =
        allNotifications.where((element) => element.isRead == false).toList();
    if (unreadNotifications.length > tempUnread.length) {
      for (CarNotification element in unreadNotifications) {
        if (tempUnread.any((e) => e.id == element.id) == false) {
          allNotifications.insert(0, element);
        }
      }
    }

    update();
  }

  ///
  /// Метод загружает постранично все уведомления
  ///
  void loadAllNotifications() async {
    loadEvents();
    isLoading = true;

    // сбрасываем на нулевую страницу и очищаем старый список уведомлений
    if (pageAll != 0) pageAll = 0;
    if (allNotifications.isNotEmpty) allNotifications = [];

    // цикличная постраничная загрузка всех уведомлений
    do {
      // print('ITERATION All /// page: $pageAll');
      final CarNotificationResponse? response = await repository
          .fetchAllNotifications(page: pageAll, perPage: perPage);

      if (response != null) {
        total = response.count;
        unreadCount = response.unreadCount;
        allNotifications.addAll(response.notifications);
        pageAll++;
        update();
      }

      // print(
      //     'CONDITION All /// ${pageAll * perPage < (total ?? 0)} /// page: $pageAll, total: $total');
    } while (pageAll * perPage < (total ?? 0));

    isLoading = false;
    update();
  }

  ///
  /// Метод помечает уведомление прочитанным и обновляет листы для UI
  ///
  void markAsRead(String id) async {
    // пытаемся получить обновленное уведомление с прочтенным статусом
    final CarNotification? updatedNotification =
        await repository.markAsRead(id);

    if (updatedNotification != null) {
      // убираем из листа непрочитанных и обновляем счетчик
      unreadNotifications.removeWhere((element) => element.id == id);
      unreadCount--;

      // ищем в листе всех уведомлений и если найдено совпадение
      // удаляем старое и добавляем в конец новое (уже прочтенное)
      int index = allNotifications.indexWhere((element) => element.id == id);
      if (index >= 0) {
        allNotifications.removeAt(index);
        allNotifications.add(updatedNotification);
      }

      update();
    } else {
      Utils.showSnackBar(title: 'Не удалось отметить прочитанным');
    }
  }

  ///
  /// Метод один раз загружает активные события (report.get_all_reasons)
  ///
  void loadEvents() async {
    if (!eventsLoaded) {
      events = await eventRepository.loadReportEvents();
      eventsLoaded = events != null;
    }
  }

  /// Сброс до дефолтных настроек
  void clearDependencies() {
    total = null;
    pageAll = 0;
    pageUnread = 0;
    unreadCount = 0;
    allNotifications = [];
    unreadNotifications = [];
    update();
  }

  /// Метод изменения вкладки
  void setCurrentTab(int newTab) {
    if (currentTab != newTab) {
      currentTab = newTab;
      update();
    }
  }

  ///
  /// Метод загружает постранично все ИСХОДЯЩИЕ уведомления
  ///
  void loadAllOutgoingNotifications() async {
    isLoadingOutgoing = true;

    // сбрасываем на нулевую страницу и очищаем старый список уведомлений
    if (pageAllOutgoing != 0) pageAllOutgoing = 0;
    if (allOutgoingNotifications.isNotEmpty) allOutgoingNotifications = [];

    // цикличная постраничная загрузка всех уведомлений
    do {
      // print('ITERATION All /// page: $pageAll');
      final CarNotificationResponse? response =
          await repository.fetchAllOutgoingNotifications(
              page: pageAllOutgoing, perPage: perPage);

      if (response != null) {
        allOutgoingNotifications.addAll(response.notifications);
        pageAllOutgoing++;
        update();
      }

      // print(
      //     'CONDITION All Outgoing /// ${pageAllOutgoing * perPage < (total ?? 0)} /// page: $pageAllOutgoing, total: $total');
    } while (pageAllOutgoing * perPage < (total ?? 0));

    isLoadingOutgoing = false;
    update();
  }
}
