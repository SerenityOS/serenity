/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 * @bug 6912868 8170868
 * @summary Basic test to provide some coverage of system proxy code. Will
 * always pass. Should be run manually for specific systems to inspect output.
 * @run main/othervm -Djava.net.useSystemProxies=true SystemProxies
 */

import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class SystemProxies {

    static final String[] uriAuthority = { "myMachine/", "local", "localhost",
                                           "127.0.0.1", "127.0.0.123",
                                           "127.0.2.2", "127.3.3.3",
                                           "128.0.0.1" };
    static final ProxySelector proxySel = ProxySelector.getDefault();

    public static void main(String[] args) {
        if (! "true".equals(System.getProperty("java.net.useSystemProxies"))) {
            System.out.println("Usage: java -Djava.net.useSystemProxies=true SystemProxies");
            return;
        }

        printProxies("http://");
        printProxies("https://");
        printProxies("ftp://");
        printProxies("none://");
        printProxies("rtsp://");
        printProxies("socket://");
    }

    static void printProxies(String proto) {
        System.out.println("Protocol:" + proto);
        for (String uri : uriAuthority) {
            String uriStr =  proto + uri;
            try {
                List<Proxy> proxies = proxySel.select(new URI(uriStr));
                System.out.println("\tProxies returned for " + uriStr);
                for (Proxy proxy : proxies)
                    System.out.println("\t\t" + proxy);
            } catch (URISyntaxException e) {
                System.err.println(e);
            }
        }
    }
}
