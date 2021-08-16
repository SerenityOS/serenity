/*
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

/*
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal;

import sun.security.util.*;
import java.io.IOException;
import sun.security.krb5.Asn1Exception;
import sun.security.krb5.internal.ccache.CCacheOutputStream;

public class AuthorizationDataEntry implements Cloneable {

    public int adType;
    public byte[] adData;

    private AuthorizationDataEntry() {
    }

    public AuthorizationDataEntry(
            int new_adType,
            byte[] new_adData) {
        adType = new_adType;
        adData = new_adData;
    }

    public Object clone() {
        AuthorizationDataEntry new_authorizationDataEntry =
                new AuthorizationDataEntry();
        new_authorizationDataEntry.adType = adType;
        if (adData != null) {
            new_authorizationDataEntry.adData = new byte[adData.length];
            System.arraycopy(adData, 0,
                    new_authorizationDataEntry.adData, 0, adData.length);
        }
        return new_authorizationDataEntry;
    }

    /**
     * Constructs an instance of AuthorizationDataEntry.
     * @param encoding a single DER-encoded value.
     */
    public AuthorizationDataEntry(DerValue encoding) throws Asn1Exception, IOException {
        DerValue der;
        if (encoding.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        der = encoding.getData().getDerValue();
        if ((der.getTag() & (byte) 0x1F) == (byte) 0x00) {
            adType = der.getData().getBigInteger().intValue();
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        der = encoding.getData().getDerValue();
        if ((der.getTag() & (byte) 0x1F) == (byte) 0x01) {
            adData = der.getData().getOctetString();
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        if (encoding.getData().available() > 0) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
    }

    /**
     * Encodes an AuthorizationDataEntry object.
     * @return byte array of encoded AuthorizationDataEntry object.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream bytes = new DerOutputStream();
        DerOutputStream temp = new DerOutputStream();
        temp.putInteger(adType);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x00), temp);
        temp = new DerOutputStream();
        temp.putOctetString(adData);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x01), temp);
        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, bytes);
        return temp.toByteArray();
    }

    /**
     * Writes the entry's data fields in FCC format to an output stream.
     *
     * @param cos a <code>CCacheOutputStream</code>.
     * @exception IOException if an I/O exception occurs.
     */
    public void writeEntry(CCacheOutputStream cos) throws IOException {
        cos.write16(adType);
        cos.write32(adData.length);
        cos.write(adData, 0, adData.length);
    }

    public String toString() {
        return ("adType=" + adType + " adData.length=" + adData.length);
    }
}
