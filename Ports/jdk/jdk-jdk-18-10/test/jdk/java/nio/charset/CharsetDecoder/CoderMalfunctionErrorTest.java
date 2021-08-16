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

/* @test
 * @bug 8253832
 * @run testng CoderMalfunctionErrorTest
 * @summary Check CoderMalfunctionError is thrown for any RuntimeException
 *      on CharsetDecoder.decodeLoop() invocation.
 */

import org.testng.annotations.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

@Test
public class CoderMalfunctionErrorTest {
    @Test (expectedExceptions = CoderMalfunctionError.class)
    public void testDecodeLoop() {
        new CharsetDecoder(StandardCharsets.US_ASCII, 1, 1) {
            @Override
            protected CoderResult decodeLoop(ByteBuffer byteBuffer, CharBuffer charBuffer) {
                throw new RuntimeException("This exception should be wrapped in CoderMalfunctionError");
            }
        }.decode(null, null, true);
    }
}
