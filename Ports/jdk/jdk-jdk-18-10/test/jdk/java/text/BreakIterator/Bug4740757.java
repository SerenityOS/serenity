/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4740757
 * @summary Confirm line-breaking behavior of Hangul
 */

import java.text.*;
import java.util.*;

public class Bug4740757 {

    private static boolean err = false;

    public static void main(String[] args) {
        Locale defaultLocale = Locale.getDefault();
        if (defaultLocale.getLanguage().equals("th")) {
            Locale.setDefault(Locale.KOREA);
            test4740757();
            Locale.setDefault(defaultLocale);
        } else {
            test4740757();
        }

        if (err) {
            throw new RuntimeException("Incorrect Line-breaking");
        }
    }

    private static void test4740757() {
        String source = "\uc548\ub155\ud558\uc138\uc694? \uc88b\uc740 \uc544\uce68, \uc5ec\ubcf4\uc138\uc694! \uc548\ub155. End.";
        String expected = "\uc548/\ub155/\ud558/\uc138/\uc694? /\uc88b/\uc740 /\uc544/\uce68, /\uc5ec/\ubcf4/\uc138/\uc694! /\uc548/\ub155. /End./";

        BreakIterator bi = BreakIterator.getLineInstance(Locale.KOREAN);
        bi.setText(source);
        int start = bi.first();
        int end = bi.next();
        StringBuilder sb =  new StringBuilder();

        for (; end != BreakIterator.DONE; start = end, end = bi.next()) {
            sb.append(source.substring(start,end));
            sb.append('/');
        }

        if (!expected.equals(sb.toString())) {
            System.err.println("Failed: Hangul line-breaking failed." +
                "\n\tExpected: " + expected +
                "\n\tGot:      " + sb +
                "\nin " + Locale.getDefault() + " locale.");
            err = true;
        }
    }

}
