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

import javax.swing.plaf.*;
import javax.swing.text.JTextComponent;

import apple.laf.JRSUIConstants.*;

import com.apple.laf.AquaUtilControlSize.*;
import com.apple.laf.AquaUtils.*;

public class AquaTextFieldBorder extends AquaBorder {
    private static final RecyclableSingleton<AquaTextFieldBorder> instance = new RecyclableSingletonFromDefaultConstructor<AquaTextFieldBorder>(AquaTextFieldBorder.class);
    public static AquaTextFieldBorder getTextFieldBorder() {
        return instance.get();
    }

    public AquaTextFieldBorder() {
        this(new SizeDescriptor(new SizeVariant().alterMargins(6, 7, 6, 7).alterInsets(3, 3, 3, 3)));
        painter.state.set(Widget.FRAME_TEXT_FIELD);
        painter.state.set(FrameOnly.YES);
        painter.state.set(Size.LARGE);
    }

    public AquaTextFieldBorder(final SizeDescriptor sizeDescriptor) {
        super(sizeDescriptor);
    }

    public AquaTextFieldBorder(final AquaTextFieldBorder other) {
        super(other);
    }

    protected void setSize(final Size size) {
        super.setSize(size);
        painter.state.set(Size.LARGE);
    }

    public void paintBorder(final Component c, final Graphics g, int x, int y, int width, int height) {
//        g.setColor(Color.MAGENTA);
//        g.drawRect(x, y, width - 1, height - 1);

        if (!(c instanceof JTextComponent)) {
            painter.state.set(State.ACTIVE);
            painter.state.set(Focused.NO);
            painter.paint(g, c, x, y, width, height);
            return;
        }

        final JTextComponent jc = (JTextComponent)c;
        final State state = getStateFor(jc);
        painter.state.set(state);
        painter.state.set(State.ACTIVE == state && jc.hasFocus() ? Focused.YES : Focused.NO);

        if (jc.isOpaque()) {
            painter.paint(g, c, x, y, width, height);
            return;
        }

        final int shrinkage = getShrinkageFor(jc, height);
        final Insets subInsets = getSubInsets(shrinkage);
        x += subInsets.left;
        y += subInsets.top;
        width -= (subInsets.left + subInsets.right);
        height -= (subInsets.top + subInsets.bottom);

        if (shrinkage > 0) {
            final Rectangle clipBounds = g.getClipBounds();
            clipBounds.x += shrinkage;
            clipBounds.width -= shrinkage * 2;
            g.setClip(clipBounds);
        }

        painter.paint(g, c, x, y, width, height);
//        g.setColor(Color.ORANGE);
//        g.drawRect(x, y, width - 1, height - 1);
    }

    static int getShrinkageFor(final JTextComponent jc, final int height) {
        if (jc == null) return 0;
        final TextUI ui = jc.getUI();
        if (ui == null) return 0;
        final Dimension size = ui.getPreferredSize(jc);
        if (size == null) return 0;
        final int shrinkage = size.height - height;
        return (shrinkage < 0) ? 0 : (shrinkage > 3) ? 3 : shrinkage;
    }

    // this determines the rect that we should draw inset to our existing bounds
    protected Insets getSubInsets(final int shrinkage) {
        final Insets insets = sizeVariant.insets;

        if (shrinkage > 0) {
            return new InsetsUIResource(insets.top - shrinkage, insets.left, insets.bottom - shrinkage, insets.right);
        }

        return insets;
    }

    public Insets getBorderInsets(final Component c) {
        if (!(c instanceof JTextComponent) || c.isOpaque()) return new InsetsUIResource(3, 7, 3, 7);
        return new InsetsUIResource(5, 5, 5, 5);
    }

    protected static State getStateFor(final JTextComponent jc) {
        if (!AquaFocusHandler.isActive(jc)) {
            return State.INACTIVE;
        }

        if (!jc.isEnabled()) {
            return State.DISABLED;
        }

        if (!jc.isEditable()) {
            return State.DISABLED;
        }

        return State.ACTIVE;
    }
}
