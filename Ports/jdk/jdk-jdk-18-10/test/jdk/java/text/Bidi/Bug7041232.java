/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7041232
 * @summary verify that an unexpected exception isn't thrown for unnatural data to keep backward compatibility with JDK 6.
 */
import java.text.*;

public class Bug7041232 {

    public static void main(String[] args) {
        String UnicodeChars;
        StringBuffer sb = new StringBuffer();

        // Generates String which includes U+2028(line separator) and
        // U+2029(paragraph separator)
        for (int i = 0x2000; i < 0x2100; i++) {
            sb.append((char)i);
        }
        UnicodeChars = sb.toString();

        Bidi bidi = new Bidi(UnicodeChars, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
        bidi.createLineBidi(0, UnicodeChars.length());
    }

}
