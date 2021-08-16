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
 * @summary converted from VM Testbase nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect002 test:
 *     This test is the test for the mechanism for creating pluggable Connectors
 *     on base of classes which implement the Connector interfaces
 *     (AttachingConnector, ListeningConnector, or LaunchingConnector).
 *     The test checks up that at start-up time when
 *     Bootstrap.virtualMachineManager() is invoked the 6 expected
 *     pluggable connectors are created  properly on base of 6 different
 *     Connector implementations of different types, but 3 other pluggable
 *     connectors are NOT created for Connector implementations for which
 *     instances can not be created.
 *     The test expects that 2 created pluggable connectors should be
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
 *     Also the test checks up that at start-up time when
 *     Bootstrap.virtualMachineManager() is invoked the 3 pluggable
 *     connectors are NOT created as they are based on classes which
 *     implement the Connector interfaces
 *     (AttachingConnector, ListeningConnector, and LaunchingConnector),
 *     but constructors of these classes throw Exception.
 *     These "invalid" Connector implementations are:
 *         PlugAttachConnector002_03 class;
 *         PlugLaunchConnector002_03 class;
 *         PlugListenConnector002_03 class;
 * COMMENTS:
 *     Fixed bug 6426609: nsk/share/jdi/plug_connectors_jar_file.pl should use
 *                        "system" instead of back-quotes
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect002.plugMultiConnect002
 *
 * @comment build connectors.jar to jars
 * @build nsk.share.jdi.PlugConnectors
 * @run driver nsk.jdi.ConnectorsJarBuilder
 *
 * @run main/othervm
 *      -cp jars${file.separator}connectors.jar${path.separator}${test.class.path}${path.separator}${java.class.path}
 *      nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect002.plugMultiConnect002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 */

package nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect002;

import nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect002.connectors.*;

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
 * Connector implementations of different types, but 3 other pluggable <BR>
 * connectors are NOT created for Connector implementations for which  <BR>
 * instances can not be created.                                       <BR>
 *                                                                     <BR>
 * The test expects that 2 created pluggable connectors should be      <BR>
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
 * Also the test checks up that at start-up time when                  <BR>
 * Bootstrap.virtualMachineManager() is invoked the 3 pluggable        <BR>
 * connectors are NOT created as they are based on classes which       <BR>
 * implement the Connector interfaces                                  <BR>
 * (AttachingConnector, ListeningConnector, and LaunchingConnector),   <BR>
 * but constructors of these classes throw Exception.                  <BR>
 * These "invalid" Connector implementations are:                      <BR>
 *     PlugAttachConnector002_03 class;                                <BR>
 *     PlugLaunchConnector002_03 class;                                <BR>
 *     PlugListenConnector002_03 class;                                <BR>
 *                                                                     <BR>
 */

