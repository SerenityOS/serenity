/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.rmi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import java.io.OutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;

import sun.rmi.server.MarshalInputStream;
import sun.rmi.server.MarshalOutputStream;

/**
 * A <code>MarshalledObject</code> contains a byte stream with the serialized
 * representation of an object given to its constructor.  The <code>get</code>
 * method returns a new copy of the original object, as deserialized from
 * the contained byte stream.  The contained object is serialized and
 * deserialized with the same serialization semantics used for marshaling
 * and unmarshaling parameters and return values of RMI calls:  When the
 * serialized form is created:
 *
 * <ul>
 * <li> classes are annotated with a codebase URL from where the class
 *      can be loaded (if available), and
 * <li> any remote object in the <code>MarshalledObject</code> is
 *      represented by a serialized instance of its stub.
 * </ul>
 *
 * <p>When copy of the object is retrieved (via the <code>get</code> method),
 * if the class is not available locally, it will be loaded from the
 * appropriate location (specified the URL annotated with the class descriptor
 * when the class was serialized.
 *
 * <p><code>MarshalledObject</code> facilitates passing objects in RMI calls
 * that are not automatically deserialized immediately by the remote peer.
 *
 * @param <T> the type of the object contained in this
 * <code>MarshalledObject</code>
 *
 * @author  Ann Wollrath
 * @author  Peter Jones
 * @since   1.2
 */
public final class MarshalledObject<T> implements Serializable {
    /**
     * @serial Bytes of serialized representation.  If <code>objBytes</code> is
     * <code>null</code> then the object marshalled was a <code>null</code>
     * reference.
     */
    private byte[] objBytes = null;

    /**
     * @serial Bytes of location annotations, which are ignored by
     * <code>equals</code>.  If <code>locBytes</code> is null, there were no
     * non-<code>null</code> annotations during marshalling.
     */
    private byte[] locBytes = null;

    /**
     * @serial Stored hash code of contained object.
     *
     * @see #hashCode
     */
    private int hash;

    /** Filter used when creating the instance from a stream; may be null. */
    private transient ObjectInputFilter objectInputFilter = null;

    /** Indicate compatibility with 1.2 version of class. */
    private static final long serialVersionUID = 8988374069173025854L;

    /**
     * Creates a new <code>MarshalledObject</code> that contains the
     * serialized representation of the current state of the supplied object.
     * The object is serialized with the semantics used for marshaling
     * parameters for RMI calls.
     *
     * @param obj the object to be serialized (must be serializable)
     * @throws IOException if an <code>IOException</code> occurs; an
     *         <code>IOException</code> may occur if <code>obj</code> is not
     *         serializable.
     * @since 1.2
     */
    public MarshalledObject(T obj) throws IOException {
        if (obj == null) {
            hash = 13;
            return;
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ByteArrayOutputStream lout = new ByteArrayOutputStream();
        MarshalledObjectOutputStream out =
            new MarshalledObjectOutputStream(bout, lout);
        out.writeObject(obj);
        out.flush();
        objBytes = bout.toByteArray();
        // locBytes is null if no annotations
        locBytes = (out.hadAnnotations() ? lout.toByteArray() : null);

        /*
         * Calculate hash from the marshalled representation of object
         * so the hashcode will be comparable when sent between VMs.
         */
        int h = 0;
        for (int i = 0; i < objBytes.length; i++) {
            h = 31 * h + objBytes[i];
        }
        hash = h;
    }

    /**
     * Reads in the state of the object and saves the stream's
     * serialization filter to be used when the object is deserialized.
     *
     * @param stream the stream
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a class cannot be found
     */
    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
        stream.defaultReadObject();     // read in all fields
        objectInputFilter = stream.getObjectInputFilter();
    }

    /**
     * Returns a new copy of the contained marshalledobject.  The internal
     * representation is deserialized with the semantics used for
     * unmarshaling parameters for RMI calls.
     * If the MarshalledObject was read from an ObjectInputStream,
     * the filter from that stream is used to deserialize the object.
     *
     * @return a copy of the contained object
     * @throws IOException if an <code>IOException</code> occurs while
     *         deserializing the object from its internal representation.
     * @throws ClassNotFoundException if a
     *         <code>ClassNotFoundException</code> occurs while deserializing
     *         the object from its internal representation.
     *         could not be found
     * @since 1.2
     */
    public T get() throws IOException, ClassNotFoundException {
        if (objBytes == null)   // must have been a null object
            return null;

        ByteArrayInputStream bin = new ByteArrayInputStream(objBytes);
        // locBytes is null if no annotations
        ByteArrayInputStream lin =
            (locBytes == null ? null : new ByteArrayInputStream(locBytes));
        MarshalledObjectInputStream in =
            new MarshalledObjectInputStream(bin, lin, objectInputFilter);
        @SuppressWarnings("unchecked")
        T obj = (T) in.readObject();
        in.close();
        return obj;
    }

