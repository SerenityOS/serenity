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

import java.io.Serial;
import java.io.Serializable;

/**
 * Class {@code AttributeSetUtilities} provides static methods for manipulating
 * {@code AttributeSets}.
 * <ul>
 *   <li>Methods for creating unmodifiable and synchronized views of attribute
 *   sets.
 *   <li>operations useful for building implementations of interface
 *   {@link AttributeSet AttributeSet}
 * </ul>
 * An <b>unmodifiable view</b> <i>U</i> of an {@code AttributeSet} <i>S</i>
 * provides a client with "read-only" access to <i>S</i>. Query operations on
 * <i>U</i> "read through" to <i>S</i>; thus, changes in <i>S</i> are reflected
 * in <i>U</i>. However, any attempt to modify <i>U</i>, results in an
 * {@code UnmodifiableSetException}. The unmodifiable view object <i>U</i> will
 * be serializable if the attribute set object <i>S</i> is serializable.
 * <p>
 * A <b>synchronized view</b> <i>V</i> of an attribute set <i>S</i> provides a
 * client with synchronized (multiple thread safe) access to <i>S</i>. Each
 * operation of <i>V</i> is synchronized using <i>V</i> itself as the lock
 * object and then merely invokes the corresponding operation of <i>S</i>. In
 * order to guarantee mutually exclusive access, it is critical that all access
 * to <i>S</i> is accomplished through <i>V</i>. The synchronized view object
 * <i>V</i> will be serializable if the attribute set object <i>S</i> is
 * serializable.
 * <p>
 * As mentioned in the package description of {@code javax.print}, a
 * {@code null} reference parameter to methods is incorrect unless explicitly
 * documented on the method as having a meaningful interpretation. Usage to the
 * contrary is incorrect coding and may result in a run time exception either
 * immediately or at some later time. {@code IllegalArgumentException} and
 * {@code NullPointerException} are examples of typical and acceptable run time
 * exceptions for such cases.
 *
 * @author Alan Kaminsky
 */
public final class AttributeSetUtilities {

    /**
     * Suppress default constructor, ensuring non-instantiability.
     */
    private AttributeSetUtilities() {
    }

    /**
     * Unmodifiable view of {@code AttributeSet}.
     *
     * @serial include
     */
    private static class UnmodifiableAttributeSet
        implements AttributeSet, Serializable {

        /**
         * Use serialVersionUID from JDK 1.4 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = -6131802583863447813L;

        /**
         * The attribute set.
         */
        @SuppressWarnings("serial") // Not statically typed as Serializable
        private AttributeSet attrset;

        /**
         * Constructs unmodifiable view of the underlying attribute set.
         *
         * @param  attributeSet the attribute set
         */
        public UnmodifiableAttributeSet(AttributeSet attributeSet) {

            attrset = attributeSet;
        }

        public Attribute get(Class<?> key) {
            return attrset.get(key);
        }

        public boolean add(Attribute attribute) {
            throw new UnmodifiableSetException();
        }

        public synchronized boolean remove(Class<?> category) {
            throw new UnmodifiableSetException();
        }

        public boolean remove(Attribute attribute) {
            throw new UnmodifiableSetException();
        }

        public boolean containsKey(Class<?> category) {
            return attrset.containsKey(category);
        }

        public boolean containsValue(Attribute attribute) {
            return attrset.containsValue(attribute);
        }

        public boolean addAll(AttributeSet attributes) {
            throw new UnmodifiableSetException();
        }

        public int size() {
            return attrset.size();
        }

        public Attribute[] toArray() {
            return attrset.toArray();
        }

        public void clear() {
            throw new UnmodifiableSetException();
        }

        public boolean isEmpty() {
            return attrset.isEmpty();
        }

        public boolean equals(Object o) {
            return attrset.equals (o);
        }

        public int hashCode() {
            return attrset.hashCode();
        }
    }

    /**
     * Unmodifiable view of {@code DocAttributeSet}.
     *
     * @serial include
     */
    private static class UnmodifiableDocAttributeSet
        extends UnmodifiableAttributeSet
        implements DocAttributeSet, Serializable {

        /**
         * Use serialVersionUID from JDK 1.4 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = -6349408326066898956L;

        /**
         * Constructs a new unmodifiable doc attribute set.
         *
         * @param  attributeSet the doc attribute set
         */
        public UnmodifiableDocAttributeSet(DocAttributeSet attributeSet) {

            super (attributeSet);
        }
    }

