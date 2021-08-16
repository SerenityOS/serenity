/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs;

import java.io.*;

import sun.security.util.*;

/**
 * A ContentInfo type, as defined in PKCS#7.
 *
 * @author Benjamin Renaud
 */

public class ContentInfo {

    // pkcs7 pre-defined content types
    public static ObjectIdentifier PKCS7_OID =
            ObjectIdentifier.of(KnownOIDs.PKCS7);
    public static ObjectIdentifier DATA_OID =
            ObjectIdentifier.of(KnownOIDs.Data);
    public static ObjectIdentifier SIGNED_DATA_OID =
            ObjectIdentifier.of(KnownOIDs.SignedData);
    public static ObjectIdentifier ENVELOPED_DATA_OID =
            ObjectIdentifier.of(KnownOIDs.EnvelopedData);
    public static ObjectIdentifier SIGNED_AND_ENVELOPED_DATA_OID =
            ObjectIdentifier.of(KnownOIDs.SignedAndEnvelopedData);
    public static ObjectIdentifier DIGESTED_DATA_OID =
            ObjectIdentifier.of(KnownOIDs.DigestedData);
    public static ObjectIdentifier ENCRYPTED_DATA_OID =
            ObjectIdentifier.of(KnownOIDs.EncryptedData);

    // this is for backwards-compatibility with JDK 1.1.x
    public static ObjectIdentifier OLD_SIGNED_DATA_OID =
            ObjectIdentifier.of(KnownOIDs.JDK_OLD_SignedData);
    public static ObjectIdentifier OLD_DATA_OID =
            ObjectIdentifier.of(KnownOIDs.JDK_OLD_Data);

    // The ASN.1 systax for the Netscape Certificate Sequence data type is
    // defined at:
    //      http://wp.netscape.com/eng/security/comm4-cert-download.html
    public static ObjectIdentifier NETSCAPE_CERT_SEQUENCE_OID =
            ObjectIdentifier.of(KnownOIDs.NETSCAPE_CertSequence);

    // timestamp token (id-ct-TSTInfo) from RFC 3161
    public static ObjectIdentifier TIMESTAMP_TOKEN_INFO_OID =
            ObjectIdentifier.of(KnownOIDs.TimeStampTokenInfo);

    ObjectIdentifier contentType;
    DerValue content; // OPTIONAL

    public ContentInfo(ObjectIdentifier contentType, DerValue content) {
        this.contentType = contentType;
        this.content = content;
    }

    /**
     * Make a contentInfo of type data.
     */
    public ContentInfo(byte[] bytes) {
        DerValue octetString = new DerValue(DerValue.tag_OctetString, bytes);
        this.contentType = DATA_OID;
        this.content = octetString;
    }

    /**
     * Parses a PKCS#7 content info.
     */
    public ContentInfo(DerInputStream derin)
        throws IOException, ParsingException
    {
        this(derin, false);
    }

    /**
     * Parses a PKCS#7 content info.
     *
     * <p>This constructor is used only for backwards compatibility with
     * PKCS#7 blocks that were generated using JDK1.1.x.
     *
     * @param derin the ASN.1 encoding of the content info.
     * @param oldStyle flag indicating whether or not the given content info
     * is encoded according to JDK1.1.x.
     */
    public ContentInfo(DerInputStream derin, boolean oldStyle)
        throws IOException, ParsingException
    {
        DerInputStream disType;
        DerInputStream disTaggedContent;
        DerValue type;
        DerValue taggedContent;
        DerValue[] typeAndContent;
        DerValue[] contents;

        typeAndContent = derin.getSequence(2);
        if (typeAndContent.length < 1 || typeAndContent.length > 2) {
            throw new ParsingException("Invalid length for ContentInfo");
        }

        // Parse the content type
        type = typeAndContent[0];
        disType = new DerInputStream(type.toByteArray());
        contentType = disType.getOID();

        if (oldStyle) {
            // JDK1.1.x-style encoding
            content = typeAndContent[1];
        } else {
            // This is the correct, standards-compliant encoding.
            // Parse the content (OPTIONAL field).
            // Skip the [0] EXPLICIT tag by pretending that the content is the
            // one and only element in an implicitly tagged set
            if (typeAndContent.length > 1) { // content is OPTIONAL
                taggedContent = typeAndContent[1];
                disTaggedContent
                    = new DerInputStream(taggedContent.toByteArray());
                contents = disTaggedContent.getSet(1, true);
                if (contents.length != 1) {
                    throw new ParsingException("ContentInfo encoding error");
                }
                content = contents[0];
            }
        }
    }

    public DerValue getContent() {
        return content;
    }

    public ObjectIdentifier getContentType() {
        return contentType;
    }

    public byte[] getData() throws IOException {
        if (contentType.equals(DATA_OID) ||
            contentType.equals(OLD_DATA_OID) ||
            contentType.equals(TIMESTAMP_TOKEN_INFO_OID)) {
            if (content == null)
                return null;
            else
                return content.getOctetString();
        }
        throw new IOException("content type is not DATA: " + contentType);
    }

    public void encode(DerOutputStream out) throws IOException {
        DerOutputStream contentDerCode;
        DerOutputStream seq;

        seq = new DerOutputStream();
        seq.putOID(contentType);

        // content is optional, it could be external
        if (content != null) {
            DerValue taggedContent = null;
            contentDerCode = new DerOutputStream();
            content.encode(contentDerCode);

            // Add the [0] EXPLICIT tag in front of the content encoding
            taggedContent = new DerValue((byte)0xA0,
                                         contentDerCode.toByteArray());
            seq.putDerValue(taggedContent);
        }

        out.write(DerValue.tag_Sequence, seq);
    }

    /**
     * Returns a byte array representation of the data held in
     * the content field.
     */
    public byte[] getContentBytes() throws IOException {
        if (content == null)
            return null;

        DerValue v = new DerValue(content.toByteArray());
        return v.getOctetString();
    }

    public String toString() {
        String out = "";

        out += "Content Info Sequence\n\tContent type: " + contentType + "\n";
        out += "\tContent: " + content;
        return out;
    }
}
