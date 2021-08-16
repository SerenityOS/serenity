/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @author Vincent Ryan
 * @bug 6228412
 * @modules java.security.sasl
 * @summary Check that a Properties object can be passed to the Sasl create
 *          client and create server methods.
 */

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import org.ietf.jgss.GSSException;

public class PassSysProps {

    private static final String PLAIN = "PLAIN";
    private static final String DIGEST = "DIGEST-MD5";
    private static final String CRAM = "CRAM-MD5";
    private static final String EXTERNAL = "EXTERNAL";
    private static final String GSSAPI = "GSSAPI";

    public static void main(String[] args) throws Exception {

        String authorizationId = null;
        String protocol = "ldap";
        String serverName = "server1";

        CallbackHandler callbackHandler = new CallbackHandler(){
            public void handle(Callback[] callbacks) {
            }
        };

        // pass in system properties

        Properties sysprops = System.getProperties();

        SaslClient client1 =
            Sasl.createSaslClient(new String[]{DIGEST, PLAIN}, authorizationId,
                protocol, serverName, (Map) sysprops, callbackHandler);
        System.out.println(client1);

        SaslServer server1 =
            Sasl.createSaslServer(DIGEST, protocol, serverName, (Map) sysprops,
                callbackHandler);
        System.out.println(server1);

        // pass in string-valued props

        Map<String, String> stringProps = new Hashtable<String, String>();
        stringProps.put(Sasl.POLICY_NOPLAINTEXT, "true");

        try {

            SaslClient client2 =
                Sasl.createSaslClient(new String[]{GSSAPI, PLAIN},
                    authorizationId, protocol, serverName, stringProps,
                    callbackHandler);
            System.out.println(client2);

            SaslServer server2 =
                Sasl.createSaslServer(GSSAPI, protocol, serverName,
                    stringProps, callbackHandler);
            System.out.println(server2);

        } catch (SaslException se) {
            Throwable t = se.getCause();
            if (t instanceof GSSException) {
                // allow GSSException because kerberos has not been initialized

            } else {
                throw se;
            }
        }

        // pass in object-valued props

        Map<String, Object> objProps = new Hashtable<String, Object>();
        objProps.put("some.object.valued.property", System.err);

        SaslClient client3 =
            Sasl.createSaslClient(new String[]{EXTERNAL, CRAM}, authorizationId,
                protocol, serverName, objProps, callbackHandler);
        System.out.println(client3);

        SaslServer server3 =
            Sasl.createSaslServer(CRAM, protocol, serverName, objProps,
                callbackHandler);
        System.out.println(server3);

        // pass in raw-type props

        Map rawProps = new Hashtable();
        rawProps.put(Sasl.POLICY_NOPLAINTEXT, "true");
        rawProps.put("some.object.valued.property", System.err);

        SaslClient client4 =
            Sasl.createSaslClient(new String[]{EXTERNAL, CRAM}, authorizationId,
                protocol, serverName, rawProps, callbackHandler);
        System.out.println(client4);

        SaslServer server4 =
            Sasl.createSaslServer(CRAM, protocol, serverName, rawProps,
                callbackHandler);
        System.out.println(server4);

    }
}
