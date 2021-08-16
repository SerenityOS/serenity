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

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;

/**
 * This is utility class for DNS test, it contains many helper methods to
 * support test construction.
 *
 * For basic test sequence see TestBase and DNSTestBase.
 */
public class DNSTestUtils {
    public static final String TEST_DNS_SERVER_THREAD = "test.dns.server.thread";
    public static final String TEST_DNS_ROOT_URL = "test.dns.root.url";
    public static final int HOSTS_LOOKUP_MAX_DEPTH = 3;

    protected static boolean debug = true;

    /**
     * Check that attributes contains the mandatory attributes and the right
     * objectclass attribute.
     *
     * @param attrs     given attributes to verify
     * @param mandatory given mandatory for verification
     * @param optional  given optional for verification
     * @return <tt>true</tt>  if check ok
     */
    public static boolean checkSchema(Attributes attrs, String[] mandatory,
            String[] optional) {
        // Check mandatory attributes
        for (String mandatoryAttr : mandatory) {
            if (attrs.get(mandatoryAttr) == null) {
                debug("missing mandatory attribute: " + mandatoryAttr);
                return false;
            }
        }

        // Check optional attributes
        int optMissing = 0;
        for (String optionalAttr : optional) {
            if (attrs.get(optionalAttr) == null) {
                debug("warning: missing optional attribute: " + optionalAttr);
                ++optMissing;
            }
        }

        if (attrs.size() > (mandatory.length + (optional.length
                - optMissing))) {
            debug("too many attributes: " + attrs);
            return false;
        }

        return true;
    }

    /**
     * Process command line arguments and init env.
     * This method will prepare default environments which to be used to initial
     * DirContext.
     *
     * @param localServer <tt>true</tt> if this test will run against with local
     *                    server against dump message playback
     * @param testname    test case name to identify playback file
     * @param args        additional arguments for env preparation
     * @return prepared env which will be used later to initial DirContext
     */
    public static Hashtable<Object, Object> initEnv(boolean localServer,
            String testname, String[] args) {

        Hashtable<Object, Object> env = new Hashtable<>();

        // set some default parameters if no additional specified
        env.put("DNS_DOMAIN", "domain1.com.");
        env.put("FOREIGN_DOMAIN", "Central.Sun.COM.");
        env.put("FOREIGN_LEAF", "sunweb");

        // set defaults for some JNDI properties
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.dns.DnsContextFactory");

        boolean traceEnable = false;
        boolean loopPlayback = false;
        for (String arg : args) {
            if (arg.startsWith("-D")) {
                extractProperty(arg.substring(2), env);
            } else if (arg.equalsIgnoreCase("-trace")) {
                traceEnable = true;
            } else if (arg.equalsIgnoreCase("-loop")) {
                loopPlayback = true;
            }
        }

        debug = Boolean.valueOf(System.getProperty("debug", "true"));

        // override testname here if it's been specified
        String newTestName = (String) env.get("testname");
        if (newTestName != null && !newTestName.isEmpty()) {
            testname = newTestName;
        }

        if (env.get("DNS_SERVER") != null) {
            String port = (String) env.get("DNS_PORT");
            String portSuffix = (port == null) ? "" : ":" + port;
            String url = "dns://" + env.get("DNS_SERVER") + portSuffix;
            env.put(Context.PROVIDER_URL, url);
            env.put(Context.PROVIDER_URL, url + "/" + env.get("DNS_DOMAIN"));
        }

        Thread inst = null;
        if (traceEnable) {
            // if trace is enabled, create DNSTracer to dump those message
            inst = createDNSTracer(testname, env);
        } else {
            if (localServer) {
                // if use local server, create local DNSServer for playback
                inst = createDNSServer(testname, loopPlayback);
            } else {
                // for tests which run against remote server
                // or no server required
                debug("Skip local DNS Server creation ");
            }
        }

        if (inst != null) {
            inst.start();
            env.put(TEST_DNS_SERVER_THREAD, inst);
            String url = "dns://localhost:" + ((Server) inst).getPort();

            env.put(TEST_DNS_ROOT_URL, url);
            env.put(Context.PROVIDER_URL, url + "/" + env.get("DNS_DOMAIN"));
        }

        return env;
    }

