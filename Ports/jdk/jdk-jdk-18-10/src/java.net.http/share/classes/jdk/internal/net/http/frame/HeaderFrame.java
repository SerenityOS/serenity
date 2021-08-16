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

import jdk.internal.net.http.common.Utils;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Either a HeadersFrame or a ContinuationFrame
 */
public abstract class HeaderFrame extends Http2Frame {

    final int headerLength;
    final List<ByteBuffer> headerBlocks;

    public static final int END_STREAM = 0x1;
    public static final int END_HEADERS = 0x4;

    public HeaderFrame(int streamid, int flags, List<ByteBuffer> headerBlocks) {
        super(streamid, flags);
        this.headerBlocks = headerBlocks;
        this.headerLength = Utils.remaining(headerBlocks, Integer.MAX_VALUE);
    }

    @Override
    public String flagAsString(int flag) {
        return switch (flag) {
            case END_HEADERS -> "END_HEADERS";
            case END_STREAM  -> "END_STREAM";

            default -> super.flagAsString(flag);
        };
    }


    public List<ByteBuffer> getHeaderBlock() {
        return headerBlocks;
    }

    int getHeaderLength() {
        return headerLength;
    }

    /**
     * Returns true if this block is the final block of headers.
     */
    public boolean endHeaders() {
        return getFlag(END_HEADERS);
    }
}
