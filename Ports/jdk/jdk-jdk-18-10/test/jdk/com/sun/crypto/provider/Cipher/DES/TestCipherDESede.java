/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048601
 * @library ../
 * @summary Test DES/DESede cipher with different MODES and padding
 */

public class TestCipherDESede extends TestCipher {

    TestCipherDESede(String[] modes, String[] paddings) {
        super("DESede", modes, paddings);
    }

    public static void main(String[] args) throws Exception {
        new TestCipherDESede(
            new String[]{ "CBC", "ECB", "PCBC",
                //CFBx
                "CFB", "CFB8", "CFB16", "CFB24", "CFB32", "CFB40",
                "CFB48", "CFB56", "CFB64",
                //OFBx
                "OFB", "OFB8", "OFB16", "OFB24", "OFB32", "OFB40",
                "OFB48", "OFB56", "OFB64"},
                new String[]{ "NoPaDDing", "PKCS5Padding" }).runAll();
        new TestCipherDESede(
            new String[]{ "CTR", "CTS" },
            new String[]{ "NoPaDDing" }).runAll();
    }
}
