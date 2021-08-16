/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8211422
 * @summary Test verifies that PNGImageReader does not throw
 *          IIOException when IEND chunk has corrupt CRC chunk.
 * @run     main PNGCorruptCRCForIENDChunkTest
 */

import java.io.ByteArrayInputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.util.Iterator;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;

public class PNGCorruptCRCForIENDChunkTest {

    // PNG image stream having corrupt CRC for IEND chunk
    private static String inputImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAA" +
            "8AAAAQCAYAAADJViUEAAAAIElEQVR4XmNQllf4Ty5mABHkgFHNJIJRzSSCo" +
            "a6ZXAwA26ElUIYphtYAAAAASUVORK5C";

    public static void main(String[] args) throws Exception {

        byte[] inputBytes = Base64.getDecoder().decode(inputImageBase64);
        ByteArrayInputStream bais = new ByteArrayInputStream(inputBytes);
        ImageInputStream input = ImageIO.createImageInputStream(bais);
        Iterator iter = ImageIO.getImageReaders(input);
        ImageReader reader = (ImageReader) iter.next();
        reader.setInput(input, false, false);
        BufferedImage image = reader.read(0, reader.getDefaultReadParam());
    }
}
