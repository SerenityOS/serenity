/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8035158 8145732 8144300 8241138
 * @run main/othervm B8035158
 */

import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Callable;

public class B8035158 {

    public static void main(String[] args) {
        for (TestCase t : emptyNonProxiesHosts()) t.run();
        for (TestCase t : nonEmptyNonProxiesHosts()) t.run();
        for (TestCase t : misc()) t.run();
    }

    // Setting http.nonProxyHosts to an empty string has an effect of
    // not including default hosts to the list of exceptions
    // (i.e. if you want everything to be connected directly rather than
    // through proxy, you should set this property to an empty string)
    private static Collection<TestCase> emptyNonProxiesHosts() {
        List<TestCase> tests = new LinkedList<>();
        String[] loopbacks = {"localhost", "[::1]", "[::0]", "0.0.0.0",
                "127.0.0.0", "127.0.0.1", "127.0.1.0", "127.0.1.1",
                "127.1.0.0", "127.1.0.1", "127.1.1.0", "127.1.1.1"};
        Map<String, String> properties = new HashMap<>();
        properties.put("http.proxyHost", "http://proxy.example.com");
        properties.put("http.nonProxyHosts", "");
        for (String s : loopbacks) {
            tests.add(new TestCase(properties, "http://" + s, true));
        }
        return tests;
    }

    // No matter what is set into the http.nonProxyHosts (as far as it is not
    // an empty string) loopback address aliases must be always connected
    // directly
    private static Collection<TestCase> nonEmptyNonProxiesHosts() {
        List<TestCase> tests = new LinkedList<>();
        String[] nonProxyHosts = {
                "google.com",
                "localhost", "[::1]", "[::0]", "0.0.0.0",
                "127.0.0.0", "127.0.0.1", "127.0.1.0", "127.0.1.1",
                "127.1.0.0", "127.1.0.1", "127.1.1.0", "127.1.1.1"};
        String[] loopbacks = {"localhost", "[::1]", "[::0]", "0.0.0.0",
                "127.0.0.0", "127.0.0.1", "127.0.1.0", "127.0.1.1",
                "127.1.0.0", "127.1.0.1", "127.1.1.0", "127.1.1.1"};
        for (String h : nonProxyHosts) {
            for (String s : loopbacks) {
                Map<String, String> properties = new HashMap<>();
                properties.put("http.proxyHost", "http://proxy.example.com");
                properties.put("http.nonProxyHosts", h);
                tests.add(new TestCase(properties, "http://" + s, false));
            }
        }
        return tests;
    }

    // unsorted tests
    private static Collection<TestCase> misc() {
        List<TestCase> t = new LinkedList<>();
        t.add(new TestCase("oracle.com", "http://137.254.16.101", true));
        t.add(new TestCase("google.com", "http://74.125.200.101", true));

        t.add(new TestCase("google.com|google.ie", "http://google.co.uk",
                true));
        t.add(new TestCase("google.com|google.ie", "http://google.com",
                false));
        t.add(new TestCase("google.com|google.ie", "http://google.ie",
                false));
        t.add(new TestCase("google.com|google.com|google.ie",
                "http://google.ie", false));

        t.add(new TestCase("google.com|bing.com|yahoo.com",
                "http://127.0.0.1", false));
        t.add(new TestCase("google.com|bing.com|yahoo.com",
                "http://google.com", false));
        t.add(new TestCase("google.com|bing.com|yahoo.com",
                "http://bing.com", false));
        t.add(new TestCase("google.com|bing.com|yahoo.com",
                "http://yahoo.com", false));

        t.add(new TestCase("google.com|bing.com", "http://google.com", false));
        t.add(new TestCase("google.com|bing.com", "http://bing.com", false));
        t.add(new TestCase("google.com|bing.com", "http://yahoo.com",
                true));
        t.add(new TestCase("google.com|bing.co*", "http://google.com", false));
        t.add(new TestCase("google.com|bing.co*", "http://bing.com", false));
        t.add(new TestCase("google.com|bing.co*", "http://yahoo.com",
                true));
        t.add(new TestCase("google.com|*ing.com", "http://google.com", false));
        t.add(new TestCase("google.com|*ing.com", "http://bing.com", false));
        t.add(new TestCase("google.com|*ing.com", "http://yahoo.com",
                true));
        t.add(new TestCase("google.co*|bing.com", "http://google.com", false));
        t.add(new TestCase("google.co*|bing.com", "http://bing.com", false));
        t.add(new TestCase("google.co*|bing.com", "http://yahoo.com",
                true));
        t.add(new TestCase("google.co*|bing.co*", "http://google.com", false));
        t.add(new TestCase("google.co*|bing.co*", "http://bing.com", false));
        t.add(new TestCase("google.co*|bing.co*", "http://yahoo.com",
                true));
        t.add(new TestCase("google.co*|*ing.com", "http://google.com", false));
        t.add(new TestCase("google.co*|*ing.com", "http://bing.com", false));
        t.add(new TestCase("google.co*|*ing.com", "http://yahoo.com",
                true));
        t.add(new TestCase("*oogle.com|bing.com", "http://google.com", false));
        t.add(new TestCase("*oogle.com|bing.com", "http://bing.com", false));
        t.add(new TestCase("*oogle.com|bing.com", "http://yahoo.com",
                true));
        t.add(new TestCase("*oogle.com|bing.co*", "http://google.com", false));
        t.add(new TestCase("*oogle.com|bing.co*", "http://bing.com", false));
        t.add(new TestCase("*oogle.com|bing.co*", "http://yahoo.com",
                true));
        t.add(new TestCase("*oogle.com|*ing.com", "http://google.com", false));
        t.add(new TestCase("*oogle.com|*ing.com", "http://bing.com", false));
        t.add(new TestCase("*oogle.com|*ing.com", "http://yahoo.com",
                true));

        t.add(new TestCase("google.com|bing.com|yahoo.com", "http://google.com", false));
        t.add(new TestCase("google.com|bing.com|yahoo.com", "http://bing.com", false));
        t.add(new TestCase("google.com|bing.com|yahoo.com", "http://yahoo.com", false));
        t.add(new TestCase("google.com|bing.com|yahoo.com",
                "http://duckduckgo.com", true));

        t.add(new TestCase("p-proxy.com", "http://p-proxy.com", false));
        t.add(new TestCase("google.co*|google.ie", "http://google.co.uk",
                false));
        t.add(new TestCase("*google.*", "http://google.co.uk",
                false));
        t.add(new TestCase("*", "http://google.co.uk",false));
        t.add(new TestCase("localhost|*", "http://google.co.uk",false));
        t.add(new TestCase("*|oracle.com", "http://google.co.uk",false));
        t.add(new TestCase("*|oracle.com|*", "http://google.co.uk",false));
        t.add(new TestCase("*|*", "http://google.co.uk",false));


        t.add(new TestCase("*oracle.com", "http://my.oracle.com", false));
        t.add(new TestCase("google.com|bing.com|yahoo.com", "http://127.0.0.1", false));
        t.add(new TestCase("google.com|bing.com|yahoo.com", "http://yahoo.com", false));

        t.add(new TestCase("localhost|host.example.com", "http://localhost",
                false));
        t.add(new TestCase("localhost|host.example.com",
                "http://host.example.com", false));
        t.add(new TestCase("localhost|host.example.com",
                "http://google.com", true));
        return t;
    }


