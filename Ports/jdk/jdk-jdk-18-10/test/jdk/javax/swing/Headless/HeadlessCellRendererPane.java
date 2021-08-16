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
 * @summary Check that CellRendererPane constructors and methods do not throw
 *          unexpected exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessCellRendererPane
 */

public class HeadlessCellRendererPane {
    public static void main(String args[]) {
        CellRendererPane crp = new CellRendererPane();
        Component c1 = crp.add(new Component(){});
        Component c2 = crp.add(new Component(){});
        Component c3 = crp.add(new Component(){});
        crp.setLayout(new FlowLayout());
        crp.invalidate();
        crp.getAccessibleContext();
        crp.getComponentCount();
        crp.countComponents();
        crp.getComponent(1);
        crp.getComponent(2);
        Component[] cs = crp.getComponents();
        Insets ins = crp.getInsets();
        ins = crp.insets();
        crp.getLayout();
        crp.setLayout(new FlowLayout());
        crp.setLayout(new FlowLayout());
        crp.doLayout();
        crp.layout();
        crp.invalidate();
        crp.validate();
        crp.remove(0);
        crp.remove((Component) c2);
        crp.removeAll();
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                crp.setFont(f1);
                crp.setFont(f2);
                crp.setFont(f3);
                crp.setFont(f4);

                crp.getFontMetrics(f1);
                crp.getFontMetrics(f2);
                crp.getFontMetrics(f3);
                crp.getFontMetrics(f4);
            }
        }
        crp.getPreferredSize();
        crp.preferredSize();
        crp.getMinimumSize();
        crp.minimumSize();
        crp.getMaximumSize();
        crp.getAlignmentX();
        crp.getAlignmentY();
        crp.getComponentAt(1, 2);
        crp.locate(1, 2);
        crp.getComponentAt(new Point(1, 2));
        crp.isFocusCycleRoot(new Container());
        crp.transferFocusBackward();
        crp.setName("goober");
        crp.getName();
        crp.getParent();
        crp.getGraphicsConfiguration();
        crp.getTreeLock();
        crp.getToolkit();
        crp.isValid();
        crp.isDisplayable();
        crp.isVisible();
        crp.isShowing();
        crp.isEnabled();
        crp.setEnabled(false);
        crp.setEnabled(true);
        crp.enable();
        crp.enable(false);
        crp.enable(true);
        crp.disable();
        crp.isDoubleBuffered();
        crp.enableInputMethods(false);
        crp.enableInputMethods(true);
        crp.setVisible(false);
        crp.setVisible(true);
        crp.show();
        crp.show(false);
        crp.show(true);
        crp.hide();
        crp.getForeground();
        crp.setForeground(Color.red);
        crp.isForegroundSet();
        crp.getBackground();
        crp.setBackground(Color.red);
        crp.isBackgroundSet();
        crp.getFont();
        crp.isFontSet();

        boolean exceptions = false;
        try {
            Container c = new Container();
            c.add(crp);
            crp.getLocale();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        for (Locale locale : Locale.getAvailableLocales())
            crp.setLocale(locale);

        crp.getColorModel();
        crp.getLocation();

        exceptions = false;
        try {
            crp.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        crp.location();
        crp.setLocation(1, 2);
        crp.move(1, 2);
        crp.setLocation(new Point(1, 2));
        crp.getSize();
        crp.size();
        crp.setSize(1, 32);
        crp.resize(1, 32);
        crp.setSize(new Dimension(1, 32));
        crp.resize(new Dimension(1, 32));
        crp.getBounds();
        crp.bounds();
        crp.setBounds(10, 10, 10, 10);
        crp.reshape(10, 10, 10, 10);
        crp.setBounds(new Rectangle(10, 10, 10, 10));
        crp.getX();
        crp.getY();
        crp.getWidth();
        crp.getHeight();
        crp.getBounds(new Rectangle(1, 1, 1, 1));
        crp.getSize(new Dimension(1, 2));
        crp.getLocation(new Point(1, 2));
        crp.isOpaque();
        crp.isLightweight();
        crp.getGraphics();
        crp.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        crp.getCursor();
        crp.isCursorSet();
        crp.contains(1, 2);
        crp.inside(1, 2);
        crp.contains(new Point(1, 2));
        crp.isFocusTraversable();
        crp.isFocusable();
        crp.setFocusable(true);
        crp.setFocusable(false);
        crp.requestFocus();
        crp.requestFocusInWindow();
        crp.transferFocus();
        crp.getFocusCycleRootAncestor();
        crp.nextFocus();
        crp.transferFocusUpCycle();
        crp.hasFocus();
        crp.isFocusOwner();
        crp.toString();
        crp.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        crp.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        crp.setComponentOrientation(ComponentOrientation.UNKNOWN);
        crp.getComponentOrientation();
    }
}
