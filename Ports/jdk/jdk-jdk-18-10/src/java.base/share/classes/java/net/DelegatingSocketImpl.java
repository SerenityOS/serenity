/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package java.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;

import sun.net.PlatformSocketImpl;

/**
 * A SocketImpl that delegates all methods to another SocketImpl.
 */

class DelegatingSocketImpl extends SocketImpl {
    protected final SocketImpl delegate;

    DelegatingSocketImpl(SocketImpl delegate) {
        assert delegate instanceof PlatformSocketImpl;
        this.delegate = Objects.requireNonNull(delegate);
    }

    final SocketImpl delegate() {
        return delegate;
    }

    @Override
    protected FileDescriptor getFileDescriptor() {
        return delegate.getFileDescriptor();
    }

    @Override
    protected InetAddress getInetAddress() {
        return delegate.getInetAddress();
    }

    @Override
    protected int getPort() {
        return delegate.getPort();
    }

    @Override
    protected int getLocalPort() {
        return delegate.getLocalPort();
    }

    @Override
    protected void create(boolean stream) throws IOException {
        delegate.create(stream);
    }

    @Override
    protected void connect(String host, int port) throws IOException {
        delegate.connect(host, port);
    }

    @Override
    protected void connect(InetAddress address, int port) throws IOException {
        delegate.connect(address, port);
    }

    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {
        delegate.connect(address, timeout);
    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {
        delegate.bind(host, port);
    }

    @Override
    protected void listen(int backlog) throws IOException {
        delegate.listen(backlog);
    }

    @Override
    protected void accept(SocketImpl s) throws IOException {
        delegate.accept(s);
    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        return delegate.getOutputStream();
    }

    @Override
    protected int available() throws IOException {
        return delegate.available();
    }

    @Override
    protected void close() throws IOException {
        delegate.close();
    }

    @Override
    protected boolean supportsUrgentData() {
        return delegate.supportsUrgentData();
    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        delegate.sendUrgentData(data);
    }

    @Override
    protected Set<SocketOption<?>> supportedOptions() {
        return delegate.supportedOptions();
    }

    @Override
    protected <T> void setOption(SocketOption<T> opt, T value) throws IOException {
        delegate.setOption(opt, value);
    }

    @Override
    protected <T> T getOption(SocketOption<T> opt) throws IOException {
        return delegate.getOption(opt);
    }

    @Override
    public void setOption(int optID, Object value) throws SocketException {
        delegate.setOption(optID, value);
    }

    @Override
    public Object getOption(int optID) throws SocketException {
        return delegate.getOption(optID);
    }

    @Override
    protected void shutdownInput() throws IOException {
        delegate.shutdownInput();
    }

    @Override
    protected void shutdownOutput() throws IOException {
        delegate.shutdownOutput();
    }
}
