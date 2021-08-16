/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import java.util.logging.Level;

import javax.security.sasl.*;
import javax.security.auth.callback.*;

import static java.nio.charset.StandardCharsets.*;

/**
 * An implementation of the DIGEST-MD5 server SASL mechanism.
 * (<a href="http://www.ietf.org/rfc/rfc2831.txt">RFC 2831</a>)
 * <p>
 * The DIGEST-MD5 SASL mechanism specifies two modes of authentication.
 * <ul><li>Initial Authentication
 * <li>Subsequent Authentication - optional, (currently not supported)
 * </ul>
 *
 * Required callbacks:
 * - RealmCallback
 *      used as key by handler to fetch password
 * - NameCallback
 *      used as key by handler to fetch password
 * - PasswordCallback
 *      handler must enter password for username/realm supplied
 * - AuthorizeCallback
 *      handler must verify that authid/authzids are allowed and set
 *      authorized ID to be the canonicalized authzid (if applicable).
 *
 * Environment properties that affect the implementation:
 * javax.security.sasl.qop:
 *    specifies list of qops; default is "auth"; typically, caller should set
 *    this to "auth, auth-int, auth-conf".
 * javax.security.sasl.strength
 *    specifies low/medium/high strength of encryption; default is all available
 *    ciphers [high,medium,low]; high means des3 or rc4 (128); medium des or
 *    rc4-56; low is rc4-40.
 * javax.security.sasl.maxbuf
 *    specifies max receive buf size; default is 65536
 * javax.security.sasl.sendmaxbuffer
 *    specifies max send buf size; default is 65536 (min of this and client's max
 *    recv size)
 *
 * com.sun.security.sasl.digest.utf8:
 *    "true" means to use UTF-8 charset; "false" to use ISO-8859-1 encoding;
 *    default is "true".
 * com.sun.security.sasl.digest.realm:
 *    space-separated list of realms; default is server name (fqdn parameter)
 *
 * @author Rosanna Lee
 */

final class DigestMD5Server extends DigestMD5Base implements SaslServer {
    private static final String MY_CLASS_NAME = DigestMD5Server.class.getName();

    private static final String UTF8_DIRECTIVE = "charset=utf-8,";
    private static final String ALGORITHM_DIRECTIVE = "algorithm=md5-sess";

    /*
     * Always expect nonce count value to be 1 because we support only
     * initial authentication.
     */
    private static final int NONCE_COUNT_VALUE = 1;

    /* "true" means use UTF8; "false" ISO 8859-1; default is "true" */
    private static final String UTF8_PROPERTY =
        "com.sun.security.sasl.digest.utf8";

    /* List of space-separated realms used for authentication */
    private static final String REALM_PROPERTY =
        "com.sun.security.sasl.digest.realm";

    /* Directives encountered in responses sent by the client. */
    private static final String[] DIRECTIVE_KEY = {
        "username",    // exactly once
        "realm",       // exactly once if sent by server
        "nonce",       // exactly once
        "cnonce",      // exactly once
        "nonce-count", // atmost once; default is 00000001
        "qop",         // atmost once; default is "auth"
        "digest-uri",  // atmost once; (default?)
        "response",    // exactly once
        "maxbuf",      // atmost once; default is 65536
        "charset",     // atmost once; default is ISO-8859-1
        "cipher",      // exactly once if qop is "auth-conf"
        "authzid",     // atmost once; default is none
        "auth-param",  // >= 0 times (ignored)
    };

    /* Indices into DIRECTIVE_KEY */
    private static final int USERNAME = 0;
    private static final int REALM = 1;
    private static final int NONCE = 2;
    private static final int CNONCE = 3;
    private static final int NONCE_COUNT = 4;
    private static final int QOP = 5;
    private static final int DIGEST_URI = 6;
    private static final int RESPONSE = 7;
    private static final int MAXBUF = 8;
    private static final int CHARSET = 9;
    private static final int CIPHER = 10;
    private static final int AUTHZID = 11;
    private static final int AUTH_PARAM = 12;

    /* Server-generated/supplied information */
    private String specifiedQops;
    private byte[] myCiphers;
    private List<String> serverRealms;

