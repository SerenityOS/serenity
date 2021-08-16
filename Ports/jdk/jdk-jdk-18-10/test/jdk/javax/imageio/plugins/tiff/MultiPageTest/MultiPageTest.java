/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 *
 * @bug     8145776
 * @author  a.stepanov
 * @summary A simple write-read test for the multi-page tiff.
 *          Create the file programmaticaly, then do some simple checks
 *          (number of pages, sizes, colors). Use -Dseed=X to set
 *          the random generator seed.
 *
 * @build   jdk.test.lib.RandomFactory
 * @run     main MultiPageTest
 * @key     randomness
 */


import java.awt.*;
import java.awt.image.*;
import java.io.*;

import java.util.*;

import javax.imageio.*;
import javax.imageio.stream.*;

import jdk.test.lib.RandomFactory;


public class MultiPageTest {

    private final String fileName;

    private final int NUM_IMAGES = 51;

    private final static Random rnd = RandomFactory.getRandom();

    private final int w[], h[];
    private final Color colors[];
    private final int BLACK_SIZE = 100;

    private final int imageType;


    public MultiPageTest(int type, String tName) {

        imageType = type;
        fileName  = "test__" + tName + ".tif";

        w = new int[NUM_IMAGES + 4];
        h = new int[NUM_IMAGES + 4];

        for (int i = 2; i < NUM_IMAGES + 2; i++) {
            w[i] = 10 + rnd.nextInt(21);
            h[i] = 10 + rnd.nextInt(21);
        }

        w[0] = BLACK_SIZE;  h[0] = BLACK_SIZE;
        w[1] = BLACK_SIZE;  h[1] = BLACK_SIZE;
        w[NUM_IMAGES + 2] = BLACK_SIZE;  h[NUM_IMAGES + 2] = BLACK_SIZE;
        w[NUM_IMAGES + 3] = BLACK_SIZE;  h[NUM_IMAGES + 3] = BLACK_SIZE;


        colors = new Color[NUM_IMAGES + 4];
        for (int i = 2; i < NUM_IMAGES + 2; ++i) {
            colors[i] = new Color(
                rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
        }

        colors[0] = Color.black;
        colors[1] = Color.black;
        colors[NUM_IMAGES + 2] = Color.black;
        colors[NUM_IMAGES + 3] = Color.black;
    }


    private ImageWriter getTIFFWriter() {

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("TIFF");
        if (!writers.hasNext()) {
            throw new RuntimeException("No writers available for " + fileName);
        }
        return writers.next();
    }

    private ImageReader getTIFFReader() {

        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("TIFF");
        if (!readers.hasNext()) {
            throw new RuntimeException("No readers available for " + fileName);
        }
        return readers.next();
    }


    private void createImage() throws Exception {

        OutputStream s = new BufferedOutputStream(new FileOutputStream(fileName));
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(s)) {

            ImageWriter writer = getTIFFWriter();
            writer.setOutput(ios);

            Graphics g;

            BufferedImage blackImg =
                new BufferedImage(BLACK_SIZE, BLACK_SIZE, imageType);
            g = blackImg.getGraphics();
            g.setColor(Color.black);
            g.fillRect(0, 0, BLACK_SIZE, BLACK_SIZE);

            writer.prepareWriteSequence(null);

            for (int i = 2; i < NUM_IMAGES + 2; i++) {
                BufferedImage img = new BufferedImage(w[i], h[i], imageType);

                g = img.getGraphics();
                g.setColor(colors[i]);
                g.fillRect(0, 0, w[i], h[i]);

                writer.writeToSequence(new IIOImage(img, null, null), null);
            }

            writer.endWriteSequence();

            // check: insert to the beginning
            writer.writeInsert(0, new IIOImage(blackImg, null, null), null);

            // check: insert to non-zero position
            writer.writeInsert(1, new IIOImage(blackImg, null, null), null);

            // check: append to the end by index
            writer.writeInsert(NUM_IMAGES + 2,
                new IIOImage(blackImg, null, null), null);

            // check: append to the end using index "-1"
            writer.writeInsert(-1, new IIOImage(blackImg, null, null), null);

            ios.flush();
            writer.dispose();
        }
        s.close();
    }



    private void readAndCheckImage() throws Exception {

        ImageReader reader = getTIFFReader();

        ImageInputStream s = ImageIO.createImageInputStream(new File(fileName));
        reader.setInput(s);


        // check number of pages
        if ((NUM_IMAGES + 4) != reader.getNumImages(true)) {
            throw new RuntimeException("invalid number of images!");
        }

        // check colors / sizes
        for (int i = 0; i < NUM_IMAGES + 4; i++) {

            BufferedImage img = reader.read(i);

            int imw = w[i], imh = h[i];

            if ( (img.getWidth() != imw) || (img.getHeight() != imh) ) {
                throw new RuntimeException("NOK: size(" + i + ")");
            }

            Color
                    c1 = new Color(img.getRGB(0, 0)),
                    c2 = new Color(img.getRGB(imw / 2, imh / 2)),
                    c3 = new Color(img.getRGB(imw - 1, imh - 1));
            if (! (c1.equals(colors[i]) && c1.equals(c2) && c1.equals(c3) ) ) {
                throw new RuntimeException("NOK: color(" + i + ")");
            }
        }

        reader.dispose();
        s.close();
    }

    public void doTest() throws Exception {
        createImage();
        readAndCheckImage();
    }

    public static void main(String[] args) throws Exception {

        int types[] = new int[]{
            BufferedImage.TYPE_INT_RGB,
            BufferedImage.TYPE_INT_ARGB,
            BufferedImage.TYPE_INT_ARGB_PRE,
            BufferedImage.TYPE_INT_BGR,
            BufferedImage.TYPE_3BYTE_BGR,
            BufferedImage.TYPE_4BYTE_ABGR,
            BufferedImage.TYPE_4BYTE_ABGR_PRE
        };

        String names[] = new String[]{
            "TYPE_INT_RGB",
            "TYPE_INT_ARGB",
            "TYPE_INT_ARGB_PRE",
            "TYPE_INT_BGR",
            "TYPE_3BYTE_BGR",
            "TYPE_4BYTE_ABGR",
            "TYPE_4BYTE_ABGR_PRE"
        };

        for (int i = 0; i < types.length; i++) {
            System.out.println("image type: " + names[i]);
            (new MultiPageTest(types[i], names[i])).doTest();
        }
    }
}
