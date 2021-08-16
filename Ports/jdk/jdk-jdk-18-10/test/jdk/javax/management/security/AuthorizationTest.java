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
 * @summary Checks various authentication behavior from remote jmx client
 * @author Olivier Lagneau
 * @modules java.management.rmi
 * @library /test/lib
 * @compile Simple.java
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Dusername=username1 -Dpassword=password1 AuthorizationTest -server -mapType x.access.file;x.password.file -populate -client -mapType credentials
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Dusername=username2 -Dpassword=password2 AuthorizationTest -server -mapType x.access.file;x.password.file -populate -client -mapType credentials -expectedCreateException -expectedSetException -expectedInvokeException
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD -Dusername=username6 -Dpassword=password6 AuthorizationTest -server -mapType x.access.file;x.password.file -populate -client -mapType credentials -expectedCreateException -expectedGetException -expectedSetException -expectedInvokeException
 * @run main/othervm/timeout=300/policy=java.policy.authorization -DDEBUG_STANDARD -Dusername=username1 -Dpassword=password1 AuthorizationTest -server -mapType x.password.file -populate -client -mapType credentials
 * @run main/othervm/timeout=300/policy=java.policy.authorization -DDEBUG_STANDARD -Dusername=username3 -Dpassword=password3 AuthorizationTest -server -mapType x.password.file -populate -client -mapType credentials -expectedGetException
 * @run main/othervm/timeout=300/policy=java.policy.authorization -DDEBUG_STANDARD -Dusername=username5 -Dpassword=password5 AuthorizationTest -server -mapType x.password.file -populate -client -mapType credentials -expectedCreateException -expectedGetException -expectedSetException -expectedInvokeException
 * @run main/othervm/timeout=300/policy=java.policy.authorization -DDEBUG_STANDARD -Dusername=username6 -Dpassword=password6 AuthorizationTest -server -mapType x.password.file -populate -client -mapType credentials -expectedCreateException -expectedGetException -expectedSetException -expectedInvokeException
 * @run main/othervm/timeout=300/policy=java.policy.authorization -DDEBUG_STANDARD -Dusername=username1 -Dpassword=password1 AuthorizationTest -server -mapType x.access.file;x.password.file -populate -client -mapType credentials
 * @run main/othervm/timeout=300/policy=java.policy.authorization -DDEBUG_STANDARD -Dusername=username2 -Dpassword=password2 AuthorizationTest -server -mapType x.access.file;x.password.file -populate -client -mapType credentials -expectedCreateException -expectedSetException -expectedInvokeException
 * @run main/othervm/timeout=300/policy=java.policy.authorization -DDEBUG_STANDARD -Dusername=username3 -Dpassword=password3 AuthorizationTest -server -mapType x.access.file;x.password.file -populate -client -mapType credentials -expectedCreateException -expectedGetException -expectedSetException -expectedInvokeException
 * @run main/othervm/timeout=300/policy=java.policy.authorization -DDEBUG_STANDARD -Dusername=username4 -Dpassword=password4 AuthorizationTest -server -mapType x.access.file;x.password.file -populate -client -mapType credentials -expectedGetException -expectedSetException
 * @run main/othervm/timeout=300/policy=java.policy.authorization -DDEBUG_STANDARD -Dusername=username5 -Dpassword=password5 AuthorizationTest -server -mapType x.access.file;x.password.file -populate -client -mapType credentials -expectedCreateException -expectedGetException -expectedSetException -expectedInvokeException
 */

import java.io.File;
import java.util.Map ;
import java.util.HashMap ;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.lang.management.ManagementFactory;

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

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.ProcessTools;

public class AuthorizationTest {

    static final String SERVER_CLASS_NAME = "AuthorizationTest";
    static final String CLIENT_CLASS_NAME = "AuthorizationTest$ClientSide";
    static final String CLIENT_CLASS_MAIN = CLIENT_CLASS_NAME;

    static final String USERNAME_PROPERTY = "username";
    static final String PASSWORD_PROPERTY = "password";

