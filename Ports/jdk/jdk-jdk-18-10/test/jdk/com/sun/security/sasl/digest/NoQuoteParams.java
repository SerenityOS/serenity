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
 * @bug 6287172
 * @summary SASL + Digest-MD5, charset quoted
 */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.auth.callback.CallbackHandler;

/*
 * According to RFC 2831, DIGEST-MD5 servers must generate challenge strings
 * whose charset and algorithm values are not enclosed within quotes.
 * For example,
 *     challenge: realm="127.0.0.1",nonce="8GBOabRGeIqZB5BiaYJ1NDTuteV+D7n+qbSTH1fo",qop="auth",charset=utf-8,algorithm=md5-sess
 */
public class NoQuoteParams {

    private static Logger logger = Logger.getLogger("global");
    private static final String DIGEST_MD5 = "DIGEST-MD5";
    private static final byte[] EMPTY = new byte[0];

    private static CallbackHandler authCallbackHandler =
        new SampleCallbackHandler();

    public static void main(String[] args) throws Exception {

        Map<String, String> props = new TreeMap<String, String>();
        props.put(Sasl.QOP, "auth");

        // client
        SaslClient client = Sasl.createSaslClient(new String[]{ DIGEST_MD5 },
            "user1", "xmpp", "127.0.0.1", props, authCallbackHandler);
        if (client == null) {
            throw new Exception("Unable to find client implementation for: " +
                DIGEST_MD5);
        }

        byte[] response = client.hasInitialResponse()
            ? client.evaluateChallenge(EMPTY) : EMPTY;
        logger.info("initial: " + new String(response));

        // server
        byte[] challenge = null;
        SaslServer server = Sasl.createSaslServer(DIGEST_MD5, "xmpp",
          "127.0.0.1", props, authCallbackHandler);
        if (server == null) {
            throw new Exception("Unable to find server implementation for: " +
                DIGEST_MD5);
        }

        if (!client.isComplete() || !server.isComplete()) {
            challenge = server.evaluateResponse(response);

            logger.info("challenge: " + new String(challenge));

            if (challenge != null) {
                response = client.evaluateChallenge(challenge);
            }
        }

        String challengeString = new String(challenge, "UTF-8").toLowerCase();

        if (challengeString.indexOf("\"md5-sess\"") > 0 ||
            challengeString.indexOf("\"utf-8\"") > 0) {
            throw new Exception("The challenge string's charset and " +
                "algorithm values must not be enclosed within quotes");
        }

        client.dispose();
        server.dispose();
    }
}

class SampleCallbackHandler implements CallbackHandler {

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
                    cb.setText(getInput(cb.getPrompt()));

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
