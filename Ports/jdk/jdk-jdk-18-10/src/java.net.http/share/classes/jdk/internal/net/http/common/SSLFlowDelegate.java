/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.common;

import jdk.internal.net.http.common.SubscriberWrapper.SchedulingAction;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;

/**
 * Implements SSL using two SubscriberWrappers.
 *
 * <p> Constructor takes two Flow.Subscribers: one that receives the network
 * data (after it has been encrypted by SSLFlowDelegate) data, and one that
 * receives the application data (before it has been encrypted by SSLFlowDelegate).
 *
 * <p> Methods upstreamReader() and upstreamWriter() return the corresponding
 * Flow.Subscribers containing Flows for the encrypted/decrypted upstream data.
 * See diagram below.
 *
 * <p> How Flow.Subscribers are used in this class, and where they come from:
 * <pre>
 * {@code
 *
 *
 *
 * --------->  data flow direction
 *
 *
 *                         +------------------+
 *        upstreamWriter   |                  | downWriter
 *        ---------------> |                  | ------------>
 *  obtained from this     |                  | supplied to constructor
 *                         | SSLFlowDelegate  |
 *        downReader       |                  | upstreamReader
 *        <--------------- |                  | <--------------
 * supplied to constructor |                  | obtained from this
 *                         +------------------+
 *
 * Errors are reported to the downReader Flow.Subscriber
 *
 * }
 * </pre>
 */
public class SSLFlowDelegate {

    final Logger debug =
            Utils.getDebugLogger(this::dbgString, Utils.DEBUG);

    private static final ByteBuffer SENTINEL = Utils.EMPTY_BYTEBUFFER;
    private static final ByteBuffer HS_TRIGGER = ByteBuffer.allocate(0);
    // When handshake is in progress trying to wrap may produce no bytes.
    private static final ByteBuffer NOTHING = ByteBuffer.allocate(0);
    private static final String monProp = Utils.getProperty("jdk.internal.httpclient.monitorFlowDelegate");
    private static final boolean isMonitored =
            monProp != null && (monProp.isEmpty() || monProp.equalsIgnoreCase("true"));

    final Executor exec;
    final Reader reader;
    final Writer writer;
    final SSLEngine engine;
    final String tubeName; // hack
    final CompletableFuture<String> alpnCF; // completes on initial handshake
    final Monitorable monitor = isMonitored ? this::monitor : null; // prevent GC until SSLFD is stopped
    volatile boolean close_notify_received;
    final CompletableFuture<Void> readerCF;
    final CompletableFuture<Void> writerCF;
    final CompletableFuture<Void> stopCF;
    final Consumer<ByteBuffer> recycler;
    static AtomicInteger scount = new AtomicInteger(1);
    final int id;

    /**
     * Creates an SSLFlowDelegate fed from two Flow.Subscribers. Each
     * Flow.Subscriber requires an associated {@link CompletableFuture}
     * for errors that need to be signaled from downstream to upstream.
     */
    public SSLFlowDelegate(SSLEngine engine,
                           Executor exec,
                           Subscriber<? super List<ByteBuffer>> downReader,
                           Subscriber<? super List<ByteBuffer>> downWriter)
    {
        this(engine, exec, null, downReader, downWriter);
    }

    /**
     * Creates an SSLFlowDelegate fed from two Flow.Subscribers. Each
     * Flow.Subscriber requires an associated {@link CompletableFuture}
     * for errors that need to be signaled from downstream to upstream.
     */
    public SSLFlowDelegate(SSLEngine engine,
            Executor exec,
            Consumer<ByteBuffer> recycler,
            Subscriber<? super List<ByteBuffer>> downReader,
            Subscriber<? super List<ByteBuffer>> downWriter)
        {
        this.id = scount.getAndIncrement();
        this.tubeName = String.valueOf(downWriter);
        this.recycler = recycler;
        this.reader = new Reader();
        this.writer = new Writer();
        this.engine = engine;
        this.exec = exec;
        this.handshakeState = new AtomicInteger(NOT_HANDSHAKING);
        this.readerCF = reader.completion();
        this.writerCF = reader.completion();
        readerCF.exceptionally(this::stopOnError);
        writerCF.exceptionally(this::stopOnError);
        this.stopCF = CompletableFuture.allOf(reader.completion(), writer.completion())
            .thenRun(this::normalStop);
        this.alpnCF = new MinimalFuture<>();

        // connect the Reader to the downReader and the
        // Writer to the downWriter.
        connect(downReader, downWriter);

        if (isMonitored) Monitor.add(monitor);
    }

    /**
     * Returns true if the SSLFlowDelegate has detected a TLS
     * close_notify from the server.
     * @return true, if a close_notify was detected.
     */
    public boolean closeNotifyReceived() {
        return close_notify_received;
    }

    /**
     * Connects the read sink (downReader) to the SSLFlowDelegate Reader,
     * and the write sink (downWriter) to the SSLFlowDelegate Writer.
     * Called from within the constructor. Overwritten by SSLTube.
     *
     * @param downReader  The left hand side read sink (typically, the
     *                    HttpConnection read subscriber).
     * @param downWriter  The right hand side write sink (typically
     *                    the SocketTube write subscriber).
     */
    void connect(Subscriber<? super List<ByteBuffer>> downReader,
                 Subscriber<? super List<ByteBuffer>> downWriter) {
        this.reader.subscribe(downReader);
        this.writer.subscribe(downWriter);
    }

