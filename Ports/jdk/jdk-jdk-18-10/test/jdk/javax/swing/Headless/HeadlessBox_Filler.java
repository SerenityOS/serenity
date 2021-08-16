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
 * @summary Check that Box.Filler constructors and methods do not throw
 *          unexpected exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessBox_Filler
 */

public class HeadlessBox_Filler {
    public static void main(String args[]) {
        Box.Filler bf = new Box.Filler(new Dimension(10, 10),
                new Dimension(20, 20),
                new Dimension(30, 30));
        bf.getMinimumSize();
        bf.getPreferredSize();
        bf.getMaximumSize();
        bf.getAccessibleContext();
        bf.requestFocus();
        bf.requestFocusInWindow();
        bf.contains(1, 2);
        Component c1 = bf.add(new Component(){});
        Component c2 = bf.add(new Component(){});
        Component c3 = bf.add(new Component(){});
        Insets ins = bf.getInsets();
        bf.getAlignmentY();
        bf.getAlignmentX();
        bf.getGraphics();
        bf.setVisible(false);
        bf.setVisible(true);
        bf.setEnabled(false);
        bf.setEnabled(true);
        bf.setForeground(Color.red);
        bf.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                bf.setFont(f1);
                bf.setFont(f2);
                bf.setFont(f3);
                bf.setFont(f4);

                bf.getFontMetrics(f1);
                bf.getFontMetrics(f2);
                bf.getFontMetrics(f3);
                bf.getFontMetrics(f4);
            }
        }
        bf.enable();
        bf.disable();
        bf.reshape(10, 10, 10, 10);
        bf.getBounds(new Rectangle(1, 1, 1, 1));
        bf.getSize(new Dimension(1, 2));
        bf.getLocation(new Point(1, 2));
        bf.getX();
        bf.getY();
        bf.getWidth();
        bf.getHeight();
        bf.isOpaque();
        bf.isValidateRoot();
        bf.isOptimizedDrawingEnabled();
        bf.isDoubleBuffered();
        bf.getComponentCount();
        bf.countComponents();
        bf.getComponent(1);
        bf.getComponent(2);
        Component[] cs = bf.getComponents();
        bf.getLayout();
        bf.setLayout(new FlowLayout());
        bf.doLayout();
        bf.layout();
        bf.invalidate();
        bf.validate();
        bf.preferredSize();
        bf.remove(0);
        bf.remove(c2);
        bf.removeAll();
        bf.minimumSize();
        bf.getComponentAt(1, 2);
        bf.locate(1, 2);
        bf.getComponentAt(new Point(1, 2));
        bf.isFocusCycleRoot(new Container());
        bf.transferFocusBackward();
        bf.setName("goober");
        bf.getName();
        bf.getParent();
        bf.getGraphicsConfiguration();
        bf.getTreeLock();
        bf.getToolkit();
        bf.isValid();
        bf.isDisplayable();
        bf.isVisible();
        bf.isShowing();
        bf.isEnabled();
        bf.enable(false);
        bf.enable(true);
        bf.enableInputMethods(false);
        bf.enableInputMethods(true);
        bf.show();
        bf.show(false);
        bf.show(true);
        bf.hide();
        bf.getForeground();
        bf.isForegroundSet();
        bf.getBackground();
        bf.isBackgroundSet();
        bf.getFont();
        bf.isFontSet();

        Container c = new Container();
        c.add(bf);
        bf.getLocale();

        for (Locale locale : Locale.getAvailableLocales())
            bf.setLocale(locale);

        bf.getColorModel();
        bf.getLocation();

        boolean exceptions = false;
        try {
            bf.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        bf.setLocation(1, 2);
        bf.move(1, 2);
        bf.setLocation(new Point(1, 2));
        bf.getSize();
        bf.size();
        bf.setSize(1, 32);
        bf.resize(1, 32);
        bf.setSize(new Dimension(1, 32));
        bf.resize(new Dimension(1, 32));
        bf.getBounds();
        bf.bounds();
        bf.setBounds(10, 10, 10, 10);
        bf.setBounds(new Rectangle(10, 10, 10, 10));
        bf.isLightweight();
        bf.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        bf.getCursor();
        bf.isCursorSet();
        bf.inside(1, 2);
        bf.contains(new Point(1, 2));
        bf.isFocusTraversable();
        bf.isFocusable();
        bf.setFocusable(true);
        bf.setFocusable(false);
        bf.transferFocus();
        bf.getFocusCycleRootAncestor();
        bf.nextFocus();
        bf.transferFocusUpCycle();
        bf.hasFocus();
        bf.isFocusOwner();
        bf.toString();
        bf.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        bf.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        bf.setComponentOrientation(ComponentOrientation.UNKNOWN);
        bf.getComponentOrientation();
    }
}
