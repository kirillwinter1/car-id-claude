package ru.car.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
//import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
//import org.apache.batik.anim.dom.SVGOMSVGElement;
//import org.apache.batik.dom.GenericDOMImplementation;
//import org.apache.batik.svggen.SVGGraphics2D;
//import org.apache.batik.util.XMLResourceDescriptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
//import org.w3c.dom.*;
//import org.w3c.dom.svg.SVGDocument;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;

public class QrUtils {
    private final static Random random = new Random();
    private final static int QR_LENGTH = 370;
    private final static int ORANGE_COLOR = 0xFFCF6244;
    private final static int WHITE_COLOR = MatrixToImageConfig.WHITE;
    private final static int BLACK_COLOR = MatrixToImageConfig.BLACK;
    private final static List<QrOptions> opt = List.of(
            new QrOptions("png/sticker_1.png", "png/logo_1_2_3_6_8_11.png", 100, QR_LENGTH, BLACK_COLOR),
            new QrOptions("png/sticker_2.png", "png/logo_1_2_3_6_8_11.png", 85, 330, WHITE_COLOR),
            new QrOptions("png/sticker_3.png", "png/logo_1_2_3_6_8_11.png", 75, QR_LENGTH, BLACK_COLOR),
            new QrOptions("png/sticker_4.png", "png/logo_5.png", 100, QR_LENGTH, ORANGE_COLOR),
            new QrOptions("png/sticker_5.png", "png/logo_5.png", 85, 330, WHITE_COLOR),
            new QrOptions("png/sticker_6.png", "png/logo_1_2_3_6_8_11.png", 75, QR_LENGTH, BLACK_COLOR),

            new QrOptions("png/sticker_7.png", "png/logo_7.png", 75, 350, BLACK_COLOR),
            new QrOptions("png/sticker_8.png", "png/logo_1_2_3_6_8_11.png", 45, 350, BLACK_COLOR),
            new QrOptions("png/sticker_9.png", "png/logo_9.png", 80, 280, WHITE_COLOR),
            new QrOptions("png/sticker_10.png", "png/logo_10.png", 75, 350, ORANGE_COLOR),
            new QrOptions("png/sticker_11.png", "png/logo_1_2_3_6_8_11.png", 45, 350, BLACK_COLOR),
            new QrOptions("png/sticker_12.png", "png/logo_12.png", 80, 280, WHITE_COLOR)
    );

