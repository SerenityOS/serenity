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
/**
 * @test
 * @bug 8265237
 * @summary tests StringJoiner OOME when joining sub-max-length Strings
 * @modules java.base/jdk.internal.util
 * @requires vm.bits == "64" & os.maxMemory > 4G
 * @run testng/othervm -Xmx4g -XX:+CompactStrings StringJoinerOomUtf16Test
 */

import org.testng.annotations.Test;

import static jdk.internal.util.ArraysSupport.SOFT_MAX_ARRAY_LENGTH;
import static org.testng.Assert.fail;

import java.util.StringJoiner;


@Test(groups = {"unit","string","util","libs"})
public class StringJoinerOomUtf16Test {

    // the sum of lengths of the following two strings is way less than
    // SOFT_MAX_ARRAY_LENGTH, but the byte[] array holding the UTF16 representation
    // would need to be bigger than Integer.MAX_VALUE...
    private static final String HALF_MAX_LATIN1_STRING =
        "*".repeat(SOFT_MAX_ARRAY_LENGTH >> 1);
    private static final String OVERFLOW_UTF16_STRING =
        "\u017D".repeat(((Integer.MAX_VALUE - SOFT_MAX_ARRAY_LENGTH) >> 1) + 1);

    public void OOM1() {
        try {
            new StringJoiner("")
                .add(HALF_MAX_LATIN1_STRING)
                .add(OVERFLOW_UTF16_STRING)
                .toString();
            fail("Should have thrown OutOfMemoryError");
        } catch (OutOfMemoryError ex) {
            System.out.println("Expected: " + ex);
        }
    }

    public void OOM2() {
        try {
            new StringJoiner(HALF_MAX_LATIN1_STRING)
                .add("")
                .add(OVERFLOW_UTF16_STRING)
                .toString();
            fail("Should have thrown OutOfMemoryError");
        } catch (OutOfMemoryError ex) {
            System.out.println("Expected: " + ex);
        }
    }

    public void OOM3() {
        try {
            new StringJoiner(OVERFLOW_UTF16_STRING)
                .add("")
                .add(HALF_MAX_LATIN1_STRING)
                .toString();
            fail("Should have thrown OutOfMemoryError");
        } catch (OutOfMemoryError ex) {
            System.out.println("Expected: " + ex);
        }
    }

    public void OOM4() {
        try {
            new StringJoiner("", HALF_MAX_LATIN1_STRING, OVERFLOW_UTF16_STRING)
                .toString();
            fail("Should have thrown OutOfMemoryError");
        } catch (OutOfMemoryError ex) {
            System.out.println("Expected: " + ex);
        }
    }
}

