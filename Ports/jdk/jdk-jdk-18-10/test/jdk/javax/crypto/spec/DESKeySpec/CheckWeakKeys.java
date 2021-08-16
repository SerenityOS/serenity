/*
 * Copyright (c) 2005, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6340919
 * @summary Incorrect list of keys reported as weak by DESKeySpec.isWeak()
 * @author Brad R. Wetmore
 */
import javax.crypto.spec.DESKeySpec;

public class CheckWeakKeys {

    /**
     * Weak/semi-weak keys copied from FIPS 74.
     *
     * "...The first 6 keys have duals different than themselves, hence
     * each is both a key and a dual giving 12 keys with duals. The last
     * four keys equal their duals, and are called self-dual keys..."
     *
     * 1.   E001E001F101F101    01E001E001F101F1
     * 2.   FE1FFE1FFEOEFEOE    1FFE1FFEOEFEOEFE
     * 3.   E01FE01FF10EF10E    1FE01FEOOEF10EF1
     * 4.   01FE01FE01FE01FE    FE01FE01FE01FE01
     * 5.   011F011F010E010E    1F011F010E010E01
     * 6.   E0FEE0FEF1FEF1FE    FEE0FEE0FEF1FEF1
     * 7.   0101010101010101    0101010101010101
     * 8.   FEFEFEFEFEFEFEFE    FEFEFEFEFEFEFEFE
     * 9.   E0E0E0E0F1F1F1F1    E0E0E0E0F1F1F1F1
     * 10.  1F1F1F1F0E0E0E0E    1F1F1F1F0E0E0E0E
     */
    static private byte [][] weakKeys = {
        { (byte)0xE0, (byte)0x01, (byte)0xE0, (byte)0x01,
          (byte)0xF1, (byte)0x01, (byte)0xF1, (byte)0x01 },

        { (byte)0x01, (byte)0xE0, (byte)0x01, (byte)0xE0,
          (byte)0x01, (byte)0xF1, (byte)0x01, (byte)0xF1 },

        { (byte)0xFE, (byte)0x1F, (byte)0xFE, (byte)0x1F,
          (byte)0xFE, (byte)0x0E, (byte)0xFE, (byte)0x0E },

        { (byte)0x1F, (byte)0xFE, (byte)0x1F, (byte)0xFE,
          (byte)0x0E, (byte)0xFE, (byte)0x0E, (byte)0xFE },

        { (byte)0xE0, (byte)0x1F, (byte)0xE0, (byte)0x1F,
          (byte)0xF1, (byte)0x0E, (byte)0xF1, (byte)0x0E },

        { (byte)0x1F, (byte)0xE0, (byte)0x1F, (byte)0xE0,
          (byte)0x0E, (byte)0xF1, (byte)0x0E, (byte)0xF1 },

        { (byte)0x01, (byte)0xFE, (byte)0x01, (byte)0xFE,
          (byte)0x01, (byte)0xFE, (byte)0x01, (byte)0xFE },

        { (byte)0xFE, (byte)0x01, (byte)0xFE, (byte)0x01,
          (byte)0xFE, (byte)0x01, (byte)0xFE, (byte)0x01 },

        { (byte)0x01, (byte)0x1F, (byte)0x01, (byte)0x1F,
          (byte)0x01, (byte)0x0E, (byte)0x01, (byte)0x0E },

        { (byte)0x1F, (byte)0x01, (byte)0x1F, (byte)0x01,
          (byte)0x0E, (byte)0x01, (byte)0x0E, (byte)0x01 },

        { (byte)0xE0, (byte)0xFE, (byte)0xE0, (byte)0xFE,
          (byte)0xF1, (byte)0xFE, (byte)0xF1, (byte)0xFE },

        { (byte)0xFE, (byte)0xE0, (byte)0xFE, (byte)0xE0,
          (byte)0xFE, (byte)0xF1, (byte)0xFE, (byte)0xF1 },

        { (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01,
          (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01 },

        { (byte)0xFE, (byte)0xFE, (byte)0xFE, (byte)0xFE,
          (byte)0xFE, (byte)0xFE, (byte)0xFE, (byte)0xFE },

        { (byte)0xE0, (byte)0xE0, (byte)0xE0, (byte)0xE0,
          (byte)0xF1, (byte)0xF1, (byte)0xF1, (byte)0xF1 },

        { (byte)0x1F, (byte)0x1F, (byte)0x1F, (byte)0x1F,
          (byte)0x0E, (byte)0x0E, (byte)0x0E, (byte)0x0E }
    };

    public static void main(String[] args) throws Exception {

        boolean failed = false;

        for (int i = 0; i < weakKeys.length; i++) {
            DESKeySpec desSpec = new DESKeySpec(weakKeys[i]);
            if (!DESKeySpec.isWeak(weakKeys[i], 0)) {
                failed = true;
                System.out.println("Entry " + i + " should be weak");
            }
        }

        if (failed) {
            throw new Exception("Failed test!!!");
        }

        System.out.println("Passed test.");
    }
}