    @SneakyThrows
    public static Resource generateQRCodeImage(String barcodeText) {
        QRCodeWriter barcodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix =
                barcodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, QR_LENGTH, QR_LENGTH);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);
        ByteArrayOutputStream output = new ByteArrayOutputStream() {
            @Override
            public synchronized byte[] toByteArray() {
                return this.buf;
            }
        };


        URL url = QrUtils.class.getClassLoader().getResource("png/car-id.png");

        BufferedImage logo = ImageIO.read(url);
        double scale = 0.25;
        logo = getScaledImage( logo,
                (int)( logo.getWidth() * scale),
                (int)( logo.getHeight() * scale) );

        image.createGraphics();
        Graphics2D graphics = (Graphics2D) image.getGraphics();

        graphics.drawImage( logo,
                image.getWidth()/2 - logo.getWidth()/2,
                image.getHeight()/2 - logo.getHeight()/2,
                image.getWidth()/2 + logo.getWidth()/2,
                image.getHeight()/2 + logo.getHeight()/2,
                0, 0, logo.getWidth(), logo.getHeight(), null);


        ImageIO.write(image, "png", output);
        return new ByteArrayResource(output.toByteArray());
    }

    public static String getQrUrlById(String home, UUID id) {
        return home + "/qr/" + id;
    }

    @SneakyThrows
    private static BufferedImage getScaledImage(BufferedImage image, int width, int height) {
        int imageWidth  = image.getWidth();
        int imageHeight = image.getHeight();

        double scaleX = (double)width/imageWidth;
        double scaleY = (double)height/imageHeight;
        AffineTransform scaleTransform = AffineTransform.getScaleInstance(scaleX, scaleY);
        AffineTransformOp bilinearScaleOp = new AffineTransformOp( scaleTransform, AffineTransformOp.TYPE_BILINEAR);

        return bilinearScaleOp.filter( image, new BufferedImage(width, height, image.getType()));
    }


    public static BufferedImage readFile(String path) throws IOException {
        URL url = QrUtils.class.getClassLoader().getResource(path);
        return ImageIO.read(url);
    }

    public static BufferedImage generateQRCodeImage(String barcodeText, int length, int color) throws WriterException {
        BitMatrix bitMatrix = generateQRCodeBits(barcodeText, length);
        MatrixToImageConfig config = new MatrixToImageConfig(color, 0x00ffffff);
        return MatrixToImageWriter.toBufferedImage(bitMatrix, config);
    }

    public static BitMatrix generateQRCodeBits(String barcodeText, int length) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        return qrCodeWriter.encode(barcodeText, BarcodeFormat.QR_CODE, length, length, hints);
    }

    public static void setPicture(BufferedImage image, BufferedImage logo, int height) {
        image.createGraphics();
        Graphics2D graphics = (Graphics2D) image.getGraphics();

        graphics.drawImage( logo,
                image.getWidth()/2 - logo.getWidth()/2,
                image.getHeight()/2 - logo.getHeight()/2 - height,
                image.getWidth()/2 + logo.getWidth()/2,
                image.getHeight()/2 + logo.getHeight()/2 - height,
                0, 0, logo.getWidth(), logo.getHeight(), null);
    }

    public static BufferedImage createQrCode(String text, QrOptions opt) throws IOException, WriterException {
        BufferedImage image = readFile(opt.getSticker());

        BufferedImage qrCode = generateQRCodeImage(text, opt.getQrSize(), opt.getQrColor());
        setPicture(image, qrCode, opt.getYShift());

        BufferedImage logo = readFile(opt.getLogo());
        setPicture(image, logo, opt.getYShift());
        return image;
    }

    @SneakyThrows
    public static InputStream createQrCode(String text) {
        BufferedImage image =  createQrCode(text, opt.get(random.nextInt(opt.size())));

        ByteArrayOutputStream output = new ByteArrayOutputStream() {
            @Override
            public synchronized byte[] toByteArray() {
                return this.buf;
            }
        };
        ImageIO.write(image, "png", output);
        return new ByteArrayInputStream(output.toByteArray());
    }

    public static void main(String[] args) throws IOException, WriterException {
//        byte[] data = generateQRCodeImage("http://cat-id/qr/" + UUID.randomUUID()).getContentAsByteArray();
//        Files.write(new File("test.png").toPath(), data);


//
//        BufferedImage image = readFile("png/sticker_1.png");
//
//        BufferedImage qrCode = generateQRCode("http://car-id.ru/qr/" + UUID.randomUUID(), QR_LENGTH, MatrixToImageConfig.BLACK);
//        setPicture(image, qrCode, 100);
//
//        BufferedImage logo = readFile("png/logo_1_2_3_6_8_11.png");
//        setPicture(image, logo, 100);

//        for (int i = 1; i < opt.size(); i++) {
//            BufferedImage img = createQrCode("http://car-id.ru/qr/" + UUID.randomUUID(), opt.get(i));
//
//            ImageIO.write(img, "png", new File("test" + i + ".png"));
//        }



//        SVGGraphics2D qr = generateQRCodeSvg("http://car-id/8-8008080-8-080", 100, BLACK_COLOR);
//
//        File file1 = new File("/Users/work/IdeaProjects/carId/src/main/resources/svg/stickers_10.svg");
//        SVGGraphics2D sticker = loadSVGDocument(file1.toURI().toString());
//
//        Element g = (Element)sticker.getRoot().getChildNodes().item(2);
//        NodeList list = qr.getRoot().getChildNodes();
//        for (int i = 0; i < list.getLength(); i++) {
//
//            if (Node.ELEMENT_NODE == list.item(i).getNodeType()) {
//                g.appendChild(list.item(i));
//            }
//        }
//        sticker.d( qr,
//                sticker.getSVGCanvasSize().width/2 - qr.getSVGCanvasSize().width/2,
//                sticker.getSVGCanvasSize().height/2 - qr.getSVGCanvasSize().height/2 - 50,
//                sticker.getSVGCanvasSize().width/2 + qr.getSVGCanvasSize().width/2,
//                sticker.getSVGCanvasSize().height/2 + qr.getSVGCanvasSize().height/2 - 50,
//                0, 0, qr.getSVGCanvasSize().width, qr.getSVGCanvasSize().height, null);
//
//        boolean useCSS = true;
//        File file = new File("test.svg");
//        OutputStream outputStream = new FileOutputStream(file);
//        Writer out = new OutputStreamWriter(outputStream, "UTF-8");
//        sticker.stream(out, useCSS);

    }

