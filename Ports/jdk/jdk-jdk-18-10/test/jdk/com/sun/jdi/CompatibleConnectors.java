/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4287596
 * @summary Unit test for "Pluggable Connectors and Transports" feature.
 *
 * This test checks that VirtualMachineManager creates Connectors that
 * are "compatible" those created by 1.4 or earilier releases.
 */

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.util.*;

public class CompatibleConnectors {

    // number of tests that fail
    static int failures;

    static void fail(String msg) {
        System.out.println(msg + " - test failed.");
        failures++;
    }

    // the AttachingConnectors that we expect
    static Object[][] attachingConnectors() {
        return new Object[][] {
            { "com.sun.jdi.SocketAttach",
              "dt_socket",
              new String[] { "hostname", "Connector.StringArgument", "false" },
              new String[] { "port",     "Connector.IntegerArgument", "true" }
            },

            { "com.sun.jdi.SharedMemoryAttach",
              "dt_shmem",
              new String[] { "name", "Connector.StringArgument", "true" }
            }
        };
    }

    // the ListeningConnectors that we expect
    static Object[][] listeningConnectors() {
        return new Object[][] {
            { "com.sun.jdi.SocketListen",
              "dt_socket",
              new String[] { "port", "Connector.IntegerArgument", "true" }
            },

            { "com.sun.jdi.SharedMemoryListen",
              "dt_shmem",
              new String[] { "name", "Connector.StringArgument", "false" }
            }
        };

    }

    // the LaunchingConnectors that we expect
    // - note that we don't indicate the transport name (as it varies
    // for these connectors)
    static Object[][] launchingConnectors() {
        return new Object[][] {
            { "com.sun.jdi.CommandLineLaunch",
              null,
              new String[] { "home",    "Connector.StringArgument",     "false" },
              new String[] { "options", "Connector.StringArgument",     "false" },
              new String[] { "main",    "Connector.StringArgument",     "true"  },
              new String[] { "suspend", "Connector.BooleanArgument",    "false" },
              new String[] { "quote",   "Connector.StringArgument",     "true"  },
              new String[] { "vmexec",  "Connector.StringArgument",     "true" }
            },

            { "com.sun.jdi.RawCommandLineLaunch",
              null,
              new String[] { "command",  "Connector.StringArgument",     "true" },
              new String[] { "address",  "Connector.StringArgument",     "true" },
              new String[] { "quote",    "Connector.StringArgument",     "true" }
            }
        };

    }

    // find Connector by name, return null if not found
    static Connector find(String name, List l) {
        Iterator i = l.iterator();
        while (i.hasNext()) {
            Connector c = (Connector)i.next();
            if (c.name().equals(name)) {
                return c;
            }
        }
        return null;
    }

    // check that a connector is of the expected type
    static void type_match(String arg_name, String arg_type, Connector.Argument arg) {
        boolean fail = false;
        if (arg_type.equals("Connector.StringArgument")) {
            if (!(arg instanceof Connector.StringArgument)) {
                fail = true;
            }
        }
        if (arg_type.equals("Connector.IntegerArgument")) {
            if (!(arg instanceof Connector.IntegerArgument)) {
                fail = true;
            }
        }
        if (arg_type.equals("Connector.BooleanArgument")) {
            if (!(arg instanceof Connector.BooleanArgument)) {
                fail = true;
            }
        }
        if (arg_type.equals("Connector.SelectedArgument")) {
            if (!(arg instanceof Connector.IntegerArgument)) {
                fail = true;
            }
        }
        if (fail) {
            fail(arg_name + " is of type: " + arg.getClass() + ", expected: "
                 + arg_type);
        }
    }


