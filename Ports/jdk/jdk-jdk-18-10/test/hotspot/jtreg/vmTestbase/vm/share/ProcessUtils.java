/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package vm.share;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;

import nsk.share.TestBug;

import com.sun.management.HotSpotDiagnosticMXBean;

public final class ProcessUtils {
    static {
        System.loadLibrary("ProcessUtils");
    }

    private ProcessUtils() {}

    /**
     * Send Ctrl-\ to java process and Ctrl-Break on Windows.
     * This will usually trigger stack dump for all threads and
     * may trigger heap dump.
     *
     * @return true if it was successful
     */
    public static native boolean sendCtrlBreak();

    /**
     * Send any signal to java process on Unix. It currently does nothing on Windows.
     *
     * @return true if it was successful
     */
    public static native boolean sendSignal(int signalNum);

    /**
     * Force java process to dump core.
     *
     * This is done by sending SIGSEGV on unix systems.
     *
     * @return true if it was successful, false if not (for example on Windows)
     */
    public static native boolean dumpCore();

    /**
     * Get PID of java process.
     *
     * @return PID
     */
    public static native int getPid();

    public static int getPid(Process process) {
        Throwable exception;
        try {
            Field pidField = process.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            return ((Integer) pidField.get(process)).intValue();
        } catch (NoSuchFieldException e) {
            exception = e;
        } catch (IllegalAccessException e) {
            exception = e;
        }
        // Try to get Windows handle
        try {
            Field handleField = process.getClass().getDeclaredField("handle");
            handleField.setAccessible(true);
            long handle = ((Long) handleField.get(process)).longValue();
            return getWindowsPid(handle);
        } catch (NoSuchFieldException e) {
            exception = e;
        } catch (IllegalAccessException e) {
            exception = e;
        }
        throw new TestBug("Unable to determine pid from process class " + process.getClass(), exception);
    }

    private static native int getWindowsPid(long handle);

    @SuppressWarnings("restriction")
    public static void dumpHeapWithHotspotDiagnosticMXBean(String fileName) throws IOException {
        System.err.println("Dumping heap to " + fileName);

        File f = new File(fileName);
        if (f.exists())
            f.delete();

        HotSpotDiagnosticMXBean b = ManagementFactory.getPlatformMXBeans(
                com.sun.management.HotSpotDiagnosticMXBean.class).get(0);
        b.dumpHeap(fileName, false);
    }
}
