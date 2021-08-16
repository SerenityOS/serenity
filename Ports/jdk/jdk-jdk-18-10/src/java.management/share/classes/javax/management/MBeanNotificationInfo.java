/*
 * Copyright (c) 1999, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javax.management;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Objects;

/**
 * <p>The {@code MBeanNotificationInfo} class is used to describe the
 * characteristics of the different notification instances
 * emitted by an MBean, for a given Java class of notification.
 * If an MBean emits notifications that can be instances of different Java classes,
 * then the metadata for that MBean should provide an {@code MBeanNotificationInfo}
 * object for each of these notification Java classes.</p>
 *
 * <p>Instances of this class are immutable.  Subclasses may be
 * mutable but this is not recommended.</p>
 *
 * <p>This class extends {@code javax.management.MBeanFeatureInfo}
 * and thus provides {@code name} and {@code description} fields.
 * The {@code name} field should be the fully qualified Java class name of
 * the notification objects described by this class.</p>
 *
 * <p>The {@code getNotifTypes} method returns an array of
 * strings containing the notification types that the MBean may
 * emit. The notification type is a dot-notation string which
 * describes what the emitted notification is about, not the Java
 * class of the notification.  A single generic notification class can
 * be used to send notifications of several types.  All of these types
 * are returned in the string array result of the
 * {@code getNotifTypes} method.
 *
 * @since 1.5
 */
public class MBeanNotificationInfo extends MBeanFeatureInfo implements Cloneable {

    /* Serial version */
    static final long serialVersionUID = -3888371564530107064L;

    private static final String[] NO_TYPES = new String[0];

    static final MBeanNotificationInfo[] NO_NOTIFICATIONS =
        new MBeanNotificationInfo[0];

    /**
     * @serial The different types of the notification.
     */
    private String[] types;

    /** @see MBeanInfo#arrayGettersSafe */
    private final transient boolean arrayGettersSafe;

    /**
     * Constructs an {@code MBeanNotificationInfo} object.
     *
     * @param notifTypes The array of strings (in dot notation)
     * containing the notification types that the MBean may emit.
     * This may be null with the same effect as a zero-length array.
     * @param name The fully qualified Java class name of the
     * described notifications.
     * @param description A human readable description of the data.
     */
    public MBeanNotificationInfo(String[] notifTypes,
                                 String name,
                                 String description) {
        this(notifTypes, name, description, null);
    }

    /**
     * Constructs an {@code MBeanNotificationInfo} object.
     *
     * @param notifTypes The array of strings (in dot notation)
     * containing the notification types that the MBean may emit.
     * This may be null with the same effect as a zero-length array.
     * @param name The fully qualified Java class name of the
     * described notifications.
     * @param description A human readable description of the data.
     * @param descriptor The descriptor for the notifications.  This may be null
     * which is equivalent to an empty descriptor.
     *
     * @since 1.6
     */
    public MBeanNotificationInfo(String[] notifTypes,
                                 String name,
                                 String description,
                                 Descriptor descriptor) {
        super(name, description, descriptor);

        /* We do not validate the notifTypes, since the spec just says
           they are dot-separated, not that they must look like Java
           classes.  E.g. the spec doesn't forbid "sun.prob.25" as a
           notifType, though it doesn't explicitly allow it
           either.  */

        this.types = (notifTypes != null && notifTypes.length > 0) ?
                        notifTypes.clone() : NO_TYPES;
        this.arrayGettersSafe =
            MBeanInfo.arrayGettersSafe(this.getClass(),
                                       MBeanNotificationInfo.class);
    }


    /**
     * Returns a shallow clone of this instance.
     * The clone is obtained by simply calling {@code super.clone()},
     * thus calling the default native shallow cloning mechanism
     * implemented by {@code Object.clone()}.
     * No deeper cloning of any internal field is made.
     */
     public Object clone () {
         try {
             return super.clone() ;
         } catch (CloneNotSupportedException e) {
             // should not happen as this class is cloneable
             return null;
         }
     }


    /**
     * Returns the array of strings (in dot notation) containing the
     * notification types that the MBean may emit.
     *
     * @return the array of strings.  Changing the returned array has no
     * effect on this MBeanNotificationInfo.
     */
    public String[] getNotifTypes() {
        if (types.length == 0)
            return NO_TYPES;
        else
            return types.clone();
    }

    private String[] fastGetNotifTypes() {
        if (arrayGettersSafe)
            return types;
        else
            return getNotifTypes();
    }

    public String toString() {
        return
            getClass().getName() + "[" +
            "description=" + getDescription() + ", " +
            "name=" + getName() + ", " +
            "notifTypes=" + Arrays.asList(fastGetNotifTypes()) + ", " +
            "descriptor=" + getDescriptor() +
            "]";
    }

    /**
     * Compare this MBeanNotificationInfo to another.
     *
     * @param o the object to compare to.
     *
     * @return true if and only if {@code o} is an MBeanNotificationInfo
     * such that its {@link #getName()}, {@link #getDescription()},
     * {@link #getDescriptor()},
     * and {@link #getNotifTypes()} values are equal (not necessarily
     * identical) to those of this MBeanNotificationInfo.  Two
     * notification type arrays are equal if their corresponding
     * elements are equal.  They are not equal if they have the same
     * elements but in a different order.
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MBeanNotificationInfo))
            return false;
        MBeanNotificationInfo p = (MBeanNotificationInfo) o;
        return (Objects.equals(p.getName(), getName()) &&
                Objects.equals(p.getDescription(), getDescription()) &&
                Objects.equals(p.getDescriptor(), getDescriptor()) &&
                Arrays.equals(p.fastGetNotifTypes(), fastGetNotifTypes()));
    }

    public int hashCode() {
        int hash = getName().hashCode();
        for (int i = 0; i < types.length; i++)
            hash ^= types[i].hashCode();
        return hash;
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField gf = ois.readFields();
        String[] t = (String[])gf.get("types", null);

        types = (t != null && t.length != 0) ? t.clone() : NO_TYPES;
    }
}
