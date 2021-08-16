/*
 * Copyright (c) 2002, 2005, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import java.io.OutputStream;
import javax.naming.InterruptedNamingException;
import javax.naming.CommunicationException;
import javax.naming.NamingException;

import com.sun.jndi.ldap.pool.PoolCallback;
import com.sun.jndi.ldap.pool.PooledConnection;
import com.sun.jndi.ldap.pool.PooledConnectionFactory;

/**
 * Creates an LdapClient. Encapsulates the parameters required to create
 * an LdapClient and provides methods for returning appropriate exceptions
 * to throw when acquiring a pooled LdapClient fails.
 *
 * @author Rosanna Lee
 */
final class LdapClientFactory implements PooledConnectionFactory {
    private final String host;
    private final int port;
    private final String socketFactory;
    private final int connTimeout;
    private final int readTimeout;
    private final OutputStream trace;

    LdapClientFactory(String host, int port, String socketFactory,
        int connTimeout, int readTimeout, OutputStream trace) {
        this.host = host;
        this.port = port;
        this.socketFactory = socketFactory;
        this.connTimeout = connTimeout;
        this.readTimeout = readTimeout;
        this.trace = trace;
    }

    public PooledConnection createPooledConnection(PoolCallback pcb)
        throws NamingException {
        return new LdapClient(host, port, socketFactory,
                connTimeout, readTimeout, trace, pcb);
    }

    public String toString() {
        return host + ":" + port;
    }
}
