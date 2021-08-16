/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.print.attribute;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Class {@code HashAttributeSet} provides an {@code AttributeSet}
 * implementation with characteristics of a hash map.
 *
 * @author Alan Kaminsky
 */
public class HashAttributeSet implements AttributeSet, Serializable {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 5311560590283707917L;

    /**
     * The interface of which all members of this attribute set must be an
     * instance. It is assumed to be interface {@link Attribute Attribute} or a
     * subinterface thereof.
     *
     * @serial
     */
    private Class<?> myInterface;

    /**
     * A {@code HashMap} used by the implementation. The serialised form doesn't
     * include this instance variable.
     */
    private transient HashMap<Class<?>, Attribute> attrMap = new HashMap<>();

    /**
     * Write the instance to a stream (ie serialize the object).
     *
     * @param  s the output stream
     * @throws IOException if an I/O exception has occurred
     * @serialData The serialized form of an attribute set explicitly writes the
     *             number of attributes in the set, and each of the attributes.
     *             This does not guarantee equality of serialized forms since
     *             the order in which the attributes are written is not defined.
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {

        s.defaultWriteObject();
        Attribute [] attrs = toArray();
        s.writeInt(attrs.length);
        for (int i = 0; i < attrs.length; i++) {
            s.writeObject(attrs[i]);
        }
    }

    /**
     * Reconstitute an instance from a stream that is, deserialize it).
     *
     * @param  s the input stream
     * @throws ClassNotFoundException if the class is not found
     * @throws IOException if an I/O exception has occurred
     */
    @Serial
    private void readObject(ObjectInputStream s)
        throws ClassNotFoundException, IOException {

        s.defaultReadObject();
        attrMap = new HashMap<>();
        int count = s.readInt();
        Attribute attr;
        for (int i = 0; i < count; i++) {
            attr = (Attribute)s.readObject();
            add(attr);
        }
    }

    /**
     * Construct a new, empty attribute set.
     */
    public HashAttributeSet() {
        this(Attribute.class);
    }

    /**
     * Construct a new attribute set, initially populated with the given
     * attribute.
     *
     * @param  attribute attribute value to add to the set
     * @throws NullPointerException if {@code attribute} is {@code null}
     */
    public HashAttributeSet(Attribute attribute) {
        this (attribute, Attribute.class);
    }

    /**
     * Construct a new attribute set, initially populated with the values from
     * the given array. The new attribute set is populated by adding the
     * elements of {@code attributes} array to the set in sequence, starting at
     * index 0. Thus, later array elements may replace earlier array elements if
     * the array contains duplicate attribute values or attribute categories.
     *
     * @param  attributes array of attribute values to add to the set. If
     *         {@code null}, an empty attribute set is constructed.
     * @throws NullPointerException if any element of {@code attributes} is
     *         {@code null}
     */
    public HashAttributeSet(Attribute[] attributes) {
        this (attributes, Attribute.class);
    }

    /**
     * Construct a new attribute set, initially populated with the values from
     * the given set.
     *
     * @param  attributes set of attributes from which to initialise this set.
     *         If {@code null}, an empty attribute set is constructed.
     */
    public HashAttributeSet(AttributeSet attributes) {
        this (attributes, Attribute.class);
    }

    /**
     * Construct a new, empty attribute set, where the members of the attribute
     * set are restricted to the given interface.
     *
     * @param  interfaceName the interface of which all members of this
     *         attribute set must be an instance. It is assumed to be interface
     *         {@link Attribute Attribute} or a subinterface thereof.
     * @throws NullPointerException if {@code interfaceName} is {@code null}
     */
    protected HashAttributeSet(Class<?> interfaceName) {
        if (interfaceName == null) {
            throw new NullPointerException("null interface");
        }
        myInterface = interfaceName;
    }

    /**
     * Construct a new attribute set, initially populated with the given
     * attribute, where the members of the attribute set are restricted to the
     * given interface.
     *
     * @param  attribute attribute value to add to the set
     * @param  interfaceName the interface of which all members of this
     *         attribute set must be an instance. It is assumed to be interface
     *         {@link Attribute Attribute} or a subinterface thereof.
     * @throws NullPointerException if {@code attribute} or
     *         {@code interfaceName} are {@code null}
     * @throws ClassCastException if {@code attribute} is not an instance of
     *         {@code interfaceName}
     */
    protected HashAttributeSet(Attribute attribute, Class<?> interfaceName) {
        if (interfaceName == null) {
            throw new NullPointerException("null interface");
        }
        myInterface = interfaceName;
        add (attribute);
    }

