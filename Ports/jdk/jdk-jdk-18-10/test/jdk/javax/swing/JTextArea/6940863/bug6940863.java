/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 6940863
 * @summary Textarea within scrollpane shows vertical scrollbar
 * @author Pavel Porvatov
 * @requires (os.family == "windows")
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run main bug6940863
 */

import jdk.test.lib.Platform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class bug6940863 {
    private static JFrame frame;

    private static JScrollPane scrollPane;

    private static final Timer timer = new Timer(1000, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            boolean failed = scrollPane.getVerticalScrollBar().isShowing() ||
                    scrollPane.getHorizontalScrollBar().isShowing();

            frame.dispose();

            if (failed) {
                throw new RuntimeException("The test failed");
            }
        }
    });

    public static void main(String[] args) throws Exception {
        if (!Platform.isWindows()) {
            System.out.println("The test is suitable only for Windows OS. Skipped");
            return;
        }

        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                JTextArea textArea = new JTextArea();

                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);

                scrollPane = new JScrollPane(textArea);

                scrollPane.setMinimumSize(new Dimension(200, 100));
                scrollPane.setPreferredSize(new Dimension(300, 150));

                frame = new JFrame("Vertical scrollbar shown without text");

                frame.setContentPane(scrollPane);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);

                timer.setRepeats(false);
                timer.start();
            }
        });
    }
}
