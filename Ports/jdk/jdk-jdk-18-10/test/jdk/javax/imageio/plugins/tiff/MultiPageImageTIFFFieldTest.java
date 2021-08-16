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
 * @bug     8152183 8148454
 * @author  a.stepanov
 * @summary check that TIFFields are derived properly for multi-page tiff
 * @run     main MultiPageImageTIFFFieldTest
 */

import java.awt.*;
import java.awt.color.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import javax.imageio.plugins.tiff.*;


public class MultiPageImageTIFFFieldTest {

    private final static String FILENAME = "test.tiff";
    private final static int W1 = 20, H1 = 40, W2 = 100, H2 = 15;
    private final static Color C1 = Color.BLACK, C2 = Color.RED;

    private final static int N_WIDTH  = BaselineTIFFTagSet.TAG_IMAGE_WIDTH;
    private final static int N_HEIGHT = BaselineTIFFTagSet.TAG_IMAGE_LENGTH;

    private static final String DESCRIPTION_1[] = {"Description-1", "abc ABC"};
    private static final String DESCRIPTION_2[] = {"Description-2", "1-2-3"};
    private final static int N_DESCRIPTION =
        BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION;

    private final static String EXIF_DATA_1[] = {"2001:01:01 00:00:01"};
    private final static String EXIF_DATA_2[] = {"2002:02:02 00:00:02"};
    private final static int N_EXIF = ExifTIFFTagSet.TAG_DATE_TIME_ORIGINAL;

    private final static String GPS_DATA[] = {
        ExifGPSTagSet.STATUS_MEASUREMENT_IN_PROGRESS};
    private final static int N_GPS = ExifGPSTagSet.TAG_GPS_STATUS;

    private final static short FAX_DATA =
        FaxTIFFTagSet.CLEAN_FAX_DATA_ERRORS_UNCORRECTED;
    private final static int N_FAX = FaxTIFFTagSet.TAG_CLEAN_FAX_DATA;

    private static final byte[] ICC_PROFILE_2 =
        ICC_ProfileRGB.getInstance(ColorSpace.CS_sRGB).getData();
    private static final int N_ICC = BaselineTIFFTagSet.TAG_ICC_PROFILE;

    private static final int N_BPS = BaselineTIFFTagSet.TAG_BITS_PER_SAMPLE;

    private static final int
        COMPRESSION_1 = BaselineTIFFTagSet.COMPRESSION_DEFLATE,
        COMPRESSION_2 = BaselineTIFFTagSet.COMPRESSION_LZW;
    private static final int N_COMPRESSION = BaselineTIFFTagSet.TAG_COMPRESSION;

    private static final int
        GRAY_1 = BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO,
        GRAY_2 = BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO,
        RGB    = BaselineTIFFTagSet.PHOTOMETRIC_INTERPRETATION_RGB;

    private static final int N_PHOTO =
        BaselineTIFFTagSet.TAG_PHOTOMETRIC_INTERPRETATION;

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

            BufferedImage img1 =
                new BufferedImage(W1, H1, BufferedImage.TYPE_BYTE_GRAY);
            Graphics g = img1.getGraphics();
            g.setColor(C1);
            g.fillRect(0, 0, W1, H1);
            g.dispose();

            BufferedImage img2 =
                new BufferedImage(W2, H2, BufferedImage.TYPE_INT_RGB);
            g = img2.getGraphics();
            g.setColor(C2);
            g.fillRect(0, 0, W2, H2);
            g.dispose();

            ImageWriteParam param1 = writer.getDefaultWriteParam();
            param1.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param1.setCompressionType("Deflate");
            param1.setCompressionQuality(0.5f);

            ImageWriteParam param2 = writer.getDefaultWriteParam();
            param2.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param2.setCompressionType("LZW");
            param2.setCompressionQuality(0.5f);

            IIOMetadata
                md1 = writer.getDefaultImageMetadata(
                    new ImageTypeSpecifier(img1), param1),
                md2 = writer.getDefaultImageMetadata(
                    new ImageTypeSpecifier(img2), param2);

            TIFFDirectory
                dir1 = TIFFDirectory.createFromMetadata(md1),
                dir2 = TIFFDirectory.createFromMetadata(md2);

