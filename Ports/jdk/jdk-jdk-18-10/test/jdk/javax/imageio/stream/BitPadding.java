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
 * @bug 4430395
 * @summary Checks if write(int) properly pads unwritten bits with zeros
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.stream.FileCacheImageOutputStream;

public class BitPadding {

    public static void main(String[] args) throws IOException {
        OutputStream ostream = new ByteArrayOutputStream();
        File f = null;
        FileCacheImageOutputStream fcios =
            new FileCacheImageOutputStream(ostream, f);
        fcios.writeBit(1);
        fcios.write(96);

        fcios.seek(0);
        int r1 = fcios.read();
        if (r1 != 128 ) {
            throw new RuntimeException("Failed, first byte is " + r1);
        }

        int r2 = fcios.read();
        if (r2 != 96) {
            throw new RuntimeException("Failed, second byte is " + r2);
        }
    }
}
