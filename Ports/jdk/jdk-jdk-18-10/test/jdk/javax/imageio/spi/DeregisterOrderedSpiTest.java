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
 * @bug 4936495 8037743
 * @summary This test verifies whether deregistering an ordered spi object does
 *          not throw any exception
 * @modules java.desktop/com.sun.imageio.plugins.bmp
 *          java.desktop/com.sun.imageio.plugins.gif
 *          java.desktop/com.sun.imageio.plugins.jpeg
 *          java.desktop/com.sun.imageio.plugins.png
 */

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ServiceRegistry;

public class DeregisterOrderedSpiTest {

     public DeregisterOrderedSpiTest() {

         try {

             ServiceRegistry reg = IIORegistry.getDefaultInstance();
             ImageReaderSpi gifSpi = (ImageReaderSpi) reg.getServiceProviderByClass(com.sun.imageio.plugins.gif.GIFImageReaderSpi.class);
             ImageReaderSpi pngSpi = (ImageReaderSpi) reg.getServiceProviderByClass(com.sun.imageio.plugins.png.PNGImageReaderSpi.class);
             ImageReaderSpi jpgSpi = (ImageReaderSpi) reg.getServiceProviderByClass(com.sun.imageio.plugins.jpeg.JPEGImageReaderSpi.class);
             ImageReaderSpi bmpSpi = (ImageReaderSpi) reg.getServiceProviderByClass(com.sun.imageio.plugins.bmp.BMPImageReaderSpi.class);

             boolean ordered = reg.setOrdering(ImageReaderSpi.class, pngSpi,
                                               gifSpi);

             ordered = reg.setOrdering(ImageReaderSpi.class, gifSpi, jpgSpi);
             ordered = reg.setOrdering(ImageReaderSpi.class, bmpSpi, gifSpi);
             reg.deregisterServiceProvider(gifSpi);
             System.out.println("PASS");

         } catch (Exception e) {
             System.out.println("FAIL");
             throw new RuntimeException("Deregistering a spi object involved in some "
                                        + "ordering throws the following exception: " + e.toString());
         }
     }

     public static void main(String args[]) {
         DeregisterOrderedSpiTest test = new DeregisterOrderedSpiTest();
     }
}