   /**
    * Returns a CompletableFuture<String> which completes after
    * the initial handshake completes, and which contains the negotiated
    * alpn.
    */
    public CompletableFuture<String> alpn() {
        return alpnCF;
    }

    private void setALPN() {
        // Handshake is finished. So, can retrieve the ALPN now
        if (alpnCF.isDone())
            return;
        String alpn = engine.getApplicationProtocol();
        if (debug.on()) debug.log("setALPN = %s", alpn);
        alpnCF.complete(alpn);
    }

    public String monitor() {
        StringBuilder sb = new StringBuilder();
        sb.append("SSL: id ").append(id);
        sb.append(" ").append(dbgString());
        sb.append(" HS state: " + states(handshakeState));
        sb.append(" Engine state: " + engine.getHandshakeStatus().toString());
        if (stateList != null) {
            sb.append(" LL : ");
            for (String s : stateList) {
                sb.append(s).append(" ");
            }
        }
        sb.append("\r\n");
        sb.append("Reader:: ").append(reader.toString());
        sb.append("\r\n");
        sb.append("Writer:: ").append(writer.toString());
        sb.append("\r\n===================================");
        return sb.toString();
    }

    protected SchedulingAction enterReadScheduling() {
        return SchedulingAction.CONTINUE;
    }

    protected Throwable checkForHandshake(Throwable t) {
        return t;
    }


    /**
     * Processing function for incoming data. Pass it thru SSLEngine.unwrap().
     * Any decrypted buffers returned to be passed downstream.
     * Status codes:
     *     NEED_UNWRAP: do nothing. Following incoming data will contain
     *                  any required handshake data
     *     NEED_WRAP: call writer.addData() with empty buffer
     *     NEED_TASK: delegate task to executor
     *     BUFFER_OVERFLOW: allocate larger output buffer. Repeat unwrap
     *     BUFFER_UNDERFLOW: keep buffer and wait for more data
     *     OK: return generated buffers.
     *
     * Upstream subscription strategy is to try and keep no more than
     * TARGET_BUFSIZE bytes in readBuf
     */
    final class Reader extends SubscriberWrapper implements FlowTube.TubeSubscriber {
        // Maximum record size is 16k.
        // Because SocketTube can feeds us up to 3 16K buffers,
        // then setting this size to 16K means that the readBuf
        // can store up to 64K-1 (16K-1 + 3*16K)
        static final int TARGET_BUFSIZE = 16 * 1024;

        final SequentialScheduler scheduler;
        volatile ByteBuffer readBuf;
        volatile boolean completing;
        final Object readBufferLock = new Object();
        final Logger debugr = Utils.getDebugLogger(this::dbgString, Utils.DEBUG);

        private final class ReaderDownstreamPusher implements Runnable {
            @Override
            public void run() {
                processData();
            }
        }

        Reader() {
            super();
            scheduler = SequentialScheduler.lockingScheduler(
                    new ReaderDownstreamPusher());
            this.readBuf = ByteBuffer.allocate(1024);
            readBuf.limit(0); // keep in read mode
        }

        @Override
        public boolean supportsRecycling() {
            return recycler != null;
        }

        protected SchedulingAction enterScheduling() {
            return enterReadScheduling();
        }

        public final String dbgString() {
            return "SSL Reader(" + tubeName + ")";
        }

        /**
         * entry point for buffers delivered from upstream Subscriber
         */
        @Override
        public void incoming(List<ByteBuffer> buffers, boolean complete) {
            if (debugr.on())
                debugr.log("Adding %d bytes to read buffer",
                        Utils.remaining(buffers));
            addToReadBuf(buffers, complete);
            scheduler.runOrSchedule(exec);
        }

        @Override
        public String toString() {
            return "READER: " + super.toString() + ", readBuf: " + readBuf.toString()
                    + ", count: " + count.toString() + ", scheduler: "
                    + (scheduler.isStopped() ? "stopped" : "running")
                    + ", status: " + lastUnwrapStatus
                    + ", handshakeState: " + handshakeState.get()
                    + ", engine: " + engine.getHandshakeStatus();
        }

        private void reallocReadBuf() {
            int sz = readBuf.capacity();
            ByteBuffer newb = ByteBuffer.allocate(sz * 2);
            readBuf.flip();
            Utils.copy(readBuf, newb);
            readBuf = newb;
        }

        @Override
        protected long upstreamWindowUpdate(long currentWindow, long downstreamQsize) {
            if (needsMoreData()) {
                // run the scheduler to see if more data should be requested
                if (debugr.on()) {
                    int remaining = readBuf.remaining();
                    if (remaining > TARGET_BUFSIZE) {
                        // just some logging to check how much we have in the read buffer
                        debugr.log("readBuf has more than TARGET_BUFSIZE: %d",
                                remaining);
                    }
                }
                scheduler.runOrSchedule();
            }
            return 0; // we will request more from the scheduler loop (processData).
        }

