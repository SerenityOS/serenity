/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.websocket;

/**
 * WebSocket server listener interface, which is the same as the client API
 * in java.net.http. See MessageStreamResponder for how listener methods
 * can send response messages back to the client
 *
 * All MessageStreamConsumer methods must be implemented (plus the handler method
 * declared here). DefaultMessageStreamHandler provides empty implementations of all
 * that can be extended, except for onInit() which must always be implemented.
 *
 *    void onText(CharSequence data, boolean last);
 *
 *    void onBinary(ByteBuffer data, boolean last);
 *
 *    void onPing(ByteBuffer data);
 *
 *    void onPong(ByteBuffer data);
 *
 *    void onClose(int statusCode, CharSequence reason);
 *
 *    void onComplete();
 *
 *    void onError(Throwable e);
 */
interface MessageStreamHandler extends MessageStreamConsumer {

    /**
     * called before any of the methods above to supply a
     * MessageStreamResponder for any new connection, which can be used to send replies
     * sendText(), sendBinary(), sendClose() etc
     */
    void onInit(MessageStreamResponder responder);
}

