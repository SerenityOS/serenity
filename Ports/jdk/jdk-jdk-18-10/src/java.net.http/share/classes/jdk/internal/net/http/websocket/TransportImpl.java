/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.websocket;

import jdk.internal.net.http.common.Demand;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.MinimalFuture;
import jdk.internal.net.http.common.SequentialScheduler;
import jdk.internal.net.http.common.SequentialScheduler.CompleteRestartableTask;
import jdk.internal.net.http.common.Utils;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static jdk.internal.net.http.websocket.TransportImpl.ChannelState.AVAILABLE;
import static jdk.internal.net.http.websocket.TransportImpl.ChannelState.CLOSED;
import static jdk.internal.net.http.websocket.TransportImpl.ChannelState.UNREGISTERED;
import static jdk.internal.net.http.websocket.TransportImpl.ChannelState.WAITING;

public class TransportImpl implements Transport {

    // -- Debugging infrastructure --

    private static final Logger debug =
            Utils.getWebSocketLogger("[Transport]"::toString, Utils.DEBUG_WS);

    /* Used for correlating enters to and exists from a method */
    private final AtomicLong counter = new AtomicLong();

    private final SequentialScheduler sendScheduler = new SequentialScheduler(new SendTask());

    private final MessageQueue queue;
    private final MessageEncoder encoder = new MessageEncoder();
    /* A reusable buffer for writing, initially with no remaining bytes */
    private final ByteBuffer dst = createWriteBuffer().position(0).limit(0);
    /* This array is created once for gathering writes accepted by RawChannel */
    private final ByteBuffer[] dstArray = new ByteBuffer[]{dst};
    private final MessageStreamConsumer messageConsumer;
    private final MessageDecoder decoder;
    private final Frame.Reader reader = new Frame.Reader();

    private final Demand demand = new Demand();
    private final SequentialScheduler receiveScheduler;
    private final RawChannel channel;
    private final Object closeLock = new Object();
    private final RawChannel.RawEvent writeEvent = new WriteEvent();
    private final RawChannel.RawEvent readEvent = new ReadEvent();
    private final AtomicReference<ChannelState> writeState
            = new AtomicReference<>(UNREGISTERED);
    private ByteBuffer data;
    private volatile ChannelState readState = UNREGISTERED;
    private boolean inputClosed;
    private boolean outputClosed;

    public TransportImpl(MessageQueue queue, MessageStreamConsumer consumer,
                         RawChannel channel) {
        this.queue = queue;
        this.messageConsumer = consumer;
        this.channel = channel;
        this.decoder = new MessageDecoder(this.messageConsumer);
        this.data = channel.initialByteBuffer();
        // To ensure the initial non-final `data` will be visible
        // (happens-before) when `readEvent.handle()` invokes `receiveScheduler`
        // the following assignment is done last:
        receiveScheduler = new SequentialScheduler(new ReceiveTask());
    }

    private ByteBuffer createWriteBuffer() {
        String name = "jdk.httpclient.websocket.writeBufferSize";
        int capacity = Utils.getIntegerNetProperty(name, 16384);
        if (debug.on()) {
            debug.log("write buffer capacity %s", capacity);
        }

        // TODO (optimization?): allocateDirect if SSL?
        return ByteBuffer.allocate(capacity);
    }

