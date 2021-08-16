/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * A Unix domain socket address.
 * A Unix domain socket address encapsulates a file-system path that Unix domain sockets
 * bind or connect to.
 *
 * <p> An <a id="unnamed"></a><i>unnamed</i> {@code UnixDomainSocketAddress} has
 * an empty path. The local address of a {@link SocketChannel} to a Unix domain socket
 * that is <i>automatically</i> or <i>implicitly</i> bound will be unnamed.
 *
 * <p> {@link Path} objects used to create instances of this class must be obtained
 * from the {@linkplain FileSystems#getDefault system-default} file system.
 *
 * @see java.nio.channels.SocketChannel
 * @see java.nio.channels.ServerSocketChannel
 * @since 16
 */
public final class UnixDomainSocketAddress extends SocketAddress {
    @java.io.Serial
    static final long serialVersionUID = 92902496589351288L;

    private final transient Path path;

    /**
     * A serial proxy for all {@link UnixDomainSocketAddress} instances.
     * It captures the file path name and reconstructs using the public static
     * {@link #of(String) factory}.
     *
     * @serial include
     */
    private static final class Ser implements Serializable {
        @java.io.Serial
        static final long serialVersionUID = -7955684448513979814L;

        /**
         * The path name.
         * @serial
         */
        private final String pathname;

        Ser(String pathname) {
            this.pathname = pathname;
        }

        /**
         * Creates a {@link UnixDomainSocketAddress} instance, by an invocation
         * of the {@link #of(String) factory} method passing the path name.
         * @return a UnixDomainSocketAddress
         */
        @java.io.Serial
        private Object readResolve() {
            return UnixDomainSocketAddress.of(pathname);
        }
    }

    /**
     * Returns a
     * <a href="{@docRoot}/serialized-form.html#java.net.UnixDomainSocketAddress.Ser">
     * Ser</a> containing the path name of this instance.
     *
     * @return a {@link Ser} representing the path name of this instance
     *
     * @throws ObjectStreamException if an error occurs
     */
    @java.io.Serial
    private Object writeReplace() throws ObjectStreamException {
        return new Ser(path.toString());
    }

    /**
     * Throws InvalidObjectException, always.
     * @param s the stream
     * @throws java.io.InvalidObjectException always
     */
    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.InvalidObjectException
    {
        throw new java.io.InvalidObjectException("Proxy required");
    }

    /**
     * Throws InvalidObjectException, always.
     * @throws java.io.InvalidObjectException always
     */
    @java.io.Serial
    private void readObjectNoData()
        throws java.io.InvalidObjectException
    {
        throw new java.io.InvalidObjectException("Proxy required");
    }

    private UnixDomainSocketAddress(Path path) {
        this.path = path;
    }

    /**
     * Creates a UnixDomainSocketAddress from the given path string.
     *
     * @param  pathname
     *         The path string, which can be empty
     *
     * @return A UnixDomainSocketAddress
     *
     * @throws InvalidPathException
     *         If the path cannot be converted to a Path
     *
     * @throws NullPointerException if pathname is {@code null}
     */
    public static UnixDomainSocketAddress of(String pathname) {
        return of(Path.of(pathname));
    }

    /**
     * Creates a UnixDomainSocketAddress for the given path.
     *
     * @param  path
     *         The path to the socket, which can be empty
     *
     * @return A UnixDomainSocketAddress
     *
     * @throws IllegalArgumentException
     *         If the path is not associated with the default file system
     *
     * @throws NullPointerException if path is {@code null}
     */
    public static UnixDomainSocketAddress of(Path path) {
        FileSystem fs = path.getFileSystem();
        if (fs != FileSystems.getDefault()) {
            throw new IllegalArgumentException();
        }
        if (fs.getClass().getModule() != Object.class.getModule()) {
            throw new IllegalArgumentException();
        }
        return new UnixDomainSocketAddress(path);
    }

    /**
     * Returns this address's path.
     *
     * @return this address's path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Returns the hash code of this {@code UnixDomainSocketAddress}
     */
    @Override
    public int hashCode() {
        return path.hashCode();
    }

    /**
     * Compares this address with another object.
     *
     * @return true if the path fields are equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UnixDomainSocketAddress that))
            return false;
        return this.path.equals(that.path);
    }

    /**
     * Returns a string representation of this {@code UnixDomainSocketAddress}.
     *
     * @return this address's path which may be empty for an unnamed address
     */
    @Override
    public String toString() {
        return path.toString();
    }
}
