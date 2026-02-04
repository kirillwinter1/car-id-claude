package ru.car.util;

import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import ru.car.enums.BatchTemplates;
import ru.car.util.svg.Matrix;
import ru.car.util.svg.PdfConverter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SvgUtils {

    private static final Random RANDOM = new Random();

    private static final Map<BatchTemplates, List<String>> PATH_BY_TYPE = new HashMap<>();

    static {
        Arrays.stream(BatchTemplates.values()).forEach(templates -> {
            try {
                PATH_BY_TYPE.put(templates, readSvgFileLines(templates.getPath()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void writeRectWithRadius(StringBuilder sb, int x1, int y1, int x2, int y2, boolean tlr, boolean trr, boolean brr, boolean blr) {

        double r = 0.5;

        sb.append("<path d=\"M");

        //start point
        if (tlr) {
            sb.append(x1 + r).append(" ").append(y1);
        } else {
            sb.append(x1).append(" ").append(y1);
        }

        //horizontal line
        if (trr) {
            sb.append("L").append(x2 - r).append(" ").append(y1);
        } else {
            sb.append("L").append(x2).append(" ").append(y1);
        }

        if (trr && brr) {
            sb.append("A0.5 0.5 0 0 1 ").append(x2 - r).append(" ").append(y2);
        } else if (trr) {
            sb.append("A0.5 0.5 0 0 1 ").append(x2).append(" ").append(y1 + r);
            //vertical line
            sb.append("L").append(x2).append(" ").append(y2);
        } else if (brr) {
            //vertical line
            sb.append("L").append(x2).append(" ").append(y2 - r);
            sb.append("A0.5 0.5 0 0 1 ").append(x2 - r).append(" ").append(y2);
        } else {
            sb.append("L").append(x2).append(" ").append(y2);
        }

        //horizontal line
        if (blr) {
            sb.append("L").append(x1 + r).append(" ").append(y2);
        } else {
            sb.append("L").append(x1).append(" ").append(y2);
        }

        if (blr && tlr) {
            sb.append("A0.5 0.5 0 0 1 ").append(x1 + r).append(" ").append(y1);
        } else if (blr) {
            sb.append("A0.5 0.5 0 0 1 ").append(x1).append(" ").append(y2 - r);
            //vertical line
            sb.append("L").append(x1).append(" ").append(y1);
        } else if (tlr) {
            //vertical line
            sb.append("L").append(x1).append(" ").append(y1 + r);
            sb.append("A0.5 0.5 0 0 1 ").append(x1 + r).append(" ").append(y1);
        } else {
            sb.append("L").append(x1).append(" ").append(y1);
        }
        sb.append("Z\"/>");
    };

    private static void writeLine(StringBuilder sb, int x1, int y1, int x2, int y2, boolean r) {
        sb.append("<rect x=\"").append(x1).append("\"")
                .append(" y=\"").append(y1).append("\"")
                .append(" width=\"").append(x2 - x1).append("\"")
                .append(" height=\"").append(1).append("\"");
        if (r) {
            sb.append(" rx=\"0.5\" ry=\"0.5\" ");
        }
        sb
                .append("/>");
    }

    private static void writeRect(StringBuilder sb, int x1, int y1, int x2, int y2, boolean tlr, boolean trr, boolean brr, boolean blr) {
        double r = 0.5;
        if (tlr && trr && brr && blr) {
            writeLine(sb, x1, y1, x2, y2, true);
        } else if (tlr || trr || brr || blr) {
            writeRectWithRadius(sb, x1, y1, x2, y2, tlr, trr, brr, blr);
        } else {
            writeLine(sb, x1, y1, x2, y2, false);
        }
    }



    private static String generateXmlLines(BitMatrix bitMatrix) {
        StringBuilder sb = new StringBuilder();
        int[] shape = bitMatrix.getEnclosingRectangle();

        for (int i = 0; i < shape[3]; i++) {
            int j = 0;
            while (j < shape[2]) {
                //skip
                while (j < shape[2] && !bitMatrix.get(i + shape[0], j + shape[1])) {
                    j++;
                }

                int y = i;
                int x = j;
                int length = 0;
                while (j < shape[2] && bitMatrix.get(i + shape[0], j + shape[1])) {
                    j++;
                    length++;
                }
                if (length > 0) {
                    writeRect(sb, x, y, j, y + 1,
                            !bitMatrix.get(y + shape[0] - 1, x + shape[1]),
                            !bitMatrix.get(y + shape[0] - 1, j + shape[1] - 1),
                            !bitMatrix.get(y + shape[0] + 1, j + shape[1] - 1),
                            !bitMatrix.get(y + shape[0] + 1, x + shape[1]));
                }
            }
        }
        return sb.toString();
    }

    public static String readSvgFile(String path) throws IOException, URISyntaxException {
        URL url = SvgUtils.class.getClassLoader().getResource(path);
        return Files.readString(Paths.get(url.toURI()));
    }

    public static List<String> readSvgFileLines(String path) throws IOException, URISyntaxException {
        URL url = SvgUtils.class.getClassLoader().getResource(path);
        return Files.readAllLines(Paths.get(url.toURI()));
    }

    public static void main4(String[] args) throws Exception {
        String dir = "stickers11";
        new File("./" + dir).mkdir();
//        new File("./" + dir + "/white").mkdir();
//        new File("./" + dir + "/black").mkdir();

        for (int j = 1; j <= 1; j++) {
            new File("./" + dir + "/white_" + j).mkdir();
            new File("./" + dir + "/black_" + j).mkdir();

            for (int i = 0; i < 10; i++) {
                String s = UUID.randomUUID().toString();
                create2(s, BatchTemplates.PT_WHITE_1, dir + "/white_" + j + "/" + s);
                System.out.printf("\"%s\",\"%d\"\n", s, j + 1);
            }
            for (int i = 0; i < 10; i++) {
                String s = UUID.randomUUID().toString();
                create2(s, BatchTemplates.PT_BLACK_1, dir + "/black_" + j + "/" + s);
                System.out.printf("\"%s\",\"%d\"\n", s, j + 1);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String dir = "stickers13";
        new File("./" + dir).mkdir();

        List<Triple<BatchTemplates, Integer, Integer>> templates = new ArrayList<>();
        templates.add(Triple.of(BatchTemplates.PT_WHITE_1, 1000, 22));
        templates.add(Triple.of(BatchTemplates.PT_BLACK_1, 2000, 23));

        FileWriter fileWriter = new FileWriter(new File("./" + dir + "/names.txt"));

        for (int j = 0; j < templates.size(); j++) {
            BatchTemplates template = templates.get(j).getLeft();
            String folder = j + "_" + template.getFolder();
            new File("./" + dir + "/" + folder).mkdir();

            int count = templates.get(j).getMiddle();
            int batchNumber = templates.get(j).getRight();
            for (int i = 0; i < count; i++) {
                String s = UUID.randomUUID().toString();
                create2(s, template, dir + "/" + folder + "/" + s);
                String format = String.format("\"%s\",\"%d\",\"%s\"\n", s, batchNumber, template.name());
                fileWriter.append(format);
            }
        }
    }

    public static void main2(String[] args) throws WriterException {
        BitMatrix bitMatrix = QrUtils.generateQRCodeBits("https://car-id.ru", 50);
        Matrix matrix = new Matrix(bitMatrix);
        String svgQrCoordinates =  matrix.toXml(" class=\"cls-back\" ");
        System.out.println(svgQrCoordinates);
    }

    public static void main3(String[] args) throws Exception {
        byte[] bytes = PdfConverter.create(SvgUtils.getSvgText("09d73df3-01e0-4ade-9796-b43945e621b8", BatchTemplates.PT_BLACK_1));
        File file = new File("09d73df3-01e0-4ade-9796-b43945e621b8_black.pdf");
        Files.write(file.toPath(), bytes);
    }

    public static void create(String uuid, String svgFileName, String fileName) throws Exception {
        String svgText = readSvgFile(svgFileName);

        BitMatrix bitMatrix = QrUtils.generateQRCodeBits("https://car-id.ru/qr/" + uuid , 50);
        String svgQrCoordinates = generateXmlLines(bitMatrix);
        svgText = svgText.replace("<!--qr-->", svgQrCoordinates);

        Files.writeString(new File("./"+ fileName + ".svg").toPath(), svgText);
        System.out.println(uuid);
    }

    public static List<String> getSvgText(String uuid, BatchTemplates template) throws Exception {
        BitMatrix bitMatrix = QrUtils.generateQRCodeBits("https://car-id.ru/qr/" + uuid , 50);
        Matrix matrix = new Matrix(bitMatrix);
        String svgQrCoordinates =  matrix.toXml(" class=\"cls-back\" ");

        List<String> svgText = new ArrayList<>(PATH_BY_TYPE.get(template));
        int i = svgText.indexOf("    <!--qr-->");
        svgText.set(i, svgQrCoordinates);
        return svgText;
    }

    public static void create2(String uuid, BatchTemplates template, String fileName) throws Exception {
        List<String> svgText = getSvgText(uuid, template);
        Files.write(new File("./"+ fileName + ".svg").toPath(), svgText);
    }

//    public static void create3(String svgFileName, String fileName) throws Exception {
//        String svgText = readSvgFile(svgFileName);
////            String svgText = readSvgFile("svg/stickers_" + (i + 1) + "_2.svg");
//
//
//        BitMatrix bitMatrix = QrUtils.generateQRCodeBits("https://apps.apple.com/ru/app/id6499054732" , 50);
//        Matrix matrix = new Matrix(bitMatrix);
//        String svgQrCoordinates =  matrix.toXml(" class=\"cls-back\" ");
//        svgText = svgText.replace("<!--qr-->", svgQrCoordinates);
//
//        Files.writeString(new File("./"+ fileName + ".svg").toPath(), svgText);
//        System.out.println("https://car-id.ru");
//    }
}
