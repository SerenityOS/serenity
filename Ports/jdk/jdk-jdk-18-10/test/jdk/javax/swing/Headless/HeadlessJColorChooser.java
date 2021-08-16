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
 * @summary Check that JColorChooser constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJColorChooser
 */

public class HeadlessJColorChooser {
    public static void main(String args[]) {
        JColorChooser cc;
        cc = new JColorChooser();
        cc.getAccessibleContext();
        cc.isFocusTraversable();
        cc.setEnabled(false);
        cc.setEnabled(true);
        cc.requestFocus();
        cc.requestFocusInWindow();
        cc.getPreferredSize();
        cc.getMaximumSize();
        cc.getMinimumSize();
        cc.contains(1, 2);
        Component c1 = cc.add(new Component(){});
        Component c2 = cc.add(new Component(){});
        Component c3 = cc.add(new Component(){});
        Insets ins = cc.getInsets();
        cc.getAlignmentY();
        cc.getAlignmentX();
        cc.getGraphics();
        cc.setVisible(false);
        cc.setVisible(true);
        cc.setForeground(Color.red);
        cc.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                cc.setFont(f1);
                cc.setFont(f2);
                cc.setFont(f3);
                cc.setFont(f4);

                cc.getFontMetrics(f1);
                cc.getFontMetrics(f2);
                cc.getFontMetrics(f3);
                cc.getFontMetrics(f4);
            }
        }
        cc.enable();
        cc.disable();
        cc.reshape(10, 10, 10, 10);
        cc.getBounds(new Rectangle(1, 1, 1, 1));
        cc.getSize(new Dimension(1, 2));
        cc.getLocation(new Point(1, 2));
        cc.getX();
        cc.getY();
        cc.getWidth();
        cc.getHeight();
        cc.isOpaque();
        cc.isValidateRoot();
        cc.isOptimizedDrawingEnabled();
        cc.isDoubleBuffered();
        cc.getComponentCount();
        cc.countComponents();
        cc.getComponent(1);
        cc.getComponent(2);
        Component[] cs = cc.getComponents();
        cc.getLayout();
        cc.setLayout(new FlowLayout());
        cc.doLayout();
        cc.layout();
        cc.invalidate();
        cc.validate();
        cc.remove(0);
        cc.remove(c2);
        cc.removeAll();
        cc.preferredSize();
        cc.minimumSize();
        cc.getComponentAt(1, 2);
        cc.locate(1, 2);
        cc.getComponentAt(new Point(1, 2));
        cc.isFocusCycleRoot(new Container());
        cc.transferFocusBackward();
        cc.setName("goober");
        cc.getName();
        cc.getParent();
        cc.getGraphicsConfiguration();
        cc.getTreeLock();
        cc.getToolkit();
        cc.isValid();
        cc.isDisplayable();
        cc.isVisible();
        cc.isShowing();
        cc.isEnabled();
        cc.enable(false);
        cc.enable(true);
        cc.enableInputMethods(false);
        cc.enableInputMethods(true);
        cc.show();
        cc.show(false);
        cc.show(true);
        cc.hide();
        cc.getForeground();
        cc.isForegroundSet();
        cc.getBackground();
        cc.isBackgroundSet();
        cc.getFont();
        cc.isFontSet();
        Container c = new Container();
        c.add(cc);
        cc.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            cc.setLocale(locale);

        cc.getColorModel();
        cc.getLocation();

        boolean exceptions = false;
        try {
            cc.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        cc.location();
        cc.setLocation(1, 2);
        cc.move(1, 2);
        cc.setLocation(new Point(1, 2));
        cc.getSize();
        cc.size();
        cc.setSize(1, 32);
        cc.resize(1, 32);
        cc.setSize(new Dimension(1, 32));
        cc.resize(new Dimension(1, 32));
        cc.getBounds();
        cc.bounds();
        cc.setBounds(10, 10, 10, 10);
        cc.setBounds(new Rectangle(10, 10, 10, 10));
        cc.isLightweight();
        cc.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        cc.getCursor();
        cc.isCursorSet();
        cc.inside(1, 2);
        cc.contains(new Point(1, 2));
        cc.isFocusable();
        cc.setFocusable(true);
        cc.setFocusable(false);
        cc.transferFocus();
        cc.getFocusCycleRootAncestor();
        cc.nextFocus();
        cc.transferFocusUpCycle();
        cc.hasFocus();
        cc.isFocusOwner();
        cc.toString();
        cc.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        cc.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        cc.setComponentOrientation(ComponentOrientation.UNKNOWN);
        cc.getComponentOrientation();
    }
}
