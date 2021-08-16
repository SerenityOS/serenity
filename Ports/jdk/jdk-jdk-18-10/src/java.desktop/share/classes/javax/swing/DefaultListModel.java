/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing;

import java.util.Vector;
import java.util.Collection;
import java.util.Enumeration;


/**
 * This class loosely implements the {@code java.util.Vector}
 * API, in that it implements the 1.1.x version of
 * {@code java.util.Vector}, has no collection class support,
 * and notifies the {@code ListDataListener}s when changes occur.
 * Presently it delegates to a {@code Vector},
 * in a future release it will be a real Collection implementation.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the {@code java.beans} package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @param <E> the type of the elements of this model
 *
 * @author Hans Muller
 * @since 1.2
 */
@SuppressWarnings("serial") // Same-version serialization only
public class DefaultListModel<E> extends AbstractListModel<E>
{
    private Vector<E> delegate = new Vector<E>();

    /**
     * Constructs a {@code DefaultListModel}.
     */
    public DefaultListModel() {}

    /**
     * Returns the number of components in this list.
     * <p>
     * This method is identical to {@code size}, which implements the
     * {@code List} interface defined in the 1.2 Collections framework.
     * This method exists in conjunction with {@code setSize} so that
     * {@code size} is identifiable as a JavaBean property.
     *
     * @return  the number of components in this list
     * @see #size()
     */
    public int getSize() {
        return delegate.size();
    }

    /**
     * Returns the component at the specified index.
     * <blockquote>
     * <b>Note:</b> Although this method is not deprecated, the preferred
     *    method to use is {@code get(int)}, which implements the {@code List}
     *    interface defined in the 1.2 Collections framework.
     * </blockquote>
     * @param      index   an index into this list
     * @return     the component at the specified index
     * @throws     ArrayIndexOutOfBoundsException  if the {@code index}
     *             is negative or greater than the current size of this
     *             list
     * @see #get(int)
     */
    public E getElementAt(int index) {
        return delegate.elementAt(index);
    }

    /**
     * Copies the components of this list into the specified array.
     * The array must be big enough to hold all the objects in this list,
     * else an {@code IndexOutOfBoundsException} is thrown.
     *
     * @param   anArray   the array into which the components get copied
     * @see Vector#copyInto(Object[])
     */
    public void copyInto(Object[] anArray) {
        delegate.copyInto(anArray);
    }

    /**
     * Trims the capacity of this list to be the list's current size.
     *
     * @see Vector#trimToSize()
     */
    public void trimToSize() {
        delegate.trimToSize();
    }

    /**
     * Increases the capacity of this list, if necessary, to ensure
     * that it can hold at least the number of components specified by
     * the minimum capacity argument.
     *
     * @param   minCapacity   the desired minimum capacity
     * @see Vector#ensureCapacity(int)
     */
    public void ensureCapacity(int minCapacity) {
        delegate.ensureCapacity(minCapacity);
    }

    /**
     * Sets the size of this list.
     *
     * @param   newSize   the new size of this list
     * @see Vector#setSize(int)
     */
    public void setSize(int newSize) {
        int oldSize = delegate.size();
        delegate.setSize(newSize);
        if (oldSize > newSize) {
            fireIntervalRemoved(this, newSize, oldSize-1);
        }
        else if (oldSize < newSize) {
            fireIntervalAdded(this, oldSize, newSize-1);
        }
    }

    /**
     * Returns the current capacity of this list.
     *
     * @return  the current capacity
     * @see Vector#capacity()
     */
    public int capacity() {
        return delegate.capacity();
    }

    /**
     * Returns the number of components in this list.
     *
     * @return  the number of components in this list
     * @see Vector#size()
     */
    public int size() {
        return delegate.size();
    }

    /**
     * Tests whether this list has any components.
     *
     * @return  {@code true} if and only if this list has
     *          no components, that is, its size is zero;
     *          {@code false} otherwise
     * @see Vector#isEmpty()
     */
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    /**
     * Returns an enumeration of the components of this list.
     *
     * @return  an enumeration of the components of this list
     * @see Vector#elements()
     */
    public Enumeration<E> elements() {
        return delegate.elements();
    }

    /**
     * Tests whether the specified object is a component in this list.
     *
     * @param   elem   an object
     * @return  {@code true} if the specified object
     *          is the same as a component in this list
     * @see Vector#contains(Object)
     */
    public boolean contains(Object elem) {
        return delegate.contains(elem);
    }

    /**
     * Searches for the first occurrence of {@code elem}.
     *
     * @param   elem   an object
     * @return  the index of the first occurrence of the argument in this
     *          list; returns {@code -1} if the object is not found
     * @see Vector#indexOf(Object)
     */
    public int indexOf(Object elem) {
        return delegate.indexOf(elem);
    }

