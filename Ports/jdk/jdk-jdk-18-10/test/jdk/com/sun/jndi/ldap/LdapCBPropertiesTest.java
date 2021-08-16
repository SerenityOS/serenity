/*
 * Copyright (c) 2020, Azul Systems, Inc. All rights reserved.
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

/**
 * @test
 * @bug 8245527
 * @library lib/ /test/lib
 * @run main/othervm LdapCBPropertiesTest true  true  com.sun.jndi.ldap.tls.cbtype tls-server-end-point
 * @run main/othervm LdapCBPropertiesTest false false com.sun.jndi.ldap.tls.cbtype tls-server-end-point
 * @run main/othervm LdapCBPropertiesTest true  true  com.sun.jndi.ldap.tls.cbtype tls-server-end-point com.sun.jndi.ldap.connect.timeout 2000
 * @run main/othervm LdapCBPropertiesTest false false com.sun.jndi.ldap.tls.cbtype tls-server-end-point com.sun.jndi.ldap.connect.timeout 2000
 * @run main/othervm LdapCBPropertiesTest false true  com.sun.jndi.ldap.tls.cbtype tls-unknown
 * @run main/othervm LdapCBPropertiesTest false true  jdk.internal.sasl.tlschannelbinding value
 * @summary test new JNDI property to control the Channel Binding data
 */

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetAddress;
import java.net.URI;
import java.util.Hashtable;

import org.ietf.jgss.GSSException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.security.sasl.SaslException;

import jdk.test.lib.net.URIBuilder;

public class LdapCBPropertiesTest {
    /*
     * Where do we find the keystores?
     */
    static String pathToStores = "../../../../javax/net/ssl/etc";
    static String keyStoreFile = "keystore";
    static String trustStoreFile = "truststore";
    static String passwd = "passphrase";

    static boolean debug = false;

    public static void main(String[] args) throws Exception {
        String keyFilename =
                System.getProperty("test.src", "./") + "/" + pathToStores +
                        "/" + keyStoreFile;
        String trustFilename =
                System.getProperty("test.src", "./") + "/" + pathToStores +
                        "/" + trustStoreFile;

        System.setProperty("javax.net.ssl.keyStore", keyFilename);
        System.setProperty("javax.net.ssl.keyStorePassword", passwd);
        System.setProperty("javax.net.ssl.trustStore", trustFilename);
        System.setProperty("javax.net.ssl.trustStorePassword", passwd);

        if (debug)
            System.setProperty("javax.net.debug", "all");

        /*
         * Start the tests.
         */
        new LdapCBPropertiesTest(args);
    }

    /*
     * Primary constructor, used to drive remainder of the test.
     */
    LdapCBPropertiesTest(String[] args) throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        SSLServerSocketFactory sslssf =
                (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket sslServerSocket =
                (SSLServerSocket) sslssf.createServerSocket(0, 0, loopback);
        int serverPort = sslServerSocket.getLocalPort();

        try (var ignore = new BaseLdapServer(sslServerSocket).start()) {
            doClientSide(serverPort, args);
        }
    }

    /*
     * Define the client side of the test.
     *
     * The server should start at this time already
     */
    void doClientSide(int serverPort, String[] args) throws Exception {
        boolean passed = false;
        boolean shouldPass = Boolean.parseBoolean(args[0]);
        boolean shouldConnect = Boolean.parseBoolean(args[1]);
        // set disableEndpointIdentification to disable hostname verification
        if (shouldConnect) {
            System.setProperty(
                    "com.sun.jndi.ldap.object.disableEndpointIdentification", "true");
        }

        // Set up the environment for creating the initial context
        Hashtable env = new Hashtable();
        URI uri = URIBuilder.newBuilder()
            .scheme("ldaps")
            .loopback()
            .port(serverPort)
            .build();
        env.put(Context.PROVIDER_URL, uri.toString());
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");

        // read properties
        for (int i = 2; i < args.length; i += 2) {
            env.put(args[i], args[i + 1]);
            if (debug)
                System.out.println("Env=" + args[i] + "=" + args[i + 1]);
        }

        try {
            DirContext ctx = new InitialDirContext(env);
            passed = shouldPass;
            ctx.close();
        } catch (NamingException ne) {
            // only NamingException is allowed
            if (debug)
                System.out.println("Exception=" + ne + " cause=" + ne.getRootCause());
            passed = handleNamingException(ne, shouldPass, shouldConnect);
        } catch(Exception e) {
            System.err.println("Failed: caught an unexpected Exception - " + e);
            throw e;
        } finally {
            // test if internal property accessible to application
            if(shouldPass &&
                    env.get("jdk.internal.sasl.tlschannelbinding") != null) {
                throw new Exception(
                        "Test FAILED: jdk.internal.sasl.tlschannelbinding should not be accessible");
            }
        }
        if (!passed) {
            throw new Exception(
                    "Test FAILED: NamingException exception should be thrown");
        }
        System.out.println("Test PASSED");
    }

    private static boolean handleNamingException(NamingException ne, boolean shouldPass, boolean shouldConnect)
        throws NamingException {
        if (ne instanceof AuthenticationException &&
            ne.getRootCause() instanceof SaslException) {
            SaslException saslEx = (SaslException) ne.getRootCause();
            if (shouldConnect && saslEx.getCause() instanceof GSSException) {
                // SSL connection successful, expected exception from SaslClient
                if (shouldPass)
                    return true;
            }
        }
        if (!shouldConnect) {
            // SSL handshake fails
            Exception ex = ne;
            while(ex != null && !(ex instanceof CommunicationException)) {
                ex = (Exception)ex.getCause();
            }
            if (ex != null) {
                if (ex.getCause() instanceof SSLException) {
                    if (!shouldPass)
                        return true;
                }
            }
        }
        if (!shouldPass && ne.getRootCause() == null) {
            // Expected exception caused by Channel Binding parameter inconsistency
            return true;
        }
        throw ne;
    }
}
