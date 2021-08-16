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

package nsk.jdi.AttachingConnector.attach;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;


/**
 * The test check up two cases:<BR>
 *   1. Parameter has <code>null</code> value. In this case <code>NullPointerException</code><BR>
 *      is expected.<BR>
 *   2. Parameter has wrong arguments. In this case <BR>
 *      <code>IllegalConnectorArgumentsException</code> is expected.<BR>
 * Checking of the each case is executed by  <code>performStep</code> method.<BR>
 * Note, this test establishes no connections with debugee, just checks performing of<BR>
 * <code>attach</code> with wrong parameters.<BR>
 */
public class attach003 {

    private final static String[][] ARGS = {
                                                {"first_key", "first_value"},
                                                {"second_key","second_value"},
                                                {"third_key", "third_value"}
    };

    private static Log log;

    public static void main(String argv[]) {
        System.exit(Consts.JCK_STATUS_BASE + run(argv, System.out));
    }

    public static int run(String argv[], PrintStream out) {
        return execTest(argv, out);
    }

    // this method does bad things
    @SuppressWarnings("unchecked")
    private static int execTest(String argv[], PrintStream out) {

        int exitCode = Consts.TEST_PASSED;

        log = new Log(out, new ArgumentHandler(argv));

        List connectors = Bootstrap.virtualMachineManager().attachingConnectors();
        log.display("Getting of " + connectors.size() + " connectors.");
        log.display("-----------------------------------------------");

        Map<java.lang.String, com.sun.jdi.connect.Connector.Argument> arguments = null;
        log.display("Checking of <null> parameter.");
        if (!performStep(connectors, arguments))
            exitCode = Consts.TEST_FAILED;

        log.display("-----------------------------------------------");
        log.display("Checking of parameter with wrong arguments:");
        Attributes amap = new Attributes();
        arguments = (Map<java.lang.String, com.sun.jdi.connect.Connector.Argument>)(Map)amap;

        for (int i =0 ; i < ARGS.length; i++) {
            log.display(i + ". " + ARGS[i][0] + " , " +  ARGS[i][1] );
            amap.putValue(ARGS[i][0], ARGS[i][1]);
        }
        log.display("");
        if (!performStep(connectors, arguments))
            exitCode = Consts.TEST_FAILED;

        return exitCode;
    }

    private static boolean performStep(List connectors, Map<java.lang.String,? extends com.sun.jdi.connect.Connector.Argument> arguments) {
        boolean res = true;
        VirtualMachine vm;
        AttachingConnector connector;
        for (int i = 0; i < connectors.size(); i++) {
            connector = (AttachingConnector )connectors.get(i);
            if (connector == null) {
                log.complain("Wrong connector " + connector + ": index=" + i
                                    + " size of connectors list="
                                    + connectors.size());
                res = false;
                continue;
            }
            try {
                log.display("Connector " + connector.name() + ":");
                vm = connector.attach(arguments);
                if (arguments == null) {
                    log.complain("\tNullPointerException is not thrown.\n");
                    res = false;
                } else {
                    log.complain("\tIllegalConnectorArgumentsException "
                                        + "is not thrown.\n");
                    res = false;
                }
            } catch (NullPointerException e) {
                if (arguments != null) {
                    log.complain("\tunexpected NullPointerException.\n");
                    res = false;
                    continue;
                }
                log.display("\texpected NullPointerException.\n");
            } catch (IllegalConnectorArgumentsException e) {
                if (arguments == null) {
                    log.complain("\tunexpected IllegalConnectorArgumentsException.\n");
                    res = false;
                    continue;
                }
                log.display("\texpected IllegalConnectorArgumentsException.\n");
            } catch (Exception e) {
                log.complain("\tunexpected " + e );
                res = false;
            }
        }
        return res;
    }

}
