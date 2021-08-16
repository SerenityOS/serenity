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
import java.awt.event.*;
import java.util.Locale;

/*
 * @test
 * @summary Check that Component methods do not throw unexpected exceptions
 *          in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessComponent
 */

public class HeadlessComponent {
    public static void main(String args[]) {
        Component comp = new Component(){};
        comp.addComponentListener(new ComponentAdapter() {});
        comp.addFocusListener(new FocusAdapter(){});
        comp.addHierarchyBoundsListener(new HierarchyBoundsAdapter(){});
        comp.addHierarchyListener(e -> {});
        comp.addInputMethodListener(new InputMethodListener() {
            public void inputMethodTextChanged(InputMethodEvent event) {}
            public void caretPositionChanged(InputMethodEvent event) {}
        });
        comp.addKeyListener(new KeyAdapter() {});
        comp.addMouseListener(new MouseAdapter() {});
        comp.addMouseMotionListener(new MouseMotionAdapter() {});
        comp.addMouseWheelListener(e -> {});
        comp.addPropertyChangeListener(e -> {});
        comp.addNotify();
        comp.getName();
        comp.setName("goober");
        comp.getParent();
        comp.getGraphicsConfiguration();
        comp.getTreeLock();
        comp.getToolkit();
        comp.isValid();
        comp.isDisplayable();
        comp.isVisible();
        comp.isShowing();
        comp.isEnabled();
        comp.setEnabled(false);
        comp.setEnabled(true);
        comp.enable();
        comp.enable(false);
        comp.enable(true);
        comp.disable();
        comp.isDoubleBuffered();
        comp.enableInputMethods(false);
        comp.enableInputMethods(true);
        comp.setVisible(false);
        comp.setVisible(true);
        comp.show();
        comp.show(false);
        comp.show(true);
        comp.hide();
        comp.getForeground();
        comp.setForeground(Color.red);
        comp.isForegroundSet();
        comp.getBackground();
        comp.setBackground(Color.red);
        comp.isBackgroundSet();
        comp.getFont();
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int i = 8; i < 17; i++) {
                Font f1 = new Font(font, Font.PLAIN, i);
                Font f2 = new Font(font, Font.BOLD, i);
                Font f3 = new Font(font, Font.ITALIC, i);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, i);

                comp.setFont(f1);
                comp.getFontMetrics(f1);
                comp.setFont(f2);
                comp.getFontMetrics(f2);
                comp.setFont(f3);
                comp.getFontMetrics(f3);
                comp.setFont(f4);
                comp.getFontMetrics(f4);
            }
        }
        comp.isFontSet();

        boolean exceptions = false;
        try {
            Container c = new Container();
            c.add(comp);
            comp.getLocale();
        } catch (IllegalComponentStateException ex) {
           exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        for (Locale locale : Locale.getAvailableLocales())
            comp.setLocale(locale);

        comp.getColorModel();
        comp.getLocation();

        exceptions = false;
        try {
            comp = new Component(){};
            comp.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        comp.location();
        comp.setLocation(1, 2);
        comp.move(1, 2);
        comp.setLocation(new Point(1, 2));
        comp.getSize();
        comp.size();
        comp.setSize(1, 32);
        comp.resize(1, 32);
        comp.setSize(new Dimension(1, 32));
        comp.resize(new Dimension(1, 32));
        comp.getBounds();
        comp.bounds();
        comp.setBounds(10, 10, 10, 10);
        comp.reshape(10, 10, 10, 10);
        comp.setBounds(new Rectangle(10, 10, 10, 10));
        comp.getX();
        comp.getY();
        comp.getWidth();
        comp.getHeight();
        comp.getBounds(new Rectangle(1, 1, 1, 1));
        comp.getSize(new Dimension(1, 2));
        comp.getLocation(new Point(1, 2));
        comp.isOpaque();
        comp.isLightweight();
        comp.getPreferredSize();
        comp.preferredSize();
        comp.getMinimumSize();
        comp.minimumSize();
        comp.getMaximumSize();
        comp.getAlignmentX();
        comp.getAlignmentY();
        comp.doLayout();
        comp.layout();
        comp.validate();
        comp.invalidate();
        comp.getGraphics();
        Cursor c = new Cursor(Cursor.CROSSHAIR_CURSOR);
        comp.setCursor(c);
        comp.getCursor();
        comp.isCursorSet();
        comp.contains(1, 2);
        comp.inside(1, 2);
        comp.contains(new Point(1, 2));
        comp.getComponentAt(1, 2);
        comp.locate(1, 2);
        comp.getComponentAt(new Point(1, 2));
        comp.isFocusTraversable();
        comp.isFocusable();
        comp.setFocusable(true);
        comp.setFocusable(false);
        comp.requestFocus();
        comp.requestFocusInWindow();
        comp.transferFocus();
        comp.getFocusCycleRootAncestor();
        comp.isFocusCycleRoot(new Container());
        comp.nextFocus();
        comp.transferFocusBackward();
        comp.transferFocusUpCycle();
        comp.hasFocus();
        comp.isFocusOwner();
        comp.toString();
        comp.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        comp.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        comp.setComponentOrientation(ComponentOrientation.UNKNOWN);
        comp.getComponentOrientation();
        comp.getAccessibleContext();
    }
}
