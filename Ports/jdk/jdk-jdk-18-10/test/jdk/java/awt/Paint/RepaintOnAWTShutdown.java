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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.SwingUtilities;

/**
 * @test
 * @bug 6847157
 * @key headful
 * @summary the java2D/AWT should die silently without exceptions
 * @run main/othervm                           RepaintOnAWTShutdown
 * @run main/othervm -Dsun.java2d.uiScale=1    RepaintOnAWTShutdown
 * @run main/othervm -Dsun.java2d.uiScale=1.2  RepaintOnAWTShutdown
 * @run main/othervm -Dsun.java2d.uiScale=1.25 RepaintOnAWTShutdown
 * @run main/othervm -Dsun.java2d.uiScale=1.5  RepaintOnAWTShutdown
 * @run main/othervm -Dsun.java2d.uiScale=1.75 RepaintOnAWTShutdown
 * @run main/othervm -Dsun.java2d.uiScale=2    RepaintOnAWTShutdown
 * @run main/othervm -Dsun.java2d.uiScale=2.25 RepaintOnAWTShutdown
 * @run main/othervm -Dsun.java2d.uiScale=5    RepaintOnAWTShutdown
 * @run main/othervm -Dsun.java2d.uiScale=10   RepaintOnAWTShutdown
 */
public final class RepaintOnAWTShutdown implements Runnable {

    private static final CountDownLatch go = new CountDownLatch(1);

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(new RepaintOnAWTShutdown());
        go.await(5, TimeUnit.SECONDS);
        // The test will check that no exception is thrown when the jtreg will
        // kill this test at the moment the frame will be painted
    }

    public void run() {
        JFrame frame = new JFrame();
        JPanel panel = new MyPanel();
        panel.setPreferredSize(new Dimension(100, 100));
        panel.setLayout(new FlowLayout());
        panel.add(new JTree());
        panel.add(new JList(new String[]{"one", "two"}));
        panel.add(new JTable(new String[][]{{"one", "two"}},
                             new String[]{"one", "two"}));
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        // the frame is not disposed intentionally
    }


    private final class MyPanel extends JPanel {

        public void paint(Graphics g) {
            super.paint(g);
            go.countDown();
        }
    }
}
