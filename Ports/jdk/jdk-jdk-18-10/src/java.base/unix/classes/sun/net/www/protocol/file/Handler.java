/*
 * Copyright (c) 1994, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www.protocol.file;

import java.net.InetAddress;
import java.net.URLConnection;
import java.net.URL;
import java.net.Proxy;
import java.net.MalformedURLException;
import java.net.URLStreamHandler;
import java.io.InputStream;
import java.io.IOException;
import sun.net.www.ParseUtil;
import java.io.File;

/**
 * Open an file input stream given a URL.
 * @author      James Gosling
 */
public class Handler extends URLStreamHandler {

    private String getHost(URL url) {
        String host = url.getHost();
        if (host == null)
            host = "";
        return host;
    }


    protected void parseURL(URL u, String spec, int start, int limit) {
        /*
         * Ugly backwards compatibility. Flip any file separator
         * characters to be forward slashes. This is a nop on Unix
         * and "fixes" win32 file paths. According to RFC 2396,
         * only forward slashes may be used to represent hierarchy
         * separation in a URL but previous releases unfortunately
         * performed this "fixup" behavior in the file URL parsing code
         * rather than forcing this to be fixed in the caller of the URL
         * class where it belongs. Since backslash is an "unwise"
         * character that would normally be encoded if literally intended
         * as a non-seperator character the damage of veering away from the
         * specification is presumably limited.
         */
        super.parseURL(u, spec.replace(File.separatorChar, '/'), start, limit);
    }

    public synchronized URLConnection openConnection(URL u)
        throws IOException {
        return openConnection(u, null);
    }

    public synchronized URLConnection openConnection(URL u, Proxy p)
           throws IOException {
        String host = u.getHost();
        if (host == null || host.isEmpty() || host.equals("~") ||
            host.equalsIgnoreCase("localhost")) {
            File file = new File(ParseUtil.decode(u.getPath()));
            return createFileURLConnection(u, file);
        }

        /* If you reach here, it implies that you have a hostname
           so attempt an ftp connection.
         */
        URLConnection uc;
        URL ru;

        try {
            ru = new URL("ftp", host, u.getFile() +
                             (u.getRef() == null ? "": "#" + u.getRef()));
            if (p != null) {
                uc = ru.openConnection(p);
            } else {
                uc = ru.openConnection();
            }
        } catch (IOException e) {
            uc = null;
        }
        if (uc == null) {
            throw new IOException("Unable to connect to: " +
                                                       u.toExternalForm());
        }
        return uc;
    }

    // Template method to be overridden by Java Plug-in. [stanleyh]
    //
    protected URLConnection createFileURLConnection(URL u, File file)
    {
        return new FileURLConnection(u, file);
    }

    /**
     * Compares the host components of two URLs.
     * @param u1 the URL of the first host to compare
     * @param u2 the URL of the second host to compare
     * @return  {@code true} if and only if they
     * are equal, {@code false} otherwise.
     */
    protected boolean hostsEqual(URL u1, URL u2) {
        /*
         * Special case for file: URLs
         * per RFC 1738 no hostname is equivalent to 'localhost'
         * i.e. file:///path is equal to file://localhost/path
         */
        String s1 = u1.getHost();
        String s2 = u2.getHost();
        if ("localhost".equalsIgnoreCase(s1) && (s2 == null || s2.isEmpty()))
            return true;
        if ("localhost".equalsIgnoreCase(s2) && (s1 == null || s1.isEmpty()))
            return true;
        return super.hostsEqual(u1, u2);
    }
}
