/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.Random;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.security.spec.KeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.InvalidAlgorithmParameterException;

import static java.nio.charset.StandardCharsets.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.DESedeKeySpec;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.*;

import com.sun.security.sasl.util.AbstractSaslImpl;

/**
 * Utility class for DIGEST-MD5 mechanism. Provides utility methods
 * and contains two inner classes which implement the SecurityCtx
 * interface. The inner classes provide the funtionality to allow
 * for quality-of-protection (QOP) with integrity checking and
 * privacy.
 *
 * @author Jonathan Bruce
 * @author Rosanna Lee
 */
abstract class DigestMD5Base extends AbstractSaslImpl {
    /* ------------------------- Constants ------------------------ */

    // Used for logging
    private static final String DI_CLASS_NAME = DigestIntegrity.class.getName();
    private static final String DP_CLASS_NAME = DigestPrivacy.class.getName();

    /* Constants - defined in RFC2831 */
    protected static final int MAX_CHALLENGE_LENGTH = 2048;
    protected static final int MAX_RESPONSE_LENGTH = 4096;
    protected static final int DEFAULT_MAXBUF = 65536;

    /* Supported ciphers for 'auth-conf' */
    protected static final int DES3 = 0;
    protected static final int RC4 = 1;
    protected static final int DES = 2;
    protected static final int RC4_56 = 3;
    protected static final int RC4_40 = 4;
    protected static final String[] CIPHER_TOKENS = { "3des",
                                                      "rc4",
                                                      "des",
                                                      "rc4-56",
                                                      "rc4-40" };
    private static final String[] JCE_CIPHER_NAME = {
        "DESede/CBC/NoPadding",
        "RC4",
        "DES/CBC/NoPadding",
    };

    /*
     * If QOP is set to 'auth-conf', a DIGEST-MD5 mechanism must have
     * support for the DES and Triple DES cipher algorithms (optionally,
     * support for RC4 [128/56/40 bit keys] ciphers) to provide for
     * confidentiality. See RFC 2831 for details. This implementation
     * provides support for DES, Triple DES and RC4 ciphers.
     *
     * The value of strength effects the strength of cipher used. The mappings
     * of 'high', 'medium', and 'low' give the following behaviour.
     *
     *  HIGH_STRENGTH   - Triple DES
     *                  - RC4 (128bit)
     *  MEDIUM_STRENGTH - DES
     *                  - RC4 (56bit)
     *  LOW_SRENGTH     - RC4 (40bit)
     */
    protected static final byte DES_3_STRENGTH = HIGH_STRENGTH;
    protected static final byte RC4_STRENGTH = HIGH_STRENGTH;
    protected static final byte DES_STRENGTH = MEDIUM_STRENGTH;
    protected static final byte RC4_56_STRENGTH = MEDIUM_STRENGTH;
    protected static final byte RC4_40_STRENGTH = LOW_STRENGTH;
    protected static final byte UNSET = (byte)0;
    protected static final byte[] CIPHER_MASKS = { DES_3_STRENGTH,
                                                   RC4_STRENGTH,
                                                   DES_STRENGTH,
                                                   RC4_56_STRENGTH,
                                                   RC4_40_STRENGTH };

    private static final String SECURITY_LAYER_MARKER =
        ":00000000000000000000000000000000";

    protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /* ------------------- Variable Fields ----------------------- */

    /* Used to track progress of authentication; step numbers from RFC 2831 */
    protected int step;

    /* Used to get username/password, choose realm for client */
    /* Used to obtain authorization, pw info, canonicalized authzid for server */
    protected CallbackHandler cbh;

    protected SecurityCtx secCtx;
    protected byte[] H_A1; // component of response-value

    protected byte[] nonce;         // server generated nonce

    /* Variables set when parsing directives in digest challenge/response. */
    protected String negotiatedStrength;
    protected String negotiatedCipher;
    protected String negotiatedQop;
    protected String negotiatedRealm;
    protected boolean useUTF8 = false;
    protected Charset encoding = ISO_8859_1;  // default unless server specifies utf-8

    protected String digestUri;
    protected String authzid;       // authzid or canonicalized authzid

    /**
     * Constucts an instance of DigestMD5Base. Calls super constructor
     * to parse properties for mechanism.
     *
     * @param props A map of property/value pairs
     * @param className name of class to use for logging
     * @param firstStep number of first step in authentication state machine
     * @param digestUri digestUri used in authentication
     * @param cbh callback handler used to get info required for auth
     *
     * @throws SaslException If invalid value found in props.
     */
    protected DigestMD5Base(Map<String, ?> props, String className,
        int firstStep, String digestUri, CallbackHandler cbh)
        throws SaslException {
        super(props, className); // sets QOP, STENGTH and BUFFER_SIZE

        step = firstStep;
        this.digestUri = digestUri;
        this.cbh = cbh;
    }

    /**
     * Retrieves the SASL mechanism IANA name.
     *
     * @return The String "DIGEST-MD5"
     */
    public String getMechanismName() {
        return "DIGEST-MD5";
    }

    /**
     * Unwrap the incoming message using the wrap method of the secCtx object
     * instance.
     *
     * @param incoming The byte array containing the incoming bytes.
     * @param start The offset from which to read the byte array.
     * @param len The number of bytes to read from the offset.
     * @return The unwrapped message according to either the integrity or
     * privacy quality-of-protection specifications.
     * @throws SaslException if an error occurs when unwrapping the incoming
     * message
     */
    public byte[] unwrap(byte[] incoming, int start, int len) throws SaslException {
        if (!completed) {
            throw new IllegalStateException(
                "DIGEST-MD5 authentication not completed");
        }

        if (secCtx == null) {
            throw new IllegalStateException(
                "Neither integrity nor privacy was negotiated");
        }

        return (secCtx.unwrap(incoming, start, len));
    }

    /**
     * Wrap outgoing bytes using the wrap method of the secCtx object
     * instance.
     *
     * @param outgoing The byte array containing the outgoing bytes.
     * @param start The offset from which to read the byte array.
     * @param len The number of bytes to read from the offset.
     * @return The wrapped message according to either the integrity or
     * privacy quality-of-protection specifications.
     * @throws SaslException if an error occurs when wrapping the outgoing
     * message
     */
    public byte[] wrap(byte[] outgoing, int start, int len) throws SaslException {
        if (!completed) {
            throw new IllegalStateException(
                "DIGEST-MD5 authentication not completed");
        }

        if (secCtx == null) {
            throw new IllegalStateException(
                "Neither integrity nor privacy was negotiated");
        }

        return (secCtx.wrap(outgoing, start, len));
    }

    public void dispose() throws SaslException {
        if (secCtx != null) {
            secCtx = null;
        }
    }

