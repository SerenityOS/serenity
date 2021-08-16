/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.Transport;
import com.sun.jdi.connect.spi.Connection;
import com.sun.jdi.connect.spi.TransportService;

/*
 * An AttachingConnector to attach to a running VM using any
 * TransportService.
 */

public class GenericAttachingConnector
        extends ConnectorImpl implements AttachingConnector
{
    /*
     * The arguments that this connector supports
     */
    static final String ARG_ADDRESS = "address";
    static final String ARG_TIMEOUT = "timeout";

    TransportService transportService;
    Transport transport;

    /*
     * Initialize a new instance of this connector. The connector
     * encapsulates a transport service and optionally has an
     * "address" connector argument.
     */
    private GenericAttachingConnector(TransportService ts,
                                      boolean addAddressArgument)
    {
        transportService = ts;
        transport = new Transport() {
            public String name() {
                // delegate name to the transport service
                return transportService.name();
            }
        };

        if (addAddressArgument) {
            addStringArgument(
                ARG_ADDRESS,
                getString("generic_attaching.address.label"),
                getString("generic_attaching.address"),
                "",
                true);
        }

        addIntegerArgument(
                ARG_TIMEOUT,
                getString("generic_attaching.timeout.label"),
                getString("generic_attaching.timeout"),
                "",
                false,
                0, Integer.MAX_VALUE);
    }

    /**
     * Initializes a new instance of this connector. This constructor
     * is used when sub-classing - the resulting connector will have
     * a "timeout" connector argument.
     */
    protected GenericAttachingConnector(TransportService ts) {
        this(ts, false);
    }

    /*
     * Create an instance of this connector. The resulting AttachingConnector
     * will have address and timeout connector arguments.
     */
    public static GenericAttachingConnector create(TransportService ts) {
        return new GenericAttachingConnector(ts, true);
    }

    /**
     * Attach to a target VM using the specified address and Connector arguments.
     */
    public VirtualMachine attach(String address, Map<String, ? extends Connector.Argument> args)
        throws IOException, IllegalConnectorArgumentsException
    {
        String ts  = argument(ARG_TIMEOUT, args).value();
        int timeout = 0;
        if (ts.length() > 0) {
            timeout = Integer.decode(ts).intValue();
        }
        Connection connection = transportService.attach(address, timeout, 0);
        return Bootstrap.virtualMachineManager().createVirtualMachine(connection);
    }

    /**
     * Attach to a target VM using the specified arguments - the address
     * of the target VM is specified by the <code>address</code> connector
     * argument.
     */
    public VirtualMachine attach(Map<String, ? extends Connector.Argument> args)
        throws IOException, IllegalConnectorArgumentsException
    {
        String address = argument(ARG_ADDRESS, args).value();
        return attach(address, args);
    }

    public String name() {
        return transport.name() + "Attach";
    }

    public String description() {
        return transportService.description();
    }

    public Transport transport() {
        return transport;
    }
}
