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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Permission;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;

import sun.net.PortConfig;

import jdk.test.lib.RandomFactory;

/**
 * @test
 * @bug 8160768
 * @key randomness intermittent
 * @summary ctx provider tests for ldap.
 *  Two test cases need to establish connection to the
 *  unreachable port on localhost. Each tries 5 connection
 *  attempts with a random port expecting for connection to fail.
 *  In rare cases it could establish connections due to services
 *  running on these ports, therefore it can fail intermittently.
 * @modules java.naming/com.sun.jndi.ldap java.base/sun.net
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @compile dnsprovider/TestDnsProvider.java
 * @run main/othervm LdapDnsProviderTest
 * @run main/othervm LdapDnsProviderTest nosm
 * @run main/othervm -Djava.security.manager=allow LdapDnsProviderTest smnodns
 * @run main/othervm -Djava.security.manager=allow LdapDnsProviderTest smdns
 * @run main/othervm LdapDnsProviderTest nosmbaddns
 */

class DNSSecurityManager extends SecurityManager {

    private boolean dnsProvider = false;

    public void setAllowDnsProvider(boolean allow) {
        dnsProvider = allow;
    }

    @Override
    public void checkPermission(Permission p) {
        if (p.getName().equals("ldapDnsProvider") && !dnsProvider) {
            throw new SecurityException(p.getName());
        }
    }
}

class ProviderTest implements Callable<Boolean> {

    private final String url;
    private final String expected;
    private final Hashtable<String, String> env = new Hashtable<>(11);

    public ProviderTest(String url, String expected) {
        this.url = url;
        this.expected = expected;
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    }

    boolean shutItDown(InitialContext ctx) {
        try {
            if (ctx != null) ctx.close();
            return true;
        } catch (NamingException ex) {
            return false;
        }
    }

    public Boolean call() {
        boolean passed;
        InitialContext ctx = null;

        if (url != null) {
            env.put(Context.PROVIDER_URL, url);
        }

        // Set JNDI LDAP connect timeout property. It helps to prevent
        // initial bind operation from blocking in case of a local process
        // listening on the port specified in the URL. With the property set,
        // the bind operation will fail with timeout exception, and then it
        // could be retried with another port number.
        env.put("com.sun.jndi.ldap.connect.timeout", "1000");

        try {
            ctx = new InitialDirContext(env);
            SearchControls scl = new SearchControls();
            scl.setSearchScope(SearchControls.SUBTREE_SCOPE);
            ((InitialDirContext)ctx).search(
                    "ou=People,o=Test", "(objectClass=*)", scl);
            throw new RuntimeException("Search should not complete");
        } catch (NamingException e) {
            passed = e.toString().contains(expected);
            System.err.println((passed ? "Expected" : "Unexpected") +
                    " NamingException observed: " + e.toString());
            // Print stack trace only for unexpected exceptions
            if (!passed) {
                e.printStackTrace();
            }
        } finally {
            shutItDown(ctx);
        }
        return passed;
    }
}

public class LdapDnsProviderTest {

    private static final String TEST_CLASSES =
            System.getProperty("test.classes", ".");

    public static void writeFile(String content, File dstFile)
        throws IOException
    {
        try (FileOutputStream dst = new FileOutputStream(dstFile)) {
            byte[] buf = content.getBytes();
            dst.write(buf, 0, buf.length);
        }
    }

