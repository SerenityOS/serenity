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
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;

/*
 * @test
 * @summary Check that JInternalFrame constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessJInternalFrame
 */

public class HeadlessJInternalFrame {
    public static void main(String args[]) {
        JInternalFrame intf = new JInternalFrame("TEST");
        intf.setUI(new BasicInternalFrameUI(intf) {
            protected JComponent createNorthPane(JInternalFrame w) {
                titlePane = new BasicInternalFrameTitlePane(w) {
                    protected PropertyChangeListener createPropertyChangeListener() {
                        return new BasicInternalFrameTitlePane.PropertyChangeHandler() {
                            int countUI = 0;

                            public void propertyChange(PropertyChangeEvent evt) {
                                if (evt.getPropertyName().equals("UI"))
                                    countUI++;
                                else if (countUI > 1)
                                    throw new RuntimeException("Test failed. Listener not removed!");
                            }
                        };
                    }
                };
                return titlePane;
            }
        });
        intf.setUI(null);
        intf.getAccessibleContext();
        intf.isFocusTraversable();
        intf.setEnabled(false);
        intf.setEnabled(true);
        intf.requestFocus();
        intf.requestFocusInWindow();
        intf.getPreferredSize();
        intf.getMaximumSize();
        intf.getMinimumSize();
        intf.contains(1, 2);
        Component c1 = intf.add(new Component(){});
        Component c2 = intf.add(new Component(){});
        Component c3 = intf.add(new Component(){});
        Insets ins = intf.getInsets();
        intf.getAlignmentY();
        intf.getAlignmentX();
        intf.getGraphics();
        intf.setVisible(false);
        intf.setVisible(true);
        intf.setForeground(Color.red);
        intf.setBackground(Color.red);
        for (String font : Toolkit.getDefaultToolkit().getFontList()) {
            for (int j = 8; j < 17; j++) {
                Font f1 = new Font(font, Font.PLAIN, j);
                Font f2 = new Font(font, Font.BOLD, j);
                Font f3 = new Font(font, Font.ITALIC, j);
                Font f4 = new Font(font, Font.BOLD | Font.ITALIC, j);

                intf.setFont(f1);
                intf.setFont(f2);
                intf.setFont(f3);
                intf.setFont(f4);

                intf.getFontMetrics(f1);
                intf.getFontMetrics(f2);
                intf.getFontMetrics(f3);
                intf.getFontMetrics(f4);
            }
        }
        intf.enable();
        intf.disable();
        intf.reshape(10, 10, 10, 10);
        intf.getBounds(new Rectangle(1, 1, 1, 1));
        intf.getSize(new Dimension(1, 2));
        intf.getLocation(new Point(1, 2));
        intf.getX();
        intf.getY();
        intf.getWidth();
        intf.getHeight();
        intf.isOpaque();
        intf.isValidateRoot();
        intf.isOptimizedDrawingEnabled();
        intf.isDoubleBuffered();
        intf.getComponentCount();
        intf.countComponents();
        intf.getComponent(0);
        Component[] cs = intf.getComponents();
        intf.getLayout();
        intf.setLayout(new FlowLayout());
        intf.doLayout();
        intf.layout();
        intf.invalidate();
        intf.validate();
        intf.remove(0);
        intf.remove(c2);
        intf.removeAll();
        intf.preferredSize();
        intf.minimumSize();
        intf.getComponentAt(1, 2);
        intf.locate(1, 2);
        intf.getComponentAt(new Point(1, 2));
        intf.isFocusCycleRoot(new Container());
        intf.transferFocusBackward();
        intf.setName("goober");
        intf.getName();
        intf.getParent();
        intf.getGraphicsConfiguration();
        intf.getTreeLock();
        intf.getToolkit();
        intf.isValid();
        intf.isDisplayable();
        intf.isVisible();
        intf.isShowing();
        intf.isEnabled();
        intf.enable(false);
        intf.enable(true);
        intf.enableInputMethods(false);
        intf.enableInputMethods(true);
        intf.show();
        intf.show(false);
        intf.show(true);
        intf.hide();
        intf.getForeground();
        intf.isForegroundSet();
        intf.getBackground();
        intf.isBackgroundSet();
        intf.getFont();
        intf.isFontSet();
        Container c = new Container();
        c.add(intf);
        intf.getLocale();
        for (Locale locale : Locale.getAvailableLocales())
            intf.setLocale(locale);

        intf.getColorModel();
        intf.getLocation();

        boolean exceptions = false;
        try {
            intf.getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("IllegalComponentStateException did not occur when expected");

        intf.location();
        intf.setLocation(1, 2);
        intf.move(1, 2);
        intf.setLocation(new Point(1, 2));
        intf.getSize();
        intf.size();
        intf.setSize(1, 32);
        intf.resize(1, 32);
        intf.setSize(new Dimension(1, 32));
        intf.resize(new Dimension(1, 32));
        intf.getBounds();
        intf.bounds();
        intf.setBounds(10, 10, 10, 10);
        intf.setBounds(new Rectangle(10, 10, 10, 10));
        intf.isLightweight();
        intf.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        intf.getCursor();
        intf.isCursorSet();
        intf.inside(1, 2);
        intf.contains(new Point(1, 2));
        intf.isFocusable();
        intf.setFocusable(true);
        intf.setFocusable(false);
        intf.transferFocus();
        intf.getFocusCycleRootAncestor();
        intf.nextFocus();
        intf.transferFocusUpCycle();
        intf.hasFocus();
        intf.isFocusOwner();
        intf.toString();
        intf.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        intf.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        intf.setComponentOrientation(ComponentOrientation.UNKNOWN);
        intf.getComponentOrientation();
    }
}
