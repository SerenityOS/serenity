/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8160818
 * @summary GssKrb5Client violates RFC 4752
 * @library /test/lib
 * @compile -XDignore.symbol.file SaslMutual.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts SaslMutual
 */
import jdk.test.lib.Asserts;

import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.sasl.*;

public class SaslMutual {

    public static void main(String[] args) throws Exception {

        String name = "host." + OneKDC.REALM_LOWER_CASE;

        new OneKDC(null).writeJAASConf();
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

        SaslClient sc;

        sc = Sasl.createSaslClient(
                new String[]{"GSSAPI"}, null, "server",
                name,
                Map.of(),
                null);
        Asserts.assertEQ(round(sc, server()), 2);

        sc = Sasl.createSaslClient(
                new String[]{"GSSAPI"}, null, "server",
                name,
                Map.of(Sasl.SERVER_AUTH, "true"),
                null);
        Asserts.assertEQ(round(sc, server()), 3);

        sc = Sasl.createSaslClient(
                new String[]{"GSSAPI"}, null, "server",
                name,
                Map.of(Sasl.QOP, "auth-int"),
                null);
        Asserts.assertEQ(round(sc, server()), 3);

        sc = Sasl.createSaslClient(
                new String[]{"GSSAPI"}, null, "server",
                name,
                Map.of(Sasl.QOP, "auth-conf"),
                null);
        Asserts.assertEQ(round(sc, server()), 3);
    }

    static SaslServer server() throws Exception {
        return Sasl.createSaslServer("GSSAPI", "server",
                null,
                Map.of(Sasl.QOP, "auth,auth-int,auth-conf"),
                callbacks -> {
                    for (Callback cb : callbacks) {
                        if (cb instanceof RealmCallback) {
                            ((RealmCallback) cb).setText(OneKDC.REALM);
                        } else if (cb instanceof AuthorizeCallback) {
                            ((AuthorizeCallback) cb).setAuthorized(true);
                        }
                    }
                });
    }

    static int round(SaslClient sc, SaslServer ss) throws Exception {
        int round = 0;
        byte[] token = new byte[0];
        while (!sc.isComplete() || !ss.isComplete()) {
            if (!sc.isComplete()) {
                token = sc.evaluateChallenge(token);
            }
            if (!ss.isComplete()) {
                token = ss.evaluateResponse(token);
            }
            round++;
        }
        return round;
    }
}
