/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr;

/**
 * Indicates a state in the life cycle of a recording.
 *
 * @since 9
 */
public enum RecordingState {

    /**
     * The initial state when a {@code Recording} is created.
     */
    NEW,

    /**
     * The recording is scheduled to start with a start time in the future.
     * <p>
     * An invocation of the {@link Recording#start()} method will transition the
     * recording to the {@code RUNNING} state.
     */
    DELAYED,

    /**
     * The recording is recording data and an invocation of the {@link Recording#stop()}
     * method will transition the recording to the {@code STOPPED} state.
     */
    RUNNING,

    /**
     * The recording is stopped and is holding recorded data that can be dumped to
     * disk.
     * <p>
     * An invocation of the {@link Recording#close()} method will release the
     * data and transition the recording to the {@code CLOSED} state.
     */
    STOPPED,

    /**
     * The recording is closed and all resources that are associated with the
     * recording are released.
     * <p>
     * Nothing that can be done with a recording from this point, and it's
     * no longer retrievable from the {@link FlightRecorder#getRecordings()} method.
     */
    CLOSED;
}
