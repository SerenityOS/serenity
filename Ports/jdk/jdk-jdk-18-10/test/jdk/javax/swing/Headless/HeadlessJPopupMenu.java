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
 * @summary Check that JPopupMenu constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJPopupMenu
 */

public class HeadlessJPopupMenu {
    public static void main(String args[]) {
        JMenu m = new JMenu();
        m.getAccessibleContext();
        m.isFocusTraversable();
        m.setEnabled(false);
        m.setEnabled(true);
        m.requestFocus();
        m.requestFocusInWindow();
        m.getPreferredSize();
        m.getMaximumSize();
        m.getMinimumSize();
        m.contains(1, 2);
        Component c1 = m.add(new Component(){});
        Component c2 = m.add(new Component(){});
        Component c3 = m.add(new Component(){});
        Insets ins = m.getInsets();
        m.getAlignmentY();
        m.getAlignmentX();
        m.getGraphics();
        m.setVisible(false);
        m.setVisible(true);
        m.setForeground(Color.red);
        m.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                m.setFont(f1);
                m.setFont(f2);
                m.setFont(f3);
                m.setFont(f4);

                m.getFontMetrics(f1);
                m.getFontMetrics(f2);
                m.getFontMetrics(f3);
                m.getFontMetrics(f4);
            }
        }
        m.enable();
        m.disable();
        m.reshape(10, 10, 10, 10);
        m.getBounds(new Rectangle(1, 1, 1, 1));
        m.getSize(new Dimension(1, 2));
        m.getLocation(new Point(1, 2));
        m.getX();
        m.getY();
        m.getWidth();
        m.getHeight();
        m.isOpaque();
        m.isValidateRoot();
        m.isOptimizedDrawingEnabled();
        m.isDoubleBuffered();
        m.getComponentCount();
        m.countComponents();
        Component[] cs = m.getComponents();
        m.getLayout();
        m.setLayout(new FlowLayout());
        m.doLayout();
        m.layout();
        m.invalidate();
        m.validate();
        m.remove(0);
        m.remove(c2);
        m.removeAll();
        m.preferredSize();
        m.minimumSize();
        m.getComponentAt(1, 2);
        m.locate(1, 2);
        m.getComponentAt(new Point(1, 2));
        m.isFocusCycleRoot(new Container());
        m.transferFocusBackward();
        m.setName("goober");
        m.getName();
        m.getParent();
        m.getGraphicsConfiguration();
        m.getTreeLock();
        m.getToolkit();
        m.isValid();
        m.isDisplayable();
        m.isVisible();
        m.isShowing();
        m.isEnabled();
        m.enable(false);
        m.enable(true);
        m.enableInputMethods(false);
        m.enableInputMethods(true);
        m.show();
        m.show(false);
        m.show(true);
        m.hide();
        m.getForeground();
        m.isForegroundSet();
        m.getBackground();
        m.isBackgroundSet();
        m.getFont();
        m.isFontSet();
        Container c = new Container();
        c.add(m);
        m.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            m.setLocale(locale);

        m.getColorModel();
        m.getLocation();

        boolean exceptions = false;
        try {
            m.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        m.location();
        m.setLocation(1, 2);
        m.move(1, 2);
        m.setLocation(new Point(1, 2));
        m.getSize();
        m.size();
        m.setSize(1, 32);
        m.resize(1, 32);
        m.setSize(new Dimension(1, 32));
        m.resize(new Dimension(1, 32));
        m.getBounds();
        m.bounds();
        m.setBounds(10, 10, 10, 10);
        m.setBounds(new Rectangle(10, 10, 10, 10));
        m.isLightweight();
        m.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        m.getCursor();
        m.isCursorSet();
        m.inside(1, 2);
        m.contains(new Point(1, 2));
        m.isFocusable();
        m.setFocusable(true);
        m.setFocusable(false);
        m.transferFocus();
        m.getFocusCycleRootAncestor();
        m.nextFocus();
        m.transferFocusUpCycle();
        m.hasFocus();
        m.isFocusOwner();
        m.toString();
        m.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        m.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        m.setComponentOrientation(ComponentOrientation.UNKNOWN);
        m.getComponentOrientation();
    }
}
