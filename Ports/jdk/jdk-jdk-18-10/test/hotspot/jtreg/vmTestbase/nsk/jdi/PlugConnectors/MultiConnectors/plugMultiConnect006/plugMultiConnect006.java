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
 * @summary converted from VM Testbase nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect006.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect006 test:
 *     The test for the mechanism for creating pluggable Connectors
 *     on base of classes which implement the Connector interfaces
 *     (AttachingConnector, ListeningConnector, or LaunchingConnector).
 *     and the TransportService abstract class.
 *     The test checks up that at start-up time when
 *     Bootstrap.virtualMachineManager() is invoked the 6 expected
 *     pluggable connectors are created  properly on base of 6 different
 *     Connector implementations of different types and four expected
 *     pluggable connectors (two AttachingConnectors and two
 *     ListeningConnectors) are created  properly on base of two different
 *     TransportService implementations.
 *     Also the test checks up that at start-up time when
 *     Bootstrap.virtualMachineManager() is invoked the 3 pluggable connectors
 *     are NOT created for Connector implementations for which instances can
 *     not be created and 2 pluggable connectors (AttachingConnector and
 *     ListeningConnector) are NOT created for TransportService implementation
 *     for which instances can not be created.
 *     These "invalid" Connector implementations are:
 *         PlugAttachConnector006_03 class;
 *         PlugLaunchConnector006_03 class;
 *         PlugListenConnector006_03 class;
 *     and "invalid" TransportService implementations is:
 *         PlugTransportService006_03 class;
 *     For connectors created on base of Connector implementations the
 *     test expects that 2 pluggable connectors should be
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
 *     For connectors created on base of TransportService implementations
 *     the test expects that the created attaching pluggable connectors have
 *     to be contained in List returned by
 *     VirtualMachineManager.attachingConnectors().
 *     The created listening pluggable connectors have to be contained in List
 *     returned by VirtualMachineManager.listeningConnectors().
 *     And all four connectors have to be contained in List returned
 *     by VirtualMachineManager.allConnectors() methods.
 *     The attaching pluggable connectors should have names based on the names
 *     of the corresponding base transport services concatenated with the
 *     string "Attach", i.e.
 *         TransportService.name() + "Attach"
 *     Similarly, the listening pluggable connectors names are formed of names
 *     of transport services with string "Listen":
 *         TransportService.name() + "Listen";
 *     All pluggable connectors should have the description conterminous with
 *     the description of the corresponding base transport service
 *     (TransportService001.description()).
 *     All pluggable connectors should have two default arguments named
 *     "address" and "timeout".
 *     In addition the listening pluggable connectorsshould have
 *     'supportsMultipleConnections' capability matching the same base
 *      TransportService capability.
 * COMMENTS:
 *     Fixed bug 6426609: nsk/share/jdi/plug_connectors_jar_file.pl should use
 *                        "system" instead of back-quotes
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect006.plugMultiConnect006
 *
 * @comment build connectors.jar to jars
 * @build nsk.share.jdi.PlugConnectors
 * @run driver nsk.jdi.ConnectorsJarBuilder
 *
 * @run main/othervm
 *      -cp jars${file.separator}connectors.jar${path.separator}${test.class.path}${path.separator}${java.class.path}
 *      nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect006.plugMultiConnect006
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 */

package nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect006;

import nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect006.connectors.*;

