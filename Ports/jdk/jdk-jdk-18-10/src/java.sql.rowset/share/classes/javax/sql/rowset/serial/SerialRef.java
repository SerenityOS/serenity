/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.sql.rowset.serial;

import java.sql.*;
import java.io.*;
import java.util.*;

/**
 * A serialized mapping of a <code>Ref</code> object, which is the mapping in the
 * Java programming language of an SQL <code>REF</code> value.
 * <p>
 * The <code>SerialRef</code> class provides a constructor  for
 * creating a <code>SerialRef</code> instance from a <code>Ref</code>
 * object and provides methods for getting and setting the <code>Ref</code> object.
 *
 * <h2> Thread safety </h2>
 *
 * A SerialRef is not safe for use by multiple concurrent threads.  If a
 * SerialRef is to be used by more than one thread then access to the SerialRef
 * should be controlled by appropriate synchronization.
 *
 * @since 1.5
 */
public class SerialRef implements Ref, Serializable, Cloneable {

    /**
     * String containing the base type name.
     * @serial
     */
    private String baseTypeName;

    /**
     * This will store the type <code>Ref</code> as an <code>Object</code>.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private Object object;

    /**
     * Private copy of the Ref reference.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable; checked in writeObject
    private Ref reference;

    /**
     * Constructs a <code>SerialRef</code> object from the given <code>Ref</code>
     * object.
     *
     * @param ref a Ref object; cannot be <code>null</code>
     * @throws SQLException if a database access occurs; if <code>ref</code>
     *     is <code>null</code>; or if the <code>Ref</code> object returns a
     *     <code>null</code> value base type name.
     * @throws SerialException if an error occurs serializing the <code>Ref</code>
     *     object
     */
    public SerialRef(Ref ref) throws SerialException, SQLException {
        if (ref == null) {
            throw new SQLException("Cannot instantiate a SerialRef object " +
                "with a null Ref object");
        }
        reference = ref;
        object = ref;
        if (ref.getBaseTypeName() == null) {
            throw new SQLException("Cannot instantiate a SerialRef object " +
                "that returns a null base type name");
        } else {
            baseTypeName = ref.getBaseTypeName();
        }
    }

    /**
     * Returns a string describing the base type name of the <code>Ref</code>.
     *
     * @return a string of the base type name of the Ref
     * @throws SerialException in no Ref object has been set
     */
    public String getBaseTypeName() throws SerialException {
        return baseTypeName;
    }

    /**
     * Returns an <code>Object</code> representing the SQL structured type
     * to which this <code>SerialRef</code> object refers.  The attributes
     * of the structured type are mapped according to the given type map.
     *
     * @param map a <code>java.util.Map</code> object containing zero or
     *        more entries, with each entry consisting of 1) a <code>String</code>
     *        giving the fully qualified name of a UDT and 2) the
     *        <code>Class</code> object for the <code>SQLData</code> implementation
     *        that defines how the UDT is to be mapped
     * @return an object instance resolved from the Ref reference and mapped
     *        according to the supplied type map
     * @throws SerialException if an error is encountered in the reference
     *        resolution
     */
    public Object getObject(java.util.Map<String,Class<?>> map)
        throws SerialException
    {
        map = new Hashtable<String, Class<?>>(map);
        if (object != null) {
            return map.get(object);
        } else {
            throw new SerialException("The object is not set");
        }
    }

    /**
     * Returns an <code>Object</code> representing the SQL structured type
     * to which this <code>SerialRef</code> object refers.
     *
     * @return an object instance resolved from the Ref reference
     * @throws SerialException if an error is encountered in the reference
     *         resolution
     */
    public Object getObject() throws SerialException {

        if (reference != null) {
            try {
                return reference.getObject();
            } catch (SQLException e) {
                throw new SerialException("SQLException: " + e.getMessage());
            }
        }

        if (object != null) {
            return object;
        }


        throw new SerialException("The object is not set");

    }

    /**
     * Sets the SQL structured type that this <code>SerialRef</code> object
     * references to the given <code>Object</code> object.
     *
     * @param obj an <code>Object</code> representing the SQL structured type
     *        to be referenced
     * @throws SerialException if an error is encountered generating the
     * the structured type referenced by this <code>SerialRef</code> object
     */
    public void setObject(Object obj) throws SerialException {
        try {
            reference.setObject(obj);
        } catch (SQLException e) {
            throw new SerialException("SQLException: " + e.getMessage());
        }
        object = obj;
    }

    /**
     * Compares this SerialRef to the specified object.  The result is {@code
     * true} if and only if the argument is not {@code null} and is a {@code
     * SerialRef} object that represents the same object as this
     * object.
     *
     * @param  obj The object to compare this {@code SerialRef} against
     *
     * @return  {@code true} if the given object represents a {@code SerialRef}
     *          equivalent to this SerialRef, {@code false} otherwise
     *
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if(obj instanceof SerialRef) {
            SerialRef ref = (SerialRef)obj;
            return baseTypeName.equals(ref.baseTypeName) &&
                    object.equals(ref.object);
        }
        return false;
    }

    /**
     * Returns a hash code for this {@code SerialRef}.
     * @return  a hash code value for this object.
     */
    public int hashCode() {
        return (31 + object.hashCode()) * 31 + baseTypeName.hashCode();
    }

    /**
     * Returns a clone of this {@code SerialRef}.
     * The underlying {@code Ref} object will be set to null.
     *
     * @return  a clone of this SerialRef
     */
    public Object clone() {
        try {
            SerialRef ref = (SerialRef) super.clone();
            ref.reference = null;
            return ref;
        } catch (CloneNotSupportedException ex) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }

    }

    /**
     * readObject is called to restore the state of the SerialRef from
     * a stream.
     * @param s the {@code ObjectInputStream} to read from.
     *
     * @throws  ClassNotFoundException if the class of a serialized object
     *          could not be found.
     * @throws  IOException if an I/O error occurs.
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = s.readFields();
        object = fields.get("object", null);
        baseTypeName = (String) fields.get("baseTypeName", null);
        reference = (Ref) fields.get("reference", null);
    }

    /**
     * writeObject is called to save the state of the SerialRef
     * to a stream.
     * @param s the {@code ObjectOutputStream} to write to.
     * @throws  IOException if an I/O error occurs.
     */
    private void writeObject(ObjectOutputStream s)
            throws IOException {

        ObjectOutputStream.PutField fields = s.putFields();
        fields.put("baseTypeName", baseTypeName);
        fields.put("object", object);
        // Note: this check to see if it is an instance of Serializable
        // is for backwards compatibility
        fields.put("reference", reference instanceof Serializable ? reference : null);
        s.writeFields();
    }

    /**
     * The identifier that assists in the serialization of this <code>SerialRef</code>
     * object.
     */
    static final long serialVersionUID = -4727123500609662274L;


}
