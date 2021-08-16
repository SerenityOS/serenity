/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 4370469
  @summary tests that selectBestTextFlavor doesn't throw NPE
  @author prs@sparc.spb.su: area=
  @modules java.datatransfer
  @run main SelectBestFlavorNPETest
*/

import java.awt.datatransfer.DataFlavor;

public class SelectBestFlavorNPETest {

    public static void main(String[] args) {

        DataFlavor flavor1 = new DataFlavor("text/plain; charset=unicode; class=java.io.InputStream",
                "Flavor 1");
        DataFlavor flavor2 = new DataFlavor("text/plain; class=java.io.InputStream", "Flavor 2");
        DataFlavor[] flavors = new DataFlavor[]{flavor1, flavor2};
        try {
            DataFlavor best = DataFlavor.selectBestTextFlavor(flavors);
            System.out.println("best=" + best);
        } catch (NullPointerException e1) {
            throw new RuntimeException("Test FAILED because of NPE in selectBestTextFlavor");
        }
    }

}
