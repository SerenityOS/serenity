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
import sun.management.jmxremote.ConnectorBootstrap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.BindException;
import java.rmi.server.ExportException;

import java.util.Properties;
import java.util.Iterator;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;

import javax.management.remote.*;
import javax.management.*;

import jdk.internal.agent.AgentConfigurationError;

import java.security.Security;

/**
 * <p>This class implements unit test for RMI Bootstrap.
 * When called with no arguments main() looks in the directory indicated
 * by the "test.src" system property for files called management*ok.properties
 * or management*ko.properties. The *ok.properties files are assumed to be
 * valid Java M&M config files for which the bootstrap should succeed.
 * The *ko.properties files are assumed to be configurations for which the
 * bootstrap & connection test will fail.</p>
 *
 * <p>The rmi port number can be specified with the "rmi.port" system property.
 * If not, this test will use the first available port</p>
 *
 * <p>When called with some argument, the main() will interprete its args to
 * be Java M&M configuration file names. The filenames are expected to end
 * with ok.properties or ko.properties - and are interpreted as above.</p>
 *
 * <p>Note that a limitation of the RMI registry (bug 4267864) prevent
 * this test from succeeding if more than 1 configuration is used.
 * As long as 4267864 isn't fix, this test must be called as many times
 * as needed but with a single argument (no arguments, or several arguments
 * will fail).</p>
 *
 * <p>Debug traces are logged in "sun.management.test"</p>
 **/
public class RmiBootstrapTest {
    // the number of consecutive ports to test for availability
    private static int MAX_GET_FREE_PORT_TRIES = 10;
    static TestLogger log =
        new TestLogger("RmiBootstrapTest");

    /**
     * Default values for RMI configuration properties.
     **/
    public static interface DefaultValues {
        public static final String PORT="0";
        public static final String CONFIG_FILE_NAME="management.properties";
        public static final String USE_SSL="true";
        public static final String USE_AUTHENTICATION="true";
        public static final String PASSWORD_FILE_NAME="jmxremote.password";
        public static final String ACCESS_FILE_NAME="jmxremote.access";
        public static final String KEYSTORE="keystore";
        public static final String KEYSTORE_PASSWD="password";
        public static final String TRUSTSTORE="truststore";
        public static final String TRUSTSTORE_PASSWD="trustword";
        public static final String SSL_NEED_CLIENT_AUTH="false";
    }

    /**
     * Names of RMI configuration properties.
     **/
    public static interface PropertyNames {
        public static final String PORT=
            "com.sun.management.jmxremote.port";
        public static final String CONFIG_FILE_NAME=
            "com.sun.management.config.file";
        public static final String USE_SSL=
            "com.sun.management.jmxremote.ssl";
        public static final String USE_AUTHENTICATION=
            "com.sun.management.jmxremote.authenticate";
        public static final String PASSWORD_FILE_NAME=
            "com.sun.management.jmxremote.password.file";
        public static final String ACCESS_FILE_NAME=
            "com.sun.management.jmxremote.access.file";
        public static final String INSTRUMENT_ALL=
            "com.sun.management.instrumentall";
        public static final String CREDENTIALS =
            "jmx.remote.credentials";
        public static final String KEYSTORE=
            "javax.net.ssl.keyStore";
        public static final String KEYSTORE_PASSWD=
            "javax.net.ssl.keyStorePassword";
        public static final String TRUSTSTORE=
            "javax.net.ssl.trustStore";
        public static final String TRUSTSTORE_PASSWD=
            "javax.net.ssl.trustStorePassword";
        public static final String SSL_ENABLED_CIPHER_SUITES =
            "com.sun.management.jmxremote.ssl.enabled.cipher.suites";
        public static final String SSL_ENABLED_PROTOCOLS =
            "com.sun.management.jmxremote.ssl.enabled.protocols";
        public static final String SSL_NEED_CLIENT_AUTH =
            "com.sun.management.jmxremote.ssl.need.client.auth";
        public static final String SSL_CLIENT_ENABLED_CIPHER_SUITES =
            "javax.rmi.ssl.client.enabledCipherSuites";
    }

