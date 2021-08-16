/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @test %I% %E%
 * @bug 6954275
 * @summary Check that UnsyncByteArrayOutputStream does not
 *          throw ArrayIndexOutOfBoundsException
 * @modules java.xml.crypto/com.sun.org.apache.xml.internal.security.utils
 * @compile -XDignore.symbol.file BufferOverflowTest.java
 * @run main BufferOverflowTest
 */

import com.sun.org.apache.xml.internal.security.utils.UnsyncByteArrayOutputStream;

public class BufferOverflowTest {

    public static void main(String[] args) throws Exception {
        try {
            UnsyncByteArrayOutputStream out = new UnsyncByteArrayOutputStream();
            out.write(new byte[(8192) << 2 + 1]);
            System.out.println("PASSED");
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("FAILED, got ArrayIndexOutOfBoundsException");
            throw new Exception(e);
        }
    }
}