import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.connect.spi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the mechanism for creating pluggable Connectors             <BR>
 * on base of classes which implement the Connector interfaces              <BR>
 * (AttachingConnector, ListeningConnector, or LaunchingConnector).         <BR>
 * and the TransportService abstract class.                                 <BR>
 *                                                                          <BR>
 * The test checks up that at start-up time when                            <BR>
 * Bootstrap.virtualMachineManager() is invoked the 6 expected              <BR>
 * pluggable connectors are created  properly on base of 6 different        <BR>
 * Connector implementations of different types and four expected           <BR>
 * pluggable connectors (two AttachingConnectors and two                    <BR>
 * ListeningConnectors) are created  properly on base of two different      <BR>
 * TransportService implementations.                                        <BR>
 *                                                                          <BR>
 * Also the test checks up that at start-up time when                       <BR>
 * Bootstrap.virtualMachineManager() is invoked the 3 pluggable connectors  <BR>
 * are NOT created for Connector implementations for which instances can    <BR>
 * not be created and 2 pluggable connectors (AttachingConnector and        <BR>
 * ListeningConnector) are NOT created for TransportService implementation  <BR>
 * for which instances can not be created.                                  <BR>
 * These "invalid" Connector implementations are:                           <BR>
 *     PlugAttachConnector006_03 class;                                     <BR>
 *     PlugLaunchConnector006_03 class;                                     <BR>
 *     PlugListenConnector006_03 class;                                     <BR>
 * and "invalid" TransportService implementations is:                       <BR>
 *     PlugTransportService006_03 class;                                    <BR>
 *                                                                          <BR>
 * For connectors created on base of Connector implementations the          <BR>
 * test expects that 2 pluggable connectors should be                       <BR>
 * of AttachingConnector type, other 2 connectors -                         <BR>
 * of ListeningConnector type, and last 2 coonectors -                      <BR>
 * of LaunchingConnector type                                               <BR>
 *                                                                          <BR>
 * Each pluggable connector has to be contained in corresponding list:      <BR>
 * VirtualMachineManager.attachingConnectors(),                             <BR>
 * VirtualMachineManager.launchingConnectors() or                           <BR>
 * VirtualMachineManager.listeningConnectors().                             <BR>
 *                                                                          <BR>
 * All 6 pluggable connectors have to be contained in                       <BR>
 * VirtualMachineManager.allConnectors() list.                              <BR>
 *                                                                          <BR>
 * Each pluggable connector is checked for the certain expected             <BR>
 * description and for the certain expected transport.                      <BR>
 *                                                                          <BR>
 * For each connector type one pluggable connector should have the          <BR>
 * empty Map of default arguments, and other pluggable connector            <BR>
 * should have 4 default arguments of all types:                            <BR>
 *      Connector.StringArgument;                                           <BR>
 *      Connector.IntegerArgument;                                          <BR>
 *      Connector.BooleanArgument;                                          <BR>
 *      Connector.SelectedArgument;                                         <BR>
 *                                                                          <BR>
 * For connectors created on base of TransportService implementations       <BR>
 * the test expects that the created attaching pluggable connectors have    <BR>
 * to be contained in List returned by                                      <BR>
 * VirtualMachineManager.attachingConnectors().                             <BR>
 * The created listening pluggable connectors have to be contained in List  <BR>
 * returned by VirtualMachineManager.listeningConnectors().                 <BR>
 * And all four connectors have to be contained in List returned            <BR>
 * by VirtualMachineManager.allConnectors() methods.                        <BR>
 *                                                                          <BR>
 * The attaching pluggable connectors should have names based on the names  <BR>
 * of the corresponding base transport services concatenated with the       <BR>
 * string "Attach", i.e.                                                    <BR>
 *     TransportService.name() + "Attach"                                   <BR>
 * Similarly, the listening pluggable connectors names are formed of names  <BR>
 * of transport services with string "Listen":                              <BR>
 *     TransportService.name() + "Listen";                                  <BR>
 *                                                                          <BR>
 * All pluggable connectors should have the description conterminous with   <BR>
 * the description of the corresponding base transport service              <BR>
 * (TransportService001.description()).                                     <BR>
 *                                                                          <BR>
 * All pluggable connectors should have two default arguments named         <BR>
 * "address" and "timeout".                                                 <BR>
 *                                                                          <BR>
 * In addition the listening pluggable connectorsshould have                <BR>
 * 'supportsMultipleConnections' capability matching the same base          <BR>
 *  TransportService capability.                                            <BR>
 *                                                                          <BR>
 */

