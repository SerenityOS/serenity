/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot;

import sun.jvm.hotspot.*;
import sun.jvm.hotspot.debugger.*;

import java.io.*;
import java.util.*;

public class CLHSDB {

    public CLHSDB(JVMDebugger d) {
        pid = -1;
        execPath = null;
        coreFilename = null;
        debugServerName = null;
        jvmDebugger = d;
    }

    public static void main(String[] args) {
        new CLHSDB(args).run();
    }

    public void run() {
        // If jvmDebugger is already set, we have been given a JVMDebugger.
        // Otherwise, if pid != -1 we are supposed to attach to it.
        // If execPath != null, it is the path of a jdk/bin/java
        // and coreFilename is the pathname of a core file we are
        // supposed to attach to.
        // Finally, if debugServerName != null, we are supposed to
        // connect to remote debug server.

        agent = new HotSpotAgent();

        Runtime.getRuntime().addShutdownHook(new java.lang.Thread() {
                public void run() {
                    detachDebugger();
                }
            });

        if (jvmDebugger != null) {
            attachDebugger(jvmDebugger);
        } else if (pid != -1) {
            attachDebugger(pid);
        } else if (execPath != null) {
            attachDebugger(execPath, coreFilename);
        } else if (debugServerName != null) {
            connect(debugServerName);
        }


        CommandProcessor.DebuggerInterface di = new CommandProcessor.DebuggerInterface() {
                public HotSpotAgent getAgent() {
                    return agent;
                }
                public boolean isAttached() {
                    return attached;
                }
                public void attach(int pid) {
                    attachDebugger(pid);
                }
                public void attach(String java, String core) {
                    attachDebugger(java, core);
                }
                public void attach(String debugServerName) {
                    connect(debugServerName);
                }
                public void detach() {
                    detachDebugger();
                }
                public void reattach() {
                    if (attached) {
                        detachDebugger();
                    }
                    if (pid != -1) {
                        attach(pid);
                    } else if (debugServerName != null) {
                        connect(debugServerName);
                    } else {
                        attach(execPath, coreFilename);
                    }
                }
            };


        BufferedReader in =
            new BufferedReader(new InputStreamReader(System.in));
        CommandProcessor cp = new CommandProcessor(di, in, System.out, System.err);
        cp.run(true);

    }

    //--------------------------------------------------------------------------------
    // Internals only below this point
    //
    private HotSpotAgent agent;
    private JVMDebugger jvmDebugger;
    private boolean      attached;
    // These had to be made data members because they are referenced in inner classes.
    private int pid;
    private String execPath;
    private String coreFilename;
    private String debugServerName;

    private void doUsage() {
        System.out.println("Usage:  java CLHSDB [[pid] | [path-to-java-executable [path-to-corefile]] | help ]");
        System.out.println("           pid:                     attach to the process whose id is 'pid'");
        System.out.println("           path-to-java-executable: Debug a core file produced by this program");
        System.out.println("           path-to-corefile:        Debug this corefile.  The default is 'core'");
        System.out.println("        If no arguments are specified, you can select what to do from the GUI.\n");
        HotSpotAgent.showUsage();
    }

    private CLHSDB(String[] args) {
        pid = -1;
        execPath = null;
        coreFilename = null;
        debugServerName = null;

        switch (args.length) {
        case (0):
            break;

        case (1):
            if (args[0].equals("help") || args[0].equals("-help")) {
                doUsage();
                return;
            }
            try {
                // Attempt to attach as a PID
                pid = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                // Attempt to connect to remote debug server
                debugServerName = args[0];
            }
            break;

        case (2):
            execPath = args[0];
            coreFilename = args[1];
            break;

        default:
            System.out.println("HSDB Error: Too many options specified");
            doUsage();
            return;
        }
    }

    private void attachDebugger(JVMDebugger d) {
        agent.attach(d);
        attached = true;
     }

    /** NOTE we are in a different thread here than either the main
        thread or the Swing/AWT event handler thread, so we must be very
        careful when creating or removing widgets */
    private void attachDebugger(int pid) {
        this.pid = pid;
        try {
            System.out.println("Attaching to process " + pid + ", please wait...");

            // FIXME: display exec'd debugger's output messages during this
            // lengthy call
            agent.attach(pid);
            attached = true;
        }
        catch (DebuggerException e) {
            final String errMsg = formatMessage(e.getMessage(), 80);
            System.err.println("Unable to connect to process ID " + pid + ":\n\n" + errMsg);
            agent.detach();
            e.printStackTrace();
            return;
        }
    }

    /** NOTE we are in a different thread here than either the main
        thread or the Swing/AWT event handler thread, so we must be very
        careful when creating or removing widgets */
    private void attachDebugger(final String executablePath, final String corePath) {
        // Try to open this core file
        try {
            System.out.println("Opening core file, please wait...");

            // FIXME: display exec'd debugger's output messages during this
            // lengthy call
            agent.attach(executablePath, corePath);
            attached = true;
        }
        catch (DebuggerException e) {
            final String errMsg = formatMessage(e.getMessage(), 80);
            System.err.println("Unable to open core file\n" + corePath + ":\n\n" + errMsg);
            agent.detach();
            e.printStackTrace();
            return;
        }
    }

    /** NOTE we are in a different thread here than either the main
        thread or the Swing/AWT event handler thread, so we must be very
        careful when creating or removing widgets */
    private void connect(final String debugServerName) {
        // Try to open this core file
        try {
            System.out.println("Connecting to debug server, please wait...");
            agent.attach(debugServerName);
            this.debugServerName = debugServerName;
            attached = true;
        }
        catch (DebuggerException e) {
            final String errMsg = formatMessage(e.getMessage(), 80);
            System.err.println("Unable to connect to debug server \"" + debugServerName + "\":\n\n" + errMsg);
            agent.detach();
            e.printStackTrace();
            return;
        }
    }

    private void detachDebugger() {
        if (!attached) {
            return;
        }
        agent.detach();
        attached = false;
    }

    private void detach() {
        detachDebugger();
    }

    /** Punctuates the given string with \n's where necessary to not
        exceed the given number of characters per line. Strips
        extraneous whitespace. */
    private String formatMessage(String message, int charsPerLine) {
        StringBuilder buf = new StringBuilder(message.length());
        StringTokenizer tokenizer = new StringTokenizer(message);
        int curLineLength = 0;
        while (tokenizer.hasMoreTokens()) {
            String tok = tokenizer.nextToken();
            if (curLineLength + tok.length() > charsPerLine) {
                buf.append('\n');
                curLineLength = 0;
            } else {
                if (curLineLength != 0) {
                    buf.append(' ');
                    ++curLineLength;
                }
            }
            buf.append(tok);
            curLineLength += tok.length();
        }
        return buf.toString();
    }
}
