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
 * @summary Check that JMenuItem constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJMenuItem
 */

public class HeadlessJMenuItem {
    public static void main(String args[]) {
        JMenuItem mi = new JMenuItem();
        mi.getAccessibleContext();
        mi.isFocusTraversable();
        mi.setEnabled(false);
        mi.setEnabled(true);
        mi.requestFocus();
        mi.requestFocusInWindow();
        mi.getPreferredSize();
        mi.getMaximumSize();
        mi.getMinimumSize();
        mi.contains(1, 2);
        Component c1 = mi.add(new Component(){});
        Component c2 = mi.add(new Component(){});
        Component c3 = mi.add(new Component(){});
        Insets ins = mi.getInsets();
        mi.getAlignmentY();
        mi.getAlignmentX();
        mi.getGraphics();
        mi.setVisible(false);
        mi.setVisible(true);
        mi.setForeground(Color.red);
        mi.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                mi.setFont(f1);
                mi.setFont(f2);
                mi.setFont(f3);
                mi.setFont(f4);

                mi.getFontMetrics(f1);
                mi.getFontMetrics(f2);
                mi.getFontMetrics(f3);
                mi.getFontMetrics(f4);
            }
        }
        mi.enable();
        mi.disable();
        mi.reshape(10, 10, 10, 10);
        mi.getBounds(new Rectangle(1, 1, 1, 1));
        mi.getSize(new Dimension(1, 2));
        mi.getLocation(new Point(1, 2));
        mi.getX();
        mi.getY();
        mi.getWidth();
        mi.getHeight();
        mi.isOpaque();
        mi.isValidateRoot();
        mi.isOptimizedDrawingEnabled();
        mi.isDoubleBuffered();
        mi.getComponentCount();
        mi.countComponents();
        mi.getComponent(1);
        mi.getComponent(2);
        Component[] cs = mi.getComponents();
        mi.getLayout();
        mi.setLayout(new FlowLayout());
        mi.doLayout();
        mi.layout();
        mi.invalidate();
        mi.validate();
        mi.remove(0);
        mi.remove(c2);
        mi.removeAll();
        mi.preferredSize();
        mi.minimumSize();
        mi.getComponentAt(1, 2);
        mi.locate(1, 2);
        mi.getComponentAt(new Point(1, 2));
        mi.isFocusCycleRoot(new Container());
        mi.transferFocusBackward();
        mi.setName("goober");
        mi.getName();
        mi.getParent();
        mi.getGraphicsConfiguration();
        mi.getTreeLock();
        mi.getToolkit();
        mi.isValid();
        mi.isDisplayable();
        mi.isVisible();
        mi.isShowing();
        mi.isEnabled();
        mi.enable(false);
        mi.enable(true);
        mi.enableInputMethods(false);
        mi.enableInputMethods(true);
        mi.show();
        mi.show(false);
        mi.show(true);
        mi.hide();
        mi.getForeground();
        mi.isForegroundSet();
        mi.getBackground();
        mi.isBackgroundSet();
        mi.getFont();
        mi.isFontSet();
        Container c = new Container();
        c.add(mi);
        mi.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            mi.setLocale(locale);

        mi.getColorModel();
        mi.getLocation();

        boolean exceptions = false;
        try {
            mi.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        mi.location();
        mi.setLocation(1, 2);
        mi.move(1, 2);
        mi.setLocation(new Point(1, 2));
        mi.getSize();
        mi.size();
        mi.setSize(1, 32);
        mi.resize(1, 32);
        mi.setSize(new Dimension(1, 32));
        mi.resize(new Dimension(1, 32));
        mi.getBounds();
        mi.bounds();
        mi.setBounds(10, 10, 10, 10);
        mi.setBounds(new Rectangle(10, 10, 10, 10));
        mi.isLightweight();
        mi.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        mi.getCursor();
        mi.isCursorSet();
        mi.inside(1, 2);
        mi.contains(new Point(1, 2));
        mi.isFocusable();
        mi.setFocusable(true);
        mi.setFocusable(false);
        mi.transferFocus();
        mi.getFocusCycleRootAncestor();
        mi.nextFocus();
        mi.transferFocusUpCycle();
        mi.hasFocus();
        mi.isFocusOwner();
        mi.toString();
        mi.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        mi.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        mi.setComponentOrientation(ComponentOrientation.UNKNOWN);
        mi.getComponentOrientation();
    }
}