            addASCIIField(dir1, "ImageDescription", DESCRIPTION_1, N_DESCRIPTION);
            addASCIIField(dir2, "ImageDescription", DESCRIPTION_2, N_DESCRIPTION);

            addASCIIField(dir1, "GPSStatus", GPS_DATA, N_GPS);
            addASCIIField(dir2, "GPSStatus", GPS_DATA, N_GPS);

            addASCIIField(dir1, "DateTimeOriginal", EXIF_DATA_1, N_EXIF);
            addASCIIField(dir2, "DateTimeOriginal", EXIF_DATA_2, N_EXIF);

            TIFFTag faxTag = new TIFFTag(
                "CleanFaxData", N_FAX, 1 << TIFFTag.TIFF_SHORT);
            dir1.addTIFFField(new TIFFField(faxTag, FAX_DATA));
            dir2.addTIFFField(new TIFFField(faxTag, FAX_DATA));

            dir2.addTIFFField(new TIFFField(
                new TIFFTag("ICC Profile", N_ICC, 1 << TIFFTag.TIFF_UNDEFINED),
                TIFFTag.TIFF_UNDEFINED, ICC_PROFILE_2.length, ICC_PROFILE_2));

            writer.prepareWriteSequence(null);
            writer.writeToSequence(
                new IIOImage(img1, null, dir1.getAsMetadata()), param1);
            writer.writeToSequence(
                new IIOImage(img2, null, dir2.getAsMetadata()), param2);
            writer.endWriteSequence();