    DigestMD5Server(String protocol, String serverName, Map<String, ?> props,
            CallbackHandler cbh) throws SaslException {
        super(props, MY_CLASS_NAME, 1,
                protocol + "/" + (serverName==null?"*":serverName),
                cbh);

        serverRealms = new ArrayList<String>();

        useUTF8 = true;  // default

        if (props != null) {
            specifiedQops = (String) props.get(Sasl.QOP);
            if ("false".equals((String) props.get(UTF8_PROPERTY))) {
                useUTF8 = false;
                logger.log(Level.FINE, "DIGEST80:Server supports ISO-Latin-1");
            }

            String realms = (String) props.get(REALM_PROPERTY);
            if (realms != null) {
                StringTokenizer parser = new StringTokenizer(realms, ", \t\n");
                int tokenCount = parser.countTokens();
                String token = null;
                for (int i = 0; i < tokenCount; i++) {
                    token = parser.nextToken();
                    logger.log(Level.FINE, "DIGEST81:Server supports realm {0}",
                        token);
                    serverRealms.add(token);
                }
            }
        }

        encoding = (useUTF8 ? UTF_8 : ISO_8859_1);

        // By default, use server name as realm
        if (serverRealms.isEmpty()) {
            if (serverName == null) {
                throw new SaslException(
                        "A realm must be provided in props or serverName");
            } else {
                serverRealms.add(serverName);
            }
        }
    }

    public  byte[] evaluateResponse(byte[] response) throws SaslException {
        if (response.length > MAX_RESPONSE_LENGTH) {
            throw new SaslException(
                "DIGEST-MD5: Invalid digest response length. Got:  " +
                response.length + " Expected < " + MAX_RESPONSE_LENGTH);
        }

        byte[] challenge;
        switch (step) {
        case 1:
            if (response.length != 0) {
                // We do not support "subsequent authentication" (client
                // initial response). According to
                // https://tools.ietf.org/html/rfc2831#section-2.2
                // If the server does not support subsequent authentication,
                // then it sends a "digest-challenge", and authentication
                // proceeds as in initial authentication.
                logger.log(Level.FINE, "Ignoring initial response");
            }

            /* Generate first challenge */
            String supportedCiphers = null;
            if ((allQop&PRIVACY_PROTECTION) != 0) {
                myCiphers = getPlatformCiphers();
                StringBuilder sb = new StringBuilder();

                // myCipher[i] is a byte that indicates whether CIPHER_TOKENS[i]
                // is supported
                for (int i = 0; i < CIPHER_TOKENS.length; i++) {
                    if (myCiphers[i] != 0) {
                        if (sb.length() > 0) {
                            sb.append(',');
                        }
                        sb.append(CIPHER_TOKENS[i]);
                    }
                }
                supportedCiphers = sb.toString();
            }

            try {
                challenge = generateChallenge(serverRealms, specifiedQops,
                    supportedCiphers);

                step = 3;
                return challenge;
            } catch (IOException e) {
                throw new SaslException(
                    "DIGEST-MD5: Error generating challenge", e);
            }

            // Step 2 is performed by client

        case 3:
            /* Validates client's response and generate challenge:
             *    response-auth = "rspauth" "=" response-value
             */
            try {
                byte[][] responseVal = parseDirectives(response, DIRECTIVE_KEY,
                    null, REALM);
                challenge = validateClientResponse(responseVal);
            } finally {
                step = 0;  // Set to invalid state
            }

            completed = true;

            /* Initialize SecurityCtx implementation */
            if (integrity && privacy) {
                secCtx = new DigestPrivacy(false /* not client */);
            } else if (integrity) {
                secCtx = new DigestIntegrity(false /* not client */);
            }

            return challenge;

        default:
            // No other possible state
            throw new SaslException("DIGEST-MD5: Server at illegal state");
        }
    }

