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
import java.util.Set;
import java.lang.Comparable; // to be substituted for jdk1.1.x


// jmx import
//


/**
 * <p>Describes a parameter used in one or more operations or
 * constructors of an open MBean.</p>
 *
 * <p>This interface declares the same methods as the class {@link
 * javax.management.MBeanParameterInfo}.  A class implementing this
 * interface (typically {@link OpenMBeanParameterInfoSupport}) should
 * extend {@link javax.management.MBeanParameterInfo}.</p>
 *
 *
 * @since 1.5
 */
public interface OpenMBeanParameterInfo {


    // Re-declares methods that are in class MBeanParameterInfo of JMX 1.0
    // (these will be removed when MBeanParameterInfo is made a parent interface of this interface)

    /**
     * Returns a human readable description of the parameter
     * described by this {@code OpenMBeanParameterInfo} instance.
     *
     * @return the description.
     */
    public String getDescription() ;

    /**
     * Returns the name of the parameter
     * described by this {@code OpenMBeanParameterInfo} instance.
     *
     * @return the name.
     */
    public String getName() ;


    // Now declares methods that are specific to open MBeans
    //

    /**
     * Returns the <i>open type</i> of the values of the parameter
     * described by this {@code OpenMBeanParameterInfo} instance.
     *
     * @return the open type.
     */
    public OpenType<?> getOpenType() ;

    /**
     * Returns the default value for this parameter, if it has one, or
     * {@code null} otherwise.
     *
     * @return the default value.
     */
    public Object getDefaultValue() ;

    /**
     * Returns the set of legal values for this parameter, if it has
     * one, or {@code null} otherwise.
     *
     * @return the set of legal values.
     */
    public Set<?> getLegalValues() ;

    /**
     * Returns the minimal value for this parameter, if it has one, or
     * {@code null} otherwise.
     *
     * @return the minimum value.
     */
    public Comparable<?> getMinValue() ;

    /**
     * Returns the maximal value for this parameter, if it has one, or
     * {@code null} otherwise.
     *
     * @return the maximum value.
     */
    public Comparable<?> getMaxValue() ;

    /**
     * Returns {@code true} if this parameter has a specified default
     * value, or {@code false} otherwise.
     *
     * @return true if there is a default value.
     */
    public boolean hasDefaultValue() ;

    /**
     * Returns {@code true} if this parameter has a specified set of
     * legal values, or {@code false} otherwise.
     *
     * @return true if there is a set of legal values.
     */
    public boolean hasLegalValues() ;

    /**
     * Returns {@code true} if this parameter has a specified minimal
     * value, or {@code false} otherwise.
     *
     * @return true if there is a minimum value.
     */
    public boolean hasMinValue() ;

    /**
     * Returns {@code true} if this parameter has a specified maximal
     * value, or {@code false} otherwise.
     *
     * @return true if there is a maximum value.
     */
    public boolean hasMaxValue() ;

    /**
     * Tests whether <var>obj</var> is a valid value for the parameter
     * described by this {@code OpenMBeanParameterInfo} instance.
     *
     * @param obj the object to be tested.
     *
     * @return {@code true} if <var>obj</var> is a valid value
     * for the parameter described by this
     * {@code OpenMBeanParameterInfo} instance,
     * {@code false} otherwise.
     */
    public boolean isValue(Object obj) ;


    /**
     * Compares the specified <var>obj</var> parameter with this {@code OpenMBeanParameterInfo} instance for equality.
     * <p>
     * Returns {@code true} if and only if all of the following statements are true:
     * <ul>
     * <li><var>obj</var> is non null,</li>
     * <li><var>obj</var> also implements the {@code OpenMBeanParameterInfo} interface,</li>
     * <li>their names are equal</li>
     * <li>their open types are equal</li>
     * <li>their default, min, max and legal values are equal.</li>
     * </ul>
     * This ensures that this {@code equals} method works properly for <var>obj</var> parameters which are
     * different implementations of the {@code OpenMBeanParameterInfo} interface.
     * <br>&nbsp;
     * @param  obj  the object to be compared for equality with this {@code OpenMBeanParameterInfo} instance;
     *
     * @return  {@code true} if the specified object is equal to this {@code OpenMBeanParameterInfo} instance.
     */
    public boolean equals(Object obj);

    /**
     * Returns the hash code value for this {@code OpenMBeanParameterInfo} instance.
     * <p>
     * The hash code of an {@code OpenMBeanParameterInfo} instance is the sum of the hash codes
     * of all elements of information used in {@code equals} comparisons
     * (ie: its name, its <i>open type</i>, and its default, min, max and legal values).
     * <p>
     * This ensures that {@code t1.equals(t2)} implies that {@code t1.hashCode()==t2.hashCode()}
     * for any two {@code OpenMBeanParameterInfo} instances {@code t1} and {@code t2},
     * as required by the general contract of the method
     * {@link Object#hashCode() Object.hashCode()}.
     *
     * @return  the hash code value for this {@code OpenMBeanParameterInfo} instance
     */
    public int hashCode();

    /**
     * Returns a string representation of this {@code OpenMBeanParameterInfo} instance.
     * <p>
     * The string representation consists of the name of this class (ie {@code javax.management.openmbean.OpenMBeanParameterInfo}),
     * the string representation of the name and open type of the described parameter,
     * and the string representation of its default, min, max and legal values.
     *
     * @return  a string representation of this {@code OpenMBeanParameterInfo} instance
     */
    public String toString();

}
