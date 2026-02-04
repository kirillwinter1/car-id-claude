import 'package:app/utils/res/ui_colors.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/widgets/page_view/widget_size.dart';
import 'package:flutter/material.dart';
import 'package:smooth_page_indicator/smooth_page_indicator.dart';

class DynamicHeightPageView extends StatefulWidget {
  final List<Widget> children;
  const DynamicHeightPageView(this.children, {super.key});

  @override
  State<DynamicHeightPageView> createState() => _DynamicHeightPageViewState();
}

class _DynamicHeightPageViewState extends State<DynamicHeightPageView> {
  late PageController pageController;
  late List<double> _heights;
  int _currentPage = 0;

  double get _currentHeight => _heights[_currentPage];

  @override
  void initState() {
    super.initState();

    _heights = widget.children.map((e) => 0.0).toList();

    pageController = PageController(viewportFraction: 0.945)
      ..addListener(() {
        final newPage = pageController.page?.round() ?? 0;
        if (_currentPage != newPage) {
          setState(() => _currentPage = newPage);
        }
      });
  }

  @override
  void didUpdateWidget(DynamicHeightPageView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.children != oldWidget.children) {
      if (widget.children.length > _heights.length) {
        int difference = widget.children.length - _heights.length;

        for (int i = 0; i < difference; i++) {
          _heights.add(0.0);
        }
      }
    }
  }

  // строим лист и собираем высоту каждого виджета
  List<Widget> get _childrenWithHeights => widget.children
      .asMap()
      .map(
        (index, child) => MapEntry(
          index,
          OverflowBox(
            minHeight: 0,
            maxHeight: double.infinity,
            alignment: Alignment.topCenter,
            child: WidgetSize(
              onSizeChange: (size) =>
                  setState(() => _heights[index] = size.height),
              child: Align(child: child),
            ),
          ),
        ),
      )
      .values
      .toList();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        TweenAnimationBuilder<double>(
          curve: Curves.easeInOutCubic,
          duration: const Duration(milliseconds: 300),
          tween: Tween<double>(begin: _heights[0], end: _currentHeight),
          builder: (context, value, child) =>
              SizedBox(height: value, child: child),
          child: PageView(
            clipBehavior: Clip.none,
            controller: pageController,
            children: _childrenWithHeights
                .asMap()
                .map((index, child) => MapEntry(
                    index,
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 6),
                      child: child,
                    )))
                .values
                .toList(),
          ),
        ),

        ///
        /// Dots
        ///
        AnimatedCrossFade(
          firstChild: Align(
            alignment: Alignment.center,
            child: widget.children.length > 1
                ? Padding(
                    padding: EdgeInsets.only(top: SC.s12),
                    child: SmoothPageIndicator(
                      controller: pageController,
                      count: widget.children.length,
                      effect: ScrollingDotsEffect(
                        dotColor: UiColors.orangeLightLight,
                        activeDotColor: UiColors.orange,
                        dotWidth: SC.s6,
                        dotHeight: SC.s6,
                        activeDotScale: 1.33,
                        spacing: SC.s6,
                        radius: 10,
                        paintStyle: PaintingStyle.fill,
                      ),
                    ),
                  )
                : const SizedBox(),
          ),
          secondChild: const SizedBox(),
          crossFadeState: widget.children.length > 1
              ? CrossFadeState.showFirst
              : CrossFadeState.showSecond,
          duration: const Duration(milliseconds: 200),
        ),
      ],
    );
  }
}