    /**
     * Searches for the first occurrence of {@code elem}, beginning
     * the search at {@code index}.
     *
     * @param   elem    the desired component
     * @param   index   the index from which to begin searching
     * @return  the index where the first occurrence of {@code elem}
     *          is found after {@code index}; returns {@code -1}
     *          if the {@code elem} is not found in the list
     * @see Vector#indexOf(Object,int)
     */
     public int indexOf(Object elem, int index) {
        return delegate.indexOf(elem, index);
    }

    /**
     * Returns the index of the last occurrence of {@code elem}.
     *
     * @param   elem   the desired component
     * @return  the index of the last occurrence of {@code elem}
     *          in the list; returns {@code elem} if the object is not found
     * @see Vector#lastIndexOf(Object)
     */
    public int lastIndexOf(Object elem) {
        return delegate.lastIndexOf(elem);
    }

    /**
     * Searches backwards for {@code elem}, starting from the
     * specified index, and returns an index to it.
     *
     * @param  elem    the desired component
     * @param  index   the index to start searching from
     * @return the index of the last occurrence of the {@code elem}
     *          in this list at position less than {@code index};
     *          returns {@code -1} if the object is not found
     * @see Vector#lastIndexOf(Object,int)
     */
    public int lastIndexOf(Object elem, int index) {
        return delegate.lastIndexOf(elem, index);
    }

    /**
     * Returns the component at the specified index.
     * <blockquote>
     * <b>Note:</b> Although this method is not deprecated, the preferred
     *    method to use is {@code get(int)}, which implements the
     *    {@code List} interface defined in the 1.2 Collections framework.
     * </blockquote>
     *
     * @param      index   an index into this list
     * @return     the component at the specified index
     * @throws     ArrayIndexOutOfBoundsException if the index
     * is negative or not less than the size of the list
     * @see #get(int)
     * @see Vector#elementAt(int)
     */
    public E elementAt(int index) {
        return delegate.elementAt(index);
    }

    /**
     * Returns the first component of this list.
     * @return     the first component of this list
     * @see Vector#firstElement()
     * @throws java.util.NoSuchElementException if this
     * vector has no components
     */
    public E firstElement() {
        return delegate.firstElement();
    }

    /**
     * Returns the last component of the list.
     *
     * @return  the last component of the list
     * @see Vector#lastElement()
     * @throws java.util.NoSuchElementException if this vector
     * has no components
     */
    public E lastElement() {
        return delegate.lastElement();
    }

    /**
     * Sets the component at the specified {@code index} of this
     * list to be the specified element. The previous component at that
     * position is discarded.
     * <blockquote>
     * <b>Note:</b> Although this method is not deprecated, the preferred
     *    method to use is {@code set(int,Object)}, which implements the
     *    {@code List} interface defined in the 1.2 Collections framework.
     * </blockquote>
     *
     * @param      element what the component is to be set to
     * @param      index   the specified index
     * @throws     ArrayIndexOutOfBoundsException if the index is invalid
     * @see #set(int,Object)
     * @see Vector#setElementAt(Object,int)
     */
    public void setElementAt(E element, int index) {
        delegate.setElementAt(element, index);
        fireContentsChanged(this, index, index);
    }

    /**
     * Deletes the component at the specified index.
     * <blockquote>
     * <b>Note:</b> Although this method is not deprecated, the preferred
     *    method to use is {@code remove(int)}, which implements the
     *    {@code List} interface defined in the 1.2 Collections framework.
     * </blockquote>
     *
     * @param      index   the index of the object to remove
     * @see #remove(int)
     * @see Vector#removeElementAt(int)
     * @throws ArrayIndexOutOfBoundsException if the index is invalid
     */
    public void removeElementAt(int index) {
        delegate.removeElementAt(index);
        fireIntervalRemoved(this, index, index);
    }

    /**
     * Inserts the specified element as a component in this list at the
     * specified <code>index</code>.
     * <blockquote>
     * <b>Note:</b> Although this method is not deprecated, the preferred
     *    method to use is {@code add(int,Object)}, which implements the
     *    {@code List} interface defined in the 1.2 Collections framework.
     * </blockquote>
     *
     * @param      element the component to insert
     * @param      index   where to insert the new component
     * @exception  ArrayIndexOutOfBoundsException if the index was invalid
     * @see #add(int,Object)
     * @see Vector#insertElementAt(Object,int)
     */
    public void insertElementAt(E element, int index) {
        delegate.insertElementAt(element, index);
        fireIntervalAdded(this, index, index);
    }

    /**
     * Adds the specified component to the end of this list.
     *
     * @param   element   the component to be added
     * @see Vector#addElement(Object)
     */
    public void addElement(E element) {
        int index = delegate.size();
        delegate.addElement(element);
        fireIntervalAdded(this, index, index);
    }

