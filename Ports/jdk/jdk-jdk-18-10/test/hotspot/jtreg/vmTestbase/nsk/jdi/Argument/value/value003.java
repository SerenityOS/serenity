/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.Argument.value;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.io.*;
import javax.naming.directory.Attribute;
import java.util.*;

/**
 * Test for the control of
 *
 *      Interface:      com.sun.jdi.connect.Connector.Argument
 *      Method:         public java.lang.String value()
 *      Assertion:      "Returns the current value of the argument."
 *
 *
 *      Comments:       The test aims on the concrete Sun's JDI
 *                      reference implementations. It uses
 *                      com.sun.jdi.CommandLineLaunch connector and its
 *                      "options" and "main" arguments.
 *                      The test sets up the new "options" and "main" arguments
 *                      values and then checks that new values remain previously
 *                      after connection establishing with debugee VM and
 *                      after debugee VM finishing.
 */

public class value003 {
    private static Log log;

    public static void main(String argv[] ) {
        System.exit(run(argv, System.out)+95); // JCK-compatible exit status
    }

    public static int run(String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();

        String javaKind = argHandler.getOptions().getProperty("debugee.vmkind");
        boolean java_g = javaKind != null && javaKind.startsWith("java_g"); // ...or java_g.exe
        if (java_g)
            log.display("Test option: java_g");

        List lcl = vmm.launchingConnectors();
        if (lcl.size() > 0) {
            log.display("Number of all known JDI launching connectors: " +
                        lcl.size());
        } else {
            log.complain("FAILURE: no JDI launching connectors found!");
            return 2;
        }

        Iterator lci = lcl.iterator();
        for (int i = 1; lci.hasNext(); i++) {
            Connector c = (Connector) lci.next();
            if (c.name().compareTo("com.sun.jdi.CommandLineLaunch") != 0) {
                continue;
            }
            Map<String,? extends Connector.Argument> cdfltArgmnts = c.defaultArguments();
            int ksz = cdfltArgmnts.size();
            String av[] = new String[ksz + 1];
            Set ks = cdfltArgmnts.keySet();
            if (ks.isEmpty()) {
                log.complain("FAILURE: empty default argument set is found "
                           + "for " + c.name() + " connector!");
                return 2;
            }

            log.display("Looking over " + c.name() + " connector arguments: ");

            boolean flg = false;
            Iterator argi = ks.iterator();
            String ovl = null;
            String nvl = null;
            for (int j = 1; argi.hasNext(); j++) {
                String argkey = (String)argi.next();
                Connector.Argument argval =
                    (Connector.Argument)cdfltArgmnts.get((Object) argkey);

                if (java_g && argval.name().equals("vmexec")) {
                    log.display("Substitute: vmexec --> java_g");
                    argval.setValue("java_g");
                };

                log.display("Connector.Argument argval = "+ argval);
                if (argkey.compareTo("options") != 0 &&
                    argkey.compareTo("main") != 0) {
                    continue;
                }
                if (argkey.compareTo("main") == 0) {
                    if (argval.isValid("nsk.jdi.Argument.value.value003a")) {
                        argval.setValue("nsk.jdi.Argument.value.value003a");
                    } else {
                        log.complain("FAILURE: Can't set up new value for "
                                   + "main-argument");
                        return 2;
                    }
                    continue;
                }
                flg = true;
                ovl = argval.value();
                if (argval.isValid(ovl + "-verify ")) {
                    argval.setValue(ovl + "-verify ");
                } else {
                    log.complain("FAILURE: Can't set up new value for "
                               + "options-argument");
                    return 2;
                }

                nvl = argval.value();
                if (nvl.compareTo(ovl + "-verify ") != 0) {
                    log.complain("FAILURE: Can't set up argument value!");
                    return 2;
                }
                log.display("Changed " + argval.name() + " argument's "
                          + "value is: " + nvl);
            };

            Binder binder = new Binder(argHandler, log);
            Debugee debugee = null;

            try {
                if (flg) {
                    flg = false;
                    VirtualMachine vm =
                        ((LaunchingConnector) c).launch(cdfltArgmnts);
                    log.display("VM = (" + vm + ")");
                    debugee = binder.enwrapDebugee(vm, vm.process());

                    if (((Connector.Argument)cdfltArgmnts
                       .get((Object)"options")).value()
                       .compareTo(ovl + "-verify ") != 0) {
                        log.complain("FAILURE: Current 'options' argument "
                                   + "value is not coinsides with the last "
                                   + "setted up value.");
                        return 2;
                    }
                    if (((Connector.Argument)c.defaultArguments()
                        .get((Object) "options")).value()
                        .compareTo(ovl) != 0) {
                        log.complain("FAILURE: Default 'options' argument "
                                   + "value can not be changed.");
                        return 2;
                    }

                    debugee.resume();
                }
            } catch ( java.io.IOException exc) {
                log.complain("FAILURE: Unable to launch, so "
                           + "java.io.IOException is arisen.");
                log.complain(exc.getMessage());
                return 2;
            } catch (com.sun.jdi.connect.IllegalConnectorArgumentsException
                     exc) {
                log.complain("FAILURE: One of the connector arguments is "
                           + "invalid, so IllegalConnectorArgumentsException "
                           + "is arisen.");
                log.complain(exc.getMessage());
                return 2;
            } catch ( com.sun.jdi.connect.VMStartException exc) {
                log.complain("FAILURE: VM was terminated with error before "
                           + "a connection could be established, so "
                           + "VMStartException is arisen.");
                log.complain(exc.getMessage());
                log.complain(Binder.readVMStartExceptionOutput(exc, log.getOutStream()));
                return 2;
            } finally {
                if (debugee != null) {
                    try {
                        debugee.dispose();
                    } catch (VMDisconnectedException ignore) {
                    }

                    int extcd = debugee.waitFor();
                    if (extcd != 95) {
                        log.complain("FAILURE: Launching VM crushes with "
                                     + extcd + " exit code.");
                        return 2;
                    }
                }
            }
        };

        log.display("Test PASSED!");
        return 0;
    }
}
