/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.krb5.internal;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

import sun.security.krb5.Asn1Exception;
import sun.security.krb5.internal.util.KerberosString;
import sun.security.util.*;

/**
 * Implements the ASN.1 ETYPE-INFO-ENTRY type.
 *
 * ETYPE-INFO-ENTRY     ::= SEQUENCE {
 *         etype        [0] Int32,
 *         salt         [1] OCTET STRING OPTIONAL
 *   }
 *
 * @author Seema Malkani
 */

public class ETypeInfo {

    private int etype;
    private String salt = null;

    private static final byte TAG_TYPE = 0;
    private static final byte TAG_VALUE = 1;

    private ETypeInfo() {
    }

    public ETypeInfo(int etype, String salt) {
        this.etype = etype;
        this.salt = salt;
    }

    public Object clone() {
        return new ETypeInfo(etype, salt);
    }

    /**
     * Constructs a ETypeInfo object.
     * @param encoding a DER-encoded data.
     * @exception Asn1Exception if an error occurs while decoding an
     *            ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public ETypeInfo(DerValue encoding) throws Asn1Exception, IOException {
        DerValue der = null;

        if (encoding.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }

        // etype
        der = encoding.getData().getDerValue();
        if ((der.getTag() & 0x1F) == 0x00) {
            this.etype = der.getData().getBigInteger().intValue();
        }
        else
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);

        // salt
        if (encoding.getData().available() > 0) {
            der = encoding.getData().getDerValue();
            if ((der.getTag() & 0x1F) == 0x01) {
                byte[] saltBytes = der.getData().getOctetString();

                // Although salt is defined as an OCTET STRING, it's the
                // encoding from of a string. As RFC 4120 says:
                //
                // "The salt, ..., is also completely unspecified with respect
                // to character set and is probably locale-specific".
                //
                // It's known that this field is using the same encoding as
                // KerberosString in most implementations.

                if (KerberosString.MSNAME) {
                    this.salt = new String(saltBytes, UTF_8);
                } else {
                    this.salt = new String(saltBytes);
                }
            }
        }

        if (encoding.getData().available() > 0)
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
    }

    /**
     * Encodes this object to an OutputStream.
     *
     * @return byte array of the encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     * @exception Asn1Exception on encoding errors.
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {

        DerOutputStream bytes = new DerOutputStream();
        DerOutputStream temp = new DerOutputStream();

        temp.putInteger(etype);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true,
                                        TAG_TYPE), temp);

        if (salt != null) {
            temp = new DerOutputStream();
            if (KerberosString.MSNAME) {
                temp.putOctetString(salt.getBytes(UTF_8));
            } else {
                temp.putOctetString(salt.getBytes());
            }
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true,
                                        TAG_VALUE), temp);
        }

        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, bytes);
        return temp.toByteArray();
    }

    // accessor methods
    public int getEType() {
        return etype;
    }

    public String getSalt() {
        return salt;
    }

}