    private static <T> T withSystemPropertiesSet(
            Map<String, String> localProperties,
            Callable<? extends T> code) {
        Map<String, String> backup = new HashMap<>();
        try {
            backupAndSetProperties(localProperties, backup);
            return code.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            restoreProperties(backup);
        }
    }

    private static void backupAndSetProperties(
            Map<String, String> localProperties,
            Map<String, String> oldProperties) {
        for (Map.Entry<String, String> e : localProperties.entrySet()) {
            String oldValue = System.setProperty(e.getKey(), e.getValue());
            oldProperties.put(e.getKey(), oldValue);
        }
    }

    private static void restoreProperties(Map<String, String> oldProperties) {
        for (Map.Entry<String, String> e : oldProperties.entrySet()) {
            String oldValue = e.getValue();
            String key = e.getKey();
            if (oldValue == null)
                System.getProperties().remove(key);
            else
                System.setProperty(key, oldValue);
        }
    }

    private static class TestCase {

        final Map<String, String> localProperties;
        final String urlhost;
        final boolean expectedProxying;

        TestCase(String nonProxyHosts, String urlhost,
                 boolean expectedProxying) {
            this(nonProxyHosts, "proxy.example.com", urlhost,
                    expectedProxying);
        }

        TestCase(String nonProxyHosts, String proxyHost, String urlhost,
                 boolean expectedProxying) {
            this(new HashMap<String, String>() {
                {
                    put("http.nonProxyHosts", nonProxyHosts);
                    put("http.proxyHost", proxyHost);
                }
            }, urlhost, expectedProxying);
        }

        TestCase(Map<String, String> localProperties, String urlhost,
                 boolean expectedProxying) {
            this.localProperties = localProperties;
            this.urlhost = urlhost;
            this.expectedProxying = expectedProxying;
        }

        void run() {
            System.out.printf("urlhost=%s properties=%s: proxied? %s%n",
                    urlhost, localProperties, expectedProxying);

            List<Proxy> proxies = withSystemPropertiesSet(localProperties,
                    () -> ProxySelector.getDefault().select(
                            URI.create(urlhost))
            );

            verify(proxies);
        }

        void verify(List<? extends Proxy> proxies) {

            boolean actualProxying = !(proxies.size() == 1 &&
                    proxies.get(0).type() == Proxy.Type.DIRECT);

            if (actualProxying != expectedProxying)
                throw new AssertionError(String.format(
                        "Expected %s connection for %s (given " +
                                "properties=%s). Here's the list of proxies " +
                                "returned: %s",
                        expectedProxying ? "proxied" : "direct", urlhost,
                        localProperties, proxies
                ));
        }
    }
}
