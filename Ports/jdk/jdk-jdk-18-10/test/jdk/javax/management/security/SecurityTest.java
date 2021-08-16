/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8058865
 * @summary Checks various secure ways of connecting from remote jmx client
 * @author Olivier Lagneau
 * @modules java.management.rmi
 * @library /test/lib
 * @compile MBS_Light.java ServerDelegate.java TestSampleLoginModule.java
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Dusername=SQE_username -Dpassword=SQE_password SecurityTest -server -mapType x.password.file -client -mapType credentials
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Dusername=UNKNOWN_username -Dpassword=SQE_password SecurityTest -server -mapType x.password.file -client -mapType credentials -expectedThrowable java.lang.SecurityException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Dusername=SQE_username -Dpassword=WRONG_password SecurityTest -server -mapType x.password.file -client -mapType credentials -expectedThrowable java.lang.SecurityException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Dsusername=TestJMXAuthenticatorUsername -Dspassword=TestJMXAuthenticatorPassword -Dusername=TestJMXAuthenticatorUsername -Dpassword=TestJMXAuthenticatorPassword SecurityTest -server -mapType x.authenticator -client -mapType credentials
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Dsusername=TestJMXAuthenticatorUsername -Dspassword=TestJMXAuthenticatorPassword -Dusername=AnotherTestJMXAuthenticatorUsername -Dpassword=TestJMXAuthenticatorPassword SecurityTest -server -mapType x.authenticator -client -mapType credentials -expectedThrowable java.lang.SecurityException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Dlogin.config.file=${test.src}/login.config -Dpassword.file=password.properties -Dusername=usernameFileLoginModule -Dpassword=passwordFileLoginModule SecurityTest -server -mapType x.login.config.PasswordFileAuthentication -client -mapType credentials
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Dlogin.config.file=${test.src}/login.config.UNKNOWN -Dpassword.file=password.properties -Dusername=usernameFileLoginModule -Dpassword=passwordFileLoginModule SecurityTest -server -mapType x.login.config.PasswordFileAuthentication -client -mapType credentialss -expectedThrowable java.lang.SecurityException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Dlogin.config.file=${test.src}/login.config -Dpassword.file=password.properties -Dusername=usernameFileLoginModule -Dpassword=passwordFileLoginModule SecurityTest -server -mapType x.login.config.UnknownAuthentication -client -mapType credentials -expectedThrowable java.lang.SecurityException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Dlogin.config.file=${test.src}/login.config -Dsusername=usernameSampleLoginModule -Dspassword=passwordSampleLoginModule -Dpassword.file=password.properties -Dusername=usernameSampleLoginModule -Dpassword=passwordSampleLoginModule SecurityTest -server -mapType x.login.config.SampleLoginModule -client -mapType credentials
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Dlogin.config.file=${test.src}/login.config -Dsusername=usernameSampleLoginModule -Dspassword=passwordSampleLoginModule -Dpassword.file=password.properties -Dusername=AnotherUsernameSampleLoginModule -Dpassword=passwordSampleLoginModule SecurityTest -server -mapType x.login.config.SampleLoginModule -client -mapType credentials -expectedThrowable java.lang.SecurityException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD SecurityTest -server -mapType rmi.client.socket.factory.ssl;rmi.server.socket.factory.ssl -keystore keystoreAgent -keystorepassword glopglop -client -truststore truststoreClient -truststorepassword glopglop
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD SecurityTest -server -mapType rmi.client.socket.factory.ssl;rmi.server.socket.factory.ssl -keystore keystoreAgent -keystorepassword glopglop -client -truststore truststoreClient -truststorepassword WRONG_password -expectedThrowable java.io.IOException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD SecurityTest -server -mapType rmi.server.socket.factory.ssl -keystore keystoreAgent -keystorepassword glopglop -client -truststore truststoreClient -truststorepassword glopglop -expectedThrowable java.io.IOException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD SecurityTest -server -mapType rmi.client.socket.factory.ssl -keystore keystoreAgent -keystorepassword glopglop -client -truststore truststoreClient -truststorepassword glopglop -expectedThrowable java.io.IOException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD SecurityTest -server -mapType rmi.client.socket.factory.ssl;rmi.server.socket.factory.ssl -keystore keystoreAgent -keystorepassword glopglop -client -expectedThrowable java.io.IOException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD SecurityTest -server -mapType rmi.client.socket.factory.ssl;rmi.server.socket.factory.ssl.need.client.authentication -keystore keystoreAgent -keystorepassword glopglop -truststore truststoreAgent -truststorepassword glopglop -client  -keystore keystoreClient -keystorepassword glopglop -truststore truststoreClient -truststorepassword glopglop
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD SecurityTest -server -mapType rmi.client.socket.factory.ssl;rmi.server.socket.factory.ssl.need.client.authentication -keystore keystoreAgent -keystorepassword glopglop -truststore truststoreAgent -truststorepassword glopglop -client -keystore keystoreClient -keystorepassword WRONG_password -truststore truststoreClient -truststorepassword glopglop -expectedThrowable java.io.IOException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD SecurityTest -server -mapType rmi.client.socket.factory.ssl;rmi.server.socket.factory.ssl.need.client.authentication -keystore keystoreAgent -keystorepassword glopglop -truststore truststoreAgent -truststorepassword glopglop -client -truststore truststoreClient -truststorepassword glopglop -expectedThrowable java.io.IOException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD SecurityTest -server -mapType rmi.client.socket.factory.ssl;rmi.server.socket.factory.ssl.need.client.authentication -keystore keystoreAgent -keystorepassword glopglop -client -keystore keystoreClient -keystorepassword glopglop -truststore truststoreClient -truststorepassword glopglop -expectedThrowable java.io.IOException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Djavax.rmi.ssl.client.enabledCipherSuites=SSL_RSA_WITH_RC4_128_MD5 SecurityTest -server -mapType rmi.client.socket.factory.ssl;rmi.server.socket.factory.ssl.enabled.cipher.suites.md5 -keystore keystoreAgent -keystorepassword glopglop -client -truststore truststoreClient -truststorepassword glopglop
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Djavax.rmi.ssl.client.enabledCipherSuites=SSL_RSA_WITH_RC4_128_SHA SecurityTest -server -mapType rmi.client.socket.factory.ssl;rmi.server.socket.factory.ssl.enabled.cipher.suites.md5 -keystore keystoreAgent -keystorepassword glopglop -client -truststore truststoreClient -truststorepassword glopglop -expectedThrowable java.io.IOException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Djavax.rmi.ssl.client.enabledCipherSuites=SSL_RSA_WITH_RC4_128_MD5 SecurityTest -server -mapType rmi.client.socket.factory.ssl;rmi.server.socket.factory.ssl.enabled.cipher.suites.sha -keystore keystoreAgent -keystorepassword glopglop -client -truststore truststoreClient -truststorepassword glopglop -expectedThrowable java.io.IOException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Djavax.rmi.ssl.client.enabledProtocols=SSLv3 SecurityTest -server -mapType rmi.client.socket.factory.ssl;rmi.server.socket.factory.ssl.enabled.protocols.sslv3 -keystore keystoreAgent -keystorepassword glopglop -client -truststore truststoreClient -truststorepassword glopglop
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Djavax.rmi.ssl.client.enabledProtocols=TLSv1 SecurityTest -server -mapType rmi.client.socket.factory.ssl;rmi.server.socket.factory.ssl.enabled.protocols.sslv3 -keystore keystoreAgent -keystorepassword glopglop -client -truststore truststoreClient -truststorepassword glopglop -expectedThrowable java.io.IOException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Djavax.rmi.ssl.client.enabledProtocols=SSLv3 SecurityTest -server -mapType rmi.client.socket.factory.ssl;rmi.server.socket.factory.ssl.enabled.protocols.tlsv1 -keystore keystoreAgent -keystorepassword glopglop -client -truststore truststoreClient -truststorepassword glopglop -expectedThrowable java.io.IOException
 */

