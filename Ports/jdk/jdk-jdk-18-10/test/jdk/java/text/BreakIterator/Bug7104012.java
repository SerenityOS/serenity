/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7104012
 * @summary Confirm that AIOBE is not thrown.
 */

import java.text.*;
import java.util.*;

public class Bug7104012 {

    public static void main(String[] args) {
        boolean err = false;

        List<String> data = new ArrayList<>();
        data.add("\udb40");
        data.add(" \udb40");
        data.add("\udc53");
        data.add(" \udc53");
        data.add(" \udb40\udc53");
        data.add("\udb40\udc53");
        data.add("ABC \udb40\udc53 123");
        data.add("\udb40\udc53 ABC \udb40\udc53");

        for (Locale locale : Locale.getAvailableLocales()) {
            List<BreakIterator> breakIterators = new ArrayList<>();
            breakIterators.add(BreakIterator.getCharacterInstance(locale));
            breakIterators.add(BreakIterator.getLineInstance(locale));
            breakIterators.add(BreakIterator.getSentenceInstance(locale));
            breakIterators.add(BreakIterator.getWordInstance(locale));

            for (BreakIterator bi : breakIterators) {
                for (String str : data) {
                    try {
                        bi.setText(str);
                        bi.first();
                        while (bi.next() != BreakIterator.DONE) { }
                        bi.last();
                        while (bi.previous() != BreakIterator.DONE) { }
                    }
                    catch (ArrayIndexOutOfBoundsException ex) {
                        System.out.println("    " + data.indexOf(str)
                            + ": BreakIterator(" + locale
                            + ") threw AIOBE.");
                        err = true;
                    }
                }
            }
        }

        if (err) {
            throw new RuntimeException("Unexpected exeption.");
        }
    }

}
