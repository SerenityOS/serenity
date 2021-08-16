/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.krb5;

import sun.security.krb5.internal.*;
import sun.security.util.*;
import java.net.*;
import java.util.Vector;
import java.util.Locale;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import sun.security.krb5.internal.ccache.CCacheOutputStream;
import sun.security.krb5.internal.util.KerberosString;


/**
 * Implements the ASN.1 PrincipalName type and its realm in a single class.
 * <pre>{@code
 *    Realm           ::= KerberosString
 *
 *    PrincipalName   ::= SEQUENCE {
 *            name-type       [0] Int32,
 *            name-string     [1] SEQUENCE OF KerberosString
 *    }
 * }</pre>
 * This class is immutable.
 * @see Realm
 */
public class PrincipalName implements Cloneable {

    //name types

    /**
     * Name type not known
     */
    public static final int KRB_NT_UNKNOWN =   0;

    /**
     * Just the name of the principal as in DCE, or for users
     */
    public static final int KRB_NT_PRINCIPAL = 1;

    /**
     * Service and other unique instance (krbtgt)
     */
    public static final int KRB_NT_SRV_INST =  2;

    /**
     * Service with host name as instance (telnet, rcommands)
     */
    public static final int KRB_NT_SRV_HST =   3;

    /**
     * Service with host as remaining components
     */
    public static final int KRB_NT_SRV_XHST =  4;

    /**
     * Unique ID
     */
    public static final int KRB_NT_UID = 5;

    /**
     * Enterprise name (alias)
     */
    public static final int KRB_NT_ENTERPRISE = 10;

    /**
     * TGS Name
     */
    public static final String TGS_DEFAULT_SRV_NAME = "krbtgt";
    public static final int TGS_DEFAULT_NT = KRB_NT_SRV_INST;

    public static final char NAME_COMPONENT_SEPARATOR = '/';
    public static final char NAME_REALM_SEPARATOR = '@';
    public static final char REALM_COMPONENT_SEPARATOR = '.';

    public static final String NAME_COMPONENT_SEPARATOR_STR = "/";
    public static final String NAME_REALM_SEPARATOR_STR = "@";
    public static final String REALM_COMPONENT_SEPARATOR_STR = ".";

    // Instance fields.

    /**
     * The name type, from PrincipalName's name-type field.
     */
    private final int nameType;

    /**
     * The name strings, from PrincipalName's name-strings field. This field
     * must be neither null nor empty. Each entry of it must also be neither
     * null nor empty. Make sure to clone the field when it's passed in or out.
     */
    private final String[] nameStrings;

    /**
     * The realm this principal belongs to.
     */
    private final Realm nameRealm;      // not null


    /**
     * When constructing a PrincipalName, whether the realm is included in
     * the input, or deduced from default realm or domain-realm mapping.
     */
    private final boolean realmDeduced;

    // cached default salt, not used in clone
    private transient String salt = null;

    // There are 3 basic constructors. All other constructors must call them.
    // All basic constructors must call validateNameStrings.
    // 1. From name components
    // 2. From name
    // 3. From DER encoding

    /**
     * Creates a PrincipalName.
     */
    public PrincipalName(int nameType, String[] nameStrings, Realm nameRealm) {
        if (nameRealm == null) {
            throw new IllegalArgumentException("Null realm not allowed");
        }
        validateNameStrings(nameStrings);
        this.nameType = nameType;
        this.nameStrings = nameStrings.clone();
        this.nameRealm = nameRealm;
        this.realmDeduced = false;
    }

    // Warning: called by NativeCreds.c
    public PrincipalName(String[] nameParts, String realm) throws RealmException {
        this(KRB_NT_UNKNOWN, nameParts, new Realm(realm));
    }

    // Validate a nameStrings argument
    private static void validateNameStrings(String[] ns) {
        if (ns == null) {
            throw new IllegalArgumentException("Null nameStrings not allowed");
        }
        if (ns.length == 0) {
            throw new IllegalArgumentException("Empty nameStrings not allowed");
        }
        for (String s: ns) {
            if (s == null) {
                throw new IllegalArgumentException("Null nameString not allowed");
            }
            if (s.isEmpty()) {
                throw new IllegalArgumentException("Empty nameString not allowed");
            }
        }
    }

