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
 * @bug 4431503
 * @summary Checks if flushBefore(pos) throws an IndexOutOfBoundsException if
 *          pos lies in the flushed portion of the stream
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.stream.FileCacheImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class FlushBefore {

    public static void main(String[] args) throws IOException {
        OutputStream ostream = new ByteArrayOutputStream();

        FileCacheImageOutputStream fcios =
            new FileCacheImageOutputStream(ostream, null);
        test(fcios);

        MemoryCacheImageOutputStream mcios =
            new MemoryCacheImageOutputStream(ostream);
        test(mcios);
    }

    private static void test(ImageOutputStream ios) throws IOException {
        try {
            ios.write(new byte[10], 0, 10);
            ios.flushBefore(5);
            ios.flushBefore(4);

            throw new RuntimeException
                ("Failed to get IndexOutOfBoundsException!");
        } catch (IndexOutOfBoundsException e) {
        }
    }
}
