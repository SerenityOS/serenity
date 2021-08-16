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

package java.net;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Set;

/**
 * A SocketImpl that delegates all operations to a platform SocketImpl. It also
 * overrides all methods with public methods and implements AutoCloseable to make
 * it easy to write tests.
 */

public class PlatformSocketImpl extends SocketImpl implements AutoCloseable {
    private final SocketImpl impl;

    public PlatformSocketImpl(boolean server) {
        impl = new sun.nio.ch.NioSocketImpl(server);
    }

    @Override
    public void close() throws IOException {
        impl.close();
    }

    @Override
    public void create(boolean stream) throws IOException {
        impl.create(stream);
    }

    @Override
    public void connect(SocketAddress remote, int millis) throws IOException {
        impl.connect(remote, millis);
    }

    @Override
    public void connect(String host, int port) throws IOException {
        impl.connect(host, port);
    }

    @Override
    public void connect(InetAddress address, int port) throws IOException {
        impl.connect(address, port);
    }

    @Override
    public void bind(InetAddress address, int port) throws IOException {
        impl.bind(address, port);
    }

    @Override
    public void listen(int backlog) throws IOException {
        impl.listen(backlog);
    }

    @Override
    public void accept(SocketImpl si) throws IOException {
        impl.accept(((PlatformSocketImpl) si).impl);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return impl.getInputStream();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return impl.getOutputStream();
    }

    @Override
    public int available() throws IOException {
        return impl.available();
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return impl.supportedOptions();
    }

    @Override
    public <T> void setOption(SocketOption<T> opt, T value) throws IOException {
        impl.setOption(opt, value);
    }

    @Override
    public <T> T getOption(SocketOption<T> opt) throws IOException {
        return impl.getOption(opt);
    }

    @Override
    public void setOption(int opt, Object value) throws SocketException {
        impl.setOption(opt, value);
    }

    @Override
    public Object getOption(int opt) throws SocketException {
       return impl.getOption(opt);
    }

    @Override
    public void shutdownInput() throws IOException {
       impl.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        impl.shutdownOutput();
    }

    @Override
    public boolean supportsUrgentData() {
        return impl.supportsUrgentData();
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        impl.sendUrgentData(data);
    }
}
