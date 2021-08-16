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
 * @summary Check that JRadioButtonMenuItem constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJRadioButtonMenuItem
 */

public class HeadlessJRadioButtonMenuItem {
    public static void main(String args[]) {
        JRadioButtonMenuItem i = new JRadioButtonMenuItem();
        i.getAccessibleContext();
        i.isFocusTraversable();
        i.setEnabled(false);
        i.setEnabled(true);
        i.requestFocus();
        i.requestFocusInWindow();
        i.getPreferredSize();
        i.getMaximumSize();
        i.getMinimumSize();
        i.contains(1, 2);
        Component c1 = i.add(new Component(){});
        Component c2 = i.add(new Component(){});
        Component c3 = i.add(new Component(){});
        Insets ins = i.getInsets();
        i.getAlignmentY();
        i.getAlignmentX();
        i.getGraphics();
        i.setVisible(false);
        i.setVisible(true);
        i.setForeground(Color.red);
        i.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                i.setFont(f1);
                i.setFont(f2);
                i.setFont(f3);
                i.setFont(f4);

                i.getFontMetrics(f1);
                i.getFontMetrics(f2);
                i.getFontMetrics(f3);
                i.getFontMetrics(f4);
            }
        }
        i.enable();
        i.disable();
        i.reshape(10, 10, 10, 10);
        i.getBounds(new Rectangle(1, 1, 1, 1));
        i.getSize(new Dimension(1, 2));
        i.getLocation(new Point(1, 2));
        i.getX();
        i.getY();
        i.getWidth();
        i.getHeight();
        i.isOpaque();
        i.isValidateRoot();
        i.isOptimizedDrawingEnabled();
        i.isDoubleBuffered();
        i.getComponentCount();
        i.countComponents();
        i.getComponent(1);
        i.getComponent(2);
        Component[] cs = i.getComponents();
        i.getLayout();
        i.setLayout(new FlowLayout());
        i.doLayout();
        i.layout();
        i.invalidate();
        i.validate();
        i.remove(0);
        i.remove(c2);
        i.removeAll();
        i.preferredSize();
        i.minimumSize();
        i.getComponentAt(1, 2);
        i.locate(1, 2);
        i.getComponentAt(new Point(1, 2));
        i.isFocusCycleRoot(new Container());
        i.transferFocusBackward();
        i.setName("goober");
        i.getName();
        i.getParent();
        i.getGraphicsConfiguration();
        i.getTreeLock();
        i.getToolkit();
        i.isValid();
        i.isDisplayable();
        i.isVisible();
        i.isShowing();
        i.isEnabled();
        i.enable(false);
        i.enable(true);
        i.enableInputMethods(false);
        i.enableInputMethods(true);
        i.show();
        i.show(false);
        i.show(true);
        i.hide();
        i.getForeground();
        i.isForegroundSet();
        i.getBackground();
        i.isBackgroundSet();
        i.getFont();
        i.isFontSet();
        Container c = new Container();
        c.add(i);
        i.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            i.setLocale(locale);

        i.getColorModel();
        i.getLocation();

        boolean exceptions = false;
        try {
            i.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        i.location();
        i.setLocation(1, 2);
        i.move(1, 2);
        i.setLocation(new Point(1, 2));
        i.getSize();
        i.size();
        i.setSize(1, 32);
        i.resize(1, 32);
        i.setSize(new Dimension(1, 32));
        i.resize(new Dimension(1, 32));
        i.getBounds();
        i.bounds();
        i.setBounds(10, 10, 10, 10);
        i.setBounds(new Rectangle(10, 10, 10, 10));
        i.isLightweight();
        i.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        i.getCursor();
        i.isCursorSet();
        i.inside(1, 2);
        i.contains(new Point(1, 2));
        i.isFocusable();
        i.setFocusable(true);
        i.setFocusable(false);
        i.transferFocus();
        i.getFocusCycleRootAncestor();
        i.nextFocus();
        i.transferFocusUpCycle();
        i.hasFocus();
        i.isFocusOwner();
        i.toString();
        i.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        i.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        i.setComponentOrientation(ComponentOrientation.UNKNOWN);
        i.getComponentOrientation();
    }
}
