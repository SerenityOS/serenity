/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.Connector._bounds_;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;

import java.io.*;
import java.util.*;

import nsk.share.*;
import nsk.share.jdi.*;


/**
 * The test checks up the methods:                                                  <br>
 *     1. <code>com.sun.jdi.connect.AttachingConnector.attach(Map)</code>           <br>
 *     2. <code>com.sun.jdi.connect.LaunchingConnector.launch(Map)</code>           <br>
 *     3. <code>com.sun.jdi.connect.ListeningConnector.startListening(Map)</code>   <br>
 *     4. <code>com.sun.jdi.connect.ListeningConnector.stopListening(Map)</code>    <br>
 *     5. <code>com.sun.jdi.connect.ListeningConnector.accept(Map)</code>           <br>
 * to throw NullPointerException for <code>null</code> value of arguments.          <br>
 */
public class bounds001 {

    public static int exitStatus;
    private static Log log;

    public static void display(String msg) {
        log.display(msg);
    }

    public static void complain(String msg) {
        log.complain("debugger FAILURE> " + msg + "\n");
    }

    public static void main(String args[]) {
        System.exit(run(args,System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        return new bounds001(args,out).run();
    }

    private bounds001(String args[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
    }

    private int run() {

        exitStatus = Consts.TEST_PASSED;
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();

        List attachConnectors;
        List launchConnectors;
        List listConnectors;
        AttachingConnector attachc;
        LaunchingConnector launchc;
        ListeningConnector listc;
        Map arg = null;
        for (int j = 0; j < 2; j++) {
            if (j == 1 ) {
                arg = new HashMap();
                display("arguments - Map of <null>");
                display("-------------------------");
            } else {
                display("arguments - <null>");
                display("------------------");
            }

            display("Attaching connectors:");
            attachConnectors = vmm.attachingConnectors();
            for (int i = 0; i < attachConnectors.size(); i++) {
                attachc = (AttachingConnector )attachConnectors.get(i);
                display("\t" + attachc.name() + ": invoking attach(null)");
                try {
                    attachc.attach(null);
                    complain("NullPointerException is not thrown");
                    exitStatus = Consts.TEST_FAILED;
                } catch (NullPointerException e) {
                    display("\tExpected " + e);
                } catch (Exception e) {
                    complain("Unexpected " + e);
                    exitStatus = Consts.TEST_FAILED;
                }
                display("");

            }

            // launching connector
            display("Launching connectors:");
            launchConnectors = vmm.launchingConnectors();
            for (int i = 0; i < launchConnectors.size(); i++) {
                launchc = (LaunchingConnector )launchConnectors.get(i);
                display("\t" + launchc.name() + ": invoking launch(null)");
                try {
                    launchc.launch(null);
                    complain("NullPointerException is not thrown");
                    exitStatus = Consts.TEST_FAILED;
                } catch (NullPointerException e) {
                    display("\tExpected " + e);
                } catch (Exception e) {
                    complain("Unexpected " + e);
                    exitStatus = Consts.TEST_FAILED;
                }
                display("");

            }

            display("Listening connectors:");
            listConnectors = vmm.listeningConnectors();
            for (int i = 0; i < listConnectors.size(); i++) {
                listc = (ListeningConnector )listConnectors.get(i);
                display("\t" + listc.name() + ": invoking startListening(null)");
                try {
                    listc.startListening(null);
                    complain("NullPointerException is not thrown");
                    exitStatus = Consts.TEST_FAILED;
                } catch (NullPointerException e) {
                    display("\tExpected " + e);
                } catch (Exception e) {
                    complain("Unexpected " + e);
                    exitStatus = Consts.TEST_FAILED;
                }
                display("");

                display("\t" + listc.name() + ": invoking stopListening(null)");
                try {
                    listc.stopListening(null);
                } catch (NullPointerException e) {
                    display("\tExpected " + e);
                } catch (Exception e) {
                    complain("Unexpected " + e);
                    exitStatus = Consts.TEST_FAILED;
                }
                display("");

                display("\t" + listc.name() + ": invoking accept(null)");
                try {
                    listc.accept(null);
                } catch (NullPointerException e) {
                    display("\tExpected " + e);
                } catch (Exception e) {
                    complain("Unexpected " + e);
                    exitStatus = Consts.TEST_FAILED;
                }
                display("");
            }
        }

        return exitStatus;
    }
}
