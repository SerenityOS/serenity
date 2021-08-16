/*
 * Copyright (c) 1995, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.util.zip;

import java.io.Closeable;
import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner.Cleanable;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.file.InvalidPathException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import jdk.internal.access.JavaUtilZipFileAccess;
import jdk.internal.access.JavaUtilJarAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.VM;
import jdk.internal.perf.PerfCounter;
import jdk.internal.ref.CleanerFactory;
import jdk.internal.vm.annotation.Stable;
import sun.nio.cs.UTF_8;
import sun.security.util.SignatureFileVerifier;

import static java.util.zip.ZipConstants64.*;
import static java.util.zip.ZipUtils.*;

/**
 * This class is used to read entries from a zip file.
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 *
 * @apiNote
 * To release resources used by this {@code ZipFile}, the {@link #close()} method
 * should be called explicitly or by try-with-resources. Subclasses are responsible
 * for the cleanup of resources acquired by the subclass. Subclasses that override
 * {@link #finalize()} in order to perform cleanup should be modified to use alternative
 * cleanup mechanisms such as {@link java.lang.ref.Cleaner} and remove the overriding
 * {@code finalize} method.
 *
 * @author      David Connelly
 * @since 1.1
 */
public class ZipFile implements ZipConstants, Closeable {

    private final String name;     // zip file name
    private volatile boolean closeRequested;

    // The "resource" used by this zip file that needs to be
    // cleaned after use.
    // a) the input streams that need to be closed
    // b) the list of cached Inflater objects
    // c) the "native" source of this zip file.
    private final @Stable CleanableResource res;

    private static final int STORED = ZipEntry.STORED;
    private static final int DEFLATED = ZipEntry.DEFLATED;

    /**
     * Mode flag to open a zip file for reading.
     */
    public static final int OPEN_READ = 0x1;

    /**
     * Mode flag to open a zip file and mark it for deletion.  The file will be
     * deleted some time between the moment that it is opened and the moment
     * that it is closed, but its contents will remain accessible via the
     * {@code ZipFile} object until either the close method is invoked or the
     * virtual machine exits.
     */
    public static final int OPEN_DELETE = 0x4;

    /**
     * Opens a zip file for reading.
     *
     * <p>First, if there is a security manager, its {@code checkRead}
     * method is called with the {@code name} argument as its argument
     * to ensure the read is allowed.
     *
     * <p>The UTF-8 {@link java.nio.charset.Charset charset} is used to
     * decode the entry names and comments.
     *
     * @param name the name of the zip file
     * @throws ZipException if a ZIP format error has occurred
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if a security manager exists and its
     *         {@code checkRead} method doesn't allow read access to the file.
     *
     * @see SecurityManager#checkRead(java.lang.String)
     */
    public ZipFile(String name) throws IOException {
        this(new File(name), OPEN_READ);
    }

    /**
     * Opens a new {@code ZipFile} to read from the specified
     * {@code File} object in the specified mode.  The mode argument
     * must be either {@code OPEN_READ} or {@code OPEN_READ | OPEN_DELETE}.
     *
     * <p>First, if there is a security manager, its {@code checkRead}
     * method is called with the {@code name} argument as its argument to
     * ensure the read is allowed.
     *
     * <p>The UTF-8 {@link java.nio.charset.Charset charset} is used to
     * decode the entry names and comments
     *
     * @param file the ZIP file to be opened for reading
     * @param mode the mode in which the file is to be opened
     * @throws ZipException if a ZIP format error has occurred
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException if a security manager exists and
     *         its {@code checkRead} method
     *         doesn't allow read access to the file,
     *         or its {@code checkDelete} method doesn't allow deleting
     *         the file when the {@code OPEN_DELETE} flag is set.
     * @throws IllegalArgumentException if the {@code mode} argument is invalid
     * @see SecurityManager#checkRead(java.lang.String)
     * @since 1.3
     */
    public ZipFile(File file, int mode) throws IOException {
        this(file, mode, UTF_8.INSTANCE);
    }

    /**
     * Opens a ZIP file for reading given the specified File object.
     *
     * <p>The UTF-8 {@link java.nio.charset.Charset charset} is used to
     * decode the entry names and comments.
     *
     * @param file the ZIP file to be opened for reading
     * @throws ZipException if a ZIP format error has occurred
     * @throws IOException if an I/O error has occurred
     */
    public ZipFile(File file) throws ZipException, IOException {
        this(file, OPEN_READ);
    }

    /**
     * Opens a new {@code ZipFile} to read from the specified
     * {@code File} object in the specified mode.  The mode argument
     * must be either {@code OPEN_READ} or {@code OPEN_READ | OPEN_DELETE}.
     *
     * <p>First, if there is a security manager, its {@code checkRead}
     * method is called with the {@code name} argument as its argument to
     * ensure the read is allowed.
     *
     * @param file the ZIP file to be opened for reading
     * @param mode the mode in which the file is to be opened
     * @param charset
     *        the {@linkplain java.nio.charset.Charset charset} to
     *        be used to decode the ZIP entry name and comment that are not
     *        encoded by using UTF-8 encoding (indicated by entry's general
     *        purpose flag).
     *
     * @throws ZipException if a ZIP format error has occurred
     * @throws IOException if an I/O error has occurred
     *
     * @throws SecurityException
     *         if a security manager exists and its {@code checkRead}
     *         method doesn't allow read access to the file,or its
     *         {@code checkDelete} method doesn't allow deleting the
     *         file when the {@code OPEN_DELETE} flag is set
     *
     * @throws IllegalArgumentException if the {@code mode} argument is invalid
     *
     * @see SecurityManager#checkRead(java.lang.String)
     *
     * @since 1.7
     */
    public ZipFile(File file, int mode, Charset charset) throws IOException
    {
        if (((mode & OPEN_READ) == 0) ||
            ((mode & ~(OPEN_READ | OPEN_DELETE)) != 0)) {
            throw new IllegalArgumentException("Illegal mode: 0x"+
                                               Integer.toHexString(mode));
        }
        String name = file.getPath();
        file = new File(name);
        @SuppressWarnings("removal")
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkRead(name);
            if ((mode & OPEN_DELETE) != 0) {
                sm.checkDelete(name);
            }
        }
        Objects.requireNonNull(charset, "charset");

        this.name = name;
        long t0 = System.nanoTime();

        this.res = new CleanableResource(this, ZipCoder.get(charset), file, mode);

