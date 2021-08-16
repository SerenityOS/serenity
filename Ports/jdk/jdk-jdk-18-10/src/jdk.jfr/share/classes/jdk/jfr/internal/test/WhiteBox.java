/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal.test;

public final class WhiteBox {

    private static boolean writeAllObjectSamples;
    private static boolean skipBFS;

    /**
     * If OldObjectSample event is enabled, calling this method
     * ensures that all object samples are written, including short-lived objects.
     * Purpose of this method is to increase determinism in tests.
     *
     * @param writeAllObjectSamples if all samples should be written or not
     *
     */
    public static void setWriteAllObjectSamples(boolean writeAllSamples) {
        writeAllObjectSamples = writeAllSamples;
    }

    public static boolean getWriteAllObjectSamples() {
        return writeAllObjectSamples;
    }

    /**
     * If OldObjectSample event is enabled, calling this method
     * ensures that BFS is not used when searching for path to GC root.
     * Purpose of this method is to trigger code paths that are
     * hard to provoke reliably in testing.
     *
     * @param skipBFS if only DFS should be used
     */
    public static void setSkipBFS(boolean skip) {
        skipBFS = skip;
    }

    public static boolean getSkipBFS() {
        return skipBFS;
    }
}