    /**
     * A filter to find all filenames who match <prefix>*<suffix>.
     * Note that <prefix> and <suffix> can overlap.
     **/
    private static class ConfigFilenameFilter implements FilenameFilter {
        final String suffix;
        final String prefix;
        ConfigFilenameFilter(String prefix, String suffix) {
            this.suffix=suffix;
            this.prefix=prefix;
        }
        public boolean accept(File dir, String name) {
            return (name.startsWith(prefix) && name.endsWith(suffix));
        }
    }

    /**
     * Get all "management*ok.properties" files in the directory
     * indicated by the "test.src" management property.
     **/
    private static File[] findConfigurationFilesOk() {
        final String testSrc = System.getProperty("test.src");
        final File dir = new File(testSrc);
        final FilenameFilter filter =
            new ConfigFilenameFilter("management_test","ok.properties");
        return dir.listFiles(filter);
    }

    /**
     * Get all "management*ko.properties" files in the directory
     * indicated by the "test.src" management property.
     **/
    private static File[] findConfigurationFilesKo() {
        final String testSrc = System.getProperty("test.src");
        final File dir = new File(testSrc);
        final FilenameFilter filter =
            new ConfigFilenameFilter("management_test","ko.properties");
        return dir.listFiles(filter);
    }

    /**
     * List all MBeans and their attributes. Used to test communication
     * with the Java M&M MBean Server.
     * @return the number of queried MBeans.
     */
    public static int listMBeans(MBeanServerConnection server)
        throws IOException {
        return listMBeans(server,null,null);
    }

    /**
     * List all matching MBeans and their attributes.
     * Used to test communication with the Java M&M MBean Server.
     * @return the number of matching MBeans.
     */
    public static int listMBeans(MBeanServerConnection server,
                                  ObjectName pattern, QueryExp query)
        throws IOException {

        final Set names = server.queryNames(pattern,query);
        for (final Iterator i=names.iterator(); i.hasNext(); ) {
            ObjectName name = (ObjectName)i.next();
            log.trace("listMBeans","Got MBean: "+name);
            try {
                MBeanInfo info =
                    server.getMBeanInfo((ObjectName)name);
                MBeanAttributeInfo[] attrs = info.getAttributes();
                if (attrs == null) continue;
                for (int j=0; j<attrs.length; j++) {
                    if (attrs[j].isReadable()) {
                        try {
                            Object o =
                                server.getAttribute(name,attrs[j].getName());
                            if (log.isDebugOn())
                                log.debug("listMBeans","\t\t" +
                                          attrs[j].getName() +
                                          " = "+o);
                        } catch (Exception x) {
                            log.trace("listMBeans","JmxClient failed to get " +
                                      attrs[j].getName() + ": " + x);
                            final IOException io =
                                new IOException("JmxClient failed to get " +
                                                attrs[j].getName());
                            io.initCause(x);
                            throw io;
                        }
                    }
                }
            } catch (Exception x) {
               log.trace("listMBeans",
                         "JmxClient failed to get MBeanInfo: "  + x);
                final IOException io =
                    new IOException("JmxClient failed to get MBeanInfo: "+x);
                io.initCause(x);
                throw io;
            }
        }
        return names.size();
    }

    /**
     * Compute the full path name for a default file.
     * @param basename basename (with extension) of the default file.
     * @return ${JRE}/conf/management/${basename}
     **/
    private static String getDefaultFileName(String basename) {
        final String fileSeparator = File.separator;
        final StringBuffer defaultFileName =
            new StringBuffer(System.getProperty("java.home")).
            append(fileSeparator).append("conf").append(fileSeparator).
            append("management").append(fileSeparator).
            append(basename);
        return defaultFileName.toString();
    }

    /**
     * Compute the full path name for a default file.
     * @param basename basename (with extension) of the default file.
     * @return ${JRE}/conf/management/${basename}
     **/
    private static String getDefaultStoreName(String basename) {
        final String fileSeparator = File.separator;
        final StringBuffer defaultFileName =
            new StringBuffer(System.getProperty("test.src")).
            append(fileSeparator).append("ssl").append(fileSeparator).
            append(basename);
        return defaultFileName.toString();
    }


