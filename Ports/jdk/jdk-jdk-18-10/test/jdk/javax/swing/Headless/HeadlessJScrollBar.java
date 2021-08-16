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
 * @summary Check that JScrollBar constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJScrollBar
 */

public class HeadlessJScrollBar {
    public static void main(String args[]) {
        JScrollBar sb = new JScrollBar();
        sb.getAccessibleContext();
        sb.isFocusTraversable();
        sb.setEnabled(false);
        sb.setEnabled(true);
        sb.requestFocus();
        sb.requestFocusInWindow();
        sb.getPreferredSize();
        sb.getMaximumSize();
        sb.getMinimumSize();
        sb.contains(1, 2);
        Component c1 = sb.add(new Component(){});
        Component c2 = sb.add(new Component(){});
        Component c3 = sb.add(new Component(){});
        Insets ins = sb.getInsets();
        sb.getAlignmentY();
        sb.getAlignmentX();
        sb.getGraphics();
        sb.setVisible(false);
        sb.setVisible(true);
        sb.setForeground(Color.red);
        sb.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                sb.setFont(f1);
                sb.setFont(f2);
                sb.setFont(f3);
                sb.setFont(f4);

                sb.getFontMetrics(f1);
                sb.getFontMetrics(f2);
                sb.getFontMetrics(f3);
                sb.getFontMetrics(f4);
            }
        }
        sb.enable();
        sb.disable();
        sb.reshape(10, 10, 10, 10);
        sb.getBounds(new Rectangle(1, 1, 1, 1));
        sb.getSize(new Dimension(1, 2));
        sb.getLocation(new Point(1, 2));
        sb.getX();
        sb.getY();
        sb.getWidth();
        sb.getHeight();
        sb.isOpaque();
        sb.isValidateRoot();
        sb.isOptimizedDrawingEnabled();
        sb.isDoubleBuffered();
        sb.getComponentCount();
        sb.countComponents();
        sb.getComponent(1);
        sb.getComponent(2);
        Component[] cs = sb.getComponents();
        sb.getLayout();
        sb.setLayout(new FlowLayout());
        sb.doLayout();
        sb.layout();
        sb.invalidate();
        sb.validate();
        sb.remove(0);
        sb.remove(c2);
        sb.removeAll();
        sb.preferredSize();
        sb.minimumSize();
        sb.getComponentAt(1, 2);
        sb.locate(1, 2);
        sb.getComponentAt(new Point(1, 2));
        sb.isFocusCycleRoot(new Container());
        sb.transferFocusBackward();
        sb.setName("goober");
        sb.getName();
        sb.getParent();
        sb.getGraphicsConfiguration();
        sb.getTreeLock();
        sb.getToolkit();
        sb.isValid();
        sb.isDisplayable();
        sb.isVisible();
        sb.isShowing();
        sb.isEnabled();
        sb.enable(false);
        sb.enable(true);
        sb.enableInputMethods(false);
        sb.enableInputMethods(true);
        sb.show();
        sb.show(false);
        sb.show(true);
        sb.hide();
        sb.getForeground();
        sb.isForegroundSet();
        sb.getBackground();
        sb.isBackgroundSet();
        sb.getFont();
        sb.isFontSet();
        Container c = new Container();
        c.add(sb);
        sb.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            sb.setLocale(locale);

        sb.getColorModel();
        sb.getLocation();

        boolean exceptions = false;
        try {
            sb.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        sb.location();
        sb.setLocation(1, 2);
        sb.move(1, 2);
        sb.setLocation(new Point(1, 2));
        sb.getSize();
        sb.size();
        sb.setSize(1, 32);
        sb.resize(1, 32);
        sb.setSize(new Dimension(1, 32));
        sb.resize(new Dimension(1, 32));
        sb.getBounds();
        sb.bounds();
        sb.setBounds(10, 10, 10, 10);
        sb.setBounds(new Rectangle(10, 10, 10, 10));
        sb.isLightweight();
        sb.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        sb.getCursor();
        sb.isCursorSet();
        sb.inside(1, 2);
        sb.contains(new Point(1, 2));
        sb.isFocusable();
        sb.setFocusable(true);
        sb.setFocusable(false);
        sb.transferFocus();
        sb.getFocusCycleRootAncestor();
        sb.nextFocus();
        sb.transferFocusUpCycle();
        sb.hasFocus();
        sb.isFocusOwner();
        sb.toString();
        sb.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        sb.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        sb.setComponentOrientation(ComponentOrientation.UNKNOWN);
        sb.getComponentOrientation();
    }
}