        // readBuf is kept ready for reading outside of this method
        private void addToReadBuf(List<ByteBuffer> buffers, boolean complete) {
            assert Utils.remaining(buffers) > 0 || buffers.isEmpty();
            synchronized (readBufferLock) {
                for (ByteBuffer buf : buffers) {
                    readBuf.compact();
                    while (readBuf.remaining() < buf.remaining())
                        reallocReadBuf();
                    readBuf.put(buf);
                    readBuf.flip();
                    // should be safe to call inside lock
                    // since the only implementation
                    // offers the buffer to an unbounded queue.
                    // WARNING: do not touch buf after this point!
                    if (recycler != null) recycler.accept(buf);
                }
                if (complete) {
                    this.completing = complete;
                    minBytesRequired = 0;
                }
            }
        }

        @Override
        protected boolean errorCommon(Throwable throwable) {
            throwable = SSLFlowDelegate.this.checkForHandshake(throwable);
            return super.errorCommon(throwable);
        }

        void schedule() {
            scheduler.runOrSchedule(exec);
        }

        void stop() {
            if (debugr.on()) debugr.log("stop");
            scheduler.stop();
        }

        AtomicInteger count = new AtomicInteger();

        // minimum number of bytes required to call unwrap.
        // Usually this is 0, unless there was a buffer underflow.
        // In this case we need to wait for more bytes than what
        // we had before calling unwrap() again.
        volatile int minBytesRequired;

        // We might need to request more data if:
        //  - we have a subscription from upstream
        //  - and we don't have enough data to decrypt in the read buffer
        //  - *and* - either we're handshaking, and more data is required (NEED_UNWRAP),
        //          - or we have demand from downstream, but we have nothing decrypted
        //            to forward downstream.
        boolean needsMoreData() {
            if (upstreamSubscription != null && readBuf.remaining() <= minBytesRequired &&
                    (engine.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP
                            || !downstreamSubscription.demand.isFulfilled() && hasNoOutputData())) {
                return true;
            }
            return false;
        }

        // If the readBuf has not enough data, and we either need to
        // unwrap (handshaking) or we have demand from downstream,
        // then request more data
        void requestMoreDataIfNeeded() {
            if (needsMoreData()) {
                // request more will only request more if our
                // demand from upstream is fulfilled
                requestMore();
            }
        }

        // work function where it all happens
        final void processData() {
            try {
                if (debugr.on())
                    debugr.log("processData:"
                            + " readBuf remaining:" + readBuf.remaining()
                            + ", state:" + states(handshakeState)
                            + ", engine handshake status:" + engine.getHandshakeStatus());
                int len;
                boolean complete = false;
                while (readBuf.remaining() > (len = minBytesRequired)) {
                    boolean handshaking = false;
                    try {
                        EngineResult result;
                        synchronized (readBufferLock) {
                            complete = this.completing;
                            if (debugr.on()) debugr.log("Unwrapping: %s", readBuf.remaining());
                            // Unless there is a BUFFER_UNDERFLOW, we should try to
                            // unwrap any number of bytes. Set minBytesRequired to 0:
                            // we only need to do that if minBytesRequired is not already 0.
                            len = len > 0 ? minBytesRequired = 0 : len;
                            result = unwrapBuffer(readBuf);
                            len = readBuf.remaining();
                            if (debugr.on()) {
                                debugr.log("Unwrapped: result: %s", result.result);
                                debugr.log("Unwrapped: consumed: %s", result.bytesConsumed());
                            }
                        }
                        if (result.bytesProduced() > 0) {
                            if (debugr.on())
                                debugr.log("sending %d", result.bytesProduced());
                            count.addAndGet(result.bytesProduced());
                            outgoing(result.destBuffer, false);
                        }
                        if (result.status() == Status.BUFFER_UNDERFLOW) {
                            if (debugr.on()) debugr.log("BUFFER_UNDERFLOW");
                            // not enough data in the read buffer...
                            // no need to try to unwrap again unless we get more bytes
                            // than minBytesRequired = len in the read buffer.
                            synchronized (readBufferLock) {
                                minBytesRequired = len;
                                // more bytes could already have been added...
                                assert readBuf.remaining() >= len;
                                // check if we have received some data, and if so
                                // we can just re-spin the loop
                                if (readBuf.remaining() > len) continue;
                                else if (this.completing) {
                                    if (debug.on()) {
                                        debugr.log("BUFFER_UNDERFLOW with EOF," +
                                                " %d bytes non decrypted.", len);
                                    }
                                    // The channel won't send us any more data, and
                                    // we are in underflow: we need to fail.
                                    throw new IOException("BUFFER_UNDERFLOW with EOF, "
                                            + len + " bytes non decrypted.");
                                }
                            }
                            // request more data and return.
                            requestMore();
                            return;
                        }
                        if (complete && result.status() == Status.CLOSED) {
                            if (debugr.on()) debugr.log("Closed: completing");
                            outgoing(Utils.EMPTY_BB_LIST, true);
                            // complete ALPN if not yet completed
                            setALPN();
                            requestMoreDataIfNeeded();
                            return;
                        }
                        if (result.handshaking()) {
                            handshaking = true;
                            if (debugr.on()) debugr.log("handshaking");
                            if (doHandshake(result, READER)) continue; // need unwrap
                            else break; // doHandshake will have triggered the write scheduler if necessary
                        } else {
                            if (trySetALPN()) {
                                resumeActivity();
                            }
                        }
                    } catch (IOException ex) {
                        Throwable cause = checkForHandshake(ex);
                        errorCommon(cause);
                        handleError(cause);
                        return;
                    }
                    if (handshaking && !complete) {
                        requestMoreDataIfNeeded();
                        return;
                    }
                }
                if (!complete) {
                    synchronized (readBufferLock) {
                        complete = this.completing && !readBuf.hasRemaining();
                    }
                }
                if (complete) {
                    if (debugr.on()) debugr.log("completing");
                    // Complete the alpnCF, if not already complete, regardless of
                    // whether or not the ALPN is available, there will be no more
                    // activity.
                    setALPN();
                    outgoing(Utils.EMPTY_BB_LIST, true);
                } else {
                    requestMoreDataIfNeeded();
                }
            } catch (Throwable ex) {
                ex = checkForHandshake(ex);
                errorCommon(ex);
                handleError(ex);
            }
        }

