/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jdb;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.ArgumentHandler;

import java.io.*;
import java.util.*;

/**
 * Parser for <code>jdb</code> test's specific command-line arguments.
 * This class also parses JDI specific command-line arguments.
 * <p>
 * <code>JdbArgumentHandler</code> handles <code>jdb</code> test's specific
 * arguments related to launching of <code>jdb</code> to debugged VM
 * in addition to general arguments recognized by <code>ArgumentHandler</code>,
 * <code>DebugeeArgumentHandler</code> and <code>ArgumentParser</code>.
 * <p>
 * Following is the list of specific options recognized by <code>AgrumentHandler</code>:
 * <ul>
 * <li> <code>-jdb=JAVA_SDK_HOME_PATH/bin/$JDB</code> -                                        <br>
 *   full path to <code>jdb</code> binaries. There is no default value.                        <br>
 * <li> <code>-workdir=CURRENT_TEST_DIRECTORY_PATH</code> -
 *   full path to current test directory. There is no default value.
 * <li> <code>-jdb.option=JDB_COMMAND_LINE_OPTIONS</code> -
 *   jdb options (see <code>jdb -help</code>) except these ones:
 *   <ul>
 *      <li><code>-attach [address] </code></li>
 *      <li><code>-listen [address] </code></li>
 *      <li><code>-connect [connector-name]:[name1]=[value1],... </code></li>
 *   </ul>
 * </ul>
 * <p>
 * See also list of arguments recognized by the base <code>ArgumentHandler</code>,
 * <code>DebugeeArgumentHandler</code> and <code>ArgumentParser</code> classes.
 * <p>
 * See also description of <code>ArgumentParser</code> how to work with
 * command line arguments and options.
 *
 * @see nsk.share.ArgumentParser
 * @see nsk.share.jpda.DebugeeArgumentHandler
 * @see nsk.share.jdi.ArgumentHandler
 */
public class JdbArgumentHandler extends nsk.share.jdi.ArgumentHandler {

    /**
     * Keep a copy of raw command-line arguments and parse them;
     * but throw an exception on parsing error.
     *
     * @param  args  Array of the raw command-line arguments.
     *
     * @throws  NullPointerException  If <code>args==null</code>.
     * @throws  IllegalArgumentException  If Binder or Log options
     *                                    are set incorrectly.
     *
     * @see #setRawArguments(String[])
     */
    public JdbArgumentHandler(String args[]) {
        super(args);
    }


    /**
     * Checks if an option is admissible and has proper value.
     * This method is invoked by <code>parseArguments()</code>
     *
     * @param option option name
     * @param value string representation of value (could be an empty string)
     *              null if this option has no value
     * @return <i>true</i> if option is admissible and has proper value
     *         <i>false</i> if option is not admissible
     *
     * @throws <i>BadOption</i> if option has illegal value
     *
     * @see #parseArguments()
     */
    protected boolean checkOption(String option, String value) {

        if (option.equals("jdb")) {
            if (value == null) {
                throw new BadOption(option + ": value must be not null");
            }
            return true;
        }

        if (option.equals("workdir")) {
            if (value == null) {
                throw new BadOption(option + ": value must be not null");
            }
            return true;
        }

        if (option.equals("java.options")) {
            return true;
        }

        if (option.equals("jdb.option")) {
            if (value != null) {
                if (value.indexOf("-attach") > 0) {
                    throw new BadOption("jdb option -attach is not admissible: " + value);
                }
                if (value.indexOf("-listen") > 0) {
                    throw new BadOption("jdb option -listen is not admissible: "  + value);
                }
                if (value.indexOf("-connect") > 0) {
                    throw new BadOption("jdb option -connect is not admissible: " + value);
                }
                if (value.indexOf("-help") > 0) {
                    throw new BadOption("jdb option -help is not admissible: " + value);
                }
                if (value.indexOf("-listenany") > 0) {
                    throw new BadOption("jdb option -listenany is not admissible: " + value);
                }
                if (value.indexOf("-launch") > 0) {
                    throw new BadOption("jdb option -launch is not admissible: " + value);
                }
                if (value.indexOf("-tclient") > 0) {
                    throw new BadOption("jdb option -tclient is not admissible: " + value);
                }
                if (value.indexOf("-tserver") > 0) {
                    throw new BadOption("jdb option -tserver is not admissible: " + value);
                }
            }
            return true;
        }

        return super.checkOption(option, value);
    }

