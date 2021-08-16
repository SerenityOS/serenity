/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.jshell.execution;

/**
 * Communication constants shared between the main process and the remote
 * execution process.  These are not enums to allow for future expansion, and
 * remote/local of different versions.
 *
 * @author Robert Field
 */
class RemoteCodes {

    /**
     * Command prefix markers.
     */
    static final int COMMAND_PREFIX = 0xC03DC03D;

    // Command codes

    /**
     * Exit the the agent.
     */
    static final String CMD_CLOSE          = "CMD_CLOSE";
    /**
     * Load classes.
     */
    static final String CMD_LOAD           = "CMD_LOAD";
    /**
     * Redefine classes.
     */
    static final String CMD_REDEFINE       = "CMD_REDEFINE";
    /**
     * Invoke a method.
     */
    static final String CMD_INVOKE         = "CMD_INVOKE";
    /**
     * Retrieve the value of a variable.
     */
    static final String CMD_VAR_VALUE      = "CMD_VAR_VALUE";
    /**
     * Add to the class-path.
     */
    static final String CMD_ADD_CLASSPATH  = "CMD_ADD_CLASSPATH";
    /**
     * Stop an invoke.
     */
    static final String CMD_STOP           = "CMD_STOP";

    // Return result codes

    /**
     * The command succeeded.
     */
    static final int RESULT_SUCCESS                 = 100;
    /**
     * Unbidden execution engine termination.
     */
    static final int RESULT_TERMINATED              = 101;
    /**
     * Command not implemented.
     */
    static final int RESULT_NOT_IMPLEMENTED         = 102;
    /**
     * The command failed.
     */
    static final int RESULT_INTERNAL_PROBLEM        = 103;
    /**
     * User exception encountered. Legacy and used within RESULT_USER_EXCEPTION_CHAINED
     */
    static final int RESULT_USER_EXCEPTION          = 104;
    /**
     * Corralled code exception encountered.
     */
    static final int RESULT_CORRALLED               = 105;
    /**
     * Exception encountered during class load/redefine.
     */
    static final int RESULT_CLASS_INSTALL_EXCEPTION = 106;
    /**
     * The invoke has been stopped.
     */
    static final int RESULT_STOPPED                 = 107;
    /**
     * User exception encountered.
     * @since 11
     */
    static final int RESULT_USER_EXCEPTION_CHAINED  = 108;
}
