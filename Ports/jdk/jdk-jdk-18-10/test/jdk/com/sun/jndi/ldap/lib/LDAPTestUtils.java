/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jndi.ldap.LdapURL;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class LDAPTestUtils {
    public static final String TEST_LDAP_SERVER_THREAD = "test.ldap.server.thread";
    public static final int CERTS_LOOKUP_MAX_DEPTH = 4;

    protected static boolean debug = true;

    /*
     * Process command line arguments and return properties in a Hashtable.
     */
    public static Hashtable<Object, Object> initEnv(String testname,
                                                    String[] args) {
        return initEnv(null, testname, args, false);
    }

    public static Hashtable<Object, Object> initEnv(ServerSocket socket,
                                                    String testname, String[] args, boolean authInfo) {
        return initEnv(socket, null, testname, args, authInfo);
    }

    public static Hashtable<Object, Object> initEnv(ServerSocket socket, String providerUrl,
                                                    String testname, String[] args, boolean authInfo) {

        Hashtable<Object, Object> env = new Hashtable<>();
        String root = "o=IMC,c=US";
        String vendor = "Vendor1";
        String client = "Client1";
        String realm = "";
        Vector<String> refs = new Vector<>();

        // set defaults for some JNDI properties
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");

        if (authInfo) {
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, "cn=admin,o=IMC,c=US");
            env.put(Context.SECURITY_CREDENTIALS, "secret99");
        }

        env.put("root", root);
        env.put("vendor", vendor);
        env.put("client", client);

        boolean traceEnable = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-D") && (args.length > i + 1)) {
                extractProperty(args[++i], env);
            } else if (args[i].startsWith("-D")) {
                extractProperty(args[i].substring(2), env);
            } else if (args[i].equals("-referral") && (args.length > i + 1)) {
                refs.addElement(args[++i]);
            } else if (args[i].equals("-trace")) {
                traceEnable = true;
            }
        }

        env.put("disabled.realm", realm);

        if (refs.size() > 0) {
            env.put("referrals", refs);
        }

        if (traceEnable) {
            enableLDAPTrace(env, testname);
        } else {
            if (socket != null) {
                env.put(TEST_LDAP_SERVER_THREAD,
                        startLDAPServer(socket, getCaptureFile(testname)));
                String url = providerUrl != null ? providerUrl :
                        "ldap://localhost:" + socket.getLocalPort();
                env.put("java.naming.provider.url", url);
            } else {
                // for tests which run against remote server or no server
                // required
                debug("Skip local LDAP Server creation "
                        + "since ServerSocket is null");
            }
        }

        return env;
    }

    /*
     * Clean-up the directory context.
     */
    public static void cleanup(DirContext ctx) {
        if (ctx != null) {
            try {
                ctx.close();
            } catch (NamingException e) {
                // ignore
            }
        }
    }

    /*
     * Clean-up the sub context.
     */
    public static void cleanupSubcontext(DirContext ctx, String name) {
        if (ctx != null) {
            try {
                ctx.destroySubcontext(name);
            } catch (NamingException ne) {
                // ignore
            }
        }
    }

    /*
     * Assemble a distinguished name from the base components and the
     * namespace root.
     *
     * The components are prefixed with 'dc=' if the root is a DC-style name.
     * Otherwise they are prefixed with 'ou='.
     */
    public static String buildDN(String[] bases, String root) {

        StringBuilder dn = new StringBuilder();
        String prefix;

        if (!root.contains("dc=")) {
            prefix = "ou=";
        } else {
            prefix = "dc=";
        }

        for (String base : bases) {
            dn.append(prefix).append(base).append(",");
        }

        return dn.append(root).toString();
    }

    /*
     * Scan the results to confirm that the expected name is present.
     */
    public static int checkResult(NamingEnumeration results, String name)
            throws NamingException {

        return checkResult(results, new String[] { name }, null);
    }

    /*
     * Scan the results to confirm that the expected names and attributes
     * are present.
     */
    public static int checkResult(NamingEnumeration results, String[] names,
            Attributes attrs) throws NamingException {

        int found = 0;

        while (results != null && results.hasMore()) {

            SearchResult entry = (SearchResult) results.next();
            String entryDN = entry.getName();

            debug(">>> received: " + entryDN);

            if (entry.isRelative()) {
                entryDN = entryDN.toLowerCase(); // normalize
            } else {
                LdapURL url = new LdapURL(entryDN); // extract DN
                entryDN = url.getDN().toLowerCase(); // normalize
            }

            for (String name : names) {
                if ((entryDN.contains(name.toLowerCase())) || (entryDN
                        .equalsIgnoreCase(name))) {

                    debug(">>> checked results: found '" + name + "'");

                    if (attrs == null || foundAttributes(entry, attrs)) {
                        found++;
                        break;
                    }
                }
            }
        }

        debug(">>> checked results: found " + found
                + " entries that meet the criteria.");

        return found;
    }

    /*
     * Confirm that the attributes are present in the entry.
     */
    public static boolean foundAttributes(SearchResult entry, Attributes attrs)
            throws NamingException {

        Attributes eattrs = entry.getAttributes();
        int found = 0;

        if ((eattrs == null) || (attrs == null)) {
            return false;
        }

        for (NamingEnumeration ne = attrs.getAll(); ne.hasMoreElements(); ) {

            Attribute attr = (Attribute) ne.next();

            if (equalsIgnoreCase(eattrs.get(attr.getID()), attr)) {
                found++;
            } else {
                debug(">>> foundAttributes: no match for " + attr.getID());
            }
        }
        debug(">>> foundAttributes: found " + found + " attributes");
        return (found == attrs.size());
    }

    public static Thread startLDAPServer(ServerSocket serverSocket,
            String fileName) {
        if (serverSocket == null) {
            throw new RuntimeException("Error: failed to create LDAPServer "
                    + "since ServerSocket is null");
        }

        if (!Files.exists(Paths.get(fileName))) {
            throw new RuntimeException(
                    "Error: failed to create LDAPServer, not found ldap "
                            + "cache file " + fileName);
        }

        Thread thread = new Thread(() -> {
            try {
                new test.LDAPServer(serverSocket, fileName);
            } catch (Exception e) {
                System.out.println("Warning: LDAP server running with issue");
                e.printStackTrace();
            }
        });

        thread.start();
        return thread;
    }

    private static boolean equalsIgnoreCase(Attribute received,
            Attribute expected) {

        if (received == null || !received.getID()
                .equalsIgnoreCase(expected.getID())) {
            return false;
        }

        try {

            Enumeration expectedVals = expected.getAll();
            Object obj;
            while (expectedVals.hasMoreElements()) {
                obj = expectedVals.nextElement();
                if (!received.contains(obj)) {
                    if (!(obj instanceof String)) {
                        return false;
                    }
                    if (!received.contains(((String) obj).toLowerCase())) {
                        return false;
                    }
                }
            }

        } catch (NamingException e) {
            return false;
        }

        return true;
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

    private static void enableLDAPTrace(Hashtable<Object, Object> env,
            String testname) {
        try {
            PrintStream outStream = new PrintStream(getCaptureFile(testname));
            env.put("com.sun.jndi.ldap.trace.ber", outStream);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(
                    "Error: failed to enable ldap trace: " + e.getMessage(), e);
        }
    }

    private static String getCaptureFile(String testname) {
        return Paths.get(System.getProperty("test.src"))
                .resolve(testname + ".ldap").toString();
    }

    public static void debug(Object object) {
        if (debug) {
            System.out.println(object);
        }
    }

    public static String findCertsHome(int depth) {
        Path path = Paths.get(System.getProperty("test.src", "."))
                .toAbsolutePath();
        for (int i = depth; i >= 0; i--) {
            Path homePath = path.resolve("certs");
            if (Files.exists(homePath) && Files.isDirectory(homePath)) {
                return homePath.toString();
            }

            path = path.getParent();
            if (path == null) {
                break;
            }
        }

        return System.getProperty("test.src", ".");
    }
}
