/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.*;

import com.apple.laf.AquaUtils.JComponentPainter;

public class AquaTextFieldUI extends BasicTextFieldUI {
    public static ComponentUI createUI(final JComponent c) {
        return new AquaTextFieldUI();
    }

    protected JComponentPainter delegate;
    protected AquaFocusHandler handler;

    @Override
    protected void installListeners() {
        super.installListeners();

        handler = new AquaFocusHandler();
        final JTextComponent c = getComponent();
        c.addFocusListener(handler);
        c.addPropertyChangeListener(handler);

        LookAndFeel.installProperty(c, "opaque", UIManager.getBoolean(getPropertyPrefix() + "opaque"));
        AquaUtilControlSize.addSizePropertyListener(c);
        AquaTextFieldSearch.installSearchFieldListener(c);
    }

    @Override
    protected void uninstallListeners() {
        final JTextComponent c = getComponent();
        AquaTextFieldSearch.uninstallSearchFieldListener(c);
        AquaUtilControlSize.removeSizePropertyListener(c);
        c.removeFocusListener(handler);
        c.removePropertyChangeListener(handler);
        handler = null;

        super.uninstallListeners();
    }

    boolean oldDragState = false;
    @Override
    protected void installDefaults() {
        if (!GraphicsEnvironment.isHeadless()) {
            oldDragState = getComponent().getDragEnabled();
            getComponent().setDragEnabled(true);
        }

        super.installDefaults();
    }

    @Override
    protected void uninstallDefaults() {
        super.uninstallDefaults();

        if (!GraphicsEnvironment.isHeadless()) {
            getComponent().setDragEnabled(oldDragState);
        }
    }

    // Install a default keypress action which handles Cmd and Option keys
    // properly
    @Override
    protected void installKeyboardActions() {
        super.installKeyboardActions();
        AquaKeyBindings.instance().setDefaultAction(getKeymapName());
    }

    @Override
    protected Rectangle getVisibleEditorRect() {
        final Rectangle rect = super.getVisibleEditorRect();
        if (rect == null) return null;

        if (!getComponent().isOpaque()) {
            rect.y -= 3;
            rect.height += 6;
        }

        return rect;
    }

    @Override
    protected void paintSafely(final Graphics g) {
        paintBackgroundSafely(g);
        super.paintSafely(g);
    }

    protected void paintBackgroundSafely(final Graphics g) {
        final JTextComponent c = getComponent();
        final int width = c.getWidth();
        final int height = c.getHeight();

        // a delegate takes precedence
        if (delegate != null) {
            delegate.paint(c, g, 0, 0, width, height);
            return;
        }

        final boolean isOpaque = c.isOpaque();
        if (!(c.getBorder() instanceof AquaTextFieldBorder)) {
            // developer must have set a custom border
            if (!isOpaque && AquaUtils.hasOpaqueBeenExplicitlySet(c)) return;

            // must fill whole region with background color if opaque
            g.setColor(c.getBackground());
            g.fillRect(0, 0, width, height);
            return;
        }

        // using our own border
        g.setColor(c.getBackground());
        if (isOpaque) {
            g.fillRect(0, 0, width, height);
            return;
        }

        final Insets margin = c.getMargin();
        Insets insets = c.getInsets();

        if (insets == null) insets = new Insets(0, 0, 0, 0);
        if (margin != null) {
            insets.top -= margin.top;
            insets.left -= margin.left;
            insets.bottom -= margin.bottom;
            insets.right -= margin.right;
        }

        // the common case
        final int shrinkage = AquaTextFieldBorder.getShrinkageFor(c, height);
        g.fillRect(insets.left - 2, insets.top - shrinkage - 1,
                   width - insets.right - insets.left + 4,
                   height - insets.bottom - insets.top + shrinkage * 2 + 2);
    }

    @Override
    protected void paintBackground(final Graphics g) {
        // we have already ensured that the background is painted to our liking
        // by paintBackgroundSafely(), called from paintSafely().
    }

    @Override
    protected Caret createCaret() {
        return new AquaCaret();
    }

    @Override
    protected Highlighter createHighlighter() {
        return new AquaHighlighter();
    }

    protected void setPaintingDelegate(final JComponentPainter delegate) {
        this.delegate = delegate;
    }
}
