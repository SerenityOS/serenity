/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.javax.crypto.small;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;


public abstract class CipherBench extends
    org.openjdk.bench.javax.crypto.full.CipherBench {

    public static class GCM extends
        org.openjdk.bench.javax.crypto.full.CipherBench.GCM {

        @Param({"AES"})
        private String permutation;

        @Param({"GCM"})
        private String mode;

        @Param({"NoPadding"})
        private String padding;

        @Param({"128"})
        private int keyLength;

        @Param({"" + 16 * 1024})
        private int dataSize;
    }

    public static class ChaCha20Poly1305 extends
        org.openjdk.bench.javax.crypto.full.CipherBench.ChaCha20Poly1305 {

        @Param({"ChaCha20-Poly1305"})
        private String permutation;

        @Param({"None"})
        private String mode;

        @Param({"NoPadding"})
        private String padding;

        @Param({"256"})
        private int keyLength;

        @Param({"" + 16 * 1024})
        private int dataSize;

    }
}
