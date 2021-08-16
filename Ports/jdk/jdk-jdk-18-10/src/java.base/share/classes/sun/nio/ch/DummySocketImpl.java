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

package sun.nio.ch;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketImpl;
import java.net.SocketOption;
import java.util.Set;

/**
 * Dummy SocketImpl for use by the socket adaptors. All methods are overridden
 * to throw an error.
 */

class DummySocketImpl extends SocketImpl {
    private DummySocketImpl() { }

    static SocketImpl create() {
        return new DummySocketImpl();
    }

    private static <T> T shouldNotGetHere() {
        throw new InternalError("Should not get here");
    }

    @Override
    protected void create(boolean stream) {
        shouldNotGetHere();
    }

    @Override
    protected void connect(SocketAddress remote, int millis) {
        shouldNotGetHere();
    }

    @Override
    protected void connect(String host, int port) {
        shouldNotGetHere();
    }

    @Override
    protected void connect(InetAddress address, int port) {
        shouldNotGetHere();
    }

    @Override
    protected void bind(InetAddress host, int port) {
        shouldNotGetHere();
    }

    @Override
    protected void listen(int backlog) {
        shouldNotGetHere();
    }

    @Override
    protected void accept(SocketImpl si) {
        shouldNotGetHere();
    }

    @Override
    protected InputStream getInputStream() {
        return shouldNotGetHere();
    }
    @Override
    protected OutputStream getOutputStream() {
        return shouldNotGetHere();
    }
    @Override
    protected int available() {
        return shouldNotGetHere();
    }

    @Override
    protected void close() {
        shouldNotGetHere();
    }

    @Override
    protected Set<SocketOption<?>> supportedOptions() {
        return shouldNotGetHere();
    }

    @Override
    protected <T> void setOption(SocketOption<T> opt, T value) {
        shouldNotGetHere();
    }

    @Override
    protected <T> T getOption(SocketOption<T> opt) {
        return shouldNotGetHere();
    }

    @Override
    public void setOption(int opt, Object value) {
        shouldNotGetHere();
    }

    @Override
    public Object getOption(int opt) {
        return shouldNotGetHere();
    }

    @Override
    protected void shutdownInput() {
        shouldNotGetHere();
    }

    @Override
    protected void shutdownOutput() {
        shouldNotGetHere();
    }

    @Override
    protected boolean supportsUrgentData() {
        return shouldNotGetHere();
    }

    @Override
    protected void sendUrgentData(int data) {
        shouldNotGetHere();
    }
}
