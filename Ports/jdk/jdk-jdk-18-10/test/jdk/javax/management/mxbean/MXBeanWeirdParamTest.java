/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Checks that a serialized instance is not transmitted from an MXBean.
 * All the communication should be done via Open Types
 * @author Olivier Lagneau
 * @modules java.management.rmi
 * @library /test/lib
 * @compile Basic.java
 * @run main/othervm/timeout=300 -DDEBUG_STANDARD MXBeanWeirdParamTest
 */

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.lang.Process;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.ProcessTools;

public class MXBeanWeirdParamTest {

    private static String BASIC_MXBEAN_CLASS_NAME = "Basic";

    private static final String CLIENT_CLASS_MAIN =
        "MXBeanWeirdParamTest$ClientSide";

    private JMXConnectorServer cs;

    /*
     * First Debug properties and arguments are collect in expected
     * map  (argName, value) format, then calls original test's run method.
     */
    public static void main(String args[]) throws Exception {

        System.out.println("=================================================");

        // Parses parameters
        Utils.parseDebugProperties();
        Map<String, Object> map = Utils.parseParameters(args) ;

        // Run test
        MXBeanWeirdParamTest test = new MXBeanWeirdParamTest();
        test.run(map);

    }

    /*
     * Create the MBeansServe side of the test and returns its address
     */
    private JMXServiceURL createServerSide() throws Exception {
        final int NINETY_SECONDS = 90;

        // We will use the platform mbean server
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        JMXServiceURL url = new JMXServiceURL("rmi", null, 0);
        cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
        cs.start();

        Utils.waitReady(cs, NINETY_SECONDS);

        JMXServiceURL addr = cs.getAddress();
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
    private List<String> buildCommandLine() {
        List<String> opts = new ArrayList<>();
        opts.add(JDKToolFinder.getJDKTool("java"));
        opts.addAll(Arrays.asList(jdk.test.lib.Utils.getTestJavaOpts()));
        // We need to set WEIRD_PARAM propertty on the client-side
        opts.add("-DWEIRD_PARAM");
        opts.add("-cp");
        opts.add(System.getProperty("test.class.path", "test.class.path"));
        opts.add(CLIENT_CLASS_MAIN);

        return opts;
    }

    /**
     * Runs MXBeanWeirdParamTest$ClientSide with the passed options and redirects
     * subprocess standard I/O to the current (parent) process. This provides a
     * trace of what happens in the subprocess while it is runnning (and before
     * it terminates).
     *
     * @param serviceUrlStr string representing the JMX service Url to connect to.
     */
    private int runClientSide(String serviceUrlStr) throws Exception {

        // Building command-line
        List<String> opts = buildCommandLine();
        opts.add(serviceUrlStr);

        // Launch separate JVM subprocess
        int exitCode = 0;
        String[] optsArray = opts.toArray(new String[0]);
        ProcessBuilder pb = new ProcessBuilder(optsArray);
        Process p = ProcessTools.startProcess("MXBeanWeirdParamTest$ClientSide", pb);

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
            return exitCode;
        }

     }

    public void run(Map<String, Object> args) throws Exception {

        System.out.println("MXBeanWeirdParamTest::run: Start") ;
        int errorCount = 0;

        try {
            // Initialise the server side
            JMXServiceURL urlToUse = createServerSide();

            // Run client side
            errorCount = runClientSide(urlToUse.toString());

            if ( errorCount == 0 ) {
                System.out.println("MXBeanWeirdParamTest::run: Done without any error") ;
            } else {
                System.out.println("MXBeanWeirdParamTest::run: Done with "
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
        public static void main(String args[]) throws Exception {

            int errorCount = 0 ;
            String msgTag = "ClientSide::main: ";

            try {

                // Get a connection to remote mbean server
                JMXServiceURL addr = new JMXServiceURL(args[0]);
                JMXConnector cc = JMXConnectorFactory.connect(addr);
                MBeanServerConnection mbsc = cc.getMBeanServerConnection();

                // ----
                System.out.println(msgTag + "Create and register the MBean");
                ObjectName objName = new ObjectName("sqe:type=Basic,protocol=rmi") ;
                mbsc.createMBean(BASIC_MXBEAN_CLASS_NAME, objName);
                System.out.println(msgTag +"---- OK\n") ;

                // ----
                System.out.println(msgTag +"Get attribute SqeParameterAtt on our MXBean");
                Object result = mbsc.getAttribute(objName, "SqeParameterAtt");
                System.out.println(msgTag +"(OK) Got result of class "
                        + result.getClass().getName());
                System.out.println(msgTag +"Received CompositeData is " + result);
                System.out.println(msgTag +"---- OK\n") ;

                // ----
                // We use the value returned by getAttribute to perform the invoke.
                System.out.println(msgTag +"Call operation doWeird on our MXBean [1]");
                mbsc.invoke(objName, "doWeird",
                        new Object[]{result},
                        new String[]{"javax.management.openmbean.CompositeData"});
                System.out.println(msgTag +"---- OK\n") ;

                // ----
                // We build the CompositeData ourselves that time.
                System.out.println(msgTag +"Call operation doWeird on our MXBean [2]");
                String typeName = "SqeParameter";
                String[] itemNames = new String[] {"glop"};
                OpenType<?>[] openTypes = new OpenType<?>[] {SimpleType.STRING};
                CompositeType rowType = new CompositeType(typeName, typeName,
                        itemNames, itemNames, openTypes);
                Object[] itemValues = {"HECTOR"};
                CompositeData data =
                        new CompositeDataSupport(rowType, itemNames, itemValues);
                TabularType tabType = new TabularType(typeName, typeName,
                        rowType, new String[]{"glop"});
                TabularDataSupport tds = new TabularDataSupport(tabType);
                tds.put(data);
                System.out.println(msgTag +"Source CompositeData is " + data);
                mbsc.invoke(objName, "doWeird",
                        new Object[]{data},
                        new String[]{"javax.management.openmbean.CompositeData"});
                System.out.println(msgTag +"---- OK\n") ;

                // ----
                System.out.println(msgTag +"Unregister the MBean");
                mbsc.unregisterMBean(objName);
                System.out.println(msgTag +"---- OK\n") ;

                // Terminate the JMX Client
                cc.close();

            } catch(Exception e) {
                Utils.printThrowable(e, true) ;
                errorCount++;
                throw new RuntimeException(e);
            } finally {
                System.exit(errorCount);
            }
        }
    }
}
