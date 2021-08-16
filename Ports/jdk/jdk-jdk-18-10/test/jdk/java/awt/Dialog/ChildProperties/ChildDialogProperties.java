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
 * @summary Verify that child Dialog does not inherit parent's Properties
 * @run main ChildDialogProperties
 */

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;

public class ChildDialogProperties {

    private Dialog parentDialog;
    private Dialog dialogChild;
    private Frame parentFrame;
    private Dialog frameChildDialog;
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

        dialogChild = new Dialog(parentDialog, "Dialog's child");
        dialogChild.setSize(WIDTH, HEIGHT);
        dialogChild.setLocation(WIDTH + 200, 100);
        childLabel = new Label("ChildForegroundAndFont");
        dialogChild.add(childLabel);

        dialogChild.setVisible(true);

        if (parentDialog.getBackground() == dialogChild.getBackground()) {
            dispose();
            throw new RuntimeException("Child Dialog Should NOT Inherit "
                    + "Parent Dialog's Background Color");
        }

        if (parentDialog.getForeground() == dialogChild.getForeground()) {
            dispose();
            throw new RuntimeException("Child Dialog Should NOT Inherit "
                    + "Parent Dialog's Foreground Color");
        }

        if (parentDialog.getFont() == dialogChild.getFont()) {
            dispose();
            throw new RuntimeException("Child Dialog Should NOT Inherit "
                    + "Parent Dialog's Font Style/Color");
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

        frameChildDialog = new Dialog(parentFrame, "Frame's child");
        frameChildDialog.setSize(WIDTH, HEIGHT);
        frameChildDialog.setLocation(WIDTH + 200, 400);
        childLabel = new Label("ChildForegroundAndFont");
        frameChildDialog.add(childLabel);
        frameChildDialog.setVisible(true);

        if (parentFrame.getBackground() == frameChildDialog.getBackground()) {
            dispose();
            throw new RuntimeException("Child Dialog Should NOT Inherit "
                    + "Parent Frame's Background Color");
        }

        if (parentFrame.getForeground() == frameChildDialog.getForeground()) {
            dispose();
            throw new RuntimeException("Child Dialog Should NOT Inherit "
                    + "Parent Frame's Foreground Color");
        }

        if (parentFrame.getFont() == frameChildDialog.getFont()) {
            dispose();
            throw new RuntimeException("Child Dialog Should NOT Inherit "
                    + "Parent Frame's Font Style/Color");
        }
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

        ChildDialogProperties obj = new ChildDialogProperties();
        // TestCase1: When Parent is Dialog, Child is Dialog
        obj.testChildPropertiesWithDialogAsParent();
        // TestCase2: When Parent is Frame, chis is Dialog
        obj.testChildPropertiesWithFrameAsParent();
        obj.dispose();
    }

}
