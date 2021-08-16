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
 * @summary Check that JRadioButton constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJRadioButton
 */

public class HeadlessJRadioButton {
    public static void main(String args[]) {
        JRadioButton rb = new JRadioButton();
        rb.getAccessibleContext();
        rb.isFocusTraversable();
        rb.setEnabled(false);
        rb.setEnabled(true);
        rb.requestFocus();
        rb.requestFocusInWindow();
        rb.getPreferredSize();
        rb.getMaximumSize();
        rb.getMinimumSize();
        rb.contains(1, 2);
        Component c1 = rb.add(new Component(){});
        Component c2 = rb.add(new Component(){});
        Component c3 = rb.add(new Component(){});
        Insets ins = rb.getInsets();
        rb.getAlignmentY();
        rb.getAlignmentX();
        rb.getGraphics();
        rb.setVisible(false);
        rb.setVisible(true);
        rb.setForeground(Color.red);
        rb.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                rb.setFont(f1);
                rb.setFont(f2);
                rb.setFont(f3);
                rb.setFont(f4);

                rb.getFontMetrics(f1);
                rb.getFontMetrics(f2);
                rb.getFontMetrics(f3);
                rb.getFontMetrics(f4);
            }
        }
        rb.enable();
        rb.disable();
        rb.reshape(10, 10, 10, 10);
        rb.getBounds(new Rectangle(1, 1, 1, 1));
        rb.getSize(new Dimension(1, 2));
        rb.getLocation(new Point(1, 2));
        rb.getX();
        rb.getY();
        rb.getWidth();
        rb.getHeight();
        rb.isOpaque();
        rb.isValidateRoot();
        rb.isOptimizedDrawingEnabled();
        rb.isDoubleBuffered();
        rb.getComponentCount();
        rb.countComponents();
        rb.getComponent(1);
        rb.getComponent(2);
        Component[] cs = rb.getComponents();
        rb.getLayout();
        rb.setLayout(new FlowLayout());
        rb.doLayout();
        rb.layout();
        rb.invalidate();
        rb.validate();
        rb.remove(0);
        rb.remove(c2);
        rb.removeAll();
        rb.preferredSize();
        rb.minimumSize();
        rb.getComponentAt(1, 2);
        rb.locate(1, 2);
        rb.getComponentAt(new Point(1, 2));
        rb.isFocusCycleRoot(new Container());
        rb.transferFocusBackward();
        rb.setName("goober");
        rb.getName();
        rb.getParent();
        rb.getGraphicsConfiguration();
        rb.getTreeLock();
        rb.getToolkit();
        rb.isValid();
        rb.isDisplayable();
        rb.isVisible();
        rb.isShowing();
        rb.isEnabled();
        rb.enable(false);
        rb.enable(true);
        rb.enableInputMethods(false);
        rb.enableInputMethods(true);
        rb.show();
        rb.show(false);
        rb.show(true);
        rb.hide();
        rb.getForeground();
        rb.isForegroundSet();
        rb.getBackground();
        rb.isBackgroundSet();
        rb.getFont();
        rb.isFontSet();
        Container c = new Container();
        c.add(rb);
        rb.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            rb.setLocale(locale);

        rb.getColorModel();
        rb.getLocation();

        boolean exceptions = false;
        try {
            rb.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        rb.location();
        rb.setLocation(1, 2);
        rb.move(1, 2);
        rb.setLocation(new Point(1, 2));
        rb.getSize();
        rb.size();
        rb.setSize(1, 32);
        rb.resize(1, 32);
        rb.setSize(new Dimension(1, 32));
        rb.resize(new Dimension(1, 32));
        rb.getBounds();
        rb.bounds();
        rb.setBounds(10, 10, 10, 10);
        rb.setBounds(new Rectangle(10, 10, 10, 10));
        rb.isLightweight();
        rb.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        rb.getCursor();
        rb.isCursorSet();
        rb.inside(1, 2);
        rb.contains(new Point(1, 2));
        rb.isFocusable();
        rb.setFocusable(true);
        rb.setFocusable(false);
        rb.transferFocus();
        rb.getFocusCycleRootAncestor();
        rb.nextFocus();
        rb.transferFocusUpCycle();
        rb.hasFocus();
        rb.isFocusOwner();
        rb.toString();
        rb.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        rb.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        rb.setComponentOrientation(ComponentOrientation.UNKNOWN);
        rb.getComponentOrientation();
    }
}
