/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.io.IOException;
import java.nio.channels.DatagramChannel;

public class MyDatagramSocketImplFactory implements DatagramSocketImplFactory {
    static class MyDatagramSocketImpl extends DatagramSocketImpl {
        DatagramSocket ds;

        @Override
        protected void create() throws SocketException {
            try {
                ds = DatagramChannel.open().socket();
            } catch (IOException ex) { throw new SocketException(ex.getMessage());}
        }

        @Override
        protected void bind(int lport, InetAddress laddr) throws SocketException {
            ds.bind(new InetSocketAddress(laddr, lport));
            localPort = ds.getLocalPort();
        }

        @Override
        protected void send(DatagramPacket p) throws IOException {
            ds.send(p);
        }

        @Override
        protected int peek(InetAddress i) throws IOException {
            return 0;
        }

        @Override
        protected int peekData(DatagramPacket p) throws IOException {
            return 0;
        }

        @Override
        protected void receive(DatagramPacket p) throws IOException {
            ds.receive(p);
        }

        @Override
        protected void setTTL(byte ttl) throws IOException {
        }

        @Override
        protected byte getTTL() throws IOException {
            return 0;
        }

        @Override
        protected void setTimeToLive(int ttl) throws IOException {

        }

        @Override
        protected int getTimeToLive() throws IOException {
            return 0;
        }

        @Override
        protected void join(InetAddress inetaddr) throws IOException {

        }

        @Override
        protected void leave(InetAddress inetaddr) throws IOException {

        }

        @Override
        protected void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf) throws IOException {

        }

        @Override
        protected void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf) throws IOException {

        }

        @Override
        protected void close() {
            ds.close();
        }

        @Override
        public void setOption(int optID, Object value) throws SocketException {
            try {
                if (optID == SocketOptions.SO_SNDBUF) {
                    if (((Integer) value).intValue() < 0)
                        throw new IllegalArgumentException("Invalid send buffer size:" + value);
                    ds.setOption(StandardSocketOptions.SO_SNDBUF, (Integer) value);
                } else if (optID == SocketOptions.SO_RCVBUF) {
                    if (((Integer) value).intValue() < 0)
                        throw new IllegalArgumentException("Invalid recv buffer size:" + value);
                    ds.setOption(StandardSocketOptions.SO_RCVBUF, (Integer) value);
                } else if (optID == SocketOptions.SO_REUSEADDR) {
                    ds.setOption(StandardSocketOptions.SO_REUSEADDR, (Boolean) value);
                } else if (optID == SocketOptions.SO_REUSEPORT) {
                    ds.setOption(StandardSocketOptions.SO_REUSEPORT, (Boolean) value);
                } else if (optID == SocketOptions.SO_BROADCAST) {
                     ds.setOption(StandardSocketOptions.SO_BROADCAST, (Boolean) value);
                } else if (optID == SocketOptions.IP_TOS) {
                    int i = ((Integer) value).intValue();
                    if (i < 0 || i > 255)
                        throw new IllegalArgumentException("Invalid IP_TOS value: " + value);
                    ds.setOption(StandardSocketOptions.IP_TOS, (Integer) value);
                } else if (optID == SocketOptions.IP_MULTICAST_LOOP) {
                    boolean enable = (boolean) value;
                    // Legacy ds.setOption expects true to mean 'disabled'
                    ds.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, !enable);
                } else {
                    throw new AssertionError("unknown option :" + optID);
                }
            } catch (IOException ex) { throw new SocketException(ex.getMessage()); }
        }

        @Override
        public Object getOption(int optID) throws SocketException {
            try {
                if (optID == SocketOptions.SO_SNDBUF) {
                    return ds.getOption(StandardSocketOptions.SO_SNDBUF);
                } else if (optID == SocketOptions.SO_RCVBUF) {
                    return ds.getOption(StandardSocketOptions.SO_RCVBUF);
                } else if (optID == SocketOptions.SO_REUSEADDR) {
                    return ds.getOption(StandardSocketOptions.SO_REUSEADDR);
                } else if (optID == SocketOptions.SO_REUSEPORT) {
                    return ds.getOption(StandardSocketOptions.SO_REUSEPORT);
                } else if (optID == SocketOptions.SO_BROADCAST) {
                    return ds.getOption(StandardSocketOptions.SO_BROADCAST);
                } else if (optID == SocketOptions.IP_TOS) {
                    return ds.getOption(StandardSocketOptions.IP_TOS);
                } else if (optID == SocketOptions.IP_MULTICAST_LOOP) {
                    boolean disabled = (boolean) ds.getOption(StandardSocketOptions.IP_MULTICAST_LOOP);
                    // Legacy getOption returns true when disabled
                    return Boolean.valueOf(!disabled);
                } else {
                    throw new AssertionError("unknown option: " + optID);
                }
            } catch (IOException ex) { throw new SocketException(ex.getMessage()); }
        }
    }
    @Override
    public MyDatagramSocketImpl createDatagramSocketImpl() {
        return new MyDatagramSocketImpl();
  }
}
