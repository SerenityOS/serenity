/*
 * Copyright (c) 1996, 1998, Oracle and/or its affiliates. All rights reserved.
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

package java.rmi;

/**
 * An <code>UnmarshalException</code> can be thrown while unmarshalling the
 * parameters or results of a remote method call if any of the following
 * conditions occur:
 * <ul>
 * <li> if an exception occurs while unmarshalling the call header
 * <li> if the protocol for the return value is invalid
 * <li> if a <code>java.io.IOException</code> occurs unmarshalling
 * parameters (on the server side) or the return value (on the client side).
 * <li> if a <code>java.lang.ClassNotFoundException</code> occurs during
 * unmarshalling parameters or return values
 * <li> if no skeleton can be loaded on the server-side; note that skeletons
 * are required in the 1.1 stub protocol, but not in the 1.2 stub protocol.
 * <li> if the method hash is invalid (i.e., missing method).
 * <li> if there is a failure to create a remote reference object for
 * a remote object's stub when it is unmarshalled.
 * </ul>
 *
 * @author  Ann Wollrath
 * @since   1.1
 */
public class UnmarshalException extends RemoteException {

    /* indicate compatibility with JDK 1.1.x version of class */
    private static final long serialVersionUID = 594380845140740218L;

    /**
     * Constructs an <code>UnmarshalException</code> with the specified
     * detail message.
     *
     * @param s the detail message
     * @since 1.1
     */
    public UnmarshalException(String s) {
        super(s);
    }

    /**
     * Constructs an <code>UnmarshalException</code> with the specified
     * detail message and nested exception.
     *
     * @param s the detail message
     * @param ex the nested exception
     * @since 1.1
     */
    public UnmarshalException(String s, Exception ex) {
        super(s, ex);
    }
}
