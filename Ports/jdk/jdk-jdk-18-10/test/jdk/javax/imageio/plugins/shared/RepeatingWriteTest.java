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
 *
 * @bug     8144991 8150154
 * @author  a.stepanov
 * @summary Check if repeating image writing doesn't fail
 *          (particularly, no AIOOB occurs)
 *
 * @run     main RepeatingWriteTest
 */


import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.plugins.bmp.BMPImageWriteParam;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;

public class RepeatingWriteTest {

    private static final int TYPES[] = new int[]{
       BufferedImage.TYPE_INT_RGB,        BufferedImage.TYPE_INT_ARGB,
       BufferedImage.TYPE_INT_ARGB_PRE,   BufferedImage.TYPE_INT_BGR,
       BufferedImage.TYPE_3BYTE_BGR,      BufferedImage.TYPE_4BYTE_ABGR,
       BufferedImage.TYPE_4BYTE_ABGR_PRE, BufferedImage.TYPE_BYTE_GRAY,
       BufferedImage.TYPE_USHORT_GRAY,    BufferedImage.TYPE_BYTE_BINARY,
       BufferedImage.TYPE_BYTE_INDEXED,   BufferedImage.TYPE_USHORT_565_RGB,
       BufferedImage.TYPE_USHORT_555_RGB};

    private static final String NAMES[] = new String[]{
       "TYPE_INT_RGB",        "TYPE_INT_ARGB",
       "TYPE_INT_ARGB_PRE",   "TYPE_INT_BGR",
       "TYPE_3BYTE_BGR",      "TYPE_4BYTE_ABGR",
       "TYPE_4BYTE_ABGR_PRE", "TYPE_BYTE_GRAY",
       "TYPE_USHORT_GRAY",    "TYPE_BYTE_BINARY",
       "TYPE_BYTE_INDEXED",   "TYPE_USHORT_565_RGB",
       "TYPE_USHORT_555_RGB"};

    private static final int SZ1 = 200, SZ2 = 100;
    private static final Color C1 = Color.BLACK, C2 = Color.WHITE;

    private static ImageWriter getWriter(String format) {
        Iterator<ImageWriter> writers =
            ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            throw new RuntimeException("no writers available for " + format);
        }
        return writers.next();
    }

    private static ImageReader getReader(String format) {
        Iterator<ImageReader> readers =
            ImageIO.getImageReadersByFormatName(format);
        if (!readers.hasNext()) {
            throw new RuntimeException("no readers available for " + format);
        }
        return readers.next();
    }

    private static class ImageCheckException extends Exception {
        public ImageCheckException(String msg) { super(msg); }
    }

    private final String format;

    public RepeatingWriteTest(String format) { this.format = format; }

    private void checkImage(String fileName, boolean is1st) throws Exception {

        Color cRef = is1st ? C1  : C2;
        int  szRef = is1st ? SZ1 : SZ2;

        ImageReader reader = getReader(format);
        ImageInputStream iis = ImageIO.createImageInputStream(new File(fileName));
        reader.setInput(iis);
        BufferedImage img = reader.read(0);
        Color c = new Color(img.getRGB(szRef / 2, szRef / 2));
        int w = img.getWidth(), h = img.getHeight();

        if (w != szRef || h != szRef) {
            throw new ImageCheckException(fileName +
                ": invalid image size " + w + " x " + h +
                " expected " + szRef + " x " + szRef);
        }

        if (!c.equals(cRef)) {
            throw new ImageCheckException(fileName +
                ": invalid image color " + c +
                " expected " + cRef);
        }
    }

    private void doTest(int i, int j) throws Exception {

        String pair = NAMES[i] + " " + NAMES[j];

        // some type checks: avoid IO exceptions
        if ((format.equals("jpeg") || format.equals("bmp")) &&
            (pair.contains("USHORT_GRAY") ||
             pair.contains("ARGB") || pair.contains("ABGR"))) {
            return;
        }

        // If JDK-8163323 is fixed this if block should be removed.
        if (format.equals("tiff")
            && (NAMES[i].equals("TYPE_USHORT_555_RGB")
                 || NAMES[i].equals("TYPE_USHORT_565_RGB"))
                && (NAMES[j].equals("TYPE_USHORT_555_RGB")
                    || NAMES[j].equals("TYPE_USHORT_565_RGB"))) {
            return;
        }

        String f1 = "test-1-" + NAMES[i] + "." + format;
        String f2 = "test-2-" + NAMES[j] + "." + format;

        ImageWriter writer = getWriter(format);

        // --- write 1st image ---
        OutputStream s = new BufferedOutputStream(new FileOutputStream(f1));
        ImageOutputStream ios = ImageIO.createImageOutputStream(s);
        writer.setOutput(ios);

        BufferedImage img = new BufferedImage(SZ1, SZ1, TYPES[i]);
        Graphics g = img.getGraphics();
        g.setColor(C1);
        g.fillRect(0, 0, SZ1, SZ1);
        g.dispose();

        if (format.equals("jpeg")) {
            writer.write(null, new IIOImage(img, null, null),
                new JPEGImageWriteParam(Locale.getDefault()));
        } if (format.equals("bmp")) {
            writer.write(null, new IIOImage(img, null, null),
                new BMPImageWriteParam());
        } else {
            writer.write(img);
        }
        ios.flush();
        s.close();

        // --- write 2nd image ---
        s = new BufferedOutputStream(new FileOutputStream(f2));
        ios = ImageIO.createImageOutputStream(s);
        writer.setOutput(ios);

        img = new BufferedImage(SZ2, SZ2, TYPES[j]);
        g = img.getGraphics();
        g.setColor(C2);
        g.fillRect(0, 0, SZ2, SZ2);
        g.dispose();

        if (format.equals("jpeg")) {
            writer.write(null, new IIOImage(img, null, null),
                new JPEGImageWriteParam(Locale.getDefault()));
        } if (format.equals("bmp")) {
            writer.write(null, new IIOImage(img, null, null),
                new BMPImageWriteParam());
        } else {
            writer.write(img);
        }
        ios.flush();
        s.close();

        // --- check files ---
        checkImage(f1, true);
        checkImage(f2, false);
    }

    public static void main(String args[]) throws Exception {


        int n = TYPES.length;
        int nAIOOB = 0, nChecksFailed = 0;

        String formats[] = {"bmp", "jpeg", "gif", "png", "tiff"};

        for (String f: formats) {
            System.out.println("\nformat: " + f);
            RepeatingWriteTest test = new RepeatingWriteTest(f);
            for (int i = 0; i < n; ++i) {
                for (int j = 0; j < n; ++j) {
                    try {
                        test.doTest(i, j);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.err.println(f + ": AIOOB for pair " +
                            NAMES[i] + ", " + NAMES[j] + ": " + e.getMessage());
                        nAIOOB++;
                    } catch (ImageCheckException e) {
                        System.err.println(f +
                            ": image check failed for " + e.getMessage());
                        nChecksFailed++;
                    }
                }
            }
        }

        if (nAIOOB > 0 || nChecksFailed > 0) {
            throw new RuntimeException("test failed: " + nAIOOB + " AIOOBs, " +
                nChecksFailed + " image check failures");
        }
    }
}
