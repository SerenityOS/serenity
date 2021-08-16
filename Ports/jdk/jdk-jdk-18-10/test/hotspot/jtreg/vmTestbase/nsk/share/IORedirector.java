/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share;

import java.io.*;

/**
 * This class implements a thread transfering bytes from
 * a given InputStream to a given OutputStream.
 */
public class IORedirector extends Thread {
    private BufferedReader bin  = null;
    private PrintStream    pout = null;
    private Log            log  = null;

    /**
     * Few symbols to precede every text line being redirected.
     */
    private String prefix = "";

    /**
     * Input and output streams must be specified.
     */
    private IORedirector() {
        super("IORedirector");
    }

    /**
     * Redirect <code>in</code> to <code>out</code>.
     *
     * @deprecated Use newer constructor.
     *
     * @see #IORedirector(BufferedReader,Log,String)
     */
    @Deprecated
    public IORedirector(InputStream in, OutputStream out) {
        this();
        bin  = new BufferedReader(new InputStreamReader(in));
        pout = new PrintStream(out);
    }

    /**
     * Redirect <code>in</code> to <code>log</code>; and assign
     * few <code>prefix</code> symbols to precede each text line
     * being redirected.
     */
    public IORedirector(BufferedReader in, Log log, String prefix) {
        this();
        this.prefix = prefix;
        this.bin  = in;
        this.log = log;
    }

    /**
     * Set the prefix for redirected messages;
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    private boolean cancelled = false;
    private boolean stopped = false;
    private boolean started = false;

    /**
     * Signal to <code>run()</code> method that it should terminate,
     * and wait until it is finished.
     */
    public void cancel () {
        cancelled = true;
        while (this.isAlive())
            try {
                this.join();
            } catch (InterruptedException ie) {
                throw new Failure(ie);
            };
        // stopped==true here.
    }

    /**
     * Pass data bytes from <code>in</code> to <code>out</code> stream
     * until EOF is read, or this IORedirector is cancelled.
     */
    public void run () {
        started = true;
        String logPrefix = "IORedirector-" + prefix;
        if (bin == null || (pout == null && log == null))
            return;
        try {
            while (!cancelled) {
                String line = bin.readLine();
                if (line == null)
                    break; //EOF
                String message = prefix + line;
                if (log != null) {
                    // It's synchronized and auto-flushed:
                    log.println(message);
                } else if (pout != null) {
                    synchronized (pout) {
                        pout.println(message);
                        pout.flush();
                    }
                }
            }
        } catch (IOException e) {
            // e.printStackTrace(log.getOutStream());
            String msg = "# WARNING: Caught IOException while redirecting output stream:\n\t" + e;
            if (log != null) {
                log.println(msg);
            } else if (pout != null) {
                synchronized (pout) {
                    pout.println(msg);
                    pout.flush();
                }
            } else {
                System.err.println(msg);
                System.err.flush();
            }
        };
        stopped = true;
    }
}
