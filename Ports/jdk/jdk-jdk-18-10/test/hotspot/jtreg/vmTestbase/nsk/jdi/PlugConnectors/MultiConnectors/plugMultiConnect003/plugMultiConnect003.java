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
 * @summary converted from VM Testbase nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect003.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect003 test:
 *     The test for the mechanism for creating pluggable Connectors
 *     on base of classes which implement the TransportService abstract
 *     class (com.sun.jdi.connect.spi.TransportService).
 *     The test checks up that at start-up time when
 *     Bootstrap.virtualMachineManager() is invoked four expected pluggable
 *     connectors (two AttachingConnectors and two ListeningConnectors) are
 *     created  properly on base of two different TransportService
 *     implementations.
 *     The created attaching pluggable connectors have to be contained in Lis
 *     returned by VirtualMachineManager.attachingConnectors().
 *     The created listening pluggable connectors have to be contained in Lis
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
 *      The baee TransportServices are:
 *      PlugTransportService003_01 class which has:
 *          name() = "PlugTransportService003_01_Name"
 *          description() = "PlugTransportService003_01_Description"
 *          capabilities().supportsAcceptTimeout = true
 *          capabilities().supportsAttachTimeout = true
 *          capabilities().supportsHandshakeTimeout = true
 *          capabilities().supportsMultipleConnections = true
 *      PlugTransportService003_02 class which has:
 *          name() = "PlugTransportService003_02_Name"
 *          description() = "PlugTransportService003_02_Description"
 *          capabilities().supportsAcceptTimeout = false
 *          capabilities().supportsAttachTimeout = false
 *          capabilities().supportsHandshakeTimeout = false
 *          capabilities().supportsMultipleConnections = false
 * COMMENTS:
 *     Fixed bug 6426609: nsk/share/jdi/plug_connectors_jar_file.pl should use
 *                        "system" instead of back-quotes
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect003.plugMultiConnect003
 *
 * @comment build connectors.jar to jars
 * @build nsk.share.jdi.PlugConnectors
 * @run driver nsk.jdi.ConnectorsJarBuilder
 *
 * @run main/othervm
 *      -cp jars${file.separator}connectors.jar${path.separator}${test.class.path}${path.separator}${java.class.path}
 *      nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect003.plugMultiConnect003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 */

package nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect003;

import nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect003.connectors.*;

import nsk.share.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.connect.spi.*;
import java.util.*;
import java.io.*;

/**
 * The test for the mechanism for creating pluggable Connectors             <BR>
 * on base of classes which implement the TransportService abstract         <BR>
 * class (com.sun.jdi.connect.spi.TransportService).                        <BR>
 *                                                                          <BR>
 * The test checks up that at start-up time when                            <BR>
 * Bootstrap.virtualMachineManager() is invoked four expected pluggable     <BR>
 * connectors (two AttachingConnectors and two ListeningConnectors) are     <BR>
 * created  properly on base of two different TransportService              <BR>
 * implementations.                                                         <BR>
 *                                                                          <BR>
 * The created attaching pluggable connectors have to be contained in List  <BR>
 * returned by VirtualMachineManager.attachingConnectors().                 <BR>
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
 *  The baee TransportServices are:                                         <BR>
 *  PlugTransportService003_01 class which has:                             <BR>
 *      name() = "PlugTransportService003_01_Name"                          <BR>
 *      description() = "PlugTransportService003_01_Description"            <BR>
 *      capabilities().supportsAcceptTimeout = true                         <BR>
 *      capabilities().supportsAttachTimeout = true                         <BR>
 *      capabilities().supportsHandshakeTimeout = true                      <BR>
 *      capabilities().supportsMultipleConnections = true                   <BR>
 *  PlugTransportService003_02 class which has:                             <BR>
 *      name() = "PlugTransportService003_02_Name"                          <BR>
 *      description() = "PlugTransportService003_02_Description"            <BR>
 *      capabilities().supportsAcceptTimeout = false                        <BR>
 *      capabilities().supportsAttachTimeout = false                        <BR>
 *      capabilities().supportsHandshakeTimeout = false                     <BR>
 *      capabilities().supportsMultipleConnections = false                  <BR>
 *                                                                          <BR>
 */

public class plugMultiConnect003 {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "plugMultiConnect003: ";
    static final String errorLogPrefix     = "                     ";
    static final String infoLogPrefixNead = "--> plugMultiConnect003: ";
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
        int result =  new plugMultiConnect003().runThis(argv, out);
        if ( result == STATUS_FAILED ) {
            logAlways("\n##> nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect003 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect003 test PASSED");
        }
        return result;
    }


    private int runThis (String argv[], PrintStream out) {
        int testResult = STATUS_PASSED;

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);
        logHandler.enableErrorsSummary(false);

        logAlways("==> nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect003 test...");
        logOnVerbose
            ("==> Test checks that expected pluggable connectors are created properly");
        logOnVerbose
            ("==> on base of two different TransportService implementations.");

        int referenceTransportServicesNumber = 2;

        TransportService[] referenceTransportServices
            = new TransportService[referenceTransportServicesNumber];
        referenceTransportServices[0] = new PlugTransportService003_01();
        referenceTransportServices[1] = new PlugTransportService003_02();

        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        if (virtualMachineManager == null) {
            logOnError(errorLogPrefixHead + "Bootstrap.virtualMachineManager() returns null.");
            return STATUS_FAILED;
        }

        for (int i=0; i < referenceTransportServicesNumber; i++ ) {
            int checkResult = checkConnectorsForTransportService(virtualMachineManager,
                                                                referenceTransportServices[i]);
            if ( testResult == STATUS_PASSED ) {
                testResult = checkResult;
            }
        }

        return testResult;
    }

    private int checkConnectorsForTransportService
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

} // end of plugMultiConnect003 class
