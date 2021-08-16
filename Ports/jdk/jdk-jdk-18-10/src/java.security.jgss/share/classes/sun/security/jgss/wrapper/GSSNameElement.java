/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss.wrapper;

import org.ietf.jgss.*;
import java.security.Provider;
import java.security.Security;
import java.io.IOException;
import sun.security.krb5.Realm;
import sun.security.jgss.GSSUtil;
import sun.security.util.ObjectIdentifier;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.jgss.GSSUtil;
import sun.security.jgss.GSSExceptionImpl;
import sun.security.jgss.spi.GSSNameSpi;

import javax.security.auth.kerberos.ServicePermission;

/**
 * This class is essentially a wrapper class for the gss_name_t
 * structure of the native GSS library.
 * @author Valerie Peng
 * @since 1.6
 */

public class GSSNameElement implements GSSNameSpi {

    long pName = 0; // Pointer to the gss_name_t structure
    private String printableName;
    private Oid printableType;
    private GSSLibStub cStub;

    static final GSSNameElement DEF_ACCEPTOR = new GSSNameElement();

    private static Oid getNativeNameType(Oid nameType, GSSLibStub stub) {
        if (GSSUtil.NT_GSS_KRB5_PRINCIPAL.equals(nameType)) {
            Oid[] supportedNTs = null;
            try {
                supportedNTs = stub.inquireNamesForMech();
            } catch (GSSException ge) {
                if (ge.getMajor() == GSSException.BAD_MECH &&
                    GSSUtil.isSpNegoMech(stub.getMech())) {
                    // Workaround known Heimdal issue and retry with KRB5
                    try {
                        stub = GSSLibStub.getInstance
                            (GSSUtil.GSS_KRB5_MECH_OID);
                        supportedNTs = stub.inquireNamesForMech();
                    } catch (GSSException ge2) {
                        // Should never happen
                        SunNativeProvider.debug("Name type list unavailable: " +
                            ge2.getMajorString());
                    }
                } else {
                    SunNativeProvider.debug("Name type list unavailable: " +
                        ge.getMajorString());
                }
            }
            if (supportedNTs != null) {
                for (int i = 0; i < supportedNTs.length; i++) {
                    if (supportedNTs[i].equals(nameType)) return nameType;
                }
                // Special handling the specified name type
                SunNativeProvider.debug("Override " + nameType +
                    " with mechanism default(null)");
                return null; // Use mechanism specific default
            }
        }
        return nameType;
    }

    private GSSNameElement() {
        printableName = "<DEFAULT ACCEPTOR>";
    }

    // Warning: called by NativeUtil.c
    GSSNameElement(long pNativeName, GSSLibStub stub) throws GSSException {
        assert(stub != null);
        if (pNativeName == 0) {
            throw new GSSException(GSSException.BAD_NAME);
        }
        // Note: pNativeName is assumed to be a MN.
        pName = pNativeName;
        cStub = stub;
        setPrintables();
    }

    GSSNameElement(byte[] nameBytes, Oid nameType, GSSLibStub stub)
        throws GSSException {
        assert(stub != null);
        if (nameBytes == null) {
            throw new GSSException(GSSException.BAD_NAME);
        }
        cStub = stub;
        byte[] name = nameBytes;

        if (nameType != null) {
            // Special handling the specified name type if
            // necessary
            nameType = getNativeNameType(nameType, stub);

            if (GSSName.NT_EXPORT_NAME.equals(nameType)) {
                // Need to add back the mech Oid portion (stripped
                // off by GSSNameImpl class prior to calling this
                // method) for "NT_EXPORT_NAME"
                byte[] mechBytes = null;
                DerOutputStream dout = new DerOutputStream();
                Oid mech = cStub.getMech();
                try {
                    dout.putOID(ObjectIdentifier.of(mech.toString()));
                } catch (IOException e) {
                    throw new GSSExceptionImpl(GSSException.FAILURE, e);
                }
                mechBytes = dout.toByteArray();
                name = new byte[2 + 2 + mechBytes.length + 4 + nameBytes.length];
                int pos = 0;
                name[pos++] = 0x04;
                name[pos++] = 0x01;
                name[pos++] = (byte) (mechBytes.length>>>8);
                name[pos++] = (byte) mechBytes.length;
                System.arraycopy(mechBytes, 0, name, pos, mechBytes.length);
                pos += mechBytes.length;
                name[pos++] = (byte) (nameBytes.length>>>24);
                name[pos++] = (byte) (nameBytes.length>>>16);
                name[pos++] = (byte) (nameBytes.length>>>8);
                name[pos++] = (byte) nameBytes.length;
                System.arraycopy(nameBytes, 0, name, pos, nameBytes.length);
            }
        }
        pName = cStub.importName(name, nameType);
        setPrintables();

        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null && !Realm.AUTODEDUCEREALM) {
            String krbName = getKrbName();
            int atPos = krbName.lastIndexOf('@');
            if (atPos != -1) {
                String atRealm = krbName.substring(atPos);
                // getNativeNameType() can modify NT_GSS_KRB5_PRINCIPAL to null
                if ((nameType == null
                            || nameType.equals(GSSUtil.NT_GSS_KRB5_PRINCIPAL))
                        && new String(nameBytes).endsWith(atRealm)) {
                    // Created from Kerberos name with realm, no need to check
                } else {
                    try {
                        sm.checkPermission(new ServicePermission(atRealm, "-"));
                    } catch (SecurityException se) {
                        // Do not chain the actual exception to hide info
                        throw new GSSException(GSSException.FAILURE);
                    }
                }
            }
        }

