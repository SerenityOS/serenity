/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.util;

import java.io.IOException;
import java.net.URL;
import java.net.URLPermission;
import java.security.Permission;

/**
 * URL Utility class.
 */
public class URLUtil {
    /**
     * Returns a string form of the url suitable for use as a key in HashMap/Sets.
     *
     * The string form should be behave in the same manner as the URL when
     * compared for equality in a HashMap/Set, except that no nameservice
     * lookup is done on the hostname (only string comparison), and the fragment
     * is not considered.
     *
     * @see java.net.URLStreamHandler.sameFile(java.net.URL)
     */
    public static String urlNoFragString(URL url) {
        StringBuilder strForm = new StringBuilder();

        String protocol = url.getProtocol();
        if (protocol != null) {
            /* protocol is compared case-insensitive, so convert to lowercase */
            protocol = protocol.toLowerCase();
            strForm.append(protocol);
            strForm.append("://");
        }

        String host = url.getHost();
        if (host != null) {
            /* host is compared case-insensitive, so convert to lowercase */
            host = host.toLowerCase();
            strForm.append(host);

            int port = url.getPort();
            if (port == -1) {
                /* if no port is specificed then use the protocols
                 * default, if there is one */
                port = url.getDefaultPort();
            }
            if (port != -1) {
                strForm.append(":").append(port);
            }
        }

        String file = url.getFile();
        if (file != null) {
            strForm.append(file);
        }

        return strForm.toString();
    }

    public static Permission getConnectPermission(URL url) throws IOException {
        String urlStringLowerCase = url.toString().toLowerCase();
        if (urlStringLowerCase.startsWith("http:") || urlStringLowerCase.startsWith("https:")) {
            return getURLConnectPermission(url);
        } else if (urlStringLowerCase.startsWith("jar:http:") || urlStringLowerCase.startsWith("jar:https:")) {
            String urlString = url.toString();
            int bangPos = urlString.indexOf("!/");
            urlString = urlString.substring(4, bangPos > -1 ? bangPos : urlString.length());
            URL u = new URL(urlString);
            return getURLConnectPermission(u);
            // If protocol is HTTP or HTTPS than use URLPermission object
        } else {
            return url.openConnection().getPermission();
        }
    }

    private static Permission getURLConnectPermission(URL url) {
        String urlString = url.getProtocol() + "://" + url.getAuthority() + url.getPath();
        return new URLPermission(urlString);
    }
}

