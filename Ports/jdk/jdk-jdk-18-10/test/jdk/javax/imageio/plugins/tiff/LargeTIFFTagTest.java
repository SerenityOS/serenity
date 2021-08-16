/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8270893
 * @summary Ensure that we don't throw IndexOutOfBoundsException when
 *          we read TIFF tag with content more than 1024000 bytes
 * @run main LargeTIFFTagTest
 */

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

public class LargeTIFFTagTest {
    public static void main(String[] args) throws IOException {
        // TIFF stream length to hold 22 bytes of TIFF header
        // plus 1024002 bytes of data in one TIFFTag
        int length = 1024024;
        byte[] ba = new byte[length];
        // Little endian TIFF stream with header and only one
        // IFD entry at offset 22 having count value 1024002.
        byte[] header = new byte[] { (byte)0x49, (byte) 0x49,
                (byte)0x2a, (byte)0x00, (byte)0x08, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00,
                (byte)0x73, (byte)0x87, (byte)0x07, (byte)0x00,
                (byte)0x02, (byte)0xA0, (byte)0x0F, (byte)0x00,
                (byte)0x16, (byte)0x00, (byte)0x00, (byte)0x00};
        // copy first 22 bytes of TIFF header to byte array
        for (int i = 0; i < 22; i++) {
            ba[i] = header[i];
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        ImageInputStream stream = ImageIO.createImageInputStream(bais);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);

        if(readers.hasNext()) {
            ImageReader reader = readers.next();
            reader.setInput(stream);
            try {
                reader.readAll(0, null);
            } catch (IllegalArgumentException e) {
                // do nothing we expect IllegalArgumentException but we
                // should not throw IndexOutOfBoundsException.
                System.out.println(e.toString());
                System.out.println("Caught IllegalArgumentException ignore it");
            }
        } else {
            throw new RuntimeException("No readers available for TIFF format");
        }
    }
}