        SunNativeProvider.debug("Imported " + printableName + " w/ type " +
                                printableType);
    }

    private void setPrintables() throws GSSException {
        Object[] printables = null;
        printables = cStub.displayName(pName);
        assert((printables != null) && (printables.length == 2));
        printableName = (String) printables[0];
        assert(printableName != null);
        printableType = (Oid) printables[1];
        if (printableType == null) {
            printableType = GSSName.NT_USER_NAME;
        }
    }

    // Need to be public for GSSUtil.getSubject()
    public String getKrbName() throws GSSException {
        long mName = 0;
        GSSLibStub stub = cStub;
        if (!GSSUtil.isKerberosMech(cStub.getMech())) {
            stub = GSSLibStub.getInstance(GSSUtil.GSS_KRB5_MECH_OID);
        }
        mName = stub.canonicalizeName(pName);
        Object[] printables2 = stub.displayName(mName);
        stub.releaseName(mName);
        SunNativeProvider.debug("Got kerberized name: " + printables2[0]);
        return (String) printables2[0];
    }

    public Provider getProvider() {
        return SunNativeProvider.INSTANCE;
    }

    public boolean equals(GSSNameSpi other) throws GSSException {
        if (!(other instanceof GSSNameElement)) {
            return false;
        }
        return cStub.compareName(pName, ((GSSNameElement)other).pName);
    }

    public boolean equals(Object other) {
        if (!(other instanceof GSSNameElement)) {
            return false;
        }
        try {
            return equals((GSSNameElement) other);
        } catch (GSSException ex) {
            return false;
        }
    }

    public int hashCode() {
        return Long.hashCode(pName);
    }

    public byte[] export() throws GSSException {
        byte[] nameVal = cStub.exportName(pName);

        // Need to strip off the mech Oid portion of the exported
        // bytes since GSSNameImpl class will subsequently add it.
        int pos = 0;
        if ((nameVal[pos++] != 0x04) ||
            (nameVal[pos++] != 0x01))
            throw new GSSException(GSSException.BAD_NAME);

        int mechOidLen  = (((0xFF & nameVal[pos++]) << 8) |
                           (0xFF & nameVal[pos++]));
        ObjectIdentifier temp = null;
        try {
            DerInputStream din = new DerInputStream(nameVal, pos,
                                                    mechOidLen);
            temp = new ObjectIdentifier(din);
        } catch (IOException e) {
            throw new GSSExceptionImpl(GSSException.BAD_NAME, e);
        }
        Oid mech2 = new Oid(temp.toString());
        assert(mech2.equals(getMechanism()));
        pos += mechOidLen;
        int mechPortionLen = (((0xFF & nameVal[pos++]) << 24) |
                              ((0xFF & nameVal[pos++]) << 16) |
                              ((0xFF & nameVal[pos++]) << 8) |
                              (0xFF & nameVal[pos++]));
        if (mechPortionLen < 0) {
            throw new GSSException(GSSException.BAD_NAME);
        }
        byte[] mechPortion = new byte[mechPortionLen];
        System.arraycopy(nameVal, pos, mechPortion, 0, mechPortionLen);
        return mechPortion;
    }

    public Oid getMechanism() {
        return cStub.getMech();
    }

    public String toString() {
        return printableName;
    }

    public Oid getStringNameType() {
        return printableType;
    }

    public boolean isAnonymousName() {
        return (GSSName.NT_ANONYMOUS.equals(printableType));
    }

    public void dispose() {
        if (pName != 0) {
            cStub.releaseName(pName);
            pName = 0;
        }
    }

    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        dispose();
    }
}
