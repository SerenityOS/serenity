/*
 * Copyright (c) 2000, 2008, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;

import javax.management.Descriptor;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanParameterInfo;


/**
 * Describes a constructor of an Open MBean.
 *
 *
 * @since 1.5
 */
public class OpenMBeanConstructorInfoSupport
    extends MBeanConstructorInfo
    implements OpenMBeanConstructorInfo {

    /* Serial version */
    static final long serialVersionUID = -4400441579007477003L;


    // As this instance is immutable,
    // these two values need only be calculated once.
    private transient Integer myHashCode = null;
    private transient String  myToString = null;

    /**
     * <p>Constructs an {@code OpenMBeanConstructorInfoSupport}
     * instance, which describes the constructor of a class of open
     * MBeans with the specified {@code name}, {@code description} and
     * {@code signature}.</p>
     *
     * <p>The {@code signature} array parameter is internally copied,
     * so that subsequent changes to the array referenced by {@code
     * signature} have no effect on this instance.</p>
     *
     * @param name cannot be a null or empty string.
     *
     * @param description cannot be a null or empty string.
     *
     * @param signature can be null or empty if there are no
     * parameters to describe.
     *
     * @throws IllegalArgumentException if {@code name} or {@code
     * description} are null or empty string.
     *
     * @throws ArrayStoreException If {@code signature} is not an
     * array of instances of a subclass of {@code MBeanParameterInfo}.
     */
    public OpenMBeanConstructorInfoSupport(String name,
                                           String description,
                                           OpenMBeanParameterInfo[] signature) {
        this(name, description, signature, (Descriptor) null);
    }

    /**
     * <p>Constructs an {@code OpenMBeanConstructorInfoSupport}
     * instance, which describes the constructor of a class of open
     * MBeans with the specified {@code name}, {@code description},
     * {@code signature}, and {@code descriptor}.</p>
     *
     * <p>The {@code signature} array parameter is internally copied,
     * so that subsequent changes to the array referenced by {@code
     * signature} have no effect on this instance.</p>
     *
     * @param name cannot be a null or empty string.
     *
     * @param description cannot be a null or empty string.
     *
     * @param signature can be null or empty if there are no
     * parameters to describe.
     *
     * @param descriptor The descriptor for the constructor.  This may
     * be null which is equivalent to an empty descriptor.
     *
     * @throws IllegalArgumentException if {@code name} or {@code
     * description} are null or empty string.
     *
     * @throws ArrayStoreException If {@code signature} is not an
     * array of instances of a subclass of {@code MBeanParameterInfo}.
     *
     * @since 1.6
     */
    public OpenMBeanConstructorInfoSupport(String name,
                                           String description,
                                           OpenMBeanParameterInfo[] signature,
                                           Descriptor descriptor) {
        super(name,
              description,
              arrayCopyCast(signature), // may throw an ArrayStoreException
              descriptor);

        // check parameters that should not be null or empty
        // (unfortunately it is not done in superclass :-( ! )
        //
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Argument name cannot be " +
                                               "null or empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Argument description cannot " +
                                               "be null or empty");
        }

    }

    private static MBeanParameterInfo[]
            arrayCopyCast(OpenMBeanParameterInfo[] src) {
        if (src == null)
            return null;

        MBeanParameterInfo[] dst = new MBeanParameterInfo[src.length];
        System.arraycopy(src, 0, dst, 0, src.length);
        // may throw an ArrayStoreException
        return dst;
    }


    /* ***  Commodity methods from java.lang.Object  *** */


    /**
     * <p>Compares the specified {@code obj} parameter with this
     * {@code OpenMBeanConstructorInfoSupport} instance for
     * equality.</p>
     *
     * <p>Returns {@code true} if and only if all of the following
     * statements are true:
     *
     * <ul>
     * <li>{@code obj} is non null,</li>
     * <li>{@code obj} also implements the {@code
     * OpenMBeanConstructorInfo} interface,</li>
     * <li>their names are equal</li>
     * <li>their signatures are equal.</li>
     * </ul>
     *
     * This ensures that this {@code equals} method works properly for
     * {@code obj} parameters which are different implementations of
     * the {@code OpenMBeanConstructorInfo} interface.
     *
     * @param obj the object to be compared for equality with this
     * {@code OpenMBeanConstructorInfoSupport} instance;
     *
     * @return {@code true} if the specified object is equal to this
     * {@code OpenMBeanConstructorInfoSupport} instance.
     */
    public boolean equals(Object obj) {

        // if obj is null, return false
        //
        if (obj == null) {
            return false;
        }

        // if obj is not a OpenMBeanConstructorInfo, return false
        //
        OpenMBeanConstructorInfo other;
        try {
            other = (OpenMBeanConstructorInfo) obj;
        } catch (ClassCastException e) {
            return false;
        }

        // Now, really test for equality between this
        // OpenMBeanConstructorInfo implementation and the other:
        //

        // their Name should be equal
        if ( ! this.getName().equals(other.getName()) ) {
            return false;
        }

        // their Signatures should be equal
        if ( ! Arrays.equals(this.getSignature(), other.getSignature()) ) {
            return false;
        }

        // All tests for equality were successfull
        //
        return true;
    }

    /**
     * <p>Returns the hash code value for this {@code
     * OpenMBeanConstructorInfoSupport} instance.</p>
     *
     * <p>The hash code of an {@code OpenMBeanConstructorInfoSupport}
     * instance is the sum of the hash codes of all elements of
     * information used in {@code equals} comparisons (ie: its name
     * and signature, where the signature hashCode is calculated by a
     * call to {@code
     * java.util.Arrays.asList(this.getSignature).hashCode()}).</p>
     *
     * <p>This ensures that {@code t1.equals(t2)} implies that {@code
     * t1.hashCode()==t2.hashCode()} for any two {@code
     * OpenMBeanConstructorInfoSupport} instances {@code t1} and
     * {@code t2}, as required by the general contract of the method
     * {@link Object#hashCode() Object.hashCode()}.</p>
     *
     * <p>However, note that another instance of a class implementing
     * the {@code OpenMBeanConstructorInfo} interface may be equal to
     * this {@code OpenMBeanConstructorInfoSupport} instance as
     * defined by {@link #equals(java.lang.Object)}, but may have a
     * different hash code if it is calculated differently.</p>
     *
     * <p>As {@code OpenMBeanConstructorInfoSupport} instances are
     * immutable, the hash code for this instance is calculated once,
     * on the first call to {@code hashCode}, and then the same value
     * is returned for subsequent calls.</p>
     *
     * @return the hash code value for this {@code
     * OpenMBeanConstructorInfoSupport} instance
     */
    public int hashCode() {

        // Calculate the hash code value if it has not yet been done
        // (ie 1st call to hashCode())
        //
        if (myHashCode == null) {
            int value = 0;
            value += this.getName().hashCode();
            value += Arrays.asList(this.getSignature()).hashCode();
            myHashCode = Integer.valueOf(value);
        }

        // return always the same hash code for this instance (immutable)
        //
        return myHashCode.intValue();
    }

    /**
     * <p>Returns a string representation of this {@code
     * OpenMBeanConstructorInfoSupport} instance.</p>
     *
     * <p>The string representation consists of the name of this class
     * (ie {@code
     * javax.management.openmbean.OpenMBeanConstructorInfoSupport}),
     * the name and signature of the described constructor and the
     * string representation of its descriptor.</p>
     *
     * <p>As {@code OpenMBeanConstructorInfoSupport} instances are
     * immutable, the string representation for this instance is
     * calculated once, on the first call to {@code toString}, and
     * then the same value is returned for subsequent calls.</p>
     *
     * @return a string representation of this {@code
     * OpenMBeanConstructorInfoSupport} instance
     */
    public String toString() {

        // Calculate the string value if it has not yet been done (ie
        // 1st call to toString())
        //
        if (myToString == null) {
            myToString = new StringBuilder()
                .append(this.getClass().getName())
                .append("(name=")
                .append(this.getName())
                .append(",signature=")
                .append(Arrays.asList(this.getSignature()).toString())
                .append(",descriptor=")
                .append(this.getDescriptor())
                .append(")")
                .toString();
        }

        // return always the same string representation for this
        // instance (immutable)
        //
        return myToString;
    }

}
