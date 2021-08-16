/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.file;

import java.nio.file.attribute.BasicFileAttributes;
import java.io.IOException;

/**
 * A visitor of files. An implementation of this interface is provided to the
 * {@link Files#walkFileTree Files.walkFileTree} methods to visit each file in
 * a file tree.
 *
 * <p> <b>Usage Examples:</b>
 * Suppose we want to delete a file tree. In that case, each directory should
 * be deleted after the entries in the directory are deleted.
 * <pre>
 *     Path start = ...
 *     Files.walkFileTree(start, new SimpleFileVisitor&lt;Path&gt;() {
 *         &#64;Override
 *         public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
 *             throws IOException
 *         {
 *             Files.delete(file);
 *             return FileVisitResult.CONTINUE;
 *         }
 *         &#64;Override
 *         public FileVisitResult postVisitDirectory(Path dir, IOException e)
 *             throws IOException
 *         {
 *             if (e == null) {
 *                 Files.delete(dir);
 *                 return FileVisitResult.CONTINUE;
 *             } else {
 *                 // directory iteration failed
 *                 throw e;
 *             }
 *         }
 *     });
 * </pre>
 * <p> Furthermore, suppose we want to copy a file tree to a target location.
 * In that case, symbolic links should be followed and the target directory
 * should be created before the entries in the directory are copied.
 * <pre>
 *     final Path source = ...
 *     final Path target = ...
 *
 *     Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
 *         new SimpleFileVisitor&lt;Path&gt;() {
 *             &#64;Override
 *             public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
 *                 throws IOException
 *             {
 *                 Path targetdir = target.resolve(source.relativize(dir));
 *                 try {
 *                     Files.copy(dir, targetdir);
 *                 } catch (FileAlreadyExistsException e) {
 *                      if (!Files.isDirectory(targetdir))
 *                          throw e;
 *                 }
 *                 return CONTINUE;
 *             }
 *             &#64;Override
 *             public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
 *                 throws IOException
 *             {
 *                 Files.copy(file, target.resolve(source.relativize(file)));
 *                 return CONTINUE;
 *             }
 *         });
 * </pre>
 *
 * @since 1.7
 */

public interface FileVisitor<T> {

    /**
     * Invoked for a directory before entries in the directory are visited.
     *
     * <p> If this method returns {@link FileVisitResult#CONTINUE CONTINUE},
     * then entries in the directory are visited. If this method returns {@link
     * FileVisitResult#SKIP_SUBTREE SKIP_SUBTREE} or {@link
     * FileVisitResult#SKIP_SIBLINGS SKIP_SIBLINGS} then entries in the
     * directory (and any descendants) will not be visited.
     *
     * @param   dir
     *          a reference to the directory
     * @param   attrs
     *          the directory's basic attributes
     *
     * @return  the visit result
     *
     * @throws  IOException
     *          if an I/O error occurs
     */
    FileVisitResult preVisitDirectory(T dir, BasicFileAttributes attrs)
        throws IOException;

    /**
     * Invoked for a file in a directory.
     *
     * @param   file
     *          a reference to the file
     * @param   attrs
     *          the file's basic attributes
     *
     * @return  the visit result
     *
     * @throws  IOException
     *          if an I/O error occurs
     */
    FileVisitResult visitFile(T file, BasicFileAttributes attrs)
        throws IOException;

    /**
     * Invoked for a file that could not be visited. This method is invoked
     * if the file's attributes could not be read, the file is a directory
     * that could not be opened, and other reasons.
     *
     * @param   file
     *          a reference to the file
     * @param   exc
     *          the I/O exception that prevented the file from being visited
     *
     * @return  the visit result
     *
     * @throws  IOException
     *          if an I/O error occurs
     */
    FileVisitResult visitFileFailed(T file, IOException exc)
        throws IOException;

    /**
     * Invoked for a directory after entries in the directory, and all of their
     * descendants, have been visited. This method is also invoked when iteration
     * of the directory completes prematurely (by a {@link #visitFile visitFile}
     * method returning {@link FileVisitResult#SKIP_SIBLINGS SKIP_SIBLINGS},
     * or an I/O error when iterating over the directory).
     *
     * @param   dir
     *          a reference to the directory
     * @param   exc
     *          {@code null} if the iteration of the directory completes without
     *          an error; otherwise the I/O exception that caused the iteration
     *          of the directory to complete prematurely
     *
     * @return  the visit result
     *
     * @throws  IOException
     *          if an I/O error occurs
     */
    FileVisitResult postVisitDirectory(T dir, IOException exc)
        throws IOException;
}