    /**
     * Construct a new attribute set, where the members of the attribute set are
     * restricted to the given interface. The new attribute set is populated by
     * adding the elements of {@code attributes} array to the set in sequence,
     * starting at index 0. Thus, later array elements may replace earlier array
     * elements if the array contains duplicate attribute values or attribute
     * categories.
     *
     * @param  attributes array of attribute values to add to the set. If
     *         {@code null}, an empty attribute set is constructed.
     * @param  interfaceName the interface of which all members of this
     *         attribute set must be an instance. It is assumed to be interface
     *         {@link Attribute Attribute} or a subinterface thereof.
     * @throws NullPointerException if {@code interfaceName} is {@code null}, or
     *         if any element of {@code attributes} is {@code null}
     * @throws ClassCastException if any element of {@code attributes} is not an
     *         instance of {@code interfaceName}
     */
    protected HashAttributeSet(Attribute[] attributes, Class<?> interfaceName) {
        if (interfaceName == null) {
            throw new NullPointerException("null interface");
        }
        myInterface = interfaceName;
        int n = attributes == null ? 0 : attributes.length;
        for (int i = 0; i < n; ++ i) {
            add (attributes[i]);
        }
    }

    /**
     * Construct a new attribute set, initially populated with the values from
     * the given set where the members of the attribute set are restricted to
     * the given interface.
     *
     * @param  attributes set of attribute values to initialise the set. If
     *         {@code null}, an empty attribute set is constructed.
     * @param  interfaceName The interface of which all members of this
     *         attribute set must be an instance. It is assumed to be interface
     *         {@link Attribute Attribute} or a subinterface thereof.
     * @throws ClassCastException if any element of {@code attributes} is not an
     *         instance of {@code interfaceName}
     */
    protected HashAttributeSet(AttributeSet attributes, Class<?> interfaceName) {
      myInterface = interfaceName;
      if (attributes != null) {
        Attribute[] attribArray = attributes.toArray();
        int n = attribArray == null ? 0 : attribArray.length;
        for (int i = 0; i < n; ++ i) {
          add (attribArray[i]);
        }
      }
    }

    /**
     * Returns the attribute value which this attribute set contains in the
     * given attribute category. Returns {@code null} if this attribute set does
     * not contain any attribute value in the given attribute category.
     *
     * @param  category attribute category whose associated attribute value is
     *         to be returned. It must be a {@link Class Class} that implements
     *         interface {@link Attribute Attribute}.
     * @return the attribute value in the given attribute category contained in
     *         this attribute set, or {@code null} if this attribute set does
     *         not contain any attribute value in the given attribute category
     * @throws NullPointerException if the {@code category} is {@code null}
     * @throws ClassCastException if the {@code category} is not a
     *         {@link Class Class} that implements interface
     *         {@link Attribute Attribute}
     */
    public Attribute get(Class<?> category) {
        return attrMap.get(AttributeSetUtilities.
                           verifyAttributeCategory(category,
                                                   Attribute.class));
    }

    /**
     * Adds the specified attribute to this attribute set if it is not already
     * present, first removing any existing in the same attribute category as
     * the specified attribute value.
     *
     * @param  attribute attribute value to be added to this attribute set
     * @return {@code true} if this attribute set changed as a result of the
     *         call, i.e., the given attribute value was not already a member of
     *         this attribute set
     * @throws NullPointerException if the {@code attribute} is {@code null}
     * @throws UnmodifiableSetException if this attribute set does not support
     *         the {@code add()} operation
     */
    public boolean add(Attribute attribute) {
        Object oldAttribute =
            attrMap.put(attribute.getCategory(),
                        AttributeSetUtilities.
                        verifyAttributeValue(attribute, myInterface));
        return (!attribute.equals(oldAttribute));
    }

    /**
     * Removes any attribute for this category from this attribute set if
     * present. If {@code category} is {@code null}, then {@code remove()} does
     * nothing and returns {@code false}.
     *
     * @param  category attribute category to be removed from this attribute set
     * @return {@code true} if this attribute set changed as a result of the
     *         call, i.e., the given attribute category had been a member of
     *         this attribute set
     * @throws UnmodifiableSetException if this attribute set does not support
     *         the {@code remove()} operation
     */
    public boolean remove(Class<?> category) {
        return
            category != null &&
            AttributeSetUtilities.
            verifyAttributeCategory(category, Attribute.class) != null &&
            attrMap.remove(category) != null;
    }

    /**
     * Removes the specified attribute from this attribute set if present. If
     * {@code attribute} is {@code null}, then {@code remove()} does nothing and
     * returns {@code false}.
     *
     * @param  attribute attribute value to be removed from this attribute set
     * @return {@code true} if this attribute set changed as a result of the
     *         call, i.e., the given attribute value had been a member of this
     *         attribute set
     * @throws UnmodifiableSetException if this attribute set does not support
     *         the {@code remove()} operation
     */
    public boolean remove(Attribute attribute) {
        return
            attribute != null &&
            attrMap.remove(attribute.getCategory()) != null;
    }

