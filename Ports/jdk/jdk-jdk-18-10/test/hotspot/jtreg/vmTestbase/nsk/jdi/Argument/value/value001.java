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
import com.sun.jdi.connect.Connector;
import java.io.*;
import javax.naming.directory.Attribute;
import java.util.*;

/**
 * Test for the control of
 *
 *      Interface:      com.sun.jdi.connect.Connector.Argument
 *      Method:         public java.lang.String setValue()
 *      Assertion:      "Returns the current value of the argument."
 */

public class value001 {
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
            int ksz = cdfltArgmnts.size();
            String av[] = new String[ksz + 1];
            Set ks = cdfltArgmnts.keySet();
            if (ks.isEmpty()) {
                log.complain("FAILURE: empty default argument set is found "
                           + "for " + c.name() + " connector!");
                return 2;
            }

            log.display("Looking over " + c.name() + " connector arguments: ");

            Iterator argi = ks.iterator();
            for (int j = 1; argi.hasNext(); j++) {
                String argkey = (String) argi.next();
                Connector.Argument argval =
                    (Connector.Argument)cdfltArgmnts.get((Object) argkey);

                String ovl = argval.value();
                String nvl = null;
                try {
                    argval.setValue(nvl);
                    nvl = argval.value();
                    if (nvl != null) {
                        log.complain("FAILURE: Can't set up new argument "
                                   + "null-value!");
                        return 2;
                    }
                } catch (java.lang.NullPointerException exc) {
                    log.display("Can't set up new argument null-value.");
                }

                argval.setValue("*");
                nvl = argval.value();
                if (!nvl.equals("*")) {
                    log.complain("FAILURE: Can't set up new argument "
                               + "'*'");
                    return 2;
                }

                argval.setValue(ovl);
                nvl = argval.value();
                if (nvl != ovl) {
                    log.complain("FAILURE: Can't reset old argument value!");
                    return 2;
                }

                log.display("Changed " + argval.name() + " argument's value "
                          + "is: " + nvl);
            };
        };
        log.display("Test PASSED!");
        return 0;
    }
}
