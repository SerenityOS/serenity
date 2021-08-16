/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.runtime;

/** This is a type-safe enum mirroring the ThreadState enum in
 osThread.hpp. The conversion between the underlying ints
 and these values is done in OSThread. */

public class ThreadState {

    private String printVal;

    /** Memory has been allocated but not initialized */
    public static final ThreadState ALLOCATED = new ThreadState("allocated");
    /** The thread has been initialized but yet started */
    public static final ThreadState INITIALIZED = new ThreadState("initialized");
    /** Has been started and is runnable, but not necessarily running */
    public static final ThreadState RUNNABLE = new ThreadState("runnable");
    /** Waiting on a contended monitor lock */
    public static final ThreadState MONITOR_WAIT = new ThreadState("waiting for monitor entry");
    /** Waiting on a condition variable */
    public static final ThreadState CONDVAR_WAIT = new ThreadState("waiting on condition");
    /** Waiting on an Object.wait() call */
    public static final ThreadState OBJECT_WAIT = new ThreadState("in Object.wait()");
    /** Suspended at breakpoint */
    public static final ThreadState BREAKPOINTED = new ThreadState("at breakpoint");
    /** Thread.sleep() */
    public static final ThreadState SLEEPING = new ThreadState("sleeping");
    /** All done, but not reclaimed yet */
    public static final ThreadState ZOMBIE = new ThreadState("zombie");

    private ThreadState(String printVal){
        this.printVal = printVal;
    }

    public String getPrintVal() {
        return printVal;
    }
}
