/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6532025
 * @summary Test verifies that we dont throw IOOBE for truncated
 *          GIF image
 */

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.IIOException;

public class TruncatedGIFTest {
    public static void main(String[] args) throws IOException {
        // GIFF stream with no GCT/LCT but one truncated LZW
        // image block
        byte[] ba = new byte[] { (byte)0x47, (byte) 0x49,
                (byte)0x46, (byte)0x38, (byte)0x39, (byte)0x61,
                (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x2c,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00,
                (byte)0x00, (byte)0x04, (byte)0x0A, (byte)0x00};

        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(ba));
        } catch (IIOException e) {
            // do nothing we expect IIOException but we should not
            // throw IndexOutOfBoundsException
            System.out.println(e.toString());
            System.out.println("Caught IIOException ignore it");
            System.out.println("Test passed");
        }
    }
}

