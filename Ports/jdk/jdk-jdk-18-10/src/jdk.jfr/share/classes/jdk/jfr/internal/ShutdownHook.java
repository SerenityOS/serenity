/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal;

import java.io.IOException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import jdk.jfr.RecordingState;

/**
 * Class responsible for dumping recordings on exit
 *
 */
final class ShutdownHook implements Runnable {
    private final PlatformRecorder recorder;
    Object tlabDummyObject;

    ShutdownHook(PlatformRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void run() {
        // this allocation is done in order to fetch a new TLAB before
        // starting any "real" operations. In low memory situations,
        // we would like to take an OOM as early as possible.
        tlabDummyObject = new Object();
        recorder.setInShutDown();
        for (PlatformRecording recording : recorder.getRecordings()) {
            if (recording.getDumpOnExit() && recording.getState() == RecordingState.RUNNING) {
                dump(recording);
            }
        }
        recorder.destroy();
    }

    private void dump(PlatformRecording recording) {
        try {
            WriteableUserPath dest = recording.getDestination();
            if (dest == null) {
                dest = makeDumpOnExitPath(recording);
                recording.setDestination(dest);
            }
            if (dest != null) {
                recording.stop("Dump on exit");
            }
        } catch (Exception e) {
            if (Logger.shouldLog(LogTag.JFR, LogLevel.DEBUG)) {
                Logger.log(LogTag.JFR, LogLevel.DEBUG, "Could not dump recording " + recording.getName() + " on exit.");
            }
        }
    }

    @SuppressWarnings("removal")
    private WriteableUserPath makeDumpOnExitPath(PlatformRecording recording) {
        try {
            String name = Utils.makeFilename(recording.getRecording());
            AccessControlContext acc = recording.getNoDestinationDumpOnExitAccessControlContext();
            return AccessController.doPrivileged(new PrivilegedExceptionAction<WriteableUserPath>() {
                @Override
                public WriteableUserPath run() throws Exception {
                    return new WriteableUserPath(recording.getDumpOnExitDirectory().toPath().resolve(name));
                }
            }, acc);
        } catch (PrivilegedActionException e) {
            Throwable t = e.getCause();
            if (t instanceof SecurityException) {
                Logger.log(LogTag.JFR, LogLevel.WARN, "Not allowed to create dump path for recording " + recording.getId() + " on exit.");
            }
            if (t instanceof IOException) {
                Logger.log(LogTag.JFR, LogLevel.WARN, "Could not dump " + recording.getId() + " on exit.");
            }
            return null;
        }
    }

    static final class ExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            JVM.getJVM().uncaughtException(t, e);
        }
    }
}
