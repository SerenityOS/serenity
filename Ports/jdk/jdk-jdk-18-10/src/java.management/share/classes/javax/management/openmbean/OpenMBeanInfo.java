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
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanNotificationInfo;



/**
 * <p>Describes an Open MBean: an Open MBean is recognized as such if
 * its {@link javax.management.DynamicMBean#getMBeanInfo()
 * getMBeanInfo()} method returns an instance of a class which
 * implements the {@link OpenMBeanInfo} interface, typically {@link
 * OpenMBeanInfoSupport}.</p>
 *
 * <p>This interface declares the same methods as the class {@link
 * javax.management.MBeanInfo}.  A class implementing this interface
 * (typically {@link OpenMBeanInfoSupport}) should extend {@link
 * javax.management.MBeanInfo}.</p>
 *
 * <p>The {@link #getAttributes()}, {@link #getOperations()} and
 * {@link #getConstructors()} methods of the implementing class should
 * return at runtime an array of instances of a subclass of {@link
 * MBeanAttributeInfo}, {@link MBeanOperationInfo} or {@link
 * MBeanConstructorInfo} respectively which implement the {@link
 * OpenMBeanAttributeInfo}, {@link OpenMBeanOperationInfo} or {@link
 * OpenMBeanConstructorInfo} interface respectively.
 *
 *
 * @since 1.5
 */
public interface OpenMBeanInfo {

    // Re-declares the methods that are in class MBeanInfo of JMX 1.0
    // (methods will be removed when MBeanInfo is made a parent interface of this interface)

    /**
     * Returns the fully qualified Java class name of the open MBean
     * instances this {@code OpenMBeanInfo} describes.
     *
     * @return the class name.
     */
    public String getClassName() ;

    /**
     * Returns a human readable description of the type of open MBean
     * instances this {@code OpenMBeanInfo} describes.
     *
     * @return the description.
     */
    public String getDescription() ;

    /**
     * Returns an array of {@code OpenMBeanAttributeInfo} instances
     * describing each attribute in the open MBean described by this
     * {@code OpenMBeanInfo} instance.  Each instance in the returned
     * array should actually be a subclass of
     * {@code MBeanAttributeInfo} which implements the
     * {@code OpenMBeanAttributeInfo} interface (typically {@link
     * OpenMBeanAttributeInfoSupport}).
     *
     * @return the attribute array.
     */
    public MBeanAttributeInfo[] getAttributes() ;

    /**
     * Returns an array of {@code OpenMBeanOperationInfo} instances
     * describing each operation in the open MBean described by this
     * {@code OpenMBeanInfo} instance.  Each instance in the returned
     * array should actually be a subclass of
     * {@code MBeanOperationInfo} which implements the
     * {@code OpenMBeanOperationInfo} interface (typically {@link
     * OpenMBeanOperationInfoSupport}).
     *
     * @return the operation array.
     */
    public MBeanOperationInfo[] getOperations() ;

    /**
     * Returns an array of {@code OpenMBeanConstructorInfo} instances
     * describing each constructor in the open MBean described by this
     * {@code OpenMBeanInfo} instance.  Each instance in the returned
     * array should actually be a subclass of
     * {@code MBeanConstructorInfo} which implements the
     * {@code OpenMBeanConstructorInfo} interface (typically {@link
     * OpenMBeanConstructorInfoSupport}).
     *
     * @return the constructor array.
     */
    public MBeanConstructorInfo[] getConstructors() ;

    /**
     * Returns an array of {@code MBeanNotificationInfo} instances
     * describing each notification emitted by the open MBean
     * described by this {@code OpenMBeanInfo} instance.
     *
     * @return the notification array.
     */
    public MBeanNotificationInfo[] getNotifications() ;


    // commodity methods
    //

    /**
     * Compares the specified <var>obj</var> parameter with this {@code OpenMBeanInfo} instance for equality.
     * <p>
     * Returns {@code true} if and only if all of the following statements are true:
     * <ul>
     * <li><var>obj</var> is non null,</li>
     * <li><var>obj</var> also implements the {@code OpenMBeanInfo} interface,</li>
     * <li>their class names are equal</li>
     * <li>their infos on attributes, constructors, operations and notifications are equal</li>
     * </ul>
     * This ensures that this {@code equals} method works properly for <var>obj</var> parameters which are
     * different implementations of the {@code OpenMBeanInfo} interface.
     *
     * @param  obj  the object to be compared for equality with this {@code OpenMBeanInfo} instance;
     *
     * @return  {@code true} if the specified object is equal to this {@code OpenMBeanInfo} instance.
     */
    public boolean equals(Object obj);

    /**
     * Returns the hash code value for this {@code OpenMBeanInfo} instance.
     * <p>
     * The hash code of an {@code OpenMBeanInfo} instance is the sum of the hash codes
     * of all elements of information used in {@code equals} comparisons
     * (ie: its class name, and its infos on attributes, constructors, operations and notifications,
     * where the hashCode of each of these arrays is calculated by a call to
     * {@code new java.util.HashSet(java.util.Arrays.asList(this.getSignature)).hashCode()}).
     * <p>
     * This ensures that {@code t1.equals(t2)} implies that {@code t1.hashCode()==t2.hashCode()}
     * for any two {@code OpenMBeanInfo} instances {@code t1} and {@code t2},
     * as required by the general contract of the method
     * {@link Object#hashCode() Object.hashCode()}.
     *
     * @return  the hash code value for this {@code OpenMBeanInfo} instance
     */
    public int hashCode();

    /**
     * Returns a string representation of this {@code OpenMBeanInfo} instance.
     * <p>
     * The string representation consists of the name of this class
     * (ie {@code javax.management.openmbean.OpenMBeanInfo}), the MBean class name,
     * and the string representation of infos on attributes, constructors,
     * operations and notifications of the described MBean.
     *
     * @return  a string representation of this {@code OpenMBeanInfo} instance
     */
    public String toString();

}