    /**
     * Generates challenge to be sent to client.
     *  digest-challenge  =
     *    1#( realm | nonce | qop-options | stale | maxbuf | charset
     *               algorithm | cipher-opts | auth-param )
     *
     *        realm             = "realm" "=" <"> realm-value <">
     *        realm-value       = qdstr-val
     *        nonce             = "nonce" "=" <"> nonce-value <">
     *        nonce-value       = qdstr-val
     *        qop-options       = "qop" "=" <"> qop-list <">
     *        qop-list          = 1#qop-value
     *        qop-value         = "auth" | "auth-int" | "auth-conf" |
     *                             token
     *        stale             = "stale" "=" "true"
     *        maxbuf            = "maxbuf" "=" maxbuf-value
     *        maxbuf-value      = 1*DIGIT
     *        charset           = "charset" "=" "utf-8"
     *        algorithm         = "algorithm" "=" "md5-sess"
     *        cipher-opts       = "cipher" "=" <"> 1#cipher-value <">
     *        cipher-value      = "3des" | "des" | "rc4-40" | "rc4" |
     *                            "rc4-56" | token
     *        auth-param        = token "=" ( token | quoted-string )
     */
    private byte[] generateChallenge(List<String> realms, String qopStr,
        String cipherStr) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Realms (>= 0)
        for (int i = 0; realms != null && i < realms.size(); i++) {
            out.write("realm=\"".getBytes(encoding));
            writeQuotedStringValue(out, realms.get(i).getBytes(encoding));
            out.write('"');
            out.write(',');
        }

        // Nonce - required (1)
        out.write(("nonce=\"").getBytes(encoding));
        nonce = generateNonce();
        writeQuotedStringValue(out, nonce);
        out.write('"');
        out.write(',');

        // QOP - optional (1) [default: auth]
        // qop="auth,auth-conf,auth-int"
        if (qopStr != null) {
            out.write(("qop=\"").getBytes(encoding));
            // Check for quotes in case of non-standard qop options
            writeQuotedStringValue(out, qopStr.getBytes(encoding));
            out.write('"');
            out.write(',');
        }

        // maxbuf - optional (1) [default: 65536]
        if (recvMaxBufSize != DEFAULT_MAXBUF) {
            out.write(("maxbuf=\"" + recvMaxBufSize + "\",").getBytes(encoding));
        }

        // charset - optional (1) [default: ISO 8859_1]
        if (useUTF8) {
            out.write(UTF8_DIRECTIVE.getBytes(encoding));
        }

        if (cipherStr != null) {
            out.write("cipher=\"".getBytes(encoding));
            // Check for quotes in case of custom ciphers
            writeQuotedStringValue(out, cipherStr.getBytes(encoding));
            out.write('"');
            out.write(',');
        }

        // algorithm - required (1)
        out.write(ALGORITHM_DIRECTIVE.getBytes(encoding));

