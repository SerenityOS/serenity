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
package jdk.internal.net.http.hpack;

import java.nio.ByteBuffer;
import java.util.Arrays;

final class IntegerWriter {

    private static final int NEW                = 0;
    private static final int CONFIGURED         = 1;
    private static final int FIRST_BYTE_WRITTEN = 2;
    private static final int DONE               = 4;

    private int state = NEW;

    private int payload;
    private int N;
    private int value;

    //
    //      0   1   2   3   4   5   6   7
    //    +---+---+---+---+---+---+---+---+
    //    |   |   |   |   |   |   |   |   |
    //    +---+---+---+-------------------+
    //    |<--------->|<----------------->|
    //       payload           N=5
    //
    // payload is the contents of the left-hand side part of the octet;
    //         it is truncated to fit into 8-N bits, where 1 <= N <= 8;
    //
    public IntegerWriter configure(int value, int N, int payload) {
        if (state != NEW) {
            throw new IllegalStateException("Already configured");
        }
        if (value < 0) {
            throw new IllegalArgumentException("value >= 0: value=" + value);
        }
        checkPrefix(N);
        this.value = value;
        this.N = N;
        this.payload = payload & 0xFF & (0xFFFFFFFF << N);
        state = CONFIGURED;
        return this;
    }

    public boolean write(ByteBuffer output) {
        if (state == NEW) {
            throw new IllegalStateException("Configure first");
        }
        if (state == DONE) {
            return true;
        }

        if (!output.hasRemaining()) {
            return false;
        }
        if (state == CONFIGURED) {
            int max = (2 << (N - 1)) - 1;
            if (value < max) {
                output.put((byte) (payload | value));
                state = DONE;
                return true;
            }
            output.put((byte) (payload | max));
            value -= max;
            state = FIRST_BYTE_WRITTEN;
        }
        if (state == FIRST_BYTE_WRITTEN) {
            while (value >= 128 && output.hasRemaining()) {
                output.put((byte) ((value & 127) + 128));
                value /= 128;
            }
            if (!output.hasRemaining()) {
                return false;
            }
            output.put((byte) value);
            state = DONE;
            return true;
        }
        throw new InternalError(Arrays.toString(
                new Object[]{state, payload, N, value}));
    }

    private static void checkPrefix(int N) {
        if (N < 1 || N > 8) {
            throw new IllegalArgumentException("1 <= N <= 8: N= " + N);
        }
    }

    public IntegerWriter reset() {
        state = NEW;
        return this;
    }
}