    // check that a Connector is compatible
    static void check(Object[] desc, Connector connector) {
        String name = (String)desc[0];
        String transport_name = (String)desc[1];

        // if the transport name is "null" it means its transport may
        // vary (eg: SunCommandLineLauncher will choose shared memory
        // on Windows and dt_socket on Solaris). In that case we can't
        // check the transport name.
        //
        if (transport_name != null) {
            System.out.println("Checking transpot name");
            if (!(transport_name.equals(connector.transport().name()))) {
                fail("transport().name() returns: " +
                    connector.transport().name() + ", expected: " + transport_name);
            }
        }

        // check that all the old arguments still exist
        for (int i=2; i<desc.length; i++) {
            String[] args = (String[])desc[i];
            String arg_name = args[0];
            String arg_type = args[1];
            String arg_mandatory = args[2];

            System.out.println("Checking argument: " + arg_name);

            // check that the arg still exists
            Map defaultArgs = connector.defaultArguments();
            Object value = defaultArgs.get(arg_name);
            if (value == null) {
                fail(name + " is missing Connector.Argument: " + arg_name);
                continue;
            }

            // next check that the type matches
            Connector.Argument connector_arg = (Connector.Argument)value;

            // check that the argument type hasn't changed
            type_match(arg_name, arg_type, connector_arg);

            // check that an optional argument has been made mandatory
            if (arg_mandatory.equals("false")) {
                if (connector_arg.mustSpecify()) {
                    fail(arg_name + " is now mandatory");
                }
            }
        }

        // next we check for new arguments that are mandatory but
        // have no default value

        System.out.println("Checking for new arguments");
        Map dfltArgs = connector.defaultArguments();
        Iterator iter = dfltArgs.keySet().iterator();
        while (iter.hasNext()) {
            String arg_name = (String)iter.next();

            // see if the argument is new
            boolean found = false;
            for (int j=2; j<desc.length; j++) {
                String[] args = (String[])desc[j];
                if (args[0].equals(arg_name)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Connector.Argument connector_arg =
                    (Connector.Argument)dfltArgs.get(arg_name);

                if (connector_arg.mustSpecify()) {
                    String value = connector_arg.value();
                    if (value.equals("")) {
                        value = null;
                    }
                    if (value == null) {
                        fail("New Connector.Argument \"" + connector_arg.name() +
                            "\" added - argument is mandatory");
                    }
                }
            }
        }
    }


    // compare the actual list of Connectors against the
    // expected list of Connectors.
    static void compare(Object[][] prev, List list) {
        String os = System.getProperty("os.name");
        for (int i=0; i<prev.length; i++) {
            Object[] desc = prev[i];
            String name = (String)desc[0];

            // ignore Windows specific Connectors are non-Windows machines
            if (!(os.startsWith("Windows"))) {
                if (name.equals("com.sun.jdi.SharedMemoryAttach") ||
                    name.equals("com.sun.jdi.SharedMemoryListen")) {
                    continue;
                }
            }

            System.out.println("");
            System.out.println("Checking Connector " + name);

            // check that the Connector exists
            Connector c = find(name, list);
            if (c == null) {
                fail("Connector is missing");
                continue;
            }

            check(desc, c);
        }
    }

    public static void main(String args[]) throws Exception {
        VirtualMachineManager vmm = Bootstrap.virtualMachineManager();

        // in 1.2/1.3/1.4 the defualtConnector was
        // com.sun.jdi.CommandLineLaunch. Many debuggers probably
        // depend on this so check that it's always the default.
        //
        String expected = "com.sun.jdi.CommandLineLaunch";
        System.out.println("Checking that defaultConnector is: " + expected);
        String dflt = vmm.defaultConnector().name();
        if (!(dflt.equals(expected))) {
            System.err.println("defaultConnector() is: " + dflt +
                ", expected:" + expected);
            failures++;
        } else {
            System.out.println("Okay");
        }

        compare(attachingConnectors(), vmm.attachingConnectors());
        compare(listeningConnectors(), vmm.listeningConnectors());
        compare(launchingConnectors(), vmm.launchingConnectors());

        // test results
        if (failures > 0) {
            System.out.println("");
            throw new RuntimeException(failures + " test(s) failed");
        }
    }
}
