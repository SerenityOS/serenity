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
 * @bug 4892609
 * @summary Tests that the appropriate IllegalStateException is thrown if
 *          ImageReader.getNumImages() is called with a null source or if
 *          allowSearch is specified at the same time that seekForwardOnly is
 *          true
 */

import java.io.ByteArrayInputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public class GetNumImages {

    public static void main(String[] args) throws Exception {
        IIORegistry registry = IIORegistry.getDefaultInstance();

        // test ImageReader.getNumImages() for all available ImageReaders,
        // with no source set
        Iterator readerspis = registry.getServiceProviders(ImageReaderSpi.class,
                                                           false);
        while (readerspis.hasNext()) {
            boolean caughtEx = false;
            ImageReaderSpi readerspi = (ImageReaderSpi)readerspis.next();
            ImageReader reader = readerspi.createReaderInstance();
            try {
                reader.getNumImages(false);
            } catch (IllegalStateException ise) {
                // caught exception, everything's okay
                caughtEx = true;
            }

            if (!caughtEx) {
                throw new RuntimeException("Test failed: exception was not " +
                                           "thrown for null input: " +
                                           reader);
            }
        }

        // test ImageReader.getNumImages() for all available ImageReaders,
        // with source set, seekForwardOnly and allowSearch both true
        readerspis = registry.getServiceProviders(ImageReaderSpi.class,
                                                  false);
        while (readerspis.hasNext()) {
            boolean caughtEx = false;
            ImageReaderSpi readerspi = (ImageReaderSpi)readerspis.next();
            ImageReader reader = readerspi.createReaderInstance();
            byte[] barr = new byte[100];
            ByteArrayInputStream bais = new ByteArrayInputStream(barr);
            ImageInputStream iis = ImageIO.createImageInputStream(bais);
            try {
                reader.setInput(iis, true);
                reader.getNumImages(true);
            } catch (IllegalStateException ise) {
                // caught exception, everything's okay
                caughtEx = true;
            }

            if (!caughtEx) {
                throw new RuntimeException("Test failed: exception was not " +
                                           "thrown when allowSearch and " +
                                           "seekForwardOnly are both true: " +
                                           reader);
            }
        }
    }
}
