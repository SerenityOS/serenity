/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug     6438508
 * @summary Test verifies that ImageIO.write() does not create new
 *          empty file if requested image format is not supported.
 *
 * @run     main EmptyFileTest
 */

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class EmptyFileTest {
    public static void main(String[] args) throws IOException {
        String format = "MY_IMG";

        File out = new File("output.myimg");

        System.out.printf("File %s: %s\n", out.getAbsolutePath(),
                          out.exists() ? "EXISTS" : "NEW");

        BufferedImage img = createTestImage();

        boolean status = false;
        try {
            status = ImageIO.write(img, format, out);
        } catch (IOException e) {
            throw new RuntimeException("Test FAILED: unexpected exception", e);
        }
        if (status) {
            throw new RuntimeException("Test FAILED: Format " +
                                       format + " is supported.");
        }

        if (out.exists()) {
            throw new RuntimeException("Test FAILED.");
        }
        System.out.println("Test PASSED.");
    }

    private static BufferedImage createTestImage() {
        return new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
    }
}
