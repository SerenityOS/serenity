/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8233296
 * @summary Check that MulticastSocket::setOption and MulticastSocket::getOption
 *          return the correct result for StandardSocketOptions.IP_MULTICAST_LOOP.
 *          The test sets a DatagramSocketImplFactory and needs to run in /othervm
 *          mode.
 * @run testng/othervm SetLoopbackOption
 * @run testng/othervm -Djava.net.preferIPv4Stack=true SetLoopbackOption
 * @run testng/othervm -Djava.net.preferIPv6Addresses=true SetLoopbackOption
 */

import java.io.FileDescriptor;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.DatagramSocketImplFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketOption;
import java.net.SocketOptions;
import java.net.StandardSocketOptions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

import static java.lang.System.out;

public class SetLoopbackOption {

    final InetAddress loopbackAddress = InetAddress.getLoopbackAddress();

    @Test
    public void run() throws Exception {
        var bindAddress = new InetSocketAddress(loopbackAddress, 0);
        try (MulticastSocket sock = new MulticastSocket(null)) {
            out.println("Testing unbound socket");
            test(sock, null);
            out.printf("\nBinding socket to %s and testing again%n", bindAddress);
            sock.bind(bindAddress);
            test(sock, null);
        }
        TestDatagramSocketImplFactory factory = new TestDatagramSocketImplFactory();
        DatagramSocket.setDatagramSocketImplFactory(factory);
        try (MulticastSocket sock = new MulticastSocket(null)) {
            out.println("\nTesting unbound socket with custom impl");
            TestDatagramSocketImpl impl = factory.last;
            test(sock, impl);
            out.printf("\nBinding socket to %s and testing again%n", bindAddress);
            sock.bind(new InetSocketAddress(loopbackAddress, 0));
            test(sock, impl);
        }
    }

    private void test(MulticastSocket sock, TestDatagramSocketImpl impl) throws Exception {
        out.println("Testing with " + sock.getClass() + (impl == null ? "" : ", " + impl.getClass()));
        var op = StandardSocketOptions.IP_MULTICAST_LOOP;
        var opId = SocketOptions.IP_MULTICAST_LOOP;
        boolean enable = sock.getOption(op);
        assertTrue(enable, "Initial Value for " + op);
        boolean disable = sock.getLoopbackMode();
        assertFalse(disable, "Initial Value for getLoopbackMode()");
        if (impl != null) {
            assertFalse((Boolean)impl.getOption(opId));
            assertTrue((Boolean)impl.getOption(op));
        }

        out.println("Setting " + op + " to " + false);
        if (impl != null) {
            // allows setOption(SocketOption, Object) to be called
            impl.allowAllSetOptions(true);
        }
        sock.setOption(op, false);
        enable = sock.getOption(op);
        assertFalse(enable, "Value for " + op);
        disable = sock.getLoopbackMode();
        assertTrue(disable, "Value for getLoopbackMode()");
        if (impl != null) {
            assertTrue((Boolean)impl.getOption(opId));
            assertFalse((Boolean)impl.getOption(op));
        }
        out.println("Setting " + op + " to " + true);
        sock.setOption(op, true);
        enable = sock.getOption(op);
        assertTrue(enable, "Value for " + op);
        disable = sock.getLoopbackMode();
        assertFalse(disable, "Value for getLoopbackMode()");
        if (impl != null) {
            assertFalse((Boolean)impl.getOption(opId));
            assertTrue((Boolean)impl.getOption(op));
        }

        out.println("Calling setLoopbackMode(true)");
        if (impl != null) {
            // for backward compatibility reason, setLoopbackMode
            // should call setOption(int, Object), not setOption(SocketOption, Object)
            // Make sure that an exception is thrown if the latter is ever called.
            impl.allowAllSetOptions(false);
        }
        sock.setLoopbackMode(true);
        enable = sock.getOption(op);
        assertFalse(enable, "Value for " + op);
        disable = sock.getLoopbackMode();
        assertTrue(disable, "Value for getLoopbackMode()");
        if (impl != null) {
            assertTrue((Boolean)impl.getOption(opId));
            assertFalse((Boolean)impl.getOption(op));
        }
        out.println("Calling setLoopbackMode(false)");
        sock.setLoopbackMode(false);
        enable = sock.getOption(op);
        assertTrue(enable, "Value for " + op);
        disable = sock.getLoopbackMode();
        assertFalse(disable, "Value for getLoopbackMode()");
        if (impl != null) {
            assertFalse((Boolean)impl.getOption(opId));
            assertTrue((Boolean)impl.getOption(op));
        }
    }

