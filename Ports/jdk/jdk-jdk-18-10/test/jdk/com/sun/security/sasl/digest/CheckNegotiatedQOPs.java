/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5091174
 * @summary DigestMD5Server does not return correct value for getNegotiatedProperty(Sasl.QOP)
 */

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.auth.callback.CallbackHandler;

public final class CheckNegotiatedQOPs {

    static final String DIGEST_MD5 = "DIGEST-MD5";
    private final int caseNumber;
    private final String requestedQOPs;
    private final String supportedQOPs;
    private final SampleClient client;
    private final SampleServer server;

    public static void main(String[] args) throws Exception {

        new CheckNegotiatedQOPs(1,  "auth",      "auth-conf,auth-int,auth")
            .execute(false);
        new CheckNegotiatedQOPs(2,  "auth-int",  "auth-conf,auth-int,auth")
            .execute(false);
        new CheckNegotiatedQOPs(3,  "auth-conf", "auth-conf,auth-int,auth")
            .execute(false);
        new CheckNegotiatedQOPs(4,  "auth",      "auth-int,auth")
            .execute(false);
        new CheckNegotiatedQOPs(5,  "auth-int",  "auth-int,auth")
            .execute(false);
        new CheckNegotiatedQOPs(6,  "auth-conf", "auth-int,auth")
            .execute(true);
        new CheckNegotiatedQOPs(7,  "auth",      "auth")
            .execute(false);
        new CheckNegotiatedQOPs(8,  "auth-int",  "auth")
            .execute(true);
        new CheckNegotiatedQOPs(9,  "auth-conf", "auth")
            .execute(true);
        new CheckNegotiatedQOPs(10, "auth",      null)
            .execute(false);
        new CheckNegotiatedQOPs(11, "auth-int",  null)
            .execute(true);
        new CheckNegotiatedQOPs(12, "auth-conf", null)
            .execute(true);
    }

    private CheckNegotiatedQOPs(int caseNumber, String requestedQOPs,
        String supportedQOPs) throws SaslException {

      this.caseNumber = caseNumber;
      this.requestedQOPs = requestedQOPs;
      this.supportedQOPs = supportedQOPs;
      this.client = new SampleClient(requestedQOPs);
      this.server = new SampleServer(supportedQOPs);
    }

    private void execute(boolean expectException) throws Exception {

        System.err.println ("Case #" + caseNumber);
        System.err.println ("client requested QOPs=" + requestedQOPs);
        System.err.println ("server supported QOPs=" + supportedQOPs);

        try {
            client.negotiate(server);

            if (expectException) {
                throw new
                    Exception("An exception was expected but none was thrown");
            }

        } catch (SaslException e) {

            if (expectException) {
                System.err.println(e);
                return;

            } else {
                throw e;
            }
        }

        System.err.println("client negotiated QOP=" +
            client.getSaslClient ().getNegotiatedProperty (Sasl.QOP));

        System.err.println("server negotiated QOP=" +
            server.getSaslServer ().getNegotiatedProperty (Sasl.QOP));

        System.err.println();
    }

private final class SampleCallbackHandler implements CallbackHandler {

    public void handle(Callback[] callbacks)
        throws java.io.IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof NameCallback) {
                    NameCallback cb = (NameCallback)callbacks[i];
                    cb.setName(getInput(cb.getPrompt()));

                } else if (callbacks[i] instanceof PasswordCallback) {
                    PasswordCallback cb = (PasswordCallback)callbacks[i];

                    String pw = getInput(cb.getPrompt());
                    char[] passwd = new char[pw.length()];
                    pw.getChars(0, passwd.length, passwd, 0);

                    cb.setPassword(passwd);

                } else if (callbacks[i] instanceof RealmCallback) {
                    RealmCallback cb = (RealmCallback)callbacks[i];
                    //cb.setText(getInput(cb.getPrompt()));
                    cb.setText("127.0.0.1");

                } else if (callbacks[i] instanceof AuthorizeCallback) {
                    AuthorizeCallback cb = (AuthorizeCallback)callbacks[i];
                    cb.setAuthorized(true);

                } else {
                    throw new UnsupportedCallbackException(callbacks[i]);
                }
            }
    }

    /**
     * In real world apps, this would typically be a TextComponent or
     * similar widget.
     */
    private String getInput(String prompt) throws IOException {
        return "dummy-value";
    }
}

private final class SampleClient {

    private final SaslClient saslClient;

    public SampleClient(String requestedQOPs) throws SaslException {

        Map<String,String> properties = new HashMap<String,String>();

        if (requestedQOPs != null) {
            properties.put(Sasl.QOP, requestedQOPs);
        }
        saslClient = Sasl.createSaslClient(new String[]{ DIGEST_MD5 }, null,
            "local", "127.0.0.1", properties, new SampleCallbackHandler());
    }

    public SaslClient getSaslClient() {
        return saslClient;
    }

    public void negotiate(SampleServer server) throws SaslException {

        byte[] challenge;
        byte[] response;

        response = (saslClient.hasInitialResponse () ?
                  saslClient.evaluateChallenge (new byte [0]) : new byte [0]);

        while (! saslClient.isComplete()) {
            challenge = server.evaluate(response);
            response = saslClient.evaluateChallenge(challenge);
        }
   }
}

private final class SampleServer {

    private final SaslServer saslServer;

    public SampleServer(String supportedQOPs) throws SaslException {

        Map<String,String> properties = new HashMap<String,String>();

        if (supportedQOPs != null) {
            properties.put(Sasl.QOP, supportedQOPs);
        }
        saslServer = Sasl.createSaslServer(DIGEST_MD5, "local", "127.0.0.1",
            properties, new SampleCallbackHandler());
    }

    public SaslServer getSaslServer() {
        return saslServer;
    }

    public byte[] evaluate(byte[] response) throws SaslException {

      if (saslServer.isComplete()) {
         throw new SaslException ("Server is already complete");
      }

      return saslServer.evaluateResponse(response);
   }
}
}
