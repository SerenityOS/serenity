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

import java.util.Hashtable;
import org.ietf.jgss.Oid;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.ChannelBinding;
import org.ietf.jgss.MessageProp;
import org.ietf.jgss.GSSException;
import sun.security.jgss.GSSUtil;

/**
 * This class is essentially a JNI calling stub for all wrapper classes.
 *
 * @author Valerie Peng
 * @since 1.6
 */

class GSSLibStub {

    private Oid mech;
    private long pMech; // Warning: used by NativeUtil.c

    /**
     * Initialization routine to dynamically load function pointers.
     *
     * @param lib library name to dlopen
     * @param debug set to true for reporting native debugging info
     * @return true if succeeded, false otherwise.
     */
    static native boolean init(String lib, boolean debug);
    private static native long getMechPtr(byte[] oidDerEncoding);

    // Miscellaneous routines
    static native Oid[] indicateMechs();
    native Oid[] inquireNamesForMech() throws GSSException;

    // Name related routines
    native void releaseName(long pName);
    native long importName(byte[] name, Oid type);
    native boolean compareName(long pName1, long pName2);
    native long canonicalizeName(long pName);
    native byte[] exportName(long pName) throws GSSException;
    native Object[] displayName(long pName) throws GSSException;

    // Credential related routines
    native long acquireCred(long pName, int lifetime, int usage)
                                        throws GSSException;
    native long releaseCred(long pCred);
    native long getCredName(long pCred);
    native int getCredTime(long pCred);
    native int getCredUsage(long pCred);

    // Context related routines
    native NativeGSSContext importContext(byte[] interProcToken);
    native byte[] initContext(long pCred, long targetName, ChannelBinding cb,
                              byte[] inToken, NativeGSSContext context);
    native byte[] acceptContext(long pCred, ChannelBinding cb,
                                byte[] inToken, NativeGSSContext context);
    native long[] inquireContext(long pContext);
    native Oid getContextMech(long pContext);
    native long getContextName(long pContext, boolean isSrc);
    native int getContextTime(long pContext);
    native long deleteContext(long pContext);
    native int wrapSizeLimit(long pContext, int flags, int qop, int outSize);
    native byte[] exportContext(long pContext);
    native byte[] getMic(long pContext, int qop, byte[] msg);
    native void verifyMic(long pContext, byte[] token, byte[] msg,
                          MessageProp prop) ;
    native byte[] wrap(long pContext, byte[] msg, MessageProp prop);
    native byte[] unwrap(long pContext, byte[] msgToken, MessageProp prop);

    private static Hashtable<Oid, GSSLibStub>
        table = new Hashtable<Oid, GSSLibStub>(5);

    static GSSLibStub getInstance(Oid mech) throws GSSException {
        GSSLibStub s = table.get(mech);
        if (s == null) {
            s = new GSSLibStub(mech);
            table.put(mech, s);
        }
        return s;
    }
    private GSSLibStub(Oid mech) throws GSSException {
        SunNativeProvider.debug("Created GSSLibStub for mech " + mech);
        this.mech = mech;
        this.pMech = getMechPtr(mech.getDER());
    }
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof GSSLibStub)) {
            return false;
        }
        return (mech.equals(((GSSLibStub) obj).getMech()));
    }
    public int hashCode() {
        return mech.hashCode();
    }
    Oid getMech() {
        return mech;
    }
}
