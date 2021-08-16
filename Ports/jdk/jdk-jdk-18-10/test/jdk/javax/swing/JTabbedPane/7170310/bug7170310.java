/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;


/**
 * @test
 * @key headful
 * @bug 7170310
 * @author Alexey Ivanov
 * @summary Selected tab should be scrolled into view.
 * @library /lib/client/
 * @build ExtendedRobot
 * @run main bug7170310
 */

public class bug7170310 {
    private static final int TABS_NUMBER = 3;

    private static volatile JTabbedPane tabbedPane;
    private static volatile int count = 1;

    private static volatile JFrame frame;

    private static volatile Exception exception = null;

    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
            SwingUtilities.invokeAndWait(bug7170310::createAndShowUI);

            sync();

            for (int i = 0; i < TABS_NUMBER; i++) {
                SwingUtilities.invokeAndWait(bug7170310::addTab);
                sync();
            }

            SwingUtilities.invokeAndWait(bug7170310::check);

            if (exception != null) {
                System.out.println("Test failed: " + exception.getMessage());
                throw exception;
            } else {
                System.out.printf("Test passed");
            }
        } finally {
            if (frame != null) { frame.dispose(); }
        }
    }

    private static void createAndShowUI() {
        frame = new JFrame("bug7170310");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(200, 100);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Main Tab", new JPanel());

        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        frame.getContentPane().add(tabbedPane);
        frame.setVisible(true);
    }

    private static void addTab() {
        tabbedPane.addTab("Added Tab " + count++, new JPanel());
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }

    private static void check() {
        try {
            JViewport vp = null;
            for (Component c : tabbedPane.getComponents()) {
                if (c instanceof JViewport) {
                    vp = (JViewport) c;
                    break;
                }
            }

            JComponent v = (JComponent) vp.getView();
            Rectangle vr = vp.getViewRect();
            Dimension vs = v.getSize();

            // The tab view must be scrolled to the end so that the last tab is visible
            if (vs.width != (vr.x + vr.width)) {
                throw new RuntimeException("tabScroller.tabPanel view is positioned incorrectly: "
                        + vs.width + " vs " + (vr.x + vr.width));
            }
        } catch (Exception e) {
            exception = e;
        }
    }
    private static void sync() {
        try {
             ExtendedRobot robot = new ExtendedRobot();
             robot.waitForIdle(300);
         }catch(Exception ex) {
             ex.printStackTrace();
             throw new Error("Unexpected Failure");
         }
    }
}