        return out.toByteArray();
    }

    /**
     * Validates client's response.
     *   digest-response  = 1#( username | realm | nonce | cnonce |
     *                          nonce-count | qop | digest-uri | response |
     *                          maxbuf | charset | cipher | authzid |
     *                          auth-param )
     *
     *       username         = "username" "=" <"> username-value <">
     *       username-value   = qdstr-val
     *       cnonce           = "cnonce" "=" <"> cnonce-value <">
     *       cnonce-value     = qdstr-val
     *       nonce-count      = "nc" "=" nc-value
     *       nc-value         = 8LHEX
     *       qop              = "qop" "=" qop-value
     *       digest-uri       = "digest-uri" "=" <"> digest-uri-value <">
     *       digest-uri-value  = serv-type "/" host [ "/" serv-name ]
     *       serv-type        = 1*ALPHA
     *       host             = 1*( ALPHA | DIGIT | "-" | "." )
     *       serv-name        = host
     *       response         = "response" "=" response-value
     *       response-value   = 32LHEX
     *       LHEX             = "0" | "1" | "2" | "3" |
     *                          "4" | "5" | "6" | "7" |
     *                          "8" | "9" | "a" | "b" |
     *                          "c" | "d" | "e" | "f"
     *       cipher           = "cipher" "=" cipher-value
     *       authzid          = "authzid" "=" <"> authzid-value <">
     *       authzid-value    = qdstr-val
     * sets:
     *   negotiatedQop
     *   negotiatedCipher
     *   negotiatedRealm
     *   negotiatedStrength
     *   digestUri (checked and set to clients to account for case diffs)
     *   sendMaxBufSize
     *   authzid (gotten from callback)
     * @return response-value ('rspauth') for client to validate
     */
    private byte[] validateClientResponse(byte[][] responseVal)
        throws SaslException {

        /* CHARSET: optional atmost once */
        if (responseVal[CHARSET] != null) {
            // The client should send this directive only if the server has
            // indicated it supports UTF-8.
            if (!useUTF8 ||
                !"utf-8".equals(new String(responseVal[CHARSET], encoding))) {
                throw new SaslException("DIGEST-MD5: digest response format " +
                    "violation. Incompatible charset value: " +
                    new String(responseVal[CHARSET]));
            }
        }

        // maxbuf: atmost once
        int clntMaxBufSize =
            (responseVal[MAXBUF] == null) ? DEFAULT_MAXBUF
            : Integer.parseInt(new String(responseVal[MAXBUF], encoding));

        // Max send buf size is min of client's max recv buf size and
        // server's max send buf size
        sendMaxBufSize = ((sendMaxBufSize == 0) ? clntMaxBufSize :
            Math.min(sendMaxBufSize, clntMaxBufSize));

        /* username: exactly once */
        String username;
        if (responseVal[USERNAME] != null) {
            username = new String(responseVal[USERNAME], encoding);
            logger.log(Level.FINE, "DIGEST82:Username: {0}", username);
        } else {
            throw new SaslException("DIGEST-MD5: digest response format " +
                "violation. Missing username.");
        }

        /* realm: exactly once if sent by server */
        negotiatedRealm = ((responseVal[REALM] != null) ?
            new String(responseVal[REALM], encoding) : "");
        logger.log(Level.FINE, "DIGEST83:Client negotiated realm: {0}",
            negotiatedRealm);

        if (!serverRealms.contains(negotiatedRealm)) {
            // Server had sent at least one realm
            // Check that response is one of these
            throw new SaslException("DIGEST-MD5: digest response format " +
                "violation. Nonexistent realm: " + negotiatedRealm);
        }
        // Else, client specified realm was one of server's or server had none

        /* nonce: exactly once */
        if (responseVal[NONCE] == null) {
            throw new SaslException("DIGEST-MD5: digest response format " +
                "violation. Missing nonce.");
        }
        byte[] nonceFromClient = responseVal[NONCE];
        if (!Arrays.equals(nonceFromClient, nonce)) {
            throw new SaslException("DIGEST-MD5: digest response format " +
                "violation. Mismatched nonce.");
        }

        /* cnonce: exactly once */
        if (responseVal[CNONCE] == null) {
            throw new SaslException("DIGEST-MD5: digest response format " +
                "violation. Missing cnonce.");
        }
        byte[] cnonce = responseVal[CNONCE];

        /* nonce-count: atmost once */
        if (responseVal[NONCE_COUNT] != null &&
            NONCE_COUNT_VALUE != Integer.parseInt(
                new String(responseVal[NONCE_COUNT], encoding), 16)) {
            throw new SaslException("DIGEST-MD5: digest response format " +
                "violation. Nonce count does not match: " +
                new String(responseVal[NONCE_COUNT]));
        }

        /* qop: atmost once; default is "auth" */
        negotiatedQop = ((responseVal[QOP] != null) ?
            new String(responseVal[QOP], encoding) : "auth");

        logger.log(Level.FINE, "DIGEST84:Client negotiated qop: {0}",
            negotiatedQop);

        // Check that QOP is one sent by server
        byte cQop;
        switch (negotiatedQop) {
            case "auth":
                cQop = NO_PROTECTION;
                break;
            case "auth-int":
                cQop = INTEGRITY_ONLY_PROTECTION;
                integrity = true;
                rawSendSize = sendMaxBufSize - 16;
                break;
            case "auth-conf":
                cQop = PRIVACY_PROTECTION;
                integrity = privacy = true;
                rawSendSize = sendMaxBufSize - 26;
                break;
            default:
                throw new SaslException("DIGEST-MD5: digest response format " +
                    "violation. Invalid QOP: " + negotiatedQop);
        }
        if ((cQop&allQop) == 0) {
            throw new SaslException("DIGEST-MD5: server does not support " +
                " qop: " + negotiatedQop);
        }

        if (privacy) {
            negotiatedCipher = ((responseVal[CIPHER] != null) ?
                new String(responseVal[CIPHER], encoding) : null);
            if (negotiatedCipher == null) {
                throw new SaslException("DIGEST-MD5: digest response format " +
                    "violation. No cipher specified.");
            }

            int foundCipher = -1;
            logger.log(Level.FINE, "DIGEST85:Client negotiated cipher: {0}",
                negotiatedCipher);

            // Check that cipher is one that we offered
            for (int j = 0; j < CIPHER_TOKENS.length; j++) {
                if (negotiatedCipher.equals(CIPHER_TOKENS[j]) &&
                    myCiphers[j] != 0) {
                    foundCipher = j;
                    break;
                }
            }
            if (foundCipher == -1) {
                throw new SaslException("DIGEST-MD5: server does not " +
                    "support cipher: " + negotiatedCipher);
            }
            // Set negotiatedStrength
            if ((CIPHER_MASKS[foundCipher]&HIGH_STRENGTH) != 0) {
                negotiatedStrength = "high";
            } else if ((CIPHER_MASKS[foundCipher]&MEDIUM_STRENGTH) != 0) {
                negotiatedStrength = "medium";
            } else {
                // assume default low
                negotiatedStrength = "low";
            }

            logger.log(Level.FINE, "DIGEST86:Negotiated strength: {0}",
                negotiatedStrength);
        }

        // atmost once
        String digestUriFromResponse = ((responseVal[DIGEST_URI]) != null ?
            new String(responseVal[DIGEST_URI], encoding) : null);

        if (digestUriFromResponse != null) {
            logger.log(Level.FINE, "DIGEST87:digest URI: {0}",
                digestUriFromResponse);
        }

        // serv-type "/" host [ "/" serv-name ]
        // e.g.: smtp/mail3.example.com/example.com
        // e.g.: ftp/ftp.example.com
        // e.g.: ldap/ldapserver.example.com

        // host should match one of service's configured service names
        // Check against digest URI that mech was created with

        if (uriMatches(digestUri, digestUriFromResponse)) {
            digestUri = digestUriFromResponse; // account for case-sensitive diffs
        } else {
            throw new SaslException("DIGEST-MD5: digest response format " +
                "violation. Mismatched URI: " + digestUriFromResponse +
                "; expecting: " + digestUri);
        }

        // response: exactly once
        byte[] responseFromClient = responseVal[RESPONSE];
        if (responseFromClient == null) {
            throw new SaslException("DIGEST-MD5: digest response format " +
                " violation. Missing response.");
        }

        // authzid: atmost once
        byte[] authzidBytes;
        String authzidFromClient = ((authzidBytes=responseVal[AUTHZID]) != null?
            new String(authzidBytes, encoding) : username);

        if (authzidBytes != null) {
            logger.log(Level.FINE, "DIGEST88:Authzid: {0}",
                new String(authzidBytes));
        }

        // Ignore auth-param

        // Get password need to generate verifying response
        char[] passwd;
        try {
            // Realm and Name callbacks are used to provide info
            RealmCallback rcb = new RealmCallback("DIGEST-MD5 realm: ",
                negotiatedRealm);
            NameCallback ncb = new NameCallback("DIGEST-MD5 authentication ID: ",
                username);

            // PasswordCallback is used to collect info
            PasswordCallback pcb =
                new PasswordCallback("DIGEST-MD5 password: ", false);

            cbh.handle(new Callback[] {rcb, ncb, pcb});
            passwd = pcb.getPassword();
            pcb.clearPassword();

        } catch (UnsupportedCallbackException e) {
            throw new SaslException(
                "DIGEST-MD5: Cannot perform callback to acquire password", e);

        } catch (IOException e) {
            throw new SaslException(
                "DIGEST-MD5: IO error acquiring password", e);
        }

        if (passwd == null) {
            throw new SaslException(
                "DIGEST-MD5: cannot acquire password for " + username +
                " in realm : " + negotiatedRealm);
        }

        try {
            // Validate response value sent by client
            byte[] expectedResponse;

            try {
                expectedResponse = generateResponseValue("AUTHENTICATE",
                    digestUri, negotiatedQop, username, negotiatedRealm,
                    passwd, nonce /* use own nonce */,
                    cnonce, NONCE_COUNT_VALUE, authzidBytes);

            } catch (NoSuchAlgorithmException e) {
                throw new SaslException(
                    "DIGEST-MD5: problem duplicating client response", e);
            } catch (IOException e) {
                throw new SaslException(
                    "DIGEST-MD5: problem duplicating client response", e);
            }

            if (!Arrays.equals(responseFromClient, expectedResponse)) {
                throw new SaslException("DIGEST-MD5: digest response format " +
                    "violation. Mismatched response.");
            }

            // Ensure that authzid mapping is OK
            try {
                AuthorizeCallback acb =
                    new AuthorizeCallback(username, authzidFromClient);
                cbh.handle(new Callback[]{acb});

                if (acb.isAuthorized()) {
                    authzid = acb.getAuthorizedID();
                } else {
                    throw new SaslException("DIGEST-MD5: " + username +
                        " is not authorized to act as " + authzidFromClient);
                }
            } catch (SaslException e) {
                throw e;
            } catch (UnsupportedCallbackException e) {
                throw new SaslException(
                    "DIGEST-MD5: Cannot perform callback to check authzid", e);
            } catch (IOException e) {
                throw new SaslException(
                    "DIGEST-MD5: IO error checking authzid", e);
            }

            return generateResponseAuth(username, passwd, cnonce,
                NONCE_COUNT_VALUE, authzidBytes);
        } finally {
            // Clear password
            for (int i = 0; i < passwd.length; i++) {
                passwd[i] = 0;
            }
        }
    }

    private static boolean uriMatches(String thisUri, String incomingUri) {
        // Full match
        if (thisUri.equalsIgnoreCase(incomingUri)) {
            return true;
        }
        // Unbound match
        if (thisUri.endsWith("/*")) {
            int protoAndSlash = thisUri.length() - 1;
            String thisProtoAndSlash = thisUri.substring(0, protoAndSlash);
            String incomingProtoAndSlash = incomingUri.substring(0, protoAndSlash);
            return thisProtoAndSlash.equalsIgnoreCase(incomingProtoAndSlash);
        }
        return false;
    }

    /**
     * Server sends a message formatted as follows:
     *    response-auth = "rspauth" "=" response-value
     *   where response-value is calculated as above, using the values sent in
     *   step two, except that if qop is "auth", then A2 is
     *
     *       A2 = { ":", digest-uri-value }
     *
     *   And if qop is "auth-int" or "auth-conf" then A2 is
     *
     *       A2 = { ":", digest-uri-value, ":00000000000000000000000000000000" }
     *
     * Clears password afterwards.
     */
    private byte[] generateResponseAuth(String username, char[] passwd,
        byte[] cnonce, int nonceCount, byte[] authzidBytes) throws SaslException {

        // Construct response value

        try {
            byte[] responseValue = generateResponseValue("",
                digestUri, negotiatedQop, username, negotiatedRealm,
                passwd, nonce, cnonce, nonceCount, authzidBytes);

            byte[] challenge = new byte[responseValue.length + 8];
            System.arraycopy("rspauth=".getBytes(encoding), 0, challenge, 0, 8);
            System.arraycopy(responseValue, 0, challenge, 8,
                responseValue.length );

            return challenge;

        } catch (NoSuchAlgorithmException e) {
            throw new SaslException("DIGEST-MD5: problem generating response", e);
        } catch (IOException e) {
            throw new SaslException("DIGEST-MD5: problem generating response", e);
        }
    }

    public String getAuthorizationID() {
        if (completed) {
            return authzid;
        } else {
            throw new IllegalStateException(
                "DIGEST-MD5 server negotiation not complete");
        }
    }
}
