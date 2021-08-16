/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase nsk/jdi/PlugConnectors/LaunchConnector/plugLaunchConnect001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     nsk/jdi/PlugConnectors/LaunchConnector/plugLaunchConnect001 test:
 *     This test is the test for the mechanism for creating pluggable Connectors
 *     on base of classes which implement the Connector interfaces
 *     (AttachingConnector, ListeningConnector, or LaunchingConnector).
 *     The test checks up that at start-up time when
 *     Bootstrap.virtualMachineManager() is invoked the pluggable
 *     connector named "PlugLaunchConnector001_Name" is created on base
 *     of PlugLaunchConnector001 class which implements
 *     com.sun.jdi.connect.LaunchingConnector interface.
 *     This pluggable connector has to be contained in lists returned
 *     by VirtualMachineManager.launchingConnectors() and
 *     by VirtualMachineManager.allConnectors() methods.
 *     This pluggable connector has to have:
 *      Connector.description() = "PlugLaunchConnector001_Description";
 *      Connector.transport().name() = "PlugLaunchConnector001_Transport";
 *      Connector.defaultArguments() = <empty Map>, i.e without arguments;
 * COMMENTS:
 *     Fixed bug 6426609: nsk/share/jdi/plug_connectors_jar_file.pl should use
 *                        "system" instead of back-quotes
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.PlugConnectors.LaunchConnector.plugLaunchConnect001.plugLaunchConnect001
 *
 * @comment build connectors.jar to jars
 * @build nsk.share.jdi.PlugConnectors
 * @run driver nsk.jdi.ConnectorsJarBuilder
 *
 * @run main/othervm
 *      -cp jars${file.separator}connectors.jar${path.separator}${test.class.path}${path.separator}${java.class.path}
 *      nsk.jdi.PlugConnectors.LaunchConnector.plugLaunchConnect001.plugLaunchConnect001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 */

package nsk.jdi.PlugConnectors.LaunchConnector.plugLaunchConnect001;

import nsk.jdi.PlugConnectors.LaunchConnector.plugLaunchConnect001.connectors.*;

import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.util.*;
import java.io.*;

/**
 * The test for the mechanism for creating pluggable Connectors        <BR>
 * on base of classes which implement the Connector interfaces         <BR>
 * (AttachingConnector, ListeningConnector, or LaunchingConnector).    <BR>
 *                                                                     <BR>
 * The test checks up that at start-up time when                       <BR>
 * Bootstrap.virtualMachineManager() is invoked the pluggable          <BR>
 * connector named "PlugLaunchConnector001_Name" is created on base    <BR>
 * of PlugLaunchConnector001 class which implements                    <BR>
 * com.sun.jdi.connect.LaunchingConnector interface.                   <BR>
 *                                                                     <BR>
 * This pluggable connector has to be contained in lists returned      <BR>
 * by VirtualMachineManager.launchingConnectors() and                  <BR>
 * by VirtualMachineManager.allConnectors() methods.                   <BR>
 *                                                                     <BR>
 * This pluggable connector has to have:                               <BR>
 *  Connector.description() = "PlugLaunchConnector001_Description";    <BR>
 *  Connector.transport().name() = "PlugLaunchConnector001_Transport"; <BR>
 *  Connector.defaultArguments() = <empty Map>, i.e without arguments; <BR>
 *                                                                     <BR>
 */

public class plugLaunchConnect001 {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "plugLaunchConnect001: ";
    static final String errorLogPrefix     = "                      ";
    static final String infoLogPrefixNead = "--> plugLaunchConnect001: ";
    static final String infoLogPrefix     = "-->                       ";

    static ArgumentHandler  argsHandler;
    static Log logHandler;

    private static void logOnVerbose(String message) {
        logHandler.display(message);
    }

    private static void logOnError(String message) {
        logHandler.complain(message);
    }

    private static void logAlways(String message) {
        logHandler.println(message);
    }

    public static void main (String argv[]) {
        int result = run(argv, System.out);
        System.exit(result + STATUS_TEMP);
    }

    public static int run (String argv[], PrintStream out) {
        int result =  new plugLaunchConnect001().runThis(argv, out);
        if ( result == STATUS_FAILED ) {
            logAlways("\n##> nsk/jdi/PlugConnectors/LaunchConnector/plugLaunchConnect001 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/PlugConnectors/LaunchConnector/plugLaunchConnect001 test PASSED");
        }
        return result;
    }


    private int runThis (String argv[], PrintStream out) {
        int testResult = STATUS_PASSED;

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);
        logHandler.enableErrorsSummary(false);

