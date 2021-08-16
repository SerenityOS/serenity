/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4892682 4892698
 * @summary Tests that the appropriate IllegalStateException is thrown if
 *          ImageReader.read() or ImageWriter.write() is called without having
 *          first set the input/output stream
 */

import java.awt.image.BufferedImage;
import java.util.Iterator;

import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;

public class NullInputOutput {

    public static void main(String[] args) throws Exception {
        IIORegistry registry = IIORegistry.getDefaultInstance();

        // test ImageReader.read() for all available ImageReaders
        Iterator readerspis = registry.getServiceProviders(ImageReaderSpi.class,
                                                           false);
        while (readerspis.hasNext()) {
            ImageReaderSpi readerspi = (ImageReaderSpi)readerspis.next();
            ImageReader reader = readerspi.createReaderInstance();
            try {
                reader.read(0);
            } catch (IllegalStateException ise) {
                // caught exception, everything's okay
            }
        }

        // test ImageWriter.write() for all available ImageWriters
        BufferedImage bi = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Iterator writerspis = registry.getServiceProviders(ImageWriterSpi.class,
                                                           false);
        while (writerspis.hasNext()) {
            ImageWriterSpi writerspi = (ImageWriterSpi)writerspis.next();
            ImageWriter writer = writerspi.createWriterInstance();
            try {
                writer.write(bi);
            } catch (IllegalStateException ise) {
                // caught exception, everything's okay
            }
        }
    }
}
