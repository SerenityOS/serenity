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
 * @summary converted from VM Testbase nsk/jdi/PlugConnectors/TransportService/transportService002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     nsk/jdi/PlugConnectors/TransportService/transportService002 test:
 *     The test for the mechanism for creating pluggable Connectors
 *     on base of classes which implement the TransportService abstract
 *     class (com.sun.jdi.connect.spi.TransportService).
 *     The test checks up that at start-up time when
 *     Bootstrap.virtualMachineManager() is invoked two pluggable
 *     connectors (AttachingConnector and ListeningConnector) are created
 *     on base of PlugTransportService002 class which extends
 *     com.sun.jdi.connect.spi.TransportService abstract class
 *     Base PlugTransportService002 has:
 *      TransportService.name() =  "PlugTransportService002_Name"
 *      TransportService.description() =  "PlugTransportService002_Description"
 *      TransportService.capabilities().supportsAcceptTimeout = false
 *      TransportService.capabilities().supportsAttachTimeout = false
 *      TransportService.capabilities().supportsHandshakeTimeout = false
 *      TransportService.capabilities().supportsMultipleConnections = false
 *     The created attaching pluggable connector has to be contained in list
 *     returned by VirtualMachineManager.attachingConnectors().
 *     The created listening pluggable connector has to be contained in list
 *     returned by VirtualMachineManager.listeningConnectors().
 *     And both connectors have to be contained in list returned
 *     by VirtualMachineManager.allConnectors() methods.
 *     The attaching pluggable connector has to have name
 *         PlugTransportService002.name() + "Attach";
 *     The listening pluggable connector has to have name
 *         PlugTransportService002.name() + "Listen";
 *     Both pluggable connectors have to have description
 *         PlugTransportService002.description();
 *     Both pluggable connectors have to have two default arguments named
 *     "address" and "timeout".
 *     In addition the listening pluggable connector has to have
 *     'supportsMultipleConnections' capability matching the same base
 *      PlugTransportService002 capability, i.e. = false.
 * COMMENTS:
 *     Fixed bug 6426609: nsk/share/jdi/plug_connectors_jar_file.pl should use
 *                        "system" instead of back-quotes
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.PlugConnectors.TransportService.transportService002.transportService002
 *
 * @comment build connectors.jar to jars
 * @build nsk.share.jdi.PlugConnectors
 * @run driver nsk.jdi.ConnectorsJarBuilder
 *
 * @run main/othervm
 *      -cp jars${file.separator}connectors.jar${path.separator}${test.class.path}${path.separator}${java.class.path}
 *      nsk.jdi.PlugConnectors.TransportService.transportService002.transportService002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 */

package nsk.jdi.PlugConnectors.TransportService.transportService002;

import nsk.jdi.PlugConnectors.TransportService.transportService002.connectors.*;

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
 * Bootstrap.virtualMachineManager() is invoked two pluggable               <BR>
 * connectors (AttachingConnector and ListeningConnector) are created       <BR>
 * on base of PlugTransportService002 class which extends                   <BR>
 * com.sun.jdi.connect.spi.TransportService abstract class                  <BR>
 *                                                                          <BR>
 * Base PlugTransportService002 has:                                        <BR>
 *  TransportService.name() =  "PlugTransportService002_Name"               <BR>
 *  TransportService.description() =  "PlugTransportService002_Description" <BR>
 *  TransportService.capabilities().supportsAcceptTimeout = false           <BR>
 *  TransportService.capabilities().supportsAttachTimeout = false           <BR>
 *  TransportService.capabilities().supportsHandshakeTimeout = false        <BR>
 *  TransportService.capabilities().supportsMultipleConnections = false     <BR>
 *                                                                          <BR>
 * The created attaching pluggable connector has to be contained in list    <BR>
 * returned by VirtualMachineManager.attachingConnectors().                 <BR>
 * The created listening pluggable connector has to be contained in list    <BR>
 * returned by VirtualMachineManager.listeningConnectors().                 <BR>
 * And both connectors have to be contained in list returned                <BR>
 * by VirtualMachineManager.allConnectors() methods.                        <BR>
 *                                                                          <BR>
 * The attaching pluggable connector has to have name                       <BR>
 *     PlugTransportService002.name() + "Attach";                           <BR>
 * The listening pluggable connector has to have name                       <BR>
 *     PlugTransportService002.name() + "Listen";                           <BR>
 * Both pluggable connectors have to have description                       <BR>
 *     PlugTransportService002.description();                               <BR>
 * Both pluggable connectors have to have two default arguments named       <BR>
 * "address" and "timeout".                                                 <BR>
 * In addition the listening pluggable connector has to have                <BR>
 * 'supportsMultipleConnections' capability matching the same base          <BR>
 *  PlugTransportService002 capability, i.e. = false.                       <BR>
 *                                                                          <BR>
 */

