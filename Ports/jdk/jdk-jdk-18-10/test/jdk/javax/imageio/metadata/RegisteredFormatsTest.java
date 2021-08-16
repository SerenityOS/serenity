/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      5017991
 * @summary  This test verifies two things:
 *            a) we can get MetadataFormat classes for
 *                each registered metadata format.
 *            b) all metadata formats for standard plugins
 *                are registered.
 * @run main RegisteredFormatsTest
 */

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.metadata.IIOMetadataFormat;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.Enumeration;

public class RegisteredFormatsTest {

    private static Hashtable fmts;

    public static void main(String[] args) {
        fmts = new Hashtable();

        fmts.put("javax_imageio_jpeg_stream_1.0", Boolean.FALSE);
        fmts.put("javax_imageio_jpeg_image_1.0",  Boolean.FALSE);
        fmts.put("javax_imageio_png_1.0",         Boolean.FALSE);
        fmts.put("javax_imageio_bmp_1.0",         Boolean.FALSE);
        fmts.put("javax_imageio_wbmp_1.0",        Boolean.FALSE);
        fmts.put("javax_imageio_gif_stream_1.0",  Boolean.FALSE);
        fmts.put("javax_imageio_gif_image_1.0",   Boolean.FALSE);

        IIORegistry registry = IIORegistry.getDefaultInstance();
        Iterator iter = registry.getServiceProviders(ImageReaderSpi.class,
                                                     false);
        while(iter.hasNext()) {
            ImageReaderSpi spi = (ImageReaderSpi)iter.next();
            String fmt_name;
            fmt_name = spi.getNativeStreamMetadataFormatName();
            testStreamMetadataFormat(spi, fmt_name);

            fmt_name = spi.getNativeImageMetadataFormatName();
            testImageMetadataFormat(spi, fmt_name);

            String[] fmt_names;
            fmt_names = spi.getExtraStreamMetadataFormatNames();
            for (int i=0; fmt_names != null && i < fmt_names.length; i++) {
                testStreamMetadataFormat(spi, fmt_names[i]);
            }

            fmt_names = spi.getExtraImageMetadataFormatNames();
            for (int i=0; fmt_names != null && i < fmt_names.length; i++) {
                testImageMetadataFormat(spi, fmt_names[i]);
            }
        }
        Enumeration keys = fmts.keys();
        while (keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            boolean val = ((Boolean)fmts.get(key)).booleanValue();
            if (!val) {
                throw new RuntimeException("Test failed: format " +
                                           key + "is not registered.");
            }
        }
    }

    private static void testStreamMetadataFormat(ImageReaderSpi spi,
                                                 String fmt_name) {
        if (fmt_name == null) {
            return;
        }
        try {
            testMetadataFormat(spi.getStreamMetadataFormat(fmt_name),
                               fmt_name);
        } catch (Exception e) {
            throw new RuntimeException("Test failed for " + fmt_name,
                                       e);
        }
    }

    private static void testImageMetadataFormat(ImageReaderSpi spi,
                                                String fmt_name) {
        if (fmt_name == null) {
            return;
        }
        try {
            testMetadataFormat(spi.getImageMetadataFormat(fmt_name),
                               fmt_name);
        } catch (Exception e) {
            throw new RuntimeException("Test failed for " + fmt_name,
                                       e);
        }
    }
    private static void testMetadataFormat(IIOMetadataFormat fmt,
                                           String fmt_name) {
        System.out.print(fmt_name + "...");
        if (fmt != null) {
            fmts.put(fmt_name, Boolean.TRUE);
            System.out.println("Ok");
        } else {
            throw new RuntimeException("Test failed for " + fmt_name);
        }
    }
}
