/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6632810
 * @summary javax.swing.plaf.basic.BasicScrollPaneUI.getBaseline(JComponent, int, int) doesn't throw NPE and IAE
 * @author Pavel Porvatov
 */

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollPaneUI;

public class Test6632810 {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                BasicScrollPaneUI ui = new BasicScrollPaneUI();

                ui.installUI(new JScrollPane());

                try {
                    ui.getBaseline(null, 1, 1);

                    throw new RuntimeException("getBaseline(null, 1, 1) does not throw NPE");
                } catch (NullPointerException e) {
                    // Ok
                }

                int[][] illegelParams = new int[][]{
                        {-1, 1,},
                        {1, -1,},
                        {-1, -1,},
                };

                for (int[] illegelParam : illegelParams) {
                    try {
                        int width = illegelParam[0];
                        int height = illegelParam[1];

                        ui.getBaseline(new JScrollPane(), width, height);

                        throw new RuntimeException("getBaseline(new JScrollPane(), " + width + ", " + height +
                                ") does not throw IAE");
                    } catch (IllegalArgumentException e) {
                        // Ok
                    }
                }
            }
        });
    }
}
