/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.util.random.RandomSupport;

/**
 * Defines implementations of the
 * {@linkplain java.util.random.RandomGenerator RandomGenerator Interface}.
 *
 * @provides jdk.random.L128X1024MixRandom
 * @provides jdk.random.L128X128MixRandom
 * @provides jdk.random.L128X256MixRandom
 * @provides jdk.random.L32X64MixRandom
 * @provides jdk.random.L64X1024MixRandom
 * @provides jdk.random.L64X128MixRandom
 * @provides jdk.random.L64X128StarStarRandom
 * @provides jdk.random.L64X256MixRandom
 * @provides jdk.random.Xoroshiro128PlusPlus
 * @provides jdk.random.Xoshiro256PlusPlus
 *
 * @use java.util.random.RandomGenerator
 * @use jdk.internal.util.random.RandomSupport
 *
 * @moduleGraph
 * @since 16
 */
module jdk.random {
    exports jdk.random to
            java.base;

    provides java.util.random.RandomGenerator with
        jdk.random.L32X64MixRandom,
        jdk.random.L64X128MixRandom,
        jdk.random.L64X128StarStarRandom,
        jdk.random.L64X256MixRandom,
        jdk.random.L64X1024MixRandom,
        jdk.random.L128X128MixRandom,
        jdk.random.L128X256MixRandom,
        jdk.random.L128X1024MixRandom,
        jdk.random.Xoroshiro128PlusPlus,
        jdk.random.Xoshiro256PlusPlus;
}
