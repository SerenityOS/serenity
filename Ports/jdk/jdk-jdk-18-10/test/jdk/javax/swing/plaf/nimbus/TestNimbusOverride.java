/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8043315
 * @summary  Verifies if setting Nimbus.Overrides property affects
 *           keymap installation
 * @key headful
 * @run main TestNimbusOverride
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.text.Keymap;


public class TestNimbusOverride extends JFrame
{
    private static TestNimbusOverride tf;
    private static boolean passed = false;

    public static void main(String [] args) throws Exception {
        try {
            Robot robot = new Robot();
            SwingUtilities.invokeAndWait(() -> {
                try {
                    UIManager.setLookAndFeel(
                            "javax.swing.plaf.nimbus.NimbusLookAndFeel");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                tf = new TestNimbusOverride();
                tf.pack();
                tf.setVisible(true);
            });
            robot.setAutoDelay(100);
            robot.waitForIdle();
            robot.keyPress(KeyEvent.VK_SPACE);
            robot.keyRelease(KeyEvent.VK_SPACE);
            robot.waitForIdle();
            if (!passed) {
                throw new RuntimeException(
                        "Setting Nimbus.Overrides property affects custom" +
                                " keymap installation");
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> tf.dispose());
        }
    }

    public TestNimbusOverride()
    {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        /*
         * Create a frame containing a JEditorPane, and override the action for
         * the space bar to show a dialog.
         */
        JEditorPane pp = new JEditorPane();
        UIDefaults defaults = new UIDefaults();

        pp.putClientProperty("Nimbus.Overrides", defaults);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        setContentPane(contentPanel);

        contentPanel.setPreferredSize(new Dimension(400, 300));
        contentPanel.add(pp, BorderLayout.CENTER);

        Keymap origKeymap = pp.getKeymap();
        Keymap km = JEditorPane.addKeymap("Test keymap", origKeymap);

        km.addActionForKeyStroke(KeyStroke.getKeyStroke(' '),
                new AbstractAction("SHOW_SPACE") {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                passed = true;
            }
        });

        pp.setKeymap(km);
    }
}