public class plugMultiConnect002 {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "plugMultiConnect002: ";
    static final String errorLogPrefix     = "                     ";
    static final String infoLogPrefixNead = "--> plugMultiConnect002: ";
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
        int result =  new plugMultiConnect002().runThis(argv, out);
        if ( result == STATUS_FAILED ) {
            logAlways("\n##> nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect002 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect002 test PASSED");
        }
        return result;
    }


    private int runThis (String argv[], PrintStream out) {
        int testResult = STATUS_PASSED;

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);
        logHandler.enableErrorsSummary(false);

        logAlways("==> nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect002 test...");
        logOnVerbose
            ("==> Test checks that expected pluggable connectors are created properly");
        logOnVerbose
            ("==> on base of 6 different Connector implementations,");
        logOnVerbose
            ("==> but other pluggable connectors are NOT created");
        logOnVerbose
            ("==> for Connector implementations for which instances can not be created.\n");
        int expectedConnectorsNumber = 6;

        String[] checkedPlugConnectorNames = new String[expectedConnectorsNumber];
        checkedPlugConnectorNames[0] = "PlugAttachConnector002_01_Name";
        checkedPlugConnectorNames[1] = "PlugAttachConnector002_02_Name";
        checkedPlugConnectorNames[2] = "PlugLaunchConnector002_01_Name";
        checkedPlugConnectorNames[3] = "PlugLaunchConnector002_02_Name";
        checkedPlugConnectorNames[4] = "PlugListenConnector002_01_Name";
        checkedPlugConnectorNames[5] = "PlugListenConnector002_02_Name";

        Connector[] referencePlugConnectors = new Connector[expectedConnectorsNumber];
        referencePlugConnectors[0] = new PlugAttachConnector002_01();
        referencePlugConnectors[1] = new PlugAttachConnector002_02();
        referencePlugConnectors[2] = new PlugLaunchConnector002_01();
        referencePlugConnectors[3] = new PlugLaunchConnector002_02();
        referencePlugConnectors[4] = new PlugListenConnector002_01();
        referencePlugConnectors[5] = new PlugListenConnector002_02();

        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        if (virtualMachineManager == null) {
            logOnError(errorLogPrefixHead + "Bootstrap.virtualMachineManager() returns null.");
            return STATUS_FAILED;
        }

        for (int i=0; i < expectedConnectorsNumber; i++ ) {
            int checkResult = checkForValidConnector(virtualMachineManager,
                                                checkedPlugConnectorNames[i],
                                                referencePlugConnectors[i]);
            if ( testResult == STATUS_PASSED ) {
                testResult = checkResult;
            }
        }

        int invalidConnectorsNumber = 3;

        String[] invalidPlugConnectorClassesNames = new String[invalidConnectorsNumber];
        invalidPlugConnectorClassesNames[0] = "PlugAttachConnector002_03";
        invalidPlugConnectorClassesNames[1] = "PlugLaunchConnector002_03";
        invalidPlugConnectorClassesNames[2] = "PlugListenConnector002_03";

        for (int i=0; i < invalidConnectorsNumber; i++ ) {
            int checkResult = checkForInvalidConnector(virtualMachineManager,
                                                  invalidPlugConnectorClassesNames[i]);
            if ( testResult == STATUS_PASSED ) {
                testResult = checkResult;
            }
        }

        return testResult;
    }

    private int checkForValidConnector (VirtualMachineManager virtualMachineManager,
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

    private int checkForInvalidConnector (VirtualMachineManager virtualMachineManager,
                                          String invalidPlugConnectorClassName) {
        int checkResult = STATUS_PASSED;

        List connectorsList = null;
        String connectorsListName = null;
        if ( invalidPlugConnectorClassName.equals("PlugAttachConnector002_03") ) {
            connectorsList = virtualMachineManager.attachingConnectors();
            connectorsListName = "attachingConnectors() List";
        }
        if ( invalidPlugConnectorClassName.equals("PlugLaunchConnector002_03") ) {
            connectorsList = virtualMachineManager.launchingConnectors();
            connectorsListName = "launchingConnectors() List";
        }
        if ( invalidPlugConnectorClassName.equals("PlugListenConnector002_03") ) {
            connectorsList = virtualMachineManager.listeningConnectors();
            connectorsListName = "listeningConnectors() List";
        }
        int connectorsNumber = connectorsList.size();

        // check that pluggable connector is NOT created on base of Connector
        // implementation for which instance can not be created
        Connector invalidPlugConnector = null;

        for (int i=0; i < connectorsNumber; i++ ) {
            Connector foundConnector = (Connector)connectorsList.get(i);
            if ( invalidPlugConnectorClassName.equals("PlugAttachConnector002_03") ) {
                if ( foundConnector instanceof PlugAttachConnector002_03 ) {
                    invalidPlugConnector = foundConnector;
                    break;
                }
            }
            if ( invalidPlugConnectorClassName.equals("PlugLaunchConnector002_03") ) {
                if ( foundConnector instanceof PlugLaunchConnector002_03 ) {
                    invalidPlugConnector = foundConnector;
                    break;
                }
            }
            if ( invalidPlugConnectorClassName.equals("PlugListenConnector002_03") ) {
                if ( foundConnector instanceof PlugListenConnector002_03 ) {
                    invalidPlugConnector = foundConnector;
                    break;
                }
            }
        }

        if ( invalidPlugConnector != null ) {
            logOnError(errorLogPrefixHead + "Pluggable connector is created on base of Connector");
            logOnError(errorLogPrefix + "implementation for which instance can not be created.");
            logOnError(errorLogPrefix + "This connector is found out in " + connectorsListName);
            logOnError(errorLogPrefix + "Connector instance = '" + invalidPlugConnector + "'");
            logOnError(errorLogPrefix + "Connector name = '" + invalidPlugConnector.name() + "'");
            checkResult = STATUS_FAILED;
        }


        // check that invalid pluggable connector is NOT contained in allConnectors() List too
        List allConnectorsList = virtualMachineManager.allConnectors();
        int allConnectorsNumber = allConnectorsList.size();

        invalidPlugConnector = null;
        for (int i=0; i < allConnectorsNumber; i++ ) {
            Connector foundConnector = (Connector)allConnectorsList.get(i);
            if ( invalidPlugConnectorClassName.equals("PlugAttachConnector002_03") ) {
                if ( foundConnector instanceof PlugAttachConnector002_03 ) {
                    invalidPlugConnector = foundConnector;
                    break;
                }
            }
            if ( invalidPlugConnectorClassName.equals("PlugLaunchConnector002_03") ) {
                if ( foundConnector instanceof PlugLaunchConnector002_03 ) {
                    invalidPlugConnector = foundConnector;
                    break;
                }
            }
            if ( invalidPlugConnectorClassName.equals("PlugListenConnector002_03") ) {
                if ( foundConnector instanceof PlugListenConnector002_03 ) {
                    invalidPlugConnector = foundConnector;
                    break;
                }
            }
        }

        if ( invalidPlugConnector != null ) {
            logOnError(errorLogPrefixHead + "Pluggable connector is created on base of Connector");
            logOnError(errorLogPrefix + "implementation for which instance can not be created.");
            logOnError(errorLogPrefix + "This connector is found out in allConnectors() List");
            logOnError(errorLogPrefix + "Connector instance = '" + invalidPlugConnector + "'");
            logOnError(errorLogPrefix + "Connector name = '" + invalidPlugConnector.name() + "'");
            checkResult = STATUS_FAILED;
        }

        return checkResult;
    }

} // end of plugMultiConnect002 class