    // Used to attempt to control what is called/passed to the impl.
    static class TestDatagramSocketImplFactory implements DatagramSocketImplFactory {
        TestDatagramSocketImpl last;
        public synchronized DatagramSocketImpl createDatagramSocketImpl() {
            TestDatagramSocketImpl last = this.last;
            if (last == null) {
                return (last = this.last = new TestDatagramSocketImpl());
            } else {
                throw new AssertionError("Only one instance should be created");
            }
        }
    }

    // Used to attempt to control what is called/passed to the impl.
    static class TestDatagramSocketImpl extends DatagramSocketImpl {
        InetAddress address;
        private boolean allowAllSetOptions;

        @Override
        protected void create() throws SocketException {
            legacyOptions.put(SocketOptions.IP_MULTICAST_LOOP, false);
            options.put(StandardSocketOptions.IP_MULTICAST_LOOP, true);
        }

        final Map<Integer, Object> legacyOptions = new HashMap<>();
        final Map<SocketOption<?>, Object> options = new HashMap<>();

        static <T> T shouldNotComeHere() {
            throw new AssertionError("should not come here");
        }

        @Override
        protected void bind(int lport, InetAddress laddr) throws SocketException {
            this.localPort = (lport == 0 ? 6789 : lport);
            this.address = laddr;
        }

        @Override
        protected void send(DatagramPacket p) throws IOException {
            shouldNotComeHere();
        }

        @Override
        protected int peek(InetAddress i) throws IOException {
            return shouldNotComeHere();
        }

        @Override
        protected int peekData(DatagramPacket p) throws IOException {
            return shouldNotComeHere();
        }

        @Override
        protected void receive(DatagramPacket p) throws IOException {
            shouldNotComeHere();
        }

        @Override
        protected void setTTL(byte ttl) throws IOException {
            shouldNotComeHere();
        }

        @Override
        protected byte getTTL() throws IOException {
            return shouldNotComeHere();
        }

        @Override
        protected void setTimeToLive(int ttl) throws IOException {
            shouldNotComeHere();
        }

        @Override
        protected int getTimeToLive() throws IOException {
            return shouldNotComeHere();
        }

        @Override
        protected void join(InetAddress inetaddr) throws IOException {
            shouldNotComeHere();
        }

        @Override
        protected void leave(InetAddress inetaddr) throws IOException {
            shouldNotComeHere();
        }

        @Override
        protected void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf)
                throws IOException {
            shouldNotComeHere();
        }

        @Override
        protected void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf)
                throws IOException {
            shouldNotComeHere();
        }

        @Override
        protected void close() {

        }

        @Override
        public void setOption(int optID, Object value) throws SocketException {
            legacyOptions.put(optID, value);
            if (optID == SocketOptions.IP_MULTICAST_LOOP) {
                boolean disable = (Boolean) value;
                options.put(StandardSocketOptions.IP_MULTICAST_LOOP, !disable);
            }
        }

        @Override
        public Object getOption(int optID) throws SocketException {
            return legacyOptions.get(optID);
        }

        @Override
        protected Set<SocketOption<?>> supportedOptions() {
            return Set.of(StandardSocketOptions.IP_MULTICAST_LOOP);
        }

        @Override
        protected void connect(InetAddress address, int port) throws SocketException {
            shouldNotComeHere();
        }

        @Override
        protected void disconnect() {
            shouldNotComeHere();
        }

        @Override
        protected FileDescriptor getFileDescriptor() {
            return super.getFileDescriptor();
        }

        @Override
        protected <T> void setOption(SocketOption<T> name, T value) throws IOException {
            if (!allowAllSetOptions) shouldNotComeHere();
            options.put(name, value);
            if (name.equals(StandardSocketOptions.IP_MULTICAST_LOOP)) {
                boolean enable = (Boolean)value;
                legacyOptions.put(SocketOptions.IP_MULTICAST_LOOP, !enable);
            }
        }

        @Override
        protected <T> T getOption(SocketOption<T> name) throws IOException {
            return (T) options.get(name);
        }

        public void allowAllSetOptions(boolean allow) {
            this.allowAllSetOptions = allow;
        }
    }

    public static void main (String args[]) throws Exception {
        new SetLoopbackOption().run();
    }
}
