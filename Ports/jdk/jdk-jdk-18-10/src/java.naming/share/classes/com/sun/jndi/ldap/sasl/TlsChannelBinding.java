/*
 * Copyright (c) 2020, Azul Systems, Inc. All rights reserved.
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
package com.sun.jndi.ldap.sasl;

import javax.naming.NamingException;
import javax.security.sasl.SaslException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Hashtable;

/**
 * This class implements the Channel Binding for TLS as defined in
 * <a href="https://www.ietf.org/rfc/rfc5929.txt">
 *     Channel Bindings for TLS</a>
 *
 * Format of the Channel Binding data is also defined in
 * <a href="https://www.ietf.org/rfc/rfc5056.txt">
 *     On the Use of Channel Bindings to Secure Channels</a>
 * section 2.1.
 *
 */

public class TlsChannelBinding {

    // TLS channel binding type property
    public static final String CHANNEL_BINDING_TYPE =
            "com.sun.jndi.ldap.tls.cbtype";

    // internal TLS channel binding property
    public static final String CHANNEL_BINDING =
            "jdk.internal.sasl.tlschannelbinding";

    public enum TlsChannelBindingType {

        /**
         * Channel binding on the basis of TLS Finished message.
         * TLS_UNIQUE is defined by RFC 5929 but is not supported
         * by the current LDAP stack.
         */
        TLS_UNIQUE("tls-unique"),

        /**
         * Channel binding on the basis of TLS server certificate.
         */
        TLS_SERVER_END_POINT("tls-server-end-point");

        public String getName() {
            return name;
        }

        private final String name;
        TlsChannelBindingType(String name) {
            this.name = name;
        }
    }

    /**
     * Parse value of "com.sun.jndi.ldap.tls.cbtype" property
     * @param  cbType
     * @return TLS Channel Binding type or null if
     *         "com.sun.jndi.ldap.tls.cbtype" property has not been set.
     * @throws NamingException
     */
    public static TlsChannelBindingType parseType(String cbType) throws NamingException {
        if (cbType != null) {
            if (cbType.equals(TlsChannelBindingType.TLS_SERVER_END_POINT.getName())) {
                return TlsChannelBindingType.TLS_SERVER_END_POINT;
            } else {
                throw new NamingException("Illegal value for " +
                        CHANNEL_BINDING_TYPE + " property.");
            }
        }
        return null;
    }

    private final TlsChannelBindingType cbType;
    private final byte[] cbData;

    /**
     * Construct tls-server-end-point Channel Binding data
     * @param serverCertificate
     * @throws SaslException
     */
    public static TlsChannelBinding create(X509Certificate serverCertificate) throws SaslException {
        try {
            final byte[] prefix =
                TlsChannelBindingType.TLS_SERVER_END_POINT.getName().concat(":").getBytes();
            String hashAlg = serverCertificate.getSigAlgName().
                    replace("SHA", "SHA-").toUpperCase();
            int ind = hashAlg.indexOf("WITH");
            if (ind > 0) {
                hashAlg = hashAlg.substring(0, ind);
                if (hashAlg.equals("MD5") || hashAlg.equals("SHA-1")) {
                    hashAlg = "SHA-256";
                }
            } else {
                hashAlg = "SHA-256";
            }
            MessageDigest md = MessageDigest.getInstance(hashAlg);
            byte[] hash = md.digest(serverCertificate.getEncoded());
            byte[] cbData = Arrays.copyOf(prefix, prefix.length + hash.length );
            System.arraycopy(hash, 0, cbData, prefix.length, hash.length);
            return new TlsChannelBinding(TlsChannelBindingType.TLS_SERVER_END_POINT, cbData);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new SaslException("Cannot create TLS channel binding data", e);
        }
    }

    private TlsChannelBinding(TlsChannelBindingType cbType, byte[] cbData) {
        this.cbType = cbType;
        this.cbData = cbData;
    }

    public TlsChannelBindingType getType() {
        return cbType;
    }

    public byte[] getData() {
        return cbData;
    }
}
