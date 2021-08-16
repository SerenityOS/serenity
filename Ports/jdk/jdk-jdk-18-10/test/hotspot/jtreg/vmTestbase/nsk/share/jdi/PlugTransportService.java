/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * A Super class for Transport Services used by
 * nsk/jdi/PlugConnectors tests
 */

package nsk.share.jdi;

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.connect.spi.*;
import java.io.*;
import java.util.*;

public class PlugTransportService extends TransportService {

    String plugTransportServiceName = "Undefined_PlugTransportService_Name";
    String plugTransportServiceDescription = "Undefined_PlugTransportService_Description";
    TransportService.Capabilities plugTransportServiceCapabilities = new TestCapabilities();

    /*
     * Simple implementation of TransportService.Capabilities
     */
    public static class TestCapabilities extends TransportService.Capabilities {
        boolean supportsAcceptTimeout = false;
        boolean supportsAttachTimeout = false;
        boolean supportsHandshakeTimeout = false;
        boolean supportsMultipleConnections = false;

        public TestCapabilities() {
        }

        public TestCapabilities(
                boolean supportsAcceptTimeout,
                boolean supportsAttachTimeout,
                boolean supportsHandshakeTimeout,
                boolean supportsMultipleConnections) {

            this.supportsAcceptTimeout = supportsAcceptTimeout;
            this.supportsAttachTimeout = supportsAttachTimeout;
            this.supportsHandshakeTimeout = supportsHandshakeTimeout;
            this.supportsMultipleConnections = supportsMultipleConnections;
        }

        public boolean supportsAcceptTimeout() {
            return supportsAcceptTimeout;
        }

        public boolean supportsAttachTimeout() {
            return supportsAttachTimeout;
        }

        public boolean supportsHandshakeTimeout() {
            return supportsHandshakeTimeout;
        }

        public boolean supportsMultipleConnections() {
            return supportsMultipleConnections;
        }

    } // end of TestCapabilities static class

    /*
     * Simple implementation of TransportService.ListenKey
     */
    public static class TestListenKey extends TransportService.ListenKey {
        String address = null;

        public TestListenKey() {
        }

        public TestListenKey(String address) {

            this.address = address;
        }

        public String address() {
            return address;
        }

    } // end of TestListenKey static class

    public PlugTransportService() {
    }

    public PlugTransportService(
        String plugTransportServiceName,
        String plugTransportServiceDescription,
        TransportService.Capabilities plugTransportServiceCapabilities
        ) {

        this.plugTransportServiceName = plugTransportServiceName;
        this.plugTransportServiceDescription = plugTransportServiceDescription;
        this.plugTransportServiceCapabilities = plugTransportServiceCapabilities;
    }

    public String name() {
        return plugTransportServiceName;
    }

    public String description() {
        return plugTransportServiceDescription;
    }

    public TransportService.Capabilities capabilities() {
        return plugTransportServiceCapabilities;
    }

    public Connection attach(
            String address,
            long attachTimeout,
            long handshakeTimeout) throws IOException {

        String exceptionMessage = "## PlugTransportService: TransportService name = '" +
            plugTransportServiceName + "';\nNon-authorized call of attach(...) method!";

        if ( true ) {
            throw new RuntimeException(exceptionMessage);
        }

        return null;
    }

    public TransportService.ListenKey startListening(String address) throws IOException {

        String exceptionMessage = "## PlugTransportService: TransportService name = '" +
            plugTransportServiceName + "';\nNon-authorized call of startListening(...) method!";

        if ( true ) {
            throw new RuntimeException(exceptionMessage);
        }

        return null;
    }

    public TransportService.ListenKey startListening() throws IOException {

        String exceptionMessage = "## PlugTransportService: TransportService name = '" +
            plugTransportServiceName + "';\nNon-authorized call of startListening() method!";

        if ( true ) {
            throw new RuntimeException(exceptionMessage);
        }

        return null;
    }

    public void stopListening(TransportService.ListenKey listenKey) throws IOException {

        String exceptionMessage = "## PlugTransportService: TransportService name = '" +
            plugTransportServiceName + "';\nNon-authorized call of stopListening() method!";

        if ( true ) {
            throw new RuntimeException(exceptionMessage);
        }
    }

    public Connection accept(
            TransportService.ListenKey listenKey,
            long acceptTimeout,
            long handshakeTimeout) throws IOException {

        String exceptionMessage = "## PlugTransportService: TransportService name = '" +
            plugTransportServiceName + "';\nNon-authorized call of accept(...) method!";

        if ( true ) {
            throw new RuntimeException(exceptionMessage);
        }

        return null;
    }

    /*
     * Simple implementation of Connection
     */
    public static class PlugTransportServiceConnection extends Connection {

        public void close() throws IOException {
            String exceptionMessage =
                "## PlugTransportConnection: \nNon-authorized call of close() method!";

            if ( true ) {
                throw new RuntimeException(exceptionMessage);
            }
        }

        public boolean isOpen() {
            String exceptionMessage =
                "## PlugTransportConnection: \nNon-authorized call of isOpen() method!";

            if ( true ) {
                throw new RuntimeException(exceptionMessage);
            }
            return false;
        }

        public byte[] readPacket() throws IOException {
            String exceptionMessage =
                "## PlugTransportConnection: \nNon-authorized call of readPacket() method!";

            if ( true ) {
                throw new ClosedConnectionException(exceptionMessage);
            }

            if ( true ) {
                throw new ClosedConnectionException();
            }

            return null;
        }

        public void writePacket(byte[] pkt) throws IOException {
            String exceptionMessage =
                "## PlugTransportConnection: \nNon-authorized call of writePacket(...) method!";

            if ( true ) {
                throw new ClosedConnectionException(exceptionMessage);
            }

            if ( true ) {
                throw new ClosedConnectionException();
            }

        }

    } // end of PlugTransportServiceConnection class

} // end of PlugTransportService class
