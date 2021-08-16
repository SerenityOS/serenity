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

public class PingFrame extends Http2Frame {


    private final byte[] data;

    public static final int TYPE = 0x6;

    // Flags
    public static final int ACK = 0x1;

    public PingFrame(int flags, byte[] data) {
        super(0, flags);
        assert data.length == 8;
        this.data = data.clone();
    }

    @Override
    public int type() {
        return TYPE;
    }

    @Override
    int length() {
        return 8;
    }

    @Override
    public String flagAsString(int flag) {
        return switch (flag) {
            case ACK -> "ACK";
            default -> super.flagAsString(flag);
        };
    }

    public byte[] getData() {
        return data.clone();
    }

}
