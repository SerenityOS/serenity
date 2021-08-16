/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4507868
 * @summary Checks that ImageOutputStreamImpl.writeBits() advances the stream
 *          position and bit offset correctly. Also verifies that the
 *          MemoryCacheImageOutputStream.read() variants reset the bitOffset
 *          before the read actually occurs.
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class WriteBitsTest {

    private static void verify(ImageOutputStream ios,
                               long expstreampos, int expbitoffset)
        throws IOException, RuntimeException
    {
        long actstreampos = ios.getStreamPosition();
        int actbitoffset = ios.getBitOffset();

        if ((actstreampos != expstreampos) ||
            (actbitoffset != expbitoffset))
        {
            System.err.println("Expected stream position: " + expstreampos +
                               " Actual: " + actstreampos);
            System.err.println("Expected bit offset: " + expbitoffset +
                               " Actual: " + actbitoffset);
            throw new RuntimeException("Test failed.");
        }
    }

    public static void main(String argv[]) throws RuntimeException {
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream mcios = new
            MemoryCacheImageOutputStream(ostream);

        try {
            // verify correct writeBits() functionality
            long streampos = 0;
            int bitoffset = 0;

            mcios.setBitOffset(bitoffset);
            verify(mcios, streampos, bitoffset);

            bitoffset = 3;
            mcios.setBitOffset(bitoffset);
            verify(mcios, streampos, bitoffset);

            for (int incr = 3; incr <= 15; incr += 12) {
                for (int i = 0; i < 64; i += incr) {
                    mcios.writeBits(10, incr);

                    bitoffset += incr;

                    if (bitoffset > 7) {
                        int stroffset = bitoffset / 8;
                        bitoffset = bitoffset % 8;
                        streampos += stroffset;
                    }

                    verify(mcios, streampos, bitoffset);
                }
            }

            // verify correct read(byte[], int, int) functionality
            byte[] bytearr = new byte[2];
            mcios.seek(2);
            mcios.setBitOffset(3);
            int numread = mcios.read(bytearr, 0, 2);
            if (numread != 2) {
                throw new RuntimeException("Error in mcios.read([BII)I");
            }
            verify(mcios, 4, 0);

            // verify correct read() functionality
            mcios.setBitOffset(3);
            mcios.read();
            verify(mcios, 5, 0);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected IOException: " + e);
        }
    }
}
