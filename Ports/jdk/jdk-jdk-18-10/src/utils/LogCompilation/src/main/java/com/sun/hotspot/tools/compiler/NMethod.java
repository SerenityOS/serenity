/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package com.sun.hotspot.tools.compiler;

import java.io.PrintStream;

/**
 * A compilation log event that is signalled whenever a new nmethod (a native
 * method, a compilation result) is created.
 */
public class NMethod extends BasicLogEvent {

    /**
     * The nmethod's starting address in memory.
     */
    private long address;

    /**
     * The nmethod's size in bytes.
     */
    private long size;

    /**
     * The nmethod's insts size in bytes.
     */
    private long instSize;

    /**
     * The nmethod's compilation level.
     */
    private long level;

    /**
     * The name of the compiler performing this compilation.
     */
    private String compiler;

    NMethod(double s, String i, long a, long sz) {
        super(s, i);
        address = a;
        size = sz;
    }

    public void print(PrintStream out, boolean printID) {
        // XXX Currently we do nothing
        // throw new InternalError();
    }

    public long getAddress() {
        return address;
    }

    public void setAddress(long address) {
        this.address = address;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getInstSize() {
        return instSize;
    }

    public void setInstSize(long size) {
        this.instSize = size;
    }

    /**
     * @return the level
     */
    public long getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(long level) {
        assert this.level == 0 || this.level == level;
        this.level = level;
    }

    /**
     * @return the compiler
     */
    public String getCompiler() {
        return compiler;
    }

    /**
     * @param compiler the compiler to set
     */
    public void setCompiler(String compiler) {
        this.compiler = compiler;
    }
}
