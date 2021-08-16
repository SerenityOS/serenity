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
 * @summary Check that JProgressBar constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJProgressBar
 */

public class HeadlessJProgressBar {
    public static void main(String args[]) {
        JProgressBar pb = new JProgressBar();
        pb.getAccessibleContext();
        pb.isFocusTraversable();
        pb.setEnabled(false);
        pb.setEnabled(true);
        pb.requestFocus();
        pb.requestFocusInWindow();
        pb.getPreferredSize();
        pb.getMaximumSize();
        pb.getMinimumSize();
        pb.contains(1, 2);
        Component c1 = pb.add(new Component(){});
        Component c2 = pb.add(new Component(){});
        Component c3 = pb.add(new Component(){});
        Insets ins = pb.getInsets();
        pb.getAlignmentY();
        pb.getAlignmentX();
        pb.getGraphics();
        pb.setVisible(false);
        pb.setVisible(true);
        pb.setForeground(Color.red);
        pb.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                pb.setFont(f1);
                pb.setFont(f2);
                pb.setFont(f3);
                pb.setFont(f4);

                pb.getFontMetrics(f1);
                pb.getFontMetrics(f2);
                pb.getFontMetrics(f3);
                pb.getFontMetrics(f4);
            }
        }
        pb.enable();
        pb.disable();
        pb.reshape(10, 10, 10, 10);
        pb.getBounds(new Rectangle(1, 1, 1, 1));
        pb.getSize(new Dimension(1, 2));
        pb.getLocation(new Point(1, 2));
        pb.getX();
        pb.getY();
        pb.getWidth();
        pb.getHeight();
        pb.isOpaque();
        pb.isValidateRoot();
        pb.isOptimizedDrawingEnabled();
        pb.isDoubleBuffered();
        pb.getComponentCount();
        pb.countComponents();
        pb.getComponent(1);
        pb.getComponent(2);
        Component[] cs = pb.getComponents();
        pb.getLayout();
        pb.setLayout(new FlowLayout());
        pb.doLayout();
        pb.layout();
        pb.invalidate();
        pb.validate();
        pb.remove(0);
        pb.remove(c2);
        pb.removeAll();
        pb.preferredSize();
        pb.minimumSize();
        pb.getComponentAt(1, 2);
        pb.locate(1, 2);
        pb.getComponentAt(new Point(1, 2));
        pb.isFocusCycleRoot(new Container());
        pb.transferFocusBackward();
        pb.setName("goober");
        pb.getName();
        pb.getParent();
        pb.getGraphicsConfiguration();
        pb.getTreeLock();
        pb.getToolkit();
        pb.isValid();
        pb.isDisplayable();
        pb.isVisible();
        pb.isShowing();
        pb.isEnabled();
        pb.enable(false);
        pb.enable(true);
        pb.enableInputMethods(false);
        pb.enableInputMethods(true);
        pb.show();
        pb.show(false);
        pb.show(true);
        pb.hide();
        pb.getForeground();
        pb.isForegroundSet();
        pb.getBackground();
        pb.isBackgroundSet();
        pb.getFont();
        pb.isFontSet();
        Container c = new Container();
        c.add(pb);
        pb.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            pb.setLocale(locale);

        pb.getColorModel();
        pb.getLocation();

        boolean exceptions = false;
        try {
            pb.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        pb.location();
        pb.setLocation(1, 2);
        pb.move(1, 2);
        pb.setLocation(new Point(1, 2));
        pb.getSize();
        pb.size();
        pb.setSize(1, 32);
        pb.resize(1, 32);
        pb.setSize(new Dimension(1, 32));
        pb.resize(new Dimension(1, 32));
        pb.getBounds();
        pb.bounds();
        pb.setBounds(10, 10, 10, 10);
        pb.setBounds(new Rectangle(10, 10, 10, 10));
        pb.isLightweight();
        pb.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        pb.getCursor();
        pb.isCursorSet();
        pb.inside(1, 2);
        pb.contains(new Point(1, 2));
        pb.isFocusable();
        pb.setFocusable(true);
        pb.setFocusable(false);
        pb.transferFocus();
        pb.getFocusCycleRootAncestor();
        pb.nextFocus();
        pb.transferFocusUpCycle();
        pb.hasFocus();
        pb.isFocusOwner();
        pb.toString();
        pb.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        pb.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        pb.setComponentOrientation(ComponentOrientation.UNKNOWN);
        pb.getComponentOrientation();
    }
}
