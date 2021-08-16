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
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal;

import sun.security.util.*;
import sun.security.krb5.Asn1Exception;
import java.util.Vector;
import java.io.IOException;

/**
 * Implements the ASN.1 LastReq type.
 *
 * <pre>{@code
 * LastReq         ::=     SEQUENCE OF SEQUENCE {
 *         lr-type         [0] Int32,
 *         lr-value        [1] KerberosTime
 * }
 * }</pre>
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 */

public class LastReq {
    private LastReqEntry[] entry = null;

    public LastReq(LastReqEntry[] entries) throws IOException {
        if (entries != null) {
            entry = new LastReqEntry[entries.length];
            for (int i = 0; i < entries.length; i++) {
                if (entries[i] == null) {
                    throw new IOException("Cannot create a LastReqEntry");
                } else {
                    entry[i] = (LastReqEntry)entries[i].clone();
                }
            }
        }

    }

    /**
     * Constructs a LastReq object.
     * @param encoding a Der-encoded data.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */

    public LastReq(DerValue encoding) throws Asn1Exception, IOException {
        Vector<LastReqEntry> v= new Vector<>();
        if (encoding.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        while (encoding.getData().available() > 0) {
            v.addElement(new LastReqEntry(encoding.getData().getDerValue()));
        }
        if (v.size() > 0) {
            entry = new LastReqEntry[v.size()];
            v.copyInto(entry);
        }
    }

    /**
     * Encodes an LastReq object.
     * @return the byte array of encoded LastReq object.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream bytes = new DerOutputStream();
        if (entry != null && entry.length > 0) {
            DerOutputStream temp = new DerOutputStream();
            for (int i = 0; i < entry.length; i++)
                temp.write(entry[i].asn1Encode());
            bytes.write(DerValue.tag_Sequence, temp);
            return bytes.toByteArray();
        }
        return null;
    }

    /**
     * Parse (unmarshal) a last request from a DER input stream.  This form
     * parsing might be used when expanding a value which is part of
     * a constructed sequence and uses explicitly tagged type.
     *
     * @exception Asn1Exception on error.
     * @param data the Der input stream value, which contains one or more marshaled value.
     * @param explicitTag tag number.
     * @param optional indicates if this data field is optional
     * @return an instance of LastReq.
     *
     */

    public static LastReq parse(DerInputStream data, byte explicitTag, boolean optional) throws Asn1Exception, IOException {
        if ((optional) && (((byte)data.peekByte() & (byte)0x1F) != explicitTag))
            return null;
        DerValue der = data.getDerValue();
        if (explicitTag != (der.getTag() & (byte)0x1F))  {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        else {
            DerValue subDer = der.getData().getDerValue();
            return new LastReq(subDer);
        }
    }

}
