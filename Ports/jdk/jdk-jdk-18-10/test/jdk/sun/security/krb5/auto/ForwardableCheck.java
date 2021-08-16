/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8022582 8194486
 * @summary Relax response flags checking in sun.security.krb5.KrbKdcRep.check.
 * @library /test/lib
 * @compile -XDignore.symbol.file ForwardableCheck.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts ForwardableCheck
 */

import org.ietf.jgss.GSSException;
import sun.security.jgss.GSSUtil;

import java.util.Arrays;

public class ForwardableCheck {

    public static void main(String[] args) throws Exception {
        OneKDC kdc = new OneKDC(null);
        kdc.writeJAASConf();

        // USER can impersonate someone else
        kdc.setOption(KDC.Option.ALLOW_S4U2SELF,
                Arrays.asList(OneKDC.USER + "@" + OneKDC.REALM));
        // USER2 is sensitive
        kdc.setOption(KDC.Option.SENSITIVE_ACCOUNTS,
                Arrays.asList(OneKDC.USER2 + "@" + OneKDC.REALM));

        Context c;

        // USER2 is sensitive but it's still able to get a normal ticket
        c = Context.fromUserPass(OneKDC.USER2, OneKDC.PASS2, false);

        // ... and connect to another account
        c.startAsClient(OneKDC.USER, GSSUtil.GSS_KRB5_MECH_OID);
        c.x().requestCredDeleg(true);
        c.x().requestMutualAuth(false);

        c.take(new byte[0]);

        if (!c.x().isEstablished()) {
            throw new Exception("Context should have been established");
        }

        // ... but will not be able to delegate itself
        if (c.x().getCredDelegState()) {
            throw new Exception("Impossible");
        }

        // Although USER is allowed to impersonate other people,
        // it cannot impersonate USER2 coz it's sensitive.
        c = Context.fromUserPass(OneKDC.USER, OneKDC.PASS, false);
        try {
            c.impersonate(OneKDC.USER2);
            throw new Exception("Should fail");
        } catch (GSSException e) {
            e.printStackTrace();
        }
    }
}
