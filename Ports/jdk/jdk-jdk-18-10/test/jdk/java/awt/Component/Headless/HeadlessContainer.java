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

import java.awt.*;
import java.util.*;

/*
 * @test
 * @summary Check that Container methods do not throw unexpected exceptions
 *          in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessContainer
 */

public class HeadlessContainer {
    public static void main(String args[]) {
        Container lw = new java.awt.Container();
        Component c1 = lw.add(new Component(){});
        Component c2 = lw.add(new Component(){});
        Component c3 = lw.add(new Component(){});

        lw.getComponentCount();
        lw.countComponents();
        lw.getComponent(1);
        lw.getComponent(2);
        Component[] cs = lw.getComponents();
        Insets ins = lw.getInsets();
        ins = lw.insets();
        lw.remove(1);
        lw.remove((java.awt.Component) c2);
        lw.removeAll();

        lw.add(c1);
        lw.add(c2);
        lw.add(c3);
        lw.getLayout();
        lw.setLayout(new FlowLayout());
        lw.doLayout();
        lw.layout();
        lw.invalidate();
        lw.validate();
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int i = 8; i < 17; i++) {
                Font f1 = new Font(font, Font.PLAIN, i);
                Font f2 = new Font(font, Font.BOLD, i);
                Font f3 = new Font(font, Font.ITALIC, i);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, i);

                lw.setFont(f1);
                lw.setFont(f2);
                lw.setFont(f3);
                lw.setFont(f4);
            }
        }
        lw.getPreferredSize();
        lw.preferredSize();
        lw.getMinimumSize();
        lw.minimumSize();
        lw.getMaximumSize();
        lw.getAlignmentX();
        lw.getAlignmentY();
        lw.getComponentAt(1, 2);
        lw.locate(1, 2);
        lw.getComponentAt(new Point(1, 2));
        lw.isFocusCycleRoot(new Container());
        lw.transferFocusBackward();
        lw.getName();
        lw.setName("goober");
        lw.getName();
        lw.getParent();
        lw.getGraphicsConfiguration();
        lw.getTreeLock();
        lw.getToolkit();
        lw.isValid();
        lw.isDisplayable();
        lw.isVisible();
        lw.isShowing();
        lw.isEnabled();
        lw.setEnabled(false);
        lw.setEnabled(true);
        lw.enable();
        lw.enable(false);
        lw.enable(true);
        lw.disable();
        lw.isDoubleBuffered();
        lw.enableInputMethods(false);
        lw.enableInputMethods(true);
        lw.setVisible(false);
        lw.setVisible(true);
        lw.show();
        lw.show(false);
        lw.show(true);
        lw.hide();
        lw.getForeground();
        lw.setForeground(Color.red);
        lw.isForegroundSet();
        lw.getBackground();
        lw.setBackground(Color.red);
        lw.isBackgroundSet();
        lw.getFont();
        lw.isFontSet();

        boolean exceptions = false;
        try {
            Container c = new Container();
            lw = new java.awt.Container();
            c.add(lw);
            lw.getLocale();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        for (Locale locale : Locale.getAvailableLocales())
            lw.setLocale(locale);

        lw.getColorModel();
        lw.getLocation();

        exceptions = false;
        try {
            lw.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        lw.location();
        lw.setLocation(1, 2);
        lw.move(1, 2);
        lw.setLocation(new Point(1, 2));
        lw.getSize();
        lw.size();
        lw.setSize(1, 32);
        lw.resize(1, 32);
        lw.setSize(new Dimension(1, 32));
        lw.resize(new Dimension(1, 32));
        lw.getBounds();
        lw.bounds();
        lw.setBounds(10, 10, 10, 10);
        lw.reshape(10, 10, 10, 10);
        lw.setBounds(new Rectangle(10, 10, 10, 10));
        lw.getX();
        lw.getY();
        lw.getWidth();
        lw.getHeight();
        lw.getBounds(new Rectangle(1, 1, 1, 1));
        lw.getSize(new Dimension(1, 2));
        lw.getLocation(new Point(1, 2));
        lw.isOpaque();
        lw.isLightweight();
        lw.getGraphics();

        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                lw.getFontMetrics(f1);
                lw.getFontMetrics(f2);
                lw.getFontMetrics(f3);
                lw.getFontMetrics(f4);
            }
        }

        Cursor c = new Cursor(Cursor.CROSSHAIR_CURSOR);
        lw.setCursor(c);
        lw.getCursor();
        lw.isCursorSet();
        lw.contains(1, 2);
        lw.inside(1, 2);
        lw.contains(new Point(1, 2));
        lw.isFocusTraversable();
        lw.isFocusable();
        lw.setFocusable(true);
        lw.setFocusable(false);
        lw.requestFocus();
        lw.requestFocusInWindow();
        lw.transferFocus();
        lw.getFocusCycleRootAncestor();
        lw.nextFocus();
        lw.transferFocusUpCycle();
        lw.hasFocus();
        lw.isFocusOwner();
        lw.toString();
        lw.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        lw.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        lw.setComponentOrientation(ComponentOrientation.UNKNOWN);
        lw.getComponentOrientation();
        lw.getAccessibleContext();
    }
}
