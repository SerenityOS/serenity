/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

import sun.security.action.GetBooleanAction;
import sun.security.krb5.internal.Krb5;
import sun.security.util.*;
import java.io.IOException;
import java.util.*;

import sun.security.krb5.internal.util.KerberosString;

/**
 * Implements the ASN.1 Realm type.
 *
 * {@code Realm ::= GeneralString}
 *
 * This class is immutable.
 */
public class Realm implements Cloneable {

    public static final boolean AUTODEDUCEREALM = GetBooleanAction
            .privilegedGetProperty("sun.security.krb5.autodeducerealm");

    private final String realm; // not null nor empty

    public Realm(String name) throws RealmException {
        realm = parseRealm(name);
    }

    public static Realm getDefault() throws RealmException {
        try {
            return new Realm(Config.getInstance().getDefaultRealm());
        } catch (RealmException re) {
            throw re;
        } catch (KrbException ke) {
            throw new RealmException(ke);
        }
    }

    // Immutable class, no need to clone
    public Object clone() {
        return this;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Realm)) {
            return false;
        }

        Realm that = (Realm)obj;
        return this.realm.equals(that.realm);
    }

    public int hashCode() {
        return realm.hashCode();
    }

    /**
     * Constructs a Realm object.
     * @param encoding a Der-encoded data.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     * @exception RealmException if an error occurs while parsing a Realm object.
     */
    public Realm(DerValue encoding)
        throws Asn1Exception, RealmException, IOException {
        if (encoding == null) {
            throw new IllegalArgumentException("encoding can not be null");
        }
        realm = new KerberosString(encoding).toString();
        if (realm == null || realm.length() == 0)
            throw new RealmException(Krb5.REALM_NULL);
        if (!isValidRealmString(realm))
            throw new RealmException(Krb5.REALM_ILLCHAR);
    }

    public String toString() {
        return realm;
    }

    // Extract realm from a string like dummy@REALM
    public static String parseRealmAtSeparator(String name)
        throws RealmException {
        if (name == null) {
            throw new IllegalArgumentException
                ("null input name is not allowed");
        }
        String temp = new String(name);
        String result = null;
        int i = 0;
        while (i < temp.length()) {
            if (temp.charAt(i) == PrincipalName.NAME_REALM_SEPARATOR) {
                if (i == 0 || temp.charAt(i - 1) != '\\') {
                    if (i + 1 < temp.length()) {
                        result = temp.substring(i + 1, temp.length());
                    } else {
                        throw new IllegalArgumentException
                                ("empty realm part not allowed");
                    }
                    break;
                }
            }
            i++;
        }
        if (result != null) {
            if (result.length() == 0)
                throw new RealmException(Krb5.REALM_NULL);
            if (!isValidRealmString(result))
                throw new RealmException(Krb5.REALM_ILLCHAR);
        }
        return result;
    }

    public static String parseRealmComponent(String name) {
        if (name == null) {
            throw new IllegalArgumentException
                ("null input name is not allowed");
        }
        String temp = new String(name);
        String result = null;
        int i = 0;
        while (i < temp.length()) {
            if (temp.charAt(i) == PrincipalName.REALM_COMPONENT_SEPARATOR) {
                if (i == 0 || temp.charAt(i - 1) != '\\') {
                    if (i + 1 < temp.length())
                        result = temp.substring(i + 1, temp.length());
                    break;
                }
            }
            i++;
        }
        return result;
    }

    protected static String parseRealm(String name) throws RealmException {
        String result = parseRealmAtSeparator(name);
        if (result == null)
            result = name;
        if (result == null || result.length() == 0)
            throw new RealmException(Krb5.REALM_NULL);
        if (!isValidRealmString(result))
            throw new RealmException(Krb5.REALM_ILLCHAR);
        return result;
    }

    // This is protected because the definition of a realm
    // string is fixed
    protected static boolean isValidRealmString(String name) {
        if (name == null)
            return false;
        if (name.length() == 0)
            return false;
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == '/' ||
                name.charAt(i) == '\0') {
                return false;
            }
        }
        return true;
    }

    /**
     * Encodes a Realm object.
     * @return the byte array of encoded KrbCredInfo object.
     * @exception Asn1Exception if an error occurs while decoding an ASN1 encoded data.
     * @exception IOException if an I/O error occurs while reading encoded data.
     *
     */
    public byte[] asn1Encode() throws Asn1Exception, IOException {
        DerOutputStream out = new DerOutputStream();
        out.putDerValue(new KerberosString(this.realm).toDerValue());
        return out.toByteArray();
    }


    /**
     * Parse (unmarshal) a realm from a DER input stream.  This form
     * parsing might be used when expanding a value which is part of
     * a constructed sequence and uses explicitly tagged type.
     *
     * @exception Asn1Exception on error.
     * @param data the Der input stream value, which contains one or more marshaled value.
     * @param explicitTag tag number.
     * @param optional indicate if this data field is optional
     * @return an instance of Realm.
     *
     */
    public static Realm parse(DerInputStream data, byte explicitTag, boolean optional)
            throws Asn1Exception, IOException, RealmException {
        if ((optional) && (((byte)data.peekByte() & (byte)0x1F) != explicitTag)) {
            return null;
        }
        DerValue der = data.getDerValue();
        if (explicitTag != (der.getTag() & (byte)0x1F))  {
            throw new Asn1Exception(Krb5.ASN1_BAD_ID);
        } else {
            DerValue subDer = der.getData().getDerValue();
            return new Realm(subDer);
        }
    }

    /**
     * Returns an array of realms that may be traversed to obtain
     * a TGT from the initiating realm cRealm to the target realm
     * sRealm.
     * <br>
     * This method would read [capaths] to create a path, or generate a
     * hierarchical path if [capaths] does not contain a sub-stanza for cRealm
     * or the sub-stanza does not contain a tag for sRealm.
     * <br>
     * The returned list would never be null, and it always contains
     * cRealm as the head entry. sRealm is not included as the tail.
     *
     * @param cRealm the initiating realm, not null
     * @param sRealm the target realm, not null, not equals to cRealm
     * @return array of realms including at least cRealm as the first
     *         element
     */
    public static String[] getRealmsList(String cRealm, String sRealm) {
        try {
            // Try [capaths]
            return parseCapaths(cRealm, sRealm);
        } catch (KrbException ke) {
            // Now assume the realms are organized hierarchically.
            return parseHierarchy(cRealm, sRealm);
        }
    }

    /**
     * Parses the [capaths] stanza of the configuration file for a
     * list of realms to traverse to obtain credentials from the
     * initiating realm cRealm to the target realm sRealm.
     *
     * For a given client realm C there is a tag C in [capaths] whose
     * subtag S has a value which is a (possibly partial) path from C
     * to S. When the path is partial, it contains only the tail of the
     * full path. Values of other subtags will be used to build the full
     * path. The value "." means a direct path from C to S. If realm S
     * does not appear as a subtag, there is no path defined here.
     *
     * The implementation ignores all values which equals to C or S, or
     * a "." in multiple values, or any duplicated realm names.
     *
     * When a path value has more than two realms, they can be specified
     * with multiple key-value pairs each having a single value, but the
     * order must not change.
     *
     * For example:
     *
     * [capaths]
     *    TIVOLI.COM = {
     *        IBM.COM = IBM_LDAPCENTRAL.COM MOONLITE.ORG
     *        IBM_LDAPCENTRAL.COM = LDAPCENTRAL.NET
     *        LDAPCENTRAL.NET = .
     *    }
     *
     * TIVOLI.COM has a direct path to LDAPCENTRAL.NET, which has a direct
     * path to IBM_LDAPCENTRAL.COM. It also has a partial path to IBM.COM
     * being "IBM_LDAPCENTRAL.COM MOONLITE.ORG". Merging these info together,
     * a full path from TIVOLI.COM to IBM.COM will be
     *
     *   TIVOLI.COM -> LDAPCENTRAL.NET -> IBM_LDAPCENTRAL.COM
     *              -> IBM_LDAPCENTRAL.COM -> MOONLITE.ORG
     *
     * Please note the sRealm IBM.COM does not appear in the path.
     *
     * @param cRealm the initiating realm
     * @param sRealm the target realm, not the same as cRealm
     * @return array of realms including at least cRealm as the first
     *          element
     * @throws KrbException if the config does not contain a sub-stanza
     *          for cRealm in [capaths] or the sub-stanza does not contain
     *          sRealm as a tag
     */
    private static String[] parseCapaths(String cRealm, String sRealm)
            throws KrbException {

        // This line could throw a KrbException
        Config cfg = Config.getInstance();

        if (!cfg.exists("capaths", cRealm, sRealm)) {
            throw new KrbException("No conf");
        }

        LinkedList<String> path = new LinkedList<>();

        String head = sRealm;
        while (true) {
            String value = cfg.getAll("capaths", cRealm, head);
            if (value == null) {
                break;
            }
            String[] more = value.split("\\s+");
            boolean changed = false;
            for (int i=more.length-1; i>=0; i--) {
                if (path.contains(more[i])
                        || more[i].equals(".")
                        || more[i].equals(cRealm)
                        || more[i].equals(sRealm)
                        || more[i].equals(head)) {
                    // Ignore invalid values
                    continue;
                }
                changed = true;
                path.addFirst(more[i]);
            }
            if (!changed) break;
            head = path.getFirst();
        }
        path.addFirst(cRealm);
        return path.toArray(new String[path.size()]);
   }

    /**
     * Build a list of realm that can be traversed
     * to obtain credentials from the initiating realm cRealm
     * for a service in the target realm sRealm.
     * @param cRealm the initiating realm
     * @param sRealm the target realm, not the same as cRealm
     * @return array of realms including cRealm as the first element
     */
    private static String[] parseHierarchy(String cRealm, String sRealm) {

        String[] cComponents = cRealm.split("\\.");
        String[] sComponents = sRealm.split("\\.");

        int cPos = cComponents.length;
        int sPos = sComponents.length;

        boolean hasCommon = false;
        for (sPos--, cPos--; sPos >=0 && cPos >= 0 &&
                sComponents[sPos].equals(cComponents[cPos]);
                sPos--, cPos--) {
            hasCommon = true;
        }

        // For those with common components:
        //                            length  pos
        // SITES1.SALES.EXAMPLE.COM   4       1
        //   EVERYWHERE.EXAMPLE.COM   3       0

        // For those without common components:
        //                     length  pos
        // DEVEL.EXAMPLE.COM   3       2
        // PROD.EXAMPLE.ORG    3       2

        LinkedList<String> path = new LinkedList<>();

        // Un-common ones for client side
        for (int i=0; i<=cPos; i++) {
            path.addLast(subStringFrom(cComponents, i));
        }

        // Common one
        if (hasCommon) {
            path.addLast(subStringFrom(cComponents, cPos+1));
        }

        // Un-common ones for server side
        for (int i=sPos; i>=0; i--) {
            path.addLast(subStringFrom(sComponents, i));
        }

        // Remove sRealm from path. Note that it might be added at last loop
        // or as a common component, if sRealm is a parent of cRealm
        path.removeLast();

        return path.toArray(new String[path.size()]);
    }

    /**
     * Creates a realm name using components from the given position.
     * For example, subStringFrom({"A", "B", "C"}, 1) is "B.C".
     */
    private static String subStringFrom(String[] components, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i=from; i<components.length; i++) {
            if (sb.length() != 0) sb.append('.');
            sb.append(components[i]);
        }
        return sb.toString();
    }
}
