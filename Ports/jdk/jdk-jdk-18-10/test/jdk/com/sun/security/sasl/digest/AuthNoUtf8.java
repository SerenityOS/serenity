/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4634892
 * @summary Ensure that setting com.sun.security.sasl.digest.utf8 to "false"
 *      for the SASL server causes server to not issue a charset=utf-8 directive.
 */
/**
 * Default is to use UTF-8 (server will by default issue charset directive).
 * Can set logging to FINEST to view exchange.
 */

import javax.security.sasl.*;
import javax.security.auth.callback.*;
import java.security.Security;
import java.util.*;

public class AuthNoUtf8 {
    private static final String MECH = "DIGEST-MD5";
    private static final String SERVER_FQDN = "machineX.imc.org";
    private static final String PROTOCOL = "jmx";

    private static final byte[] EMPTY = new byte[0];

    private static String pwfile, namesfile, proxyfile;
    private static boolean auto;
    private static boolean verbose = false;

    private static void init(String[] args) throws Exception {
        if (args.length == 0) {
            pwfile = "pw.properties";
            namesfile = "names.properties";
            auto = true;
        } else {
            int i = 0;
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

        Map props = new HashMap();
        props.put("com.sun.security.sasl.digest.utf8", "false");

        SaslClient clnt = Sasl.createSaslClient(
            new String[]{MECH}, null, PROTOCOL, SERVER_FQDN, null, clntCbh);

        SaslServer srv = Sasl.createSaslServer(MECH, PROTOCOL, SERVER_FQDN, props,
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
            throw new IllegalStateException("FAILURE: mismatched state:" +
                " client complete? " + clnt.isComplete() +
                " server complete? " + srv.isComplete());
        }

        clnt.dispose();
        srv.dispose();
    }
}
