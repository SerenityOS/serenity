/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.security.sasl.digest;

import java.security.NoSuchAlgorithmException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.logging.Level;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.security.sasl.*;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * An implementation of the DIGEST-MD5
 * (<a href="http://www.ietf.org/rfc/rfc2831.txt">RFC 2831</a>) SASL
 * (<a href="http://www.ietf.org/rfc/rfc2222.txt">RFC 2222</a>) mechanism.
 *
 * The DIGEST-MD5 SASL mechanism specifies two modes of authentication.
 * - Initial Authentication
 * - Subsequent Authentication - optional, (currently unsupported)
 *
 * Required callbacks:
 * - RealmChoiceCallback
 *    shows user list of realms server has offered; handler must choose one
 *    from list
 * - RealmCallback
 *    shows user the only realm server has offered or none; handler must
 *    enter realm to use
 * - NameCallback
 *    handler must enter username to use for authentication
 * - PasswordCallback
 *    handler must enter password for username to use for authentication
 *
 * Environment properties that affect behavior of implementation:
 *
 * javax.security.sasl.qop
 *    quality of protection; list of auth, auth-int, auth-conf; default is "auth"
 * javax.security.sasl.strength
 *    auth-conf strength; list of high, medium, low; default is highest
 *    available on platform ["high,medium,low"].
 *    high means des3 or rc4 (128); medium des or rc4-56; low is rc4-40;
 *    choice of cipher depends on its availablility on platform
 * javax.security.sasl.maxbuf
 *    max receive buffer size; default is 65536
 * javax.security.sasl.sendmaxbuffer
 *    max send buffer size; default is 65536; (min with server max recv size)
 *
 * com.sun.security.sasl.digest.cipher
 *    name a specific cipher to use; setting must be compatible with the
 *    setting of the javax.security.sasl.strength property.
 *
 * @see <a href="http://www.ietf.org/rfc/rfc2222.txt">RFC 2222</a>
 * - Simple Authentication and Security Layer (SASL)
 * @see <a href="http://www.ietf.org/rfc/rfc2831.txt">RFC 2831</a>
 * - Using Digest Authentication as a SASL Mechanism
 * @see <a href="http://java.sun.com/products/jce">Java(TM)
 * Cryptography Extension 1.2.1 (JCE)</a>
 * @see <a href="http://java.sun.com/products/jaas">Java(TM)
 * Authentication and Authorization Service (JAAS)</a>
 *
 * @author Jonathan Bruce
 * @author Rosanna Lee
 */
final class DigestMD5Client extends DigestMD5Base implements SaslClient {
    private static final String MY_CLASS_NAME = DigestMD5Client.class.getName();

    // Property for specifying cipher explicitly
    private static final String CIPHER_PROPERTY =
        "com.sun.security.sasl.digest.cipher";

    /* Directives encountered in challenges sent by the server. */
    private static final String[] DIRECTIVE_KEY = {
        "realm",      // >= 0 times
        "qop",        // atmost once; default is "auth"
        "algorithm",  // exactly once
        "nonce",      // exactly once
        "maxbuf",     // atmost once; default is 65536
        "charset",    // atmost once; default is ISO 8859-1
        "cipher",     // exactly once if qop is "auth-conf"
        "rspauth",    // exactly once in 2nd challenge
        "stale",      // atmost once for in subsequent auth (not supported)
    };

    /* Indices into DIRECTIVE_KEY */
    private static final int REALM = 0;
    private static final int QOP = 1;
    private static final int ALGORITHM = 2;
    private static final int NONCE = 3;
    private static final int MAXBUF = 4;
    private static final int CHARSET = 5;
    private static final int CIPHER = 6;
    private static final int RESPONSE_AUTH = 7;
    private static final int STALE = 8;

    private int nonceCount; // number of times nonce has been used/seen

    /* User-supplied/generated information */
    private String specifiedCipher;  // cipher explicitly requested by user
    private byte[] cnonce;        // client generated nonce
    private String username;
    private char[] passwd;
    private byte[] authzidBytes;  // byte repr of authzid

