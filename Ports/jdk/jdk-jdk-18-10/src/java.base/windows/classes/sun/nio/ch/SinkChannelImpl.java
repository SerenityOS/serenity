/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 */

package sun.nio.ch;

import java.io.IOException;
import java.io.FileDescriptor;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.*;
import java.util.Objects;


/**
 * Pipe.SinkChannel implementation based on socket connection.
 */

class SinkChannelImpl
    extends Pipe.SinkChannel
    implements SelChImpl
{
    // The SocketChannel assoicated with this pipe
    private final SocketChannelImpl sc;

    public FileDescriptor getFD() {
        return sc.getFD();
    }

    public int getFDVal() {
        return sc.getFDVal();
    }

    SinkChannelImpl(SelectorProvider sp, SocketChannel sc) {
        super(sp);
        this.sc = (SocketChannelImpl) sc;
    }

    boolean isNetSocket() {
        return sc.isNetSocket();
    }

    <T> void setOption(SocketOption<T> name, T value) throws IOException {
        sc.setOption(name, value);
    }

    protected void implCloseSelectableChannel() throws IOException {
        if (!isRegistered())
            kill();
    }

    public void kill() throws IOException {
        sc.close();
    }

    protected void implConfigureBlocking(boolean block) throws IOException {
        sc.configureBlocking(block);
    }

    public boolean translateReadyOps(int ops, int initialOps, SelectionKeyImpl ski) {
        int intOps = ski.nioInterestOps();
        int oldOps = ski.nioReadyOps();
        int newOps = initialOps;

        if ((ops & Net.POLLNVAL) != 0)
            throw new Error("POLLNVAL detected");

        if ((ops & (Net.POLLERR | Net.POLLHUP)) != 0) {
            newOps = intOps;
            ski.nioReadyOps(newOps);
            return (newOps & ~oldOps) != 0;
        }

        if (((ops & Net.POLLOUT) != 0) &&
            ((intOps & SelectionKey.OP_WRITE) != 0))
            newOps |= SelectionKey.OP_WRITE;

        ski.nioReadyOps(newOps);
        return (newOps & ~oldOps) != 0;
    }

    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl ski) {
        return translateReadyOps(ops, ski.nioReadyOps(), ski);
    }

    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl ski) {
        return translateReadyOps(ops, 0, ski);
    }

    public int translateInterestOps(int ops) {
        int newOps = 0;
        if ((ops & SelectionKey.OP_WRITE) != 0)
            newOps |= Net.POLLOUT;
        return newOps;
    }

    public int write(ByteBuffer src) throws IOException {
        try {
            return sc.write(src);
        } catch (AsynchronousCloseException x) {
            close();
            throw x;
        }
    }

    public long write(ByteBuffer[] srcs) throws IOException {
        try {
            return sc.write(srcs);
        } catch (AsynchronousCloseException x) {
            close();
            throw x;
        }
    }

    public long write(ByteBuffer[] srcs, int offset, int length)
        throws IOException
    {
        Objects.checkFromIndexSize(offset, length, srcs.length);
        try {
            return write(Util.subsequence(srcs, offset, length));
        } catch (AsynchronousCloseException x) {
            close();
            throw x;
        }
    }
}