    /**
     * Unmodifiable view of {@code PrintRequestAttributeSet}.
     *
     * @serial include
     */
    private static class UnmodifiablePrintRequestAttributeSet
        extends UnmodifiableAttributeSet
        implements PrintRequestAttributeSet, Serializable
    {

        /**
         * Use serialVersionUID from JDK 1.4 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 7799373532614825073L;

        /**
         * Constructs a new unmodifiable print request attribute set.
         *
         * @param  attributeSet the print request attribute set
         */
        public UnmodifiablePrintRequestAttributeSet
            (PrintRequestAttributeSet attributeSet) {

            super (attributeSet);
        }
    }

    /**
     * Unmodifiable view of {@code PrintJobAttributeSet}.
     *
     * @serial include
     */
    private static class UnmodifiablePrintJobAttributeSet
        extends UnmodifiableAttributeSet
        implements PrintJobAttributeSet, Serializable
    {
        /**
         * Use serialVersionUID from JDK 1.4 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = -8002245296274522112L;

        /**
         * Constructs a new unmodifiable print job attribute set.
         *
         * @param  attributeSet the print job attribute set
         */
        public UnmodifiablePrintJobAttributeSet
            (PrintJobAttributeSet attributeSet) {

            super (attributeSet);
        }
    }

    /**
     * Unmodifiable view of {@code PrintServiceAttributeSet}.
     *
     * @serial include
     */
    private static class UnmodifiablePrintServiceAttributeSet
        extends UnmodifiableAttributeSet
        implements PrintServiceAttributeSet, Serializable
    {
        /**
         * Use serialVersionUID from JDK 1.4 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = -7112165137107826819L;

        /**
         * Constructs a new unmodifiable print service attribute set.
         *
         * @param  attributeSet the print service attribute set
         */
        public UnmodifiablePrintServiceAttributeSet
            (PrintServiceAttributeSet attributeSet) {

            super (attributeSet);
        }
    }

    /**
     * Creates an unmodifiable view of the given attribute set.
     *
     * @param  attributeSet underlying attribute set
     * @return unmodifiable view of {@code attributeSet}
     * @throws NullPointerException if {@code attributeSet} is {@code null}
     */
    public static AttributeSet unmodifiableView(AttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }

