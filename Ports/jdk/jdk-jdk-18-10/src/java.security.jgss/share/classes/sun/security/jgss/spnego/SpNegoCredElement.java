/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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
package sun.security.jgss.spnego;

import org.ietf.jgss.*;
import java.security.Provider;
import sun.security.jgss.GSSUtil;
import sun.security.jgss.spi.GSSNameSpi;
import sun.security.jgss.spi.GSSCredentialSpi;

/**
 * This class is the cred element implementation for SPNEGO mech.
 * NOTE: The current implementation can only support one mechanism.
 * This should be changed once multi-mechanism support is needed.
 *
 * @author Valerie Peng
 * @since 1.6
 */
public class SpNegoCredElement implements GSSCredentialSpi {

    private GSSCredentialSpi cred = null;

    public SpNegoCredElement(GSSCredentialSpi cred) throws GSSException {
        this.cred = cred;
    }

    Oid getInternalMech() {
        return cred.getMechanism();
    }

    // Used by GSSUtil.populateCredentials()
    public GSSCredentialSpi getInternalCred() {
        return cred;
    }

    public Provider getProvider() {
        return SpNegoMechFactory.PROVIDER;
    }

    public void dispose() throws GSSException {
        cred.dispose();
    }

    public GSSNameSpi getName() throws GSSException {
        return cred.getName();
    }

    public int getInitLifetime() throws GSSException {
        return cred.getInitLifetime();
    }

    public int getAcceptLifetime() throws GSSException {
        return cred.getAcceptLifetime();
    }

    public boolean isInitiatorCredential() throws GSSException {
        return cred.isInitiatorCredential();
    }

    public boolean isAcceptorCredential() throws GSSException {
        return cred.isAcceptorCredential();
    }

    public Oid getMechanism() {
        return GSSUtil.GSS_SPNEGO_MECH_OID;
    }

    @Override
    public GSSCredentialSpi impersonate(GSSNameSpi name) throws GSSException {
        return cred.impersonate(name);
    }
}
