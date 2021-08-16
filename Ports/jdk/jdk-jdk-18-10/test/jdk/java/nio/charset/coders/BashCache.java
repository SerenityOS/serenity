/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4517279
 * @summary Stochastic test of thread-local coder caches
 * @key randomness
 */

import java.nio.*;
import java.nio.charset.*;
import java.util.*;


public class BashCache {

    private static final int THREADS = 10;
    private static final int TRIALS = 1000;

    private static final Charset[] charsets
        = new Charset[] {
            Charset.forName("US-ASCII"),
            Charset.forName("UTF-8"),
            Charset.forName("CP1252"),
            Charset.forName("UTF-16BE") };

    private static volatile boolean failed = false;

    private static class Basher extends Thread {

        Random rnd = new Random(System.identityHashCode(this));

        public void run() {
            for (int i = 0; i < TRIALS; i++) {
                Charset cs = charsets[rnd.nextInt(4)];
                try {
                    if (rnd.nextBoolean()) {
                        cs.encode("hi mom");
                    } else {
                        cs.decode(ByteBuffer.wrap(new byte[] {
                            (byte)'x', (byte)'y',
                            (byte)'z', (byte)'z',
                            (byte)'y' }));
                    }
                } catch (Exception x) {
                    x.printStackTrace();
                    failed = true;
                    return;
                }
                if (rnd.nextBoolean())
                    Thread.yield();
            }
        }

    }

    public static void main(String[] args) throws Exception {
        Charset cs = Charset.forName("us-ascii");
        Basher[] bashers = new Basher[THREADS];
        for (int i = 0; i < THREADS; i++) {
            bashers[i] = new Basher();
            bashers[i].start();
        }
        for (int i = 0; i < THREADS; i++)
            bashers[i].join();
        if (failed)
            throw new Exception("Test failed");
    }

}
