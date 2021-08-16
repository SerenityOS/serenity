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

/* @test
 * @bug 6545054
 * @summary Channels.newInputStream.read throws IAE when invoked with
 *          different offsets.
 */

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;
import java.io.InputStream;
import java.io.IOException;

public class ReadOffset {
    public static void main(String[] args) throws IOException {
        ReadableByteChannel rbc = new ReadableByteChannel() {
            public int read(ByteBuffer dst) {
                dst.put((byte)0);
                return 1;
            }
            public boolean isOpen() {
                return true;
            }
            public void close() {
            }
        };

        InputStream in = Channels.newInputStream(rbc);

        byte[] b = new byte[3];
        in.read(b, 0, 1);
        in.read(b, 2, 1);       // throws IAE
    }
}
