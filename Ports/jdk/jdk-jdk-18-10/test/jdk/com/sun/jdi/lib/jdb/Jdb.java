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

package lib.jdb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Utils;
import jdk.test.lib.process.StreamPumper;

public class Jdb implements AutoCloseable {
    public Jdb(String... args) {
        ProcessBuilder pb = new ProcessBuilder(JDKToolFinder.getTestJDKTool("jdb"));
        pb.command().addAll(Arrays.asList(args));
        try {
            jdb = pb.start();
        } catch (IOException ex) {
            throw new RuntimeException("failed to launch pdb", ex);
        }
        try {
            StreamPumper stdout = new StreamPumper(jdb.getInputStream());
            StreamPumper stderr = new StreamPumper(jdb.getErrorStream());

            stdout.addPump(new StreamPumper.StreamPump(outputHandler));
            stderr.addPump(new StreamPumper.StreamPump(outputHandler));

            stdout.process();
            stderr.process();

            inputWriter = new PrintWriter(jdb.getOutputStream(), true);
        } catch (Throwable ex) {
            // terminate jdb if something went wrong
            jdb.destroy();
            throw ex;
        }
    }

    private final Process jdb;
    private final OutputHandler outputHandler = new OutputHandler();
    private final PrintWriter inputWriter;
    private final List<String> jdbOutput = new LinkedList<>();

    private static final String lineSeparator = System.getProperty("line.separator");
    // wait time before check jdb output (in ms)
    private static final long sleepTime = 1000;
    // max time to wait for  jdb output (in ms)
    private static final long timeout = Utils.adjustTimeout(60000);

    // pattern for message of a breakpoint hit
    public static final String BREAKPOINT_HIT = "Breakpoint hit:";
    // pattern for message of an application exit
    public static final String APPLICATION_EXIT = "The application exited";
    // pattern for message of an application disconnect
    public static final String APPLICATION_DISCONNECTED = "The application has been disconnected";


    @Override
    public void close() throws Exception {
        shutdown();
    }

    // waits until the process shutdown or crash
    public boolean waitFor(long timeout, TimeUnit unit) {
        try {
            return jdb.waitFor(Utils.adjustTimeout(timeout), unit);
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void shutdown() {
        // shutdown jdb
        if (jdb.isAlive()) {
            try {
                quit();
                // wait some time after the command for the process termination
                waitFor(10, TimeUnit.SECONDS);
            } finally {
                if (jdb.isAlive()) {
                    jdb.destroy();
                }
            }
        }
    }


    // waits until string {@pattern} appears in the jdb output, within the last {@code lines} lines.
    /* Comment from original /test/jdk/com/sun/jdi/ShellScaffold.sh
        # Now we have to wait for the next jdb prompt.  We wait for a pattern
        # to appear in the last line of jdb output.  Normally, the prompt is
        #
        # 1) ^main[89] @
        #
        # where ^ means start of line, and @ means end of file with no end of line
        # and 89 is the current command counter. But we have complications e.g.,
        # the following jdb output can appear:
        #
        # 2) a[89] = 10
        #
        # The above form is an array assignment and not a prompt.
        #
        # 3) ^main[89] main[89] ...
        #
        # This occurs if the next cmd is one that causes no jdb output, e.g.,
        # 'trace methods'.
        #
        # 4) ^main[89] [main[89]] .... > @
        #
        # jdb prints a > as a prompt after something like a cont.
        # Thus, even though the above is the last 'line' in the file, it
        # isn't the next prompt we are waiting for after the cont completes.
        # HOWEVER, sometimes we see this for a cont command:
        #
        #   ^main[89] $
        #      <lines output for hitting a bkpt>
        #
        # 5) ^main[89] > @
        #
        # i.e., the > prompt comes out AFTER the prompt we we need to wait for.
    */
    // compile regexp once
    private final static String promptPattern = "[a-zA-Z0-9_-][a-zA-Z0-9_-]*\\[[1-9][0-9]*\\] [ >]*$";
    final static Pattern PROMPT_REGEXP = Pattern.compile(promptPattern);

    public List<String> waitForPrompt(int lines, boolean allowExit) {
        return waitForPrompt(lines, allowExit, PROMPT_REGEXP);
    }

    // jdb prompt when debuggee is not started and is not suspended after breakpoint
    private static final String SIMPLE_PROMPT = "> ";
    public List<String> waitForSimplePrompt(int lines, boolean allowExit) {
        return waitForPrompt(lines, allowExit, Pattern.compile(SIMPLE_PROMPT));
    }

    private List<String> waitForPrompt(int lines, boolean allowExit, Pattern promptRegexp) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // ignore
            }
            synchronized (outputHandler) {
                if (!outputHandler.updated()) {
                    try {
                        outputHandler.wait(sleepTime);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                } else {
                    // if something appeared in the jdb output, reset the timeout
                    startTime = System.currentTimeMillis();
                }
            }
            List<String> reply = outputHandler.get();
            if ((promptRegexp.flags() & Pattern.MULTILINE) > 0) {
                String replyString = reply.stream().collect(Collectors.joining(lineSeparator));
                if (promptRegexp.matcher(replyString).find()) {
                    logJdb(reply);
                    return outputHandler.reset();
                }
            } else {
                for (String line : reply.subList(Math.max(0, reply.size() - lines), reply.size())) {
                    if (promptRegexp.matcher(line).find()) {
                        logJdb(reply);
                        return outputHandler.reset();
                    }
                }
            }
            if (!jdb.isAlive()) {
                // ensure we get the whole output
                reply = outputHandler.reset();
                logJdb(reply);
                if (!allowExit) {
                    throw new RuntimeException("waitForPrompt timed out after " + (timeout/1000)
                            + " seconds, looking for '" + promptRegexp.pattern() + "', in " + lines + " lines");
                }
                return reply;
            }
        }
        // timeout
        logJdb(outputHandler.get());
        throw new RuntimeException("waitForPrompt timed out after " + (timeout/1000)
                + " seconds, looking for '" + promptRegexp.pattern() + "', in " + lines + " lines");
    }

