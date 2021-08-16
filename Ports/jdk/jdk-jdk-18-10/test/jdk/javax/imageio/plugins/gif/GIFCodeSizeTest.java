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

/**
 * @test
 * @bug 8238842
 * @summary Test incorrect code size results in IOException
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public class GIFCodeSizeTest {

    static final byte[] DATA =  {
        (byte)0x47, (byte)0x49, (byte)0x46, (byte)0x38, (byte)0x37,
        (byte)0x61, (byte)0x02, (byte)0x00, (byte)0x02, (byte)0x00,
        (byte)0x80, (byte)0x00, (byte)0x80, (byte)0x00, (byte)0xff,
        (byte)0xff, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x2c,
        (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x02,
        (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x12,
        (byte)0x02, (byte)0x84, (byte)0x51, (byte)0x00, (byte)0x3b
    };

    public static void main(String[] args) /*throws Exception */{
        try {
            ImageIO.read(new ByteArrayInputStream(DATA));
        } catch (IOException e) {
        }
    }
}
