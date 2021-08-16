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

import java.nio.ByteBuffer;
import java.util.List;

public class HeadersFrame extends HeaderFrame {

    public static final int TYPE = 0x1;

    // Flags
    public static final int END_STREAM = 0x1;
    public static final int PADDED = 0x8;
    public static final int PRIORITY = 0x20;


    private int padLength;
    private int streamDependency;
    private int weight;
    private boolean exclusive;

    public HeadersFrame(int streamid, int flags, List<ByteBuffer> headerBlocks, int padLength) {
        super(streamid, flags, headerBlocks);
        if (padLength > 0) {
            setPadLength(padLength);
        }
    }

    public HeadersFrame(int streamid, int flags, List<ByteBuffer> headerBlocks) {
        super(streamid, flags, headerBlocks);
    }

    public HeadersFrame(int streamid, int flags, ByteBuffer headerBlock) {
        this(streamid, flags, List.of(headerBlock));
    }

    @Override
    public int type() {
        return TYPE;
    }

    @Override
    int length() {
        return headerLength
                + ((flags & PADDED) != 0 ? (1 + padLength) : 0)
                + ((flags & PRIORITY) != 0 ? 5 : 0);
    }

    @Override
    public String flagAsString(int flag) {
        return switch (flag) {
            case END_STREAM -> "END_STREAM";
            case PADDED     -> "PADDED";
            case PRIORITY   -> "PRIORITY";

            default -> super.flagAsString(flag);
        };
    }

    public void setPadLength(int padLength) {
        this.padLength = padLength;
        flags |= PADDED;
    }

    int getPadLength() {
        return padLength;
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

}
