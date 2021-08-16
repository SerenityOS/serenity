/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jdwp;

import nsk.share.*;
import nsk.share.jpda.*;

import java.io.*;
import java.util.*;

/**
 * Parser for JDWP test's specific command-line arguments.
 * <p>
 * <code>ArgumentHandler</code> handles JDWP test's specific
 * arguments related to launching debugee VM using JDWP features
 * in addition to general arguments recognized by
 * <code>DebugeeArgumentHandler</code> and <code>ArgumentParser</code>.
 * <p>
 * Following is the list of specific options recognized by
 * <code>AgrumentHandler</code>:
 * <ul>
 * <li> <code>-connector=[attaching|listening]</code> -
 *   JDWP connection type
 * <li> <code>-transport=[socket|shmem]</code> -
 *   JDWP transport kind
 * </ul>
 * <p>
 * See also list of arguments recognized by the base <code>DebugeeArgumnethandler</code>
 * and <code>ArgumentParser</code> classes.
 * <p>
 * See also description of <code>ArgumentParser</code> how to work with
 * command line arguments and options.
 *
 * @see ArgumentParser
 * @see DebugeeArgumentHandler
 */
public class ArgumentHandler extends DebugeeArgumentHandler {

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
    public ArgumentHandler(String args[]) {
        super(args);
    }

    /**
     * Check if an option is admissible and has proper value.
     * This method is invoked by <code>parseArguments()</code>
     *
     * @param option option name
     * @param value string representation of value (could be an empty string)
     *              null if this option has no value
     * @return <i>true</i> if option is admissible and has proper value
     *         <i>false</i> if otion is not admissible
     *
     * @throws <i>BadOption</i> if option has illegal value
     *
     * @see #parseArguments()
     */
    protected boolean checkOption(String option, String value) {
        return super.checkOption(option, value);
    }

    /**
     * Check options against inconcistence.
     * This method is invoked by <code>parseArguments()</code>
     *
     * @see #parseArguments()
     */
    protected void checkOptions() {

        if (isShmemTransport()) {
            throw new BadOption("transport: shared memory transport is not supported yet");
        }

        super.checkOptions();
    }

}