            ios.flush();
            writer.dispose();
        }
        s.close();
    }

    private void checkBufferedImages(BufferedImage im1, BufferedImage im2) {

        check(im1.getWidth()  == W1, "invalid width for image 1");
        check(im1.getHeight() == H1, "invalid height for image 1");
        check(im2.getWidth()  == W2, "invalid width for image 2");
        check(im2.getHeight() == H2, "invalid height for image 2");

        Color
            c1 = new Color(im1.getRGB(W1 / 2, H1 / 2)),
            c2 = new Color(im2.getRGB(W2 / 2, H2 / 2));

        check(c1.equals(C1), "invalid image 1 color");
        check(c2.equals(C2), "invalid image 2 color");
    }

    private void readAndCheckImage() throws Exception {

        ImageReader reader = getTIFFReader();

        ImageInputStream s = ImageIO.createImageInputStream(new File(FILENAME));
        reader.setInput(s, false, false);

        int ni = reader.getNumImages(true);
        check(ni == 2, "invalid number of images");

        // check TIFFImageReadParam for multipage image
        TIFFImageReadParam
            param1 = new TIFFImageReadParam(), param2 = new TIFFImageReadParam();

        param1.addAllowedTagSet(ExifTIFFTagSet.getInstance());
        param1.addAllowedTagSet(ExifGPSTagSet.getInstance());

        param2.addAllowedTagSet(ExifTIFFTagSet.getInstance());
        param2.addAllowedTagSet(GeoTIFFTagSet.getInstance());

        // FaxTIFFTagSet is allowed by default
        param2.removeAllowedTagSet(FaxTIFFTagSet.getInstance());


        // read images and metadata
        IIOImage i1 = reader.readAll(0, param1), i2 = reader.readAll(1, param2);
        BufferedImage
            bi1 = (BufferedImage) i1.getRenderedImage(),
            bi2 = (BufferedImage) i2.getRenderedImage();

        // check rendered images, just in case
        checkBufferedImages(bi1, bi2);

        TIFFDirectory
            dir1 = TIFFDirectory.createFromMetadata(i1.getMetadata()),
            dir2 = TIFFDirectory.createFromMetadata(i2.getMetadata());

        // check ASCII fields
        checkASCIIField(
            dir1, "image 1 description", DESCRIPTION_1, N_DESCRIPTION);
        checkASCIIField(
            dir2, "image 2 description", DESCRIPTION_2, N_DESCRIPTION);

        checkASCIIField(dir1, "image 1 datetime", EXIF_DATA_1, N_EXIF);
        checkASCIIField(dir2, "image 2 datetime", EXIF_DATA_2, N_EXIF);

        // check sizes
        TIFFField f = dir1.getTIFFField(N_WIDTH);
        check((f.getCount() == 1) && (f.getAsInt(0) == W1),
            "invalid width field for image 1");
        f = dir2.getTIFFField(N_WIDTH);
        check((f.getCount() == 1) && (f.getAsInt(0) == W2),
            "invalid width field for image 2");

        f = dir1.getTIFFField(N_HEIGHT);
        check((f.getCount() == 1) && (f.getAsInt(0) == H1),
            "invalid height field for image 1");
        f = dir2.getTIFFField(N_HEIGHT);
        check((f.getCount() == 1) && (f.getAsInt(0) == H2),
            "invalid height field for image 2");

        // check fax data
        check(dir1.containsTIFFField(N_FAX), "image 2 TIFF directory " +
            "must contain clean fax data");
        f = dir1.getTIFFField(N_FAX);
        check(
            (f.getCount() == 1) && f.isIntegral() && (f.getAsInt(0) == FAX_DATA),
            "invalid clean fax data");

        check(!dir2.containsTIFFField(N_FAX), "image 2 TIFF directory " +
            "must not contain fax fields");

        // check GPS data
        checkASCIIField(dir1, "GPS status", GPS_DATA, N_GPS);

        check(!dir2.containsTIFFField(N_GPS), "image 2 TIFF directory " +
            "must not contain GPS fields");

        // check ICC profile data
        check(!dir1.containsTIFFField(N_ICC), "image 1 TIFF directory "
            + "must not contain ICC Profile field");
        check(dir2.containsTIFFField(N_ICC), "image 2 TIFF directory "
            + "must contain ICC Profile field");

        f = dir2.getTIFFField(N_ICC);
        check(f.getType() == TIFFTag.TIFF_UNDEFINED,
            "invalid ICC profile field type");
        int cnt = f.getCount();
        byte icc[] = f.getAsBytes();
        check((cnt == ICC_PROFILE_2.length) && (cnt == icc.length),
                "invalid ICC profile");
        for (int i = 0; i < cnt; i++) {
            check(icc[i] == ICC_PROFILE_2[i], "invalid ICC profile data");
        }

        // check component sizes
        check(dir1.getTIFFField(N_BPS).isIntegral() &&
              dir2.getTIFFField(N_BPS).isIntegral(),
              "invalid bits per sample type");
        int sz1[] = bi1.getColorModel().getComponentSize(),
            sz2[] = bi2.getColorModel().getComponentSize(),
            bps1[] = dir1.getTIFFField(N_BPS).getAsInts(),
            bps2[] = dir2.getTIFFField(N_BPS).getAsInts();

        check((bps1.length == sz1.length) && (bps2.length == sz2.length),
            "invalid component size count");

        for (int i = 0; i < bps1.length; i++) {
            check(bps1[i] == sz1[i], "image 1: invalid bits per sample data");
        }

        for (int i = 0; i < bps2.length; i++) {
            check(bps2[i] == sz2[i], "image 2: invalid bits per sample data");
        }

        // check compression data
        check(dir1.containsTIFFField(N_COMPRESSION) &&
              dir2.containsTIFFField(N_COMPRESSION),
              "compression info lost");
        f = dir1.getTIFFField(N_COMPRESSION);
        check(f.isIntegral() && (f.getCount() == 1) &&
            (f.getAsInt(0) == COMPRESSION_1), "invalid image 1 compression data");

        f = dir2.getTIFFField(N_COMPRESSION);
        check(f.isIntegral() && (f.getCount() == 1) &&
            (f.getAsInt(0) == COMPRESSION_2), "invalid image 2 compression data");

        // check photometric interpretation
        f = dir1.getTIFFField(N_PHOTO);
        check(f.isIntegral() && (f.getCount() == 1) &&
            ((f.getAsInt(0) == GRAY_1) || (f.getAsInt(0) == GRAY_2)),
            "invalid photometric interpretation for image 1");

        f = dir2.getTIFFField(N_PHOTO);
        check(f.isIntegral() && (f.getCount() == 1) && (f.getAsInt(0) == RGB),
            "invalid photometric interpretation for image 2");
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
        (new MultiPageImageTIFFFieldTest()).run();
    }
}
