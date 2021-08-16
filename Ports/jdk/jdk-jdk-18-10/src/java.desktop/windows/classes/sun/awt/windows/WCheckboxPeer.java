/*
 * Copyright (c) 1996, 2014, Oracle and/or its affiliates. All rights reserved.
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
package sun.awt.windows;

import java.awt.*;
import java.awt.peer.*;
import java.awt.event.ItemEvent;

final class WCheckboxPeer extends WComponentPeer implements CheckboxPeer {

    // CheckboxPeer implementation

    @Override
    public native void setState(boolean state);
    @Override
    public native void setCheckboxGroup(CheckboxGroup g);
    @Override
    public native void setLabel(String label);

    private static native int getCheckMarkSize();

    @Override
    public Dimension getMinimumSize() {
        String lbl = ((Checkbox)target).getLabel();
        int marksize = getCheckMarkSize();
        if (lbl == null) {
            lbl = "";
        }
        FontMetrics fm = getFontMetrics(((Checkbox)target).getFont());
        /*
         * Borders between check mark and text and between text and edge of
         * checkbox should both be equal to marksize/4, here's where marksize/2
         * goes from. Marksize is currently constant ( = 16 pixels) on win32.
         */
        return new Dimension(fm.stringWidth(lbl) + marksize/2 + marksize,
                             Math.max(fm.getHeight() + 8,  marksize));
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    // Toolkit & peer internals

    WCheckboxPeer(Checkbox target) {
        super(target);
    }

    @Override
    native void create(WComponentPeer parent);

    @Override
    void initialize() {
        Checkbox t = (Checkbox)target;
        setState(t.getState());
        setCheckboxGroup(t.getCheckboxGroup());

        Color bg = ((Component)target).getBackground();
        if (bg != null) {
            setBackground(bg);
        }

        super.initialize();
    }

    @Override
    public boolean shouldClearRectBeforePaint() {
        return false;
    }

    // native callbacks

    void handleAction(final boolean state) {
        final Checkbox cb = (Checkbox)this.target;
        WToolkit.executeOnEventHandlerThread(cb, new Runnable() {
            @Override
            public void run() {
                CheckboxGroup chg = cb.getCheckboxGroup();
                if ((chg != null) && (cb == chg.getSelectedCheckbox()) && cb.getState()) {
                    return;
                }
                cb.setState(state);
                postEvent(new ItemEvent(cb, ItemEvent.ITEM_STATE_CHANGED,
                                cb.getLabel(),
                                state? ItemEvent.SELECTED : ItemEvent.DESELECTED));
            }
        });
    }
}
