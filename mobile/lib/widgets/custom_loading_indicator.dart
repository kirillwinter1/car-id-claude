import 'dart:math' as math;

import 'package:app/utils/res/ui_colors.dart';
import 'package:flutter/widgets.dart';

class CustomLoadingIndicator extends StatefulWidget {
  final double startRatio;
  final double endRatio;
  final double radius;
  final int tickCount;
  final Color activeColor;
  final Color inactiveColor;
  final Duration animationDuration;
  final double relativeWidth;
  final bool animating;

  const CustomLoadingIndicator({
    this.startRatio = 0.5,
    this.endRatio = 1.0,
    this.radius = 18,
    this.tickCount = 8,
    this.activeColor = UiColors.iconMainOrange,
    this.inactiveColor = UiColors.iconMainOrange10,
    this.animationDuration = const Duration(milliseconds: 1000),
    this.relativeWidth = 1,
    this.animating = true,
    super.key,
  });

  @override
  State<CustomLoadingIndicator> createState() => _CustomLoadingIndicatorState();
}

class _CustomLoadingIndicatorState extends State<CustomLoadingIndicator>
    with SingleTickerProviderStateMixin {
  late AnimationController _animationController;

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      duration: widget.animationDuration,
      vsync: this,
    );
    if (widget.animating) {
      _animationController.repeat();
    }
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  @override
  void didUpdateWidget(CustomLoadingIndicator oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.animating != oldWidget.animating) {
      if (widget.animating) {
        _animationController.repeat();
      } else {
        _animationController.stop();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: widget.radius * 2,
      width: widget.radius * 2,
      child: CustomPaint(
        painter: _CustomLoadingIndicatorPainter(
          startRatio: widget.startRatio,
          endRatio: widget.endRatio,
          radius: widget.radius,
          tickCount: widget.tickCount,
          activeColor: widget.activeColor,
          inactiveColor: widget.inactiveColor,
          relativeWidth: widget.relativeWidth,
          animationController: _animationController,
        ),
      ),
    );
  }
}

class _CustomLoadingIndicatorPainter extends CustomPainter {
  final int _halfTickCount;
  final Animation<double> animationController;
  final Color activeColor;
  final Color inactiveColor;
  final double relativeWidth;
  final int tickCount;
  final double radius;
  final RRect _tickRRect;
  final double startRatio;
  final double endRatio;

  _CustomLoadingIndicatorPainter({
    required this.radius,
    required this.tickCount,
    required this.animationController,
    required this.activeColor,
    required this.inactiveColor,
    required this.relativeWidth,
    required this.startRatio,
    required this.endRatio,
  })  : _halfTickCount = tickCount ~/ 2,
        _tickRRect = RRect.fromLTRBXY(
          -radius * endRatio,
          relativeWidth * radius / 10,
          -radius * startRatio,
          -relativeWidth * radius / 10,
          relativeWidth * radius / 10,
          relativeWidth * radius / 10,
        ),
        super(repaint: animationController);

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint();
    canvas
      ..save()
      ..translate(size.width / 2, size.height / 2);
    final activeTick = (tickCount * animationController.value).floor();
    for (int i = 0; i < tickCount; ++i) {
      paint.color = Color.lerp(
        activeColor,
        inactiveColor,
        ((i + activeTick) % tickCount) / _halfTickCount,
      )!;
      canvas
        ..drawRRect(_tickRRect, paint)
        ..rotate(-math.pi * 2 / tickCount);
    }
    canvas.restore();
  }

  @override
  bool shouldRepaint(_CustomLoadingIndicatorPainter oldPainter) {
    return oldPainter.animationController != animationController;
  }
}
