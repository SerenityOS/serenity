/*
 * Copyright (c) 2020, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.openjdk.micro.bench.java.util;

import org.openjdk.jmh.annotations.*;
import java.util.*;

public class Base64VarLenDecode {

    @State(Scope.Thread)
    public static class MyState {

        @Setup(Level.Trial)
        public void doSetupTrial() {
            ran = new Random(10101); // fixed seed for repeatability
            encoder = Base64.getEncoder();
            decoder = Base64.getDecoder();
            System.out.println("Do Trial Setup");
        }

        @Setup(Level.Invocation)
        public void doSetupInvocation() {
            bin_src_len = 8 + ran.nextInt(20000);
            base64_len = ((bin_src_len + 2) / 3) * 4;
            unencoded = new byte[bin_src_len];
            encoded = new byte[base64_len];
            decoded = new byte[bin_src_len];
            ran.nextBytes(unencoded);
            encoder.encode(unencoded, encoded);
        }

        @TearDown(Level.Invocation)
        public void doTearDownInvocation() {
            // This isn't really a teardown.  It's a check for correct functionality.
            // Each iteration should produce a correctly decoded buffer that's equal
            // to the unencoded data.
            if (!Arrays.equals(unencoded, decoded)) {
                System.out.println("Original data and decoded data are not equal!");
                for (int j = 0; j < unencoded.length; j++) {
                    if (unencoded[j] != decoded[j]) {
                        System.out.format("%06x: %02x %02x\n", j, unencoded[j], decoded[j]);
                    }
                }
                System.exit(1);
            }
        }

        public Random ran;
        public Base64.Encoder encoder;
        public Base64.Decoder decoder;
        public int bin_src_len;
        public int base64_len;
        public byte[] unencoded;
        public byte[] encoded;
        public byte[] decoded;
    }

    @Benchmark
    public void decodeMethod(MyState state) {
       state.decoder.decode(state.encoded, state.decoded);
    }
}