    /**
     * Removes the first (lowest-indexed) occurrence of the argument
     * from this list.
     *
     * @param   obj   the component to be removed
     * @return  {@code true} if the argument was a component of this
     *          list; {@code false} otherwise
     * @see Vector#removeElement(Object)
     */
    public boolean removeElement(Object obj) {
        int index = indexOf(obj);
        boolean rv = delegate.removeElement(obj);
        if (index >= 0) {
            fireIntervalRemoved(this, index, index);
        }
        return rv;
    }


    /**
     * Removes all components from this list and sets its size to zero.
     * <blockquote>
     * <b>Note:</b> Although this method is not deprecated, the preferred
     *    method to use is {@code clear}, which implements the
     *    {@code List} interface defined in the 1.2 Collections framework.
     * </blockquote>
     *
     * @see #clear()
     * @see Vector#removeAllElements()
     */
    public void removeAllElements() {
        int index1 = delegate.size()-1;
        delegate.removeAllElements();
        if (index1 >= 0) {
            fireIntervalRemoved(this, 0, index1);
        }
    }


    /**
     * Returns a string that displays and identifies this
     * object's properties.
     *
     * @return a String representation of this object
     */
   public String toString() {
        return delegate.toString();
    }


    /* The remaining methods are included for compatibility with the
     * Java 2 platform Vector class.
     */

    /**
     * Returns an array containing all of the elements in this list in the
     * correct order.
     *
     * @return an array containing the elements of the list
     * @see Vector#toArray()
     */
    public Object[] toArray() {
        Object[] rv = new Object[delegate.size()];
        delegate.copyInto(rv);
        return rv;
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of element to return
     * @return the element at the specified position in this list
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     * ({@code index &lt; 0 || index &gt;= size()})
     */
    public E get(int index) {
        return delegate.elementAt(index);
    }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element.
     *
     * @param index index of element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     * ({@code index &lt; 0 || index &gt;= size()})
     */
    public E set(int index, E element) {
        E rv = delegate.elementAt(index);
        delegate.setElementAt(element, index);
        fireContentsChanged(this, index, index);
        return rv;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     *
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     * ({@code index &lt; 0 || index &gt; size()})
     */
    public void add(int index, E element) {
        delegate.insertElementAt(element, index);
        fireIntervalAdded(this, index, index);
    }

    /**
     * Removes the element at the specified position in this list.
     * Returns the element that was removed from the list
     *
     * @param index the index of the element to removed
     * @return the element previously at the specified position
     * @throws ArrayIndexOutOfBoundsException if the index is out of range
     * ({@code index &lt; 0 || index &gt;= size()})
     */
    public E remove(int index) {
        E rv = delegate.elementAt(index);
        delegate.removeElementAt(index);
        fireIntervalRemoved(this, index, index);
        return rv;
    }

    /**
     * Removes all of the elements from this list.  The list will
     * be empty after this call returns (unless it throws an exception).
     */
    public void clear() {
        int index1 = delegate.size()-1;
        delegate.removeAllElements();
        if (index1 >= 0) {
            fireIntervalRemoved(this, 0, index1);
        }
    }

    /**
     * Deletes the components at the specified range of indexes.
     * The removal is inclusive, so specifying a range of (1,5)
     * removes the component at index 1 and the component at index 5,
     * as well as all components in between.
     *
     * @param      fromIndex the index of the lower end of the range
     * @param      toIndex   the index of the upper end of the range
     * @throws ArrayIndexOutOfBoundsException if the index was invalid
     * @throws IllegalArgumentException if {@code fromIndex &gt; toIndex}
     * @see        #remove(int)
     */
    public void removeRange(int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex must be <= toIndex");
        }
        for(int i = toIndex; i >= fromIndex; i--) {
            delegate.removeElementAt(i);
        }
        fireIntervalRemoved(this, fromIndex, toIndex);
    }

    /**
     * Adds all of the elements present in the collection to the list.
     *
     * @param c the collection which contains the elements to add
     * @throws NullPointerException if {@code c} is null
     */
    public void addAll(Collection<? extends E> c) {
        if (c.isEmpty()) {
            return;
        }

        int startIndex = getSize();

        delegate.addAll(c);
        fireIntervalAdded(this, startIndex, getSize() - 1);
    }

    /**
     * Adds all of the elements present in the collection, starting
     * from the specified index.
     *
     * @param index index at which to insert the first element from the
     * specified collection
     * @param c the collection which contains the elements to add
     * @throws ArrayIndexOutOfBoundsException if {@code index} does not
     * fall within the range of number of elements currently held
     * @throws NullPointerException if {@code c} is null
     */
    public void addAll(int index, Collection<? extends E> c) {
        if (index < 0 || index > getSize()) {
            throw new ArrayIndexOutOfBoundsException("index out of range: " +
                                                                       index);
        }

        if (c.isEmpty()) {
            return;
        }

        delegate.addAll(index, c);
        fireIntervalAdded(this, index, index + c.size() - 1);
    }
}