public class plugMultiConnect006 {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "plugMultiConnect006: ";
    static final String errorLogPrefix     = "                     ";
    static final String infoLogPrefixNead = "--> plugMultiConnect006: ";
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
        int result =  new plugMultiConnect006().runThis(argv, out);
        if ( result == STATUS_FAILED ) {
            logAlways("\n##> nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect006 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect006 test PASSED");
        }
        return result;
    }


    private int runThis (String argv[], PrintStream out) {
        int testResult = STATUS_PASSED;

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);
        logHandler.enableErrorsSummary(false);

        logAlways("==> nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect006 test...");
        logOnVerbose
            ("==> Test checks that expected pluggable connectors are created properly");
        logOnVerbose
            ("==> on base of 6 different Connector implementations and of 2 different");
        logOnVerbose
            ("==> TransportService implementations.");
        logOnVerbose
            ("==> But other pluggable connectors are NOT created for Connector and ");
        logOnVerbose
            ("==> TransportService implementations for which instances can not be created.\n");
        int expectedConnectorsNumber = 6;

        String[] checkedPlugConnectorNames = new String[expectedConnectorsNumber];
        checkedPlugConnectorNames[0] = "PlugAttachConnector006_01_Name";
        checkedPlugConnectorNames[1] = "PlugAttachConnector006_02_Name";
        checkedPlugConnectorNames[2] = "PlugLaunchConnector006_01_Name";
        checkedPlugConnectorNames[3] = "PlugLaunchConnector006_02_Name";
        checkedPlugConnectorNames[4] = "PlugListenConnector006_01_Name";
        checkedPlugConnectorNames[5] = "PlugListenConnector006_02_Name";

        Connector[] referencePlugConnectors = new Connector[expectedConnectorsNumber];
        referencePlugConnectors[0] = new PlugAttachConnector006_01();
        referencePlugConnectors[1] = new PlugAttachConnector006_02();
        referencePlugConnectors[2] = new PlugLaunchConnector006_01();
        referencePlugConnectors[3] = new PlugLaunchConnector006_02();
        referencePlugConnectors[4] = new PlugListenConnector006_01();
        referencePlugConnectors[5] = new PlugListenConnector006_02();

        int referenceTransportServicesNumber = 2;

        TransportService[] referenceTransportServices
            = new TransportService[referenceTransportServicesNumber];
        referenceTransportServices[0] = new PlugTransportService006_01();
        referenceTransportServices[1] = new PlugTransportService006_02();

        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        if (virtualMachineManager == null) {
            logOnError(errorLogPrefixHead + "Bootstrap.virtualMachineManager() returns null.");
            return STATUS_FAILED;
        }

        for (int i=0; i < expectedConnectorsNumber; i++ ) {
            int checkResult = checkValidConnector(virtualMachineManager,
                                            checkedPlugConnectorNames[i],
                                            referencePlugConnectors[i]);
            if ( testResult == STATUS_PASSED ) {
                testResult = checkResult;
            }
        }

        for (int i=0; i < referenceTransportServicesNumber; i++ ) {
            int checkResult = checkConnectorsForValidTransportService(virtualMachineManager,
                                                                referenceTransportServices[i]);
            if ( testResult == STATUS_PASSED ) {
                testResult = checkResult;
            }
        }

        int invalidConnectorsNumber = 3;

        String[] invalidPlugConnectorClassesNames = new String[invalidConnectorsNumber];
        invalidPlugConnectorClassesNames[0] = "PlugAttachConnector006_03";
        invalidPlugConnectorClassesNames[1] = "PlugLaunchConnector006_03";
        invalidPlugConnectorClassesNames[2] = "PlugListenConnector006_03";

        int invalidTransportServicesNumber = 1;

        String[] invalidTransportServicesNames
            = new String[invalidTransportServicesNumber];
        invalidTransportServicesNames[0] = "PlugTransportService006_03_Name";

        for (int i=0; i < invalidConnectorsNumber; i++ ) {
            int checkResult = checkForInvalidConnector(virtualMachineManager,
                                                  invalidPlugConnectorClassesNames[i]);
            if ( testResult == STATUS_PASSED ) {
                testResult = checkResult;
            }
        }

        for (int i=0; i < invalidTransportServicesNumber; i++ ) {
            int checkResult = checkConnectorsForInvalidTransportService(virtualMachineManager,
                                                                        invalidTransportServicesNames[i]);
            if ( testResult == STATUS_PASSED ) {
                testResult = checkResult;
            }
        }

        return testResult;
    }

    private int checkValidConnector (VirtualMachineManager virtualMachineManager,
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

    private int checkConnectorsForValidTransportService
            (VirtualMachineManager virtualMachineManager,
             TransportService referenceTransportService) {

        int checkResult = STATUS_PASSED;

        String checkedPlugAttachConnectorName = referenceTransportService.name() + "Attach";
        String checkedPlugListenConnectorName = referenceTransportService.name() + "Listen";
        String checkedPlugConnectorsDescription = referenceTransportService.description();
        String addressArgumentName = "address";
        String timeoutArgumentName = "timeout";
        boolean expectedMultipleConnectionsCapability
            = referenceTransportService.capabilities().supportsMultipleConnections();

        // check that expected pluggable attaching connector is found out in attachingConnectors() List
        List attachingConnectorsList = virtualMachineManager.attachingConnectors();
        int attachingConnectorsNumber = attachingConnectorsList.size();
        AttachingConnector checkedPlugAttachConnector = null;

        for (int i=0; i < attachingConnectorsNumber; i++ ) {
            AttachingConnector attachingConnector = (AttachingConnector)attachingConnectorsList.get(i);
            String attachConnectorName = attachingConnector.name();
            if ( checkedPlugAttachConnectorName.equals(attachConnectorName) ) {
                if ( checkedPlugAttachConnector != null ) {
                    logOnError(errorLogPrefixHead
                        + "One more pluggable attaching connector with the same name is found out.");
                    logOnError(errorLogPrefix + "Connector name = '" + checkedPlugAttachConnectorName + "'");
                    logOnError(errorLogPrefix + "First Connector = '" + checkedPlugAttachConnector + "'");
                    logOnError(errorLogPrefix + "Found Connector = '" + attachingConnector + "'");
                    checkResult = STATUS_FAILED;
                } else {
                    checkedPlugAttachConnector = attachingConnector;
                }
            }
        }

        if ( checkedPlugAttachConnector == null ) {
            logOnError(errorLogPrefixHead + "Expected pluggable attaching connector is NOT found out.");
            logOnError(errorLogPrefix + "Expected connector name = '" + checkedPlugAttachConnectorName + "'");
            checkResult = STATUS_FAILED;
        }

        // check that expected pluggable listening connector is found out in listeningConnectors() List
        List listeningConnectorsList = virtualMachineManager.listeningConnectors();
        int listeningConnectorsNumber = listeningConnectorsList.size();
        ListeningConnector checkedPlugListenConnector = null;

        for (int i=0; i < listeningConnectorsNumber; i++ ) {
            ListeningConnector listeningConnector = (ListeningConnector)listeningConnectorsList.get(i);
            String listenConnectorName = listeningConnector.name();
            if ( checkedPlugListenConnectorName.equals(listenConnectorName) ) {
                if ( checkedPlugListenConnector != null ) {
                    logOnError(errorLogPrefixHead
                        + "One more pluggable listening connector with the same name is found out.");
                    logOnError(errorLogPrefix + "Connector name = '" + checkedPlugListenConnectorName + "'");
                    logOnError(errorLogPrefix + "First Connector = '" + checkedPlugListenConnector + "'");
                    logOnError(errorLogPrefix + "Found Connector = '" + listeningConnector + "'");
                    checkResult = STATUS_FAILED;
                } else {
                    checkedPlugListenConnector = listeningConnector;
                }
            }
        }

        if ( checkedPlugListenConnector == null ) {
            logOnError(errorLogPrefixHead + "Expected pluggable listening connector is NOT found out.");
            logOnError(errorLogPrefix + "Expected connector name = '" + checkedPlugListenConnectorName + "'");
            checkResult = STATUS_FAILED;
        }

        List allConnectorsList = virtualMachineManager.allConnectors();
        int allConnectorsNumber = allConnectorsList.size();
        if ( checkedPlugAttachConnector != null ) {
            // check that expected pluggable attaching connector is found out in allConnectors() List too
            boolean checkedPlugAttachConnectorFound = false;
            for (int i=0; i < allConnectorsNumber; i++ ) {
                Connector foundConnector = (Connector)allConnectorsList.get(i);
                if ( foundConnector instanceof AttachingConnector ) {
                    AttachingConnector foundAttachingConnector = (AttachingConnector)foundConnector;
                    if ( checkedPlugAttachConnector.equals(foundAttachingConnector) ) {
                        if ( checkedPlugAttachConnectorFound ) {
                            logOnError(errorLogPrefixHead
                                + "One more pluggable attaching connector with the same name "
                                + "is found out in allConnectors() List.");
                            logOnError(errorLogPrefix + "Connector name = '" + checkedPlugAttachConnectorName + "'");
                            logOnError(errorLogPrefix + "First Connector = '" + checkedPlugAttachConnector + "'");
                            logOnError(errorLogPrefix + "Found Connector = '" + foundAttachingConnector + "'");
                            checkResult = STATUS_FAILED;
                        } else {
                            checkedPlugAttachConnectorFound = true;
                        }
                    }
                }
            }

            if ( ! checkedPlugAttachConnectorFound ) {
                logOnError(errorLogPrefixHead
                    + "Expected pluggable attaching connector is NOT found out in allConnectors() List");
                logOnError(errorLogPrefix + "Expected connector = " + checkedPlugAttachConnector);
                checkResult = STATUS_FAILED;
            }

            // check that expected pluggable attaching connector has expected description
            String actualDescription = checkedPlugAttachConnector.description();
            if ( ! checkedPlugConnectorsDescription.equals(actualDescription) ) {
                logOnError(errorLogPrefixHead + "Pluggable attaching connector has unexpected descripton:");
                logOnError(errorLogPrefix + "Attaching connector = '" + checkedPlugAttachConnector + "'");
                logOnError(errorLogPrefix + "Expected descripton = '" + checkedPlugConnectorsDescription + "'");
                logOnError(errorLogPrefix + "Actual descripton = '" + actualDescription + "'");
                checkResult = STATUS_FAILED;
            }

            // check that expected pluggable attaching connector has default argument 'address'
            // and this argument is of Connector.StringArgument type
            Connector.Argument addressArgument = PlugConnectors.getConnectorDefaultArgument
                (checkedPlugAttachConnector, addressArgumentName);
            if ( addressArgument == null ) {
                logOnError(errorLogPrefixHead + "Pluggable attaching connector has NOT address argument:");
                logOnError(errorLogPrefix + "Attaching connector = '" + checkedPlugAttachConnector + "'");
                logOnError(errorLogPrefix + "Expected argument name = '" + addressArgumentName + "'");
                checkResult = STATUS_FAILED;
            } else {
                if ( ! (addressArgument instanceof Connector.StringArgument) ) {
                    logOnError(errorLogPrefixHead +
                        "Address argument of Pluggable attaching connector is NOT of Connector.StringArgument type");
                    logOnError(errorLogPrefix + "Attaching connector = '" + checkedPlugAttachConnector + "'");
                    checkResult = STATUS_FAILED;
                }
            }

            // check that expected pluggable attaching connector has default argument 'timeout'
            // and this argument is of Connector.IntegerArgument type
            Connector.Argument timeoutArgument = PlugConnectors.getConnectorDefaultArgument
                (checkedPlugAttachConnector, timeoutArgumentName);
            if ( timeoutArgument == null ) {
                logOnError(errorLogPrefixHead + "Pluggable attaching connector has NOT timeout argument:");
                logOnError(errorLogPrefix + "Attaching connector = '" + checkedPlugAttachConnector + "'");
                logOnError(errorLogPrefix + "Expected argument name = '" + timeoutArgumentName + "'");
                checkResult = STATUS_FAILED;
            } else {
                if ( ! (timeoutArgument instanceof Connector.IntegerArgument) ) {
                    logOnError(errorLogPrefixHead +
                        "Timeout argument of Pluggable attaching connector is NOT of Connector.IntegerArgument type");
                    logOnError(errorLogPrefix + "Attaching connector = '" + checkedPlugAttachConnector + "'");
                    checkResult = STATUS_FAILED;
                }
            }

        }

        if ( checkedPlugListenConnector != null ) {
            // check that expected pluggable listening connector is found out in allConnectors() List too
            boolean checkedPlugListenConnectorFound = false;
            for (int i=0; i < allConnectorsNumber; i++ ) {
                Connector foundConnector = (Connector)allConnectorsList.get(i);
                if ( foundConnector instanceof ListeningConnector ) {
                    ListeningConnector foundListeningConnector = (ListeningConnector)foundConnector;
                    if ( checkedPlugListenConnector.equals(foundListeningConnector) ) {
                        if ( checkedPlugListenConnectorFound ) {
                            logOnError(errorLogPrefixHead
                                + "One more pluggable listening connector with the same name "
                                + "is found out in allConnectors() List.");
                            logOnError(errorLogPrefix + "Connector name = '" + checkedPlugListenConnectorName + "'");
                            logOnError(errorLogPrefix + "First Connector = '" + checkedPlugListenConnector + "'");
                            logOnError(errorLogPrefix + "Found Connector = '" + foundListeningConnector + "'");
                            checkResult = STATUS_FAILED;
                        } else {
                            checkedPlugListenConnectorFound = true;
                        }
                    }
                }
            }

            if ( ! checkedPlugListenConnectorFound ) {
                logOnError(errorLogPrefixHead
                    + "Expected pluggable listening connector is NOT found out in allConnectors() List");
                logOnError(errorLogPrefix + "Expected connector = " + checkedPlugListenConnector);
                checkResult = STATUS_FAILED;
            }

            // check that expected pluggable listenhing connector has expected description
            String actualDescription = checkedPlugListenConnector.description();
            if ( ! checkedPlugConnectorsDescription.equals(actualDescription) ) {
                logOnError(errorLogPrefixHead + "Pluggable listening connector has unexpected descripton:");
                logOnError(errorLogPrefix + "Listening connector = '" + checkedPlugListenConnector + "'");
                logOnError(errorLogPrefix + "Expected descripton = '" + checkedPlugConnectorsDescription + "'");
                logOnError(errorLogPrefix + "Actual descripton = '" + actualDescription + "'");
                checkResult = STATUS_FAILED;
            }

            // check that expected pluggable listening connector has default argument 'address'
            // and this argument is of Connector.StringArgument type
            Connector.Argument addressArgument = PlugConnectors.getConnectorDefaultArgument
                (checkedPlugListenConnector, addressArgumentName);
            if ( addressArgument == null ) {
                logOnError(errorLogPrefixHead + "Pluggable listening connector has NOT address argument:");
                logOnError(errorLogPrefix + "Listening connector = '" + checkedPlugListenConnector + "'");
                logOnError(errorLogPrefix + "Expected argument name = '" + addressArgumentName + "'");
                checkResult = STATUS_FAILED;
            } else {
                if ( ! (addressArgument instanceof Connector.StringArgument) ) {
                    logOnError(errorLogPrefixHead +
                        "Address argument of Pluggable listening connector is NOT of Connector.StringArgument type");
                    logOnError(errorLogPrefix + "Listening connector = '" + checkedPlugListenConnector + "'");
                    checkResult = STATUS_FAILED;
                }
            }

            // check that expected pluggable listening connector has default argument 'timeout'
            // and this argument is of Connector.IntegerArgument type
            Connector.Argument timeoutArgument = PlugConnectors.getConnectorDefaultArgument
                (checkedPlugListenConnector, timeoutArgumentName);
            if ( timeoutArgument == null ) {
                logOnError(errorLogPrefixHead + "Pluggable listening connector has NOT timeout argument:");
                logOnError(errorLogPrefix + "Listening connector = '" + checkedPlugListenConnector + "'");
                logOnError(errorLogPrefix + "Expected argument name = '" + timeoutArgumentName + "'");
                checkResult = STATUS_FAILED;
            } else {
                if ( ! (timeoutArgument instanceof Connector.IntegerArgument) ) {
                    logOnError(errorLogPrefixHead +
                        "Timeout argument of Pluggable listening connector is NOT of Connector.IntegerArgument type");
                    logOnError(errorLogPrefix + "Listening connector = '" + checkedPlugListenConnector + "'");
                    checkResult = STATUS_FAILED;
                }
            }

            // check that expected pluggable listening connector has 'supportsMultipleConnections'
            // capability matching the same base TransportService capability
            boolean actualMultipleConnectionsCapability = checkedPlugListenConnector.supportsMultipleConnections();
            if ( actualMultipleConnectionsCapability != expectedMultipleConnectionsCapability ) {
                logOnError(errorLogPrefixHead + "Pluggable listening connector has 'supportsMultipleConnections'");
                logOnError(errorLogPrefix + "capability NOT matching the same base TransportService capability:");
                logOnError(errorLogPrefix + "Listening connector = '" + checkedPlugListenConnector + "'");
                logOnError(errorLogPrefix + "Expected 'supportsMultipleConnections' capability = "
                    + expectedMultipleConnectionsCapability);
                logOnError(errorLogPrefix + "Actual 'supportsMultipleConnections' capability = "
                    + actualMultipleConnectionsCapability);
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
        if ( invalidPlugConnectorClassName.equals("PlugAttachConnector006_03") ) {
            connectorsList = virtualMachineManager.attachingConnectors();
            connectorsListName = "attachingConnectors() List";
        }
        if ( invalidPlugConnectorClassName.equals("PlugLaunchConnector006_03") ) {
            connectorsList = virtualMachineManager.launchingConnectors();
            connectorsListName = "launchingConnectors() List";
        }
        if ( invalidPlugConnectorClassName.equals("PlugListenConnector006_03") ) {
            connectorsList = virtualMachineManager.listeningConnectors();
            connectorsListName = "listeningConnectors() List";
        }
        int connectorsNumber = connectorsList.size();

        // check that pluggable connector is NOT created on base of Connector
        // implementation for which instance can not be created
        Connector invalidPlugConnector = null;

        for (int i=0; i < connectorsNumber; i++ ) {
            Connector foundConnector = (Connector)connectorsList.get(i);
            if ( invalidPlugConnectorClassName.equals("PlugAttachConnector006_03") ) {
                if ( foundConnector instanceof PlugAttachConnector006_03 ) {
                    invalidPlugConnector = foundConnector;
                    break;
                }
            }
            if ( invalidPlugConnectorClassName.equals("PlugLaunchConnector006_03") ) {
                if ( foundConnector instanceof PlugLaunchConnector006_03 ) {
                    invalidPlugConnector = foundConnector;
                    break;
                }
            }
            if ( invalidPlugConnectorClassName.equals("PlugListenConnector006_03") ) {
                if ( foundConnector instanceof PlugListenConnector006_03 ) {
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
            if ( invalidPlugConnectorClassName.equals("PlugAttachConnector006_03") ) {
                if ( foundConnector instanceof PlugAttachConnector006_03 ) {
                    invalidPlugConnector = foundConnector;
                    break;
                }
            }
            if ( invalidPlugConnectorClassName.equals("PlugLaunchConnector006_03") ) {
                if ( foundConnector instanceof PlugLaunchConnector006_03 ) {
                    invalidPlugConnector = foundConnector;
                    break;
                }
            }
            if ( invalidPlugConnectorClassName.equals("PlugListenConnector006_03") ) {
                if ( foundConnector instanceof PlugListenConnector006_03 ) {
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

    private int checkConnectorsForInvalidTransportService
            (VirtualMachineManager virtualMachineManager,
             String invalidTransportServiceName) {

        int checkResult = STATUS_PASSED;

        String checkedPlugAttachConnectorName = invalidTransportServiceName + "Attach";
        String checkedPlugListenConnectorName = invalidTransportServiceName + "Listen";

        // check that pluggable attaching connector is NOT created on base of TransportService
        // implementation for which instance can not be created
        List attachingConnectorsList = virtualMachineManager.attachingConnectors();
        int attachingConnectorsNumber = attachingConnectorsList.size();
        AttachingConnector checkedPlugAttachConnector = null;

        for (int i=0; i < attachingConnectorsNumber; i++ ) {
            AttachingConnector attachingConnector = (AttachingConnector)attachingConnectorsList.get(i);
            String attachConnectorName = attachingConnector.name();
            if ( checkedPlugAttachConnectorName.equals(attachConnectorName) ) {
                checkedPlugAttachConnector = attachingConnector;
                break;
            }
        }

        if ( checkedPlugAttachConnector != null ) {
            logOnError(errorLogPrefixHead + "Pluggable attaching connector is created for TransportService");
            logOnError(errorLogPrefix + "implementation for which instance can not be created.");
            logOnError(errorLogPrefix + "This connector is found out in attachingConnectors() list");
            logOnError(errorLogPrefix + "Connector instance = '" + checkedPlugAttachConnector + "'");
            logOnError(errorLogPrefix + "Connector name = '" + checkedPlugAttachConnectorName + "'");
            checkResult = STATUS_FAILED;
        }

        // check that pluggable listening connector is NOT created on base of TransportService
        // implementation for which instance can not be created
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

        if ( checkedPlugListenConnector != null ) {
            logOnError(errorLogPrefixHead + "Pluggable listening connector is created for TransportService");
            logOnError(errorLogPrefix + "implementation for which instance can not be created.");
            logOnError(errorLogPrefix + "This connector is found out in listeningConnectors() list");
            logOnError(errorLogPrefix + "Connector instance = '" + checkedPlugListenConnector + "'");
            logOnError(errorLogPrefix + "Connector name = '" + checkedPlugListenConnectorName + "'");
            checkResult = STATUS_FAILED;
        }

        // check that pluggable connectors are NOT contained in allConnectors() List too
        List allConnectorsList = virtualMachineManager.allConnectors();
        int allConnectorsNumber = allConnectorsList.size();

        Connector attachConnector = null;
        Connector listenConnector = null;
        for (int i=0; i < allConnectorsNumber; i++ ) {
            Connector connector = (Connector)allConnectorsList.get(i);
            String connectorName = connector.name();
            if ( checkedPlugAttachConnectorName.equals(connectorName) ) {
                attachConnector = connector;
            }
            if ( checkedPlugListenConnectorName.equals(connectorName) ) {
                listenConnector = connector;
            }
        }

        if ( attachConnector != null ) {
            logOnError(errorLogPrefixHead + "Pluggable attaching connector is created for TransportService");
            logOnError(errorLogPrefix + "implementation for which instance can not be created.");
            logOnError(errorLogPrefix + "This connector is found out in allConnectors() list");
            logOnError(errorLogPrefix + "Connector instance = '" + attachConnector + "'");
            logOnError(errorLogPrefix + "Connector name = '" + checkedPlugAttachConnectorName + "'");
            checkResult = STATUS_FAILED;
        }

        if ( listenConnector != null ) {
            logOnError(errorLogPrefixHead + "Pluggable listening connector is created for TransportService");
            logOnError(errorLogPrefix + "implementation for which instance can not be created.");
            logOnError(errorLogPrefix + "This connector is found out in allConnectors() list");
            logOnError(errorLogPrefix + "Connector instance = '" + listenConnector + "'");
            logOnError(errorLogPrefix + "Connector name = '" + checkedPlugListenConnectorName + "'");
            checkResult = STATUS_FAILED;
        }

        return checkResult;
    }

} // end of plugMultiConnect006 class
