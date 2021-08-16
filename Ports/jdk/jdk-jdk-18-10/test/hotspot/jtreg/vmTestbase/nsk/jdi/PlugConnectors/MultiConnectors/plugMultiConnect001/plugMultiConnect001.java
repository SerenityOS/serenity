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
 * @summary converted from VM Testbase nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect001 test:
 *     This test is the test for the mechanism for creating pluggable Connectors
 *     on base of classes which implement the Connector interfaces
 *     (AttachingConnector, ListeningConnector, or LaunchingConnector).
 *     The test checks up that at start-up time when
 *     Bootstrap.virtualMachineManager() is invoked the 6 expected
 *     pluggable connectors are created  properly on base of 6 differen
 *     Connector implementations of different types.
 *     The test expects that 2 pluggable connectors should be
 *     of AttachingConnector type, other 2 connectors -
 *     of ListeningConnector type, and last 2 coonectors -
 *     of LaunchingConnector type
 *     Each pluggable connector has to be contained in corresponding list:
 *     VirtualMachineManager.attachingConnectors(),
 *     VirtualMachineManager.launchingConnectors() or
 *     VirtualMachineManager.listeningConnectors().
 *     All 6 pluggable connectors have to be contained in
 *     VirtualMachineManager.allConnectors() list.
 *     Each pluggable connector is checked for the certain expected
 *     description and for the certain expected transport.
 *     For each connector type one pluggable connector should have the
 *     empty Map of default arguments, and other pluggable connector
 *     should have 4 default arguments of all types:
 *          Connector.StringArgument;
 *          Connector.IntegerArgument;
 *          Connector.BooleanArgument;
 *          Connector.SelectedArgument;
 * COMMENTS:
 *     Fixed bug 6426609: nsk/share/jdi/plug_connectors_jar_file.pl should use
 *                        "system" instead of back-quotes
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect001.plugMultiConnect001
 *
 * @comment build connectors.jar to jars
 * @build nsk.share.jdi.PlugConnectors
 * @run driver nsk.jdi.ConnectorsJarBuilder
 *
 * @run main/othervm
 *      -cp jars${file.separator}connectors.jar${path.separator}${test.class.path}${path.separator}${java.class.path}
 *      nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect001.plugMultiConnect001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 */

package nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect001;

import nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect001.connectors.*;

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
 * Bootstrap.virtualMachineManager() is invoked the 6 expected         <BR>
 * pluggable connectors are created  properly on base of 6 different   <BR>
 * Connector implementations of different types.                       <BR>
 *                                                                     <BR>
 * The test expects that 2 pluggable connectors should be              <BR>
 * of AttachingConnector type, other 2 connectors -                    <BR>
 * of ListeningConnector type, and last 2 coonectors -                 <BR>
 * of LaunchingConnector type                                          <BR>
 *                                                                     <BR>
 * Each pluggable connector has to be contained in corresponding list: <BR>
 * VirtualMachineManager.attachingConnectors(),                        <BR>
 * VirtualMachineManager.launchingConnectors() or                      <BR>
 * VirtualMachineManager.listeningConnectors().                        <BR>
 *                                                                     <BR>
 * All 6 pluggable connectors have to be contained in                  <BR>
 * VirtualMachineManager.allConnectors() list.                         <BR>
 *                                                                     <BR>
 * Each pluggable connector is checked for the certain expected        <BR>
 * description and for the certain expected transport.                 <BR>
 *                                                                     <BR>
 * For each connector type one pluggable connector should have the     <BR>
 * empty Map of default arguments, and other pluggable connector       <BR>
 * should have 4 default arguments of all types:                       <BR>
 *      Connector.StringArgument;                                      <BR>
 *      Connector.IntegerArgument;                                     <BR>
 *      Connector.BooleanArgument;                                     <BR>
 *      Connector.SelectedArgument;                                    <BR>
 *                                                                     <BR>
 */

