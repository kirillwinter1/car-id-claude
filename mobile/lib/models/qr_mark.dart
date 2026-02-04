///
/// Оновная сущность - QR и соответствующие атрибуты
///

class QRMark {
  final String qrId;
  int? seqNumber;
  int? batchNumber;
  String? qrName;
  bool? printed;
  String? status;
  DateTime? createdDate;
  DateTime? updatedDate;
  DateTime? activateDate;
  DateTime? deleteDate;
  int? userId;

  QRMark({
    required this.qrId,
    this.seqNumber = 0,
    this.batchNumber = 0,
    this.qrName = '',
    this.printed = false,
    this.status = 'NEW',
    this.createdDate,
    this.updatedDate,
    this.activateDate,
    this.deleteDate,
    this.userId,
  });

  bool get isDeleted => status == 'DELETED';
  bool get isActive => status != 'DELETED';

  factory QRMark.fromJson(Map json) => QRMark(
        qrId: json['qr_id'] ?? '0',
        seqNumber: json['seq_number'] ?? 0,
        batchNumber: json['batch_number'] ?? 0,
        qrName: json['qr_name'] ?? 'name',
        printed: json['printed'] ?? false,
        status: json['status'] ?? 'NEW',
        createdDate: DateTime.tryParse(json['created_date']) ?? DateTime.now(),
        updatedDate: DateTime.tryParse(json['updated_date']) ?? DateTime.now(),
        activateDate:
            DateTime.tryParse(json['activate_date']) ?? DateTime.now(),
        deleteDate: json['delete_date'] != null
            ? DateTime.tryParse(json['delete_date']) ?? DateTime.now()
            : null,
        userId: json['userId'] ?? 0,
      );
}

/*
  {
  "method": "string",
  "result": "string",
  "params": [
    {
      "qr_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "seq_number": 0,
      "batch_number": 0,
      "qr_name": "string",
      "printed": true,
      "status": "NEW",
      "created_date": "2024-04-07T09:25:48.597Z",
      "updated_date": "2024-04-07T09:25:48.597Z",
      "activate_date": "2024-04-07T09:25:48.597Z",
      "delete_date": "2024-04-07T09:25:48.597Z",
      "userId": 0
    }
  ]
}
   */
