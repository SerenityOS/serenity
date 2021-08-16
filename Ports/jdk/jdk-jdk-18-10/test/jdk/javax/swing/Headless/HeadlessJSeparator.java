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
 * @summary Check that JSeparator constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJSeparator
 */

public class HeadlessJSeparator {
    public static void main(String args[]) {
        JSeparator sp = new JSeparator();
        sp.getAccessibleContext();
        sp.isFocusTraversable();
        sp.setEnabled(false);
        sp.setEnabled(true);
        sp.requestFocus();
        sp.requestFocusInWindow();
        sp.getPreferredSize();
        sp.getMaximumSize();
        sp.getMinimumSize();
        sp.contains(1, 2);
        Component c1 = sp.add(new Component(){});
        Component c2 = sp.add(new Component(){});
        Component c3 = sp.add(new Component(){});
        Insets ins = sp.getInsets();
        sp.getAlignmentY();
        sp.getAlignmentX();
        sp.getGraphics();
        sp.setVisible(false);
        sp.setVisible(true);
        sp.setForeground(Color.red);
        sp.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                sp.setFont(f1);
                sp.setFont(f2);
                sp.setFont(f3);
                sp.setFont(f4);

                sp.getFontMetrics(f1);
                sp.getFontMetrics(f2);
                sp.getFontMetrics(f3);
                sp.getFontMetrics(f4);
            }
        }
        sp.enable();
        sp.disable();
        sp.reshape(10, 10, 10, 10);
        sp.getBounds(new Rectangle(1, 1, 1, 1));
        sp.getSize(new Dimension(1, 2));
        sp.getLocation(new Point(1, 2));
        sp.getX();
        sp.getY();
        sp.getWidth();
        sp.getHeight();
        sp.isOpaque();
        sp.isValidateRoot();
        sp.isOptimizedDrawingEnabled();
        sp.isDoubleBuffered();
        sp.getComponentCount();
        sp.countComponents();
        sp.getComponent(1);
        sp.getComponent(2);
        Component[] cs = sp.getComponents();
        sp.getLayout();
        sp.setLayout(new FlowLayout());
        sp.doLayout();
        sp.layout();
        sp.invalidate();
        sp.validate();
        sp.remove(0);
        sp.remove(c2);
        sp.removeAll();
        sp.preferredSize();
        sp.minimumSize();
        sp.getComponentAt(1, 2);
        sp.locate(1, 2);
        sp.getComponentAt(new Point(1, 2));
        sp.isFocusCycleRoot(new Container());
        sp.transferFocusBackward();
        sp.setName("goober");
        sp.getName();
        sp.getParent();
        sp.getGraphicsConfiguration();
        sp.getTreeLock();
        sp.getToolkit();
        sp.isValid();
        sp.isDisplayable();
        sp.isVisible();
        sp.isShowing();
        sp.isEnabled();
        sp.enable(false);
        sp.enable(true);
        sp.enableInputMethods(false);
        sp.enableInputMethods(true);
        sp.show();
        sp.show(false);
        sp.show(true);
        sp.hide();
        sp.getForeground();
        sp.isForegroundSet();
        sp.getBackground();
        sp.isBackgroundSet();
        sp.getFont();
        sp.isFontSet();
        Container c = new Container();
        c.add(sp);
        sp.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            sp.setLocale(locale);

        sp.getColorModel();
        sp.getLocation();

        boolean exceptions = false;
        try {
            sp.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        sp.location();
        sp.setLocation(1, 2);
        sp.move(1, 2);
        sp.setLocation(new Point(1, 2));
        sp.getSize();
        sp.size();
        sp.setSize(1, 32);
        sp.resize(1, 32);
        sp.setSize(new Dimension(1, 32));
        sp.resize(new Dimension(1, 32));
        sp.getBounds();
        sp.bounds();
        sp.setBounds(10, 10, 10, 10);
        sp.setBounds(new Rectangle(10, 10, 10, 10));
        sp.isLightweight();
        sp.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        sp.getCursor();
        sp.isCursorSet();
        sp.inside(1, 2);
        sp.contains(new Point(1, 2));
        sp.isFocusable();
        sp.setFocusable(true);
        sp.setFocusable(false);
        sp.transferFocus();
        sp.getFocusCycleRootAncestor();
        sp.nextFocus();
        sp.transferFocusUpCycle();
        sp.hasFocus();
        sp.isFocusOwner();
        sp.toString();
        sp.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        sp.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        sp.setComponentOrientation(ComponentOrientation.UNKNOWN);
        sp.getComponentOrientation();
    }
}
