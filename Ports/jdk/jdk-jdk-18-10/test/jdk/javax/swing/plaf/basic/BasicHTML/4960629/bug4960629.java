/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4960629 7124238
 * @summary  Tests if font for html text on widgets in correct.
 * @author Denis Sharypov
 * @run main bug4960629
 */

import java.awt.Font;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.AttributeSet;
import javax.swing.text.View;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.HTMLDocument;

public class bug4960629 {
    private boolean passed = false;
    private JLabel label = null;
    private JFrame f = null;

    public void createAndShowGUI() throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            label = new JLabel("<html><P>This is a test of the</P></html>");
            System.out.println("UIManager.getLookAndFeel()"
                   + UIManager.getLookAndFeel().getClass());
            f = new JFrame();
            f.getContentPane().add(label);
            f.pack();
            f.setVisible(true);
            test();
        } finally {
            if (f != null) { f.dispose(); }
        }
    }

    bug4960629() throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    createAndShowGUI();
                } catch (Exception e) {
                    throw new RuntimeException("Exception "
                              + e.getMessage());
                }
            }
        });
    }

    private void test() {
        View root = ((View)label.getClientProperty(BasicHTML.propertyKey))
                .getView(0);
        int n = root.getViewCount();
        View v  = root.getView(n - 1);
        AttributeSet attrs = v.getAttributes();
        StyleSheet ss = ((HTMLDocument) v.getDocument()).getStyleSheet();
        Font font = ss.getFont(attrs);
        System.out.println(font.getSize());
        passed = (font.getSize() == 12);
        if(!passed) {
            throw new RuntimeException("Test failed.");
        }
    }

    public static void main(String args[]) throws Throwable {
        new bug4960629();
   }
}
