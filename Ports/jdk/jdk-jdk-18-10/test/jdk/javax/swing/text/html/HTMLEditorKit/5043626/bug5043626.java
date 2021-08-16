/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 5043626
 * @summary  Tests pressing Home or Ctrl+Home set cursor to invisible element <head>
 * @run main bug5043626
 */

import java.awt.Robot;
import java.awt.Toolkit;
import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import java.awt.event.KeyEvent;

public class bug5043626 {

    private static Document doc;
    private static Robot robot;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            robot = new Robot();
            robot.setAutoDelay(100);

            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createAndShowGUI();
                }
            });

            robot.waitForIdle();
            robot.delay(1000);

            robot.keyPress(KeyEvent.VK_HOME);
            robot.keyRelease(KeyEvent.VK_HOME);
            robot.keyPress(KeyEvent.VK_1);
            robot.keyRelease(KeyEvent.VK_1);

            robot.waitForIdle();

            String test = getText();

            if (!"1test".equals(test)) {
                throw new RuntimeException("Begin line action set cursor inside <head> tag");
            }

            robot.keyPress(KeyEvent.VK_HOME);
            robot.keyRelease(KeyEvent.VK_HOME);
            robot.keyPress(KeyEvent.VK_2);
            robot.keyRelease(KeyEvent.VK_2);

            robot.waitForIdle();

            test = getText();

            if (!"21test".equals(test)) {
                throw new RuntimeException("Begin action set cursor inside <head> tag");
            }
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(frame::dispose);
            }
        }
    }

    private static String getText() throws Exception {
        final String[] result = new String[1];

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    result[0] = doc.getText(0, doc.getLength()).trim();
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });

        return result[0];
    }

    private static void createAndShowGUI() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setText("test");
        editorPane.setEditable(true);
        frame.add(editorPane);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        doc = editorPane.getDocument();
        editorPane.setCaretPosition(doc.getLength());
    }
}
