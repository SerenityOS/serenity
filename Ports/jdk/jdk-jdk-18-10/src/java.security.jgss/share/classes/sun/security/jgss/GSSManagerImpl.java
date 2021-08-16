/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
import sun.security.action.GetBooleanAction;
import sun.security.jgss.spi.*;
import java.security.Provider;

/**
 * This class provides the default implementation of the GSSManager
 * interface.
 */
public class GSSManagerImpl extends GSSManager {

    // Undocumented property
    private static final Boolean USE_NATIVE = GetBooleanAction
            .privilegedGetProperty("sun.security.jgss.native");

    private ProviderList list;

    // Used by java SPNEGO impl to make sure native is disabled
    public GSSManagerImpl(GSSCaller caller, boolean useNative) {
        list = new ProviderList(caller, useNative);
    }

    // Used by HTTP/SPNEGO NegotiatorImpl
    public GSSManagerImpl(GSSCaller caller) {
        list = new ProviderList(caller, USE_NATIVE);
    }

    public GSSManagerImpl() {
        list = new ProviderList(GSSCaller.CALLER_UNKNOWN, USE_NATIVE);
    }

    public Oid[] getMechs(){
        return list.getMechs();
    }

    public Oid[] getNamesForMech(Oid mech)
        throws GSSException {
        MechanismFactory factory = list.getMechFactory(mech);
        return factory.getNameTypes().clone();
    }

    public Oid[] getMechsForName(Oid nameType){
        Oid[] mechs = list.getMechs();
        Oid[] retVal = new Oid[mechs.length];
        int pos = 0;

        // Compatibility with RFC 2853 old NT_HOSTBASED_SERVICE value.
        if (nameType.equals(GSSNameImpl.oldHostbasedServiceName)) {
            nameType = GSSName.NT_HOSTBASED_SERVICE;
        }

        // Iterate thru all mechs in GSS
        for (int i = 0; i < mechs.length; i++) {
            // what nametypes does this mech support?
            Oid mech = mechs[i];
            try {
                Oid[] namesForMech = getNamesForMech(mech);
                // Is the desired Oid present in that list?
                if (nameType.containedIn(namesForMech)) {
                    retVal[pos++] = mech;
                }
            } catch (GSSException e) {
                // Squelch it and just skip over this mechanism
                GSSUtil.debug("Skip " + mech +
                              ": error retrieving supported name types");
            }
        }

        // Trim the list if needed
        if (pos < retVal.length) {
            Oid[] temp = new Oid[pos];
            for (int i = 0; i < pos; i++)
                temp[i] = retVal[i];
            retVal = temp;
        }

        return retVal;
    }

    public GSSName createName(String nameStr, Oid nameType)
        throws GSSException {
        return new GSSNameImpl(this, nameStr, nameType);
    }

    public GSSName createName(byte[] name, Oid nameType)
        throws GSSException {
        return new GSSNameImpl(this, name, nameType);
    }

    public GSSName createName(String nameStr, Oid nameType,
                              Oid mech) throws GSSException {
        return new GSSNameImpl(this, nameStr, nameType, mech);
    }

    public GSSName createName(byte[] name, Oid nameType, Oid mech)
        throws GSSException {
        return new GSSNameImpl(this, name, nameType, mech);
    }

    public GSSCredential createCredential(int usage)
        throws GSSException {
        return wrap(new GSSCredentialImpl(this, usage));
    }

    public GSSCredential createCredential(GSSName aName,
                                          int lifetime, Oid mech, int usage)
        throws GSSException {
        return wrap(new GSSCredentialImpl(this, aName, lifetime, mech, usage));
    }

    public GSSCredential createCredential(GSSName aName,
                                          int lifetime, Oid[] mechs, int usage)
        throws GSSException {
        return wrap(new GSSCredentialImpl(this, aName, lifetime, mechs, usage));
    }

