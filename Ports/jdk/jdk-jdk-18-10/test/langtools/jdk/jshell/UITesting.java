/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jdk.jshell.JShell;
import jdk.jshell.tool.JavaShellToolBuilder;

public class UITesting {

    protected static final String TAB = "\011";
    protected static final String INTERRUPT = "\u0003";
    protected static final String BELL = "\u0007";
    protected static final String PROMPT = " \u0005";
    protected static final String CONTINUATION_PROMPT = " \u0006";
    protected static final String REDRAW_PROMPT = "\n\r?" + PROMPT;
    protected static final String UP = "\033[A";
    protected static final String DOWN = "\033[B";
    private final boolean laxLineEndings;

    public UITesting() {
        this(false);
    }

    public UITesting(boolean laxLineEndings) {
        this.laxLineEndings = laxLineEndings;
    }

    protected void doRunTest(Test test) throws Exception {
        // turn on logging of launch failures
        Logger.getLogger("jdk.jshell.execution").setLevel(Level.ALL);

        PipeInputStream input = new PipeInputStream();
        StringBuilder out = new StringBuilder();
        PrintStream outS = new PrintStream(new OutputStream() {
            @Override public void write(int b) throws IOException {
                synchronized (out) {
                    System.out.print((char) b);
                    out.append((char) b);
                    out.notifyAll();
                }
            }
            @Override public void write(byte[] b, int off, int len) throws IOException {
                synchronized (out) {
                    String data = new String(b, off, len);
                    System.out.print(data);
                    out.append(data);
                    out.notifyAll();
                }
            }
        });
        Thread runner = new Thread(() -> {
            try {
                JavaShellToolBuilder.builder()
                        .in(input, input)
                        .out(outS)
                        .err(outS)
                        .promptCapture(true)
                        .persistence(new HashMap<>())
                        .locale(Locale.US)
                        .run("--no-startup");
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });

        Writer inputSink = new OutputStreamWriter(input.createOutput(), StandardCharsets.UTF_8) {
            @Override
            public void write(String str) throws IOException {
                super.write(str);
                flush();
            }
        };

        runner.start();

        try {
            Class<?> jshellToolClass = Class.forName("jdk.internal.jshell.tool.JShellTool");
            Field promptField = jshellToolClass.getDeclaredField("PROMPT");
            promptField.setAccessible(true);
            promptField.set(null, PROMPT);
            Field continuationPromptField = jshellToolClass.getDeclaredField("CONTINUATION_PROMPT");
            continuationPromptField.setAccessible(true);
            continuationPromptField.set(null, CONTINUATION_PROMPT);
            waitOutput(out, PROMPT);
            test.test(inputSink, out);
        } finally {
            inputSink.write(INTERRUPT + INTERRUPT + "/exit");

            runner.join(1000);
            if (runner.isAlive()) {
                runner.stop();
            }
        }
    }

    protected interface Test {
        public void test(Writer inputSink, StringBuilder out) throws Exception;
    }

    private static final long TIMEOUT;

    static {
        long factor;

        try {
            factor = (long) Double.parseDouble(System.getProperty("test.timeout.factor", "1"));
        } catch (NumberFormatException ex) {
            factor = 1;
        }
        TIMEOUT = 60_000 * factor;
    }

    protected void waitOutput(StringBuilder out, String expected) {
        waitOutput(out, expected, null);
    }

    // Return true if expected is found, false if secondary is found,
    // otherwise, time out with an IllegalStateException
    protected boolean waitOutput(StringBuilder out, String expected, String secondary) {
        expected = expected.replaceAll("\n", laxLineEndings ? "\r*\n" : System.getProperty("line.separator"));
        Pattern expectedPattern = Pattern.compile(expected, Pattern.DOTALL);
        Pattern secondaryPattern = null;
        if (secondary != null) {
            secondary = secondary.replaceAll("\n", laxLineEndings ? "\r*\n" : System.getProperty("line.separator"));
            secondaryPattern = Pattern.compile(secondary, Pattern.DOTALL);
        }
        synchronized (out) {
            long s = System.currentTimeMillis();

            while (true) {
                Matcher m = expectedPattern.matcher(out);
                if (m.find()) {
                    out.delete(0, m.end());
                    return true;
                }
                if (secondaryPattern != null) {
                    m = secondaryPattern.matcher(out);
                    if (m.find()) {
                        out.delete(0, m.end());
                        return false;
                    }
                }
                long e =  System.currentTimeMillis();
                if ((e - s) > TIMEOUT) {
                    throw new IllegalStateException("Timeout waiting for: " + quote(expected) + ", actual output so far: " + quote(out.toString()));
                }
                try {
                    out.wait(TIMEOUT);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private String quote(String original) {
        StringBuilder output = new StringBuilder();

        for (char c : original.toCharArray()) {
            if (c < 32) {
                output.append(String.format("\\u%04X", (int) c));
            } else {
                output.append(c);
            }
        }

        return output.toString();
    }

    protected String clearOut(String what) {
        return backspace(what.length()) + "\\u001B\\[K";
    }

    protected String backspace(int n) {
        return fill(n, '\010');
    }

    protected String space(int n) {
        return fill(n, ' ');
    }

    private String fill(int n, char c) {
        StringBuilder result = new StringBuilder(n);

        while (n-- > 0)
            result.append(c);

        return result.toString();
    }

    private final ResourceBundle resources;
    {
        resources = ResourceBundle.getBundle("jdk.internal.jshell.tool.resources.l10n", Locale.US, JShell.class.getModule());
    }

    protected String getResource(String key) {
        return resources.getString(key);
    }

    protected String resource(String key) {
        return patternQuote(getResource(key));
    }

    protected String patternQuote(String str) {
        //from JDK-6507804:
        return str.replaceAll("([\\\\\\[\\].^$?*+{}()|])", "\\\\$1");
    }

    protected String getMessage(String key, Object... args) {
        return MessageFormat.format(resources.getString(key), args);
    }
    private static class PipeInputStream extends InputStream {

        private static final int INITIAL_SIZE = 128;
        private int[] buffer = new int[INITIAL_SIZE];
        private int start;
        private int end;
        private boolean closed;

        @Override
        public synchronized int read() throws IOException {
            if (start == end && !closed) {
                inputNeeded();
            }
            while (start == end) {
                if (closed) {
                    return -1;
                }
                try {
                    wait();
                } catch (InterruptedException ex) {
                    //ignore
                }
            }
            try {
                return buffer[start];
            } finally {
                start = (start + 1) % buffer.length;
            }
        }

        @Override
        public synchronized int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            int c = read();
            if (c == -1) {
                return -1;
            }
            b[off] = (byte)c;

            int totalRead = 1;
            while (totalRead < len && start != end) {
                int r = read();
                if (r == (-1))
                    break;
                b[off + totalRead++] = (byte) r;
            }
            return totalRead;
        }

        protected void inputNeeded() throws IOException {}

        private synchronized void write(int b) {
            if (closed) {
                throw new IllegalStateException("Already closed.");
            }
            int newEnd = (end + 1) % buffer.length;
            if (newEnd == start) {
                //overflow:
                int[] newBuffer = new int[buffer.length * 2];
                int rightPart = (end > start ? end : buffer.length) - start;
                int leftPart = end > start ? 0 : start - 1;
                System.arraycopy(buffer, start, newBuffer, 0, rightPart);
                System.arraycopy(buffer, 0, newBuffer, rightPart, leftPart);
                buffer = newBuffer;
                start = 0;
                end = rightPart + leftPart;
                newEnd = end + 1;
            }
            buffer[end] = b;
            end = newEnd;
            notifyAll();
        }

        @Override
        public synchronized void close() {
            closed = true;
            notifyAll();
        }

        public OutputStream createOutput() {
            return new OutputStream() {
                @Override public void write(int b) throws IOException {
                    PipeInputStream.this.write(b);
                }
                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    for (int i = 0 ; i < len ; i++) {
                        write(Byte.toUnsignedInt(b[off + i]));
                    }
                }
                @Override
                public void close() throws IOException {
                    PipeInputStream.this.close();
                }
            };
        }

    }

}
