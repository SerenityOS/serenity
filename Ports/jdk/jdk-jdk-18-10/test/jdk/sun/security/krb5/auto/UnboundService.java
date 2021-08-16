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
 * @bug 8001104 8194486
 * @summary Unbound SASL service: the GSSAPI/krb5 mech
 * @library /test/lib
 * @compile -XDignore.symbol.file UnboundService.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts UnboundService null null
 * @run main/othervm -Djdk.net.hosts.file=TestHosts UnboundService
 *      server/host.rabbit.hole null
 * @run main/othervm -Djdk.net.hosts.file=TestHosts UnboundService
 *      server/host.rabbit.hole@RABBIT.HOLE null
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts UnboundService
 *      backend/host.rabbit.hole null
 * @run main/othervm -Djdk.net.hosts.file=TestHosts UnboundService
 *      null server@host.rabbit.hole
 * @run main/othervm -Djdk.net.hosts.file=TestHosts UnboundService
 *      server/host.rabbit.hole server@host.rabbit.hole
 * @run main/othervm -Djdk.net.hosts.file=TestHosts UnboundService
 *      server/host.rabbit.hole@RABBIT.HOLE server@host.rabbit.hole
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts UnboundService
 *      backend/host.rabbit.hole server@host.rabbit.hole
 */

import java.io.File;
import java.io.FileOutputStream;
import sun.security.jgss.GSSUtil;

public class UnboundService {

    /**
     * @param args JAAS config pricipal and GSSCredential creation name
     */
    public static void main(String[] args) throws Exception {

        String principal = args[0];
        if (principal.equals("null")) principal = null;

        String server = args[1];
        if (server.equals("null")) server = null;

        new OneKDC(null).writeJAASConf();
        File f = new File(OneKDC.JAAS_CONF);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write((
                "client {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required;\n};\n" +
                "unbound {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required\n" +
                "    useKeyTab=true\n" +
                "    principal=" +
                    (principal==null? "*" :("\"" + principal + "\"")) + "\n" +
                "    doNotPrompt=true\n" +
                "    isInitiator=false\n" +
                "    storeKey=true;\n};\n"
                ).getBytes());
        }

        Context c, s;
        c = Context.fromJAAS("client");
        s = Context.fromJAAS("unbound");

        c.startAsClient(OneKDC.SERVER, GSSUtil.GSS_KRB5_MECH_OID);
        s.startAsServer(server, GSSUtil.GSS_KRB5_MECH_OID);

        Context.handshake(c, s);

        s.dispose();
        c.dispose();
    }
}
