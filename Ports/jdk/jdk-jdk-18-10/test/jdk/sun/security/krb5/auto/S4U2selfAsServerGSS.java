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
 * @compile -XDignore.symbol.file S4U2selfAsServerGSS.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts
 *      -Djavax.security.auth.useSubjectCredsOnly=false
 *      S4U2selfAsServerGSS krb5
 * @run main/othervm -Djdk.net.hosts.file=TestHosts
 *      -Djavax.security.auth.useSubjectCredsOnly=false
 *      S4U2selfAsServerGSS spnego
 */

import java.io.File;
import java.io.FileOutputStream;
import java.security.Security;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ietf.jgss.Oid;
import sun.security.jgss.GSSUtil;

public class S4U2selfAsServerGSS {

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
                new String[]{OneKDC.SERVER + "@" + OneKDC.REALM}));
        kdc.setOption(KDC.Option.ALLOW_S4U2PROXY, map);
        kdc.setOption(KDC.Option.ALLOW_S4U2SELF, Arrays.asList(
                new String[]{OneKDC.SERVER + "@" + OneKDC.REALM}));

        Context s, b;
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
        System.setProperty("java.security.auth.login.config", OneKDC.JAAS_CONF);
        File f = new File(OneKDC.JAAS_CONF);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write((
                "com.sun.security.jgss.krb5.accept {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required\n" +
                "    principal=\"" + OneKDC.SERVER + "\"\n" +
                "    useKeyTab=true\n" +
                "    storeKey=true;\n};\n"
                ).getBytes());
        fos.close();
        Security.setProperty("auth.login.defaultCallbackHandler", "OneKDC$CallbackForClient");
        s = Context.fromThinAir();
        b = Context.fromThinAir();
        s.startAsServer(mech);

        Context p = s.impersonate(OneKDC.USER);

        p.startAsClient(OneKDC.SERVER, mech);
        b.startAsServer(mech);
        Context.handshake(p, b);

        String n1 = p.x().getSrcName().toString().split("@")[0];
        String n2 = b.x().getSrcName().toString().split("@")[0];
        if (!n1.equals(OneKDC.USER) || !n2.equals(OneKDC.USER)) {
            throw new Exception("Delegation failed");
        }
    }
}
