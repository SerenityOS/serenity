/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8014863
 * @bug 8024395
 * @summary  Tests the calculation of the line breaks when a text is inserted
 * @author Dmitry Markov
 * @modules java.desktop/javax.swing.text:open
 * @library ../../../regtesthelpers
 * @build Util
 * @run main bug8014863
 */

import javax.swing.*;
import javax.swing.text.GlyphView;
import javax.swing.text.View;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

public class bug8014863 {

    private static JEditorPane editorPane;
    private static JFrame frame;
    private static Robot robot;

    private static String text1 = "<p>one two qqqq <em>this is a test sentence</em> qqqq <em>pp</em> qqqq <em>pp</em> " +
            "qqqq <em>pp</em> qqqq <em>pp</em> qqqq <em>pp</em> qqqq <em>pp</em> qqqq <em>pp</em> qqqq <em>pp</em> qqqq</p>";
    private static String text2 = "<p>qqqq <em>this is a test sentence</em> qqqq <em>pp</em> qqqq <em>pp</em> " +
            "qqqq <em>pp</em> qqqq <em>pp</em> qqqq <em>pp</em> qqqq <em>pp</em> qqqq <em>pp</em> qqqq <em>pp</em> qqqq</p>";

    private static ArrayList<GlyphView> glyphViews;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        robot.setAutoDelay(100);
        glyphViews = new ArrayList<GlyphView>();

        createAndShowGUI(text1);

        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                retrieveGlyphViews(editorPane.getUI().getRootView(editorPane));
            }
        });
        GlyphView [] arr1 = glyphViews.toArray(new GlyphView[glyphViews.size()]);

        frame.dispose();
        glyphViews.clear();

        createAndShowGUI(text2);

        robot.waitForIdle();

        Util.hitKeys(robot, KeyEvent.VK_HOME);
        robot.waitForIdle();

        Util.hitKeys(robot, KeyEvent.VK_O);
        Util.hitKeys(robot, KeyEvent.VK_N);
        Util.hitKeys(robot, KeyEvent.VK_E);
        Util.hitKeys(robot, KeyEvent.VK_SPACE);
        Util.hitKeys(robot, KeyEvent.VK_T);
        Util.hitKeys(robot, KeyEvent.VK_W);
        Util.hitKeys(robot, KeyEvent.VK_O);
        Util.hitKeys(robot, KeyEvent.VK_SPACE);

        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                retrieveGlyphViews(editorPane.getUI().getRootView(editorPane));
            }
        });
        GlyphView [] arr2 = glyphViews.toArray(new GlyphView[glyphViews.size()]);

        if (arr1.length != arr2.length) {
            throw new RuntimeException("Test Failed!");
        }

        for (int i=0; i<arr1.length; i++) {
            GlyphView v1 = arr1[i];
            GlyphView v2 = arr2[i];
            Field field = GlyphView.class.getDeclaredField("breakSpots");
            field.setAccessible(true);
            int[] breakSpots1 = (int[])field.get(v1);
            int[] breakSpots2 = (int[])field.get(v2);
            if (!Arrays.equals(breakSpots1,breakSpots2)) {
                throw new RuntimeException("Test Failed!");
            }
        }

        frame.dispose();
    }

    private static void retrieveGlyphViews(View root) {
        for (int i=0; i<= root.getViewCount()-1; i++) {
            View view = root.getView(i);
            if (view instanceof GlyphView && view.isVisible()) {
                if (!glyphViews.contains(view)) {
                    glyphViews.add((GlyphView)view);
                }
            } else {
                retrieveGlyphViews(view);
            }
        }
    }

    private static void createAndShowGUI(String text) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                editorPane = new JEditorPane();
                HTMLEditorKit editorKit = new HTMLEditorKit();
                editorPane.setEditorKit(editorKit);
                editorPane.setText(text);
                editorPane.setCaretPosition(1);

                frame.add(editorPane);
                frame.setSize(200, 200);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
