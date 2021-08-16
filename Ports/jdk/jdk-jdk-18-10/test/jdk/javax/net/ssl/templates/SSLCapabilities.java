/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import javax.net.ssl.SNIServerName;

/**
 * Encapsulates the security capabilities of an SSL/TLS connection.
 * <P>
 * The security capabilities are the list of ciphersuites to be accepted in
 * an SSL/TLS handshake, the record version, the hello version, and server
 * name indication, etc., of an SSL/TLS connection.
 * <P>
 * <code>SSLCapabilities</code> can be retrieved by exploring the network
 * data of an SSL/TLS connection via {@link SSLExplorer#explore(ByteBuffer)}
 * or {@link SSLExplorer#explore(byte[], int, int)}.
 *
 * @see SSLExplorer
 */
public abstract class SSLCapabilities {

    /**
     * Returns the record version of an SSL/TLS connection
     *
     * @return a non-null record version
     */
    public abstract String getRecordVersion();

    /**
     * Returns the hello version of an SSL/TLS connection
     *
     * @return a non-null hello version
     */
    public abstract String getHelloVersion();

    /**
     * Returns a <code>List</code> containing all {@link SNIServerName}s
     * of the server name indication.
     *
     * @return a non-null immutable list of {@link SNIServerName}s
     *         of the server name indication parameter, may be empty
     *         if no server name indication.
     *
     * @see SNIServerName
     */
    public abstract List<SNIServerName> getServerNames();
}

