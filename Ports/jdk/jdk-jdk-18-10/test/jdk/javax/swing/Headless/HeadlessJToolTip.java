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
 * @summary Check that JToolTip constructors and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJToolTip
 */

public class HeadlessJToolTip {
    public static void main(String args[]) {
        JToolTip tt = new JToolTip();
        tt.getAccessibleContext();
        tt.isFocusTraversable();
        tt.setEnabled(false);
        tt.setEnabled(true);
        tt.requestFocus();
        tt.requestFocusInWindow();
        tt.getPreferredSize();
        tt.getMaximumSize();
        tt.getMinimumSize();
        tt.contains(1, 2);
        Component c1 = tt.add(new Component(){});
        Component c2 = tt.add(new Component(){});
        Component c3 = tt.add(new Component(){});
        Insets ins = tt.getInsets();
        tt.getAlignmentY();
        tt.getAlignmentX();
        tt.getGraphics();
        tt.setVisible(false);
        tt.setVisible(true);
        tt.setForeground(Color.red);
        tt.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                tt.setFont(f1);
                tt.setFont(f2);
                tt.setFont(f3);
                tt.setFont(f4);

                tt.getFontMetrics(f1);
                tt.getFontMetrics(f2);
                tt.getFontMetrics(f3);
                tt.getFontMetrics(f4);
            }
        }
        tt.enable();
        tt.disable();
        tt.reshape(10, 10, 10, 10);
        tt.getBounds(new Rectangle(1, 1, 1, 1));
        tt.getSize(new Dimension(1, 2));
        tt.getLocation(new Point(1, 2));
        tt.getX();
        tt.getY();
        tt.getWidth();
        tt.getHeight();
        tt.isOpaque();
        tt.isValidateRoot();
        tt.isOptimizedDrawingEnabled();
        tt.isDoubleBuffered();
        tt.getComponentCount();
        tt.countComponents();
        tt.getComponent(1);
        tt.getComponent(2);
        Component[] cs = tt.getComponents();
        tt.getLayout();
        tt.setLayout(new FlowLayout());
        tt.doLayout();
        tt.layout();
        tt.invalidate();
        tt.validate();
        tt.remove(0);
        tt.remove(c2);
        tt.removeAll();
        tt.preferredSize();
        tt.minimumSize();
        tt.getComponentAt(1, 2);
        tt.locate(1, 2);
        tt.getComponentAt(new Point(1, 2));
        tt.isFocusCycleRoot(new Container());
        tt.transferFocusBackward();
        tt.setName("goober");
        tt.getName();
        tt.getParent();
        tt.getGraphicsConfiguration();
        tt.getTreeLock();
        tt.getToolkit();
        tt.isValid();
        tt.isDisplayable();
        tt.isVisible();
        tt.isShowing();
        tt.isEnabled();
        tt.enable(false);
        tt.enable(true);
        tt.enableInputMethods(false);
        tt.enableInputMethods(true);
        tt.show();
        tt.show(false);
        tt.show(true);
        tt.hide();
        tt.getForeground();
        tt.isForegroundSet();
        tt.getBackground();
        tt.isBackgroundSet();
        tt.getFont();
        tt.isFontSet();
        Container c = new Container();
        c.add(tt);
        tt.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            tt.setLocale(locale);

        tt.getColorModel();
        tt.getLocation();

        boolean exceptions = false;
        try {
            tt.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        tt.location();
        tt.setLocation(1, 2);
        tt.move(1, 2);
        tt.setLocation(new Point(1, 2));
        tt.getSize();
        tt.size();
        tt.setSize(1, 32);
        tt.resize(1, 32);
        tt.setSize(new Dimension(1, 32));
        tt.resize(new Dimension(1, 32));
        tt.getBounds();
        tt.bounds();
        tt.setBounds(10, 10, 10, 10);
        tt.setBounds(new Rectangle(10, 10, 10, 10));
        tt.isLightweight();
        tt.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        tt.getCursor();
        tt.isCursorSet();
        tt.inside(1, 2);
        tt.contains(new Point(1, 2));
        tt.isFocusable();
        tt.setFocusable(true);
        tt.setFocusable(false);
        tt.transferFocus();
        tt.getFocusCycleRootAncestor();
        tt.nextFocus();
        tt.transferFocusUpCycle();
        tt.hasFocus();
        tt.isFocusOwner();
        tt.toString();
        tt.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        tt.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        tt.setComponentOrientation(ComponentOrientation.UNKNOWN);
        tt.getComponentOrientation();
    }
}
