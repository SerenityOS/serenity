/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package org.netbeans.jemmy.util;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * This class allows to encode BufferedImage into B/W, greyscale or true color
 * PNG image format with maximum compression.<br>
 * It also provides complete functionality for capturing full screen, part of
 * screen or single component, encoding and saving captured image info PNG file.
 *
 * @author Adam Sotona
 * @version 1.0
 */
public class PNGEncoder extends Object {

    /**
     * black and white image mode.
     */
    public static final byte BW_MODE = 0;
    /**
     * grey scale image mode.
     */
    public static final byte GREYSCALE_MODE = 1;
    /**
     * full color image mode.
     */
    public static final byte COLOR_MODE = 2;

    OutputStream out;
    CRC32 crc;
    byte mode;

    /**
     * public constructor of PNGEncoder class with greyscale mode by default.
     *
     * @param out output stream for PNG image format to write into
     */
    public PNGEncoder(OutputStream out) {
        this(out, GREYSCALE_MODE);
    }

    /**
     * public constructor of PNGEncoder class.
     *
     * @param out output stream for PNG image format to write into
     * @param mode BW_MODE, GREYSCALE_MODE or COLOR_MODE
     */
    public PNGEncoder(OutputStream out, byte mode) {
        crc = new CRC32();
        this.out = out;
        if (mode < 0 || mode > 2) {
            throw new IllegalArgumentException("Unknown color mode");
        }
        this.mode = mode;
    }

    void write(int i) throws IOException {
        byte b[] = {(byte) ((i >> 24) & 0xff), (byte) ((i >> 16) & 0xff), (byte) ((i >> 8) & 0xff), (byte) (i & 0xff)};
        write(b);
    }

    void write(byte b[]) throws IOException {
        out.write(b);
        crc.update(b);
    }

    /**
     * main encoding method (stays blocked till encoding is finished).
     *
     * @param image BufferedImage to encode
     * @throws IOException IOException
     */
    public void encode(BufferedImage image) throws IOException {
        int width = image.getWidth(null);
        int height = image.getHeight(null);
        final byte id[] = {-119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13};
        write(id);
        crc.reset();
        write("IHDR".getBytes());
        write(width);
        write(height);
        byte head[];
        switch (mode) {
            case BW_MODE:
                head = new byte[]{1, 0, 0, 0, 0};
                break;
            case GREYSCALE_MODE:
                head = new byte[]{8, 0, 0, 0, 0};
                break;
            case COLOR_MODE:
                head = new byte[]{8, 2, 0, 0, 0};
                break;
            default:
                throw new AssertionError("Unexpected mode: " + mode);
        }
        write(head);
        write((int) crc.getValue());
        ByteArrayOutputStream compressed = new ByteArrayOutputStream(65536);
        BufferedOutputStream bos = new BufferedOutputStream(new DeflaterOutputStream(compressed, new Deflater(9)));
        int pixel;
        int color;
        int colorset;
        switch (mode) {
            case BW_MODE:
                int rest = width % 8;
                int bytes = width / 8;
                for (int y = 0; y < height; y++) {
                    bos.write(0);
                    for (int x = 0; x < bytes; x++) {
                        colorset = 0;
                        for (int sh = 0; sh < 8; sh++) {
                            pixel = image.getRGB(x * 8 + sh, y);
                            color = ((pixel >> 16) & 0xff);
                            color += ((pixel >> 8) & 0xff);
                            color += (pixel & 0xff);
                            colorset <<= 1;
                            if (color >= 3 * 128) {
                                colorset |= 1;
                            }
                        }
                        bos.write((byte) colorset);
                    }
                    if (rest > 0) {
                        colorset = 0;
                        for (int sh = 0; sh < width % 8; sh++) {
                            pixel = image.getRGB(bytes * 8 + sh, y);
                            color = ((pixel >> 16) & 0xff);
                            color += ((pixel >> 8) & 0xff);
                            color += (pixel & 0xff);
                            colorset <<= 1;
                            if (color >= 3 * 128) {
                                colorset |= 1;
                            }
                        }
                        colorset <<= 8 - rest;
                        bos.write((byte) colorset);
                    }
                }
                break;
            case GREYSCALE_MODE:
                for (int y = 0; y < height; y++) {
                    bos.write(0);
                    for (int x = 0; x < width; x++) {
                        pixel = image.getRGB(x, y);
                        color = ((pixel >> 16) & 0xff);
                        color += ((pixel >> 8) & 0xff);
                        color += (pixel & 0xff);
                        bos.write((byte) (color / 3));
                    }
                }
                break;
            case COLOR_MODE:
                for (int y = 0; y < height; y++) {
                    bos.write(0);
                    for (int x = 0; x < width; x++) {
                        pixel = image.getRGB(x, y);
                        bos.write((byte) ((pixel >> 16) & 0xff));
                        bos.write((byte) ((pixel >> 8) & 0xff));
                        bos.write((byte) (pixel & 0xff));
                    }
                }
                break;
        }
        bos.close();
        write(compressed.size());
        crc.reset();
        write("IDAT".getBytes());
        write(compressed.toByteArray());
        write((int) crc.getValue());
        write(0);
        crc.reset();
        write("IEND".getBytes());
        write((int) crc.getValue());
        out.close();
    }

    /**
     * Static method performing screen capture into PNG image format file with
     * given fileName.
     *
     * @param rect Rectangle of screen to be captured
     * @param fileName file name for screen capture PNG image file
     */
    public static void captureScreen(Rectangle rect, String fileName) {
        captureScreen(rect, fileName, GREYSCALE_MODE);
    }

    /**
     * Static method performing screen capture into PNG image format file with
     * given fileName.
     *
     * @param rect Rectangle of screen to be captured
     * @param mode image color mode
     * @param fileName file name for screen capture PNG image file
     */
    public static void captureScreen(Rectangle rect, String fileName, byte mode) {
        try {
            BufferedImage capture = new Robot().createScreenCapture(rect);
            BufferedOutputStream file = new BufferedOutputStream(new FileOutputStream(fileName));
            PNGEncoder encoder = new PNGEncoder(file, mode);
            encoder.encode(capture);
        } catch (AWTException awte) {
            awte.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Static method performing one component screen capture into PNG image
     * format file with given fileName.
     *
     * @param comp Component to be captured
     * @param fileName String image target filename
     */
    public static void captureScreen(Component comp, String fileName) {
        captureScreen(comp, fileName, GREYSCALE_MODE);
    }

    /**
     * Static method performing one component screen capture into PNG image
     * format file with given fileName.
     *
     * @param comp Component to be captured
     * @param fileName String image target filename
     * @param mode image color mode
     */
    public static void captureScreen(Component comp, String fileName, byte mode) {
        captureScreen(new Rectangle(comp.getLocationOnScreen(),
                comp.getSize()),
                fileName, mode);
    }

    /**
     * Static method performing whole screen capture into PNG image format file
     * with given fileName.
     *
     * @param fileName String image target filename
     */
    public static void captureScreen(String fileName) {
        captureScreen(fileName, GREYSCALE_MODE);
    }

    /**
     * Static method performing whole screen capture into PNG image format file
     * with given fileName.
     *
     * @param fileName String image target filename
     * @param mode image color mode
     */
    public static void captureScreen(String fileName, byte mode) {
        captureScreen(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()), fileName, mode);
    }
}