        return new UnmodifiableAttributeSet(attributeSet);
    }

    /**
     * Creates an unmodifiable view of the given doc attribute set.
     *
     * @param  attributeSet underlying doc attribute set
     * @return unmodifiable view of {@code attributeSet}
     * @throws NullPointerException if {@code attributeSet} is {@code null}
     */
    public static DocAttributeSet unmodifiableView
        (DocAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new UnmodifiableDocAttributeSet(attributeSet);
    }

    /**
     * Creates an unmodifiable view of the given print request attribute set.
     *
     * @param  attributeSet underlying print request attribute set
     * @return unmodifiable view of {@code attributeSet}
     * @throws NullPointerException if {@code attributeSet} is {@code null}
     */
    public static PrintRequestAttributeSet
        unmodifiableView(PrintRequestAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new UnmodifiablePrintRequestAttributeSet(attributeSet);
    }

    /**
     * Creates an unmodifiable view of the given print job attribute set.
     *
     * @param  attributeSet underlying print job attribute set
     * @return unmodifiable view of {@code attributeSet}
     * @throws NullPointerException if {@code attributeSet} is {@code null}
     */
    public static PrintJobAttributeSet
        unmodifiableView(PrintJobAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new UnmodifiablePrintJobAttributeSet(attributeSet);
    }

    /**
     * Creates an unmodifiable view of the given print service attribute set.
     *
     * @param  attributeSet underlying print service attribute set
     * @return unmodifiable view of {@code attributeSet}
     * @throws NullPointerException if {@code attributeSet} is {@code null}
     */
    public static PrintServiceAttributeSet
        unmodifiableView(PrintServiceAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new UnmodifiablePrintServiceAttributeSet (attributeSet);
    }

    /**
     * Synchronized view of {@code AttributeSet}.
     *
     * @serial include
     */
    private static class SynchronizedAttributeSet
                        implements AttributeSet, Serializable {

        /**
         * Use serialVersionUID from JDK 1.4 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 8365731020128564925L;

        /**
         * The attribute set.
         */
        @SuppressWarnings("serial") // Not statically typed as Serializable
        private AttributeSet attrset;

        /**
         * Constructs a new synchronized attribute set.
         *
         * @param  attributeSet the attribute set
         */
        public SynchronizedAttributeSet(AttributeSet attributeSet) {
            attrset = attributeSet;
        }

        public synchronized Attribute get(Class<?> category) {
            return attrset.get(category);
        }

        public synchronized boolean add(Attribute attribute) {
            return attrset.add(attribute);
        }

        public synchronized boolean remove(Class<?> category) {
            return attrset.remove(category);
        }

        public synchronized boolean remove(Attribute attribute) {
            return attrset.remove(attribute);
        }

        public synchronized boolean containsKey(Class<?> category) {
            return attrset.containsKey(category);
        }

        public synchronized boolean containsValue(Attribute attribute) {
            return attrset.containsValue(attribute);
        }

        public synchronized boolean addAll(AttributeSet attributes) {
            return attrset.addAll(attributes);
        }

        public synchronized int size() {
            return attrset.size();
        }

        public synchronized Attribute[] toArray() {
            return attrset.toArray();
        }

        public synchronized void clear() {
            attrset.clear();
        }

        public synchronized boolean isEmpty() {
            return attrset.isEmpty();
        }

        public synchronized boolean equals(Object o) {
            return attrset.equals (o);
        }

        public synchronized int hashCode() {
            return attrset.hashCode();
        }
    }

    /**
     * Synchronized view of {@code DocAttributeSet}.
     *
     * @serial include
     */
    private static class SynchronizedDocAttributeSet
        extends SynchronizedAttributeSet
        implements DocAttributeSet, Serializable {

        /**
         * Use serialVersionUID from JDK 1.4 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 6455869095246629354L;

        /**
         * Constructs a new synchronized doc attribute set.
         *
         * @param  attributeSet the doc attribute set
         */
        public SynchronizedDocAttributeSet(DocAttributeSet attributeSet) {
            super(attributeSet);
        }
    }

    /**
     * Synchronized view of {@code PrintRequestAttributeSet}.
     *
     * @serial include
     */
    private static class SynchronizedPrintRequestAttributeSet
        extends SynchronizedAttributeSet
        implements PrintRequestAttributeSet, Serializable {

        /**
         * Use serialVersionUID from JDK 1.4 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 5671237023971169027L;

        /**
         * Constructs a new synchronized print request attribute set.
         *
         * @param  attributeSet the print request attribute set
         */
        public SynchronizedPrintRequestAttributeSet
            (PrintRequestAttributeSet attributeSet) {
            super(attributeSet);
        }
    }

    /**
     * Synchronized view of {@code PrintJobAttributeSet}.
     *
     * @serial include
     */
    private static class SynchronizedPrintJobAttributeSet
        extends SynchronizedAttributeSet
        implements PrintJobAttributeSet, Serializable {

        /**
         * Use serialVersionUID from JDK 1.4 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = 2117188707856965749L;

        /**
         * Constructs a new synchronized print job attribute set.
         *
         * @param  attributeSet the print job attribute set
         */
        public SynchronizedPrintJobAttributeSet
            (PrintJobAttributeSet attributeSet) {
            super(attributeSet);
        }
    }

    /**
     * Synchronized view of {@code PrintServiceAttributeSet}.
     *
     * @serial include
     */
    private static class SynchronizedPrintServiceAttributeSet
        extends SynchronizedAttributeSet
        implements PrintServiceAttributeSet, Serializable {

        /**
         * Use serialVersionUID from JDK 1.4 for interoperability.
         */
        @Serial
        private static final long serialVersionUID = -2830705374001675073L;

        /**
         * Constructs a new synchronized print service attribute set.
         *
         * @param  attributeSet the print service attribute set
         */
        public SynchronizedPrintServiceAttributeSet
            (PrintServiceAttributeSet attributeSet) {
            super(attributeSet);
        }
    }

    /**
     * Creates a synchronized view of the given attribute set.
     *
     * @param  attributeSet underlying attribute set
     * @return synchronized view of {@code attributeSet}
     * @throws NullPointerException if {@code attributeSet} is {@code null}
     */
    public static AttributeSet synchronizedView
        (AttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedAttributeSet(attributeSet);
    }

    /**
     * Creates a synchronized view of the given doc attribute set.
     *
     * @param  attributeSet underlying doc attribute set
     * @return synchronized view of {@code attributeSet}
     * @throws NullPointerException if {@code attributeSet} is {@code null}
     */
    public static DocAttributeSet
        synchronizedView(DocAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedDocAttributeSet(attributeSet);
    }

    /**
     * Creates a synchronized view of the given print request attribute set.
     *
     * @param  attributeSet underlying print request attribute set
     * @return synchronized view of {@code attributeSet}
     * @throws NullPointerException if {@code attributeSet} is {@code null}
     */
    public static PrintRequestAttributeSet
        synchronizedView(PrintRequestAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedPrintRequestAttributeSet(attributeSet);
    }

    /**
     * Creates a synchronized view of the given print job attribute set.
     *
     * @param  attributeSet underlying print job attribute set
     * @return synchronized view of {@code attributeSet}
     * @throws NullPointerException if {@code attributeSet} is {@code null}
     */
    public static PrintJobAttributeSet
        synchronizedView(PrintJobAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedPrintJobAttributeSet(attributeSet);
    }

    /**
     * Creates a synchronized view of the given print service attribute set.
     *
     * @param  attributeSet underlying print service attribute set
     * @return synchronized view of {@code attributeSet}
     * @throws NullPointerException if {@code attributeSet} is {@code null}
     */
    public static PrintServiceAttributeSet
        synchronizedView(PrintServiceAttributeSet attributeSet) {
        if (attributeSet == null) {
            throw new NullPointerException();
        }
        return new SynchronizedPrintServiceAttributeSet(attributeSet);
    }

    /**
     * Verify that the given object is a {@link Class Class} that implements the
     * given interface, which is assumed to be interface
     * {@link Attribute Attribute} or a subinterface thereof.
     *
     * @param  object {@code Object} to test
     * @param  interfaceName interface the object must implement
     * @return if {@code object} is a {@link Class Class} that implements
     *         {@code interfaceName}, {@code object} is returned downcast to
     *         type {@link Class Class}; otherwise an exception is thrown
     * @throws NullPointerException if {@code object} is {@code null}
     * @throws ClassCastException if {@code object} is not a
     *         {@link Class Class} that implements {@code interfaceName}
     */
    public static Class<?>
        verifyAttributeCategory(Object object, Class<?> interfaceName) {

        Class<?> result = (Class<?>) object;
        if (interfaceName.isAssignableFrom (result)) {
            return result;
        }
        else {
            throw new ClassCastException();
        }
    }

    /**
     * Verify that the given object is an instance of the given interface, which
     * is assumed to be interface {@link Attribute Attribute} or a subinterface
     * thereof.
     *
     * @param  object {@code Object} to test
     * @param  interfaceName interface of which the object must be an instance
     * @return if {@code object} is an instance of {@code interfaceName},
     *         {@code object} is returned downcast to type
     *         {@link Attribute Attribute}; otherwise an exception is thrown
     * @throws NullPointerException if {@code object} is {@code null}
     * @throws ClassCastException if {@code object} is not an instance of
     *         {@code interfaceName}
     */
    public static Attribute
        verifyAttributeValue(Object object, Class<?> interfaceName) {

        if (object == null) {
            throw new NullPointerException();
        }
        else if (interfaceName.isInstance (object)) {
            return (Attribute) object;
        } else {
            throw new ClassCastException();
        }
    }

    /**
     * Verify that the given attribute category object is equal to the category
     * of the given attribute value object. If so, this method returns doing
     * nothing. If not, this method throws an exception.
     *
     * @param  category attribute category to test
     * @param  attribute attribute value to test
     * @throws NullPointerException if the {@code category} or {@code attribute}
     *         are {@code null}
     * @throws IllegalArgumentException if the {@code category} is not equal to
     *         the category of the {@code attribute}
     */
    public static void
        verifyCategoryForValue(Class<?> category, Attribute attribute) {

        if (!category.equals (attribute.getCategory())) {
            throw new IllegalArgumentException();
        }
    }
}
