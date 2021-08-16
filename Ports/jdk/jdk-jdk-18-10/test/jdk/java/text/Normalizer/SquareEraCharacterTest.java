/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8221431
 * @summary Tests decomposition of Japanese square era characters.
 * @run testng/othervm SquareEraCharacterTest
 */

import static org.testng.Assert.assertEquals;

import java.text.Normalizer;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public class SquareEraCharacterTest {

    @DataProvider
    Object[][] squareEras() {
        return new Object[][] {

            // square era character, expected decomposed string
            {'\u337e',  "\u660e\u6cbb"},    // Meizi
            {'\u337d',  "\u5927\u6b63"},    // Taisho
            {'\u337c',  "\u662d\u548c"},    // Showa
            {'\u337b',  "\u5e73\u6210"},    // Heisei
            {'\u32ff',  "\u4ee4\u548c"},    // Reiwa
        };
    }

    @Test(dataProvider="squareEras")
    public void test_normalize(char squareChar, String expected) {

        assertEquals(
            Normalizer.normalize(Character.toString(squareChar), Normalizer.Form.NFKD),
            expected,
            "decomposing " + Character.getName(squareChar) + ".");
    }
}
