/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.invoke;

/**
 * Thrown to indicate that code has attempted to call a method handle
 * via the wrong method type.  As with the bytecode representation of
 * normal Java method calls, method handle calls are strongly typed
 * to a specific type descriptor associated with a call site.
 * <p>
 * This exception may also be thrown when two method handles are
 * composed, and the system detects that their types cannot be
 * matched up correctly.  This amounts to an early evaluation
 * of the type mismatch, at method handle construction time,
 * instead of when the mismatched method handle is called.
 *
 * @author John Rose, JSR 292 EG
 * @since 1.7
 */
public class WrongMethodTypeException extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 292L;

    /**
     * Constructs a {@code WrongMethodTypeException} with no detail message.
     */
    public WrongMethodTypeException() {
        super();
    }

    /**
     * Constructs a {@code WrongMethodTypeException} with the specified
     * detail message.
     *
     * @param s the detail message.
     */
    public WrongMethodTypeException(String s) {
        super(s);
    }

    /**
     * Constructs a {@code WrongMethodTypeException} with the specified
     * detail message and cause.
     *
     * @param s the detail message.
     * @param cause the cause of the exception, or null.
     */
    //FIXME: make this public in MR1
    /*non-public*/
    WrongMethodTypeException(String s, Throwable cause) {
        super(s, cause);
    }

    /**
     * Constructs a {@code WrongMethodTypeException} with the specified
     * cause.
     *
     * @param cause the cause of the exception, or null.
     */
    //FIXME: make this public in MR1
    /*non-public*/
    WrongMethodTypeException(Throwable cause) {
        super(cause);
    }
}
