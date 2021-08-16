/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8178430
 * @summary JMenu in GridBagLayout flickers when label text shows "..." and
 * is updated
 * @key headful
 * @run main LabelDotTest
 */

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Robot;

import java.util.stream.IntStream;

import javax.swing.SwingUtilities;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class LabelDotTest
{
    private final static String longText = "show a very long text to have it " +
            "automatically shortened";
    private final static String shortText = "show short text";

    private static JFrame frame;
    private static JLabel label;
    private static JMenu menu;
    private static volatile boolean isException = false;

    private static void createUI() {
       System.out.println("BEFORE CREATION");
       frame = new JFrame();
       frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
       frame.setSize(new Dimension(50, 150));
       frame.setLocationRelativeTo(null);

       frame.setLayout(new GridBagLayout());
       GridBagConstraints c = new GridBagConstraints();
       c.fill = GridBagConstraints.BOTH;
       c.weightx = 1.0;
       c.weighty = 0.0;
       c.gridwidth = GridBagConstraints.REMAINDER;

       JMenuBar menuBar = new JMenuBar();
       menu = new JMenu("Menu");
       menuBar.add(menu);
       frame.add(menuBar, c);

       frame.add(new JLabel("Title", SwingConstants.CENTER), c);

       c.weighty = 1.0;
       frame.add(new JPanel(new GridBagLayout()), c);
       c.weighty = 0.0;

       label = new JLabel(shortText);
       frame.add(label, c);

       frame.setVisible(true);
   }

   private static void runTest(int iterations) throws Exception{
        Robot robot = new Robot();

        IntStream.range(0, iterations).forEach((i) -> {
                SwingUtilities.invokeLater(() -> {
                    if (label.getText().equals(shortText)) {
                        label.setText(longText);
                    } else {
                        label.setText(shortText);
                    }
                    /*  For a top level menu item, minimum size and the
                        preferred size should be the same, and should not be
                        equal to 1. Save the exception state and throw later
                        once the iterations are completed.
                    */
                    isException = (menu.getMinimumSize().height == 1 &&
                        !menu.getMinimumSize().equals(menu.getPreferredSize())) ||
                        isException;
                });
                robot.waitForIdle();
        });
   }

   public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> createUI());
            runTest(50);
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(() -> frame.dispose());
            }
            if (isException)
                throw new RuntimeException("Size of Menu bar is not correct.");
        }
   }
}
