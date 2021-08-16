/*
 * Copyright (c) 2000, 2014, Oracle and/or its affiliates. All rights reserved.
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


// jmx import
//


/**
 * <p>Describes an attribute of an open MBean.</p>
 *
 * <p>This interface declares the same methods as the class {@link
 * javax.management.MBeanAttributeInfo}.  A class implementing this
 * interface (typically {@link OpenMBeanAttributeInfoSupport}) should
 * extend {@link javax.management.MBeanAttributeInfo}.</p>
 *
 *
 * @since 1.5
 */
public interface OpenMBeanAttributeInfo extends OpenMBeanParameterInfo {


    // Re-declares the methods that are in class MBeanAttributeInfo of JMX 1.0
    // (these will be removed when MBeanAttributeInfo is made a parent interface of this interface)

    /**
     * Returns {@code true} if the attribute described by this {@code OpenMBeanAttributeInfo}
     * instance is readable, {@code false} otherwise.
     *
     * @return true if the attribute is readable.
     */
    public boolean isReadable() ;

    /**
     * Returns {@code true} if the attribute described by this {@code OpenMBeanAttributeInfo}
     * instance is writable, {@code false} otherwise.
     *
     * @return true if the attribute is writable.
     */
    public boolean isWritable() ;

    /**
     * Returns {@code true} if the attribute described by this {@code OpenMBeanAttributeInfo} instance
     * is accessed through a <code>is<i>XXX</i></code> getter
     * (applies only to {@code boolean} and {@code Boolean} values),
     * {@code false} otherwise.
     *
     * @return true if the attribute is accessed through <code>is<i>XXX</i></code>.
     */
    public boolean isIs() ;


    // commodity methods
    //

    /**
     * Compares the specified <var>obj</var> parameter with this
     * {@code OpenMBeanAttributeInfo} instance for equality.
     * <p>
     * Returns {@code true} if and only if all of the following statements are true:
     * <ul>
     * <li><var>obj</var> is non null,</li>
     * <li><var>obj</var> also implements the {@code OpenMBeanAttributeInfo} interface,</li>
     * <li>their names are equal</li>
     * <li>their open types are equal</li>
     * <li>their access properties (isReadable, isWritable and isIs) are equal</li>
     * <li>their default, min, max and legal values are equal.</li>
     * </ul>
     * This ensures that this {@code equals} method works properly for <var>obj</var> parameters which are
     * different implementations of the {@code OpenMBeanAttributeInfo} interface.
     * <br>&nbsp;
     * @param  obj  the object to be compared for equality with this {@code OpenMBeanAttributeInfo} instance;
     *
     * @return  {@code true} if the specified object is equal to this {@code OpenMBeanAttributeInfo} instance.
     */
    public boolean equals(Object obj);

    /**
     * Returns the hash code value for this {@code OpenMBeanAttributeInfo} instance.
     * <p>
     * The hash code of an {@code OpenMBeanAttributeInfo} instance is the sum of the hash codes
     * of all elements of information used in {@code equals} comparisons
     * (ie: its name, its <i>open type</i>, and its default, min, max and legal values).
     * <p>
     * This ensures that {@code t1.equals(t2)} implies that {@code t1.hashCode()==t2.hashCode()}
     * for any two {@code OpenMBeanAttributeInfo} instances {@code t1} and {@code t2},
     * as required by the general contract of the method
     * {@link Object#hashCode() Object.hashCode()}.
     *
     * @return  the hash code value for this {@code OpenMBeanAttributeInfo} instance
     */
    public int hashCode();

    /**
     * Returns a string representation of this {@code OpenMBeanAttributeInfo} instance.
     * <p>
     * The string representation consists of the name of this class
     * (ie {@code javax.management.openmbean.OpenMBeanAttributeInfo}),
     * the string representation of the name and open type of the described attribute,
     * and the string representation of its default, min, max and legal values.
     *
     * @return  a string representation of this {@code OpenMBeanAttributeInfo} instance
     */
    public String toString();


    // methods specific to open MBeans are inherited from
    // OpenMBeanParameterInfo
    //

}
