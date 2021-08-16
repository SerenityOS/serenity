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
 * @bug 4895483
 * @summary Test checks that the IllegalStateException was thrown if input was
 *          not set to the BMPImageReader
 */

import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;

public class EmptyInputBmpMetadataTest {
    private static String fmt = "BMP";

    public static void main(String[] args) {
        boolean isPassed = false;
        ImageReader ir = (ImageReader)ImageIO.getImageReadersByFormatName(fmt).next();

        if (ir == null) {
            throw new RuntimeException("No available reader for " + fmt);
        }
        IIOMetadata meta = null;
        try {
            meta = ir.getImageMetadata(0);
        } catch (IllegalStateException e) {
            System.out.println("Correct exception was thrown. Test passed.");
            isPassed = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!isPassed) {
            throw new RuntimeException("The IllegalStateException was not thrown."
                                       +"Test failed.");
        }
    }
}
