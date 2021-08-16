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

import java.nio.ByteBuffer;

/**
 * No implementation provided for onInit() because that must always be
 * implemented by user
 */
abstract class DefaultMessageStreamHandler implements MessageStreamHandler {

    public void onText(CharSequence data, boolean last) {}

    public void onBinary(ByteBuffer data, boolean last) {}

    public void onPing(ByteBuffer data) {}

    public void onPong(ByteBuffer data) {}

    public void onClose(int statusCode, CharSequence reason) {}

    public void onComplete() {}

    public void onError(Throwable e) {}
}

