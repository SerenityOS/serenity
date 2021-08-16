/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.net.BindException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jdk.internal.agent.Agent;
import jdk.internal.agent.AgentConfigurationError;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.ProcessTools;

/**
 * A helper class for issuing ManagementAgent.* diagnostic commands and capturing
 * their output.
 */
final class ManagementAgentJcmd {
    private static final String CMD_STOP = "ManagementAgent.stop";
    private static final String CMD_START = "ManagementAgent.start";
    private static final String CMD_START_LOCAL = "ManagementAgent.start_local";
    private static final String CMD_STATUS = "ManagementAgent.status";
    private static final String CMD_PRINTPERF = "PerfCounter.print";

    private final String id;
    private final boolean verbose;

    public ManagementAgentJcmd(String targetApp, boolean verbose) {
        this.id = targetApp;
        this.verbose = verbose;
    }

    /**
     * `jcmd`
     * @return The JCMD output
     * @throws IOException
     * @throws InterruptedException
     */
    public String list() throws IOException, InterruptedException {
        return jcmd();
    }

    /**
     * `jcmd PerfCounter.print`
     * @return Returns the available performance counters with their values as
     *         {@linkplain Properties} instance
     * @throws IOException
     * @throws InterruptedException
     */
    public Properties perfCounters() throws IOException, InterruptedException {
        return perfCounters(".*");
    }

    /**
     * `jcmd PerfCounter.print | grep {exp}>`
     * @param regex Regular expression for including perf counters in the result
     * @return Returns the matching performance counters with their values
     *         as {@linkplain Properties} instance
     * @throws IOException
     * @throws InterruptedException
     */
    public Properties perfCounters(String regex) throws IOException, InterruptedException {
        Pattern pat = Pattern.compile(regex);
        Properties p = new Properties();
        for(String l : jcmd(CMD_PRINTPERF).split("\\n")) {
            String[] kv = l.split("=");
            if (kv.length > 1) {
                if (pat.matcher(kv[0]).matches()) {
                    p.setProperty(kv[0], kv[1].replace("\"", ""));
                }
            }
        }
        return p;
    }

    /**
     * `jcmd <app> ManagementAgent.stop`
     * @return The JCMD output
     * @throws IOException
     * @throws InterruptedException
     */
    public String stop() throws IOException, InterruptedException {
        return jcmd(CMD_STOP);
    }

    /**
     * `jcmd <app> ManagementAgent.start_local`
     * @return The JCMD output
     * @throws IOException
     * @throws InterruptedException
     */
    public String startLocal() throws IOException, InterruptedException {
        return jcmd(CMD_START_LOCAL);
    }

    /**
     * `jcmd <app> ManagementAgent.start <args>`
     * @return The JCMD output
     * @param params The arguments to <b>ManagementAgent.start</b> command
     * @throws IOException
     * @throws InterruptedException
     */
    public String start(String ... params) throws IOException, InterruptedException {
        return start(c->{}, params);
    }

    /**
     * `jcmd <pp> ManagementAgent.start <args>`
     * @param c A string consumer used to inspect the jcmd output line-by-line
     * @param params The arguments to <b>ManagementAgent.start</b> command
     * @return The JCMD output
     * @throws IOException
     * @throws InterruptedException
     */
    public String start(Consumer<String> c, String ... params) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>();
        args.add(CMD_START);
        args.addAll(Arrays.asList(params));
        return jcmd(c, args.toArray(new String[args.size()]));
    }

    public String status() throws IOException, InterruptedException {
        return jcmd(CMD_STATUS);
    }

    /**
     * Run the "jcmd" command
     *
     * @param command Command + arguments
     * @return The JCMD output
     * @throws IOException
     * @throws InterruptedException
     */
    private String jcmd(String ... command) throws IOException, InterruptedException {
        if (command.length == 0) {
            return jcmd(null, c->{});
        } else {
            return jcmd(c->{}, command);
        }
    }

    /**
     * Run the "jcmd" command
     *
     * @param c {@linkplain Consumer} instance
     * @param command Command + arguments
     * @return The JCMD output
     * @throws IOException
     * @throws InterruptedException
     */
    private String jcmd(Consumer<String> c, String ... command) throws IOException, InterruptedException {
        return jcmd(id, c, command);
    }

    /**
     * Run the "jcmd" command
     *
     * @param target The target application name (or PID)
     * @param c {@linkplain Consumer} instance
     * @param command Command + arguments
     * @return The JCMD output
     * @throws IOException
     * @throws InterruptedException
     */
    private String jcmd(String target, final Consumer<String> c, String ... command) throws IOException, InterruptedException {
        dbg_print("[jcmd] " + (command.length > 0 ? command[0] : "list"));

        JDKToolLauncher l = JDKToolLauncher.createUsingTestJDK("jcmd");
        l.addToolArg(target);
        for (String cmd : command) {
            l.addToolArg(cmd);
        }

        // this buffer will get filled in different threads
        //   -> must be the synchronized StringBuffer
        StringBuffer output = new StringBuffer();

        AtomicBoolean portUnavailable = new AtomicBoolean(false);
        Process p = ProcessTools.startProcess(
            "jcmd",
            new ProcessBuilder(l.getCommand()),
            line -> {
                if (line.contains("BindException") ||
                    line.contains(Agent.getText(AgentConfigurationError.CONNECTOR_SERVER_IO_ERROR))) {
                    portUnavailable.set(true);
                } else {
                    output.append(line).append('\n');
                    c.accept(line);
                }
            }
        );

        p.waitFor();
        dbg_print("[jcmd] --------");
        if (portUnavailable.get()) {
            String cmd = Arrays.asList(l.getCommand()).stream()
                    .collect(
                            Collectors.joining(" ", "", ": Unable to bind address")
                    );
            throw new BindException(cmd);
        }

        return output.toString();
    }

    private void dbg_print(String msg) {
        if (verbose) {
            System.out.println("DBG: " + msg);
        }
    }
}
