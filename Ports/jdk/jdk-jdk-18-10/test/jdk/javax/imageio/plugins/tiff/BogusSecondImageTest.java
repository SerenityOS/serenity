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
 * @bug 8145014
 * @summary Verify reader correctly fails for zero-entry IFDs and EOFs
 *          encountered in locateImage().
 */

import java.awt.Image;
import java.awt.image.*;
import java.io.*;
import java.util.Iterator;
import javax.imageio.*;
import javax.imageio.stream.*;

public class BogusSecondImageTest {
    public static void main(String[] args) throws Throwable {
        int failures = 0;

        try {
            testZeroEntryIFD();
        } catch (Exception e) {
            System.out.printf("Failed testZeroEntryIFD: %s%n", e);
            failures++;
        }

        try {
            testOutOfStreamIFD();
        } catch (Exception e) {
            System.out.printf("Failed testOutOfStreamIFD: %s%n", e);
            failures++;
        }

        if (failures == 0) {
            System.out.println("Test succeeded");
        } else {
            throw new RuntimeException
                ("Test failed with " + failures + " errors");
        }
    }

    private static void testZeroEntryIFD() throws Exception {
        // Create an image.
        File f = createImageFile();

        ImageOutputStream s = new FileImageOutputStream(f);
        long length = s.length();

        // Skip the endianness and magic number
        s.skipBytes(4);

        // Read and seek to offset of 0th IFD
        long ifd0 = s.readUnsignedInt();
        s.seek(ifd0);

        // Read number of 0th IFD entries and skip over them
        int entries0 = s.readUnsignedShort();
        s.skipBytes(12*entries0);

        // Write the offset of the 1st IFD as the current file length
        s.write((int)length);

        // Seek to the 1st IFD and write a zero entry count to it
        s.seek(length);
        s.writeShort(0);
        s.close();

        try {
            Load(f);
        } catch (Exception e) {
            throw e;
        } finally {
            f.delete();
        }
    }

    private static void testOutOfStreamIFD() throws Exception {
        // Create an image.
        File f = createImageFile();
        ImageOutputStream s = new FileImageOutputStream(f);
        long length = s.length();

        // Skip the endianness and magic number
        s.skipBytes(4);

        // Read and seek to offset of 0th IFD
        long ifd0 = s.readUnsignedInt();
        s.seek(ifd0);

        // Read number of 0th IFD entries and skip over them
        int entries0 = s.readUnsignedShort();
        s.skipBytes(12*entries0);

        // Write the offset of the 1st IFD as the current file length + 7
        s.write((int)length + 7);
        s.close();

        try {
            Load(f);
        } catch (Exception e) {
            throw e;
        } finally {
            f.delete();
        }
    }

    private static File createImageFile() throws Exception {
        BufferedImage im =
        new BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY);
        File f = File.createTempFile("BogusSecondImage", "tif", new File("."));
        f.deleteOnExit();
        if (!ImageIO.write(im, "TIFF", f)) {
            throw new RuntimeException("Failed to write " + f);
        }
        return f;
    }

    private final static boolean printTrace = false;

    public static void Load(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException(file + " does not exist");
        } else if (!file.isFile()) {
            throw new IllegalArgumentException(file + " is not a regular file");
        } else if (!file.canRead()) {
            throw new IllegalArgumentException(file + " cannot be read");
        }

        ImageInputStream input = null;
        try {
            input = ImageIO.createImageInputStream(file);
        } catch (Throwable e) {
            System.err.println("NOK: createImageInputStream()\t" + e.getMessage());
            if (printTrace) { e.printStackTrace(); }
            return;
        }

        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("TIFF");
        if (!readers.hasNext()) { throw new RuntimeException("No readers available for TIFF"); }
        ImageReader reader = readers.next();
        reader.setInput(input);

        Image images[] = null;
        int numImages = 0;

        int failures = 0;
        try {
            numImages = reader.getNumImages(true);
            images = new Image[numImages];
        } catch (Throwable e) {
            failures++;
            System.err.println("NOK: getNumImages()\t" + e.getMessage());
            if (printTrace) { e.printStackTrace(); }
        }
        System.out.printf("numImages %d%n", numImages);

        for (int i = 0; i < numImages; i++) {
            System.out.printf("reading image %d%n", i);
            try {
                images[i] = reader.read(i);
            } catch (Throwable e) {
                failures++;
                System.err.println("NOK: read()\t" + e.getMessage());
                if (printTrace) { e.printStackTrace(); }
            }
        }

        if (failures == 0) {
            System.err.println("OK");
        } else {
            throw new RuntimeException("NOK");
        }
    }
}
