/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/*
 * A WebSocket view of the underlying communication channel. This view provides
 * an asynchronous exchange of WebSocket messages rather than asynchronous
 * exchange of bytes.
 *
 * Methods sendText, sendBinary, sendPing, sendPong and sendClose initiate a
 * corresponding operation and return a CompletableFuture (CF) which will
 * complete once the operation has completed (succeeded or failed).
 *
 * These methods are designed such that their clients may take an advantage on
 * possible implementation optimizations. Namely, these methods:
 *
 * 1. May return null which is considered the same as a CF completed normally
 * 2. Accept an arbitrary attachment to complete a CF with
 * 3. Accept an action to take once the operation has completed
 *
 * All of the above allows not to create unnecessary instances of CF.
 * For example, if a message has been sent straight away, there's no need to
 * create a CF (given the parties agree on the meaning of null and are prepared
 * to handle it).
 * If the result of a returned CF is useless to the client, they may specify the
 * exact instance (attachment) they want the CF to complete with. Thus, no need
 * to create transforming stages (e.g. thenApply(useless -> myResult)).
 * If there is the same action that needs to be done each time the CF completes,
 * the client may pass it directly to the method instead of creating a dependant
 * stage (e.g. whenComplete(action)).
 */
public interface Transport {

    <T> CompletableFuture<T> sendText(CharSequence message,
                                      boolean isLast,
                                      T attachment,
                                      BiConsumer<? super T, ? super Throwable> action);

    <T> CompletableFuture<T> sendBinary(ByteBuffer message,
                                        boolean isLast,
                                        T attachment,
                                        BiConsumer<? super T, ? super Throwable> action);

    <T> CompletableFuture<T> sendPing(ByteBuffer message,
                                      T attachment,
                                      BiConsumer<? super T, ? super Throwable> action);

    <T> CompletableFuture<T> sendPong(ByteBuffer message,
                                      T attachment,
                                      BiConsumer<? super T, ? super Throwable> action);

    /*
     * Sends a Pong message with initially unknown data. Used for sending the
     * most recent automatic Pong reply.
     */
    <T> CompletableFuture<T> sendPong(Supplier<? extends ByteBuffer> message,
                                      T attachment,
                                      BiConsumer<? super T, ? super Throwable> action);

    <T> CompletableFuture<T> sendClose(int statusCode,
                                       String reason,
                                       T attachment,
                                       BiConsumer<? super T, ? super Throwable> action);

    void request(long n);

    /*
     * Why is this method needed? Since receiving of messages operates through
     * callbacks this method allows to abstract out what constitutes as a
     * message being received (i.e. to decide outside this type when exactly one
     * should decrement the demand).
     */
    void acknowledgeReception(); // TODO: hide

    /*
     * If this method is invoked, then all pending and subsequent send
     * operations will fail with IOException.
     */
    void closeOutput() throws IOException;

    void closeInput() throws IOException;
}
