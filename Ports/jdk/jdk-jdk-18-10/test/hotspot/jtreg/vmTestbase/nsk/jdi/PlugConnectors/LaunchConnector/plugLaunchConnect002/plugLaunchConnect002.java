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
 * @summary converted from VM Testbase nsk/jdi/PlugConnectors/LaunchConnector/plugLaunchConnect002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     nsk/jdi/PlugConnectors/LaunchConnector/plugLaunchConnect002 test:
 *     This test is the test for the mechanism for creating pluggable Connectors
 *     on base of classes which implement the Connector interfaces
 *     (AttachingConnector, ListeningConnector, or LaunchingConnector).
 *     The test checks up that at start-up time when
 *     Bootstrap.virtualMachineManager() is invoked the pluggable
 *     connector named "PlugLaunchConnector002_Name" is created on base
 *     of PlugLaunchConnector002 class which implements
 *     com.sun.jdi.connect.LaunchingConnector interface.
 *     This pluggable connector has to be contained in lists returned
 *     by VirtualMachineManager.launchingConnectors() and
 *     by VirtualMachineManager.allConnectors() methods.
 *     This pluggable connector has to have:
 *      Connector.description() = "PlugLaunchConnector002_Description";
 *      Connector.transport().name() = "PlugLaunchConnector002_Transport";
 *      Connector.defaultArguments() = List of 4 Connector.Argument:
 *          Connector.StringArgument named
 *              'PlugLaunchConnector002_StringArgument_Name'
 *          Connector.IntegerArgument named
 *              'PlugLaunchConnector002_IntegerArgument_Name'
 *          Connector.BooleanArgument named
 *              'PlugLaunchConnector002_BooleanArgument_Name'
 *          Connector.SelectedArgument named
 *              'PlugLaunchConnector002_SelectedArgument_Name'
 * COMMENTS:
 *     Fixed bug 6426609: nsk/share/jdi/plug_connectors_jar_file.pl should use
 *                        "system" instead of back-quotes
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.PlugConnectors.LaunchConnector.plugLaunchConnect002.plugLaunchConnect002
 *
 * @comment build connectors.jar to jars
 * @build nsk.share.jdi.PlugConnectors
 * @run driver nsk.jdi.ConnectorsJarBuilder
 *
 * @run main/othervm
 *      -cp jars${file.separator}connectors.jar${path.separator}${test.class.path}${path.separator}${java.class.path}
 *      nsk.jdi.PlugConnectors.LaunchConnector.plugLaunchConnect002.plugLaunchConnect002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 */

package nsk.jdi.PlugConnectors.LaunchConnector.plugLaunchConnect002;

import nsk.jdi.PlugConnectors.LaunchConnector.plugLaunchConnect002.connectors.*;

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
 * connector named "PlugLaunchConnector002_Name" is created on base    <BR>
 * of PlugLaunchConnector002 class which implements                    <BR>
 * com.sun.jdi.connect.LaunchingConnector interface.                   <BR>
 *                                                                     <BR>
 * This pluggable connector has to be contained in lists returned      <BR>
 * by VirtualMachineManager.launchingConnectors() and                  <BR>
 * by VirtualMachineManager.allConnectors() methods.                   <BR>
 *                                                                     <BR>
 * This pluggable connector has to have:                               <BR>
 *  Connector.description() = "PlugLaunchConnector002_Description";    <BR>
 *  Connector.transport().name() = "PlugLaunchConnector002_Transport"; <BR>
 *  Connector.defaultArguments() = List of 4 Connector.Argument:       <BR>
 *      Connector.StringArgument named                                 <BR>
 *          'PlugLaunchConnector002_StringArgument_Name'               <BR>
 *      Connector.IntegerArgument named                                <BR>
 *          'PlugLaunchConnector002_IntegerArgument_Name'              <BR>
 *      Connector.BooleanArgument named                                <BR>
 *          'PlugLaunchConnector002_BooleanArgument_Name'              <BR>
 *      Connector.SelectedArgument named                               <BR>
 *          'PlugLaunchConnector002_SelectedArgument_Name'             <BR>
 *                                                                     <BR>
 */