    public GSSContext createContext(GSSName peer, Oid mech,
                                    GSSCredential myCred, int lifetime)
        throws GSSException {
        return wrap(new GSSContextImpl(this, peer, mech, myCred, lifetime));
    }

    public GSSContext createContext(GSSCredential myCred)
        throws GSSException {
        return wrap(new GSSContextImpl(this, myCred));
    }

    public GSSContext createContext(byte[] interProcessToken)
        throws GSSException {
        return wrap(new GSSContextImpl(this, interProcessToken));
    }

    public void addProviderAtFront(Provider p, Oid mech)
        throws GSSException {
        list.addProviderAtFront(p, mech);
    }

    public void addProviderAtEnd(Provider p, Oid mech)
        throws GSSException {
        list.addProviderAtEnd(p, mech);
    }

    public GSSCredentialSpi getCredentialElement(GSSNameSpi name, int initLifetime,
                                          int acceptLifetime, Oid mech, int usage)
        throws GSSException {
        MechanismFactory factory = list.getMechFactory(mech);
        return factory.getCredentialElement(name, initLifetime,
                                            acceptLifetime, usage);
    }

    // Used by java SPNEGO impl
    public GSSNameSpi getNameElement(String name, Oid nameType, Oid mech)
        throws GSSException {
        // Just use the most preferred MF impl assuming GSSNameSpi
        // objects are interoperable among providers
        MechanismFactory factory = list.getMechFactory(mech);
        return factory.getNameElement(name, nameType);
    }

    // Used by java SPNEGO impl
    public GSSNameSpi getNameElement(byte[] name, Oid nameType, Oid mech)
        throws GSSException {
        // Just use the most preferred MF impl assuming GSSNameSpi
        // objects are interoperable among providers
        MechanismFactory factory = list.getMechFactory(mech);
        return factory.getNameElement(name, nameType);
    }

    GSSContextSpi getMechanismContext(GSSNameSpi peer,
                                      GSSCredentialSpi myInitiatorCred,
                                      int lifetime, Oid mech)
        throws GSSException {
        Provider p = null;
        if (myInitiatorCred != null) {
            p = myInitiatorCred.getProvider();
        }
        MechanismFactory factory = list.getMechFactory(mech, p);
        return factory.getMechanismContext(peer, myInitiatorCred, lifetime);
    }

    GSSContextSpi getMechanismContext(GSSCredentialSpi myAcceptorCred,
                                      Oid mech)
        throws GSSException {
        Provider p = null;
        if (myAcceptorCred != null) {
            p = myAcceptorCred.getProvider();
        }
        MechanismFactory factory = list.getMechFactory(mech, p);
        return factory.getMechanismContext(myAcceptorCred);
    }

    GSSContextSpi getMechanismContext(byte[] exportedContext)
        throws GSSException {
        if ((exportedContext == null) || (exportedContext.length == 0)) {
            throw new GSSException(GSSException.NO_CONTEXT);
        }
        GSSContextSpi result = null;

        // Only allow context import with native provider since JGSS
        // still has not defined its own interprocess token format
        Oid[] mechs = list.getMechs();
        for (int i = 0; i < mechs.length; i++) {
            MechanismFactory factory = list.getMechFactory(mechs[i]);
            if (factory.getProvider().getName().equals("SunNativeGSS")) {
                result = factory.getMechanismContext(exportedContext);
                if (result != null) break;
            }
        }
        if (result == null) {
            throw new GSSException(GSSException.UNAVAILABLE);
        }
        return result;
    }

    static {
        // Load the extended JGSS interfaces if exist
        try {
            Class.forName("com.sun.security.jgss.Extender");
        } catch (Exception e) {
        }
    }

    static GSSCredential wrap(GSSCredentialImpl cred) {
        return sun.security.jgss.JgssExtender.getExtender().wrap(cred);
    }

    static GSSContext wrap(GSSContextImpl ctxt) {
        return sun.security.jgss.JgssExtender.getExtender().wrap(ctxt);
    }
}
