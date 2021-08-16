/*
 * Copyright (c) 1999, 2010, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.util;

/** Throwing an instance of this class causes immediate termination
 *  of the main compiler method.  It is used when some non-recoverable
 *  error has been detected in the compiler environment at runtime.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class FatalError extends Error {
    private static final long serialVersionUID = 0;

    /** Construct a <code>FatalError</code> with the specified detail message.
     *  @param d A diagnostic containing the reason for failure.
     */
    public FatalError(JCDiagnostic d) {
        super(d.toString());
    }

    /** Construct a <code>FatalError</code> with the specified detail message
     * and cause.
     *  @param d A diagnostic containing the reason for failure.
     *  @param t An exception causing the error
     */
    public FatalError(JCDiagnostic d, Throwable t) {
        super(d.toString(), t);
    }

    /** Construct a <code>FatalError</code> with the specified detail message.
     *  @param s An English(!) string describing the failure, typically because
     *           the diagnostic resources are missing.
     */
    public FatalError(String s) {
        super(s);
    }
}
