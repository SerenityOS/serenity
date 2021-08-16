/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import jdk.internal.misc.Unsafe;
import jdk.jfr.Event;
import jdk.test.lib.jfr.StreamingUtils;
import jdk.test.lib.process.ProcessTools;

import com.sun.tools.attach.VirtualMachine;

/**
 * Class that emits a NUMBER_OF_EVENTS and then awaits crash or exit
 *
 * Requires jdk.attach module.
 *
 */
public final class TestProcess implements AutoCloseable {

    private static class TestEvent extends Event {
    }

    public final static int NUMBER_OF_EVENTS = 10;

    private final Process process;
    private final Path path;

    public TestProcess(String name) throws IOException {
        this.path = Paths.get("action-" + System.currentTimeMillis()).toAbsolutePath();
        String[] args = {
                "--add-exports",
                "java.base/jdk.internal.misc=ALL-UNNAMED",
                "-XX:StartFlightRecording:settings=none",
                TestProcess.class.getName(), path.toString()
            };
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(args);
        process = ProcessTools.startProcess(name, pb);
    }

    public static void main(String... args) throws Exception {
        for (int i = 0; i < NUMBER_OF_EVENTS; i++) {
            TestEvent e = new TestEvent();
            e.commit();
        }

        Path path = Paths.get(args[0]);
        while (true) {
            try {
                String action = Files.readString(path);
                if ("crash".equals(action)) {
                    System.out.println("About to crash...");
                    Unsafe.getUnsafe().putInt(0L, 0);
                }
                if ("exit".equals(action)) {
                    System.out.println("About to exit...");
                    System.exit(0);
                }
            } catch (Exception ioe) {
                // Ignore
            }
            takeNap();
        }
    }

    public Path getRepository() throws Exception {
        return StreamingUtils.getJfrRepository(process);
    }

    private static void takeNap() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ie) {
            // ignore
        }
    }

    public void crash() {
        try {
            Files.writeString(path, "crash");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void exit() {
        try {
            Files.writeString(path, "exit");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public long pid() {
        return process.pid();
    }

    @Override
    public void close() throws Exception {
        try  {
            if (path != null)  {
                Files.delete(path);
            }
        } catch(NoSuchFileException nfe)  {
            // ignore
        }
    }

    public void awaitDeath() {
        while (process.isAlive())  {
            takeNap();
        }
    }
}
