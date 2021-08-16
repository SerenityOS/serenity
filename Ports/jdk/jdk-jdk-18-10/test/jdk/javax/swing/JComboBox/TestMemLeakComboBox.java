/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 6542439
 * @summary Verifies memory leak in BasicComboBoxUI and MetalComboBoxButton
 * @run main TestMemLeakComboBox
 */
import java.awt.Graphics;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.CellRendererPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class TestMemLeakComboBox {
    private static JFrame frame;
    private static String failed = null;

    private static void setLookAndFeel(UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
        } catch (UnsupportedLookAndFeelException ignored) {
            System.out.println("Unsupported L&F: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException
                 | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static class MyPanel extends JPanel {
        public void paint(Graphics g) {
            super.paint(g);
            for (Component child : getComponents()) {
                verifyChild(child);
            }
        }

        private void verifyChild(Component c) {
            if (c instanceof JComboBox) {
                for (Component child : ((Container)c).getComponents()) {
                    if (child instanceof CellRendererPane &&
                            ((CellRendererPane)child).getComponentCount() > 0) {
                        failed = new String("CellRendererPane still has children for: " + c);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
            System.out.println("Testing l&f : " + laf.getClassName());
            SwingUtilities.invokeAndWait(() -> setLookAndFeel(laf));
            test();
            if (failed != null) {
                throw new RuntimeException(failed);
            }
        }
    }

    private static void test() throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame();
                JPanel panel = new MyPanel();
                panel.setPreferredSize(new Dimension(100, 100));
                panel.setLayout(new FlowLayout());
                panel.add(new JComboBox(new String[]{"one", "two", "three"}));

                frame.add(panel);
                frame.pack();
                frame.setVisible(true);
            });
        } finally  {
            if (frame != null) {
                SwingUtilities.invokeAndWait(() -> frame.dispose());
            }
        }
    }
}
