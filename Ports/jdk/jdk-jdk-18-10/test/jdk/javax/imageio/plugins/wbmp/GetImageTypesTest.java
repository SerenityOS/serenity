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
 * @bug 4928273
 * @summary Verifies what IllegalStateException is thrown if image input was not
 *          set
 */

import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

public class GetImageTypesTest {

    private static final String format = "wbmp";

    public static void main(String[] args) {

        boolean passed = false;
        ImageReader ir = (ImageReader)ImageIO.getImageReadersByFormatName(format).next();

        if (ir == null) {
            throw new RuntimeException("No matching reader found. Test Failed");
        }

        try {
            Iterator types = ir.getImageTypes(0);
        } catch (IllegalStateException e) {
            System.out.println("Test passed.");
            passed = true;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception was thrown. "
                                       + "Test failed.");
        }

        if (!passed) {
            throw new RuntimeException("IllegalStateException is not thrown when "
                                       + "calling getImageTypes() without setting "
                                       + "the input source for the image format: "
                                       + format
                                       + ". Test failed");
        }

    }
}
