/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.x509;

import java.io.IOException;
import java.security.PublicKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import sun.security.util.HexDumpEncoder;
import sun.security.util.*;

/**
 * Represent the Key Identifier ASN.1 object.
 *
 * @author Amit Kapoor
 * @author Hemma Prafullchandra
 */
public class KeyIdentifier {
    private byte[] octetString;

    /**
     * Create a KeyIdentifier with the passed bit settings.
     *
     * @param octetString the octet string identifying the key identifier.
     */
    public KeyIdentifier(byte[] octetString) {
        this.octetString = octetString.clone();
    }

    /**
     * Create a KeyIdentifier from the DER encoded value.
     *
     * @param val the DerValue
     */
    public KeyIdentifier(DerValue val) throws IOException {
        octetString = val.getOctetString();
    }

    /**
     * Creates a KeyIdentifier from a public-key value.
     *
     * <p>From RFC 5280: Two common methods for generating key identifiers from
     * the public key are:
     * <ol>
     * <li>The keyIdentifier is composed of the 160-bit SHA-1 hash of the
     * value of the BIT STRING subjectPublicKey (excluding the tag,
     * length, and number of unused bits).
     *
     * <li>The keyIdentifier is composed of a four bit type field with
     * the value 0100 followed by the least significant 60 bits of the
     * SHA-1 hash of the value of the BIT STRING subjectPublicKey.
     * </ol>
     * <p>This method supports method 1.
     *
     * @param pubKey the public key from which to construct this KeyIdentifier
     * @throws IOException on parsing errors
     */
    public KeyIdentifier(PublicKey pubKey)
        throws IOException
    {
        DerValue algAndKey = new DerValue(pubKey.getEncoded());
        if (algAndKey.tag != DerValue.tag_Sequence)
            throw new IOException("PublicKey value is not a valid "
                                  + "X.509 public key");

        AlgorithmId algid = AlgorithmId.parse(algAndKey.data.getDerValue());
        byte[] key = algAndKey.data.getUnalignedBitString().toByteArray();

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e3) {
            throw new IOException("SHA1 not supported");
        }
        md.update(key);
        this.octetString = md.digest();
    }

    /**
     * Return the value of the KeyIdentifier as byte array.
     */
    public byte[] getIdentifier() {
        return octetString.clone();
    }

    /**
     * Returns a printable representation of the KeyUsage.
     */
    public String toString() {
        String s = "KeyIdentifier [\n";

        HexDumpEncoder encoder = new HexDumpEncoder();
        s += encoder.encodeBuffer(octetString);
        s += "]\n";
        return (s);
    }

    /**
     * Write the KeyIdentifier to the DerOutputStream.
     *
     * @param out the DerOutputStream to write the object to.
     * @exception IOException
     */
    void encode(DerOutputStream out) throws IOException {
        out.putOctetString(octetString);
    }

    /**
     * Returns a hash code value for this object.
     * Objects that are equal will also have the same hashcode.
     */
    public int hashCode () {
        int retval = 0;
        for (int i = 0; i < octetString.length; i++)
            retval += octetString[i] * i;
        return retval;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof KeyIdentifier))
            return false;
        byte[] otherString = ((KeyIdentifier)other).octetString;
        return java.util.Arrays.equals(octetString, otherString);
    }
}
