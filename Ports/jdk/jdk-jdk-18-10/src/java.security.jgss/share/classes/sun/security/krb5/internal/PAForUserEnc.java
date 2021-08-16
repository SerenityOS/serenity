/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import sun.security.krb5.*;
import sun.security.krb5.internal.crypto.KeyUsage;
import sun.security.krb5.internal.util.KerberosString;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Implements the ASN.1 PA-FOR-USER type.
 *
 * <pre>{@code
 * padata-type  ::= PA-FOR-USER
 *                  -- value 129
 * padata-value ::= EncryptedData
 *                  -- PA-FOR-USER-ENC
 * PA-FOR-USER-ENC ::= SEQUENCE {
 *     userName[0] PrincipalName,
 *     userRealm[1] Realm,
 *     cksum[2] Checksum,
 *     auth-package[3] KerberosString
 * }
 * }</pre>
 *
 * <p>
 * This definition reflects MS-SFU.
 */

public class PAForUserEnc {
    public final PrincipalName name;
    private final EncryptionKey key;
    public static final String AUTH_PACKAGE = "Kerberos";

    public PAForUserEnc(PrincipalName name, EncryptionKey key) {
        this.name = name;
        this.key = key;
    }

    /**
     * Constructs a PA-FOR-USER object from a DER encoding.
     * @param encoding the input object
     * @param key the key to verify the checksum inside encoding
     * @throws KrbException if the verification fails.
     * Note: this method is now only used by test KDC, therefore
     * the verification is ignored (at the moment).
     */
    public PAForUserEnc(DerValue encoding, EncryptionKey key)
            throws Asn1Exception, KrbException, IOException {
        DerValue der = null;
        this.key = key;

        if (encoding.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }

        // Realm after name? Quite abnormal.
        PrincipalName tmpName = null;
        der = encoding.getData().getDerValue();
        if ((der.getTag() & 0x1F) == 0x00) {
            try {
                tmpName = new PrincipalName(der.getData().getDerValue(),
                    new Realm("PLACEHOLDER"));
            } catch (RealmException re) {
                // Impossible
            }
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }

        der = encoding.getData().getDerValue();
        if ((der.getTag() & 0x1F) == 0x01) {
            try {
                Realm realm = new Realm(der.getData().getDerValue());
                name = new PrincipalName(
                        tmpName.getNameType(), tmpName.getNameStrings(), realm);
            } catch (RealmException re) {
                throw new IOException(re);
            }
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }

        der = encoding.getData().getDerValue();
        if ((der.getTag() & 0x1F) == 0x02) {
            // Deal with the checksum
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }

        der = encoding.getData().getDerValue();
        if ((der.getTag() & 0x1F) == 0x03) {
            String authPackage = new KerberosString(der.getData().getDerValue()).toString();
            if (!authPackage.equalsIgnoreCase(AUTH_PACKAGE)) {
                throw new IOException("Incorrect auth-package");
            }
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        if (encoding.getData().available() > 0)
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
    }

    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream bytes = new DerOutputStream();
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x00), name.asn1Encode());
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x01), name.getRealm().asn1Encode());

        try {
            // MS-SFU 2.2.1: use hmac-md5 checksum regardless of key type
            Checksum cks = new Checksum(
                    Checksum.CKSUMTYPE_HMAC_MD5_ARCFOUR,
                    getS4UByteArray(),
                    key,
                    KeyUsage.KU_PA_FOR_USER_ENC_CKSUM);
            bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x02), cks.asn1Encode());
        } catch (KrbException ke) {
            throw new IOException(ke);
        }

        DerOutputStream temp = new DerOutputStream();
        temp.putDerValue(new KerberosString(AUTH_PACKAGE).toDerValue());
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x03), temp);

        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, bytes);
        return temp.toByteArray();
    }

    /**
     * Returns S4UByteArray, the block to calculate checksum inside a
     * PA-FOR-USER-ENC data structure. It includes:
     * 1. userName.name-type encoded as a 4-byte integer in little endian
     *    byte order
     * 2. all string values in the sequence of strings contained in the
     *    userName.name-string field
     * 3. the string value of the userRealm field
     * 4. the string value of auth-package field
     */
    public byte[] getS4UByteArray() {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        ba.writeBytes(new byte[4]);
        for (String s: name.getNameStrings()) {
            ba.writeBytes(s.getBytes(UTF_8));
        }
        ba.writeBytes(name.getRealm().toString().getBytes(UTF_8));
        ba.writeBytes(AUTH_PACKAGE.getBytes(UTF_8));
        byte[] output = ba.toByteArray();
        int pnType = name.getNameType();
        output[0] = (byte)(pnType & 0xff);
        output[1] = (byte)((pnType>>8) & 0xff);
        output[2] = (byte)((pnType>>16) & 0xff);
        output[3] = (byte)((pnType>>24) & 0xff);
        return output;
    }

    public PrincipalName getName() {
        return name;
    }

    public String toString() {
        return "PA-FOR-USER: " + name;
    }
}
