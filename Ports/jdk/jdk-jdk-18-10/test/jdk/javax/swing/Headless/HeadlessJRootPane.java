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
 * @summary Check that JRootPane constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJRootPane
 */

public class HeadlessJRootPane {
    public static void main(String args[]) {
        JRootPane rp = new JRootPane();
        rp.getAccessibleContext();
        rp.isFocusTraversable();
        rp.setEnabled(false);
        rp.setEnabled(true);
        rp.requestFocus();
        rp.requestFocusInWindow();
        rp.getPreferredSize();
        rp.getMaximumSize();
        rp.getMinimumSize();
        rp.contains(1, 2);
        Component c1 = rp.add(new Component(){});
        Component c2 = rp.add(new Component(){});
        Component c3 = rp.add(new Component(){});
        Insets ins = rp.getInsets();
        rp.getAlignmentY();
        rp.getAlignmentX();
        rp.getGraphics();
        rp.setVisible(false);
        rp.setVisible(true);
        rp.setForeground(Color.red);
        rp.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                rp.setFont(f1);
                rp.setFont(f2);
                rp.setFont(f3);
                rp.setFont(f4);

                rp.getFontMetrics(f1);
                rp.getFontMetrics(f2);
                rp.getFontMetrics(f3);
                rp.getFontMetrics(f4);
            }
        }
        rp.enable();
        rp.disable();
        rp.reshape(10, 10, 10, 10);
        rp.getBounds(new Rectangle(1, 1, 1, 1));
        rp.getSize(new Dimension(1, 2));
        rp.getLocation(new Point(1, 2));
        rp.getX();
        rp.getY();
        rp.getWidth();
        rp.getHeight();
        rp.isOpaque();
        rp.isValidateRoot();
        rp.isOptimizedDrawingEnabled();
        rp.isDoubleBuffered();
        rp.getComponentCount();
        rp.countComponents();
        rp.getComponent(1);
        rp.getComponent(2);
        Component[] cs = rp.getComponents();
        rp.getLayout();
        rp.setLayout(new FlowLayout());
        rp.doLayout();
        rp.layout();
        rp.invalidate();
        rp.validate();
        rp.remove(0);
        rp.remove(c2);
        rp.removeAll();
        rp.preferredSize();
        rp.minimumSize();
        rp.getComponentAt(1, 2);
        rp.locate(1, 2);
        rp.getComponentAt(new Point(1, 2));
        rp.isFocusCycleRoot(new Container());
        rp.transferFocusBackward();
        rp.setName("goober");
        rp.getName();
        rp.getParent();
        rp.getGraphicsConfiguration();
        rp.getTreeLock();
        rp.getToolkit();
        rp.isValid();
        rp.isDisplayable();
        rp.isVisible();
        rp.isShowing();
        rp.isEnabled();
        rp.enable(false);
        rp.enable(true);
        rp.enableInputMethods(false);
        rp.enableInputMethods(true);
        rp.show();
        rp.show(false);
        rp.show(true);
        rp.hide();
        rp.getForeground();
        rp.isForegroundSet();
        rp.getBackground();
        rp.isBackgroundSet();
        rp.getFont();
        rp.isFontSet();
        Container c = new Container();
        c.add(rp);
        rp.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            rp.setLocale(locale);

        rp.getColorModel();
        rp.getLocation();

        boolean exceptions = false;
        try {
            rp.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        rp.location();
        rp.setLocation(1, 2);
        rp.move(1, 2);
        rp.setLocation(new Point(1, 2));
        rp.getSize();
        rp.size();
        rp.setSize(1, 32);
        rp.resize(1, 32);
        rp.setSize(new Dimension(1, 32));
        rp.resize(new Dimension(1, 32));
        rp.getBounds();
        rp.bounds();
        rp.setBounds(10, 10, 10, 10);
        rp.setBounds(new Rectangle(10, 10, 10, 10));
        rp.isLightweight();
        rp.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        rp.getCursor();
        rp.isCursorSet();
        rp.inside(1, 2);
        rp.contains(new Point(1, 2));
        rp.isFocusable();
        rp.setFocusable(true);
        rp.setFocusable(false);
        rp.transferFocus();
        rp.getFocusCycleRootAncestor();
        rp.nextFocus();
        rp.transferFocusUpCycle();
        rp.hasFocus();
        rp.isFocusOwner();
        rp.toString();
        rp.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        rp.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        rp.setComponentOrientation(ComponentOrientation.UNKNOWN);
        rp.getComponentOrientation();
    }
}
