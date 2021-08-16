/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8000653 8194486
 * @summary SPNEGO tests fail at context.getDelegCred().getRemainingInitLifetime(mechOid)
 * @library /test/lib
 * @compile -XDignore.symbol.file SpnegoLifeTime.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts SpnegoLifeTime
 */

import org.ietf.jgss.Oid;
import org.ietf.jgss.GSSCredential;
import sun.security.jgss.GSSUtil;

public class SpnegoLifeTime {

    public static void main(String[] args) throws Exception {

        Oid oid = GSSUtil.GSS_SPNEGO_MECH_OID;
        new OneKDC(null).writeJAASConf();

        Context c, s;
        c = Context.fromJAAS("client");
        s = Context.fromJAAS("server");

        c.startAsClient(OneKDC.SERVER, oid);
        c.x().requestCredDeleg(true);
        s.startAsServer(oid);

        Context.handshake(c, s);

        GSSCredential cred = s.delegated().cred();
        cred.getRemainingInitLifetime(oid);
        cred.getUsage(oid);
    }
}

