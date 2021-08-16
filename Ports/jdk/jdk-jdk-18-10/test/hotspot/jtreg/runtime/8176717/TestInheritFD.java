/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.Long.parseLong;
import static java.lang.System.getProperty;
import static java.nio.file.Files.readAllBytes;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static jdk.test.lib.process.ProcessTools.createJavaProcessBuilder;
import static jdk.test.lib.Platform.isWindows;
import jdk.test.lib.Utils;
import jdk.test.lib.Platform;
import jtreg.SkippedException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/*
 * @test TestInheritFD
 * @bug 8176717 8176809 8222500
 * @summary a new process should not inherit open file descriptors
 * @comment On Aix lsof requires root privileges.
 * @requires os.family != "aix"
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver TestInheritFD
 */

/**
 * Test that HotSpot does not leak logging file descriptors.
 *
 * This test is performed in three steps. The first VM starts a second VM with
 * gc logging enabled. The second VM starts a third VM and redirects the third
 * VMs output to the first VM, it then exits and hopefully closes its log file.
 *
 * The third VM waits for the second to exit and close its log file. After that,
 * the third VM tries to rename the log file of the second VM. If it succeeds in
 * doing so it means that the third VM did not inherit the open log file
 * (windows can not rename opened files easily)
 *
 * The third VM communicates the success to rename the file by printing "CLOSED
 * FD". The first VM checks that the string was printed by the third VM.
 *
 * On unix like systems "lsof" is used.
 */

public class TestInheritFD {

    public static final String LEAKS_FD = "VM RESULT => LEAKS FD";
    public static final String RETAINS_FD = "VM RESULT => RETAINS FD";
    public static final String EXIT = "VM RESULT => VM EXIT";
    public static final String LOG_SUFFIX = ".strangelogsuffixthatcanbecheckedfor";

    // first VM
    public static void main(String[] args) throws Exception {
        String logPath = Utils.createTempFile("logging", LOG_SUFFIX).toFile().getName();
        File commFile = Utils.createTempFile("communication", ".txt").toFile();

        if (!isWindows() && !lsofCommand().isPresent()) {
            throw new SkippedException("Could not find lsof like command");
        }

        ProcessBuilder pb = createJavaProcessBuilder(
            "-Xlog:gc:\"" + logPath + "\"",
            "-Dtest.jdk=" + getProperty("test.jdk"),
            VMStartedWithLogging.class.getName(),
            logPath);

        pb.redirectOutput(commFile); // use temp file to communicate between processes
        pb.start();

        String out = "";
        do {
            out = new String(readAllBytes(commFile.toPath()));
            Thread.sleep(100);
            System.out.println("SLEEP 100 millis");
        } while (!out.contains(EXIT));

        System.out.println(out);
        if (out.contains(RETAINS_FD)) {
            System.out.println("Log file was not inherited by third VM");
        } else {
            throw new RuntimeException("could not match: " + RETAINS_FD);
        }
    }

    static class VMStartedWithLogging {
        // second VM
        public static void main(String[] args) throws IOException, InterruptedException {
            ProcessBuilder pb = createJavaProcessBuilder(
                "-Dtest.jdk=" + getProperty("test.jdk"),
                VMShouldNotInheritFileDescriptors.class.getName(),
                args[0],
                "" + ProcessHandle.current().pid());
            pb.inheritIO(); // in future, redirect information from third VM to first VM
            pb.start();

            if (!isWindows()) {
                System.out.println("(Second VM) Open file descriptors:\n" + outputContainingFilenames().stream().collect(joining("\n")));
            }
        }
    }

    static class VMShouldNotInheritFileDescriptors {
        // third VM
        public static void main(String[] args) throws InterruptedException {
            try {
                File logFile = new File(args[0]);
                long parentPid = parseLong(args[1]);
                fakeLeakyJVM(false); // for debugging of test case

                if (isWindows()) {
                    windows(logFile, parentPid);
                } else {
                    Collection<String> output = outputContainingFilenames();
                    System.out.println("(Third VM) Open file descriptors:\n" + output.stream().collect(joining("\n")));
                    System.out.println(findOpenLogFile(output) ? LEAKS_FD : RETAINS_FD);
                }
            } catch (Exception e) {
                System.out.println(e.toString());
            } finally {
                System.out.println(EXIT);
            }
        }
    }

    // for debugging of test case
    @SuppressWarnings("resource")
    static void fakeLeakyJVM(boolean fake) {
        if (fake) {
            try {
                new FileOutputStream("fakeLeakyJVM" + LOG_SUFFIX, false);
            } catch (FileNotFoundException e) {
            }
        }
    }

    static Stream<String> run(String... args){
        try {
            return new BufferedReader(new InputStreamReader(new ProcessBuilder(args).start().getInputStream())).lines();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static Optional<String[]> lsofCommandCache = stream(new String[][]{
            {"/usr/bin/lsof", "-p"},
            {"/usr/sbin/lsof", "-p"},
            {"/bin/lsof", "-p"},
            {"/sbin/lsof", "-p"},
            {"/usr/local/bin/lsof", "-p"}})
        .filter(args -> new File(args[0]).exists())
        .findFirst();

    static Optional<String[]> lsofCommand() {
        return lsofCommandCache;
    }

    static Collection<String> outputContainingFilenames() {
        long pid = ProcessHandle.current().pid();

        String[] command = lsofCommand().orElseThrow(() -> new RuntimeException("lsof like command not found"));
        System.out.println("using command: " + command[0] + " " + command[1]);
        return run(command[0], command[1], "" + pid).collect(toList());
    }

    static boolean findOpenLogFile(Collection<String> fileNames) {
        String pid = Long.toString(ProcessHandle.current().pid());
        String[] command = lsofCommand().orElseThrow(() ->
                new RuntimeException("lsof like command not found"));
        String lsof = command[0];
        boolean isBusybox = Platform.isBusybox(lsof);
        return fileNames.stream()
            // lsof from busybox does not support "-p" option
            .filter(fileName -> !isBusybox || fileName.contains(pid))
            .filter(fileName -> fileName.contains(LOG_SUFFIX))
            .findAny()
            .isPresent();
    }

    static void windows(File f, long parentPid) throws InterruptedException {
        System.out.println("waiting for pid: " + parentPid);
        ProcessHandle.of(parentPid).ifPresent(handle -> handle.onExit().join());
        System.out.println("trying to rename file to the same name: " + f);
        System.out.println(f.renameTo(f) ? RETAINS_FD : LEAKS_FD); // this parts communicates a closed file descriptor by printing "VM RESULT => RETAINS FD"
    }
}

