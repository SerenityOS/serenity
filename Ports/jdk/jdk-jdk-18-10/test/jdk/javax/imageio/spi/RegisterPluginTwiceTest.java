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
 * @bug 4836432 8037743
 * @summary This test attempts to register two instances of one ImageReaderSPI.
 *          Expected behavior is that only one instance of ImageReaderSPI will
 *          be registered.
 */

import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;

public class RegisterPluginTwiceTest {

    public RegisterPluginTwiceTest() throws Exception {
        BMPImageReaderSPI BMPSpi = new BMPImageReaderSPI();
        BMPImageReaderSPI BMPSpi1 = new BMPImageReaderSPI();

        IIORegistry regis = IIORegistry.getDefaultInstance();
        boolean res1
            = regis.registerServiceProvider(BMPSpi,
                                            javax.imageio.spi.ImageReaderSpi.class);
        boolean res2
            = regis.registerServiceProvider(BMPSpi1,
                                            javax.imageio.spi.ImageReaderSpi.class);

        if(!res1 || res2) {
            throw new RuntimeException("Bad returned values for registerServiceProvider");
        }
        Iterator it = regis.getServiceProviders(Class.forName("javax.imageio.spi.ImageReaderSpi"), true);
        int count = 0;
        while (it.hasNext()) {
            Object o = it.next();
            if(o instanceof BMPImageReaderSPI) {
                count++;
                System.out.println("Found next BMPImageReaderSPI, count = " +count);
            }
        }
        if(count > 1) {
            throw new RuntimeException("Too many instances of the BMPImageReaderSPI was registered!");
        }
    }

    public static void main(String args[]) throws Exception{
        RegisterPluginTwiceTest fnio = new RegisterPluginTwiceTest();
    }


 /**
  Not a perfect implementation of SPI. This is just a dummy implementation
  which denotes some arbitrary reader class. The intention is to check how this
  is getting registered in the registry. Hence some of the values in this class
  may be inappropriate..
 */
    public static class BMPImageReaderSPI extends javax.imageio.spi.ImageReaderSpi{

        private static final String vendorName = "Javasoft";

        private static final String version = "2.0";

        private static final String[] names = { "bmp" };

        private static final String[] suffixes = { "bmp" };

        private static final String[] MIMETypes = { "image/x-bmp"};

        private static final String readerClassName =
        "com.sun.imageio.plugins.png.PNGImageReader";

        private static final String[] writerSpiNames = {
            "com.sun.imageio.plugins.png.PNGImageWriterSpi"
        };

        public BMPImageReaderSPI() {
            super(vendorName,
                  version,
                  names,
                  suffixes,
                  MIMETypes,
                  readerClassName,
                  STANDARD_INPUT_TYPE,
                  writerSpiNames,
                  false,
                  null, null,
                  null, null,
                  true,
                  "BMP Native Metadata",
                  "com.sun.imageio.plugins.png.PNGMetadataFormat",
                  null, null
                  );
        }

        public String getDescription(Locale locale) {
            return "Standard BMP image reader";
        }

        public boolean canDecodeInput(Object input) throws IOException {
            if (!(input instanceof ImageInputStream)) {
                return false;
            }

            ImageInputStream stream = (ImageInputStream)input;
            byte[] b = new byte[8];
            stream.mark();
            stream.readFully(b);
            stream.reset();

            return (b[0] == (byte)137 &&
                    b[1] == (byte)80 &&
                    b[2] == (byte)78 &&
                    b[3] == (byte)71 &&
                    b[4] == (byte)13 &&
                    b[5] == (byte)10 &&
                    b[6] == (byte)26 &&
                    b[7] == (byte)10);
        }

        public ImageReader createReaderInstance(Object extension) {
            //return new PNGImageReader(this);
            return null;
        }
        public void onRegistration(ServiceRegistry sr, Class<?> category) {
            //System.out.println("Registered "+category);
            super.onRegistration(sr, category);
        }

        public void onDeregistration(ServiceRegistry sr, Class<?> category) {
            //System.out.println("De-Registered "+category);
            //super.onRegistration(sr, category);
        }
    }
}
