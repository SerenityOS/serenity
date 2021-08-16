/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.apple.laf;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import javax.swing.*;
import javax.swing.plaf.UIResource;

/**
 * This class is used by the text components, AquaEditorPaneUI, AquaTextAreaUI, AquaTextFieldUI and AquaTextPaneUI to control painting of the
 * component's border.  NOTE: It is assumed that this handler is added to components that extend JComponent.
 */
public class AquaFocusHandler implements FocusListener, PropertyChangeListener {
    // Flag to help focusGained() determine whether the origin focus loss was due to a temporary focus loss or not.
    private boolean wasTemporary = false;

    // Flag to track when a border needs a repaint due to a window becoming activate/inactive.
    private boolean repaintBorder = false;

    protected static final String FRAME_ACTIVE_PROPERTY = "Frame.active";

    public void focusGained(final FocusEvent ev) {
        // If we gained focus and it wasn't due to a previous temporary focus loss
        // or the frame became active again, then repaint the border on the component.
        if (!wasTemporary || repaintBorder) {
            AquaBorder.repaintBorder((JComponent)ev.getSource());
            repaintBorder = false;
        }
        wasTemporary = false;
    }

    public void focusLost(final FocusEvent ev) {
        wasTemporary = ev.isTemporary();

        // If we lost focus due to a permanent focus loss then repaint the border on the component.
        if (!wasTemporary) {
            AquaBorder.repaintBorder((JComponent)ev.getSource());
        }
    }

    public void propertyChange(final PropertyChangeEvent ev) {
        if (!FRAME_ACTIVE_PROPERTY.equals(ev.getPropertyName())) return;

        if (Boolean.TRUE.equals(ev.getNewValue())) {
            // The FRAME_ACTIVE_PROPERTY change event is sent before a component gains focus.
            // We set a flag to help the focusGained() determine when they should be repainting
            // the components focus.
            repaintBorder = true;
        } else if (wasTemporary) {
            // The FRAME_ACTIVE_PROPERTY change event is sent after a component loses focus.
            // We use the wasTemporary flag to determine if we need to repaint the border.
            AquaBorder.repaintBorder((JComponent)ev.getSource());
        }
    }

    protected static boolean isActive(final JComponent c) {
        if (c == null) return true;
        final Object activeObj = c.getClientProperty(AquaFocusHandler.FRAME_ACTIVE_PROPERTY);
        if (Boolean.FALSE.equals(activeObj)) return false;
        return true;
    }

    static final PropertyChangeListener REPAINT_LISTENER = new PropertyChangeListener() {
        public void propertyChange(final PropertyChangeEvent evt) {
            final Object source = evt.getSource();
            if (source instanceof JComponent) {
                ((JComponent)source).repaint();
            }
        }
    };

    protected static void install(final JComponent c) {
        c.addPropertyChangeListener(FRAME_ACTIVE_PROPERTY, REPAINT_LISTENER);
    }

    protected static void uninstall(final JComponent c) {
        c.removePropertyChangeListener(FRAME_ACTIVE_PROPERTY, REPAINT_LISTENER);
    }

    static void swapSelectionColors(final String prefix, final JTree c, final Object value) {
        // <rdar://problem/8166173> JTree: selection color does not dim when window becomes inactive
        // TODO inject our colors into the DefaultTreeCellRenderer
    }

    static void swapSelectionColors(final String prefix, final JTable c, final Object value) {
        if (!isComponentValid(c)) return;

        final Color bg = c.getSelectionBackground();
        final Color fg = c.getSelectionForeground();
        if (!(bg instanceof UIResource) || !(fg instanceof UIResource)) return;

        if (Boolean.FALSE.equals(value)) {
            setSelectionColors(c, "Table.selectionInactiveForeground", "Table.selectionInactiveBackground");
            return;
        }

        if (Boolean.TRUE.equals(value)) {
            setSelectionColors(c, "Table.selectionForeground", "Table.selectionBackground");
            return;
        }
    }

    static void setSelectionColors(final JTable c, final String fgName, final String bgName) {
        c.setSelectionForeground(UIManager.getColor(fgName));
        c.setSelectionBackground(UIManager.getColor(bgName));
    }

    static void swapSelectionColors(final String prefix, final JList<?> c, final Object value) {
        if (!isComponentValid(c)) return;

        final Color bg = c.getSelectionBackground();
        final Color fg = c.getSelectionForeground();
        if (!(bg instanceof UIResource) || !(fg instanceof UIResource)) return;

        if (Boolean.FALSE.equals(value)) {
            setSelectionColors(c, "List.selectionInactiveForeground", "List.selectionInactiveBackground");
            return;
        }

        if (Boolean.TRUE.equals(value)) {
            setSelectionColors(c, "List.selectionForeground", "List.selectionBackground");
            return;
        }
    }

    static void setSelectionColors(final JList<?> c, final String fgName, final String bgName) {
        c.setSelectionForeground(UIManager.getColor(fgName));
        c.setSelectionBackground(UIManager.getColor(bgName));
    }

    static boolean isComponentValid(final JComponent c) {
        if (c == null) return false;
        final Window window = SwingUtilities.getWindowAncestor(c);
        if (window == null) return false;
        return true;
    }
}
