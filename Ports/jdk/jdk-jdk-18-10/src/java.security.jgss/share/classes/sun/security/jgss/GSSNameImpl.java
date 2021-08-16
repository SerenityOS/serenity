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

package sun.security.jgss;

import org.ietf.jgss.*;
import sun.security.jgss.spi.*;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.io.IOException;
import sun.security.util.ObjectIdentifier;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This is the implementation class for GSSName. Conceptually the
 * GSSName is a container with mechanism specific name elements. Each
 * name element is a representation of how that particular mechanism
 * would canonicalize this principal.
 *
 * Generally a GSSName is created by an application when it supplies
 * a sequence of bytes and a nametype that helps each mechanism
 * decide how to interpret those bytes.
 *
 * It is not necessary to create name elements for each available
 * mechanism at the time the application creates the GSSName. This
 * implementation does this lazily, as and when name elements for
 * mechanisms are required to be handed out. (Generally, other GSS
 * classes like GSSContext and GSSCredential request specific
 * elements depending on the mechanisms that they are dealing with.)
 * Assume that getting a mechanism to parse the applciation specified
 * bytes is an expensive call.
 *
 * When a GSSName is canonicalized wrt some mechanism, it is supposed
 * to discard all elements of other mechanisms and retain only the
 * element for this mechanism. In GSS terminology this is called a
 * Mechanism Name or MN. This implementation tries to retain the
 * application provided bytes and name type just in case the MN is
 * asked to produce an element for a mechanism that is different.
 *
 * When a GSSName is to be exported, the name element for the desired
 * mechanism is converted to a byte representation and written
 * out. It might happen that a name element for that mechanism cannot
 * be obtained. This happens when the mechanism is just not supported
 * in this GSS-API or when the mechanism is supported but bytes
 * corresponding to the nametypes that it understands are not
 * available in this GSSName.
 *
 * This class is safe for sharing. Each retrieval of a name element
 * from getElement() might potentially add a new element to the
 * hashmap of elements, but getElement() is synchronized.
 *
 * @author Mayank Upadhyay
 * @since 1.4
 */

public class GSSNameImpl implements GSSName {

    /**
     * The old Oid used in RFC 2853. Now supported as
     * input parameters in:
     *
     * 1. The four overloaded GSSManager.createName(*) methods
     * 2. GSSManager.getMechsForName(Oid)
     *
     * Note that even if a GSSName is created with this old Oid,
     * its internal name type and getStringNameType() output are
     * always the new value.
     */
    static final Oid oldHostbasedServiceName;

    static {
        Oid tmp = null;
        try {
            tmp = new Oid("1.3.6.1.5.6.2");
        } catch (Exception e) {
            // should never happen
        }
        oldHostbasedServiceName = tmp;
    }

    private GSSManagerImpl gssManager = null;

    /*
     * Store whatever the application passed in. We will use this to
     * get individual mechanisms to create name elements as and when
     * needed.
     * Store both the String and the byte[]. Leave I18N to the
     * mechanism by allowing it to extract bytes from the String!
     */

    private String appNameStr = null;
    private byte[] appNameBytes = null;
    private Oid appNameType = null;

    /*
     * When we figure out what the printable name would be, we store
     * both the name and its type.
     */

    private String printableName = null;
    private Oid printableNameType = null;

    private HashMap<Oid, GSSNameSpi> elements = null;
    private GSSNameSpi mechElement = null;

    static GSSNameImpl wrapElement(GSSManagerImpl gssManager,
        GSSNameSpi mechElement) throws GSSException {
        return (mechElement == null ?
            null : new GSSNameImpl(gssManager, mechElement));
    }

    GSSNameImpl(GSSManagerImpl gssManager, GSSNameSpi mechElement) {
        this.gssManager = gssManager;
        appNameStr = printableName = mechElement.toString();
        appNameType = printableNameType = mechElement.getStringNameType();
        this.mechElement = mechElement;
        elements = new HashMap<Oid, GSSNameSpi>(1);
        elements.put(mechElement.getMechanism(), this.mechElement);
    }

    GSSNameImpl(GSSManagerImpl gssManager,
                       Object appName,
                       Oid appNameType)
        throws GSSException {
        this(gssManager, appName, appNameType, null);
    }

    GSSNameImpl(GSSManagerImpl gssManager,
                        Object appName,
                        Oid appNameType,
                        Oid mech)
        throws GSSException {

        if (oldHostbasedServiceName.equals(appNameType)) {
            appNameType = GSSName.NT_HOSTBASED_SERVICE;
        }
        if (appName == null)
            throw new GSSExceptionImpl(GSSException.BAD_NAME,
                                   "Cannot import null name");
        if (mech == null) mech = ProviderList.DEFAULT_MECH_OID;
        if (NT_EXPORT_NAME.equals(appNameType)) {
            importName(gssManager, appName);
        } else {
            init(gssManager, appName, appNameType, mech);
        }
    }

