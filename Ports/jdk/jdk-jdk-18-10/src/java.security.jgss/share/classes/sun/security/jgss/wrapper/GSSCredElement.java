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
import sun.security.jgss.GSSUtil;
import sun.security.jgss.spi.GSSCredentialSpi;
import sun.security.jgss.spi.GSSNameSpi;

/**
 * This class is essentially a wrapper class for the gss_cred_id_t
 * structure of the native GSS library.
 * @author Valerie Peng
 * @since 1.6
 */
public class GSSCredElement implements GSSCredentialSpi {

    private int usage;
    long pCred; // Pointer to the gss_cred_id_t structure
    private GSSNameElement name = null;
    private GSSLibStub cStub;

    // Perform the necessary ServicePermission check on this cred
    @SuppressWarnings("removal")
    void doServicePermCheck() throws GSSException {
        if (GSSUtil.isKerberosMech(cStub.getMech())) {
            if (System.getSecurityManager() != null) {
                if (isInitiatorCredential()) {
                    String tgsName = Krb5Util.getTGSName(name);
                    Krb5Util.checkServicePermission(tgsName, "initiate");
                }
                if (isAcceptorCredential() &&
                    name != GSSNameElement.DEF_ACCEPTOR) {
                    String krbName = name.getKrbName();
                    Krb5Util.checkServicePermission(krbName, "accept");
                }
            }
        }
    }

    // Construct delegation cred using the actual context mech and srcName
    // Warning: called by NativeUtil.c
    GSSCredElement(long pCredentials, GSSNameElement srcName, Oid mech)
        throws GSSException {
        pCred = pCredentials;
        cStub = GSSLibStub.getInstance(mech);
        usage = GSSCredential.INITIATE_ONLY;
        name = srcName;
    }

    GSSCredElement(GSSNameElement name, int lifetime, int usage,
                   GSSLibStub stub) throws GSSException {
        cStub = stub;
        this.usage = usage;

        if (name != null) { // Could be GSSNameElement.DEF_ACCEPTOR
            this.name = name;
            doServicePermCheck();
            pCred = cStub.acquireCred(this.name.pName, lifetime, usage);
        } else {
            pCred = cStub.acquireCred(0, lifetime, usage);
            this.name = new GSSNameElement(cStub.getCredName(pCred), cStub);
            doServicePermCheck();
        }
    }

    public Provider getProvider() {
        return SunNativeProvider.INSTANCE;
    }

    public void dispose() throws GSSException {
        name = null;
        if (pCred != 0) {
            pCred = cStub.releaseCred(pCred);
        }
    }

    public GSSNameElement getName() throws GSSException {
        return (name == GSSNameElement.DEF_ACCEPTOR ?
            null : name);
    }

    public int getInitLifetime() throws GSSException {
        if (isInitiatorCredential()) {
            return cStub.getCredTime(pCred);
        } else return 0;
    }

    public int getAcceptLifetime() throws GSSException {
        if (isAcceptorCredential()) {
            return cStub.getCredTime(pCred);
        } else return 0;
    }

    public boolean isInitiatorCredential() {
        return (usage != GSSCredential.ACCEPT_ONLY);
    }

    public boolean isAcceptorCredential() {
        return (usage != GSSCredential.INITIATE_ONLY);
    }

    public Oid getMechanism() {
        return cStub.getMech();
    }

    public String toString() {
        // No hex bytes available for native impl
        return "N/A";
    }

    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        dispose();
    }

    @Override
    public GSSCredentialSpi impersonate(GSSNameSpi name) throws GSSException {
        throw new GSSException(GSSException.FAILURE, -1,
                "Not supported yet");
    }
}
