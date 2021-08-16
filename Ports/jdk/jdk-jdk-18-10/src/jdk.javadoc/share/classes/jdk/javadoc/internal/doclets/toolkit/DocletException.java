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

package jdk.javadoc.internal.doclets.toolkit;


/**
 * Supertype for all checked doclet exceptions.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 *
 * @apiNote This is primarily intended for the benefit of the builder API
 *  in {@code jdk.javadoc.internal.doclets.toolkit.builders}.
 */
public class DocletException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a DocletException with a given detail message.
     *
     * The message may or may not be intended for presentation to the end user.
     *
     * @param message the detail message.
     */
    protected DocletException(String message) {
        super(message);
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Creates a DocletException with a given detail message and underlying cause.
     *
     * The message may or may not be intended for presentation to the end user.
     *
     * @param message the detail message.
     * @param cause the underlying cause
     */
    protected DocletException(String message, Throwable cause) {
        super(message, cause);
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException();
        }
    }
}
