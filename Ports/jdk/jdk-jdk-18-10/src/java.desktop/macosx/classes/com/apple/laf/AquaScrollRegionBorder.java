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

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.JComponent;

import apple.laf.JRSUIState;
import apple.laf.JRSUIConstants.Focused;
import apple.laf.JRSUIConstants.State;
import apple.laf.JRSUIConstants.Widget;

import com.apple.laf.AquaUtilControlSize.SizeDescriptor;
import com.apple.laf.AquaUtilControlSize.SizeVariant;
import com.apple.laf.AquaUtils.RecyclableSingletonFromDefaultConstructor;

public class AquaScrollRegionBorder extends AquaBorder {
    private static final RecyclableSingletonFromDefaultConstructor<AquaScrollRegionBorder> instance = new RecyclableSingletonFromDefaultConstructor<AquaScrollRegionBorder>(AquaScrollRegionBorder.class);

    public static AquaScrollRegionBorder getScrollRegionBorder() {
        return instance.get();
    }

    public AquaScrollRegionBorder() {
        super(new SizeDescriptor(new SizeVariant().alterMargins(2, 2, 2, 2)));
    }

    @Override
    protected AquaPainter<? extends JRSUIState> createPainter() {
        JRSUIState state =  JRSUIState.getInstance();
        state.set(Widget.FRAME_LIST_BOX);
        return AquaPainter.<JRSUIState>create(state, 7, 7, 3, 3, 3, 3);
    }

    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
        final State state = getState((JComponent)c);
        painter.state.set(state);
        painter.state.set(isFocused(c) && state == State.ACTIVE ? Focused.YES : Focused.NO);
        painter.paint(g, c, x, y, width, height);
    }

    protected State getState(final JComponent c) {
        if (!AquaFocusHandler.isActive(c)) return State.INACTIVE;
        if (!c.isEnabled()) return State.DISABLED;
        return State.ACTIVE;
    }
}
