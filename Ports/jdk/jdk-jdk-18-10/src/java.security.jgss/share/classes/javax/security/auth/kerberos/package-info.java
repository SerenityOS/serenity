/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * This package contains utility classes related to the Kerberos network
 * authentication protocol. They do not provide much Kerberos support
 * themselves.<p>
 *
 * The Kerberos network authentication protocol is defined in
 * <a href=http://www.ietf.org/rfc/rfc4120.txt>RFC 4120</a>. The Java
 * platform contains support for the client side of Kerberos via the
 * {@link org.ietf.jgss} package. There might also be
 * a login module that implements
 * {@link javax.security.auth.spi.LoginModule LoginModule} to authenticate
 * Kerberos principals.<p>
 *
 * You can provide the name of your default realm and Key Distribution
 * Center (KDC) host for that realm using the system properties
 * {@systemProperty java.security.krb5.realm} and
 * {@systemProperty java.security.krb5.kdc}. Both properties must be set.
 * Alternatively, the {@systemProperty java.security.krb5.conf} system property
 * can be set to the location of an MIT style {@code krb5.conf} configuration
 * file. If none of these system properties are set, the {@code krb5.conf}
 * file is searched for in an implementation-specific manner. Typically,
 * an implementation will first look for a {@code krb5.conf} file in
 * {@code <java-home>/conf/security} and failing that, in an OS-specific
 * location.<p>
 *
 * The {@code krb5.conf} file is formatted in the Windows INI file style,
 * which contains a series of relations grouped into different sections.
 * Each relation contains a key and a value, the value can be an arbitrary
 * string or a boolean value. A boolean value can be one of "true", "false",
 * "yes", or "no", and values are case-insensitive.
 *
 * @since 1.4
 */
package javax.security.auth.kerberos;
