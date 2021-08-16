/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8224477
 * @summary Basic test for java.net.DatagramSocketImpl default behavior
 * @run testng TestDefaultBehavior
 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.util.Set;
import org.testng.annotations.Test;
import static java.lang.Boolean.*;
import static java.net.StandardSocketOptions.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

public class TestDefaultBehavior {

    static final Class<NullPointerException> NPE = NullPointerException.class;
    static final Class<UnsupportedOperationException> UOE = UnsupportedOperationException.class;

    @Test
    public void datagramSocketImpl() {
        CustomDatagramSocketImpl dsi = new CustomDatagramSocketImpl();

        assertEquals(dsi.supportedOptions().size(), 0);

        expectThrows(NPE, () -> dsi.setOption(null, null));
        expectThrows(NPE, () -> dsi.setOption(null, 1));
        expectThrows(UOE, () -> dsi.setOption(SO_RCVBUF, 100));
        expectThrows(UOE, () -> dsi.setOption(SO_KEEPALIVE, TRUE));
        expectThrows(UOE, () -> dsi.setOption(SO_KEEPALIVE, FALSE));
        expectThrows(UOE, () -> dsi.setOption(FAKE_SOCK_OPT, TRUE));
        expectThrows(UOE, () -> dsi.setOption(FAKE_SOCK_OPT, FALSE));
        expectThrows(UOE, () -> dsi.setOption(SO_KEEPALIVE, TRUE));

        expectThrows(NPE, () -> dsi.getOption(null));
        expectThrows(UOE, () -> dsi.getOption(SO_RCVBUF));
        expectThrows(UOE, () -> dsi.getOption(SO_KEEPALIVE));
        expectThrows(UOE, () -> dsi.getOption(FAKE_SOCK_OPT));
    }

    static final SocketOption<Boolean> FAKE_SOCK_OPT = new SocketOption<>() {
        @Override public String name() { return "FAKE_SOCK_OPT"; }
        @Override public Class<Boolean> type() { return Boolean.class; }
    };

    // A DatagramSocketImpl that delegates the three new-style socket option
    // methods to the default java.net.DatagramSocketImpl implementation.
    static class CustomDatagramSocketImpl extends DatagramSocketImpl {

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
        @Override protected void create() { }
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