    /**
     * Parses the password file to read the credentials.
     * Returns an ArrayList of arrays of 2 string:
     * {<subject>, <password>}.
     * If the password file does not exists, return an empty list.
     * (File not found = empty file).
     **/
    private ArrayList readCredentials(String passwordFileName)
        throws IOException {
        final Properties pws = new Properties();
        final ArrayList  result = new ArrayList();
        final File f = new File(passwordFileName);
        if (!f.exists()) return result;
        FileInputStream fin = new FileInputStream(passwordFileName);
        try {pws.load(fin);}finally{fin.close();}
        for (Enumeration en=pws.propertyNames();en.hasMoreElements();) {
            final String[] cred = new String[2];
            cred[0]=(String)en.nextElement();
            cred[1]=pws.getProperty(cred[0]);
            result.add(cred);
        }
        return result;
    }


    /**
     * Connect with the given url, using all given credentials in turn.
     * A null entry in the useCredentials arrays indicate a connection
     * where no credentials are used.
     * @param url JMXServiceURL of the server.
     * @param useCredentials An array of credentials (a credential
     *        is a two String array, so this is an array of arrays
     *        of strings:
     *           useCredentials[i][0]=subject
     *           useCredentials[i][1]=password
     *        if useCredentials[i] == null means no credentials.
     * @param expectConnectOk true if connection is expected to succeed
     *    Note: if expectConnectOk=false and the test fails to connect
     *          the number of failure is not incremented. Conversely,
     *          if expectConnectOk=false and the test does not fail to
     *          connect the number of failure is incremented.
     * @param expectReadOk true if communication (listMBeans) is expected
     *        to succeed.
     *    Note: if expectReadOk=false and the test fails to read MBeans
     *          the number of failure is not incremented. Conversely,
     *          if expectReadOk=false and the test does not fail to
     *          read MBeans the number of failure is incremented.
     * @return number of failure.
     **/
    public int connectAndRead(JMXServiceURL url,
                              Object[] useCredentials,
                              boolean  expectConnectOk,
                              boolean  expectReadOk)
        throws IOException {

        int errorCount = 0;

        for (int i=0 ; i<useCredentials.length ; i++) {
            final Map m = new HashMap();
            final String[] credentials = (String[])useCredentials[i];
            final String   crinfo;
            if (credentials != null) {
                crinfo = "{"+credentials[0] + ", " + credentials[1] + "}";
                m.put(PropertyNames.CREDENTIALS,credentials);
            } else {
                crinfo="no credentials";
            }
            log.trace("testCommunication","using credentials: " + crinfo);

            final JMXConnector c;
            try {
                c = JMXConnectorFactory.connect(url,m);
            } catch (IOException x ) {
                if (expectConnectOk) {
                    final String err = "Connection failed for " + crinfo +
                        ": " + x;
                    System.out.println(err);
                    log.trace("testCommunication",err);
                    log.debug("testCommunication",x);
                    errorCount++;
                    continue;
                } else {
                    System.out.println("Connection failed as expected for " +
                                       crinfo + ": " + x);
                    continue;
                }
            } catch (RuntimeException x ) {
                if (expectConnectOk) {
                    final String err = "Connection failed for " + crinfo +
                        ": " + x;
                    System.out.println(err);
                    log.trace("testCommunication",err);
                    log.debug("testCommunication",x);
                    errorCount++;
                    continue;
                } else {
                    System.out.println("Connection failed as expected for " +
                                       crinfo + ": " + x);
                    continue;
                }
            }
            try {
                MBeanServerConnection conn =
                    c.getMBeanServerConnection();
                if (log.isDebugOn()) {
                    log.debug("testCommunication","Connection is:" + conn);
                    log.debug("testCommunication","Server domain is: " +
                              conn.getDefaultDomain());
                }
                final ObjectName pattern =
                    new ObjectName("java.lang:type=Memory,*");
                final int count = listMBeans(conn,pattern,null);
                if (count == 0)
                    throw new Exception("Expected at least one matching "+
                                        "MBean for "+pattern);
                if (expectReadOk) {
                    System.out.println("Communication succeeded " +
                                       "as expected for "+
                                       crinfo + ": found " + count
                                       + ((count<2)?"MBean":"MBeans"));
                } else {
                    final String err = "Expected failure didn't occur for " +
                        crinfo;
                    System.out.println(err);
                    errorCount++;
                }
            } catch (IOException x ) {
                if (expectReadOk) {
                    final String err = "Communication failed with " + crinfo +
                        ": " + x;
                    System.out.println(err);
                    log.trace("testCommunication",err);
                    log.debug("testCommunication",x);
                    errorCount++;
                    continue;
                } else {
                    System.out.println("Communication failed as expected for "+
                                       crinfo + ": " + x);
                    continue;
                }
            } catch (RuntimeException x ) {
                if (expectReadOk) {
                    final String err = "Communication failed with " + crinfo +
                        ": " + x;
                    System.out.println(err);
                    log.trace("testCommunication",err);
                    log.debug("testCommunication",x);
                    errorCount++;
                    continue;
                } else {
                    System.out.println("Communication failed as expected for "+
                                       crinfo + ": " + x);
                }
            } catch (Exception x) {
                final String err = "Failed to read MBeans with " + crinfo +
                    ": " + x;
                System.out.println(err);
                log.trace("testCommunication",err);
                log.debug("testCommunication",x);
                errorCount++;
                continue;
            } finally {
                c.close();
            }
        }
        return errorCount;
    }


