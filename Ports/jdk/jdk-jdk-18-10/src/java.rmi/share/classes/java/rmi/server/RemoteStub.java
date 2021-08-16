/*
 * Copyright (c) 1996, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * The {@code RemoteStub} class is the common superclass of
 * statically generated client
 * stubs and provides the framework to support a wide range of remote
 * reference semantics.  Stub objects are surrogates that support
 * exactly the same set of remote interfaces defined by the actual
 * implementation of the remote object.
 *
 * @author  Ann Wollrath
 * @since   1.1
 *
 * @deprecated Statically generated stubs are deprecated, since
 * stubs are generated dynamically. See {@link UnicastRemoteObject}
 * for information about dynamic stub generation.
 */
@Deprecated
abstract public class RemoteStub extends RemoteObject {

    /** indicate compatibility with JDK 1.1.x version of class */
    private static final long serialVersionUID = -1585587260594494182L;

    /**
     * Constructs a {@code RemoteStub}.
     */
    protected RemoteStub() {
        super();
    }

    /**
     * Constructs a {@code RemoteStub} with the specified remote
     * reference.
     *
     * @param ref the remote reference
     * @since 1.1
     */
    protected RemoteStub(RemoteRef ref) {
        super(ref);
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     *
     * @param stub the remote stub
     * @param ref the remote reference
     * @throws UnsupportedOperationException always
     * @since 1.1
     * @deprecated No replacement.  The {@code setRef} method
     * was intended for setting the remote reference of a remote
     * stub. This is unnecessary, since {@code RemoteStub}s can be created
     * and initialized with a remote reference through use of
     * the {@link #RemoteStub(RemoteRef)} constructor.
     */
    @Deprecated
    protected static void setRef(RemoteStub stub, RemoteRef ref) {
        throw new UnsupportedOperationException();
    }
}
