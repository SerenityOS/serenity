/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.java.swing.plaf.motif;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;

/**
 * A CDE/Motif {@code L&F} implementation of ScrollPaneUI.
 *
 * @author Hans Muller
 */
public class MotifScrollPaneUI extends BasicScrollPaneUI
{
    private static final Border vsbMarginBorderR = new EmptyBorder(0, 4, 0, 0);
    private static final Border vsbMarginBorderL = new EmptyBorder(0, 0, 0, 4);
    private static final Border hsbMarginBorder = new EmptyBorder(4, 0, 0, 0);

    private CompoundBorder vsbBorder;
    private CompoundBorder hsbBorder;

    private PropertyChangeListener propertyChangeHandler;

    @Override
    protected void installListeners(JScrollPane scrollPane) {
        super.installListeners(scrollPane);
        propertyChangeHandler = createPropertyChangeHandler();
        scrollPane.addPropertyChangeListener(propertyChangeHandler);
    }

    @Override
    protected void uninstallListeners(JComponent scrollPane) {
        super.uninstallListeners(scrollPane);
        scrollPane.removePropertyChangeListener(propertyChangeHandler);
    }

    private PropertyChangeListener createPropertyChangeHandler() {
        return new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                  String propertyName = e.getPropertyName();

                  if (propertyName.equals("componentOrientation")) {
                        JScrollPane pane = (JScrollPane)e.getSource();
                        JScrollBar vsb = pane.getVerticalScrollBar();
                        if (vsb != null && vsbBorder != null &&
                            vsb.getBorder() == vsbBorder) {
                            // The Border on the verticall scrollbar matches
                            // what we installed, reset it.
                            if (pane.getComponentOrientation().isLeftToRight()) {
                                vsbBorder = new CompoundBorder(vsbMarginBorderR,
                                                vsbBorder.getInsideBorder());
                            } else {
                                vsbBorder = new CompoundBorder(vsbMarginBorderL,
                                                vsbBorder.getInsideBorder());
                            }
                            vsb.setBorder(vsbBorder);
                        }
                  }
        }};
    }

    @Override
    protected void installDefaults(JScrollPane scrollpane) {
        super.installDefaults(scrollpane);

        JScrollBar vsb = scrollpane.getVerticalScrollBar();
        if (vsb != null) {
            if (scrollpane.getComponentOrientation().isLeftToRight()) {
                vsbBorder = new CompoundBorder(vsbMarginBorderR,
                                               vsb.getBorder());
            }
            else {
                vsbBorder = new CompoundBorder(vsbMarginBorderL,
                                               vsb.getBorder());
            }
            vsb.setBorder(vsbBorder);
        }

        JScrollBar hsb = scrollpane.getHorizontalScrollBar();
        if (hsb != null) {
            hsbBorder = new CompoundBorder(hsbMarginBorder, hsb.getBorder());
            hsb.setBorder(hsbBorder);
        }
    }

    @Override
    protected void uninstallDefaults(JScrollPane c) {
        super.uninstallDefaults(c);

        JScrollBar vsb = scrollpane.getVerticalScrollBar();
        if (vsb != null) {
            if (vsb.getBorder() == vsbBorder) {
                vsb.setBorder(null);
            }
            vsbBorder = null;
        }

        JScrollBar hsb = scrollpane.getHorizontalScrollBar();
        if (hsb != null) {
            if (hsb.getBorder() == hsbBorder) {
                hsb.setBorder(null);
            }
            hsbBorder = null;
        }
    }


    public static ComponentUI createUI(JComponent x) {
        return new MotifScrollPaneUI();
    }
}