    public Object getNegotiatedProperty(String propName) {
        if (completed) {
            if (propName.equals(Sasl.STRENGTH)) {
                return negotiatedStrength;
            } else if (propName.equals(Sasl.BOUND_SERVER_NAME)) {
                return digestUri.substring(digestUri.indexOf('/') + 1);
            } else {
                return super.getNegotiatedProperty(propName);
            }
        } else {
            throw new IllegalStateException(
                "DIGEST-MD5 authentication not completed");
        }
    }

    /* ----------------- Digest-MD5 utilities ---------------- */
    /**
     * Generate random-string used for digest-response.
     * This method uses Random to get random bytes and then
     * base64 encodes the bytes. Could also use binaryToHex() but this
     * is slightly faster and a more compact representation of the same info.
     * @return A non-null byte array containing the nonce value for the
     * digest challenge or response.
     * Could use SecureRandom to be more secure but it is very slow.
     */

    /** This array maps the characters to their 6 bit values */
    private static final char[] pem_array = {
        //       0   1   2   3   4   5   6   7
                'A','B','C','D','E','F','G','H', // 0
                'I','J','K','L','M','N','O','P', // 1
                'Q','R','S','T','U','V','W','X', // 2
                'Y','Z','a','b','c','d','e','f', // 3
                'g','h','i','j','k','l','m','n', // 4
                'o','p','q','r','s','t','u','v', // 5
                'w','x','y','z','0','1','2','3', // 6
                '4','5','6','7','8','9','+','/'  // 7
    };

    // Make sure that this is a multiple of 3
    private static final int RAW_NONCE_SIZE = 30;

    // Base 64 encoding turns each 3 bytes into 4
    private static final int ENCODED_NONCE_SIZE = RAW_NONCE_SIZE*4/3;

    protected static final byte[] generateNonce() {

        // SecureRandom random = new SecureRandom();
        Random random = new Random();
        byte[] randomData = new byte[RAW_NONCE_SIZE];
        random.nextBytes(randomData);

        byte[] nonce = new byte[ENCODED_NONCE_SIZE];

        // Base64-encode bytes
        byte a, b, c;
        int j = 0;
        for (int i = 0; i < randomData.length; i += 3) {
            a = randomData[i];
            b = randomData[i+1];
            c = randomData[i+2];
            nonce[j++] = (byte)(pem_array[(a >>> 2) & 0x3F]);
            nonce[j++] = (byte)(pem_array[((a << 4) & 0x30) + ((b >>> 4) & 0xf)]);
            nonce[j++] = (byte)(pem_array[((b << 2) & 0x3c) + ((c >>> 6) & 0x3)]);
            nonce[j++] = (byte)(pem_array[c & 0x3F]);
        }

        return nonce;

        // %%% For testing using RFC 2831 example, uncomment the following 2 lines
        // System.out.println("!!!Using RFC 2831's cnonce for testing!!!");
        // return "OA6MHXh6VqTrRk".getBytes();
    }

    /**
     * Checks if a byte[] contains characters that must be quoted
     * and write the resulting, possibly escaped, characters to out.
     */
    protected static void writeQuotedStringValue(ByteArrayOutputStream out,
        byte[] buf) {

        int len = buf.length;
        byte ch;
        for (int i = 0; i < len; i++) {
            ch = buf[i];
            if (needEscape((char)ch)) {
                out.write('\\');
            }
            out.write(ch);
        }
    }

