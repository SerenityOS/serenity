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
 * <p>Describes a constructor of an Open MBean.</p>
 *
 * <p>This interface declares the same methods as the class {@link
 * javax.management.MBeanConstructorInfo}.  A class implementing this
 * interface (typically {@link OpenMBeanConstructorInfoSupport})
 * should extend {@link javax.management.MBeanConstructorInfo}.</p>
 *
 * <p>The {@link #getSignature()} method should return at runtime an
 * array of instances of a subclass of {@link MBeanParameterInfo}
 * which implements the {@link OpenMBeanParameterInfo} interface
 * (typically {@link OpenMBeanParameterInfoSupport}).</p>
 *
 *
 *
 * @since 1.5
 */
public interface OpenMBeanConstructorInfo {

    // Re-declares the methods that are in class MBeanConstructorInfo of JMX 1.0
    // (methods will be removed when MBeanConstructorInfo is made a parent interface of this interface)

    /**
     * Returns a human readable description of the constructor
     * described by this {@code OpenMBeanConstructorInfo} instance.
     *
     * @return the description.
     */
    public String getDescription() ;

    /**
     * Returns the name of the constructor
     * described by this {@code OpenMBeanConstructorInfo} instance.
     *
     * @return the name.
     */
    public String getName() ;

    /**
     * Returns an array of {@code OpenMBeanParameterInfo} instances
     * describing each parameter in the signature of the constructor
     * described by this {@code OpenMBeanConstructorInfo} instance.
     *
     * @return the signature.
     */
    public MBeanParameterInfo[] getSignature() ;


    // commodity methods
    //

    /**
     * Compares the specified <var>obj</var> parameter with this {@code OpenMBeanConstructorInfo} instance for equality.
     * <p>
     * Returns {@code true} if and only if all of the following statements are true:
     * <ul>
     * <li><var>obj</var> is non null,</li>
     * <li><var>obj</var> also implements the {@code OpenMBeanConstructorInfo} interface,</li>
     * <li>their names are equal</li>
     * <li>their signatures are equal.</li>
     * </ul>
     * This ensures that this {@code equals} method works properly for <var>obj</var> parameters which are
     * different implementations of the {@code OpenMBeanConstructorInfo} interface.
     * <br>&nbsp;
     * @param  obj  the object to be compared for equality with this {@code OpenMBeanConstructorInfo} instance;
     *
     * @return  {@code true} if the specified object is equal to this {@code OpenMBeanConstructorInfo} instance.
     */
    public boolean equals(Object obj);

    /**
     * Returns the hash code value for this {@code OpenMBeanConstructorInfo} instance.
     * <p>
     * The hash code of an {@code OpenMBeanConstructorInfo} instance is the sum of the hash codes
     * of all elements of information used in {@code equals} comparisons
     * (ie: its name and signature, where the signature hashCode is calculated by a call to
     *  {@code java.util.Arrays.asList(this.getSignature).hashCode()}).
     * <p>
     * This ensures that {@code t1.equals(t2)} implies that {@code t1.hashCode()==t2.hashCode()}
     * for any two {@code OpenMBeanConstructorInfo} instances {@code t1} and {@code t2},
     * as required by the general contract of the method
     * {@link Object#hashCode() Object.hashCode()}.
     *
     * @return  the hash code value for this {@code OpenMBeanConstructorInfo} instance
     */
    public int hashCode();

    /**
     * Returns a string representation of this {@code OpenMBeanConstructorInfo} instance.
     * <p>
     * The string representation consists of the name of this class
     * (ie {@code javax.management.openmbean.OpenMBeanConstructorInfo}),
     * and the name and signature of the described constructor.
     *
     * @return  a string representation of this {@code OpenMBeanConstructorInfo} instance
     */
    public String toString();

}
