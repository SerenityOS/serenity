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

package com.sun.jndi.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.naming.NamingException;
import javax.naming.ldap.spi.LdapDnsProviderResult;

public class DefaultLdapDnsProvider {

    public Optional<LdapDnsProviderResult> lookupEndpoints(String url,
                                                           Map<?,?> env)
            throws NamingException
    {
        if (url == null || env == null) {
            throw new NullPointerException();
        }

        String domainName;
        List<String> endpoints = new ArrayList<>();
        LdapURL ldapUrl = new LdapURL(url);
        String dn = ldapUrl.getDN();
        String host = ldapUrl.getHost();
        int port = ldapUrl.getPort();
        String[] hostports;

        // handle a URL with no hostport (ldap:/// or ldaps:///)
        // locate the LDAP service using the URL's distinguished name
        if (host == null
                && port == -1
                && dn != null
                && (domainName = ServiceLocator.mapDnToDomainName(dn)) != null
                && (hostports = ServiceLocator.getLdapService(domainName, env)) != null) {
            // Generate new URLs that include the discovered hostports.
            // Reuse the original URL scheme.
            String scheme = ldapUrl.getScheme() + "://";
            String query = ldapUrl.getQuery();
            String urlSuffix = ldapUrl.getPath() + (query != null ? query : "");
            for (String hostPort : hostports) {
                // the hostports come from the DNS SRV records
                // we assume the SRV record is scheme aware
                endpoints.add(scheme + hostPort + urlSuffix);
            }
        } else {
            // we don't have enough information to set the domain name
            // correctly
            domainName = "";
            endpoints.add(url);
        }

        LdapDnsProviderResult res = new LdapDnsProviderResult(domainName, endpoints);
        if (res.getEndpoints().isEmpty() && res.getDomainName().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(res);
        }
    }
}