public class transportService002 {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "transportService002: ";
    static final String errorLogPrefix     = "                     ";
    static final String infoLogPrefixNead = "--> transportService002: ";
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
        int result =  new transportService002().runThis(argv, out);
        if ( result == STATUS_FAILED ) {
            logAlways("\n##> nsk/jdi/PlugConnectors/TransportService/transportService002 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/PlugConnectors/TransportService/transportService002 test PASSED");
        }
        return result;
    }


    private int runThis (String argv[], PrintStream out) {
        int testResult = STATUS_PASSED;

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);
        logHandler.enableErrorsSummary(false);

        logAlways("==> nsk/jdi/PlugConnectors/TransportService/transportService002 test...");
        logOnVerbose
            ("==> Test checks that expected pluggable attaching and listening connectors are created properly.");


        VirtualMachineManager virtualMachineManager = Bootstrap.virtualMachineManager();
        if (virtualMachineManager == null) {
            logOnError(errorLogPrefixHead + "Bootstrap.virtualMachineManager() returns null.");
            return STATUS_FAILED;
        }

        TransportService referenceTransportService = new PlugTransportService002();
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
                checkedPlugAttachConnector = attachingConnector;
                break;
            }
        }

        if ( checkedPlugAttachConnector == null ) {
            logOnError(errorLogPrefixHead + "Expected pluggable attaching connector is NOT found out.");
            logOnError(errorLogPrefix + "Expected connector name = '" + checkedPlugAttachConnectorName + "'");
            testResult = STATUS_FAILED;
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
            testResult = STATUS_FAILED;
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
                        checkedPlugAttachConnectorFound = true;
                        break;
                    }
                }
            }

            if ( ! checkedPlugAttachConnectorFound ) {
                logOnError(errorLogPrefixHead
                    + "Expected pluggable attaching connector is NOT found out in allConnectors() List");
                logOnError(errorLogPrefix + "Expected connector = " + checkedPlugAttachConnector);
                testResult = STATUS_FAILED;
            }

            // check that expected pluggable attaching connector has expected description
            String actualDescription = checkedPlugAttachConnector.description();
            if ( ! checkedPlugConnectorsDescription.equals(actualDescription) ) {
                logOnError(errorLogPrefixHead + "Pluggable attaching connector has unexpected descripton:");
                logOnError(errorLogPrefix + "Expected descripton = '" + checkedPlugConnectorsDescription + "'");
                logOnError(errorLogPrefix + "Actual descripton = '" + actualDescription + "'");
                testResult = STATUS_FAILED;
            }

            // check that expected pluggable attaching connector has default argument 'address'
            // and this argument is of Connector.StringArgument type
            Connector.Argument addressArgument = PlugConnectors.getConnectorDefaultArgument
                (checkedPlugAttachConnector, addressArgumentName);
            if ( addressArgument == null ) {
                logOnError(errorLogPrefixHead + "Pluggable attaching connector has NOT address argument:");
                logOnError(errorLogPrefix + "Expected argument name = '" + addressArgumentName + "'");
                testResult = STATUS_FAILED;
            } else {
                if ( ! (addressArgument instanceof Connector.StringArgument) ) {
                    logOnError(errorLogPrefixHead +
                        "Address argument of Pluggable attaching connector is NOT of Connector.StringArgument type");
                    testResult = STATUS_FAILED;
                }
            }

            // check that expected pluggable attaching connector has default argument 'timeout'
            // and this argument is of Connector.IntegerArgument type
            Connector.Argument timeoutArgument = PlugConnectors.getConnectorDefaultArgument
                (checkedPlugAttachConnector, timeoutArgumentName);
            if ( timeoutArgument == null ) {
                logOnError(errorLogPrefixHead + "Pluggable attaching connector has NOT timeout argument:");
                logOnError(errorLogPrefix + "Expected argument name = '" + timeoutArgumentName + "'");
                testResult = STATUS_FAILED;
            } else {
                if ( ! (timeoutArgument instanceof Connector.IntegerArgument) ) {
                    logOnError(errorLogPrefixHead +
                        "Timeout argument of Pluggable attaching connector is NOT of Connector.IntegerArgument type");
                    testResult = STATUS_FAILED;
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

            // check that expected pluggable listenhing connector has expected description
            String actualDescription = checkedPlugListenConnector.description();
            if ( ! checkedPlugConnectorsDescription.equals(actualDescription) ) {
                logOnError(errorLogPrefixHead + "Pluggable listening connector has unexpected descripton:");
                logOnError(errorLogPrefix + "Expected descripton = '" + checkedPlugConnectorsDescription + "'");
                logOnError(errorLogPrefix + "Actual descripton = '" + actualDescription + "'");
                testResult = STATUS_FAILED;
            }

            // check that expected pluggable listening connector has default argument 'address'
            // and this argument is of Connector.StringArgument type
            Connector.Argument addressArgument = PlugConnectors.getConnectorDefaultArgument
                (checkedPlugListenConnector, addressArgumentName);
            if ( addressArgument == null ) {
                logOnError(errorLogPrefixHead + "Pluggable listening connector has NOT address argument:");
                logOnError(errorLogPrefix + "Expected argument name = '" + addressArgumentName + "'");
                testResult = STATUS_FAILED;
            } else {
                if ( ! (addressArgument instanceof Connector.StringArgument) ) {
                    logOnError(errorLogPrefixHead +
                        "Address argument of Pluggable listening connector is NOT of Connector.StringArgument type");
                    testResult = STATUS_FAILED;
                }
            }

            // check that expected pluggable listening connector has default argument 'timeout'
            // and this argument is of Connector.IntegerArgument type
            Connector.Argument timeoutArgument = PlugConnectors.getConnectorDefaultArgument
                (checkedPlugListenConnector, timeoutArgumentName);
            if ( timeoutArgument == null ) {
                logOnError(errorLogPrefixHead + "Pluggable listening connector has NOT timeout argument:");
                logOnError(errorLogPrefix + "Expected argument name = '" + timeoutArgumentName + "'");
                testResult = STATUS_FAILED;
            } else {
                if ( ! (timeoutArgument instanceof Connector.IntegerArgument) ) {
                    logOnError(errorLogPrefixHead +
                        "Timeout argument of Pluggable listening connector is NOT of Connector.IntegerArgument type");
                    testResult = STATUS_FAILED;
                }
            }

            // check that expected pluggable listening connector has 'supportsMultipleConnections'
            // capability matching the same base TransportService capability, i.e. = false
            boolean actualMultipleConnectionsCapability = checkedPlugListenConnector.supportsMultipleConnections();
            if ( actualMultipleConnectionsCapability != expectedMultipleConnectionsCapability ) {
                logOnError(errorLogPrefixHead + "Pluggable listening connector has 'supportsMultipleConnections'");
                logOnError(errorLogPrefix + "capability NOT matching the same base TransportService capability:");
                logOnError(errorLogPrefix + "Expected 'supportsMultipleConnections' capability = "
                    + expectedMultipleConnectionsCapability);
                logOnError(errorLogPrefix + "Actual 'supportsMultipleConnections' capability = "
                    + actualMultipleConnectionsCapability);
                testResult = STATUS_FAILED;
            }

        }
        return testResult;
    }
} // end of transportService002 class
