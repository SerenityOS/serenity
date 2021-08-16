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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BBPoint {

    static {
        System.loadLibrary("JNIPoint");
    }

    private final ByteBuffer buff;

    public BBPoint(int x, int y) {
        this.buff = ByteBuffer.allocateDirect(4 * 2).order(ByteOrder.nativeOrder());
        setX(x);
        setY(y);
    }

    public void setX(int x) {
        buff.putInt(0, x);
    }

    public int getX() {
        return buff.getInt(0);
    }

    public int getY() {
        return buff.getInt(1);
    }

    public void setY(int y) {
        buff.putInt(0, y);
    }

    public double distanceTo(BBPoint other) {
        return distance(buff, other.buff);
    }

    private static native double distance(ByteBuffer p1, ByteBuffer p2);
}
