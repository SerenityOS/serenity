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
 * @summary Check that JInternalFrame.JDesktopIcon constructor and methods do not throw
 *          unexpected exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJInternalFrame_JDesktopIcon
 */

public class HeadlessJInternalFrame_JDesktopIcon {
    public static void main(String args[]) {
        JInternalFrame.JDesktopIcon jdi = new JInternalFrame.JDesktopIcon(new JInternalFrame());
        jdi.getAccessibleContext();
        jdi.isFocusTraversable();
        jdi.setEnabled(false);
        jdi.setEnabled(true);
        jdi.requestFocus();
        jdi.requestFocusInWindow();
        jdi.getPreferredSize();
        jdi.getMaximumSize();
        jdi.getMinimumSize();
        jdi.contains(1, 2);
        Component c1 = jdi.add(new Component(){});
        Component c2 = jdi.add(new Component(){});
        Component c3 = jdi.add(new Component(){});
        Insets ins = jdi.getInsets();
        jdi.getAlignmentY();
        jdi.getAlignmentX();
        jdi.getGraphics();
        jdi.setVisible(false);
        jdi.setVisible(true);
        jdi.setForeground(Color.red);
        jdi.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                jdi.setFont(f1);
                jdi.setFont(f2);
                jdi.setFont(f3);
                jdi.setFont(f4);

                jdi.getFontMetrics(f1);
                jdi.getFontMetrics(f2);
                jdi.getFontMetrics(f3);
                jdi.getFontMetrics(f4);
            }
        }
        jdi.enable();
        jdi.disable();
        jdi.reshape(10, 10, 10, 10);
        jdi.getBounds(new Rectangle(1, 1, 1, 1));
        jdi.getSize(new Dimension(1, 2));
        jdi.getLocation(new Point(1, 2));
        jdi.getX();
        jdi.getY();
        jdi.getWidth();
        jdi.getHeight();
        jdi.isOpaque();
        jdi.isValidateRoot();
        jdi.isOptimizedDrawingEnabled();
        jdi.isDoubleBuffered();
        jdi.getComponentCount();
        jdi.countComponents();
        jdi.getComponent(1);
        jdi.getComponent(2);
        Component[] cs = jdi.getComponents();
        jdi.getLayout();
        jdi.setLayout(new FlowLayout());
        jdi.doLayout();
        jdi.layout();
        jdi.invalidate();
        jdi.validate();
        jdi.remove(0);
        jdi.remove(c2);
        jdi.removeAll();
        jdi.preferredSize();
        jdi.minimumSize();
        jdi.getComponentAt(1, 2);
        jdi.locate(1, 2);
        jdi.getComponentAt(new Point(1, 2));
        jdi.isFocusCycleRoot(new Container());
        jdi.transferFocusBackward();
        jdi.setName("goober");
        jdi.getName();
        jdi.getParent();
        jdi.getGraphicsConfiguration();
        jdi.getTreeLock();
        jdi.getToolkit();
        jdi.isValid();
        jdi.isDisplayable();
        jdi.isVisible();
        jdi.isShowing();
        jdi.isEnabled();
        jdi.enable(false);
        jdi.enable(true);
        jdi.enableInputMethods(false);
        jdi.enableInputMethods(true);
        jdi.show();
        jdi.show(false);
        jdi.show(true);
        jdi.hide();
        jdi.getForeground();
        jdi.isForegroundSet();
        jdi.getBackground();
        jdi.isBackgroundSet();
        jdi.getFont();
        jdi.isFontSet();
        Container c = new Container();
        c.add(jdi);
        jdi.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            jdi.setLocale(locale);

        jdi.getColorModel();
        jdi.getLocation();

        boolean exceptions = false;
        try {
            jdi.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        jdi.location();
        jdi.setLocation(1, 2);
        jdi.move(1, 2);
        jdi.setLocation(new Point(1, 2));
        jdi.getSize();
        jdi.size();
        jdi.setSize(1, 32);
        jdi.resize(1, 32);
        jdi.setSize(new Dimension(1, 32));
        jdi.resize(new Dimension(1, 32));
        jdi.getBounds();
        jdi.bounds();
        jdi.setBounds(10, 10, 10, 10);
        jdi.setBounds(new Rectangle(10, 10, 10, 10));
        jdi.isLightweight();
        jdi.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        jdi.getCursor();
        jdi.isCursorSet();
        jdi.inside(1, 2);
        jdi.contains(new Point(1, 2));
        jdi.isFocusable();
        jdi.setFocusable(true);
        jdi.setFocusable(false);
        jdi.transferFocus();
        jdi.getFocusCycleRootAncestor();
        jdi.nextFocus();
        jdi.transferFocusUpCycle();
        jdi.hasFocus();
        jdi.isFocusOwner();
        jdi.toString();
        jdi.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        jdi.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        jdi.setComponentOrientation(ComponentOrientation.UNKNOWN);
        jdi.getComponentOrientation();
    }
}
