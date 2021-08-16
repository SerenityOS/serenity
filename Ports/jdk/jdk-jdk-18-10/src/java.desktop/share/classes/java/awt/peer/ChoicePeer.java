/*
 * Copyright (c) 1995, 2007, Oracle and/or its affiliates. All rights reserved.
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
package java.awt.peer;

import java.awt.Choice;

/**
 * The peer interface for {@link Choice}.
 *
 * The peer interfaces are intended only for use in porting
 * the AWT. They are not intended for use by application
 * developers, and developers should not implement peers
 * nor invoke any of the peer methods directly on the peer
 * instances.
 */
public interface ChoicePeer extends ComponentPeer {

    /**
     * Adds an item with the string {@code item} to the combo box list
     * at index {@code index}.
     *
     * @param item the label to be added to the list
     * @param index the index where to add the item
     *
     * @see Choice#add(String)
     */
    void add(String item, int index);

    /**
     * Removes the item at index {@code index} from the combo box list.
     *
     * @param index the index where to remove the item
     *
     * @see Choice#remove(int)
     */
    void remove(int index);

    /**
     * Removes all items from the combo box list.
     *
     * @see Choice#removeAll()
     */
    void removeAll();

    /**
     * Selects the item at index {@code index}.
     *
     * @param index the index which should be selected
     *
     * @see Choice#select(int)
     */
    void select(int index);

}
