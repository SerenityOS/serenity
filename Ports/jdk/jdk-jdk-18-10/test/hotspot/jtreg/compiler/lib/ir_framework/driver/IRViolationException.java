/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package compiler.lib.ir_framework.driver;

import compiler.lib.ir_framework.IR;
import compiler.lib.ir_framework.Test;

/**
 * Exception that is thrown if an {@link IR} rule/constraint failed. The exception message contains a detailed list of
 * all failures, including failing method(s), {@code @IR} rule(s) (the first {@code @IR} constraint is rule 1) and the
 * specific regex(es) that could not be matched.
 *
 * @see IR
 * @see Test
 */
public class IRViolationException extends RuntimeException {
    private final String compilations;
    private String exceptionInfo;

    IRViolationException(String message, String compilations) {
        super("There were one or multiple IR rule failures. Please check stderr for more information.");
        this.exceptionInfo = message;
        this.compilations = compilations;
    }

    /**
     * Get some more detailed information about the violated IR rule(s) and how to reproduce it.
     *
     * @return a formatted string containing information about the violated IR rule(s) and how to reproduce it.
     */
    public String getExceptionInfo() {
        return exceptionInfo;
    }

    public String getCompilations() {
        return compilations;
    }

    public void addCommandLine(String commandLine) {
        this.exceptionInfo = commandLine + System.lineSeparator() + exceptionInfo;
    }
}
