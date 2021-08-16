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
 * @summary converted from VM Testbase nsk/jdi/PlugConnectors/ListenConnector/plugListenConnect002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     nsk/jdi/PlugConnectors/ListenConnector/plugListenConnect002 test:
 *     This test is the test for the mechanism for creating pluggable Connectors
 *     on base of classes which implement the Connector interfaces
 *     (AttachingConnector, ListeningConnector, or LaunchingConnector).
 *     The test checks up that at start-up time when
 *     Bootstrap.virtualMachineManager() is invoked the pluggable
 *     connector named "PlugListenConnector002_Name" is created on base
 *     of PlugListenConnector002 class which implements
 *     com.sun.jdi.connect.ListeningConnector interface.
 *     This pluggable connector has to be contained in lists returned
 *     by VirtualMachineManager.listeningConnectors() and
 *     by VirtualMachineManager.allConnectors() methods.
 *     This pluggable connector has to have:
 *      Connector.description() = "PlugListenConnector002_Description";
 *      Connector.transport().name() = "PlugListenConnector002_Transport";
 *      Connector.defaultArguments() = List of 4 Connector.Argument:
 *          Connector.StringArgument named
 *              'PlugListenConnector002_StringArgument_Name'
 *          Connector.IntegerArgument named
 *              'PlugListenConnector002_IntegerArgument_Name'
 *          Connector.BooleanArgument named
 *              'PlugListenConnector002_BooleanArgument_Name'
 *          Connector.SelectedArgument named
 *              'PlugListenConnector002_SelectedArgument_Name'
 * COMMENTS:
 *     Fixed bug 6426609: nsk/share/jdi/plug_connectors_jar_file.pl should use
 *                        "system" instead of back-quotes
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.PlugConnectors.ListenConnector.plugListenConnect002.plugListenConnect002
 *
 * @comment build connectors.jar to jars
 * @build nsk.share.jdi.PlugConnectors
 * @run driver nsk.jdi.ConnectorsJarBuilder
 *
 * @run main/othervm
 *      -cp jars${file.separator}connectors.jar${path.separator}${test.class.path}${path.separator}${java.class.path}
 *      nsk.jdi.PlugConnectors.ListenConnector.plugListenConnect002.plugListenConnect002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 */

package nsk.jdi.PlugConnectors.ListenConnector.plugListenConnect002;

import nsk.jdi.PlugConnectors.ListenConnector.plugListenConnect002.connectors.*;

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
 * connector named "PlugListenConnector002_Name" is created on base    <BR>
 * of PlugListenConnector002 class which implements                    <BR>
 * com.sun.jdi.connect.ListeningConnector interface.                   <BR>
 *                                                                     <BR>
 * This pluggable connector has to be contained in lists returned      <BR>
 * by VirtualMachineManager.listeningConnectors() and                  <BR>
 * by VirtualMachineManager.allConnectors() methods.                   <BR>
 *                                                                     <BR>
 * This pluggable connector has to have:                               <BR>
 *  Connector.description() = "PlugListenConnector002_Description";    <BR>
 *  Connector.transport().name() = "PlugListenConnector002_Transport"; <BR>
 *  Connector.defaultArguments() = List of 4 Connector.Argument:       <BR>
 *      Connector.StringArgument named                                 <BR>
 *          'PlugListenConnector002_StringArgument_Name'               <BR>
 *      Connector.IntegerArgument named                                <BR>
 *          'PlugListenConnector002_IntegerArgument_Name'              <BR>
 *      Connector.BooleanArgument named                                <BR>
 *          'PlugListenConnector002_BooleanArgument_Name'              <BR>
 *      Connector.SelectedArgument named                               <BR>
 *          'PlugListenConnector002_SelectedArgument_Name'             <BR>
 *                                                                     <BR>
 */

