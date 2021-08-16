/*
 * Copyright (c) 2000, 2001, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4400571
 * @summary Verify that ObjectInputStream.available() functions properly when
 *          called at the beginning of a data block, and that it does not
 *          attempt any read operations that may potentially block.
 */

import java.io.*;

public class Available {
    public static void main(String[] args) throws Exception {
        ObjectOutputStream oout;
        ByteArrayOutputStream bout;
        byte[] buf;

        // write stream containing short data block
        oout = new ObjectOutputStream(bout = new ByteArrayOutputStream());
        oout.write(new byte[100]);
        oout.close();
        buf = bout.toByteArray();

        // 4 byte stream header + 2 byte block header + data
        if ((getAvailable(buf, 4) != 0) || (getAvailable(buf, 5) != 0)) {
            throw new Error();
        }
        for (int i = 0; i < 100; i++) {
            if (getAvailable(buf, 6 + i) != i) {
                throw new Error();
            }
        }

        // write stream containing long data block
        oout = new ObjectOutputStream(bout = new ByteArrayOutputStream());
        oout.write(new byte[500]);
        oout.close();
        buf = bout.toByteArray();

        // 4 byte stream header + 5 byte block header + data
        for (int i = 4; i < 9; i++) {
            if (getAvailable(buf, i) != 0) {
                throw new Error();
            }
        }
        for (int i = 0; i < 500; i++) {
            if (getAvailable(buf, 9 + i) != i) {
                throw new Error();
            }
        }
    }

    /**
     * Given a byte array containing a serialized stream, creates a copy of the
     * given data truncated to the specified length, then returns the result of
     * a call to available() on an ObjectInputStream created on top of the
     * truncated data.  As a side effect, a StreamCorrupted or EOFException
     * will get thrown if the available() call attempts to read past the
     * underlying stream's available data.
     */
    static int getAvailable(byte[] data, int truncateLen) throws IOException {
        byte[] trunc = new byte[truncateLen];
        System.arraycopy(data, 0, trunc, 0, truncateLen);
        return new ObjectInputStream(
            new ByteArrayInputStream(trunc)).available();
    }
}
