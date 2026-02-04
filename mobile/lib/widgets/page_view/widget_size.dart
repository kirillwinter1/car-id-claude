import 'package:flutter/material.dart';

class WidgetSize extends StatefulWidget {
  final Widget child;
  final ValueChanged<Size> onSizeChange;

  const WidgetSize({
    required this.child,
    required this.onSizeChange,
    super.key,
  });

  @override
  State<WidgetSize> createState() => _WidgetSizeState();
}

class _WidgetSizeState extends State<WidgetSize> {
  Size? _oldSize;

  @override
  Widget build(BuildContext context) {
    WidgetsBinding.instance.addPostFrameCallback((_) => _notifySize());
    return widget.child;
  }

  void _notifySize() {
    if (!mounted) {
      return;
    }
    final Size? size = context.size;
    if (_oldSize != size && size != null) {
      _oldSize = size;
      widget.onSizeChange(size);
    }
  }
}