import java.io.File;
import java.util.Map ;
import java.util.HashMap ;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory ;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import javax.management.Attribute ;
import javax.management.ObjectName ;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import java.security.Security;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.ProcessTools;

public class SecurityTest {

    static final String SERVER_CLASS_NAME = "SecurityTest";
    static final String CLIENT_CLASS_NAME = "SecurityTest$ClientSide";
    static final String CLIENT_CLASS_MAIN = CLIENT_CLASS_NAME;

    static final String USERNAME_PROPERTY = "username";
    static final String PASSWORD_PROPERTY = "password";

    static final String SERVER_DELEGATE_MBEAN_NAME =
        "defaultDomain:class=ServerDelegate";

    static final String RMI_SERVER_SOCKET_FACTORY_SSL = "rmi.server.socket.factory.ssl";
    static final String RMI_CLIENT_SOCKET_FACTORY_SSL = "rmi.client.socket.factory.ssl";
    static final String KEYSTORE_PROPNAME = "javax.net.ssl.keyStore";
    static final String KEYSTORE_PWD_PROPNAME = "javax.net.ssl.keyStorePassword";
    static final String TRUSTSTORE_PROPNAME = "javax.net.ssl.trustStore";
    static final String TRUSTSTORE_PWD_PROPNAME = "javax.net.ssl.trustStorePassword";

