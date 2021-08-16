/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.Robot;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * @test
 * @key headful
 * @bug 8017284
 * @author Alexander Scherbatiy
 * @summary  Aqua LaF: memory leak when HTML is used for JTabbedPane tab titles
 * @run main/othervm/timeout=60 -Xmx128m bug8017284
 */
public class bug8017284 {

    private static final int TAB_COUNT = 100;
    private static final int ITERATIONS = 100;
    private static JFrame frame;
    private static JTabbedPane tabbedPane;

    public static void main(String[] args) throws Exception {

        Robot robot = new Robot();
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame();
            frame.setSize(500, 500);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            tabbedPane = new JTabbedPane();

            for (int i = 0; i < TAB_COUNT; i++) {
                tabbedPane.add("Header " + i, new JLabel("Content: " + i));
            }

            frame.getContentPane().setLayout(new BorderLayout());
            frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
            frame.setVisible(true);
        });

        robot.waitForIdle();

        SwingUtilities.invokeAndWait(() -> {
            for (int j = 0; j < ITERATIONS; j++) {
                for (int i = 0; i < TAB_COUNT; i++) {
                    tabbedPane.setTitleAt(i, getHtmlText(j * TAB_COUNT + i));
                }
            }
        });
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(() -> frame.dispose());
    }

    private static String getHtmlText(int i) {
        return "<html><b><i>" + i + "</b></i>";
    }
}
