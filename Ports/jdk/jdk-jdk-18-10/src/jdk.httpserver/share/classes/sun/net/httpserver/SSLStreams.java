/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.httpserver;

import java.net.*;
import java.nio.*;
import java.io.*;
import java.nio.channels.*;
import java.util.concurrent.locks.*;
import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.*;
import com.sun.net.httpserver.*;

/**
 * given a non-blocking SocketChannel, it produces
 * (blocking) streams which encrypt/decrypt the SSL content
 * and handle the SSL handshaking automatically.
 */

class SSLStreams {

    SSLContext sslctx;
    SocketChannel chan;
    TimeSource time;
    ServerImpl server;
    SSLEngine engine;
    EngineWrapper wrapper;
    OutputStream os;
    InputStream is;

    /* held by thread doing the hand-shake on this connection */
    Lock handshaking = new ReentrantLock();

    SSLStreams (ServerImpl server, SSLContext sslctx, SocketChannel chan) throws IOException {
        this.server = server;
        this.time= (TimeSource)server;
        this.sslctx= sslctx;
        this.chan= chan;
        InetSocketAddress addr =
                (InetSocketAddress)chan.socket().getRemoteSocketAddress();
        engine = sslctx.createSSLEngine (addr.getHostName(), addr.getPort());
        engine.setUseClientMode (false);
        HttpsConfigurator cfg = server.getHttpsConfigurator();
        configureEngine (cfg, addr);
        wrapper = new EngineWrapper (chan, engine);
    }

    private void configureEngine(HttpsConfigurator cfg, InetSocketAddress addr){
        if (cfg != null) {
            Parameters params = new Parameters (cfg, addr);
//BEGIN_TIGER_EXCLUDE
            cfg.configure (params);
            SSLParameters sslParams = params.getSSLParameters();
            if (sslParams != null) {
                engine.setSSLParameters (sslParams);
            } else
//END_TIGER_EXCLUDE
            {
                /* tiger compatibility */
                if (params.getCipherSuites() != null) {
                    try {
                        engine.setEnabledCipherSuites (
                            params.getCipherSuites()
                        );
                    } catch (IllegalArgumentException e) { /* LOG */}
                }
                engine.setNeedClientAuth (params.getNeedClientAuth());
                engine.setWantClientAuth (params.getWantClientAuth());
                if (params.getProtocols() != null) {
                    try {
                        engine.setEnabledProtocols (
                            params.getProtocols()
                        );
                    } catch (IllegalArgumentException e) { /* LOG */}
                }
            }
        }
    }

    class Parameters extends HttpsParameters {
        InetSocketAddress addr;
        HttpsConfigurator cfg;

        Parameters (HttpsConfigurator cfg, InetSocketAddress addr) {
            this.addr = addr;
            this.cfg = cfg;
        }
        public InetSocketAddress getClientAddress () {
            return addr;
        }
        public HttpsConfigurator getHttpsConfigurator() {
            return cfg;
        }
//BEGIN_TIGER_EXCLUDE
        SSLParameters params;
        public void setSSLParameters (SSLParameters p) {
            params = p;
        }
        SSLParameters getSSLParameters () {
            return params;
        }
//END_TIGER_EXCLUDE
    }

    /**
     * cleanup resources allocated inside this object
     */
    void close () throws IOException {
        wrapper.close();
    }

    /**
     * return the SSL InputStream
     */
    InputStream getInputStream () throws IOException {
        if (is == null) {
            is = new InputStream();
        }
        return is;
    }

    /**
     * return the SSL OutputStream
     */
    OutputStream getOutputStream () throws IOException {
        if (os == null) {
            os = new OutputStream();
        }
        return os;
    }

    SSLEngine getSSLEngine () {
        return engine;
    }

    /**
     * request the engine to repeat the handshake on this session
     * the handshake must be driven by reads/writes on the streams
     * Normally, not necessary to call this.
     */
    void beginHandshake() throws SSLException {
        engine.beginHandshake();
    }

    class WrapperResult {
        SSLEngineResult result;

        /* if passed in buffer was not big enough then the
         * a reallocated buffer is returned here
         */
        ByteBuffer buf;
    }

    int app_buf_size;
    int packet_buf_size;

    enum BufType {
        PACKET, APPLICATION
    };

    private ByteBuffer allocate (BufType type) {
        return allocate (type, -1);
    }

