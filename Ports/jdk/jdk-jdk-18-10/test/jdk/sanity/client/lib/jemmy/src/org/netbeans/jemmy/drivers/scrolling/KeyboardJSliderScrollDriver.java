/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.drivers.scrolling;

import java.awt.Adjustable;
import java.awt.event.KeyEvent;
import org.netbeans.jemmy.operators.ComponentOperator;

/**
 *
 * @author shura
 */
public class KeyboardJSliderScrollDriver extends JSliderDriver {

    private static int getButton(int direction, int orientation) {
        if (direction == ScrollAdjuster.DECREASE_SCROLL_DIRECTION) {
            return (orientation == Adjustable.HORIZONTAL) ? KeyEvent.VK_LEFT : KeyEvent.VK_DOWN;
        } else {
            return (orientation == Adjustable.HORIZONTAL) ? KeyEvent.VK_RIGHT : KeyEvent.VK_UP;
        }
    }

    @Override
    protected boolean doPushAndWait(ComponentOperator oper, ScrollAdjuster adj, long freezeTimeout) {
        super.doPushAndWait(oper, adj, freezeTimeout);
        return true;
    }

    @Override
    protected void step(ComponentOperator oper, ScrollAdjuster adj) {
        oper.pushKey(getButton(adj.getScrollDirection(), adj.getScrollOrientation()));
        oper.getTimeouts().create("Waiter.TimeDelta").sleep();
    }
}
