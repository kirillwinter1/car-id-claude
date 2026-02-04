import 'package:app/models/qr_mark.dart';
import 'package:app/utils/res/svg_assets.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';

class QRMarkTile extends StatelessWidget {
  final QRMark qrMark;
  final VoidCallback? onDeletePressed;

  const QRMarkTile({
    required this.qrMark,
    this.onDeletePressed,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return Opacity(
      opacity: qrMark.isActive ? 1 : 0.4,
      child: Container(
        padding: EdgeInsets.symmetric(horizontal: SC.s20, vertical: SC.s24),
        decoration: BoxDecoration(
            color: UiColors.white,
            borderRadius: BorderRadius.circular(SC.s24),
            boxShadow: [
              BoxShadow(
                offset: Offset(0, SC.s1),
                blurRadius: SC.s4,
                color: UiColors.black.withOpacity(0.16),
              ),
              BoxShadow(
                offset: Offset(0, SC.s1),
                blurRadius: SC.s6,
                color: UiColors.black.withOpacity(0.08),
              ),
            ]),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              height: SC.s28,
              width: SC.s28,
              padding: EdgeInsets.all(SC.s4),
              decoration: BoxDecoration(
                color: UiColors.orangeLightLight,
                borderRadius: BorderRadius.circular(50),
              ),
              child: SvgPicture.asset(SvgAssets.qr),
            ),
            SizedBox(height: SC.s20),
            Text(
              qrMark.qrName ?? '',
              style: TextStyles.medium16.copyWith(color: UiColors.black),
            ),
            SizedBox(height: SC.s4),
            // Text('ID метки: ${qrMark.qrId}',
            Text('ID метки: ${qrMark.seqNumber.toString().padLeft(7, '0')}',
                style:
                    TextStyles.regular12.copyWith(color: UiColors.blackC_80)),
            SizedBox(height: SC.s30),
            qrMark.isActive
                ? Align(
                    alignment: Alignment.centerRight,
                    child: GestureDetector(
                      onTap: onDeletePressed,
                      child: Text(
                        'Удалить',
                        style: TextStyles.medium14
                            .copyWith(color: UiColors.blackA_94),
                      ),
                    ),
                  )
                : Align(
                    alignment: Alignment.centerRight,
                    child: Text(
                      'Удалена',
                      style: TextStyles.medium14
                          .copyWith(color: UiColors.blackA_94),
                    ),
                  ),
          ],
        ),
      ),
    );
  }
}