public class plugLaunchConnect002 {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "plugLaunchConnect002: ";
    static final String errorLogPrefix     = "                      ";
    static final String infoLogPrefixNead = "--> plugLaunchConnect002: ";
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
        int result =  new plugLaunchConnect002().runThis(argv, out);
        if ( result == STATUS_FAILED ) {
            logAlways("\n##> nsk/jdi/PlugConnectors/LaunchConnector/plugLaunchConnect002 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/PlugConnectors/LaunchConnector/plugLaunchConnect002 test PASSED");
        }
        return result;
    }


    private int runThis (String argv[], PrintStream out) {
        int testResult = STATUS_PASSED;

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);
        logHandler.enableErrorsSummary(false);

        String checkedPlugLaunchConnectorName = "PlugLaunchConnector002_Name";
        String checkedPlugLaunchConnectorDescription = "PlugLaunchConnector002_Description";
        String checkedPlugLaunchConnectorTransportName = "PlugLaunchConnector002_Transport";

        logAlways("==> nsk/jdi/PlugConnectors/LaunchConnector/plugLaunchConnect002 test...");
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
        LaunchingConnector checkedPlugLaunchConnector = null;

        for (int i=0; i < launchingConnectorsNumber; i++ ) {
            LaunchingConnector launchingConnector = (LaunchingConnector)launchingConnectorsList.get(i);
            String launchConnectorName = launchingConnector.name();
            if ( checkedPlugLaunchConnectorName.equals(launchConnectorName) ) {
                checkedPlugLaunchConnector = launchingConnector;
                break;
            }
        }

        if ( checkedPlugLaunchConnector == null ) {
            logOnError(errorLogPrefixHead + "Expected pluggable launching connector is NOT found out.");
            logOnError(errorLogPrefix + "Expected connector name = '" + checkedPlugLaunchConnectorName + "'");
            return STATUS_FAILED;
        }

        // check that expected pluggable launching connector is found out in allConnectors() List too
        List allConnectorsList = virtualMachineManager.allConnectors();
        int allConnectorsNumber = allConnectorsList.size();

        boolean checkedPlugLaunchConnectorFound = false;
        for (int i=0; i < allConnectorsNumber; i++ ) {
            Connector foundConnector = (Connector)allConnectorsList.get(i);
            if ( foundConnector instanceof LaunchingConnector ) {
                LaunchingConnector foundLaunchingConnector = (LaunchingConnector)foundConnector;
                if ( checkedPlugLaunchConnector.equals(foundLaunchingConnector) ) {
                    checkedPlugLaunchConnectorFound = true;
                    break;
                }
            }
        }

        if ( ! checkedPlugLaunchConnectorFound ) {
            logOnError(errorLogPrefixHead
                + "Expected pluggable launching connector is NOT found out in allConnectors() List");
            logOnError(errorLogPrefix + "Expected connector = " + checkedPlugLaunchConnector);
            testResult = STATUS_FAILED;
        }


        LaunchingConnector referencePlugLaunchConnector = new PlugLaunchConnector002();

        String emptyString = "";
        String errorMessage = PlugConnectors.compareConnectors(
            errorLogPrefixHead,
            errorLogPrefix,
            referencePlugLaunchConnector,
            checkedPlugLaunchConnector);

        if ( ! emptyString.equals(errorMessage) ) {
            logOnError(errorMessage);
            return STATUS_FAILED;
        }

        // check default Arguments of checked pluggable connector

        // strings below are for info only
//        String plugLaunchConnectorStringArgumentKey = "PlugLaunchConnector002_StringArgument_Key";
//        String plugLaunchConnectorIntegerArgumentKey = "PlugLaunchConnector002_IntegerArgument_Key";
//        String plugLaunchConnectorBooleanArgumentKey = "PlugLaunchConnector002_BooleanArgument_Key";
//        String plugLaunchConnectorSelectedArgumentKey = "PlugLaunchConnector002_SelectedArgument_Key";

        Map referenceDefaultArguments = referencePlugLaunchConnector.defaultArguments();
        Map checkedDefaultArguments = checkedPlugLaunchConnector.defaultArguments();

        int referenceDefaultArgumentsNumber = referenceDefaultArguments.size();
        Object[] referenceDefaultArgumentsKeys = referenceDefaultArguments.keySet().toArray();
        for (int i=0; i < referenceDefaultArgumentsNumber; i++) {
            String referenceKey = (String)referenceDefaultArgumentsKeys[i];
            Connector.Argument referenceArgument =
                (Connector.Argument)(referenceDefaultArguments.get(referenceKey));
            Connector.Argument checkedArgument =
                (Connector.Argument)(checkedDefaultArguments.get(referenceKey));
            errorMessage = PlugConnectors.compareConnectorArguments(
                errorLogPrefixHead,
                errorLogPrefix,
                referenceArgument,
                checkedArgument);

            if ( ! emptyString.equals(errorMessage) ) {
                logOnError(errorMessage);
                testResult = STATUS_FAILED;
            }
        }

        return testResult;
    }
} // end of plugLaunchConnect002 class