    private void setSslProperties(String clientEnabledCipherSuites) {
        final String defaultKeyStore =
            getDefaultStoreName(DefaultValues.KEYSTORE);
        final String defaultTrustStore =
            getDefaultStoreName(DefaultValues.TRUSTSTORE);

        final String keyStore =
            System.getProperty(PropertyNames.KEYSTORE, defaultKeyStore);
        System.setProperty(PropertyNames.KEYSTORE,keyStore);
        log.trace("setSslProperties",PropertyNames.KEYSTORE+"="+keyStore);

        final String password =
            System.getProperty(PropertyNames.KEYSTORE_PASSWD,
                               DefaultValues.KEYSTORE_PASSWD);
        System.setProperty(PropertyNames.KEYSTORE_PASSWD,password);
        log.trace("setSslProperties",
                  PropertyNames.KEYSTORE_PASSWD+"="+password);

        final String trustStore =
            System.getProperty(PropertyNames.TRUSTSTORE,
                               defaultTrustStore);
        System.setProperty(PropertyNames.TRUSTSTORE,trustStore);
        log.trace("setSslProperties",
                  PropertyNames.TRUSTSTORE+"="+trustStore);

        final String trustword =
            System.getProperty(PropertyNames.TRUSTSTORE_PASSWD,
                               DefaultValues.TRUSTSTORE_PASSWD);
        System.setProperty(PropertyNames.TRUSTSTORE_PASSWD,trustword);
        log.trace("setSslProperties",
                  PropertyNames.TRUSTSTORE_PASSWD+"="+trustword);

        if (clientEnabledCipherSuites != null) {
            System.setProperty("javax.rmi.ssl.client.enabledCipherSuites",
                    clientEnabledCipherSuites);
        } else {
            System.clearProperty("javax.rmi.ssl.client.enabledCipherSuites");
        }
    }