        private volatile Status lastUnwrapStatus;
        EngineResult unwrapBuffer(ByteBuffer src) throws IOException {
            ByteBuffer dst = getAppBuffer();
            int len = src.remaining();
            while (true) {
                SSLEngineResult sslResult = engine.unwrap(src, dst);
                switch (lastUnwrapStatus = sslResult.getStatus()) {
                    case BUFFER_OVERFLOW:
                        // may happen if app size buffer was changed, or if
                        // our 'adaptiveBufferSize' guess was too small for
                        // the current payload. In that case, update the
                        // value of applicationBufferSize, and allocate a
                        // buffer of that size, which we are sure will be
                        // big enough to decode whatever needs to be
                        // decoded. We will later update adaptiveBufferSize
                        // in OK: below.
                        int appSize = applicationBufferSize =
                                engine.getSession().getApplicationBufferSize();
                        ByteBuffer b = ByteBuffer.allocate(appSize + dst.position());
                        dst.flip();
                        b.put(dst);
                        dst = b;
                        break;
                    case CLOSED:
                        assert dst.position() == 0;
                        return doClosure(new EngineResult(sslResult));
                    case BUFFER_UNDERFLOW:
                        // handled implicitly by compaction/reallocation of readBuf
                        assert dst.position() == 0;
                        return new EngineResult(sslResult);
                    case OK:
                        int size = dst.position();
                        if (debug.on()) {
                            debugr.log("Decoded " + size + " bytes out of " + len
                                    + " into buffer of " + dst.capacity()
                                    + " remaining to decode: " + src.remaining());
                        }
                        // if the record payload was bigger than what was originally
                        // allocated, then sets the adaptiveAppBufferSize to size
                        // and we will use that new size as a guess for the next app
                        // buffer.
                        if (size > adaptiveAppBufferSize) {
                            adaptiveAppBufferSize = ((size + 7) >>> 3) << 3;
                        }
                        dst.flip();
                        return new EngineResult(sslResult, dst);
                }
            }
        }
    }

    public interface Monitorable {
        public String getInfo();
    }

    public static class Monitor extends Thread {
        final List<WeakReference<Monitorable>> list;
        final List<FinalMonitorable> finalList;
        final ReferenceQueue<Monitorable> queue = new ReferenceQueue<>();
        static Monitor themon;

        static {
            themon = new Monitor();
            themon.start(); // uncomment to enable Monitor
        }

        // An instance used to temporarily store the
        // last observable state of a monitorable object.
        // When Monitor.remove(o) is called, we replace
        // 'o' with a FinalMonitorable whose reference
        // will be enqueued after the last observable state
        // has been printed.
        final class FinalMonitorable implements Monitorable {
            final String finalState;
            FinalMonitorable(Monitorable o) {
                finalState = o.getInfo();
                finalList.add(this);
            }
            @Override
            public String getInfo() {
                finalList.remove(this);
                return finalState;
            }
        }

        Monitor() {
            super("Monitor");
            setDaemon(true);
            list = Collections.synchronizedList(new LinkedList<>());
            finalList = new ArrayList<>(); // access is synchronized on list above
        }

        void addTarget(Monitorable o) {
            list.add(new WeakReference<>(o, queue));
        }
        void removeTarget(Monitorable o) {
            // It can take a long time for GC to clean up references.
            // Calling Monitor.remove() early helps removing noise from the
            // logs/
            synchronized (list) {
                Iterator<WeakReference<Monitorable>> it = list.iterator();
                while (it.hasNext()) {
                    Monitorable m = it.next().get();
                    if (m == null) it.remove();
                    if (o == m) {
                        it.remove();
                        break;
                    }
                }
                FinalMonitorable m = new FinalMonitorable(o);
                addTarget(m);
                Reference.reachabilityFence(m);
            }
        }

        public static void add(Monitorable o) {
            themon.addTarget(o);
        }
        public static void remove(Monitorable o) {
            themon.removeTarget(o);
        }

        @Override
        public void run() {
            System.out.println("Monitor starting");
            try {
                while (true) {
                    Thread.sleep(20 * 1000);
                    synchronized (list) {
                        Reference<? extends Monitorable> expired;
                        while ((expired = queue.poll()) != null) list.remove(expired);
                        for (WeakReference<Monitorable> ref : list) {
                            Monitorable o = ref.get();
                            if (o == null) continue;
                            if (o instanceof FinalMonitorable) {
                                ref.enqueue();
                            }
                            System.out.println(o.getInfo());
                            System.out.println("-------------------------");
                        }
                    }
                    System.out.println("--o-o-o-o-o-o-o-o-o-o-o-o-o-o-");
                }
            } catch (InterruptedException e) {
                System.out.println("Monitor exiting with " + e);
            }
        }
    }

