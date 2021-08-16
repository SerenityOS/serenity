/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package com.sun.tools.jdi;

import java.io.IOException;
import java.util.Map;

import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.Transport;
import com.sun.jdi.connect.spi.TransportService;

/*
 * A ListeningConnector that uses the SocketTransportService
 */
public class SocketListeningConnector extends GenericListeningConnector {

    static final String ARG_PORT = "port";
    static final String ARG_LOCALADDR = "localAddress";

    public SocketListeningConnector() {
        super(new SocketTransportService());

        addIntegerArgument(
            ARG_PORT,
            getString("socket_listening.port.label"),
            getString("socket_listening.port"),
            "",
            false,
            0, Integer.MAX_VALUE);

        addStringArgument(
            ARG_LOCALADDR,
            getString("socket_listening.localaddr.label"),
            getString("socket_listening.localaddr"),
            "",                                         // default is wildcard
            false);

        transport = new Transport() {
            public String name() {
                return "dt_socket";     // for compatibility reasons
            }
        };
    }

    public String
        startListening(Map<String,? extends Connector.Argument> args)
        throws IOException, IllegalConnectorArgumentsException
    {
        String port = argument(ARG_PORT, args).value();
        String localaddr = argument(ARG_LOCALADDR, args).value();

        // default to system chosen port
        if (port.length() == 0) {
            port = "0";
        }

        if (localaddr.length() > 0) {
           localaddr = localaddr + ":" + port;
        } else {
           localaddr = port;
        }

        return super.startListening(localaddr, args);
    }

    public String name() {
        return "com.sun.jdi.SocketListen";
    }

    public String description() {
        return getString("socket_listening.description");
    }

    // If the port is auto detected update the argument map with the bound port number.
    @Override
    protected void updateArgumentMapIfRequired(
        Map<String, ? extends Connector.Argument> args, TransportService.ListenKey listener) {
        if (isWildcardPort(args)) {
            String[] address = listener.address().split(":");
            if (address.length > 1) {
                args.get(ARG_PORT).setValue(address[address.length - 1]);
            }
        }
    }

    private boolean isWildcardPort(Map<String, ? extends Connector.Argument> args) {
        String port = args.get(ARG_PORT).value();
        return port.isEmpty() || Integer.valueOf(port) == 0;
    }
}
