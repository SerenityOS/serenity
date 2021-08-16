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

/*
 * @test
 * @key headful
 * @bug 8132136
 * @summary [PIT] RTL orientation in JEditorPane is broken
 * @author Semyon Sadetsky
 */

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;

public class JTextPaneDocumentAlignment {

    private static JFrame frame;
    private static JTextPane jTextPane;
    private static int position;

    public static void main(String[] args) throws Exception{
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame = new JFrame();
                frame.setUndecorated(true);
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                frame.setSize(200, 200);
                jTextPane = new JTextPane();
                jTextPane.setContentType("text/html");
                jTextPane.setText(
                        "<html><body><b id='test'>Test</b></body></html>");
                SimpleAttributeSet right = new SimpleAttributeSet();
                StyleConstants.setAlignment(right, StyleConstants.ALIGN_RIGHT);
                jTextPane.getStyledDocument()
                        .setParagraphAttributes(0, 10, right, true);
                frame.getContentPane().add(jTextPane);
                frame.setVisible(true);
            }
        });
        Robot robot = new Robot();
        robot.waitForIdle();
        robot.delay(200);
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    position = jTextPane.modelToView(1).x;
                    SimpleAttributeSet center = new SimpleAttributeSet();
                    StyleConstants.setAlignment(center,
                            StyleConstants.ALIGN_CENTER);
                    jTextPane.getStyledDocument()
                            .setParagraphAttributes(0, 10, center, true);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        });
        if(position < 100) {
            throw  new RuntimeException("Text is not right aligned " + position);
        }
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    position = jTextPane.modelToView(1).x;
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
                frame.dispose();
            }
        });
        if(position < 20) {
            throw  new RuntimeException("Text is not center aligned " + position);
        }
        System.out.println("ok");
    }
}
