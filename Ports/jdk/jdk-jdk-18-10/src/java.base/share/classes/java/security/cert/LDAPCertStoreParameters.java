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

package java.security.cert;

/**
 * Parameters used as input for the LDAP {@code CertStore} algorithm.
 * <p>
 * This class is used to provide necessary configuration parameters (server
 * name and port number) to implementations of the LDAP {@code CertStore}
 * algorithm. However, if you are retrieving certificates or CRLs from
 * an ldap URI as specified by RFC 5280, use the
 * {@link java.security.cert.URICertStoreParameters URICertStoreParameters}
 * instead as the URI may contain additional information such as the
 * distinguished name that will help the LDAP CertStore find the specific
 * certificates and CRLs.
 * <p>
 * <b>Concurrent Access</b>
 * <p>
 * Unless otherwise specified, the methods defined in this class are not
 * thread-safe. Multiple threads that need to access a single
 * object concurrently should synchronize amongst themselves and
 * provide the necessary locking. Multiple threads each manipulating
 * separate objects need not synchronize.
 *
 * @since       1.4
 * @author      Steve Hanna
 * @see         CertStore
 */
public class LDAPCertStoreParameters implements CertStoreParameters {

    private static final int LDAP_DEFAULT_PORT = 389;

    /**
     * the port number of the LDAP server
     */
    private int port;

    /**
     * the DNS name of the LDAP server
     */
    private String serverName;

    /**
     * Creates an instance of {@code LDAPCertStoreParameters} with the
     * specified parameter values.
     *
     * @param serverName the DNS name of the LDAP server
     * @param port the port number of the LDAP server
     * @throws    NullPointerException if {@code serverName} is
     * {@code null}
     */
    public LDAPCertStoreParameters(String serverName, int port) {
        if (serverName == null)
            throw new NullPointerException();
        this.serverName = serverName;
        this.port = port;
    }

    /**
     * Creates an instance of {@code LDAPCertStoreParameters} with the
     * specified server name and a default port of 389.
     *
     * @param serverName the DNS name of the LDAP server
     * @throws    NullPointerException if {@code serverName} is
     * {@code null}
     */
    public LDAPCertStoreParameters(String serverName) {
        this(serverName, LDAP_DEFAULT_PORT);
    }

    /**
     * Creates an instance of {@code LDAPCertStoreParameters} with the
     * default parameter values (server name "localhost", port 389).
     */
    public LDAPCertStoreParameters() {
        this("localhost", LDAP_DEFAULT_PORT);
    }

    /**
     * Returns the DNS name of the LDAP server.
     *
     * @return the name (not {@code null})
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Returns the port number of the LDAP server.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns a copy of this object. Changes to the copy will not affect
     * the original and vice versa.
     * <p>
     * Note: this method currently performs a shallow copy of the object
     * (simply calls {@code Object.clone()}). This may be changed in a
     * future revision to perform a deep copy if new parameters are added
     * that should not be shared.
     *
     * @return the copy
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            /* Cannot happen */
            throw new InternalError(e.toString(), e);
        }
    }

    /**
     * Returns a formatted string describing the parameters.
     *
     * @return a formatted string describing the parameters
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LDAPCertStoreParameters: [\n");

        sb.append("  serverName: " + serverName + "\n");
        sb.append("  port: " + port + "\n");
        sb.append("]");
        return sb.toString();
    }
}
