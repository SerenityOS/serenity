/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.management.OperatingSystemMXBean;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.PrintWriter;
import java.lang.InterruptedException;
import java.lang.Override;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.function.Consumer;


/**
 * Command driven subprocess with useful child functions.
 */
public class JavaChild extends Process {

private static volatile int commandSeq = 0;         // Command sequence number
    private static final ProcessHandle self = ProcessHandle.current();
    private static int finalStatus = 0;
    private static final List<JavaChild> children = new ArrayList<>();
    private static final Set<JavaChild> completedChildren =
            Collections.synchronizedSet(new HashSet<>());

    private final Process delegate;
    private final PrintWriter inputWriter;
    private final BufferedReader outputReader;


    /**
     * Create a JavaChild control instance that delegates to the spawned process.
     * {@link #sendAction} is used to send commands via the processes stdin.
     * {@link #forEachOutputLine} can be used to process output from the child
     * @param delegate the process to delegate and send commands to and get responses from
     */
    private JavaChild(ProcessBuilder pb) throws IOException {
        allArgs = pb.command();
        delegate = pb.start();
        // Initialize PrintWriter with autoflush (on println)
        inputWriter = new PrintWriter(delegate.getOutputStream(), true);
        outputReader = new BufferedReader(new InputStreamReader(delegate.getInputStream()));
    }

    @Override
    public void destroy() {
        delegate.destroy();
    }

    @Override
    public int exitValue() {
        return delegate.exitValue();
    }

    @Override
    public int waitFor() throws InterruptedException {
        return delegate.waitFor();
    }

    @Override
    public OutputStream getOutputStream() {
        return delegate.getOutputStream();
    }

    @Override
    public InputStream getInputStream() {
        return delegate.getInputStream();
    }

    @Override
    public InputStream getErrorStream() {
        return delegate.getErrorStream();
    }

    @Override
    public ProcessHandle toHandle() {
        return delegate.toHandle();
    }

    @Override
    public CompletableFuture<Process> onExit() {
        return delegate.onExit();
    }
    @Override
    public String toString() {
        return "delegate: " + delegate.toString();
    }

    public List<String> getArgs() {
        return allArgs;
    }

    public CompletableFuture<JavaChild> onJavaChildExit() {
        return onExit().thenApply(ph -> this);
    }

