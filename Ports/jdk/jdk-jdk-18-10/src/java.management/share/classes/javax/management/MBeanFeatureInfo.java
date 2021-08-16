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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.Objects;

/**
 * <p>Provides general information for an MBean descriptor object.
 * The feature described can be an attribute, an operation, a
 * parameter, or a notification.  Instances of this class are
 * immutable.  Subclasses may be mutable but this is not
 * recommended.</p>
 *
 * @since 1.5
 */
public class MBeanFeatureInfo implements Serializable, DescriptorRead {


    /* Serial version */
    static final long serialVersionUID = 3952882688968447265L;

    /**
     * The name of the feature.  It is recommended that subclasses call
     * {@link #getName} rather than reading this field, and that they
     * not change it.
     *
     * @serial The name of the feature.
     */
    protected String name;

    /**
     * The human-readable description of the feature.  It is
     * recommended that subclasses call {@link #getDescription} rather
     * than reading this field, and that they not change it.
     *
     * @serial The human-readable description of the feature.
     */
    protected String description;

    /**
     * @serial The Descriptor for this MBeanFeatureInfo.  This field
     * can be null, which is equivalent to an empty Descriptor.
     */
    private transient Descriptor descriptor;


    /**
     * Constructs an <CODE>MBeanFeatureInfo</CODE> object.  This
     * constructor is equivalent to {@code MBeanFeatureInfo(name,
     * description, (Descriptor) null}.
     *
     * @param name The name of the feature.
     * @param description A human readable description of the feature.
     */
    public MBeanFeatureInfo(String name, String description) {
        this(name, description, null);
    }

    /**
     * Constructs an <CODE>MBeanFeatureInfo</CODE> object.
     *
     * @param name The name of the feature.
     * @param description A human readable description of the feature.
     * @param descriptor The descriptor for the feature.  This may be null
     * which is equivalent to an empty descriptor.
     *
     * @since 1.6
     */
    public MBeanFeatureInfo(String name, String description,
                            Descriptor descriptor) {
        this.name = name;
        this.description = description;
        this.descriptor = descriptor;
    }

    /**
     * Returns the name of the feature.
     *
     * @return the name of the feature.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the human-readable description of the feature.
     *
     * @return the human-readable description of the feature.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the descriptor for the feature.  Changing the returned value
     * will have no affect on the original descriptor.
     *
     * @return a descriptor that is either immutable or a copy of the original.
     *
     * @since 1.6
     */
    public Descriptor getDescriptor() {
        return (Descriptor) ImmutableDescriptor.nonNullDescriptor(descriptor).clone();
    }

    /**
     * Compare this MBeanFeatureInfo to another.
     *
     * @param o the object to compare to.
     *
     * @return true if and only if <code>o</code> is an MBeanFeatureInfo such
     * that its {@link #getName()}, {@link #getDescription()}, and
     * {@link #getDescriptor()}
     * values are equal (not necessarily identical) to those of this
     * MBeanFeatureInfo.
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MBeanFeatureInfo))
            return false;
        MBeanFeatureInfo p = (MBeanFeatureInfo) o;
        return (Objects.equals(p.getName(), getName()) &&
                Objects.equals(p.getDescription(), getDescription()) &&
                Objects.equals(p.getDescriptor(), getDescriptor()));
    }

    public int hashCode() {
        return getName().hashCode() ^ getDescription().hashCode() ^
               getDescriptor().hashCode();
    }

    /**
     * Serializes an {@link MBeanFeatureInfo} to an {@link ObjectOutputStream}.
     * @serialData
     * For compatibility reasons, an object of this class is serialized as follows.
     * <p>
     * The method {@link ObjectOutputStream#defaultWriteObject defaultWriteObject()}
     * is called first to serialize the object except the field {@code descriptor}
     * which is declared as transient. The field {@code descriptor} is serialized
     * as follows:
     *     <ul>
     *     <li>If {@code descriptor} is an instance of the class
     *        {@link ImmutableDescriptor}, the method {@link ObjectOutputStream#write
     *        write(int val)} is called to write a byte with the value {@code 1},
     *        then the method {@link ObjectOutputStream#writeObject writeObject(Object obj)}
     *        is called twice to serialize the field names and the field values of the
     *        {@code descriptor}, respectively as a {@code String[]} and an
     *        {@code Object[]};</li>
     *     <li>Otherwise, the method {@link ObjectOutputStream#write write(int val)}
     * is called to write a byte with the value {@code 0}, then the method
     * {@link ObjectOutputStream#writeObject writeObject(Object obj)} is called
     * to serialize directly the field {@code descriptor}.
     *     </ul>
     *
     * @since 1.6
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        if (descriptor != null &&
            descriptor.getClass() == ImmutableDescriptor.class) {

            out.write(1);

            final String[] names = descriptor.getFieldNames();

            out.writeObject(names);
            out.writeObject(descriptor.getFieldValues(names));
        } else {
            out.write(0);

            out.writeObject(descriptor);
        }
    }

    /**
     * Deserializes an {@link MBeanFeatureInfo} from an {@link ObjectInputStream}.
     * @serialData
     * For compatibility reasons, an object of this class is deserialized as follows.
     * <p>
     * The method {@link ObjectInputStream#defaultReadObject defaultReadObject()}
     * is called first to deserialize the object except the field
     * {@code descriptor}, which is not serialized in the default way. Then the method
     * {@link ObjectInputStream#read read()} is called to read a byte, the field
     * {@code descriptor} is deserialized according to the value of the byte value:
     *    <ul>
     *    <li>1. The method {@link ObjectInputStream#readObject readObject()}
     *       is called twice to obtain the field names (a {@code String[]}) and
     *       the field values (an {@code Object[]}) of the {@code descriptor}.
     *       The two obtained values then are used to construct
     *       an {@link ImmutableDescriptor} instance for the field
     *       {@code descriptor};</li>
     *    <li>0. The value for the field {@code descriptor} is obtained directly
     *       by calling the method {@link ObjectInputStream#readObject readObject()}.
     *       If the obtained value is null, the field {@code descriptor} is set to
     *       {@link ImmutableDescriptor#EMPTY_DESCRIPTOR EMPTY_DESCRIPTOR};</li>
     *    <li>-1. This means that there is no byte to read and that the object is from
     *       an earlier version of the JMX API. The field {@code descriptor} is set
     *       to {@link ImmutableDescriptor#EMPTY_DESCRIPTOR EMPTY_DESCRIPTOR}</li>
     *    <li>Any other value. A {@link StreamCorruptedException} is thrown.</li>
     *    </ul>
     *
     * @since 1.6
     */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

        in.defaultReadObject();

        switch (in.read()) {
        case 1:
            final String[] names = (String[])in.readObject();

            final Object[] values = (Object[]) in.readObject();
            descriptor = (names.length == 0) ?
                ImmutableDescriptor.EMPTY_DESCRIPTOR :
                new ImmutableDescriptor(names, values);

            break;
        case 0:
            descriptor = (Descriptor)in.readObject();

            if (descriptor == null) {
                descriptor = ImmutableDescriptor.EMPTY_DESCRIPTOR;
            }

            break;
        case -1: // from an earlier version of the JMX API
            descriptor = ImmutableDescriptor.EMPTY_DESCRIPTOR;

            break;
        default:
            throw new StreamCorruptedException("Got unexpected byte.");
        }
    }
}