    private void init(GSSManagerImpl gssManager,
                      Object appName, Oid appNameType,
                      Oid mech)
        throws GSSException {

        this.gssManager = gssManager;
        this.elements =
                new HashMap<Oid, GSSNameSpi>(gssManager.getMechs().length);

        if (appName instanceof String) {
            this.appNameStr = (String) appName;
            /*
             * If appNameType is null, then the nametype for this printable
             * string is determined only by interrogating the
             * mechanism. Thus, defer the setting of printableName and
             * printableNameType till later.
             */
            if (appNameType != null) {
                printableName = appNameStr;
                printableNameType = appNameType;
            }
        } else {
            this.appNameBytes = (byte[]) appName;
        }

        this.appNameType = appNameType;

        mechElement = getElement(mech);

        /*
         * printableName will be null if appName was in a byte[] or if
         * appName was in a String but appNameType was null.
         */
        if (printableName == null) {
            printableName = mechElement.toString();
            printableNameType = mechElement.getStringNameType();
        }

        /*
         *  At this point the GSSNameImpl has the following set:
         *   appNameStr or appNameBytes
         *   appNameType (could be null)
         *   printableName
         *   printableNameType
         *   mechElement (which also exists in the hashmap of elements)
         */
    }

    private void importName(GSSManagerImpl gssManager,
                            Object appName)
        throws GSSException {

        int pos = 0;
        byte[] bytes = null;

        if (appName instanceof String) {
            bytes = ((String) appName).getBytes(UTF_8);
        } else {
            bytes = (byte[]) appName;
        }

        if ((bytes[pos++] != 0x04) ||
            (bytes[pos++] != 0x01))
            throw new GSSExceptionImpl(GSSException.BAD_NAME,
                                   "Exported name token id is corrupted!");

        int oidLen  = (((0xFF & bytes[pos++]) << 8) |
                       (0xFF & bytes[pos++]));
        ObjectIdentifier temp = null;
        try {
            DerInputStream din = new DerInputStream(bytes, pos,
                                                    oidLen);
            temp = new ObjectIdentifier(din);
        } catch (IOException e) {
            throw new GSSExceptionImpl(GSSException.BAD_NAME,
                       "Exported name Object identifier is corrupted!");
        }
        Oid oid = new Oid(temp.toString());
        pos += oidLen;
        int mechPortionLen = (((0xFF & bytes[pos++]) << 24) |
                              ((0xFF & bytes[pos++]) << 16) |
                              ((0xFF & bytes[pos++]) << 8) |
                              (0xFF & bytes[pos++]));

        if (mechPortionLen < 0 || pos > bytes.length - mechPortionLen) {
            throw new GSSExceptionImpl(GSSException.BAD_NAME,
                    "Exported name mech name is corrupted!");
        }
        byte[] mechPortion = new byte[mechPortionLen];
        System.arraycopy(bytes, pos, mechPortion, 0, mechPortionLen);

        init(gssManager, mechPortion, NT_EXPORT_NAME, oid);
    }

    public GSSName canonicalize(Oid mech) throws GSSException {
        if (mech == null) mech = ProviderList.DEFAULT_MECH_OID;

        return wrapElement(gssManager, getElement(mech));
    }

    /**
     * This method may return false negatives. But if it says two
     * names are equals, then there is some mechanism that
     * authenticates them as the same principal.
     */
    public boolean equals(GSSName other) throws GSSException {

        if (this.isAnonymous() || other.isAnonymous())
            return false;

        if (other == this)
            return true;

        if (! (other instanceof GSSNameImpl))
            return equals(gssManager.createName(other.toString(),
                                                other.getStringNameType()));

        /*
         * XXX Do a comparison of the appNameStr/appNameBytes if
         * available. If that fails, then proceed with this test.
         */

        GSSNameImpl that = (GSSNameImpl) other;

        GSSNameSpi myElement = this.mechElement;
        GSSNameSpi element = that.mechElement;

        /*
         * XXX If they are not of the same mechanism type, convert both to
         * Kerberos since it is guaranteed to be present.
         */
        if ((myElement == null) && (element != null)) {
            myElement = this.getElement(element.getMechanism());
        } else if ((myElement != null) && (element == null)) {
            element = that.getElement(myElement.getMechanism());
        }

        if (myElement != null && element != null) {
            return myElement.equals(element);
        }

        if ((this.appNameType != null) &&
            (that.appNameType != null)) {
            if (!this.appNameType.equals(that.appNameType)) {
                return false;
            }
            byte[] myBytes =
                    (this.appNameStr != null ?
                     this.appNameStr.getBytes(UTF_8) :
                     this.appNameBytes);
            byte[] bytes =
                    (that.appNameStr != null ?
                     that.appNameStr.getBytes(UTF_8) :
                     that.appNameBytes);
            return Arrays.equals(myBytes, bytes);
        }

        return false;

    }

