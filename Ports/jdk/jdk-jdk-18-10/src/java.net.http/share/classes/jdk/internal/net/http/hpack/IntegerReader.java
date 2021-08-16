/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static java.lang.String.format;

final class IntegerReader {

    private static final int NEW             = 0;
    private static final int CONFIGURED      = 1;
    private static final int FIRST_BYTE_READ = 2;
    private static final int DONE            = 4;

    private int state = NEW;

    private int N;
    private int maxValue;
    private int value;
    private long r;
    private long b = 1;

    public IntegerReader configure(int N) {
        return configure(N, Integer.MAX_VALUE);
    }

    //
    // Why is it important to configure 'maxValue' here. After all we can wait
    // for the integer to be fully read and then check it. Can't we?
    //
    // Two reasons.
    //
    // 1. Value wraps around long won't be unnoticed.
    // 2. It can spit out an exception as soon as it becomes clear there's
    // an overflow. Therefore, no need to wait for the value to be fully read.
    //
    public IntegerReader configure(int N, int maxValue) {
        if (state != NEW) {
            throw new IllegalStateException("Already configured");
        }
        checkPrefix(N);
        if (maxValue < 0) {
            throw new IllegalArgumentException(
                    "maxValue >= 0: maxValue=" + maxValue);
        }
        this.maxValue = maxValue;
        this.N = N;
        state = CONFIGURED;
        return this;
    }

    public boolean read(ByteBuffer input) throws IOException {
        if (state == NEW) {
            throw new IllegalStateException("Configure first");
        }
        if (state == DONE) {
            return true;
        }
        if (!input.hasRemaining()) {
            return false;
        }
        if (state == CONFIGURED) {
            int max = (2 << (N - 1)) - 1;
            int n = input.get() & max;
            if (n != max) {
                value = n;
                state = DONE;
                return true;
            } else {
                r = max;
            }
            state = FIRST_BYTE_READ;
        }
        if (state == FIRST_BYTE_READ) {
            // variable-length quantity (VLQ)
            byte i;
            do {
                if (!input.hasRemaining()) {
                    return false;
                }
                i = input.get();
                long increment = b * (i & 127);
                if (r + increment > maxValue) {
                    throw new IOException(format(
                            "Integer overflow: maxValue=%,d, value=%,d",
                            maxValue, r + increment));
                }
                r += increment;
                b *= 128;
            } while ((128 & i) == 128);

            value = (int) r;
            state = DONE;
            return true;
        }
        throw new InternalError(Arrays.toString(
                new Object[]{state, N, maxValue, value, r, b}));
    }

    public int get() throws IllegalStateException {
        if (state != DONE) {
            throw new IllegalStateException("Has not been fully read yet");
        }
        return value;
    }

    private static void checkPrefix(int N) {
        if (N < 1 || N > 8) {
            throw new IllegalArgumentException("1 <= N <= 8: N= " + N);
        }
    }

    public IntegerReader reset() {
        b = 1;
        state = NEW;
        return this;
    }
}
