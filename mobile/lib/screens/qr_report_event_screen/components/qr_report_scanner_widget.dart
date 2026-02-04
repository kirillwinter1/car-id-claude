import 'package:app/controllers/qr_report_event_controller.dart';
import 'package:flutter/cupertino.dart';
import 'package:qr_code_scanner/qr_code_scanner.dart';

class QRReportScannerWidget extends StatefulWidget {
  final QRReportEventController controller;

  const QRReportScannerWidget(this.controller, {Key? key}) : super(key: key);

  @override
  State<QRReportScannerWidget> createState() => _QRReportScannerWidgetState();
}

class _QRReportScannerWidgetState extends State<QRReportScannerWidget> {
  final GlobalKey qrKey = GlobalKey(debugLabel: 'QR');

  // In order to get hot reload to work we need to pause the camera if the platform
  // is android, or resume the camera if the platform is iOS.
  @override
  void reassemble() {
    super.reassemble();
    runScanner();
  }

  void runScanner() {
    if (widget.controller.scannerController != null) {
      widget.controller.scannerController!.resumeCamera();
    }
  }

  @override
  void initState() {
    super.initState();

    // widget.controller.overlayMeta.listen((p0) {
    //   if (p0 is ScannerOverlayMeta) {
    //     scannerController?.pauseCamera();
    //   } else {
    //     scannerController?.resumeCamera();
    //   }
    // });
  }

  @override
  void dispose() {
    widget.controller.scannerController?.dispose();
    super.dispose();
  }

  void _onQRViewCreated(QRViewController ctrl) {
    widget.controller.scannerController = ctrl;
    ctrl.scannedDataStream.listen((scanData) {
      widget.controller.onCodeRecognized(scanData.code!);
    });
    runScanner();
  }

  @override
  Widget build(BuildContext context) {
    return QRView(
      key: qrKey,
      onQRViewCreated: _onQRViewCreated,
    );
  }
}
