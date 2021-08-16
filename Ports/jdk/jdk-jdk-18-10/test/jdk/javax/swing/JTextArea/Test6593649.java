/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6593649
 * @summary Word wrap does not work in JTextArea: long lines are not wrapped
 * @author Lillian Angel
 * @run main Test6593649
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Test6593649 {
    private static JFrame frame;

    private static JTextArea textArea;

    private static final Timer timer = new Timer(1000, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            boolean failed = !textArea.getParent().getSize().equals(textArea.getSize());

            frame.dispose();

            if (failed) {
                throw new RuntimeException("The test failed");
            }
        }
    });

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame();

                frame.setSize(200, 100);

                textArea = new JTextArea("This is a long line that should wrap, but doesn't...");

                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);

                JPanel innerPanel = new JPanel();

                innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.LINE_AXIS));
                innerPanel.add(textArea);

                frame.getContentPane().add(innerPanel, BorderLayout.SOUTH);

                frame.setVisible(true);

                timer.setRepeats(false);
                timer.start();
            }
        });
    }
}