public class plugListenConnect002 {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "plugListenConnect002: ";
    static final String errorLogPrefix     = "                      ";
    static final String infoLogPrefixNead = "--> plugListenConnect002: ";
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
        int result =  new plugListenConnect002().runThis(argv, out);
        if ( result == STATUS_FAILED ) {
            logAlways("\n##> nsk/jdi/PlugConnectors/ListenConnector/plugListenConnect002 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/PlugConnectors/ListenConnector/plugListenConnect002 test PASSED");
        }
        return result;
    }


    private int runThis (String argv[], PrintStream out) {
        int testResult = STATUS_PASSED;

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);
        logHandler.enableErrorsSummary(false);

        String checkedPlugListenConnectorName = "PlugListenConnector002_Name";
        String checkedPlugListenConnectorDescription = "PlugListenConnector002_Description";
        String checkedPlugListenConnectorTransportName = "PlugListenConnector002_Transport";

        logAlways("==> nsk/jdi/PlugConnectors/ListenConnector/plugListenConnect002 test...");
        logOnVerbose
            ("==> Test checks that expected pluggable listening connector is created properly.");


        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        if (virtualMachineManager == null) {
            logOnError(errorLogPrefixHead + "Bootstrap.virtualMachineManager() returns null.");
            return STATUS_FAILED;
        }

        // check that expected pluggable listening connector is found out in listeningConnectors() List
        List listeningConnectorsList = virtualMachineManager.listeningConnectors();
        int listeningConnectorsNumber = listeningConnectorsList.size();
        ListeningConnector checkedPlugListenConnector = null;

        for (int i=0; i < listeningConnectorsNumber; i++ ) {
            ListeningConnector listeningConnector = (ListeningConnector)listeningConnectorsList.get(i);
            String listenConnectorName = listeningConnector.name();
            if ( checkedPlugListenConnectorName.equals(listenConnectorName) ) {
                checkedPlugListenConnector = listeningConnector;
                break;
            }
        }

        if ( checkedPlugListenConnector == null ) {
            logOnError(errorLogPrefixHead + "Expected pluggable listening connector is NOT found out.");
            logOnError(errorLogPrefix + "Expected connector name = '" + checkedPlugListenConnectorName + "'");
            return STATUS_FAILED;
        }

        // check that expected pluggable listening connector is found out in allConnectors() List too
        List allConnectorsList = virtualMachineManager.allConnectors();
        int allConnectorsNumber = allConnectorsList.size();

        boolean checkedPlugListenConnectorFound = false;
        for (int i=0; i < allConnectorsNumber; i++ ) {
            Connector foundConnector = (Connector)allConnectorsList.get(i);
            if ( foundConnector instanceof ListeningConnector ) {
                ListeningConnector foundListeningConnector = (ListeningConnector)foundConnector;
                if ( checkedPlugListenConnector.equals(foundListeningConnector) ) {
                    checkedPlugListenConnectorFound = true;
                    break;
                }
            }
        }

        if ( ! checkedPlugListenConnectorFound ) {
            logOnError(errorLogPrefixHead
                + "Expected pluggable listening connector is NOT found out in allConnectors() List");
            logOnError(errorLogPrefix + "Expected connector = " + checkedPlugListenConnector);
            testResult = STATUS_FAILED;
        }


        ListeningConnector referencePlugListenConnector = new PlugListenConnector002();

        String emptyString = "";
        String errorMessage = PlugConnectors.compareConnectors(
            errorLogPrefixHead,
            errorLogPrefix,
            referencePlugListenConnector,
            checkedPlugListenConnector);

        if ( ! emptyString.equals(errorMessage) ) {
            logOnError(errorMessage);
            return STATUS_FAILED;
        }

        // check default Arguments of checked pluggable connector

        // strings below are for info only
//        String plugListenConnectorStringArgumentKey = "PlugListenConnector002_StringArgument_Key";
//        String plugListenConnectorIntegerArgumentKey = "PlugListenConnector002_IntegerArgument_Key";
//        String plugListenConnectorBooleanArgumentKey = "PlugListenConnector002_BooleanArgument_Key";
//        String plugListenConnectorSelectedArgumentKey = "PlugListenConnector002_SelectedArgument_Key";

        Map referenceDefaultArguments = referencePlugListenConnector.defaultArguments();
        Map checkedDefaultArguments = checkedPlugListenConnector.defaultArguments();

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
} // end of plugListenConnect002 class
