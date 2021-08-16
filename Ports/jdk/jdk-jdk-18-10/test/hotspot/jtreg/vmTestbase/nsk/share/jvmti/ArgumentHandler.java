/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jvmti;

import nsk.share.ArgumentParser;
import nsk.share.Failure;

import java.util.StringTokenizer;

/**
 * Parser for JVMTI test's specific command-line arguments.
 * <p>
 * <code>ArgumentHandler</code> handles JVMTI test's specific arguments
 * in addition to general arguments recognized by <code>ArgumentParser</code>.
 * In addition to arguments obtained from command line, it also obtains options
 * form agent option string. These options are added to the options list
 * and can be found using <i>FindOption*Value()</i> methods.
 * <p>
 * Currently no specific options are recognized by <code>ArgumentHandler</code>.
 * <p>
 * See description of <code>ArgumentParser</code> to know which general options
 * are recognized and how to get them.
 *
 * @see ArgumentParser
 * @see #findOptionValue(String)
 * @see #findOptionIntValue(String, int)
 */
public class ArgumentHandler extends ArgumentParser {

    /**
     * Keep a copy of raw command-line arguments and parse them;
     * but throw an exception on parsing error; and also add options
     * obtained from agent options string.
     *
     * @param args Array of the raw command-line arguments.
     * @throws NullPointerException     If <code>args==null</code>.
     * @throws IllegalArgumentException If Binder or Log options
     *                                  are set incorrectly.
     * @see #setRawArguments(String[])
     */
    public ArgumentHandler(String[] args) {
        super(args);
        String agentOptions = getAgentOptionsString();
        if (agentOptions == null) {
            throw new Failure("No agent options obtained from agent library");
        }
        parseOptionString(agentOptions);
    }

    /**
     * Return value of given option if specified; or <i>null</i> otherwise.
     */
    public String findOptionValue(String name) {
        return options.getProperty(name);
    }

    /**
     * Return integer value of given option if specified; or <i>defaultValue</i> otherwise.
     *
     * @throws BadOption if option is specified but has not integer value.
     */
    public int findOptionIntValue(String name, int defaultValue) {
        String value = options.getProperty(name);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BadOption("Not integer value of option: " + name + "=" + value);
        }
    }

    /**
     * Check if an option is admissible and has proper value.
     * This method is invoked by <code>parseArguments()</code>.
     *
     * @param option option name
     * @param value  string representation of value (could be an empty string);
     *               or null if this option has no value
     * @return <i>true</i> if option is admissible and has proper value;
     * <i>false</i> if option is not admissible
     * @throws BadOption if admissible option has illegal value
     * @see #parseArguments()
     */
    protected boolean checkOption(String option, String value) {
        return super.checkOption(option, value);
    }

    /**
     * Check consistence of all options.
     * This method is invoked by <code>parseArguments()</code>.
     *
     * @see #parseArguments()
     */
    protected void checkOptions() {
        super.checkOptions();
    }

    /**
     * Parse options string and add all recognized options and their values.
     * If optionString is <i>null</i> this method just does nothing.
     *
     * @throws BadOption if known option has illegal value
     *                   or all options are inconsistent
     */
    protected void parseOptionString(String optionString) {
        if (optionString == null)
            return;

        StringTokenizer st = new StringTokenizer(optionString, " ,~");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int start = token.startsWith("-") ? 1 : 0;
            String name, value;
            int k = token.indexOf('=');
            if (k < 0) {
                name = token.substring(start);
                value = "";
            } else {
                name = token.substring(start, k);
                value = token.substring(k + 1);
            }
            if (name.length() <= 0) {
                throw new BadOption("Empty option found: " + token);
            }
            checkOption(name, value);
            options.setProperty(name, value);
        }
        checkOptions();
    }

    /**
     * Obtain agent options string form agent native library.
     */
    private native String getAgentOptionsString();
}
