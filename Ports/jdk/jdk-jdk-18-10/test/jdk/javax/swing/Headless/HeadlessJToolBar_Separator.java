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
 * @summary Check that JToolBar.Separator constructors and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJToolBar_Separator
 */

public class HeadlessJToolBar_Separator {
    public static void main(String args[]) {
        JToolBar.Separator s = new JToolBar.Separator();
        s.getAccessibleContext();
        s.isFocusTraversable();
        s.setEnabled(false);
        s.setEnabled(true);
        s.requestFocus();
        s.requestFocusInWindow();
        s.getPreferredSize();
        s.getMaximumSize();
        s.getMinimumSize();
        s.contains(1, 2);
        Component c1 = s.add(new Component(){});
        Component c2 = s.add(new Component(){});
        Component c3 = s.add(new Component(){});
        Insets ins = s.getInsets();
        s.getAlignmentY();
        s.getAlignmentX();
        s.getGraphics();
        s.setVisible(false);
        s.setVisible(true);
        s.setForeground(Color.red);
        s.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                s.setFont(f1);
                s.setFont(f2);
                s.setFont(f3);
                s.setFont(f4);

                s.getFontMetrics(f1);
                s.getFontMetrics(f2);
                s.getFontMetrics(f3);
                s.getFontMetrics(f4);
            }
        }
        s.enable();
        s.disable();
        s.reshape(10, 10, 10, 10);
        s.getBounds(new Rectangle(1, 1, 1, 1));
        s.getSize(new Dimension(1, 2));
        s.getLocation(new Point(1, 2));
        s.getX();
        s.getY();
        s.getWidth();
        s.getHeight();
        s.isOpaque();
        s.isValidateRoot();
        s.isOptimizedDrawingEnabled();
        s.isDoubleBuffered();
        s.getComponentCount();
        s.countComponents();
        s.getComponent(1);
        s.getComponent(2);
        Component[] cs = s.getComponents();
        s.getLayout();
        s.setLayout(new FlowLayout());
        s.doLayout();
        s.layout();
        s.invalidate();
        s.validate();
        s.remove(0);
        s.remove(c2);
        s.removeAll();
        s.preferredSize();
        s.minimumSize();
        s.getComponentAt(1, 2);
        s.locate(1, 2);
        s.getComponentAt(new Point(1, 2));
        s.isFocusCycleRoot(new Container());
        s.transferFocusBackward();
        s.setName("goober");
        s.getName();
        s.getParent();
        s.getGraphicsConfiguration();
        s.getTreeLock();
        s.getToolkit();
        s.isValid();
        s.isDisplayable();
        s.isVisible();
        s.isShowing();
        s.isEnabled();
        s.enable(false);
        s.enable(true);
        s.enableInputMethods(false);
        s.enableInputMethods(true);
        s.show();
        s.show(false);
        s.show(true);
        s.hide();
        s.getForeground();
        s.isForegroundSet();
        s.getBackground();
        s.isBackgroundSet();
        s.getFont();
        s.isFontSet();
        Container c = new Container();
        c.add(s);
        s.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            s.setLocale(locale);

        s.getColorModel();
        s.getLocation();

        boolean exceptions = false;
        try {
            s.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        s.location();
        s.setLocation(1, 2);
        s.move(1, 2);
        s.setLocation(new Point(1, 2));
        s.getSize();
        s.size();
        s.setSize(1, 32);
        s.resize(1, 32);
        s.setSize(new Dimension(1, 32));
        s.resize(new Dimension(1, 32));
        s.getBounds();
        s.bounds();
        s.setBounds(10, 10, 10, 10);
        s.setBounds(new Rectangle(10, 10, 10, 10));
        s.isLightweight();
        s.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        s.getCursor();
        s.isCursorSet();
        s.inside(1, 2);
        s.contains(new Point(1, 2));
        s.isFocusable();
        s.setFocusable(true);
        s.setFocusable(false);
        s.transferFocus();
        s.getFocusCycleRootAncestor();
        s.nextFocus();
        s.transferFocusUpCycle();
        s.hasFocus();
        s.isFocusOwner();
        s.toString();
        s.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        s.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        s.setComponentOrientation(ComponentOrientation.UNKNOWN);
        s.getComponentOrientation();
    }
}