    /**
     * Clean-up the given directory context.
     *
     * @param ctx given context object
     */
    public static void cleanup(Context ctx) {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (NamingException e) {
                // ignore
            }
        }
    }

    private static void extractProperty(String propString,
            Hashtable<Object, Object> env) {
        int index;

        if ((index = propString.indexOf('=')) > 0) {
            env.put(propString.substring(0, index),
                    propString.substring(index + 1));
        } else {
            throw new RuntimeException(
                    "Failed to extract test args property from " + propString);
        }
    }

    /**
     * Return new created DNS tracer.
     *
     * @param testname test case name to identify playback file
     * @param env      given env for initialization
     * @return created DNS tracer
     * @see DNSTracer
     */
    public static DNSTracer createDNSTracer(String testname,
            Hashtable<Object, Object> env) {
        try {
            PrintStream outStream = new PrintStream(getCaptureFile(testname));
            return new DNSTracer(outStream, (String) env.get("DNS_SERVER"),
                    Integer.parseInt((String) env.get("DNS_PORT")));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error: failed to create DNSTracer : " + e.getMessage(), e);
        }
    }

    /**
     * Return new created local DNS Server.
     *
     * @param testname test case name to identify playback file
     * @param loop     <tt>true</tt> if DNS server required playback message in loop
     * @return created local DNS Server
     * @see DNSServer
     */
    public static DNSServer createDNSServer(String testname, boolean loop) {
        String path = getCaptureFile(testname);
        if (Files.exists(Paths.get(path))) {
            try {
                return new DNSServer(path, loop);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Error: failed to create DNSServer : " + e.getMessage(),
                        e);
            }
        } else {
            throw new RuntimeException(
                    "Error: failed to create DNSServer, not found dns "
                            + "cache file " + path);
        }
    }

    /**
     * Return dns message capture file path.
     *
     * @param testname test case name to identify playback file
     * @return capture file path
     */
    public static String getCaptureFile(String testname) {
        return Paths.get(System.getProperty("test.src"))
                .resolve(testname + ".dns").toString();
    }

    /**
     * Enable hosts file.
     *
     * @param hostsFile given hosts file
     */
    public static void enableHostsFile(String hostsFile) {
        System.out.println("Enable jdk.net.hosts.file = " + hostsFile);
        System.setProperty("jdk.net.hosts.file", hostsFile);
    }

    /**
     * Enable hosts file by given searching depth.
     *
     * @param depth given depth for searching hosts file
     */
    public static void enableHostsFile(int depth) {
        Path path = Paths.get(System.getProperty("test.src", "."))
                .toAbsolutePath();
        for (int i = depth; i >= 0; i--) {
            Path filePath = path.resolve("hosts");
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                enableHostsFile(filePath.toString());
                break;
            }

            path = path.getParent();
            if (path == null) {
                break;
            }
        }
    }

    /**
     * Print given object if debug enabled.
     *
     * @param object given object to print
     */
    public static void debug(Object object) {
        if (debug) {
            System.out.println(object);
        }
    }

    /**
     * Verify attributes contains the mandatory attributes and the right
     * objectclass attribute, will throw RuntimeException if verify failed.
     *
     * @param attrs     given attributes to verify
     * @param mandatory given mandatory for verification
     * @param optional  given optional for verification
     */
    public static void verifySchema(Attributes attrs, String[] mandatory,
            String[] optional) {
        debug(attrs);
        if (!checkSchema(attrs, mandatory, optional)) {
            throw new RuntimeException("Check schema failed.");
        }
    }

    /**
     * Return dns root url.
     *
     * @param env given env
     * @return dns root url
     */
    public static String getRootUrl(Hashtable<Object, Object> env) {
        return (String) env.get(TEST_DNS_ROOT_URL);
    }

    /**
     * Assemble a fully-qualified domain name from the base component and the
     * domain name.
     *
     * @param base    given base component
     * @param env     given env
     * @param primary <tt>true</tt> if primary domain
     * @return assembled fully-qualified domain name
     */
    public static String buildFqdn(String base, Hashtable<Object, Object> env,
            boolean primary) {
        String domain = (String) (primary ?
                env.get("DNS_DOMAIN") :
                env.get("FOREIGN_DOMAIN"));

        return base + "." + domain;
    }
}
