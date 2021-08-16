/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.security.sasl.util;

import javax.security.sasl.*;
import java.io.*;
import java.util.Map;
import java.util.StringTokenizer;

import java.util.logging.Logger;
import java.util.logging.Level;

import sun.security.util.HexDumpEncoder;

/**
 * The base class used by client and server implementations of SASL
 * mechanisms to process properties passed in the props argument
 * and strings with the same format (e.g., used in digest-md5).
 *
 * Also contains utilities for doing int to network-byte-order
 * transformations.
 *
 * @author Rosanna Lee
 */
public abstract class AbstractSaslImpl {

    protected boolean completed = false;
    protected boolean privacy = false;
    protected boolean integrity = false;
    protected byte[] qop;           // ordered list of qops
    protected byte allQop;          // a mask indicating which QOPs are requested
    protected byte[] strength;      // ordered list of cipher strengths

    // These are relevant only when privacy or integray have been negotiated
    protected int sendMaxBufSize = 0;     // specified by peer but can override
    protected int recvMaxBufSize = 65536; // optionally specified by self
    protected int rawSendSize;            // derived from sendMaxBufSize

    protected String myClassName;

    protected AbstractSaslImpl(Map<String, ?> props, String className)
            throws SaslException {
        myClassName = className;

        // Parse properties  to set desired context options
        if (props != null) {
            String prop;

            // "auth", "auth-int", "auth-conf"
            qop = parseQop(prop=(String)props.get(Sasl.QOP));
            logger.logp(Level.FINE, myClassName, "constructor",
                "SASLIMPL01:Preferred qop property: {0}", prop);
            allQop = combineMasks(qop);

            if (logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, myClassName, "constructor",
                    "SASLIMPL02:Preferred qop mask: {0}", allQop);

                if (qop.length > 0) {
                    StringBuilder str = new StringBuilder();
                    for (int i = 0; i < qop.length; i++) {
                        str.append(Byte.toString(qop[i]));
                        str.append(' ');
                    }
                    logger.logp(Level.FINE, myClassName, "constructor",
                            "SASLIMPL03:Preferred qops : {0}", str.toString());
                }
            }

            // "low", "medium", "high"
            strength = parseStrength(prop=(String)props.get(Sasl.STRENGTH));
            logger.logp(Level.FINE, myClassName, "constructor",
                "SASLIMPL04:Preferred strength property: {0}", prop);
            if (logger.isLoggable(Level.FINE) && strength.length > 0) {
                StringBuilder str = new StringBuilder();
                for (int i = 0; i < strength.length; i++) {
                    str.append(Byte.toString(strength[i]));
                    str.append(' ');
                }
                logger.logp(Level.FINE, myClassName, "constructor",
                        "SASLIMPL05:Cipher strengths: {0}", str.toString());
            }

            // Max receive buffer size
            prop = (String)props.get(Sasl.MAX_BUFFER);
            if (prop != null) {
                try {
                    logger.logp(Level.FINE, myClassName, "constructor",
                        "SASLIMPL06:Max receive buffer size: {0}", prop);
                    recvMaxBufSize = Integer.parseInt(prop);
                } catch (NumberFormatException e) {
                    throw new SaslException(
                "Property must be string representation of integer: " +
                        Sasl.MAX_BUFFER);
                }
            }

            // Max send buffer size
            prop = (String)props.get(MAX_SEND_BUF);
            if (prop != null) {
                try {
                    logger.logp(Level.FINE, myClassName, "constructor",
                        "SASLIMPL07:Max send buffer size: {0}", prop);
                    sendMaxBufSize = Integer.parseInt(prop);
                } catch (NumberFormatException e) {
                    throw new SaslException(
                "Property must be string representation of integer: " +
                        MAX_SEND_BUF);
                }
            }
        } else {
            qop = DEFAULT_QOP;
            allQop = NO_PROTECTION;
            strength = STRENGTH_MASKS;
        }
    }

    /**
     * Determines whether this mechanism has completed.
     *
     * @return true if has completed; false otherwise;
     */
    public boolean isComplete() {
        return completed;
    }

    /**
     * Retrieves the negotiated property.
     * @exception IllegalStateException if this authentication exchange has
     * not completed
     */
    public Object getNegotiatedProperty(String propName) {
        if (!completed) {
            throw new IllegalStateException("SASL authentication not completed");
        }
        switch (propName) {
            case Sasl.QOP:
                if (privacy) {
                    return "auth-conf";
                } else if (integrity) {
                    return "auth-int";
                } else {
                    return "auth";
                }
            case Sasl.MAX_BUFFER:
                return Integer.toString(recvMaxBufSize);
            case Sasl.RAW_SEND_SIZE:
                return Integer.toString(rawSendSize);
            case MAX_SEND_BUF:
                return Integer.toString(sendMaxBufSize);
            default:
                return null;
        }
    }

    protected static final byte combineMasks(byte[] in) {
        byte answer = 0;
        for (int i = 0; i < in.length; i++) {
            answer |= in[i];
        }
        return answer;
    }

    protected static final byte findPreferredMask(byte pref, byte[] in) {
        for (int i = 0; i < in.length; i++) {
            if ((in[i]&pref) != 0) {
                return in[i];
            }
        }
        return (byte)0;
    }

    private static final byte[] parseQop(String qop) throws SaslException {
        return parseQop(qop, null, false);
    }

    protected static final byte[] parseQop(String qop, String[] saveTokens,
        boolean ignore) throws SaslException {
        if (qop == null) {
            return DEFAULT_QOP;   // default
        }

        return parseProp(Sasl.QOP, qop, QOP_TOKENS, QOP_MASKS, saveTokens, ignore);
    }

    private static final byte[] parseStrength(String strength)
        throws SaslException {
        if (strength == null) {
            return DEFAULT_STRENGTH;   // default
        }

        return parseProp(Sasl.STRENGTH, strength, STRENGTH_TOKENS,
            STRENGTH_MASKS, null, false);
    }

    private static final byte[] parseProp(String propName, String propVal,
        String[] vals, byte[] masks, String[] tokens, boolean ignore)
        throws SaslException {

        StringTokenizer parser = new StringTokenizer(propVal, ", \t\n");
        String token;
        byte[] answer = new byte[vals.length];
        int i = 0;
        boolean found;

        while (parser.hasMoreTokens() && i < answer.length) {
            token = parser.nextToken();
            found = false;
            for (int j = 0; !found && j < vals.length; j++) {
                if (token.equalsIgnoreCase(vals[j])) {
                    found = true;
                    answer[i++] = masks[j];
                    if (tokens != null) {
                        tokens[j] = token;    // save what was parsed
                    }
                }
            }
            if (!found && !ignore) {
                throw new SaslException(
                    "Invalid token in " + propName + ": " + propVal);
            }
        }
        // Initialize rest of array with 0
        for (int j = i; j < answer.length; j++) {
            answer[j] = 0;
        }
        return answer;
    }


    /**
     * Outputs a byte array. Can be null.
     */
    protected static final void traceOutput(String srcClass, String srcMethod,
        String traceTag, byte[] output) {
        traceOutput(srcClass, srcMethod, traceTag, output, 0,
                output == null ? 0 : output.length);
    }

    protected static final void traceOutput(String srcClass, String srcMethod,
        String traceTag, byte[] output, int offset, int len) {
        try {
            int origlen = len;
            Level lev;

            if (!logger.isLoggable(Level.FINEST)) {
                len = Math.min(16, len);
                lev = Level.FINER;
            } else {
                lev = Level.FINEST;
            }

            String content;

            if (output != null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(len);
                new HexDumpEncoder().encodeBuffer(
                    new ByteArrayInputStream(output, offset, len), out);
                content = out.toString();
            } else {
                content = "NULL";
            }

            // Message id supplied by caller as part of traceTag
            logger.logp(lev, srcClass, srcMethod, "{0} ( {1} ): {2}",
                new Object[] {traceTag, origlen, content});
        } catch (Exception e) {
            logger.logp(Level.WARNING, srcClass, srcMethod,
                "SASLIMPL09:Error generating trace output: {0}", e);
        }
    }


    /**
     * Returns the integer represented by  4 bytes in network byte order.
     */
    protected static final int networkByteOrderToInt(byte[] buf, int start,
        int count) {
        if (count > 4) {
            throw new IllegalArgumentException("Cannot handle more than 4 bytes");
        }

        int answer = 0;

        for (int i = 0; i < count; i++) {
            answer <<= 8;
            answer |= ((int)buf[start+i] & 0xff);
        }
        return answer;
    }

    /**
     * Encodes an integer into 4 bytes in network byte order in the buffer
     * supplied.
     */
    protected static final void intToNetworkByteOrder(int num, byte[] buf,
        int start, int count) {
        if (count > 4) {
            throw new IllegalArgumentException("Cannot handle more than 4 bytes");
        }

        for (int i = count-1; i >= 0; i--) {
            buf[start+i] = (byte)(num & 0xff);
            num >>>= 8;
        }
    }

    // ---------------- Constants  -----------------
    private static final String SASL_LOGGER_NAME = "javax.security.sasl";
    protected static final String MAX_SEND_BUF = "javax.security.sasl.sendmaxbuffer";

    /**
     * Logger for debug messages
     */
    protected static final Logger logger = Logger.getLogger(SASL_LOGGER_NAME);

    // default 0 (no protection); 1 (integrity only)
    protected static final byte NO_PROTECTION = (byte)1;
    protected static final byte INTEGRITY_ONLY_PROTECTION = (byte)2;
    protected static final byte PRIVACY_PROTECTION = (byte)4;

    protected static final byte LOW_STRENGTH = (byte)1;
    protected static final byte MEDIUM_STRENGTH = (byte)2;
    protected static final byte HIGH_STRENGTH = (byte)4;

    private static final byte[] DEFAULT_QOP = new byte[]{NO_PROTECTION};
    private static final String[] QOP_TOKENS = {"auth-conf",
                                       "auth-int",
                                       "auth"};
    private static final byte[] QOP_MASKS = {PRIVACY_PROTECTION,
                                     INTEGRITY_ONLY_PROTECTION,
                                     NO_PROTECTION};

    private static final byte[] DEFAULT_STRENGTH = new byte[]{
        HIGH_STRENGTH, MEDIUM_STRENGTH, LOW_STRENGTH};
    private static final String[] STRENGTH_TOKENS = {"low",
                                                     "medium",
                                                     "high"};
    private static final byte[] STRENGTH_MASKS = {LOW_STRENGTH,
                                                  MEDIUM_STRENGTH,
                                                  HIGH_STRENGTH};
}
