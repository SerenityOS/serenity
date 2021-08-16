/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * @test
 * @bug     8149028
 * @author  a.stepanov
 * @summary a simple write-read test for TIFFDirectory
 * @run     main TIFFDirectoryWriteReadTest
 */

import java.awt.*;
import java.awt.color.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import javax.imageio.plugins.tiff.*;


public class TIFFDirectoryWriteReadTest {

    private final static String FILENAME = "test.tiff";
    private final static int SZ = 100;
    private final static Color C = Color.RED;

    private static final String COPYRIGHT[] = {"Copyright 123ABC.."};
    private static final String DESCRIPTION[] = {"Test Image", "Description"};
    private static final String SOFTWARE[] = {"test", "software", "123"};

    private static final long RES_X[][] = {{2, 1}}, RES_Y[][] = {{1, 1}};

    private static final byte[] ICC_PROFILE =
        ICC_ProfileRGB.getInstance(ColorSpace.CS_sRGB).getData();


    private ImageWriter getTIFFWriter() {

        java.util.Iterator<ImageWriter> writers =
            ImageIO.getImageWritersByFormatName("TIFF");
        if (!writers.hasNext()) {
            throw new RuntimeException("No writers available for TIFF format");
        }
        return writers.next();
    }

    private ImageReader getTIFFReader() {

        java.util.Iterator<ImageReader> readers =
            ImageIO.getImageReadersByFormatName("TIFF");
        if (!readers.hasNext()) {
            throw new RuntimeException("No readers available for TIFF format");
        }
        return readers.next();
    }

    private void addASCIIField(TIFFDirectory d,
                               String        name,
                               String        data[],
                               int           num) {

        d.addTIFFField(new TIFFField(
            new TIFFTag(name, num, 1 << TIFFTag.TIFF_ASCII),
                TIFFTag.TIFF_ASCII, data.length, data));
    }

    private void checkASCIIField(TIFFDirectory d,
                                 String        what,
                                 String        data[],
                                 int           num) {

        String notFound = what + " field was not found";
        check(d.containsTIFFField(num), notFound);
        TIFFField f = d.getTIFFField(num);
        check(f.getType() == TIFFTag.TIFF_ASCII, "field type != ASCII");
        check(f.getCount() == data.length, "invalid " + what + " data count");
        for (int i = 0; i < data.length; i++) {
            check(f.getValueAsString(i).equals(data[i]),
                "invalid " + what + " data");
        }
    }

    private void writeImage() throws Exception {

        OutputStream s = new BufferedOutputStream(new FileOutputStream(FILENAME));
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(s)) {

            ImageWriter writer = getTIFFWriter();
            writer.setOutput(ios);

            BufferedImage img = new BufferedImage(
                SZ, SZ, BufferedImage.TYPE_INT_RGB);
            Graphics g = img.getGraphics();
            g.setColor(C);
            g.fillRect(0, 0, SZ, SZ);
            g.dispose();

            IIOMetadata metadata = writer.getDefaultImageMetadata(
                new ImageTypeSpecifier(img), writer.getDefaultWriteParam());

            TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);

            addASCIIField(dir, "Copyright",
                COPYRIGHT, BaselineTIFFTagSet.TAG_COPYRIGHT);

            addASCIIField(dir, "ImageDescription",
                DESCRIPTION, BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION);

            addASCIIField(dir, "Software",
                SOFTWARE, BaselineTIFFTagSet.TAG_SOFTWARE);

            dir.addTIFFField(new TIFFField(
                new TIFFTag("XResolution", BaselineTIFFTagSet.TAG_X_RESOLUTION,
                1 << TIFFTag.TIFF_RATIONAL), TIFFTag.TIFF_RATIONAL, 1, RES_X));
            dir.addTIFFField(new TIFFField(
                new TIFFTag("YResolution", BaselineTIFFTagSet.TAG_Y_RESOLUTION,
                1 << TIFFTag.TIFF_RATIONAL), TIFFTag.TIFF_RATIONAL, 1, RES_Y));

