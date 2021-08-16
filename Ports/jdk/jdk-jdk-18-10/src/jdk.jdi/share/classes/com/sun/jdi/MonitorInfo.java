/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdi;

/**
 * Information about a monitor owned by a thread.
 *
 * @author Swamy Venkataramanappa
 * @since  1.6
 */

public interface MonitorInfo extends Mirror {

    /**
     * Returns the {@link ObjectReference} object for the monitor.
     * @return the {@link ObjectReference} object for the monitor.
     * @throws InvalidStackFrameException if the associated stack
     * frame has become invalid. Once the frame's thread is resumed,
     * the stack frame is no longer valid.
     * @see ThreadReference#ownedMonitorsAndFrames
     * @since 1.6
     */
    public ObjectReference monitor();

    /**
     * Returns the stack depth at which this monitor was
     * acquired by the owning thread. Returns -1 if the
     * implementation cannot determine the stack depth
     * (e.g., for monitors acquired by JNI MonitorEnter).
     * @return the stack depth at which this monitor was
     * acquired by the owning thread.
     * @throws InvalidStackFrameException if the associated stack
     * frame has become invalid. Once the frame's thread is resumed,
     * the stack frame is no longer valid.
     * @see ThreadReference#ownedMonitorsAndFrames
     */
    public int stackDepth();

    /**
     * Returns a {@link ThreadReference} object for the thread that
     * owns the monitor.
     * @return a {@link ThreadReference} object for the thread that
     * owns the monitor.
     * @throws InvalidStackFrameException if the associated stack
     * frame has become invalid. Once the frame's thread is resumed,
     * the stack frame is no longer valid.
     * @see ThreadReference#frame
     */
    ThreadReference thread();
}