    private void checkSslConfiguration() {
        try {
            final String defaultConf =
                getDefaultFileName(DefaultValues.CONFIG_FILE_NAME);
            final String confname =
                System.getProperty(PropertyNames.CONFIG_FILE_NAME,defaultConf);

            final Properties props = new Properties();
            final File conf = new File(confname);
            if (conf.exists()) {
                FileInputStream fin = new FileInputStream(conf);
                try {props.load(fin);} finally {fin.close();}
            }

            // Do we use SSL?
            final String  useSslStr =
                props.getProperty(PropertyNames.USE_SSL,
                                  DefaultValues.USE_SSL);
            final boolean useSsl =
                Boolean.valueOf(useSslStr).booleanValue();

            log.debug("checkSslConfiguration",
                      PropertyNames.USE_SSL+"="+useSsl+
                      ": setting SSL");
            // Do we use SSL client authentication?
            final String  useSslClientAuthStr =
                props.getProperty(PropertyNames.SSL_NEED_CLIENT_AUTH,
                                  DefaultValues.SSL_NEED_CLIENT_AUTH);
            final boolean useSslClientAuth =
                Boolean.valueOf(useSslClientAuthStr).booleanValue();

            log.debug("checkSslConfiguration",
                      PropertyNames.SSL_NEED_CLIENT_AUTH+"="+useSslClientAuth);

            // Do we use customized SSL cipher suites?
            final String sslCipherSuites =
                props.getProperty(PropertyNames.SSL_ENABLED_CIPHER_SUITES);

            log.debug("checkSslConfiguration",
                      PropertyNames.SSL_ENABLED_CIPHER_SUITES + "=" +
                      sslCipherSuites);

            // Do we use customized SSL protocols?
            final String sslProtocols =
                props.getProperty(PropertyNames.SSL_ENABLED_PROTOCOLS);

            log.debug("checkSslConfiguration",
                      PropertyNames.SSL_ENABLED_PROTOCOLS + "=" +
                      sslProtocols);

            if (useSsl) {
                setSslProperties(props.getProperty(
                        PropertyNames.SSL_CLIENT_ENABLED_CIPHER_SUITES));
            }
        } catch (Exception x) {
            System.out.println("Failed to setup SSL configuration: " + x);
            log.debug("checkSslConfiguration",x);
        }
    }

    /**
     * Tests the server bootstraped at the given URL.
     * Uses the system properties to determine which config file is used.
     * Loads the config file to determine which password file is used.
     * Loads the password file to find out wich credentials to use.
     * Also checks that unregistered user/passwords are not allowed to
     * connect when a password file is used.
     *
     * This method calls connectAndRead().
     **/
    public void testCommunication(JMXServiceURL url)
        throws IOException {

        final String defaultConf =
            getDefaultFileName(DefaultValues.CONFIG_FILE_NAME);
        final String confname =
            System.getProperty(PropertyNames.CONFIG_FILE_NAME,defaultConf);

        final Properties props = new Properties();
        final File conf = new File(confname);
        if (conf.exists()) {
            FileInputStream fin = new FileInputStream(conf);
            try {props.load(fin);} finally {fin.close();}
        }

        // Do we use authentication?
        final String  useAuthenticationStr =
            props.getProperty(PropertyNames.USE_AUTHENTICATION,
                              DefaultValues.USE_AUTHENTICATION);
        final boolean useAuthentication =
            Boolean.valueOf(useAuthenticationStr).booleanValue();

        // Get Password File
        final String defaultPasswordFileName = Utils.convertPath(
            getDefaultFileName(DefaultValues.PASSWORD_FILE_NAME));
        final String passwordFileName = Utils.convertPath(
            props.getProperty(PropertyNames.PASSWORD_FILE_NAME,
                              defaultPasswordFileName));

        // Get Access File
        final String defaultAccessFileName = Utils.convertPath(
            getDefaultFileName(DefaultValues.ACCESS_FILE_NAME));
        final String accessFileName = Utils.convertPath(
            props.getProperty(PropertyNames.ACCESS_FILE_NAME,
                              defaultAccessFileName));

        if (useAuthentication) {
            System.out.println("PasswordFileName: " + passwordFileName);
            System.out.println("accessFileName: " + accessFileName);
        }

        final Object[] allCredentials;
        final Object[] noCredentials = { null };
        if (useAuthentication) {
            final ArrayList l = readCredentials(passwordFileName);
            if (l.size() == 0) allCredentials = null;
            else allCredentials = l.toArray();
        } else allCredentials = noCredentials;

        int errorCount = 0;
        if (allCredentials!=null) {
            // Tests that the registered user/passwords are allowed to
            // connect & read
            //
            errorCount += connectAndRead(url,allCredentials,true,true);
        } else {
            // Tests that no one is allowed
            // connect & read
            //
            final String[][] someCredentials = {
                null,
                { "modify", "R&D" },
                { "measure", "QED" }
            };
            errorCount += connectAndRead(url,someCredentials,false,false);
        }

        if (useAuthentication && allCredentials != noCredentials) {
            // Tests that the registered user/passwords are not allowed to
            // connect & read
            //
            final String[][] badCredentials = {
                { "bad.user", "R&D" },
                { "measure", "bad.password" }
            };
            errorCount += connectAndRead(url,badCredentials,false,false);
        }
        if (errorCount > 0) {
            final String err = "Test " + confname + " failed with " +
                errorCount + " error(s)";
            log.debug("testCommunication",err);
            throw new RuntimeException(err);
        }
    }


