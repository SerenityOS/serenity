/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.StringTokenizer;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.JDWP;
import static jdk.test.lib.Asserts.assertFalse;

/**
 * Launches the debuggee with the necessary JDWP options and handles the output
 */
public class DebuggeeLauncher implements StreamHandler.Listener {

    public interface Listener {

        /**
         * Callback to use when a module name is received from the debuggee
         *
         * @param modName module name reported by the debuggee
         */
        void onDebuggeeModuleInfo(String modName);

        /**
         * Callback to use when the debuggee completes sending out the info
         */
        void onDebuggeeSendingCompleted();

        /**
         * Callback to handle any debuggee error
         *
         * @param line line from the debuggee's stderr
         */
        void onDebuggeeError(String line);
    }

    private int jdwpPort = -1;
    private static final String CLS_DIR = System.getProperty("test.classes", "").trim();
    private static final String DEBUGGEE = "AllModulesCommandTestDebuggee";
    private Process p;
    private final Listener listener;
    private StreamHandler inputHandler;
    private StreamHandler errorHandler;

    /**
     * @param listener the listener we report the debuggee events to
     */
    public DebuggeeLauncher(Listener listener) {
        this.listener = listener;
    }

    /**
     * Starts the debuggee with the necessary JDWP options and handles the
     * debuggee's stdout and stderr outputs
     *
     * @throws Throwable
     */
    public void launchDebuggee() throws Throwable {

        ProcessBuilder pb = new ProcessBuilder(getCommand());
        p = pb.start();
        inputHandler = new StreamHandler(p.getInputStream(), this);
        errorHandler = new StreamHandler(p.getErrorStream(), this);
        inputHandler.start();
        errorHandler.start();
    }

    /**
     * Command to start the debuggee with the JDWP options and using the JDK
     * under test
     *
     * @return the command
     */
    private String[] getCommand() {
        return new String[]{
            JDKToolFinder.getTestJDKTool("java"),
            getJdwpOptions(),
            "-cp",
            CLS_DIR,
            DEBUGGEE
        };
    }

    /**
     * Terminates the debuggee
     */
    public void terminateDebuggee() {
        if (p.isAlive()) {
            p.destroyForcibly();
        }
    }

    /**
     * Debuggee JDWP options
     *
     * @return the JDWP options to start the debuggee with
     */
    private static String getJdwpOptions() {
        return "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0";
    }

    /**
     * Gets JDWP port debuggee is listening on.
     *
     * @return JDWP port
     */
    public int getJdwpPort() {
        assertFalse(jdwpPort == -1, "JDWP port is not detected");
        return jdwpPort;
    }

    @Override
    public void onStringRead(StreamHandler handler, String line) {
        if (handler.equals(errorHandler)) {
            terminateDebuggee();
            listener.onDebuggeeError(line);
        } else {
            processDebuggeeOutput(line);
        }
    }

    private void processDebuggeeOutput(String line) {
        if (jdwpPort == -1) {
            JDWP.ListenAddress addr = JDWP.parseListenAddress(line);
            if (addr != null) {
                jdwpPort = Integer.parseInt(addr.address());
            }
        }
        StringTokenizer st = new StringTokenizer(line);
        String token = st.nextToken();
        switch (token) {
            case "module":
                listener.onDebuggeeModuleInfo(st.nextToken());
                break;
            case "ready":
                listener.onDebuggeeSendingCompleted();
                break;
        }
    }
}