    private ByteBuffer allocate (BufType type, int len) {
        assert engine != null;
        synchronized (this) {
            int size;
            if (type == BufType.PACKET) {
                if (packet_buf_size == 0) {
                    SSLSession sess = engine.getSession();
                    packet_buf_size = sess.getPacketBufferSize();
                }
                if (len > packet_buf_size) {
                    packet_buf_size = len;
                }
                size = packet_buf_size;
            } else {
                if (app_buf_size == 0) {
                    SSLSession sess = engine.getSession();
                    app_buf_size = sess.getApplicationBufferSize();
                }
                if (len > app_buf_size) {
                    app_buf_size = len;
                }
                size = app_buf_size;
            }
            return ByteBuffer.allocate (size);
        }
    }

    /* reallocates the buffer by :-
     * 1. creating a new buffer double the size of the old one
     * 2. putting the contents of the old buffer into the new one
     * 3. set xx_buf_size to the new size if it was smaller than new size
     *
     * flip is set to true if the old buffer needs to be flipped
     * before it is copied.
     */
    private ByteBuffer realloc (ByteBuffer b, boolean flip, BufType type) {
        synchronized (this) {
            int nsize = 2 * b.capacity();
            ByteBuffer n = allocate (type, nsize);
            if (flip) {
                b.flip();
            }
            n.put(b);
            b = n;
        }
        return b;
    }
    /**
     * This is a thin wrapper over SSLEngine and the SocketChannel,
     * which guarantees the ordering of wraps/unwraps with respect to the underlying
     * channel read/writes. It handles the UNDER/OVERFLOW status codes
     * It does not handle the handshaking status codes, or the CLOSED status code
     * though once the engine is closed, any attempt to read/write to it
     * will get an exception.  The overall result is returned.
     * It functions synchronously/blocking
     */
    class EngineWrapper {

        SocketChannel chan;
        SSLEngine engine;
        Object wrapLock, unwrapLock;
        ByteBuffer unwrap_src, wrap_dst;
        boolean closed = false;
        int u_remaining; // the number of bytes left in unwrap_src after an unwrap()

        EngineWrapper (SocketChannel chan, SSLEngine engine) throws IOException {
            this.chan = chan;
            this.engine = engine;
            wrapLock = new Object();
            unwrapLock = new Object();
            unwrap_src = allocate(BufType.PACKET);
            wrap_dst = allocate(BufType.PACKET);
        }

        void close () throws IOException {
        }

        /* try to wrap and send the data in src. Handles OVERFLOW.
         * Might block if there is an outbound blockage or if another
         * thread is calling wrap(). Also, might not send any data
         * if an unwrap is needed.
         */
        WrapperResult wrapAndSend(ByteBuffer src) throws IOException {
            return wrapAndSendX(src, false);
        }

        WrapperResult wrapAndSendX(ByteBuffer src, boolean ignoreClose) throws IOException {
            if (closed && !ignoreClose) {
                throw new IOException ("Engine is closed");
            }
            Status status;
            WrapperResult r = new WrapperResult();
            synchronized (wrapLock) {
                wrap_dst.clear();
                do {
                    r.result = engine.wrap (src, wrap_dst);
                    status = r.result.getStatus();
                    if (status == Status.BUFFER_OVERFLOW) {
                        wrap_dst = realloc (wrap_dst, true, BufType.PACKET);
                    }
                } while (status == Status.BUFFER_OVERFLOW);
                if (status == Status.CLOSED && !ignoreClose) {
                    closed = true;
                    return r;
                }
                if (r.result.bytesProduced() > 0) {
                    wrap_dst.flip();
                    int l = wrap_dst.remaining();
                    assert l == r.result.bytesProduced();
                    while (l>0) {
                        l -= chan.write (wrap_dst);
                    }
                }
            }
            return r;
        }

        /* block until a complete message is available and return it
         * in dst, together with the Result. dst may have been re-allocated
         * so caller should check the returned value in Result
         * If handshaking is in progress then, possibly no data is returned
         */
        WrapperResult recvAndUnwrap(ByteBuffer dst) throws IOException {
            Status status = Status.OK;
            WrapperResult r = new WrapperResult();
            r.buf = dst;
            if (closed) {
                throw new IOException ("Engine is closed");
            }
            boolean needData;
            if (u_remaining > 0) {
                unwrap_src.compact();
                unwrap_src.flip();
                needData = false;
            } else {
                unwrap_src.clear();
                needData = true;
            }
            synchronized (unwrapLock) {
                int x;
                do {
                    if (needData) {
                        do {
                        x = chan.read (unwrap_src);
                        } while (x == 0);
                        if (x == -1) {
                            throw new IOException ("connection closed for reading");
                        }
                        unwrap_src.flip();
                    }
                    r.result = engine.unwrap (unwrap_src, r.buf);
                    status = r.result.getStatus();
                    if (status == Status.BUFFER_UNDERFLOW) {
                        if (unwrap_src.limit() == unwrap_src.capacity()) {
                            /* buffer not big enough */
                            unwrap_src = realloc (
                                unwrap_src, false, BufType.PACKET
                            );
                        } else {
                            /* Buffer not full, just need to read more
                             * data off the channel. Reset pointers
                             * for reading off SocketChannel
                             */
                            unwrap_src.position (unwrap_src.limit());
                            unwrap_src.limit (unwrap_src.capacity());
                        }
                        needData = true;
                    } else if (status == Status.BUFFER_OVERFLOW) {
                        r.buf = realloc (r.buf, true, BufType.APPLICATION);
                        needData = false;
                    } else if (status == Status.CLOSED) {
                        closed = true;
                        r.buf.flip();
                        return r;
                    }
                } while (status != Status.OK);
            }
            u_remaining = unwrap_src.remaining();
            return r;
        }
    }

