/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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


package com.sun.security.sasl.gsskerb;

import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import javax.security.sasl.*;
import com.sun.security.sasl.util.AbstractSaslImpl;
import org.ietf.jgss.*;
import com.sun.security.jgss.ExtendedGSSContext;
import com.sun.security.jgss.InquireType;

abstract class GssKrb5Base extends AbstractSaslImpl {

    private static final String KRB5_OID_STR = "1.2.840.113554.1.2.2";
    protected static Oid KRB5_OID;
    protected static final byte[] EMPTY = new byte[0];

    static {
        try {
            KRB5_OID = new Oid(KRB5_OID_STR);
        } catch (GSSException ignore) {}
    }

    protected GSSContext secCtx = null;
    protected static final int JGSS_QOP = 0;    // unrelated to SASL QOP mask

    protected GssKrb5Base(Map<String, ?> props, String className)
        throws SaslException {
        super(props, className);
    }

    /**
     * Retrieves this mechanism's name.
     *
     * @return  The string "GSSAPI".
     */
    public String getMechanismName() {
        return "GSSAPI";
    }

    @Override
    public Object getNegotiatedProperty(String propName) {
        if (!completed) {
            throw new IllegalStateException("Authentication incomplete");
        }
        String xprefix = "com.sun.security.jgss.inquiretype.";
        if (propName.startsWith(xprefix)) {
            String type = propName.substring(xprefix.length());
            if (logger.isLoggable(Level.FINEST)) {
                logger.logp(Level.FINE, "GssKrb5Base",
                        "getNegotiatedProperty", propName);
            }
            for (InquireType t: InquireType.values()) {
                if (t.name().toLowerCase(Locale.US).equals(type)) {
                    try {
                        return ((ExtendedGSSContext)secCtx).inquireSecContext(t);
                    } catch (GSSException e) {
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.WARNING, "inquireSecContext error", e);
                        }
                        return null;
                    }
                }
            }
            // No such InquireType. Although not likely to be defined
            // as a property in a parent class, still try it.
        }
        return super.getNegotiatedProperty(propName);
    }

    public byte[] unwrap(byte[] incoming, int start, int len)
        throws SaslException {
        if (!completed) {
            throw new IllegalStateException("GSSAPI authentication not completed");
        }

        // integrity will be true if either privacy or integrity negotiated
        if (!integrity) {
            throw new IllegalStateException("No security layer negotiated");
        }

        try {
            MessageProp msgProp = new MessageProp(JGSS_QOP, false);
            byte[] answer = secCtx.unwrap(incoming, start, len, msgProp);
            if (privacy && !msgProp.getPrivacy()) {
                throw new SaslException("Privacy not protected");
            }
            checkMessageProp("", msgProp);
            if (logger.isLoggable(Level.FINEST)) {
                traceOutput(myClassName, "KRB501:Unwrap", "incoming: ",
                    incoming, start, len);
                traceOutput(myClassName, "KRB502:Unwrap", "unwrapped: ",
                    answer, 0, answer.length);
            }
            return answer;
        } catch (GSSException e) {
            throw new SaslException("Problems unwrapping SASL buffer", e);
        }
    }

    public byte[] wrap(byte[] outgoing, int start, int len) throws SaslException {
        if (!completed) {
            throw new IllegalStateException("GSSAPI authentication not completed");
        }

        // integrity will be true if either privacy or integrity negotiated
        if (!integrity) {
            throw new IllegalStateException("No security layer negotiated");
        }

        // Generate GSS token
        try {
            MessageProp msgProp = new MessageProp(JGSS_QOP, privacy);
            byte[] answer = secCtx.wrap(outgoing, start, len, msgProp);
            if (logger.isLoggable(Level.FINEST)) {
                traceOutput(myClassName, "KRB503:Wrap", "outgoing: ",
                    outgoing, start, len);
                traceOutput(myClassName, "KRB504:Wrap", "wrapped: ",
                    answer, 0, answer.length);
            }
            return answer;

        } catch (GSSException e) {
            throw new SaslException("Problem performing GSS wrap", e);
        }
    }

    public void dispose() throws SaslException {
        if (secCtx != null) {
            try {
                secCtx.dispose();
            } catch (GSSException e) {
                throw new SaslException("Problem disposing GSS context", e);
            }
            secCtx = null;
        }
    }

    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        dispose();
    }

    void checkMessageProp(String label, MessageProp msgProp)
            throws SaslException {
        if (msgProp.isDuplicateToken()) {
            throw new SaslException(label + "Duplicate token");
        }
        if (msgProp.isGapToken()) {
            throw new SaslException(label + "Gap token");
        }
        if (msgProp.isOldToken()) {
            throw new SaslException(label + "Old token");
        }
        if (msgProp.isUnseqToken()) {
            throw new SaslException(label + "Token not in sequence");
        }
    }
}
