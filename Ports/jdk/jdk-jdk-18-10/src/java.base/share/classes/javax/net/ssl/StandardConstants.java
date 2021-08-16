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

package javax.net.ssl;

/**
 * Standard constants definitions
 *
 * @since 1.8
 */
public final class StandardConstants {

    // Suppress default constructor for noninstantiability
    private StandardConstants() {
        throw new AssertionError(
            "No javax.net.ssl.StandardConstants instances for you!");
    }

    /**
     * The "host_name" type representing of a DNS hostname
     * (see {@link SNIHostName}) in a Server Name Indication (SNI) extension.
     * <P>
     * The SNI extension is a feature that extends the SSL/TLS protocols to
     * indicate what server name the client is attempting to connect to during
     * handshaking.  See section 3, "Server Name Indication", of <A
     * HREF="http://www.ietf.org/rfc/rfc6066.txt">TLS Extensions (RFC 6066)</A>.
     * <P>
     * The value of this constant is {@value}.
     *
     * @see SNIServerName
     * @see SNIHostName
     */
    public static final int SNI_HOST_NAME = 0x00;
}