        PerfCounter.getZipFileOpenTime().addElapsedTimeFrom(t0);
        PerfCounter.getZipFileCount().increment();
    }

    /**
     * Opens a zip file for reading.
     *
     * <p>First, if there is a security manager, its {@code checkRead}
     * method is called with the {@code name} argument as its argument
     * to ensure the read is allowed.
     *
     * @param name the name of the zip file
     * @param charset
     *        the {@linkplain java.nio.charset.Charset charset} to
     *        be used to decode the ZIP entry name and comment that are not
     *        encoded by using UTF-8 encoding (indicated by entry's general
     *        purpose flag).
     *
     * @throws ZipException if a ZIP format error has occurred
     * @throws IOException if an I/O error has occurred
     * @throws SecurityException
     *         if a security manager exists and its {@code checkRead}
     *         method doesn't allow read access to the file
     *
     * @see SecurityManager#checkRead(java.lang.String)
     *
     * @since 1.7
     */
    public ZipFile(String name, Charset charset) throws IOException
    {
        this(new File(name), OPEN_READ, charset);
    }

    /**
     * Opens a ZIP file for reading given the specified File object.
     *
     * @param file the ZIP file to be opened for reading
     * @param charset
     *        The {@linkplain java.nio.charset.Charset charset} to be
     *        used to decode the ZIP entry name and comment (ignored if
     *        the <a href="package-summary.html#lang_encoding"> language
     *        encoding bit</a> of the ZIP entry's general purpose bit
     *        flag is set).
     *
     * @throws ZipException if a ZIP format error has occurred
     * @throws IOException if an I/O error has occurred
     *
     * @since 1.7
     */
    public ZipFile(File file, Charset charset) throws IOException
    {
        this(file, OPEN_READ, charset);
    }

    /**
     * Returns the zip file comment, or null if none.
     *
     * @return the comment string for the zip file, or null if none
     *
     * @throws IllegalStateException if the zip file has been closed
     *
     * @since 1.7
     */
    public String getComment() {
        synchronized (this) {
            ensureOpen();
            if (res.zsrc.comment == null) {
                return null;
            }
            return res.zsrc.zc.toString(res.zsrc.comment);
        }
    }

    /**
     * Returns the zip file entry for the specified name, or null
     * if not found.
     *
     * @param name the name of the entry
     * @return the zip file entry, or null if not found
     * @throws IllegalStateException if the zip file has been closed
     */
    public ZipEntry getEntry(String name) {
        Objects.requireNonNull(name, "name");
        ZipEntry entry = null;
        synchronized (this) {
            ensureOpen();
            int pos = res.zsrc.getEntryPos(name, true);
            if (pos != -1) {
                entry = getZipEntry(name, pos);
            }
        }
        return entry;
    }

    /**
     * Returns an input stream for reading the contents of the specified
     * zip file entry.
     * <p>
     * Closing this ZIP file will, in turn, close all input streams that
     * have been returned by invocations of this method.
     *
     * @param entry the zip file entry
     * @return the input stream for reading the contents of the specified
     * zip file entry.
     * @throws ZipException if a ZIP format error has occurred
     * @throws IOException if an I/O error has occurred
     * @throws IllegalStateException if the zip file has been closed
     */
    public InputStream getInputStream(ZipEntry entry) throws IOException {
        Objects.requireNonNull(entry, "entry");
        int pos;
        ZipFileInputStream in;
        Source zsrc = res.zsrc;
        Set<InputStream> istreams = res.istreams;
        synchronized (this) {
            ensureOpen();
            if (Objects.equals(lastEntryName, entry.name)) {
                pos = lastEntryPos;
            } else {
                pos = zsrc.getEntryPos(entry.name, false);
            }
            if (pos == -1) {
                return null;
            }
            in = new ZipFileInputStream(zsrc.cen, pos);
            switch (CENHOW(zsrc.cen, pos)) {
            case STORED:
                synchronized (istreams) {
                    istreams.add(in);
                }
                return in;
            case DEFLATED:
                // Inflater likes a bit of slack
                // MORE: Compute good size for inflater stream:
                long size = CENLEN(zsrc.cen, pos) + 2;
                if (size > 65536) {
                    size = 8192;
                }
                if (size <= 0) {
                    size = 4096;
                }
                InputStream is = new ZipFileInflaterInputStream(in, res, (int)size);
                synchronized (istreams) {
                    istreams.add(is);
                }
                return is;
            default:
                throw new ZipException("invalid compression method");
            }
        }
    }

    private static class InflaterCleanupAction implements Runnable {
        private final Inflater inf;
        private final CleanableResource res;

        InflaterCleanupAction(Inflater inf, CleanableResource res) {
            this.inf = inf;
            this.res = res;
        }

        @Override
        public void run() {
            res.releaseInflater(inf);
        }
    }

    private class ZipFileInflaterInputStream extends InflaterInputStream {
        private volatile boolean closeRequested;
        private boolean eof = false;
        private final Cleanable cleanable;

        ZipFileInflaterInputStream(ZipFileInputStream zfin,
                                   CleanableResource res, int size) {
            this(zfin, res, res.getInflater(), size);
        }

        private ZipFileInflaterInputStream(ZipFileInputStream zfin,
                                           CleanableResource res,
                                           Inflater inf, int size) {
            super(zfin, inf, size);
            this.cleanable = CleanerFactory.cleaner().register(this,
                    new InflaterCleanupAction(inf, res));
        }

        public void close() throws IOException {
            if (closeRequested)
                return;
            closeRequested = true;
            super.close();
            synchronized (res.istreams) {
                res.istreams.remove(this);
            }
            cleanable.clean();
        }

        // Override fill() method to provide an extra "dummy" byte
        // at the end of the input stream. This is required when
        // using the "nowrap" Inflater option.
        protected void fill() throws IOException {
            if (eof) {
                throw new EOFException("Unexpected end of ZLIB input stream");
            }
            len = in.read(buf, 0, buf.length);
            if (len == -1) {
                buf[0] = 0;
                len = 1;
                eof = true;
            }
            inf.setInput(buf, 0, len);
        }

        public int available() throws IOException {
            if (closeRequested)
                return 0;
            long avail = ((ZipFileInputStream)in).size() - inf.getBytesWritten();
            return (avail > (long) Integer.MAX_VALUE ?
                    Integer.MAX_VALUE : (int) avail);
        }
    }

    /**
     * Returns the path name of the ZIP file.
     * @return the path name of the ZIP file
     */
    public String getName() {
        return name;
    }

    private class ZipEntryIterator<T extends ZipEntry>
            implements Enumeration<T>, Iterator<T> {

        private int i = 0;
        private final int entryCount;

        public ZipEntryIterator(int entryCount) {
            this.entryCount = entryCount;
        }

        @Override
        public boolean hasMoreElements() {
            return hasNext();
        }

        @Override
        public boolean hasNext() {
            return i < entryCount;
        }

        @Override
        public T nextElement() {
            return next();
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            synchronized (ZipFile.this) {
                ensureOpen();
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                // each "entry" has 3 ints in table entries
                return (T)getZipEntry(null, res.zsrc.getEntryPos(i++ * 3));
            }
        }

        @Override
        public Iterator<T> asIterator() {
            return this;
        }
    }

    /**
     * Returns an enumeration of the ZIP file entries.
     * @return an enumeration of the ZIP file entries
     * @throws IllegalStateException if the zip file has been closed
     */
    public Enumeration<? extends ZipEntry> entries() {
        synchronized (this) {
            ensureOpen();
            return new ZipEntryIterator<ZipEntry>(res.zsrc.total);
        }
    }

    private Enumeration<JarEntry> jarEntries() {
        synchronized (this) {
            ensureOpen();
            return new ZipEntryIterator<JarEntry>(res.zsrc.total);
        }
    }

    private class EntrySpliterator<T> extends Spliterators.AbstractSpliterator<T> {
        private int index;
        private final int fence;
        private final IntFunction<T> gen;

        EntrySpliterator(int index, int fence, IntFunction<T> gen) {
            super((long)fence,
                  Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.IMMUTABLE |
                  Spliterator.NONNULL);
            this.index = index;
            this.fence = fence;
            this.gen = gen;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (action == null)
                throw new NullPointerException();
            if (index >= 0 && index < fence) {
                synchronized (ZipFile.this) {
                    ensureOpen();
                    action.accept(gen.apply(res.zsrc.getEntryPos(index++ * 3)));
                }
                return true;
            }
            return false;
        }
    }

    /**
     * Returns an ordered {@code Stream} over the ZIP file entries.
     *
     * Entries appear in the {@code Stream} in the order they appear in
     * the central directory of the ZIP file.
     *
     * @return an ordered {@code Stream} of entries in this ZIP file
     * @throws IllegalStateException if the zip file has been closed
     * @since 1.8
     */
    public Stream<? extends ZipEntry> stream() {
        synchronized (this) {
            ensureOpen();
            return StreamSupport.stream(new EntrySpliterator<>(0, res.zsrc.total,
                pos -> getZipEntry(null, pos)), false);
       }
    }

    private String getEntryName(int pos) {
        byte[] cen = res.zsrc.cen;
        int nlen = CENNAM(cen, pos);
        ZipCoder zc = res.zsrc.zipCoderForPos(pos);
        return zc.toString(cen, pos + CENHDR, nlen);
    }

    /*
     * Returns an ordered {@code Stream} over the zip file entry names.
     *
     * Entry names appear in the {@code Stream} in the order they appear in
     * the central directory of the ZIP file.
     *
     * @return an ordered {@code Stream} of entry names in this zip file
     * @throws IllegalStateException if the zip file has been closed
     * @since 10
     */
    private Stream<String> entryNameStream() {
        synchronized (this) {
            ensureOpen();
            return StreamSupport.stream(
                new EntrySpliterator<>(0, res.zsrc.total, this::getEntryName), false);
        }
    }

    /*
     * Returns an ordered {@code Stream} over the zip file entries.
     *
     * Entries appear in the {@code Stream} in the order they appear in
     * the central directory of the jar file.
     *
     * @return an ordered {@code Stream} of entries in this zip file
     * @throws IllegalStateException if the zip file has been closed
     * @since 10
     */
    private Stream<JarEntry> jarStream() {
        synchronized (this) {
            ensureOpen();
            return StreamSupport.stream(new EntrySpliterator<>(0, res.zsrc.total,
                pos -> (JarEntry)getZipEntry(null, pos)), false);
        }
    }

    private String lastEntryName;
    private int lastEntryPos;

    /* Check ensureOpen() before invoking this method */
    private ZipEntry getZipEntry(String name, int pos) {
        byte[] cen = res.zsrc.cen;
        int nlen = CENNAM(cen, pos);
        int elen = CENEXT(cen, pos);
        int clen = CENCOM(cen, pos);

        ZipCoder zc = res.zsrc.zipCoderForPos(pos);
        if (name != null) {
            // only need to check for mismatch of trailing slash
            if (nlen > 0 &&
                !name.isEmpty() &&
                zc.hasTrailingSlash(cen, pos + CENHDR + nlen) &&
                !name.endsWith("/"))
            {
                name += '/';
            }
        } else {
            // invoked from iterator, use the entry name stored in cen
            name = zc.toString(cen, pos + CENHDR, nlen);
        }
        ZipEntry e;
        if (this instanceof JarFile) {
            e = Source.JUJA.entryFor((JarFile)this, name);
        } else {
            e = new ZipEntry(name);
        }
        e.flag = CENFLG(cen, pos);
        e.xdostime = CENTIM(cen, pos);
        e.crc = CENCRC(cen, pos);
        e.size = CENLEN(cen, pos);
        e.csize = CENSIZ(cen, pos);
        e.method = CENHOW(cen, pos);
        if (CENVEM_FA(cen, pos) == FILE_ATTRIBUTES_UNIX) {
            // read all bits in this field, including sym link attributes
            e.extraAttributes = CENATX_PERMS(cen, pos) & 0xFFFF;
        }

        if (elen != 0) {
            int start = pos + CENHDR + nlen;
            e.setExtra0(Arrays.copyOfRange(cen, start, start + elen), true, false);
        }
        if (clen != 0) {
            int start = pos + CENHDR + nlen + elen;
            e.comment = zc.toString(cen, start, clen);
        }
        lastEntryName = e.name;
        lastEntryPos = pos;
        return e;
    }

    /**
     * Returns the number of entries in the ZIP file.
     *
     * @return the number of entries in the ZIP file
     * @throws IllegalStateException if the zip file has been closed
     */
    public int size() {
        synchronized (this) {
            ensureOpen();
            return res.zsrc.total;
        }
    }

    private static class CleanableResource implements Runnable {
        // The outstanding inputstreams that need to be closed
        final Set<InputStream> istreams;

        // List of cached Inflater objects for decompression
        Deque<Inflater> inflaterCache;

        final Cleanable cleanable;

        Source zsrc;

        CleanableResource(ZipFile zf, ZipCoder zc, File file, int mode) throws IOException {
            this.cleanable = CleanerFactory.cleaner().register(zf, this);
            this.istreams = Collections.newSetFromMap(new WeakHashMap<>());
            this.inflaterCache = new ArrayDeque<>();
            this.zsrc = Source.get(file, (mode & OPEN_DELETE) != 0, zc);
        }

        void clean() {
            cleanable.clean();
        }

        /*
         * Gets an inflater from the list of available inflaters or allocates
         * a new one.
         */
        Inflater getInflater() {
            Inflater inf;
            synchronized (inflaterCache) {
                if ((inf = inflaterCache.poll()) != null) {
                    return inf;
                }
            }
            return new Inflater(true);
        }

        /*
         * Releases the specified inflater to the list of available inflaters.
         */
        void releaseInflater(Inflater inf) {
            Deque<Inflater> inflaters = this.inflaterCache;
            if (inflaters != null) {
                synchronized (inflaters) {
                    // double checked!
                    if (inflaters == this.inflaterCache) {
                        inf.reset();
                        inflaters.add(inf);
                        return;
                    }
                }
            }
            // inflaters cache already closed - just end it.
            inf.end();
        }

        public void run() {
            IOException ioe = null;

            // Release cached inflaters and close the cache first
            Deque<Inflater> inflaters = this.inflaterCache;
            if (inflaters != null) {
                synchronized (inflaters) {
                    // no need to double-check as only one thread gets a
                    // chance to execute run() (Cleaner guarantee)...
                    Inflater inf;
                    while ((inf = inflaters.poll()) != null) {
                        inf.end();
                    }
                    // close inflaters cache
                    this.inflaterCache = null;
                }
            }

            // Close streams, release their inflaters
            if (istreams != null) {
                synchronized (istreams) {
                    if (!istreams.isEmpty()) {
                        InputStream[] copy = istreams.toArray(new InputStream[0]);
                        istreams.clear();
                        for (InputStream is : copy) {
                            try {
                                is.close();
                            } catch (IOException e) {
                                if (ioe == null) ioe = e;
                                else ioe.addSuppressed(e);
                            }
                        }
                    }
                }
            }

            // Release zip src
            if (zsrc != null) {
                synchronized (zsrc) {
                    try {
                        Source.release(zsrc);
                        zsrc = null;
                    } catch (IOException e) {
                        if (ioe == null) ioe = e;
                        else ioe.addSuppressed(e);
                    }
                }
            }
            if (ioe != null) {
                throw new UncheckedIOException(ioe);
            }
        }
    }

    /**
     * Closes the ZIP file.
     *
     * <p> Closing this ZIP file will close all of the input streams
     * previously returned by invocations of the {@link #getInputStream
     * getInputStream} method.
     *
     * @throws IOException if an I/O error has occurred
     */
    public void close() throws IOException {
        if (closeRequested) {
            return;
        }
        closeRequested = true;

        synchronized (this) {
            // Close streams, release their inflaters, release cached inflaters
            // and release zip source
            try {
                res.clean();
            } catch (UncheckedIOException ioe) {
                throw ioe.getCause();
            }
        }
    }

    private void ensureOpen() {
        if (closeRequested) {
            throw new IllegalStateException("zip file closed");
        }
        if (res.zsrc == null) {
            throw new IllegalStateException("The object is not initialized.");
        }
    }

    private void ensureOpenOrZipException() throws IOException {
        if (closeRequested) {
            throw new ZipException("ZipFile closed");
        }
    }

    /*
     * Inner class implementing the input stream used to read a
     * (possibly compressed) zip file entry.
     */
    private class ZipFileInputStream extends InputStream {
        private volatile boolean closeRequested;
        private   long pos;     // current position within entry data
        private   long startingPos; // Start position for the entry data
        protected long rem;     // number of remaining bytes within entry
        protected long size;    // uncompressed size of this entry

        ZipFileInputStream(byte[] cen, int cenpos) {
            rem = CENSIZ(cen, cenpos);
            size = CENLEN(cen, cenpos);
            pos = CENOFF(cen, cenpos);
            // zip64
            if (rem == ZIP64_MAGICVAL || size == ZIP64_MAGICVAL ||
                pos == ZIP64_MAGICVAL) {
                checkZIP64(cen, cenpos);
            }
            // negative for lazy initialization, see getDataOffset();
            pos = - (pos + ZipFile.this.res.zsrc.locpos);
        }

        private void checkZIP64(byte[] cen, int cenpos) {
            int off = cenpos + CENHDR + CENNAM(cen, cenpos);
            int end = off + CENEXT(cen, cenpos);
            while (off + 4 < end) {
                int tag = get16(cen, off);
                int sz = get16(cen, off + 2);
                off += 4;
                if (off + sz > end)         // invalid data
                    break;
                if (tag == EXTID_ZIP64) {
                    if (size == ZIP64_MAGICVAL) {
                        if (sz < 8 || (off + 8) > end)
                            break;
                        size = get64(cen, off);
                        sz -= 8;
                        off += 8;
                    }
                    if (rem == ZIP64_MAGICVAL) {
                        if (sz < 8 || (off + 8) > end)
                            break;
                        rem = get64(cen, off);
                        sz -= 8;
                        off += 8;
                    }
                    if (pos == ZIP64_MAGICVAL) {
                        if (sz < 8 || (off + 8) > end)
                            break;
                        pos = get64(cen, off);
                        sz -= 8;
                        off += 8;
                    }
                    break;
                }
                off += sz;
            }
        }

        /*
         * The Zip file spec explicitly allows the LOC extra data size to
         * be different from the CEN extra data size. Since we cannot trust
         * the CEN extra data size, we need to read the LOC to determine
         * the entry data offset.
         */
        private long initDataOffset() throws IOException {
            if (pos <= 0) {
                byte[] loc = new byte[LOCHDR];
                pos = -pos;
                int len = ZipFile.this.res.zsrc.readFullyAt(loc, 0, loc.length, pos);
                if (len != LOCHDR) {
                    throw new ZipException("ZipFile error reading zip file");
                }
                if (LOCSIG(loc) != LOCSIG) {
                    throw new ZipException("ZipFile invalid LOC header (bad signature)");
                }
                pos += LOCHDR + LOCNAM(loc) + LOCEXT(loc);
                startingPos = pos; // Save starting position for the entry
            }
            return pos;
        }

        public int read(byte b[], int off, int len) throws IOException {
            synchronized (ZipFile.this) {
                ensureOpenOrZipException();
                initDataOffset();
                if (rem == 0) {
                    return -1;
                }
                if (len > rem) {
                    len = (int) rem;
                }
                if (len <= 0) {
                    return 0;
                }
                len = ZipFile.this.res.zsrc.readAt(b, off, len, pos);
                if (len > 0) {
                    pos += len;
                    rem -= len;
                }
            }
            if (rem == 0) {
                close();
            }
            return len;
        }

        public int read() throws IOException {
            byte[] b = new byte[1];
            if (read(b, 0, 1) == 1) {
                return b[0] & 0xff;
            } else {
                return -1;
            }
        }

        public long skip(long n) throws IOException {
            synchronized (ZipFile.this) {
                initDataOffset();
                long newPos = pos + n;
                if (n > 0) {
                    // If we overflowed adding the skip value or are moving
                    // past EOF, set the skip value to number of bytes remaining
                    // to reach EOF
                    if (newPos < 0 || n > rem) {
                        n = rem;
                    }
                } else if (newPos < startingPos) {
                    // Tried to position before BOF so set position to the
                    // BOF and return the number of bytes we moved backwards
                    // to reach BOF
                    n = startingPos - pos;
                }
                pos += n;
                rem -= n;
            }
            if (rem == 0) {
                close();
            }
            return n;
        }

        public int available() {
            return rem > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) rem;
        }

        public long size() {
            return size;
        }

        public void close() {
            if (closeRequested) {
                return;
            }
            closeRequested = true;
            rem = 0;
            synchronized (res.istreams) {
                res.istreams.remove(this);
            }
        }

    }

    /**
     * Returns the names of the META-INF/MANIFEST.MF entry - if exists -
     * and any signature-related files under META-INF. This method is used in
     * JarFile, via SharedSecrets, as an optimization.
     */
    private List<String> getManifestAndSignatureRelatedFiles() {
        synchronized (this) {
            ensureOpen();
            Source zsrc = res.zsrc;
            int[] metanames = zsrc.signatureMetaNames;
            List<String> files = null;
            if (zsrc.manifestPos >= 0) {
                files = new ArrayList<>();
                files.add(getEntryName(zsrc.manifestPos));
            }
            if (metanames != null) {
                if (files == null) {
                    files = new ArrayList<>();
                }
                for (int i = 0; i < metanames.length; i++) {
                    files.add(getEntryName(metanames[i]));
                }
            }
            return files == null ? List.of() : files;
        }
    }

    /**
     * Returns the number of the META-INF/MANIFEST.MF entries, case insensitive.
     * When this number is greater than 1, JarVerifier will treat a file as
     * unsigned.
     */
    private int getManifestNum() {
        synchronized (this) {
            ensureOpen();
            return res.zsrc.manifestNum;
        }
    }

    /**
     * Returns the name of the META-INF/MANIFEST.MF entry, ignoring
     * case. If {@code onlyIfSignatureRelatedFiles} is true, we only return the
     * manifest if there is also at least one signature-related file.
     * This method is used in JarFile, via SharedSecrets, as an optimization
     * when looking up the manifest file.
     */
    private String getManifestName(boolean onlyIfSignatureRelatedFiles) {
        synchronized (this) {
            ensureOpen();
            Source zsrc = res.zsrc;
            int pos = zsrc.manifestPos;
            if (pos >= 0 && (!onlyIfSignatureRelatedFiles || zsrc.signatureMetaNames != null)) {
                return getEntryName(pos);
            }
        }
        return null;
    }

    /**
     * Returns the versions for which there exists a non-directory
     * entry that begin with "META-INF/versions/" (case ignored).
     * This method is used in JarFile, via SharedSecrets, as an
     * optimization when looking up potentially versioned entries.
     * Returns an empty array if no versioned entries exist.
     */
    private int[] getMetaInfVersions() {
        synchronized (this) {
            ensureOpen();
            return res.zsrc.metaVersions;
        }
    }

    private static boolean isWindows;

    static {
        SharedSecrets.setJavaUtilZipFileAccess(
            new JavaUtilZipFileAccess() {
                @Override
                public boolean startsWithLocHeader(ZipFile zip) {
                    return zip.res.zsrc.startsWithLoc;
                }
                @Override
                public List<String> getManifestAndSignatureRelatedFiles(JarFile jar) {
                    return ((ZipFile)jar).getManifestAndSignatureRelatedFiles();
                }
                @Override
                public int getManifestNum(JarFile jar) {
                    return ((ZipFile)jar).getManifestNum();
                }
                @Override
                public String getManifestName(JarFile jar, boolean onlyIfHasSignatureRelatedFiles) {
                    return ((ZipFile)jar).getManifestName(onlyIfHasSignatureRelatedFiles);
                }
                @Override
                public int[] getMetaInfVersions(JarFile jar) {
                    return ((ZipFile)jar).getMetaInfVersions();
                }
                @Override
                public Enumeration<JarEntry> entries(ZipFile zip) {
                    return zip.jarEntries();
                }
                @Override
                public Stream<JarEntry> stream(ZipFile zip) {
                    return zip.jarStream();
                }
                @Override
                public Stream<String> entryNameStream(ZipFile zip) {
                    return zip.entryNameStream();
                }
                @Override
                public int getExtraAttributes(ZipEntry ze) {
                    return ze.extraAttributes;
                }
                @Override
                public void setExtraAttributes(ZipEntry ze, int extraAttrs) {
                    ze.extraAttributes = extraAttrs;
                }

             }
        );
        isWindows = VM.getSavedProperty("os.name").contains("Windows");
    }

    private static class Source {
        // While this is only used from ZipFile, defining it there would cause
        // a bootstrap cycle that would leave this initialized as null
        private static final JavaUtilJarAccess JUJA = SharedSecrets.javaUtilJarAccess();
        // "META-INF/".length()
        private static final int META_INF_LEN = 9;
        private static final int[] EMPTY_META_VERSIONS = new int[0];

        private final Key key;               // the key in files
        private final @Stable ZipCoder zc;   // zip coder used to decode/encode

        private int refs = 1;

        private RandomAccessFile zfile;      // zfile of the underlying zip file
        private byte[] cen;                  // CEN & ENDHDR
        private long locpos;                 // position of first LOC header (usually 0)
        private byte[] comment;              // zip file comment
                                             // list of meta entries in META-INF dir
        private int   manifestPos = -1;      // position of the META-INF/MANIFEST.MF, if exists
        private int   manifestNum = 0;       // number of META-INF/MANIFEST.MF, case insensitive
        private int[] signatureMetaNames;    // positions of signature related entries, if such exist
        private int[] metaVersions;          // list of unique versions found in META-INF/versions/
        private final boolean startsWithLoc; // true, if zip file starts with LOCSIG (usually true)

        // A Hashmap for all entries.
        //
        // A cen entry of Zip/JAR file. As we have one for every entry in every active Zip/JAR,
        // We might have a lot of these in a typical system. In order to save space we don't
        // keep the name in memory, but merely remember a 32 bit {@code hash} value of the
        // entry name and its offset {@code pos} in the central directory hdeader.
        //
        // private static class Entry {
        //     int hash;       // 32 bit hashcode on name
        //     int next;       // hash chain: index into entries
        //     int pos;        // Offset of central directory file header
        // }
        // private Entry[] entries;             // array of hashed cen entry
        //
        // To reduce the total size of entries further, we use a int[] here to store 3 "int"
        // {@code hash}, {@code next} and {@code pos} for each entry. The entry can then be
        // referred by their index of their positions in the {@code entries}.
        //
        private int[] entries;                  // array of hashed cen entry

        // Checks the entry at offset pos in the CEN, calculates the Entry values as per above,
        // then returns the length of the entry name.
        private int checkAndAddEntry(int pos, int index)
            throws ZipException
        {
            byte[] cen = this.cen;
            if (CENSIG(cen, pos) != CENSIG) {
                zerror("invalid CEN header (bad signature)");
            }
            int method = CENHOW(cen, pos);
            int flag   = CENFLG(cen, pos);
            if ((flag & 1) != 0) {
                zerror("invalid CEN header (encrypted entry)");
            }
            if (method != STORED && method != DEFLATED) {
                zerror("invalid CEN header (bad compression method: " + method + ")");
            }
            int entryPos = pos + CENHDR;
            int nlen = CENNAM(cen, pos);
            if (entryPos + nlen > cen.length - ENDHDR) {
                zerror("invalid CEN header (bad header size)");
            }
            try {
                ZipCoder zcp = zipCoderForPos(pos);
                int hash = zcp.checkedHash(cen, entryPos, nlen);
                int hsh = (hash & 0x7fffffff) % tablelen;
                int next = table[hsh];
                table[hsh] = index;
                // Record the CEN offset and the name hash in our hash cell.
                entries[index++] = hash;
                entries[index++] = next;
                entries[index  ] = pos;
            } catch (Exception e) {
                zerror("invalid CEN header (bad entry name)");
            }
            return nlen;
        }

        private int getEntryHash(int index) { return entries[index]; }
        private int getEntryNext(int index) { return entries[index + 1]; }
        private int getEntryPos(int index)  { return entries[index + 2]; }
        private static final int ZIP_ENDCHAIN  = -1;
        private int total;                   // total number of entries
        private int[] table;                 // Hash chain heads: indexes into entries
        private int tablelen;                // number of hash heads

        private static class Key {
            final BasicFileAttributes attrs;
            File file;
            final boolean utf8;

            public Key(File file, BasicFileAttributes attrs, ZipCoder zc) {
                this.attrs = attrs;
                this.file = file;
                this.utf8 = zc.isUTF8();
            }

            public int hashCode() {
                long t = utf8 ? 0 : Long.MAX_VALUE;
                t += attrs.lastModifiedTime().toMillis();
                return ((int)(t ^ (t >>> 32))) + file.hashCode();
            }

            public boolean equals(Object obj) {
                if (obj instanceof Key key) {
                    if (key.utf8 != utf8) {
                        return false;
                    }
                    if (!attrs.lastModifiedTime().equals(key.attrs.lastModifiedTime())) {
                        return false;
                    }
                    Object fk = attrs.fileKey();
                    if (fk != null) {
                        return fk.equals(key.attrs.fileKey());
                    } else {
                        return file.equals(key.file);
                    }
                }
                return false;
            }
        }
        private static final HashMap<Key, Source> files = new HashMap<>();


        static Source get(File file, boolean toDelete, ZipCoder zc) throws IOException {
            final Key key;
            try {
                key = new Key(file,
                        Files.readAttributes(file.toPath(), BasicFileAttributes.class),
                        zc);
            } catch (InvalidPathException ipe) {
                throw new IOException(ipe);
            }
            Source src;
            synchronized (files) {
                src = files.get(key);
                if (src != null) {
                    src.refs++;
                    return src;
                }
            }
            src = new Source(key, toDelete, zc);

            synchronized (files) {
                if (files.containsKey(key)) {    // someone else put in first
                    src.close();                 // close the newly created one
                    src = files.get(key);
                    src.refs++;
                    return src;
                }
                files.put(key, src);
                return src;
            }
        }

        static void release(Source src) throws IOException {
            synchronized (files) {
                if (src != null && --src.refs == 0) {
                    files.remove(src.key);
                    src.close();
                }
            }
        }

        private Source(Key key, boolean toDelete, ZipCoder zc) throws IOException {
            this.zc = zc;
            this.key = key;
            if (toDelete) {
                if (isWindows) {
                    this.zfile = SharedSecrets.getJavaIORandomAccessFileAccess()
                                              .openAndDelete(key.file, "r");
                } else {
                    this.zfile = new RandomAccessFile(key.file, "r");
                    key.file.delete();
                }
            } else {
                this.zfile = new RandomAccessFile(key.file, "r");
            }
            try {
                initCEN(-1);
                byte[] buf = new byte[4];
                readFullyAt(buf, 0, 4, 0);
                this.startsWithLoc = (LOCSIG(buf) == LOCSIG);
            } catch (IOException x) {
                try {
                    this.zfile.close();
                } catch (IOException xx) {}
                throw x;
            }
        }

        private void close() throws IOException {
            zfile.close();
            zfile = null;
            cen = null;
            entries = null;
            table = null;
            manifestPos = -1;
            manifestNum = 0;
            signatureMetaNames = null;
            metaVersions = EMPTY_META_VERSIONS;
        }

        private static final int BUF_SIZE = 8192;
        private final int readFullyAt(byte[] buf, int off, int len, long pos)
            throws IOException
        {
            synchronized (zfile) {
                zfile.seek(pos);
                int N = len;
                while (N > 0) {
                    int n = Math.min(BUF_SIZE, N);
                    zfile.readFully(buf, off, n);
                    off += n;
                    N -= n;
                }
                return len;
            }
        }

        private final int readAt(byte[] buf, int off, int len, long pos)
            throws IOException
        {
            synchronized (zfile) {
                zfile.seek(pos);
                return zfile.read(buf, off, len);
            }
        }


        private static class End {
            int  centot;     // 4 bytes
            long cenlen;     // 4 bytes
            long cenoff;     // 4 bytes
            long endpos;     // 4 bytes
        }

        /*
         * Searches for end of central directory (END) header. The contents of
         * the END header will be read and placed in endbuf. Returns the file
         * position of the END header, otherwise returns -1 if the END header
         * was not found or an error occurred.
         */
        private End findEND() throws IOException {
            long ziplen = zfile.length();
            if (ziplen <= 0)
                zerror("zip file is empty");
            End end = new End();
            byte[] buf = new byte[READBLOCKSZ];
            long minHDR = (ziplen - END_MAXLEN) > 0 ? ziplen - END_MAXLEN : 0;
            long minPos = minHDR - (buf.length - ENDHDR);
            for (long pos = ziplen - buf.length; pos >= minPos; pos -= (buf.length - ENDHDR)) {
                int off = 0;
                if (pos < 0) {
                    // Pretend there are some NUL bytes before start of file
                    off = (int)-pos;
                    Arrays.fill(buf, 0, off, (byte)0);
                }
                int len = buf.length - off;
                if (readFullyAt(buf, off, len, pos + off) != len ) {
                    zerror("zip END header not found");
                }
                // Now scan the block backwards for END header signature
                for (int i = buf.length - ENDHDR; i >= 0; i--) {
                    if (buf[i+0] == (byte)'P'    &&
                        buf[i+1] == (byte)'K'    &&
                        buf[i+2] == (byte)'\005' &&
                        buf[i+3] == (byte)'\006') {
                        // Found ENDSIG header
                        byte[] endbuf = Arrays.copyOfRange(buf, i, i + ENDHDR);
                        end.centot = ENDTOT(endbuf);
                        end.cenlen = ENDSIZ(endbuf);
                        end.cenoff = ENDOFF(endbuf);
                        end.endpos = pos + i;
                        int comlen = ENDCOM(endbuf);
                        if (end.endpos + ENDHDR + comlen != ziplen) {
                            // ENDSIG matched, however the size of file comment in it does
                            // not match the real size. One "common" cause for this problem
                            // is some "extra" bytes are padded at the end of the zipfile.
                            // Let's do some extra verification, we don't care about the
                            // performance in this situation.
                            byte[] sbuf = new byte[4];
                            long cenpos = end.endpos - end.cenlen;
                            long locpos = cenpos - end.cenoff;
                            if  (cenpos < 0 ||
                                 locpos < 0 ||
                                 readFullyAt(sbuf, 0, sbuf.length, cenpos) != 4 ||
                                 GETSIG(sbuf) != CENSIG ||
                                 readFullyAt(sbuf, 0, sbuf.length, locpos) != 4 ||
                                 GETSIG(sbuf) != LOCSIG) {
                                continue;
                            }
                        }
                        if (comlen > 0) {    // this zip file has comlen
                            comment = new byte[comlen];
                            if (readFullyAt(comment, 0, comlen, end.endpos + ENDHDR) != comlen) {
                                zerror("zip comment read failed");
                            }
                        }
                        // must check for a zip64 end record; it is always permitted to be present
                        try {
                            byte[] loc64 = new byte[ZIP64_LOCHDR];
                            if (end.endpos < ZIP64_LOCHDR ||
                                readFullyAt(loc64, 0, loc64.length, end.endpos - ZIP64_LOCHDR)
                                != loc64.length || GETSIG(loc64) != ZIP64_LOCSIG) {
                                return end;
                            }
                            long end64pos = ZIP64_LOCOFF(loc64);
                            byte[] end64buf = new byte[ZIP64_ENDHDR];
                            if (readFullyAt(end64buf, 0, end64buf.length, end64pos)
                                != end64buf.length || GETSIG(end64buf) != ZIP64_ENDSIG) {
                                return end;
                            }
                            // end64 candidate found,
                            long cenlen64 = ZIP64_ENDSIZ(end64buf);
                            long cenoff64 = ZIP64_ENDOFF(end64buf);
                            long centot64 = ZIP64_ENDTOT(end64buf);
                            // double-check
                            if (cenlen64 != end.cenlen && end.cenlen != ZIP64_MAGICVAL ||
                                cenoff64 != end.cenoff && end.cenoff != ZIP64_MAGICVAL ||
                                centot64 != end.centot && end.centot != ZIP64_MAGICCOUNT) {
                                return end;
                            }
                            // to use the end64 values
                            end.cenlen = cenlen64;
                            end.cenoff = cenoff64;
                            end.centot = (int)centot64; // assume total < 2g
                            end.endpos = end64pos;
                        } catch (IOException x) {}    // no zip64 loc/end
                        return end;
                    }
                }
            }
            throw new ZipException("zip END header not found");
        }

        // Reads zip file central directory.
        private void initCEN(int knownTotal) throws IOException {
            // Prefer locals for better performance during startup
            byte[] cen;
            if (knownTotal == -1) {
                End end = findEND();
                if (end.endpos == 0) {
                    locpos = 0;
                    total = 0;
                    entries = new int[0];
                    this.cen = null;
                    return;         // only END header present
                }
                if (end.cenlen > end.endpos)
                    zerror("invalid END header (bad central directory size)");
                long cenpos = end.endpos - end.cenlen;     // position of CEN table
                // Get position of first local file (LOC) header, taking into
                // account that there may be a stub prefixed to the zip file.
                locpos = cenpos - end.cenoff;
                if (locpos < 0) {
                    zerror("invalid END header (bad central directory offset)");
                }
                // read in the CEN and END
                cen = this.cen = new byte[(int)(end.cenlen + ENDHDR)];
                if (readFullyAt(cen, 0, cen.length, cenpos) != end.cenlen + ENDHDR) {
                    zerror("read CEN tables failed");
                }
                this.total = end.centot;
            } else {
                cen = this.cen;
                this.total = knownTotal;
            }
            // hash table for entries
            int entriesLength = this.total * 3;
            entries = new int[entriesLength];

            int tablelen = ((total/2) | 1); // Odd -> fewer collisions
            this.tablelen = tablelen;

            int[] table = new int[tablelen];
            this.table = table;

            Arrays.fill(table, ZIP_ENDCHAIN);

            // list for all meta entries
            ArrayList<Integer> signatureNames = null;
            // Set of all version numbers seen in META-INF/versions/
            Set<Integer> metaVersionsSet = null;

            // Iterate through the entries in the central directory
            int idx = 0; // Index into the entries array
            int pos = 0;
            int entryPos = CENHDR;
            int limit = cen.length - ENDHDR;
            manifestNum = 0;
            while (entryPos <= limit) {
                if (idx >= entriesLength) {
                    // This will only happen if the zip file has an incorrect
                    // ENDTOT field, which usually means it contains more than
                    // 65535 entries.
                    initCEN(countCENHeaders(cen, limit));
                    return;
                }

                // Checks the entry and adds values to entries[idx ... idx+2]
                int nlen = checkAndAddEntry(pos, idx);
                idx += 3;

                // Adds name to metanames.
                if (isMetaName(cen, entryPos, nlen)) {
                    // nlen is at least META_INF_LENGTH
                    if (isManifestName(entryPos + META_INF_LEN, nlen - META_INF_LEN)) {
                        manifestPos = pos;
                        manifestNum++;
                    } else {
                        if (isSignatureRelated(entryPos, nlen)) {
                            if (signatureNames == null)
                                signatureNames = new ArrayList<>(4);
                            signatureNames.add(pos);
                        }

                        // If this is a versioned entry, parse the version
                        // and store it for later. This optimizes lookup
                        // performance in multi-release jar files
                        int version = getMetaVersion(entryPos + META_INF_LEN, nlen - META_INF_LEN);
                        if (version > 0) {
                            if (metaVersionsSet == null)
                                metaVersionsSet = new TreeSet<>();
                            metaVersionsSet.add(version);
                        }
                    }
                }
                // skip to the start of the next entry
                pos = nextEntryPos(pos, entryPos, nlen);
                entryPos = pos + CENHDR;
            }

            // Adjust the total entries
            this.total = idx / 3;

            if (signatureNames != null) {
                int len = signatureNames.size();
                signatureMetaNames = new int[len];
                for (int j = 0; j < len; j++) {
                    signatureMetaNames[j] = signatureNames.get(j);
                }
            }
            if (metaVersionsSet != null) {
                metaVersions = new int[metaVersionsSet.size()];
                int c = 0;
                for (Integer version : metaVersionsSet) {
                    metaVersions[c++] = version;
                }
            } else {
                metaVersions = EMPTY_META_VERSIONS;
            }
            if (pos + ENDHDR != cen.length) {
                zerror("invalid CEN header (bad header size)");
            }
        }

        private int nextEntryPos(int pos, int entryPos, int nlen) {
            return entryPos + nlen + CENCOM(cen, pos) + CENEXT(cen, pos);
        }

        private static void zerror(String msg) throws ZipException {
            throw new ZipException(msg);
        }

        /*
         * Returns the {@code pos} of the zip cen entry corresponding to the
         * specified entry name, or -1 if not found.
         */
        private int getEntryPos(String name, boolean addSlash) {
            if (total == 0) {
                return -1;
            }

            int hsh = ZipCoder.hash(name);
            int idx = table[(hsh & 0x7fffffff) % tablelen];

            // Search down the target hash chain for a entry whose
            // 32 bit hash matches the hashed name.
            while (idx != ZIP_ENDCHAIN) {
                if (getEntryHash(idx) == hsh) {
                    // The CEN name must match the specfied one
                    int pos = getEntryPos(idx);

                    try {
                        ZipCoder zc = zipCoderForPos(pos);
                        String entry = zc.toString(cen, pos + CENHDR, CENNAM(cen, pos));

                        // If addSlash is true we'll test for name+/ in addition to
                        // name, unless name is the empty string or already ends with a
                        // slash
                        int entryLen = entry.length();
                        int nameLen = name.length();
                        if ((entryLen == nameLen && entry.equals(name)) ||
                                (addSlash &&
                                nameLen + 1 == entryLen &&
                                entry.startsWith(name) &&
                                entry.charAt(entryLen - 1) == '/')) {
                            return pos;
                        }
                    } catch (IllegalArgumentException iae) {
                        // Ignore
                    }
                }
                idx = getEntryNext(idx);
            }
            return -1;
        }

        private ZipCoder zipCoderForPos(int pos) {
            if (zc.isUTF8()) {
                return zc;
            }
            if ((CENFLG(cen, pos) & USE_UTF8) != 0) {
                return ZipCoder.UTF8;
            }
            return zc;
        }

        /**
         * Returns true if the bytes represent a non-directory name
         * beginning with "META-INF/", disregarding ASCII case.
         */
        private static boolean isMetaName(byte[] name, int off, int len) {
            // Use the "oldest ASCII trick in the book":
            // ch | 0x20 == Character.toLowerCase(ch)
            return len > META_INF_LEN       // "META-INF/".length()
                && name[off + len - 1] != '/'  // non-directory
                && (name[off++] | 0x20) == 'm'
                && (name[off++] | 0x20) == 'e'
                && (name[off++] | 0x20) == 't'
                && (name[off++] | 0x20) == 'a'
                && (name[off++]       ) == '-'
                && (name[off++] | 0x20) == 'i'
                && (name[off++] | 0x20) == 'n'
                && (name[off++] | 0x20) == 'f'
                && (name[off]         ) == '/';
        }

        /*
         * Check if the bytes represents a name equals to MANIFEST.MF
         */
        private boolean isManifestName(int off, int len) {
            byte[] name = cen;
            return (len == 11 // "MANIFEST.MF".length()
                    && (name[off++] | 0x20) == 'm'
                    && (name[off++] | 0x20) == 'a'
                    && (name[off++] | 0x20) == 'n'
                    && (name[off++] | 0x20) == 'i'
                    && (name[off++] | 0x20) == 'f'
                    && (name[off++] | 0x20) == 'e'
                    && (name[off++] | 0x20) == 's'
                    && (name[off++] | 0x20) == 't'
                    && (name[off++]       ) == '.'
                    && (name[off++] | 0x20) == 'm'
                    && (name[off]   | 0x20) == 'f');
        }

        private boolean isSignatureRelated(int off, int len) {
            // Only called when isMetaName(name, off, len) is true, which means
            // len is at least META_INF_LENGTH
            // assert isMetaName(name, off, len)
            boolean signatureRelated = false;
            byte[] name = cen;
            if (name[off + len - 3] == '.') {
                // Check if entry ends with .EC and .SF
                int b1 = name[off + len - 2] | 0x20;
                int b2 = name[off + len - 1] | 0x20;
                if ((b1 == 'e' && b2 == 'c') || (b1 == 's' && b2 == 'f')) {
                    signatureRelated = true;
                }
            } else if (name[off + len - 4] == '.') {
                // Check if entry ends with .DSA and .RSA
                int b1 = name[off + len - 3] | 0x20;
                int b2 = name[off + len - 2] | 0x20;
                int b3 = name[off + len - 1] | 0x20;
                if ((b1 == 'r' || b1 == 'd') && b2 == 's' && b3 == 'a') {
                    signatureRelated = true;
                }
            }
            // Above logic must match SignatureFileVerifier.isBlockOrSF
            assert(signatureRelated == SignatureFileVerifier
                .isBlockOrSF(new String(name, off, len, UTF_8.INSTANCE)
                    .toUpperCase(Locale.ENGLISH)));
            return signatureRelated;
        }

        /*
         * If the bytes represents a non-directory name beginning
         * with "versions/", continuing with a positive integer,
         * followed by a '/', then return that integer value.
         * Otherwise, return 0
         */
        private int getMetaVersion(int off, int len) {
            byte[] name = cen;
            int nend = off + len;
            if (!(len > 10                         // "versions//".length()
                    && name[off + len - 1] != '/'  // non-directory
                    && (name[off++] | 0x20) == 'v'
                    && (name[off++] | 0x20) == 'e'
                    && (name[off++] | 0x20) == 'r'
                    && (name[off++] | 0x20) == 's'
                    && (name[off++] | 0x20) == 'i'
                    && (name[off++] | 0x20) == 'o'
                    && (name[off++] | 0x20) == 'n'
                    && (name[off++] | 0x20) == 's'
                    && (name[off++]       ) == '/')) {
                return 0;
            }
            int version = 0;
            while (off < nend) {
                final byte c = name[off++];
                if (c == '/') {
                    return version;
                }
                if (c < '0' || c > '9') {
                    return 0;
                }
                version = version * 10 + c - '0';
                // Check for overflow and leading zeros
                if (version <= 0) {
                    return 0;
                }
            }
            return 0;
        }

        /**
         * Returns the number of CEN headers in a central directory.
         * Will not throw, even if the zip file is corrupt.
         *
         * @param cen copy of the bytes in a zip file's central directory
         * @param size number of bytes in central directory
         */
        private static int countCENHeaders(byte[] cen, int size) {
            int count = 0;
            for (int p = 0;
                 p + CENHDR <= size;
                 p += CENHDR + CENNAM(cen, p) + CENEXT(cen, p) + CENCOM(cen, p))
                count++;
            return count;
        }
    }
}
