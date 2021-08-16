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
 * @bug     8154058
 * @author  a.stepanov
 * @summary Some checks for ignoring metadata
 * @run     main ReadUnknownTagsTest
 */

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;
import javax.imageio.metadata.*;

import javax.imageio.stream.*;
import javax.imageio.plugins.tiff.*;


public class ReadUnknownTagsTest {

    private final static int SZ = 50;
    private final static Color C = Color.RED;

    private final static int DESCRIPTION_TAG =
        BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION;
    private final static String DESCRIPTION = "A Test Image";

    private final static int FAX_TAG = FaxTIFFTagSet.TAG_CLEAN_FAX_DATA;
    private final static short FAX_DATA =
        FaxTIFFTagSet.CLEAN_FAX_DATA_ERRORS_UNCORRECTED;

    private final boolean ignoreMetadata;
    private final boolean readUnknownTags;

    public ReadUnknownTagsTest(boolean ignoreMetadata,
        boolean readUnknownTags) {
        this.ignoreMetadata = ignoreMetadata;
        this.readUnknownTags = readUnknownTags;
    }

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


    private void writeImage() throws Exception {

        String fn = "test-" + ignoreMetadata + ".tiff";
        OutputStream s = new BufferedOutputStream(new FileOutputStream(fn));
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(s)) {

            ImageWriter writer = getTIFFWriter();
            writer.setOutput(ios);

            BufferedImage img = new BufferedImage(SZ, SZ,
                BufferedImage.TYPE_INT_RGB);
            Graphics g = img.getGraphics();
            g.setColor(C);
            g.fillRect(0, 0, SZ, SZ);
            g.dispose();

            ImageWriteParam param = writer.getDefaultWriteParam();

            IIOMetadata md = writer.getDefaultImageMetadata(
                    new ImageTypeSpecifier(img), param);

            TIFFDirectory dir = TIFFDirectory.createFromMetadata(md);

            TIFFTag descTag =
                BaselineTIFFTagSet.getInstance().getTag(DESCRIPTION_TAG);
            dir.addTIFFField(new TIFFField(descTag, TIFFTag.TIFF_ASCII, 1,
                new String[] {DESCRIPTION}));

            TIFFTag faxTag = FaxTIFFTagSet.getInstance().getTag(FAX_TAG);
            dir.addTIFFField(new TIFFField(faxTag, FAX_DATA));

            writer.write(new IIOImage(img, null, dir.getAsMetadata()));

            ios.flush();
            writer.dispose();
        }
        s.close();
    }

    private void readAndCheckImage() throws Exception {

        ImageReader reader = getTIFFReader();

        String fn = "test-" + ignoreMetadata + ".tiff";
        ImageInputStream s = ImageIO.createImageInputStream(new File(fn));

        reader.setInput(s, false, ignoreMetadata);

        int ni = reader.getNumImages(true);
        check(ni == 1, "invalid number of images");


        TIFFImageReadParam param = new TIFFImageReadParam();
        // fax data are allowed by default
        param.removeAllowedTagSet(FaxTIFFTagSet.getInstance());

        // readUnknownTags setting
        if (param.getReadUnknownTags()) {
            throw new RuntimeException("Default readUnknownTags is not false");
        }
        param.setReadUnknownTags(readUnknownTags);
        if (param.getReadUnknownTags() != readUnknownTags) {
            throw new RuntimeException("Incorrect readUnknownTags setting "
                + "\"" + readUnknownTags + "\"");
        }

        // read images and metadata
        IIOImage i = reader.readAll(0, param);
        BufferedImage bi = (BufferedImage) i.getRenderedImage();

        check(bi.getWidth()  == SZ, "invalid width");
        check(bi.getHeight() == SZ, "invalid height");
        Color c = new Color(bi.getRGB(SZ / 2, SZ / 2));
        check(c.equals(C), "invalid color");

        IIOMetadata metadata = i.getMetadata();

        //
        // Verify presence of image metadata
        //
        if (metadata == null) {
            throw new RuntimeException("No image metadata retrieved");
        }

        TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);

        //
        // Verify presence of essential ImageWidth field regardless of
        // settings of ignoreMetadata and readUnknownTags
        //
        int failures = 0;
        if (!dir.containsTIFFField(BaselineTIFFTagSet.TAG_IMAGE_WIDTH)) {
            System.err.println("Metadata is missing essential ImageWidth tag");
            failures++;
        } else {
            TIFFField widthField =
                dir.getTIFFField(BaselineTIFFTagSet.TAG_IMAGE_WIDTH);
            System.out.printf("ImageWidth: %d%n", widthField.getAsLong(0));
        }

        //
        // Verify presence of non-essential baseline ImageDescription field
        // if and only if ignoreMetadata == false
        //
        boolean hasDescription = dir.containsTIFFField(DESCRIPTION_TAG);
        System.out.println("ImageDescription (" + !ignoreMetadata + "): "
            + hasDescription);
        if (ignoreMetadata && hasDescription) {
            System.err.println
                ("Description metadata present despite ignoreMetadata");
            failures++;
        } else if (!ignoreMetadata && !hasDescription) {
            System.err.println
                ("Description metadata absent despite !ignoreMetadata");
            failures++;
        }

        //
        // Verify presence of CleanFaxData field if and only if
        // ignoreMetadata == false and readUnknownTags == true
        //
        boolean shouldHaveFaxField = !ignoreMetadata && readUnknownTags;
        boolean hasFaxField = dir.containsTIFFField(FAX_TAG);
        System.out.println("CleanFaxData (" + shouldHaveFaxField + "): "
            + hasFaxField);

        if (ignoreMetadata) {
            if (hasFaxField) {
                System.err.println
                    ("Fax metadata present despite ignoreMetadata");
                failures++;
            }
        } else { // !ignoreMetadata
            if (!readUnknownTags && hasFaxField) {
                System.err.println
                    ("Fax metadata present despite !readUnknownTags");
                failures++;
            } else if (readUnknownTags && !hasFaxField) {
                System.err.println
                    ("Fax metadata absent despite readUnknownTags");
                failures++;
            }
        }

        if (failures > 0) {
            throw new RuntimeException("Test failed for ignoreMetadata "
                + ignoreMetadata + " and readUnknownTags " + readUnknownTags);
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
        int failures = 0;

        System.out.println();
        for (boolean ignoreMetadata : new boolean[] {false, true}) {
            for (boolean readUnknownTags : new boolean[] {false, true}) {
                try {
                    System.out.printf
                        ("ignoreMetadata: %s, readUnknownTags: %s%n",
                        ignoreMetadata, readUnknownTags);
                    (new ReadUnknownTagsTest(ignoreMetadata,
                        readUnknownTags)).run();
                } catch (Exception e) {
                    e.printStackTrace();
                    failures++;
                } finally {
                    System.out.println();
                }
            }
        }
    }
}
