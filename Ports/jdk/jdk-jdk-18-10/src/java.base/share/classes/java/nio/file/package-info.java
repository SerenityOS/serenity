/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Defines interfaces and classes for the Java virtual machine to access files,
 * file attributes, and file systems.
 *
 * <p> The java.nio.file package defines classes to access files and file
 * systems. The API to access file and file system attributes is defined in the
 * {@link java.nio.file.attribute} package. The {@link java.nio.file.spi}
 * package is used by service provider implementors wishing to extend the
 * platform default provider, or to construct other provider implementations. </p>
 *
 * <h2><a id="links">Symbolic Links</a></h2>
 * <p> Many operating systems and file systems have support for <em>symbolic links</em>.
 * A symbolic link is a special file that serves as a reference to another file.
 * For the most part, symbolic links are transparent to applications and
 * operations on symbolic links are automatically redirected to the <em>target</em>
 * of the link. Exceptions to this are when a symbolic link is deleted or
 * renamed/moved in which case the link is deleted or removed rather than the
 * target of the link. This package includes support for symbolic links where
 * implementations provide these semantics. File systems may support other types
 * that are semantically close but support for these other types of links is
 * not included in this package. </p>
 *
 * <h2><a id="interop">Interoperability</a></h2>
 * <p> The {@link java.io.File} class defines the {@link java.io.File#toPath
 * toPath} method to construct a {@link java.nio.file.Path} by converting
 * the abstract path represented by the {@code java.io.File} object. The resulting
 * {@code Path} can be used to operate on the same file as the {@code File}
 * object. The {@code Path} specification provides further information
 * on the <a href="Path.html#interop">interoperability</a> between {@code Path}
 * and {@code java.io.File} objects. </p>
 *
 * <h2>Visibility</h2>
 * <p> The view of the files and file system provided by classes in this package are
 * guaranteed to be consistent with other views provided by other instances in the
 * same Java virtual machine.  The view may or may not, however, be consistent with
 * the view of the file system as seen by other concurrently running programs due
 * to caching performed by the underlying operating system and delays induced by
 * network-filesystem protocols. This is true regardless of the language in which
 * these other programs are written, and whether they are running on the same machine
 * or on some other machine.  The exact nature of any such inconsistencies is
 * system-dependent and therefore unspecified. </p>
 *
 * <h2><a id="integrity">Synchronized I/O File Integrity</a></h2>
 * <p> The {@link java.nio.file.StandardOpenOption#SYNC SYNC} and {@link
 * java.nio.file.StandardOpenOption#DSYNC DSYNC} options are used when opening a file
 * to require that updates to the file are written synchronously to the underlying
 * storage device. In the case of the default provider, and the file resides on
 * a local storage device, and the {@link java.nio.channels.SeekableByteChannel
 * seekable} channel is connected to a file that was opened with one of these
 * options, then an invocation of the {@link
 * java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer) write}
 * method is only guaranteed to return when all changes made to the file
 * by that invocation have been written to the device. These options are useful
 * for ensuring that critical information is not lost in the event of a system
 * crash. If the file does not reside on a local device then no such guarantee
 * is made. Whether this guarantee is possible with other {@link
 * java.nio.file.spi.FileSystemProvider provider} implementations is provider
 * specific. </p>
 *
 * <h2>General Exceptions</h2>
 * <p> Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method of any class or interface in this package will cause a {@link
 * java.lang.NullPointerException NullPointerException} to be thrown. Additionally,
 * invoking a method with an array or collection containing a {@code null} element
 * will cause a {@code NullPointerException}, unless otherwise specified. </p>
 *
 * <p> Unless otherwise noted, methods that attempt to access the file system
 * will throw {@link java.nio.file.ClosedFileSystemException} when invoked on
 * objects associated with a {@link java.nio.file.FileSystem} that has been
 * {@link java.nio.file.FileSystem#close closed}. Additionally, any methods
 * that attempt write access to a file system will throw {@link
 * java.nio.file.ReadOnlyFileSystemException} when invoked on an object associated
 * with a {@link java.nio.file.FileSystem} that only provides read-only
 * access. </p>
 *
 * <p> Unless otherwise noted, invoking a method of any class or interface in
 * this package created by one {@link java.nio.file.spi.FileSystemProvider
 * provider} with a parameter that is an object created by another provider,
 * will throw {@link java.nio.file.ProviderMismatchException}. </p>
 *
 * <h2><a id="optspecex">Optional Specific Exceptions</a></h2>
 * Most of the methods defined by classes in this package that access the
 * file system specify that {@link java.io.IOException} be thrown when an I/O
 * error occurs. In some cases, these methods define specific I/O exceptions
 * for common cases. These exceptions, noted as <i>optional specific exceptions</i>,
 * are thrown by the implementation where it can detect the specific error.
 * Where the specific error cannot be detected then the more general {@code
 * IOException} is thrown.
 *
 * @since 1.7
 */
package java.nio.file;
