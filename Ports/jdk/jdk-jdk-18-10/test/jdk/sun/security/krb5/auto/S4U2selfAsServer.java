/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6355584 8194486
 * @summary Introduce constrained Kerberos delegation
 * @library /test/lib
 * @compile -XDignore.symbol.file S4U2selfAsServer.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts S4U2selfAsServer krb5
 * @run main/othervm -Djdk.net.hosts.file=TestHosts S4U2selfAsServer spnego
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ietf.jgss.Oid;
import sun.security.jgss.GSSUtil;

public class S4U2selfAsServer {

    public static void main(String[] args) throws Exception {
        Oid mech;
        if (args[0].equals("spnego")) {
            mech = GSSUtil.GSS_SPNEGO_MECH_OID;
        } else if (args[0].contains("krb5")) {
            mech = GSSUtil.GSS_KRB5_MECH_OID;
        } else {
            throw new Exception("Unknown mech");
        }

        OneKDC kdc = new OneKDC(null);
        kdc.writeJAASConf();
        kdc.setOption(KDC.Option.PREAUTH_REQUIRED, false);
        Map<String,List<String>> map = new HashMap<>();
        map.put(OneKDC.SERVER + "@" + OneKDC.REALM, Arrays.asList(
                new String[]{OneKDC.BACKEND + "@" + OneKDC.REALM}));
        kdc.setOption(KDC.Option.ALLOW_S4U2PROXY, map);
        kdc.setOption(KDC.Option.ALLOW_S4U2SELF, Arrays.asList(
                new String[]{OneKDC.SERVER + "@" + OneKDC.REALM}));

        Context s, b;
        s = Context.fromJAAS("server");
        b = Context.fromJAAS("backend");

        s.startAsServer(null, mech, false);

        Context p = s.impersonate(OneKDC.USER);

        p.startAsClient(OneKDC.BACKEND, mech);
        b.startAsServer(mech);
        Context.handshake(p, b);

        p.startAsClient(OneKDC.BACKEND, mech);
        b.startAsServer(mech);
        Context.handshake(p, b);
    }
}