public class plugMultiConnect001 {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "plugMultiConnect001: ";
    static final String errorLogPrefix     = "                     ";
    static final String infoLogPrefixNead = "--> plugMultiConnect001: ";
    static final String infoLogPrefix     = "-->                      ";
    static final String emptyString = "";


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
        int result =  new plugMultiConnect001().runThis(argv, out);
        if ( result == STATUS_FAILED ) {
            logAlways("\n##> nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect001 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect001 test PASSED");
        }
        return result;
    }


    private int runThis (String argv[], PrintStream out) {
        int testResult = STATUS_PASSED;

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);
        logHandler.enableErrorsSummary(false);

        logAlways("==> nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect001 test...");
        logOnVerbose
            ("==> Test checks that expected pluggable connectors are created properly");
        logOnVerbose
            ("==> on base of 6 different Connector implementations.");
        int expectedConnectorsNumber = 6;

        String[] checkedPlugConnectorNames = new String[expectedConnectorsNumber];
        checkedPlugConnectorNames[0] = "PlugAttachConnector001_01_Name";
        checkedPlugConnectorNames[1] = "PlugAttachConnector001_02_Name";
        checkedPlugConnectorNames[2] = "PlugLaunchConnector001_01_Name";
        checkedPlugConnectorNames[3] = "PlugLaunchConnector001_02_Name";
        checkedPlugConnectorNames[4] = "PlugListenConnector001_01_Name";
        checkedPlugConnectorNames[5] = "PlugListenConnector001_02_Name";

        Connector[] referencePlugConnectors = new Connector[expectedConnectorsNumber];
        referencePlugConnectors[0] = new PlugAttachConnector001_01();
        referencePlugConnectors[1] = new PlugAttachConnector001_02();
        referencePlugConnectors[2] = new PlugLaunchConnector001_01();
        referencePlugConnectors[3] = new PlugLaunchConnector001_02();
        referencePlugConnectors[4] = new PlugListenConnector001_01();
        referencePlugConnectors[5] = new PlugListenConnector001_02();

        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        if (virtualMachineManager == null) {
            logOnError(errorLogPrefixHead + "Bootstrap.virtualMachineManager() returns null.");
            return STATUS_FAILED;
        }

        for (int i=0; i < expectedConnectorsNumber; i++ ) {
            int checkResult = checkConnector(virtualMachineManager,
                                            checkedPlugConnectorNames[i],
                                            referencePlugConnectors[i]);
            if ( testResult == STATUS_PASSED ) {
                testResult = checkResult;
            }
        }

        return testResult;
    }

    private int checkConnector (VirtualMachineManager virtualMachineManager,
                                String checkedPlugConnectorName,
                                Connector referencePlugConnector) {
        int checkResult = STATUS_PASSED;

        // check that checked pluggable connector is found out
        // in attaching/launching/listeningConnectors() List
        List connectorsList = null;
        if ( referencePlugConnector instanceof AttachingConnector) {
            connectorsList = virtualMachineManager.attachingConnectors();
        }
        if ( referencePlugConnector instanceof LaunchingConnector) {
            connectorsList = virtualMachineManager.launchingConnectors();
        }
        if ( referencePlugConnector instanceof ListeningConnector) {
            connectorsList = virtualMachineManager.listeningConnectors();
        }

        int connectorsNumber = connectorsList.size();
        Connector checkedPlugConnector = null;

        for (int i=0; i < connectorsNumber; i++ ) {
            Connector connector = (Connector)connectorsList.get(i);
            String connectorName = connector.name();
            if ( checkedPlugConnectorName.equals(connectorName) ) {
                if ( checkedPlugConnector != null ) {
                    logOnError(errorLogPrefixHead
                        + "One more pluggable connector with the same name is found out.");
                    logOnError(errorLogPrefix + "Connector name = '" + checkedPlugConnectorName + "'");
                    logOnError(errorLogPrefix + "First Connector = '" + checkedPlugConnector + "'");
                    logOnError(errorLogPrefix + "Found Connector = '" + connector + "'");
                    checkResult = STATUS_FAILED;
                } else {
                    checkedPlugConnector = connector;
                }
            }
        }

        if ( checkedPlugConnector == null ) {
            logOnError(errorLogPrefixHead + "Expected pluggable connector is NOT found out.");
            logOnError(errorLogPrefix + "Expected connector name = '" + checkedPlugConnectorName + "'");
            return STATUS_FAILED;
        }

        // check that checked pluggable connector is found out in allConnectors() List too
        List allConnectorsList = virtualMachineManager.allConnectors();
        int allConnectorsNumber = allConnectorsList.size();

        boolean checkedPlugConnectorFound = false;
        for (int i=0; i < allConnectorsNumber; i++ ) {
            Connector foundConnector = (Connector)allConnectorsList.get(i);
            if ( checkedPlugConnector.equals(foundConnector) ) {
                if ( checkedPlugConnectorFound ) {
                    logOnError(errorLogPrefixHead
                        + "One more pluggable connector with the same name is found out in allConnectors() List.");
                    logOnError(errorLogPrefix + "Connector name = '" + checkedPlugConnectorName + "'");
                    logOnError(errorLogPrefix + "First Connector = '" + checkedPlugConnector + "'");
                    logOnError(errorLogPrefix + "Found Connector = '" + foundConnector + "'");
                    checkResult = STATUS_FAILED;
                } else {
                    checkedPlugConnectorFound = true;
                }
            }
        }

        if ( ! checkedPlugConnectorFound ) {
            logOnError(errorLogPrefixHead
                + "Checked pluggable connector is NOT found out in allConnectors() List");
            logOnError(errorLogPrefix + "Checked connector = " + checkedPlugConnector);
            checkResult = STATUS_FAILED;
        }

        // check that checked pluggable connector matches corresponding reference connector
        String errorMessage = PlugConnectors.compareConnectors(
            errorLogPrefixHead,
            errorLogPrefix,
            referencePlugConnector,
            checkedPlugConnector);

        if ( ! emptyString.equals(errorMessage) ) {
            logOnError(errorMessage);
            return STATUS_FAILED;
        }

        // check default Arguments of checked pluggable connector

        Map referenceDefaultArguments = referencePlugConnector.defaultArguments();
        Map checkedDefaultArguments = checkedPlugConnector.defaultArguments();

        int referenceDefaultArgumentsNumber = referenceDefaultArguments.size();
        int checkedDefaultArgumentsNumber = checkedDefaultArguments.size();

        if ( referenceDefaultArgumentsNumber != checkedDefaultArgumentsNumber ) {
            logOnError(errorLogPrefixHead
                + "Checked pluggable connector contains unexpected number of default arguments");
            logOnError(errorLogPrefix + "Checked connector = " + checkedPlugConnector);
            logOnError(errorLogPrefix + "Expected number of default arguments = "
                + referenceDefaultArgumentsNumber);
            logOnError(errorLogPrefix + "Actual number of default arguments = "
                + checkedDefaultArgumentsNumber);
            return STATUS_FAILED;
        }

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
                checkResult = STATUS_FAILED;
            }
        }

        return checkResult;
    }
} // end of plugMultiConnect001 class
