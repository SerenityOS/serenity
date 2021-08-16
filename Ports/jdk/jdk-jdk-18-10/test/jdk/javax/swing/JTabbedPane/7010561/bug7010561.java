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

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.synth.SynthLookAndFeel;
import java.lang.reflect.Method;

/* @test
   @bug 7010561
   @summary Tab text position with Synth based LaF is different to Java 5/6
   @modules java.desktop/javax.swing.plaf.basic:open
   @author Pavel Porvatov
*/
public class bug7010561 {
    private static int[] TAB_PLACEMENT = {
            SwingConstants.BOTTOM,
            SwingConstants.BOTTOM,
            SwingConstants.TOP,
            SwingConstants.TOP,

    };

    private static boolean[] IS_SELECTED = {
            false,
            true,
            false,
            true
    };

    private static int[] RETURN_VALUE = {
            -1,
            1,
            1,
            -1
    };

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new SynthLookAndFeel());

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JTabbedPane tabbedPane = new JTabbedPane();

                tabbedPane.addTab("Tab 1", new JLabel("Tab 1"));

                // Ensure internal TabbedPane fields are initialized
                tabbedPane.doLayout();

                BasicTabbedPaneUI basicTabbedPaneUI = (BasicTabbedPaneUI) tabbedPane.getUI();

                try {
                    Method method = BasicTabbedPaneUI.class.getDeclaredMethod("getTabLabelShiftY", int.class,
                            int.class, boolean.class);

                    method.setAccessible(true);

                    for (int i = 0; i < 4; i++) {
                        int res = ((Integer) method.invoke(basicTabbedPaneUI, TAB_PLACEMENT[i], 0,
                                IS_SELECTED[i])).intValue();

                        if (res != RETURN_VALUE[i]) {
                            throw new RuntimeException("Test bug7010561 failed on index " + i);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                System.out.println("Test bug7010561 passed");
            }
        });
    }
}
