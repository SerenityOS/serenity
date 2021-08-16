/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jmx.mbeanserver.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * Describes a management operation exposed by an MBean.  Instances of
 * this class are immutable.  Subclasses may be mutable but this is
 * not recommended.
 *
 * @since 1.5
 */
public class MBeanOperationInfo extends MBeanFeatureInfo implements Cloneable {

    /* Serial version */
    static final long serialVersionUID = -6178860474881375330L;

    static final MBeanOperationInfo[] NO_OPERATIONS =
        new MBeanOperationInfo[0];

    /**
     * Indicates that the operation is read-like:
     * it returns information but does not change any state.
     */
    public static final int INFO = 0;

    /**
     * Indicates that the operation is write-like: it has an effect but does
     * not return any information from the MBean.
     */
    public static final int ACTION = 1;

    /**
     * Indicates that the operation is both read-like and write-like:
     * it has an effect, and it also returns information from the MBean.
     */
    public static final int ACTION_INFO = 2;

    /**
     * Indicates that the impact of the operation is unknown or cannot be
     * expressed using one of the other values.
     */
    public static final int UNKNOWN = 3;

    /**
     * @serial The method's return value.
     */
    private final String type;

    /**
     * @serial The signature of the method, that is, the class names
     * of the arguments.
     */
    private final MBeanParameterInfo[] signature;

    /**
     * @serial The impact of the method, one of
     *         {@code INFO, ACTION, ACTION_INFO, UNKNOWN}.
     */
    private final int impact;

    /** @see MBeanInfo#arrayGettersSafe */
    private final transient boolean arrayGettersSafe;


    /**
     * Constructs an {@code MBeanOperationInfo} object.  The
     * {@link Descriptor} of the constructed object will include
     * fields contributed by any annotations on the {@code Method}
     * object that contain the {@link DescriptorKey} meta-annotation.
     *
     * @param method The {@code java.lang.reflect.Method} object
     * describing the MBean operation.
     * @param description A human readable description of the operation.
     */
    public MBeanOperationInfo(String description, Method method) {
        this(method.getName(),
             description,
             methodSignature(method),
             method.getReturnType().getName(),
             UNKNOWN,
             Introspector.descriptorForElement(method));
    }

    /**
     * Constructs an {@code MBeanOperationInfo} object.
     *
     * @param name The name of the method.
     * @param description A human readable description of the operation.
     * @param signature {@code MBeanParameterInfo} objects
     * describing the parameters(arguments) of the method.  This may be
     * null with the same effect as a zero-length array.
     * @param type The type of the method's return value.
     * @param impact The impact of the method, one of
     * {@link #INFO}, {@link #ACTION}, {@link #ACTION_INFO},
     * {@link #UNKNOWN}.
     */
    public MBeanOperationInfo(String name,
                              String description,
                              MBeanParameterInfo[] signature,
                              String type,
                              int impact) {
        this(name, description, signature, type, impact, (Descriptor) null);
    }

    /**
     * Constructs an {@code MBeanOperationInfo} object.
     *
     * @param name The name of the method.
     * @param description A human readable description of the operation.
     * @param signature {@code MBeanParameterInfo} objects
     * describing the parameters(arguments) of the method.  This may be
     * null with the same effect as a zero-length array.
     * @param type The type of the method's return value.
     * @param impact The impact of the method, one of
     * {@link #INFO}, {@link #ACTION}, {@link #ACTION_INFO},
     * {@link #UNKNOWN}.
     * @param descriptor The descriptor for the operation.  This may be null
     * which is equivalent to an empty descriptor.
     *
     * @throws IllegalArgumentException if {@code impact} is not one of
     * {@linkplain #ACTION}, {@linkplain #ACTION_INFO}, {@linkplain #INFO} or {@linkplain #UNKNOWN}.
     *
     * @since 1.6
     */
    public MBeanOperationInfo(String name,
                              String description,
                              MBeanParameterInfo[] signature,
                              String type,
                              int impact,
                              Descriptor descriptor) {

        super(name, description, descriptor);

        if (impact < INFO || impact > UNKNOWN) {
            throw new IllegalArgumentException("Argument impact can only be "
                    + "one of ACTION, ACTION_INFO, "
                    + "INFO, or UNKNOWN" + " given value is :" + impact);
        }

        if (signature == null || signature.length == 0)
            signature = MBeanParameterInfo.NO_PARAMS;
        else
            signature = signature.clone();
        this.signature = signature;
        this.type = type;
        this.impact = impact;
        this.arrayGettersSafe =
            MBeanInfo.arrayGettersSafe(this.getClass(),
                                       MBeanOperationInfo.class);
    }

