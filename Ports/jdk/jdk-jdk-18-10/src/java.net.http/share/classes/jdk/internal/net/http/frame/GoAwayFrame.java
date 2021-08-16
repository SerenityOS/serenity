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

import static java.nio.charset.StandardCharsets.UTF_8;

public class GoAwayFrame extends ErrorFrame {

    private final int lastStream;
    private final byte[] debugData;

    public static final int TYPE = 0x7;


    public GoAwayFrame(int lastStream, int errorCode) {
        this(lastStream, errorCode, new byte[0]);
    }

    public GoAwayFrame(int lastStream, int errorCode, byte[] debugData) {
        super(0, 0, errorCode);
        this.lastStream = lastStream;
        this.debugData = debugData.clone();
    }

    @Override
    public int type() {
        return TYPE;
    }

    @Override
    int length() {
        return 8 + debugData.length;
    }

    @Override
    public String toString() {
        return super.toString() + " Debugdata: " + new String(debugData, UTF_8);
    }

    public int getLastStream() {
        return this.lastStream;
    }

    public byte[] getDebugData() {
        return debugData.clone();
    }

}