    /**
     * Returns {@code true} if this attribute set contains an attribute for the
     * specified category.
     *
     * @param  category whose presence in this attribute set is to be tested
     * @return {@code true} if this attribute set contains an attribute value
     *         for the specified category
     */
    public boolean containsKey(Class<?> category) {
        return
            category != null &&
            AttributeSetUtilities.
            verifyAttributeCategory(category, Attribute.class) != null &&
            attrMap.get(category) != null;
    }

    /**
     * Returns {@code true} if this attribute set contains the given attribute.
     *
     * @param  attribute value whose presence in this attribute set is to be
     *         tested
     * @return {@code true} if this attribute set contains the given attribute
     *         value
     */
    public boolean containsValue(Attribute attribute) {
        return
           attribute != null &&
           attribute instanceof Attribute &&
           attribute.equals(attrMap.get(attribute.getCategory()));
    }

    /**
     * Adds all of the elements in the specified set to this attribute. The
     * outcome is the same as if the {@link #add(Attribute) add(Attribute)}
     * operation had been applied to this attribute set successively with each
     * element from the specified set. The behavior of the
     * {@code addAll(AttributeSet)} operation is unspecified if the specified
     * set is modified while the operation is in progress.
     * <p>
     * If the {@code addAll(AttributeSet)} operation throws an exception, the
     * effect on this attribute set's state is implementation dependent;
     * elements from the specified set before the point of the exception may or
     * may not have been added to this attribute set.
     *
     * @param  attributes whose elements are to be added to this attribute set
     * @return {@code true} if this attribute set changed as a result of the
     *         call
     * @throws UnmodifiableSetException if this attribute set does not support
     *         the {@code addAll(AttributeSet)} method
     * @throws NullPointerException if some element in the specified set is
     *         {@code null}, or the set is {@code null}
     * @see #add(Attribute)
     */
    public boolean addAll(AttributeSet attributes) {

        Attribute []attrs = attributes.toArray();
        boolean result = false;
        for (int i=0; i<attrs.length; i++) {
            Attribute newValue =
                AttributeSetUtilities.verifyAttributeValue(attrs[i],
                                                           myInterface);
            Object oldValue = attrMap.put(newValue.getCategory(), newValue);
            result = (! newValue.equals(oldValue)) || result;
        }
        return result;
    }

    /**
     * Returns the number of attributes in this attribute set. If this attribute
     * set contains more than {@code Integer.MAX_VALUE} elements, returns
     * {@code Integer.MAX_VALUE}.
     *
     * @return the number of attributes in this attribute set
     */
    public int size() {
        return attrMap.size();
    }

    /**
     * Returns an array of the attributes contained in this set.
     *
     * @return the attributes contained in this set as an array, zero length if
     *         the {@code AttributeSet} is empty
     */
    public Attribute[] toArray() {
        Attribute []attrs = new Attribute[size()];
        attrMap.values().toArray(attrs);
        return attrs;
    }

    /**
     * Removes all attributes from this attribute set.
     *
     * @throws UnmodifiableSetException if this attribute set does not support
     *         the {@code clear()} operation
     */
    public void clear() {
        attrMap.clear();
    }

    /**
     * Returns {@code true} if this attribute set contains no attributes.
     *
     * @return {@code true} if this attribute set contains no attributes
     */
    public boolean isEmpty() {
        return attrMap.isEmpty();
    }

    /**
     * Compares the specified object with this attribute set for equality.
     * Returns {@code true} if the given object is also an attribute set and the
     * two attribute sets contain the same attribute category-attribute value
     * mappings. This ensures that the {@code equals()} method works properly
     * across different implementations of the {@code AttributeSet} interface.
     *
     * @param  object to be compared for equality with this attribute set
     * @return {@code true} if the specified object is equal to this attribute
     *         set
     */
    public boolean equals(Object object) {
        if (object == null || !(object instanceof AttributeSet)) {
            return false;
        }

        AttributeSet aset = (AttributeSet)object;
        if (aset.size() != size()) {
            return false;
        }

        Attribute[] attrs = toArray();
        for (int i=0;i<attrs.length; i++) {
            if (!aset.containsValue(attrs[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the hash code value for this attribute set. The hash code of an
     * attribute set is defined to be the sum of the hash codes of each entry in
     * the {@code AttributeSet}. This ensures that {@code t1.equals(t2)} implies
     * that {@code t1.hashCode()==t2.hashCode()} for any two attribute sets
     * {@code t1} and {@code t2}, as required by the general contract of
     * {@link Object#hashCode() Object.hashCode()}.
     *
     * @return the hash code value for this attribute set
     */
    public int hashCode() {
        int hcode = 0;
        Attribute[] attrs = toArray();
        for (int i=0;i<attrs.length; i++) {
            hcode += attrs[i].hashCode();
        }
        return hcode;
    }
}
