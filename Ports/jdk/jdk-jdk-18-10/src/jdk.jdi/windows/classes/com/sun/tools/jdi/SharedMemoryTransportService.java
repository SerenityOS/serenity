/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import com.sun.jdi.connect.spi.*;

import java.io.IOException;
import java.util.Map;
import java.util.ResourceBundle;

class SharedMemoryTransportService extends TransportService {
    private ResourceBundle messages = null;

    /**
     * The listener returned by startListening
     */
    static class SharedMemoryListenKey extends ListenKey {
        long id;
        String name;

        SharedMemoryListenKey(long id, String name) {
            this.id = id;
            this.name = name;
        }

        long id() {
            return id;
        }

        void setId(long id) {
            this.id = id;
        }

        public String address() {
            return name;
        }

        public String toString() {
            return address();
        }
    }

    SharedMemoryTransportService() {
        System.loadLibrary("dt_shmem");
        initialize();
    }

    public String name() {
        return "SharedMemory";
    }

    public String defaultAddress() {
        return "javadebug";
    }

    /**
     * Return localized description of this transport service
     */
    public String description() {
        synchronized (this) {
            if (messages == null) {
                messages = ResourceBundle.getBundle("com.sun.tools.jdi.resources.jdi");
            }
        }
        return messages.getString("memory_transportservice.description");
    }

    public Capabilities capabilities() {
        return new SharedMemoryTransportServiceCapabilities();
    }

    private native void initialize();
    private native long startListening0(String address) throws IOException;
    private native long attach0(String address, long attachTimeout) throws IOException;
    private native void stopListening0(long id) throws IOException;
    private native long accept0(long id, long acceptTimeout) throws IOException;
    private native String name(long id) throws IOException;

    public Connection attach(String address, long attachTimeout, long handshakeTimeout) throws IOException {
        if (address == null) {
            throw new NullPointerException("address is null");
        }
        long id = attach0(address, attachTimeout);
        SharedMemoryConnection conn = new SharedMemoryConnection(id);
        conn.handshake(handshakeTimeout);
        return conn;
    }

    public TransportService.ListenKey startListening(String address) throws IOException {
        if (address == null || address.length() == 0) {
            address = defaultAddress();
        }
        long id = startListening0(address);
        return new SharedMemoryListenKey(id, name(id));
    }

    public ListenKey startListening() throws IOException {
        return startListening(null);
    }

    public void stopListening(ListenKey listener) throws IOException {
        if (!(listener instanceof SharedMemoryListenKey)) {
            throw new IllegalArgumentException("Invalid listener");
        }

        long id;
        SharedMemoryListenKey key = (SharedMemoryListenKey)listener;
        synchronized (key) {
            id = key.id();
            if (id == 0) {
                throw new IllegalArgumentException("Invalid listener");
            }

            // invalidate the id
            key.setId(0);
        }
        stopListening0(id);
    }

    public Connection accept(ListenKey listener, long acceptTimeout, long handshakeTimeout) throws IOException {
        if (!(listener instanceof SharedMemoryListenKey)) {
            throw new IllegalArgumentException("Invalid listener");
        }

        long transportId;
        SharedMemoryListenKey key = (SharedMemoryListenKey)listener;
        synchronized (key) {
            transportId = key.id();
            if (transportId == 0) {
                throw new IllegalArgumentException("Invalid listener");
            }
        }

        // in theory another thread could call stopListening before
        // accept0 is called. In that case accept0 will try to accept
        // with an invalid "transport id" - this should result in an
        // IOException.

        long connectId = accept0(transportId, acceptTimeout);
        SharedMemoryConnection conn = new SharedMemoryConnection(connectId);
        conn.handshake(handshakeTimeout);
        return conn;
    }
}


class SharedMemoryTransportServiceCapabilities extends TransportService.Capabilities {

    public boolean supportsMultipleConnections() {
        return false;
    }

    public boolean supportsAttachTimeout() {
        return true;
    }

    public boolean supportsAcceptTimeout() {
        return true;
    }

    public boolean supportsHandshakeTimeout() {
        return false;
    }

}
