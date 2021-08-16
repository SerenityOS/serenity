/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.tool;

import jdk.javadoc.internal.tool.Main.Result;

/**
 * Provides a mechanism for the javadoc tool to terminate execution.
 * This class is constructed with a result and an error message,
 * that can be printed out before termination, a cause can also
 * be wrapped to supply extended information about the exception.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */

class ToolException extends Exception {

    private static final long serialVersionUID = 0;

    final String message;

    final Result result;

    /**
     * Constructs an object containing a result and a message to be
     * printed out by the catcher.
     * @param result the exit code
     * @param message the detailed message
     */
    ToolException(Result result, String message) {
        this.message = message;
        this.result = result;
        if (result == null || result.isOK() || message == null) {
            throw new AssertionError("result == null || result.isOK() || message == null");
        }
    }

    /**
     * Constructs an object containing a result, a messages and an underlying cause.
     * @param result the exit code
     * @param message the detailed message
     * @param cause the underlying cause
     */
    ToolException(Result result, String message, Throwable cause) {
        super(cause);
        this.message = message;
        this.result = result;
        if (result == null || message == null || cause == null || result.isOK()) {
            throw new AssertionError("result == null || message == null"
                    + " || cause == null || result.isOK()");
        }
    }
}
