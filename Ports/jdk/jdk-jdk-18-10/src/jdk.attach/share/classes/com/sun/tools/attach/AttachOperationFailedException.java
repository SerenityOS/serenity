/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.attach;

import java.io.IOException;

/**
 * Exception type to signal that an attach operation failed in the target VM.
 *
 * <p> This exception can be thrown by the various operations of
 * {@link com.sun.tools.attach.VirtualMachine} when the operation
 * fails in the target VM. If there is a communication error,
 * a regular IOException will be thrown.
 *
 * @since 9
 */
public class AttachOperationFailedException extends IOException {

    private static final long serialVersionUID = 2140308168167478043L;

    /**
     * Constructs an <code>AttachOperationFailedException</code> with
     * the specified detail message.
     *
     * @param message the detail message.
     */
    public AttachOperationFailedException(String message) {
        super(message);
    }
}
