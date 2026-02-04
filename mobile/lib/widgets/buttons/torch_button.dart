import 'package:app/utils/res/svg_assets.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../../utils/scale_config.dart';

class TorchButton extends StatelessWidget {
  final bool isOn;
  final void Function()? onTap;

  const TorchButton({
    required this.isOn,
    this.onTap,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.translucent,
      onTap: onTap,
      child: Container(
        width: SC.s(44.0),
        height: SC.s(44.0),
        decoration: BoxDecoration(
          color: isOn ? const Color(0xCCFFFFFF) : const Color(0x99000000),
          borderRadius: BorderRadius.all(Radius.circular(SC.s22)),
        ),
        child: Center(
          child: SvgPicture.asset(
            isOn ? SvgAssets.flashlightOn : SvgAssets.flashlightOff,
            height: SC.s(24.0),
            // colorFilter: isOn
            //     ? const ColorFilter.mode(Color(0xFFE86C4A), BlendMode.srcIn)
            //     : null,
          ),
        ),
      ),
    );
  }
}