    static final String RMI_SSL_CLIENT_ENABLEDCIPHERSUITES =
        "javax.rmi.ssl.client.enabledCipherSuites";
    static final String RMI_SSL_CLIENT_ENABLEDPROTOCOLS =
        "javax.rmi.ssl.client.enabledProtocols";

    private JMXConnectorServer cs;

    // Construct and set keyStore properties from given map
    static void setKeyStoreProperties(Map<String, Object> map) {

        String keyStore = (String) map.get("-keystore");
        keyStore = buildSourcePath(keyStore);
        System.setProperty(KEYSTORE_PROPNAME, keyStore);
        System.out.println("keyStore location = \"" + keyStore + "\"");

        String password = (String) map.get("-keystorepassword");
        System.setProperty(KEYSTORE_PWD_PROPNAME, password);
        System.out.println("keyStore password = " + password);

    }

    // Construct and set trustStore properties from given map
    static void setTrustStoreProperties(Map<String, Object> map) {

        String trustStore = (String) map.get("-truststore");
        trustStore = buildSourcePath(trustStore);
        System.setProperty(TRUSTSTORE_PROPNAME, trustStore);
        System.out.println("trustStore location = \"" + trustStore + "\"");

        String password = (String) map.get("-truststorepassword");
        System.setProperty(TRUSTSTORE_PWD_PROPNAME, password);
        System.out.println("trustStore password = " + password);

    }

    /*
     * First Debug properties and arguments are collect in expected
     * map  (argName, value) format, then calls original test's run method.
     */
    public static void main(String args[]) throws Exception {

        System.out.println("=================================================");

        // Parses parameters
        Utils.parseDebugProperties();

        // Supported parameters list format is :
        // "MainClass [-server <param-spec> ...] [-client <param-spec> ...]
        // with <param-spec> either "-parami valuei" or "-parami"
        HashMap<String, Object> serverMap = new HashMap<>() ;
        int clientArgsIndex =
            Utils.parseServerParameters(args, SERVER_CLASS_NAME, serverMap);

        // Extract and records client params
        String[] clientParams = null;
        if (clientArgsIndex < args.length) {
            int clientParamsSize = args.length - clientArgsIndex;
            clientParams = new String[clientParamsSize];
            System.arraycopy(args, clientArgsIndex, clientParams, 0, clientParamsSize);
        } else {
            clientParams = new String[0];
        }

        // Run test
        SecurityTest test = new SecurityTest();
        test.run(serverMap, clientParams);

    }

    // Return full path of filename in the test sopurce directory
    private static String buildSourcePath(String filename) {
        return System.getProperty("test.src") + File.separator + filename;
    }

