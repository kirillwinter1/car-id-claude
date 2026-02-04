import 'package:app/utils/res/ui_colors.dart';
import 'package:flutter/material.dart';

class CustomProgressIndicator extends StatelessWidget {
  const CustomProgressIndicator({super.key});

  @override
  Widget build(BuildContext context) {
    return const Center(
      child: CircularProgressIndicator(
        color: UiColors.orange,
        strokeWidth: 2,
        strokeCap: StrokeCap.round,
      ),
    );
  }
}
