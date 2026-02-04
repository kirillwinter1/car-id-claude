import 'package:flutter/cupertino.dart';

import '../../utils/scale_config.dart';

class CIRadioButton extends StatelessWidget {
  final bool isSelected;

  const CIRadioButton({
    required this.isSelected,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    var s = SC.s(40.0);
    var ins = s * 0.55;
    var ins2 = s * 0.28;
    return AnimatedContainer(
      duration: const Duration(milliseconds: 300),
      width: s,
      height: s,
      decoration: BoxDecoration(
        color: isSelected ? const Color(0xFFF2C8BD) : const Color(0xFFB9ECF9),
        borderRadius: BorderRadius.all(Radius.circular(s * 0.5)),
      ),
      child: Center(
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 300),
          width: ins,
          height: ins,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.all(Radius.circular(ins * 0.5)),
            border: Border.all(
              color: isSelected
                  ? const Color(0xFFDB5430)
                  : const Color.fromARGB(255, 59, 71, 74),
              width: SC.s(1.75),
            ),
          ),
          child: Center(
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 300),
              width: ins2,
              height: ins2,
              decoration: BoxDecoration(
                color: isSelected ? const Color(0xFFDB5430) : null,
                borderRadius: BorderRadius.all(Radius.circular(ins2 * 0.5)),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
