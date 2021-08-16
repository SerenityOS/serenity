/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Expect ConnectException for all non-security related connect errors
 * @bug 8204864
 * @run testng/othervm ConnectExceptionTest
 * @run testng/othervm/java.security.policy=noPermissions.policy ConnectExceptionTest
 */

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static java.lang.System.out;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class ConnectExceptionTest {

    static final ProxySelector INVALID_PROXY = new ProxySelector() {
        final List<Proxy> proxy = List.of(new Proxy(Proxy.Type.HTTP,
                InetSocketAddress.createUnresolved("proxy.invalid", 8080)));
        @Override public List<Proxy> select(URI uri) { return proxy; }
        @Override public void connectFailed(URI uri, SocketAddress sa, IOException ioe) { }
        @Override public String toString() { return "INVALID_PROXY"; }
    };

    static final ProxySelector NO_PROXY = new ProxySelector() {
        @Override public List<Proxy> select(URI uri) { return List.of(Proxy.NO_PROXY); }
        @Override public void connectFailed(URI uri, SocketAddress sa, IOException ioe) { }
        @Override public String toString() { return "NO_PROXY"; }
    };

    @DataProvider(name = "uris")
    public Object[][] uris() {
        return new Object[][]{
            { "http://test.invalid/",  NO_PROXY       },
            { "https://test.invalid/", NO_PROXY       },
            { "http://test.invalid/",  INVALID_PROXY  },
            { "https://test.invalid/", INVALID_PROXY  },
        };
    }

    @Test(dataProvider = "uris")
    void testSynchronousGET(String uriString, ProxySelector proxy) throws Exception {
        out.printf("%n---%ntestSynchronousGET starting uri:%s, proxy:%s%n", uriString, proxy);
        HttpClient client = HttpClient.newBuilder().proxy(proxy).build();

        URI uri = URI.create(uriString);
        HttpRequest request = HttpRequest.newBuilder(uri).build();
        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            fail("UNEXPECTED response: " + response + ", body:" + response.body());
        } catch (ConnectException ioe) {
            out.println("Caught expected: " + ioe);
            //ioe.printStackTrace(out);
        } catch (SecurityException expectedIfSMIsSet) {
            out.println("Caught expected: " + expectedIfSMIsSet);
            assertTrue(System.getSecurityManager() != null);
        }
    }

    @Test(dataProvider = "uris")
    void testSynchronousPOST(String uriString, ProxySelector proxy) throws Exception {
        out.printf("%n---%ntestSynchronousPOST starting uri:%s, proxy:%s%n", uriString, proxy);
        HttpClient client = HttpClient.newBuilder().proxy(proxy).build();

        URI uri = URI.create(uriString);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .POST(BodyPublishers.ofString("Does not matter"))
                .build();
        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            fail("UNEXPECTED response: " + response + ", body:" + response.body());
        } catch (ConnectException ioe) {
            out.println("Caught expected: " + ioe);
            //ioe.printStackTrace(out);
        } catch (SecurityException expectedIfSMIsSet) {
            out.println("Caught expected: " + expectedIfSMIsSet);
            assertTrue(System.getSecurityManager() != null);
        }
    }

    @Test(dataProvider = "uris")
    void testAsynchronousGET(String uriString, ProxySelector proxy) throws Exception {
        out.printf("%n---%ntestAsynchronousGET starting uri:%s, proxy:%s%n", uriString, proxy);
        HttpClient client = HttpClient.newBuilder().proxy(proxy).build();

        URI uri = URI.create(uriString);
        HttpRequest request = HttpRequest.newBuilder(uri).build();
        try {
            HttpResponse<String> response = client.sendAsync(request, BodyHandlers.ofString()).get();
            fail("UNEXPECTED response: " + response + ", body:" + response.body());
        } catch (ExecutionException ee) {
            Throwable t = ee.getCause();
            if (t instanceof ConnectException) {
                out.println("Caught expected: " + t);
            } else if (t instanceof SecurityException) {
                out.println("Caught expected: " + t);
                assertTrue(System.getSecurityManager() != null);
            } else {
                t.printStackTrace(out);
                fail("Unexpected exception: " + t);
            }
        }
    }

    @Test(dataProvider = "uris")
    void testAsynchronousPOST(String uriString, ProxySelector proxy) throws Exception {
        out.printf("%n---%ntestAsynchronousPOST starting uri:%s, proxy:%s%n", uriString, proxy);
        HttpClient client = HttpClient.newBuilder().proxy(proxy).build();

        URI uri = URI.create(uriString);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .POST(BodyPublishers.ofString("Does not matter"))
                .build();
        try {
            HttpResponse<String> response = client.sendAsync(request, BodyHandlers.ofString()).get();
            fail("UNEXPECTED response: " + response + ", body:" + response.body());
        } catch (ExecutionException ee) {
            Throwable t = ee.getCause();
            if (t instanceof ConnectException) {
                out.println("Caught expected: " + t);
            } else if (t instanceof SecurityException) {
                out.println("Caught expected: " + t);
                assertTrue(System.getSecurityManager() != null);
            } else {
                t.printStackTrace(out);
                fail("Unexpected exception: " + t);
            }
        }
    }
}
