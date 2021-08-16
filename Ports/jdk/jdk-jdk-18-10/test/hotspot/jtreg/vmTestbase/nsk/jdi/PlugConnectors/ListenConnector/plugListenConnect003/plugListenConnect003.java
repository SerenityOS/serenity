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
 * @summary converted from VM Testbase nsk/jdi/PlugConnectors/ListenConnector/plugListenConnect003.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     nsk/jdi/PlugConnectors/ListenConnector/plugListenConnect003 test:
 *     This test is the test for the mechanism for creating pluggable Connectors
 *     on base of classes which implement the Connector interfaces
 *     (AttachingConnector, ListeningConnector, or LaunchingConnector).
 *     The test checks up that at start-up time when
 *     Bootstrap.virtualMachineManager() is invoked a pluggable
 *     connector is NOT created on base of PlugListenConnector003 class
 *     which implements com.sun.jdi.connect.ListeningConnector interface,
 *     but constructor of PlugListenConnector003 throws Exception.
 * COMMENTS:
 *     Fixed bug 6426609: nsk/share/jdi/plug_connectors_jar_file.pl should use
 *                        "system" instead of back-quotes
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.PlugConnectors.ListenConnector.plugListenConnect003.plugListenConnect003
 *
 * @comment build connectors.jar to jars
 * @build nsk.share.jdi.PlugConnectors
 * @run driver nsk.jdi.ConnectorsJarBuilder
 *
 * @run main/othervm
 *      -cp jars${file.separator}connectors.jar${path.separator}${test.class.path}${path.separator}${java.class.path}
 *      nsk.jdi.PlugConnectors.ListenConnector.plugListenConnect003.plugListenConnect003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 */

package nsk.jdi.PlugConnectors.ListenConnector.plugListenConnect003;

import nsk.jdi.PlugConnectors.ListenConnector.plugListenConnect003.connectors.*;

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
 * Bootstrap.virtualMachineManager() is invoked a pluggable            <BR>
 * connector is NOT created on base  of PlugListenConnector003 class   <BR>
 * which implements com.sun.jdi.connect.ListeningConnector interface,  <BR>
 * but constructor of PlugListenConnector003 throws Exception.         <BR>
 *                                                                     <BR>
 */

public class plugListenConnect003 {

    static final int STATUS_PASSED = 0;
    static final int STATUS_FAILED = 2;
    static final int STATUS_TEMP = 95;

    static final String errorLogPrefixHead = "plugListenConnect003: ";
    static final String errorLogPrefix     = "                      ";
    static final String infoLogPrefixNead = "--> plugListenConnect003: ";
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
        int result =  new plugListenConnect003().runThis(argv, out);
        if ( result == STATUS_FAILED ) {
            logAlways("\n##> nsk/jdi/PlugConnectors/ListenConnector/plugListenConnect003 test FAILED");
        }
        else {
            logAlways("\n==> nsk/jdi/PlugConnectors/ListenConnector/plugListenConnect003 test PASSED");
        }
        return result;
    }


    private int runThis (String argv[], PrintStream out) {
        int testResult = STATUS_PASSED;

        argsHandler = new ArgumentHandler(argv);
        logHandler = new Log(out, argsHandler);
        logHandler.enableErrorsSummary(false);

        logAlways("==> nsk/jdi/PlugConnectors/ListenConnector/plugListenConnect003 test...");
        logOnVerbose
            ("==> Test checks that pluggable listening connector is NOT created.");
        logOnVerbose
            ("    for ListeningConnector implementation for which instance can not be created.");


        VirtualMachineManager virtualMachineManager = null;
        try {
            virtualMachineManager = Bootstrap.virtualMachineManager();
        } catch (Throwable thrown) {
            // OK: Bootstrap.virtualMachineManager() may throw an unspecified error
            // if initialization of the VirtualMachineManager fails or if the virtual
            // machine manager is unable to locate or create any Connectors.
            logOnVerbose
                (infoLogPrefixNead + "Bootstrap.virtualMachineManager() throws:\n" + thrown);
            return STATUS_PASSED;
        }
        if (virtualMachineManager == null) {
            logOnError(errorLogPrefixHead + "Bootstrap.virtualMachineManager() returns null.");
            return STATUS_FAILED;
        }

        // check that pluggable listening connector is NOT created on base of ListeningConnector
        // implementation (PlugListenConnector003 class) for which instance can not be created
        List listeningConnectorsList = virtualMachineManager.listeningConnectors();
        int listeningConnectorsNumber = listeningConnectorsList.size();
        ListeningConnector checkedPlugListenConnector = null;

        for (int i=0; i < listeningConnectorsNumber; i++ ) {
            ListeningConnector listeningConnector = (ListeningConnector)listeningConnectorsList.get(i);
            if ( listeningConnector instanceof PlugListenConnector003 ) {
                checkedPlugListenConnector = listeningConnector;
                break;
            }
        }

        if ( checkedPlugListenConnector != null ) {
            logOnError(errorLogPrefixHead + "Pluggable listening connector is created on base of ListeningConnector");
            logOnError(errorLogPrefix + "implementation for which instance can not be created.");
            logOnError(errorLogPrefix + "This connector is found out in listeningConnectors() list");
            logOnError(errorLogPrefix + "Connector instance = '" + checkedPlugListenConnector + "'");
            logOnError(errorLogPrefix + "Connector name = '" + checkedPlugListenConnector.name() + "'");
            testResult = STATUS_FAILED;
        }


        // check that pluggable connectors are NOT contained in allConnectors() List too
        List allConnectorsList = virtualMachineManager.allConnectors();
        int allConnectorsNumber = allConnectorsList.size();

        Connector foundListenConnector = null;
        for (int i=0; i < allConnectorsNumber; i++ ){
            Connector connector = (Connector)allConnectorsList.get(i);
            if ( connector instanceof PlugListenConnector003 ) {
                foundListenConnector = connector;
                break;
            }
        }

        if ( foundListenConnector != null ) {
            logOnError(errorLogPrefixHead + "Pluggable listening connector is created on base of ListeningConnector");
            logOnError(errorLogPrefix + "implementation for which instance can not be created.");
            logOnError(errorLogPrefix + "This connector is found out in allConnectors() list");
            logOnError(errorLogPrefix + "Connector instance = '" + foundListenConnector + "'");
            logOnError(errorLogPrefix + "Connector name = '" + foundListenConnector.name() + "'");
            testResult = STATUS_FAILED;
        }

        return testResult;
    }
} // end of plugListenConnect003 class
