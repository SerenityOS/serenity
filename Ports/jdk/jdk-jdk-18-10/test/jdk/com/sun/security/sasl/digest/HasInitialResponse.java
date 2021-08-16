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
 * @bug 6682540
 * @summary Incorrect SASL DIGEST-MD5 behavior
 */

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.nio.charset.StandardCharsets;

public class HasInitialResponse {

    private static final String MECH = "DIGEST-MD5";
    private static final String SERVER_FQDN = "machineX.imc.org";
    private static final String PROTOCOL = "jmx";

    private static final byte[] EMPTY = new byte[0];

    public static void main(String[] args) throws Exception {

        CallbackHandler clntCbh = new ClientCallbackHandler(true);
        CallbackHandler srvCbh = new PropertiesFileCallbackHandler(
                "pw.properties", "names.properties", null);

        // Get an existing SaslClient as base of our own client
        SaslClient base = Sasl.createSaslClient(
                new String[]{MECH}, null, PROTOCOL, SERVER_FQDN,
                null, clntCbh);

        // Our own client that has initial response
        SaslClient clnt = new MyDigestMD5Client(base);

        SaslServer srv = Sasl.createSaslServer(MECH, PROTOCOL, SERVER_FQDN,
                null, srvCbh);

        // The usual loop
        byte[] response = clnt.hasInitialResponse()
                ? clnt.evaluateChallenge(EMPTY) : EMPTY;
        byte[] challenge;

        while (!clnt.isComplete() || !srv.isComplete()) {
            challenge = srv.evaluateResponse(response);

            if (challenge != null) {
                response = clnt.evaluateChallenge(challenge);
            }
        }

        if (clnt.isComplete() && srv.isComplete()) {
            System.out.println("SUCCESS");
            System.out.println("authzid is " + srv.getAuthorizationID());
        } else {
            throw new IllegalStateException(
                    "FAILURE: mismatched state:" +
                            " client complete? " + clnt.isComplete() +
                            " server complete? " + srv.isComplete());
        }

        clnt.dispose();
        srv.dispose();
    }

    public static class MyDigestMD5Client implements SaslClient {

        final SaslClient base;
        boolean first = true;

        public MyDigestMD5Client(SaslClient base) {
            this.base = base;
        }

        @Override
        public String getMechanismName() {
            return base.getMechanismName();
        }

        @Override
        public boolean hasInitialResponse() {
            return true; // I have initial response
        }

        @Override
        public byte[] evaluateChallenge(byte[] challenge) throws SaslException {
            if (first) {
                first = false;
                if (challenge.length == 0) {
                    return "hello".getBytes(StandardCharsets.UTF_8);
                } else {
                    throw new SaslException("Non-empty challenge");
                }
            } else {
                return base.evaluateChallenge(challenge);
            }
        }

        @Override
        public boolean isComplete() {
            return base.isComplete();
        }

        @Override
        public byte[] unwrap(byte[] incoming, int offset, int len)
                throws SaslException {
            return base.unwrap(incoming, offset, len);
        }

        @Override
        public byte[] wrap(byte[] outgoing, int offset, int len)
                throws SaslException {
            return base.wrap(outgoing, offset, len);
        }

        @Override
        public Object getNegotiatedProperty(String propName) {
            return base.getNegotiatedProperty(propName);
        }

        @Override
        public void dispose() throws SaslException {
            base.dispose();
        }
    }
}
