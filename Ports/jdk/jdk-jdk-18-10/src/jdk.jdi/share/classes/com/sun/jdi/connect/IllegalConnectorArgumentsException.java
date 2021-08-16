/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdi.connect;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Thrown to indicate an invalid argument or
 * inconsistent passed to a {@link Connector}.
 *
 * @author Gordon Hirsch
 * @since  1.3
 */
public class IllegalConnectorArgumentsException extends Exception {

    private static final long serialVersionUID = -3042212603611350941L;

    @SuppressWarnings("serial") // Conditionally serializable
    List<String> names;

    /**
     * Construct an <code>IllegalConnectorArgumentsException</code>
     * with the specified detail message and the name of the argument
     * which is invalid or inconsistent.
     * @param s the detailed message.
     * @param name the name of the invalid or inconsistent argument.
     */
    public IllegalConnectorArgumentsException(String s, String name) {
        super(s);
        names = new ArrayList<String>(1);
        names.add(name);
    }

    /**
     * Construct an <code>IllegalConnectorArgumentsException</code>
     * with the specified detail message and a <code>List</code> of
     * names of arguments which are invalid or inconsistent.
     * @param s the detailed message.
     * @param names a <code>List</code> containing the names of the
     * invalid or inconsistent argument.
     */
    public IllegalConnectorArgumentsException(String s, List<String> names) {
        super(s);
        this.names = new ArrayList<String>(names);
    }

    /**
     * Return a <code>List</code> containing the names of the
     * invalid or inconsistent arguments.
     * @return a <code>List</code> of argument names.
     */
    public List<String> argumentNames() {
        return Collections.unmodifiableList(names);
    }
}