    public static void installServiceConfigurationFile(String content) {
        String filename = "javax.naming.ldap.spi.LdapDnsProvider";

        File dstDir = new File(TEST_CLASSES, "META-INF/services");
        if (!dstDir.exists()) {
            if (!dstDir.mkdirs()) {
                throw new RuntimeException(
                    "could not create META-INF/services directory " + dstDir);
            }
        }
        File dstFile = new File(dstDir, filename);

        try {
            writeFile(content, dstFile);
        } catch (IOException e) {
            throw new RuntimeException("could not install " + dstFile, e);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("nosm")) {
            // no security manager, serviceloader
            installServiceConfigurationFile("dnsprovider.TestDnsProvider");
            runTest("ldap:///dc=example,dc=com", "yupyupyup:389");
        } else if (args.length > 0 && args[0].equals("smnodns")) {
            // security manager & serviceloader
            installServiceConfigurationFile("dnsprovider.TestDnsProvider");
            // install security manager
            System.setSecurityManager(new DNSSecurityManager());
            runTest("ldap:///dc=example,dc=com", "ServiceConfigurationError");
        } else if (args.length > 0 && args[0].equals("smdns")) {
            // security manager & serviceloader
            DNSSecurityManager sm = new DNSSecurityManager();
            installServiceConfigurationFile("dnsprovider.TestDnsProvider");
            // install security manager
            System.setSecurityManager(sm);
            sm.setAllowDnsProvider(true);
            runTest("ldap:///dc=example,dc=com", "yupyupyup:389");
        } else if (args.length > 0 && args[0].equals("nosmbaddns")) {
            // no security manager, no serviceloader
            // DefaultLdapDnsProvider
            installServiceConfigurationFile("dnsprovider.MissingDnsProvider");
            // no SecurityManager
            runTest("ldap:///dc=example,dc=com", "not found");
        } else {
            // no security manager, no serviceloader
            // DefaultLdapDnsProvider
            System.err.println("TEST_CLASSES:");
            System.err.println(TEST_CLASSES);
            File f = new File(
                    TEST_CLASSES, "META-INF/services/javax.naming.ldap.spi.LdapDnsProvider");
            if (f.exists()) {
                f.delete();
            }

            // no SecurityManager
            runTest("ldap:///dc=example,dc=com", "localhost:389");
            runTest("ldap://localhost/dc=example,dc=com", "localhost:389");
            runLocalHostTestWithRandomPort("ldap", "/dc=example,dc=com", 5);
            runLocalHostTestWithRandomPort("ldaps", "/dc=example,dc=com", 5);
            runTest("ldaps://localhost/dc=example,dc=com", "localhost:636");
            runTest(null, "localhost:389");
            runTest("", "ConfigurationException");
        }
    }

    // Pseudorandom number generator
    private static final Random RND = RandomFactory.getRandom();
    // Port numbers already seen to be generated by pseudorandom generator
    private static final Set<Integer> SEEN_PORTS = new HashSet<>();

    // Get random, previously unseen port number from [1111, PortConfig.getUpper()) range
    private static int generateUnseenPort() {
        int port;
        do {
            port = 1111 + RND.nextInt(PortConfig.getUpper() - 1111);
            // Seen ports will never contain more than maxAttempts*2 ports
        } while (SEEN_PORTS.contains(port));
        SEEN_PORTS.add(port);
        return port;
    }

    // Run test with ldap connection to localhost and random port. The test is expected to fail
    // with CommunicationException that is caused by connection refuse exception.
    // But in case if there is a service running on the same port the connection
    // will be established and then closed or timed-out. Both cases will generate exception
    // messages which differ from the expected one.
    // For such cases the test will be repeated with another random port. That will be done
    // maxAttempts times. If the expected exception won't be observed - test will be treated
    // as failed.
    private static void runLocalHostTestWithRandomPort(String scheme, String path, int maxAttempts) {
        for (int attempt = 0; attempt <= maxAttempts; attempt++) {
            boolean attemptSuccessful = true;
            int port = generateUnseenPort();

            // Construct URL for the current attempt
            String url = scheme + "://localhost" + ":" + port + path;

            // Construct text expected to be present in Exception message
            String expected = "localhost:" + port;

            System.err.printf("Iteration %d: Testing: url='%s', expected content='%s'%n",
                    attempt, url, expected);

            FutureTask<Boolean> future = new FutureTask<>(
                    new ProviderTest(url, expected));
            new Thread(future).start();
            while (!future.isDone()) {
                try {
                    if (!future.get()) {
                        if (attempt == maxAttempts) {
                            throw new RuntimeException("Test failed, ProviderTest" +
                                    " returned false " + maxAttempts + " times");
                        } else {
                            System.err.printf("Iteration %d failed:" +
                                    " ProviderTest returned false%n", attempt);
                            attemptSuccessful = false;
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Iteration %d failed to execute provider test: " + e.getMessage());
                    attemptSuccessful = false;
                }
            }
            if (attemptSuccessful) {
                System.err.println("Test passed. It took " + (attempt + 1) + " iterations to complete");
                break;
            }
        }
    }

    private static void runTest(String url, String expected) {
        FutureTask<Boolean> future =
            new FutureTask<>(
                    new ProviderTest(url, expected));
        new Thread(future).start();

        System.err.printf("Testing: url='%s', expected content='%s'%n", url, expected);
        while (!future.isDone()) {
            try {
                if (!future.get()) {
                    System.err.println("Test failed");
                    throw new RuntimeException(
                            "Test failed, ProviderTest returned false");
                }
            } catch (Exception e) {
                if (!e.toString().contains(expected)) {
                    System.err.println("Test failed");
                    throw new RuntimeException(
                            "Test failed, unexpected result");
                }
            }
        }
        System.err.println("Test passed");
    }

}

