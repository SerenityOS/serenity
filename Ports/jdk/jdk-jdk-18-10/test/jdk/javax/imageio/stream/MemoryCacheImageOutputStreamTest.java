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
 * @bug 4417672 4422328
 * @summary Checks the functionality of MemoryCacheImageOutputStream
 *          particularly with regard to seeking and flushing
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class MemoryCacheImageOutputStreamTest {

    public static void main(String[] args) throws IOException {
        try {
            MemoryCacheImageOutputStream stream =
                new MemoryCacheImageOutputStream(new ByteArrayOutputStream());
            stream.write(0);  // or write anything, for that matter
            stream.flush();
        } catch (Exception e) {
            throw new RuntimeException("Error flushing stream: " + e);
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageOutputStream ios = new MemoryCacheImageOutputStream(os);

        byte[] b = new byte[30*256];
        byte byteVal = (byte)0;
        for (int i = 0; i < b.length; i++) {
            b[i] = byteVal++;
        }

        // Write 261,120 bytes
        for (int i = 0; i < 34; i++) {
            ios.write(b);
        }
        // Scatter 256 values at positions 1000, 2000, ...
        // Using both write(int) and write(byte[])
        byte[] buf = new byte[1];
        for (int i = 0; i < 256; i += 2) {
            ios.seek(1000*i);
            ios.write(i);

            ios.seek(1000*(i + 1));
            buf[0] = (byte)(i + 1);
            ios.write(buf);
        }

        // Re-read scattered values
        for (int i = 0; i < 256; i++) {
            ios.seek(1000*i);
            int val = ios.read();
            if (val != i) {
                System.out.println("Got bad value (1) at pos = " + (1000*i));
            }
        }

        // Discard two buffers and re-read scattered values
        ios.flushBefore(2*8192);

        for (int i = 0; i < 256; i++) {
            long pos = 1000*i;
            if (pos >= 2*8192) {
                ios.seek(pos);
                int val = ios.read();
                if (val != i) {
                    System.out.println("Got bad value (2) at pos = " + (1000*i));
                }
            }
        }
        ios.close();

        byte[] data = os.toByteArray();
        for (int i = 0; i < data.length; i++) {
            byte val = data[i];
            if ((i < 256000) && (i % 1000) == 0) {
                if (val != (byte)(i/1000)) {
                    System.out.println("Got bad value (3) at pos = " + i);
                }
            } else {
                byte gval = (byte)((i % (30*256)) % 256);
                if (val != gval) {
                    System.out.println("Got bad value (4) at pos = " + i +
                                       "(got " + (val & 0xff) +
                                       " wanted " + (gval & 0xff) +")");
                }
            }
        }
    }
}
