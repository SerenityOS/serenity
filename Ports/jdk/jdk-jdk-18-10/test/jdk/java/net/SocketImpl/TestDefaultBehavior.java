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
 * @summary Basic test for java.net.SocketImpl default behavior
 * @run testng TestDefaultBehavior
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketImpl;
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
    public void socketImpl() {
        CustomSocketImpl csi = new CustomSocketImpl();

        assertEquals(csi.supportedOptions().size(), 0);

        expectThrows(NPE, () -> csi.setOption(null, null));
        expectThrows(NPE, () -> csi.setOption(null, 1));
        expectThrows(UOE, () -> csi.setOption(SO_RCVBUF, 100));
        expectThrows(UOE, () -> csi.setOption(SO_KEEPALIVE, TRUE));
        expectThrows(UOE, () -> csi.setOption(SO_KEEPALIVE, FALSE));
        expectThrows(UOE, () -> csi.setOption(FAKE_SOCK_OPT, TRUE));
        expectThrows(UOE, () -> csi.setOption(FAKE_SOCK_OPT, FALSE));
        expectThrows(UOE, () -> csi.setOption(SO_KEEPALIVE, TRUE));

        expectThrows(NPE, () -> csi.getOption(null));
        expectThrows(UOE, () -> csi.getOption(SO_RCVBUF));
        expectThrows(UOE, () -> csi.getOption(SO_KEEPALIVE));
        expectThrows(UOE, () -> csi.getOption(FAKE_SOCK_OPT));
    }

    static final SocketOption<Boolean> FAKE_SOCK_OPT = new SocketOption<>() {
        @Override public String name() { return "FAKE_SOCK_OPT"; }
        @Override public Class<Boolean> type() { return Boolean.class; }
    };

    // A SocketImpl that delegates the three new-style socket option
    // methods to the default java.net.SocketImpl implementation.
    static class CustomSocketImpl extends SocketImpl {

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

        @Override protected void create(boolean stream) { }
        @Override protected void connect(String host, int port) { }
        @Override protected void connect(InetAddress address, int port) { }
        @Override protected void connect(SocketAddress address, int timeout)  { }
        @Override protected void bind(InetAddress host, int port) { }
        @Override protected void listen(int backlog) { }
        @Override protected void accept(SocketImpl s)  { }
        @Override protected InputStream getInputStream() { return null; }
        @Override protected OutputStream getOutputStream() { return null; }
        @Override protected int available() { return 0; }
        @Override protected void close() { }
        @Override protected void sendUrgentData(int data)  { }
        @Override public void setOption(int optID, Object value) { }
        @Override public Object getOption(int optID) { return null; }
    }
}
