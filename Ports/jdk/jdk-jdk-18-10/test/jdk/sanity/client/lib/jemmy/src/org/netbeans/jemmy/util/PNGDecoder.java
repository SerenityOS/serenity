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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.netbeans.jemmy.JemmyException;

/**
 * Allows to load PNG graphical file.
 *
 * @author Alexandre Iline
 */
public class PNGDecoder extends Object {

    InputStream in;

    /**
     * Constructs a PNGDecoder object.
     *
     * @param in input stream to read PNG image from.
     */
    public PNGDecoder(InputStream in) {
        this.in = in;
    }

    byte read() throws IOException {
        byte b = (byte) in.read();
        return b;
    }

    int readInt() throws IOException {
        byte b[] = read(4);
        return (((b[0] & 0xff) << 24)
                + ((b[1] & 0xff) << 16)
                + ((b[2] & 0xff) << 8)
                + ((b[3] & 0xff)));
    }

    byte[] read(int count) throws IOException {
        byte[] result = new byte[count];
        for (int i = 0; i < count; i++) {
            result[i] = read();
        }
        return result;
    }

    boolean compare(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) {
            return false;
        }
        for (int i = 0; i < b1.length; i++) {
            if (b1[i] != b2[i]) {
                return false;
            }
        }
        return true;
    }

    void checkEquality(byte[] b1, byte[] b2) {
        if (!compare(b1, b2)) {
            throw (new JemmyException("Format error"));
        }
    }

    /**
     * Decodes image from an input stream passed into constructor.
     *
     * @return a BufferedImage object
     * @throws IOException
     */
    public BufferedImage decode() throws IOException {

        byte[] id = read(12);
        checkEquality(id, new byte[]{-119, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13});

        byte[] ihdr = read(4);
        checkEquality(ihdr, "IHDR".getBytes());

        int width = readInt();
        int height = readInt();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        byte[] head = read(5);
        int mode;
        if (compare(head, new byte[]{1, 0, 0, 0, 0})) {
            mode = PNGEncoder.BW_MODE;
        } else if (compare(head, new byte[]{8, 0, 0, 0, 0})) {
            mode = PNGEncoder.GREYSCALE_MODE;
        } else if (compare(head, new byte[]{8, 2, 0, 0, 0})) {
            mode = PNGEncoder.COLOR_MODE;
        } else {
            throw (new JemmyException("Format error"));
        }

        readInt();//!!crc

        int size = readInt();

        byte[] idat = read(4);
        checkEquality(idat, "IDAT".getBytes());

        byte[] data = read(size);

        Inflater inflater = new Inflater();
        inflater.setInput(data, 0, size);

        int color;

        try {
            switch (mode) {
                case PNGEncoder.BW_MODE: {
                    int bytes = width / 8;
                    if ((width % 8) != 0) {
                        bytes++;
                    }
                    byte colorset;
                    byte[] row = new byte[bytes];
                    for (int y = 0; y < height; y++) {
                        inflater.inflate(new byte[1]);
                        inflater.inflate(row);
                        for (int x = 0; x < bytes; x++) {
                            colorset = row[x];
                            for (int sh = 0; sh < 8; sh++) {
                                if (x * 8 + sh >= width) {
                                    break;
                                }
                                if ((colorset & 0x80) == 0x80) {
                                    result.setRGB(x * 8 + sh, y, Color.white.getRGB());
                                } else {
                                    result.setRGB(x * 8 + sh, y, Color.black.getRGB());
                                }
                                colorset <<= 1;
                            }
                        }
                    }
                }
                break;
                case PNGEncoder.GREYSCALE_MODE: {
                    byte[] row = new byte[width];
                    for (int y = 0; y < height; y++) {
                        inflater.inflate(new byte[1]);
                        inflater.inflate(row);
                        for (int x = 0; x < width; x++) {
                            color = row[x];
                            result.setRGB(x, y, (color << 16) + (color << 8) + color);
                        }
                    }
                }
                break;
                case PNGEncoder.COLOR_MODE: {
                    byte[] row = new byte[width * 3];
                    for (int y = 0; y < height; y++) {
                        inflater.inflate(new byte[1]);
                        inflater.inflate(row);
                        for (int x = 0; x < width; x++) {
                            result.setRGB(x, y,
                                    ((row[x * 3 + 0] & 0xff) << 16)
                                    + ((row[x * 3 + 1] & 0xff) << 8)
                                    + ((row[x * 3 + 2] & 0xff)));
                        }
                    }
                }
            }
        } catch (DataFormatException e) {
            throw (new JemmyException("ZIP error", e));
        }

        readInt();//!!crc
        readInt();//0

        byte[] iend = read(4);
        checkEquality(iend, "IEND".getBytes());

        readInt();//!!crc
        in.close();

        return result;
    }

    /**
     * Decodes image from file.
     *
     * @param fileName a file to read image from
     * @return a BufferedImage instance.
     */
    public static BufferedImage decode(String fileName) {
        try {
            return new PNGDecoder(new FileInputStream(fileName)).decode();
        } catch (IOException e) {
            throw (new JemmyException("IOException during image reading", e));
        }
    }

}