    /**
     * Checks options against inconcistence.
     * This method is invoked by <code>parseArguments()</code>
     *
     * @see #parseArguments()
     */
    protected void checkOptions() {

        super.checkOptions();
    }

    /**
     * Returns the path to current test directory.
     */

    public String getWorkDir() {
        return options.getProperty("workdir", "");
    }

    /**
     * Return sfull path to jdb executable.
     *
     */
    public String getJdbExecPath() {
        return options.getProperty("jdb");
    }

    /**
     * Returns command line options <code>jdb</code> was launched with.
     */
    public String getJdbOptions() {
        String value = options.getProperty("jdb.option", "").trim();
        if (value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    /**
     * Adds "<code>-J</code>" prefix to each Java option, so that
     * <code>jdb</code> could apply them to the target Java VM.
     */
    public static List<String> enwrapJavaOptions(String javaOptions) {
        List<String> result = new ArrayList<String>();
        for (String option : javaOptions.split("\\s+"))
            if (option.length() > 0)
                result.add("-J" + option);
        return result;
    }

    /**
     * Returns adjusted additional options for debuggee VM with launching connector.
     */
    public String getDebuggeeOptions() {
        StringBuilder sb = new StringBuilder();
        String value = options.getProperty("debugee.vmkeys", "").trim();
        if (value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        for (String option : value.split("\\s+")) {
            if (option.length() > 0) {
                sb.append(checkAndQuote(option, ","));
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    /**
     * Returns options for debugger VM.
     */
    public String getJavaOptions() {
        String value = options.getProperty("java.options", "").trim();
        if (value.length() > 1 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    /**
     * Remove "<code>-server</code>" or "<code>-client</code>" from options string,
     * if anything of them are presented.
     */
    public static String removeVMFlavorOption(String javaOptions) {
        StringBuffer result = new StringBuffer();
        StringTokenizer tokenizer = new StringTokenizer(javaOptions);
        while (tokenizer.hasMoreTokens()) {
            String option = tokenizer.nextToken();
            if (!option.equals("-server") && !option.equals("-client")) {
                result.append( (result.length() > 0 ? " " : "") + option);
            }
        };
        return result.toString();
    }

    /**
     * @return "<code>-tserver</code>" if "<code>-server</code>" is presented in options string.
     * @return "<code>-tclient</code>" if "<code>-client</code>" is presented in options string.
     * @return empty string otherwise.
     */
    public static String stripVMFlavorOption(String javaOptions) {
        String result = "";
        StringTokenizer tokenizer = new StringTokenizer(javaOptions);
        while (tokenizer.hasMoreTokens()) {
            String option = tokenizer.nextToken();
            if (option.equals("-server")) {
                result = "-tserver";
                break;
            } else if (option.equals("-client")) {
                result = "-tclient";
                break;
            }
        };
        return result;
    }

    // check if target substring exists and quote value if needed
    // ex for subString == "," and value == disk=true,dumponexit=true
    // return 'disk=true,dumponexit=true'
    private static String checkAndQuote(String value, String subString) {
        if (NO_SUBSTR_MATCH == value.indexOf(subString)) {
            // return as-is
            return value;
        }
        // already single quoted 'value' ?
        if (isEnclosed(value, "'")) {
            return value;
        }
        // already double quoted "value" ?
        if (isEnclosed(value, "\"")) {
            // change to 'value'
            return value.replaceAll("\"", "'");
        }
        // else single quote the value
        return "'" + value + "'";
    }

    private static boolean isEnclosed(String value,
                                      String enclosingChar) {
        int firstEnclosePos = value.indexOf(enclosingChar);
        if (0 == firstEnclosePos) {
            int lastEnclosePos = value.lastIndexOf(enclosingChar);
            if (lastEnclosePos > firstEnclosePos && lastEnclosePos == value.length() - 1) {
                //already quoted
                return true;
            }
            //only a single quote? subString outside quotes? Wrongly quoted, needs fix
            throw new BadOption(value +  " not correctly quoted");
        }
        return false;
    }

    private static final int NO_SUBSTR_MATCH = -1;
}