    public List<String> command(JdbCommand cmd) {
        if (!jdb.isAlive()) {
            if (cmd.allowExit) {
                // return remaining output
                return outputHandler.reset();
            }
            throw new RuntimeException("Attempt to send command '" + cmd.cmd + "' to terminated jdb");
        }

        log("> " + cmd.cmd);

        inputWriter.println(cmd.cmd);

        if (inputWriter.checkError()) {
            throw new RuntimeException("Unexpected IO error while writing command '" + cmd.cmd + "' to jdb stdin stream");
        }

        return waitForPrompt(1, cmd.allowExit, cmd.waitForPattern);
    }

    public List<String> command(String cmd) {
        return command(new JdbCommand(cmd));
    }

    // sends "cont" command up to maxTimes until debuggee exit
    public void contToExit(int maxTimes) {
        boolean exited = false;
        JdbCommand cont = JdbCommand.cont().allowExit();
        for (int i = 0; i < maxTimes && jdb.isAlive(); i++) {
            String reply = command(cont).stream().collect(Collectors.joining(lineSeparator));
            if (reply.contains(APPLICATION_EXIT)) {
                exited = true;
                break;
            }
        }
        if (!exited && jdb.isAlive()) {
            throw new RuntimeException("Debuggee did not exit after " + maxTimes + " <cont> commands");
        }
    }

    // quits jdb by using "quit" command
    public void quit() {
        command(JdbCommand.quit());
    }

    private void log(String s) {
        System.out.println(s);
    }

    private void logJdb(List<String> reply) {
        jdbOutput.addAll(reply);
        reply.forEach(s -> log("[jdb] " + s));
    }

    // returns the whole jdb output as a string
    public String getJdbOutput() {
        return jdbOutput.stream().collect(Collectors.joining(lineSeparator));
    }

    // handler for out/err of the pdb process
    private class OutputHandler extends OutputStream {
        // there are 2 buffers:
        // outStream - data from the process stdout/stderr after last get() call
        // cachedData - data collected at get(), cleared by reset()

        private final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        // if the last line in the reply had EOL, the list's last element is empty
        private final List<String> cachedData = new ArrayList<>();

        @Override
        public synchronized void write(int b) throws IOException {
            outStream.write((byte)(b & 0xFF));
            notifyAll();
        }
        @Override
        public synchronized void write(byte b[], int off, int len) throws IOException {
            outStream.write(b, off, len);
            notifyAll();
        }

        // gets output after the last {@ reset}.
        // returned data becomes invalid after {@reset}.
        public synchronized List<String> get() {
            if (updated()) {
                // we don't want to discard empty lines
                String[] newLines = outStream.toString().split("\\R", -1);
                if (!cachedData.isEmpty()) {
                    // concat the last line if previous data had no EOL
                    newLines[0] = cachedData.remove(cachedData.size()-1) + newLines[0];
                }
                cachedData.addAll(Arrays.asList(newLines));
                outStream.reset();
            }
            return Collections.unmodifiableList(cachedData);
        }

        // clears last replay (does not touch replyStream)
        // returns list as the last get()
        public synchronized List<String> reset() {
            List<String> result = new ArrayList<>(cachedData);
            cachedData.clear();
            return result;
        }

        // tests if there are some new data after the last lastReply() call
        public synchronized boolean updated() {
            return outStream.size() > 0;
        }
    }
}