    /**
     * Constructor for DIGEST-MD5 mechanism.
     *
     * @param authzid A non-null String representing the principal
     * for which authorization is being granted..
     * @param digestURI A non-null String representing detailing the
     * combined protocol and host being used for authentication.
     * @param props The possibly null properties to be used by the SASL
     * mechanism to configure the authentication exchange.
     * @param cbh The non-null CallbackHanlder object for callbacks
     * @throws SaslException if no authentication ID or password is supplied
     */
    DigestMD5Client(String authzid, String protocol, String serverName,
        Map<String, ?> props, CallbackHandler cbh) throws SaslException {

        super(props, MY_CLASS_NAME, 2, protocol + "/" + serverName, cbh);

        // authzID can only be encoded in UTF8 - RFC 2222
        if (authzid != null) {
            this.authzid = authzid;
            authzidBytes = authzid.getBytes(UTF_8);
        }

        if (props != null) {
            specifiedCipher = (String)props.get(CIPHER_PROPERTY);

            logger.log(Level.FINE, "DIGEST60:Explicitly specified cipher: {0}",
                specifiedCipher);
        }
   }

    /**
     * DIGEST-MD5 has no initial response
     *
     * @return false
     */
    public boolean hasInitialResponse() {
        return false;
    }

    /**
     * Process the challenge data.
     *
     * The server sends a digest-challenge which the client must reply to
     * in a digest-response. When the authentication is complete, the
     * completed field is set to true.
     *
     * @param challengeData A non-null byte array containing the challenge
     * data from the server.
     * @return A possibly null byte array containing the response to
     * be sent to the server.
     *
     * @throws SaslException If the platform does not have MD5 digest support
     * or if the server sends an invalid challenge.
     */
    public byte[] evaluateChallenge(byte[] challengeData) throws SaslException {

        if (challengeData.length > MAX_CHALLENGE_LENGTH) {
            throw new SaslException(
                "DIGEST-MD5: Invalid digest-challenge length. Got:  " +
                challengeData.length + " Expected < " + MAX_CHALLENGE_LENGTH);
        }

        /* Extract and process digest-challenge */
        byte[][] challengeVal;

        switch (step) {
        case 2:
            /* Process server's first challenge (from Step 1) */
            /* Get realm, qop, maxbuf, charset, algorithm, cipher, nonce
               directives */
            List<byte[]> realmChoices = new ArrayList<byte[]>(3);
            challengeVal = parseDirectives(challengeData, DIRECTIVE_KEY,
                realmChoices, REALM);

            try {
                processChallenge(challengeVal, realmChoices);
                checkQopSupport(challengeVal[QOP], challengeVal[CIPHER]);
                ++step;
                return generateClientResponse(challengeVal[CHARSET]);
            } catch (SaslException e) {
                step = 0;
                clearPassword();
                throw e; // rethrow
            } catch (IOException e) {
                step = 0;
                clearPassword();
                throw new SaslException("DIGEST-MD5: Error generating " +
                    "digest response-value", e);
            }

        case 3:
            try {
                /* Process server's step 3 (server response to digest response) */
                /* Get rspauth directive */
                challengeVal = parseDirectives(challengeData, DIRECTIVE_KEY,
                    null, REALM);
                validateResponseValue(challengeVal[RESPONSE_AUTH]);


                /* Initialize SecurityCtx implementation */
                if (integrity && privacy) {
                    secCtx = new DigestPrivacy(true /* client */);
                } else if (integrity) {
                    secCtx = new DigestIntegrity(true /* client */);
                }

                return null; // Mechanism has completed.
            } finally {
                clearPassword();
                step = 0;  // Set to invalid state
                completed = true;
            }

        default:
            // No other possible state
            throw new SaslException("DIGEST-MD5: Client at illegal state");
        }
    }


