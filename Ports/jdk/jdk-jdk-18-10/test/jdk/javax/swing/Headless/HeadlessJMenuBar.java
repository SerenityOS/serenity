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
 * @summary Check that JMenuBar constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJMenuBar
 */

public class HeadlessJMenuBar {
    public static void main(String args[]) {
        JMenuBar mb = new JMenuBar();
        mb.getAccessibleContext();
        mb.isFocusTraversable();
        mb.setEnabled(false);
        mb.setEnabled(true);
        mb.requestFocus();
        mb.requestFocusInWindow();
        mb.getPreferredSize();
        mb.getMaximumSize();
        mb.getMinimumSize();
        mb.contains(1, 2);
        Component c1 = mb.add(new Component(){});
        Component c2 = mb.add(new Component(){});
        Component c3 = mb.add(new Component(){});
        Insets ins = mb.getInsets();
        mb.getAlignmentY();
        mb.getAlignmentX();
        mb.getGraphics();
        mb.setVisible(false);
        mb.setVisible(true);
        mb.setForeground(Color.red);
        mb.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                mb.setFont(f1);
                mb.setFont(f2);
                mb.setFont(f3);
                mb.setFont(f4);

                mb.getFontMetrics(f1);
                mb.getFontMetrics(f2);
                mb.getFontMetrics(f3);
                mb.getFontMetrics(f4);
            }
        }
        mb.enable();
        mb.disable();
        mb.reshape(10, 10, 10, 10);
        mb.getBounds(new Rectangle(1, 1, 1, 1));
        mb.getSize(new Dimension(1, 2));
        mb.getLocation(new Point(1, 2));
        mb.getX();
        mb.getY();
        mb.getWidth();
        mb.getHeight();
        mb.isOpaque();
        mb.isValidateRoot();
        mb.isOptimizedDrawingEnabled();
        mb.isDoubleBuffered();
        mb.getComponentCount();
        mb.countComponents();
        mb.getComponent(1);
        mb.getComponent(2);
        Component[] cs = mb.getComponents();
        mb.getLayout();
        mb.setLayout(new FlowLayout());
        mb.doLayout();
        mb.layout();
        mb.invalidate();
        mb.validate();
        mb.remove(0);
        mb.remove(c2);
        mb.removeAll();
        mb.preferredSize();
        mb.minimumSize();
        mb.getComponentAt(1, 2);
        mb.locate(1, 2);
        mb.getComponentAt(new Point(1, 2));
        mb.isFocusCycleRoot(new Container());
        mb.transferFocusBackward();
        mb.setName("goober");
        mb.getName();
        mb.getParent();
        mb.getGraphicsConfiguration();
        mb.getTreeLock();
        mb.getToolkit();
        mb.isValid();
        mb.isDisplayable();
        mb.isVisible();
        mb.isShowing();
        mb.isEnabled();
        mb.enable(false);
        mb.enable(true);
        mb.enableInputMethods(false);
        mb.enableInputMethods(true);
        mb.show();
        mb.show(false);
        mb.show(true);
        mb.hide();
        mb.getForeground();
        mb.isForegroundSet();
        mb.getBackground();
        mb.isBackgroundSet();
        mb.getFont();
        mb.isFontSet();
        Container c = new Container();
        c.add(mb);
        mb.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            mb.setLocale(locale);

        mb.getColorModel();
        mb.getLocation();

        boolean exceptions = false;
        try {
            mb.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        mb.location();
        mb.setLocation(1, 2);
        mb.move(1, 2);
        mb.setLocation(new Point(1, 2));
        mb.getSize();
        mb.size();
        mb.setSize(1, 32);
        mb.resize(1, 32);
        mb.setSize(new Dimension(1, 32));
        mb.resize(new Dimension(1, 32));
        mb.getBounds();
        mb.bounds();
        mb.setBounds(10, 10, 10, 10);
        mb.setBounds(new Rectangle(10, 10, 10, 10));
        mb.isLightweight();
        mb.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        mb.getCursor();
        mb.isCursorSet();
        mb.inside(1, 2);
        mb.contains(new Point(1, 2));
        mb.isFocusable();
        mb.setFocusable(true);
        mb.setFocusable(false);
        mb.transferFocus();
        mb.getFocusCycleRootAncestor();
        mb.nextFocus();
        mb.transferFocusUpCycle();
        mb.hasFocus();
        mb.isFocusOwner();
        mb.toString();
        mb.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        mb.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        mb.setComponentOrientation(ComponentOrientation.UNKNOWN);
        mb.getComponentOrientation();
    }
}
