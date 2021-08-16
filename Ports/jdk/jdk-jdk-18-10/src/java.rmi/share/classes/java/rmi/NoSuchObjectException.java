/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * A <code>NoSuchObjectException</code> is thrown if an attempt is made to
 * invoke a method on an object that no longer exists in the remote virtual
 * machine.  If a <code>NoSuchObjectException</code> occurs attempting to
 * invoke a method on a remote object, the call may be retransmitted and still
 * preserve RMI's "at most once" call semantics.
 *
 * A <code>NoSuchObjectException</code> is also thrown by the method
 * <code>java.rmi.server.RemoteObject.toStub</code> and by the
 * <code>unexportObject</code> methods of
 * <code>java.rmi.server.UnicastRemoteObject</code>.
 *
 * @author  Ann Wollrath
 * @since   1.1
 * @see     java.rmi.server.RemoteObject#toStub(Remote)
 * @see     java.rmi.server.UnicastRemoteObject#unexportObject(Remote,boolean)
 */
public class NoSuchObjectException extends RemoteException {

    /* indicate compatibility with JDK 1.1.x version of class */
    private static final long serialVersionUID = 6619395951570472985L;

    /**
     * Constructs a <code>NoSuchObjectException</code> with the specified
     * detail message.
     *
     * @param s the detail message
     * @since   1.1
     */
    public NoSuchObjectException(String s) {
        super(s);
    }
}