    /**
     * send the data in the given ByteBuffer. If a handshake is needed
     * then this is handled within this method. When this call returns,
     * all of the given user data has been sent and any handshake has been
     * completed. Caller should check if engine has been closed.
     */
    public WrapperResult sendData (ByteBuffer src) throws IOException {
        WrapperResult r=null;
        while (src.remaining() > 0) {
            r = wrapper.wrapAndSend(src);
            Status status = r.result.getStatus();
            if (status == Status.CLOSED) {
                doClosure ();
                return r;
            }
            HandshakeStatus hs_status = r.result.getHandshakeStatus();
            if (hs_status != HandshakeStatus.FINISHED &&
                hs_status != HandshakeStatus.NOT_HANDSHAKING)
            {
                doHandshake(hs_status);
            }
        }
        return r;
    }

    /**
     * read data thru the engine into the given ByteBuffer. If the
     * given buffer was not large enough, a new one is allocated
     * and returned. This call handles handshaking automatically.
     * Caller should check if engine has been closed.
     */
    public WrapperResult recvData (ByteBuffer dst) throws IOException {
        /* we wait until some user data arrives */
        WrapperResult r = null;
        assert dst.position() == 0;
        while (dst.position() == 0) {
            r = wrapper.recvAndUnwrap (dst);
            dst = (r.buf != dst) ? r.buf: dst;
            Status status = r.result.getStatus();
            if (status == Status.CLOSED) {
                doClosure ();
                return r;
            }

            HandshakeStatus hs_status = r.result.getHandshakeStatus();
            if (hs_status != HandshakeStatus.FINISHED &&
                hs_status != HandshakeStatus.NOT_HANDSHAKING)
            {
                doHandshake (hs_status);
            }
        }
        dst.flip();
        return r;
    }

    /* we've received a close notify. Need to call wrap to send
     * the response
     */
    void doClosure () throws IOException {
        try {
            handshaking.lock();
            ByteBuffer tmp = allocate(BufType.APPLICATION);
            WrapperResult r;
            Status st;
            HandshakeStatus hs;
            do {
                tmp.clear();
                tmp.flip ();
                r = wrapper.wrapAndSendX (tmp, true);
                hs = r.result.getHandshakeStatus();
                st = r.result.getStatus();
            } while (st != Status.CLOSED &&
                        !(st == Status.OK && hs == HandshakeStatus.NOT_HANDSHAKING));
        } finally {
            handshaking.unlock();
        }
    }

    /* do the (complete) handshake after acquiring the handshake lock.
     * If two threads call this at the same time, then we depend
     * on the wrapper methods being idempotent. eg. if wrapAndSend()
     * is called with no data to send then there must be no problem
     */
    @SuppressWarnings("fallthrough")
    void doHandshake (HandshakeStatus hs_status) throws IOException {
        try {
            handshaking.lock();
            ByteBuffer tmp = allocate(BufType.APPLICATION);
            while (hs_status != HandshakeStatus.FINISHED &&
                   hs_status != HandshakeStatus.NOT_HANDSHAKING)
            {
                WrapperResult r = null;
                switch (hs_status) {
                    case NEED_TASK:
                        Runnable task;
                        while ((task = engine.getDelegatedTask()) != null) {
                            /* run in current thread, because we are already
                             * running an external Executor
                             */
                            task.run();
                        }
                        /* fall thru - call wrap again */
                    case NEED_WRAP:
                        tmp.clear();
                        tmp.flip();
                        r = wrapper.wrapAndSend(tmp);
                        break;

                    case NEED_UNWRAP:
                        tmp.clear();
                        r = wrapper.recvAndUnwrap (tmp);
                        if (r.buf != tmp) {
                            tmp = r.buf;
                        }
                        assert tmp.position() == 0;
                        break;
                }
                hs_status = r.result.getHandshakeStatus();
            }
        } finally {
            handshaking.unlock();
        }
    }