        String expectedPlugLaunchConnectorName = "PlugLaunchConnector001_Name";
        String expectedPlugLaunchConnectorDescription = "PlugLaunchConnector001_Description";
        String expectedPlugLaunchConnectorTransportName = "PlugLaunchConnector001_Transport";

        logAlways("==> nsk/jdi/PlugConnectors/LaunchConnector/plugLaunchConnect001 test...");
        logOnVerbose
            ("==> Test checks that expected pluggable launching connector is created properly.");


        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        if (virtualMachineManager == null) {
            logOnError(errorLogPrefixHead + "Bootstrap.virtualMachineManager() returns null.");
            return STATUS_FAILED;
        }

        // check that expected pluggable launching connector is found out in launchingConnectors() List
        List launchingConnectorsList = virtualMachineManager.launchingConnectors();
        int launchingConnectorsNumber = launchingConnectorsList.size();
        LaunchingConnector expectedPlugLaunchConnector = null;

        for (int i=0; i < launchingConnectorsNumber; i++ ) {
            LaunchingConnector launchingConnector = (LaunchingConnector)launchingConnectorsList.get(i);
            String launchConnectorName = launchingConnector.name();
            if ( expectedPlugLaunchConnectorName.equals(launchConnectorName) ) {
                expectedPlugLaunchConnector = launchingConnector;
                break;
            }
        }

        if ( expectedPlugLaunchConnector == null ) {
            logOnError(errorLogPrefixHead + "Expected pluggable launching connector is NOT found out.");
            logOnError(errorLogPrefix + "Expected connector name = '" + expectedPlugLaunchConnectorName + "'");
            return STATUS_FAILED;
        }

        // check that expected pluggable launching connector has expected description
        String actualDescription = expectedPlugLaunchConnector.description();
        if ( ! expectedPlugLaunchConnectorDescription.equals(actualDescription) ) {
            logOnError(errorLogPrefixHead + "Pluggable launching connector has unexpected descripton:");
            logOnError(errorLogPrefix + "Expected descripton = '" + expectedPlugLaunchConnectorDescription + "'");
            logOnError(errorLogPrefix + "Actual descripton = '" + actualDescription + "'");
            testResult = STATUS_FAILED;
        }

        // check that expected pluggable launching connector has expected transport name
        String actualTransportName = expectedPlugLaunchConnector.transport().name();
        if ( ! expectedPlugLaunchConnectorTransportName.equals(actualTransportName) ) {
            logOnError(errorLogPrefixHead + "Pluggable launching connector has unexpected Transport:");
            logOnError
                (errorLogPrefix + "Expected Transport name = '" + expectedPlugLaunchConnectorTransportName + "'");
            logOnError(errorLogPrefix + "Actual Transport name = '" + actualTransportName + "'");
            testResult = STATUS_FAILED;
        }

        // check that expected pluggable launching connector has not defaultArguments
        Map actualDefaultArguments = expectedPlugLaunchConnector.defaultArguments();
        int actualDefaultArgumentsNumber = actualDefaultArguments.size();
        if ( actualDefaultArgumentsNumber != 0 ) {
            logOnError(errorLogPrefixHead + "Pluggable launching connector has unexpected defaultArguments:");
            logOnError(errorLogPrefix + "Expected defaultArguments Map - <empty>");
            logOnError(errorLogPrefix + "Actual defaultArguments Map size = " + actualDefaultArgumentsNumber);
            testResult = STATUS_FAILED;
        }

        // check that expected pluggable launching connector is found out in allConnectors() List too
        List allConnectorsList = virtualMachineManager.allConnectors();
        int allConnectorsNumber = allConnectorsList.size();

        boolean expectedPlugLaunchConnectorFound = false;
        for (int i=0; i < allConnectorsNumber; i++ ) {
            Connector foundConnector = (Connector)allConnectorsList.get(i);
            if ( foundConnector instanceof LaunchingConnector ) {
                LaunchingConnector foundLaunchingConnector = (LaunchingConnector)foundConnector;
                if ( expectedPlugLaunchConnector.equals(foundLaunchingConnector) ) {
                    expectedPlugLaunchConnectorFound = true;
                    break;
                }
            }
        }

        if ( ! expectedPlugLaunchConnectorFound ) {
            logOnError(errorLogPrefixHead
                + "Expected pluggable launching connector is NOT found out in allConnectors() List");
            logOnError(errorLogPrefix + "Expected connector = " + expectedPlugLaunchConnector);
            testResult = STATUS_FAILED;
        }

        return testResult;
    }
} // end of plugLaunchConnect001 class
