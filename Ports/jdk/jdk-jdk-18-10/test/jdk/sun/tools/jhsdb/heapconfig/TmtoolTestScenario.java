/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.SA.SATestUtils;
import jdk.test.lib.Utils;

public class TmtoolTestScenario {

    private final ArrayList<String> toolOutput = new ArrayList<String>();
    private LingeredApp theApp = null;
    private final String toolName;
    private final String[] toolArgs;

    /**
     *  @param toolName - name of tool to test
     *  @param toolArgs - tool arguments
     *  @return the object
     */
    public static TmtoolTestScenario create(String toolName, String... toolArgs) {
        return new TmtoolTestScenario(toolName, toolArgs);
    }

    /**
     * @return STDOUT of tool
     */
    public List<String> getToolOutput() {
        return toolOutput;
    }

    /**
     *
     * @return STDOUT of test app
     */
    public List<String> getAppOutput() {
        return theApp.getOutput().getStdoutAsList();
    }

    /**
     * @return Value of the app output with -XX:+PrintFlagsFinal as a map.
     */
    public Map<String, String> parseFlagsFinal() {
        List<String> astr = getAppOutput();
        Map<String, String> vmMap = new HashMap<String, String>();

        for (String line : astr) {
            String[] lv = line.trim().split("\\s+");
            try {
                vmMap.put(lv[1], lv[3]);
            } catch (ArrayIndexOutOfBoundsException ex) {
                // ignore mailformed lines
            }
        }
        return vmMap;
    }

    /**
     *
     * @param vmArgs  - vm and java arguments to launch test app
     * @return exit code of tool
     */
    public int launch(String... vmArgs) {
        System.out.println("Starting LingeredApp");
        try {
            try {
                List<String> vmArgsExtended = new ArrayList<String>();
                vmArgsExtended.add("-XX:+UsePerfData");
                Collections.addAll(vmArgsExtended, vmArgs);
                theApp = new LingeredApp();
                LingeredApp.startAppExactJvmOpts(theApp, vmArgsExtended.toArray(new String[0]));

                System.out.println("Starting " + toolName + " against " + theApp.getPid());
                JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("jhsdb");
                launcher.addToolArg(toolName);

                for (String cmd : toolArgs) {
                    launcher.addToolArg(cmd);
                }
                launcher.addToolArg("--pid");
                launcher.addToolArg(Long.toString(theApp.getPid()));

                ProcessBuilder processBuilder = SATestUtils.createProcessBuilder(launcher);
                processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
                Process toolProcess = processBuilder.start();

                // By default child process output stream redirected to pipe, so we are reading it in foreground.
                BufferedReader reader = new BufferedReader(new InputStreamReader(toolProcess.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    toolOutput.add(line.trim());
                }

                toolProcess.waitFor();

                return toolProcess.exitValue();
            } finally {
                LingeredApp.stopApp(theApp);
            }
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        }
    }

    private TmtoolTestScenario(String toolName, String[] toolArgs) {
        this.toolName = toolName;
        this.toolArgs = toolArgs;
    }

}