    /**
     * Test the configuration indicated by `file'.
     * Sets the appropriate System properties for config file and
     * port and then calls ConnectorBootstrap.initialize().
     * eventually cleans up by calling ConnectorBootstrap.terminate().
     * @return null if the test succeeds, an error message otherwise.
     **/
    private String testConfiguration(File file) throws IOException, InterruptedException {

        for (int i = 0; i < MAX_GET_FREE_PORT_TRIES; i++) {
            try {
                int port = jdk.test.lib.Utils.getFreePort();
                final String path;
                try {
                    path=(file==null)?null:file.getCanonicalPath();
                } catch(IOException x) {
                    final String err = "Failed to test configuration " + file +
                        ": " + x;
                    log.trace("testConfiguration",err);
                    log.debug("testConfiguration",x);
                    return err;
                }
                final String config = (path==null)?"Default config file":path;

                System.out.println("***");
                System.out.println("*** Testing configuration (port=" + port + "): "
                                   + path);
                System.out.println("***");

                System.setProperty("com.sun.management.jmxremote.port",
                                    Integer.toString(port));
                if (path != null)
                    System.setProperty("com.sun.management.config.file", path);
                else
                    System.getProperties().remove("com.sun.management.config.file");

                log.trace("testConfiguration","com.sun.management.jmxremote.port="+port);
                if (path != null && log.isDebugOn())
                    log.trace("testConfiguration",
                              "com.sun.management.config.file="+path);

                checkSslConfiguration();

                final JMXConnectorServer cs;
                try {
                    cs = ConnectorBootstrap.initialize();
                } catch (AgentConfigurationError x) {
                    if (x.getCause() instanceof ExportException) {
                        if (x.getCause().getCause() instanceof BindException) {
                            throw (BindException)x.getCause().getCause();
                        }
                    }
                    final String err = "Failed to initialize connector:" +
                        "\n\tcom.sun.management.jmxremote.port=" + port +
                        ((path!=null)?"\n\tcom.sun.management.config.file="+path:
                         "\n\t"+config) +
                        "\n\tError is: " + x;
                    log.trace("testConfiguration",err);
                    log.debug("testConfiguration",x);
                    return err;
                } catch (Exception x) {
                    log.debug("testConfiguration",x);
                    return x.toString();
                }

                try {
                    JMXServiceURL url =
                        new JMXServiceURL("rmi",null,0,"/jndi/rmi://localhost:"+
                                          port+"/jmxrmi");

                    try {
                        testCommunication(url);
                    } catch (Exception x) {
                        final String err = "Failed to connect to agent {url="+url+
                            "}: " + x;
                        log.trace("testConfiguration",err);
                        log.debug("testConfiguration",x);
                        return err;
                    }
                } catch (Exception x) {
                    final String err = "Failed to test configuration "+config+
                        ": "+x;
                    log.trace("testConfiguration",err);
                    log.debug("testConfiguration",x);
                    return err;
                } finally {
                    try {
                        cs.stop();
                    } catch (Exception x) {
                        final String err = "Failed to terminate: "+x;
                        log.trace("testConfiguration",err);
                        log.debug("testConfiguration",x);
                    }
                }
                System.out.println("Configuration " + config + " successfully tested");
                return null;
            } catch(BindException ex) {
            }
        }
        System.err.println("Cannot find a free port after " + MAX_GET_FREE_PORT_TRIES + " tries");
        return "Failed: cannot find a free port after " + MAX_GET_FREE_PORT_TRIES + " tries";
    }

    /**
     * Test a configuration file which should make the bootstrap fail.
     * The test is assumed to have succeeded if the bootstrap fails.
     * @return null if the test succeeds, an error message otherwise.
     **/
    private String testConfigurationKo(File conf) throws InterruptedException, IOException {
        String errStr = null;
        errStr = testConfiguration(conf);
        if (errStr == null) {
            return "Configuration " +
                conf + " should have failed!";
        }
        System.out.println("Configuration " +
                           conf + " failed as expected");
        log.debug("runko", "Error was: " + errStr);
        return null;
    }

