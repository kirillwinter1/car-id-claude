import 'package:app/utils/scale_config.dart';
import 'package:app/widgets/buttons/ci_radio_button.dart';
import 'package:flutter/material.dart';

import '../../../models/report_event.dart';

class ReportEventWidget extends StatelessWidget {
  final ReportEvent event;
  final bool isSelected;
  final void Function()? onTap;

  const ReportEventWidget({
    required this.event,
    required this.isSelected,
    required this.onTap,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.translucent,
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        margin: EdgeInsets.symmetric(vertical: SC.s6),
        padding: EdgeInsets.fromLTRB(SC.s20, SC.s16, SC.s18, SC.s16),
        width: double.infinity,
        decoration: isSelected
            ? BoxDecoration(
                color: const Color(0xFFF5E8E7),
                border:
                    Border.all(color: const Color(0xFFE86C4A), width: SC.s2),
                borderRadius: BorderRadius.all(Radius.circular(SC.s24)),
              )
            : BoxDecoration(
                color: const Color(0xFFE3F7FD),
                borderRadius: BorderRadius.all(Radius.circular(SC.s24)),
              ),
        child: Row(
          children: [
            Expanded(
              child: Text(
                event.description,
                style: TextStyle(
                  fontSize: SC.s17,
                  fontWeight: FontWeight.w400,
                  fontFamily: 'Roboto',
                  color: Colors.black,
                  height: 22.0 / 17.0,
                ),
              ),
            ),
            CIRadioButton(isSelected: isSelected),
          ],
        ),
      ),
    );
  }
}
