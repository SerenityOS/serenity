/*
 * Copyright (c) 2000, 2002, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Hashtable;
import java.util.StringTokenizer;

import com.sun.jndi.toolkit.url.Uri;
import com.sun.jndi.toolkit.url.UrlUtil;


/**
 * A DnsUrl represents a DNS pseudo-URL of the form
 * <pre>
 *   dns://[host][:port][/[domain]]
 * or
 *   dns:[/][domain]
 * </pre>
 * The host names a DNS server.  If the host is not provided, it
 * indicates that the underlying platform's DNS server(s) should be
 * used if possible, or that "localhost" should be used otherwise.  If
 * the port is not provided, the DNS default port 53 will be used.
 * The domain indicates the domain name of the context, and is not
 * necessarily related to the domain of the server; if it is not
 * provided, the root domain "." is used.  Special characters in
 * the domain name must be %-escaped as described in RFC 2396.
 *
 * @author Scott Seligman
 */


public class DnsUrl extends Uri {

    private String domain;      // domain name of the context


    /**
     * Given a space-separated list of DNS URLs, returns an array of DnsUrl
     * objects.
     */
    public static DnsUrl[] fromList(String urlList)
            throws MalformedURLException {

        DnsUrl[] urls = new DnsUrl[(urlList.length() + 1) / 2];
        int i = 0;              // next available index in urls
        StringTokenizer st = new StringTokenizer(urlList, " ");

        while (st.hasMoreTokens()) {
            urls[i++] = new DnsUrl(st.nextToken());
        }
        DnsUrl[] trimmed = new DnsUrl[i];
        System.arraycopy(urls, 0, trimmed, 0, i);
        return trimmed;
    }

    public DnsUrl(String url) throws MalformedURLException {
        super(url);

        if (!scheme.equals("dns")) {
            throw new MalformedURLException(
                    url + " is not a valid DNS pseudo-URL");
        }

        domain = path.startsWith("/")
            ? path.substring(1)
            : path;
        domain = domain.isEmpty()
            ? "."
            : UrlUtil.decode(domain);

        // Debug
        // System.out.println("host=" + host + " port=" + port +
        //                    " domain=" + domain);
    }

    /**
     * Returns the domain of this URL, or "." if none is provided.
     * Never null.
     */
    public String getDomain() {
        return domain;
    }


/*
    // Debug
    public static void main(String args[]) throws MalformedURLException {
        DnsUrl[] urls = fromList(args[0]);
        for (int i = 0; i < urls.length; i++) {
            System.out.println(urls[i].toString());
        }
    }
*/
}