    /**
     * Return a hash code for this <code>MarshalledObject</code>.
     *
     * @return a hash code
     */
    public int hashCode() {
        return hash;
    }

    /**
     * Compares this <code>MarshalledObject</code> to another object.
     * Returns true if and only if the argument refers to a
     * <code>MarshalledObject</code> that contains exactly the same
     * serialized representation of an object as this one does. The
     * comparison ignores any class codebase annotation, meaning that
     * two objects are equivalent if they have the same serialized
     * representation <i>except</i> for the codebase of each class
     * in the serialized representation.
     *
     * @param obj the object to compare with this <code>MarshalledObject</code>
     * @return <code>true</code> if the argument contains an equivalent
     * serialized object; <code>false</code> otherwise
     * @since 1.2
     */
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj != null && obj instanceof MarshalledObject) {
            MarshalledObject<?> other = (MarshalledObject<?>) obj;

            // if either is a ref to null, both must be
            if (objBytes == null || other.objBytes == null)
                return objBytes == other.objBytes;

            // quick, easy test
            if (objBytes.length != other.objBytes.length)
                return false;

            //!! There is talk about adding an array comparision method
            //!! at 1.2 -- if so, this should be rewritten.  -arnold
            for (int i = 0; i < objBytes.length; ++i) {
                if (objBytes[i] != other.objBytes[i])
                    return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * This class is used to marshal objects for
     * <code>MarshalledObject</code>.  It places the location annotations
     * to one side so that two <code>MarshalledObject</code>s can be
     * compared for equality if they differ only in location
     * annotations.  Objects written using this stream should be read back
     * from a <code>MarshalledObjectInputStream</code>.
     *
     * @see java.rmi.MarshalledObject
     * @see MarshalledObjectInputStream
     */
    private static class MarshalledObjectOutputStream
        extends MarshalOutputStream
    {
        /** The stream on which location objects are written. */
        private ObjectOutputStream locOut;

        /** <code>true</code> if non-<code>null</code> annotations are
         *  written.
         */
        private boolean hadAnnotations;

        /**
         * Creates a new <code>MarshalledObjectOutputStream</code> whose
         * non-location bytes will be written to <code>objOut</code> and whose
         * location annotations (if any) will be written to
         * <code>locOut</code>.
         */
        MarshalledObjectOutputStream(OutputStream objOut, OutputStream locOut)
            throws IOException
        {
            super(objOut);
            this.useProtocolVersion(ObjectStreamConstants.PROTOCOL_VERSION_2);
            this.locOut = new ObjectOutputStream(locOut);
            hadAnnotations = false;
        }

        /**
         * Returns <code>true</code> if any non-<code>null</code> location
         * annotations have been written to this stream.
         */
        boolean hadAnnotations() {
            return hadAnnotations;
        }

        /**
         * Overrides MarshalOutputStream.writeLocation implementation to write
         * annotations to the location stream.
         */
        protected void writeLocation(String loc) throws IOException {
            hadAnnotations |= (loc != null);
            locOut.writeObject(loc);
        }


        public void flush() throws IOException {
            super.flush();
            locOut.flush();
        }
    }

    /**
     * The counterpart to <code>MarshalledObjectOutputStream</code>.
     *
     * @see MarshalledObjectOutputStream
     */
    private static class MarshalledObjectInputStream
        extends MarshalInputStream
    {
        /**
         * The stream from which annotations will be read.  If this is
         * <code>null</code>, then all annotations were <code>null</code>.
         */
        private ObjectInputStream locIn;

        /**
         * Creates a new <code>MarshalledObjectInputStream</code> that
         * reads its objects from <code>objIn</code> and annotations
         * from <code>locIn</code>.  If <code>locIn</code> is
         * <code>null</code>, then all annotations will be
         * <code>null</code>.
         */
        @SuppressWarnings("removal")
        MarshalledObjectInputStream(InputStream objIn, InputStream locIn,
                    ObjectInputFilter filter)
            throws IOException
        {
            super(objIn);
            this.locIn = (locIn == null ? null : new ObjectInputStream(locIn));
            if (filter != null) {
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    MarshalledObjectInputStream.this.setObjectInputFilter(filter);
                    if (MarshalledObjectInputStream.this.locIn != null) {
                        MarshalledObjectInputStream.this.locIn.setObjectInputFilter(filter);
                    }
                    return null;
                });
            }
        }

        /**
         * Overrides MarshalInputStream.readLocation to return locations from
         * the stream we were given, or <code>null</code> if we were given a
         * <code>null</code> location stream.
         */
        protected Object readLocation()
            throws IOException, ClassNotFoundException
        {
            return (locIn == null ? null : locIn.readObject());
        }
    }

}
