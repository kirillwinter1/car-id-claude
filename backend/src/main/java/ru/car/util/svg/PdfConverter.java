package ru.car.util.svg;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PdfConverter {
    public static void main2(String[] argv) throws Exception {
        String root = "/Users/work/IdeaProjects/carId/stickers11";

        for (int i = 1; i <= 1; i++) {


            new File(root + "/whitePdf_" + i).mkdir();
            new File(root + "/blackPdf_" + i).mkdir();
            Path whitePath = Paths.get(root + "/white_" + i + "/");
            Path blackPath = Paths.get(root + "/black_" + i + "/");
            Path whitePdfPath = Paths.get(root + "/whitePdf_" + i + "/");
            Path blackPdfPath = Paths.get(root + "/blackPdf_" + i + "/");

            for (File file : whitePath.toFile().listFiles()) {
                if (file.getName().endsWith(".svg")) {
                    create(file);
                }
            }
            for (File file : blackPath.toFile().listFiles()) {
                if (file.getName().endsWith(".svg")) {
                    create(file);
                }
            }

            PDFMergerUtility ut = new PDFMergerUtility();
            for (File file : whitePdfPath.toFile().listFiles()) {
                if (file.getName().endsWith(".pdf")) {
                    ut.addSource(file);
                }
            }
            ut.setDestinationFileName(root + "/stickers_white_" + i + ".pdf");
            ut.mergeDocuments(null);

            ut = new PDFMergerUtility();
            for (File file : blackPdfPath.toFile().listFiles()) {
                if (file.getName().endsWith(".pdf")) {
                    ut.addSource(file);
                }
            }
            ut.setDestinationFileName(root + "/stickers_black_" + i + ".pdf");
            ut.mergeDocuments(null);

        }
    }

    public static void main(String[] argv) throws Exception {
        String root = "/Users/work/IdeaProjects/carId/stickers13";

        File rootDir = new File(root);
        File[] directories = rootDir.listFiles();

        for (File dir : directories) {
            if (!dir.isDirectory()) {
                continue;
            }

            File pdfDir = new File(dir.getAbsolutePath() + "Pdf");
            pdfDir.mkdir();

            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".svg")) {
                    File filePdf = new File(file.getAbsolutePath()
                            .replaceAll(dir.getName(), dir.getName() + "Pdf")
                            .replaceAll("\\.svg$", ".pdf"));
                    createTo(file, filePdf);
                }
            }

            PDFMergerUtility ut = new PDFMergerUtility();
            for (File file : pdfDir.listFiles()) {
                if (file.getName().endsWith(".pdf")) {
                    ut.addSource(file);
                }
            }
            ut.setDestinationFileName(root + "/stickers_" + dir.getName() + ".pdf");
            ut.mergeDocuments(null);

        }
    }

    public static File createTo(File fileSvg, File filePdf) throws Exception {
        writePdf(new FileInputStream(fileSvg), new FileOutputStream(filePdf));
        return filePdf;
    }

    public static File create(File fileSvg) throws Exception {
        File filePdf = new File(fileSvg.getAbsolutePath()
                .replaceAll("/white", "/whitePdf")
                .replaceAll("/black", "/blackPdf")
                .replaceAll("\\.svg$", ".pdf"));

        writePdf(new FileInputStream(fileSvg), new FileOutputStream(filePdf));
        return filePdf;
    }

    public static byte[] create(List<String> svg) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream() {
            @Override
            public synchronized byte[] toByteArray() {
                return this.buf;
            }
        };

        List<ByteArrayInputStream> list = svg.stream()
                .map(s -> new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)))
                .collect(Collectors.toList());
        SequenceInputStream is = new SequenceInputStream(Collections.enumeration(list));

        writePdf(is, output);
        return output.toByteArray();
    }

    public static void writePdf(InputStream from, OutputStream to) throws Exception {
        PDFTranscoder transcoder = new PDFTranscoder();
        transcoder.addTranscodingHint(PDFTranscoder.KEY_HEIGHT, (float)(64/0.264583));
        transcoder.addTranscodingHint(PDFTranscoder.KEY_WIDTH, (float)(44/0.264583));
        TranscoderInput transcoderInput = new TranscoderInput(from);
        TranscoderOutput transcoderOutput = new TranscoderOutput(to);
        transcoder.transcode(transcoderInput, transcoderOutput);
    }
}
