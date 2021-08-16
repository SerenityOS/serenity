/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider.certpath;

import java.util.Arrays;
import java.io.IOException;
import java.security.PublicKey;
import javax.security.auth.x500.X500Principal;
import sun.security.x509.KeyIdentifier;
import sun.security.util.DerValue;

/**
 * Class for ResponderId entities as described in RFC6960.  ResponderId objects
 * are used to uniquely identify OCSP responders.
 * <p>
 * The RFC 6960 defines a ResponderID structure as:
 * <pre>
 * ResponderID ::= CHOICE {
 *      byName              [1] Name,
 *      byKey               [2] KeyHash }
 *
 * KeyHash ::= OCTET STRING -- SHA-1 hash of responder's public key
 * (excluding the tag and length fields)
 *
 * Name is defined in RFC 5280.
 * </pre>
 *
 * @see ResponderId.Type
 * @since 9
 */
public final class ResponderId {

    /**
     * A {@code ResponderId} enumeration describing the accepted forms for a
     * {@code ResponderId}.
     *
     * @see ResponderId
     * @since 9
     */
    public static enum Type {
        /**
         * A BY_NAME {@code ResponderId} will be built from a subject name,
         * either as an {@code X500Principal} or a DER-encoded byte array.
         */
        BY_NAME(1, "byName"),

        /**
         * A BY_KEY {@code ResponderId} will be built from a public key
         * identifier, either derived from a {@code PublicKey} or directly
         * from a DER-encoded byte array containing the key identifier.
         */
        BY_KEY(2, "byKey");

        private final int tagNumber;
        private final String ridTypeName;

        private Type(int value, String name) {
            this.tagNumber = value;
            this.ridTypeName = name;
        }

        public int value() {
            return tagNumber;
        }

        @Override
        public String toString() {
            return ridTypeName;
        }
    }

    private Type type;
    private X500Principal responderName;
    private KeyIdentifier responderKeyId;
    private byte[] encodedRid;

    /**
     * Constructs a {@code ResponderId} object using an {@code X500Principal}.
     * When encoded in DER this object will use the BY_NAME option.
     *
     * @param subjectName the subject name of the certificate used
     * to sign OCSP responses.
     *
     * @throws IOException if the internal DER-encoding of the
     *      {@code X500Principal} fails.
     */
    public ResponderId(X500Principal subjectName) throws IOException {
        responderName = subjectName;
        responderKeyId = null;
        encodedRid = principalToBytes();
        type = Type.BY_NAME;
    }

    /**
     * Constructs a {@code ResponderId} object using a {@code PublicKey}.
     * When encoded in DER this object will use the byKey option, a
     * SHA-1 hash of the responder's public key.
     *
     * @param pubKey the OCSP responder's public key
     *
     * @throws IOException if the internal DER-encoding of the
     *      {@code KeyIdentifier} fails.
     */
    public ResponderId(PublicKey pubKey) throws IOException {
        responderKeyId = new KeyIdentifier(pubKey);
        responderName = null;
        encodedRid = keyIdToBytes();
        type = Type.BY_KEY;
    }

    /**
     * Constructs a {@code ResponderId} object from its DER-encoding.
     *
     * @param encodedData the DER-encoded bytes
     *
     * @throws IOException if the encodedData is not properly DER encoded
     */
    public ResponderId(byte[] encodedData) throws IOException {
        DerValue outer = new DerValue(encodedData);

        if (outer.isContextSpecific((byte)Type.BY_NAME.value())
                && outer.isConstructed()) {
            // Use the X500Principal constructor as a way to sanity
            // check the incoming data.
            responderName = new X500Principal(outer.getDataBytes());
            encodedRid = principalToBytes();
            type = Type.BY_NAME;
        } else if (outer.isContextSpecific((byte)Type.BY_KEY.value())
                && outer.isConstructed()) {
            // Use the KeyIdentifier constructor as a way to sanity
            // check the incoming data.
            responderKeyId =
                new KeyIdentifier(new DerValue(outer.getDataBytes()));
            encodedRid = keyIdToBytes();
            type = Type.BY_KEY;
        } else {
            throw new IOException("Invalid ResponderId content");
        }
    }

