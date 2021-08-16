/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Point;
import java.awt.Choice;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.peer.ChoicePeer;

import javax.accessibility.Accessible;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import javax.swing.JPopupMenu;

/**
 * Lightweight implementation of {@link ChoicePeer}. Delegates most of the work
 * to the {@link JComboBox}.
 */
final class LWChoicePeer extends LWComponentPeer<Choice, JComboBox<String>>
        implements ChoicePeer, ItemListener {

    /**
     * According to Choice specification item events are sent in response to
     * user input, but not in response to calls to select(). But JComboBox are
     * sent item events in both cases. Should be used under delegateLock.
     */
    private boolean skipPostMessage;

    LWChoicePeer(final Choice target,
                 final PlatformComponent platformComponent) {
        super(target, platformComponent);
    }

    @Override
    JComboBox<String> createDelegate() {
        return new JComboBoxDelegate();
    }

    @Override
    void initializeImpl() {
        super.initializeImpl();
        final Choice choice = getTarget();
        final JComboBox<String> combo = getDelegate();
        synchronized (getDelegateLock()) {
            final int count = choice.getItemCount();
            for (int i = 0; i < count; ++i) {
                combo.addItem(choice.getItem(i));
            }
            select(choice.getSelectedIndex());

            // NOTE: the listener must be added at the very end, otherwise it
            // fires events upon initialization of the combo box.
            combo.addItemListener(this);
        }
    }

    @Override
    public void itemStateChanged(final ItemEvent e) {
        // AWT Choice sends SELECTED event only whereas JComboBox
        // sends both SELECTED and DESELECTED.
        if (e.getStateChange() == ItemEvent.SELECTED) {
            synchronized (getDelegateLock()) {
                if (skipPostMessage) {
                    return;
                }
                getTarget().select(getDelegate().getSelectedIndex());
            }
            postEvent(new ItemEvent(getTarget(), ItemEvent.ITEM_STATE_CHANGED,
                                    e.getItem(), ItemEvent.SELECTED));
        }
    }

    @Override
    public void add(final String item, final int index) {
        synchronized (getDelegateLock()) {
            getDelegate().insertItemAt(item, index);
        }
    }

    @Override
    public void remove(final int index) {
        synchronized (getDelegateLock()) {
            // We shouldn't post event, if selected item was removed.
            skipPostMessage = true;
            getDelegate().removeItemAt(index);
            skipPostMessage = false;
        }
    }

    @Override
    public void removeAll() {
        synchronized (getDelegateLock()) {
            getDelegate().removeAllItems();
        }
    }

    @Override
    public void select(final int index) {
        synchronized (getDelegateLock()) {
            if (index != getDelegate().getSelectedIndex()) {
                skipPostMessage = true;
                getDelegate().setSelectedIndex(index);
                skipPostMessage = false;
            }
        }
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @SuppressWarnings("serial")// Safe: outer class is non-serializable.
    private final class JComboBoxDelegate extends JComboBox<String> {

        @Override
        public boolean hasFocus() {
            return getTarget().hasFocus();
        }

        //Needed for proper popup menu location
        @Override
        public Point getLocationOnScreen() {
            return LWChoicePeer.this.getLocationOnScreen();
        }

        @Override
        public void firePopupMenuWillBecomeVisible() {
            super.firePopupMenuWillBecomeVisible();
            SwingUtilities.invokeLater(() -> {
                JPopupMenu popupMenu = getPopupMenu();
                // Need to override the invoker for proper grab handling
                if (popupMenu != null
                        && popupMenu.isShowing()
                        && popupMenu.getInvoker() != getTarget()) {
                    // The popup is now visible with correct location
                    // Save it and restore after toggling visibility and changing invoker
                    Point loc = popupMenu.getLocationOnScreen();
                    SwingUtilities.convertPointFromScreen(loc, this);
                    popupMenu.setVisible(false);
                    popupMenu.show(getTarget(), loc.x, loc.y);
                }
            });
        }

        private JPopupMenu getPopupMenu() {
            for (int i = 0; i < getAccessibleContext().getAccessibleChildrenCount(); i++) {
                Accessible child = getAccessibleContext().getAccessibleChild(i);
                if (child instanceof JPopupMenu) {
                    return  (JPopupMenu) child;
                }
            }
            return null;
        }
    }
}
