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

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.peer.ScrollPanePeer;
import java.util.List;

/**
 * Lightweight implementation of {@link ScrollPanePeer}. Delegates most of the
 * work to the {@link JScrollPane}.
 */
final class LWScrollPanePeer extends LWContainerPeer<ScrollPane, JScrollPane>
        implements ScrollPanePeer, ChangeListener {

    LWScrollPanePeer(final ScrollPane target,
                     final PlatformComponent platformComponent) {
        super(target, platformComponent);
    }

    @Override
    JScrollPane createDelegate() {
        final JScrollPane sp = new JScrollPane();
        final JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setVisible(false);
        sp.getViewport().setView(panel);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().addChangeListener(this);
        return sp;
    }

    @Override
    public void handleEvent(AWTEvent e) {
        if (e instanceof MouseWheelEvent) {
            MouseWheelEvent wheelEvent = (MouseWheelEvent) e;
            //java.awt.ScrollPane consumes the event
            // in case isWheelScrollingEnabled() is true,
            // forcibly send the consumed event to the delegate
            if (getTarget().isWheelScrollingEnabled() && wheelEvent.isConsumed()) {
                sendEventToDelegate(wheelEvent);
            }
        } else {
            super.handleEvent(e);
        }
    }

    @Override
    public void stateChanged(final ChangeEvent e) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final LWComponentPeer<?, ?> viewPeer = getViewPeer();
                if (viewPeer != null) {
                    final Rectangle r;
                    synchronized (getDelegateLock()) {
                        r = getDelegate().getViewport().getView().getBounds();
                    }
                    viewPeer.setBounds(r.x, r.y, r.width, r.height, SET_BOUNDS,
                                       true, true);
                }
            }
        });
    }

    @Override
    void initializeImpl() {
        super.initializeImpl();
        final int policy = getTarget().getScrollbarDisplayPolicy();
        synchronized (getDelegateLock()) {
            getDelegate().getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
            getDelegate().setVerticalScrollBarPolicy(convertVPolicy(policy));
            getDelegate().setHorizontalScrollBarPolicy(convertHPolicy(policy));
        }
    }

    LWComponentPeer<?, ?> getViewPeer() {
        final List<LWComponentPeer<?, ?>> peerList = getChildren();
        return peerList.isEmpty() ? null : peerList.get(0);
    }

    @Override
    Rectangle getContentSize() {
        Rectangle viewRect = getDelegate().getViewport().getViewRect();
        return new Rectangle(viewRect.width, viewRect.height);
    }

    @Override
    public void layout() {
        super.layout();
        synchronized (getDelegateLock()) {
            final LWComponentPeer<?, ?> viewPeer = getViewPeer();
            if (viewPeer != null) {
                Component view = getDelegate().getViewport().getView();
                view.setBounds(viewPeer.getBounds());
                view.setPreferredSize(viewPeer.getPreferredSize());
                view.setMinimumSize(viewPeer.getMinimumSize());
                getDelegate().invalidate();
                getDelegate().validate();
                viewPeer.setBounds(view.getBounds());
            }
        }
    }

    @Override
    public void setScrollPosition(int x, int y) {
    }

    @Override
    public int getHScrollbarHeight() {
        synchronized (getDelegateLock()) {
            return getDelegate().getHorizontalScrollBar().getHeight();
        }
    }

    @Override
    public int getVScrollbarWidth() {
        synchronized (getDelegateLock()) {
            return getDelegate().getVerticalScrollBar().getWidth();
        }
    }

    @Override
    public void childResized(int w, int h) {
        synchronized (getDelegateLock()) {
            getDelegate().invalidate();
            getDelegate().validate();
        }
    }

    @Override
    public void setUnitIncrement(Adjustable adj, int u) {
    }

    @Override
    public void setValue(Adjustable adj, int v) {
    }

    private static int convertHPolicy(final int policy) {
        switch (policy) {
            case ScrollPane.SCROLLBARS_NEVER:
                return ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
            case ScrollPane.SCROLLBARS_ALWAYS:
                return ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS;
            default:
                return ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED;
        }
    }

    private static int convertVPolicy(final int policy) {
        switch (policy) {
            case ScrollPane.SCROLLBARS_NEVER:
                return ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;
            case ScrollPane.SCROLLBARS_ALWAYS:
                return ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS;
            default:
                return ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED;
        }
    }
}
