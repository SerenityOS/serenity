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
 * @compile -XDignore.symbol.file SaslUnbound.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts SaslUnbound 0
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts SaslUnbound 1
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts SaslUnbound 2
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts SaslUnbound 3
 * @run main/othervm/fail -Djdk.net.hosts.file=TestHosts SaslUnbound 4
 */
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.*;

public class SaslUnbound {

    public static void main(String[] args) throws Exception {

        String serverProtocol, serverName;
        switch (args[0].charAt(0)) {
            case '1':       // Using another protocol, should fail
                serverProtocol = "serv";
                serverName = null;
                break;
            case '2':       // Using another protocol, should fail
                serverProtocol = "otherwise";
                serverName = null;
                break;
            case '3':       // Using another protocol, should fail
                serverProtocol = "otherwise";
                serverName = "host." + OneKDC.REALM;
                break;
            case '4':       // Bound to another serverName, should fail.
                serverProtocol = "server";
                serverName = "host2." + OneKDC.REALM;
                break;
            default:        // Good unbound server
                serverProtocol = "server";
                serverName = null;
                break;
        }
        new OneKDC(null).writeJAASConf();
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

        HashMap clntprops = new HashMap();
        clntprops.put(Sasl.QOP, "auth-conf");
        SaslClient sc = Sasl.createSaslClient(
                new String[]{"GSSAPI"}, null, "server",
                "host." + OneKDC.REALM, clntprops, null);

        final HashMap srvprops = new HashMap();
        srvprops.put(Sasl.QOP, "auth,auth-int,auth-conf");
        SaslServer ss = Sasl.createSaslServer("GSSAPI", serverProtocol,
                serverName, srvprops,
                new CallbackHandler() {
                    public void handle(Callback[] callbacks)
                            throws IOException, UnsupportedCallbackException {
                        for (Callback cb : callbacks) {
                            if (cb instanceof RealmCallback) {
                                ((RealmCallback) cb).setText(OneKDC.REALM);
                            } else if (cb instanceof AuthorizeCallback) {
                                ((AuthorizeCallback) cb).setAuthorized(true);
                            }
                        }
                    }
                });

        byte[] token = new byte[0];
        while (!sc.isComplete() || !ss.isComplete()) {
            if (!sc.isComplete()) {
                token = sc.evaluateChallenge(token);
            }
            if (!ss.isComplete()) {
                token = ss.evaluateResponse(token);
            }
        }
        System.out.println(ss.getNegotiatedProperty(Sasl.BOUND_SERVER_NAME));
        byte[] hello = "hello".getBytes();
        token = sc.wrap(hello, 0, hello.length);
        token = ss.unwrap(token, 0, token.length);
        if (!Arrays.equals(hello, token)) {
            throw new Exception("Message altered");
        }
    }
}
