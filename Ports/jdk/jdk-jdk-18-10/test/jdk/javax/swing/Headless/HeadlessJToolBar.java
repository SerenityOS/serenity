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
 * @summary Check that JToolBar constructors and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJToolBar
 */

public class HeadlessJToolBar {
    public static void main(String args[]) {
        JToolBar tb = new JToolBar();
        tb.getAccessibleContext();
        tb.isFocusTraversable();
        tb.setEnabled(false);
        tb.setEnabled(true);
        tb.requestFocus();
        tb.requestFocusInWindow();
        tb.getPreferredSize();
        tb.getMaximumSize();
        tb.getMinimumSize();
        tb.contains(1, 2);
        Component c1 = tb.add(new Component(){});
        Component c2 = tb.add(new Component(){});
        Component c3 = tb.add(new Component(){});
        Insets ins = tb.getInsets();
        tb.getAlignmentY();
        tb.getAlignmentX();
        tb.getGraphics();
        tb.setVisible(false);
        tb.setVisible(true);
        tb.setForeground(Color.red);
        tb.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                tb.setFont(f1);
                tb.setFont(f2);
                tb.setFont(f3);
                tb.setFont(f4);

                tb.getFontMetrics(f1);
                tb.getFontMetrics(f2);
                tb.getFontMetrics(f3);
                tb.getFontMetrics(f4);
            }
        }
        tb.enable();
        tb.disable();
        tb.reshape(10, 10, 10, 10);
        tb.getBounds(new Rectangle(1, 1, 1, 1));
        tb.getSize(new Dimension(1, 2));
        tb.getLocation(new Point(1, 2));
        tb.getX();
        tb.getY();
        tb.getWidth();
        tb.getHeight();
        tb.isOpaque();
        tb.isValidateRoot();
        tb.isOptimizedDrawingEnabled();
        tb.isDoubleBuffered();
        tb.getComponentCount();
        tb.countComponents();
        tb.getComponent(1);
        tb.getComponent(2);
        Component[] cs = tb.getComponents();
        tb.getLayout();
        tb.setLayout(new FlowLayout());
        tb.doLayout();
        tb.layout();
        tb.invalidate();
        tb.validate();
        tb.remove(0);
        tb.remove(c2);
        tb.removeAll();
        tb.preferredSize();
        tb.minimumSize();
        tb.getComponentAt(1, 2);
        tb.locate(1, 2);
        tb.getComponentAt(new Point(1, 2));
        tb.isFocusCycleRoot(new Container());
        tb.transferFocusBackward();
        tb.setName("goober");
        tb.getName();
        tb.getParent();
        tb.getGraphicsConfiguration();
        tb.getTreeLock();
        tb.getToolkit();
        tb.isValid();
        tb.isDisplayable();
        tb.isVisible();
        tb.isShowing();
        tb.isEnabled();
        tb.enable(false);
        tb.enable(true);
        tb.enableInputMethods(false);
        tb.enableInputMethods(true);
        tb.show();
        tb.show(false);
        tb.show(true);
        tb.hide();
        tb.getForeground();
        tb.isForegroundSet();
        tb.getBackground();
        tb.isBackgroundSet();
        tb.getFont();
        tb.isFontSet();
        Container c = new Container();
        c.add(tb);
        tb.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            tb.setLocale(locale);

        tb.getColorModel();
        tb.getLocation();

        boolean exceptions = false;
        try {
            tb.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        tb.location();
        tb.setLocation(1, 2);
        tb.move(1, 2);
        tb.setLocation(new Point(1, 2));
        tb.getSize();
        tb.size();
        tb.setSize(1, 32);
        tb.resize(1, 32);
        tb.setSize(new Dimension(1, 32));
        tb.resize(new Dimension(1, 32));
        tb.getBounds();
        tb.bounds();
        tb.setBounds(10, 10, 10, 10);
        tb.setBounds(new Rectangle(10, 10, 10, 10));
        tb.isLightweight();
        tb.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        tb.getCursor();
        tb.isCursorSet();
        tb.inside(1, 2);
        tb.contains(new Point(1, 2));
        tb.isFocusable();
        tb.setFocusable(true);
        tb.setFocusable(false);
        tb.transferFocus();
        tb.getFocusCycleRootAncestor();
        tb.nextFocus();
        tb.transferFocusUpCycle();
        tb.hasFocus();
        tb.isFocusOwner();
        tb.toString();
        tb.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        tb.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        tb.setComponentOrientation(ComponentOrientation.UNKNOWN);
        tb.getComponentOrientation();
    }
}
