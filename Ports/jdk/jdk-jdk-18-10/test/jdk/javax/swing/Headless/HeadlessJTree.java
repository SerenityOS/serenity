/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.Locale;

/*
 * @test
 * @summary Check that JTree constructors and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJTree
 */

public class HeadlessJTree {
    public static void main(String args[]) {
        JTree t;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("JTree");
        DefaultMutableTreeNode parent;

        parent = new DefaultMutableTreeNode("colors");
        root.add(parent);
        parent.add(new DefaultMutableTreeNode("blue"));
        parent.add(new DefaultMutableTreeNode("violet"));
        parent.add(new DefaultMutableTreeNode("red"));
        parent.add(new DefaultMutableTreeNode("yellow"));

        parent = new DefaultMutableTreeNode("sports");
        root.add(parent);
        parent.add(new DefaultMutableTreeNode("basketball"));
        parent.add(new DefaultMutableTreeNode("soccer"));
        parent.add(new DefaultMutableTreeNode("football"));
        parent.add(new DefaultMutableTreeNode("hockey"));

        parent = new DefaultMutableTreeNode("food");
        root.add(parent);
        parent.add(new DefaultMutableTreeNode("hot dogs"));
        parent.add(new DefaultMutableTreeNode("pizza"));
        parent.add(new DefaultMutableTreeNode("ravioli"));
        parent.add(new DefaultMutableTreeNode("bananas"));

        t = new JTree(root);
        t = new JTree(new DefaultTreeModel(root));
        t.setCellRenderer(new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(
                    JTree tree, Object value, boolean selected, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {
                return super.getTreeCellRendererComponent(
                        tree, value, selected, expanded,
                        leaf, row, hasFocus);
            }
        });
        t.getCellRenderer();

        t.getAccessibleContext();
        t.isFocusTraversable();
        t.setEnabled(false);
        t.setEnabled(true);
        t.requestFocus();
        t.requestFocusInWindow();
        t.getPreferredSize();
        t.getMaximumSize();
        t.getMinimumSize();
        t.contains(1, 2);
        Component c1 = t.add(new Component(){});
        Component c2 = t.add(new Component(){});
        Component c3 = t.add(new Component(){});
        Insets ins = t.getInsets();
        t.getAlignmentY();
        t.getAlignmentX();
        t.getGraphics();
        t.setVisible(false);
        t.setVisible(true);
        t.setForeground(Color.red);
        t.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                t.setFont(f1);
                t.setFont(f2);
                t.setFont(f3);
                t.setFont(f4);

                t.getFontMetrics(f1);
                t.getFontMetrics(f2);
                t.getFontMetrics(f3);
                t.getFontMetrics(f4);
            }
        }
        t.enable();
        t.disable();
        t.reshape(10, 10, 10, 10);
        t.getBounds(new Rectangle(1, 1, 1, 1));
        t.getSize(new Dimension(1, 2));
        t.getLocation(new Point(1, 2));
        t.getX();
        t.getY();
        t.getWidth();
        t.getHeight();
        t.isOpaque();
        t.isValidateRoot();
        t.isOptimizedDrawingEnabled();
        t.isDoubleBuffered();
        t.getComponentCount();
        t.countComponents();
        t.getComponent(1);
        t.getComponent(2);
        Component[] cs = t.getComponents();
        t.getLayout();
        t.setLayout(new FlowLayout());
        t.doLayout();
        t.layout();
        t.invalidate();
        t.validate();
        t.remove(0);
        t.remove(c2);
        t.removeAll();
        t.preferredSize();
        t.minimumSize();
        t.getComponentAt(1, 2);
        t.locate(1, 2);
        t.getComponentAt(new Point(1, 2));
        t.isFocusCycleRoot(new Container());
        t.transferFocusBackward();
        t.setName("goober");
        t.getName();
        t.getParent();
        t.getGraphicsConfiguration();
        t.getTreeLock();
        t.getToolkit();
        t.isValid();
        t.isDisplayable();
        t.isVisible();
        t.isShowing();
        t.isEnabled();
        t.enable(false);
        t.enable(true);
        t.enableInputMethods(false);
        t.enableInputMethods(true);
        t.show();
        t.show(false);
        t.show(true);
        t.hide();
        t.getForeground();
        t.isForegroundSet();
        t.getBackground();
        t.isBackgroundSet();
        t.getFont();
        t.isFontSet();
        Container c = new Container();
        c.add(t);
        t.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            t.setLocale(locale);

        t.getColorModel();
        t.getLocation();

        boolean exceptions = false;
        try {
            t.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        t.location();
        t.setLocation(1, 2);
        t.move(1, 2);
        t.setLocation(new Point(1, 2));
        t.getSize();
        t.size();
        t.setSize(1, 32);
        t.resize(1, 32);
        t.setSize(new Dimension(1, 32));
        t.resize(new Dimension(1, 32));
        t.getBounds();
        t.bounds();
        t.setBounds(10, 10, 10, 10);
        t.setBounds(new Rectangle(10, 10, 10, 10));
        t.isLightweight();
        t.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        t.getCursor();
        t.isCursorSet();
        t.inside(1, 2);
        t.contains(new Point(1, 2));
        t.isFocusable();
        t.setFocusable(true);
        t.setFocusable(false);
        t.transferFocus();
        t.getFocusCycleRootAncestor();
        t.nextFocus();
        t.transferFocusUpCycle();
        t.hasFocus();
        t.isFocusOwner();
        t.toString();
        t.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        t.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        t.setComponentOrientation(ComponentOrientation.UNKNOWN);
        t.getComponentOrientation();
    }
}
