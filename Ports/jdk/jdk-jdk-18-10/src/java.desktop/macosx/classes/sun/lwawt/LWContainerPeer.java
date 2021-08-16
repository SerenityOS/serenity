/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt;

import sun.awt.SunGraphicsCallback;
import sun.java2d.pipe.Region;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.peer.ContainerPeer;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;

abstract class LWContainerPeer<T extends Container, D extends JComponent>
        extends LWCanvasPeer<T, D> implements ContainerPeer {

    /**
     * List of child peers sorted by z-order from bottom-most to top-most.
     */
    private final List<LWComponentPeer<?, ?>> childPeers = new LinkedList<>();

    LWContainerPeer(final T target, final PlatformComponent platformComponent) {
        super(target, platformComponent);
    }

    final void addChildPeer(final LWComponentPeer<?, ?> child) {
        synchronized (getPeerTreeLock()) {
            childPeers.add(childPeers.size(), child);
            // TODO: repaint
        }
    }

    final void removeChildPeer(final LWComponentPeer<?, ?> child) {
        synchronized (getPeerTreeLock()) {
            childPeers.remove(child);
        }
        // TODO: repaint
    }

    // Used by LWComponentPeer.setZOrder()
    final void setChildPeerZOrder(final LWComponentPeer<?, ?> peer,
                                  final LWComponentPeer<?, ?> above) {
        synchronized (getPeerTreeLock()) {
            childPeers.remove(peer);
            int index = (above != null) ? childPeers.indexOf(above) : childPeers.size();
            if (index >= 0) {
                childPeers.add(index, peer);
            } else {
                // TODO: log
            }
        }
        // TODO: repaint
    }

    // ---- PEER METHODS ---- //

    /*
     * Overridden in LWWindowPeer.
     */
    @Override
    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    @Override
    public final void beginValidate() {
        // TODO: it seems that begin/endValidate() is only useful
        // for heavyweight windows, when a batch movement for
        // child windows  occurs. That's why no-op
    }

    @Override
    public final void endValidate() {
        // TODO: it seems that begin/endValidate() is only useful
        // for heavyweight windows, when a batch movement for
        // child windows  occurs. That's why no-op
    }

    @Override
    public final void beginLayout() {
        // Skip all painting till endLayout()
        setLayouting(true);
    }

    @Override
    public final void endLayout() {
        setLayouting(false);

        // Post an empty event to flush all the pending target paints
        postPaintEvent(0, 0, 0, 0);
    }

    // ---- PEER NOTIFICATIONS ---- //

    /**
     * Returns a copy of the childPeer collection.
     */
    @SuppressWarnings("unchecked")
    final List<LWComponentPeer<?, ?>> getChildren() {
        synchronized (getPeerTreeLock()) {
            Object copy = ((LinkedList<?>) childPeers).clone();
            return (List<LWComponentPeer<?, ?>>) copy;
        }
    }

    @Override
    final Region getVisibleRegion() {
        return cutChildren(super.getVisibleRegion(), null);
    }

    /**
     * Removes bounds of children above specific child from the region. If above
     * is null removes all bounds of children.
     */
    final Region cutChildren(Region r, final LWComponentPeer<?, ?> above) {
        boolean aboveFound = above == null;
        for (final LWComponentPeer<?, ?> child : getChildren()) {
            if (!aboveFound && child == above) {
                aboveFound = true;
                continue;
            }
            if (aboveFound) {
                if(child.isVisible()){
                    final Rectangle cb = child.getBounds();
                    final Region cr = child.getRegion();
                    final Region tr = cr.getTranslatedRegion(cb.x, cb.y);
                    r = r.getDifference(tr.getIntersection(getContentSize()));
                }
            }
        }
        return r;
    }

    // ---- UTILITY METHODS ---- //

    /**
     * Finds a top-most visible component for the given point. The location is
     * specified relative to the peer's parent.
     */
    @Override
    final LWComponentPeer<?, ?> findPeerAt(int x, int y) {
        LWComponentPeer<?, ?> peer = super.findPeerAt(x, y);
        final Rectangle r = getBounds();
        // Translate to this container's coordinates to pass to children
        x -= r.x;
        y -= r.y;
        if (peer != null && getContentSize().contains(x, y)) {
            synchronized (getPeerTreeLock()) {
                for (int i = childPeers.size() - 1; i >= 0; --i) {
                    LWComponentPeer<?, ?> p = childPeers.get(i).findPeerAt(x, y);
                    if (p != null) {
                        peer = p;
                        break;
                    }
                }
            }
        }
        return peer;
    }

    /*
    * Called by the container when any part of this peer or child
    * peers should be repainted
    */
    @Override
    final void repaintPeer(final Rectangle r) {
        final Rectangle toPaint = getSize().intersection(r);
        if (!isShowing() || toPaint.isEmpty()) {
            return;
        }
        // First, post the PaintEvent for this peer
        super.repaintPeer(toPaint);
        // Second, handle all the children
        // Use the straight order of children, so the bottom
        // ones are painted first
        repaintChildren(toPaint);
    }

    /**
     * Paints all the child peers in the straight z-order, so the
     * bottom-most ones are painted first.
     */
    private void repaintChildren(final Rectangle r) {
        final Rectangle content = getContentSize();
        for (final LWComponentPeer<?, ?> child : getChildren()) {
            final Rectangle childBounds = child.getBounds();
            Rectangle toPaint = r.intersection(childBounds);
            toPaint = toPaint.intersection(content);
            toPaint.translate(-childBounds.x, -childBounds.y);
            child.repaintPeer(toPaint);
        }
    }

    Rectangle getContentSize() {
        return getSize();
    }

    @Override
    public void setEnabled(final boolean e) {
        super.setEnabled(e);
        for (final LWComponentPeer<?, ?> child : getChildren()) {
            child.setEnabled(e && child.getTarget().isEnabled());
        }
    }

    @Override
    public void setBackground(final Color c) {
        for (final LWComponentPeer<?, ?> child : getChildren()) {
            if (!child.getTarget().isBackgroundSet()) {
                child.setBackground(c);
            }
        }
        super.setBackground(c);
    }

    @Override
    public void setForeground(final Color c) {
        for (final LWComponentPeer<?, ?> child : getChildren()) {
            if (!child.getTarget().isForegroundSet()) {
                child.setForeground(c);
            }
        }
        super.setForeground(c);
    }

    @Override
    public void setFont(final Font f) {
        for (final LWComponentPeer<?, ?> child : getChildren()) {
            if (!child.getTarget().isFontSet()) {
                child.setFont(f);
            }
        }
        super.setFont(f);
    }

    @Override
    public final void paint(final Graphics g) {
        super.paint(g);
        SunGraphicsCallback.PaintHeavyweightComponentsCallback.getInstance()
                .runComponents(getTarget().getComponents(), g,
                               SunGraphicsCallback.LIGHTWEIGHTS
                               | SunGraphicsCallback.HEAVYWEIGHTS);
    }

    @Override
    public final void print(final Graphics g) {
        super.print(g);
        SunGraphicsCallback.PrintHeavyweightComponentsCallback.getInstance()
                .runComponents(getTarget().getComponents(), g,
                               SunGraphicsCallback.LIGHTWEIGHTS
                               | SunGraphicsCallback.HEAVYWEIGHTS);
    }
}