//    private static SVGGraphics2D generateQRCodeSvg(String barcodeText, int length, int color) throws WriterException {
//        BitMatrix bitMatrix = generateQRCodeBits(barcodeText, length);
//
//        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
//        String svgNS = "http://www.w3.org/2000/svg";
//        Document document = domImpl.createDocument(svgNS, "svg", null);
//
//        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
//        svgGenerator.setSVGCanvasSize(getSize(bitMatrix));
//
//        paintBits(svgGenerator, bitMatrix, color);
//        return svgGenerator;
//    }
//
//    private static Dimension getSize(BitMatrix bitMatrix) {
//        int[] size = bitMatrix.getEnclosingRectangle();
//        return new Dimension(size[2] + 1, size[3] + 1);
//    }
//
//    private static SVGGraphics2D loadSVGDocument(String uri) throws IOException {
//        String parser = XMLResourceDescriptor.getXMLParserClassName();
//        SAXSVGDocumentFactory factory = new SAXSVGDocumentFactory(parser);
//        SVGDocument svgDocument = factory.createSVGDocument(uri);
//
//
//
//        Element elSVG = svgDocument.getRootElement();
//        float width = ((SVGOMSVGElement) elSVG).getViewBox().getBaseVal().getWidth();
//        float height = ((SVGOMSVGElement) elSVG).getViewBox().getBaseVal().getHeight();
//
//        NodeList nodes = elSVG.getElementsByTagName("g");
//        Node node = nodes.item(0);
//        Element el = null;
//        if(Node.ELEMENT_NODE == node.getNodeType()) {
//            el = (Element)node;
//        }
//
//        SVGGraphics2D g = new SVGGraphics2D(svgDocument);
////        Element root = svgDocument.getRootElement();
////        g.setTopLevelGroup(root);
//        g.setTopLevelGroup(el);
//        g.setSVGCanvasSize(new Dimension((int) width, (int) height));
////        g.getRoot(root);
//        return g;
//    }
//
//    private static void paintBits(Graphics2D g2d, BitMatrix bitMatrix, int color) {
//        int[] shape = bitMatrix.getEnclosingRectangle();
//
//        g2d.setPaint(new Color(0x00ffffff));
//        g2d.fill(new Rectangle(0, 0, shape[2] + 1, shape[3] + 1));
//
//        g2d.setPaint(new Color(color));
//        for (int i = 0; i < shape[3]; i++) {
//            int j = 0;
//            while (j < shape[2]) {
//                while (j < shape[2] && !bitMatrix.get(i + shape[0], j + shape[1])) {
//                    j++;
//                }
//
//                int y = i + 1;
//                int x = j + 1;
//                int length = 0;
//                while (j < shape[2] && bitMatrix.get(i + shape[0], j + shape[1])) {
//                    j++;
//                    length++;
//                }
//                if (length > 0) {
//                    g2d.drawLine(x, y, j, y);
//                }
//            }
//        }
//    }

    @Getter
    @AllArgsConstructor
    static class QrOptions {
        private String sticker;
        private String logo;
        private int yShift;
        private int qrSize;
        private int qrColor;
    }
}

