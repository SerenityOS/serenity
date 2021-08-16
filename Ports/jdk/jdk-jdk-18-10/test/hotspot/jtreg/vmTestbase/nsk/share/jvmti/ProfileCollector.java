/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jvmti;

/**
 * Profile collector class intended to be used with HotSwap-agent.
 *
 * <p>This class provides methods which are used in bytecode instrumentation
 * to profile method calls and memory allocations.</p>
 */

public class ProfileCollector {

    static int callCount = 0;
    static int allocCount = 0;

    public static void reset() {
        callCount = 0;
        allocCount = 0;
    }

    public static void callTracker() {
        ++callCount;
    }

    public static void allocTracker() {
        ++allocCount;
    }
}