    /**
     * Send an action and arguments to the child via stdin.
     * @param action the action
     * @param args additional arguments
     * @throws IOException if something goes wrong writing to the child
     */
    void sendAction(String action, Object... args) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(action);
        for (Object arg :args) {
            sb.append(" ");
            sb.append(arg);
        }
        String cmd = sb.toString();
        synchronized (this) {
            inputWriter.println(cmd);
        }
    }

    public BufferedReader outputReader() {
        return outputReader;
    }

    /**
     * Asynchronously evaluate each line of output received back from the child process.
     * @param consumer a Consumer of each line read from the child
     * @return a CompletableFuture that is completed when the child closes System.out.
     */
    CompletableFuture<String> forEachOutputLine(Consumer<String> consumer) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        String name = "OutputLineReader-" + pid();
        Thread t = new Thread(() -> {
            try (BufferedReader reader = outputReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    consumer.accept(line);
                }
            } catch (IOException | RuntimeException ex) {
                consumer.accept("IOE (" + pid() + "):" + ex.getMessage());
                future.completeExceptionally(ex);
            }
            future.complete("success");
        }, name);
        t.start();
        return future;
    }

    /**
     * Spawn a JavaChild with the provided arguments.
     * Commands can be send to the child with {@link #sendAction}.
     * Output lines from the child can be processed with {@link #forEachOutputLine}.
     * System.err is set to inherit and is the unstructured async logging
     * output for all subprocesses.
     * @param args the command line arguments to JavaChild
     * @return the JavaChild that was started
     * @throws IOException thrown by ProcessBuilder.start
     */
    static JavaChild spawnJavaChild(Object... args) throws IOException {
        String[] stringArgs = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            stringArgs[i] = args[i].toString();
        }
        ProcessBuilder pb = build(stringArgs);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        return new JavaChild(pb);
    }

    /**
     * Spawn a JavaChild with the provided arguments.
     * Sets the process to inherit the I/O channels.
     * @param args the command line arguments to JavaChild
     * @return the Process that was started
     * @throws IOException thrown by ProcessBuilder.start
     */
    static Process spawn(String... args) throws IOException {
        ProcessBuilder pb = build(args);
        pb.inheritIO();
        return pb.start();
    }

    /**
     * Return a ProcessBuilder with the javaChildArgs and
     * any additional supplied args.
     *
     * @param args the command line arguments to JavaChild
     * @return the ProcessBuilder
     */
    static ProcessBuilder build(String ... args) {
        ProcessBuilder pb = new ProcessBuilder();
        List<String> list = new ArrayList<>(javaChildArgs);
        for (String arg : args)
            list.add(arg);
        pb.command(list);
        return pb;
    }

    static final String javaHome = (System.getProperty("test.jdk") != null)
            ? System.getProperty("test.jdk")
            : System.getProperty("java.home");

    static final String javaExe =
            javaHome + File.separator + "bin" + File.separator + "java";

    static final String classpath =
            System.getProperty("java.class.path");

    static final List<String> javaChildArgs =
            Arrays.asList(javaExe,
                    "-XX:+DisplayVMOutputToStderr",
                    "-Dtest.jdk=" + javaHome,
                    "-classpath", absolutifyPath(classpath),
                    "JavaChild");

    // Will hold the complete list of arguments which was given to Processbuilder.command()
    private List<String> allArgs;

    private static String absolutifyPath(String path) {
        StringBuilder sb = new StringBuilder();
        for (String file : path.split(File.pathSeparator)) {
            if (sb.length() != 0)
                sb.append(File.pathSeparator);
            sb.append(new File(file).getAbsolutePath());
        }
        return sb.toString();
    }

    /**
     * Main program that interprets commands from the command line args or stdin.
     * Each command produces output to stdout confirming the command and
     * providing results.
     * System.err is used for unstructured information.
     * @param args an array of strings to be interpreted as commands;
     *             each command uses additional arguments as needed
     */
    public static void main(String[] args) {
        System.out.printf("args: %s %s%n", ProcessHandle.current(), Arrays.toString(args));
        interpretCommands(args);
        System.exit(finalStatus);
    }

    /**
     * Interpret an array of strings as a command line.
     * @param args an array of strings to be interpreted as commands;
     *             each command uses additional arguments as needed
     */
    private static void interpretCommands(String[] args) {
        try {
            int nextArg = 0;
            while (nextArg < args.length) {
                String action = args[nextArg++];
                switch (action) {
                    case "help":
                        sendResult(action, "");
                        help();
                        break;
                    case "sleep":
                        int millis = Integer.valueOf(args[nextArg++]);
                        Thread.sleep(millis);
                        sendResult(action, Integer.toString(millis));
                        break;
                    case "cpuloop":
                        long cpuMillis = Long.valueOf(args[nextArg++]);
                        long cpuTarget = getCpuTime() + cpuMillis * 1_000_000L;
                        while (getCpuTime() < cpuTarget) {
                            // burn the cpu until the time is up
                        }
                        sendResult(action, cpuMillis);
                        break;
                    case "cputime":
                        sendResult(action, getCpuTime());
                        break;
                    case "out":
                    case "err":
                        String value = args[nextArg++];
                        sendResult(action, value);
                        if (action.equals("err")) {
                            System.err.println(value);
                        }
                        break;
                    case "stdin":
                        // Read commands from stdin;  at eof, close stdin of
                        // children and wait for each to exit
                        sendResult(action, "start");
                        try (Reader reader = new InputStreamReader(System.in);
                             BufferedReader input = new BufferedReader(reader)) {
                            String line;
                            while ((line = input.readLine()) != null) {
                                line = line.trim();
                                if (!line.isEmpty()) {
                                    String[] split = line.split("\\s");
                                    interpretCommands(split);
                                }
                            }
                            // EOF on stdin, close stdin on all spawned processes
                            for (JavaChild p : children) {
                                try {
                                    p.getOutputStream().close();
                                } catch (IOException ie) {
                                    sendResult("stdin_closing", p.pid(),
                                            "exception", ie.getMessage());
                                }
                            }

                            for (JavaChild p : children) {
                                do {
                                    try {
                                        p.waitFor();
                                        break;
                                    } catch (InterruptedException e) {
                                        // retry
                                    }
                                } while (true);
                            }
                            // Wait for all children to be gone
                            Instant timeOut = Instant.now().plusSeconds(10L);
                            while (!completedChildren.containsAll(children)) {
                                if (Instant.now().isBefore(timeOut)) {
                                    Thread.sleep(100L);
                                } else {
                                    System.err.printf("Timeout waiting for " +
                                            "children to terminate%n");
                                    children.removeAll(completedChildren);
                                    for (JavaChild c : children) {
                                        sendResult("stdin_noterm", c.pid());
                                        System.err.printf("  Process not terminated: " +
                                                "pid: %d%n", c.pid());
                                    }
                                    System.exit(2);
                                }
                            }
                        }
                        sendResult(action, "done");
                        return;                 // normal exit from JavaChild Process
                    case "parent":
                        sendResult(action, self.parent().toString());
                        break;
                    case "pid":
                        sendResult(action, self.toString());
                        break;
                    case "exit":
                        int exitValue = (nextArg < args.length)
                                ?  Integer.valueOf(args[nextArg]) : 0;
                        sendResult(action, exitValue);
                        System.exit(exitValue);
                        break;
                    case "spawn": {
                        if (args.length - nextArg < 2) {
                            throw new RuntimeException("not enough args for respawn: " +
                                    (args.length - 2));
                        }
                        // Spawn as many children as requested and
                        // pass on rest of the arguments
                        int ncount = Integer.valueOf(args[nextArg++]);
                        Object[] subargs = new String[args.length - nextArg];
                        System.arraycopy(args, nextArg, subargs, 0, subargs.length);
                        for (int i = 0; i < ncount; i++) {
                            JavaChild p = spawnJavaChild(subargs);
                            sendResult(action, p.pid());
                            p.forEachOutputLine(JavaChild::sendRaw);
                            p.onJavaChildExit().thenAccept((p1) -> {
                                int excode = p1.exitValue();
                                sendResult("child_exit", p1.pid(), excode);
                                completedChildren.add(p1);
                            });
                            children.add(p);        // Add child to spawned list
                        }
                        nextArg = args.length;
                        break;
                    }
                    case "child": {
                        // Send the command to all the live children;
                        // ignoring those that are not alive
                        int sentCount = 0;
                        Object[] result =
                                Arrays.copyOfRange(args, nextArg - 1, args.length);
                        Object[] subargs =
                                Arrays.copyOfRange(args, nextArg + 1, args.length);
                        for (JavaChild p : children) {
                            if (p.isAlive()) {
                                sentCount++;
                                // overwrite with current pid
                                result[0] = Long.toString(p.pid());
                                sendResult(action, result);
                                p.sendAction(args[nextArg], subargs);
                            }
                        }
                        if (sentCount == 0) {
                            sendResult(action, "n/a");
                        }
                        nextArg = args.length;
                        break;
                    }
                    case "child_eof" :
                        // Close the InputStream of all the live children;
                        // ignoring those that are not alive
                        for (JavaChild p : children) {
                            if (p.isAlive()) {
                                sendResult(action, p.pid());
                                p.getOutputStream().close();
                            }
                        }
                        break;
                    case "property":
                        String name = args[nextArg++];
                        sendResult(action, name, System.getProperty(name));
                        break;
                    case "threaddump":
                        Thread.dumpStack();
                        break;
                    case "waitpid":
                        long pid = Long.parseLong(args[nextArg++]);
                        Optional<String> s = ProcessHandle.of(pid).map(ph -> waitAlive(ph));
                        sendResult(action, s.orElse("pid not valid: " + pid));
                        break;
                    default:
                        throw new Error("JavaChild action unknown: " + action);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static String waitAlive(ProcessHandle ph) {
        String status;
        try {
            boolean isAlive = ph.onExit().get().isAlive();
            status = Boolean.toString(isAlive);
        } catch (InterruptedException | ExecutionException ex ) {
            status = "interrupted";
        }
        return status;
    }

    static synchronized void sendRaw(String s) {
        System.out.println(s);
        System.out.flush();
    }
    static void sendResult(String action, Object... results) {
        sendRaw(new Event(action, results).toString());
    }

    static long getCpuTime() {
        OperatingSystemMXBean osMbean =
                (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
        return osMbean.getProcessCpuTime();
    }

    /**
     * Print command usage to stderr.
     */
    private static void help() {
        System.err.println("Commands:");
        System.err.println("  help");
        System.err.println("  pid");
        System.err.println("  parent");
        System.err.println("  cpuloop <loopcount>");
        System.err.println("  cputime");
        System.err.println("  stdin - read commands from stdin");
        System.err.println("  sleep <millis>");
        System.err.println("  spawn <n> command... - spawn n new children and send command");
        System.err.println("  child command... - send command to all live children");
        System.err.println("  child_eof - send eof to all live children");
        System.err.println("  waitpid <pid> - wait for the pid to exit");
        System.err.println("  exit <exitcode>");
        System.err.println("  out arg...");
        System.err.println("  err arg...");
    }

    static class Event {
        long pid;
        long seq;
        String command;
        Object[] results;
        Event(String command, Object... results) {
            this(self.pid(), ++commandSeq, command, results);
        }
        Event(long pid, int seq, String command, Object... results) {
            this.pid = pid;
            this.seq = seq;
            this.command = command;
            this.results = results;
        }

        /**
         * Create a String encoding the pid, seq, command, and results.
         *
         * @return a String formatted  to send to the stream.
         */
        String format() {
            StringBuilder sb = new StringBuilder();
            sb.append(pid);
            sb.append(":");
            sb.append(seq);
            sb.append(" ");
            sb.append(command);
            for (int i = 0; i < results.length; i++) {
                sb.append(" ");
                sb.append(results[i]);
            }
            return sb.toString();
        }

        Event(String encoded) {
            String[] split = encoded.split("\\s");
            String[] pidSeq = split[0].split(":");
            pid = Long.valueOf(pidSeq[0]);
            seq = Integer.valueOf(pidSeq[1]);
            command = split[1];
            Arrays.copyOfRange(split, 1, split.length);
        }

        public String toString() {
            return format();
        }

    }
}
