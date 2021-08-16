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

/*
 * A Simple LaunchingConnector used by unit test :-
 * test/com/sun/jdi/connect/spi/DebugUsingCustomConnector.java
 */
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.Transport;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.connect.spi.TransportService;
import com.sun.jdi.connect.spi.Connection;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.HashMap;

public class SimpleLaunchingConnector implements LaunchingConnector {
    TransportService ts;
    String ARG_NAME = "class";

    /*
     * Simple implementation of Connector.StringArgument
     */
    static class StringArgumentImpl implements Connector.StringArgument {
        String name;
        String label;
        String description;
        String value;

        StringArgumentImpl(String name, String label, String description, String value) {
            this.name = name;
            this.label = label;
            this.description = description;
            this.value = value;
        }

        public String name() {
            return name;
        }

        public String label() {
            return label;
        }

        public String description() {
            return description;
        }

        public String value() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean isValid(String value) {
            if (value.length() > 0) {
                return true;
            }
            return false;
        }

        public boolean mustSpecify() {
            return true;
        }
    }

    public SimpleLaunchingConnector() {
        try {
            Class<?> c = Class.forName("com.sun.tools.jdi.SocketTransportService");
            ts = (TransportService)c.newInstance();
        } catch (Exception x) {
            throw new Error(x);
        }
    }

    public String name() {
        return "SimpleLaunchingConnector";
    }

    public String description() {
        return "SimpleLaunchingConnector";
    }

    public Transport transport() {
        return new Transport() {
            public String name() {
                return ts.name();
            }
        };
    }

    public Map defaultArguments() {
        HashMap map = new HashMap();
        map.put(ARG_NAME,
                new StringArgumentImpl(ARG_NAME, "class name", "class name", ""));
        return map;
    }

    public VirtualMachine launch(Map<String, ? extends Connector.Argument> arguments) throws
                              IOException,
                              IllegalConnectorArgumentsException,
                              VMStartException {

        /*
         * Get the class name that we are to execute
         */
        String className = ((StringArgumentImpl)arguments.get(ARG_NAME)).value();
        if (className.length() == 0) {
            throw new IllegalConnectorArgumentsException("class name missing", ARG_NAME);
        }

        /*
         * Listen on an emperical port; launch the debuggee; wait for
         * for the debuggee to connect; stop listening;
         */
        TransportService.ListenKey key = ts.startListening();

        String exe = System.getProperty("java.home") + File.separator + "bin" +
            File.separator + "java";
        String cmd = exe + " -Xdebug -Xrunjdwp:transport=dt_socket,timeout=15000,address=" +
            key.address() +
            " -classpath " + System.getProperty("test.classes") +
            " " + className;
        Process process = Runtime.getRuntime().exec(cmd);
        Connection conn = ts.accept(key, 30*1000, 9*1000);
        ts.stopListening(key);

        /*
         * Debugee is connected - return the virtual machine mirror
         */
        return Bootstrap.virtualMachineManager().createVirtualMachine(conn);
    }
}
