class Marketplaces {
  final bool enabled;
  final String? wbLink;
  final String? ozonLink;

  const Marketplaces({
    required this.enabled,
    this.wbLink,
    this.ozonLink,
  });

  bool get hasAnyLink =>
      (wbLink != null && wbLink!.isNotEmpty) ||
      (ozonLink != null && ozonLink!.isNotEmpty);

  factory Marketplaces.fromJson(Map<String, dynamic> json) {
    return Marketplaces(
      enabled: json['activity'] is bool ? json['activity'] : false,
      wbLink: json['wb'] is String ? json['wb'] : null,
      ozonLink: json['ozon'] is String ? json['ozon'] : null,
    );
  }

  @override
  String toString() {
    return 'Marketplaces: {enabled: $enabled, wbLink: $wbLink, ozonLink: $ozonLink}';
  }
}
