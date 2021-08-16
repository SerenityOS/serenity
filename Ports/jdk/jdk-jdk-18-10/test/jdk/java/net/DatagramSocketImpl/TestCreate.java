/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8238231
 * @summary test that DatagramSocket calls java.net.DatagramSocketImpl::create
 * @run testng/othervm TestCreate
 */

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.testng.Assert.assertTrue;

public class TestCreate {

    @Test
    public void datagramSocketImpl() throws IOException {
        CustomDatagramSocketImpl dsi = new CustomDatagramSocketImpl();
        CustomDatagramSocket ds = new CustomDatagramSocket(dsi);
        ds.bind(new InetSocketAddress(0));
        assertTrue(dsi.created.get(), "new CustomDatagramSocket(dsi)");

        CustomDatagramSocketImpl dsi2 = new CustomDatagramSocketImpl();
        CustomDatagramSocketImpl dsi3 = new CustomDatagramSocketImpl();
        Iterator<CustomDatagramSocketImpl> iterator = List.of(dsi2, dsi3).iterator();
        DatagramSocket.setDatagramSocketImplFactory(() -> iterator.next());

        DatagramSocket ds2 = new DatagramSocket(null);
        assertTrue(dsi2.created.get(), "new DatagramSocket()");

        MulticastSocket ds3 = new MulticastSocket(null);
        assertTrue(dsi3.created.get(), "new MulticastSocket()");
    }

    static class CustomDatagramSocket extends DatagramSocket {
        CustomDatagramSocket(DatagramSocketImpl impl) {
            super(impl);
        }
    }

    // A DatagramSocketImpl that delegates the three new-style socket option
    // methods to the default java.net.DatagramSocketImpl implementation.
    static class CustomDatagramSocketImpl extends DatagramSocketImpl {

        final AtomicBoolean created = new AtomicBoolean(false);

        @Override
        public <T> void setOption(SocketOption<T> name, T value) throws IOException {
            super.setOption(name, value);
        }

        @Override
        public Set<SocketOption<?>> supportedOptions() {
            return super.supportedOptions();
        }

        @Override
        public <T> T getOption(SocketOption<T> name) throws IOException {
            return super.getOption(name);
        }

        // --
        @Override protected void create() {
            if (created.compareAndExchange(false, true)) {
                throw new AssertionError("create called twice");
            }
        }
        @Override protected void bind(int lport, InetAddress laddr) { }
        @Override protected void send(DatagramPacket p) { }
        @Override protected int peek(InetAddress i) { return 0; }
        @Override protected int peekData(DatagramPacket p) { return 0; }
        @Override protected void receive(DatagramPacket p) { }
        @Override protected void setTTL(byte ttl) { }
        @Override protected byte getTTL() { return 0; }
        @Override protected void setTimeToLive(int ttl) { }
        @Override protected int getTimeToLive() { return 0; }
        @Override protected void join(InetAddress inetaddr) { }
        @Override protected void leave(InetAddress inetaddr)  { }
        @Override protected void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf) { }
        @Override protected void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf) { }
        @Override protected void close() { }
        @Override public void setOption(int optID, Object value) { }
        @Override public Object getOption(int optID) { return null; }
    }
}
