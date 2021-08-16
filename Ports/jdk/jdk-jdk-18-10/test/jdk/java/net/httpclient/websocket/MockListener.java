/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

public class MockListener implements WebSocket.Listener {

    private final long bufferSize;
    private long count;
    private final List<Invocation> invocations = new ArrayList<>();
    private final CompletableFuture<?> lastCall = new CompletableFuture<>();
    private final Predicate<? super Invocation> collectUntil;

    public MockListener() {
        this(1, MockListener::closeOrError);
    }

    public MockListener(long bufferSize) {
        this(bufferSize, MockListener::closeOrError);
    }

    public MockListener(Predicate<? super Invocation> collectUntil) {
        this(1, collectUntil);
    }

    /*
     * Typical buffer sizes: 1, n, Long.MAX_VALUE
     */
    public MockListener(long bufferSize,
                        Predicate<? super Invocation> collectUntil) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException();
        }
        Objects.requireNonNull(collectUntil);
        this.bufferSize = bufferSize;
        this.collectUntil = collectUntil;
    }

    private static boolean closeOrError(Invocation i) {
        return i instanceof OnClose || i instanceof OnError;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        System.out.printf("onOpen(%s)%n", webSocket);
        OnOpen inv = new OnOpen(webSocket);
        synchronized (invocations) {
            invocations.add(inv);
        }
        if (collectUntil.test(inv)) {
            lastCall.complete(null);
        }
        onOpen0(webSocket);
    }

    protected void onOpen0(WebSocket webSocket) {
        count = bufferSize - bufferSize / 2;
        System.out.printf("request(%d)%n", bufferSize);
        webSocket.request(bufferSize);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket,
                                     CharSequence message,
                                     boolean last) {
        System.out.printf("onText(%s, message.length=%s, %s)%n", webSocket, message.length(), last);
        OnText inv = new OnText(webSocket, message.toString(), last);
        synchronized (invocations) {
            invocations.add(inv);
        }
        if (collectUntil.test(inv)) {
            lastCall.complete(null);
        }
        return onText0(webSocket, message, last);
    }

    protected CompletionStage<?> onText0(WebSocket webSocket,
                                         CharSequence message,
                                         boolean last) {
        replenish(webSocket);
        return null;
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket,
                                       ByteBuffer message,
                                       boolean last) {
        System.out.printf("onBinary(%s, %s, %s)%n", webSocket, message, last);
        OnBinary inv = new OnBinary(webSocket, fullCopy(message), last);
        synchronized (invocations) {
            invocations.add(inv);
        }
        if (collectUntil.test(inv)) {
            lastCall.complete(null);
        }
        return onBinary0(webSocket, message, last);
    }

    protected CompletionStage<?> onBinary0(WebSocket webSocket,
                                           ByteBuffer message,
                                           boolean last) {
        replenish(webSocket);
        return null;
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        System.out.printf("onPing(%s, %s)%n", webSocket, message);
        OnPing inv = new OnPing(webSocket, fullCopy(message));
        synchronized (invocations) {
            invocations.add(inv);
        }
        if (collectUntil.test(inv)) {
            lastCall.complete(null);
        }
        return onPing0(webSocket, message);
    }

    protected CompletionStage<?> onPing0(WebSocket webSocket, ByteBuffer message) {
        replenish(webSocket);
        return null;
    }

    @Override
    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
        System.out.printf("onPong(%s, %s)%n", webSocket, message);
        OnPong inv = new OnPong(webSocket, fullCopy(message));
        synchronized (invocations) {
            invocations.add(inv);
        }
        if (collectUntil.test(inv)) {
            lastCall.complete(null);
        }
        return onPong0(webSocket, message);
    }

    protected CompletionStage<?> onPong0(WebSocket webSocket, ByteBuffer message) {
        replenish(webSocket);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket,
                                      int statusCode,
                                      String reason) {
        System.out.printf("onClose(%s, %s, %s)%n", webSocket, statusCode, reason);
        OnClose inv = new OnClose(webSocket, statusCode, reason);
        synchronized (invocations) {
            invocations.add(inv);
        }
        if (collectUntil.test(inv)) {
            lastCall.complete(null);
        }
        return onClose0(webSocket, statusCode, reason);
    }

    protected CompletionStage<?> onClose0(WebSocket webSocket,
                                          int statusCode,
                                          String reason) {
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        System.out.printf("onError(%s, %s)%n", webSocket, error);
        error.printStackTrace(System.out);
        OnError inv = new OnError(webSocket, error == null ? null : error.getClass());
        synchronized (invocations) {
            invocations.add(inv);
        }
        if (collectUntil.test(inv)) {
            lastCall.complete(null);
        }
        onError0(webSocket, error);
    }

    protected void onError0(WebSocket webSocket, Throwable error) { }

    public List<Invocation> invocationsSoFar() {
        synchronized (invocations) {
            return new ArrayList<>(invocations);
        }
    }

    public List<Invocation> invocations() {
        lastCall.join();
        synchronized (invocations) {
            return new ArrayList<>(invocations);
        }
    }

    protected void replenish(WebSocket webSocket) {
        if (--count <= 0) {
            count = bufferSize - bufferSize / 2;
            webSocket.request(count);
            System.out.printf("request(%d)%n", count);
        }
    }

    public abstract static class Invocation {

        public static OnOpen onOpen(WebSocket webSocket) {
            return new OnOpen(webSocket);
        }

        public static OnText onText(WebSocket webSocket,
                                    String text,
                                    boolean last) {
            return new OnText(webSocket, text, last);
        }

        public static OnBinary onBinary(WebSocket webSocket,
                                        ByteBuffer data,
                                        boolean last) {
            return new OnBinary(webSocket, data, last);
        }

        public static OnPing onPing(WebSocket webSocket,
                                    ByteBuffer data) {
            return new OnPing(webSocket, data);
        }

        public static OnPong onPong(WebSocket webSocket,
                                    ByteBuffer data) {
            return new OnPong(webSocket, data);
        }

        public static OnClose onClose(WebSocket webSocket,
                                      int statusCode,
                                      String reason) {
            return new OnClose(webSocket, statusCode, reason);
        }

        public static OnError onError(WebSocket webSocket,
                                      Class<? extends Throwable> clazz) {
            return new OnError(webSocket, clazz);
        }

        final WebSocket webSocket;

        private Invocation(WebSocket webSocket) {
            this.webSocket = webSocket;
        }
    }

    public static final class OnOpen extends Invocation {

        public OnOpen(WebSocket webSocket) {
            super(webSocket);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Invocation that = (Invocation) o;
            return Objects.equals(webSocket, that.webSocket);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(webSocket);
        }

        @Override
        public String toString() {
            return String.format("onOpen(%s)", webSocket);
        }
    }

    public static final class OnText extends Invocation {

        final String text;
        final boolean last;

        public OnText(WebSocket webSocket, String text, boolean last) {
            super(webSocket);
            this.text = text;
            this.last = last;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OnText onText = (OnText) o;
            return Objects.equals(text, onText.text) &&
                    last == onText.last &&
                    Objects.equals(webSocket, onText.webSocket);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, last, webSocket);
        }

        @Override
        public String toString() {
            return String.format("onText(%s, message.length=%s, %s)", webSocket, text.length(), last);
        }
    }

    public static final class OnBinary extends Invocation {

        final ByteBuffer data;
        final boolean last;

        public OnBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            super(webSocket);
            this.data = data;
            this.last = last;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OnBinary onBinary = (OnBinary) o;
            return Objects.equals(data, onBinary.data) &&
                    last == onBinary.last &&
                    Objects.equals(webSocket, onBinary.webSocket);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, last, webSocket);
        }

        @Override
        public String toString() {
            return String.format("onBinary(%s, %s, %s)", webSocket, data, last);
        }
    }

    public static final class OnPing extends Invocation {

        final ByteBuffer data;

        public OnPing(WebSocket webSocket, ByteBuffer data) {
            super(webSocket);
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OnPing onPing = (OnPing) o;
            return Objects.equals(data, onPing.data) &&
                    Objects.equals(webSocket, onPing.webSocket);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, webSocket);
        }

        @Override
        public String toString() {
            return String.format("onPing(%s, %s)", webSocket, data);
        }
    }

    public static final class OnPong extends Invocation {

        final ByteBuffer data;

        public OnPong(WebSocket webSocket, ByteBuffer data) {
            super(webSocket);
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OnPong onPong = (OnPong) o;
            return Objects.equals(data, onPong.data) &&
                    Objects.equals(webSocket, onPong.webSocket);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data, webSocket);
        }

        @Override
        public String toString() {
            return String.format("onPong(%s, %s)", webSocket, data);
        }
    }

    public static final class OnClose extends Invocation {

        final int statusCode;
        final String reason;

        public OnClose(WebSocket webSocket, int statusCode, String reason) {
            super(webSocket);
            this.statusCode = statusCode;
            this.reason = reason;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OnClose onClose = (OnClose) o;
            return statusCode == onClose.statusCode &&
                    Objects.equals(reason, onClose.reason) &&
                    Objects.equals(webSocket, onClose.webSocket);
        }

        @Override
        public int hashCode() {
            return Objects.hash(statusCode, reason, webSocket);
        }

        @Override
        public String toString() {
            return String.format("onClose(%s, %s, %s)", webSocket, statusCode, reason);
        }
    }

    public static final class OnError extends Invocation {

        final Class<? extends Throwable> clazz;

        public OnError(WebSocket webSocket, Class<? extends Throwable> clazz) {
            super(webSocket);
            this.clazz = clazz;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OnError onError = (OnError) o;
            return Objects.equals(clazz, onError.clazz) &&
                    Objects.equals(webSocket, onError.webSocket);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, webSocket);
        }

        @Override
        public String toString() {
            return String.format("onError(%s, %s)", webSocket, clazz);
        }
    }

    private static ByteBuffer fullCopy(ByteBuffer src) {
        ByteBuffer copy = ByteBuffer.allocate(src.capacity());
        int p = src.position();
        int l = src.limit();
        src.clear();
        copy.put(src).position(p).limit(l);
        src.position(p).limit(l);
        return copy;
    }
}
