/*
 * Copyright (c) 1997, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.accessibility;

/**
 * This {@code AccessibleSelection} interface provides the standard mechanism
 * for an assistive technology to determine what the current selected children
 * are, as well as modify the selection set. Any object that has children that
 * can be selected should support the {@code AccessibleSelection} interface.
 * Applications can determine if an object supports the
 * {@code AccessibleSelection} interface by first obtaining its
 * {@code AccessibleContext} (see {@link Accessible}) and then calling the
 * {@link AccessibleContext#getAccessibleSelection} method. If the return value
 * is not {@code null}, the object supports this interface.
 *
 * @author Peter Korn
 * @author Hans Muller
 * @author Willie Walker
 * @see Accessible
 * @see Accessible#getAccessibleContext
 * @see AccessibleContext
 * @see AccessibleContext#getAccessibleSelection
 */
public interface AccessibleSelection {

    /**
     * Returns the number of {@code Accessible} children currently selected. If
     * no children are selected, the return value will be 0.
     *
     * @return the number of items currently selected
     */
    public int getAccessibleSelectionCount();

    /**
     * Returns an {@code Accessible} representing the specified selected child
     * of the object. If there isn't a selection, or there are fewer children
     * selected than the integer passed in, the return value will be
     * {@code null}.
     * <p>
     * Note that the index represents the i-th selected child, which is
     * different from the i-th child.
     *
     * @param  i the zero-based index of selected children
     * @return the i-th selected child
     * @see #getAccessibleSelectionCount
     */
    public Accessible getAccessibleSelection(int i);

    /**
     * Determines if the current child of this object is selected.
     *
     * @param  i the zero-based index of the child in this {@code Accessible}
     *         object
     * @return {@code true} if the current child of this object is selected;
     *         else {@code false}
     * @see AccessibleContext#getAccessibleChild
     */
    public boolean isAccessibleChildSelected(int i);

    /**
     * Adds the specified {@code Accessible} child of the object to the object's
     * selection. If the object supports multiple selections, the specified
     * child is added to any existing selection, otherwise it replaces any
     * existing selection in the object. If the specified child is already
     * selected, this method has no effect.
     *
     * @param  i the zero-based index of the child
     * @see AccessibleContext#getAccessibleChild
     */
    public void addAccessibleSelection(int i);

    /**
     * Removes the specified child of the object from the object's selection. If
     * the specified item isn't currently selected, this method has no effect.
     *
     * @param  i the zero-based index of the child
     * @see AccessibleContext#getAccessibleChild
     */
    public void removeAccessibleSelection(int i);

    /**
     * Clears the selection in the object, so that no children in the object are
     * selected.
     */
    public void clearAccessibleSelection();

    /**
     * Causes every child of the object to be selected if the object supports
     * multiple selections.
     */
    public void selectAllAccessibleSelection();
}
