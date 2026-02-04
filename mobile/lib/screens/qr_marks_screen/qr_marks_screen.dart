import 'package:app/controllers/qrmarks_controller.dart';
import 'package:app/screens/qr_marks_screen/components/qr_mark_tile.dart';
import 'package:app/utils/routes.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/widgets/app_bars/title_app_bar.dart';
import 'package:app/widgets/buttons/solid_button.dart';
import 'package:flutter/material.dart';
import 'package:flutter_staggered_grid_view/flutter_staggered_grid_view.dart';
import 'package:get/get.dart';

class QRMarksScreen extends StatelessWidget {
  const QRMarksScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return GetBuilder<QRMarksController>(builder: (controller) {
      return Scaffold(
        appBar: const TitleAppBar(
          title: 'Метки',
        ),
        body: Column(
          children: [
            Expanded(
              child: MasonryGridView.count(
                physics: const BouncingScrollPhysics(),
                padding:
                    EdgeInsets.symmetric(horizontal: SC.s16, vertical: SC.s14),
                crossAxisCount: 2,
                mainAxisSpacing: SC.s10,
                crossAxisSpacing: SC.s10,
                itemCount: controller.qrMarks.length,
                itemBuilder: (ctx, i) => QRMarkTile(
                  qrMark: controller.qrMarks[i],
                  onDeletePressed: () =>
                      controller.deleteQRMark(controller.qrMarks[i]),
                ),
              ),
            ),
            SolidButton(
              title: 'Добавить метку',
              onPressed: () =>
                  Get.toNamed(CustomRouter.PAGENAME_QR_SCAN, arguments: true),
              padding: EdgeInsets.fromLTRB(SC.s16, SC.s12, SC.s16,
                  SC.s10 + Get.mediaQuery.viewPadding.bottom),
            )
          ],
        ),
      );
    });
  }
}