    // See Section 7.2 of RFC 2831; double-quote character is not allowed
    // unless escaped; also escape the escape character and CTL chars except LWS
    private static boolean needEscape(String str) {
        int len = str.length();
        for (int i = 0; i < len; i++) {
            if (needEscape(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    // Determines whether a character needs to be escaped in a quoted string
    private static boolean needEscape(char ch) {
        return ch == '"' ||  // escape char
            ch == '\\' ||    // quote
            ch == 127 ||     // DEL

            // 0 <= ch <= 31 except CR, HT and LF
            (ch >= 0 && ch <= 31 && ch != 13 && ch != 9 && ch != 10);
    }

    protected static String quotedStringValue(String str) {
        if (needEscape(str)) {
            int len = str.length();
            char[] buf = new char[len+len];
            int j = 0;
            char ch;
            for (int i = 0; i < len; i++) {
                ch = str.charAt(i);
                if (needEscape(ch)) {
                    buf[j++] =  '\\';
                }
                buf[j++] = ch;
            }
            return new String(buf, 0, j);
        } else {
            return str;
        }
    }

    /**
     * Convert a byte array to hexadecimal string.
     *
     * @param a non-null byte array
     * @return a non-null String contain the HEX value
     */
    protected byte[] binaryToHex(byte[] digest) {

        StringBuilder digestString = new StringBuilder();

        for (int i = 0; i < digest.length; i ++) {
            if ((digest[i] & 0x000000ff) < 0x10) {
                digestString.append('0').append(Integer.toHexString(digest[i] & 0x000000ff));
            } else {
                digestString.append(
                    Integer.toHexString(digest[i] & 0x000000ff));
            }
        }
        return digestString.toString().getBytes(encoding);
    }

    /**
     * Used to convert username-value, passwd or realm to 8859_1 encoding
     * if all chars in string are within the 8859_1 (Latin 1) encoding range.
     *
     * @param a non-null String
     * @return a non-null byte array containing the correct character encoding
     * for username, paswd or realm.
     */
    protected byte[] stringToByte_8859_1(String str) {

        char[] buffer = str.toCharArray();

        if (useUTF8) {
            for (int i = 0; i < buffer.length; i++) {
                if (buffer[i] > '\u00FF') {
                    return str.getBytes(UTF_8);
                }
            }
        }
        return str.getBytes(ISO_8859_1);
    }

    protected static byte[] getPlatformCiphers() {
        byte[] ciphers = new byte[CIPHER_TOKENS.length];

        for (int i = 0; i < JCE_CIPHER_NAME.length; i++) {
            try {
                // Checking whether the transformation is available from the
                // current installed providers.
                Cipher.getInstance(JCE_CIPHER_NAME[i]);

                logger.log(Level.FINE, "DIGEST01:Platform supports {0}", JCE_CIPHER_NAME[i]);
                ciphers[i] |= CIPHER_MASKS[i];
            } catch (NoSuchAlgorithmException e) {
                // no implementation found for requested algorithm.
            } catch (NoSuchPaddingException e) {
                // no implementation found for requested algorithm.
            }
        }

        if (ciphers[RC4] != UNSET) {
            ciphers[RC4_56] |= CIPHER_MASKS[RC4_56];
            ciphers[RC4_40] |= CIPHER_MASKS[RC4_40];
        }

        return ciphers;
    }

    /**
     * Assembles response-value for digest-response.
     *
     * @param authMethod "AUTHENTICATE" for client-generated response;
     *        "" for server-generated response
     * @return A non-null byte array containing the repsonse-value.
     * @throws NoSuchAlgorithmException if the platform does not have MD5
     * digest support.
     * @throws IOException if an error occurs writing to the output
     * byte array buffer.
     */
    protected byte[] generateResponseValue(
        String authMethod,
        String digestUriValue,
        String qopValue,
        String usernameValue,
        String realmValue,
        char[] passwdValue,
        byte[] nonceValue,
        byte[] cNonceValue,
        int nonceCount,
        byte[] authzidValue
        ) throws NoSuchAlgorithmException,
            IOException {

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] hexA1, hexA2;
        ByteArrayOutputStream A2, beginA1, A1, KD;

        // A2
        // --
        // A2 = { "AUTHENTICATE:", digest-uri-value,
        // [:00000000000000000000000000000000] }  // if auth-int or auth-conf
        //
        A2 = new ByteArrayOutputStream();
        A2.write((authMethod + ":" + digestUriValue).getBytes(encoding));
        if (qopValue.equals("auth-conf") ||
            qopValue.equals("auth-int")) {

            logger.log(Level.FINE, "DIGEST04:QOP: {0}", qopValue);

            A2.write(SECURITY_LAYER_MARKER.getBytes(encoding));
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "DIGEST05:A2: {0}", A2.toString());
        }

        md5.update(A2.toByteArray());
        byte[] digest = md5.digest();
        hexA2 = binaryToHex(digest);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "DIGEST06:HEX(H(A2)): {0}", new String(hexA2));
        }

        // A1
        // --
        // H(user-name : realm-value : passwd)
        //
        beginA1 = new ByteArrayOutputStream();
        beginA1.write(stringToByte_8859_1(usernameValue));
        beginA1.write(':');
        // if no realm, realm will be an empty string
        beginA1.write(stringToByte_8859_1(realmValue));
        beginA1.write(':');
        beginA1.write(stringToByte_8859_1(new String(passwdValue)));

        md5.update(beginA1.toByteArray());
        digest = md5.digest();

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "DIGEST07:H({0}) = {1}",
                new Object[]{beginA1.toString(), new String(binaryToHex(digest))});
        }

        // A1
        // --
        // A1 = { H ( {user-name : realm-value : passwd } ),
        // : nonce-value, : cnonce-value : authzid-value
        //
        A1 = new ByteArrayOutputStream();
        A1.write(digest);
        A1.write(':');
        A1.write(nonceValue);
        A1.write(':');
        A1.write(cNonceValue);

        if (authzidValue != null) {
            A1.write(':');
            A1.write(authzidValue);
        }
        md5.update(A1.toByteArray());
        digest = md5.digest();
        H_A1 = digest; // Record H(A1). Use for integrity & privacy.
        hexA1 = binaryToHex(digest);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "DIGEST08:H(A1) = {0}", new String(hexA1));
        }

        //
        // H(k, : , s);
        //
        KD = new ByteArrayOutputStream();
        KD.write(hexA1);
        KD.write(':');
        KD.write(nonceValue);
        KD.write(':');
        KD.write(nonceCountToHex(nonceCount).getBytes(encoding));
        KD.write(':');
        KD.write(cNonceValue);
        KD.write(':');
        KD.write(qopValue.getBytes(encoding));
        KD.write(':');
        KD.write(hexA2);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "DIGEST09:KD: {0}", KD.toString());
        }

        md5.update(KD.toByteArray());
        digest = md5.digest();

        byte[] answer = binaryToHex(digest);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "DIGEST10:response-value: {0}",
                new String(answer));
        }
        return (answer);
    }

    /**
     * Takes 'nonceCount' value and returns HEX value of the value.
     *
     * @return A non-null String representing the current NONCE-COUNT
     */
    protected static String nonceCountToHex(int count) {

        String str = Integer.toHexString(count);
        StringBuilder pad = new StringBuilder();

        if (str.length() < 8) {
            for (int i = 0; i < 8-str.length(); i ++) {
                pad.append("0");
            }
        }

        return pad.toString() + str;
    }

    /**
     * Parses digest-challenge string, extracting each token
     * and value(s)
     *
     * @param buf A non-null digest-challenge string.
     * @param multipleAllowed true if multiple qop or realm or QOP directives
     *  are allowed.
     * @throws SaslException if the buf cannot be parsed according to RFC 2831
     */
    protected static byte[][] parseDirectives(byte[] buf,
        String[]keyTable, List<byte[]> realmChoices, int realmIndex) throws SaslException {

        byte[][] valueTable = new byte[keyTable.length][];

        ByteArrayOutputStream key = new ByteArrayOutputStream(10);
        ByteArrayOutputStream value = new ByteArrayOutputStream(10);
        boolean gettingKey = true;
        boolean gettingQuotedValue = false;
        boolean expectSeparator = false;
        byte bch;

        int i = skipLws(buf, 0);
        while (i < buf.length) {
            bch = buf[i];

            if (gettingKey) {
                if (bch == ',') {
                    if (key.size() != 0) {
                        throw new SaslException("Directive key contains a ',':" +
                            key);
                    }
                    // Empty element, skip separator and lws
                    i = skipLws(buf, i+1);

                } else if (bch == '=') {
                    if (key.size() == 0) {
                        throw new SaslException("Empty directive key");
                    }
                    gettingKey = false;      // Termination of key
                    i = skipLws(buf, i+1);   // Skip to next nonwhitespace

                    // Check whether value is quoted
                    if (i < buf.length) {
                        if (buf[i] == '"') {
                            gettingQuotedValue = true;
                            ++i; // Skip quote
                        }
                    } else {
                        throw new SaslException(
                            "Valueless directive found: " + key.toString());
                    }
                } else if (isLws(bch)) {
                    // LWS that occurs after key
                    i = skipLws(buf, i+1);

                    // Expecting '='
                    if (i < buf.length) {
                        if (buf[i] != '=') {
                            throw new SaslException("'=' expected after key: " +
                                key.toString());
                        }
                    } else {
                        throw new SaslException(
                            "'=' expected after key: " + key.toString());
                    }
                } else {
                    key.write(bch);    // Append to key
                    ++i;               // Advance
                }
            } else if (gettingQuotedValue) {
                // Getting a quoted value
                if (bch == '\\') {
                    // quoted-pair = "\" CHAR  ==> CHAR
                    ++i;       // Skip escape
                    if (i < buf.length) {
                        value.write(buf[i]);
                        ++i;   // Advance
                    } else {
                        // Trailing escape in a quoted value
                        throw new SaslException(
                            "Unmatched quote found for directive: "
                            + key.toString() + " with value: " + value.toString());
                    }
                } else if (bch == '"') {
                    // closing quote
                    ++i;  // Skip closing quote
                    gettingQuotedValue = false;
                    expectSeparator = true;
                } else {
                    value.write(bch);
                    ++i;  // Advance
                }

            } else if (isLws(bch) || bch == ',') {
                //  Value terminated

                extractDirective(key.toString(), value.toByteArray(),
                    keyTable, valueTable, realmChoices, realmIndex);
                key.reset();
                value.reset();
                gettingKey = true;
                gettingQuotedValue = expectSeparator = false;
                i = skipLws(buf, i+1);   // Skip separator and LWS

            } else if (expectSeparator) {
                throw new SaslException(
                    "Expecting comma or linear whitespace after quoted string: \""
                        + value.toString() + "\"");
            } else {
                value.write(bch);   // Unquoted value
                ++i;                // Advance
            }
        }

        if (gettingQuotedValue) {
            throw new SaslException(
                "Unmatched quote found for directive: " + key.toString() +
                " with value: " + value.toString());
        }

        // Get last pair
        if (key.size() > 0) {
            extractDirective(key.toString(), value.toByteArray(),
                keyTable, valueTable, realmChoices, realmIndex);
        }

        return valueTable;
    }

    // Is character a linear white space?
    // LWS            = [CRLF] 1*( SP | HT )
    // %%% Note that we're checking individual bytes instead of CRLF
    private static boolean isLws(byte b) {
        switch (b) {
        case 13:   // US-ASCII CR, carriage return
        case 10:   // US-ASCII LF, linefeed
        case 32:   // US-ASCII SP, space
        case 9:    // US-ASCII HT, horizontal-tab
            return true;
        }
        return false;
    }

    // Skip all linear white spaces
    private static int skipLws(byte[] buf, int start) {
        int i;
        for (i = start; i < buf.length; i++) {
            if (!isLws(buf[i])) {
                return i;
            }
        }
        return i;
    }

    /**
     * Processes directive/value pairs from the digest-challenge and
     * fill out the challengeVal array.
     *
     * @param key A non-null String challenge token name.
     * @param value A non-null String token value.
     * @throws SaslException if a either the key or the value is null
     */
    private static void  extractDirective(String key, byte[] value,
        String[] keyTable, byte[][] valueTable,
        List<byte[]> realmChoices, int realmIndex) throws SaslException {

        for (int i = 0; i < keyTable.length; i++) {
            if (key.equalsIgnoreCase(keyTable[i])) {
                if (valueTable[i] == null) {
                    valueTable[i] = value;
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "DIGEST11:Directive {0} = {1}",
                            new Object[]{
                                keyTable[i],
                                new String(valueTable[i])});
                    }
                } else if (realmChoices != null && i == realmIndex) {
                    // > 1 realm specified
                    if (realmChoices.isEmpty()) {
                        realmChoices.add(valueTable[i]); // add existing one
                    }
                    realmChoices.add(value);  // add new one
                } else {
                    throw new SaslException(
                        "DIGEST-MD5: peer sent more than one " +
                        key + " directive: " + new String(value));
                }

                break; // end search
            }
        }
     }


    /**
     * Implementation of the SecurityCtx interface allowing for messages
     * between the client and server to be integrity checked. After a
     * successful DIGEST-MD5 authentication, integtrity checking is invoked
     * if the SASL QOP (quality-of-protection) is set to 'auth-int'.
     * <p>
     * Further details on the integrity-protection mechanism can be found
     * at section 2.3 - Integrity protection in the
     * <a href="http://www.ietf.org/rfc/rfc2831.txt">RFC2831</a> definition.
     *
     * @author Jonathan Bruce
     */
    class DigestIntegrity implements SecurityCtx {
        /* Used for generating integrity keys - specified in RFC 2831*/
        private static final String CLIENT_INT_MAGIC = "Digest session key to " +
            "client-to-server signing key magic constant";
        private static final String SVR_INT_MAGIC = "Digest session key to " +
            "server-to-client signing key magic constant";

        /* Key pairs for integrity checking */
        protected byte[] myKi;     // == Kic for client; == Kis for server
        protected byte[] peerKi;   // == Kis for client; == Kic for server

        protected int mySeqNum = 0;
        protected int peerSeqNum = 0;

        // outgoing messageType and sequenceNum
        protected final byte[] messageType = new byte[2];
        protected final byte[] sequenceNum = new byte[4];

        /**
         * Initializes DigestIntegrity implementation of SecurityCtx to
         * enable DIGEST-MD5 integrity checking.
         *
         * @throws SaslException if an error is encountered generating the
         * key-pairs for integrity checking.
         */
        DigestIntegrity(boolean clientMode) throws SaslException {
            /* Initialize magic strings */

            try {
                generateIntegrityKeyPair(clientMode);

            } catch (IOException e) {
                throw new SaslException("DIGEST-MD5: Error accessing buffers " +
                    "required to create integrity key pairs", e);
            } catch (NoSuchAlgorithmException e) {
                throw new SaslException("DIGEST-MD5: Unsupported digest " +
                    "algorithm used to create integrity key pairs", e);
            }

            /* Message type is a fixed value */
            intToNetworkByteOrder(1, messageType, 0, 2);
        }

        /**
         * Generate client-server, server-client key pairs for DIGEST-MD5
         * integrity checking.
         *
         * @throws IOException if an error occurs when writing to or from the
         * byte array output buffers.
         * @throws NoSuchAlgorithmException if the MD5 message digest algorithm
         * cannot loaded.
         */
        private void generateIntegrityKeyPair(boolean clientMode)
            throws IOException, NoSuchAlgorithmException {

            byte[] cimagic = CLIENT_INT_MAGIC.getBytes(encoding);
            byte[] simagic = SVR_INT_MAGIC.getBytes(encoding);

            MessageDigest md5 = MessageDigest.getInstance("MD5");

            // Both client-magic-keys and server-magic-keys are the same length
            byte[] keyBuffer = new byte[H_A1.length + cimagic.length];

            // Kic: Key for protecting msgs from client to server.
            System.arraycopy(H_A1, 0, keyBuffer, 0, H_A1.length);
            System.arraycopy(cimagic, 0, keyBuffer, H_A1.length, cimagic.length);
            md5.update(keyBuffer);
            byte[] Kic = md5.digest();

            // Kis: Key for protecting msgs from server to client
            // No need to recopy H_A1
            System.arraycopy(simagic, 0, keyBuffer, H_A1.length, simagic.length);

            md5.update(keyBuffer);
            byte[] Kis = md5.digest();

            if (logger.isLoggable(Level.FINER)) {
                traceOutput(DI_CLASS_NAME, "generateIntegrityKeyPair",
                    "DIGEST12:Kic: ", Kic);
                traceOutput(DI_CLASS_NAME, "generateIntegrityKeyPair",
                    "DIGEST13:Kis: ", Kis);
            }

            if (clientMode) {
                myKi = Kic;
                peerKi = Kis;
            } else {
                myKi = Kis;
                peerKi = Kic;
            }
        }

        /**
         * Append MAC onto outgoing message.
         *
         * @param outgoing A non-null byte array containing the outgoing message.
         * @param start The offset from which to read the byte array.
         * @param len The non-zero number of bytes for be read from the offset.
         * @return The message including the integrity MAC
         * @throws SaslException if an error is encountered converting a string
         * into a UTF-8 byte encoding, or if the MD5 message digest algorithm
         * cannot be found or if there is an error writing to the byte array
         * output buffers.
         */
        public byte[] wrap(byte[] outgoing, int start, int len)
            throws SaslException {

            if (len == 0) {
                return EMPTY_BYTE_ARRAY;
            }

            /* wrapped = message, MAC, message type, sequence number */
            byte[] wrapped = new byte[len+10+2+4];

            /* Start with message itself */
            System.arraycopy(outgoing, start, wrapped, 0, len);

            incrementSeqNum();

            /* Calculate MAC */
            byte[] mac = getHMAC(myKi, sequenceNum, outgoing, start, len);

            if (logger.isLoggable(Level.FINEST)) {
                traceOutput(DI_CLASS_NAME, "wrap", "DIGEST14:outgoing: ",
                    outgoing, start, len);
                traceOutput(DI_CLASS_NAME, "wrap", "DIGEST15:seqNum: ",
                    sequenceNum);
                traceOutput(DI_CLASS_NAME, "wrap", "DIGEST16:MAC: ", mac);
            }

            /* Add MAC[0..9] to message */
            System.arraycopy(mac, 0, wrapped, len, 10);

            /* Add message type [0..1] */
            System.arraycopy(messageType, 0, wrapped, len+10, 2);

            /* Add sequence number [0..3] */
            System.arraycopy(sequenceNum, 0, wrapped, len+12, 4);
            if (logger.isLoggable(Level.FINEST)) {
                traceOutput(DI_CLASS_NAME, "wrap", "DIGEST17:wrapped: ", wrapped);
            }
            return wrapped;
        }

        /**
         * Return verified message without MAC - only if the received MAC
         * and re-generated MAC are the same.
         *
         * @param incoming A non-null byte array containing the incoming
         * message.
         * @param start The offset from which to read the byte array.
         * @param len The non-zero number of bytes to read from the offset
         * position.
         * @return The verified message or null if integrity checking fails.
         * @throws SaslException if an error is encountered converting a string
         * into a UTF-8 byte encoding, or if the MD5 message digest algorithm
         * cannot be found or if there is an error writing to the byte array
         * output buffers
         */
        public byte[] unwrap(byte[] incoming, int start, int len)
            throws SaslException {

            if (len == 0) {
                return EMPTY_BYTE_ARRAY;
            }

            // shave off last 16 bytes of message
            byte[] mac = new byte[10];
            byte[] msg = new byte[len - 16];
            byte[] msgType = new byte[2];
            byte[] seqNum = new byte[4];

            /* Get Msg, MAC, msgType, sequenceNum */
            System.arraycopy(incoming, start, msg, 0, msg.length);
            System.arraycopy(incoming, start+msg.length, mac, 0, 10);
            System.arraycopy(incoming, start+msg.length+10, msgType, 0, 2);
            System.arraycopy(incoming, start+msg.length+12, seqNum, 0, 4);

            /* Calculate MAC to ensure integrity */
            byte[] expectedMac = getHMAC(peerKi, seqNum, msg, 0, msg.length);

            if (logger.isLoggable(Level.FINEST)) {
                traceOutput(DI_CLASS_NAME, "unwrap", "DIGEST18:incoming: ",
                    msg);
                traceOutput(DI_CLASS_NAME, "unwrap", "DIGEST19:MAC: ",
                    mac);
                traceOutput(DI_CLASS_NAME, "unwrap", "DIGEST20:messageType: ",
                    msgType);
                traceOutput(DI_CLASS_NAME, "unwrap", "DIGEST21:sequenceNum: ",
                    seqNum);
                traceOutput(DI_CLASS_NAME, "unwrap", "DIGEST22:expectedMAC: ",
                    expectedMac);
            }

            /* First, compare MAC's before updating any of our state */
            if (!Arrays.equals(mac, expectedMac)) {
                //  Discard message and do not increment sequence number
                logger.log(Level.INFO, "DIGEST23:Unmatched MACs");
                return EMPTY_BYTE_ARRAY;
            }

            /* Ensure server-sequence numbers are correct */
            if (peerSeqNum != networkByteOrderToInt(seqNum, 0, 4)) {
                throw new SaslException("DIGEST-MD5: Out of order " +
                    "sequencing of messages from server. Got: " +
                    networkByteOrderToInt(seqNum, 0, 4) +
                    " Expected: " +     peerSeqNum);
            }

            if (!Arrays.equals(messageType, msgType)) {
                throw new SaslException("DIGEST-MD5: invalid message type: " +
                    networkByteOrderToInt(msgType, 0, 2));
            }

            // Increment sequence number and return message
            peerSeqNum++;
            return msg;
        }

        /**
         * Generates MAC to be appended onto out-going messages.
         *
         * @param Ki A non-null byte array containing the key for the digest
         * @param SeqNum A non-null byte array contain the sequence number
         * @param msg  The message to be digested
         * @param start The offset from which to read the msg byte array
         * @param len The non-zero number of bytes to be read from the offset
         * @return The MAC of a message.
         *
         * @throws SaslException if an error occurs when generating MAC.
         */
        protected byte[] getHMAC(byte[] Ki, byte[] seqnum, byte[] msg,
            int start, int len) throws SaslException {

            byte[] seqAndMsg = new byte[4+len];
            System.arraycopy(seqnum, 0, seqAndMsg, 0, 4);
            System.arraycopy(msg, start, seqAndMsg, 4, len);

            try {
                SecretKey keyKi = new SecretKeySpec(Ki, "HmacMD5");
                Mac m = Mac.getInstance("HmacMD5");
                m.init(keyKi);
                m.update(seqAndMsg);
                byte[] hMAC_MD5 = m.doFinal();

                /* First 10 bytes of HMAC_MD5 digest */
                byte[] macBuffer = new byte[10];
                System.arraycopy(hMAC_MD5, 0, macBuffer, 0, 10);

                return macBuffer;
            } catch (InvalidKeyException e) {
                throw new SaslException("DIGEST-MD5: Invalid bytes used for " +
                    "key of HMAC-MD5 hash.", e);
            } catch (NoSuchAlgorithmException e) {
                throw new SaslException("DIGEST-MD5: Error creating " +
                    "instance of MD5 digest algorithm", e);
            }
        }

        /**
         * Increment own sequence number and set answer in NBO sequenceNum field.
         */
        protected void incrementSeqNum() {
            intToNetworkByteOrder(mySeqNum++, sequenceNum, 0, 4);
        }
    }

    /**
     * Implementation of the SecurityCtx interface allowing for messages
     * between the client and server to be integrity checked and encrypted.
     * After a successful DIGEST-MD5 authentication, privacy is invoked if the
     * SASL QOP (quality-of-protection) is set to 'auth-conf'.
     * <p>
     * Further details on the integrity-protection mechanism can be found
     * at section 2.4 - Confidentiality protection in
     * <a href="http://www.ietf.org/rfc/rfc2831.txt">RFC2831</a> definition.
     *
     * @author Jonathan Bruce
     */
    final class DigestPrivacy extends DigestIntegrity implements SecurityCtx {
        /* Used for generating privacy keys - specified in RFC 2831 */
        private static final String CLIENT_CONF_MAGIC =
            "Digest H(A1) to client-to-server sealing key magic constant";
        private static final String SVR_CONF_MAGIC =
            "Digest H(A1) to server-to-client sealing key magic constant";

        private Cipher encCipher;
        private Cipher decCipher;

        /**
         * Initializes the cipher object instances for encryption and decryption.
         *
         * @throws SaslException if an error occurs with the Key
         * initialization, or a string cannot be encoded into a byte array
         * using the UTF-8 encoding, or an error occurs when writing to a
         * byte array output buffers or the mechanism cannot load the MD5
         * message digest algorithm or invalid initialization parameters are
         * passed to the cipher object instances.
         */
        DigestPrivacy(boolean clientMode) throws SaslException {

            super(clientMode); // generate Kic, Kis keys for integrity-checking.

            try {
                generatePrivacyKeyPair(clientMode);

            } catch (SaslException e) {
                throw e;
            } catch (IOException e) {
                throw new SaslException("DIGEST-MD5: Error accessing " +
                    "buffers required to generate cipher keys", e);
            } catch (NoSuchAlgorithmException e) {
                throw new SaslException("DIGEST-MD5: Error creating " +
                    "instance of required cipher or digest", e);
            }
        }

        /**
         * Generates client-server and server-client keys to encrypt and
         * decrypt messages. Also generates IVs for DES ciphers.
         *
         * @throws IOException if an error occurs when writing to or from the
         * byte array output buffers.
         * @throws NoSuchAlgorithmException if the MD5 message digest algorithm
         * cannot loaded.
         * @throws SaslException if an error occurs initializing the keys and
         * IVs for the chosen cipher.
         */
        private void generatePrivacyKeyPair(boolean clientMode)
            throws IOException, NoSuchAlgorithmException, SaslException {

            byte[] ccmagic = CLIENT_CONF_MAGIC.getBytes(encoding);
            byte[] scmagic = SVR_CONF_MAGIC.getBytes(encoding);

            /* Kcc = MD5{H(A1)[0..n], "Digest ... client-to-server"} */
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            int n;
            if (negotiatedCipher.equals(CIPHER_TOKENS[RC4_40])) {
                n = 5;          /* H(A1)[0..5] */
            } else if (negotiatedCipher.equals(CIPHER_TOKENS[RC4_56])) {
                n = 7;          /* H(A1)[0..7] */
            } else { // des and 3des and rc4
                n = 16;         /* H(A1)[0..16] */
            }

            /* {H(A1)[0..n], "Digest ... client-to-server..."} */
            // Both client-magic-keys and server-magic-keys are the same length
            byte[] keyBuffer =  new byte[n + ccmagic.length];
            System.arraycopy(H_A1, 0, keyBuffer, 0, n);   // H(A1)[0..n]

            /* Kcc: Key for encrypting messages from client->server */
            System.arraycopy(ccmagic, 0, keyBuffer, n, ccmagic.length);
            md5.update(keyBuffer);
            byte[] Kcc = md5.digest();

            /* Kcs: Key for decrypting messages from server->client */
            // No need to copy H_A1 again since it hasn't changed
            System.arraycopy(scmagic, 0, keyBuffer, n, scmagic.length);
            md5.update(keyBuffer);
            byte[] Kcs = md5.digest();

            if (logger.isLoggable(Level.FINER)) {
                traceOutput(DP_CLASS_NAME, "generatePrivacyKeyPair",
                    "DIGEST24:Kcc: ", Kcc);
                traceOutput(DP_CLASS_NAME, "generatePrivacyKeyPair",
                    "DIGEST25:Kcs: ", Kcs);
            }

            byte[] myKc;
            byte[] peerKc;

            if (clientMode) {
                myKc = Kcc;
                peerKc = Kcs;
            } else {
                myKc = Kcs;
                peerKc = Kcc;
            }

            try {
                SecretKey encKey;
                SecretKey decKey;

                /* Initialize cipher objects */
                if (negotiatedCipher.indexOf(CIPHER_TOKENS[RC4]) > -1) {
                    encCipher = Cipher.getInstance("RC4");
                    decCipher = Cipher.getInstance("RC4");

                    encKey = new SecretKeySpec(myKc, "RC4");
                    decKey = new SecretKeySpec(peerKc, "RC4");

                    encCipher.init(Cipher.ENCRYPT_MODE, encKey);
                    decCipher.init(Cipher.DECRYPT_MODE, decKey);

                } else if ((negotiatedCipher.equals(CIPHER_TOKENS[DES])) ||
                    (negotiatedCipher.equals(CIPHER_TOKENS[DES3]))) {

                    // DES or 3DES
                    String cipherFullname, cipherShortname;

                        // Use "NoPadding" when specifying cipher names
                        // RFC 2831 already defines padding rules for producing
                        // 8-byte aligned blocks
                    if (negotiatedCipher.equals(CIPHER_TOKENS[DES])) {
                        cipherFullname = "DES/CBC/NoPadding";
                        cipherShortname = "des";
                    } else {
                        /* 3DES */
                        cipherFullname = "DESede/CBC/NoPadding";
                        cipherShortname = "desede";
                    }

                    encCipher = Cipher.getInstance(cipherFullname);
                    decCipher = Cipher.getInstance(cipherFullname);

                    encKey = makeDesKeys(myKc, cipherShortname);
                    decKey = makeDesKeys(peerKc, cipherShortname);

                    // Set up the DES IV, which is the last 8 bytes of Kcc/Kcs
                    IvParameterSpec encIv = new IvParameterSpec(myKc, 8, 8);
                    IvParameterSpec decIv = new IvParameterSpec(peerKc, 8, 8);

                    // Initialize cipher objects
                    encCipher.init(Cipher.ENCRYPT_MODE, encKey, encIv);
                    decCipher.init(Cipher.DECRYPT_MODE, decKey, decIv);

                    if (logger.isLoggable(Level.FINER)) {
                        traceOutput(DP_CLASS_NAME, "generatePrivacyKeyPair",
                            "DIGEST26:" + negotiatedCipher + " IVcc: ",
                            encIv.getIV());
                        traceOutput(DP_CLASS_NAME, "generatePrivacyKeyPair",
                            "DIGEST27:" + negotiatedCipher + " IVcs: ",
                            decIv.getIV());
                        traceOutput(DP_CLASS_NAME, "generatePrivacyKeyPair",
                            "DIGEST28:" + negotiatedCipher + " encryption key: ",
                            encKey.getEncoded());
                        traceOutput(DP_CLASS_NAME, "generatePrivacyKeyPair",
                            "DIGEST29:" + negotiatedCipher + " decryption key: ",
                            decKey.getEncoded());
                    }
                }
            } catch (InvalidKeySpecException e) {
                throw new SaslException("DIGEST-MD5: Unsupported key " +
                    "specification used.", e);
            } catch (InvalidAlgorithmParameterException e) {
                throw new SaslException("DIGEST-MD5: Invalid cipher " +
                    "algorithem parameter used to create cipher instance", e);
            } catch (NoSuchPaddingException e) {
                throw new SaslException("DIGEST-MD5: Unsupported " +
                    "padding used for chosen cipher", e);
            } catch (InvalidKeyException e) {
                throw new SaslException("DIGEST-MD5: Invalid data " +
                    "used to initialize keys", e);
            }
        }

        // -------------------------------------------------------------------

        /**
         * Encrypt out-going message.
         *
         * @param outgoing A non-null byte array containing the outgoing message.
         * @param start The offset from which to read the byte array.
         * @param len The non-zero number of bytes to be read from the offset.
         * @return The encrypted message.
         *
         * @throws SaslException if an error occurs when writing to or from the
         * byte array output buffers or if the MD5 message digest algorithm
         * cannot loaded or if an UTF-8 encoding is not supported on the
         * platform.
         */
        public byte[] wrap(byte[] outgoing, int start, int len)
            throws SaslException {

            if (len == 0) {
                return EMPTY_BYTE_ARRAY;
            }

            /* HMAC(Ki, {SeqNum, msg})[0..9] */
            incrementSeqNum();
            byte[] mac = getHMAC(myKi, sequenceNum, outgoing, start, len);

            if (logger.isLoggable(Level.FINEST)) {
                traceOutput(DP_CLASS_NAME, "wrap", "DIGEST30:Outgoing: ",
                    outgoing, start, len);
                traceOutput(DP_CLASS_NAME, "wrap", "seqNum: ",
                    sequenceNum);
                traceOutput(DP_CLASS_NAME, "wrap", "MAC: ", mac);
            }

            // Calculate padding
            int bs = encCipher.getBlockSize();
            byte[] padding;
            if (bs > 1 ) {
                int pad = bs - ((len + 10) % bs); // add 10 for HMAC[0..9]
                padding = new byte[pad];
                for (int i=0; i < pad; i++) {
                    padding[i] = (byte)pad;
                }
            } else {
                padding = EMPTY_BYTE_ARRAY;
            }

            byte[] toBeEncrypted = new byte[len+padding.length+10];

            /* {msg, pad, HMAC(Ki, {SeqNum, msg}[0..9])} */
            System.arraycopy(outgoing, start, toBeEncrypted, 0, len);
            System.arraycopy(padding, 0, toBeEncrypted, len, padding.length);
            System.arraycopy(mac, 0, toBeEncrypted, len+padding.length, 10);

            if (logger.isLoggable(Level.FINEST)) {
                traceOutput(DP_CLASS_NAME, "wrap",
                    "DIGEST31:{msg, pad, KicMAC}: ", toBeEncrypted);
            }

            /* CIPHER(Kc, {msg, pad, HMAC(Ki, {SeqNum, msg}[0..9])}) */
            byte[] cipherBlock;
            try {
                // Do CBC (chaining) across packets
                cipherBlock = encCipher.update(toBeEncrypted);

                if (cipherBlock == null) {
                    // update() can return null
                    throw new IllegalBlockSizeException(""+toBeEncrypted.length);
                }
            } catch (IllegalBlockSizeException e) {
                throw new SaslException(
                    "DIGEST-MD5: Invalid block size for cipher", e);
            }

            byte[] wrapped = new byte[cipherBlock.length+2+4];
            System.arraycopy(cipherBlock, 0, wrapped, 0, cipherBlock.length);
            System.arraycopy(messageType, 0, wrapped, cipherBlock.length, 2);
            System.arraycopy(sequenceNum, 0, wrapped, cipherBlock.length+2, 4);

            if (logger.isLoggable(Level.FINEST)) {
                traceOutput(DP_CLASS_NAME, "wrap", "DIGEST32:Wrapped: ", wrapped);
            }

            return wrapped;
        }

        /*
         * Decrypt incoming messages and verify their integrity.
         *
         * @param incoming A non-null byte array containing the incoming
         * encrypted message.
         * @param start The offset from which to read the byte array.
         * @param len The non-zero number of bytes to read from the offset
         * position.
         * @return The decrypted, verified message or null if integrity
         * checking
         * fails.
         * @throws SaslException if there are the SASL buffer is empty or if
         * if an error occurs reading the SASL buffer.
         */
        public byte[] unwrap(byte[] incoming, int start, int len)
            throws SaslException {

            if (len == 0) {
                return EMPTY_BYTE_ARRAY;
            }

            byte[] encryptedMsg = new byte[len - 6];
            byte[] msgType = new byte[2];
            byte[] seqNum = new byte[4];

            /* Get cipherMsg; msgType; sequenceNum */
            System.arraycopy(incoming, start,
                encryptedMsg, 0, encryptedMsg.length);
            System.arraycopy(incoming, start+encryptedMsg.length,
                msgType, 0, 2);
            System.arraycopy(incoming, start+encryptedMsg.length+2,
                seqNum, 0, 4);

            if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST,
                    "DIGEST33:Expecting sequence num: {0}",
                    peerSeqNum);
                traceOutput(DP_CLASS_NAME, "unwrap", "DIGEST34:incoming: ",
                    encryptedMsg);
            }

            // Decrypt message
            /* CIPHER(Kc, {msg, pad, HMAC(Ki, {SeqNum, msg}[0..9])}) */
            byte[] decryptedMsg;

            try {
                // Do CBC (chaining) across packets
                decryptedMsg = decCipher.update(encryptedMsg);

                if (decryptedMsg == null) {
                    // update() can return null
                    throw new IllegalBlockSizeException(""+encryptedMsg.length);
                }
            } catch (IllegalBlockSizeException e) {
                throw new SaslException("DIGEST-MD5: Illegal block " +
                    "sizes used with chosen cipher", e);
            }

            byte[] msgWithPadding = new byte[decryptedMsg.length - 10];
            byte[] mac = new byte[10];

            System.arraycopy(decryptedMsg, 0,
                msgWithPadding, 0, msgWithPadding.length);
            System.arraycopy(decryptedMsg, msgWithPadding.length,
                mac, 0, 10);

            if (logger.isLoggable(Level.FINEST)) {
                traceOutput(DP_CLASS_NAME, "unwrap",
                    "DIGEST35:Unwrapped (w/padding): ", msgWithPadding);
                traceOutput(DP_CLASS_NAME, "unwrap", "DIGEST36:MAC: ", mac);
                traceOutput(DP_CLASS_NAME, "unwrap", "DIGEST37:messageType: ",
                    msgType);
                traceOutput(DP_CLASS_NAME, "unwrap", "DIGEST38:sequenceNum: ",
                    seqNum);
            }

            int msgLength = msgWithPadding.length;
            int blockSize = decCipher.getBlockSize();
            if (blockSize > 1) {
                // get value of last octet of the byte array
                msgLength -= (int)msgWithPadding[msgWithPadding.length - 1];
                if (msgLength < 0) {
                    //  Discard message and do not increment sequence number
                    if (logger.isLoggable(Level.INFO)) {
                        logger.log(Level.INFO,
                            "DIGEST39:Incorrect padding: {0}",
                            msgWithPadding[msgWithPadding.length - 1]);
                    }
                    return EMPTY_BYTE_ARRAY;
                }
            }

            /* Re-calculate MAC to ensure integrity */
            byte[] expectedMac = getHMAC(peerKi, seqNum, msgWithPadding,
                0, msgLength);

            if (logger.isLoggable(Level.FINEST)) {
                traceOutput(DP_CLASS_NAME, "unwrap", "DIGEST40:KisMAC: ",
                    expectedMac);
            }

            // First, compare MACs before updating state
            if (!Arrays.equals(mac, expectedMac)) {
                //  Discard message and do not increment sequence number
                logger.log(Level.INFO, "DIGEST41:Unmatched MACs");
                return EMPTY_BYTE_ARRAY;
            }

            /* Ensure sequence number is correct */
            if (peerSeqNum != networkByteOrderToInt(seqNum, 0, 4)) {
                throw new SaslException("DIGEST-MD5: Out of order " +
                    "sequencing of messages from server. Got: " +
                    networkByteOrderToInt(seqNum, 0, 4) + " Expected: " +
                    peerSeqNum);
            }

            /* Check message type */
            if (!Arrays.equals(messageType, msgType)) {
                throw new SaslException("DIGEST-MD5: invalid message type: " +
                    networkByteOrderToInt(msgType, 0, 2));
            }

            // Increment sequence number and return message
            peerSeqNum++;

            if (msgLength == msgWithPadding.length) {
                return msgWithPadding; // no padding
            } else {
                // Get a copy of the message without padding
                byte[] clearMsg = new byte[msgLength];
                System.arraycopy(msgWithPadding, 0, clearMsg, 0, msgLength);
                return clearMsg;
            }
        }
    }

    // ---------------- DES and 3 DES key manipulation routines

    private static final BigInteger MASK = new BigInteger("7f", 16);

    /**
     * Sets the parity bit (0th bit) in each byte so that each byte
     * contains an odd number of 1's.
     */
    private static void setParityBit(byte[] key) {
        for (int i = 0; i < key.length; i++) {
            int b = key[i] & 0xfe;
            b |= (Integer.bitCount(b) & 1) ^ 1;
            key[i] = (byte) b;
        }
    }

    /**
     * Expands a 7-byte array into an 8-byte array that contains parity bits
     * The binary format of a cryptographic key is:
     *     (B1,B2,...,B7,P1,B8,...B14,P2,B15,...,B49,P7,B50,...,B56,P8)
     * where (B1,B2,...,B56) are the independent bits of a DES key and
     * (PI,P2,...,P8) are reserved for parity bits computed on the preceding
     * seven independent bits and set so that the parity of the octet is odd,
     * i.e., there is an odd number of "1" bits in the octet.
     */
    private static byte[] addDesParity(byte[] input, int offset, int len) {
        if (len != 7)
            throw new IllegalArgumentException(
                "Invalid length of DES Key Value:" + len);

        byte[] raw = new byte[7];
        System.arraycopy(input, offset, raw, 0, len);

        byte[] result = new byte[8];
        BigInteger in = new BigInteger(raw);

        // Shift 7 bits each time into a byte
        for (int i=result.length-1; i>=0; i--) {
            result[i] = in.and(MASK).toByteArray()[0];
            result[i] <<= 1;         // make room for parity bit
            in = in.shiftRight(7);
        }
        setParityBit(result);
        return result;
    }

    /**
     * Create parity-adjusted keys suitable for DES / DESede encryption.
     *
     * @param input A non-null byte array containing key material for
     * DES / DESede.
     * @param desStrength A string specifying eithe a DES or a DESede key.
     * @return SecretKey An instance of either DESKeySpec or DESedeKeySpec.
     *
     * @throws NoSuchAlgorithmException if the either the DES or DESede
     * algorithms cannote be lodaed by JCE.
     * @throws InvalidKeyException if an invalid array of bytes is used
     * as a key for DES or DESede.
     * @throws InvalidKeySpecException in an invalid parameter is passed
     * to either te DESKeySpec of the DESedeKeySpec constructors.
     */
    private static SecretKey makeDesKeys(byte[] input, String desStrength)
        throws NoSuchAlgorithmException, InvalidKeyException,
            InvalidKeySpecException {

        // Generate first subkey using first 7 bytes
        byte[] subkey1 = addDesParity(input, 0, 7);

        KeySpec spec = null;
        SecretKeyFactory desFactory =
            SecretKeyFactory.getInstance(desStrength);
        switch (desStrength) {
            case "des":
                spec = new DESKeySpec(subkey1, 0);
                if (logger.isLoggable(Level.FINEST)) {
                    traceOutput(DP_CLASS_NAME, "makeDesKeys",
                        "DIGEST42:DES key input: ", input);
                    traceOutput(DP_CLASS_NAME, "makeDesKeys",
                        "DIGEST43:DES key parity-adjusted: ", subkey1);
                    traceOutput(DP_CLASS_NAME, "makeDesKeys",
                        "DIGEST44:DES key material: ", ((DESKeySpec)spec).getKey());
                    logger.log(Level.FINEST, "DIGEST45: is parity-adjusted? {0}",
                        Boolean.valueOf(DESKeySpec.isParityAdjusted(subkey1, 0)));
                }
                break;
            case "desede":
                // Generate second subkey using second 7 bytes
                byte[] subkey2 = addDesParity(input, 7, 7);
                // Construct 24-byte encryption-decryption-encryption sequence
                byte[] ede = new byte[subkey1.length*2+subkey2.length];
                System.arraycopy(subkey1, 0, ede, 0, subkey1.length);
                System.arraycopy(subkey2, 0, ede, subkey1.length, subkey2.length);
                System.arraycopy(subkey1, 0, ede, subkey1.length+subkey2.length,
                    subkey1.length);
                spec = new DESedeKeySpec(ede, 0);
                if (logger.isLoggable(Level.FINEST)) {
                    traceOutput(DP_CLASS_NAME, "makeDesKeys",
                        "DIGEST46:3DES key input: ", input);
                    traceOutput(DP_CLASS_NAME, "makeDesKeys",
                        "DIGEST47:3DES key ede: ", ede);
                    traceOutput(DP_CLASS_NAME, "makeDesKeys",
                        "DIGEST48:3DES key material: ",
                        ((DESedeKeySpec)spec).getKey());
                    logger.log(Level.FINEST, "DIGEST49: is parity-adjusted? ",
                        Boolean.valueOf(DESedeKeySpec.isParityAdjusted(ede, 0)));
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid DES strength:" +
                    desStrength);
        }
        return desFactory.generateSecret(spec);
    }
}
