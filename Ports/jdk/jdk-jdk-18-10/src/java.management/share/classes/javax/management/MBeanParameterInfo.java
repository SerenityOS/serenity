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

import java.util.Objects;


/**
 * Describes an argument of an operation exposed by an MBean.
 * Instances of this class are immutable.  Subclasses may be mutable
 * but this is not recommended.
 *
 * @since 1.5
 */
public class MBeanParameterInfo extends MBeanFeatureInfo implements Cloneable {

    /* Serial version */
    static final long serialVersionUID = 7432616882776782338L;

    /* All zero-length arrays are interchangeable. */
    static final MBeanParameterInfo[] NO_PARAMS = new MBeanParameterInfo[0];

    /**
     * @serial The type or class name of the data.
     */
    private final String type;


    /**
     * Constructs an {@code MBeanParameterInfo} object.
     *
     * @param name The name of the data
     * @param type The type or class name of the data
     * @param description A human readable description of the data. Optional.
     */
    public MBeanParameterInfo(String name,
                              String type,
                              String description) {
        this(name, type, description, (Descriptor) null);
    }

    /**
     * Constructs an {@code MBeanParameterInfo} object.
     *
     * @param name The name of the data
     * @param type The type or class name of the data
     * @param description A human readable description of the data. Optional.
     * @param descriptor The descriptor for the operation.  This may be null
     * which is equivalent to an empty descriptor.
     *
     * @since 1.6
     */
    public MBeanParameterInfo(String name,
                              String type,
                              String description,
                              Descriptor descriptor) {
        super(name, description, descriptor);

        this.type = type;
    }


    /**
     * <p>Returns a shallow clone of this instance.
     * The clone is obtained by simply calling {@code super.clone()},
     * thus calling the default native shallow cloning mechanism
     * implemented by {@code Object.clone()}.
     * No deeper cloning of any internal field is made.</p>
     *
     * <p>Since this class is immutable, cloning is chiefly of
     * interest to subclasses.</p>
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
     * Returns the type or class name of the data.
     *
     * @return the type string.
     */
    public String getType() {
        return type;
    }

    public String toString() {
        return
            getClass().getName() + "[" +
            "description=" + getDescription() + ", " +
            "name=" + getName() + ", " +
            "type=" + getType() + ", " +
            "descriptor=" + getDescriptor() +
            "]";
    }

    /**
     * Compare this MBeanParameterInfo to another.
     *
     * @param o the object to compare to.
     *
     * @return true if and only if {@code o} is an MBeanParameterInfo such
     * that its {@link #getName()}, {@link #getType()},
     * {@link #getDescriptor()}, and {@link
     * #getDescription()} values are equal (not necessarily identical)
     * to those of this MBeanParameterInfo.
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MBeanParameterInfo))
            return false;
        MBeanParameterInfo p = (MBeanParameterInfo) o;
        return (Objects.equals(p.getName(), getName()) &&
                Objects.equals(p.getType(), getType()) &&
                Objects.equals(p.getDescription(), getDescription()) &&
                Objects.equals(p.getDescriptor(), getDescriptor()));
    }

    public int hashCode() {
        return Objects.hash(getName(), getType());
    }
}
