import 'package:app/utils/res/svg_assets.dart';
import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';

class AnimatedCheck extends StatefulWidget {
  final bool alreadyChecked;
  const AnimatedCheck({this.alreadyChecked = false, super.key});

  @override
  State<AnimatedCheck> createState() => _AnimatedCheckState();
}

class _AnimatedCheckState extends State<AnimatedCheck> {
  double offset = 0;
  double openedOffset = SC.s(100);

  @override
  void initState() {
    super.initState();

    if (widget.alreadyChecked) {
      offset = openedOffset;
    } else {
      _toggleOffset();
    }
  }

  Future<void> _toggleOffset() async {
    await Future.delayed(const Duration(milliseconds: 100));
    offset = openedOffset;
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      alignment: Alignment.center,
      children: [
        SvgPicture.asset(
          SvgAssets.check,
          width: SC.s(64),
          height: SC.s(64),
          color: UiColors.greenSuccess,
        ),
        AnimatedPositioned(
            left: offset,
            duration: const Duration(milliseconds: 500),
            child: Container(
              width: SC.s(76),
              height: SC.s(64),
              color: Colors.white,
            )),
      ],
    );
  }
}