   /**
    * Record information from the challengeVal array into variables/fields.
    * Check directive values that are multi-valued and ensure that mandatory
    * directives not missing from the digest-challenge.
    *
    * @throws SaslException if a sasl is a the mechanism cannot
    * correcly handle a callbacks or if a violation in the
    * digest challenge format is detected.
    */
    private void processChallenge(byte[][] challengeVal, List<byte[]> realmChoices)
        throws SaslException {

        /* CHARSET: optional atmost once */
        if (challengeVal[CHARSET] != null) {
            if (!"utf-8".equals(new String(challengeVal[CHARSET], encoding))) {
                throw new SaslException("DIGEST-MD5: digest-challenge format " +
                    "violation. Unrecognised charset value: " +
                    new String(challengeVal[CHARSET]));
            } else {
                encoding = UTF_8;
                useUTF8 = true;
            }
        }

        /* ALGORITHM: required exactly once */
        if (challengeVal[ALGORITHM] == null) {
            throw new SaslException("DIGEST-MD5: Digest-challenge format " +
                "violation: algorithm directive missing");
        } else if (!"md5-sess".equals(new String(challengeVal[ALGORITHM], encoding))) {
            throw new SaslException("DIGEST-MD5: Digest-challenge format " +
                "violation. Invalid value for 'algorithm' directive: " +
                challengeVal[ALGORITHM]);
        }

        /* NONCE: required exactly once */
        if (challengeVal[NONCE] == null) {
            throw new SaslException("DIGEST-MD5: Digest-challenge format " +
                "violation: nonce directive missing");
        } else {
            nonce = challengeVal[NONCE];
        }

        try {
            /* REALM: optional, if multiple, stored in realmChoices */
            String[] realmTokens = null;

            if (challengeVal[REALM] != null) {
                if (realmChoices == null || realmChoices.size() <= 1) {
                    // Only one realm specified
                    negotiatedRealm = new String(challengeVal[REALM], encoding);
                } else {
                    realmTokens = new String[realmChoices.size()];
                    for (int i = 0; i < realmTokens.length; i++) {
                        realmTokens[i] =
                            new String(realmChoices.get(i), encoding);
                    }
                }
            }

            NameCallback ncb = authzid == null ?
                new NameCallback("DIGEST-MD5 authentication ID: ") :
                new NameCallback("DIGEST-MD5 authentication ID: ", authzid);
            PasswordCallback pcb =
                new PasswordCallback("DIGEST-MD5 password: ", false);

            if (realmTokens == null) {
                // Server specified <= 1 realm
                // If 0, RFC 2831: the client SHOULD solicit a realm from the user.
                RealmCallback tcb =
                    (negotiatedRealm == null? new RealmCallback("DIGEST-MD5 realm: ") :
                        new RealmCallback("DIGEST-MD5 realm: ", negotiatedRealm));

                cbh.handle(new Callback[] {tcb, ncb, pcb});

                /* Acquire realm from RealmCallback */
                negotiatedRealm = tcb.getText();
                if (negotiatedRealm == null) {
                    negotiatedRealm = "";
                }
            } else {
                RealmChoiceCallback ccb = new RealmChoiceCallback(
                    "DIGEST-MD5 realm: ",
                    realmTokens,
                    0, false);
                cbh.handle(new Callback[] {ccb, ncb, pcb});

                // Acquire realm from RealmChoiceCallback
                int[] selected = ccb.getSelectedIndexes();
                if (selected == null
                        || selected[0] < 0
                        || selected[0] >= realmTokens.length) {
                    throw new SaslException("DIGEST-MD5: Invalid realm chosen");
                }
                negotiatedRealm = realmTokens[selected[0]];
            }

            passwd = pcb.getPassword();
            pcb.clearPassword();
            username = ncb.getName();

        } catch (SaslException se) {
            throw se;

        } catch (UnsupportedCallbackException e) {
            throw new SaslException("DIGEST-MD5: Cannot perform callback to " +
                "acquire realm, authentication ID or password", e);

        } catch (IOException e) {
            throw new SaslException(
                "DIGEST-MD5: Error acquiring realm, authentication ID or password", e);
        }

        if (username == null || passwd == null) {
            throw new SaslException(
                "DIGEST-MD5: authentication ID and password must be specified");
        }

        /* MAXBUF: optional atmost once */
        int srvMaxBufSize =
            (challengeVal[MAXBUF] == null) ? DEFAULT_MAXBUF
            : Integer.parseInt(new String(challengeVal[MAXBUF], encoding));
        sendMaxBufSize =
            (sendMaxBufSize == 0) ? srvMaxBufSize
            : Math.min(sendMaxBufSize, srvMaxBufSize);
    }

