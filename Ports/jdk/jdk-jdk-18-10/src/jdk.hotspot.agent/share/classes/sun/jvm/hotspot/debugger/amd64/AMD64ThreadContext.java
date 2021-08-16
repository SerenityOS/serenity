/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.amd64;

import java.lang.annotation.Native;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;

/** Specifies the thread context on amd64 platforms; only a sub-portion
 * of the context is guaranteed to be present on all operating
 * systems. */

public abstract class AMD64ThreadContext implements ThreadContext {
    // Taken from /usr/include/sys/regset.h on Solaris/AMD64.

    // NOTE: the indices for the various registers must be maintained as
    // listed across various operating systems. However, only a small
    // subset of the registers' values are guaranteed to be present (and
    // must be present for the SA's stack walking to work)

    // One instance of the Native annotation is enough to trigger header generation
    // for this file.
    @Native
    public static final int R15 = 0;
    public static final int R14 = 1;
    public static final int R13 = 2;
    public static final int R12 = 3;
    public static final int R11 = 4;
    public static final int R10 = 5;
    public static final int R9  = 6;
    public static final int R8  = 7;
    public static final int RDI = 8;
    public static final int RSI = 9;
    public static final int RBP = 10;
    public static final int RBX = 11;
    public static final int RDX = 12;
    public static final int RCX = 13;
    public static final int RAX = 14;
    public static final int TRAPNO = 15;
    public static final int ERR = 16;
    public static final int RIP = 17;
    public static final int CS = 18;
    public static final int RFL = 19;
    public static final int RSP = 20;
    public static final int SS = 21;
    public static final int FS = 22;
    public static final int GS = 23;
    public static final int ES = 24;
    public static final int DS = 25;
    public static final int FSBASE = 26;
    public static final int GSBASE = 27;

    public static final int NPRGREG = 28;

    private static final String[] regNames = {
        "r15",  "r14", "r13", "r12", "r11", "r10", "r9", "r8",
        "rdi",  "rsi", "rbp", "rbx", "rdx", "rcx", "rax", "trapno",
        "err",  "rip", "cs",  "rfl", "rsp", "ss",  "fs", "gs",
        "es",   "ds",  "fsbase", "gsbase"
    };

    private long[] data;

    public AMD64ThreadContext() {
        data = new long[NPRGREG];
    }

    public int getNumRegisters() {
        return NPRGREG;
    }

    public String getRegisterName(int index) {
        return regNames[index];
    }

    public void setRegister(int index, long value) {
        data[index] = value;
    }

    public long getRegister(int index) {
        return data[index];
    }

    public CFrame getTopFrame(Debugger dbg) {
        return null;
    }

    /** This can't be implemented in this class since we would have to
     * tie the implementation to, for example, the debugging system */
    public abstract void setRegisterAsAddress(int index, Address value);

    /** This can't be implemented in this class since we would have to
     * tie the implementation to, for example, the debugging system */
    public abstract Address getRegisterAsAddress(int index);
}