    /**
     * <p>Returns a shallow clone of this instance.
     * The clone is obtained by simply calling {@code super.clone()},
     * thus calling the default native shallow cloning mechanism
     * implemented by {@code Object.clone()}.
     * No deeper cloning of any internal field is made.</p>
     *
     * <p>Since this class is immutable, cloning is chiefly of interest
     * to subclasses.</p>
     */
     @Override
     public Object clone () {
         try {
             return super.clone() ;
         } catch (CloneNotSupportedException e) {
             // should not happen as this class is cloneable
             return null;
         }
     }

    /**
     * Returns the type of the method's return value.
     *
     * @return the return type.
     */
    public String getReturnType() {
        return type;
    }

    /**
     * <p>Returns the list of parameters for this operation.  Each
     * parameter is described by an {@code MBeanParameterInfo}
     * object.</p>
     *
     * <p>The returned array is a shallow copy of the internal array,
     * which means that it is a copy of the internal array of
     * references to the {@code MBeanParameterInfo} objects but
     * that each referenced {@code MBeanParameterInfo} object is
     * not copied.</p>
     *
     * @return  An array of {@code MBeanParameterInfo} objects.
     */
    public MBeanParameterInfo[] getSignature() {
        // If MBeanOperationInfo was created in our implementation,
        // signature cannot be null - because our constructors replace
        // null with MBeanParameterInfo.NO_PARAMS;
        //
        // However, signature could be null if an  MBeanOperationInfo is
        // deserialized from a byte array produced by another implementation.
        // This is not very likely but possible, since the serial form says
        // nothing against it. (see 6373150)
        //
        if (signature == null)
            // if signature is null simply return an empty array .
            //
            return MBeanParameterInfo.NO_PARAMS;
        else if (signature.length == 0)
            return signature;
        else
            return signature.clone();
    }

    private MBeanParameterInfo[] fastGetSignature() {
        if (arrayGettersSafe) {
            // if signature is null simply return an empty array .
            // see getSignature() above.
            //
            if (signature == null)
                return MBeanParameterInfo.NO_PARAMS;
            else return signature;
        } else return getSignature();
    }

    /**
     * Returns the impact of the method, one of
     * {@code INFO, ACTION, ACTION_INFO, UNKNOWN}.
     *
     * @return the impact code.
     */
    public int getImpact() {
        return impact;
    }

    @Override
    public String toString() {
        String impactString;
        switch (getImpact()) {
        case ACTION: impactString = "action"; break;
        case ACTION_INFO: impactString = "action/info"; break;
        case INFO: impactString = "info"; break;
        default: impactString = "unknown";
        }
        return getClass().getName() + "[" +
            "description=" + getDescription() + ", " +
            "name=" + getName() + ", " +
            "returnType=" + getReturnType() + ", " +
            "signature=" + Arrays.asList(fastGetSignature()) + ", " +
            "impact=" + impactString + ", " +
            "descriptor=" + getDescriptor() +
            "]";
    }

    /**
     * Compare this MBeanOperationInfo to another.
     *
     * @param o the object to compare to.
     *
     * @return true if and only if {@code o} is an MBeanOperationInfo such
     * that its {@link #getName()}, {@link #getReturnType()}, {@link
     * #getDescription()}, {@link #getImpact()}, {@link #getDescriptor()}
     * and {@link #getSignature()} values are equal (not necessarily identical)
     * to those of this MBeanConstructorInfo.  Two signature arrays
     * are equal if their elements are pairwise equal.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MBeanOperationInfo))
            return false;
        MBeanOperationInfo p = (MBeanOperationInfo) o;
        return (Objects.equals(p.getName(), getName()) &&
                Objects.equals(p.getReturnType(), getReturnType()) &&
                Objects.equals(p.getDescription(), getDescription()) &&
                p.getImpact() == getImpact() &&
                Arrays.equals(p.fastGetSignature(), fastGetSignature()) &&
                Objects.equals(p.getDescriptor(), getDescriptor()));
    }

    /* We do not include everything in the hashcode.  We assume that
       if two operations are different they'll probably have different
       names or types.  The penalty we pay when this assumption is
       wrong should be less than the penalty we would pay if it were
       right and we needlessly hashed in the description and the
       parameter array.  */
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getReturnType());
    }

    private static MBeanParameterInfo[] methodSignature(Method method) {
        final Class<?>[] classes = method.getParameterTypes();
        final Annotation[][] annots = method.getParameterAnnotations();
        return parameters(classes, annots);
    }

    static MBeanParameterInfo[] parameters(Class<?>[] classes,
                                           Annotation[][] annots) {
        final MBeanParameterInfo[] params =
            new MBeanParameterInfo[classes.length];
        assert(classes.length == annots.length);

        for (int i = 0; i < classes.length; i++) {
            Descriptor d = Introspector.descriptorForAnnotations(annots[i]);
            final String pn = "p" + (i + 1);
            params[i] =
                new MBeanParameterInfo(pn, classes[i].getName(), "", d);
        }

        return params;
    }
}
