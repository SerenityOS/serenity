/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.List;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.peer.ListPeer;
import java.util.Arrays;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Lightweight implementation of {@link ListPeer}. Delegates most of the work to
 * the {@link JList}, which is placed inside {@link JScrollPane}.
 */
final class LWListPeer extends LWComponentPeer<List, LWListPeer.ScrollableJList>
        implements ListPeer {

    /**
     * The default number of visible rows.
     */
    private static final int DEFAULT_VISIBLE_ROWS = 4; // From java.awt.List,

    /**
     * This text is used for cell bounds calculation.
     */
    private static final String TEXT = "0123456789abcde";

    LWListPeer(final List target, final PlatformComponent platformComponent) {
        super(target, platformComponent);
        if (!getTarget().isBackgroundSet()) {
            getTarget().setBackground(SystemColor.text);
        }
    }

    @Override
    ScrollableJList createDelegate() {
        return new ScrollableJList();
    }

    @Override
    void initializeImpl() {
        super.initializeImpl();
        setMultipleMode(getTarget().isMultipleMode());
        makeVisible(getTarget().getVisibleIndex());
        final int[] selectedIndices = getTarget().getSelectedIndexes();
        synchronized (getDelegateLock()) {
            getDelegate().setSkipStateChangedEvent(true);
            getDelegate().getView().setSelectedIndices(selectedIndices);
            getDelegate().setSkipStateChangedEvent(false);
        }
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    Component getDelegateFocusOwner() {
        return getDelegate().getView();
    }

    @Override
    public int[] getSelectedIndexes() {
        synchronized (getDelegateLock()) {
            return getDelegate().getView().getSelectedIndices();
        }
    }

    @Override
    public void add(final String item, final int index) {
        synchronized (getDelegateLock()) {
            getDelegate().getModel().add(index, item);
            revalidate();
        }
    }

    @Override
    public void delItems(final int start, final int end) {
        synchronized (getDelegateLock()) {
            getDelegate().getModel().removeRange(start, end);
            revalidate();
        }
    }

    @Override
    public void removeAll() {
        synchronized (getDelegateLock()) {
            getDelegate().getModel().removeAllElements();
            revalidate();
        }
    }

    @Override
    public void select(final int index) {
        synchronized (getDelegateLock()) {
            getDelegate().setSkipStateChangedEvent(true);
            getDelegate().getView().setSelectedIndex(index);
            getDelegate().setSkipStateChangedEvent(false);
        }
    }

    @Override
    public void deselect(final int index) {
        synchronized (getDelegateLock()) {
            getDelegate().getView().getSelectionModel().
                    removeSelectionInterval(index, index);
        }
    }

    @Override
    public void makeVisible(final int index) {
        synchronized (getDelegateLock()) {
            getDelegate().getView().ensureIndexIsVisible(index);
        }
    }

    @Override
    public void setMultipleMode(final boolean m) {
        synchronized (getDelegateLock()) {
            getDelegate().getView().setSelectionMode(m ?
                    ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
                    : ListSelectionModel.SINGLE_SELECTION);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return getMinimumSize(DEFAULT_VISIBLE_ROWS);
    }

    @Override
    public Dimension getPreferredSize(final int rows) {
        return getMinimumSize(rows);
    }

    @Override
    public Dimension getMinimumSize(final int rows) {
        synchronized (getDelegateLock()) {
            final Dimension size = getCellSize();
            size.height *= rows;
            // Always take vertical scrollbar into account.
            final JScrollBar vbar = getDelegate().getVerticalScrollBar();
            size.width += vbar != null ? vbar.getMinimumSize().width : 0;
            // JScrollPane and JList insets
            final Insets pi = getDelegate().getInsets();
            final Insets vi = getDelegate().getView().getInsets();
            size.width += pi.left + pi.right + vi.left + vi.right;
            size.height += pi.top + pi.bottom + vi.top + vi.bottom;
            return size;
        }
    }

    private Dimension getCellSize() {
        final JList<String> jList = getDelegate().getView();
        final ListCellRenderer<? super String> cr = jList.getCellRenderer();
        final Component cell = cr.getListCellRendererComponent(jList, TEXT, 0,
                                                               false, false);
        return cell.getPreferredSize();
    }

    private void revalidate() {
        synchronized (getDelegateLock()) {
            getDelegate().getView().invalidate();
            getDelegate().validate();
        }
    }

    @SuppressWarnings("serial")// Safe: outer class is non-serializable.
    final class ScrollableJList extends JScrollPane implements ListSelectionListener {

        private boolean skipStateChangedEvent;

        private final DefaultListModel<String> model =
                new DefaultListModel<String>() {
                    @Override
                    public void add(final int index, final String element) {
                        if (index == -1) {
                            addElement(element);
                        } else {
                            super.add(index, element);
                        }
                    }
                };

        private int[] oldSelectedIndices = new int[0];

        ScrollableJList() {
            getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
            final JList<String> list = new JListDelegate();
            list.addListSelectionListener(this);

            getViewport().setView(list);

            // Pull the items from the target.
            final String[] items = getTarget().getItems();
            for (int i = 0; i < items.length; i++) {
                model.add(i, items[i]);
            }
        }

        public boolean isSkipStateChangedEvent() {
            return skipStateChangedEvent;
        }

        public void setSkipStateChangedEvent(boolean skipStateChangedEvent) {
            this.skipStateChangedEvent = skipStateChangedEvent;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void valueChanged(final ListSelectionEvent e) {
            if (!e.getValueIsAdjusting() && !isSkipStateChangedEvent()) {
                final JList<?> source = (JList<?>) e.getSource();
                for(int i = 0 ; i < source.getModel().getSize(); i++) {

                    final boolean wasSelected = Arrays.binarySearch(oldSelectedIndices, i) >= 0;
                    final boolean isSelected = source.isSelectedIndex(i);

                    if (wasSelected == isSelected) {
                        continue;
                    }

                    final int state = !wasSelected && isSelected ? ItemEvent.SELECTED: ItemEvent.DESELECTED;

                    LWListPeer.this.postEvent(new ItemEvent(getTarget(), ItemEvent.ITEM_STATE_CHANGED,
                            i, state));
                }
                oldSelectedIndices = source.getSelectedIndices();
            }
        }

        @SuppressWarnings("unchecked")
        public JList<String> getView() {
            return (JList<String>) getViewport().getView();
        }

        public DefaultListModel<String> getModel() {
            return model;
        }

        @Override
        public void setEnabled(final boolean enabled) {
            getView().setEnabled(enabled);
            super.setEnabled(enabled);
        }

        @Override
        public void setOpaque(final boolean isOpaque) {
            super.setOpaque(isOpaque);
            if (getView() != null) {
                getView().setOpaque(isOpaque);
            }
        }

        @Override
        public void setFont(Font font) {
            super.setFont(font);
            if (getView() != null) {
                getView().setFont(font);
                LWListPeer.this.revalidate();
            }
        }

        private final class JListDelegate extends JList<String> {

            JListDelegate() {
                super(model);
            }

            @Override
            public boolean hasFocus() {
                return getTarget().hasFocus();
            }

            @Override
            @SuppressWarnings("deprecation")
            protected void processMouseEvent(final MouseEvent e) {
                super.processMouseEvent(e);
                if (e.getID() == MouseEvent.MOUSE_CLICKED && e.getClickCount() == 2) {
                    final int index = locationToIndex(e.getPoint());
                    if (0 <= index && index < getModel().getSize()) {
                        LWListPeer.this.postEvent(new ActionEvent(getTarget(), ActionEvent.ACTION_PERFORMED,
                            getModel().getElementAt(index), e.getWhen(), e.getModifiers()));
                    }
                }
            }

            @Override
            @SuppressWarnings("deprecation")
            protected void processKeyEvent(final KeyEvent e) {
                super.processKeyEvent(e);
                if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    final String selectedValue = getSelectedValue();
                    if(selectedValue != null){
                        LWListPeer.this.postEvent(new ActionEvent(getTarget(), ActionEvent.ACTION_PERFORMED,
                            selectedValue, e.getWhen(), e.getModifiers()));
                    }
                }
            }

            //Needed for Autoscroller.
            @Override
            public Point getLocationOnScreen() {
                return LWListPeer.this.getLocationOnScreen();
            }
        }
    }
}
