/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 * @test
 * @bug 8048194 8242151
 * @modules java.base/sun.security.util
 *          java.security.jgss/sun.security.jgss
 *          java.security.jgss/sun.security.jgss.spnego:+open
 * @run main/othervm NotPreferredMech
 * @summary GSSContext.acceptSecContext fails when a supported mech is not initiator preferred
 */

import org.ietf.jgss.*;
import sun.security.jgss.*;
import sun.security.jgss.spnego.NegTokenInit;
import sun.security.jgss.spnego.NegTokenTarg;
import sun.security.util.BitArray;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class NotPreferredMech {

    public static void main(String[] argv) throws Exception {

        // Generates a NegTokenInit mechTypes field, with an
        // unsupported mech as the preferred.
        DerOutputStream mech = new DerOutputStream();
        mech.write(new Oid("1.2.3.4").getDER());
        mech.write(GSSUtil.GSS_KRB5_MECH_OID.getDER());
        DerOutputStream mechTypeList = new DerOutputStream();
        mechTypeList.write(DerValue.tag_Sequence, mech);

        // Generates a NegTokenInit mechToken field for 1.2.3.4 mech
        GSSHeader h1 = new GSSHeader(ObjectIdentifier.of("1.2.3.4"), 1);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        h1.encode(bout);
        bout.write(new byte[1]);

        // Generates the NegTokenInit token
        Constructor<NegTokenInit> ctor = NegTokenInit.class.getDeclaredConstructor(
                byte[].class, BitArray.class, byte[].class, byte[].class);
        ctor.setAccessible(true);
        NegTokenInit initToken = ctor.newInstance(
                mechTypeList.toByteArray(),
                new BitArray(0),
                bout.toByteArray(),
                null);
        Method m = Class.forName("sun.security.jgss.spnego.SpNegoToken")
                .getDeclaredMethod("getEncoded");
        m.setAccessible(true);
        byte[] spnegoToken = (byte[])m.invoke(initToken);

        // and wraps it into a GSSToken
        GSSHeader h = new GSSHeader(
                ObjectIdentifier.of(GSSUtil.GSS_SPNEGO_MECH_OID.toString()),
                spnegoToken.length);
        bout = new ByteArrayOutputStream();
        h.encode(bout);
        bout.write(spnegoToken);
        byte[] token = bout.toByteArray();

        // and feeds it to a GSS acceptor
        GSSManager man = GSSManager.getInstance();
        GSSContext ctxt = man.createContext((GSSCredential) null);
        token = ctxt.acceptSecContext(token, 0, token.length);
        NegTokenTarg targ = new NegTokenTarg(token);

        // Make sure it's a GO-ON message
        Method m2 = NegTokenTarg.class.getDeclaredMethod("getNegotiatedResult");
        m2.setAccessible(true);
        int negResult = (int)m2.invoke(targ);

        if (negResult != 1 /* ACCEPT_INCOMPLETE */) {
            throw new Exception("Not a continue");
        }
    }
}