    private JMXConnectorServer cs;

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
        AuthorizationTest test = new AuthorizationTest();
        test.run(serverMap, clientParams);

    }

    /*
     * Create the MBeansServer side of the test and returns its address
     */
    private JMXServiceURL createServerSide(Map<String, Object> serverMap)
    throws Exception {
        final int NINETY_SECONDS = 90;

        System.out.println("AuthorizationTest::createServerSide: Start") ;

        MBeanServer mbs = MBeanServerFactory.newMBeanServer();
        JMXServiceURL url = new JMXServiceURL("rmi", null, 0);

        // Creates connection environment from server side params
        HashMap<String, Object> env = new HashMap<>();
        String value = null;

        if ((value = (String)serverMap.get("-mapType")) != null) {
            if (value.contains("x.access.file")) {
                String accessFileStr = System.getProperty("test.src") +
                    File.separator + "access.properties";
                env.put("jmx.remote.x.access.file", accessFileStr);
                System.out.println("Added " + accessFileStr + " file as jmx.remote.x.access.file");
            }
            if (value.contains("x.password.file")) {
                String passwordFileStr = System.getProperty("test.src") +
                    File.separator + "password.properties";
                env.put("jmx.remote.x.password.file", passwordFileStr);
                System.out.println("Added " + passwordFileStr + " file as jmx.remote.x.password.file");
            }
        }

        if (serverMap.containsKey("-populate")) {
            String populateClassName = "Simple";
            ObjectName on =
                new ObjectName("defaultDomain:class=Simple");

            Utils.debug(Utils.DEBUG_STANDARD, "create and register Simple MBean") ;
            mbs.createMBean(populateClassName, on);
        }

        cs = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);
        cs.start();

        Utils.waitReady(cs, NINETY_SECONDS);

        JMXServiceURL addr = cs.getAddress();

        System.out.println("AuthorizationTest::createServerSide: Done.") ;

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
        List<String> opts = new ArrayList<>();
        opts.add(JDKToolFinder.getJDKTool("java"));
        opts.addAll(Arrays.asList(jdk.test.lib.Utils.getTestJavaOpts()));

        String usernameValue = System.getProperty(USERNAME_PROPERTY);
        if (usernameValue != null) {
            opts.add("-D" + USERNAME_PROPERTY + "=" + usernameValue);
        }
        String passwordValue = System.getProperty(PASSWORD_PROPERTY);
        if (passwordValue != null) {
            opts.add("-D" + PASSWORD_PROPERTY + "=" + passwordValue);
        }

        opts.add("-cp");
        opts.add(System.getProperty("test.class.path", "test.class.path"));
        opts.add(CLIENT_CLASS_MAIN);
        opts.addAll(Arrays.asList(args));
        return opts;
    }

    /**
     * Runs AuthorizationTest$ClientSide with the passed options and redirects
     * subprocess standard I/O to the current (parent) process. This provides a
     * trace of what happens in the subprocess while it is runnning (and before
     * it terminates).
     *
     * @param serviceUrlStr string representing the JMX service Url to connect to.
     */
    private int runClientSide(String args[], String serviceUrlStr) throws Exception {

        // Building command-line
        List<String> opts = buildCommandLine(args);
        opts.add("-serviceUrl");
        opts.add(serviceUrlStr);

        // Launch separate JVM subprocess
        int exitCode = 0;
        String[] optsArray = opts.toArray(new String[0]);
        ProcessBuilder pb = new ProcessBuilder(optsArray);
        Process p = ProcessTools.startProcess("AuthorizationTest$ClientSide", pb);

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
            return exitCode;
        }

     }

    public void run(Map<String, Object> serverArgs, String clientArgs[]) {

        System.out.println("AuthorizationTest::run: Start") ;
        int errorCount = 0;

        try {
            // Initialise the server side
            JMXServiceURL urlToUse = createServerSide(serverArgs);

            // Run client side
            errorCount = runClientSide(clientArgs, urlToUse.toString());

            if ( errorCount == 0 ) {
                System.out.println("AuthorizationTest::run: Done without any error") ;
            } else {
                System.out.println("AuthorizationTest::run: Done with "
                        + errorCount
                        + " error(s)") ;
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

            int errorCount = 0 ;

            try {
                boolean expectedCreateException =
                        (args.containsKey("-expectedCreateException")) ? true : false ;
                boolean expectedGetException =
                        (args.containsKey("-expectedGetException")) ? true : false ;
                boolean expectedSetException =
                        (args.containsKey("-expectedSetException")) ? true : false ;
                boolean expectedInvokeException =
                        (args.containsKey("-expectedInvokeException")) ? true : false ;
                // JSR262 (see bug 6440374)
                // There is no special JSR262 protocol operation for connect.
                // The first request sent initiate the connection.
                // In the JSR262 current implementation, getDefaultDomain is sent to
                // the server in order to get the server part of the connection ID.
                // => the connection may fail if no access permission on get requests.
                boolean expectedConnectException =
                        (args.containsKey("-expectedConnectException")) ? true : false ;
                // Before connection,
                // remove the element of the Map with null values (not supported by RMI)
                // See bug 4982668
                args.remove("-expectedCreateException");
                args.remove("-expectedGetException");
                args.remove("-expectedSetException");
                args.remove("-expectedInvokeException");
                args.remove("-expectedConnectException");


                // Here do connect to the JMX Server
                String username = System.getProperty("username");
                Utils.debug(Utils.DEBUG_STANDARD,
                    "ClientSide::run: CONNECT on behalf of \"" + username + "\"");
                doConnect(args, expectedConnectException);

                // If the connection did not fail, perform some requests.
                // At this stage the mbeanserver connection is up and running
                if (mbsc != null) {
                    ObjectName on = new ObjectName("defaultDomain:class=Simple");

                    // Create request
                    Utils.debug(Utils.DEBUG_STANDARD,
                        "ClientSide::run: CREATE on behalf of \"" +
                        username + "\"");
                    errorCount += doCreateRequest(mbsc,
                        new ObjectName("defaultDomain:class=Simple,user=" + username),
                        expectedCreateException);

                    // Get request
                    Utils.debug(Utils.DEBUG_STANDARD,
                        "ClientSide::run: GET on behalf of \"" +
                        username + "\"");
                    errorCount += doGetRequest(mbsc, on, expectedGetException);

                    // Set request
                    Utils.debug(Utils.DEBUG_STANDARD,
                        "ClientSide::run: SET on behalf of \"" +
                        username + "\"");
                    errorCount += doSetRequest(mbsc, on, expectedSetException);

                    // Invoke request
                    Utils.debug(Utils.DEBUG_STANDARD,
                        "ClientSide::run: INVOKE on behalf of \"" +
                        username + "\"");
                    errorCount += doInvokeRequest(mbsc, on, expectedInvokeException);
                }

            } catch(Exception e) {
                Utils.printThrowable(e, true) ;
                errorCount++;
            } finally {
                // Terminate the JMX Client
                try {
                    cc.close();
                } catch (Exception e) {
                    Utils.printThrowable(e, true) ;
                    errorCount++;
                }
            }

            System.out.println("ClientSide::run: Done") ;

            // Handle result
            if (errorCount == 0) {
                System.out.println("ClientSide::run: (OK) authorization test succeeded.");
            } else {
                String message = "AuthorizationTest$ClientSide::run: (ERROR) " +
                        " authorization test failed with " +
                        errorCount + " error(s)";
                System.out.println(message);
                throw new RuntimeException(message);
            }
        }

        protected void doConnect(Map<String, Object> args,
                                 boolean expectedException) {

            String msgTag = "ClientSide::doConnect";
            boolean throwRuntimeException = false;
            String message = "";

            try {
                Utils.debug(Utils.DEBUG_STANDARD,
                    "ClientSide::doConnect: Connect the client");

                // Collect connection environment
                HashMap<String, Object> env = new HashMap<>();

                Object value = args.get("-mapType");
                if (value != null) {
                    String username = System.getProperty("username");
                    String password = System.getProperty("password");
                    Utils.debug(Utils.DEBUG_STANDARD,
                        msgTag + "add \"jmx.remote.credentials\" = \"" +
                        username + "\", \"" + password + "\"");
                    env.put("jmx.remote.credentials",
                        new String[] { username , password });
                }

                // Get a connection to remote mbean server
                JMXServiceURL addr = new JMXServiceURL((String)args.get("-serviceUrl"));
                cc = JMXConnectorFactory.connect(addr,env);
                mbsc = cc.getMBeanServerConnection();

                if (expectedException) {
                    message = "ClientSide::doConnect: (ERROR) " +
                        "Connect did not fail with expected SecurityException";
                    System.out.println(message);
                    throwRuntimeException = true;
                } else {
                    System.out.println("ClientSide::doConnect: (OK) Connect succeed");
                }
            } catch(Exception e) {
                Utils.printThrowable(e, true);
                if (expectedException) {
                    if (e instanceof java.lang.SecurityException) {
                        System.out.println("ClientSide::doConnect: (OK) " +
                            "Connect failed with expected SecurityException");
                    } else {
                        message = "ClientSide::doConnect: (ERROR) " +
                            "Create failed with " + e.getClass() +
                            " instead of expected SecurityException";
                        System.out.println(message);
                        throwRuntimeException = true;
                    }
                } else {
                    message = "ClientSide::doConnect: (ERROR) " +
                        "Connect failed";
                    System.out.println(message);
                    throwRuntimeException = true;
                }
            }

            // If the connection failed, or if the connection succeeded but should not,
            // no need to go further => throw RuntimeException and exit the test
            if (throwRuntimeException) {
                throw new RuntimeException(message);
            }
        }

        protected int doCreateRequest(MBeanServerConnection mbsc,
                                      ObjectName on,
                                      boolean expectedException) {
            int errorCount = 0;

            try {
                Utils.debug(Utils.DEBUG_STANDARD,
                    "ClientSide::doCreateRequest: Create and register the MBean") ;

                mbsc.createMBean("Simple", on) ;

                if (expectedException) {
                    System.out.println("ClientSide::doCreateRequest: " +
                        "(ERROR) Create did not fail with expected SecurityException");
                    errorCount++;
                } else {
                    System.out.println("ClientSide::doCreateRequest: (OK) Create succeed") ;
                }
            } catch(Exception e) {
                Utils.printThrowable(e, true) ;
                if (expectedException) {
                    if (e instanceof java.lang.SecurityException) {
                        System.out.println("ClientSide::doCreateRequest: " +
                            "(OK) Create failed with expected SecurityException") ;
                    } else {
                        System.out.println("ClientSide::doCreateRequest: " +
                            "(ERROR) Create failed with " +
                            e.getClass() + " instead of expected SecurityException");
                        errorCount++;
                    }
                } else {
                    System.out.println("ClientSide::doCreateRequest: " +
                        "(ERROR) Create failed");
                    errorCount++;
                }
            }
            return errorCount;
        }

        protected int doGetRequest(MBeanServerConnection mbsc,
                                   ObjectName on,
                                   boolean expectedException) {
            int errorCount = 0;

            try {
                Utils.debug(Utils.DEBUG_STANDARD,
                    "ClientSide::doGetRequest: Get attributes of the MBean") ;

                mbsc.getAttribute(on, "Attribute");

                if (expectedException) {
                    System.out.println("ClientSide::doGetRequest: " +
                        "(ERROR) Get did not fail with expected SecurityException");
                    errorCount++;
                } else {
                    System.out.println("ClientSide::doGetRequest: (OK) Get succeed") ;
                }
            } catch(Exception e) {
                Utils.printThrowable(e, true) ;
                if (expectedException) {
                    if (e instanceof java.lang.SecurityException) {
                        System.out.println("ClientSide::doGetRequest: " +
                            "(OK) Get failed with expected SecurityException") ;
                    } else {
                        System.out.println("ClientSide::doGetRequest: " +
                            "(ERROR) Get failed with " +
                            e.getClass() + " instead of expected SecurityException");
                        errorCount++;
                    }
                } else {
                    System.out.println("ClientSide::doGetRequest: (ERROR) Get failed");
                    errorCount++;
                }
            }

            return errorCount;
        }

        protected int doSetRequest(MBeanServerConnection mbsc,
                                   ObjectName on,
                                   boolean expectedException) {
            int errorCount = 0;

            try {
                Utils.debug(Utils.DEBUG_STANDARD,
                    "ClientSide::doSetRequest: Set attributes of the MBean") ;

                Attribute attribute = new Attribute("Attribute", "My value") ;
                mbsc.setAttribute(on, attribute) ;

                if (expectedException) {
                    System.out.println("ClientSide::doSetRequest: " +
                        "(ERROR) Set did not fail with expected SecurityException");
                    errorCount++;
                } else {
                    System.out.println("ClientSide::doSetRequest: (OK) Set succeed") ;
                }
            } catch(Exception e) {
                Utils.printThrowable(e, true) ;
                if (expectedException) {
                    if (e instanceof java.lang.SecurityException) {
                        System.out.println("ClientSide::doSetRequest: " +
                            "(OK) Set failed with expected SecurityException") ;
                    } else {
                        System.out.println("ClientSide::doSetRequest: " +
                            "(ERROR) Set failed with " +
                            e.getClass() + " instead of expected SecurityException");
                        errorCount++;
                    }
                } else {
                    System.out.println("ClientSide::doSetRequest: (ERROR) Set failed");
                    errorCount++;
                }
            }
            return errorCount;
        }

        protected int doInvokeRequest(MBeanServerConnection mbsc,
                                      ObjectName on,
                                      boolean expectedException) {
            int errorCount = 0;

            try {
                Utils.debug(Utils.DEBUG_STANDARD,
                    "ClientSide::doInvokeRequest: Invoke operations on the MBean") ;

                mbsc.invoke(on, "operation", null, null) ;

                if (expectedException) {
                    System.out.println("ClientSide::doInvokeRequest: " +
                        "(ERROR) Invoke did not fail with expected SecurityException");
                    errorCount++;
                } else {
                    System.out.println("ClientSide::doInvokeRequest: (OK) Invoke succeed") ;
                }
            } catch(Exception e) {
                Utils.printThrowable(e, true) ;
                if (expectedException) {
                    if (e instanceof java.lang.SecurityException) {
                        System.out.println("ClientSide::doInvokeRequest: " +
                            "(OK) Invoke failed with expected SecurityException") ;
                    } else {
                        System.out.println("ClientSide::doInvokeRequest: " +
                            " (ERROR) Invoke failed with " +
                            e.getClass() + " instead of expected SecurityException");
                        errorCount++;
                    }
                } else {
                    System.out.println("ClientSide::doInvokeRequest: " +
                        "(ERROR) Invoke failed");
                    errorCount++;
                }
            }
            return errorCount;
        }

    }
}