    /*
     * Collects security run params for server side.
     */
    private HashMap<String, Object> setServerSecurityEnv(Map<String, Object> map)
    throws Exception {

        // Creates Authentication environment from server side params
        HashMap<String, Object> env = new HashMap<>();

        // Retrieve and set keystore and truststore config if any
        if (map.containsKey("-keystore") &&
            map.get("-keystore") != null) {
            setKeyStoreProperties(map);
        }
        System.out.println("Done keystore properties");

        if (map.containsKey("-truststore") &&
            map.get("-truststore") != null) {
            setTrustStoreProperties(map);
        }
        System.out.println("Done truststore properties");

        String value = null;
        if ((value = (String)map.get("-mapType")) != null) {

            // Case of remote password file with all authorized credentials
            if (value.contains("x.password.file")) {
                String passwordFileStr = buildSourcePath("password.properties");
                env.put("jmx.remote.x.password.file", passwordFileStr);
                System.out.println("Added " + passwordFileStr +
                    " file as jmx.remote.x.password.file");
            }

            // Case of dedicated authenticator class : TestJMXAuthenticator
            if (value.contains("x.authenticator")) {
                env.put("jmx.remote.authenticator", new TestJMXAuthenticator()) ;
                System.out.println(
                    "Added \"jmx.remote.authenticator\" = TestJMXAuthenticator");
            }

            // Case of security config file with standard Authentication
            if (value.contains("x.login.config.PasswordFileAuthentication")) {
                String loginConfig = System.getProperty("login.config.file");

                // Override the default JAAS configuration
                System.setProperty("java.security.auth.login.config",
                    "file:" + loginConfig);
                System.out.println("Overrided default JAAS configuration with " +
                    "\"java.security.auth.login.config\" = \"" + loginConfig + "\"") ;

                env.put("jmx.remote.x.login.config", "PasswordFileAuthentication") ;
                System.out.println(
                    "Added \"jmx.remote.x.login.config\" = " +
                    "\"PasswordFileAuthentication\"") ;

                // redirects "password.file" property to file in ${test.src}
                String passwordFileStr =
                    buildSourcePath(System.getProperty("password.file"));
                System.setProperty("password.file", passwordFileStr);
                System.out.println(
                    "Redirected \"password.file\" property value to = " +
                    passwordFileStr) ;
            }

            // Case of security config file with unexisting athentication config
            if (value.contains("x.login.config.UnknownAuthentication")) {
                String loginConfig = System.getProperty("login.config.file");

                // Override the default JAAS configuration
                System.setProperty("java.security.auth.login.config",
                        "file:" + loginConfig);
                System.out.println("Overrided default JAAS configuration with " +
                    "\"java.security.auth.login.config\" = \"" + loginConfig + "\"") ;

                env.put("jmx.remote.x.login.config", "UnknownAuthentication") ;
                System.out.println(
                    "Added \"jmx.remote.x.login.config\" = " +
                    "\"UnknownAuthentication\"") ;

                // redirects "password.file" property to file in ${test.src}
                 String passwordFileStr =
                   buildSourcePath(System.getProperty("password.file"));
                System.setProperty("password.file", passwordFileStr);
                System.out.println(
                    "Redirected \"password.file\" property value to = " +
                    passwordFileStr) ;
            }

            // Case of security config file with dedicated login module
            if (value.contains("x.login.config.SampleLoginModule")) {
                String loginConfig = System.getProperty("login.config.file");

                // Override the default JAAS configuration
                System.setProperty("java.security.auth.login.config",
                        "file:" + loginConfig);
                System.out.println("Overrided default JAAS configuration with " +
                    "\"java.security.auth.login.config\" = \"" + loginConfig + "\"") ;

                env.put("jmx.remote.x.login.config", "SampleLoginModule") ;
                System.out.println(
                    "Added \"jmx.remote.x.login.config\" = " +
                    "\"SampleLoginModule\"") ;
            }

            // Simple rmi ssl authentication
            if (value.contains(RMI_CLIENT_SOCKET_FACTORY_SSL)) {
                env.put("jmx.remote.rmi.client.socket.factory",
                    new SslRMIClientSocketFactory()) ;
                System.out.println(
                     "Added \"jmx.remote.rmi.client.socket.factory\"" +
                     " = SslRMIClientSocketFactory") ;
            }

            if (value.contains(RMI_SERVER_SOCKET_FACTORY_SSL)) {
                if (value.contains(
                        "rmi.server.socket.factory.ssl.need.client.authentication")) {
                   // rmi ssl authentication with client authentication
                   env.put("jmx.remote.rmi.server.socket.factory",
                       new SslRMIServerSocketFactory(null, null, true)) ;
                   System.out.println(
                       "Added \"jmx.remote.rmi.server.socket.factory\"" +
                       " = SslRMIServerSocketFactory with client authentication") ;

                } else if (value.contains("rmi.server.socket.factory.ssl.enabled.cipher.suites.md5")) {
                    // Allows all ciphering and protocols for testing purpose
                    Security.setProperty("jdk.tls.disabledAlgorithms", "");

                    env.put("jmx.remote.rmi.server.socket.factory",
                        new SslRMIServerSocketFactory(
                            new String[] {"SSL_RSA_WITH_RC4_128_MD5"}, null, false));
                    System.out.println(
                        "Added \"jmx.remote.rmi.server.socket.factory\"" +
                        " = SslRMIServerSocketFactory with SSL_RSA_WITH_RC4_128_MD5 cipher suite");

                } else if (value.contains("rmi.server.socket.factory.ssl.enabled.cipher.suites.sha")) {
                    // Allows all ciphering and protocols for testing purpose
                    Security.setProperty("jdk.tls.disabledAlgorithms", "");

                    env.put("jmx.remote.rmi.server.socket.factory",
                        new SslRMIServerSocketFactory(
                            new String[] { "SSL_RSA_WITH_RC4_128_SHA" }, null, false)) ;
                    System.out.println(
                        "Added \"jmx.remote.rmi.server.socket.factory\"" +
                        " = SslRMIServerSocketFactory with SSL_RSA_WITH_RC4_128_SHA cipher suite") ;

                } else if (value.contains("rmi.server.socket.factory.ssl.enabled.protocols.sslv3")) {
                    // Allows all ciphering and protocols for testing purpose
                    Security.setProperty("jdk.tls.disabledAlgorithms", "");

                    env.put("jmx.remote.rmi.server.socket.factory",
                        new SslRMIServerSocketFactory(null, new String[] {"SSLv3"}, false)) ;
                    System.out.println(
                        "Added \"jmx.remote.rmi.server.socket.factory\"" +
                        " = SslRMIServerSocketFactory with SSLv3 protocol") ;

                } else if (value.contains("rmi.server.socket.factory.ssl.enabled.protocols.tlsv1")) {
                    // Allows all ciphering and protocols for testing purpose
                    Security.setProperty("jdk.tls.disabledAlgorithms", "");

                    env.put("jmx.remote.rmi.server.socket.factory",
                        new SslRMIServerSocketFactory(null, new String[] {"TLSv1"}, false)) ;
                    System.out.println(
                        "Added \"jmx.remote.rmi.server.socket.factory\"" +
                        " = SslRMIServerSocketFactory with TLSv1 protocol") ;

                } else {
                    env.put("jmx.remote.rmi.server.socket.factory",
                        new SslRMIServerSocketFactory());
                    System.out.println(
                        "Added \"jmx.remote.rmi.server.socket.factory\"" +
                        " = SslRMIServerSocketFactory");
                }
            }
        }

        return env;
    }

