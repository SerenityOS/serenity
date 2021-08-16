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
 * @test 1.2 07/03/29
 * @bug 4634892
 * @summary Ensure that client requesting privacy causes resulting channel to
 * be encrypted.
 */

/*
 * Can set logging to FINEST to view exchange.
 */
import javax.security.sasl.*;
import javax.security.auth.callback.*;
import java.security.Security;
import java.util.*;

public class Privacy {
    private static final String MECH = "DIGEST-MD5";
    private static final String SERVER_FQDN = "machineX.imc.org";
    private static final String PROTOCOL = "jmx";

    private static final byte[] EMPTY = new byte[0];

    private static String pwfile, namesfile, proxyfile;
    private static boolean auto;
    private static boolean verbose = false;

    private static byte[][] clntdata, srvdata;

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

        initData();
    }


    public static void main(String[] args) throws Exception {

        init(args);

        CallbackHandler clntCbh = new ClientCallbackHandler(auto);

        CallbackHandler srvCbh =
            new PropertiesFileCallbackHandler(pwfile, namesfile, proxyfile);

        Map srvProps = new HashMap();
        srvProps.put(Sasl.QOP, "auth-conf");

        Map clntProps = new HashMap();
        clntProps.put(Sasl.QOP, "auth-conf");

        SaslClient clnt = Sasl.createSaslClient(
            new String[]{MECH}, null, PROTOCOL, SERVER_FQDN, clntProps, clntCbh);

        SaslServer srv = Sasl.createSaslServer(MECH, PROTOCOL, SERVER_FQDN,
            srvProps, srvCbh);

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

        /* Use security layer */
        int count = 0;
        for (int i = 0; i < clntStrs.length; i++) {
            byte[] orig = clntdata[i];
            byte[] wrapped = clnt.wrap(clntdata[i], 0, clntdata[i].length);
            byte[] unwrapped = srv.unwrap(wrapped, 0, wrapped.length);

            if (!Arrays.equals(orig, unwrapped)) {
                throw new SaslException("Server cannot unwrap client data");
            }

            byte[] sorig = srvdata[i];
            byte[] swrapped = srv.wrap(srvdata[i], 0, srvdata[i].length);
            byte[] sunwrapped = clnt.unwrap(swrapped, 0, swrapped.length);

            if (!Arrays.equals(sorig, sunwrapped)) {
                throw new SaslException("Client cannot unwrap server data");
            }
            ++count;
        }

        if (verbose) {
            System.out.println(count + " sets of wrap/unwrap between client/server");
        }

        clnt.dispose();
        srv.dispose();
    }

    private static final String[] srvStrs = new String[] {
"A is the 1st letter",
"B is the 2nd letter",
"C is the 3rd letter",
"D is the 4th letter",
"E is the 5th letter",
"F is the 6th letter",
"G is the 7th letter",
"H is the 8th letter",
"I is the 9th letter",
"J is the 10th letter",
"K is the 11th letter",
"L is the 12th letter",
"M is the 13th letter",
    };

    private static final String[] clntStrs = new String[] {
"0",
"1",
"2",
"3",
"4",
"5",
"6",
"7",
"8",
"9",
"10",
"11",
"12",
    };

    private static void initData() {
        clntdata = new byte[clntStrs.length][];
        for (int i = 0; i < clntStrs.length; i++) {
            clntdata[i] = clntStrs[i].getBytes();
        }

        srvdata = new byte[srvStrs.length][];
        for (int i = 0; i < srvStrs.length; i++) {
            srvdata[i] = srvStrs[i].getBytes();
        }
    }
}
