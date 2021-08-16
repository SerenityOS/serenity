/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6275112
 * @summary Test verifies that imageReaders for base image formats return
 *          meaningful name of service provider interface (SPI) for base image
 *          formats
 * @modules java.desktop/com.sun.imageio.plugins.gif
 *          java.desktop/com.sun.imageio.plugins.png
 *          java.desktop/com.sun.imageio.plugins.jpeg
 *          java.desktop/com.sun.imageio.plugins.bmp
 *          java.desktop/com.sun.imageio.plugins.wbmp
 */

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageReaderSpi;

public class PluginSpiTest {

    public static void main(String[] args) {
        String format[] = { "GIF", "PNG", "JPEG", "BMP", "WBMP" };
        for (int i = 0; i < format.length; i++) {
            System.out.println("\nFormat " + format[i]);
            testFormat(format[i]);
        }
    }

    public static void testFormat(String format) {
        ImageReader reader =
            ImageIO.getImageReadersByFormatName(format).next();
        if (reader == null) {
            throw new RuntimeException("Failed to get reader for " + format);
        }

        ImageReaderSpi readerSpi = reader.getOriginatingProvider();
        System.out.println(format + " Reader SPI: " + readerSpi);

        String writerSpiNames[] = readerSpi.getImageWriterSpiNames();
        if (writerSpiNames == null || writerSpiNames.length == 0) {
            throw new RuntimeException("Failed to get writer spi names for " +
                                       format);
        }

        System.out.println("Available writer spi names:");
        for (int i = 0; i < writerSpiNames.length; i++) {
            System.out.println(writerSpiNames[i]);
            try {
                Class spiClass = Class.forName(writerSpiNames[i]);
                if (spiClass == null) {
                    throw new RuntimeException("Failed to get spi class " +
                                               writerSpiNames[i]);
                }
                System.out.println("Got class " + spiClass.getName());

                Object spiObject = spiClass.newInstance();
                if (spiObject == null) {
                    throw new RuntimeException("Failed to instantiate spi " +
                                               writerSpiNames[i]);
                }
                System.out.println("Got instance " + spiObject);
            } catch (Throwable e) {
                throw new RuntimeException("Failed to test spi " +
                                           writerSpiNames[i]);
            }
        }

        ImageWriter writer = ImageIO.getImageWriter(reader);
        if (writer == null) {
            throw new RuntimeException("Failed to get writer for " + format);
        }
    }
}