    /*
     * Create the MBeansServer side of the test and returns its address
     */
    private JMXServiceURL createServerSide(Map<String, Object> serverMap)
    throws Exception {
        final int NINETY_SECONDS = 90;

        System.out.println("SecurityTest::createServerSide: Start") ;

        // Prepare server side security env
        HashMap<String, Object> env = setServerSecurityEnv(serverMap);

        // Create and start mbean server and connector server
        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
        cs = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
        cs.start();

        // Waits availibility of connector server
        Utils.waitReady(cs, NINETY_SECONDS);

        JMXServiceURL addr = cs.getAddress();

        System.out.println("SecurityTest::createServerSide: Done.") ;

        return addr;
    }

    /*
     * Creating command-line for running subprocess JVM:
     *
     * JVM command line is like:
     * {test_jdk}/bin/java {defaultopts} -cp {test.class.path} {testopts} main
     *
     * {defaultopts} are the default java options set by the framework.
     *
     */
    private List<String> buildCommandLine(String args[]) {

        System.out.println("SecurityTest::buildCommandLine: Start") ;

        List<String> opts = new ArrayList<>();
        opts.add(JDKToolFinder.getJDKTool("java"));
        opts.addAll(Arrays.asList(jdk.test.lib.Utils.getTestJavaOpts()));

        // We need to forward some properties to the client side
        opts.add("-Dtest.src=" + System.getProperty("test.src"));

        String usernameValue = System.getProperty(USERNAME_PROPERTY);
        if (usernameValue != null) {
            System.out.println("SecurityTest::buildCommandLine: "+
                " forward username property to client side");
            opts.add("-D" + USERNAME_PROPERTY + "=" + usernameValue);
        }
        String passwordValue = System.getProperty(PASSWORD_PROPERTY);
        if (passwordValue != null) {
            System.out.println("SecurityTest::buildCommandLine: "+
                " forward password property to client side");
            opts.add("-D" + PASSWORD_PROPERTY + "=" + passwordValue);
        }

        String enabledCipherSuites =
            System.getProperty(RMI_SSL_CLIENT_ENABLEDCIPHERSUITES);
        if (enabledCipherSuites != null) {
            System.out.println("SecurityTest::buildCommandLine: "+
                " forward enabledCipherSuites property to client side");
            opts.add("-D" + RMI_SSL_CLIENT_ENABLEDCIPHERSUITES +
                "=" + enabledCipherSuites);
        }

        String enabledProtocols =
            System.getProperty(RMI_SSL_CLIENT_ENABLEDPROTOCOLS);
        if (enabledProtocols != null) {
            System.out.println("SecurityTest::buildCommandLine: "+
                " forward enabledProtocols property to client side");
            opts.add("-D" + RMI_SSL_CLIENT_ENABLEDPROTOCOLS +
                "=" + enabledProtocols);
        }

        opts.add("-cp");
        opts.add(System.getProperty("test.class.path", "test.class.path"));
        opts.add(CLIENT_CLASS_MAIN);
        opts.addAll(Arrays.asList(args));

        System.out.println("SecurityTest::buildCommandLine: Done.") ;

        return opts;
    }

