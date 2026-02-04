import 'package:flutter/cupertino.dart';
import 'package:get/get.dart';

import '../../../utils/res/text_styles.dart';
import '../../../utils/res/ui_colors.dart';
import '../../../utils/scale_config.dart';

///
/// Виджет обратного отсчета в окне алерта
///
class AlertTimerWidget extends StatelessWidget {
  final RxInt leftSeconds;

  const AlertTimerWidget({required this.leftSeconds, super.key});

  @override
  Widget build(BuildContext context) {
    return Obx(
      () => Padding(
        padding: EdgeInsets.only(top: SC.s4),
        child: Text(
          'Отправка следующего события возможна через ${leftSeconds.value} сек',
          style: TextStyles.regular14.copyWith(
            height: SC.s22 / SC.s14,
            letterSpacing: 0.25,
            color: UiColors.blackD_70,
          ),
        ),
      ),
    );
  }
}
