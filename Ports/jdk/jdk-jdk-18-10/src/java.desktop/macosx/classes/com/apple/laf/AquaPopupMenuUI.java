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
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPopupMenuUI;

public class AquaPopupMenuUI extends BasicPopupMenuUI {
    public static ComponentUI createUI(final JComponent x) {
        return new AquaPopupMenuUI();
    }

    public boolean isPopupTrigger(final MouseEvent e) {
        // Use the awt popup trigger code since this only runs on our OS!
        return e.isPopupTrigger();
    }

    @Override
    public void paint(final Graphics g, final JComponent c) {
        if (!(g instanceof Graphics2D)) {
            super.paint(g, c);
            return;
        }

        if (!(PopupFactory.getSharedInstance() instanceof ScreenPopupFactory)) {
            super.paint(g, c);
            return;
        }

        // round off and put back edges in a new Graphics
        final Graphics2D g2d = (Graphics2D)g.create();
        final Rectangle popupBounds = popupMenu.getBounds(); // NB: origin is still at 0,0
        paintRoundRect(g2d, popupBounds);
        clipEdges(g2d, popupBounds);
        g2d.dispose();

        // if any subsequent drawing occurs over these corners, the window is square again
        super.paint(g, c);
    }

    protected void paintRoundRect(final Graphics2D g2d, final Rectangle popupBounds) {
        // setup the graphics context to blast alpha for every primitive we draw
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setComposite(AlphaComposite.Clear);

        // draw the 3px round-rect line around the outer bounds of the window,
        // this gives the appearance of rounded corners
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.drawRoundRect(-2, -2, popupBounds.width + 3, popupBounds.height + 3, 12, 12);
    }

    static final int OVERLAP_SLACK = 10;
    protected void clipEdges(final Graphics2D g2d, final Rectangle popupBounds) {
        final Component invoker = popupMenu.getInvoker();
        if (!(invoker instanceof JMenu)) return; // only point corners originating from menu items

        final Rectangle invokerBounds = invoker.getBounds();

        // only get location on screen when necessary
        invokerBounds.setLocation(invoker.getLocationOnScreen());
        popupBounds.setLocation(popupMenu.getLocationOnScreen());

        final Point invokerCenter = new Point((int)invokerBounds.getCenterX(), (int)invokerBounds.getCenterY());
        if (popupBounds.contains(invokerCenter)) {
            // invoker is "behind" the popup, no corners should be pointed
            return;
        }

        // blast opaque background over the corners we want to "put back"
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.setColor(popupMenu.getBackground());

        final Point popupCenter = new Point((int)popupBounds.getCenterX(), (int)popupBounds.getCenterY());
        final boolean invokerMidpointAbovePopupMidpoint = invokerCenter.y <= popupCenter.y;

        if (invokerBounds.x + invokerBounds.width < popupBounds.x + OVERLAP_SLACK) {
            // popup is far right of invoker
            if (invokerMidpointAbovePopupMidpoint) {
                // point upper left corner, most common case
                g2d.fillRect(-2, -2, 8, 8);
                return;
            }
            // point lower left corner
            g2d.fillRect(-2, popupBounds.height - 6, 8, 8);
            return;
        }

        if (popupBounds.x + popupBounds.width < invokerBounds.x + OVERLAP_SLACK) {
            // popup is far left of invoker
            if (invokerMidpointAbovePopupMidpoint) {
                // point upper right corner
                g2d.fillRect(popupBounds.width - 6, -2, 8, 8);
                return;
            }
            // point lower right corner
            g2d.fillRect(popupBounds.width - 6, popupBounds.height - 6, 8, 8);
            return;
        }

        // popup is neither "far right" or "far left" of it's invoker
        if (invokerBounds.y + invokerBounds.height < popupBounds.y + OVERLAP_SLACK) {
            // popup is "middle" below it's invoker,
            // this is probably the "connected" case where both upper corners should touch
            g2d.fillRect(-2, -2, popupBounds.width + 4, 8);
            return;
        }

        // if none of these cases match...don't make any corners pointed
    }
}