    /**
     * Runs SecurityTest$ClientSide with the passed options and redirects
     * subprocess standard I/O to the current (parent) process. This provides a
     * trace of what happens in the subprocess while it is runnning (and before
     * it terminates).
     *
     * @param serviceUrlStr string representing the JMX service Url to connect to.
     */
    private int runClientSide(String args[], String serviceUrlStr) throws Exception {

        System.out.println("SecurityTest::runClientSide: Start") ;

        // Building command-line
        List<String> opts = buildCommandLine(args);
        opts.add("-serviceUrl");
        opts.add(serviceUrlStr);

        // Launch separate JVM subprocess
        int exitCode = 0;
        String[] optsArray = opts.toArray(new String[0]);
        ProcessBuilder pb = new ProcessBuilder(optsArray);
        Process p = ProcessTools.startProcess("SecurityTest$ClientSide", pb);

        // Handling end of subprocess
        try {
            exitCode = p.waitFor();
            if (exitCode != 0) {
                System.out.println(
                    "Subprocess unexpected exit value of [" + exitCode +
                    "]. Expected 0.\n");
            }
        } catch (InterruptedException e) {
            System.out.println("Parent process interrupted with exception : \n " + e + " :" );

            // Parent thread unknown state, killing subprocess.
            p.destroyForcibly();

            throw new RuntimeException(
                "Parent process interrupted with exception : \n " + e + " :" );

        } finally {
            if (p.isAlive()) {
                p.destroyForcibly();
            }

            System.out.println("SecurityTest::runClientSide: Done") ;

            return exitCode;
        }

     }

