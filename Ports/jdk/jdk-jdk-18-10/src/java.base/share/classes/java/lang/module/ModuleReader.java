/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.module;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * Provides access to the content of a module.
 *
 * <p> A module reader is intended for cases where access to the resources in a
 * module is required, regardless of whether the module has been loaded.
 * A framework that scans a collection of packaged modules on the file system,
 * for example, may use a module reader to access a specific resource in each
 * module. A module reader is also intended to be used by {@code ClassLoader}
 * implementations that load classes and resources from modules. </p>
 *
 * <p> A resource in a module is identified by an abstract name that is a
 * '{@code /}'-separated path string. For example, module {@code java.base} may
 * have a resource "{@code java/lang/Object.class}" that, by convention, is the
 * class file for {@code java.lang.Object}. A module reader may treat
 * directories in the module content as resources (whether it does or not is
 * module reader specific). Where the module content contains a directory
 * that can be located as a resource then its name ends with a slash ('/'). The
 * directory can also be located with a name that drops the trailing slash. </p>
 *
 * <p> A {@code ModuleReader} is {@linkplain ModuleReference#open open} upon
 * creation and is closed by invoking the {@link #close close} method.  Failure
 * to close a module reader may result in a resource leak.  The {@code
 * try-with-resources} statement provides a useful construct to ensure that
 * module readers are closed. </p>
 *
 * <p> A {@code ModuleReader} implementation may require permissions to access
 * resources in the module. Consequently the {@link #find find}, {@link #open
 * open}, {@link #read read}, and {@link #list list} methods may throw {@code
 * SecurityException} if access is denied by the security manager. </p>
 *
 * @implSpec Implementations of {@code ModuleReader} should take great care
 * when translating an abstract resource name to the location of a resource in
 * a packaged module or on the file system. Implementations are advised to
 * treat resource names with elements such as '{@code .},  '{@code ..}',
 * elements containing file separators, or empty elements as "not found". More
 * generally, if the resource name is not in the stream of elements that the
 * {@code list} method returns then the resource should be treated as "not
 * found" to avoid inconsistencies.
 *
 * @see ModuleReference
 * @since 9
 */

public interface ModuleReader extends Closeable {

    /**
     * Finds a resource, returning a URI to the resource in the module.
     *
     * <p> If the module reader can determine that the name locates a directory
     * then the resulting URI will end with a slash ('/'). </p>
     *
     * @param  name
     *         The name of the resource to open for reading
     *
     * @return A URI to the resource; an empty {@code Optional} if the resource
     *         is not found or a URI cannot be constructed to locate the
     *         resource
     *
     * @throws IOException
     *         If an I/O error occurs or the module reader is closed
     * @throws SecurityException
     *         If denied by the security manager
     *
     * @see ClassLoader#getResource(String)
     */
    Optional<URI> find(String name) throws IOException;

    /**
     * Opens a resource, returning an input stream to read the resource in
     * the module.
     *
     * <p> The behavior of the input stream when used after the module reader
     * is closed is implementation specific and therefore not specified. </p>
     *
     * @implSpec The default implementation invokes the {@link #find(String)
     * find} method to get a URI to the resource. If found, then it attempts
     * to construct a {@link java.net.URL URL} and open a connection to the
     * resource.
     *
     * @param  name
     *         The name of the resource to open for reading
     *
     * @return An input stream to read the resource or an empty
     *         {@code Optional} if not found
     *
     * @throws IOException
     *         If an I/O error occurs or the module reader is closed
     * @throws SecurityException
     *         If denied by the security manager
     */
    default Optional<InputStream> open(String name) throws IOException {
        Optional<URI> ouri = find(name);
        if (ouri.isPresent()) {
            return Optional.of(ouri.get().toURL().openStream());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Reads a resource, returning a byte buffer with the contents of the
     * resource.
     *
     * The element at the returned buffer's position is the first byte of the
     * resource, the element at the buffer's limit is the last byte of the
     * resource. Once consumed, the {@link #release(ByteBuffer) release} method
     * must be invoked. Failure to invoke the {@code release} method may result
     * in a resource leak.
     *
     * @apiNote This method is intended for high-performance class loading. It
     * is not capable (or intended) to read arbitrary large resources that
     * could potentially be 2GB or larger. The rationale for using this method
     * in conjunction with the {@code release} method is to allow module reader
     * implementations manage buffers in an efficient manner.
     *
     * @implSpec The default implementation invokes the {@link #open(String)
     * open} method and reads all bytes from the input stream into a byte
     * buffer.
     *
     * @param  name
     *         The name of the resource to read
     *
     * @return A byte buffer containing the contents of the resource or an
     *         empty {@code Optional} if not found
     *
     * @throws IOException
     *         If an I/O error occurs or the module reader is closed
     * @throws SecurityException
     *         If denied by the security manager
     * @throws OutOfMemoryError
     *         If the resource is larger than {@code Integer.MAX_VALUE},
     *         the maximum capacity of a byte buffer
     *
     * @see ClassLoader#defineClass(String, ByteBuffer, java.security.ProtectionDomain)
     */
    default Optional<ByteBuffer> read(String name) throws IOException {
        Optional<InputStream> oin = open(name);
        if (oin.isPresent()) {
            try (InputStream in = oin.get()) {
                return Optional.of(ByteBuffer.wrap(in.readAllBytes()));
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * Releases a byte buffer. This method should be invoked after consuming
     * the contents of the buffer returned by the {@code read} method.
     * The behavior of this method when invoked to release a buffer that has
     * already been released, or the behavior when invoked to release a buffer
     * after a {@code ModuleReader} is closed is implementation specific and
     * therefore not specified.
     *
     * @param  bb
     *         The byte buffer to release
     *
     * @implSpec The default implementation doesn't do anything except check
     * if the byte buffer is null.
     */
    default void release(ByteBuffer bb) {
        Objects.requireNonNull(bb);
    }

    /**
     * Lists the contents of the module, returning a stream of elements that
     * are the names of all resources in the module. Whether the stream of
     * elements includes names corresponding to directories in the module is
     * module reader specific.
     *
     * <p> In lazy implementations then an {@code IOException} may be thrown
     * when using the stream to list the module contents. If this occurs then
     * the {@code IOException} will be wrapped in an {@link
     * java.io.UncheckedIOException} and thrown from the method that caused the
     * access to be attempted. {@code SecurityException} may also be thrown
     * when using the stream to list the module contents and access is denied
     * by the security manager. </p>
     *
     * <p> The behavior of the stream when used after the module reader is
     * closed is implementation specific and therefore not specified. </p>
     *
     * @return A stream of elements that are the names of all resources
     *         in the module
     *
     * @throws IOException
     *         If an I/O error occurs or the module reader is closed
     * @throws SecurityException
     *         If denied by the security manager
     */
    Stream<String> list() throws IOException;

    /**
     * Closes the module reader. Once closed then subsequent calls to locate or
     * read a resource will fail by throwing {@code IOException}.
     *
     * <p> A module reader is not required to be asynchronously closeable. If a
     * thread is reading a resource and another thread invokes the close method,
     * then the second thread may block until the read operation is complete. </p>
     */
    @Override
    void close() throws IOException;

}
