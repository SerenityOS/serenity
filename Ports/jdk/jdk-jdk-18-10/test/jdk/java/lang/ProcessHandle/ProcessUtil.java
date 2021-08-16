/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.management.OperatingSystemMXBean;
import jdk.test.lib.Platform;

/**
 * Useful utilities for testing Process and ProcessHandle.
 */
public abstract class ProcessUtil {
    /**
     * Constructor
     */
    public ProcessUtil() {}

    /**
     * Returns the list of direct children.
     * WIndows conhost.exe children are filtered out.
     * @param ph the Process to get children of
     * @return a list of child ProcessHandles
     */
    public static List<ProcessHandle> getChildren(ProcessHandle ph) {
        return ph.children()
                .filter(ProcessUtil::isNotWindowsConsole)
                .collect(Collectors.toList());
    }

    /**
     * Returns the list of all direct and indirect children.
     * WIndows conhost.exe children are filtered out.
     * @param ph the Process to get children of
     * @return a list of child ProcessHandles
     */
    public static List<ProcessHandle> getDescendants(ProcessHandle ph) {
        return ph.descendants()
                .filter(ProcessUtil::isNotWindowsConsole)
                .collect(Collectors.toList());
    }

    /**
     * Waits for and returns the direct expected Children of a ProcessHandle.
     * For Windows, the conhost.exe children are filtered out.
     *
     * @param ph the process to get the children of
     * @param nchildren the minimum number of children to expect
     * @return a list of ProcessHandles of the children.
     */
    public static List<ProcessHandle> waitForChildren(ProcessHandle ph, long nchildren) {
        List<ProcessHandle> subprocesses = null;
        long count = 0;
        do {
            if (subprocesses != null) {
                // Only wait if this is not the first time looking
                try {
                    Thread.sleep(500L);     // It will happen but don't burn the cpu
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
            subprocesses = getChildren(ph);
            count = subprocesses.size();
            System.out.printf(" waiting for subprocesses of %s to start," +
                    " expected: %d, current: %d%n", ph, nchildren, count);
        } while (count < nchildren);
        return subprocesses;
    }

    /**
     * Waits for and returns all expected Children of a ProcessHandle.
     * For Windows, the conhost.exe children are filtered out.
     *
     * @param ph the process to get the children of
     * @param nchildren the minimum number of children to expect
     * @return a list of ProcessHandles of the children.
     */
    public static List<ProcessHandle> waitForAllChildren(ProcessHandle ph, long nchildren) {
        List<ProcessHandle> subprocesses = null;
        long count = 0;
        do {
            if (subprocesses != null) {
                // Only wait if this is not the first time looking
                try {
                    Thread.sleep(500L);     // It will happen but don't burn the cpu
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
            subprocesses = getDescendants(ph);
            count = subprocesses.size();
            System.out.printf(" waiting for subprocesses of %s to start," +
                    " expected: %d, current: %d%n", ph, nchildren, count);
        } while (count < nchildren);
        return subprocesses;
    }

    /**
     * Destroy all children of the ProcessHandle.
     * (Except the conhost.exe on Windows)
     *
     * @param p a ProcessHandle
     * @return the ProcessHandle
     */
    public static ProcessHandle destroyProcessTree(ProcessHandle p) {
        Stream<ProcessHandle> children = p.descendants().filter(ProcessUtil::isNotWindowsConsole);
        children.forEach(ph -> {
            System.out.printf("destroyProcessTree destroyForcibly%n");
            printProcess(ph);
            ph.destroyForcibly();
        });
        return p;
    }

    /**
     * The OSMXBean for this process.
     */
    public static final OperatingSystemMXBean osMbean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    /**
     * Return the CPU time of the current process according to the OperatingSystemMXBean.
     *
     * @return the CPU time of the current process
     */
    public static Duration MXBeanCpuTime() {
        return Duration.ofNanos(osMbean.getProcessCpuTime());
    }

    /**
     * Return true if the ProcessHandle is a Windows i586 conhost.exe process.
     *
     * @param p the processHandle of the Process
     * @return Return true if the ProcessHandle is for a Windows i586 conhost.exe process
     */
    static boolean isWindowsConsole(ProcessHandle p) {
        return Platform.isWindows() && p.info().command().orElse("").endsWith("C:\\Windows\\System32\\conhost.exe");
    }

    /**
     * Return true if the ProcessHandle is NOT  a Windows i586 conhost.exe process.
     *
     * @param p the processHandle of the Process
     * @return Return true if the ProcessHandle is NOT for a Windows i586 conhost.exe process
     */
    static boolean isNotWindowsConsole(ProcessHandle p) {
        return !isWindowsConsole(p);
    }

    /**
     * Print a formatted string to System.out.
     * @param format the format
     * @param args the argument array
     */
    static void printf(String format, Object... args) {
        String s = String.format(format, args);
        System.out.print(s);
    }

    /**
     * Print information about a process.
     * Prints the pid, if it is alive, and information about the process.
     * @param ph the processHandle at the top
     */
    static void printProcess(ProcessHandle ph) {
        printProcess(ph, "");
    }

    /**
     * Print information about a process.
     * Prints the pid, if it is alive, and information about the process.
     * @param ph the processHandle at the top
     * @param prefix the String to prefix the output with
     */
    static void printProcess(ProcessHandle ph, String prefix) {
        printf("%spid %s, alive: %s; parent: %s, %s%n", prefix,
                ph.pid(), ph.isAlive(), ph.parent(), ph.info());
    }

    /**
     * Print the process hierarchy as visible via ProcessHandle.
     * Prints the pid, if it is alive, and information about the process.
     * @param ph the processHandle at the top
     * @param prefix the String to prefix the output with
     */
    static void printDeep(ProcessHandle ph, String prefix) {
        printProcess(ph, prefix);
        ph.children().forEach(p -> printDeep(p, prefix + "   "));
    }

    /**
     * Use the native command to list the active processes.
     */
    static void logTaskList() {
        String[] windowsArglist = {"tasklist.exe", "/v"};
        String[] unixArglist = {"ps", "-ef"};

        String[] argList = null;
        if (Platform.isWindows()) {
            argList = windowsArglist;
        } else if (Platform.isLinux() || Platform.isOSX()) {
            argList = unixArglist;
        } else {
            return;
        }

        ProcessBuilder pb = new ProcessBuilder(argList);
        pb.inheritIO();
        try {
            Process proc = pb.start();
            proc.waitFor();
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