    /**
     * Encode a {@code ResponderId} in DER form
     *
     * @return a byte array containing the DER-encoded representation for this
     *      {@code ResponderId}
     */
    public byte[] getEncoded() {
        return encodedRid.clone();
    }

    /**
     * Return the type of {@ResponderId}
     *
     * @return a number corresponding to the context-specific tag number
     *      used in the DER-encoding for a {@code ResponderId}
     */
    public ResponderId.Type getType() {
        return type;
    }

    /**
     * Get the length of the encoded {@code ResponderId} (including the tag and
     * length of the explicit tagging from the outer ASN.1 CHOICE).
     *
     * @return the length of the encoded {@code ResponderId}
     */
    public int length() {
        return encodedRid.length;
    }

    /**
     * Obtain the underlying {@code X500Principal} from a {@code ResponderId}
     *
     * @return the {@code X500Principal} for this {@code ResponderId} if it
     *      is a BY_NAME variant.  If the {@code ResponderId} is a BY_KEY
     *      variant, this routine will return {@code null}.
     */
    public X500Principal getResponderName() {
        return responderName;
    }

    /**
     * Obtain the underlying key identifier from a {@code ResponderId}
     *
     * @return the {@code KeyIdentifier} for this {@code ResponderId} if it
     *      is a BY_KEY variant.  If the {@code ResponderId} is a BY_NAME
     *      variant, this routine will return {@code null}.
     */
    public KeyIdentifier getKeyIdentifier() {
        return responderKeyId;
    }

    /**
     * Compares the specified object with this {@code ResponderId} for equality.
     * A ResponderId will only be considered equivalent if both the type and
     * data value are equal.  Two ResponderIds initialized by name and
     * key ID, respectively, will not be equal even if the
     * ResponderId objects are created from the same source certificate.
     *
     * @param obj the object to be compared against
     *
     * @return true if the specified object is equal to this {@code Responderid}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (obj instanceof ResponderId) {
            ResponderId respObj = (ResponderId)obj;
                return Arrays.equals(encodedRid, respObj.getEncoded());
        }

        return false;
    }

    /**
     * Returns the hash code value for this {@code ResponderId}
     *
     * @return the hash code value for this {@code ResponderId}
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(encodedRid);
    }

    /**
     * Create a String representation of this {@code ResponderId}
     *
     * @return a String representation of this {@code ResponderId}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        switch (type) {
            case BY_NAME:
                sb.append(type).append(": ").append(responderName);
                break;
            case BY_KEY:
                sb.append(type).append(": ");
                for (byte keyIdByte : responderKeyId.getIdentifier()) {
                    sb.append(String.format("%02X", keyIdByte));
                }
                break;
            default:
                sb.append("Unknown ResponderId Type: ").append(type);
        }
        return sb.toString();
    }

    /**
     * Convert the responderName data member into its DER-encoded form
     *
     * @return the DER encoding for a responder ID byName option, including
     *      explicit context-specific tagging.
     *
     * @throws IOException if any encoding error occurs
     */
    private byte[] principalToBytes() throws IOException {
        DerValue dv = new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte)Type.BY_NAME.value()),
                responderName.getEncoded());
        return dv.toByteArray();
    }

    /**
     * Convert the responderKeyId data member into its DER-encoded form
     *
     * @return the DER encoding for a responder ID byKey option, including
     *      explicit context-specific tagging.
     *
     * @throws IOException if any encoding error occurs
     */
    private byte[] keyIdToBytes() throws IOException {
        // Place the KeyIdentifier bytes into an OCTET STRING
        DerValue inner = new DerValue(DerValue.tag_OctetString,
                responderKeyId.getIdentifier());

        // Mark the OCTET STRING-wrapped KeyIdentifier bytes
        // as EXPLICIT CONTEXT 2
        DerValue outer = new DerValue(DerValue.createTag(DerValue.TAG_CONTEXT,
                true, (byte)Type.BY_KEY.value()), inner.toByteArray());

        return outer.toByteArray();
    }

}
