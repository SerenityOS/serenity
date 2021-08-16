/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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
package java.rmi.server;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An <code>ObjID</code> is used to identify a remote object exported
 * to an RMI runtime.  When a remote object is exported, it is assigned
 * an object identifier either implicitly or explicitly, depending on
 * the API used to export.
 *
 * <p>The {@link #ObjID()} constructor can be used to generate a unique
 * object identifier.  Such an <code>ObjID</code> is unique over time
 * with respect to the host it is generated on.
 *
 * The {@link #ObjID(int)} constructor can be used to create a
 * "well-known" object identifier.  The scope of a well-known
 * <code>ObjID</code> depends on the RMI runtime it is exported to.
 *
 * <p>An <code>ObjID</code> instance contains an object number (of type
 * <code>long</code>) and an address space identifier (of type
 * {@link UID}).  In a unique <code>ObjID</code>, the address space
 * identifier is unique with respect to a given host over time.  In a
 * well-known <code>ObjID</code>, the address space identifier is
 * equivalent to one returned by invoking the {@link UID#UID(short)}
 * constructor with the value zero.
 *
 * <p>If the system property {@systemProperty java.rmi.server.randomIDs}
 * is defined to equal the string <code>"true"</code> (case insensitive),
 * then the {@link #ObjID()} constructor will use a cryptographically
 * strong random number generator to choose the object number of the
 * returned <code>ObjID</code>.
 *
 * @author      Ann Wollrath
 * @author      Peter Jones
 * @since       1.1
 */
public final class ObjID implements Serializable {

    /** Object number for well-known <code>ObjID</code> of the registry. */
    public static final int REGISTRY_ID = 0;

    /** Object number for well-known <code>ObjID</code> of the activator. */
    public static final int ACTIVATOR_ID = 1;

    /**
     * Object number for well-known <code>ObjID</code> of
     * the distributed garbage collector.
     */
    public static final int DGC_ID = 2;

    /** indicate compatibility with JDK 1.1.x version of class */
    private static final long serialVersionUID = -6386392263968365220L;

    private static final AtomicLong nextObjNum = new AtomicLong();
    private static final UID mySpace = new UID();
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * @serial object number
     * @see #hashCode
     */
    private final long objNum;

    /**
     * @serial address space identifier (unique to host over time)
     */
    private final UID space;

    /**
     * Generates a unique object identifier.
     *
     * <p>If the system property <code>java.rmi.server.randomIDs</code>
     * is defined to equal the string <code>"true"</code> (case insensitive),
     * then this constructor will use a cryptographically
     * strong random number generator to choose the object number of the
     * returned <code>ObjID</code>.
     */
    public ObjID() {
        /*
         * If generating random object numbers, create a new UID to
         * ensure uniqueness; otherwise, use a shared UID because
         * sequential object numbers already ensure uniqueness.
         */
        if (useRandomIDs()) {
            space = new UID();
            objNum = secureRandom.nextLong();
        } else {
            space = mySpace;
            objNum = nextObjNum.getAndIncrement();
        }
    }

    /**
     * Creates a "well-known" object identifier.
     *
     * <p>An <code>ObjID</code> created via this constructor will not
     * clash with any <code>ObjID</code>s generated via the no-arg
     * constructor.
     *
     * @param   objNum object number for well-known object identifier
     */
    public ObjID(int objNum) {
        space = new UID((short) 0);
        this.objNum = objNum;
    }

    /**
     * Constructs an object identifier given data read from a stream.
     */
    private ObjID(long objNum, UID space) {
        this.objNum = objNum;
        this.space = space;
    }

    /**
     * Marshals a binary representation of this <code>ObjID</code> to
     * an <code>ObjectOutput</code> instance.
     *
     * <p>Specifically, this method first invokes the given stream's
     * {@link ObjectOutput#writeLong(long)} method with this object
     * identifier's object number, and then it writes its address
     * space identifier by invoking its {@link UID#write(DataOutput)}
     * method with the stream.
     *
     * @param   out the <code>ObjectOutput</code> instance to write
     * this <code>ObjID</code> to
     *
     * @throws  IOException if an I/O error occurs while performing
     * this operation
     */
    public void write(ObjectOutput out) throws IOException {
        out.writeLong(objNum);
        space.write(out);
    }

    /**
     * Constructs and returns a new <code>ObjID</code> instance by
     * unmarshalling a binary representation from an
     * <code>ObjectInput</code> instance.
     *
     * <p>Specifically, this method first invokes the given stream's
     * {@link ObjectInput#readLong()} method to read an object number,
     * then it invokes {@link UID#read(DataInput)} with the
     * stream to read an address space identifier, and then it
     * creates and returns a new <code>ObjID</code> instance that
     * contains the object number and address space identifier that
     * were read from the stream.
     *
     * @param   in the <code>ObjectInput</code> instance to read
     * <code>ObjID</code> from
     *
     * @return  unmarshalled <code>ObjID</code> instance
     *
     * @throws  IOException if an I/O error occurs while performing
     * this operation
     */
    public static ObjID read(ObjectInput in) throws IOException {
        long num = in.readLong();
        UID space = UID.read(in);
        return new ObjID(num, space);
    }

    /**
     * Returns the hash code value for this object identifier, the
     * object number.
     *
     * @return  the hash code value for this object identifier
     */
    public int hashCode() {
        return (int) objNum;
    }

    /**
     * Compares the specified object with this <code>ObjID</code> for
     * equality.
     *
     * This method returns <code>true</code> if and only if the
     * specified object is an <code>ObjID</code> instance with the same
     * object number and address space identifier as this one.
     *
     * @param   obj the object to compare this <code>ObjID</code> to
     *
     * @return  <code>true</code> if the given object is equivalent to
     * this one, and <code>false</code> otherwise
     */
    public boolean equals(Object obj) {
        if (obj instanceof ObjID) {
            ObjID id = (ObjID) obj;
            return objNum == id.objNum && space.equals(id.space);
        } else {
            return false;
        }
    }

    /**
     * Returns a string representation of this object identifier.
     *
     * @return  a string representation of this object identifier
     */
    /*
     * The address space identifier is only included in the string
     * representation if it does not denote the local address space
     * (or if the randomIDs property was set).
     */
    public String toString() {
        return "[" + (space.equals(mySpace) ? "" : space + ", ") +
            objNum + "]";
    }

    private static boolean useRandomIDs() {
        @SuppressWarnings("removal")
        String value = AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("java.rmi.server.randomIDs"));
        return value == null ? true : Boolean.parseBoolean(value);
    }
}
