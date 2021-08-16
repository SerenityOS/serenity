/*
 * Copyright (c) 2000, 2007, Oracle and/or its affiliates. All rights reserved.
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


package javax.management.openmbean;


// java import
//
import java.util.Set;
import java.util.Collection;

// jmx import
//


/**
 * The {@code TabularData} interface specifies the behavior of a specific type of complex <i>open data</i> objects
 * which represent <i>tabular data</i> structures.
 *
 * @since 1.5
 */
public interface TabularData /*extends Map*/ {


    /* *** TabularData specific information methods *** */


    /**
     * Returns the <i>tabular type</i> describing this
     * {@code TabularData} instance.
     *
     * @return the tabular type.
     */
    public TabularType getTabularType();


    /**
     * Calculates the index that would be used in this {@code TabularData} instance to refer to the specified
     * composite data <var>value</var> parameter if it were added to this instance.
     * This method checks for the type validity of the specified <var>value</var>,
     * but does not check if the calculated index is already used to refer to a value in this {@code TabularData} instance.
     *
     * @param  value                      the composite data value whose index in this
     *                                    {@code TabularData} instance is to be calculated;
     *                                    must be of the same composite type as this instance's row type;
     *                                    must not be null.
     *
     * @return the index that the specified <var>value</var> would have in this {@code TabularData} instance.
     *
     * @throws NullPointerException       if <var>value</var> is {@code null}
     *
     * @throws InvalidOpenTypeException   if <var>value</var> does not conform to this {@code TabularData} instance's
     *                                    row type definition.
     */
    public Object[] calculateIndex(CompositeData value) ;




    /* *** Content information query methods *** */

    /**
     * Returns the number of {@code CompositeData} values (ie the
     * number of rows) contained in this {@code TabularData}
     * instance.
     *
     * @return the number of values contained.
     */
    public int size() ;

    /**
     * Returns {@code true} if the number of {@code CompositeData}
     * values (ie the number of rows) contained in this
     * {@code TabularData} instance is zero.
     *
     * @return true if this {@code TabularData} is empty.
     */
    public boolean isEmpty() ;

    /**
     * Returns {@code true} if and only if this {@code TabularData} instance contains a {@code CompositeData} value
     * (ie a row) whose index is the specified <var>key</var>. If <var>key</var> is {@code null} or does not conform to
     * this {@code TabularData} instance's {@code TabularType} definition, this method simply returns {@code false}.
     *
     * @param  key  the index value whose presence in this {@code TabularData} instance is to be tested.
     *
     * @return  {@code true} if this {@code TabularData} indexes a row value with the specified key.
     */
    public boolean containsKey(Object[] key) ;

    /**
     * Returns {@code true} if and only if this {@code TabularData} instance contains the specified
     * {@code CompositeData} value. If <var>value</var> is {@code null} or does not conform to
     * this {@code TabularData} instance's row type definition, this method simply returns {@code false}.
     *
     * @param  value  the row value whose presence in this {@code TabularData} instance is to be tested.
     *
     * @return  {@code true} if this {@code TabularData} instance contains the specified row value.
     */
    public boolean containsValue(CompositeData value) ;

    /**
     * Returns the {@code CompositeData} value whose index is
     * <var>key</var>, or {@code null} if there is no value mapping
     * to <var>key</var>, in this {@code TabularData} instance.
     *
     * @param key the key of the row to return.
     *
     * @return the value corresponding to <var>key</var>.
     *
     * @throws NullPointerException if the <var>key</var> is
     * {@code null}
     * @throws InvalidKeyException if the <var>key</var> does not
     * conform to this {@code TabularData} instance's *
     * {@code TabularType} definition
     */
    public CompositeData get(Object[] key) ;




    /* *** Content modification operations (one element at a time) *** */


    /**
     * Adds <var>value</var> to this {@code TabularData} instance.
     * The composite type of <var>value</var> must be the same as this
     * instance's row type (ie the composite type returned by
     * <code>this.getTabularType().{@link TabularType#getRowType
     * getRowType()}</code>), and there must not already be an existing
     * value in this {@code TabularData} instance whose index is the
     * same as the one calculated for the <var>value</var> to be
     * added. The index for <var>value</var> is calculated according
     * to this {@code TabularData} instance's {@code TabularType}
     * definition (see <code>TabularType.{@link
     * TabularType#getIndexNames getIndexNames()}</code>).
     *
     * @param  value                      the composite data value to be added as a new row to this {@code TabularData} instance;
     *                                    must be of the same composite type as this instance's row type;
     *                                    must not be null.
     *
     * @throws NullPointerException       if <var>value</var> is {@code null}
     * @throws InvalidOpenTypeException   if <var>value</var> does not conform to this {@code TabularData} instance's
     *                                    row type definition.
     * @throws KeyAlreadyExistsException  if the index for <var>value</var>, calculated according to
     *                                    this {@code TabularData} instance's {@code TabularType} definition
     *                                    already maps to an existing value in the underlying HashMap.
     */
    public void put(CompositeData value) ;

