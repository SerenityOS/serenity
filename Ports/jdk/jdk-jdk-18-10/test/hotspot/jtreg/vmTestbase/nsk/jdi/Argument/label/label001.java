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

package nsk.jdi.Argument.label;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import java.io.*;
import javax.naming.directory.Attribute;
import java.util.*;

/**
 * Test for the control of
 *
 *      Interface:      com.sun.jdi.connect.Connector.Argument
 *      Method:         public java.lang.String label()
 *      Assertion:      "Returns a short human-readable label for
 *                       this argument."
 */

public class label001 {
    private static Log log;

    public static void main( String argv[] ) {
        System.exit(run(argv, System.out)+95); // JCK-compatible exit status
    }

    public static int run(String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();

        List acl = vmm.allConnectors();
        if (acl.size() > 0) {
            log.display("Number of all known JDI connectors: " + acl.size());
        } else {
            log.complain("FAILURE: no JDI connectors found!");
            return 2;
        }

        Iterator aci = acl.iterator();
        for (int i = 1; aci.hasNext(); i++) {
            Connector c = (Connector) aci.next();
            Map cdfltArgmnts = c.defaultArguments();
            Set ks = cdfltArgmnts.keySet();

            if (ks.isEmpty()) {
                log.complain("FAILURE: empty default arguments set is found "
                           + "for " + c.name() + " connector!");
                return 2;
            }

            log.display("Looking over " + c.name() + " connector arguments: ");

            Iterator argi = ks.iterator();
            for (int j = 1; argi.hasNext(); j++) {
                String argkey = (String) argi.next();
                Connector.Argument argval =
                    (Connector.Argument)cdfltArgmnts.get((Object) argkey);
                String lbl = argval.label();

                if (lbl == null) {
                    log.complain("FAILURE: argument label is null "
                               + "for " + argkey + " argument of "
                               + c.name() + " connector!");
                    return 2;
                }

                if (lbl.length() < 1) {
                    log.complain("FAILURE: empty argument label is found "
                               + "for " + argkey + " argument of "
                               + c.name() + " connector!");
                    return 2;
                }

                log.display("Next (" + j + ") argument's label is: " + lbl);
            };
        };
        log.display("Test PASSED!");
        return 0;
    }
}
