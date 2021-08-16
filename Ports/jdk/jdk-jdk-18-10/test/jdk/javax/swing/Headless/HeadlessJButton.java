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
 * @summary Check that JButton constructors and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJButton
 */

public class HeadlessJButton {
    public static void main(String args[]) {
        JButton b = new JButton();
        b = new JButton("Press me");
        b.getAccessibleContext();
        b.isFocusTraversable();
        b.setEnabled(false);
        b.setEnabled(true);
        b.requestFocus();
        b.requestFocusInWindow();
        b.getPreferredSize();
        b.getMaximumSize();
        b.getMinimumSize();
        b.contains(1, 2);
        Component c1 = b.add(new Component(){});
        Component c2 = b.add(new Component(){});
        Component c3 = b.add(new Component(){});
        Insets ins = b.getInsets();
        b.getAlignmentY();
        b.getAlignmentX();
        b.getGraphics();
        b.setVisible(false);
        b.setVisible(true);
        b.setForeground(Color.red);
        b.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                b.setFont(f1);
                b.setFont(f2);
                b.setFont(f3);
                b.setFont(f4);

                b.getFontMetrics(f1);
                b.getFontMetrics(f2);
                b.getFontMetrics(f3);
                b.getFontMetrics(f4);
            }
        }
        b.enable();
        b.disable();
        b.reshape(10, 10, 10, 10);
        b.getBounds(new Rectangle(1, 1, 1, 1));
        b.getSize(new Dimension(1, 2));
        b.getLocation(new Point(1, 2));
        b.getX();
        b.getY();
        b.getWidth();
        b.getHeight();
        b.isOpaque();
        b.isValidateRoot();
        b.isOptimizedDrawingEnabled();
        b.isDoubleBuffered();
        b.getComponentCount();
        b.countComponents();
        b.getComponent(1);
        b.getComponent(2);
        Component[] cs = b.getComponents();
        b.getLayout();
        b.setLayout(new FlowLayout());
        b.doLayout();
        b.layout();
        b.invalidate();
        b.validate();
        b.remove(0);
        b.remove(c2);
        b.removeAll();
        b.preferredSize();
        b.minimumSize();
        b.getComponentAt(1, 2);
        b.locate(1, 2);
        b.getComponentAt(new Point(1, 2));
        b.isFocusCycleRoot(new Container());
        b.transferFocusBackward();
        b.setName("goober");
        b.getName();
        b.getParent();
        b.getGraphicsConfiguration();
        b.getTreeLock();
        b.getToolkit();
        b.isValid();
        b.isDisplayable();
        b.isVisible();
        b.isShowing();
        b.isEnabled();
        b.enable(false);
        b.enable(true);
        b.enableInputMethods(false);
        b.enableInputMethods(true);
        b.show();
        b.show(false);
        b.show(true);
        b.hide();
        b.getForeground();
        b.isForegroundSet();
        b.getBackground();
        b.isBackgroundSet();
        b.getFont();
        b.isFontSet();
        Container c = new Container();
        c.add(b);
        b.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            b.setLocale(locale);

        b.getColorModel();
        b.getLocation();

        boolean exceptions = false;
        try {
            b.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        b.location();
        b.setLocation(1, 2);
        b.move(1, 2);
        b.setLocation(new Point(1, 2));
        b.getSize();
        b.size();
        b.setSize(1, 32);
        b.resize(1, 32);
        b.setSize(new Dimension(1, 32));
        b.resize(new Dimension(1, 32));
        b.getBounds();
        b.bounds();
        b.setBounds(10, 10, 10, 10);
        b.setBounds(new Rectangle(10, 10, 10, 10));
        b.isLightweight();
        b.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        b.getCursor();
        b.isCursorSet();
        b.inside(1, 2);
        b.contains(new Point(1, 2));
        b.isFocusable();
        b.setFocusable(true);
        b.setFocusable(false);
        b.transferFocus();
        b.getFocusCycleRootAncestor();
        b.nextFocus();
        b.transferFocusUpCycle();
        b.hasFocus();
        b.isFocusOwner();
        b.toString();
        b.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        b.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        b.setComponentOrientation(ComponentOrientation.UNKNOWN);
        b.getComponentOrientation();
    }
}
