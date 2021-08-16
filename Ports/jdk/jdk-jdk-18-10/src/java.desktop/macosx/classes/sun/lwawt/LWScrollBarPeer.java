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

import java.awt.Adjustable;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.peer.ScrollbarPeer;

import javax.swing.JScrollBar;

/**
 * Lightweight implementation of {@link ScrollbarPeer}. Delegates most of the
 * work to the {@link JScrollBar}.
 */
final class LWScrollBarPeer extends LWComponentPeer<Scrollbar, JScrollBar>
        implements ScrollbarPeer, AdjustmentListener {

    // JScrollBar fires two changes with firePropertyChange (one for old value
    // and one for new one.
    // We save the last value and don't fire event if not changed.
    private int currentValue;

    LWScrollBarPeer(final Scrollbar target,
                    final PlatformComponent platformComponent) {
        super(target, platformComponent);
    }

    @Override
    JScrollBar createDelegate() {
        return new JScrollBar();
    }

    @Override
    void initializeImpl() {
        super.initializeImpl();
        final Scrollbar target = getTarget();
        setLineIncrement(target.getUnitIncrement());
        setPageIncrement(target.getBlockIncrement());
        setValues(target.getValue(), target.getVisibleAmount(),
                  target.getMinimum(), target.getMaximum());

        final int orientation = target.getOrientation();
        final JScrollBar delegate = getDelegate();
        synchronized (getDelegateLock()) {
            delegate.setOrientation(orientation == Scrollbar.HORIZONTAL
                                    ? Adjustable.HORIZONTAL
                                    : Adjustable.VERTICAL);
            delegate.addAdjustmentListener(this);
        }
    }

    @Override
    public void setValues(final int value, final int visible, final int minimum,
                          final int maximum) {
        synchronized (getDelegateLock()) {
            currentValue = value;
            getDelegate().setValues(value, visible, minimum, maximum);
        }
    }

    @Override
    public void setLineIncrement(final int l) {
        synchronized (getDelegateLock()) {
            getDelegate().setUnitIncrement(l);
        }
    }

    @Override
    public void setPageIncrement(final int l) {
        synchronized (getDelegateLock()) {
            getDelegate().setBlockIncrement(l);
        }
    }

    // Peer also registered as a listener for ComponentDelegate component
    @Override
    public void adjustmentValueChanged(final AdjustmentEvent e) {
        final int value = e.getValue();
        synchronized (getDelegateLock()) {
            if (currentValue == value) {
                return;
            }
            currentValue = value;
        }
        getTarget().setValueIsAdjusting(e.getValueIsAdjusting());
        getTarget().setValue(value);
        postEvent(new AdjustmentEvent(getTarget(), e.getID(),
                e.getAdjustmentType(), value,
                e.getValueIsAdjusting()));
    }
}