    /**
     * represents an SSL input stream. Multiple https requests can
     * be sent over one stream. closing this stream causes an SSL close
     * input.
     */
    class InputStream extends java.io.InputStream {

        ByteBuffer bbuf;
        boolean closed = false;

        /* this stream eof */
        boolean eof = false;

        boolean needData = true;

        InputStream () {
            bbuf = allocate (BufType.APPLICATION);
        }

        public int read (byte[] buf, int off, int len) throws IOException {
            if (closed) {
                throw new IOException ("SSL stream is closed");
            }
            if (eof) {
                return 0;
            }
            int available=0;
            if (!needData) {
                available = bbuf.remaining();
                needData = (available==0);
            }
            if (needData) {
                bbuf.clear();
                WrapperResult r = recvData (bbuf);
                bbuf = r.buf== bbuf? bbuf: r.buf;
                if ((available=bbuf.remaining()) == 0) {
                    eof = true;
                    return 0;
                } else {
                    needData = false;
                }
            }
            /* copy as much as possible from buf into users buf */
            if (len > available) {
                len = available;
            }
            bbuf.get (buf, off, len);
            return len;
        }

        public int available () throws IOException {
            return bbuf.remaining();
        }

        public boolean markSupported () {
            return false; /* not possible with SSLEngine */
        }

        public void reset () throws IOException {
            throw new IOException ("mark/reset not supported");
        }

        public long skip (long s) throws IOException {
            int n = (int)s;
            if (closed) {
                throw new IOException ("SSL stream is closed");
            }
            if (eof) {
                return 0;
            }
            int ret = n;
            while (n > 0) {
                if (bbuf.remaining() >= n) {
                    bbuf.position (bbuf.position()+n);
                    return ret;
                } else {
                    n -= bbuf.remaining();
                    bbuf.clear();
                    WrapperResult r = recvData (bbuf);
                    bbuf = r.buf==bbuf? bbuf: r.buf;
                }
            }
            return ret; /* not reached */
        }

        /**
         * close the SSL connection. All data must have been consumed
         * before this is called. Otherwise an exception will be thrown.
         * [Note. May need to revisit this. not quite the normal close() symantics
         */
        public void close () throws IOException {
            eof = true;
            engine.closeInbound ();
        }

        public int read (byte[] buf) throws IOException {
            return read (buf, 0, buf.length);
        }

        byte single[] = new byte [1];

        public int read () throws IOException {
            int n = read (single, 0, 1);
            if (n == 0) {
                return -1;
            } else {
                return single[0] & 0xFF;
            }
        }
    }

    /**
     * represents an SSL output stream. plain text data written to this stream
     * is encrypted by the stream. Multiple HTTPS responses can be sent on
     * one stream. closing this stream initiates an SSL closure
     */
    class OutputStream extends java.io.OutputStream {
        ByteBuffer buf;
        boolean closed = false;
        byte single[] = new byte[1];

        OutputStream() {
            buf = allocate(BufType.APPLICATION);
        }

        public void write(int b) throws IOException {
            single[0] = (byte)b;
            write (single, 0, 1);
        }

        public void write(byte b[]) throws IOException {
            write (b, 0, b.length);
        }
        public void write(byte b[], int off, int len) throws IOException {
            if (closed) {
                throw new IOException ("output stream is closed");
            }
            while (len > 0) {
                int l = len > buf.capacity() ? buf.capacity() : len;
                buf.clear();
                buf.put (b, off, l);
                len -= l;
                off += l;
                buf.flip();
                WrapperResult r = sendData (buf);
                if (r.result.getStatus() == Status.CLOSED) {
                    closed = true;
                    if (len > 0) {
                        throw new IOException ("output stream is closed");
                    }
                }
            }
        }

        public void flush() throws IOException {
            /* no-op */
        }

        public void close() throws IOException {
            WrapperResult r=null;
            engine.closeOutbound();
            closed = true;
            HandshakeStatus stat = HandshakeStatus.NEED_WRAP;
            buf.clear();
            while (stat == HandshakeStatus.NEED_WRAP) {
                r = wrapper.wrapAndSend (buf);
                stat = r.result.getHandshakeStatus();
            }
            assert r.result.getStatus() == Status.CLOSED
                    : "status is: " + r.result.getStatus()
                    + ", handshakeStatus is: " + stat;
        }
    }
}
