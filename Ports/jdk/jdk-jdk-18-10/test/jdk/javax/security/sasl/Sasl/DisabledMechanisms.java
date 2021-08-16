/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8200400
 * @modules java.security.sasl
 * @library /test/lib
 * @run main/othervm DisabledMechanisms
 *      DIGEST-MD5 DIGEST-MD5
 * @run main/othervm -DdisabledMechanisms= DisabledMechanisms
 *      DIGEST-MD5 DIGEST-MD5
 * @run main/othervm -DdisabledMechanisms=DIGEST-MD5,NTLM DisabledMechanisms
 *      null null
 * @run main/othervm -DdisabledMechanisms=DIGEST-MD5 DisabledMechanisms
 *      NTLM null
 * @run main/othervm -DdisabledMechanisms=NTLM DisabledMechanisms
 *      DIGEST-MD5 DIGEST-MD5
 */

import java.security.Security;
import java.util.Collections;
import java.util.Map;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslServer;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;

import jdk.test.lib.Asserts;

public class DisabledMechanisms {

    public static void main(String[] args) throws Exception {

        String authorizationId = "username";
        String protocol = "ldap";
        String serverName = "server1";
        Map props = Collections.emptyMap();

        String disabled = System.getProperty("disabledMechanisms");
        if (disabled != null) {
            Security.setProperty("jdk.sasl.disabledMechanisms", disabled);
        }

        CallbackHandler callbackHandler = callbacks -> {
            for (Callback cb : callbacks) {
                if (cb instanceof PasswordCallback) {
                    ((PasswordCallback) cb).setPassword("password".toCharArray());
                }
            }
        };

        SaslClient client = Sasl.createSaslClient(
                new String[]{"DIGEST-MD5", "NTLM"}, authorizationId,
                protocol, serverName, props, callbackHandler);
        Asserts.assertEQ(client == null ? null : client.getMechanismName(),
                args[0].equals("null") ? null : args[0]);

        SaslServer server = Sasl.createSaslServer(
                "DIGEST-MD5", protocol, serverName, props, callbackHandler);
        Asserts.assertEQ(server == null ? null : server.getMechanismName(),
                args[1].equals("null") ? null : args[1]);
    }
}
