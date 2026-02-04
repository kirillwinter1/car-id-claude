class ReportEvent {
  final int id;
  final String description;

  const ReportEvent({
    required this.id,
    required this.description,
  });

  factory ReportEvent.fromJson(Map json) => ReportEvent(
        id: json['id'],
        description: json['description'],
      );
}