    /**
     * Removes the {@code CompositeData} value whose index is <var>key</var> from this {@code TabularData} instance,
     * and returns the removed value, or returns {@code null} if there is no value whose index is <var>key</var>.
     *
     * @param  key  the index of the value to get in this {@code TabularData} instance;
     *              must be valid with this {@code TabularData} instance's row type definition;
     *              must not be null.
     *
     * @return previous value associated with specified key, or {@code null}
     *         if there was no mapping for key.
     *
     * @throws NullPointerException  if the <var>key</var> is {@code null}
     * @throws InvalidKeyException   if the <var>key</var> does not conform to this {@code TabularData} instance's
     *                               {@code TabularType} definition
     */
    public CompositeData remove(Object[] key) ;




    /* ***   Content modification bulk operations   *** */


    /**
     * Add all the elements in <var>values</var> to this {@code TabularData} instance.
     * If any  element in <var>values</var> does not satisfy the constraints defined in {@link #put(CompositeData) put},
     * or if any two elements in <var>values</var> have the same index calculated according to this {@code TabularData}
     * instance's {@code TabularType} definition, then an exception describing the failure is thrown
     * and no element of <var>values</var> is added,  thus leaving this {@code TabularData} instance unchanged.
     *
     * @param  values  the array of composite data values to be added as new rows to this {@code TabularData} instance;
     *                 if <var>values</var> is {@code null} or empty, this method returns without doing anything.
     *
     * @throws NullPointerException       if an element of <var>values</var> is {@code null}
     * @throws InvalidOpenTypeException   if an element of <var>values</var> does not conform to
     *                                    this {@code TabularData} instance's row type definition
     * @throws KeyAlreadyExistsException  if the index for an element of <var>values</var>, calculated according to
     *                                    this {@code TabularData} instance's {@code TabularType} definition
     *                                    already maps to an existing value in this instance,
     *                                    or two elements of <var>values</var> have the same index.
     */
    public void putAll(CompositeData[] values) ;

    /**
     * Removes all {@code CompositeData} values (ie rows) from this {@code TabularData} instance.
     */
    public void clear();




    /* ***   Collection views of the keys and values   *** */


    /**
     * Returns a set view of the keys (ie the index values) of the
     * {@code CompositeData} values (ie the rows) contained in this
     * {@code TabularData} instance. The returned {@code Set} is a
     * {@code Set<List<?>>} but is declared as a {@code Set<?>} for
     * compatibility reasons. The returned set can be used to iterate
     * over the keys.
     *
     * @return a set view ({@code Set<List<?>>}) of the index values
     * used in this {@code TabularData} instance.
     */
    public Set<?> keySet();

    /**
     * Returns a collection view of the {@code CompositeData} values
     * (ie the rows) contained in this {@code TabularData} instance.
     * The returned {@code Collection} is a {@code Collection<CompositeData>}
     * but is declared as a {@code Collection<?>} for compatibility reasons.
     * The returned collection can be used to iterate over the values.
     *
     * @return a collection view ({@code Collection<CompositeData>})
     * of the rows contained in this {@code TabularData} instance.
     */
    public Collection<?> values();




    /* ***  Commodity methods from java.lang.Object  *** */


    /**
     * Compares the specified <var>obj</var> parameter with this {@code TabularData} instance for equality.
     * <p>
     * Returns {@code true} if and only if all of the following statements are true:
     * <ul>
     * <li><var>obj</var> is non null,</li>
     * <li><var>obj</var> also implements the {@code TabularData} interface,</li>
     * <li>their row types are equal</li>
     * <li>their contents (ie index to value mappings) are equal</li>
     * </ul>
     * This ensures that this {@code equals} method works properly for <var>obj</var> parameters which are
     * different implementations of the {@code TabularData} interface.
     * <br>&nbsp;
     * @param  obj  the object to be compared for equality with this {@code TabularData} instance;
     *
     * @return  {@code true} if the specified object is equal to this {@code TabularData} instance.
     */
    public boolean equals(Object obj);

    /**
     * Returns the hash code value for this {@code TabularData} instance.
     * <p>
     * The hash code of a {@code TabularData} instance is the sum of the hash codes
     * of all elements of information used in {@code equals} comparisons
     * (ie: its <i>tabular type</i> and its content, where the content is defined as all the index to value mappings).
     * <p>
     * This ensures that {@code t1.equals(t2)} implies that {@code t1.hashCode()==t2.hashCode()}
     * for any two {@code TabularDataSupport} instances {@code t1} and {@code t2},
     * as required by the general contract of the method
     * {@link Object#hashCode() Object.hashCode()}.
     *
     * @return  the hash code value for this {@code TabularDataSupport} instance
     */
    public int hashCode();

    /**
     * Returns a string representation of this {@code TabularData} instance.
     * <p>
     * The string representation consists of the name of the implementing class,
     * and the tabular type of this instance.
     *
     * @return  a string representation of this {@code TabularData} instance
     */
    public String toString();

}
