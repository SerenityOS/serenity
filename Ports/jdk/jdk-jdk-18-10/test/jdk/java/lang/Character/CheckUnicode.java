/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug      4114080 6565620 6959267 7070436 7198195 8032446 8072600 8221431
 * @summary  Make sure the attributes of Unicode characters, as
 * returned by the Character API, are as expected.  Do this by
 * comparing them to a baseline file together with a list of
 * known diffs.
 * @library /lib/testlibrary/java/lang
 * @build UnicodeSpec CharCheck
 * @run main CheckUnicode
 * @author Alan Liu
 * @author John O'Conner
 */

import java.io.*;

public class CheckUnicode {
    public static void main(String args[]) throws Exception {

        // 1. Check that the current 12.1 spec file is handled by the current
        // version of Character.
        File unicodeSpec = UCDFiles.UNICODE_DATA.toFile();
        for (int x = 0; x < 16; ++x) {
            int diffs = CharCheck.check(x, unicodeSpec);
            if (diffs != 0) {
                throw new RuntimeException("Unicode properties have changed " +
                                           "in an unexpected way");
            }
        }

        // 2. Check that Java identifiers are recognized correctly.
        // test a few characters that are good id starts
        char[] idStartChar = {'$', '\u20AC', 'a', 'A', 'z', 'Z', '_', '\u0E3F',
            '\u1004', '\u10A0', '\u3400', '\u4E00', '\uAC00' };
        for (int x = 0; x < idStartChar.length; x++) {
            if (Character.isJavaIdentifierStart(idStartChar[x]) != true) {
                throw new RuntimeException("Java id start characters are not recognized.");
            }
        }

        // test a few characters that are good id parts
        char[] idPartChar = {'0', '9', '\u0000', '\u0008', '\u000E', '\u007F'};
        for (int x=0; x< idStartChar.length; x++) {
            if (Character.isJavaIdentifierPart(idStartChar[x]) != true) {
                throw new RuntimeException("Java id part characters are not recognized.");
            }
        }
        for (int x=0; x<idPartChar.length; x++) {
            if (Character.isJavaIdentifierPart(idPartChar[x]) != true) {
                throw new RuntimeException("Java id part characters are not recognized.");
            }
        }

        // now do some negative checks
        for (int x=0; x< idPartChar.length; x++) {
            if (Character.isJavaIdentifierStart(idPartChar[x]) != false) {
                throw new RuntimeException("These Java id part characters" +
                    "should not be start characters.");
            }
        }
    }
}
