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

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import static javax.swing.UIManager.getInstalledLookAndFeels;

/**
 * @test
 * @key headful
 * @bug 6368321
 * @author Sergey Bylokhov
 */
public final class SilenceOfDeprecatedMenuBar implements Runnable {

    public static void main(final String[] args) throws Exception {
        for (final UIManager.LookAndFeelInfo laf : getInstalledLookAndFeels()) {
            SwingUtilities.invokeAndWait(() -> setLookAndFeel(laf));
            SwingUtilities.invokeAndWait(new SilenceOfDeprecatedMenuBar());
        }
    }

    @Override
    public void run() {
        final JFrame frame = new DeprecatedFrame();
        try {
            final JMenuBar bar = new JMenuBar();
            frame.setJMenuBar(bar);
            frame.setBounds(100, 100, 100, 100);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            if (bar != frame.getJMenuBar()) {
                throw new RuntimeException("Wrong JMenuBar");
            }
        } finally {
            frame.dispose();
        }
    }

    private static void setLookAndFeel(final UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
            System.out.println("LookAndFeel: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                UnsupportedLookAndFeelException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static class DeprecatedFrame extends JFrame {

        @Override
        protected JRootPane createRootPane() {
            return new JRootPane() {
                @Override
                public JMenuBar getMenuBar() {
                    throw new RuntimeException("Should not be here");
                }
                @Override
                public void setMenuBar(final JMenuBar menu) {
                    throw new RuntimeException("Should not be here");
                }
            };
        }
    }
}
