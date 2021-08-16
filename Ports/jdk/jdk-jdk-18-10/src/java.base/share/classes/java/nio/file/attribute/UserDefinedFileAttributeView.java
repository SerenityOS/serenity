/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file.attribute;

import java.nio.ByteBuffer;
import java.util.List;
import java.io.IOException;

/**
 * A file attribute view that provides a view of a file's user-defined
 * attributes, sometimes known as <em>extended attributes</em>. User-defined
 * file attributes are used to store metadata with a file that is not meaningful
 * to the file system. It is primarily intended for file system implementations
 * that support such a capability directly but may be emulated. The details of
 * such emulation are highly implementation specific and therefore not specified.
 *
 * <p> This {@code FileAttributeView} provides a view of a file's user-defined
 * attributes as a set of name/value pairs, where the attribute name is
 * represented by a {@code String}. An implementation may require to encode and
 * decode from the platform or file system representation when accessing the
 * attribute. The value has opaque content. This attribute view defines the
 * {@link #read read} and {@link #write write} methods to read the value into
 * or write from a {@link ByteBuffer}. This {@code FileAttributeView} is not
 * intended for use where the size of an attribute value is larger than {@link
 * Integer#MAX_VALUE}.
 *
 * <p> User-defined attributes may be used in some implementations to store
 * security related attributes so consequently, in the case of the default
 * provider at least, all methods that access user-defined attributes require the
 * {@code RuntimePermission("accessUserDefinedAttributes")} permission when a
 * security manager is installed.
 *
 * <p> The {@link java.nio.file.FileStore#supportsFileAttributeView
 * supportsFileAttributeView} method may be used to test if a specific {@link
 * java.nio.file.FileStore FileStore} supports the storage of user-defined
 * attributes.
 *
 * <p> Where dynamic access to file attributes is required, the {@link
 * java.nio.file.Files#getAttribute getAttribute} method may be used to read
 * the attribute value. The attribute value is returned as a byte array (byte[]).
 * The {@link java.nio.file.Files#setAttribute setAttribute} method may be used
 * to write the value of a user-defined attribute from a buffer (as if by
 * invoking the {@link #write write} method), or byte array (byte[]).
 *
 * @since 1.7
 */

public interface UserDefinedFileAttributeView
    extends FileAttributeView
{
    /**
     * Returns the name of this attribute view. Attribute views of this type
     * have the name {@code "user"}.
     */
    @Override
    String name();

    /**
     * Returns a list containing the names of the user-defined attributes.
     *
     * @return  An unmodifiable list containing the names of the file's
     *          user-defined
     *
     * @throws  IOException
     *          If an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, and it denies {@link
     *          RuntimePermission}{@code ("accessUserDefinedAttributes")}
     *          or its {@link SecurityManager#checkRead(String) checkRead} method
     *          denies read access to the file.
     */
    List<String> list() throws IOException;

    /**
     * Returns the size of the value of a user-defined attribute.
     *
     * @param   name
     *          The attribute name
     *
     * @return  The size of the attribute value, in bytes.
     *
     * @throws  ArithmeticException
     *          If the size of the attribute is larger than {@link Integer#MAX_VALUE}
     * @throws  IOException
     *          If an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, and it denies {@link
     *          RuntimePermission}{@code ("accessUserDefinedAttributes")}
     *          or its {@link SecurityManager#checkRead(String) checkRead} method
     *          denies read access to the file.
     */
    int size(String name) throws IOException;

    /**
     * Read the value of a user-defined attribute into a buffer.
     *
     * <p> This method reads the value of the attribute into the given buffer
     * as a sequence of bytes, failing if the number of bytes remaining in
     * the buffer is insufficient to read the complete attribute value. The
     * number of bytes transferred into the buffer is {@code n}, where {@code n}
     * is the size of the attribute value. The first byte in the sequence is at
     * index {@code p} and the last byte is at index {@code p + n - 1}, where
     * {@code p} is the buffer's position. Upon return the buffer's position
     * will be equal to {@code p + n}; its limit will not have changed.
     *
     * <p> <b>Usage Example:</b>
     * Suppose we want to read a file's MIME type that is stored as a user-defined
     * attribute with the name "{@code user.mimetype}".
     * <pre>
     *    UserDefinedFileAttributeView view =
     *        Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
     *    String name = "user.mimetype";
     *    ByteBuffer buf = ByteBuffer.allocate(view.size(name));
     *    view.read(name, buf);
     *    buf.flip();
     *    String value = Charset.defaultCharset().decode(buf).toString();
     * </pre>
     *
     * @param   name
     *          The attribute name
     * @param   dst
     *          The destination buffer
     *
     * @return  The number of bytes read, possibly zero
     *
     * @throws  IllegalArgumentException
     *          If the destination buffer is read-only
     * @throws  IOException
     *          If an I/O error occurs or there is insufficient space in the
     *          destination buffer for the attribute value
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, and it denies {@link
     *          RuntimePermission}{@code ("accessUserDefinedAttributes")}
     *          or its {@link SecurityManager#checkRead(String) checkRead} method
     *          denies read access to the file.
     *
     * @see #size
     */
    int read(String name, ByteBuffer dst) throws IOException;

    /**
     * Writes the value of a user-defined attribute from a buffer.
     *
     * <p> This method writes the value of the attribute from a given buffer as
     * a sequence of bytes. The size of the value to transfer is {@code r},
     * where {@code r} is the number of bytes remaining in the buffer, that is
     * {@code src.remaining()}. The sequence of bytes is transferred from the
     * buffer starting at index {@code p}, where {@code p} is the buffer's
     * position. Upon return, the buffer's position will be equal to {@code
     * p + n}, where {@code n} is the number of bytes transferred; its limit
     * will not have changed.
     *
     * <p> If an attribute of the given name already exists then its value is
     * replaced. If the attribute does not exist then it is created. If it
     * implementation specific if a test to check for the existence of the
     * attribute and the creation of attribute are atomic with respect to other
     * file system activities.
     *
     * <p> Where there is insufficient space to store the attribute, or the
     * attribute name or value exceed an implementation specific maximum size
     * then an {@code IOException} is thrown.
     *
     * <p> <b>Usage Example:</b>
     * Suppose we want to write a file's MIME type as a user-defined attribute:
     * <pre>
     *    UserDefinedFileAttributeView view =
     *        Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
     *    view.write("user.mimetype", Charset.defaultCharset().encode("text/html"));
     * </pre>
     *
     * @param   name
     *          The attribute name
     * @param   src
     *          The buffer containing the attribute value
     *
     * @return  The number of bytes written, possibly zero
     *
     * @throws  IOException
     *          If an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, and it denies {@link
     *          RuntimePermission}{@code ("accessUserDefinedAttributes")}
     *          or its {@link SecurityManager#checkWrite(String) checkWrite}
     *          method denies write access to the file.
     */
    int write(String name, ByteBuffer src) throws IOException;

    /**
     * Deletes a user-defined attribute.
     *
     * @param   name
     *          The attribute name
     *
     * @throws  IOException
     *          If an I/O error occurs or the attribute does not exist
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, and it denies {@link
     *          RuntimePermission}{@code ("accessUserDefinedAttributes")}
     *          or its {@link SecurityManager#checkWrite(String) checkWrite}
     *          method denies write access to the file.
     */
    void delete(String name) throws IOException;
}
