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
/*
 * @test
 * @bug 8032446
 * @summary Confirm that BreakIterator works as expected with new characters in Unicode 7.
 */

import java.text.*;
import java.util.*;

public class Bug8032446 {

    public static void main(String[] args) {
        boolean err = false;

        StringBuilder sb = new StringBuilder();
        for (int i = 0x10860; i <= 0x10876; i++) { // Palmyrene Letters
            sb.append(Character.toChars(i));
        }
        sb.append(" ");
        for (int i = 0x10879; i <= 0x1087D; i++) { // Palmyrene Numbers
            sb.append(Character.toChars(i));
        }
        String s = sb.toString();

        BreakIterator bi = BreakIterator.getWordInstance(Locale.ROOT);
        bi.setText(s);
        bi.first();

        if (bi.next() != s.indexOf(' ')) {
            throw new RuntimeException("Unexpected word breaking.");
        }
    }

}
