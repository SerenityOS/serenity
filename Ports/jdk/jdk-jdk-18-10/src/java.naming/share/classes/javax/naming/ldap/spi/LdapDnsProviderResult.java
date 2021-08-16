/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

/**
 * The result of a DNS lookup for an LDAP URL.
 *
 * <p> This class is used by an {@link LdapDnsProvider} to return the result
 * of a DNS lookup for a given LDAP URL. The result consists of a domain name
 * and its associated LDAP server endpoints.
 *
 * <p> A {@code null} {@code domainName} is equivalent to and represented
 * by an empty string.
 *
 * @since 12
 */
public final class LdapDnsProviderResult {

    private final String domainName;
    private final List<String> endpoints;

    /**
     * Construct an LdapDnsProviderResult consisting of a resolved domain name
     * and the LDAP server endpoints that serve the domain.
     *
     * @param domainName    the resolved domain name; can be null.
     * @param endpoints     the possibly empty list of resolved LDAP server
     *                      endpoints
     *
     * @throws NullPointerException   if {@code endpoints} contains {@code null}
     *                                elements.
     * @throws ClassCastException     if {@code endpoints} contains non-
     *                                {@code String} elements.
     */
    public LdapDnsProviderResult(String domainName, List<String> endpoints) {
        this.domainName = (domainName == null) ? "" : domainName;
        this.endpoints = List.copyOf(endpoints);
    }

    /**
     * Returns the domain name resolved from the LDAP URL. This method returns
     * the empty string if the {@code LdapDnsProviderResult} is created with a
     * null domain name.
     *
     * @return  the resolved domain name
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * Returns the possibly empty list of individual server endpoints resolved
     * from the LDAP URL.
     *
     * @return  a possibly empty unmodifiable {@link List} containing the
     *          resolved LDAP server endpoints
     */
    public List<String> getEndpoints() {
        return endpoints;
    }
}
