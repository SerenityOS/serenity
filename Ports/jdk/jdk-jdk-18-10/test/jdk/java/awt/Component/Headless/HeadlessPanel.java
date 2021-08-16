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

import java.awt.*;
import java.util.Locale;

/*
 * @test
 * @summary Check that Panel constructors and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessPanel
 */

public class HeadlessPanel {
    public static void main(String args[]) {
        Panel p;
        p = new Panel();
        p = new Panel(new FlowLayout());
        p.getAccessibleContext();
        Component c1 = p.add(new Component(){});
        Component c2 = p.add(new Component(){});
        Component c3 = p.add(new Component(){});
        p.getComponentCount();
        p.countComponents();
        p.getComponent(1);
        p.getComponent(2);
        Component[] cs = p.getComponents();
        Insets ins = p.getInsets();
        ins = p.insets();
        p.remove(0);
        p.remove((Component) c2);
        p.removeAll();

        p.add(c1);
        p.add(c2);
        p.add(c3);
        p.getLayout();
        p.setLayout(new FlowLayout());
        p.doLayout();
        p.layout();
        p.invalidate();
        p.validate();

        p.getPreferredSize();
        p.preferredSize();
        p.getMinimumSize();
        p.minimumSize();
        p.getMaximumSize();
        p.getAlignmentX();
        p.getAlignmentY();
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
        p.setEnabled(false);
        p.setEnabled(true);
        p.enable();
        p.enable(false);
        p.enable(true);
        p.disable();
        p.isDoubleBuffered();
        p.enableInputMethods(false);
        p.enableInputMethods(true);
        p.setVisible(false);
        p.setVisible(true);
        p.show();
        p.show(false);
        p.show(true);
        p.hide();
        p.getForeground();
        p.setForeground(Color.red);
        p.isForegroundSet();
        p.getBackground();
        p.setBackground(Color.red);
        p.isBackgroundSet();
        p.getFont();
        p.isFontSet();
        p.getColorModel();
        p.getLocation();
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
        p.reshape(10, 10, 10, 10);
        p.setBounds(new Rectangle(10, 10, 10, 10));
        p.getX();
        p.getY();
        p.getWidth();
        p.getHeight();
        p.getBounds(new Rectangle(1, 1, 1, 1));
        p.getSize(new Dimension(1, 2));
        p.getLocation(new Point(1, 2));
        p.isOpaque();
        p.isLightweight();
        p.getGraphics();


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
            }
        }

        boolean exceptions = false;
        try {
            Container c = new Container();
            p = new Panel();
            c.add(p);
            p.getLocale();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        for (Locale locale : Locale.getAvailableLocales())
            p.setLocale(locale);

        exceptions = false;
        try {
            p.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j
                    < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                p.getFontMetrics(f1);
                p.getFontMetrics(f2);
                p.getFontMetrics(f3);
                p.getFontMetrics(f4);
            }
        }

        Cursor c = new Cursor(Cursor.CROSSHAIR_CURSOR);
        p.setCursor(c);
        p.getCursor();
        p.isCursorSet();
        p.contains(1, 2);
        p.inside(1, 2);
        p.contains(new Point(1, 2));
        p.isFocusTraversable();
        p.isFocusable();
        p.setFocusable(true);
        p.setFocusable(false);
        p.requestFocus();
        p.requestFocusInWindow();
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
