/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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
* @bug 6396875
* @summary Checking that color conversion with CMM doesn't crash or throw ProfileDataException
* @run main ColorConvertTest
*/

import java.awt.color.*;
import java.awt.image.*;

public class ColorConvertTest {
    public static void main(String[] args) {
        ColorSpace rgbCS = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorSpace grayCS = ColorSpace.getInstance(ColorSpace.CS_GRAY);

        ColorConvertOp op = new ColorConvertOp(rgbCS, grayCS, null);
        BufferedImage src = new BufferedImage(100, 100,
                                              BufferedImage.TYPE_INT_RGB);
        BufferedImage dst = new BufferedImage(100, 100,
                                              BufferedImage.TYPE_BYTE_GRAY);
        try {
            op.filter(src, dst);
        } catch (ProfileDataException ex) {
            throw new RuntimeException("Test Failed", ex);
        }
    }
}
