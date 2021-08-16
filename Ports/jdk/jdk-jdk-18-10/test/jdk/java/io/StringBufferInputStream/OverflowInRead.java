/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8163518
 * @summary Integer overflow when reading in large buffer
 * @requires (sun.arch.data.model == "64" & os.maxMemory > 4g)
 * @run main/othervm -Xmx4g OverflowInRead
 */

import java.io.StringBufferInputStream;

public class OverflowInRead {
    public static void main(String[] args) throws Exception {
        String s = "_123456789_123456789_123456789_123456789"; // s.length() > 33
        try (StringBufferInputStream sbis = new StringBufferInputStream(s)) {
            int len1 = 33;
            byte[] buf1 = new byte[len1];
            if (sbis.read(buf1, 0, len1) != len1)
                throw new Exception("Expected to read " + len1 + " bytes");

            int len2 = Integer.MAX_VALUE - 32;
            byte[] buf2 = new byte[len2];
            int expLen2 = s.length() - len1;
            if (sbis.read(buf2, 0, len2) != expLen2)
                throw new Exception("Expected to read " + expLen2 + " bytes");
        }
    }
}
