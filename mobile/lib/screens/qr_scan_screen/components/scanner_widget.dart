import 'package:app/controllers/qr_scan_controller.dart';
import 'package:flutter/cupertino.dart';
import 'package:qr_code_scanner/qr_code_scanner.dart';

class ScannerWidget extends StatefulWidget {
  final QRScanController controller;

  const ScannerWidget(this.controller, {Key? key}) : super(key: key);

  @override
  State<ScannerWidget> createState() => _ScannerWidgetState();
}

class _ScannerWidgetState extends State<ScannerWidget> {
  final GlobalKey qrKey = GlobalKey(debugLabel: 'QR');
  // QRViewController? scannerController;

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

    widget.controller.showOverlay.listen((p0) {
      if (p0) {
        widget.controller.scannerController?.pauseCamera();
      } else {
        widget.controller.scannerController?.resumeCamera();
      }
    });
  }

  @override
  void dispose() {
    widget.controller.scannerController?.dispose();
    widget.controller.scannerController = null;
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
