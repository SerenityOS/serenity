/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.rmi.server;

/**
 * An <code>Operation</code> contains a description of a Java method.
 * <code>Operation</code> objects were used in JDK1.1 version stubs and
 * skeletons. The <code>Operation</code> class is not needed for 1.2 style
 * stubs; hence, this class is deprecated.
 *
 * @since 1.1
 * @deprecated no replacement
 */
@Deprecated
public class Operation {
    private String operation;

    /**
     * Creates a new Operation object.
     * @param op method name
     * @deprecated no replacement
     * @since 1.1
     */
    @Deprecated
    public Operation(String op) {
        operation = op;
    }

    /**
     * Returns the name of the method.
     * @return method name
     * @deprecated no replacement
     * @since 1.1
     */
    @Deprecated
    public String getOperation() {
        return operation;
    }

    /**
     * Returns the string representation of the operation.
     * @deprecated no replacement
     * @since 1.1
     */
    @Deprecated
    public String toString() {
        return operation;
    }
}
