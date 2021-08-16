/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7110803
 * @summary SASL service for multiple hostnames
 * @run main Unbound jmx
 * @run main/fail Unbound j
 */
import javax.security.sasl.*;
import javax.security.auth.callback.*;
import java.util.*;

public class Unbound {
    private static final String MECH = "DIGEST-MD5";
    private static final String SERVER_FQDN = "machineX.imc.org";
    private static final String PROTOCOL = "jmx";

    private static final byte[] EMPTY = new byte[0];

    private static String pwfile, namesfile, proxyfile;
    private static boolean auto;
    private static boolean verbose = false;

    private static void init(String[] args) throws Exception {
        if (args.length == 1) {
            pwfile = "pw.properties";
            namesfile = "names.properties";
            auto = true;
        } else {
            int i = 1;
            if (args[i].equals("-m")) {
                i++;
                auto = false;
            }
            if (args.length > i) {
                pwfile = args[i++];

                if (args.length > i) {
                    namesfile = args[i++];

                    if (args.length > i) {
                        proxyfile = args[i];
                    }
                }
            } else {
                pwfile = "pw.properties";
                namesfile = "names.properties";
            }
        }
    }

    public static void main(String[] args) throws Exception {

        init(args);

        CallbackHandler clntCbh = new ClientCallbackHandler(auto);

        CallbackHandler srvCbh =
            new PropertiesFileCallbackHandler(pwfile, namesfile, proxyfile);

        SaslClient clnt = Sasl.createSaslClient(
            new String[]{MECH}, null, PROTOCOL, SERVER_FQDN, null, clntCbh);

        Map props = System.getProperties();
        props.put("com.sun.security.sasl.digest.realm", SERVER_FQDN);

        SaslServer srv = Sasl.createSaslServer(MECH, args[0], null, props,
            srvCbh);

        if (clnt == null) {
            throw new IllegalStateException(
                "Unable to find client impl for " + MECH);
        }
        if (srv == null) {
            throw new IllegalStateException(
                "Unable to find server impl for " + MECH);
        }

        byte[] response = (clnt.hasInitialResponse()?
            clnt.evaluateChallenge(EMPTY) : EMPTY);
        byte[] challenge;

        while (!clnt.isComplete() || !srv.isComplete()) {
            challenge = srv.evaluateResponse(response);

            if (challenge != null) {
                response = clnt.evaluateChallenge(challenge);
            }
        }

        if (clnt.isComplete() && srv.isComplete()) {
            if (verbose) {
                System.out.println("SUCCESS");
                System.out.println("authzid is " + srv.getAuthorizationID());
            }
        } else {
            throw new IllegalStateException(
                "FAILURE: mismatched state:" +
                    " client complete? " + clnt.isComplete() +
                    " server complete? " + srv.isComplete());
        }

        if (!SERVER_FQDN.equalsIgnoreCase((String)
                srv.getNegotiatedProperty(Sasl.BOUND_SERVER_NAME))) {
            throw new Exception("Server side gets wrong requested server name");
        }
        clnt.dispose();
        srv.dispose();
    }
}
