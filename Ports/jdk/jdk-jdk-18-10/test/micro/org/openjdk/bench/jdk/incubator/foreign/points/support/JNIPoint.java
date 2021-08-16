/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.jdk.incubator.foreign.points.support;

public class JNIPoint implements AutoCloseable {

    static {
        System.loadLibrary("JNIPoint");
    }

    private final long peer;

    public JNIPoint(int x, int y) {
        peer = allocate();
        setX(peer, x);
        setY(peer, y);
    }

    public void free() {
        free(peer);
    }

    public int getX() {
        return getX(peer);
    }

    public void setX(int value) {
        setX(peer, value);
    }

    public int getY() {
        return getY(peer);
    }

    public void setY(int value) {
        setY(peer, value);
    }

    public double distanceTo(JNIPoint other) {
        return distance(peer, other.peer);
    }

    private static native long allocate();
    private static native void free(long ptr);

    private static native int getX(long ptr);
    private static native void setX(long ptr, int x);

    private static native int getY(long ptr);
    private static native void setY(long ptr, int y);

    private static native double distance(long p1, long p2);

    @Override
    public void close() {
        free();
    }
}
