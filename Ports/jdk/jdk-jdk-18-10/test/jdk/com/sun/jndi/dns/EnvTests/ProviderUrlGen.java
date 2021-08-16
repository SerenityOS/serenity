/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8208279
 * @summary If DNS name resolution is configured normally for the local
 *          machine, construct a context using the configured servers and verify
 *          that a PROVIDER_URL property is generated for the context.
 *          If DNS is not configured, or if JDK version is earlier than 1.4.1,
 *          then this test is a no-op (it passes without doing anything).
 * @library ../lib/
 * @modules jdk.naming.dns/com.sun.jndi.dns
 *          java.base/sun.security.util
 * @run main ProviderUrlGen
 */

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import java.lang.reflect.InvocationTargetException;

public class ProviderUrlGen extends DNSTestBase {

    public ProviderUrlGen() {
        setLocalServer(false);
    }

    public static void main(String[] args) throws Exception {
        if (!isPlatformServersAvailable()) {
            DNSTestUtils.debug("DNS not configured. There's nothing to test.");
            return;
        }

        new ProviderUrlGen().run(args);
    }

    @Override public void runTest() throws Exception {
        initContext();

        String providerUrl = (String) context().getEnvironment()
                .get(Context.PROVIDER_URL);

        if (providerUrl == null) {
            throw new RuntimeException("Failed: PROVIDER_URL not set");
        }

        DNSTestUtils.debug("PROVIDER_URL = \"" + providerUrl + "\"");

        // We don't know exactly what providerUrl's value should be.
        // Check that it is a space-separated list of one or more URLs
        // of the form "dns://xxxxxx".

        String[] urls = providerUrl.split(" ");
        if (urls.length < 1) {
            throw new RuntimeException("Failed:  PROVIDER_URL is empty");
        }

        for (String url : urls) {
            DNSTestUtils.debug(url);
            if (!checkUrl(url)) {
                throw new RuntimeException(
                        "Failed:  Unexpected DNS URL: " + url);
            }
        }
    }

    private void initContext() throws NamingException {
        env().remove(Context.PROVIDER_URL);
        setContext(new InitialDirContext(env()));
    }

    private static boolean checkUrl(String url) {
        return url.startsWith("dns://") && url.length() >= 7;
    }

    private static boolean isPlatformServersAvailable() {
        try {
            java.lang.reflect.Method psa = com.sun.jndi.dns.DnsContextFactory.class
                    .getMethod("platformServersAvailable", null);
            return (Boolean) psa.invoke(null, null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
    }
}
