/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming.ldap.spi;

import javax.naming.Context;
import javax.naming.NamingException;
import java.util.Map;
import java.util.Optional;

/**
 * Service-provider class for DNS lookups when performing LDAP operations.
 *
 * <p> An LDAP DNS provider is a concrete subclass of this class that
 * has a zero-argument constructor. LDAP DNS providers are located using the
 * ServiceLoader facility, as specified by
 * {@linkplain javax.naming.directory.InitialDirContext InitialDirectContext}.
 *
 * The
 * {@link java.util.ServiceLoader ServiceLoader} is used to create and register
 * implementations of {@code LdapDnsProvider}.
 *
 * <p> An LDAP DNS provider can be used in environments where the default
 * DNS resolution mechanism is not sufficient to accurately pinpoint the
 * correct LDAP servers needed to perform LDAP operations. For example, in an
 * environment containing a mix of {@code ldap} and {@code ldaps} servers
 * you may want the {@linkplain javax.naming.ldap.LdapContext LdapContext}
 * to query {@code ldaps} servers only.
 *
 * @since 12
 */
public abstract class LdapDnsProvider {

    // The {@code RuntimePermission("ldapDnsProvider")} is
    // necessary to subclass and instantiate the {@code LdapDnsProvider} class.
    private static final RuntimePermission DNSPROVIDER_PERMISSION =
            new RuntimePermission("ldapDnsProvider");

    /**
     * Creates a new instance of {@code LdapDnsProvider}.
     *
     * @throws SecurityException if a security manager is present and its
     *                           {@code checkPermission} method doesn't allow
     *                           the {@code RuntimePermission("ldapDnsProvider")}.
     */
    protected LdapDnsProvider() {
        this(checkPermission());
    }

    private LdapDnsProvider(Void unused) {
        // nothing to do.
    }

    private static Void checkPermission() {
        @SuppressWarnings("removal")
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(DNSPROVIDER_PERMISSION);
        }
        return null;
    }

    /**
     * Lookup the endpoints and domain name for the given {@link Context}
     * {@link Context#PROVIDER_URL provider URL} and environment. The resolved
     * endpoints and domain name are returned as an
     * {@link LdapDnsProviderResult}.
     *
     * <p> An endpoint is a {@code String} representation of an LDAP URL which
     * points to an LDAP server to be used for LDAP operations. The syntax of
     * an LDAP URL is defined by <a href="http://www.ietf.org/rfc/rfc2255.txt">
     * <i>RFC&nbsp;2255: The LDAP URL Format</i></a>.
     *
     * @param url   The {@link Context} {@link Context#PROVIDER_URL provider URL}
     * @param env   The {@link Context} environment.
     *
     * @return  an {@link LdapDnsProviderResult} or empty {@code Optional}
     *          if the lookup fails.
     *
     * @throws NamingException      if the {@code url} is not valid or an error
     *                              occurred while performing the lookup.
     * @throws NullPointerException if either {@code url} or {@code env} are
     *                              {@code null}.
     */
    public abstract Optional<LdapDnsProviderResult> lookupEndpoints(
            String url, Map<?,?> env) throws NamingException;
}