    /**
     * Returns a hashcode value for this GSSName.
     *
     * @return a hashCode value
     */
    public int hashCode() {
        /*
         * XXX
         * In order to get this to work reliably and properly(!), obtain a
         * Kerberos name element for the name and then call hashCode on its
         * string representation. But this cannot be done if the nametype
         * is not one of those supported by the Kerberos provider and hence
         * this name cannot be imported by Kerberos. In that case return a
         * constant value!
         */

        return 1;
    }

    public boolean equals(Object another) {

        try {
            // XXX This can lead to an infinite loop. Extract info
            // and create a GSSNameImpl with it.

            if (another instanceof GSSName)
                return equals((GSSName) another);
        } catch (GSSException e) {
            // Squelch it and return false
        }

            return false;
    }

    /**
     * Returns a flat name representation for this object. The name
     * format is defined in RFC 2743:
     *<pre>
     * Length           Name          Description
     * 2               TOK_ID          Token Identifier
     *                                 For exported name objects, this
     *                                 must be hex 04 01.
     * 2               MECH_OID_LEN    Length of the Mechanism OID
     * MECH_OID_LEN    MECH_OID        Mechanism OID, in DER
     * 4               NAME_LEN        Length of name
     * NAME_LEN        NAME            Exported name; format defined in
     *                                 applicable mechanism draft.
     *</pre>
     *
     * Note that it is not required to canonicalize a name before
     * calling export(). i.e., the name need not be an MN. If it is
     * not an MN, an implementation defined algorithm can be used for
     * choosing the mechanism which should export this name.
     *
     * @return the flat name representation for this object
     * @exception GSSException with major codes NAME_NOT_MN, BAD_NAME,
     *  BAD_NAME, FAILURE.
     */
    public byte[] export() throws GSSException {

        if (mechElement == null) {
            /* Use default mech */
            mechElement = getElement(ProviderList.DEFAULT_MECH_OID);
        }

        byte[] mechPortion = mechElement.export();
        byte[] oidBytes = null;
        ObjectIdentifier oid = null;

        try {
            oid = ObjectIdentifier.of
                (mechElement.getMechanism().toString());
        } catch (IOException e) {
            throw new GSSExceptionImpl(GSSException.FAILURE,
                                       "Invalid OID String ");
        }
        DerOutputStream dout = new DerOutputStream();
        try {
            dout.putOID(oid);
        } catch (IOException e) {
            throw new GSSExceptionImpl(GSSException.FAILURE,
                                   "Could not ASN.1 Encode "
                                   + oid.toString());
        }
        oidBytes = dout.toByteArray();

        byte[] retVal = new byte[2
                                + 2 + oidBytes.length
                                + 4 + mechPortion.length];
        int pos = 0;
        retVal[pos++] = 0x04;
        retVal[pos++] = 0x01;
        retVal[pos++] = (byte) (oidBytes.length>>>8);
        retVal[pos++] = (byte) oidBytes.length;
        System.arraycopy(oidBytes, 0, retVal, pos, oidBytes.length);
        pos += oidBytes.length;
        retVal[pos++] = (byte) (mechPortion.length>>>24);
        retVal[pos++] = (byte) (mechPortion.length>>>16);
        retVal[pos++] = (byte) (mechPortion.length>>>8);
        retVal[pos++] = (byte)  mechPortion.length;
        System.arraycopy(mechPortion, 0, retVal, pos, mechPortion.length);
        return retVal;
    }

    public String toString() {
         return printableName;

    }

    public Oid getStringNameType() throws GSSException {
        return printableNameType;
    }

    public boolean isAnonymous() {
        if (printableNameType == null) {
            return false;
        } else {
            return GSSName.NT_ANONYMOUS.equals(printableNameType);
        }
    }

    public boolean isMN() {
        return true; // Since always canonicalized for some mech
    }

    public synchronized GSSNameSpi getElement(Oid mechOid)
        throws GSSException {

        GSSNameSpi retVal = elements.get(mechOid);

        if (retVal == null) {
            if (appNameStr != null) {
                retVal = gssManager.getNameElement
                    (appNameStr, appNameType, mechOid);
            } else {
                retVal = gssManager.getNameElement
                    (appNameBytes, appNameType, mechOid);
            }
            elements.put(mechOid, retVal);
        }
        return retVal;
    }

    Set<GSSNameSpi> getElements() {
        return new HashSet<GSSNameSpi>(elements.values());
    }

    private static String getNameTypeStr(Oid nameTypeOid) {

        if (nameTypeOid == null)
            return "(NT is null)";

        if (nameTypeOid.equals(NT_USER_NAME))
            return "NT_USER_NAME";
        if (nameTypeOid.equals(NT_HOSTBASED_SERVICE))
            return "NT_HOSTBASED_SERVICE";
        if (nameTypeOid.equals(NT_EXPORT_NAME))
            return "NT_EXPORT_NAME";
        if (nameTypeOid.equals(GSSUtil.NT_GSS_KRB5_PRINCIPAL))
            return "NT_GSS_KRB5_PRINCIPAL";
        else
            return "Unknown";
    }
}
