/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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


package sun.awt.X11;

import java.awt.Component;
import java.awt.Graphics;

import sun.awt.AWTAccessor;
import sun.awt.RepaintArea;

/**
 * The {@code RepaintArea} is a geometric construct created for the
 * purpose of holding the geometry of several coalesced paint events.
 * This geometry is accessed synchronously, although it is written such
 * that painting may still be executed asynchronously.
 *
 * @author      Eric Hawkes
 */
final class XRepaintArea extends RepaintArea {

    /**
     * Calls {@code Component.update(Graphics)} with given Graphics.
     */
    protected void updateComponent(Component comp, Graphics g) {
        if (comp != null) {
            // We don't call peer.paintPeer() here, because we shouldn't paint
            // native component when processing UPDATE events.
            super.updateComponent(comp, g);
        }
    }

    /**
     * Calls {@code Component.paint(Graphics)} with given Graphics.
     */
    protected void paintComponent(Component comp, Graphics g) {
        if (comp != null) {
            final XComponentPeer peer = AWTAccessor.getComponentAccessor()
                                                   .getPeer(comp);
            if (peer != null) {
                peer.paintPeer(g);
            }
            super.paintComponent(comp, g);
        }
    }
}