    /**
     * Parses the 'qop' directive. If 'auth-conf' is specified by
     * the client and offered as a QOP option by the server, then a check
     * is client-side supported ciphers is performed.
     *
     * @throws IOException
     */
    private void checkQopSupport(byte[] qopInChallenge, byte[] ciphersInChallenge)
        throws IOException {

        /* QOP: optional; if multiple, merged earlier */
        String qopOptions;

        if (qopInChallenge == null) {
            qopOptions = "auth";
        } else {
            qopOptions = new String(qopInChallenge, encoding);
        }

        // process
        String[] serverQopTokens = new String[3];
        byte[] serverQop = parseQop(qopOptions, serverQopTokens,
            true /* ignore unrecognized tokens */);
        byte serverAllQop = combineMasks(serverQop);

        switch (findPreferredMask(serverAllQop, qop)) {
        case 0:
            throw new SaslException("DIGEST-MD5: No common protection " +
                "layer between client and server");

        case NO_PROTECTION:
            negotiatedQop = "auth";
            // buffer sizes not applicable
            break;

        case INTEGRITY_ONLY_PROTECTION:
            negotiatedQop = "auth-int";
            integrity = true;
            rawSendSize = sendMaxBufSize - 16;
            break;

        case PRIVACY_PROTECTION:
            negotiatedQop = "auth-conf";
            privacy = integrity = true;
            rawSendSize = sendMaxBufSize - 26;
            checkStrengthSupport(ciphersInChallenge);
            break;
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "DIGEST61:Raw send size: {0}",
                rawSendSize);
        }
     }

    /**
     * Processes the 'cipher' digest-challenge directive. This allows the
     * mechanism to check for client-side support against the list of
     * supported ciphers send by the server. If no match is found,
     * the mechanism aborts.
     *
     * @throws SaslException If an error is encountered in processing
     * the cipher digest-challenge directive or if no client-side
     * support is found.
     */
    private void checkStrengthSupport(byte[] ciphersInChallenge)
        throws IOException {

        /* CIPHER: required exactly once if qop=auth-conf */
        if (ciphersInChallenge == null) {
            throw new SaslException("DIGEST-MD5: server did not specify " +
                "cipher to use for 'auth-conf'");
        }

        // First determine ciphers that server supports
        String cipherOptions = new String(ciphersInChallenge, encoding);
        StringTokenizer parser = new StringTokenizer(cipherOptions, ", \t\n");
        int tokenCount = parser.countTokens();
        String token = null;
        byte[] serverCiphers = { UNSET,
                                 UNSET,
                                 UNSET,
                                 UNSET,
                                 UNSET };
        String[] serverCipherStrs = new String[serverCiphers.length];

        // Parse ciphers in challenge; mark each that server supports
        for (int i = 0; i < tokenCount; i++) {
            token = parser.nextToken();
            for (int j = 0; j < CIPHER_TOKENS.length; j++) {
                if (token.equals(CIPHER_TOKENS[j])) {
                    serverCiphers[j] |= CIPHER_MASKS[j];
                    serverCipherStrs[j] = token; // keep for replay to server
                    logger.log(Level.FINE, "DIGEST62:Server supports {0}", token);
                }
            }
        }

        // Determine which ciphers are available on client
        byte[] clntCiphers = getPlatformCiphers();

        // Take intersection of server and client supported ciphers
        byte inter = 0;
        for (int i = 0; i < serverCiphers.length; i++) {
            serverCiphers[i] &= clntCiphers[i];
            inter |= serverCiphers[i];
        }

        if (inter == UNSET) {
            throw new SaslException(
                "DIGEST-MD5: Client supports none of these cipher suites: " +
                cipherOptions);
        }

        // now have a clear picture of user / client; client / server
        // cipher options. Leverage strength array against what is
        // supported to choose a cipher.
        negotiatedCipher = findCipherAndStrength(serverCiphers, serverCipherStrs);

        if (negotiatedCipher == null) {
            throw new SaslException("DIGEST-MD5: Unable to negotiate " +
                "a strength level for 'auth-conf'");
        }
        logger.log(Level.FINE, "DIGEST63:Cipher suite: {0}", negotiatedCipher);
    }

    /**
     * Steps through the ordered 'strength' array, and compares it with
     * the 'supportedCiphers' array. The cipher returned represents
     * the best possible cipher based on the strength preference and the
     * available ciphers on both the server and client environments.
     *
     * @param tokens The array of cipher tokens sent by server
     * @return The agreed cipher.
     */
    private String findCipherAndStrength(byte[] supportedCiphers,
        String[] tokens) {
        byte s;
        for (int i = 0; i < strength.length; i++) {
            if ((s=strength[i]) != 0) {
                for (int j = 0; j < supportedCiphers.length; j++) {

                    // If user explicitly requested cipher, then it
                    // must be the one we choose

                    if (s == supportedCiphers[j] &&
                        (specifiedCipher == null ||
                            specifiedCipher.equals(tokens[j]))) {
                        switch (s) {
                        case HIGH_STRENGTH:
                            negotiatedStrength = "high";
                            break;
                        case MEDIUM_STRENGTH:
                            negotiatedStrength = "medium";
                            break;
                        case LOW_STRENGTH:
                            negotiatedStrength = "low";
                            break;
                        }

                        return tokens[j];
                    }
                }
            }
        }

        return null;  // none found
    }

    /**
     * Returns digest-response suitable for an initial authentication.
     *
     * The following are qdstr-val (quoted string values) as per RFC 2831,
     * which means that any embedded quotes must be escaped.
     *    realm-value
     *    nonce-value
     *    username-value
     *    cnonce-value
     *    authzid-value
     * @return {@code digest-response} in a byte array
     * @throws SaslException if there is an error generating the
     * response value or the cnonce value.
     */
    private byte[] generateClientResponse(byte[] charset) throws IOException {

        ByteArrayOutputStream digestResp = new ByteArrayOutputStream();

        if (useUTF8) {
            digestResp.write("charset=".getBytes(encoding));
            digestResp.write(charset);
            digestResp.write(',');
        }

        digestResp.write(("username=\"" +
            quotedStringValue(username) + "\",").getBytes(encoding));

        if (negotiatedRealm.length() > 0) {
            digestResp.write(("realm=\"" +
                quotedStringValue(negotiatedRealm) + "\",").getBytes(encoding));
        }

        digestResp.write("nonce=\"".getBytes(encoding));
        writeQuotedStringValue(digestResp, nonce);
        digestResp.write('"');
        digestResp.write(',');

        nonceCount = getNonceCount(nonce);
        digestResp.write(("nc=" +
            nonceCountToHex(nonceCount) + ",").getBytes(encoding));

        cnonce = generateNonce();
        digestResp.write("cnonce=\"".getBytes(encoding));
        writeQuotedStringValue(digestResp, cnonce);
        digestResp.write("\",".getBytes(encoding));
        digestResp.write(("digest-uri=\"" + digestUri + "\",").getBytes(encoding));

        digestResp.write("maxbuf=".getBytes(encoding));
        digestResp.write(String.valueOf(recvMaxBufSize).getBytes(encoding));
        digestResp.write(',');

        try {
            digestResp.write("response=".getBytes(encoding));
            digestResp.write(generateResponseValue("AUTHENTICATE",
                digestUri, negotiatedQop, username,
                negotiatedRealm, passwd, nonce, cnonce,
                nonceCount, authzidBytes));
            digestResp.write(',');
        } catch (Exception e) {
            throw new SaslException(
                "DIGEST-MD5: Error generating response value", e);
        }

        digestResp.write(("qop=" + negotiatedQop).getBytes(encoding));

        if (negotiatedCipher != null) {
            digestResp.write((",cipher=\"" + negotiatedCipher + "\"").getBytes(encoding));
        }

        if (authzidBytes != null) {
            digestResp.write(",authzid=\"".getBytes(encoding));
            writeQuotedStringValue(digestResp, authzidBytes);
            digestResp.write("\"".getBytes(encoding));
        }

        if (digestResp.size() > MAX_RESPONSE_LENGTH) {
            throw new SaslException ("DIGEST-MD5: digest-response size too " +
                "large. Length: "  + digestResp.size());
        }
        return digestResp.toByteArray();
     }


    /**
     * From RFC 2831, Section 2.1.3: Step Three
     * [Server] sends a message formatted as follows:
     *     response-auth = "rspauth" "=" response-value
     * where response-value is calculated as above, using the values sent in
     * step two, except that if qop is "auth", then A2 is
     *
     *  A2 = { ":", digest-uri-value }
     *
     * And if qop is "auth-int" or "auth-conf" then A2 is
     *
     *  A2 = { ":", digest-uri-value, ":00000000000000000000000000000000" }
     */
    private void validateResponseValue(byte[] fromServer) throws SaslException {
        if (fromServer == null) {
            throw new SaslException("DIGEST-MD5: Authenication failed. " +
                "Expecting 'rspauth' authentication success message");
        }

        try {
            byte[] expected = generateResponseValue("",
                digestUri, negotiatedQop, username, negotiatedRealm,
                passwd, nonce, cnonce,  nonceCount, authzidBytes);
            if (!Arrays.equals(expected, fromServer)) {
                /* Server's rspauth value does not match */
                throw new SaslException(
                    "Server's rspauth value does not match what client expects");
            }
        } catch (NoSuchAlgorithmException e) {
            throw new SaslException(
                "Problem generating response value for verification", e);
        } catch (IOException e) {
            throw new SaslException(
                "Problem generating response value for verification", e);
        }
    }

    /**
     * Returns the number of requests (including current request)
     * that the client has sent in response to nonceValue.
     * This is 1 the first time nonceValue is seen.
     *
     * We don't cache nonce values seen, and we don't support subsequent
     * authentication, so the value is always 1.
     */
    private static int getNonceCount(byte[] nonceValue) {
        return 1;
    }

    private void clearPassword() {
        if (passwd != null) {
            for (int i = 0; i < passwd.length; i++) {
                passwd[i] = 0;
            }
            passwd = null;
        }
    }
}
