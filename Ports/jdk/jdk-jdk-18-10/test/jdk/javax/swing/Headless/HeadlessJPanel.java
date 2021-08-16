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
 * @summary Check that JPanel constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJPanel
 */

public class HeadlessJPanel {
    public static void main(String args[]) {
        JPanel p = new JPanel();
        p.getAccessibleContext();
        p.isFocusTraversable();
        p.setEnabled(false);
        p.setEnabled(true);
        p.requestFocus();
        p.requestFocusInWindow();
        p.getPreferredSize();
        p.getMaximumSize();
        p.getMinimumSize();
        p.contains(1, 2);
        Component c1 = p.add(new Component(){});
        Component c2 = p.add(new Component(){});
        Component c3 = p.add(new Component(){});
        Insets ins = p.getInsets();
        p.getAlignmentY();
        p.getAlignmentX();
        p.getGraphics();
        p.setVisible(false);
        p.setVisible(true);
        p.setForeground(Color.red);
        p.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                p.setFont(f1);
                p.setFont(f2);
                p.setFont(f3);
                p.setFont(f4);

                p.getFontMetrics(f1);
                p.getFontMetrics(f2);
                p.getFontMetrics(f3);
                p.getFontMetrics(f4);
            }
        }
        p.enable();
        p.disable();
        p.reshape(10, 10, 10, 10);
        p.getBounds(new Rectangle(1, 1, 1, 1));
        p.getSize(new Dimension(1, 2));
        p.getLocation(new Point(1, 2));
        p.getX();
        p.getY();
        p.getWidth();
        p.getHeight();
        p.isOpaque();
        p.isValidateRoot();
        p.isOptimizedDrawingEnabled();
        p.isDoubleBuffered();
        p.getComponentCount();
        p.countComponents();
        p.getComponent(1);
        p.getComponent(2);
        Component[] cs = p.getComponents();
        p.getLayout();
        p.setLayout(new FlowLayout());
        p.doLayout();
        p.layout();
        p.invalidate();
        p.validate();
        p.remove(0);
        p.remove(c2);
        p.removeAll();
        p.preferredSize();
        p.minimumSize();
        p.getComponentAt(1, 2);
        p.locate(1, 2);
        p.getComponentAt(new Point(1, 2));
        p.isFocusCycleRoot(new Container());
        p.transferFocusBackward();
        p.setName("goober");
        p.getName();
        p.getParent();
        p.getGraphicsConfiguration();
        p.getTreeLock();
        p.getToolkit();
        p.isValid();
        p.isDisplayable();
        p.isVisible();
        p.isShowing();
        p.isEnabled();
        p.enable(false);
        p.enable(true);
        p.enableInputMethods(false);
        p.enableInputMethods(true);
        p.show();
        p.show(false);
        p.show(true);
        p.hide();
        p.getForeground();
        p.isForegroundSet();
        p.getBackground();
        p.isBackgroundSet();
        p.getFont();
        p.isFontSet();
        Container c = new Container();
        c.add(p);
        p.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            p.setLocale(locale);

        p.getColorModel();
        p.getLocation();

        boolean exceptions = false;
        try {
            p.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        p.location();
        p.setLocation(1, 2);
        p.move(1, 2);
        p.setLocation(new Point(1, 2));
        p.getSize();
        p.size();
        p.setSize(1, 32);
        p.resize(1, 32);
        p.setSize(new Dimension(1, 32));
        p.resize(new Dimension(1, 32));
        p.getBounds();
        p.bounds();
        p.setBounds(10, 10, 10, 10);
        p.setBounds(new Rectangle(10, 10, 10, 10));
        p.isLightweight();
        p.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        p.getCursor();
        p.isCursorSet();
        p.inside(1, 2);
        p.contains(new Point(1, 2));
        p.isFocusable();
        p.setFocusable(true);
        p.setFocusable(false);
        p.transferFocus();
        p.getFocusCycleRootAncestor();
        p.nextFocus();
        p.transferFocusUpCycle();
        p.hasFocus();
        p.isFocusOwner();
        p.toString();
        p.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        p.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        p.setComponentOrientation(ComponentOrientation.UNKNOWN);
        p.getComponentOrientation();
    }
}
