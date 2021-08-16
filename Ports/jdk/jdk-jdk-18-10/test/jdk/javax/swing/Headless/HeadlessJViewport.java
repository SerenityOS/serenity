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
 * @summary Check that JViewport constructors and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJViewport
 */

public class HeadlessJViewport {
    public static void main(String args[]) {
        JViewport v = new JViewport();
        v.getUI();
        v.getUIClassID();
        v.setUI(null);
        v.updateUI();

        JComponent view = new JPanel();
        view.setMinimumSize(new Dimension(123, 456));
        v.setView(view);

        v.getAccessibleContext();
        v.isFocusTraversable();
        v.setEnabled(false);
        v.setEnabled(true);
        v.requestFocus();
        v.requestFocusInWindow();
        v.getPreferredSize();
        v.getMaximumSize();
        v.getMinimumSize();
        v.contains(1, 2);
        Component c1 = v.add(new Component(){});
        Component c2 = v.add(new Component(){});
        Component c3 = v.add(new Component(){});
        Insets ins = v.getInsets();
        v.getAlignmentY();
        v.getAlignmentX();
        v.getGraphics();
        v.setVisible(false);
        v.setVisible(true);
        v.setForeground(Color.red);
        v.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                v.setFont(f1);
                v.setFont(f2);
                v.setFont(f3);
                v.setFont(f4);

                v.getFontMetrics(f1);
                v.getFontMetrics(f2);
                v.getFontMetrics(f3);
                v.getFontMetrics(f4);
            }
        }
        v.enable();
        v.disable();
        v.reshape(10, 10, 10, 10);
        v.getBounds(new Rectangle(1, 1, 1, 1));
        v.getSize(new Dimension(1, 2));
        v.getLocation(new Point(1, 2));
        v.getX();
        v.getY();
        v.getWidth();
        v.getHeight();
        v.isOpaque();
        v.isValidateRoot();
        v.isOptimizedDrawingEnabled();
        v.isDoubleBuffered();
        v.getComponentCount();
        v.countComponents();
        v.getComponent(0);
        Component[] cs = v.getComponents();
        v.getLayout();
        v.setLayout(new FlowLayout());
        v.doLayout();
        v.layout();
        v.invalidate();
        v.validate();
        v.remove(0);
        v.remove(c2);
        v.removeAll();
        v.preferredSize();
        v.minimumSize();
        v.getComponentAt(1, 2);
        v.locate(1, 2);
        v.getComponentAt(new Point(1, 2));
        v.isFocusCycleRoot(new Container());
        v.transferFocusBackward();
        v.setName("goober");
        v.getName();
        v.getParent();
        v.getGraphicsConfiguration();
        v.getTreeLock();
        v.getToolkit();
        v.isValid();
        v.isDisplayable();
        v.isVisible();
        v.isShowing();
        v.isEnabled();
        v.enable(false);
        v.enable(true);
        v.enableInputMethods(false);
        v.enableInputMethods(true);
        v.show();
        v.show(false);
        v.show(true);
        v.hide();
        v.getForeground();
        v.isForegroundSet();
        v.getBackground();
        v.isBackgroundSet();
        v.getFont();
        v.isFontSet();
        Container c = new Container();
        c.add(v);
        v.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            v.setLocale(locale);

        v.getColorModel();
        v.getLocation();

        boolean exceptions = false;
        try {
            v.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        v.location();
        v.setLocation(1, 2);
        v.move(1, 2);
        v.setLocation(new Point(1, 2));
        v.getSize();
        v.size();
        v.setSize(1, 32);
        v.resize(1, 32);
        v.setSize(new Dimension(1, 32));
        v.resize(new Dimension(1, 32));
        v.getBounds();
        v.bounds();
        v.setBounds(10, 10, 10, 10);
        v.setBounds(new Rectangle(10, 10, 10, 10));
        v.isLightweight();
        v.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        v.getCursor();
        v.isCursorSet();
        v.inside(1, 2);
        v.contains(new Point(1, 2));
        v.isFocusable();
        v.setFocusable(true);
        v.setFocusable(false);
        v.transferFocus();
        v.getFocusCycleRootAncestor();
        v.nextFocus();
        v.transferFocusUpCycle();
        v.hasFocus();
        v.isFocusOwner();
        v.toString();
        v.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        v.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        v.setComponentOrientation(ComponentOrientation.UNKNOWN);
        v.getComponentOrientation();
    }
}
