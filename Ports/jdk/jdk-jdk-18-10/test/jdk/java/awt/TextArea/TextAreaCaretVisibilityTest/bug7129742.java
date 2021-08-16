/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright (c) 2012 IBM Corporation
 */


/*
 * @test
 * @key headful
 * @bug 7129742
 * @summary Focus in non-editable TextArea is not shown on Linux.
 * @requires os.family == "linux"
 * @modules java.desktop/sun.awt
 *          java.desktop/java.awt.peer
 *          java.desktop/sun.awt.X11:open
 * @author Sean Chou
 */

import java.awt.FlowLayout;
import java.awt.TextArea;
import java.awt.Robot;
import java.lang.reflect.Field;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;


public class bug7129742 {

    public static DefaultCaret caret = null;
    public static JFrame frame = null;
    public static boolean fastreturn = false;

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame = new JFrame("Test");
                TextArea textArea = new TextArea("Non-editable textArea");
                textArea.setEditable(false);
                frame.setLayout(new FlowLayout());
                frame.add(textArea);
                frame.pack();
                frame.setVisible(true);

                try {
                    ComponentAccessor acc = AWTAccessor.getComponentAccessor();
                    Class XTextAreaPeerClzz = acc.getPeer(textArea).getClass();
                    System.out.println(XTextAreaPeerClzz.getName());
                    if (!XTextAreaPeerClzz.getName().equals("sun.awt.X11.XTextAreaPeer")) {
                        fastreturn = true;
                        return;
                    }

                    Field jtextField = XTextAreaPeerClzz.getDeclaredField("jtext");
                    jtextField.setAccessible(true);
                    JTextArea jtext = (JTextArea)jtextField.get(acc.getPeer(textArea));
                    caret = (DefaultCaret) jtext.getCaret();

                    textArea.requestFocusInWindow();
                } catch (NoSuchFieldException | SecurityException
                         | IllegalArgumentException | IllegalAccessException e) {
                    /* These exceptions mean the implementation of XTextAreaPeer is
                     * changed, this testcase is not valid any more, fix it or remove.
                     */
                    frame.dispose();
                    throw new RuntimeException("This testcase is not valid any more!");
                }
            }
        });
        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try{
                    if (fastreturn) {
                        return;
                    }
                    boolean passed = caret.isActive();
                    System.out.println("is caret visible : " + passed);

                    if (!passed) {
                        throw new RuntimeException("The test for bug 71297422 failed");
                    }
                } finally {
                    frame.dispose();
                }
            }
        });
    }

}