    public void run(Map<String, Object> serverArgs, String clientArgs[]) {

        System.out.println("SecurityTest::run: Start") ;
        int errorCount = 0;

        try {
            // Initialise the server side
            JMXServiceURL urlToUse = createServerSide(serverArgs);

            // Run client side
            errorCount = runClientSide(clientArgs, urlToUse.toString());

            if ( errorCount == 0 ) {
                System.out.println("SecurityTest::run: Done without any error") ;
            } else {
                System.out.println(
                    "SecurityTest::run: Done with " + errorCount + " error(s)");
                throw new RuntimeException("errorCount = " + errorCount);
            }

            cs.stop();

        } catch(Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static class ClientSide {

        private JMXConnector cc = null;
        private MBeanServerConnection mbsc = null;

        public static void main(String args[]) throws Exception {

            // Parses parameters
            Utils.parseDebugProperties();

            // Supported parameters list format is : "MainClass [-client <param-spec> ...]
            // with <param-spec> either "-parami valuei" or "-parami"
            HashMap<String, Object> clientMap = new HashMap<>() ;
            Utils.parseClientParameters(args, CLIENT_CLASS_NAME, clientMap);

            // Run test
            ClientSide test = new ClientSide();
            test.run(clientMap);
        }

        public void run(Map<String, Object> args) {

            System.out.println("ClientSide::run: Start");
            int errorCount = 0;

            try {
                // Setup client side parameters
                HashMap<String, Object> env = new HashMap<>();

                // If needed allows all ciphering and protocols for testing purpose
                if (System.getProperty(RMI_SSL_CLIENT_ENABLEDCIPHERSUITES) != null) {
                    Security.setProperty("jdk.tls.disabledAlgorithms", "");
                }

                // If needed allows all ciphering and protocols for testing purpose
                if (System.getProperty(RMI_SSL_CLIENT_ENABLEDPROTOCOLS) != null) {
                    Security.setProperty("jdk.tls.disabledAlgorithms", "");
                }

                // Retrieve and set keystore and truststore config if any
                if (args.containsKey("-keystore") &&
                    args.get("-keystore") != null) {
                    SecurityTest.setKeyStoreProperties(args);
                }
                if (args.containsKey("-truststore") &&
                    args.get("-truststore") != null) {
                    SecurityTest.setTrustStoreProperties(args);
                }

                Object value = args.get("-mapType");
                if ((value != null) &&
                    value.equals("credentials")) {
                    String username = System.getProperty("username");
                    String password = System.getProperty("password");
                    Utils.debug(Utils.DEBUG_STANDARD,
                        "add \"jmx.remote.credentials\" = \"" +
                        username + "\", \"" + password + "\"");
                    env.put("jmx.remote.credentials",
                        new String[] { username , password });
                }

                String expectedThrowable = (String) args.get("-expectedThrowable");

                String authCallCountName = "-expectedAuthenticatorCallCount";
                int authCallCountValue = 0;
                if (args.containsKey(authCallCountName)) {
                    authCallCountValue =
                        (new Integer((String) args.get(authCallCountName))).intValue();
                }

                try {
                    // Get a connection to remote mbean server
                    JMXServiceURL addr = new JMXServiceURL((String)args.get("-serviceUrl"));
                    cc = JMXConnectorFactory.connect(addr,env);
                    mbsc = cc.getMBeanServerConnection();

                    // In case we should have got an exception
                    if (expectedThrowable != null) {
                        System.out.println("ClientSide::run: (ERROR) " +
                            " Connect did not fail with expected " + expectedThrowable);
                        errorCount++;
                    } else {
                        System.out.println("ClientSide::run: (OK) Connect succeed");
                    }
                } catch (Throwable e) {
                    Utils.printThrowable(e, true);
                    if (expectedThrowable != null) {
                        if (Utils.compareThrowable(e, expectedThrowable)) {
                            System.out.println("ClientSide::run: (OK) " +
                                "Connect failed with expected " + expectedThrowable);
                        } else {
                            System.out.println("ClientSide::run: (ERROR) Connect failed with " +
                                e.getClass() + " instead of expected " +
                                expectedThrowable);
                            errorCount++;
                        }
                    } else {
                        System.out.println("ClientSide::run: (ERROR) " +
                            "Connect failed with exception");
                        errorCount++;
                    }
                }

                // Depending on the client state,
                // perform some requests
                if (mbsc != null && errorCount == 0) {
                    // Perform some little JMX requests
                    System.out.println("ClientSide::run: Start sending requests");

                    doRequests();

                    // In case authentication has been used we check how it did.
                    if (authCallCountValue != 0) {
                        errorCount += checkAuthenticator(mbsc, authCallCountValue);
                    }
                }
            } catch (Exception e) {
                Utils.printThrowable(e, true);
                errorCount++;
            } finally {
                // Terminate the JMX Client if any
                if (cc != null) {
                    try {
                        cc.close();
                    } catch (Exception e) {
                        Utils.printThrowable(e, true) ;
                        errorCount++;
                    }
                }
            }

            System.out.println("ClientSide::run: Done");

            // Handle result
            if (errorCount != 0) {
                throw new RuntimeException();
            }
        }

        private void doRequests() throws Exception {

            // Send  some requests to the remote JMX server
            ObjectName objName1 =
                new ObjectName("TestDomain:class=MBS_Light,rank=1");
            String mbeanClass = "MBS_Light";
            Exception exception = new Exception("MY TEST EXCEPTION");
            Attribute attException = new Attribute("AnException", exception);
            Error error = new Error("MY TEST ERROR");
            Attribute attError = new Attribute("AnError", error);
            String opParamString = "TOTORO";
            RjmxMBeanParameter opParam = new RjmxMBeanParameter(opParamString);
            Object[] params1 = {opParamString};
            String[] sig1 = {"java.lang.String"};
            Object[] params2 = {opParam};
            String[] sig2 = {"RjmxMBeanParameter"};

            // Create and register the MBean
            Utils.debug(Utils.DEBUG_STANDARD,
                "ClientSide::doRequests: Create and register the MBean");
            mbsc.createMBean(mbeanClass, objName1);
            if (!mbsc.isRegistered(objName1)) {
                throw new Exception("Unable to register an MBean");
            }

            // Set attributes of the MBean
            Utils.debug(Utils.DEBUG_STANDARD,
                "ClientSide::doRequests: Set attributes of the MBean");
            mbsc.setAttribute(objName1, attException);
            mbsc.setAttribute(objName1, attError);

            // Get attributes of the MBean
            Utils.debug(Utils.DEBUG_STANDARD,
                "ClientSide::doRequests: Get attributes of the MBean");
            Exception retException =
                (Exception) mbsc.getAttribute(objName1,"AnException");
            if (!retException.getMessage().equals(exception.getMessage())) {
                System.out.println("Expected = " + exception);
                System.out.println("Got = " + retException);
                throw new Exception("Attribute AnException not as expected");
            }
            Error retError = (Error) mbsc.getAttribute(objName1, "AnError");
            if (!retError.getMessage().equals(error.getMessage())) {
                System.out.println("Expected = " + error);
                System.out.println("Got = " + retError);
                throw new Exception("Attribute AnError not as expected");
            }

            // Invoke operations on the MBean
            Utils.debug(Utils.DEBUG_STANDARD,
                "ClientSide::doRequests: Invoke operations on the MBean");
            RjmxMBeanParameter res1 =
                (RjmxMBeanParameter) mbsc.invoke(objName1, "operate1", params1, sig1);
            if (!res1.equals(opParam)) {
                System.out.println("Expected = " + opParam);
                System.out.println("Got = " + res1);
                throw new Exception("Operation operate1 behaved badly");
            }
            String res2 =
                (String) mbsc.invoke(objName1, "operate2", params2, sig2);
            if (!res2.equals(opParamString)) {
                System.out.println("Expected = " + opParamString);
                System.out.println("Got = " + res2);
                throw new Exception("Operation operate2 behaved badly");
            }

            // Unregister the MBean
            Utils.debug(Utils.DEBUG_STANDARD,
                "ClientSide::doRequests: Unregister the MBean");
            mbsc.unregisterMBean(objName1);
            if (mbsc.isRegistered(objName1)) {
                throw new Exception("Unable to unregister an MBean");
            }
        }

        /**
         * Make some check about the instance of TestJMXAuthenticator.
         * The authenticator is supposed to have set some properties on
         * a ServerDelegate MBean.
         * We compare the number of times it has been called with the expected value.
         * We also check the Principal that has been given to the authenticator
         * was not null.
         * That method is of use to authentication with the JSR 262.
         * @param mbs
         * @param expectedAuthenticatorCallCount
         * @return The number of errors encountered.
         * @throws java.lang.Exception
         */
        protected int checkAuthenticator(MBeanServerConnection mbs,
                int expectedAuthenticatorCallCount) throws Exception {
            int errorCount = 0;

            // Ensure the authenticator has been called the right number
            // of times.
            int callCount =
                    ((Integer) mbs.getAttribute(
                    new ObjectName(SERVER_DELEGATE_MBEAN_NAME),
                    "TestJMXAuthenticatorCallCount")).intValue();

            if (callCount == expectedAuthenticatorCallCount) {
                System.out.println("---- OK Authenticator has been called "
                        + expectedAuthenticatorCallCount + " time");
            } else {
                errorCount++;
                System.out.println("---- ERROR Authenticator has been called " + callCount
                        + " times in place of " + expectedAuthenticatorCallCount);
            }

            // Ensure the provider has been called with
            // a non null Principal.
            String principalString =
                (String) mbs.getAttribute(
                new ObjectName(SERVER_DELEGATE_MBEAN_NAME),
                "TestJMXAuthenticatorPrincipalString");

            if (principalString == null) {
                errorCount++;
                System.out.println("---- ERROR Authenticator has been called"
                        + " with a null Principal");
            } else {
                if (principalString.length() > 0) {
                    System.out.println("---- OK Authenticator has been called"
                            + " with the Principal " + principalString);
                } else {
                    errorCount++;
                    System.out.println("---- ERROR Authenticator has been called"
                            + " with an empty Principal");
                }
            }

            return errorCount;
        }

    }

}
