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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.VmIdentifier;
import sun.jvmstat.monitor.event.HostEvent;
import sun.jvmstat.monitor.event.HostListener;
import sun.jvmstat.monitor.event.VmStatusChangeEvent;

/*

 Test starts ten Java processes, each with a unique id.

 Each process creates a file named after the id and then it waits for
 the test to remove the file, at which the Java process exits.

 The processes are monitored by the test to make sure notifications
 are sent when they are started/terminated.

 To avoid Java processes being left behind, in case of an unexpected
 failure, shutdown hooks are installed that remove files when the test
 exits. If files are not removed, i.e. due to a JVM crash, the Java
 processes will exit themselves after 1000 s.

*/

/*
 * @test
 * @bug 4990825
 * @summary attach to external but local JVM processes
 * @library /test/lib
 * @modules java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 *          jdk.internal.jvmstat/sun.jvmstat.monitor.event
 * @run main/othervm MonitorVmStartTerminate
 */
public final class MonitorVmStartTerminate {

    private static final int PROCESS_COUNT = 10;

    public static void main(String... args) throws Exception {

        MonitoredHost host = MonitoredHost.getMonitoredHost("localhost");
        host.setInterval(1); // 1 ms

        String id = UUID.randomUUID().toString();

        List<JavaProcess> javaProcesses = new ArrayList<>();
        for (int i = 0; i < PROCESS_COUNT; i++) {
            javaProcesses.add(new JavaProcess(id + "_" + i));
        }

        Listener listener = new Listener(host, javaProcesses);
        host.addHostListener(listener);
        for (JavaProcess javaProcess : javaProcesses) {
            javaProcess.start();
        }

        // Wait for all processes to start before terminating
        // them, so pids are not reused within a poll interval.
        System.out.println("Waiting for all processes to get started notification");
        listener.started.acquire(PROCESS_COUNT);

        for (JavaProcess javaProcess : javaProcesses) {
            javaProcess.terminate();
        }
        System.out.println("Waiting for all processes to get terminated notification");
        listener.terminated.acquire(PROCESS_COUNT);

        host.removeHostListener(listener);
    }

    private static final class Listener implements HostListener {
        private final Semaphore started = new Semaphore(0);
        private final Semaphore terminated = new Semaphore(0);
        private final MonitoredHost host;
        private final List<JavaProcess> processes;

        public Listener(MonitoredHost host, List<JavaProcess> processes) {
            this.host = host;
            this.processes = processes;
            printStatus();
        }

        @Override
        @SuppressWarnings("unchecked")
        public void vmStatusChanged(VmStatusChangeEvent event) {
            releaseStarted(event.getStarted());
            releaseTerminated(event.getTerminated());
            printStatus();
        }

        private void printStatus() {
            System.out.printf("started=%d, terminated=%d\n",
                    started.availablePermits(), terminated.availablePermits());
        }

        @Override
        public void disconnected(HostEvent arg0) {
            // ignore
        }

        private void releaseStarted(Set<Integer> ids) {
            System.out.println("realeaseStarted(" + ids + ")");
            for (Integer id : ids) {
                releaseStarted(id);
            }
        }

        private void releaseStarted(Integer id) {
            for (JavaProcess jp : processes) {
                if (hasMainArgs(id, jp.getMainArgsIdentifier())) {
                    // store id for terminated identification
                    jp.setId(id);
                    System.out.println("RELEASED (id=" + jp.getId() + ", args=" + jp.getMainArgsIdentifier() + ")");
                    started.release();
                    return;
                }
            }
        }

        private void releaseTerminated(Set<Integer> ids) {
            System.out.println("releaseTerminated(" + ids + ")");
            for (Integer id : ids) {
                releaseTerminated(id);
            }
        }

        private void releaseTerminated(Integer id) {
            for (JavaProcess jp : processes) {
                if (id.equals(jp.getId())) {
                    System.out.println("RELEASED (id=" + jp.getId() + ", args=" + jp.getMainArgsIdentifier() + ")");
                    terminated.release();
                    return;
                }
            }
        }

        private boolean hasMainArgs(Integer id, String args) {
            try {
                VmIdentifier vmid = new VmIdentifier("//" + id.intValue());
                MonitoredVm target = host.getMonitoredVm(vmid);
                String monitoredArgs = MonitoredVmUtil.mainArgs(target);
                if (monitoredArgs != null && monitoredArgs.contains(args)) {
                    return true;
                }
            } catch (URISyntaxException | MonitorException e) {
                // ok. process probably not running
            }
            return false;
        }
    }

    public final static class JavaProcess {

        private static final class ShutdownHook extends Thread {
            private final JavaProcess javaProcess;

            public ShutdownHook(JavaProcess javaProcess) {
                this.javaProcess = javaProcess;
            }

            public void run() {
                javaProcess.terminate();
            }
        }

        public static void main(String[] args) throws InterruptedException {
            try {
                Path path = Paths.get(args[0]);
                createFile(path);
                waitForRemoval(path);
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        private static void createFile(Path path) throws IOException {
            Files.write(path, new byte[0], StandardOpenOption.CREATE);
        }

        private static void waitForRemoval(Path path) {
            String timeoutFactorText = System.getProperty("test.timeout.factor", "1.0");
            double timeoutFactor = Double.parseDouble(timeoutFactorText);
            long timeoutNanos = 1000_000_000L*(long)(1000*timeoutFactor);
            long start = System.nanoTime();
            while (true) {
                long now = System.nanoTime();
                long waited = now - start;
                System.out.println("Waiting for " + path + " to be removed, " + waited + " ns");
                if (!Files.exists(path)) {
                    return;
                }
                if (waited > timeoutNanos) {
                    System.out.println("Start: " + start);
                    System.out.println("Now: " + now);
                    System.out.println("Process timed out after " + waited + " ns. Abort.");
                    System.exit(1);
                }
                takeNap();
            }
        }

        private static void takeNap() {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        private final String mainArgsIdentifier;
        private final ShutdownHook shutdownHook;
        private volatile Integer id;

        public JavaProcess(String mainArgsIdentifier) {
            this.mainArgsIdentifier = mainArgsIdentifier;
            this.shutdownHook = new ShutdownHook(this);
        }

        /**
         * Starts a Java process asynchronously.
         *
         * The process runs until {@link #stop()} is called. If test exits
         * unexpectedly the process will be cleaned up by a shutdown hook.
         *
         * @throws Exception
         */
        public void start() throws Exception {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            System.out.println("Starting " + getMainArgsIdentifier());

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        executeJava();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            };
            new Thread(r).start();
        }

        public void terminate() {
            try {
                System.out.println("Terminating " + mainArgsIdentifier);
                // File must be created before proceeding,
                // otherwise Java process may loop forever
                // waiting for file to be removed.
                Path path = Paths.get(mainArgsIdentifier);
                while (!Files.exists(path)) {
                    takeNap();
                }
                Files.delete(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }

        private void executeJava() throws Throwable {
            String className = JavaProcess.class.getName();
            String classPath = System.getProperty("test.classes");
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-Dtest.timeout.factor=" + System.getProperty("test.timeout.factor", "1.0"),
                "-cp", classPath, className, mainArgsIdentifier);
            OutputAnalyzer ob = ProcessTools.executeProcess(pb);
            System.out.println("Java Process " + getMainArgsIdentifier() + " stderr:"
                    + ob.getStderr());
            System.err.println("Java Process " + getMainArgsIdentifier() + " stdout:"
                    + ob.getStdout());
        }

        public String getMainArgsIdentifier() {
            return mainArgsIdentifier;
        }
    }
}
