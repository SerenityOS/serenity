/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6198277
 * @summary Test that each ListeningConnector that supports a "timeout" argument will
 * timeout with TransportTimeoutException
 */
import com.sun.jdi.Bootstrap;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.ListeningConnector;
import com.sun.jdi.connect.TransportTimeoutException;
import java.util.List;
import java.util.Map;

public class AcceptTimeout {

    public static void main(String args[]) throws Exception {
        List<ListeningConnector> connectors = Bootstrap.virtualMachineManager().listeningConnectors();
        for (ListeningConnector lc: connectors) {
            Map<String,Connector.Argument> cargs = lc.defaultArguments();
            Connector.IntegerArgument timeout = (Connector.IntegerArgument)cargs.get("timeout");

            /*
             * If the Connector has a argument named "timeout" then we set the timeout to 1 second
             * and start it listening on its default address. It should throw TranpsortTimeoutException.
             */
            if (timeout != null) {
                System.out.println("Testing " + lc.name());
                timeout.setValue(1000);

                System.out.println("Listening on: " + lc.startListening(cargs));
                try {
                    lc.accept(cargs);
                    throw new RuntimeException("Connection accepted from some debuggee - unexpected!");
                } catch (TransportTimeoutException e) {
                    System.out.println("Timed out as expected.\n");
                }
                lc.stopListening(cargs);
            }
        }
    }
}
