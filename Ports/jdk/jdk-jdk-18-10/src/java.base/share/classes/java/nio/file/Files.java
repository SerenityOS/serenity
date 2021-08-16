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

package java.nio.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;   // javadoc
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.spi.FileSystemProvider;
import java.nio.file.spi.FileTypeDetector;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jdk.internal.util.ArraysSupport;
import sun.nio.ch.FileChannelImpl;
import sun.nio.cs.UTF_8;
import sun.nio.fs.AbstractFileSystemProvider;

/**
 * This class consists exclusively of static methods that operate on files,
 * directories, or other types of files.
 *
 * <p> In most cases, the methods defined here will delegate to the associated
 * file system provider to perform the file operations.
 *
 * @since 1.7
 */

public final class Files {
    // buffer size used for reading and writing
    private static final int BUFFER_SIZE = 8192;

    private Files() { }

    /**
     * Returns the {@code FileSystemProvider} to delegate to.
     */
    private static FileSystemProvider provider(Path path) {
        return path.getFileSystem().provider();
    }

    /**
     * Convert a Closeable to a Runnable by converting checked IOException
     * to UncheckedIOException
     */
    private static Runnable asUncheckedRunnable(Closeable c) {
        return () -> {
            try {
                c.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    // -- File contents --

    /**
     * Opens a file, returning an input stream to read from the file. The stream
     * will not be buffered, and is not required to support the {@link
     * InputStream#mark mark} or {@link InputStream#reset reset} methods. The
     * stream will be safe for access by multiple concurrent threads. Reading
     * commences at the beginning of the file. Whether the returned stream is
     * <i>asynchronously closeable</i> and/or <i>interruptible</i> is highly
     * file system provider specific and therefore not specified.
     *
     * <p> The {@code options} parameter determines how the file is opened.
     * If no options are present then it is equivalent to opening the file with
     * the {@link StandardOpenOption#READ READ} option. In addition to the {@code
     * READ} option, an implementation may also support additional implementation
     * specific options.
     *
     * @param   path
     *          the path to the file to open
     * @param   options
     *          options specifying how the file is opened
     *
     * @return  a new input stream
     *
     * @throws  IllegalArgumentException
     *          if an invalid combination of options is specified
     * @throws  UnsupportedOperationException
     *          if an unsupported option is specified
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file.
     */
    public static InputStream newInputStream(Path path, OpenOption... options)
        throws IOException
    {
        return provider(path).newInputStream(path, options);
    }

    /**
     * Opens or creates a file, returning an output stream that may be used to
     * write bytes to the file. The resulting stream will not be buffered. The
     * stream will be safe for access by multiple concurrent threads. Whether
     * the returned stream is <i>asynchronously closeable</i> and/or
     * <i>interruptible</i> is highly file system provider specific and
     * therefore not specified.
     *
     * <p> This method opens or creates a file in exactly the manner specified
     * by the {@link #newByteChannel(Path,Set,FileAttribute[]) newByteChannel}
     * method with the exception that the {@link StandardOpenOption#READ READ}
     * option may not be present in the array of options. If no options are
     * present then this method works as if the {@link StandardOpenOption#CREATE
     * CREATE}, {@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING},
     * and {@link StandardOpenOption#WRITE WRITE} options are present. In other
     * words, it opens the file for writing, creating the file if it doesn't
     * exist, or initially truncating an existing {@link #isRegularFile
     * regular-file} to a size of {@code 0} if it exists.
     *
     * <p> <b>Usage Examples:</b>
     * <pre>
     *     Path path = ...
     *
     *     // truncate and overwrite an existing file, or create the file if
     *     // it doesn't initially exist
     *     OutputStream out = Files.newOutputStream(path);
     *
     *     // append to an existing file, fail if the file does not exist
     *     out = Files.newOutputStream(path, APPEND);
     *
     *     // append to an existing file, create file if it doesn't initially exist
     *     out = Files.newOutputStream(path, CREATE, APPEND);
     *
     *     // always create new file, failing if it already exists
     *     out = Files.newOutputStream(path, CREATE_NEW);
     * </pre>
     *
     * @param   path
     *          the path to the file to open or create
     * @param   options
     *          options specifying how the file is opened
     *
     * @return  a new output stream
     *
     * @throws  IllegalArgumentException
     *          if {@code options} contains an invalid combination of options
     * @throws  UnsupportedOperationException
     *          if an unsupported option is specified
     * @throws  FileAlreadyExistsException
     *          If a file of that name already exists and the {@link
     *          StandardOpenOption#CREATE_NEW CREATE_NEW} option is specified
     *          <i>(optional specific exception)</i>
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to the file. The {@link
     *          SecurityManager#checkDelete(String) checkDelete} method is
     *          invoked to check delete access if the file is opened with the
     *          {@code DELETE_ON_CLOSE} option.
     */
    public static OutputStream newOutputStream(Path path, OpenOption... options)
        throws IOException
    {
        return provider(path).newOutputStream(path, options);
    }

    /**
     * Opens or creates a file, returning a seekable byte channel to access the
     * file.
     *
     * <p> The {@code options} parameter determines how the file is opened.
     * The {@link StandardOpenOption#READ READ} and {@link
     * StandardOpenOption#WRITE WRITE} options determine if the file should be
     * opened for reading and/or writing. If neither option (or the {@link
     * StandardOpenOption#APPEND APPEND} option) is present then the file is
     * opened for reading. By default reading or writing commence at the
     * beginning of the file.
     *
     * <p> In the addition to {@code READ} and {@code WRITE}, the following
     * options may be present:
     *
     * <table class="striped">
     * <caption style="display:none">Options</caption>
     * <thead>
     * <tr> <th scope="col">Option</th> <th scope="col">Description</th> </tr>
     * </thead>
     * <tbody>
     * <tr>
     *   <th scope="row"> {@link StandardOpenOption#APPEND APPEND} </th>
     *   <td> If this option is present then the file is opened for writing and
     *     each invocation of the channel's {@code write} method first advances
     *     the position to the end of the file and then writes the requested
     *     data. Whether the advancement of the position and the writing of the
     *     data are done in a single atomic operation is system-dependent and
     *     therefore unspecified. This option may not be used in conjunction
     *     with the {@code READ} or {@code TRUNCATE_EXISTING} options. </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@link StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING} </th>
     *   <td> If this option is present then the existing file is truncated to
     *   a size of 0 bytes. This option is ignored when the file is opened only
     *   for reading. </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@link StandardOpenOption#CREATE_NEW CREATE_NEW} </th>
     *   <td> If this option is present then a new file is created, failing if
     *   the file already exists or is a symbolic link. When creating a file the
     *   check for the existence of the file and the creation of the file if it
     *   does not exist is atomic with respect to other file system operations.
     *   This option is ignored when the file is opened only for reading. </td>
     * </tr>
     * <tr>
     *   <th scope="row" > {@link StandardOpenOption#CREATE CREATE} </th>
     *   <td> If this option is present then an existing file is opened if it
     *   exists, otherwise a new file is created. This option is ignored if the
     *   {@code CREATE_NEW} option is also present or the file is opened only
     *   for reading. </td>
     * </tr>
     * <tr>
     *   <th scope="row" > {@link StandardOpenOption#DELETE_ON_CLOSE DELETE_ON_CLOSE} </th>
     *   <td> When this option is present then the implementation makes a
     *   <em>best effort</em> attempt to delete the file when closed by the
     *   {@link SeekableByteChannel#close close} method. If the {@code close}
     *   method is not invoked then a <em>best effort</em> attempt is made to
     *   delete the file when the Java virtual machine terminates. </td>
     * </tr>
     * <tr>
     *   <th scope="row">{@link StandardOpenOption#SPARSE SPARSE} </th>
     *   <td> When creating a new file this option is a <em>hint</em> that the
     *   new file will be sparse. This option is ignored when not creating
     *   a new file. </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@link StandardOpenOption#SYNC SYNC} </th>
     *   <td> Requires that every update to the file's content or metadata be
     *   written synchronously to the underlying storage device. (see <a
     *   href="package-summary.html#integrity"> Synchronized I/O file
     *   integrity</a>). </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@link StandardOpenOption#DSYNC DSYNC} </th>
     *   <td> Requires that every update to the file's content be written
     *   synchronously to the underlying storage device. (see <a
     *   href="package-summary.html#integrity"> Synchronized I/O file
     *   integrity</a>). </td>
     * </tr>
     * </tbody>
     * </table>
     *
     * <p> An implementation may also support additional implementation specific
     * options.
     *
     * <p> The {@code attrs} parameter is optional {@link FileAttribute
     * file-attributes} to set atomically when a new file is created.
     *
     * <p> In the case of the default provider, the returned seekable byte channel
     * is a {@link java.nio.channels.FileChannel}.
     *
     * <p> <b>Usage Examples:</b>
     * <pre>{@code
     *     Path path = ...
     *
     *     // open file for reading
     *     ReadableByteChannel rbc = Files.newByteChannel(path, EnumSet.of(READ)));
     *
     *     // open file for writing to the end of an existing file, creating
     *     // the file if it doesn't already exist
     *     WritableByteChannel wbc = Files.newByteChannel(path, EnumSet.of(CREATE,APPEND));
     *
     *     // create file with initial permissions, opening it for both reading and writing
     *     FileAttribute<Set<PosixFilePermission>> perms = ...
     *     SeekableByteChannel sbc =
     *         Files.newByteChannel(path, EnumSet.of(CREATE_NEW,READ,WRITE), perms);
     * }</pre>
     *
     * @param   path
     *          the path to the file to open or create
     * @param   options
     *          options specifying how the file is opened
     * @param   attrs
     *          an optional list of file attributes to set atomically when
     *          creating the file
     *
     * @return  a new seekable byte channel
     *
     * @throws  IllegalArgumentException
     *          if the set contains an invalid combination of options
     * @throws  UnsupportedOperationException
     *          if an unsupported open option is specified or the array contains
     *          attributes that cannot be set atomically when creating the file
     * @throws  FileAlreadyExistsException
     *          If a file of that name already exists and the {@link
     *          StandardOpenOption#CREATE_NEW CREATE_NEW} option is specified
     *          and the file is being opened for writing <i>(optional specific
     *          exception)</i>
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the path if the file is
     *          opened for reading. The {@link SecurityManager#checkWrite(String)
     *          checkWrite} method is invoked to check write access to the path
     *          if the file is opened for writing. The {@link
     *          SecurityManager#checkDelete(String) checkDelete} method is
     *          invoked to check delete access if the file is opened with the
     *          {@code DELETE_ON_CLOSE} option.
     *
     * @see java.nio.channels.FileChannel#open(Path,Set,FileAttribute[])
     */
    public static SeekableByteChannel newByteChannel(Path path,
                                                     Set<? extends OpenOption> options,
                                                     FileAttribute<?>... attrs)
        throws IOException
    {
        return provider(path).newByteChannel(path, options, attrs);
    }

    /**
     * Opens or creates a file, returning a seekable byte channel to access the
     * file.
     *
     * <p> This method opens or creates a file in exactly the manner specified
     * by the {@link #newByteChannel(Path,Set,FileAttribute[]) newByteChannel}
     * method.
     *
     * @param   path
     *          the path to the file to open or create
     * @param   options
     *          options specifying how the file is opened
     *
     * @return  a new seekable byte channel
     *
     * @throws  IllegalArgumentException
     *          if the set contains an invalid combination of options
     * @throws  UnsupportedOperationException
     *          if an unsupported open option is specified
     * @throws  FileAlreadyExistsException
     *          If a file of that name already exists and the {@link
     *          StandardOpenOption#CREATE_NEW CREATE_NEW} option is specified
     *          and the file is being opened for writing <i>(optional specific
     *          exception)</i>
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the path if the file is
     *          opened for reading. The {@link SecurityManager#checkWrite(String)
     *          checkWrite} method is invoked to check write access to the path
     *          if the file is opened for writing. The {@link
     *          SecurityManager#checkDelete(String) checkDelete} method is
     *          invoked to check delete access if the file is opened with the
     *          {@code DELETE_ON_CLOSE} option.
     *
     * @see java.nio.channels.FileChannel#open(Path,OpenOption[])
     */
    public static SeekableByteChannel newByteChannel(Path path, OpenOption... options)
        throws IOException
    {
        Set<OpenOption> set;
        if (options.length == 0) {
            set = Collections.emptySet();
        } else {
            set = new HashSet<>();
            Collections.addAll(set, options);
        }
        return newByteChannel(path, set);
    }

    // -- Directories --

    private static class AcceptAllFilter
        implements DirectoryStream.Filter<Path>
    {
        private AcceptAllFilter() { }

        @Override
        public boolean accept(Path entry) { return true; }

        static final AcceptAllFilter FILTER = new AcceptAllFilter();
    }

    /**
     * Opens a directory, returning a {@link DirectoryStream} to iterate over
     * all entries in the directory. The elements returned by the directory
     * stream's {@link DirectoryStream#iterator iterator} are of type {@code
     * Path}, each one representing an entry in the directory. The {@code Path}
     * objects are obtained as if by {@link Path#resolve(Path) resolving} the
     * name of the directory entry against {@code dir}.
     *
     * <p> When not using the try-with-resources construct, then directory
     * stream's {@code close} method should be invoked after iteration is
     * completed so as to free any resources held for the open directory.
     *
     * <p> When an implementation supports operations on entries in the
     * directory that execute in a race-free manner then the returned directory
     * stream is a {@link SecureDirectoryStream}.
     *
     * @param   dir
     *          the path to the directory
     *
     * @return  a new and open {@code DirectoryStream} object
     *
     * @throws  NotDirectoryException
     *          if the file could not otherwise be opened because it is not
     *          a directory <i>(optional specific exception)</i>
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the directory.
     */
    public static DirectoryStream<Path> newDirectoryStream(Path dir)
        throws IOException
    {
        return provider(dir).newDirectoryStream(dir, AcceptAllFilter.FILTER);
    }

    /**
     * Opens a directory, returning a {@link DirectoryStream} to iterate over
     * the entries in the directory. The elements returned by the directory
     * stream's {@link DirectoryStream#iterator iterator} are of type {@code
     * Path}, each one representing an entry in the directory. The {@code Path}
     * objects are obtained as if by {@link Path#resolve(Path) resolving} the
     * name of the directory entry against {@code dir}. The entries returned by
     * the iterator are filtered by matching the {@code String} representation
     * of their file names against the given <em>globbing</em> pattern.
     *
     * <p> For example, suppose we want to iterate over the files ending with
     * ".java" in a directory:
     * <pre>
     *     Path dir = ...
     *     try (DirectoryStream&lt;Path&gt; stream = Files.newDirectoryStream(dir, "*.java")) {
     *         :
     *     }
     * </pre>
     *
     * <p> The globbing pattern is specified by the {@link
     * FileSystem#getPathMatcher getPathMatcher} method.
     *
     * <p> When not using the try-with-resources construct, then directory
     * stream's {@code close} method should be invoked after iteration is
     * completed so as to free any resources held for the open directory.
     *
     * <p> When an implementation supports operations on entries in the
     * directory that execute in a race-free manner then the returned directory
     * stream is a {@link SecureDirectoryStream}.
     *
     * @param   dir
     *          the path to the directory
     * @param   glob
     *          the glob pattern
     *
     * @return  a new and open {@code DirectoryStream} object
     *
     * @throws  java.util.regex.PatternSyntaxException
     *          if the pattern is invalid
     * @throws  NotDirectoryException
     *          if the file could not otherwise be opened because it is not
     *          a directory <i>(optional specific exception)</i>
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the directory.
     */
    public static DirectoryStream<Path> newDirectoryStream(Path dir, String glob)
        throws IOException
    {
        // avoid creating a matcher if all entries are required.
        if (glob.equals("*"))
            return newDirectoryStream(dir);

        // create a matcher and return a filter that uses it.
        FileSystem fs = dir.getFileSystem();
        final PathMatcher matcher = fs.getPathMatcher("glob:" + glob);
        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<>() {
            @Override
            public boolean accept(Path entry)  {
                return matcher.matches(entry.getFileName());
            }
        };
        return fs.provider().newDirectoryStream(dir, filter);
    }

    /**
     * Opens a directory, returning a {@link DirectoryStream} to iterate over
     * the entries in the directory. The elements returned by the directory
     * stream's {@link DirectoryStream#iterator iterator} are of type {@code
     * Path}, each one representing an entry in the directory. The {@code Path}
     * objects are obtained as if by {@link Path#resolve(Path) resolving} the
     * name of the directory entry against {@code dir}. The entries returned by
     * the iterator are filtered by the given {@link DirectoryStream.Filter
     * filter}.
     *
     * <p> When not using the try-with-resources construct, then directory
     * stream's {@code close} method should be invoked after iteration is
     * completed so as to free any resources held for the open directory.
     *
     * <p> Where the filter terminates due to an uncaught error or runtime
     * exception then it is propagated to the {@link Iterator#hasNext()
     * hasNext} or {@link Iterator#next() next} method. Where an {@code
     * IOException} is thrown, it results in the {@code hasNext} or {@code
     * next} method throwing a {@link DirectoryIteratorException} with the
     * {@code IOException} as the cause.
     *
     * <p> When an implementation supports operations on entries in the
     * directory that execute in a race-free manner then the returned directory
     * stream is a {@link SecureDirectoryStream}.
     *
     * <p> <b>Usage Example:</b>
     * Suppose we want to iterate over the files in a directory that are
     * larger than 8K.
     * <pre>
     *     DirectoryStream.Filter&lt;Path&gt; filter = new DirectoryStream.Filter&lt;Path&gt;() {
     *         public boolean accept(Path file) throws IOException {
     *             return (Files.size(file) &gt; 8192L);
     *         }
     *     };
     *     Path dir = ...
     *     try (DirectoryStream&lt;Path&gt; stream = Files.newDirectoryStream(dir, filter)) {
     *         :
     *     }
     * </pre>
     *
     * @param   dir
     *          the path to the directory
     * @param   filter
     *          the directory stream filter
     *
     * @return  a new and open {@code DirectoryStream} object
     *
     * @throws  NotDirectoryException
     *          if the file could not otherwise be opened because it is not
     *          a directory <i>(optional specific exception)</i>
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the directory.
     */
    public static DirectoryStream<Path> newDirectoryStream(Path dir,
                                                           DirectoryStream.Filter<? super Path> filter)
        throws IOException
    {
        return provider(dir).newDirectoryStream(dir, filter);
    }

    // -- Creation and deletion --

    private static final Set<OpenOption> DEFAULT_CREATE_OPTIONS =
        Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

    /**
     * Creates a new and empty file, failing if the file already exists. The
     * check for the existence of the file and the creation of the new file if
     * it does not exist are a single operation that is atomic with respect to
     * all other filesystem activities that might affect the directory.
     *
     * <p> The {@code attrs} parameter is optional {@link FileAttribute
     * file-attributes} to set atomically when creating the file. Each attribute
     * is identified by its {@link FileAttribute#name name}. If more than one
     * attribute of the same name is included in the array then all but the last
     * occurrence is ignored.
     *
     * @param   path
     *          the path to the file to create
     * @param   attrs
     *          an optional list of file attributes to set atomically when
     *          creating the file
     *
     * @return  the file
     *
     * @throws  UnsupportedOperationException
     *          if the array contains an attribute that cannot be set atomically
     *          when creating the file
     * @throws  FileAlreadyExistsException
     *          If a file of that name already exists
     *          <i>(optional specific exception)</i>
     * @throws  IOException
     *          if an I/O error occurs or the parent directory does not exist
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to the new file.
     */
    public static Path createFile(Path path, FileAttribute<?>... attrs)
        throws IOException
    {
        newByteChannel(path, DEFAULT_CREATE_OPTIONS, attrs).close();
        return path;
    }

    /**
     * Creates a new directory. The check for the existence of the file and the
     * creation of the directory if it does not exist are a single operation
     * that is atomic with respect to all other filesystem activities that might
     * affect the directory. The {@link #createDirectories createDirectories}
     * method should be used where it is required to create all nonexistent
     * parent directories first.
     *
     * <p> The {@code attrs} parameter is optional {@link FileAttribute
     * file-attributes} to set atomically when creating the directory. Each
     * attribute is identified by its {@link FileAttribute#name name}. If more
     * than one attribute of the same name is included in the array then all but
     * the last occurrence is ignored.
     *
     * @param   dir
     *          the directory to create
     * @param   attrs
     *          an optional list of file attributes to set atomically when
     *          creating the directory
     *
     * @return  the directory
     *
     * @throws  UnsupportedOperationException
     *          if the array contains an attribute that cannot be set atomically
     *          when creating the directory
     * @throws  FileAlreadyExistsException
     *          if a directory could not otherwise be created because a file of
     *          that name already exists <i>(optional specific exception)</i>
     * @throws  IOException
     *          if an I/O error occurs or the parent directory does not exist
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to the new directory.
     */
    public static Path createDirectory(Path dir, FileAttribute<?>... attrs)
        throws IOException
    {
        provider(dir).createDirectory(dir, attrs);
        return dir;
    }

    /**
     * Creates a directory by creating all nonexistent parent directories first.
     * Unlike the {@link #createDirectory createDirectory} method, an exception
     * is not thrown if the directory could not be created because it already
     * exists.
     *
     * <p> The {@code attrs} parameter is optional {@link FileAttribute
     * file-attributes} to set atomically when creating the nonexistent
     * directories. Each file attribute is identified by its {@link
     * FileAttribute#name name}. If more than one attribute of the same name is
     * included in the array then all but the last occurrence is ignored.
     *
     * <p> If this method fails, then it may do so after creating some, but not
     * all, of the parent directories.
     *
     * @param   dir
     *          the directory to create
     *
     * @param   attrs
     *          an optional list of file attributes to set atomically when
     *          creating the directory
     *
     * @return  the directory
     *
     * @throws  UnsupportedOperationException
     *          if the array contains an attribute that cannot be set atomically
     *          when creating the directory
     * @throws  FileAlreadyExistsException
     *          if {@code dir} exists but is not a directory <i>(optional specific
     *          exception)</i>
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          in the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked prior to attempting to create a directory and
     *          its {@link SecurityManager#checkRead(String) checkRead} is
     *          invoked for each parent directory that is checked. If {@code
     *          dir} is not an absolute path then its {@link Path#toAbsolutePath
     *          toAbsolutePath} may need to be invoked to get its absolute path.
     *          This may invoke the security manager's {@link
     *          SecurityManager#checkPropertyAccess(String) checkPropertyAccess}
     *          method to check access to the system property {@code user.dir}
     */
    public static Path createDirectories(Path dir, FileAttribute<?>... attrs)
        throws IOException
    {
        // attempt to create the directory
        try {
            createAndCheckIsDirectory(dir, attrs);
            return dir;
        } catch (FileAlreadyExistsException x) {
            // file exists and is not a directory
            throw x;
        } catch (IOException x) {
            // parent may not exist or other reason
        }
        SecurityException se = null;
        try {
            dir = dir.toAbsolutePath();
        } catch (SecurityException x) {
            // don't have permission to get absolute path
            se = x;
        }
        // find a descendant that exists
        Path parent = dir.getParent();
        while (parent != null) {
            try {
                provider(parent).checkAccess(parent);
                break;
            } catch (NoSuchFileException x) {
                // does not exist
            }
            parent = parent.getParent();
        }
        if (parent == null) {
            // unable to find existing parent
            if (se == null) {
                throw new FileSystemException(dir.toString(), null,
                    "Unable to determine if root directory exists");
            } else {
                throw se;
            }
        }

        // create directories
        Path child = parent;
        for (Path name: parent.relativize(dir)) {
            child = child.resolve(name);
            createAndCheckIsDirectory(child, attrs);
        }
        return dir;
    }

    /**
     * Used by createDirectories to attempt to create a directory. A no-op
     * if the directory already exists.
     */
    private static void createAndCheckIsDirectory(Path dir,
                                                  FileAttribute<?>... attrs)
        throws IOException
    {
        try {
            createDirectory(dir, attrs);
        } catch (FileAlreadyExistsException x) {
            if (!isDirectory(dir, LinkOption.NOFOLLOW_LINKS))
                throw x;
        }
    }

    /**
     * Creates a new empty file in the specified directory, using the given
     * prefix and suffix strings to generate its name. The resulting
     * {@code Path} is associated with the same {@code FileSystem} as the given
     * directory.
     *
     * <p> The details as to how the name of the file is constructed is
     * implementation dependent and therefore not specified. Where possible
     * the {@code prefix} and {@code suffix} are used to construct candidate
     * names in the same manner as the {@link
     * java.io.File#createTempFile(String,String,File)} method.
     *
     * <p> As with the {@code File.createTempFile} methods, this method is only
     * part of a temporary-file facility. Where used as a <em>work files</em>,
     * the resulting file may be opened using the {@link
     * StandardOpenOption#DELETE_ON_CLOSE DELETE_ON_CLOSE} option so that the
     * file is deleted when the appropriate {@code close} method is invoked.
     * Alternatively, a {@link Runtime#addShutdownHook shutdown-hook}, or the
     * {@link java.io.File#deleteOnExit} mechanism may be used to delete the
     * file automatically.
     *
     * <p> The {@code attrs} parameter is optional {@link FileAttribute
     * file-attributes} to set atomically when creating the file. Each attribute
     * is identified by its {@link FileAttribute#name name}. If more than one
     * attribute of the same name is included in the array then all but the last
     * occurrence is ignored. When no file attributes are specified, then the
     * resulting file may have more restrictive access permissions to files
     * created by the {@link java.io.File#createTempFile(String,String,File)}
     * method.
     *
     * @param   dir
     *          the path to directory in which to create the file
     * @param   prefix
     *          the prefix string to be used in generating the file's name;
     *          may be {@code null}
     * @param   suffix
     *          the suffix string to be used in generating the file's name;
     *          may be {@code null}, in which case "{@code .tmp}" is used
     * @param   attrs
     *          an optional list of file attributes to set atomically when
     *          creating the file
     *
     * @return  the path to the newly created file that did not exist before
     *          this method was invoked
     *
     * @throws  IllegalArgumentException
     *          if the prefix or suffix parameters cannot be used to generate
     *          a candidate file name
     * @throws  UnsupportedOperationException
     *          if the array contains an attribute that cannot be set atomically
     *          when creating the directory
     * @throws  IOException
     *          if an I/O error occurs or {@code dir} does not exist
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to the file.
     */
    public static Path createTempFile(Path dir,
                                      String prefix,
                                      String suffix,
                                      FileAttribute<?>... attrs)
        throws IOException
    {
        return TempFileHelper.createTempFile(Objects.requireNonNull(dir),
                                             prefix, suffix, attrs);
    }

    /**
     * Creates an empty file in the default temporary-file directory, using
     * the given prefix and suffix to generate its name. The resulting {@code
     * Path} is associated with the default {@code FileSystem}.
     *
     * <p> This method works in exactly the manner specified by the
     * {@link #createTempFile(Path,String,String,FileAttribute[])} method for
     * the case that the {@code dir} parameter is the temporary-file directory.
     *
     * @param   prefix
     *          the prefix string to be used in generating the file's name;
     *          may be {@code null}
     * @param   suffix
     *          the suffix string to be used in generating the file's name;
     *          may be {@code null}, in which case "{@code .tmp}" is used
     * @param   attrs
     *          an optional list of file attributes to set atomically when
     *          creating the file
     *
     * @return  the path to the newly created file that did not exist before
     *          this method was invoked
     *
     * @throws  IllegalArgumentException
     *          if the prefix or suffix parameters cannot be used to generate
     *          a candidate file name
     * @throws  UnsupportedOperationException
     *          if the array contains an attribute that cannot be set atomically
     *          when creating the directory
     * @throws  IOException
     *          if an I/O error occurs or the temporary-file directory does not
     *          exist
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to the file.
     */
    public static Path createTempFile(String prefix,
                                      String suffix,
                                      FileAttribute<?>... attrs)
        throws IOException
    {
        return TempFileHelper.createTempFile(null, prefix, suffix, attrs);
    }

    /**
     * Creates a new directory in the specified directory, using the given
     * prefix to generate its name.  The resulting {@code Path} is associated
     * with the same {@code FileSystem} as the given directory.
     *
     * <p> The details as to how the name of the directory is constructed is
     * implementation dependent and therefore not specified. Where possible
     * the {@code prefix} is used to construct candidate names.
     *
     * <p> As with the {@code createTempFile} methods, this method is only
     * part of a temporary-file facility. A {@link Runtime#addShutdownHook
     * shutdown-hook}, or the {@link java.io.File#deleteOnExit} mechanism may be
     * used to delete the directory automatically.
     *
     * <p> The {@code attrs} parameter is optional {@link FileAttribute
     * file-attributes} to set atomically when creating the directory. Each
     * attribute is identified by its {@link FileAttribute#name name}. If more
     * than one attribute of the same name is included in the array then all but
     * the last occurrence is ignored.
     *
     * @param   dir
     *          the path to directory in which to create the directory
     * @param   prefix
     *          the prefix string to be used in generating the directory's name;
     *          may be {@code null}
     * @param   attrs
     *          an optional list of file attributes to set atomically when
     *          creating the directory
     *
     * @return  the path to the newly created directory that did not exist before
     *          this method was invoked
     *
     * @throws  IllegalArgumentException
     *          if the prefix cannot be used to generate a candidate directory name
     * @throws  UnsupportedOperationException
     *          if the array contains an attribute that cannot be set atomically
     *          when creating the directory
     * @throws  IOException
     *          if an I/O error occurs or {@code dir} does not exist
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access when creating the
     *          directory.
     */
    public static Path createTempDirectory(Path dir,
                                           String prefix,
                                           FileAttribute<?>... attrs)
        throws IOException
    {
        return TempFileHelper.createTempDirectory(Objects.requireNonNull(dir),
                                                  prefix, attrs);
    }

    /**
     * Creates a new directory in the default temporary-file directory, using
     * the given prefix to generate its name. The resulting {@code Path} is
     * associated with the default {@code FileSystem}.
     *
     * <p> This method works in exactly the manner specified by {@link
     * #createTempDirectory(Path,String,FileAttribute[])} method for the case
     * that the {@code dir} parameter is the temporary-file directory.
     *
     * @param   prefix
     *          the prefix string to be used in generating the directory's name;
     *          may be {@code null}
     * @param   attrs
     *          an optional list of file attributes to set atomically when
     *          creating the directory
     *
     * @return  the path to the newly created directory that did not exist before
     *          this method was invoked
     *
     * @throws  IllegalArgumentException
     *          if the prefix cannot be used to generate a candidate directory name
     * @throws  UnsupportedOperationException
     *          if the array contains an attribute that cannot be set atomically
     *          when creating the directory
     * @throws  IOException
     *          if an I/O error occurs or the temporary-file directory does not
     *          exist
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access when creating the
     *          directory.
     */
    public static Path createTempDirectory(String prefix,
                                           FileAttribute<?>... attrs)
        throws IOException
    {
        return TempFileHelper.createTempDirectory(null, prefix, attrs);
    }

    /**
     * Creates a symbolic link to a target <i>(optional operation)</i>.
     *
     * <p> The {@code target} parameter is the target of the link. It may be an
     * {@link Path#isAbsolute absolute} or relative path and may not exist. When
     * the target is a relative path then file system operations on the resulting
     * link are relative to the path of the link.
     *
     * <p> The {@code attrs} parameter is optional {@link FileAttribute
     * attributes} to set atomically when creating the link. Each attribute is
     * identified by its {@link FileAttribute#name name}. If more than one attribute
     * of the same name is included in the array then all but the last occurrence
     * is ignored.
     *
     * <p> Where symbolic links are supported, but the underlying {@link FileStore}
     * does not support symbolic links, then this may fail with an {@link
     * IOException}. Additionally, some operating systems may require that the
     * Java virtual machine be started with implementation specific privileges to
     * create symbolic links, in which case this method may throw {@code IOException}.
     *
     * @param   link
     *          the path of the symbolic link to create
     * @param   target
     *          the target of the symbolic link
     * @param   attrs
     *          the array of attributes to set atomically when creating the
     *          symbolic link
     *
     * @return  the path to the symbolic link
     *
     * @throws  UnsupportedOperationException
     *          if the implementation does not support symbolic links or the
     *          array contains an attribute that cannot be set atomically when
     *          creating the symbolic link
     * @throws  FileAlreadyExistsException
     *          if a file with the name already exists <i>(optional specific
     *          exception)</i>
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager
     *          is installed, it denies {@link LinkPermission}{@code ("symbolic")}
     *          or its {@link SecurityManager#checkWrite(String) checkWrite}
     *          method denies write access to the path of the symbolic link.
     */
    public static Path createSymbolicLink(Path link, Path target,
                                          FileAttribute<?>... attrs)
        throws IOException
    {
        provider(link).createSymbolicLink(link, target, attrs);
        return link;
    }

    /**
     * Creates a new link (directory entry) for an existing file <i>(optional
     * operation)</i>.
     *
     * <p> The {@code link} parameter locates the directory entry to create.
     * The {@code existing} parameter is the path to an existing file. This
     * method creates a new directory entry for the file so that it can be
     * accessed using {@code link} as the path. On some file systems this is
     * known as creating a "hard link". Whether the file attributes are
     * maintained for the file or for each directory entry is file system
     * specific and therefore not specified. Typically, a file system requires
     * that all links (directory entries) for a file be on the same file system.
     * Furthermore, on some platforms, the Java virtual machine may require to
     * be started with implementation specific privileges to create hard links
     * or to create links to directories.
     *
     * @param   link
     *          the link (directory entry) to create
     * @param   existing
     *          a path to an existing file
     *
     * @return  the path to the link (directory entry)
     *
     * @throws  UnsupportedOperationException
     *          if the implementation does not support adding an existing file
     *          to a directory
     * @throws  FileAlreadyExistsException
     *          if the entry could not otherwise be created because a file of
     *          that name already exists <i>(optional specific exception)</i>
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager
     *          is installed, it denies {@link LinkPermission}{@code ("hard")}
     *          or its {@link SecurityManager#checkWrite(String) checkWrite}
     *          method denies write access to either the link or the
     *          existing file.
     */
    public static Path createLink(Path link, Path existing) throws IOException {
        provider(link).createLink(link, existing);
        return link;
    }

    /**
     * Deletes a file.
     *
     * <p> An implementation may require to examine the file to determine if the
     * file is a directory. Consequently this method may not be atomic with respect
     * to other file system operations.  If the file is a symbolic link then the
     * symbolic link itself, not the final target of the link, is deleted.
     *
     * <p> If the file is a directory then the directory must be empty. In some
     * implementations a directory has entries for special files or links that
     * are created when the directory is created. In such implementations a
     * directory is considered empty when only the special entries exist.
     * This method can be used with the {@link #walkFileTree walkFileTree}
     * method to delete a directory and all entries in the directory, or an
     * entire <i>file-tree</i> where required.
     *
     * <p> On some operating systems it may not be possible to remove a file when
     * it is open and in use by this Java virtual machine or other programs.
     *
     * @param   path
     *          the path to the file to delete
     *
     * @throws  NoSuchFileException
     *          if the file does not exist <i>(optional specific exception)</i>
     * @throws  DirectoryNotEmptyException
     *          if the file is a directory and could not otherwise be deleted
     *          because the directory is not empty <i>(optional specific
     *          exception)</i>
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkDelete(String)} method
     *          is invoked to check delete access to the file
     */
    public static void delete(Path path) throws IOException {
        provider(path).delete(path);
    }

    /**
     * Deletes a file if it exists.
     *
     * <p> As with the {@link #delete(Path) delete(Path)} method, an
     * implementation may need to examine the file to determine if the file is a
     * directory. Consequently this method may not be atomic with respect to
     * other file system operations.  If the file is a symbolic link, then the
     * symbolic link itself, not the final target of the link, is deleted.
     *
     * <p> If the file is a directory then the directory must be empty. In some
     * implementations a directory has entries for special files or links that
     * are created when the directory is created. In such implementations a
     * directory is considered empty when only the special entries exist.
     *
     * <p> On some operating systems it may not be possible to remove a file when
     * it is open and in use by this Java virtual machine or other programs.
     *
     * @param   path
     *          the path to the file to delete
     *
     * @return  {@code true} if the file was deleted by this method; {@code
     *          false} if the file could not be deleted because it did not
     *          exist
     *
     * @throws  DirectoryNotEmptyException
     *          if the file is a directory and could not otherwise be deleted
     *          because the directory is not empty <i>(optional specific
     *          exception)</i>
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkDelete(String)} method
     *          is invoked to check delete access to the file.
     */
    public static boolean deleteIfExists(Path path) throws IOException {
        return provider(path).deleteIfExists(path);
    }

    // -- Copying and moving files --

    /**
     * Copy a file to a target file.
     *
     * <p> This method copies a file to the target file with the {@code
     * options} parameter specifying how the copy is performed. By default, the
     * copy fails if the target file already exists or is a symbolic link,
     * except if the source and target are the {@link #isSameFile same} file, in
     * which case the method completes without copying the file. File attributes
     * are not required to be copied to the target file. If symbolic links are
     * supported, and the file is a symbolic link, then the final target of the
     * link is copied. If the file is a directory then it creates an empty
     * directory in the target location (entries in the directory are not
     * copied). This method can be used with the {@link #walkFileTree
     * walkFileTree} method to copy a directory and all entries in the directory,
     * or an entire <i>file-tree</i> where required.
     *
     * <p> The {@code options} parameter may include any of the following:
     *
     * <table class="striped">
     * <caption style="display:none">Options</caption>
     * <thead>
     * <tr> <th scope="col">Option</th> <th scope="col">Description</th> </tr>
     * </thead>
     * <tbody>
     * <tr>
     *   <th scope="row"> {@link StandardCopyOption#REPLACE_EXISTING REPLACE_EXISTING} </th>
     *   <td> If the target file exists, then the target file is replaced if it
     *     is not a non-empty directory. If the target file exists and is a
     *     symbolic link, then the symbolic link itself, not the target of
     *     the link, is replaced. </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@link StandardCopyOption#COPY_ATTRIBUTES COPY_ATTRIBUTES} </th>
     *   <td> Attempts to copy the file attributes associated with this file to
     *     the target file. The exact file attributes that are copied is platform
     *     and file system dependent and therefore unspecified. Minimally, the
     *     {@link BasicFileAttributes#lastModifiedTime last-modified-time} is
     *     copied to the target file if supported by both the source and target
     *     file stores. Copying of file timestamps may result in precision
     *     loss. </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@link LinkOption#NOFOLLOW_LINKS NOFOLLOW_LINKS} </th>
     *   <td> Symbolic links are not followed. If the file is a symbolic link,
     *     then the symbolic link itself, not the target of the link, is copied.
     *     It is implementation specific if file attributes can be copied to the
     *     new link. In other words, the {@code COPY_ATTRIBUTES} option may be
     *     ignored when copying a symbolic link. </td>
     * </tr>
     * </tbody>
     * </table>
     *
     * <p> An implementation of this interface may support additional
     * implementation specific options.
     *
     * <p> Copying a file is not an atomic operation. If an {@link IOException}
     * is thrown, then it is possible that the target file is incomplete or some
     * of its file attributes have not been copied from the source file. When
     * the {@code REPLACE_EXISTING} option is specified and the target file
     * exists, then the target file is replaced. The check for the existence of
     * the file and the creation of the new file may not be atomic with respect
     * to other file system activities.
     *
     * <p> <b>Usage Example:</b>
     * Suppose we want to copy a file into a directory, giving it the same file
     * name as the source file:
     * <pre>
     *     Path source = ...
     *     Path newdir = ...
     *     Files.copy(source, newdir.resolve(source.getFileName());
     * </pre>
     *
     * @param   source
     *          the path to the file to copy
     * @param   target
     *          the path to the target file (may be associated with a different
     *          provider to the source path)
     * @param   options
     *          options specifying how the copy should be done
     *
     * @return  the path to the target file
     *
     * @throws  UnsupportedOperationException
     *          if the array contains a copy option that is not supported
     * @throws  FileAlreadyExistsException
     *          if the target file exists but cannot be replaced because the
     *          {@code REPLACE_EXISTING} option is not specified <i>(optional
     *          specific exception)</i>
     * @throws  DirectoryNotEmptyException
     *          the {@code REPLACE_EXISTING} option is specified but the file
     *          cannot be replaced because it is a non-empty directory
     *          <i>(optional specific exception)</i>
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the source file, the
     *          {@link SecurityManager#checkWrite(String) checkWrite} is invoked
     *          to check write access to the target file. If a symbolic link is
     *          copied the security manager is invoked to check {@link
     *          LinkPermission}{@code ("symbolic")}.
     */
    public static Path copy(Path source, Path target, CopyOption... options)
        throws IOException
    {
        FileSystemProvider provider = provider(source);
        if (provider(target) == provider) {
            // same provider
            provider.copy(source, target, options);
        } else {
            // different providers
            CopyMoveHelper.copyToForeignTarget(source, target, options);
        }
        return target;
    }

    /**
     * Move or rename a file to a target file.
     *
     * <p> By default, this method attempts to move the file to the target
     * file, failing if the target file exists except if the source and
     * target are the {@link #isSameFile same} file, in which case this method
     * has no effect. If the file is a symbolic link then the symbolic link
     * itself, not the target of the link, is moved. This method may be
     * invoked to move an empty directory. In some implementations a directory
     * has entries for special files or links that are created when the
     * directory is created. In such implementations a directory is considered
     * empty when only the special entries exist. When invoked to move a
     * directory that is not empty then the directory is moved if it does not
     * require moving the entries in the directory.  For example, renaming a
     * directory on the same {@link FileStore} will usually not require moving
     * the entries in the directory. When moving a directory requires that its
     * entries be moved then this method fails (by throwing an {@code
     * IOException}). To move a <i>file tree</i> may involve copying rather
     * than moving directories and this can be done using the {@link
     * #copy copy} method in conjunction with the {@link
     * #walkFileTree Files.walkFileTree} utility method.
     *
     * <p> The {@code options} parameter may include any of the following:
     *
     * <table class="striped">
     * <caption style="display:none">Options</caption>
     * <thead>
     * <tr> <th scope="col">Option</th> <th scope="col">Description</th> </tr>
     * </thead>
     * <tbody>
     * <tr>
     *   <th scope="row"> {@link StandardCopyOption#REPLACE_EXISTING REPLACE_EXISTING} </th>
     *   <td> If the target file exists, then the target file is replaced if it
     *     is not a non-empty directory. If the target file exists and is a
     *     symbolic link, then the symbolic link itself, not the target of
     *     the link, is replaced. </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@link StandardCopyOption#ATOMIC_MOVE ATOMIC_MOVE} </th>
     *   <td> The move is performed as an atomic file system operation and all
     *     other options are ignored. If the target file exists then it is
     *     implementation specific if the existing file is replaced or this method
     *     fails by throwing an {@link IOException}. If the move cannot be
     *     performed as an atomic file system operation then {@link
     *     AtomicMoveNotSupportedException} is thrown. This can arise, for
     *     example, when the target location is on a different {@code FileStore}
     *     and would require that the file be copied, or target location is
     *     associated with a different provider to this object. </td>
     * </tbody>
     * </table>
     *
     * <p> An implementation of this interface may support additional
     * implementation specific options.
     *
     * <p> Moving a file will copy the {@link
     * BasicFileAttributes#lastModifiedTime last-modified-time} to the target
     * file if supported by both source and target file stores. Copying of file
     * timestamps may result in precision loss. An implementation may also
     * attempt to copy other file attributes but is not required to fail if the
     * file attributes cannot be copied. When the move is performed as
     * a non-atomic operation, and an {@code IOException} is thrown, then the
     * state of the files is not defined. The original file and the target file
     * may both exist, the target file may be incomplete or some of its file
     * attributes may not been copied from the original file.
     *
     * <p> <b>Usage Examples:</b>
     * Suppose we want to rename a file to "newname", keeping the file in the
     * same directory:
     * <pre>
     *     Path source = ...
     *     Files.move(source, source.resolveSibling("newname"));
     * </pre>
     * Alternatively, suppose we want to move a file to new directory, keeping
     * the same file name, and replacing any existing file of that name in the
     * directory:
     * <pre>
     *     Path source = ...
     *     Path newdir = ...
     *     Files.move(source, newdir.resolve(source.getFileName()), REPLACE_EXISTING);
     * </pre>
     *
     * @param   source
     *          the path to the file to move
     * @param   target
     *          the path to the target file (may be associated with a different
     *          provider to the source path)
     * @param   options
     *          options specifying how the move should be done
     *
     * @return  the path to the target file
     *
     * @throws  UnsupportedOperationException
     *          if the array contains a copy option that is not supported
     * @throws  FileAlreadyExistsException
     *          if the target file exists but cannot be replaced because the
     *          {@code REPLACE_EXISTING} option is not specified <i>(optional
     *          specific exception)</i>
     * @throws  DirectoryNotEmptyException
     *          the {@code REPLACE_EXISTING} option is specified but the file
     *          cannot be replaced because it is a non-empty directory, or the
     *          source is a non-empty directory containing entries that would
     *          be required to be moved <i>(optional specific exceptions)</i>
     * @throws  AtomicMoveNotSupportedException
     *          if the options array contains the {@code ATOMIC_MOVE} option but
     *          the file cannot be moved as an atomic file system operation.
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to both the source and
     *          target file.
     */
    public static Path move(Path source, Path target, CopyOption... options)
        throws IOException
    {
        FileSystemProvider provider = provider(source);
        if (provider(target) == provider) {
            // same provider
            provider.move(source, target, options);
        } else {
            // different providers
            CopyMoveHelper.moveToForeignTarget(source, target, options);
        }
        return target;
    }

    // -- Miscellaneous --

    /**
     * Reads the target of a symbolic link <i>(optional operation)</i>.
     *
     * <p> If the file system supports <a href="package-summary.html#links">symbolic
     * links</a> then this method is used to read the target of the link, failing
     * if the file is not a symbolic link. The target of the link need not exist.
     * The returned {@code Path} object will be associated with the same file
     * system as {@code link}.
     *
     * @param   link
     *          the path to the symbolic link
     *
     * @return  a {@code Path} object representing the target of the link
     *
     * @throws  UnsupportedOperationException
     *          if the implementation does not support symbolic links
     * @throws  NotLinkException
     *          if the target could otherwise not be read because the file
     *          is not a symbolic link <i>(optional specific exception)</i>
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager
     *          is installed, it checks that {@code FilePermission} has been
     *          granted with the "{@code readlink}" action to read the link.
     */
    public static Path readSymbolicLink(Path link) throws IOException {
        return provider(link).readSymbolicLink(link);
    }

    /**
     * Returns the {@link FileStore} representing the file store where a file
     * is located.
     *
     * <p> Once a reference to the {@code FileStore} is obtained it is
     * implementation specific if operations on the returned {@code FileStore},
     * or {@link FileStoreAttributeView} objects obtained from it, continue
     * to depend on the existence of the file. In particular the behavior is not
     * defined for the case that the file is deleted or moved to a different
     * file store.
     *
     * @param   path
     *          the path to the file
     *
     * @return  the file store where the file is stored
     *
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file, and in
     *          addition it checks
     *          {@link RuntimePermission}{@code ("getFileStoreAttributes")}
     */
    public static FileStore getFileStore(Path path) throws IOException {
        return provider(path).getFileStore(path);
    }

    /**
     * Tests if two paths locate the same file.
     *
     * <p> If both {@code Path} objects are {@link Path#equals(Object) equal}
     * then this method returns {@code true} without checking if the file exists.
     * If the two {@code Path} objects are associated with different providers
     * then this method returns {@code false}. Otherwise, this method checks if
     * both {@code Path} objects locate the same file, and depending on the
     * implementation, may require to open or access both files.
     *
     * <p> If the file system and files remain static, then this method implements
     * an equivalence relation for non-null {@code Paths}.
     * <ul>
     * <li>It is <i>reflexive</i>: for {@code Path} {@code f},
     *     {@code isSameFile(f,f)} should return {@code true}.
     * <li>It is <i>symmetric</i>: for two {@code Paths} {@code f} and {@code g},
     *     {@code isSameFile(f,g)} will equal {@code isSameFile(g,f)}.
     * <li>It is <i>transitive</i>: for three {@code Paths}
     *     {@code f}, {@code g}, and {@code h}, if {@code isSameFile(f,g)} returns
     *     {@code true} and {@code isSameFile(g,h)} returns {@code true}, then
     *     {@code isSameFile(f,h)} will return {@code true}.
     * </ul>
     *
     * @param   path
     *          one path to the file
     * @param   path2
     *          the other path
     *
     * @return  {@code true} if, and only if, the two paths locate the same file
     *
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to both files.
     *
     * @see java.nio.file.attribute.BasicFileAttributes#fileKey
     */
    public static boolean isSameFile(Path path, Path path2) throws IOException {
        return provider(path).isSameFile(path, path2);
    }

    /**
     * Finds and returns the position of the first mismatched byte in the content
     * of two files, or {@code -1L} if there is no mismatch. The position will be
     * in the inclusive range of {@code 0L} up to the size (in bytes) of the
     * smaller file.
     *
     * <p> Two files are considered to match if they satisfy one of the following
     * conditions:
     * <ul>
     * <li> The two paths locate the {@linkplain #isSameFile(Path, Path) same file},
     *      even if two {@linkplain Path#equals(Object) equal} paths locate a file
     *      does not exist, or </li>
     * <li> The two files are the same size, and every byte in the first file
     *      is identical to the corresponding byte in the second file. </li>
     * </ul>
     *
     * <p> Otherwise there is a mismatch between the two files and the value
     * returned by this method is:
     * <ul>
     * <li> The position of the first mismatched byte, or </li>
     * <li> The size of the smaller file (in bytes) when the files are different
     *      sizes and every byte of the smaller file is identical to the
     *      corresponding byte of the larger file. </li>
     * </ul>
     *
     * <p> This method may not be atomic with respect to other file system
     * operations. This method is always <i>reflexive</i> (for {@code Path f},
     * {@code mismatch(f,f)} returns {@code -1L}). If the file system and files
     * remain static, then this method is <i>symmetric</i> (for two {@code Paths f}
     * and {@code g}, {@code mismatch(f,g)} will return the same value as
     * {@code mismatch(g,f)}).
     *
     * @param   path
     *          the path to the first file
     * @param   path2
     *          the path to the second file
     *
     * @return  the position of the first mismatch or {@code -1L} if no mismatch
     *
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to both files.
     *
     * @since 12
     */
    public static long mismatch(Path path, Path path2) throws IOException {
        if (isSameFile(path, path2)) {
            return -1;
        }
        byte[] buffer1 = new byte[BUFFER_SIZE];
        byte[] buffer2 = new byte[BUFFER_SIZE];
        try (InputStream in1 = Files.newInputStream(path);
             InputStream in2 = Files.newInputStream(path2);) {
            long totalRead = 0;
            while (true) {
                int nRead1 = in1.readNBytes(buffer1, 0, BUFFER_SIZE);
                int nRead2 = in2.readNBytes(buffer2, 0, BUFFER_SIZE);

                int i = Arrays.mismatch(buffer1, 0, nRead1, buffer2, 0, nRead2);
                if (i > -1) {
                    return totalRead + i;
                }
                if (nRead1 < BUFFER_SIZE) {
                    // we've reached the end of the files, but found no mismatch
                    return -1;
                }
                totalRead += nRead1;
            }
        }
    }

    /**
     * Tells whether or not a file is considered <em>hidden</em>.
     *
     * @apiNote
     * The exact definition of hidden is platform or provider dependent. On UNIX
     * for example a file is considered to be hidden if its name begins with a
     * period character ('.'). On Windows a file is considered hidden if the DOS
     * {@link DosFileAttributes#isHidden hidden} attribute is set.
     *
     * <p> Depending on the implementation this method may require to access
     * the file system to determine if the file is considered hidden.
     *
     * @param   path
     *          the path to the file to test
     *
     * @return  {@code true} if the file is considered hidden
     *
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file.
     */
    public static boolean isHidden(Path path) throws IOException {
        return provider(path).isHidden(path);
    }

    // lazy loading of default and installed file type detectors
    private static class FileTypeDetectors{
        static final FileTypeDetector defaultFileTypeDetector =
            createDefaultFileTypeDetector();
        static final List<FileTypeDetector> installedDetectors =
            loadInstalledDetectors();

        // creates the default file type detector
        @SuppressWarnings("removal")
        private static FileTypeDetector createDefaultFileTypeDetector() {
            return AccessController
                .doPrivileged(new PrivilegedAction<>() {
                    @Override public FileTypeDetector run() {
                        return sun.nio.fs.DefaultFileTypeDetector.create();
                }});
        }

        // loads all installed file type detectors
        @SuppressWarnings("removal")
        private static List<FileTypeDetector> loadInstalledDetectors() {
            return AccessController
                .doPrivileged(new PrivilegedAction<>() {
                    @Override public List<FileTypeDetector> run() {
                        List<FileTypeDetector> list = new ArrayList<>();
                        ServiceLoader<FileTypeDetector> loader = ServiceLoader
                            .load(FileTypeDetector.class, ClassLoader.getSystemClassLoader());
                        for (FileTypeDetector detector: loader) {
                            list.add(detector);
                        }
                        return list;
                }});
        }
    }

    /**
     * Probes the content type of a file.
     *
     * <p> This method uses the installed {@link FileTypeDetector} implementations
     * to probe the given file to determine its content type. Each file type
     * detector's {@link FileTypeDetector#probeContentType probeContentType} is
     * invoked, in turn, to probe the file type. If the file is recognized then
     * the content type is returned. If the file is not recognized by any of the
     * installed file type detectors then a system-default file type detector is
     * invoked to guess the content type.
     *
     * <p> A given invocation of the Java virtual machine maintains a system-wide
     * list of file type detectors. Installed file type detectors are loaded
     * using the service-provider loading facility defined by the {@link ServiceLoader}
     * class. Installed file type detectors are loaded using the system class
     * loader. If the system class loader cannot be found then the platform class
     * loader is used. File type detectors are typically installed
     * by placing them in a JAR file on the application class path,
     * the JAR file contains a provider-configuration file
     * named {@code java.nio.file.spi.FileTypeDetector} in the resource directory
     * {@code META-INF/services}, and the file lists one or more fully-qualified
     * names of concrete subclass of {@code FileTypeDetector } that have a zero
     * argument constructor. If the process of locating or instantiating the
     * installed file type detectors fails then an unspecified error is thrown.
     * The ordering that installed providers are located is implementation
     * specific.
     *
     * <p> The return value of this method is the string form of the value of a
     * Multipurpose Internet Mail Extension (MIME) content type as
     * defined by <a href="http://www.ietf.org/rfc/rfc2045.txt"><i>RFC&nbsp;2045:
     * Multipurpose Internet Mail Extensions (MIME) Part One: Format of Internet
     * Message Bodies</i></a>. The string is guaranteed to be parsable according
     * to the grammar in the RFC.
     *
     * @param   path
     *          the path to the file to probe
     *
     * @return  The content type of the file, or {@code null} if the content
     *          type cannot be determined
     *
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          If a security manager is installed and it denies an unspecified
     *          permission required by a file type detector implementation.
     */
    public static String probeContentType(Path path)
        throws IOException
    {
        // try installed file type detectors
        for (FileTypeDetector detector: FileTypeDetectors.installedDetectors) {
            String result = detector.probeContentType(path);
            if (result != null)
                return result;
        }

        // fallback to default
        return FileTypeDetectors.defaultFileTypeDetector.probeContentType(path);
    }

    // -- File Attributes --

    /**
     * Returns a file attribute view of a given type.
     *
     * <p> A file attribute view provides a read-only or updatable view of a
     * set of file attributes. This method is intended to be used where the file
     * attribute view defines type-safe methods to read or update the file
     * attributes. The {@code type} parameter is the type of the attribute view
     * required and the method returns an instance of that type if supported.
     * The {@link BasicFileAttributeView} type supports access to the basic
     * attributes of a file. Invoking this method to select a file attribute
     * view of that type will always return an instance of that class.
     *
     * <p> The {@code options} array may be used to indicate how symbolic links
     * are handled by the resulting file attribute view for the case that the
     * file is a symbolic link. By default, symbolic links are followed. If the
     * option {@link LinkOption#NOFOLLOW_LINKS NOFOLLOW_LINKS} is present then
     * symbolic links are not followed. This option is ignored by implementations
     * that do not support symbolic links.
     *
     * <p> <b>Usage Example:</b>
     * Suppose we want read or set a file's ACL, if supported:
     * <pre>
     *     Path path = ...
     *     AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);
     *     if (view != null) {
     *         List&lt;AclEntry&gt; acl = view.getAcl();
     *         :
     *     }
     * </pre>
     *
     * @param   <V>
     *          The {@code FileAttributeView} type
     * @param   path
     *          the path to the file
     * @param   type
     *          the {@code Class} object corresponding to the file attribute view
     * @param   options
     *          options indicating how symbolic links are handled
     *
     * @return  a file attribute view of the specified type, or {@code null} if
     *          the attribute view type is not available
     */
    public static <V extends FileAttributeView> V getFileAttributeView(Path path,
                                                                       Class<V> type,
                                                                       LinkOption... options)
    {
        return provider(path).getFileAttributeView(path, type, options);
    }

    /**
     * Reads a file's attributes as a bulk operation.
     *
     * <p> The {@code type} parameter is the type of the attributes required
     * and this method returns an instance of that type if supported. All
     * implementations support a basic set of file attributes and so invoking
     * this method with a  {@code type} parameter of {@code
     * BasicFileAttributes.class} will not throw {@code
     * UnsupportedOperationException}.
     *
     * <p> The {@code options} array may be used to indicate how symbolic links
     * are handled for the case that the file is a symbolic link. By default,
     * symbolic links are followed and the file attribute of the final target
     * of the link is read. If the option {@link LinkOption#NOFOLLOW_LINKS
     * NOFOLLOW_LINKS} is present then symbolic links are not followed.
     *
     * <p> It is implementation specific if all file attributes are read as an
     * atomic operation with respect to other file system operations.
     *
     * <p> <b>Usage Example:</b>
     * Suppose we want to read a file's attributes in bulk:
     * <pre>
     *    Path path = ...
     *    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
     * </pre>
     * Alternatively, suppose we want to read file's POSIX attributes without
     * following symbolic links:
     * <pre>
     *    PosixFileAttributes attrs =
     *        Files.readAttributes(path, PosixFileAttributes.class, NOFOLLOW_LINKS);
     * </pre>
     *
     * @param   <A>
     *          The {@code BasicFileAttributes} type
     * @param   path
     *          the path to the file
     * @param   type
     *          the {@code Class} of the file attributes required
     *          to read
     * @param   options
     *          options indicating how symbolic links are handled
     *
     * @return  the file attributes
     *
     * @throws  UnsupportedOperationException
     *          if an attributes of the given type are not supported
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, its {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file. If this
     *          method is invoked to read security sensitive attributes then the
     *          security manager may be invoke to check for additional permissions.
     */
    public static <A extends BasicFileAttributes> A readAttributes(Path path,
                                                                   Class<A> type,
                                                                   LinkOption... options)
        throws IOException
    {
        return provider(path).readAttributes(path, type, options);
    }

    /**
     * Sets the value of a file attribute.
     *
     * <p> The {@code attribute} parameter identifies the attribute to be set
     * and takes the form:
     * <blockquote>
     * [<i>view-name</i><b>:</b>]<i>attribute-name</i>
     * </blockquote>
     * where square brackets [...] delineate an optional component and the
     * character {@code ':'} stands for itself.
     *
     * <p> <i>view-name</i> is the {@link FileAttributeView#name name} of a {@link
     * FileAttributeView} that identifies a set of file attributes. If not
     * specified then it defaults to {@code "basic"}, the name of the file
     * attribute view that identifies the basic set of file attributes common to
     * many file systems. <i>attribute-name</i> is the name of the attribute
     * within the set.
     *
     * <p> The {@code options} array may be used to indicate how symbolic links
     * are handled for the case that the file is a symbolic link. By default,
     * symbolic links are followed and the file attribute of the final target
     * of the link is set. If the option {@link LinkOption#NOFOLLOW_LINKS
     * NOFOLLOW_LINKS} is present then symbolic links are not followed.
     *
     * <p> <b>Usage Example:</b>
     * Suppose we want to set the DOS "hidden" attribute:
     * <pre>
     *    Path path = ...
     *    Files.setAttribute(path, "dos:hidden", true);
     * </pre>
     *
     * @param   path
     *          the path to the file
     * @param   attribute
     *          the attribute to set
     * @param   value
     *          the attribute value
     * @param   options
     *          options indicating how symbolic links are handled
     *
     * @return  the given path
     *
     * @throws  UnsupportedOperationException
     *          if the attribute view is not available
     * @throws  IllegalArgumentException
     *          if the attribute name is not specified, or is not recognized, or
     *          the attribute value is of the correct type but has an
     *          inappropriate value
     * @throws  ClassCastException
     *          if the attribute value is not of the expected type or is a
     *          collection containing elements that are not of the expected
     *          type
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, its {@link SecurityManager#checkWrite(String) checkWrite}
     *          method denies write access to the file. If this method is invoked
     *          to set security sensitive attributes then the security manager
     *          may be invoked to check for additional permissions.
     */
    public static Path setAttribute(Path path, String attribute, Object value,
                                    LinkOption... options)
        throws IOException
    {
        provider(path).setAttribute(path, attribute, value, options);
        return path;
    }

    /**
     * Reads the value of a file attribute.
     *
     * <p> The {@code attribute} parameter identifies the attribute to be read
     * and takes the form:
     * <blockquote>
     * [<i>view-name</i><b>:</b>]<i>attribute-name</i>
     * </blockquote>
     * where square brackets [...] delineate an optional component and the
     * character {@code ':'} stands for itself.
     *
     * <p> <i>view-name</i> is the {@link FileAttributeView#name name} of a {@link
     * FileAttributeView} that identifies a set of file attributes. If not
     * specified then it defaults to {@code "basic"}, the name of the file
     * attribute view that identifies the basic set of file attributes common to
     * many file systems. <i>attribute-name</i> is the name of the attribute.
     *
     * <p> The {@code options} array may be used to indicate how symbolic links
     * are handled for the case that the file is a symbolic link. By default,
     * symbolic links are followed and the file attribute of the final target
     * of the link is read. If the option {@link LinkOption#NOFOLLOW_LINKS
     * NOFOLLOW_LINKS} is present then symbolic links are not followed.
     *
     * <p> <b>Usage Example:</b>
     * Suppose we require the user ID of the file owner on a system that
     * supports a "{@code unix}" view:
     * <pre>
     *    Path path = ...
     *    int uid = (Integer)Files.getAttribute(path, "unix:uid");
     * </pre>
     *
     * @param   path
     *          the path to the file
     * @param   attribute
     *          the attribute to read
     * @param   options
     *          options indicating how symbolic links are handled
     *
     * @return  the attribute value
     *
     * @throws  UnsupportedOperationException
     *          if the attribute view is not available
     * @throws  IllegalArgumentException
     *          if the attribute name is not specified or is not recognized
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, its {@link SecurityManager#checkRead(String) checkRead}
     *          method denies read access to the file. If this method is invoked
     *          to read security sensitive attributes then the security manager
     *          may be invoked to check for additional permissions.
     */
    public static Object getAttribute(Path path, String attribute,
                                      LinkOption... options)
        throws IOException
    {
        // only one attribute should be read
        if (attribute.indexOf('*') >= 0 || attribute.indexOf(',') >= 0)
            throw new IllegalArgumentException(attribute);
        Map<String,Object> map = readAttributes(path, attribute, options);
        assert map.size() == 1;
        String name;
        int pos = attribute.indexOf(':');
        if (pos == -1) {
            name = attribute;
        } else {
            name = (pos == attribute.length()) ? "" : attribute.substring(pos+1);
        }
        return map.get(name);
    }

    /**
     * Reads a set of file attributes as a bulk operation.
     *
     * <p> The {@code attributes} parameter identifies the attributes to be read
     * and takes the form:
     * <blockquote>
     * [<i>view-name</i><b>:</b>]<i>attribute-list</i>
     * </blockquote>
     * where square brackets [...] delineate an optional component and the
     * character {@code ':'} stands for itself.
     *
     * <p> <i>view-name</i> is the {@link FileAttributeView#name name} of a {@link
     * FileAttributeView} that identifies a set of file attributes. If not
     * specified then it defaults to {@code "basic"}, the name of the file
     * attribute view that identifies the basic set of file attributes common to
     * many file systems.
     *
     * <p> The <i>attribute-list</i> component is a comma separated list of
     * one or more names of attributes to read. If the list contains the value
     * {@code "*"} then all attributes are read. Attributes that are not supported
     * are ignored and will not be present in the returned map. It is
     * implementation specific if all attributes are read as an atomic operation
     * with respect to other file system operations.
     *
     * <p> The following examples demonstrate possible values for the {@code
     * attributes} parameter:
     *
     * <table class="striped" style="text-align: left; margin-left:2em">
     * <caption style="display:none">Possible values</caption>
     * <thead>
     * <tr>
     *  <th scope="col">Example
     *  <th scope="col">Description
     * </thead>
     * <tbody>
     * <tr>
     *   <th scope="row"> {@code "*"} </th>
     *   <td> Read all {@link BasicFileAttributes basic-file-attributes}. </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@code "size,lastModifiedTime,lastAccessTime"} </th>
     *   <td> Reads the file size, last modified time, and last access time
     *     attributes. </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@code "posix:*"} </th>
     *   <td> Read all {@link PosixFileAttributes POSIX-file-attributes}. </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@code "posix:permissions,owner,size"} </th>
     *   <td> Reads the POSIX file permissions, owner, and file size. </td>
     * </tr>
     * </tbody>
     * </table>
     *
     * <p> The {@code options} array may be used to indicate how symbolic links
     * are handled for the case that the file is a symbolic link. By default,
     * symbolic links are followed and the file attribute of the final target
     * of the link is read. If the option {@link LinkOption#NOFOLLOW_LINKS
     * NOFOLLOW_LINKS} is present then symbolic links are not followed.
     *
     * @param   path
     *          the path to the file
     * @param   attributes
     *          the attributes to read
     * @param   options
     *          options indicating how symbolic links are handled
     *
     * @return  a map of the attributes returned; The map's keys are the
     *          attribute names, its values are the attribute values
     *
     * @throws  UnsupportedOperationException
     *          if the attribute view is not available
     * @throws  IllegalArgumentException
     *          if no attributes are specified or an unrecognized attribute is
     *          specified
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, its {@link SecurityManager#checkRead(String) checkRead}
     *          method denies read access to the file. If this method is invoked
     *          to read security sensitive attributes then the security manager
     *          may be invoke to check for additional permissions.
     */
    public static Map<String,Object> readAttributes(Path path, String attributes,
                                                    LinkOption... options)
        throws IOException
    {
        return provider(path).readAttributes(path, attributes, options);
    }

    /**
     * Returns a file's POSIX file permissions.
     *
     * <p> The {@code path} parameter is associated with a {@code FileSystem}
     * that supports the {@link PosixFileAttributeView}. This attribute view
     * provides access to file attributes commonly associated with files on file
     * systems used by operating systems that implement the Portable Operating
     * System Interface (POSIX) family of standards.
     *
     * <p> The {@code options} array may be used to indicate how symbolic links
     * are handled for the case that the file is a symbolic link. By default,
     * symbolic links are followed and the file attribute of the final target
     * of the link is read. If the option {@link LinkOption#NOFOLLOW_LINKS
     * NOFOLLOW_LINKS} is present then symbolic links are not followed.
     *
     * @param   path
     *          the path to the file
     * @param   options
     *          options indicating how symbolic links are handled
     *
     * @return  the file permissions
     *
     * @throws  UnsupportedOperationException
     *          if the associated file system does not support the {@code
     *          PosixFileAttributeView}
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, a security manager is
     *          installed, and it denies
     *          {@link RuntimePermission}{@code ("accessUserInformation")}
     *          or its {@link SecurityManager#checkRead(String) checkRead} method
     *          denies read access to the file.
     */
    public static Set<PosixFilePermission> getPosixFilePermissions(Path path,
                                                                   LinkOption... options)
        throws IOException
    {
        return readAttributes(path, PosixFileAttributes.class, options).permissions();
    }

    /**
     * Sets a file's POSIX permissions.
     *
     * <p> The {@code path} parameter is associated with a {@code FileSystem}
     * that supports the {@link PosixFileAttributeView}. This attribute view
     * provides access to file attributes commonly associated with files on file
     * systems used by operating systems that implement the Portable Operating
     * System Interface (POSIX) family of standards.
     *
     * @param   path
     *          The path to the file
     * @param   perms
     *          The new set of permissions
     *
     * @return  The given path
     *
     * @throws  UnsupportedOperationException
     *          if the associated file system does not support the {@code
     *          PosixFileAttributeView}
     * @throws  ClassCastException
     *          if the sets contains elements that are not of type {@code
     *          PosixFilePermission}
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, it denies
     *          {@link RuntimePermission}{@code ("accessUserInformation")}
     *          or its {@link SecurityManager#checkWrite(String) checkWrite}
     *          method denies write access to the file.
     */
    public static Path setPosixFilePermissions(Path path,
                                               Set<PosixFilePermission> perms)
        throws IOException
    {
        PosixFileAttributeView view =
            getFileAttributeView(path, PosixFileAttributeView.class);
        if (view == null)
            throw new UnsupportedOperationException();
        view.setPermissions(perms);
        return path;
    }

    /**
     * Returns the owner of a file.
     *
     * <p> The {@code path} parameter is associated with a file system that
     * supports {@link FileOwnerAttributeView}. This file attribute view provides
     * access to a file attribute that is the owner of the file.
     *
     * @param   path
     *          The path to the file
     * @param   options
     *          options indicating how symbolic links are handled
     *
     * @return  A user principal representing the owner of the file
     *
     * @throws  UnsupportedOperationException
     *          if the associated file system does not support the {@code
     *          FileOwnerAttributeView}
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, it denies
     *          {@link RuntimePermission}{@code ("accessUserInformation")}
     *          or its {@link SecurityManager#checkRead(String) checkRead} method
     *          denies read access to the file.
     */
    public static UserPrincipal getOwner(Path path, LinkOption... options) throws IOException {
        FileOwnerAttributeView view =
            getFileAttributeView(path, FileOwnerAttributeView.class, options);
        if (view == null)
            throw new UnsupportedOperationException();
        return view.getOwner();
    }

    /**
     * Updates the file owner.
     *
     * <p> The {@code path} parameter is associated with a file system that
     * supports {@link FileOwnerAttributeView}. This file attribute view provides
     * access to a file attribute that is the owner of the file.
     *
     * <p> <b>Usage Example:</b>
     * Suppose we want to make "joe" the owner of a file:
     * <pre>
     *     Path path = ...
     *     UserPrincipalLookupService lookupService =
     *         provider(path).getUserPrincipalLookupService();
     *     UserPrincipal joe = lookupService.lookupPrincipalByName("joe");
     *     Files.setOwner(path, joe);
     * </pre>
     *
     * @param   path
     *          The path to the file
     * @param   owner
     *          The new file owner
     *
     * @return  The given path
     *
     * @throws  UnsupportedOperationException
     *          if the associated file system does not support the {@code
     *          FileOwnerAttributeView}
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, it denies
     *          {@link RuntimePermission}{@code ("accessUserInformation")}
     *          or its {@link SecurityManager#checkWrite(String) checkWrite}
     *          method denies write access to the file.
     *
     * @see FileSystem#getUserPrincipalLookupService
     * @see java.nio.file.attribute.UserPrincipalLookupService
     */
    public static Path setOwner(Path path, UserPrincipal owner)
        throws IOException
    {
        FileOwnerAttributeView view =
            getFileAttributeView(path, FileOwnerAttributeView.class);
        if (view == null)
            throw new UnsupportedOperationException();
        view.setOwner(owner);
        return path;
    }

    /**
     * Tests whether a file is a symbolic link.
     *
     * <p> Where it is required to distinguish an I/O exception from the case
     * that the file is not a symbolic link then the file attributes can be
     * read with the {@link #readAttributes(Path,Class,LinkOption[])
     * readAttributes} method and the file type tested with the {@link
     * BasicFileAttributes#isSymbolicLink} method.
     *
     * @param   path  The path to the file
     *
     * @return  {@code true} if the file is a symbolic link; {@code false} if
     *          the file does not exist, is not a symbolic link, or it cannot
     *          be determined if the file is a symbolic link or not.
     *
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, its {@link SecurityManager#checkRead(String) checkRead}
     *          method denies read access to the file.
     */
    public static boolean isSymbolicLink(Path path) {
        try {
            return readAttributes(path,
                                  BasicFileAttributes.class,
                                  LinkOption.NOFOLLOW_LINKS).isSymbolicLink();
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Tests whether a file is a directory.
     *
     * <p> The {@code options} array may be used to indicate how symbolic links
     * are handled for the case that the file is a symbolic link. By default,
     * symbolic links are followed and the file attribute of the final target
     * of the link is read. If the option {@link LinkOption#NOFOLLOW_LINKS
     * NOFOLLOW_LINKS} is present then symbolic links are not followed.
     *
     * <p> Where it is required to distinguish an I/O exception from the case
     * that the file is not a directory then the file attributes can be
     * read with the {@link #readAttributes(Path,Class,LinkOption[])
     * readAttributes} method and the file type tested with the {@link
     * BasicFileAttributes#isDirectory} method.
     *
     * @param   path
     *          the path to the file to test
     * @param   options
     *          options indicating how symbolic links are handled
     *
     * @return  {@code true} if the file is a directory; {@code false} if
     *          the file does not exist, is not a directory, or it cannot
     *          be determined if the file is a directory or not.
     *
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, its {@link SecurityManager#checkRead(String) checkRead}
     *          method denies read access to the file.
     */
    public static boolean isDirectory(Path path, LinkOption... options) {
        if (options.length == 0) {
            FileSystemProvider provider = provider(path);
            if (provider instanceof AbstractFileSystemProvider)
                return ((AbstractFileSystemProvider)provider).isDirectory(path);
        }

        try {
            return readAttributes(path, BasicFileAttributes.class, options).isDirectory();
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Tests whether a file is a regular file with opaque content.
     *
     * <p> The {@code options} array may be used to indicate how symbolic links
     * are handled for the case that the file is a symbolic link. By default,
     * symbolic links are followed and the file attribute of the final target
     * of the link is read. If the option {@link LinkOption#NOFOLLOW_LINKS
     * NOFOLLOW_LINKS} is present then symbolic links are not followed.
     *
     * <p> Where it is required to distinguish an I/O exception from the case
     * that the file is not a regular file then the file attributes can be
     * read with the {@link #readAttributes(Path,Class,LinkOption[])
     * readAttributes} method and the file type tested with the {@link
     * BasicFileAttributes#isRegularFile} method.
     *
     * @param   path
     *          the path to the file
     * @param   options
     *          options indicating how symbolic links are handled
     *
     * @return  {@code true} if the file is a regular file; {@code false} if
     *          the file does not exist, is not a regular file, or it
     *          cannot be determined if the file is a regular file or not.
     *
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, its {@link SecurityManager#checkRead(String) checkRead}
     *          method denies read access to the file.
     */
    public static boolean isRegularFile(Path path, LinkOption... options) {
        if (options.length == 0) {
            FileSystemProvider provider = provider(path);
            if (provider instanceof AbstractFileSystemProvider)
                return ((AbstractFileSystemProvider)provider).isRegularFile(path);
        }

        try {
            return readAttributes(path, BasicFileAttributes.class, options).isRegularFile();
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Returns a file's last modified time.
     *
     * <p> The {@code options} array may be used to indicate how symbolic links
     * are handled for the case that the file is a symbolic link. By default,
     * symbolic links are followed and the file attribute of the final target
     * of the link is read. If the option {@link LinkOption#NOFOLLOW_LINKS
     * NOFOLLOW_LINKS} is present then symbolic links are not followed.
     *
     * @param   path
     *          the path to the file
     * @param   options
     *          options indicating how symbolic links are handled
     *
     * @return  a {@code FileTime} representing the time the file was last
     *          modified, or an implementation specific default when a time
     *          stamp to indicate the time of last modification is not supported
     *          by the file system
     *
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, its {@link SecurityManager#checkRead(String) checkRead}
     *          method denies read access to the file.
     *
     * @see BasicFileAttributes#lastModifiedTime
     */
    public static FileTime getLastModifiedTime(Path path, LinkOption... options)
        throws IOException
    {
        return readAttributes(path, BasicFileAttributes.class, options).lastModifiedTime();
    }

    /**
     * Updates a file's last modified time attribute. The file time is converted
     * to the epoch and precision supported by the file system. Converting from
     * finer to coarser granularities result in precision loss. The behavior of
     * this method when attempting to set the last modified time when it is not
     * supported by the file system or is outside the range supported by the
     * underlying file store is not defined. It may or not fail by throwing an
     * {@code IOException}.
     *
     * <p> <b>Usage Example:</b>
     * Suppose we want to set the last modified time to the current time:
     * <pre>
     *    Path path = ...
     *    FileTime now = FileTime.fromMillis(System.currentTimeMillis());
     *    Files.setLastModifiedTime(path, now);
     * </pre>
     *
     * @param   path
     *          the path to the file
     * @param   time
     *          the new last modified time
     *
     * @return  the given path
     *
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, its {@link SecurityManager#checkWrite(String)
     *          checkWrite} method denies write access to the file.
     *
     * @see BasicFileAttributeView#setTimes
     */
    public static Path setLastModifiedTime(Path path, FileTime time)
        throws IOException
    {
        getFileAttributeView(path, BasicFileAttributeView.class)
            .setTimes(Objects.requireNonNull(time), null, null);
        return path;
    }

    /**
     * Returns the size of a file (in bytes). The size may differ from the
     * actual size on the file system due to compression, support for sparse
     * files, or other reasons. The size of files that are not {@link
     * #isRegularFile regular} files is implementation specific and
     * therefore unspecified.
     *
     * @param   path
     *          the path to the file
     *
     * @return  the file size, in bytes
     *
     * @throws  IOException
     *          if an I/O error occurs
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, its {@link SecurityManager#checkRead(String) checkRead}
     *          method denies read access to the file.
     *
     * @see BasicFileAttributes#size
     */
    public static long size(Path path) throws IOException {
        return readAttributes(path, BasicFileAttributes.class).size();
    }

    // -- Accessibility --

    /**
     * Returns {@code false} if NOFOLLOW_LINKS is present.
     */
    private static boolean followLinks(LinkOption... options) {
        boolean followLinks = true;
        for (LinkOption opt: options) {
            if (opt == LinkOption.NOFOLLOW_LINKS) {
                followLinks = false;
                continue;
            }
            if (opt == null)
                throw new NullPointerException();
            throw new AssertionError("Should not get here");
        }
        return followLinks;
    }

    /**
     * Tests whether a file exists.
     *
     * <p> The {@code options} parameter may be used to indicate how symbolic links
     * are handled for the case that the file is a symbolic link. By default,
     * symbolic links are followed. If the option {@link LinkOption#NOFOLLOW_LINKS
     * NOFOLLOW_LINKS} is present then symbolic links are not followed.
     *
     * <p> Note that the result of this method is immediately outdated. If this
     * method indicates the file exists then there is no guarantee that a
     * subsequent access will succeed. Care should be taken when using this
     * method in security sensitive applications.
     *
     * @param   path
     *          the path to the file to test
     * @param   options
     *          options indicating how symbolic links are handled
     * .
     * @return  {@code true} if the file exists; {@code false} if the file does
     *          not exist or its existence cannot be determined.
     *
     * @throws  SecurityException
     *          In the case of the default provider, the {@link
     *          SecurityManager#checkRead(String)} is invoked to check
     *          read access to the file.
     *
     * @see #notExists
     */
    public static boolean exists(Path path, LinkOption... options) {
        if (options.length == 0) {
            FileSystemProvider provider = provider(path);
            if (provider instanceof AbstractFileSystemProvider)
                return ((AbstractFileSystemProvider)provider).exists(path);
        }

        try {
            if (followLinks(options)) {
                provider(path).checkAccess(path);
            } else {
                // attempt to read attributes without following links
                readAttributes(path, BasicFileAttributes.class,
                               LinkOption.NOFOLLOW_LINKS);
            }
            // file exists
            return true;
        } catch (IOException x) {
            // does not exist or unable to determine if file exists
            return false;
        }

    }

    /**
     * Tests whether the file located by this path does not exist. This method
     * is intended for cases where it is required to take action when it can be
     * confirmed that a file does not exist.
     *
     * <p> The {@code options} parameter may be used to indicate how symbolic links
     * are handled for the case that the file is a symbolic link. By default,
     * symbolic links are followed. If the option {@link LinkOption#NOFOLLOW_LINKS
     * NOFOLLOW_LINKS} is present then symbolic links are not followed.
     *
     * <p> Note that this method is not the complement of the {@link #exists
     * exists} method. Where it is not possible to determine if a file exists
     * or not then both methods return {@code false}. As with the {@code exists}
     * method, the result of this method is immediately outdated. If this
     * method indicates the file does exist then there is no guarantee that a
     * subsequent attempt to create the file will succeed. Care should be taken
     * when using this method in security sensitive applications.
     *
     * @param   path
     *          the path to the file to test
     * @param   options
     *          options indicating how symbolic links are handled
     *
     * @return  {@code true} if the file does not exist; {@code false} if the
     *          file exists or its existence cannot be determined
     *
     * @throws  SecurityException
     *          In the case of the default provider, the {@link
     *          SecurityManager#checkRead(String)} is invoked to check
     *          read access to the file.
     */
    public static boolean notExists(Path path, LinkOption... options) {
        try {
            if (followLinks(options)) {
                provider(path).checkAccess(path);
            } else {
                // attempt to read attributes without following links
                readAttributes(path, BasicFileAttributes.class,
                               LinkOption.NOFOLLOW_LINKS);
            }
            // file exists
            return false;
        } catch (NoSuchFileException x) {
            // file confirmed not to exist
            return true;
        } catch (IOException x) {
            return false;
        }
    }

    /**
     * Used by isReadable, isWritable, isExecutable to test access to a file.
     */
    private static boolean isAccessible(Path path, AccessMode... modes) {
        try {
            provider(path).checkAccess(path, modes);
            return true;
        } catch (IOException x) {
            return false;
        }
    }

    /**
     * Tests whether a file is readable. This method checks that a file exists
     * and that this Java virtual machine has appropriate privileges that would
     * allow it open the file for reading. Depending on the implementation, this
     * method may require to read file permissions, access control lists, or
     * other file attributes in order to check the effective access to the file.
     * Consequently, this method may not be atomic with respect to other file
     * system operations.
     *
     * <p> Note that the result of this method is immediately outdated, there is
     * no guarantee that a subsequent attempt to open the file for reading will
     * succeed (or even that it will access the same file). Care should be taken
     * when using this method in security sensitive applications.
     *
     * @param   path
     *          the path to the file to check
     *
     * @return  {@code true} if the file exists and is readable; {@code false}
     *          if the file does not exist, read access would be denied because
     *          the Java virtual machine has insufficient privileges, or access
     *          cannot be determined
     *
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          is invoked to check read access to the file.
     */
    public static boolean isReadable(Path path) {
        return isAccessible(path, AccessMode.READ);
    }

    /**
     * Tests whether a file is writable. This method checks that a file exists
     * and that this Java virtual machine has appropriate privileges that would
     * allow it open the file for writing. Depending on the implementation, this
     * method may require to read file permissions, access control lists, or
     * other file attributes in order to check the effective access to the file.
     * Consequently, this method may not be atomic with respect to other file
     * system operations.
     *
     * <p> Note that result of this method is immediately outdated, there is no
     * guarantee that a subsequent attempt to open the file for writing will
     * succeed (or even that it will access the same file). Care should be taken
     * when using this method in security sensitive applications.
     *
     * @param   path
     *          the path to the file to check
     *
     * @return  {@code true} if the file exists and is writable; {@code false}
     *          if the file does not exist, write access would be denied because
     *          the Java virtual machine has insufficient privileges, or access
     *          cannot be determined
     *
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          is invoked to check write access to the file.
     */
    public static boolean isWritable(Path path) {
        return isAccessible(path, AccessMode.WRITE);
    }

    /**
     * Tests whether a file is executable. This method checks that a file exists
     * and that this Java virtual machine has appropriate privileges to {@link
     * Runtime#exec execute} the file. The semantics may differ when checking
     * access to a directory. For example, on UNIX systems, checking for
     * execute access checks that the Java virtual machine has permission to
     * search the directory in order to access file or subdirectories.
     *
     * <p> Depending on the implementation, this method may require to read file
     * permissions, access control lists, or other file attributes in order to
     * check the effective access to the file. Consequently, this method may not
     * be atomic with respect to other file system operations.
     *
     * <p> Note that the result of this method is immediately outdated, there is
     * no guarantee that a subsequent attempt to execute the file will succeed
     * (or even that it will access the same file). Care should be taken when
     * using this method in security sensitive applications.
     *
     * @param   path
     *          the path to the file to check
     *
     * @return  {@code true} if the file exists and is executable; {@code false}
     *          if the file does not exist, execute access would be denied because
     *          the Java virtual machine has insufficient privileges, or access
     *          cannot be determined
     *
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkExec(String)
     *          checkExec} is invoked to check execute access to the file.
     */
    public static boolean isExecutable(Path path) {
        return isAccessible(path, AccessMode.EXECUTE);
    }

    // -- Recursive operations --

    /**
     * Walks a file tree.
     *
     * <p> This method walks a file tree rooted at a given starting file. The
     * file tree traversal is <em>depth-first</em> with the given {@link
     * FileVisitor} invoked for each file encountered. File tree traversal
     * completes when all accessible files in the tree have been visited, or a
     * visit method returns a result of {@link FileVisitResult#TERMINATE
     * TERMINATE}. Where a visit method terminates due an {@code IOException},
     * an uncaught error, or runtime exception, then the traversal is terminated
     * and the error or exception is propagated to the caller of this method.
     *
     * <p> For each file encountered this method attempts to read its {@link
     * java.nio.file.attribute.BasicFileAttributes}. If the file is not a
     * directory then the {@link FileVisitor#visitFile visitFile} method is
     * invoked with the file attributes. If the file attributes cannot be read,
     * due to an I/O exception, then the {@link FileVisitor#visitFileFailed
     * visitFileFailed} method is invoked with the I/O exception.
     *
     * <p> Where the file is a directory, and the directory could not be opened,
     * then the {@code visitFileFailed} method is invoked with the I/O exception,
     * after which, the file tree walk continues, by default, at the next
     * <em>sibling</em> of the directory.
     *
     * <p> Where the directory is opened successfully, then the entries in the
     * directory, and their <em>descendants</em> are visited. When all entries
     * have been visited, or an I/O error occurs during iteration of the
     * directory, then the directory is closed and the visitor's {@link
     * FileVisitor#postVisitDirectory postVisitDirectory} method is invoked.
     * The file tree walk then continues, by default, at the next <em>sibling</em>
     * of the directory.
     *
     * <p> By default, symbolic links are not automatically followed by this
     * method. If the {@code options} parameter contains the {@link
     * FileVisitOption#FOLLOW_LINKS FOLLOW_LINKS} option then symbolic links are
     * followed. When following links, and the attributes of the target cannot
     * be read, then this method attempts to get the {@code BasicFileAttributes}
     * of the link. If they can be read then the {@code visitFile} method is
     * invoked with the attributes of the link (otherwise the {@code visitFileFailed}
     * method is invoked as specified above).
     *
     * <p> If the {@code options} parameter contains the {@link
     * FileVisitOption#FOLLOW_LINKS FOLLOW_LINKS} option then this method keeps
     * track of directories visited so that cycles can be detected. A cycle
     * arises when there is an entry in a directory that is an ancestor of the
     * directory. Cycle detection is done by recording the {@link
     * java.nio.file.attribute.BasicFileAttributes#fileKey file-key} of directories,
     * or if file keys are not available, by invoking the {@link #isSameFile
     * isSameFile} method to test if a directory is the same file as an
     * ancestor. When a cycle is detected it is treated as an I/O error, and the
     * {@link FileVisitor#visitFileFailed visitFileFailed} method is invoked with
     * an instance of {@link FileSystemLoopException}.
     *
     * <p> The {@code maxDepth} parameter is the maximum number of levels of
     * directories to visit. A value of {@code 0} means that only the starting
     * file is visited, unless denied by the security manager. A value of
     * {@link Integer#MAX_VALUE MAX_VALUE} may be used to indicate that all
     * levels should be visited. The {@code visitFile} method is invoked for all
     * files, including directories, encountered at {@code maxDepth}, unless the
     * basic file attributes cannot be read, in which case the {@code
     * visitFileFailed} method is invoked.
     *
     * <p> If a visitor returns a result of {@code null} then {@code
     * NullPointerException} is thrown.
     *
     * <p> When a security manager is installed and it denies access to a file
     * (or directory), then it is ignored and the visitor is not invoked for
     * that file (or directory).
     *
     * @param   start
     *          the starting file
     * @param   options
     *          options to configure the traversal
     * @param   maxDepth
     *          the maximum number of directory levels to visit
     * @param   visitor
     *          the file visitor to invoke for each file
     *
     * @return  the starting file
     *
     * @throws  IllegalArgumentException
     *          if the {@code maxDepth} parameter is negative
     * @throws  SecurityException
     *          If the security manager denies access to the starting file.
     *          In the case of the default provider, the {@link
     *          SecurityManager#checkRead(String) checkRead} method is invoked
     *          to check read access to the directory.
     * @throws  IOException
     *          if an I/O error is thrown by a visitor method
     */
    public static Path walkFileTree(Path start,
                                    Set<FileVisitOption> options,
                                    int maxDepth,
                                    FileVisitor<? super Path> visitor)
        throws IOException
    {
        /**
         * Create a FileTreeWalker to walk the file tree, invoking the visitor
         * for each event.
         */
        try (FileTreeWalker walker = new FileTreeWalker(options, maxDepth)) {
            FileTreeWalker.Event ev = walker.walk(start);
            do {
                FileVisitResult result = switch (ev.type()) {
                    case ENTRY -> {
                        IOException ioe = ev.ioeException();
                        if (ioe == null) {
                            assert ev.attributes() != null;
                            yield visitor.visitFile(ev.file(), ev.attributes());
                        } else {
                            yield visitor.visitFileFailed(ev.file(), ioe);
                        }
                    }
                    case START_DIRECTORY -> {
                        var res = visitor.preVisitDirectory(ev.file(), ev.attributes());

                        // if SKIP_SIBLINGS and SKIP_SUBTREE is returned then
                        // there shouldn't be any more events for the current
                        // directory.
                        if (res == FileVisitResult.SKIP_SUBTREE ||
                            res == FileVisitResult.SKIP_SIBLINGS)
                            walker.pop();
                        yield res;
                    }
                    case END_DIRECTORY -> {
                        var res = visitor.postVisitDirectory(ev.file(), ev.ioeException());

                        // SKIP_SIBLINGS is a no-op for postVisitDirectory
                        if (res == FileVisitResult.SKIP_SIBLINGS)
                            res = FileVisitResult.CONTINUE;
                        yield res;
                    }
                    default -> throw new AssertionError("Should not get here");
                };

                if (Objects.requireNonNull(result) != FileVisitResult.CONTINUE) {
                    if (result == FileVisitResult.TERMINATE) {
                        break;
                    } else if (result == FileVisitResult.SKIP_SIBLINGS) {
                        walker.skipRemainingSiblings();
                    }
                }
                ev = walker.next();
            } while (ev != null);
        }

        return start;
    }

    /**
     * Walks a file tree.
     *
     * <p> This method works as if invoking it were equivalent to evaluating the
     * expression:
     * <blockquote>{@link
     * walkFileTree(Path, Set<FileVisitOption>, int, FileVisitor<? super Path>)
     * Files.walkFileTree(start, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, visitor)
     * }</blockquote>
     * In other words, it does not follow symbolic links, and visits all levels
     * of the file tree.
     *
     * @param   start
     *          the starting file
     * @param   visitor
     *          the file visitor to invoke for each file
     *
     * @return  the starting file
     *
     * @throws  SecurityException
     *          If the security manager denies access to the starting file.
     *          In the case of the default provider, the {@link
     *          SecurityManager#checkRead(String) checkRead} method is invoked
     *          to check read access to the directory.
     * @throws  IOException
     *          if an I/O error is thrown by a visitor method
     */
    public static Path walkFileTree(Path start, FileVisitor<? super Path> visitor)
        throws IOException
    {
        return walkFileTree(start,
                            EnumSet.noneOf(FileVisitOption.class),
                            Integer.MAX_VALUE,
                            visitor);
    }


    // -- Utility methods for simple usages --


    /**
     * Opens a file for reading, returning a {@code BufferedReader} that may be
     * used to read text from the file in an efficient manner. Bytes from the
     * file are decoded into characters using the specified charset. Reading
     * commences at the beginning of the file.
     *
     * <p> The {@code Reader} methods that read from the file throw {@code
     * IOException} if a malformed or unmappable byte sequence is read.
     *
     * @param   path
     *          the path to the file
     * @param   cs
     *          the charset to use for decoding
     *
     * @return  a new buffered reader, with default buffer size, to read text
     *          from the file
     *
     * @throws  IOException
     *          if an I/O error occurs opening the file
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file.
     *
     * @see #readAllLines
     */
    public static BufferedReader newBufferedReader(Path path, Charset cs)
        throws IOException
    {
        CharsetDecoder decoder = cs.newDecoder();
        Reader reader = new InputStreamReader(newInputStream(path), decoder);
        return new BufferedReader(reader);
    }

    /**
     * Opens a file for reading, returning a {@code BufferedReader} to read text
     * from the file in an efficient manner. Bytes from the file are decoded into
     * characters using the {@link StandardCharsets#UTF_8 UTF-8} {@link Charset
     * charset}.
     *
     * <p> This method works as if invoking it were equivalent to evaluating the
     * expression:
     * <blockquote>{@link
     * newBufferedReader(Path, Charset)
     * Files.newBufferedReader(path, StandardCharsets.UTF_8)
     * }</blockquote>
     *
     * @param   path
     *          the path to the file
     *
     * @return  a new buffered reader, with default buffer size, to read text
     *          from the file
     *
     * @throws  IOException
     *          if an I/O error occurs opening the file
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file.
     *
     * @since 1.8
     */
    public static BufferedReader newBufferedReader(Path path) throws IOException {
        return newBufferedReader(path, UTF_8.INSTANCE);
    }

    /**
     * Opens or creates a file for writing, returning a {@code BufferedWriter}
     * that may be used to write text to the file in an efficient manner.
     * The {@code options} parameter specifies how the file is created or
     * opened. If no options are present then this method works as if the {@link
     * StandardOpenOption#CREATE CREATE}, {@link
     * StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING}, and {@link
     * StandardOpenOption#WRITE WRITE} options are present. In other words, it
     * opens the file for writing, creating the file if it doesn't exist, or
     * initially truncating an existing {@link #isRegularFile regular-file} to
     * a size of {@code 0} if it exists.
     *
     * <p> The {@code Writer} methods to write text throw {@code IOException}
     * if the text cannot be encoded using the specified charset.
     *
     * @param   path
     *          the path to the file
     * @param   cs
     *          the charset to use for encoding
     * @param   options
     *          options specifying how the file is opened
     *
     * @return  a new buffered writer, with default buffer size, to write text
     *          to the file
     *
     * @throws  IllegalArgumentException
     *          if {@code options} contains an invalid combination of options
     * @throws  IOException
     *          if an I/O error occurs opening or creating the file
     * @throws  UnsupportedOperationException
     *          if an unsupported option is specified
     * @throws  FileAlreadyExistsException
     *          If a file of that name already exists and the {@link
     *          StandardOpenOption#CREATE_NEW CREATE_NEW} option is specified
     *          <i>(optional specific exception)</i>
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to the file. The {@link
     *          SecurityManager#checkDelete(String) checkDelete} method is
     *          invoked to check delete access if the file is opened with the
     *          {@code DELETE_ON_CLOSE} option.
     *
     * @see #write(Path,Iterable,Charset,OpenOption[])
     */
    public static BufferedWriter newBufferedWriter(Path path, Charset cs,
                                                   OpenOption... options)
        throws IOException
    {
        CharsetEncoder encoder = cs.newEncoder();
        Writer writer = new OutputStreamWriter(newOutputStream(path, options), encoder);
        return new BufferedWriter(writer);
    }

    /**
     * Opens or creates a file for writing, returning a {@code BufferedWriter}
     * to write text to the file in an efficient manner. The text is encoded
     * into bytes for writing using the {@link StandardCharsets#UTF_8 UTF-8}
     * {@link Charset charset}.
     *
     * <p> This method works as if invoking it were equivalent to evaluating the
     * expression:
     * <blockquote>{@link
     * newBufferedWriter(Path, Charset, OpenOption...)
     * Files.newBufferedWriter(path, StandardCharsets.UTF_8, options)
     * }</blockquote>
     *
     * @param   path
     *          the path to the file
     * @param   options
     *          options specifying how the file is opened
     *
     * @return  a new buffered writer, with default buffer size, to write text
     *          to the file
     *
     * @throws  IllegalArgumentException
     *          if {@code options} contains an invalid combination of options
     * @throws  IOException
     *          if an I/O error occurs opening or creating the file
     * @throws  UnsupportedOperationException
     *          if an unsupported option is specified
     * @throws  FileAlreadyExistsException
     *          If a file of that name already exists and the {@link
     *          StandardOpenOption#CREATE_NEW CREATE_NEW} option is specified
     *          <i>(optional specific exception)</i>
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to the file. The {@link
     *          SecurityManager#checkDelete(String) checkDelete} method is
     *          invoked to check delete access if the file is opened with the
     *          {@code DELETE_ON_CLOSE} option.
     *
     * @since 1.8
     */
    public static BufferedWriter newBufferedWriter(Path path, OpenOption... options)
        throws IOException
    {
        return newBufferedWriter(path, UTF_8.INSTANCE, options);
    }

    /**
     * Copies all bytes from an input stream to a file. On return, the input
     * stream will be at end of stream.
     *
     * <p> By default, the copy fails if the target file already exists or is a
     * symbolic link. If the {@link StandardCopyOption#REPLACE_EXISTING
     * REPLACE_EXISTING} option is specified, and the target file already exists,
     * then it is replaced if it is not a non-empty directory. If the target
     * file exists and is a symbolic link, then the symbolic link is replaced.
     * In this release, the {@code REPLACE_EXISTING} option is the only option
     * required to be supported by this method. Additional options may be
     * supported in future releases.
     *
     * <p>  If an I/O error occurs reading from the input stream or writing to
     * the file, then it may do so after the target file has been created and
     * after some bytes have been read or written. Consequently the input
     * stream may not be at end of stream and may be in an inconsistent state.
     * It is strongly recommended that the input stream be promptly closed if an
     * I/O error occurs.
     *
     * <p> This method may block indefinitely reading from the input stream (or
     * writing to the file). The behavior for the case that the input stream is
     * <i>asynchronously closed</i> or the thread interrupted during the copy is
     * highly input stream and file system provider specific and therefore not
     * specified.
     *
     * <p> <b>Usage example</b>: Suppose we want to capture a web page and save
     * it to a file:
     * <pre>
     *     Path path = ...
     *     URI u = URI.create("http://www.example.com/");
     *     try (InputStream in = u.toURL().openStream()) {
     *         Files.copy(in, path);
     *     }
     * </pre>
     *
     * @param   in
     *          the input stream to read from
     * @param   target
     *          the path to the file
     * @param   options
     *          options specifying how the copy should be done
     *
     * @return  the number of bytes read or written
     *
     * @throws  IOException
     *          if an I/O error occurs when reading or writing
     * @throws  FileAlreadyExistsException
     *          if the target file exists but cannot be replaced because the
     *          {@code REPLACE_EXISTING} option is not specified <i>(optional
     *          specific exception)</i>
     * @throws  DirectoryNotEmptyException
     *          the {@code REPLACE_EXISTING} option is specified but the file
     *          cannot be replaced because it is a non-empty directory
     *          <i>(optional specific exception)</i>     *
     * @throws  UnsupportedOperationException
     *          if {@code options} contains a copy option that is not supported
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to the file. Where the
     *          {@code REPLACE_EXISTING} option is specified, the security
     *          manager's {@link SecurityManager#checkDelete(String) checkDelete}
     *          method is invoked to check that an existing file can be deleted.
     */
    public static long copy(InputStream in, Path target, CopyOption... options)
        throws IOException
    {
        // ensure not null before opening file
        Objects.requireNonNull(in);

        // check for REPLACE_EXISTING
        boolean replaceExisting = false;
        for (CopyOption opt: options) {
            if (opt == StandardCopyOption.REPLACE_EXISTING) {
                replaceExisting = true;
            } else {
                if (opt == null) {
                    throw new NullPointerException("options contains 'null'");
                }  else {
                    throw new UnsupportedOperationException(opt + " not supported");
                }
            }
        }

        // attempt to delete an existing file
        SecurityException se = null;
        if (replaceExisting) {
            try {
                deleteIfExists(target);
            } catch (SecurityException x) {
                se = x;
            }
        }

        // attempt to create target file. If it fails with
        // FileAlreadyExistsException then it may be because the security
        // manager prevented us from deleting the file, in which case we just
        // throw the SecurityException.
        OutputStream ostream;
        try {
            ostream = newOutputStream(target, StandardOpenOption.CREATE_NEW,
                                              StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException x) {
            if (se != null)
                throw se;
            // someone else won the race and created the file
            throw x;
        }

        // do the copy
        try (OutputStream out = ostream) {
            return in.transferTo(out);
        }
    }

    /**
     * Copies all bytes from a file to an output stream.
     *
     * <p> If an I/O error occurs reading from the file or writing to the output
     * stream, then it may do so after some bytes have been read or written.
     * Consequently the output stream may be in an inconsistent state. It is
     * strongly recommended that the output stream be promptly closed if an I/O
     * error occurs.
     *
     * <p> This method may block indefinitely writing to the output stream (or
     * reading from the file). The behavior for the case that the output stream
     * is <i>asynchronously closed</i> or the thread interrupted during the copy
     * is highly output stream and file system provider specific and therefore
     * not specified.
     *
     * <p> Note that if the given output stream is {@link java.io.Flushable}
     * then its {@link java.io.Flushable#flush flush} method may need to invoked
     * after this method completes so as to flush any buffered output.
     *
     * @param   source
     *          the  path to the file
     * @param   out
     *          the output stream to write to
     *
     * @return  the number of bytes read or written
     *
     * @throws  IOException
     *          if an I/O error occurs when reading or writing
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file.
     */
    public static long copy(Path source, OutputStream out) throws IOException {
        // ensure not null before opening file
        Objects.requireNonNull(out);

        try (InputStream in = newInputStream(source)) {
            return in.transferTo(out);
        }
    }

    private static final jdk.internal.access.JavaLangAccess JLA =
            jdk.internal.access.SharedSecrets.getJavaLangAccess();

    /**
     * Reads all the bytes from an input stream. Uses {@code initialSize} as a hint
     * about how many bytes the stream will have.
     *
     * @param   source
     *          the input stream to read from
     * @param   initialSize
     *          the initial size of the byte array to allocate
     *
     * @return  a byte array containing the bytes read from the file
     *
     * @throws  IOException
     *          if an I/O error occurs reading from the stream
     * @throws  OutOfMemoryError
     *          if an array of the required size cannot be allocated
     */
    private static byte[] read(InputStream source, int initialSize) throws IOException {
        int capacity = initialSize;
        byte[] buf = new byte[capacity];
        int nread = 0;
        int n;
        for (;;) {
            // read to EOF which may read more or less than initialSize (eg: file
            // is truncated while we are reading)
            while ((n = source.read(buf, nread, capacity - nread)) > 0)
                nread += n;

            // if last call to source.read() returned -1, we are done
            // otherwise, try to read one more byte; if that failed we're done too
            if (n < 0 || (n = source.read()) < 0)
                break;

            // one more byte was read; need to allocate a larger buffer
            capacity = Math.max(ArraysSupport.newLength(capacity,
                                                        1,       /* minimum growth */
                                                        capacity /* preferred growth */),
                                BUFFER_SIZE);
            buf = Arrays.copyOf(buf, capacity);
            buf[nread++] = (byte)n;
        }
        return (capacity == nread) ? buf : Arrays.copyOf(buf, nread);
    }

    /**
     * Reads all the bytes from a file. The method ensures that the file is
     * closed when all bytes have been read or an I/O error, or other runtime
     * exception, is thrown.
     *
     * <p> Note that this method is intended for simple cases where it is
     * convenient to read all bytes into a byte array. It is not intended for
     * reading in large files.
     *
     * @param   path
     *          the path to the file
     *
     * @return  a byte array containing the bytes read from the file
     *
     * @throws  IOException
     *          if an I/O error occurs reading from the stream
     * @throws  OutOfMemoryError
     *          if an array of the required size cannot be allocated, for
     *          example the file is larger that {@code 2GB}
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file.
     */
    public static byte[] readAllBytes(Path path) throws IOException {
        try (SeekableByteChannel sbc = Files.newByteChannel(path);
             InputStream in = Channels.newInputStream(sbc)) {
            if (sbc instanceof FileChannelImpl)
                ((FileChannelImpl) sbc).setUninterruptible();
            long size = sbc.size();
            if (size > (long) Integer.MAX_VALUE)
                throw new OutOfMemoryError("Required array size too large");
            return read(in, (int)size);
        }
    }

    /**
     * Reads all content from a file into a string, decoding from bytes to characters
     * using the {@link StandardCharsets#UTF_8 UTF-8} {@link Charset charset}.
     * The method ensures that the file is closed when all content have been read
     * or an I/O error, or other runtime exception, is thrown.
     *
     * <p> This method is equivalent to: {@link readString(Path, Charset)
     * readString(path, StandardCharsets.UTF_8)}.
     *
     * @param   path the path to the file
     *
     * @return  a String containing the content read from the file
     *
     * @throws  IOException
     *          if an I/O error occurs reading from the file or a malformed or
     *          unmappable byte sequence is read
     * @throws  OutOfMemoryError
     *          if the file is extremely large, for example larger than {@code 2GB}
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file.
     *
     * @since 11
     */
    public static String readString(Path path) throws IOException {
        return readString(path, UTF_8.INSTANCE);
    }

    /**
     * Reads all characters from a file into a string, decoding from bytes to characters
     * using the specified {@linkplain Charset charset}.
     * The method ensures that the file is closed when all content have been read
     * or an I/O error, or other runtime exception, is thrown.
     *
     * <p> This method reads all content including the line separators in the middle
     * and/or at the end. The resulting string will contain line separators as they
     * appear in the file.
     *
     * @apiNote
     * This method is intended for simple cases where it is appropriate and convenient
     * to read the content of a file into a String. It is not intended for reading
     * very large files.
     *
     *
     *
     * @param   path the path to the file
     * @param   cs the charset to use for decoding
     *
     * @return  a String containing the content read from the file
     *
     * @throws  IOException
     *          if an I/O error occurs reading from the file or a malformed or
     *          unmappable byte sequence is read
     * @throws  OutOfMemoryError
     *          if the file is extremely large, for example larger than {@code 2GB}
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file.
     *
     * @since 11
     */
    public static String readString(Path path, Charset cs) throws IOException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(cs);

        byte[] ba = readAllBytes(path);
        if (path.getClass().getModule() != Object.class.getModule())
            ba = ba.clone();
        return JLA.newStringNoRepl(ba, cs);
    }

    /**
     * Read all lines from a file. This method ensures that the file is
     * closed when all bytes have been read or an I/O error, or other runtime
     * exception, is thrown. Bytes from the file are decoded into characters
     * using the specified charset.
     *
     * <p> This method recognizes the following as line terminators:
     * <ul>
     *   <li> <code>&#92;u000D</code> followed by <code>&#92;u000A</code>,
     *     CARRIAGE RETURN followed by LINE FEED </li>
     *   <li> <code>&#92;u000A</code>, LINE FEED </li>
     *   <li> <code>&#92;u000D</code>, CARRIAGE RETURN </li>
     * </ul>
     * <p> Additional Unicode line terminators may be recognized in future
     * releases.
     *
     * <p> Note that this method is intended for simple cases where it is
     * convenient to read all lines in a single operation. It is not intended
     * for reading in large files.
     *
     * @param   path
     *          the path to the file
     * @param   cs
     *          the charset to use for decoding
     *
     * @return  the lines from the file as a {@code List}; whether the {@code
     *          List} is modifiable or not is implementation dependent and
     *          therefore not specified
     *
     * @throws  IOException
     *          if an I/O error occurs reading from the file or a malformed or
     *          unmappable byte sequence is read
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file.
     *
     * @see #newBufferedReader
     */
    public static List<String> readAllLines(Path path, Charset cs) throws IOException {
        try (BufferedReader reader = newBufferedReader(path, cs)) {
            List<String> result = new ArrayList<>();
            for (;;) {
                String line = reader.readLine();
                if (line == null)
                    break;
                result.add(line);
            }
            return result;
        }
    }

    /**
     * Read all lines from a file. Bytes from the file are decoded into characters
     * using the {@link StandardCharsets#UTF_8 UTF-8} {@link Charset charset}.
     *
     * <p> This method works as if invoking it were equivalent to evaluating the
     * expression:
     * <blockquote>{@link
     * readAllLines(Path, Charset)
     * Files.readAllLines(path, StandardCharsets.UTF_8)
     * }</blockquote>
     *
     * @param   path
     *          the path to the file
     *
     * @return  the lines from the file as a {@code List}; whether the {@code
     *          List} is modifiable or not is implementation dependent and
     *          therefore not specified
     *
     * @throws  IOException
     *          if an I/O error occurs reading from the file or a malformed or
     *          unmappable byte sequence is read
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file.
     *
     * @since 1.8
     */
    public static List<String> readAllLines(Path path) throws IOException {
        return readAllLines(path, UTF_8.INSTANCE);
    }

    /**
     * Writes bytes to a file. The {@code options} parameter specifies how
     * the file is created or opened. If no options are present then this method
     * works as if the {@link StandardOpenOption#CREATE CREATE}, {@link
     * StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING}, and {@link
     * StandardOpenOption#WRITE WRITE} options are present. In other words, it
     * opens the file for writing, creating the file if it doesn't exist, or
     * initially truncating an existing {@link #isRegularFile regular-file} to
     * a size of {@code 0}. All bytes in the byte array are written to the file.
     * The method ensures that the file is closed when all bytes have been
     * written (or an I/O error or other runtime exception is thrown). If an I/O
     * error occurs then it may do so after the file has been created or
     * truncated, or after some bytes have been written to the file.
     *
     * <p> <b>Usage example</b>: By default the method creates a new file or
     * overwrites an existing file. Suppose you instead want to append bytes
     * to an existing file:
     * <pre>
     *     Path path = ...
     *     byte[] bytes = ...
     *     Files.write(path, bytes, StandardOpenOption.APPEND);
     * </pre>
     *
     * @param   path
     *          the path to the file
     * @param   bytes
     *          the byte array with the bytes to write
     * @param   options
     *          options specifying how the file is opened
     *
     * @return  the path
     *
     * @throws  IllegalArgumentException
     *          if {@code options} contains an invalid combination of options
     * @throws  IOException
     *          if an I/O error occurs writing to or creating the file
     * @throws  UnsupportedOperationException
     *          if an unsupported option is specified
     * @throws  FileAlreadyExistsException
     *          If a file of that name already exists and the {@link
     *          StandardOpenOption#CREATE_NEW CREATE_NEW} option is specified
     *          <i>(optional specific exception)</i>
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to the file. The {@link
     *          SecurityManager#checkDelete(String) checkDelete} method is
     *          invoked to check delete access if the file is opened with the
     *          {@code DELETE_ON_CLOSE} option.
     */
    public static Path write(Path path, byte[] bytes, OpenOption... options)
        throws IOException
    {
        // ensure bytes is not null before opening file
        Objects.requireNonNull(bytes);

        try (OutputStream out = Files.newOutputStream(path, options)) {
            int len = bytes.length;
            int rem = len;
            while (rem > 0) {
                int n = Math.min(rem, BUFFER_SIZE);
                out.write(bytes, (len-rem), n);
                rem -= n;
            }
        }
        return path;
    }

    /**
     * Write lines of text to a file. Each line is a char sequence and is
     * written to the file in sequence with each line terminated by the
     * platform's line separator, as defined by the system property {@code
     * line.separator}. Characters are encoded into bytes using the specified
     * charset.
     *
     * <p> The {@code options} parameter specifies how the file is created
     * or opened. If no options are present then this method works as if the
     * {@link StandardOpenOption#CREATE CREATE}, {@link
     * StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING}, and {@link
     * StandardOpenOption#WRITE WRITE} options are present. In other words, it
     * opens the file for writing, creating the file if it doesn't exist, or
     * initially truncating an existing {@link #isRegularFile regular-file} to
     * a size of {@code 0}. The method ensures that the file is closed when all
     * lines have been written (or an I/O error or other runtime exception is
     * thrown). If an I/O error occurs then it may do so after the file has
     * been created or truncated, or after some bytes have been written to the
     * file.
     *
     * @param   path
     *          the path to the file
     * @param   lines
     *          an object to iterate over the char sequences
     * @param   cs
     *          the charset to use for encoding
     * @param   options
     *          options specifying how the file is opened
     *
     * @return  the path
     *
     * @throws  IllegalArgumentException
     *          if {@code options} contains an invalid combination of options
     * @throws  IOException
     *          if an I/O error occurs writing to or creating the file, or the
     *          text cannot be encoded using the specified charset
     * @throws  UnsupportedOperationException
     *          if an unsupported option is specified
     * @throws  FileAlreadyExistsException
     *          If a file of that name already exists and the {@link
     *          StandardOpenOption#CREATE_NEW CREATE_NEW} option is specified
     *          <i>(optional specific exception)</i>
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to the file. The {@link
     *          SecurityManager#checkDelete(String) checkDelete} method is
     *          invoked to check delete access if the file is opened with the
     *          {@code DELETE_ON_CLOSE} option.
     */
    public static Path write(Path path, Iterable<? extends CharSequence> lines,
                             Charset cs, OpenOption... options)
        throws IOException
    {
        // ensure lines is not null before opening file
        Objects.requireNonNull(lines);
        CharsetEncoder encoder = cs.newEncoder();
        try (OutputStream out = newOutputStream(path, options);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, encoder))) {
            for (CharSequence line: lines) {
                writer.append(line);
                writer.newLine();
            }
        }
        return path;
    }

    /**
     * Write lines of text to a file. Characters are encoded into bytes using
     * the {@link StandardCharsets#UTF_8 UTF-8} {@link Charset charset}.
     *
     * <p> This method works as if invoking it were equivalent to evaluating the
     * expression:
     * <blockquote>{@link
     * write(Path, Iterable<? extends CharSequence>, Charset, OpenOption...)
     * Files.write(path, lines, StandardCharsets.UTF_8, options)
     * }</blockquote>
     *
     * @param   path
     *          the path to the file
     * @param   lines
     *          an object to iterate over the char sequences
     * @param   options
     *          options specifying how the file is opened
     *
     * @return  the path
     *
     * @throws  IllegalArgumentException
     *          if {@code options} contains an invalid combination of options
     * @throws  IOException
     *          if an I/O error occurs writing to or creating the file, or the
     *          text cannot be encoded as {@code UTF-8}
     * @throws  UnsupportedOperationException
     *          if an unsupported option is specified
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to the file. The {@link
     *          SecurityManager#checkDelete(String) checkDelete} method is
     *          invoked to check delete access if the file is opened with the
     *          {@code DELETE_ON_CLOSE} option.
     *
     * @since 1.8
     */
    public static Path write(Path path,
                             Iterable<? extends CharSequence> lines,
                             OpenOption... options)
        throws IOException
    {
        return write(path, lines, UTF_8.INSTANCE, options);
    }

    /**
     * Write a {@linkplain java.lang.CharSequence CharSequence} to a file.
     * Characters are encoded into bytes using the
     * {@link StandardCharsets#UTF_8 UTF-8} {@link Charset charset}.
     *
     * <p> This method is equivalent to: {@link
     * writeString(Path, CharSequence, Charset, OpenOption...)
     * writeString(path, csq, StandardCharsets.UTF_8, options)}.
     *
     * @param   path
     *          the path to the file
     * @param   csq
     *          the CharSequence to be written
     * @param   options
     *          options specifying how the file is opened
     *
     * @return  the path
     *
     * @throws  IllegalArgumentException
     *          if {@code options} contains an invalid combination of options
     * @throws  IOException
     *          if an I/O error occurs writing to or creating the file, or the
     *          text cannot be encoded using the specified charset
     * @throws  UnsupportedOperationException
     *          if an unsupported option is specified
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to the file. The {@link
     *          SecurityManager#checkDelete(String) checkDelete} method is
     *          invoked to check delete access if the file is opened with the
     *          {@code DELETE_ON_CLOSE} option.
     *
     * @since 11
     */
    public static Path writeString(Path path, CharSequence csq, OpenOption... options)
            throws IOException
    {
        return writeString(path, csq, UTF_8.INSTANCE, options);
    }

    /**
     * Write a {@linkplain java.lang.CharSequence CharSequence} to a file.
     * Characters are encoded into bytes using the specified
     * {@linkplain java.nio.charset.Charset charset}.
     *
     * <p> All characters are written as they are, including the line separators in
     * the char sequence. No extra characters are added.
     *
     * <p> The {@code options} parameter specifies how the file is created
     * or opened. If no options are present then this method works as if the
     * {@link StandardOpenOption#CREATE CREATE}, {@link
     * StandardOpenOption#TRUNCATE_EXISTING TRUNCATE_EXISTING}, and {@link
     * StandardOpenOption#WRITE WRITE} options are present. In other words, it
     * opens the file for writing, creating the file if it doesn't exist, or
     * initially truncating an existing {@link #isRegularFile regular-file} to
     * a size of {@code 0}.
     *
     *
     * @param   path
     *          the path to the file
     * @param   csq
     *          the CharSequence to be written
     * @param   cs
     *          the charset to use for encoding
     * @param   options
     *          options specifying how the file is opened
     *
     * @return  the path
     *
     * @throws  IllegalArgumentException
     *          if {@code options} contains an invalid combination of options
     * @throws  IOException
     *          if an I/O error occurs writing to or creating the file, or the
     *          text cannot be encoded using the specified charset
     * @throws  UnsupportedOperationException
     *          if an unsupported option is specified
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkWrite(String) checkWrite}
     *          method is invoked to check write access to the file. The {@link
     *          SecurityManager#checkDelete(String) checkDelete} method is
     *          invoked to check delete access if the file is opened with the
     *          {@code DELETE_ON_CLOSE} option.
     *
     * @since 11
     */
    public static Path writeString(Path path, CharSequence csq, Charset cs, OpenOption... options)
            throws IOException
    {
        // ensure the text is not null before opening file
        Objects.requireNonNull(path);
        Objects.requireNonNull(csq);
        Objects.requireNonNull(cs);

        byte[] bytes = JLA.getBytesNoRepl(String.valueOf(csq), cs);
        if (path.getClass().getModule() != Object.class.getModule())
            bytes = bytes.clone();
        write(path, bytes, options);

        return path;
    }

    // -- Stream APIs --

    /**
     * Return a lazily populated {@code Stream}, the elements of
     * which are the entries in the directory.  The listing is not recursive.
     *
     * <p> The elements of the stream are {@link Path} objects that are
     * obtained as if by {@link Path#resolve(Path) resolving} the name of the
     * directory entry against {@code dir}. Some file systems maintain special
     * links to the directory itself and the directory's parent directory.
     * Entries representing these links are not included.
     *
     * <p> The stream is <i>weakly consistent</i>. It is thread safe but does
     * not freeze the directory while iterating, so it may (or may not)
     * reflect updates to the directory that occur after returning from this
     * method.
     *
     * <p> The returned stream contains a reference to an open directory.
     * The directory is closed by closing the stream.
     *
     * <p> Operating on a closed stream behaves as if the end of stream
     * has been reached. Due to read-ahead, one or more elements may be
     * returned after the stream has been closed.
     *
     * <p> If an {@link IOException} is thrown when accessing the directory
     * after this method has returned, it is wrapped in an {@link
     * UncheckedIOException} which will be thrown from the method that caused
     * the access to take place.
     *
     * @apiNote
     * This method must be used within a try-with-resources statement or similar
     * control structure to ensure that the stream's open directory is closed
     * promptly after the stream's operations have completed.
     *
     * @param   dir  The path to the directory
     *
     * @return  The {@code Stream} describing the content of the
     *          directory
     *
     * @throws  NotDirectoryException
     *          if the file could not otherwise be opened because it is not
     *          a directory <i>(optional specific exception)</i>
     * @throws  IOException
     *          if an I/O error occurs when opening the directory
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the directory.
     *
     * @see     #newDirectoryStream(Path)
     * @since   1.8
     */
    public static Stream<Path> list(Path dir) throws IOException {
        DirectoryStream<Path> ds = Files.newDirectoryStream(dir);
        try {
            final Iterator<Path> delegate = ds.iterator();

            // Re-wrap DirectoryIteratorException to UncheckedIOException
            Iterator<Path> iterator = new Iterator<>() {
                @Override
                public boolean hasNext() {
                    try {
                        return delegate.hasNext();
                    } catch (DirectoryIteratorException e) {
                        throw new UncheckedIOException(e.getCause());
                    }
                }
                @Override
                public Path next() {
                    try {
                        return delegate.next();
                    } catch (DirectoryIteratorException e) {
                        throw new UncheckedIOException(e.getCause());
                    }
                }
            };

            Spliterator<Path> spliterator =
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.DISTINCT);
            return StreamSupport.stream(spliterator, false)
                                .onClose(asUncheckedRunnable(ds));
        } catch (Error|RuntimeException e) {
            try {
                ds.close();
            } catch (IOException ex) {
                try {
                    e.addSuppressed(ex);
                } catch (Throwable ignore) {}
            }
            throw e;
        }
    }

    /**
     * Return a {@code Stream} that is lazily populated with {@code
     * Path} by walking the file tree rooted at a given starting file.  The
     * file tree is traversed <em>depth-first</em>, the elements in the stream
     * are {@link Path} objects that are obtained as if by {@link
     * Path#resolve(Path) resolving} the relative path against {@code start}.
     *
     * <p> The {@code stream} walks the file tree as elements are consumed.
     * The {@code Stream} returned is guaranteed to have at least one
     * element, the starting file itself. For each file visited, the stream
     * attempts to read its {@link BasicFileAttributes}. If the file is a
     * directory and can be opened successfully, entries in the directory, and
     * their <em>descendants</em> will follow the directory in the stream as
     * they are encountered. When all entries have been visited, then the
     * directory is closed. The file tree walk then continues at the next
     * <em>sibling</em> of the directory.
     *
     * <p> The stream is <i>weakly consistent</i>. It does not freeze the
     * file tree while iterating, so it may (or may not) reflect updates to
     * the file tree that occur after returned from this method.
     *
     * <p> By default, symbolic links are not automatically followed by this
     * method. If the {@code options} parameter contains the {@link
     * FileVisitOption#FOLLOW_LINKS FOLLOW_LINKS} option then symbolic links are
     * followed. When following links, and the attributes of the target cannot
     * be read, then this method attempts to get the {@code BasicFileAttributes}
     * of the link.
     *
     * <p> If the {@code options} parameter contains the {@link
     * FileVisitOption#FOLLOW_LINKS FOLLOW_LINKS} option then the stream keeps
     * track of directories visited so that cycles can be detected. A cycle
     * arises when there is an entry in a directory that is an ancestor of the
     * directory. Cycle detection is done by recording the {@link
     * java.nio.file.attribute.BasicFileAttributes#fileKey file-key} of directories,
     * or if file keys are not available, by invoking the {@link #isSameFile
     * isSameFile} method to test if a directory is the same file as an
     * ancestor. When a cycle is detected it is treated as an I/O error with
     * an instance of {@link FileSystemLoopException}.
     *
     * <p> The {@code maxDepth} parameter is the maximum number of levels of
     * directories to visit. A value of {@code 0} means that only the starting
     * file is visited, unless denied by the security manager. A value of
     * {@link Integer#MAX_VALUE MAX_VALUE} may be used to indicate that all
     * levels should be visited.
     *
     * <p> When a security manager is installed and it denies access to a file
     * (or directory), then it is ignored and not included in the stream.
     *
     * <p> The returned stream contains references to one or more open directories.
     * The directories are closed by closing the stream.
     *
     * <p> If an {@link IOException} is thrown when accessing the directory
     * after this method has returned, it is wrapped in an {@link
     * UncheckedIOException} which will be thrown from the method that caused
     * the access to take place.
     *
     * @apiNote
     * This method must be used within a try-with-resources statement or similar
     * control structure to ensure that the stream's open directories are closed
     * promptly after the stream's operations have completed.
     *
     * @param   start
     *          the starting file
     * @param   maxDepth
     *          the maximum number of directory levels to visit
     * @param   options
     *          options to configure the traversal
     *
     * @return  the {@link Stream} of {@link Path}
     *
     * @throws  IllegalArgumentException
     *          if the {@code maxDepth} parameter is negative
     * @throws  SecurityException
     *          If the security manager denies access to the starting file.
     *          In the case of the default provider, the {@link
     *          SecurityManager#checkRead(String) checkRead} method is invoked
     *          to check read access to the directory.
     * @throws  IOException
     *          if an I/O error is thrown when accessing the starting file.
     * @since   1.8
     */
    public static Stream<Path> walk(Path start,
                                    int maxDepth,
                                    FileVisitOption... options)
        throws IOException
    {
        FileTreeIterator iterator = new FileTreeIterator(start, maxDepth, options);
        try {
            Spliterator<FileTreeWalker.Event> spliterator =
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.DISTINCT);
            return StreamSupport.stream(spliterator, false)
                                .onClose(iterator::close)
                                .map(entry -> entry.file());
        } catch (Error|RuntimeException e) {
            iterator.close();
            throw e;
        }
    }

    /**
     * Return a {@code Stream} that is lazily populated with {@code
     * Path} by walking the file tree rooted at a given starting file.  The
     * file tree is traversed <em>depth-first</em>, the elements in the stream
     * are {@link Path} objects that are obtained as if by {@link
     * Path#resolve(Path) resolving} the relative path against {@code start}.
     *
     * <p> This method works as if invoking it were equivalent to evaluating the
     * expression:
     * <blockquote>{@link
     * walk(Path, int, FileVisitOption...)
     * Files.walk(start, Integer.MAX_VALUE, options)
     * }</blockquote>
     * In other words, it visits all levels of the file tree.
     *
     * <p> The returned stream contains references to one or more open directories.
     * The directories are closed by closing the stream.
     *
     * @apiNote
     * This method must be used within a try-with-resources statement or similar
     * control structure to ensure that the stream's open directories are closed
     * promptly after the stream's operations have completed.
     *
     * @param   start
     *          the starting file
     * @param   options
     *          options to configure the traversal
     *
     * @return  the {@link Stream} of {@link Path}
     *
     * @throws  SecurityException
     *          If the security manager denies access to the starting file.
     *          In the case of the default provider, the {@link
     *          SecurityManager#checkRead(String) checkRead} method is invoked
     *          to check read access to the directory.
     * @throws  IOException
     *          if an I/O error is thrown when accessing the starting file.
     *
     * @see     #walk(Path, int, FileVisitOption...)
     * @since   1.8
     */
    public static Stream<Path> walk(Path start, FileVisitOption... options) throws IOException {
        return walk(start, Integer.MAX_VALUE, options);
    }

    /**
     * Return a {@code Stream} that is lazily populated with {@code
     * Path} by searching for files in a file tree rooted at a given starting
     * file.
     *
     * <p> This method walks the file tree in exactly the manner specified by
     * the {@link #walk walk} method. For each file encountered, the given
     * {@link BiPredicate} is invoked with its {@link Path} and {@link
     * BasicFileAttributes}. The {@code Path} object is obtained as if by
     * {@link Path#resolve(Path) resolving} the relative path against {@code
     * start} and is only included in the returned {@link Stream} if
     * the {@code BiPredicate} returns true. Compare to calling {@link
     * java.util.stream.Stream#filter filter} on the {@code Stream}
     * returned by {@code walk} method, this method may be more efficient by
     * avoiding redundant retrieval of the {@code BasicFileAttributes}.
     *
     * <p> The returned stream contains references to one or more open directories.
     * The directories are closed by closing the stream.
     *
     * <p> If an {@link IOException} is thrown when accessing the directory
     * after returned from this method, it is wrapped in an {@link
     * UncheckedIOException} which will be thrown from the method that caused
     * the access to take place.
     *
     * @apiNote
     * This method must be used within a try-with-resources statement or similar
     * control structure to ensure that the stream's open directories are closed
     * promptly after the stream's operations have completed.
     *
     * @param   start
     *          the starting file
     * @param   maxDepth
     *          the maximum number of directory levels to search
     * @param   matcher
     *          the function used to decide whether a file should be included
     *          in the returned stream
     * @param   options
     *          options to configure the traversal
     *
     * @return  the {@link Stream} of {@link Path}
     *
     * @throws  IllegalArgumentException
     *          if the {@code maxDepth} parameter is negative
     * @throws  SecurityException
     *          If the security manager denies access to the starting file.
     *          In the case of the default provider, the {@link
     *          SecurityManager#checkRead(String) checkRead} method is invoked
     *          to check read access to the directory.
     * @throws  IOException
     *          if an I/O error is thrown when accessing the starting file.
     *
     * @see     #walk(Path, int, FileVisitOption...)
     * @since   1.8
     */
    public static Stream<Path> find(Path start,
                                    int maxDepth,
                                    BiPredicate<Path, BasicFileAttributes> matcher,
                                    FileVisitOption... options)
        throws IOException
    {
        FileTreeIterator iterator = new FileTreeIterator(start, maxDepth, options);
        try {
            Spliterator<FileTreeWalker.Event> spliterator =
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.DISTINCT);
            return StreamSupport.stream(spliterator, false)
                                .onClose(iterator::close)
                                .filter(entry -> matcher.test(entry.file(), entry.attributes()))
                                .map(entry -> entry.file());
        } catch (Error|RuntimeException e) {
            iterator.close();
            throw e;
        }
    }


    /**
     * Read all lines from a file as a {@code Stream}. Unlike {@link
     * #readAllLines(Path, Charset) readAllLines}, this method does not read
     * all lines into a {@code List}, but instead populates lazily as the stream
     * is consumed.
     *
     * <p> Bytes from the file are decoded into characters using the specified
     * charset and the same line terminators as specified by {@code
     * readAllLines} are supported.
     *
     * <p> The returned stream contains a reference to an open file. The file
     * is closed by closing the stream.
     *
     * <p> The file contents should not be modified during the execution of the
     * terminal stream operation. Otherwise, the result of the terminal stream
     * operation is undefined.
     *
     * <p> After this method returns, then any subsequent I/O exception that
     * occurs while reading from the file or when a malformed or unmappable byte
     * sequence is read, is wrapped in an {@link UncheckedIOException} that will
     * be thrown from the
     * {@link java.util.stream.Stream} method that caused the read to take
     * place. In case an {@code IOException} is thrown when closing the file,
     * it is also wrapped as an {@code UncheckedIOException}.
     *
     * @apiNote
     * This method must be used within a try-with-resources statement or similar
     * control structure to ensure that the stream's open file is closed promptly
     * after the stream's operations have completed.
     *
     * @implNote
     * This implementation supports good parallel stream performance for the
     * standard charsets {@link StandardCharsets#UTF_8 UTF-8},
     * {@link StandardCharsets#US_ASCII US-ASCII} and
     * {@link StandardCharsets#ISO_8859_1 ISO-8859-1}.  Such
     * <em>line-optimal</em> charsets have the property that the encoded bytes
     * of a line feed ('\n') or a carriage return ('\r') are efficiently
     * identifiable from other encoded characters when randomly accessing the
     * bytes of the file.
     *
     * <p> For non-<em>line-optimal</em> charsets the stream source's
     * spliterator has poor splitting properties, similar to that of a
     * spliterator associated with an iterator or that associated with a stream
     * returned from {@link BufferedReader#lines()}.  Poor splitting properties
     * can result in poor parallel stream performance.
     *
     * <p> For <em>line-optimal</em> charsets the stream source's spliterator
     * has good splitting properties, assuming the file contains a regular
     * sequence of lines.  Good splitting properties can result in good parallel
     * stream performance.  The spliterator for a <em>line-optimal</em> charset
     * takes advantage of the charset properties (a line feed or a carriage
     * return being efficient identifiable) such that when splitting it can
     * approximately divide the number of covered lines in half.
     *
     * @param   path
     *          the path to the file
     * @param   cs
     *          the charset to use for decoding
     *
     * @return  the lines from the file as a {@code Stream}
     *
     * @throws  IOException
     *          if an I/O error occurs opening the file
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file.
     *
     * @see     #readAllLines(Path, Charset)
     * @see     #newBufferedReader(Path, Charset)
     * @see     java.io.BufferedReader#lines()
     * @since   1.8
     */
    public static Stream<String> lines(Path path, Charset cs) throws IOException {
        // Use the good splitting spliterator if:
        // 1) the path is associated with the default file system;
        // 2) the character set is supported; and
        // 3) the file size is such that all bytes can be indexed by int values
        //    (this limitation is imposed by ByteBuffer)
        if (path.getFileSystem() == FileSystems.getDefault() &&
            FileChannelLinesSpliterator.SUPPORTED_CHARSET_NAMES.contains(cs.name())) {
            FileChannel fc = FileChannel.open(path, StandardOpenOption.READ);

            Stream<String> fcls = createFileChannelLinesStream(fc, cs);
            if (fcls != null) {
                return fcls;
            }
            fc.close();
        }

        return createBufferedReaderLinesStream(Files.newBufferedReader(path, cs));
    }

    private static Stream<String> createFileChannelLinesStream(FileChannel fc, Charset cs) throws IOException {
        try {
            // Obtaining the size from the FileChannel is much faster
            // than obtaining using path.toFile().length()
            long length = fc.size();
            // FileChannel.size() may in certain circumstances return zero
            // for a non-zero length file so disallow this case.
            if (length > 0 && length <= Integer.MAX_VALUE) {
                FileChannelLinesSpliterator fcls =
                    new FileChannelLinesSpliterator(fc, cs, 0, (int) length);
                return StreamSupport.stream(fcls, false)
                        .onClose(Files.asUncheckedRunnable(fc))
                        .onClose(() -> fcls.close());
            }
        } catch (Error|RuntimeException|IOException e) {
            try {
                fc.close();
            } catch (IOException ex) {
                try {
                    e.addSuppressed(ex);
                } catch (Throwable ignore) {
                }
            }
            throw e;
        }
        return null;
    }

    private static Stream<String> createBufferedReaderLinesStream(BufferedReader br) {
        try {
            return br.lines().onClose(asUncheckedRunnable(br));
        } catch (Error|RuntimeException e) {
            try {
                br.close();
            } catch (IOException ex) {
                try {
                    e.addSuppressed(ex);
                } catch (Throwable ignore) {
                }
            }
            throw e;
        }
    }

    /**
     * Read all lines from a file as a {@code Stream}. Bytes from the file are
     * decoded into characters using the {@link StandardCharsets#UTF_8 UTF-8}
     * {@link Charset charset}.
     *
     * <p> The returned stream contains a reference to an open file. The file
     * is closed by closing the stream.
     *
     * <p> The file contents should not be modified during the execution of the
     * terminal stream operation. Otherwise, the result of the terminal stream
     * operation is undefined.
     *
     * <p> This method works as if invoking it were equivalent to evaluating the
     * expression:
     * <blockquote>{@link
     * lines(Path, Charset)
     * Files.lines(path, StandardCharsets.UTF_8)
     * }</blockquote>
     *
     * @apiNote
     * This method must be used within a try-with-resources statement or similar
     * control structure to ensure that the stream's open file is closed promptly
     * after the stream's operations have completed.
     *
     * @param   path
     *          the path to the file
     *
     * @return  the lines from the file as a {@code Stream}
     *
     * @throws  IOException
     *          if an I/O error occurs opening the file
     * @throws  SecurityException
     *          In the case of the default provider, and a security manager is
     *          installed, the {@link SecurityManager#checkRead(String) checkRead}
     *          method is invoked to check read access to the file.
     *
     * @since 1.8
     */
    public static Stream<String> lines(Path path) throws IOException {
        return lines(path, UTF_8.INSTANCE);
    }
}
