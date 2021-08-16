/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/*
 * @test
 * @bug 8077559
 * @summary Tests Compact String. This one is for String.trim.
 * @run testng/othervm -XX:+CompactStrings Trim
 * @run testng/othervm -XX:-CompactStrings Trim
 */

public class Trim {

    /*
     * Data provider for testTrim
     *
     * @return input parameter for testTrim
     */
    @DataProvider
    public Object[][] trims() {
        return new Object[][] {
                { " \t \t".trim(), "" },
                { "\t \t ".trim(), "" },
                { "\t A B C\t ".trim(), "A B C" },
                { " \t A B C \t".trim(), "A B C" },
                { "\t \uFF21 \uFF22 \uFF23\t ".trim(), "\uFF21 \uFF22 \uFF23" },
                { " \t \uFF21 \uFF22 \uFF23 \t".trim(), "\uFF21 \uFF22 \uFF23" },
                { " \t \uFF41 \uFF42 \uFF43 \t".trim(), "\uFF41 \uFF42 \uFF43" },
                { " \t A\uFF21 B\uFF22 C\uFF23 \t".trim(),
                        "A\uFF21 B\uFF22 C\uFF23" } };
    }

    /*
     * test trim().
     *
     * @param res
     *            real result
     * @param expected
     *            expected result
     */
    @Test(dataProvider = "trims")
    public void testTrim(String res, String expected) {
        assertEquals(res, expected);
    }

}