    /**
     * Processing function for outgoing data. Pass it thru SSLEngine.wrap()
     * Any encrypted buffers generated are passed downstream to be written.
     * Status codes:
     *     NEED_UNWRAP: call reader.addData() with empty buffer
     *     NEED_WRAP: call addData() with empty buffer
     *     NEED_TASK: delegate task to executor
     *     BUFFER_OVERFLOW: allocate larger output buffer. Repeat wrap
     *     BUFFER_UNDERFLOW: shouldn't happen on writing side
     *     OK: return generated buffers
     */
    class Writer extends SubscriberWrapper {
        final SequentialScheduler scheduler;
        // queues of buffers received from upstream waiting
        // to be processed by the SSLEngine
        final List<ByteBuffer> writeList;
        final Logger debugw =  Utils.getDebugLogger(this::dbgString, Utils.DEBUG);
        volatile boolean completing;
        boolean completed; // only accessed in processData

        class WriterDownstreamPusher extends SequentialScheduler.CompleteRestartableTask {
            @Override public void run() { processData(); }
        }

        Writer() {
            super();
            writeList = Collections.synchronizedList(new LinkedList<>());
            scheduler = new SequentialScheduler(new WriterDownstreamPusher());
        }

        @Override
        protected void incoming(List<ByteBuffer> buffers, boolean complete) {
            assert complete ? buffers == Utils.EMPTY_BB_LIST : true;
            assert buffers != Utils.EMPTY_BB_LIST ? complete == false : true;
            if (complete) {
                if (debugw.on()) debugw.log("adding SENTINEL");
                completing = true;
                writeList.add(SENTINEL);
            } else {
                writeList.addAll(buffers);
            }
            if (debugw.on())
                debugw.log("added " + buffers.size()
                           + " (" + Utils.remaining(buffers)
                           + " bytes) to the writeList");
            scheduler.runOrSchedule();
        }

        public final String dbgString() {
            return "SSL Writer(" + tubeName + ")";
        }

        protected void onSubscribe() {
            if (debugw.on()) debugw.log("onSubscribe initiating handshaking");
            addData(HS_TRIGGER);  // initiates handshaking
        }

        void schedule() {
            scheduler.runOrSchedule();
        }

        void stop() {
            if (debugw.on()) debugw.log("stop");
            scheduler.stop();
        }

        @Override
        public boolean closing() {
            return closeNotifyReceived();
        }

        private boolean isCompleting() {
            return completing;
        }

        @Override
        protected long upstreamWindowUpdate(long currentWindow, long downstreamQsize) {
            if (writeList.size() > 10)
                return 0;
            else
                return super.upstreamWindowUpdate(currentWindow, downstreamQsize);
        }

        private boolean hsTriggered() {
            synchronized(writeList) {
                for (ByteBuffer b : writeList)
                    if (b == HS_TRIGGER)
                        return true;
                return false;
            }
        }

        void triggerWrite() {
            synchronized (writeList) {
                if (writeList.isEmpty()) {
                    writeList.add(HS_TRIGGER);
                }
            }
            scheduler.runOrSchedule();
        }

        private void processData() {
            boolean completing = isCompleting();

            try {
                if (debugw.on())
                    debugw.log("processData, writeList remaining:"
                                + Utils.synchronizedRemaining(writeList) + ", hsTriggered:"
                                + hsTriggered() + ", needWrap:" + needWrap());

                while (Utils.synchronizedRemaining(writeList) > 0 || hsTriggered() || needWrap()) {
                    ByteBuffer[] outbufs = writeList.toArray(Utils.EMPTY_BB_ARRAY);
                    EngineResult result = wrapBuffers(outbufs);
                    if (debugw.on())
                        debugw.log("wrapBuffer returned %s", result.result);

                    if (result.status() == Status.CLOSED) {
                        if (!upstreamCompleted) {
                            upstreamCompleted = true;
                            upstreamSubscription.cancel();
                            // complete ALPN if not yet completed
                            setALPN();
                        }
                        if (result.bytesProduced() <= 0)
                            return;

                        if (!completing && !completed) {
                            completing = this.completing = true;
                            // There could still be some outgoing data in outbufs.
                            writeList.add(SENTINEL);
                        }
                    }

                    boolean handshaking = false;
                    if (result.handshaking()) {
                        if (debugw.on()) debugw.log("handshaking");
                        doHandshake(result, WRITER);  // ok to ignore return
                        handshaking = true;
                    } else {
                        if (trySetALPN()) {
                            resumeActivity();
                        }
                    }
                    cleanList(writeList); // tidy up the source list
                    sendResultBytes(result);
                    if (handshaking) {
                        if (!completing && needWrap()) {
                            continue;
                        } else {
                            return;
                        }
                    }
                }
                if (completing && Utils.synchronizedRemaining(writeList) == 0) {
                    if (!completed) {
                        completed = true;
                        writeList.clear();
                        outgoing(Utils.EMPTY_BB_LIST, true);
                    }
                    return;
                }
                if (writeList.isEmpty() && needWrap()) {
                    writer.addData(HS_TRIGGER);
                }
            } catch (Throwable ex) {
                ex = checkForHandshake(ex);
                errorCommon(ex);
                handleError(ex);
            }
        }

