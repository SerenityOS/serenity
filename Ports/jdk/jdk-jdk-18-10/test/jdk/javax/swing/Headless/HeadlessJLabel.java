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
 * @summary Check that JLabel constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJLabel
 */

public class HeadlessJLabel {
    public static void main(String args[]) {
        JLabel l = new JLabel("<html><body><bold>Foo</bold></body></html>");
        l.getAccessibleContext();
        l.isFocusTraversable();
        l.setEnabled(false);
        l.setEnabled(true);
        l.requestFocus();
        l.requestFocusInWindow();
        l.getPreferredSize();
        l.getMaximumSize();
        l.getMinimumSize();
        l.contains(1, 2);
        Component c1 = l.add(new Component(){});
        Component c2 = l.add(new Component(){});
        Component c3 = l.add(new Component(){});
        Insets ins = l.getInsets();
        l.getAlignmentY();
        l.getAlignmentX();
        l.getGraphics();
        l.setVisible(false);
        l.setVisible(true);
        l.setForeground(Color.red);
        l.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                l.setFont(f1);
                l.setFont(f2);
                l.setFont(f3);
                l.setFont(f4);

                l.getFontMetrics(f1);
                l.getFontMetrics(f2);
                l.getFontMetrics(f3);
                l.getFontMetrics(f4);
            }
        }
        l.enable();
        l.disable();
        l.reshape(10, 10, 10, 10);
        l.getBounds(new Rectangle(1, 1, 1, 1));
        l.getSize(new Dimension(1, 2));
        l.getLocation(new Point(1, 2));
        l.getX();
        l.getY();
        l.getWidth();
        l.getHeight();
        l.isOpaque();
        l.isValidateRoot();
        l.isOptimizedDrawingEnabled();
        l.isDoubleBuffered();
        l.getComponentCount();
        l.countComponents();
        l.getComponent(1);
        l.getComponent(2);
        Component[] cs = l.getComponents();
        l.getLayout();
        l.setLayout(new FlowLayout());
        l.doLayout();
        l.layout();
        l.invalidate();
        l.validate();
        l.remove(0);
        l.remove(c2);
        l.removeAll();
        l.preferredSize();
        l.minimumSize();
        l.getComponentAt(1, 2);
        l.locate(1, 2);
        l.getComponentAt(new Point(1, 2));
        l.isFocusCycleRoot(new Container());
        l.transferFocusBackward();
        l.setName("goober");
        l.getName();
        l.getParent();
        l.getGraphicsConfiguration();
        l.getTreeLock();
        l.getToolkit();
        l.isValid();
        l.isDisplayable();
        l.isVisible();
        l.isShowing();
        l.isEnabled();
        l.enable(false);
        l.enable(true);
        l.enableInputMethods(false);
        l.enableInputMethods(true);
        l.show();
        l.show(false);
        l.show(true);
        l.hide();
        l.getForeground();
        l.isForegroundSet();
        l.getBackground();
        l.isBackgroundSet();
        l.getFont();
        l.isFontSet();
        Container c = new Container();
        c.add(l);
        l.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            l.setLocale(locale);

        l.getColorModel();
        l.getLocation();

        boolean exceptions = false;
        try {
            l.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        l.location();
        l.setLocation(1, 2);
        l.move(1, 2);
        l.setLocation(new Point(1, 2));
        l.getSize();
        l.size();
        l.setSize(1, 32);
        l.resize(1, 32);
        l.setSize(new Dimension(1, 32));
        l.resize(new Dimension(1, 32));
        l.getBounds();
        l.bounds();
        l.setBounds(10, 10, 10, 10);
        l.setBounds(new Rectangle(10, 10, 10, 10));
        l.isLightweight();
        l.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        l.getCursor();
        l.isCursorSet();
        l.inside(1, 2);
        l.contains(new Point(1, 2));
        l.isFocusable();
        l.setFocusable(true);
        l.setFocusable(false);
        l.transferFocus();
        l.getFocusCycleRootAncestor();
        l.nextFocus();
        l.transferFocusUpCycle();
        l.hasFocus();
        l.isFocusOwner();
        l.toString();
        l.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        l.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        l.setComponentOrientation(ComponentOrientation.UNKNOWN);
        l.getComponentOrientation();
    }
}