    /**
     * Test a configuration file. Determines whether the bootstrap
     * should succeed or fail depending on the file name:
     *     *ok.properties: bootstrap should succeed.
     *     *ko.properties: bootstrap or connection should fail.
     * @return null if the test succeeds, an error message otherwise.
     **/
    private String testConfigurationFile(String fileName) throws InterruptedException, IOException {
        File file = new File(fileName);

        if (fileName.endsWith("ok.properties")) {
            String errStr = null;
            errStr = testConfiguration(file);
            return errStr;
        }
        if (fileName.endsWith("ko.properties")) {
            return testConfigurationKo(file);
        }
        return fileName +
            ": test file suffix must be one of [ko|ok].properties";
    }

    /**
     * Find all *ko.property files and test them.
     * (see findConfigurationFilesKo() and testConfigurationKo())
     * @throws RuntimeException if the test fails.
     **/
    public void runko() throws InterruptedException, IOException {
        final File[] conf = findConfigurationFilesKo();
        if ((conf == null)||(conf.length == 0))
            throw new RuntimeException("No configuration found");

        String errStr;
        for (int i=0;i<conf.length;i++) {
            errStr = testConfigurationKo(conf[i]);
            if (errStr != null) {
                throw new RuntimeException(errStr);
            }
        }

    }

    /**
     * Find all *ok.property files and test them.
     * (see findConfigurationFilesOk() and testConfiguration())
     * @throws RuntimeException if the test fails.
     **/
    public void runok() throws InterruptedException, IOException {
        final File[] conf = findConfigurationFilesOk();
        if ((conf == null)||(conf.length == 0))
            throw new RuntimeException("No configuration found");

        String errStr = null;
        for (int i=0;i<conf.length;i++) {
            errStr = testConfiguration(conf[i]);
            if (errStr != null) {
                throw new RuntimeException(errStr);
            }
        }

        // FIXME: No jmxremote.password is not installed in JRE by default.
        // - disable the following test case.
        //
        // Test default config
        //
        // errStr = testConfiguration(null,port+testPort++);
        // if (errStr != null) {
        //    throw new RuntimeException(errStr);
        // }
    }

    /**
     * Finds all configuration files (*ok.properties and *ko.properties)
     * and tests them.
     * (see runko() and runok()).
     * @throws RuntimeException if the test fails.
     **/
    public void run() throws InterruptedException, IOException {
        runok();
        runko();
    }

    /**
     * Tests the specified configuration files.
     * If args[] is not empty, each element in args[] is expected to be
     * a filename ending either by ok.properties or ko.properties.
     * Otherwise, the configuration files will be automatically determined
     * by looking at all *.properties files located in the directory
     * indicated by the System property "test.src".
     * @throws RuntimeException if the test fails.
     **/
    public void run(String args[]) throws InterruptedException, IOException {
        if (args.length == 0) {
            run() ; return;
        }
        for (int i=0; i<args.length; i++) {
            final String errStr =testConfigurationFile(args[i]);
            if (errStr != null) {
                throw new RuntimeException(errStr);
            }
        }
    }

    /**
     * Calls run(args[]).
     * exit(1) if the test fails.
     **/
    public static void main(String args[]) throws Exception {
        Security.setProperty("jdk.tls.disabledAlgorithms", "");

        try {
            MAX_GET_FREE_PORT_TRIES = Integer.parseInt(System.getProperty("test.getfreeport.max.tries", "10"));
        } catch (NumberFormatException ex) {
        }

        RmiBootstrapTest manager = new RmiBootstrapTest();
        try {
            manager.run(args);
        } catch (RuntimeException r) {
            System.out.println("Test Failed: "+ r.getMessage());
            System.exit(1);
        } catch (Throwable t) {
            System.out.println("Test Failed: "+ t);
            t.printStackTrace();
            System.exit(2);
        }
        System.out.println("**** Test  RmiBootstrap Passed ****");
    }

}
