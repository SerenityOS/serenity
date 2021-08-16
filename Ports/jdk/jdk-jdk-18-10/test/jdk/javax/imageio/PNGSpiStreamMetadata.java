/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4403355
 * @summary Checks that PNGImage{Reader,Writer}Spi.getStreamMetadataFormatNames
 *          and getNativeStreamMetadataFormatName return null
 * @modules java.desktop/com.sun.imageio.plugins.png
 */

import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.spi.ImageWriterSpi;

import com.sun.imageio.plugins.png.PNGImageReaderSpi;
import com.sun.imageio.plugins.png.PNGImageWriterSpi;

public class PNGSpiStreamMetadata {

    private static void fatal() {
        throw new RuntimeException("Got a non-null stream metadata format!");
    }

    public static void main(String[] args) {
        ImageReaderSpi rspi = new PNGImageReaderSpi();
        if (rspi.getNativeStreamMetadataFormatName() != null) {
            fatal();
        }
        if (rspi.isStandardStreamMetadataFormatSupported() != false) {
            fatal();
        }
        if (rspi.getExtraStreamMetadataFormatNames() != null) {
            fatal();
        }

        ImageWriterSpi wspi = new PNGImageWriterSpi();
        if (wspi.getNativeStreamMetadataFormatName() != null) {
            fatal();
        }
        if (wspi.isStandardStreamMetadataFormatSupported() != false) {
            fatal();
        }
        if (wspi.getExtraStreamMetadataFormatNames() != null) {
            fatal();
        }
    }
}