    private boolean write() throws IOException {
        if (debug.on()) {
            debug.log("writing to the channel");
        }
        long count = channel.write(dstArray, 0, dstArray.length);
        if (debug.on()) {
            debug.log("%s bytes written", count);
        }
        for (ByteBuffer b : dstArray) {
            if (b.hasRemaining()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public <T> CompletableFuture<T> sendText(CharSequence message,
                                             boolean isLast,
                                             T attachment,
                                             BiConsumer<? super T, ? super Throwable> action) {
        long id = 0;
        if (debug.on()) {
            id = counter.incrementAndGet();
            debug.log("enter send text %s message.length=%s last=%s",
                              id, message.length(), isLast);
        }
        // TODO (optimization?):
        // These sendXXX methods might be a good place to decide whether or not
        // we can write straight ahead, possibly returning null instead of
        // creating a CompletableFuture

        // Even if the text is already CharBuffer, the client will not be happy
        // if they discover the position is changing. So, no instanceof
        // cheating, wrap always.
        CharBuffer text = CharBuffer.wrap(message);
        MinimalFuture<T> f = new MinimalFuture<>();
        try {
            queue.addText(text, isLast, attachment, action, f);
            sendScheduler.runOrSchedule();
        } catch (IOException e) {
            action.accept(null, e);
            f.completeExceptionally(e);
        }
        if (debug.on()) {
            debug.log("exit send text %s returned %s", id, f);
        }
        return f;
    }

    @Override
    public <T> CompletableFuture<T> sendBinary(ByteBuffer message,
                                               boolean isLast,
                                               T attachment,
                                               BiConsumer<? super T, ? super Throwable> action) {
        long id = 0;
        if (debug.on()) {
            id = counter.incrementAndGet();
            debug.log("enter send binary %s message.remaining=%s last=%s",
                              id, message.remaining(), isLast);
        }
        MinimalFuture<T> f = new MinimalFuture<>();
        try {
            queue.addBinary(message, isLast, attachment, action, f);
            sendScheduler.runOrSchedule();
        } catch (IOException e) {
            action.accept(null, e);
            f.completeExceptionally(e);
        }
        if (debug.on()) {
            debug.log("exit send binary %s returned %s", id, f);
        }
        return f;
    }

    @Override
    public <T> CompletableFuture<T> sendPing(ByteBuffer message,
                                             T attachment,
                                             BiConsumer<? super T, ? super Throwable> action) {
        long id = 0;
        if (debug.on()) {
            id = counter.incrementAndGet();
            debug.log("enter send ping %s message.remaining=%s",
                              id, message.remaining());
        }
        MinimalFuture<T> f = new MinimalFuture<>();
        try {
            queue.addPing(message, attachment, action, f);
            sendScheduler.runOrSchedule();
        } catch (IOException e) {
            action.accept(null, e);
            f.completeExceptionally(e);
        }
        if (debug.on()) {
            debug.log("exit send ping %s returned %s", id, f);
        }
        return f;
    }

    @Override
    public <T> CompletableFuture<T> sendPong(ByteBuffer message,
                                             T attachment,
                                             BiConsumer<? super T, ? super Throwable> action) {
        long id = 0;
        if (debug.on()) {
            id = counter.incrementAndGet();
            debug.log("enter send pong %s message.remaining=%s",
                              id, message.remaining());
        }
        MinimalFuture<T> f = new MinimalFuture<>();
        try {
            queue.addPong(message, attachment, action, f);
            sendScheduler.runOrSchedule();
        } catch (IOException e) {
            action.accept(null, e);
            f.completeExceptionally(e);
        }
        if (debug.on()) {
            debug.log("exit send pong %s returned %s", id, f);
        }
        return f;
    }

    @Override
    public <T> CompletableFuture<T> sendPong(Supplier<? extends ByteBuffer> message,
                                             T attachment,
                                             BiConsumer<? super T, ? super Throwable> action) {
        long id = 0;
        if (debug.on()) {
            id = counter.incrementAndGet();
            debug.log("enter send pong %s supplier=%s",
                      id, message);
        }
        MinimalFuture<T> f = new MinimalFuture<>();
        try {
            queue.addPong(message, attachment, action, f);
            sendScheduler.runOrSchedule();
        } catch (IOException e) {
            action.accept(null, e);
            f.completeExceptionally(e);
        }
        if (debug.on()) {
            debug.log("exit send pong %s returned %s", id, f);
        }
        return f;
    }

    @Override
    public <T> CompletableFuture<T> sendClose(int statusCode,
                                              String reason,
                                              T attachment,
                                              BiConsumer<? super T, ? super Throwable> action) {
        long id = 0;
        if (debug.on()) {
            id = counter.incrementAndGet();
            debug.log("enter send close %s statusCode=%s reason.length=%s",
                              id, statusCode, reason.length());
        }
        MinimalFuture<T> f = new MinimalFuture<>();
        try {
            queue.addClose(statusCode, CharBuffer.wrap(reason), attachment, action, f);
            sendScheduler.runOrSchedule();
        } catch (IOException e) {
            action.accept(null, e);
            f.completeExceptionally(e);
        }
        if (debug.on()) {
            debug.log("exit send close %s returned %s", id, f);
        }
        return f;
    }

    @Override
    public void request(long n) {
        if (debug.on()) {
            debug.log("request %s", n);
        }
        if (demand.increase(n)) {
            receiveScheduler.runOrSchedule();
        }
    }

    @Override
    public void acknowledgeReception() {
        boolean decremented = demand.tryDecrement();
        if (!decremented) {
            throw new InternalError();
        }
    }

    @Override
    public void closeOutput() throws IOException {
        if (debug.on()) {
            debug.log("closeOutput");
        }
        synchronized (closeLock) {
            if (!outputClosed) {
                outputClosed = true;
                try {
                    channel.shutdownOutput();
                } finally {
                    writeState.set(CLOSED);
                    if (inputClosed) {
                        channel.close();
                    }
                }
            }
        }
        ChannelState s = writeState.get();
        assert s == CLOSED : s;
        sendScheduler.runOrSchedule();
    }

    /*
     * Permanently stops reading from the channel and delivering messages
     * regardless of the current demand and data availability.
     */
    @Override
    public void closeInput() throws IOException {
        if (debug.on()) {
            debug.log("closeInput");
        }
        synchronized (closeLock) {
            if (!inputClosed) {
                inputClosed = true;
                try {
                    receiveScheduler.stop();
                    channel.shutdownInput();
                } finally {
                    if (outputClosed) {
                        ChannelState s = writeState.get();
                        assert s == CLOSED : s;
                        channel.close();
                    }
                }
            }
        }
    }

    /* Common states for send and receive tasks */
    enum ChannelState {
        UNREGISTERED,
        AVAILABLE,
        WAITING,
        CLOSED,
    }

    @SuppressWarnings({"rawtypes"})
    private class SendTask extends CompleteRestartableTask {

        private final MessageQueue.QueueCallback<Boolean, IOException>
                encodingCallback = new MessageQueue.QueueCallback<>() {

            @Override
            public <T> Boolean onText(CharBuffer message,
                                      boolean isLast,
                                      T attachment,
                                      BiConsumer<? super T, ? super Throwable> action,
                                      CompletableFuture<? super T> future) throws IOException
            {
                return encoder.encodeText(message, isLast, dst);
            }

            @Override
            public <T> Boolean onBinary(ByteBuffer message,
                                        boolean isLast,
                                        T attachment,
                                        BiConsumer<? super T, ? super Throwable> action,
                                        CompletableFuture<? super T> future) throws IOException
            {
                return encoder.encodeBinary(message, isLast, dst);
            }

            @Override
            public <T> Boolean onPing(ByteBuffer message,
                                      T attachment,
                                      BiConsumer<? super T, ? super Throwable> action,
                                      CompletableFuture<? super T> future) throws IOException
            {
                return encoder.encodePing(message, dst);
            }

            @Override
            public <T> Boolean onPong(ByteBuffer message,
                                      T attachment,
                                      BiConsumer<? super T, ? super Throwable> action,
                                      CompletableFuture<? super T> future) throws IOException
            {
                return encoder.encodePong(message, dst);
            }

            @Override
            public <T> Boolean onPong(Supplier<? extends ByteBuffer> message,
                                      T attachment,
                                      BiConsumer<? super T, ? super Throwable> action,
                                      CompletableFuture<? super T> future) throws IOException {
                return encoder.encodePong(message.get(), dst);
            }

            @Override
            public <T> Boolean onClose(int statusCode,
                                       CharBuffer reason,
                                       T attachment,
                                       BiConsumer<? super T, ? super Throwable> action,
                                       CompletableFuture<? super T> future) throws IOException
            {
                return encoder.encodeClose(statusCode, reason, dst);
            }

            @Override
            public Boolean onEmpty() {
                return false;
            }
        };

        /* Whether the task sees the current head message for first time */
        private boolean firstPass = true;
        /* Whether the message has been fully encoded */
        private boolean encoded;

        // -- Current message completion communication fields --

        private Object attachment;
        private BiConsumer action;
        private CompletableFuture future;
        private final MessageQueue.QueueCallback<Boolean, RuntimeException>
                /* If there is a message, loads its completion communication fields */
                loadCallback = new MessageQueue.QueueCallback<Boolean, RuntimeException>() {

            @Override
            public <T> Boolean onText(CharBuffer message,
                                      boolean isLast,
                                      T attachment,
                                      BiConsumer<? super T, ? super Throwable> action,
                                      CompletableFuture<? super T> future)
            {
                SendTask.this.attachment = attachment;
                SendTask.this.action = action;
                SendTask.this.future = future;
                return true;
            }

            @Override
            public <T> Boolean onBinary(ByteBuffer message,
                                        boolean isLast,
                                        T attachment,
                                        BiConsumer<? super T, ? super Throwable> action,
                                        CompletableFuture<? super T> future)
            {
                SendTask.this.attachment = attachment;
                SendTask.this.action = action;
                SendTask.this.future = future;
                return true;
            }

            @Override
            public <T> Boolean onPing(ByteBuffer message,
                                      T attachment,
                                      BiConsumer<? super T, ? super Throwable> action,
                                      CompletableFuture<? super T> future)
            {
                SendTask.this.attachment = attachment;
                SendTask.this.action = action;
                SendTask.this.future = future;
                return true;
            }

            @Override
            public <T> Boolean onPong(ByteBuffer message,
                                      T attachment,
                                      BiConsumer<? super T, ? super Throwable> action,
                                      CompletableFuture<? super T> future)
            {
                SendTask.this.attachment = attachment;
                SendTask.this.action = action;
                SendTask.this.future = future;
                return true;
            }

            @Override
            public <T> Boolean onPong(Supplier<? extends ByteBuffer> message,
                                      T attachment,
                                      BiConsumer<? super T, ? super Throwable> action,
                                      CompletableFuture<? super T> future)
            {
                SendTask.this.attachment = attachment;
                SendTask.this.action = action;
                SendTask.this.future = future;
                return true;
            }

            @Override
            public <T> Boolean onClose(int statusCode,
                                       CharBuffer reason,
                                       T attachment,
                                       BiConsumer<? super T, ? super Throwable> action,
                                       CompletableFuture<? super T> future)
            {
                SendTask.this.attachment = attachment;
                SendTask.this.action = action;
                SendTask.this.future = future;
                return true;
            }

            @Override
            public Boolean onEmpty() {
                return false;
            }
        };

        @Override
        public void run() {
            // Could have been only called in one of the following cases:
            //   (a) A message has been added to the queue
            //   (b) The channel is ready for writing
            if (debug.on()) {
                debug.log("enter send task");
            }
            while (!queue.isEmpty()) {
                try {
                    if (dst.hasRemaining()) {
                        if (debug.on()) {
                            debug.log("%s bytes remaining in buffer %s",
                                      dst.remaining(), dst);
                        }
                        // The previous part of the binary representation of the
                        // message hasn't been fully written
                        if (!tryCompleteWrite()) {
                            break;
                        }
                    } else if (!encoded) {
                        if (firstPass) {
                            firstPass = false;
                            queue.peek(loadCallback);
                            if (debug.on()) {
                                debug.log("load message");
                            }
                        }
                        dst.clear();
                        encoded = queue.peek(encodingCallback);
                        dst.flip();
                        if (!tryCompleteWrite()) {
                            break;
                        }
                    } else {
                        // All done, remove and complete
                        encoder.reset();
                        removeAndComplete(null);
                    }
                } catch (Throwable t) {
                    if (debug.on()) {
                        debug.log("send task exception %s", (Object) t);
                    }
                    // buffer cleanup: if there is an exception, the buffer
                    // should appear empty for the next write as there is
                    // nothing to write
                    dst.position(dst.limit());
                    encoder.reset();
                    removeAndComplete(t);
                }
            }
            if (debug.on()) {
                debug.log("exit send task");
            }
        }

        private boolean tryCompleteWrite() throws IOException {
            if (debug.on()) {
                debug.log("enter writing");
            }
            boolean finished = false;
            loop:
            while (true) {
                final ChannelState ws = writeState.get();
                if (debug.on()) {
                    debug.log("write state: %s", ws);
                }
                switch (ws) {
                    case WAITING:
                        break loop;
                    case UNREGISTERED:
                        if (debug.on()) {
                            debug.log("registering write event");
                        }
                        channel.registerEvent(writeEvent);
                        writeState.compareAndSet(UNREGISTERED, WAITING);
                        if (debug.on()) {
                            debug.log("registered write event");
                        }
                        break loop;
                    case AVAILABLE:
                        boolean written = write();
                        if (written) {
                            if (debug.on()) {
                                debug.log("finished writing to the channel");
                            }
                            finished = true;
                            break loop;   // All done
                        } else {
                            writeState.compareAndSet(AVAILABLE, UNREGISTERED);
                            continue loop; //  Effectively "goto UNREGISTERED"
                        }
                    case CLOSED:
                        throw new IOException("Output closed");
                    default:
                        throw new InternalError(String.valueOf(ws));
                }
            }
            if (debug.on()) {
                debug.log("exit writing");
            }
            return finished;
        }

        @SuppressWarnings("unchecked")
        private void removeAndComplete(Throwable error) {
            if (debug.on()) {
                debug.log("removeAndComplete error=%s", (Object) error);
            }
            queue.remove();
            if (error != null) {
                try {
                    action.accept(null, error);
                } finally {
                    future.completeExceptionally(error);
                }
            } else {
                try {
                    action.accept(attachment, null);
                } finally {
                    future.complete(attachment);
                }
            }
            encoded = false;
            firstPass = true;
            attachment = null;
            action = null;
            future = null;
        }
    }

    private class ReceiveTask extends CompleteRestartableTask {

        @Override
        public void run() {
            if (debug.on()) {
                debug.log("enter receive task");
            }
            loop:
            while (!receiveScheduler.isStopped()) {
                ChannelState rs = readState;
                if (data.hasRemaining()) {
                    if (debug.on()) {
                        debug.log("remaining bytes received %s",
                                  data.remaining());
                    }
                    if (!demand.isFulfilled()) {
                        try {
                            int oldPos = data.position();
                            reader.readFrame(data, decoder);
                            int newPos = data.position();
                            // Reader always consumes bytes:
                            assert oldPos != newPos : data;
                        } catch (Throwable e) {
                            receiveScheduler.stop();
                            messageConsumer.onError(e);
                        }
                        if (!data.hasRemaining()) {
                            rs = readState = UNREGISTERED;
                        }
                        continue;
                    }
                    break loop;
                }
                if (debug.on()) {
                    debug.log("receive state: %s", rs);
                }
                switch (rs) {
                    case WAITING:
                        break loop;
                    case UNREGISTERED:
                        try {
                            rs = readState = WAITING;
                            channel.registerEvent(readEvent);
                        } catch (Throwable e) {
                            receiveScheduler.stop();
                            messageConsumer.onError(e);
                        }
                        break loop;
                    case AVAILABLE:
                        try {
                            data = channel.read();
                        } catch (Throwable e) {
                            receiveScheduler.stop();
                            messageConsumer.onError(e);
                            break loop;
                        }
                        if (data == null) { // EOF
                            receiveScheduler.stop();
                            messageConsumer.onComplete();
                            break loop;
                        } else if (!data.hasRemaining()) {
                            // No data at the moment. Pretty much a "goto",
                            // reusing the existing code path for registration
                            rs = readState = UNREGISTERED;
                        }
                        continue loop;
                    default:
                        throw new InternalError(String.valueOf(rs));
                }
            }
            if (debug.on()) {
                debug.log("exit receive task");
            }
        }
    }

    private class WriteEvent implements RawChannel.RawEvent {

        @Override
        public int interestOps() {
            return SelectionKey.OP_WRITE;
        }

        @Override
        public void handle() {
            if (debug.on()) {
                debug.log("write event");
            }
            ChannelState s;
            do {
                s = writeState.get();
                if (s == CLOSED) {
                    if (debug.on()) {
                        debug.log("write state %s", s);
                    }
                    break;
                }
            } while (!writeState.compareAndSet(s, AVAILABLE));
            sendScheduler.runOrSchedule();
        }
    }

    private class ReadEvent implements RawChannel.RawEvent {

        @Override
        public int interestOps() {
            return SelectionKey.OP_READ;
        }

        @Override
        public void handle() {
            if (debug.on()) {
                debug.log("read event");
            }
            readState = AVAILABLE;
            receiveScheduler.runOrSchedule();
        }
    }
}
