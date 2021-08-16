/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7082443
 * @summary JComboBox not backward compatible (with Java 6)
 * @author Pavel Porvatov
 */

import javax.swing.*;
import java.awt.*;

public class bug7082443 {
    public static final String GTK_LAF_CLASS = "GTKLookAndFeel";

    public static void main(String[] args) throws Exception {
        for (UIManager.LookAndFeelInfo lookAndFeelInfo : UIManager.getInstalledLookAndFeels()) {
            if (lookAndFeelInfo.getClassName().contains(GTK_LAF_CLASS)) {
                try {
                    UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
                } catch (final UnsupportedLookAndFeelException ignored) {
                    continue;
                }
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        TestComboBox testComboBox = new TestComboBox();

                        if (testComboBox.isOldRendererOpaque()) {
                            System.out.println("Passed for " + GTK_LAF_CLASS);
                        } else {
                            throw new RuntimeException("Failed for " + GTK_LAF_CLASS);
                        }
                    }
                });

                return;
            }
        }

        System.out.println(GTK_LAF_CLASS + " is not found. The test skipped");
    }

    private static class TestComboBox extends JComboBox {
        private final ListCellRenderer renderer = new ListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                return TestComboBox.super.getRenderer().getListCellRendererComponent(list, value, index,
                        isSelected, cellHasFocus);
            }
        };

        @Override
        public ListCellRenderer getRenderer() {
            return renderer;
        }

        public boolean isOldRendererOpaque() {
            return ((JLabel) super.getRenderer()).isOpaque();
        }
    }
}

