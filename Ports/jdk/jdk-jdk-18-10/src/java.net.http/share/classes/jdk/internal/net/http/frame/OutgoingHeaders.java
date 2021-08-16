/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.frame;

import java.net.http.HttpHeaders;

/**
 * Contains all parameters for outgoing headers. Is converted to
 * HeadersFrame and ContinuationFrames by Http2Connection.
 */
public class OutgoingHeaders<T> extends Http2Frame {

    int streamDependency;
    int weight;
    boolean exclusive;
    T attachment;

    public static final int PRIORITY = 0x20;

    HttpHeaders user, system;

    public OutgoingHeaders(HttpHeaders hdrs1, HttpHeaders hdrs2, T attachment) {
        super(0, 0);
        this.user = hdrs2;
        this.system = hdrs1;
        this.attachment = attachment;
    }

    public void setPriority(int streamDependency, boolean exclusive, int weight) {
        this.streamDependency = streamDependency;
        this.exclusive = exclusive;
        this.weight = weight;
        this.flags |= PRIORITY;
    }

    public int getStreamDependency() {
        return streamDependency;
    }

    public int getWeight() {
        return weight;
    }

    public boolean getExclusive() {
        return exclusive;
    }

    public T getAttachment() {
        return attachment;
    }

    public HttpHeaders getUserHeaders() {
        return user;
    }

    public HttpHeaders getSystemHeaders() {
        return system;
    }

}