    public Object clone() {
        try {
            PrincipalName pName = (PrincipalName) super.clone();
            UNSAFE.putReference(this, NAME_STRINGS_OFFSET, nameStrings.clone());
            return pName;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError("Should never happen");
        }
    }

    private static final long NAME_STRINGS_OFFSET;
    private static final jdk.internal.misc.Unsafe UNSAFE;
    static {
        try {
            jdk.internal.misc.Unsafe unsafe = jdk.internal.misc.Unsafe.getUnsafe();
            NAME_STRINGS_OFFSET = unsafe.objectFieldOffset(
                    PrincipalName.class.getDeclaredField("nameStrings"));
            UNSAFE = unsafe;
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof PrincipalName) {
            PrincipalName other = (PrincipalName)o;
            return nameRealm.equals(other.nameRealm) &&
                    Arrays.equals(nameStrings, other.nameStrings);
        }
        return false;
    }

    /**
     * Returns the ASN.1 encoding of the
     * <pre>{@code
     * PrincipalName    ::= SEQUENCE {
     *          name-type       [0] Int32,
     *          name-string     [1] SEQUENCE OF KerberosString
     * }
     *
     * KerberosString   ::= GeneralString (IA5String)
     * }</pre>
     *
     * <p>
     * This definition reflects the Network Working Group RFC 4120
     * specification available at
     * <a href="http://www.ietf.org/rfc/rfc4120.txt">
     * http://www.ietf.org/rfc/rfc4120.txt</a>.
     *
     * @param encoding DER-encoded PrincipalName (without Realm)
     * @param realm the realm for this name
     * @exception Asn1Exception if an error occurs while decoding
     * an ASN1 encoded data.
     * @exception Asn1Exception if there is an ASN1 encoding error
     * @exception IOException if an I/O error occurs
     * @exception IllegalArgumentException if encoding is null
     * reading encoded data.
     */
    public PrincipalName(DerValue encoding, Realm realm)
            throws Asn1Exception, IOException {
        if (realm == null) {
            throw new IllegalArgumentException("Null realm not allowed");
        }
        realmDeduced = false;
        nameRealm = realm;
        DerValue der;
        if (encoding == null) {
            throw new IllegalArgumentException("Null encoding not allowed");
        }
        if (encoding.getTag() != DerValue.tag_Sequence) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        der = encoding.getData().getDerValue();
        if ((der.getTag() & 0x1F) == 0x00) {
            BigInteger bint = der.getData().getBigInteger();
            nameType = bint.intValue();
        } else {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
        der = encoding.getData().getDerValue();
        if ((der.getTag() & 0x01F) == 0x01) {
            DerValue subDer = der.getData().getDerValue();
            if (subDer.getTag() != DerValue.tag_SequenceOf) {
                throw new Asn1Exception(Krb5.ASN1_BAD_ID);
            }
            Vector<String> v = new Vector<>();
            DerValue subSubDer;
            while(subDer.getData().available() > 0) {
                subSubDer = subDer.getData().getDerValue();
                String namePart = new KerberosString(subSubDer).toString();
                v.addElement(namePart);
            }
            nameStrings = new String[v.size()];
            v.copyInto(nameStrings);
            validateNameStrings(nameStrings);
        } else  {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        }
    }

    /**
     * Parse (unmarshal) a <code>PrincipalName</code> from a DER
     * input stream.  This form
     * parsing might be used when expanding a value which is part of
     * a constructed sequence and uses explicitly tagged type.
     *
     * @exception Asn1Exception on error.
     * @param data the Der input stream value, which contains one or
     * more marshaled value.
     * @param explicitTag tag number.
     * @param optional indicate if this data field is optional
     * @param realm the realm for the name
     * @return an instance of <code>PrincipalName</code>, or null if the
     * field is optional and missing.
     */
    public static PrincipalName parse(DerInputStream data,
                                      byte explicitTag, boolean
                                      optional,
                                      Realm realm)
        throws Asn1Exception, IOException, RealmException {

        if ((optional) && (((byte)data.peekByte() & (byte)0x1F) !=
                           explicitTag))
            return null;
        DerValue der = data.getDerValue();
        if (explicitTag != (der.getTag() & (byte)0x1F)) {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        } else {
            DerValue subDer = der.getData().getDerValue();
            if (realm == null) {
                realm = Realm.getDefault();
            }
            return new PrincipalName(subDer, realm);
        }
    }


    // XXX Error checkin consistent with MIT krb5_parse_name
    // Code repetition, realm parsed again by class Realm
    private static String[] parseName(String name) {

        Vector<String> tempStrings = new Vector<>();
        String temp = name;
        int i = 0;
        int componentStart = 0;
        String component;

        while (i < temp.length()) {
            if (temp.charAt(i) == NAME_COMPONENT_SEPARATOR) {
                /*
                 * If this separator is escaped then don't treat it
                 * as a separator
                 */
                if (i > 0 && temp.charAt(i - 1) == '\\') {
                    temp = temp.substring(0, i - 1) +
                        temp.substring(i, temp.length());
                    continue;
                }
                else {
                    if (componentStart <= i) {
                        component = temp.substring(componentStart, i);
                        tempStrings.addElement(component);
                    }
                    componentStart = i + 1;
                }
            } else {
                if (temp.charAt(i) == NAME_REALM_SEPARATOR) {
                    /*
                     * If this separator is escaped then don't treat it
                     * as a separator
                     */
                    if (i > 0 && temp.charAt(i - 1) == '\\') {
                        temp = temp.substring(0, i - 1) +
                            temp.substring(i, temp.length());
                        continue;
                    } else {
                        if (componentStart < i) {
                            component = temp.substring(componentStart, i);
                            tempStrings.addElement(component);
                        }
                        componentStart = i + 1;
                        break;
                    }
                }
            }
            i++;
        }

        if (i == temp.length()) {
            component = temp.substring(componentStart, i);
            tempStrings.addElement(component);
        }

        String[] result = new String[tempStrings.size()];
        tempStrings.copyInto(result);
        return result;
    }

    /**
     * Constructs a PrincipalName from a string.
     * @param name the name
     * @param type the type
     * @param realm the realm, null if not known. Note that when realm is not
     * null, it will be always used even if there is a realm part in name. When
     * realm is null, will read realm part from name, or try to map a realm
     * (for KRB_NT_SRV_HST), or use the default realm, or fail
     * @throws RealmException
     */
    public PrincipalName(String name, int type, String realm)
            throws RealmException {
        if (name == null) {
            throw new IllegalArgumentException("Null name not allowed");
        }
        String[] nameParts = parseName(name);
        validateNameStrings(nameParts);
        if (realm == null) {
            realm = Realm.parseRealmAtSeparator(name);
        }

        // No realm info from parameter and string, must deduce later
        realmDeduced = realm == null;

        switch (type) {
        case KRB_NT_SRV_HST:
            if (nameParts.length >= 2) {
                String hostName = nameParts[1];
                Boolean option;
                try {
                    // If true, try canonicalizing and accept it if it starts
                    // with the short name. Otherwise, never. Default true.
                    option = Config.getInstance().getBooleanObject(
                            "libdefaults", "dns_canonicalize_hostname");
                } catch (KrbException e) {
                    option = null;
                }
                if (option != Boolean.FALSE) {
                    try {
                        // RFC4120 does not recommend canonicalizing a hostname.
                        // However, for compatibility reason, we will try
                        // canonicalizing it and see if the output looks better.

                        String canonicalized = (InetAddress.getByName(hostName)).
                                getCanonicalHostName();

                        // Looks if canonicalized is a longer format of hostName,
                        // we accept cases like
                        //     bunny -> bunny.rabbit.hole
                        if (canonicalized.toLowerCase(Locale.ENGLISH).startsWith(
                                hostName.toLowerCase(Locale.ENGLISH) + ".")) {
                            hostName = canonicalized;
                        }
                    } catch (UnknownHostException | SecurityException e) {
                        // not canonicalized or no permission to do so, use old
                    }
                    if (hostName.endsWith(".")) {
                        hostName = hostName.substring(0, hostName.length() - 1);
                    }
                }
                nameParts[1] = hostName.toLowerCase(Locale.ENGLISH);
            }
            nameStrings = nameParts;
            nameType = type;

            if (realm != null) {
                nameRealm = new Realm(realm);
            } else {
                // We will try to get realm name from the mapping in
                // the configuration. If it is not specified
                // we will use the default realm. This nametype does
                // not allow a realm to be specified. The name string must of
                // the form service@host and this is internally changed into
                // service/host by Kerberos
                String mapRealm =  mapHostToRealm(nameParts[1]);
                if (mapRealm != null) {
                    nameRealm = new Realm(mapRealm);
                } else {
                    nameRealm = Realm.getDefault();
                }
            }
            break;
        case KRB_NT_UNKNOWN:
        case KRB_NT_PRINCIPAL:
        case KRB_NT_SRV_INST:
        case KRB_NT_SRV_XHST:
        case KRB_NT_UID:
        case KRB_NT_ENTERPRISE:
            nameStrings = nameParts;
            nameType = type;
            if (realm != null) {
                nameRealm = new Realm(realm);
            } else {
                nameRealm = Realm.getDefault();
            }
            break;
        default:
            throw new IllegalArgumentException("Illegal name type");
        }
    }

    // Warning: called by nativeccache.c
    public PrincipalName(String name, int type) throws RealmException {
        this(name, type, (String)null);
    }

    public PrincipalName(String name) throws RealmException {
        this(name, KRB_NT_UNKNOWN);
    }

    public PrincipalName(String name, String realm) throws RealmException {
        this(name, KRB_NT_UNKNOWN, realm);
    }

    public static PrincipalName tgsService(String r1, String r2)
            throws KrbException {
        return new PrincipalName(PrincipalName.KRB_NT_SRV_INST,
                new String[] {PrincipalName.TGS_DEFAULT_SRV_NAME, r1},
                new Realm(r2));
    }

    public String getRealmAsString() {
        return getRealmString();
    }

    public String getPrincipalNameAsString() {
        StringBuilder temp = new StringBuilder(nameStrings[0]);
        for (int i = 1; i < nameStrings.length; i++)
            temp.append(nameStrings[i]);
        return temp.toString();
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public String getName() {
        return toString();
    }

    public int getNameType() {
        return nameType;
    }

    public String[] getNameStrings() {
        return nameStrings.clone();
    }

    public byte[][] toByteArray() {
        byte[][] result = new byte[nameStrings.length][];
        for (int i = 0; i < nameStrings.length; i++) {
            result[i] = nameStrings[i].getBytes();
        }
        return result;
    }

    public String getRealmString() {
        return nameRealm.toString();
    }

    public Realm getRealm() {
        return nameRealm;
    }

    public String getSalt() {
        if (salt == null) {
            StringBuilder salt = new StringBuilder();
            salt.append(nameRealm.toString());
            for (int i = 0; i < nameStrings.length; i++) {
                salt.append(nameStrings[i]);
            }
            return salt.toString();
        }
        return salt;
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < nameStrings.length; i++) {
            if (i > 0)
                str.append("/");
            String n = nameStrings[i];
            n = n.replace("@", "\\@");
            str.append(n);
        }
        str.append("@");
        str.append(nameRealm.toString());
        return str.toString();
    }

    public String getNameString() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < nameStrings.length; i++) {
            if (i > 0)
                str.append("/");
            str.append(nameStrings[i]);
        }
        return str.toString();
    }

    /**
     * Encodes a <code>PrincipalName</code> object. Note that only the type and
     * names are encoded. To encode the realm, call getRealm().asn1Encode().
     * @return the byte array of the encoded PrncipalName object.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     *
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream bytes = new DerOutputStream();
        DerOutputStream temp = new DerOutputStream();
        BigInteger bint = BigInteger.valueOf(this.nameType);
        temp.putInteger(bint);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x00), temp);
        temp = new DerOutputStream();
        DerValue[] der = new DerValue[nameStrings.length];
        for (int i = 0; i < nameStrings.length; i++) {
            der[i] = new KerberosString(nameStrings[i]).toDerValue();
        }
        temp.putSequence(der);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte)0x01), temp);
        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, bytes);
        return temp.toByteArray();
    }


    /**
     * Checks if two <code>PrincipalName</code> objects have identical values in their corresponding data fields.
     *
     * @param pname the other <code>PrincipalName</code> object.
     * @return true if two have identical values, otherwise, return false.
     */
    // It is used in <code>sun.security.krb5.internal.ccache</code> package.
    public boolean match(PrincipalName pname) {
        boolean matched = true;
        //name type is just a hint, no two names can be the same ignoring name type.
        // if (this.nameType != pname.nameType) {
        //      matched = false;
        // }
        if ((this.nameRealm != null) && (pname.nameRealm != null)) {
            if (!(this.nameRealm.toString().equalsIgnoreCase(pname.nameRealm.toString()))) {
                matched = false;
            }
        }
        if (this.nameStrings.length != pname.nameStrings.length) {
            matched = false;
        } else {
            for (int i = 0; i < this.nameStrings.length; i++) {
                if (!(this.nameStrings[i].equalsIgnoreCase(pname.nameStrings[i]))) {
                    matched = false;
                }
            }
        }
        return matched;
    }

    /**
     * Writes data field values of <code>PrincipalName</code> in FCC format to an output stream.
     *
     * @param cos a <code>CCacheOutputStream</code> for writing data.
     * @exception IOException if an I/O exception occurs.
     * @see sun.security.krb5.internal.ccache.CCacheOutputStream
     */
    public void writePrincipal(CCacheOutputStream cos) throws IOException {
        cos.write32(nameType);
        cos.write32(nameStrings.length);
        byte[] realmBytes = null;
        realmBytes = nameRealm.toString().getBytes();
        cos.write32(realmBytes.length);
        cos.write(realmBytes, 0, realmBytes.length);
        byte[] bytes = null;
        for (int i = 0; i < nameStrings.length; i++) {
            bytes = nameStrings[i].getBytes();
            cos.write32(bytes.length);
            cos.write(bytes, 0, bytes.length);
        }
    }

    /**
     * Returns the instance component of a name.
     * In a multi-component name such as a KRB_NT_SRV_INST
     * name, the second component is returned.
     * Null is returned if there are not two or more
     * components in the name.
     *
     * @return instance component of a multi-component name.
     */
    public String getInstanceComponent()
    {
        if (nameStrings != null && nameStrings.length >= 2)
            {
                return new String(nameStrings[1]);
            }

        return null;
    }

    static String mapHostToRealm(String name) {
        String result = null;
        try {
            String subname = null;
            Config c = Config.getInstance();
            if ((result = c.get("domain_realm", name)) != null)
                return result;
            else {
                for (int i = 1; i < name.length(); i++) {
                    if ((name.charAt(i) == '.') && (i != name.length() - 1)) { //mapping could be .ibm.com = AUSTIN.IBM.COM
                        subname = name.substring(i);
                        result = c.get("domain_realm", subname);
                        if (result != null) {
                            break;
                        }
                        else {
                            subname = name.substring(i + 1);      //or mapping could be ibm.com = AUSTIN.IBM.COM
                            result = c.get("domain_realm", subname);
                            if (result != null) {
                                break;
                            }
                        }
                    }
                }
            }
        } catch (KrbException e) {
        }
        return result;
    }

    public boolean isRealmDeduced() {
        return realmDeduced;
    }
}
