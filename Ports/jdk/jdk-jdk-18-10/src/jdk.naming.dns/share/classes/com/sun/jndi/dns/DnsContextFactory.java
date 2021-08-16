/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.dns;


import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.*;
import javax.naming.spi.*;

import com.sun.jndi.toolkit.url.UrlUtil;
import sun.net.dns.ResolverConfiguration;       // available since 1.4.1


/**
 * A DnsContextFactory serves as the initial context factory for DNS.
 *
 * <p> When an initial context is being created, the environment
 * property "java.naming.provider.url" should contain a DNS pseudo-URL
 * (see DnsUrl) or a space-separated list of them.  Multiple URLs must
 * all have the same domain value.
 * If the property is not set, the default "dns:" is used.
 *
 * @author Scott Seligman
 */


public class DnsContextFactory implements InitialContextFactory {

    private static final String DEFAULT_URL = "dns:";
    private static final int DEFAULT_PORT = 53;


    public Context getInitialContext(Hashtable<?,?> env) throws NamingException {
        if (env == null) {
            env = new Hashtable<>(5);
        }
        return urlToContext(getInitCtxUrl(env), env);
    }

    public static DnsContext getContext(String domain,
                                        String[] servers, Hashtable<?,?> env)
            throws NamingException {
        return new DnsContext(domain, servers, env);
    }

    /*
     * "urls" are used to determine the servers, but any domain
     * components are overridden by "domain".
     */
    public static DnsContext getContext(String domain,
                                        DnsUrl[] urls, Hashtable<?,?> env)
            throws NamingException {

        String[] servers = serversForUrls(urls);
        DnsContext ctx = getContext(domain, servers, env);
        if (platformServersUsed(urls)) {
            ctx.setProviderUrl(constructProviderUrl(domain, servers));
        }
        return ctx;
    }

    /*
     * Public for use by product test suite.
     */
    public static boolean platformServersAvailable() {
        return !filterNameServers(
                    ResolverConfiguration.open().nameservers(), true
                ).isEmpty();
    }

    private static Context urlToContext(String url, Hashtable<?,?> env)
            throws NamingException {

        DnsUrl[] urls;
        try {
            urls = DnsUrl.fromList(url);
        } catch (MalformedURLException e) {
            throw new ConfigurationException(e.getMessage());
        }
        if (urls.length == 0) {
            throw new ConfigurationException(
                    "Invalid DNS pseudo-URL(s): " + url);
        }
        String domain = urls[0].getDomain();

        // If multiple urls, all must have the same domain.
        for (int i = 1; i < urls.length; i++) {
            if (!domain.equalsIgnoreCase(urls[i].getDomain())) {
                throw new ConfigurationException(
                        "Conflicting domains: " + url);
            }
        }
        return getContext(domain, urls, env);
    }

    /*
     * Returns all the servers specified in a set of URLs.
     * If a URL has no host (or port), the servers configured on the
     * underlying platform are used if possible.  If no configured
     * servers can be found, then fall back to the old behavior of
     * using "localhost".
     * There must be at least one URL.
     */
    private static String[] serversForUrls(DnsUrl[] urls)
            throws NamingException {

        if (urls.length == 0) {
            throw new ConfigurationException("DNS pseudo-URL required");
        }

        List<String> servers = new ArrayList<>();

        for (int i = 0; i < urls.length; i++) {
            String server = urls[i].getHost();
            int port = urls[i].getPort();

            if (server == null && port < 0) {
                // No server or port given, so look to underlying platform.
                // ResolverConfiguration does some limited caching, so the
                // following is reasonably efficient even if called rapid-fire.
                List<String> platformServers = filterNameServers(
                    ResolverConfiguration.open().nameservers(), false);
                if (!platformServers.isEmpty()) {
                    servers.addAll(platformServers);
                    continue;  // on to next URL (if any, which is unlikely)
                }
            }

            if (server == null) {
                server = "localhost";
            }
            servers.add((port < 0)
                        ? server
                        : server + ":" + port);
        }
        return servers.toArray(new String[servers.size()]);
    }

    /*
     * Returns true if serversForUrls(urls) would make use of servers
     * from the underlying platform.
     */
    private static boolean platformServersUsed(DnsUrl[] urls) {
        if (!platformServersAvailable()) {
            return false;
        }
        for (int i = 0; i < urls.length; i++) {
            if (urls[i].getHost() == null &&
                urls[i].getPort() < 0) {
                return true;
            }
        }
        return false;
    }

    /*
     * Returns a value for the PROVIDER_URL property (space-separated URL
     * Strings) that reflects the given domain and servers.
     * Each server is of the form "server[:port]".
     * There must be at least one server.
     * IPv6 literal host names include delimiting brackets.
     */
    private static String constructProviderUrl(String domain,
                                               String[] servers) {
        String path = "";
        if (!domain.equals(".")) {
            try {
                path = "/" + UrlUtil.encode(domain, "ISO-8859-1");
            } catch (java.io.UnsupportedEncodingException e) {
                // assert false : "ISO-Latin-1 charset unavailable";
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < servers.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append("dns://").append(servers[i]).append(path);
        }
        return sb.toString();
    }

    /*
     * Reads environment to find URL(s) of initial context.
     * Default URL is "dns:".
     */
    private static String getInitCtxUrl(Hashtable<?,?> env) {
        String url = (String) env.get(Context.PROVIDER_URL);
        return ((url != null) ? url : DEFAULT_URL);
    }

    /**
     * Removes any DNS server that's not permitted to access
     * @param input the input server[:port] list, must not be null
     * @param oneIsEnough return output once there exists one ok
     * @return the filtered list, all non-permitted input removed
     */
    private static List<String> filterNameServers(List<String> input, boolean oneIsEnough) {
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        if (security == null || input == null || input.isEmpty()) {
            return input;
        } else {
            List<String> output = new ArrayList<>();
            for (String platformServer: input) {
                int colon = platformServer.indexOf(':',
                        platformServer.indexOf(']') + 1);

                int p = (colon < 0)
                    ? DEFAULT_PORT
                    : Integer.parseInt(
                        platformServer.substring(colon + 1));
                String s = (colon < 0)
                    ? platformServer
                    : platformServer.substring(0, colon);
                try {
                    security.checkConnect(s, p);
                    output.add(platformServer);
                    if (oneIsEnough) {
                        return output;
                    }
                } catch (SecurityException se) {
                    continue;
                }
            }
            return output;
        }
    }
}
