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
import javax.management.MBeanParameterInfo;

/**
 * <p>Describes an operation of an Open MBean.</p>
 *
 * <p>This interface declares the same methods as the class {@link
 * javax.management.MBeanOperationInfo}.  A class implementing this
 * interface (typically {@link OpenMBeanOperationInfoSupport}) should
 * extend {@link javax.management.MBeanOperationInfo}.</p>
 *
 * <p>The {@link #getSignature()} method should return at runtime an
 * array of instances of a subclass of {@link MBeanParameterInfo}
 * which implements the {@link OpenMBeanParameterInfo} interface
 * (typically {@link OpenMBeanParameterInfoSupport}).</p>
 *
 *
 * @since 1.5
 */
public interface OpenMBeanOperationInfo  {

    // Re-declares fields and methods that are in class MBeanOperationInfo of JMX 1.0
    // (fields and methods will be removed when MBeanOperationInfo is made a parent interface of this interface)

    /**
     * Returns a human readable description of the operation
     * described by this {@code OpenMBeanOperationInfo} instance.
     *
     * @return the description.
     */
    public String getDescription() ;

    /**
     * Returns the name of the operation
     * described by this {@code OpenMBeanOperationInfo} instance.
     *
     * @return the name.
     */
    public String getName() ;

    /**
     * Returns an array of {@code OpenMBeanParameterInfo} instances
     * describing each parameter in the signature of the operation
     * described by this {@code OpenMBeanOperationInfo} instance.
     * Each instance in the returned array should actually be a
     * subclass of {@code MBeanParameterInfo} which implements the
     * {@code OpenMBeanParameterInfo} interface (typically {@link
     * OpenMBeanParameterInfoSupport}).
     *
     * @return the signature.
     */
    public MBeanParameterInfo[] getSignature() ;

    /**
     * Returns an {@code int} constant qualifying the impact of the
     * operation described by this {@code OpenMBeanOperationInfo}
     * instance.
     *
     * The returned constant is one of {@link
     * javax.management.MBeanOperationInfo#INFO}, {@link
     * javax.management.MBeanOperationInfo#ACTION}, {@link
     * javax.management.MBeanOperationInfo#ACTION_INFO}, or {@link
     * javax.management.MBeanOperationInfo#UNKNOWN}.
     *
     * @return the impact code.
     */
    public int getImpact() ;

    /**
     * Returns the fully qualified Java class name of the values
     * returned by the operation described by this
     * {@code OpenMBeanOperationInfo} instance.  This method should
     * return the same value as a call to
     * {@code getReturnOpenType().getClassName()}.
     *
     * @return the return type.
     */
    public String getReturnType() ;


    // Now declares methods that are specific to open MBeans
    //

    /**
     * Returns the <i>open type</i> of the values returned by the
     * operation described by this {@code OpenMBeanOperationInfo}
     * instance.
     *
     * @return the return type.
     */
    public OpenType<?> getReturnOpenType() ; // open MBean specific method


    // commodity methods
    //

    /**
     * Compares the specified <var>obj</var> parameter with this {@code OpenMBeanOperationInfo} instance for equality.
     * <p>
     * Returns {@code true} if and only if all of the following statements are true:
     * <ul>
     * <li><var>obj</var> is non null,</li>
     * <li><var>obj</var> also implements the {@code OpenMBeanOperationInfo} interface,</li>
     * <li>their names are equal</li>
     * <li>their signatures are equal</li>
     * <li>their return open types are equal</li>
     * <li>their impacts are equal</li>
     * </ul>
     * This ensures that this {@code equals} method works properly for <var>obj</var> parameters which are
     * different implementations of the {@code OpenMBeanOperationInfo} interface.
     * <br>&nbsp;
     * @param  obj  the object to be compared for equality with this {@code OpenMBeanOperationInfo} instance;
     *
     * @return  {@code true} if the specified object is equal to this {@code OpenMBeanOperationInfo} instance.
     */
    public boolean equals(Object obj);

    /**
     * Returns the hash code value for this {@code OpenMBeanOperationInfo} instance.
     * <p>
     * The hash code of an {@code OpenMBeanOperationInfo} instance is the sum of the hash codes
     * of all elements of information used in {@code equals} comparisons
     * (ie: its name, return open type, impact and signature,
     * where the signature hashCode is calculated by a call to
     * {@code java.util.Arrays.asList(this.getSignature).hashCode()}).
     * <p>
     * This ensures that {@code t1.equals(t2)} implies that {@code t1.hashCode()==t2.hashCode()}
     * for any two {@code OpenMBeanOperationInfo} instances {@code t1} and {@code t2},
     * as required by the general contract of the method
     * {@link Object#hashCode() Object.hashCode()}.
     *
     *
     * @return  the hash code value for this {@code OpenMBeanOperationInfo} instance
     */
    public int hashCode();

    /**
     * Returns a string representation of this {@code OpenMBeanOperationInfo} instance.
     * <p>
     * The string representation consists of the name of this class
     * (ie {@code javax.management.openmbean.OpenMBeanOperationInfo}),
     * and the name, signature, return open type and impact of the described operation.
     *
     * @return  a string representation of this {@code OpenMBeanOperationInfo} instance
     */
    public String toString();

}
