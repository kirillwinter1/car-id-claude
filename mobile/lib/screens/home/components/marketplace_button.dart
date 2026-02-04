import 'package:app/controllers/app_controller.dart';
import 'package:app/utils/res/svg_assets.dart';
import 'package:app/utils/res/text_styles.dart';
import 'package:app/utils/scale_config.dart';
import 'package:app/utils/utils.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/svg.dart';

enum Marketplace { ozon, wb }

class MarketplaceButton extends StatelessWidget {
  final Marketplace marketplace;
  final String? url;
  const MarketplaceButton({
    required this.marketplace,
    required this.url,
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    final String title = switch (marketplace) {
      Marketplace.ozon => 'Ozon',
      Marketplace.wb => 'Wildberries',
    };
    final String icon = switch (marketplace) {
      Marketplace.ozon => SvgAssets.ozon,
      Marketplace.wb => SvgAssets.wb,
    };

    if (url == null || url!.isEmpty) return const SizedBox();

    return GestureDetector(
      onTap: () async {
        Utils.openUrl(url!);
      },
      child: Container(
        padding: EdgeInsets.only(top: SC.s4),
        margin: EdgeInsets.only(right: SC.s16),
        color: Colors.transparent,
        child: Column(
          children: [
            Padding(
              padding: EdgeInsets.symmetric(horizontal: SC.s12),
              child: SvgPicture.asset(icon),
            ),
            SizedBox(height: SC.s8),
            Text(
              title,
              style: TextStyles.regular12
                  .copyWith(color: Colors.black.withOpacity(0.72)),
            ),
          ],
        ),
      ),
    );
  }
}
