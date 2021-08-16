/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.peer.CheckboxPeer;
import java.beans.Transient;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/**
 * Lightweight implementation of {@link CheckboxPeer}. Delegates most of the
 * work to the {@link JCheckBox} and {@link JRadioButton}, which are placed
 * inside an empty {@link JComponent}.
 */
final class LWCheckboxPeer
        extends LWComponentPeer<Checkbox, LWCheckboxPeer.CheckboxDelegate>
        implements CheckboxPeer, ItemListener {

    LWCheckboxPeer(final Checkbox target,
                   final PlatformComponent platformComponent) {
        super(target, platformComponent);
    }

    @Override
    CheckboxDelegate createDelegate() {
        return new CheckboxDelegate();
    }

    @Override
    Component getDelegateFocusOwner() {
        return getDelegate().getCurrentButton();
    }

    @Override
    void initializeImpl() {
        super.initializeImpl();
        setLabel(getTarget().getLabel());
        setState(getTarget().getState());
        setCheckboxGroup(getTarget().getCheckboxGroup());
    }

    @Override
    public void itemStateChanged(final ItemEvent e) {
        // group.setSelectedCheckbox() will repaint the component
        // to let LWCheckboxPeer correctly handle it we should call it
        // after the current event is processed
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                boolean postEvent = true;
                final CheckboxGroup group = getTarget().getCheckboxGroup();
                if (group != null) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        if (group.getSelectedCheckbox() != getTarget()) {
                            group.setSelectedCheckbox(getTarget());
                        } else {
                            postEvent = false;
                        }
                    } else {
                        postEvent = false;
                        if (group.getSelectedCheckbox() == getTarget()) {
                            // Don't want to leave the group with no selected
                            // checkbox.
                            getTarget().setState(true);
                        }
                    }
                } else {
                    getTarget().setState(e.getStateChange()
                                         == ItemEvent.SELECTED);
                }
                if (postEvent) {
                    postEvent(new ItemEvent(getTarget(),
                                            ItemEvent.ITEM_STATE_CHANGED,
                                            getTarget().getLabel(),
                                            e.getStateChange()));
                }
            }
        });
    }

    @Override
    public void setCheckboxGroup(final CheckboxGroup g) {
        synchronized (getDelegateLock()) {
            getDelegate().getCurrentButton().removeItemListener(this);
            getDelegate().setRadioButton(g != null);
            getDelegate().getCurrentButton().addItemListener(this);
        }
        repaintPeer();
    }

    @Override
    public void setLabel(final String label) {
        synchronized (getDelegateLock()) {
            getDelegate().setText(label);
        }
    }

    @Override
    public void setState(final boolean state) {
        synchronized (getDelegateLock()) {
            getDelegate().getCurrentButton().removeItemListener(this);
            getDelegate().setSelected(state);
            getDelegate().getCurrentButton().addItemListener(this);
        }
        repaintPeer();
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @SuppressWarnings("serial")// Safe: outer class is non-serializable.
    final class CheckboxDelegate extends JComponent {

        private final JCheckBox cb;
        private final JRadioButton rb;

        CheckboxDelegate() {
            super();
            cb = new JCheckBox() {
                @Override
                public boolean hasFocus() {
                    return getTarget().hasFocus();
                }
            };
            rb = new JRadioButton() {
                @Override
                public boolean hasFocus() {
                    return getTarget().hasFocus();
                }
            };
            setLayout(null);
            setRadioButton(false);
            add(rb);
            add(cb);
        }

        @Override
        public void setEnabled(final boolean enabled) {
            super.setEnabled(enabled);
            rb.setEnabled(enabled);
            cb.setEnabled(enabled);
        }

        @Override
        public void setOpaque(final boolean isOpaque) {
            super.setOpaque(isOpaque);
            rb.setOpaque(isOpaque);
            cb.setOpaque(isOpaque);
        }

        @Override
        @Deprecated
        public void reshape(final int x, final int y, final int w,
                            final int h) {
            super.reshape(x, y, w, h);
            cb.setBounds(0, 0, w, h);
            rb.setBounds(0, 0, w, h);
        }

        @Override
        public Dimension getPreferredSize() {
            return getCurrentButton().getPreferredSize();
        }

        @Override
        @Transient
        public Dimension getMinimumSize() {
            return getCurrentButton().getMinimumSize();
        }

        void setRadioButton(final boolean showRadioButton) {
            rb.setVisible(showRadioButton);
            cb.setVisible(!showRadioButton);
        }

        @Transient
        JToggleButton getCurrentButton() {
            return cb.isVisible() ? cb : rb;
        }

        void setText(final String label) {
            cb.setText(label);
            rb.setText(label);
        }

        void setSelected(final boolean state) {
            cb.setSelected(state);
            rb.setSelected(state);
        }
    }
}