        // The SSLEngine insists on being given a buffer that is at least
        // SSLSession.getPacketBufferSize() long (usually 16K). If given
        // a smaller buffer it will go in BUFFER_OVERFLOW, even if it only
        // has 6 bytes to wrap. Typical usage shows that for GET we
        // usually produce an average of ~ 100 bytes.
        // To avoid wasting space, and because allocating and zeroing
        // 16K buffers for encoding 6 bytes is costly, we are reusing the
        // same writeBuffer to interact with SSLEngine.wrap().
        // If the SSLEngine produces less than writeBuffer.capacity() / 2,
        // then we copy off the bytes to a smaller buffer that we send
        // downstream. Otherwise, we send the writeBuffer downstream
        // and will allocate a new one next time.
        volatile ByteBuffer writeBuffer;
        private volatile Status lastWrappedStatus;
        @SuppressWarnings("fallthrough")
        EngineResult wrapBuffers(ByteBuffer[] src) throws SSLException {
            long len = Utils.remaining(src);
            if (debugw.on())
                debugw.log("wrapping " + len + " bytes");

            ByteBuffer dst = writeBuffer;
            if (dst == null) dst = writeBuffer = getNetBuffer();
            assert dst.position() == 0 : "buffer position is " + dst.position();
            assert dst.hasRemaining() : "buffer has no remaining space: capacity=" + dst.capacity();

            while (true) {
                SSLEngineResult sslResult = engine.wrap(src, dst);
                if (debugw.on()) debugw.log("SSLResult: " + sslResult);
                switch (lastWrappedStatus = sslResult.getStatus()) {
                    case BUFFER_OVERFLOW:
                        // Shouldn't happen. We allocated buffer with packet size
                        // get it again if net buffer size was changed
                        if (debugw.on()) debugw.log("BUFFER_OVERFLOW");
                        int netSize = packetBufferSize
                                = engine.getSession().getPacketBufferSize();
                        ByteBuffer b = writeBuffer = ByteBuffer.allocate(netSize + dst.position());
                        dst.flip();
                        b.put(dst);
                        dst = b;
                        break; // try again
                    case CLOSED:
                        if (debugw.on()) debugw.log("CLOSED");
                        // fallthrough. There could be some remaining data in dst.
                        // CLOSED will be handled by the caller.
                    case OK:
                        final ByteBuffer dest;
                        if (dst.position() == 0) {
                            dest = NOTHING; // can happen if handshake is in progress
                        } else if (dst.position() < dst.capacity() / 2) {
                            // less than half the buffer was used.
                            // copy off the bytes to a smaller buffer, and keep
                            // the writeBuffer for next time.
                            dst.flip();
                            dest = Utils.copyAligned(dst);
                            dst.clear();
                        } else {
                            // more than half the buffer was used.
                            // just send that buffer downstream, and we will
                            // get a new writeBuffer next time it is needed.
                            dst.flip();
                            dest = dst;
                            writeBuffer = null;
                        }
                        if (debugw.on())
                            debugw.log("OK => produced: %d bytes into %d, not wrapped: %d",
                                       dest.remaining(),  dest.capacity(), Utils.remaining(src));
                        return new EngineResult(sslResult, dest);
                    case BUFFER_UNDERFLOW:
                        // Shouldn't happen.  Doesn't returns when wrap()
                        // underflow handled externally
                        // assert false : "Buffer Underflow";
                        if (debug.on()) debug.log("BUFFER_UNDERFLOW");
                        return new EngineResult(sslResult);
                    default:
                        if (debugw.on())
                            debugw.log("result: %s", sslResult.getStatus());
                        assert false : "result:" + sslResult.getStatus();
                }
            }
        }

        private boolean needWrap() {
            return engine.getHandshakeStatus() == HandshakeStatus.NEED_WRAP;
        }

        private void sendResultBytes(EngineResult result) {
            if (result.bytesProduced() > 0) {
                if (debugw.on())
                    debugw.log("Sending %d bytes downstream",
                               result.bytesProduced());
                outgoing(result.destBuffer, false);
            }
        }

        @Override
        public String toString() {
            return "WRITER: " + super.toString()
                    + ", writeList size: " + Integer.toString(writeList.size())
                    + ", scheduler: " + (scheduler.isStopped() ? "stopped" : "running")
                    + ", status: " + lastWrappedStatus;
                    //" writeList: " + writeList.toString();
        }
    }

    private void handleError(Throwable t) {
        if (debug.on()) debug.log("handleError", t);
        readerCF.completeExceptionally(t);
        writerCF.completeExceptionally(t);
        // no-op if already completed
        alpnCF.completeExceptionally(t);
        reader.stop();
        writer.stop();
    }

    boolean stopped;

    private synchronized void normalStop() {
        if (stopped)
            return;
        stopped = true;
        reader.stop();
        writer.stop();
        // make sure the alpnCF is completed.
        if (!alpnCF.isDone()) {
            Throwable alpn = new SSLHandshakeException(
                    "Connection closed before successful ALPN negotiation");
            alpnCF.completeExceptionally(alpn);
        }
        if (isMonitored) Monitor.remove(monitor);
    }

