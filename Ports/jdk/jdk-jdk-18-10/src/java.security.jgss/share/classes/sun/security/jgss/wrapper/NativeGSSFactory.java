/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.security.Provider;
import java.util.Vector;
import org.ietf.jgss.*;
import sun.security.jgss.GSSUtil;
import sun.security.jgss.GSSCaller;
import sun.security.jgss.GSSExceptionImpl;
import sun.security.jgss.spi.*;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * JGSS plugin for generic mechanisms provided through native GSS framework.
 *
 * @author Valerie Peng
 */

public final class NativeGSSFactory implements MechanismFactory {

    GSSLibStub cStub = null;
    private final GSSCaller caller;

    private GSSCredElement getCredFromSubject(GSSNameElement name,
                                              boolean initiate)
        throws GSSException {
        Oid mech = cStub.getMech();
        Vector<GSSCredElement> creds = GSSUtil.searchSubject
            (name, mech, initiate, GSSCredElement.class);

        // If Subject is present but no native creds available
        if (creds != null && creds.isEmpty()) {
            if (GSSUtil.useSubjectCredsOnly(caller)) {
                throw new GSSException(GSSException.NO_CRED);
            }
        }

        GSSCredElement result = ((creds == null || creds.isEmpty()) ?
                                 null : creds.firstElement());
        // Force permission check before returning the cred to caller
        if (result != null) {
            result.doServicePermCheck();
        }
        return result;
    }

    public NativeGSSFactory(GSSCaller caller) {
        this.caller = caller;
        // Have to call setMech(Oid) explicitly before calling other
        // methods. Otherwise, NPE may be thrown unexpectantly
    }

    public void setMech(Oid mech) throws GSSException {
        cStub = GSSLibStub.getInstance(mech);
    }

    public GSSNameSpi getNameElement(String nameStr, Oid nameType)
        throws GSSException {
        byte[] nameBytes =
                (nameStr == null ? null : nameStr.getBytes(UTF_8));
        return new GSSNameElement(nameBytes, nameType, cStub);
    }

    public GSSNameSpi getNameElement(byte[] name, Oid nameType)
        throws GSSException {
        return new GSSNameElement(name, nameType, cStub);
    }

    public GSSCredentialSpi getCredentialElement(GSSNameSpi name,
                                                 int initLifetime,
                                                 int acceptLifetime,
                                                 int usage)
        throws GSSException {
        GSSNameElement nname = null;
        if (name != null && !(name instanceof GSSNameElement)) {
            nname = (GSSNameElement)
                getNameElement(name.toString(), name.getStringNameType());
        } else nname = (GSSNameElement) name;

        if (usage == GSSCredential.INITIATE_AND_ACCEPT) {
            // Force separate acqusition of cred element since
            // MIT's impl does not correctly report NO_CRED error.
            usage = GSSCredential.INITIATE_ONLY;
        }

        GSSCredElement credElement =
            getCredFromSubject(nname, (usage == GSSCredential.INITIATE_ONLY));

        if (credElement == null) {
            // No cred in the Subject
            if (usage == GSSCredential.INITIATE_ONLY) {
                credElement = new GSSCredElement(nname, initLifetime,
                                                 usage, cStub);
            } else if (usage == GSSCredential.ACCEPT_ONLY) {
                if (nname == null) {
                    nname = GSSNameElement.DEF_ACCEPTOR;
                }
                credElement = new GSSCredElement(nname, acceptLifetime,
                                                 usage, cStub);
            } else {
                throw new GSSException(GSSException.FAILURE, -1,
                                       "Unknown usage mode requested");
            }
        }
        return credElement;
    }

    public GSSContextSpi getMechanismContext(GSSNameSpi peer,
                                             GSSCredentialSpi myCred,
                                             int lifetime)
        throws GSSException {
        if (peer == null) {
            throw new GSSException(GSSException.BAD_NAME);
        } else if (!(peer instanceof GSSNameElement)) {
            peer = (GSSNameElement)
                getNameElement(peer.toString(), peer.getStringNameType());
        }
        if (myCred == null) {
            myCred = getCredFromSubject(null, true);
        } else if (!(myCred instanceof GSSCredElement)) {
            throw new GSSException(GSSException.NO_CRED);
        }
        return new NativeGSSContext((GSSNameElement) peer,
                                     (GSSCredElement) myCred,
                                     lifetime, cStub);
    }

    public GSSContextSpi getMechanismContext(GSSCredentialSpi myCred)
        throws GSSException {
        if (myCred == null) {
            myCred = getCredFromSubject(null, false);
        } else if (!(myCred instanceof GSSCredElement)) {
            throw new GSSException(GSSException.NO_CRED);
        }
        return new NativeGSSContext((GSSCredElement) myCred, cStub);
    }

    public GSSContextSpi getMechanismContext(byte[] exportedContext)
        throws GSSException {
        return cStub.importContext(exportedContext);
    }

    public final Oid getMechanismOid() {
        return cStub.getMech();
    }

    public Provider getProvider() {
        return SunNativeProvider.INSTANCE;
    }

    public Oid[] getNameTypes() throws GSSException {
        return cStub.inquireNamesForMech();
    }
}
