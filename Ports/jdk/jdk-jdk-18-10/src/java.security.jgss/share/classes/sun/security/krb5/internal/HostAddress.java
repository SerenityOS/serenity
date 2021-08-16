/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

import sun.security.krb5.Config;
import sun.security.krb5.Asn1Exception;
import sun.security.util.*;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.Arrays;

/**
 * Implements the ASN.1 HostAddress type.
 *
 * <pre>{@code
 * HostAddress     ::= SEQUENCE  {
 *         addr-type       [0] Int32,
 *         address         [1] OCTET STRING
 * }
 * }</pre>
 *
 * <p>
 * This definition reflects the Network Working Group RFC 4120
 * specification available at
 * <a href="http://www.ietf.org/rfc/rfc4120.txt">
 * http://www.ietf.org/rfc/rfc4120.txt</a>.
 */

public class HostAddress implements Cloneable {
    int addrType;
    byte[] address = null;

    private static InetAddress localInetAddress; //caches local inet address
    private static final boolean DEBUG = sun.security.krb5.internal.Krb5.DEBUG;
    private volatile int hashCode = 0;

    private HostAddress(int dummy) {}

    public Object clone() {
        HostAddress new_hostAddress = new HostAddress(0);
        new_hostAddress.addrType = addrType;
        if (address != null) {
            new_hostAddress.address = address.clone();
        }
        return new_hostAddress;
    }


    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37*result + addrType;
            if (address != null) {
                for (int i=0; i < address.length; i++)  {
                    result = 37*result + address[i];
                }
            }
            hashCode = result;
        }
        return hashCode;

    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof HostAddress)) {
            return false;
        }

        HostAddress h = (HostAddress)obj;
        if (addrType != h.addrType ||
            (address != null && h.address == null) ||
            (address == null && h.address != null))
            return false;
        if (address != null && h.address != null) {
            if (address.length != h.address.length)
                return false;
            for (int i = 0; i < address.length; i++)
                if (address[i] != h.address[i])
                    return false;
        }
        return true;
    }

    private static synchronized InetAddress getLocalInetAddress()
        throws UnknownHostException {

        if (localInetAddress == null) {
           localInetAddress = InetAddress.getLocalHost();
        }
        if (localInetAddress == null) {
            throw new UnknownHostException();
        }
        return (localInetAddress);
    }

    /**
     * Gets the InetAddress of this HostAddress.
     * @return the IP address for this specified host.
     * @exception UnknownHostException if no IP address for the host could be found.
     */
    public InetAddress getInetAddress() throws UnknownHostException {
        // the type of internet addresses is 2.
        if (addrType == Krb5.ADDRTYPE_INET ||
            addrType == Krb5.ADDRTYPE_INET6) {
            return (InetAddress.getByAddress(address));
        } else {
            // if it is other type (ISO address, XNS address, etc)
            return null;
        }
    }

    private int getAddrType(InetAddress inetAddress) {
        int addressType = 0;
        if (inetAddress instanceof Inet4Address)
            addressType = Krb5.ADDRTYPE_INET;
        else if (inetAddress instanceof Inet6Address)
            addressType = Krb5.ADDRTYPE_INET6;
        return (addressType);
    }

    // implicit default not in Config.java
    public HostAddress() throws UnknownHostException {
        InetAddress inetAddress = getLocalInetAddress();
        addrType = getAddrType(inetAddress);
        address = inetAddress.getAddress();
    }

    /**
     * Creates a HostAddress from the specified address and address type.
     *
     * Warning: called by nativeccache.c.
     *
     * @param new_addrType the value of the address type which matches the defined
     *                       address family constants in the Berkeley Standard
     *                       Distributions of Unix.
     * @param new_address network address.
     * @exception KrbApErrException if address type and address length do not match defined value.
     */
    public HostAddress(int new_addrType, byte[] new_address)
        throws KrbApErrException, UnknownHostException {
        switch(new_addrType) {
        case Krb5.ADDRTYPE_INET:        //Internet address
            if (new_address.length != 4)
                throw new KrbApErrException(0, "Invalid Internet address");
            break;
        case Krb5.ADDRTYPE_CHAOS:
            if (new_address.length != 2) //CHAOSnet address
                throw new KrbApErrException(0, "Invalid CHAOSnet address");
            break;
        case Krb5.ADDRTYPE_ISO:   // ISO address
            break;
        case Krb5.ADDRTYPE_IPX:   // XNS address
            if (new_address.length != 6)
                throw new KrbApErrException(0, "Invalid XNS address");
            break;
        case Krb5.ADDRTYPE_APPLETALK:  //AppleTalk DDP address
            if (new_address.length != 3)
                throw new KrbApErrException(0, "Invalid DDP address");
            break;
        case Krb5.ADDRTYPE_DECNET:    //DECnet Phase IV address
            if (new_address.length != 2)
                throw new KrbApErrException(0, "Invalid DECnet Phase IV address");
            break;
        case Krb5.ADDRTYPE_INET6:     //Internet IPv6 address
            if (new_address.length != 16)
                throw new KrbApErrException(0, "Invalid Internet IPv6 address");
            break;
        }

        addrType = new_addrType;
        if (new_address != null) {
           address = new_address.clone();
        }
        if (DEBUG) {
            if (addrType == Krb5.ADDRTYPE_INET ||
                    addrType == Krb5.ADDRTYPE_INET6) {
                System.out.println("Host address is " +
                        InetAddress.getByAddress(address));
            }
        }
    }

    public HostAddress(InetAddress inetAddress) {
        addrType = getAddrType(inetAddress);
        address = inetAddress.getAddress();
    }

    /**
     * Constructs a host address from a single DER-encoded value.
     * @param encoding a single DER-encoded value.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public HostAddress(DerValue encoding) throws Asn1Exception, IOException {
        DerValue der = encoding.getData().getDerValue();
        if ((der.getTag() & (byte)0x1F) == (byte)0x00) {
            addrType = der.getData().getBigInteger().intValue();
        }
        else
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        der = encoding.getData().getDerValue();
        if ((der.getTag() & (byte)0x1F) == (byte)0x01) {
            address = der.getData().getOctetString();
        }
        else
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        if (encoding.getData().available() > 0)
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
    }

    /**
     * Encodes a HostAddress object.
     * @return a byte array of encoded HostAddress object.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream bytes = new DerOutputStream();
        DerOutputStream temp = new DerOutputStream();
        temp.putInteger(this.addrType);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x00), temp);
        temp = new DerOutputStream();
        temp.putOctetString(address);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x01), temp);
        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, bytes);
        return temp.toByteArray();
    }

    /**
     * Parses (unmarshal) a host address from a DER input stream.  This form
     * parsing might be used when expanding a value which is part of
     * a constructed sequence and uses explicitly tagged type.
     *
     * @exception Asn1Exception on error.
     * @exception IOException if an I/O error occurs while reading encoded data.
     * @param data the Der input stream value, which contains one or more marshaled value.
     * @param explicitTag tag number.
     * @param optional indicates if this data field is optional
     * @return an instance of HostAddress.
     */
    public static HostAddress parse(DerInputStream data, byte explicitTag,
                                    boolean optional)
        throws Asn1Exception, IOException{
        if ((optional) &&
            (((byte)data.peekByte() & (byte)0x1F) != explicitTag)) {
            return null;
        }
        DerValue der = data.getDerValue();
        if (explicitTag != (der.getTag() & (byte)0x1F))  {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        else {
            DerValue subDer = der.getData().getDerValue();
            return new HostAddress(subDer);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(Arrays.toString(address));
        sb.append('(').append(addrType).append(')');
        return sb.toString();
    }
}
