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

import java.io.IOException;

/**
 * A file attribute view that provides a view of a <em>basic set</em> of file
 * attributes common to many file systems. The basic set of file attributes
 * consist of <em>mandatory</em> and <em>optional</em> file attributes as
 * defined by the {@link BasicFileAttributes} interface.
 *
 * <p> The file attributes are retrieved from the file system as a <em>bulk
 * operation</em> by invoking the {@link #readAttributes() readAttributes} method.
 * This class also defines the {@link #setTimes setTimes} method to update the
 * file's time attributes.
 *
 * <p> Where dynamic access to file attributes is required, the attributes
 * supported by this attribute view have the following names and types:
 * <blockquote>
 *  <table class="striped">
 *  <caption style="display:none">Supported attributes</caption>
 *  <thead>
 *   <tr>
 *     <th scope="col"> Name </th>
 *     <th scope="col"> Type </th>
 *   </tr>
 *  </thead>
 *  <tbody>
 *  <tr>
 *     <th scope="row"> "lastModifiedTime" </th>
 *     <td> {@link FileTime} </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> "lastAccessTime" </th>
 *     <td> {@link FileTime} </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> "creationTime" </th>
 *     <td> {@link FileTime} </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> "size" </th>
 *     <td> {@link Long} </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> "isRegularFile" </th>
 *     <td> {@link Boolean} </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> "isDirectory" </th>
 *     <td> {@link Boolean} </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> "isSymbolicLink" </th>
 *     <td> {@link Boolean} </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> "isOther" </th>
 *     <td> {@link Boolean} </td>
 *   </tr>
 *   <tr>
 *     <th scope="row"> "fileKey" </th>
 *     <td> {@link Object} </td>
 *   </tr>
 * </tbody>
 * </table>
 * </blockquote>
 *
 * <p> The {@link java.nio.file.Files#getAttribute getAttribute} method may be
 * used to read any of these attributes as if by invoking the {@link
 * #readAttributes() readAttributes()} method.
 *
 * <p> The {@link java.nio.file.Files#setAttribute setAttribute} method may be
 * used to update the file's last modified time, last access time or create time
 * attributes as if by invoking the {@link #setTimes setTimes} method.
 *
 * @since 1.7
 */

public interface BasicFileAttributeView
    extends FileAttributeView
{
    /**
     * Returns the name of the attribute view. Attribute views of this type
     * have the name {@code "basic"}.
     */
    @Override
    String name();

    /**
     * Reads the basic file attributes as a bulk operation.
     *
     * <p> It is implementation specific if all file attributes are read as an
     * atomic operation with respect to other file system operations.
     *
     * @return  the file attributes
     *
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, its {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file
     */
    BasicFileAttributes readAttributes() throws IOException;

    /**
     * Updates any or all of the file's last modified time, last access time,
     * and create time attributes.
     *
     * <p> This method updates the file's timestamp attributes. The values are
     * converted to the epoch and precision supported by the file system.
     * Converting from finer to coarser granularities result in precision loss.
     * The behavior of this method when attempting to set a timestamp that is
     * not supported or to a value that is outside the range supported by the
     * underlying file store is not defined. It may or not fail by throwing an
     * {@code IOException}.
     *
     * <p> If any of the {@code lastModifiedTime}, {@code lastAccessTime},
     * or {@code createTime} parameters has the value {@code null} then the
     * corresponding timestamp is not changed. An implementation may require to
     * read the existing values of the file attributes when only some, but not
     * all, of the timestamp attributes are updated. Consequently, this method
     * may not be an atomic operation with respect to other file system
     * operations. Reading and re-writing existing values may also result in
     * precision loss. If all of the {@code lastModifiedTime}, {@code
     * lastAccessTime} and {@code createTime} parameters are {@code null} then
     * this method has no effect.
     *
     * <p> <b>Usage Example:</b>
     * Suppose we want to change a file's last access time.
     * <pre>
     *    Path path = ...
     *    FileTime time = ...
     *    Files.getFileAttributeView(path, BasicFileAttributeView.class).setTimes(null, time, null);
     * </pre>
     *
     * @param   lastModifiedTime
     *          the new last modified time, or {@code null} to not change the
     *          value
     * @param   lastAccessTime
     *          the last access time, or {@code null} to not change the value
     * @param   createTime
     *          the file's create time, or {@code null} to not change the value
     *
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, its  {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to the file
     *
     * @see java.nio.file.Files#setLastModifiedTime
     */
    void setTimes(FileTime lastModifiedTime,
                  FileTime lastAccessTime,
                  FileTime createTime) throws IOException;
}
