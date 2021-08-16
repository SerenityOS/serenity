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

package javax.naming;

/**
  * This class represents the binary form of the address of
  * a communications end-point.
  *<p>
  * A BinaryRefAddr consists of a type that describes the communication mechanism
  * and an opaque buffer containing the address description
  * specific to that communication mechanism. The format and interpretation of
  * the address type and the contents of the opaque buffer are based on
  * the agreement of three parties: the client that uses the address,
  * the object/server that can be reached using the address,
  * and the administrator or program that creates the address.
  *<p>
  * An example of a binary reference address is an BER X.500 presentation address.
  * Another example of a binary reference address is a serialized form of
  * a service's object handle.
  *<p>
  * A binary reference address is immutable in the sense that its fields
  * once created, cannot be replaced. However, it is possible to access
  * the byte array used to hold the opaque buffer. Programs are strongly
  * recommended against changing this byte array. Changes to this
  * byte array need to be explicitly synchronized.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see RefAddr
  * @see StringRefAddr
  * @since 1.3
  */

  /*
  * The serialized form of a BinaryRefAddr object consists of its type
  * name String and a byte array containing its "contents".
  */

public class BinaryRefAddr extends RefAddr {
    /**
     * Contains the bytes of the address.
     * This field is initialized by the constructor and returned
     * using getAddressBytes() and getAddressContents().
     * @serial
     */
    private byte[] buf = null;

    /**
      * Constructs a new instance of BinaryRefAddr using its address type and a byte
      * array for contents.
      *
      * @param addrType A non-null string describing the type of the address.
      * @param src      The non-null contents of the address as a byte array.
      *                 The contents of src is copied into the new BinaryRefAddr.
      */
    public BinaryRefAddr(String addrType, byte[] src) {
        this(addrType, src, 0, src.length);
    }

    /**
      * Constructs a new instance of BinaryRefAddr using its address type and
      * a region of a byte array for contents.
      *
      * @param addrType A non-null string describing the type of the address.
      * @param src      The non-null contents of the address as a byte array.
      *                 The contents of src is copied into the new BinaryRefAddr.
      * @param offset   The starting index in src to get the bytes.
      *                 {@code 0 <= offset <= src.length}.
      * @param count    The number of bytes to extract from src.
      *                 {@code 0 <= count <= src.length-offset}.
      */
    public BinaryRefAddr(String addrType, byte[] src, int offset, int count) {
        super(addrType);
        buf = new byte[count];
        System.arraycopy(src, offset, buf, 0, count);
    }

    /**
      * Retrieves the contents of this address as an Object.
      * The result is a byte array.
      * Changes to this array will affect this BinaryRefAddr's contents.
      * Programs are recommended against changing this array's contents
      * and to lock the buffer if they need to change it.
      *
      * @return The non-null buffer containing this address's contents.
      */
    public Object getContent() {
        return buf;
    }


    /**
      * Determines whether obj is equal to this address.  It is equal if
      * it contains the same address type and their contents are byte-wise
      * equivalent.
      * @param obj      The possibly null object to check.
      * @return true if the object is equal; false otherwise.
      */
    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof BinaryRefAddr)) {
            BinaryRefAddr target = (BinaryRefAddr)obj;
            if (addrType.compareTo(target.addrType) == 0) {
                if (buf == null && target.buf == null)
                    return true;
                if (buf == null || target.buf == null ||
                    buf.length != target.buf.length)
                    return false;
                for (int i = 0; i < buf.length; i++)
                    if (buf[i] != target.buf[i])
                        return false;
                return true;
            }
        }
        return false;
    }

    /**
      * Computes the hash code of this address using its address type and contents.
      * Two BinaryRefAddrs have the same hash code if they have
      * the same address type and the same contents.
      * It is also possible for different BinaryRefAddrs to have
      * the same hash code.
      *
      * @return The hash code of this address as an int.
      */
    public int hashCode() {
        int hash = addrType.hashCode();
        for (int i = 0; i < buf.length; i++) {
            hash += buf[i];     // %%% improve later
        }
        return hash;
    }

    /**
      * Generates the string representation of this address.
      * The string consists of the address's type and contents with labels.
      * The first 32 bytes of contents are displayed (in hexadecimal).
      * If there are more than 32 bytes, "..." is used to indicate more.
      * This string is meant to used for debugging purposes and not
      * meant to be interpreted programmatically.
      * @return The non-null string representation of this address.
      */
    public String toString(){
        StringBuilder str = new StringBuilder("Address Type: " + addrType + "\n");

        str.append("AddressContents: ");
        for (int i = 0; i<buf.length && i < 32; i++) {
            str.append(Integer.toHexString(buf[i]) +" ");
        }
        if (buf.length >= 32)
            str.append(" ...\n");
        return (str.toString());
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = -3415254970957330361L;
}
