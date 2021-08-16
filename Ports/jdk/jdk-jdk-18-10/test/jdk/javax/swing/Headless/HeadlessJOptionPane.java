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
import java.awt.*;
import java.util.Locale;

/*
 * @test
 * @summary Check that JOptionPane constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJOptionPane
 */

public class HeadlessJOptionPane {
    public static void main(String args[]) {
        JOptionPane op = new JOptionPane();
        op.getAccessibleContext();
        op.isFocusTraversable();
        op.setEnabled(false);
        op.setEnabled(true);
        op.requestFocus();
        op.requestFocusInWindow();
        op.getPreferredSize();
        op.getMaximumSize();
        op.getMinimumSize();
        op.contains(1, 2);
        Component c1 = op.add(new Component(){});
        Component c2 = op.add(new Component(){});
        Component c3 = op.add(new Component(){});
        Insets ins = op.getInsets();
        op.getAlignmentY();
        op.getAlignmentX();
        op.getGraphics();
        op.setVisible(false);
        op.setVisible(true);
        op.setForeground(Color.red);
        op.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                op.setFont(f1);
                op.setFont(f2);
                op.setFont(f3);
                op.setFont(f4);

                op.getFontMetrics(f1);
                op.getFontMetrics(f2);
                op.getFontMetrics(f3);
                op.getFontMetrics(f4);
            }
        }
        op.enable();
        op.disable();
        op.reshape(10, 10, 10, 10);
        op.getBounds(new Rectangle(1, 1, 1, 1));
        op.getSize(new Dimension(1, 2));
        op.getLocation(new Point(1, 2));
        op.getX();
        op.getY();
        op.getWidth();
        op.getHeight();
        op.isOpaque();
        op.isValidateRoot();
        op.isOptimizedDrawingEnabled();
        op.isDoubleBuffered();
        op.getComponentCount();
        op.countComponents();
        op.getComponent(1);
        op.getComponent(2);
        Component[] cs = op.getComponents();
        op.getLayout();
        op.setLayout(new FlowLayout());
        op.doLayout();
        op.layout();
        op.invalidate();
        op.validate();
        op.remove(0);
        op.remove(c2);
        op.removeAll();
        op.preferredSize();
        op.minimumSize();
        op.getComponentAt(1, 2);
        op.locate(1, 2);
        op.getComponentAt(new Point(1, 2));
        op.isFocusCycleRoot(new Container());
        op.transferFocusBackward();
        op.setName("goober");
        op.getName();
        op.getParent();
        op.getGraphicsConfiguration();
        op.getTreeLock();
        op.getToolkit();
        op.isValid();
        op.isDisplayable();
        op.isVisible();
        op.isShowing();
        op.isEnabled();
        op.enable(false);
        op.enable(true);
        op.enableInputMethods(false);
        op.enableInputMethods(true);
        op.show();
        op.show(false);
        op.show(true);
        op.hide();
        op.getForeground();
        op.isForegroundSet();
        op.getBackground();
        op.isBackgroundSet();
        op.getFont();
        op.isFontSet();
        Container c = new Container();
        c.add(op);
        op.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            op.setLocale(locale);

        op.getColorModel();
        op.getLocation();

        boolean exceptions = false;
        try {
            op.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        op.location();
        op.setLocation(1, 2);
        op.move(1, 2);
        op.setLocation(new Point(1, 2));
        op.getSize();
        op.size();
        op.setSize(1, 32);
        op.resize(1, 32);
        op.setSize(new Dimension(1, 32));
        op.resize(new Dimension(1, 32));
        op.getBounds();
        op.bounds();
        op.setBounds(10, 10, 10, 10);
        op.setBounds(new Rectangle(10, 10, 10, 10));
        op.isLightweight();
        op.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        op.getCursor();
        op.isCursorSet();
        op.inside(1, 2);
        op.contains(new Point(1, 2));
        op.isFocusable();
        op.setFocusable(true);
        op.setFocusable(false);
        op.transferFocus();
        op.getFocusCycleRootAncestor();
        op.nextFocus();
        op.transferFocusUpCycle();
        op.hasFocus();
        op.isFocusOwner();
        op.toString();
        op.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        op.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        op.setComponentOrientation(ComponentOrientation.UNKNOWN);
        op.getComponentOrientation();
    }
}