            dir.addTIFFField(new TIFFField(
            new TIFFTag("ICC Profile", BaselineTIFFTagSet.TAG_ICC_PROFILE,
                1 << TIFFTag.TIFF_UNDEFINED),
                TIFFTag.TIFF_UNDEFINED, ICC_PROFILE.length, ICC_PROFILE));

            IIOMetadata data = dir.getAsMetadata();
            writer.write(new IIOImage(img, null, data));

            ios.flush();
            writer.dispose();
        }
        s.close();
    }



    private void readAndCheckImage() throws Exception {

        ImageReader reader = getTIFFReader();

        ImageInputStream s = ImageIO.createImageInputStream(new File(FILENAME));
        reader.setInput(s);

        int ni = reader.getNumImages(true);
        check(ni == 1, "invalid number of images");

        // check image
        BufferedImage img = reader.read(0);
        check(img.getWidth() == SZ && img.getHeight() == SZ,
            "invalid image size");

        Color c = new Color(img.getRGB(SZ / 2, SZ / 2));
        check(C.equals(c), "invalid image color");

        IIOMetadata metadata = reader.readAll(0, null).getMetadata();
        TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);

        reader.dispose();
        s.close();

        // ===== perform tag checks =====

        checkASCIIField(dir, "copyright", COPYRIGHT,
            BaselineTIFFTagSet.TAG_COPYRIGHT);

        checkASCIIField(dir, "description", DESCRIPTION,
            BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION);

        checkASCIIField(dir, "software", SOFTWARE,
            BaselineTIFFTagSet.TAG_SOFTWARE);

        TIFFField f = dir.getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_WIDTH);
        check(f.getCount() == 1, "invalid width field count");
        int w = f.getAsInt(0);
        check(w == SZ, "invalid width");

        f = dir.getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_LENGTH);
        check(f.getCount() == 1, "invalid height field count");
        int h = f.getAsInt(0);
        check(h == SZ, "invalid height");

        f = dir.getTIFFField(BaselineTIFFTagSet.TAG_BITS_PER_SAMPLE);
        // RGB: 3 x 8 bits for R, G and B components
        int bps[] = f.getAsInts();
        check((f.getCount() == 3) && (bps.length == 3), "invalid BPS count");
        for (int b: bps) { check(b == 8, "invalid bits per sample"); }

        // RGB: PhotometricInterpretation = 2
        f = dir.getTIFFField(BaselineTIFFTagSet.TAG_PHOTOMETRIC_INTERPRETATION);
        check(f.getCount() == 1, "invalid count");
        check(f.getAsInt(0) == BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_RGB,
            "invalid photometric interpretation value");

        String rat = " resolution must be rational";
        f = dir.getTIFFField(BaselineTIFFTagSet.TAG_X_RESOLUTION);
        check(f.getType() == TIFFTag.TIFF_RATIONAL, "x" + rat);
        check(f.getCount() == 1 &&
              f.getAsInt(0) == (int) (RES_X[0][0] / RES_X[0][1]),
              "invalid x resolution");

        f = dir.getTIFFField(BaselineTIFFTagSet.TAG_Y_RESOLUTION);
        check(f.getType() == TIFFTag.TIFF_RATIONAL, "y" + rat);
        check(f.getCount() == 1 &&
              f.getAsInt(0) == (int) (RES_Y[0][0] / RES_Y[0][1]),
              "invalid y resolution");

        f = dir.getTIFFField(BaselineTIFFTagSet.TAG_ICC_PROFILE);
        check(f.getType() == TIFFTag.TIFF_UNDEFINED,
            "invalid ICC profile field type");
        int cnt = f.getCount();
        byte icc[] = f.getAsBytes();
        check((cnt == ICC_PROFILE.length) && (cnt == icc.length),
                "invalid ICC profile");
        for (int i = 0; i < cnt; i++) {
            check(icc[i] == ICC_PROFILE[i], "invalid ICC profile");
        }
    }

    public void run() {

        try {
            writeImage();
            readAndCheckImage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void check(boolean ok, String msg) {
        if (!ok) { throw new RuntimeException(msg); }
    }

    public static void main(String[] args) {
        (new TIFFDirectoryWriteReadTest()).run();
    }
}
