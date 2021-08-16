/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Defines the Java binding of the IETF Generic Security Services API (GSS-API).
 * <P>
 * This module also contains GSS-API mechanisms including Kerberos v5 and SPNEGO.
 *
 * @moduleGraph
 * @since 9
 */
module java.security.jgss {
    requires java.naming;

    exports javax.security.auth.kerberos;
    exports org.ietf.jgss;

    exports sun.security.jgss to
        jdk.security.jgss;
    exports sun.security.jgss.krb5 to
        jdk.security.auth;
    exports sun.security.jgss.krb5.internal to
        jdk.security.jgss;
    exports sun.security.krb5 to
        jdk.security.auth;
    exports sun.security.krb5.internal to
        jdk.security.jgss;
    exports sun.security.krb5.internal.ktab to
        jdk.security.auth;

    // Opens for reflective instantiation of sun.net.www.protocol.http.spnego.NegotiatorImpl
    // to sun.net.www.protocol.http.HttpURLConnection
    opens sun.net.www.protocol.http.spnego to
        java.base;

    provides java.security.Provider with
        sun.security.jgss.SunProvider;
}

