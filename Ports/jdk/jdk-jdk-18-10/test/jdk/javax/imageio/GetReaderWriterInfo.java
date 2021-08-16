/*
 * Copyright (c) 2006, 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4703112
 * @summary Verifies that ImageIO.getReaderFileSuffixes() and similar methods
 *          return appropriate values
 */

import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;

public class GetReaderWriterInfo {

    private static void testGetReaderFormatNames() {
        String[] names = ImageIO.getReaderFormatNames();
        for (String n : names) {
            Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName(n);
            if (!it.hasNext()) {
                throw new RuntimeException("getReaderFormatNames returned " +
                                           "an unknown name: " + n);
            }
        }
    }

    private static void testGetReaderMIMETypes() {
        String[] types = ImageIO.getReaderMIMETypes();
        for (String t : types) {
            Iterator<ImageReader> it = ImageIO.getImageReadersByMIMEType(t);
            if (!it.hasNext()) {
                throw new RuntimeException("getReaderMIMETypes returned " +
                                           "an unknown type: " + t);
            }
        }
    }

    private static void testGetReaderFileSuffixes() {
        String[] suffixes = ImageIO.getReaderFileSuffixes();
        for (String s : suffixes) {
            Iterator<ImageReader> it = ImageIO.getImageReadersBySuffix(s);
            if (!it.hasNext()) {
                throw new RuntimeException("getReaderFileSuffixes returned " +
                                           "an unknown suffix: " + s);
            }
        }
    }

    private static void testGetWriterFormatNames() {
        String[] names = ImageIO.getWriterFormatNames();
        for (String n : names) {
            Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName(n);
            if (!it.hasNext()) {
                throw new RuntimeException("getWriterFormatNames returned " +
                                           "an unknown name: " + n);
            }
        }
    }

    private static void testGetWriterMIMETypes() {
        String[] types = ImageIO.getWriterMIMETypes();
        for (String t : types) {
            Iterator<ImageWriter> it = ImageIO.getImageWritersByMIMEType(t);
            if (!it.hasNext()) {
                throw new RuntimeException("getWriterMIMETypes returned " +
                                           "an unknown type: " + t);
            }
        }
    }

    private static void testGetWriterFileSuffixes() {
        String[] suffixes = ImageIO.getWriterFileSuffixes();
        for (String s : suffixes) {
            Iterator<ImageWriter> it = ImageIO.getImageWritersBySuffix(s);
            if (!it.hasNext()) {
                throw new RuntimeException("getWriterFileSuffixes returned " +
                                           "an unknown suffix: " + s);
            }
        }
    }

    public static void main(String[] args) {
        testGetReaderFormatNames();
        testGetReaderMIMETypes();
        testGetReaderFileSuffixes();
        testGetWriterFormatNames();
        testGetWriterMIMETypes();
        testGetWriterFileSuffixes();
    }
}
