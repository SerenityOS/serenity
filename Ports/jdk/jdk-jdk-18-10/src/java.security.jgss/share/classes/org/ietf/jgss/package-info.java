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

/**
 * This package presents a framework that allows application developers to
 * make use of security services like authentication, data integrity and
 * data confidentiality from a variety of underlying security mechanisms
 * like Kerberos, using a unified API. The security mechanisms that an
 * application can
 * chose to use are identified with unique object identifiers. One example
 * of such a mechanism is the Kerberos v5 GSS-API mechanism (object
 * identifier 1.2.840.113554.1.2.2). This mechanism is available through
 * the default instance of the GSSManager class.<p>
 *
 * The GSS-API is defined in a language independent way in
 * <a href=http://www.ietf.org/rfc/rfc2743.txt>RFC 2743</a>. The Java
 * language bindings are defined in
 * <a href=http://www.ietf.org/rfc/rfc2853.txt>RFC 2853</a><p>
 *
 * An application starts out by instantiating a {@code GSSManager}
 * which then serves as a factory for a security context. An application
 * can use specific principal names and credentials that are also created
 * using the GSSManager; or it can instantiate a
 * context with system defaults. It then goes through a context
 * establishment loop. Once a context is established with the
 * peer, authentication is complete. Data protection such as integrity
 * and confidentiality can then be obtained from this context.<p>
 *
 * The GSS-API does not perform any communication with the peer. It merely
 * produces tokens that the application must somehow transport to the
 * other end.
 *
 * <h2 id="useSubjectCredsOnly">Credential Acquisition</h2>
 * The GSS-API itself does not dictate how an underlying mechanism
 * obtains the credentials that are needed for authentication. It is
 * assumed that prior to calling the GSS-API, these credentials are
 * obtained and stored in a location that the mechanism provider is
 * aware of. However, the default model in the Java platform will be
 * that mechanism providers must obtain credentials only from the private
 * or public credential sets associated with the
 * {@link javax.security.auth.Subject Subject} in the
 * current access control context.  The Kerberos v5
 * mechanism will search for the required INITIATE and ACCEPT credentials
 * ({@link javax.security.auth.kerberos.KerberosTicket KerberosTicket} and
 * {@link javax.security.auth.kerberos.KerberosKey KerberosKey}) in
 * the private credential set where as some other mechanism might look
 * in the public set or in both.  If the desired credential is not
 * present in the appropriate sets of the current Subject, the GSS-API
 * call must fail.<p>
 *
 * This model has the advantage that credential management
 * is simple and predictable from the applications point of view.  An
 * application, given the right permissions, can purge the credentials in
 * the Subject or renew them using standard Java API's.  If it purged
 * the credentials, it would be sure that the JGSS mechanism would fail,
 * or if it renewed a time based credential it would be sure that a JGSS
 * mechanism would succeed.<p>
 *
 * This model does require that a {@link
 * javax.security.auth.login JAAS login} be performed in order to
 * authenticate and populate a Subject that the JGSS mechanism can later
 * utilize. However, applications have the ability to relax this
 * restriction by means of a system property:
 * {@systemProperty javax.security.auth.useSubjectCredsOnly}. By default
 * this system property will be assumed to be {@code true} (even when
 * it is unset) indicating that providers must only use the credentials
 * that are present in the current Subject. However, if this property is
 * explicitly set to false by the application, then it indicates that
 * the provider is free to use any credentials cache of its choice. Such
 * a credential cache might be a disk cache, an in-memory cache, or even
 * just the current Subject itself.
 *
 * <h2>Related Documentation</h2>
 * For an online tutorial on using Java GSS-API, please see
 * {@extLink security_guide_jgss_tutorial
 * Introduction to JAAS and Java GSS-API}.
 *
 * @since 1.4
 * */
package org.ietf.jgss;
