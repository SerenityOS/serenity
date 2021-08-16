/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8057574
 * @summary Verify that child Window does not inherit parent's Properties
 * @run main ChildWindowProperties
 */

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Window;

public class ChildWindowProperties {

    private Dialog parentDialog;
    private Window windowChild;
    private Frame parentFrame;
    private Window frameChildWindow;
    private Label parentLabel;
    private Font parentFont;
    private Label childLabel;

    private static final int WIDTH = 200;
    private static final int HEIGHT = 200;

    public void testChildPropertiesWithDialogAsParent() {

        parentDialog = new Dialog((Dialog) null, "parent Dialog");
        parentDialog.setSize(WIDTH, HEIGHT);
        parentDialog.setLocation(100, 100);
        parentDialog.setBackground(Color.RED);
        parentLabel = new Label("ParentForegroundAndFont");
        parentFont = new Font("Courier New", Font.ITALIC, 15);
        parentDialog.setForeground(Color.BLUE);
        parentDialog.setFont(parentFont);

        parentDialog.add(parentLabel);
        parentDialog.setVisible(true);

        windowChild = new Window(parentDialog);
        windowChild.setSize(WIDTH, HEIGHT);
        windowChild.setLocation(WIDTH + 200, 100);
        childLabel = new Label("ChildForegroundAndFont");
        windowChild.add(childLabel);
        windowChild.setVisible(true);

        if (parentDialog.getBackground() == windowChild.getBackground()) {
            dispose();
            throw new RuntimeException("Child Window Should NOT Inherit "
                    + "Parent Dialog's Background Color");
        }
        if (parentDialog.getForeground() == windowChild.getForeground()) {
            dispose();
            throw new RuntimeException("Child Window Should NOT Inherit "
                    + "Parent Dialog's Foreground Color");
        }
        if (parentDialog.getFont() == windowChild.getFont()) {
            dispose();
            throw new RuntimeException("Child Window Should NOT Inherit "
                    + "Parent Dialog's Font Color");
        }
    }

    public void testChildPropertiesWithFrameAsParent() {

        parentFrame = new Frame("parent Frame");
        parentFrame.setSize(WIDTH, HEIGHT);
        parentFrame.setLocation(100, 400);
        parentFrame.setBackground(Color.BLUE);
        parentLabel = new Label("ParentForegroundAndFont");
        parentFont = new Font("Courier New", Font.ITALIC, 15);
        parentFrame.setForeground(Color.RED);
        parentFrame.setFont(parentFont);
        parentFrame.add(parentLabel);
        parentFrame.setVisible(true);

        frameChildWindow = new Window(parentFrame);
        frameChildWindow.setSize(WIDTH, HEIGHT);
        frameChildWindow.setLocation(WIDTH + 200, 400);
        childLabel = new Label("ChildForegroundAndFont");
        frameChildWindow.add(childLabel);
        frameChildWindow.setVisible(true);

        if (parentFrame.getBackground() == frameChildWindow.getBackground()) {
            dispose();
            throw new RuntimeException("Child Window Should NOT Inherit "
                    + "Parent Frame's Background Color");
        }
        if (parentDialog.getForeground() == windowChild.getForeground()) {
            dispose();
            throw new RuntimeException("Child Window Should NOT Inherit "
                    + "Parent Frame's Foreground Color");
        }
        if (parentDialog.getFont() == windowChild.getFont()) {
            dispose();
            throw new RuntimeException("Child Window Should NOT Inherit "
                    + "Parent Frame's Font Color");
        }
    }

    public void testPanelBackground() {
        Window window = new Frame();
        window.setBackground(Color.GREEN);
        Panel panel = new Panel();
        window.add(panel);
        window.pack();
        window.setVisible(true);
        if (panel.getBackground() != Color.GREEN) {
            window.dispose();
            throw new RuntimeException("Panel Background Color Not Valid");
        }
        window.dispose();
    }

    private void dispose() {

        if (parentDialog != null) {
            parentDialog.dispose();
        }
        if (parentFrame != null) {
            parentFrame.dispose();
        }
    }

    public static void main(String[] args) throws Exception {

        ChildWindowProperties obj = new ChildWindowProperties();
        // TestCase1: When Parent is Dialog, Child is Window
        obj.testChildPropertiesWithDialogAsParent();
        // TestCase2: When Parent is Frame, chis is Window
        obj.testChildPropertiesWithFrameAsParent();
        // TestCase3: Panel Background Test
        obj.testPanelBackground();
        obj.dispose();
    }

}