    private Void stopOnError(Throwable error) {
        // maybe log, etc
        // ensure the ALPN is completed
        // We could also do this in SSLTube.SSLSubscriberWrapper
        // onError/onComplete - with the caveat that the ALP CF
        // would get completed externally. Doing it here keeps
        // it all inside SSLFlowDelegate.
        if (!alpnCF.isDone()) {
            alpnCF.completeExceptionally(error);
        }
        normalStop();
        return null;
    }

    private void cleanList(List<ByteBuffer> l) {
        synchronized (l) {
            Iterator<ByteBuffer> iter = l.iterator();
            while (iter.hasNext()) {
                ByteBuffer b = iter.next();
                if (!b.hasRemaining() && b != SENTINEL) {
                    iter.remove();
                }
            }
        }
    }

    /**
     * States for handshake. We avoid races when accessing/updating the AtomicInt
     * because updates always schedule an additional call to both the read()
     * and write() functions.
     */
    private static final int NOT_HANDSHAKING = 0;
    private static final int HANDSHAKING = 1;

    // Bit flags
    // a thread is currently executing tasks
    private static final int DOING_TASKS = 4;
    // a thread wants to execute tasks, while another thread is executing
    private static final int REQUESTING_TASKS = 8;
    private static final int TASK_BITS = 12; // Both bits

    private static final int READER = 1;
    private static final int WRITER = 2;

    private static String states(AtomicInteger state) {
        int s = state.get();
        StringBuilder sb = new StringBuilder();
        int x = s & ~TASK_BITS;
        switch (x) {
            case NOT_HANDSHAKING    -> sb.append(" NOT_HANDSHAKING ");
            case HANDSHAKING        -> sb.append(" HANDSHAKING ");

            default -> throw new InternalError();
        }
        if ((s & DOING_TASKS) > 0)
            sb.append("|DOING_TASKS");
        if ((s & REQUESTING_TASKS) > 0)
            sb.append("|REQUESTING_TASKS");
        return sb.toString();
    }

    private void resumeActivity() {
        reader.schedule();
        writer.schedule();
    }

    final AtomicInteger handshakeState;
    final ConcurrentLinkedQueue<String> stateList =
            debug.on() ? new ConcurrentLinkedQueue<>() : null;

    // Atomically executed to update task bits. Sets either DOING_TASKS or REQUESTING_TASKS
    // depending on previous value
    private static final IntBinaryOperator REQUEST_OR_DO_TASKS = (current, ignored) -> {
        if ((current & DOING_TASKS) == 0)
            return DOING_TASKS | (current & HANDSHAKING);
        else
            return DOING_TASKS | REQUESTING_TASKS | (current & HANDSHAKING);
    };

    // Atomically executed to update task bits. Sets DOING_TASKS if REQUESTING was set
    // clears bits if not.
    private static final IntBinaryOperator FINISH_OR_DO_TASKS = (current, ignored) -> {
        if ((current & REQUESTING_TASKS) != 0)
            return DOING_TASKS | (current & HANDSHAKING);
        // clear both bits
        return (current & HANDSHAKING);
    };

    private boolean doHandshake(EngineResult r, int caller) {
        // unconditionally sets the HANDSHAKING bit, while preserving task bits
        handshakeState.getAndAccumulate(0, (current, unused) -> HANDSHAKING | (current & TASK_BITS));
        if (stateList != null && debug.on()) {
            stateList.add(r.handshakeStatus().toString());
            stateList.add(Integer.toString(caller));
        }
        switch (r.handshakeStatus()) {
            case NEED_TASK:
                int s = handshakeState.accumulateAndGet(0, REQUEST_OR_DO_TASKS);
                if ((s & REQUESTING_TASKS) > 0) { // someone else is or will do tasks
                    return false;
                }

                if (debug.on()) debug.log("obtaining and initiating task execution");
                List<Runnable> tasks = obtainTasks();
                executeTasks(tasks);
                return false;  // executeTasks will resume activity
            case NEED_WRAP:
                if (caller == READER) {
                    writer.triggerWrite();
                    return false;
                }
                break;
            case NEED_UNWRAP:
            case NEED_UNWRAP_AGAIN:
                // do nothing else
                // receiving-side data will trigger unwrap
                if (caller == WRITER) {
                    reader.schedule();
                    return false;
                }
                break;
            default:
                throw new InternalError("Unexpected handshake status:"
                                        + r.handshakeStatus());
        }
        return true;
    }

    private List<Runnable> obtainTasks() {
        List<Runnable> l = new ArrayList<>();
        Runnable r;
        while ((r = engine.getDelegatedTask()) != null) {
            l.add(r);
        }
        return l;
    }

    private void executeTasks(List<Runnable> tasks) {
        exec.execute(() -> {
            try {
                List<Runnable> nextTasks = tasks;
                if (debug.on()) debug.log("#tasks to execute: " + Integer.toString(nextTasks.size()));
                do {
                    nextTasks.forEach(Runnable::run);
                    if (engine.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
                        nextTasks = obtainTasks();
                    } else {
                        int s = handshakeState.accumulateAndGet(0, FINISH_OR_DO_TASKS);
                        if ((s & DOING_TASKS) != 0) {
                            if (debug.on()) debug.log("re-running tasks (B)");
                            nextTasks = obtainTasks();
                            continue;
                        }
                        break;
                    }
                } while (true);
                if (debug.on()) debug.log("finished task execution");
                HandshakeStatus hs = engine.getHandshakeStatus();
                if (hs == HandshakeStatus.FINISHED || hs == HandshakeStatus.NOT_HANDSHAKING) {
                    // We're no longer handshaking, try setting ALPN
                    trySetALPN();
                }
                resumeActivity();
            } catch (Throwable t) {
                handleError(checkForHandshake(t));
            }
        });
    }

