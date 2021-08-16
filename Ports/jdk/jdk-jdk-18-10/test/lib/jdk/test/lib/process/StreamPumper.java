/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.process;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StreamPumper implements Runnable {

    private static final int BUF_SIZE = 256;

    /**
     * Pump will be called by the StreamPumper to process the incoming data
     */
    public abstract static class Pump {
        abstract void register(StreamPumper d);
    }

    /**
     * OutputStream -> Pump adapter
     */
    public final static class StreamPump extends Pump {
        private final OutputStream out;
        public StreamPump(OutputStream out) {
            this.out = out;
        }

        @Override
        void register(StreamPumper sp) {
            sp.addOutputStream(out);
        }
    }

    /**
     * Used to process the incoming data line-by-line
     */
    public abstract static class LinePump extends Pump {
        @Override
        final void register(StreamPumper sp) {
            sp.addLineProcessor(this);
        }

        protected abstract void processLine(String line);
    }

    private final InputStream in;
    private final Set<OutputStream> outStreams = new HashSet<>();
    private final Set<LinePump> linePumps = new HashSet<>();

    private final AtomicBoolean processing = new AtomicBoolean(false);

    public StreamPumper(InputStream in) {
        this.in = in;
    }

    /**
     * Create a StreamPumper that reads from in and writes to out.
     *
     * @param in The stream to read from.
     * @param out The stream to write to.
     */
    public StreamPumper(InputStream in, OutputStream out) {
        this(in);
        this.addOutputStream(out);
    }

    /**
     * Implements Thread.run(). Continuously read from {@code in} and write to
     * {@code out} until {@code in} has reached end of stream. Abort on
     * interruption. Abort on IOExceptions.
     */
    @Override
    public void run() {
        try (BufferedInputStream is = new BufferedInputStream(in)) {
            ByteArrayOutputStream lineBos = new ByteArrayOutputStream();
            byte[] buf = new byte[BUF_SIZE];
            int len = 0;
            int linelen = 0;

            while ((len = is.read(buf)) > 0 && !Thread.interrupted()) {
                for (OutputStream out : outStreams) {
                    out.write(buf, 0, len);
                }
                if (!linePumps.isEmpty()) {
                    int i = 0;
                    int lastcrlf = -1;
                    while (i < len) {
                        if (buf[i] == '\n' || buf[i] == '\r') {
                            int bufLinelen = i - lastcrlf - 1;
                            if (bufLinelen > 0) {
                                lineBos.write(buf, lastcrlf + 1, bufLinelen);
                            }
                            linelen += bufLinelen;

                            if (linelen > 0) {
                                lineBos.flush();
                                final String line = lineBos.toString();
                                linePumps.forEach((lp) -> lp.processLine(line));
                                lineBos.reset();
                                linelen = 0;
                            }
                            lastcrlf = i;
                        }

                        i++;
                    }
                    if (lastcrlf == -1) {
                        lineBos.write(buf, 0, len);
                        linelen += len;
                    } else if (lastcrlf < len - 1) {
                        lineBos.write(buf, lastcrlf + 1, len - lastcrlf - 1);
                        linelen += len - lastcrlf - 1;
                    }
                }
            }

        } catch (IOException e) {
            if (!e.getMessage().equalsIgnoreCase("stream closed")) {
                e.printStackTrace();
            }
        } finally {
            for (OutputStream out : outStreams) {
                try {
                    out.flush();
                } catch (IOException e) {}
            }
            try {
                in.close();
            } catch (IOException e) {}
        }
    }

    final void addOutputStream(OutputStream out) {
        outStreams.add(out);
    }

    final void addLineProcessor(LinePump lp) {
        linePumps.add(lp);
    }

    public final StreamPumper addPump(Pump ... pump) {
        if (processing.get()) {
            throw new IllegalStateException("Can not modify pumper while " +
                                            "processing is in progress");
        }
        for (Pump p : pump) {
            p.register(this);
        }
        return this;
    }

    public final Future<Void> process() {
        if (!processing.compareAndSet(false, true)) {
            throw new IllegalStateException("Can not re-run the processing");
        }
        FutureTask<Void> result = new FutureTask<>(this, null);
        Thread t = new Thread(result);
        t.setDaemon(true);
        t.start();

        return result;
    }
}
