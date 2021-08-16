/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7051769 8038092 8174270
 * @summary verify that Bidi.toString() returns the corect result.
 *     The second run is intended to test lazy SharedSectets init for 8038092
 * @modules java.desktop
 * @run main Bug7051769
 * @run main/othervm -DpreloadBidi=true Bug7051769
 */
import java.awt.font.*;
import java.text.*;
import java.util.*;

public class Bug7051769 {

    static {
        if (System.getProperty("preloadBidi", "").equals("true")) {
            // Make sure the SharedSecret is lazily initialized correctly
            try {
                Class.forName("jdk.internal.icu.text.BidiBase");
                System.out.println("BidiBase class has been pre-loaded.");
            } catch (ClassNotFoundException e) {
                System.out.println("BidiBase class could not be pre-loaded.");
            }
        }
    }

    private static boolean err = false;

    public static void main(String[] args) {
        testNumericShaping();

        if (err) {
            throw new RuntimeException("Failed");
        } else {
            System.out.println("Passed.");
        }
    }

    private static void testNumericShaping() {
        Map attrNS = new HashMap();
        attrNS.put(TextAttribute.NUMERIC_SHAPING,
                   NumericShaper.getContextualShaper(NumericShaper.ARABIC));
        attrNS.put(TextAttribute.RUN_DIRECTION,
                   TextAttribute.RUN_DIRECTION_RTL);

        String text = "\u0623\u0643\u062a\u0648\u0628\u0631 10";
        String expected = "jdk.internal.icu.text.BidiBase[dir: 2 baselevel: 1 length: 9 runs: [1 1 1 1 1 1 1 2 2] text: [0x623 0x643 0x62a 0x648 0x628 0x631 0x20 0x661 0x660]]";

        AttributedString as = new AttributedString(text, attrNS);
        AttributedCharacterIterator itr = as.getIterator();
        itr.last();
        itr.next();
        Bidi bidi = new Bidi(itr);
        String got = bidi.toString();

        if (!got.equals(expected)) {
            err = true;
            System.err.println("Wrong toString() output: " +
                               "\n\tExpected=" + expected +
                               "\n\tGot=" + got);
        }
    }

}