    boolean trySetALPN() {
        // complete ALPN CF if needed.
        if ((handshakeState.getAndSet(NOT_HANDSHAKING) & ~DOING_TASKS) == HANDSHAKING) {
            applicationBufferSize = engine.getSession().getApplicationBufferSize();
            packetBufferSize = engine.getSession().getPacketBufferSize();
            setALPN();
            return true;
        }
        return false;
    }

    // FIXME: acknowledge a received CLOSE request from peer
    EngineResult doClosure(EngineResult r) throws IOException {
        if (debug.on())
            debug.log("doClosure(%s): %s [isOutboundDone: %s, isInboundDone: %s]",
                      r.result, engine.getHandshakeStatus(),
                      engine.isOutboundDone(), engine.isInboundDone());
        if (engine.getHandshakeStatus() == HandshakeStatus.NEED_WRAP) {
            // we have received TLS close_notify and need to send
            // an acknowledgement back. We're calling doHandshake
            // to finish the close handshake.
            if (engine.isInboundDone() && !engine.isOutboundDone()) {
                if (debug.on()) debug.log("doClosure: close_notify received");
                close_notify_received = true;
                if (!writer.scheduler.isStopped()) {
                    doHandshake(r, READER);
                } else {
                    // We have received closed notify, but we
                    // won't be able to send the acknowledgement.
                    // Nothing more will come from the socket either,
                    // so mark the reader as completed.
                    synchronized (reader.readBufferLock) {
                        reader.completing = true;
                    }
                }
            }
        }
        return r;
    }

    /**
     * Returns the upstream Flow.Subscriber of the reading (incoming) side.
     * This flow must be given the encrypted data read from upstream (eg socket)
     * before it is decrypted.
     */
    public Flow.Subscriber<List<ByteBuffer>> upstreamReader() {
        return reader;
    }

    /**
     * Returns the upstream Flow.Subscriber of the writing (outgoing) side.
     * This flow contains the plaintext data before it is encrypted.
     */
    public Flow.Subscriber<List<ByteBuffer>> upstreamWriter() {
        return writer;
    }

    public boolean resumeReader() {
        return reader.signalScheduling();
    }

    public void resetReaderDemand() {
        reader.resetDownstreamDemand();
    }

    static class EngineResult {
        final SSLEngineResult result;
        final ByteBuffer destBuffer;

        // normal result
        EngineResult(SSLEngineResult result) {
            this(result, null);
        }

        EngineResult(SSLEngineResult result, ByteBuffer destBuffer) {
            this.result = result;
            this.destBuffer = destBuffer;
        }

        boolean handshaking() {
            HandshakeStatus s = result.getHandshakeStatus();
            return s != HandshakeStatus.FINISHED
                   && s != HandshakeStatus.NOT_HANDSHAKING
                   && result.getStatus() != Status.CLOSED;
        }

        boolean needUnwrap() {
            HandshakeStatus s = result.getHandshakeStatus();
            return s == HandshakeStatus.NEED_UNWRAP;
        }


        int bytesConsumed() {
            return result.bytesConsumed();
        }

        int bytesProduced() {
            return result.bytesProduced();
        }

        SSLEngineResult.HandshakeStatus handshakeStatus() {
            return result.getHandshakeStatus();
        }

        SSLEngineResult.Status status() {
            return result.getStatus();
        }
    }

    // The maximum network buffer size negotiated during
    // the handshake. Usually 16K.
    volatile int packetBufferSize;
    final ByteBuffer getNetBuffer() {
        int netSize = packetBufferSize;
        if (netSize <= 0) {
            packetBufferSize = netSize = engine.getSession().getPacketBufferSize();
        }
        return ByteBuffer.allocate(netSize);
    }

    // The maximum application buffer size negotiated during
    // the handshake. Usually close to 16K.
    volatile int applicationBufferSize;
    // Despite of the maximum applicationBufferSize negotiated
    // above, TLS records usually have a much smaller payload.
    // The adaptativeAppBufferSize records the max payload
    // ever decoded, and we use that as a guess for how big
    // a buffer we will need for the next payload.
    // This avoids allocating and zeroing a 16K buffer for
    // nothing...
    volatile int adaptiveAppBufferSize;
    final ByteBuffer getAppBuffer() {
        int appSize = applicationBufferSize;
        if (appSize <= 0) {
            applicationBufferSize = appSize
                    = engine.getSession().getApplicationBufferSize();
        }
        int size = adaptiveAppBufferSize;
        if (size <= 0) {
            size = 512; // start with 512 this is usually enough for handshaking / headers
        } else if (size > appSize) {
            size = appSize;
        }
        // will cause a BUFFER_OVERFLOW if not big enough, but
        // that's OK.
        return ByteBuffer.allocate(size);
    }

    final String dbgString() {
        return "SSLFlowDelegate(" + tubeName + ")";
    }
}
