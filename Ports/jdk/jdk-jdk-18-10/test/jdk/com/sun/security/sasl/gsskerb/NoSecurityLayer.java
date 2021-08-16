/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4873552
 * @summary GSS-API/krb5 SASL mechanism should throw IllegalStateException
 *     for auth-only
 * @run main/manual NoSecurityLayer
 */

/*
 * Set logging to FINEST to view exchange.
 * See run-nosec-wjaas.csh for instructions for how to run this test.
 */

import javax.security.sasl.*;
import javax.security.auth.callback.*;
import java.security.*;
import javax.security.auth.Subject;
import javax.security.auth.login.*;
import com.sun.security.auth.callback.*;
import java.util.HashMap;

public class NoSecurityLayer {
    private static final String MECH = "GSSAPI";
    private static final String SERVER_FQDN = "anti.imc.org";
    private static final String PROTOCOL = "sample";

    private static String namesfile, proxyfile;
    private static final byte[] EMPTY = new byte[0];
    private static boolean auto;
    private static boolean verbose = false;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            namesfile = null;
            auto = true;
        } else {
            int i = 0;
            if (args[i].equals("-m")) {
                i++;
                auto = false;
            }
            if (args.length > i) {
                namesfile = args[i++];
                if (args.length > i) {
                    proxyfile = args[i];
                }
            } else {
                namesfile = null;
            }
        }

        CallbackHandler clntCbh = null;
        final CallbackHandler srvCbh = new PropertiesFileCallbackHandler(
            null, namesfile, proxyfile);

        Subject clntSubj = doLogin("client");
        Subject srvSubj = doLogin("server");
        final HashMap clntprops = new HashMap();
        final HashMap srvprops = new HashMap();

        clntprops.put(Sasl.QOP, "auth");
        srvprops.put(Sasl.QOP, "auth,auth-int,auth-conf");

        final SaslClient clnt = (SaslClient)
            Subject.doAs(clntSubj, new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    return Sasl.createSaslClient(
                        new String[]{MECH}, null, PROTOCOL, SERVER_FQDN,
                        clntprops, null);
                }
            });

        if (verbose) {
            System.out.println(clntSubj);
            System.out.println(srvSubj);
        }
        final SaslServer srv = (SaslServer)
            Subject.doAs(srvSubj, new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    return Sasl.createSaslServer(MECH, PROTOCOL, SERVER_FQDN,
                        srvprops, srvCbh);
                }
            });


        if (clnt == null) {
            throw new IllegalStateException(
                "Unable to find client impl for " + MECH);
        }
        if (srv == null) {
            throw new IllegalStateException(
                "Unable to find server impl for " + MECH);
        }

        byte[] response;
        byte[] challenge;

        response = (byte[]) Subject.doAs(clntSubj,
            new PrivilegedExceptionAction() {
            public Object run() throws Exception {
                return (clnt.hasInitialResponse()? clnt.evaluateChallenge(EMPTY) : EMPTY);
            }});

        while (!clnt.isComplete() || !srv.isComplete()) {
            final byte[] responseCopy = response;
            challenge = (byte[]) Subject.doAs(srvSubj,
                new PrivilegedExceptionAction() {
                public Object run() throws Exception {
                    return srv.evaluateResponse(responseCopy);
                }});

            if (challenge != null) {
                final byte[] challengeCopy = challenge;
                response = (byte[]) Subject.doAs(clntSubj,
                    new PrivilegedExceptionAction() {
                    public Object run() throws Exception {
                        return clnt.evaluateChallenge(challengeCopy);
                    }});
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

        if (verbose) {
            System.out.println(clnt.getNegotiatedProperty(Sasl.QOP));
        }

        // Now try to use security layer

        byte[] clntBuf = new byte[]{0, 1, 2, 3};
        try {
            byte[] wrapped = clnt.wrap(clntBuf, 0, clntBuf.length);
            throw new Exception(
                "clnt wrap should not be allowed w/no security layer");
        } catch (IllegalStateException e) {
            // expected
        }

        byte[] srvBuf = new byte[]{10, 11, 12, 13};
        try {
            byte[] wrapped = srv.wrap(srvBuf, 0, srvBuf.length);
            throw new Exception(
                "srv wrap should not be allowed w/no security layer");
        } catch (IllegalStateException e) {
            // expected
        }

        try {
            byte[] unwrapped = clnt.unwrap(clntBuf, 0, clntBuf.length);
            throw new Exception(
                "clnt wrap should not be allowed w/no security layer");
        } catch (IllegalStateException e) {
            // expected
        }

        try {
            byte[] unwrapped = srv.unwrap(srvBuf, 0, srvBuf.length);
            throw new Exception(
                "srv wrap should not be allowed w/no security layer");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    private static Subject doLogin(String msg) throws LoginException {
        LoginContext lc = null;
        if (verbose) {
            System.out.println(msg);
        }
        try {
            lc = new LoginContext(msg, new TextCallbackHandler());

            // Attempt authentication
            // You might want to do this in a "for" loop to give
            // user more than one chance to enter correct username/password
            lc.login();

        } catch (LoginException le) {
            throw le;
        }
        return lc.getSubject();
    }
}
