/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.*;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * A channel for reading, writing, mapping, and manipulating a file.
 *
 * <p> A file channel is a {@link SeekableByteChannel} that is connected to
 * a file. It has a current <i>position</i> within its file which can
 * be both {@link #position() <i>queried</i>} and {@link #position(long)
 * <i>modified</i>}.  The file itself contains a variable-length sequence
 * of bytes that can be read and written and whose current {@link #size
 * <i>size</i>} can be queried.  The size of the file increases
 * when bytes are written beyond its current size; the size of the file
 * decreases when it is {@link #truncate <i>truncated</i>}.  The
 * file may also have some associated <i>metadata</i> such as access
 * permissions, content type, and last-modification time; this class does not
 * define methods for metadata access.
 *
 * <p> In addition to the familiar read, write, and close operations of byte
 * channels, this class defines the following file-specific operations: </p>
 *
 * <ul>
 *
 *   <li><p> Bytes may be {@link #read(ByteBuffer, long) read} or
 *   {@link #write(ByteBuffer, long) <i>written</i>} at an absolute
 *   position in a file in a way that does not affect the channel's current
 *   position.  </p></li>
 *
 *   <li><p> A region of a file may be {@link #map <i>mapped</i>}
 *   directly into memory; for large files this is often much more efficient
 *   than invoking the usual {@code read} or {@code write} methods.
 *   </p></li>
 *
 *   <li><p> Updates made to a file may be {@link #force <i>forced
 *   out</i>} to the underlying storage device, ensuring that data are not
 *   lost in the event of a system crash.  </p></li>
 *
 *   <li><p> Bytes can be transferred from a file {@link #transferTo <i>to
 *   some other channel</i>}, and {@link #transferFrom <i>vice
 *   versa</i>}, in a way that can be optimized by many operating systems
 *   into a very fast transfer directly to or from the filesystem cache.
 *   </p></li>
 *
 *   <li><p> A region of a file may be {@link FileLock <i>locked</i>}
 *   against access by other programs.  </p></li>
 *
 * </ul>
 *
 * <p> File channels are safe for use by multiple concurrent threads.  The
 * {@link Channel#close close} method may be invoked at any time, as specified
 * by the {@link Channel} interface.  Only one operation that involves the
 * channel's position or can change its file's size may be in progress at any
 * given time; attempts to initiate a second such operation while the first is
 * still in progress will block until the first operation completes.  Other
 * operations, in particular those that take an explicit position, may proceed
 * concurrently; whether they in fact do so is dependent upon the underlying
 * implementation and is therefore unspecified.
 *
 * <p> The view of a file provided by an instance of this class is guaranteed
 * to be consistent with other views of the same file provided by other
 * instances in the same program.  The view provided by an instance of this
 * class may or may not, however, be consistent with the views seen by other
 * concurrently-running programs due to caching performed by the underlying
 * operating system and delays induced by network-filesystem protocols.  This
 * is true regardless of the language in which these other programs are
 * written, and whether they are running on the same machine or on some other
 * machine.  The exact nature of any such inconsistencies are system-dependent
 * and are therefore unspecified.
 *
 * <p> A file channel is created by invoking one of the {@link #open open}
 * methods defined by this class. A file channel can also be obtained from an
 * existing {@link java.io.FileInputStream#getChannel FileInputStream}, {@link
 * java.io.FileOutputStream#getChannel FileOutputStream}, or {@link
 * java.io.RandomAccessFile#getChannel RandomAccessFile} object by invoking
 * that object's {@code getChannel} method, which returns a file channel that
 * is connected to the same underlying file. Where the file channel is obtained
 * from an existing stream or random access file then the state of the file
 * channel is intimately connected to that of the object whose {@code getChannel}
 * method returned the channel.  Changing the channel's position, whether
 * explicitly or by reading or writing bytes, will change the file position of
 * the originating object, and vice versa. Changing the file's length via the
 * file channel will change the length seen via the originating object, and vice
 * versa.  Changing the file's content by writing bytes will change the content
 * seen by the originating object, and vice versa.
 *
 * <a id="open-mode"></a> <p> At various points this class specifies that an
 * instance that is "open for reading," "open for writing," or "open for
 * reading and writing" is required.  A channel obtained via the {@link
 * java.io.FileInputStream#getChannel getChannel} method of a {@link
 * java.io.FileInputStream} instance will be open for reading.  A channel
 * obtained via the {@link java.io.FileOutputStream#getChannel getChannel}
 * method of a {@link java.io.FileOutputStream} instance will be open for
 * writing.  Finally, a channel obtained via the {@link
 * java.io.RandomAccessFile#getChannel getChannel} method of a {@link
 * java.io.RandomAccessFile} instance will be open for reading if the instance
 * was created with mode {@code "r"} and will be open for reading and writing
 * if the instance was created with mode {@code "rw"}.
 *
 * <a id="append-mode"></a><p> A file channel that is open for writing may be in
 * <i>append mode</i>, for example if it was obtained from a file-output stream
 * that was created by invoking the {@link
 * java.io.FileOutputStream#FileOutputStream(java.io.File,boolean)
 * FileOutputStream(File,boolean)} constructor and passing {@code true} for
 * the second parameter.  In this mode each invocation of a relative write
 * operation first advances the position to the end of the file and then writes
 * the requested data.  Whether the advancement of the position and the writing
 * of the data are done in a single atomic operation is system-dependent and
 * therefore unspecified.
 *
 * @see java.io.FileInputStream#getChannel()
 * @see java.io.FileOutputStream#getChannel()
 * @see java.io.RandomAccessFile#getChannel()
 *
 * @author Mark Reinhold
 * @author Mike McCloskey
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public abstract class FileChannel
    extends AbstractInterruptibleChannel
    implements SeekableByteChannel, GatheringByteChannel, ScatteringByteChannel
{
    /**
     * Initializes a new instance of this class.
     */
    protected FileChannel() { }

    /**
     * Opens or creates a file, returning a file channel to access the file.
     *
     * <p> The {@code options} parameter determines how the file is opened.
     * The {@link StandardOpenOption#READ READ} and {@link StandardOpenOption#WRITE
     * WRITE} options determine if the file should be opened for reading and/or
     * writing. If neither option (or the {@link StandardOpenOption#APPEND APPEND}
     * option) is contained in the array then the file is opened for reading.
     * By default reading or writing commences at the beginning of the file.
     *
     * <p> In the addition to {@code READ} and {@code WRITE}, the following
     * options may be present:
     *
     * <table class="striped">
     * <caption style="display:none">additional options</caption>
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
     *   the file already exists. When creating a file the check for the
     *   existence of the file and the creation of the file if it does not exist
     *   is atomic with respect to other file system operations. This option is
     *   ignored when the file is opened only for reading. </td>
     * </tr>
     * <tr>
     *   <th scope="row" > {@link StandardOpenOption#CREATE CREATE} </th>
     *   <td> If this option is present then an existing file is opened if it
     *   exists, otherwise a new file is created. When creating a file the check
     *   for the existence of the file and the creation of the file if it does
     *   not exist is atomic with respect to other file system operations. This
     *   option is ignored if the {@code CREATE_NEW} option is also present or
     *   the file is opened only for reading. </td>
     * </tr>
     * <tr>
     *   <th scope="row" > {@link StandardOpenOption#DELETE_ON_CLOSE DELETE_ON_CLOSE} </th>
     *   <td> When this option is present then the implementation makes a
     *   <em>best effort</em> attempt to delete the file when closed by
     *   the {@link #close close} method. If the {@code close} method is not
     *   invoked then a <em>best effort</em> attempt is made to delete the file
     *   when the Java virtual machine terminates. </td>
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
     *   href="../file/package-summary.html#integrity"> Synchronized I/O file
     *   integrity</a>). </td>
     * </tr>
     * <tr>
     *   <th scope="row"> {@link StandardOpenOption#DSYNC DSYNC} </th>
     *   <td> Requires that every update to the file's content be written
     *   synchronously to the underlying storage device. (see <a
     *   href="../file/package-summary.html#integrity"> Synchronized I/O file
     *   integrity</a>). </td>
     * </tr>
     * </tbody>
     * </table>
     *
     * <p> An implementation may also support additional options.
     *
     * <p> The {@code attrs} parameter is an optional array of file {@link
     * FileAttribute file-attributes} to set atomically when creating the file.
     *
     * <p> The new channel is created by invoking the {@link
     * FileSystemProvider#newFileChannel newFileChannel} method on the
     * provider that created the {@code Path}.
     *
     * @param   path
     *          The path of the file to open or create
     * @param   options
     *          Options specifying how the file is opened
     * @param   attrs
     *          An optional list of file attributes to set atomically when
     *          creating the file
     *
     * @return  A new file channel
     *
     * @throws  IllegalArgumentException
     *          If the set contains an invalid combination of options
     * @throws  UnsupportedOperationException
     *          If the {@code path} is associated with a provider that does not
     *          support creating file channels, or an unsupported open option is
     *          specified, or the array contains an attribute that cannot be set
     *          atomically when creating the file
     * @throws  FileAlreadyExistsException
     *          If a file of that name already exists and the {@link
     *          StandardOpenOption#CREATE_NEW CREATE_NEW} option is specified
     *          and the file is being opened for writing
     *          <i>(<a href="../file/package-summary.html#optspecex">optional
     *          specific exception</a>)</i>
     * @throws  IOException
     *          If an I/O error occurs
     * @throws  SecurityException
     *          If a security manager is installed and it denies an
     *          unspecified permission required by the implementation.
     *          In the case of the default provider, the {@link
     *          SecurityManager#checkRead(String)} method is invoked to check
     *          read access if the file is opened for reading. The {@link
     *          SecurityManager#checkWrite(String)} method is invoked to check
     *          write access if the file is opened for writing
     *
     * @since   1.7
     */
    public static FileChannel open(Path path,
                                   Set<? extends OpenOption> options,
                                   FileAttribute<?>... attrs)
        throws IOException
    {
        FileSystemProvider provider = path.getFileSystem().provider();
        return provider.newFileChannel(path, options, attrs);
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) // generic array construction
    private static final FileAttribute<?>[] NO_ATTRIBUTES = new FileAttribute[0];

    /**
     * Opens or creates a file, returning a file channel to access the file.
     *
     * <p> An invocation of this method behaves in exactly the same way as the
     * invocation
     * <pre>
     *     fc.{@link #open(Path,Set,FileAttribute[]) open}(file, opts, new FileAttribute&lt;?&gt;[0]);
     * </pre>
     * where {@code opts} is a set of the options specified in the {@code
     * options} array.
     *
     * @param   path
     *          The path of the file to open or create
     * @param   options
     *          Options specifying how the file is opened
     *
     * @return  A new file channel
     *
     * @throws  IllegalArgumentException
     *          If the set contains an invalid combination of options
     * @throws  UnsupportedOperationException
     *          If the {@code path} is associated with a provider that does not
     *          support creating file channels, or an unsupported open option is
     *          specified
     * @throws  FileAlreadyExistsException
     *          If a file of that name already exists and the {@link
     *          StandardOpenOption#CREATE_NEW CREATE_NEW} option is specified
     *          and the file is being opened for writing
     *          <i>(<a href="../file/package-summary.html#optspecex">optional
     *          specific exception</a>)</i>
     * @throws  IOException
     *          If an I/O error occurs
     * @throws  SecurityException
     *          If a security manager is installed and it denies an
     *          unspecified permission required by the implementation.
     *          In the case of the default provider, the {@link
     *          SecurityManager#checkRead(String)} method is invoked to check
     *          read access if the file is opened for reading. The {@link
     *          SecurityManager#checkWrite(String)} method is invoked to check
     *          write access if the file is opened for writing
     *
     * @since   1.7
     */
    public static FileChannel open(Path path, OpenOption... options)
        throws IOException
    {
        Set<OpenOption> set;
        if (options.length == 0) {
            set = Collections.emptySet();
        } else {
            set = new HashSet<>();
            Collections.addAll(set, options);
        }
        return open(path, set, NO_ATTRIBUTES);
    }

    // -- Channel operations --

    /**
     * Reads a sequence of bytes from this channel into the given buffer.
     *
     * <p> Bytes are read starting at this channel's current file position, and
     * then the file position is updated with the number of bytes actually
     * read.  Otherwise this method behaves exactly as specified in the {@link
     * ReadableByteChannel} interface. </p>
     */
    public abstract int read(ByteBuffer dst) throws IOException;

    /**
     * Reads a sequence of bytes from this channel into a subsequence of the
     * given buffers.
     *
     * <p> Bytes are read starting at this channel's current file position, and
     * then the file position is updated with the number of bytes actually
     * read.  Otherwise this method behaves exactly as specified in the {@link
     * ScatteringByteChannel} interface.  </p>
     */
    public abstract long read(ByteBuffer[] dsts, int offset, int length)
        throws IOException;

    /**
     * Reads a sequence of bytes from this channel into the given buffers.
     *
     * <p> Bytes are read starting at this channel's current file position, and
     * then the file position is updated with the number of bytes actually
     * read.  Otherwise this method behaves exactly as specified in the {@link
     * ScatteringByteChannel} interface.  </p>
     */
    public final long read(ByteBuffer[] dsts) throws IOException {
        return read(dsts, 0, dsts.length);
    }

    /**
     * Writes a sequence of bytes to this channel from the given buffer.
     *
     * <p> Bytes are written starting at this channel's current file position
     * unless the channel is in append mode, in which case the position is
     * first advanced to the end of the file.  The file is grown, if necessary,
     * to accommodate the written bytes, and then the file position is updated
     * with the number of bytes actually written.  Otherwise this method
     * behaves exactly as specified by the {@link WritableByteChannel}
     * interface. </p>
     */
    public abstract int write(ByteBuffer src) throws IOException;

    /**
     * Writes a sequence of bytes to this channel from a subsequence of the
     * given buffers.
     *
     * <p> Bytes are written starting at this channel's current file position
     * unless the channel is in append mode, in which case the position is
     * first advanced to the end of the file.  The file is grown, if necessary,
     * to accommodate the written bytes, and then the file position is updated
     * with the number of bytes actually written.  Otherwise this method
     * behaves exactly as specified in the {@link GatheringByteChannel}
     * interface.  </p>
     */
    public abstract long write(ByteBuffer[] srcs, int offset, int length)
        throws IOException;

    /**
     * Writes a sequence of bytes to this channel from the given buffers.
     *
     * <p> Bytes are written starting at this channel's current file position
     * unless the channel is in append mode, in which case the position is
     * first advanced to the end of the file.  The file is grown, if necessary,
     * to accommodate the written bytes, and then the file position is updated
     * with the number of bytes actually written.  Otherwise this method
     * behaves exactly as specified in the {@link GatheringByteChannel}
     * interface.  </p>
     */
    public final long write(ByteBuffer[] srcs) throws IOException {
        return write(srcs, 0, srcs.length);
    }


    // -- Other operations --

    /**
     * Returns this channel's file position.
     *
     * @return  This channel's file position,
     *          a non-negative integer counting the number of bytes
     *          from the beginning of the file to the current position
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract long position() throws IOException;

    /**
     * Sets this channel's file position.
     *
     * <p> Setting the position to a value that is greater than the file's
     * current size is legal but does not change the size of the file.  A later
     * attempt to read bytes at such a position will immediately return an
     * end-of-file indication.  A later attempt to write bytes at such a
     * position will cause the file to be grown to accommodate the new bytes;
     * the values of any bytes between the previous end-of-file and the
     * newly-written bytes are unspecified.  </p>
     *
     * @param  newPosition
     *         The new position, a non-negative integer counting
     *         the number of bytes from the beginning of the file
     *
     * @return  This file channel
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  IllegalArgumentException
     *          If the new position is negative
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract FileChannel position(long newPosition) throws IOException;

    /**
     * Returns the current size of this channel's file.
     *
     * @return  The current size of this channel's file,
     *          measured in bytes
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract long size() throws IOException;

    /**
     * Truncates this channel's file to the given size.
     *
     * <p> If the given size is less than the file's current size then the file
     * is truncated, discarding any bytes beyond the new end of the file.  If
     * the given size is greater than or equal to the file's current size then
     * the file is not modified.  In either case, if this channel's file
     * position is greater than the given size then it is set to that size.
     * </p>
     *
     * @param  size
     *         The new size, a non-negative byte count
     *
     * @return  This file channel
     *
     * @throws  NonWritableChannelException
     *          If this channel was not opened for writing
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  IllegalArgumentException
     *          If the new size is negative
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract FileChannel truncate(long size) throws IOException;

    /**
     * Forces any updates to this channel's file to be written to the storage
     * device that contains it.
     *
     * <p> If this channel's file resides on a local storage device then when
     * this method returns it is guaranteed that all changes made to the file
     * since this channel was created, or since this method was last invoked,
     * will have been written to that device.  This is useful for ensuring that
     * critical information is not lost in the event of a system crash.
     *
     * <p> If the file does not reside on a local device then no such guarantee
     * is made.
     *
     * <p> The {@code metaData} parameter can be used to limit the number of
     * I/O operations that this method is required to perform.  Passing
     * {@code false} for this parameter indicates that only updates to the
     * file's content need be written to storage; passing {@code true}
     * indicates that updates to both the file's content and metadata must be
     * written, which generally requires at least one more I/O operation.
     * Whether this parameter actually has any effect is dependent upon the
     * underlying operating system and is therefore unspecified.
     *
     * <p> Invoking this method may cause an I/O operation to occur even if the
     * channel was only opened for reading.  Some operating systems, for
     * example, maintain a last-access time as part of a file's metadata, and
     * this time is updated whenever the file is read.  Whether or not this is
     * actually done is system-dependent and is therefore unspecified.
     *
     * <p> This method is only guaranteed to force changes that were made to
     * this channel's file via the methods defined in this class.  It may or
     * may not force changes that were made by modifying the content of a
     * {@link MappedByteBuffer <i>mapped byte buffer</i>} obtained by
     * invoking the {@link #map map} method.  Invoking the {@link
     * MappedByteBuffer#force force} method of the mapped byte buffer will
     * force changes made to the buffer's content to be written.  </p>
     *
     * @param   metaData
     *          If {@code true} then this method is required to force changes
     *          to both the file's content and metadata to be written to
     *          storage; otherwise, it need only force content changes to be
     *          written
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract void force(boolean metaData) throws IOException;

    /**
     * Transfers bytes from this channel's file to the given writable byte
     * channel.
     *
     * <p> An attempt is made to read up to {@code count} bytes starting at
     * the given {@code position} in this channel's file and write them to the
     * target channel.  An invocation of this method may or may not transfer
     * all of the requested bytes; whether or not it does so depends upon the
     * natures and states of the channels.  Fewer than the requested number of
     * bytes are transferred if this channel's file contains fewer than
     * {@code count} bytes starting at the given {@code position}, or if the
     * target channel is non-blocking and it has fewer than {@code count}
     * bytes free in its output buffer.
     *
     * <p> This method does not modify this channel's position.  If the given
     * position is greater than the file's current size then no bytes are
     * transferred.  If the target channel has a position then bytes are
     * written starting at that position and then the position is incremented
     * by the number of bytes written.
     *
     * <p> This method is potentially much more efficient than a simple loop
     * that reads from this channel and writes to the target channel.  Many
     * operating systems can transfer bytes directly from the filesystem cache
     * to the target channel without actually copying them.  </p>
     *
     * @param  position
     *         The position within the file at which the transfer is to begin;
     *         must be non-negative
     *
     * @param  count
     *         The maximum number of bytes to be transferred; must be
     *         non-negative
     *
     * @param  target
     *         The target channel
     *
     * @return  The number of bytes, possibly zero,
     *          that were actually transferred
     *
     * @throws IllegalArgumentException
     *         If the preconditions on the parameters do not hold
     *
     * @throws  NonReadableChannelException
     *          If this channel was not opened for reading
     *
     * @throws  NonWritableChannelException
     *          If the target channel was not opened for writing
     *
     * @throws  ClosedChannelException
     *          If either this channel or the target channel is closed
     *
     * @throws  AsynchronousCloseException
     *          If another thread closes either channel
     *          while the transfer is in progress
     *
     * @throws  ClosedByInterruptException
     *          If another thread interrupts the current thread while the
     *          transfer is in progress, thereby closing both channels and
     *          setting the current thread's interrupt status
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract long transferTo(long position, long count,
                                    WritableByteChannel target)
        throws IOException;

    /**
     * Transfers bytes into this channel's file from the given readable byte
     * channel.
     *
     * <p> An attempt is made to read up to {@code count} bytes from the
     * source channel and write them to this channel's file starting at the
     * given {@code position}.  An invocation of this method may or may not
     * transfer all of the requested bytes; whether or not it does so depends
     * upon the natures and states of the channels.  Fewer than the requested
     * number of bytes will be transferred if the source channel has fewer than
     * {@code count} bytes remaining, or if the source channel is non-blocking
     * and has fewer than {@code count} bytes immediately available in its
     * input buffer. No bytes are transferred, and zero is returned, if the
     * source has reached end-of-stream.
     *
     * <p> This method does not modify this channel's position.  If the given
     * position is greater than the file's current size then no bytes are
     * transferred.  If the source channel has a position then bytes are read
     * starting at that position and then the position is incremented by the
     * number of bytes read.
     *
     * <p> This method is potentially much more efficient than a simple loop
     * that reads from the source channel and writes to this channel.  Many
     * operating systems can transfer bytes directly from the source channel
     * into the filesystem cache without actually copying them.  </p>
     *
     * @param  src
     *         The source channel
     *
     * @param  position
     *         The position within the file at which the transfer is to begin;
     *         must be non-negative
     *
     * @param  count
     *         The maximum number of bytes to be transferred; must be
     *         non-negative
     *
     * @return  The number of bytes, possibly zero,
     *          that were actually transferred
     *
     * @throws IllegalArgumentException
     *         If the preconditions on the parameters do not hold
     *
     * @throws  NonReadableChannelException
     *          If the source channel was not opened for reading
     *
     * @throws  NonWritableChannelException
     *          If this channel was not opened for writing
     *
     * @throws  ClosedChannelException
     *          If either this channel or the source channel is closed
     *
     * @throws  AsynchronousCloseException
     *          If another thread closes either channel
     *          while the transfer is in progress
     *
     * @throws  ClosedByInterruptException
     *          If another thread interrupts the current thread while the
     *          transfer is in progress, thereby closing both channels and
     *          setting the current thread's interrupt status
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract long transferFrom(ReadableByteChannel src,
                                      long position, long count)
        throws IOException;

    /**
     * Reads a sequence of bytes from this channel into the given buffer,
     * starting at the given file position.
     *
     * <p> This method works in the same manner as the {@link
     * #read(ByteBuffer)} method, except that bytes are read starting at the
     * given file position rather than at the channel's current position.  This
     * method does not modify this channel's position.  If the given position
     * is greater than the file's current size then no bytes are read.  </p>
     *
     * @param  dst
     *         The buffer into which bytes are to be transferred
     *
     * @param  position
     *         The file position at which the transfer is to begin;
     *         must be non-negative
     *
     * @return  The number of bytes read, possibly zero, or {@code -1} if the
     *          given position is greater than or equal to the file's current
     *          size
     *
     * @throws  IllegalArgumentException
     *          If the position is negative or the buffer is read-only
     *
     * @throws  NonReadableChannelException
     *          If this channel was not opened for reading
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  AsynchronousCloseException
     *          If another thread closes this channel
     *          while the read operation is in progress
     *
     * @throws  ClosedByInterruptException
     *          If another thread interrupts the current thread
     *          while the read operation is in progress, thereby
     *          closing the channel and setting the current thread's
     *          interrupt status
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract int read(ByteBuffer dst, long position) throws IOException;

    /**
     * Writes a sequence of bytes to this channel from the given buffer,
     * starting at the given file position.
     *
     * <p> This method works in the same manner as the {@link
     * #write(ByteBuffer)} method, except that bytes are written starting at
     * the given file position rather than at the channel's current position.
     * This method does not modify this channel's position.  If the given
     * position is greater than the file's current size then the file will be
     * grown to accommodate the new bytes; the values of any bytes between the
     * previous end-of-file and the newly-written bytes are unspecified.  </p>
     *
     * @param  src
     *         The buffer from which bytes are to be transferred
     *
     * @param  position
     *         The file position at which the transfer is to begin;
     *         must be non-negative
     *
     * @return  The number of bytes written, possibly zero
     *
     * @throws  IllegalArgumentException
     *          If the position is negative
     *
     * @throws  NonWritableChannelException
     *          If this channel was not opened for writing
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  AsynchronousCloseException
     *          If another thread closes this channel
     *          while the write operation is in progress
     *
     * @throws  ClosedByInterruptException
     *          If another thread interrupts the current thread
     *          while the write operation is in progress, thereby
     *          closing the channel and setting the current thread's
     *          interrupt status
     *
     * @throws  IOException
     *          If some other I/O error occurs
     */
    public abstract int write(ByteBuffer src, long position) throws IOException;


    // -- Memory-mapped buffers --

    /**
     * A file-mapping mode.
     *
     * @since 1.4
     *
     * @see java.nio.channels.FileChannel#map
     */
    public static class MapMode {

        /**
         * Mode for a read-only mapping.
         */
        public static final MapMode READ_ONLY
            = new MapMode("READ_ONLY");

        /**
         * Mode for a read/write mapping.
         */
        public static final MapMode READ_WRITE
            = new MapMode("READ_WRITE");

        /**
         * Mode for a private (copy-on-write) mapping.
         */
        public static final MapMode PRIVATE
            = new MapMode("PRIVATE");

        private final String name;

        /**
         * Constructs an instance of this class. This constructor may be used
         * by code in java.base to create file mapping modes beyond the file
         * mapping modes defined here.
         * @param name the name of the map mode
         */
        private MapMode(String name) {
            this.name = name;
        }

        /**
         * Returns a string describing this file-mapping mode.
         *
         * @return  A descriptive string
         */
        public String toString() {
            return name;
        }

    }

    /**
     * Maps a region of this channel's file directly into memory.
     *
     * <p> The {@code mode} parameter specifies how the region of the file is
     * mapped and may be one of the following modes:
     *
     * <ul>
     *
     *   <li><p> <i>Read-only:</i> Any attempt to modify the resulting buffer
     *   will cause a {@link java.nio.ReadOnlyBufferException} to be thrown.
     *   ({@link MapMode#READ_ONLY MapMode.READ_ONLY}) </p></li>
     *
     *   <li><p> <i>Read/write:</i> Changes made to the resulting buffer will
     *   eventually be propagated to the file; they may or may not be made
     *   visible to other programs that have mapped the same file.  ({@link
     *   MapMode#READ_WRITE MapMode.READ_WRITE}) </p></li>
     *
     *   <li><p> <i>Private:</i> Changes made to the resulting buffer will not
     *   be propagated to the file and will not be visible to other programs
     *   that have mapped the same file; instead, they will cause private
     *   copies of the modified portions of the buffer to be created.  ({@link
     *   MapMode#PRIVATE MapMode.PRIVATE}) </p></li>
     *
     * </ul>
     *
     * <p> An implementation may support additional map modes.
     *
     * <p> For a read-only mapping, this channel must have been opened for
     * reading; for a read/write or private mapping, this channel must have
     * been opened for both reading and writing.
     *
     * <p> The {@link MappedByteBuffer <i>mapped byte buffer</i>}
     * returned by this method will have a position of zero and a limit and
     * capacity of {@code size}; its mark will be undefined.  The buffer and
     * the mapping that it represents will remain valid until the buffer itself
     * is garbage-collected.
     *
     * <p> A mapping, once established, is not dependent upon the file channel
     * that was used to create it.  Closing the channel, in particular, has no
     * effect upon the validity of the mapping.
     *
     * <p> Many of the details of memory-mapped files are inherently dependent
     * upon the underlying operating system and are therefore unspecified.  The
     * behavior of this method when the requested region is not completely
     * contained within this channel's file is unspecified.  Whether changes
     * made to the content or size of the underlying file, by this program or
     * another, are propagated to the buffer is unspecified.  The rate at which
     * changes to the buffer are propagated to the file is unspecified.
     *
     * <p> For most operating systems, mapping a file into memory is more
     * expensive than reading or writing a few tens of kilobytes of data via
     * the usual {@link #read read} and {@link #write write} methods.  From the
     * standpoint of performance it is generally only worth mapping relatively
     * large files into memory.  </p>
     *
     * @param  mode
     *         One of the constants {@link MapMode#READ_ONLY READ_ONLY}, {@link
     *         MapMode#READ_WRITE READ_WRITE}, or {@link MapMode#PRIVATE
     *         PRIVATE} defined in the {@link MapMode} class, according to
     *         whether the file is to be mapped read-only, read/write, or
     *         privately (copy-on-write), respectively, or an implementation
     *         specific map mode
     *
     * @param  position
     *         The position within the file at which the mapped region
     *         is to start; must be non-negative
     *
     * @param  size
     *         The size of the region to be mapped; must be non-negative and
     *         no greater than {@link java.lang.Integer#MAX_VALUE}
     *
     * @return  The mapped byte buffer
     *
     * @throws NonReadableChannelException
     *         If the {@code mode} is {@link MapMode#READ_ONLY READ_ONLY} or
     *         an implementation specific map mode requiring read access
     *         but this channel was not opened for reading
     *
     * @throws NonWritableChannelException
     *         If the {@code mode} is {@link MapMode#READ_WRITE READ_WRITE}.
     *         {@link MapMode#PRIVATE PRIVATE} or an implementation specific
     *         map mode requiring write access but this channel was not
     *         opened for both reading and writing
     *
     * @throws IllegalArgumentException
     *         If the preconditions on the parameters do not hold
     *
     * @throws UnsupportedOperationException
     *         If an unsupported map mode is specified
     *
     * @throws IOException
     *         If some other I/O error occurs
     *
     * @see java.nio.channels.FileChannel.MapMode
     * @see java.nio.MappedByteBuffer
     */
    public abstract MappedByteBuffer map(MapMode mode, long position, long size)
        throws IOException;


    // -- Locks --

    /**
     * Acquires a lock on the given region of this channel's file.
     *
     * <p> An invocation of this method will block until the region can be
     * locked, this channel is closed, or the invoking thread is interrupted,
     * whichever comes first.
     *
     * <p> If this channel is closed by another thread during an invocation of
     * this method then an {@link AsynchronousCloseException} will be thrown.
     *
     * <p> If the invoking thread is interrupted while waiting to acquire the
     * lock then its interrupt status will be set and a {@link
     * FileLockInterruptionException} will be thrown.  If the invoker's
     * interrupt status is set when this method is invoked then that exception
     * will be thrown immediately; the thread's interrupt status will not be
     * changed.
     *
     * <p> The region specified by the {@code position} and {@code size}
     * parameters need not be contained within, or even overlap, the actual
     * underlying file.  Lock regions are fixed in size; if a locked region
     * initially contains the end of the file and the file grows beyond the
     * region then the new portion of the file will not be covered by the lock.
     * If a file is expected to grow in size and a lock on the entire file is
     * required then a region starting at zero, and no smaller than the
     * expected maximum size of the file, should be locked.  The zero-argument
     * {@link #lock()} method simply locks a region of size {@link
     * Long#MAX_VALUE}.
     *
     * <p> Some operating systems do not support shared locks, in which case a
     * request for a shared lock is automatically converted into a request for
     * an exclusive lock.  Whether the newly-acquired lock is shared or
     * exclusive may be tested by invoking the resulting lock object's {@link
     * FileLock#isShared() isShared} method.
     *
     * <p> File locks are held on behalf of the entire Java virtual machine.
     * They are not suitable for controlling access to a file by multiple
     * threads within the same virtual machine.  </p>
     *
     * @param  position
     *         The position at which the locked region is to start; must be
     *         non-negative
     *
     * @param  size
     *         The size of the locked region; must be non-negative, and the sum
     *         {@code position}&nbsp;+&nbsp;{@code size} must be non-negative
     *
     * @param  shared
     *         {@code true} to request a shared lock, in which case this
     *         channel must be open for reading (and possibly writing);
     *         {@code false} to request an exclusive lock, in which case this
     *         channel must be open for writing (and possibly reading)
     *
     * @return  A lock object representing the newly-acquired lock
     *
     * @throws  IllegalArgumentException
     *          If the preconditions on the parameters do not hold
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  AsynchronousCloseException
     *          If another thread closes this channel while the invoking
     *          thread is blocked in this method
     *
     * @throws  FileLockInterruptionException
     *          If the invoking thread is interrupted while blocked in this
     *          method
     *
     * @throws  OverlappingFileLockException
     *          If a lock that overlaps the requested region is already held by
     *          this Java virtual machine, or if another thread is already
     *          blocked in this method and is attempting to lock an overlapping
     *          region
     *
     * @throws  NonReadableChannelException
     *          If {@code shared} is {@code true} this channel was not
     *          opened for reading
     *
     * @throws  NonWritableChannelException
     *          If {@code shared} is {@code false} but this channel was not
     *          opened for writing
     *
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @see     #lock()
     * @see     #tryLock()
     * @see     #tryLock(long,long,boolean)
     */
    public abstract FileLock lock(long position, long size, boolean shared)
        throws IOException;

    /**
     * Acquires an exclusive lock on this channel's file.
     *
     * <p> An invocation of this method of the form {@code fc.lock()} behaves
     * in exactly the same way as the invocation
     *
     * <pre>
     *     fc.{@link #lock(long,long,boolean) lock}(0L, Long.MAX_VALUE, false) </pre>
     *
     * @return  A lock object representing the newly-acquired lock
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  AsynchronousCloseException
     *          If another thread closes this channel while the invoking
     *          thread is blocked in this method
     *
     * @throws  FileLockInterruptionException
     *          If the invoking thread is interrupted while blocked in this
     *          method
     *
     * @throws  OverlappingFileLockException
     *          If a lock that overlaps the requested region is already held by
     *          this Java virtual machine, or if another thread is already
     *          blocked in this method and is attempting to lock an overlapping
     *          region of the same file
     *
     * @throws  NonWritableChannelException
     *          If this channel was not opened for writing
     *
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @see     #lock(long,long,boolean)
     * @see     #tryLock()
     * @see     #tryLock(long,long,boolean)
     */
    public final FileLock lock() throws IOException {
        return lock(0L, Long.MAX_VALUE, false);
    }

    /**
     * Attempts to acquire a lock on the given region of this channel's file.
     *
     * <p> This method does not block.  An invocation always returns
     * immediately, either having acquired a lock on the requested region or
     * having failed to do so.  If it fails to acquire a lock because an
     * overlapping lock is held by another program then it returns
     * {@code null}.  If it fails to acquire a lock for any other reason then
     * an appropriate exception is thrown.
     *
     * <p> The region specified by the {@code position} and {@code size}
     * parameters need not be contained within, or even overlap, the actual
     * underlying file.  Lock regions are fixed in size; if a locked region
     * initially contains the end of the file and the file grows beyond the
     * region then the new portion of the file will not be covered by the lock.
     * If a file is expected to grow in size and a lock on the entire file is
     * required then a region starting at zero, and no smaller than the
     * expected maximum size of the file, should be locked.  The zero-argument
     * {@link #tryLock()} method simply locks a region of size {@link
     * Long#MAX_VALUE}.
     *
     * <p> Some operating systems do not support shared locks, in which case a
     * request for a shared lock is automatically converted into a request for
     * an exclusive lock.  Whether the newly-acquired lock is shared or
     * exclusive may be tested by invoking the resulting lock object's {@link
     * FileLock#isShared() isShared} method.
     *
     * <p> File locks are held on behalf of the entire Java virtual machine.
     * They are not suitable for controlling access to a file by multiple
     * threads within the same virtual machine.  </p>
     *
     * @param  position
     *         The position at which the locked region is to start; must be
     *         non-negative
     *
     * @param  size
     *         The size of the locked region; must be non-negative, and the sum
     *         {@code position}&nbsp;+&nbsp;{@code size} must be non-negative
     *
     * @param  shared
     *         {@code true} to request a shared lock,
     *         {@code false} to request an exclusive lock
     *
     * @return  A lock object representing the newly-acquired lock,
     *          or {@code null} if the lock could not be acquired
     *          because another program holds an overlapping lock
     *
     * @throws  IllegalArgumentException
     *          If the preconditions on the parameters do not hold
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  OverlappingFileLockException
     *          If a lock that overlaps the requested region is already held by
     *          this Java virtual machine, or if another thread is already
     *          blocked in this method and is attempting to lock an overlapping
     *          region of the same file
     *
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @see     #lock()
     * @see     #lock(long,long,boolean)
     * @see     #tryLock()
     */
    public abstract FileLock tryLock(long position, long size, boolean shared)
        throws IOException;

    /**
     * Attempts to acquire an exclusive lock on this channel's file.
     *
     * <p> An invocation of this method of the form {@code fc.tryLock()}
     * behaves in exactly the same way as the invocation
     *
     * <pre>
     *     fc.{@link #tryLock(long,long,boolean) tryLock}(0L, Long.MAX_VALUE, false) </pre>
     *
     * @return  A lock object representing the newly-acquired lock,
     *          or {@code null} if the lock could not be acquired
     *          because another program holds an overlapping lock
     *
     * @throws  ClosedChannelException
     *          If this channel is closed
     *
     * @throws  OverlappingFileLockException
     *          If a lock that overlaps the requested region is already held by
     *          this Java virtual machine, or if another thread is already
     *          blocked in this method and is attempting to lock an overlapping
     *          region
     *
     * @throws  IOException
     *          If some other I/O error occurs
     *
     * @see     #lock()
     * @see     #lock(long,long,boolean)
     * @see     #tryLock(long,long,boolean)
     */
    public final FileLock tryLock() throws IOException {
        return tryLock(0L, Long.MAX_VALUE, false);
    }

}
