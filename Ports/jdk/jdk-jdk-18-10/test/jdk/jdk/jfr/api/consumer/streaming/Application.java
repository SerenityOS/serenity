/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jfr.api.consumer.streaming;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class Application {
    @Name("Message")
    static class Message extends Event {
        String content;
    }
    private static int counter;
    private final Path lockFile;
    private final Path repository;
    private final String message;
    private final ScheduledExecutorService monitor;
    private final int id;
    private final Thread thread;
    private Process process;
    private OutputAnalyzer analyzer;
    private int statusCheck;

    public Application(Path repository) {
        this(repository, "Whatever");
    }

    public Application(Path repository, String message) {
        counter++;
        this.id = counter;
        String lockFilename = counter + "_";
        lockFilename += ProcessHandle.current() + "_";
        lockFilename += System.currentTimeMillis() + ".lock";
        this.lockFile  = Path.of(".").resolve(lockFilename);
        this.repository = repository;
        this.message = message;
        // For debugging
        this.thread = Thread.currentThread();
        this.monitor = Executors.newScheduledThreadPool(1);
    }

    public static void main(String... args) throws InterruptedException {
        Path p = Path.of(args[0]);
        String content = args[1];
        while (true) {
            Message event = new Message();
            event.content = content;
            event.commit();
            if (!Files.exists(p)) {
                return;
            }
            takeNap();
        }
    }

    private static void takeNap() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public void start() throws IOException {
        String[] args = new String[5];
        args[0] = "-XX:StartFlightRecording";
        args[1] = "-XX:FlightRecorderOptions:repository=" + repository;
        args[2] = Application.class.getName();
        args[3] = lockFile.toString();
        args[4] = message;
        ProcessBuilder pb = ProcessTools.createTestJvm(args);
        touch(lockFile);
        process = pb.start();
        // For debugging
        analyzer = new OutputAnalyzer(process);
        monitor.scheduleWithFixedDelay(() -> checkStatus(), 0, 1, TimeUnit.SECONDS);
        if (!process.isAlive()) {
            throw new IOException("Test application not alive after start");
        }
        System.out.println("App started");
    }

    public void stop() throws IOException {
        Files.delete(lockFile);
        monitor.shutdown();
    }

    private static void touch(Path p) throws IOException, FileNotFoundException {
        try (RandomAccessFile raf = new RandomAccessFile(p.toFile(), "rw")) {
            raf.write(4711);
        }
    }

    public void awaitRecording() throws IOException {
        String pid = Long.toString(process.pid());
        long t = System.currentTimeMillis();
        while (true) {
            if (!process.isAlive()) {
                String std = new String(process.getInputStream().readAllBytes());
                System.out.println("========= Application: " + id + " Process std out ==========");
                System.out.println(std);
                System.out.println("====================================================");
                String err = new String(process.getInputStream().readAllBytes());
                System.out.println("========= Application: " + id + " Process std err ==========");
                System.out.println(err);
                System.out.println("====================================================");
                throw new IOException("Application process not alive!");
            }
            try {
                for (VirtualMachineDescriptor vmd: VirtualMachine.list()) {
                    if (vmd.id().equals(pid)) {
                        VirtualMachine vm = VirtualMachine.attach(vmd);
                        Object repo = vm.getSystemProperties().get("jdk.jfr.repository");
                        vm.detach();
                        if (repo != null) {
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Await: " + e.getMessage());
                System.out.println("Process alive: "  + process.isAlive());
                System.out.println("PID: " + pid);
            }
            takeNap();
            if (System.currentTimeMillis() -t > 10_000) {
                checkStatus();
                t = System.currentTimeMillis();
            }
            System.out.println("Awaiting recording");
        }
     }

    // For debugging purposes
    public void checkStatus() {
        System.out.println("Application " + id + " status: ");
        try {
            boolean hasRepository = Files.exists(repository);
            boolean hasLockFile = Files.exists(lockFile);
            boolean isAlive = process == null ? false : process.isAlive();
            System.out.println("Has repository: " + hasRepository);
            System.out.println("Has lock file: " + hasLockFile);
            System.out.println("Is alive: " + isAlive);
            if (hasRepository) {
                System.out.println(directoryToText(new StringBuilder(), "", repository));
            }
            System.out.println();
            statusCheck++;
            if (statusCheck % 10 == 9) {
                System.out.println("Stack trace for thread that created the application:");
                for (StackTraceElement se : thread.getStackTrace()) {
                    System.out.println(se);
                }
                if (process != null && !process.isAlive()) {
                    System.out.println(analyzer.getStdout());
                    System.out.println(analyzer.getStderr());
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static StringBuilder directoryToText(StringBuilder output, String indent, Path directory) throws IOException {
        output.append(indent)
          .append("*- ")
          .append(directory.getFileName().toString())
          .append(System.lineSeparator());
        for (Path path : Files.list(directory).toList()) {
            if (Files.isDirectory(path)) {
                directoryToText(output, indent + " ", path);
            } else {
                fileToText(output, indent + " ", path);
            }
        }
        return output;
    }

    private static void fileToText(StringBuilder output, String indent, Path file) throws IOException {
        output.append(indent)
          .append("|- ")
          .append(file.getFileName().toString())
          .append(System.lineSeparator());
    }
}