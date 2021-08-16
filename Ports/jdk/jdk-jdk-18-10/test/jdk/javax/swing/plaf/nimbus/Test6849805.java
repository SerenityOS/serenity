/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 6849805
   @summary Tests NimbusLookAndFeel.deriveColor()
   @author Peter Zhelezniakov
   @run main Test6849805
*/

import java.awt.Color;


public class Test6849805 {

    static boolean pass = true;

    static class Minimbus extends javax.swing.plaf.nimbus.NimbusLookAndFeel {

        public void test(Color c1, Color c2, float f) {
            Color r = getDerivedColor(c1, c2, f);
            Color test = (f > 0 ? c2 : c1);
            System.out.printf("Got %s, need %s ", r, test);

            if (r.getRGB() == test.getRGB() &&
                r.getAlpha() == test.getAlpha()) {

                System.out.println("Ok");
            } else {
                System.out.println("FAIL");
                pass = false;
            }
        }
    }

    public static void main(String[] args) {
        Minimbus laf = new Minimbus();
        laf.test(Color.WHITE, Color.BLACK, 0f);
        laf.test(Color.WHITE, Color.BLACK, 1f);
        laf.test(Color.BLACK, Color.WHITE, 0f);
        laf.test(Color.BLACK, Color.WHITE, 1f);
        laf.test(Color.RED, Color.GREEN, 0f);
        laf.test(Color.RED, Color.GREEN, 1f);
        laf.test(new Color(127, 127, 127), new Color(51, 151, 212), 0f);
        laf.test(new Color(127, 127, 127), new Color(51, 151, 212), 1f);
        laf.test(new Color(221, 63, 189), new Color(112, 200, 89), 0f);
        laf.test(new Color(221, 63, 189), new Color(112, 200, 89), 1f);

        if (! pass) {
            throw new RuntimeException("Some testcases failed, see above");
        }
    }
}
