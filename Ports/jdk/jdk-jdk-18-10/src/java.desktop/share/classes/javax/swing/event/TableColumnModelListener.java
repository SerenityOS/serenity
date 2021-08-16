/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.event;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ChangeEvent;
import java.util.EventListener;

/**
 * TableColumnModelListener defines the interface for an object that listens
 * to changes in a TableColumnModel.
 *
 * @author Alan Chung
 * @see TableColumnModelEvent
 */

public interface TableColumnModelListener extends java.util.EventListener
{
    /**
     * Tells listeners that a column was added to the model.
     *
     * @param e a {@code TableColumnModelEvent}
     */
    public void columnAdded(TableColumnModelEvent e);

    /**
     * Tells listeners that a column was removed from the model.
     *
     * @param e a {@code TableColumnModelEvent}
     */
    public void columnRemoved(TableColumnModelEvent e);

    /**
     * Tells listeners that a column was repositioned.
     *
     * @param e a {@code TableColumnModelEvent}
     */
    public void columnMoved(TableColumnModelEvent e);

    /**
     * Tells listeners that a column was moved due to a margin change.
     *
     * @param e a {@code ChangeEvent}
     */
    public void columnMarginChanged(ChangeEvent e);

    /**
     * Tells listeners that the selection model of the
     * TableColumnModel changed.
     *
     * @param e a {@code ListSelectionEvent}
     */
    public void columnSelectionChanged(ListSelectionEvent e);
}
