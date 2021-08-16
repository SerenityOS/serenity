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
 * @summary Check that JLayeredPane constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJLayeredPane
 */

public class HeadlessJLayeredPane {
    public static void main(String args[]) {
        JLayeredPane lp = new JLayeredPane();
        lp.getAccessibleContext();
        lp.isFocusTraversable();
        lp.setEnabled(false);
        lp.setEnabled(true);
        lp.requestFocus();
        lp.requestFocusInWindow();
        lp.getPreferredSize();
        lp.getMaximumSize();
        lp.getMinimumSize();
        lp.contains(1, 2);
        Component c1 = lp.add(new Component(){});
        Component c2 = lp.add(new Component(){});
        Component c3 = lp.add(new Component(){});
        Insets ins = lp.getInsets();
        lp.getAlignmentY();
        lp.getAlignmentX();
        lp.getGraphics();
        lp.setVisible(false);
        lp.setVisible(true);
        lp.setForeground(Color.red);
        lp.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                lp.setFont(f1);
                lp.setFont(f2);
                lp.setFont(f3);
                lp.setFont(f4);

                lp.getFontMetrics(f1);
                lp.getFontMetrics(f2);
                lp.getFontMetrics(f3);
                lp.getFontMetrics(f4);
            }
        }
        lp.enable();
        lp.disable();
        lp.reshape(10, 10, 10, 10);
        lp.getBounds(new Rectangle(1, 1, 1, 1));
        lp.getSize(new Dimension(1, 2));
        lp.getLocation(new Point(1, 2));
        lp.getX();
        lp.getY();
        lp.getWidth();
        lp.getHeight();
        lp.isOpaque();
        lp.isValidateRoot();
        lp.isOptimizedDrawingEnabled();
        lp.isDoubleBuffered();
        lp.getComponentCount();
        lp.countComponents();
        lp.getComponent(1);
        lp.getComponent(2);
        Component[] cs = lp.getComponents();
        lp.getLayout();
        lp.setLayout(new FlowLayout());
        lp.doLayout();
        lp.layout();
        lp.invalidate();
        lp.validate();
        lp.remove(0);
        lp.remove(c2);
        lp.removeAll();
        lp.preferredSize();
        lp.minimumSize();
        lp.getComponentAt(1, 2);
        lp.locate(1, 2);
        lp.getComponentAt(new Point(1, 2));
        lp.isFocusCycleRoot(new Container());
        lp.transferFocusBackward();
        lp.setName("goober");
        lp.getName();
        lp.getParent();
        lp.getGraphicsConfiguration();
        lp.getTreeLock();
        lp.getToolkit();
        lp.isValid();
        lp.isDisplayable();
        lp.isVisible();
        lp.isShowing();
        lp.isEnabled();
        lp.enable(false);
        lp.enable(true);
        lp.enableInputMethods(false);
        lp.enableInputMethods(true);
        lp.show();
        lp.show(false);
        lp.show(true);
        lp.hide();
        lp.getForeground();
        lp.isForegroundSet();
        lp.getBackground();
        lp.isBackgroundSet();
        lp.getFont();
        lp.isFontSet();
        Container c = new Container();
        c.add(lp);
        lp.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            lp.setLocale(locale);

        lp.getColorModel();
        lp.getLocation();

        boolean exceptions = false;
        try {
            lp.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        lp.location();
        lp.setLocation(1, 2);
        lp.move(1, 2);
        lp.setLocation(new Point(1, 2));
        lp.getSize();
        lp.size();
        lp.setSize(1, 32);
        lp.resize(1, 32);
        lp.setSize(new Dimension(1, 32));
        lp.resize(new Dimension(1, 32));
        lp.getBounds();
        lp.bounds();
        lp.setBounds(10, 10, 10, 10);
        lp.setBounds(new Rectangle(10, 10, 10, 10));
        lp.isLightweight();
        lp.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        lp.getCursor();
        lp.isCursorSet();
        lp.inside(1, 2);
        lp.contains(new Point(1, 2));
        lp.isFocusable();
        lp.setFocusable(true);
        lp.setFocusable(false);
        lp.transferFocus();
        lp.getFocusCycleRootAncestor();
        lp.nextFocus();
        lp.transferFocusUpCycle();
        lp.hasFocus();
        lp.isFocusOwner();
        lp.toString();
        lp.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        lp.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        lp.setComponentOrientation(ComponentOrientation.UNKNOWN);
        lp.getComponentOrientation();
    }
}
