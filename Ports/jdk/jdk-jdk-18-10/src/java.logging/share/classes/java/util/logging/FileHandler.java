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

package java.util.logging;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple file logging {@code Handler}.
 * <p>
 * The {@code FileHandler} can either write to a specified file,
 * or it can write to a rotating set of files.
 * <p>
 * For a rotating set of files, as each file reaches a given size
 * limit, it is closed, rotated out, and a new file opened.
 * Successively older files are named by adding "0", "1", "2",
 * etc. into the base filename.
 * <p>
 * By default buffering is enabled in the IO libraries but each log
 * record is flushed out when it is complete.
 * <p>
 * By default the {@code XMLFormatter} class is used for formatting.
 * <p>
 * <b>Configuration:</b>
 * By default each {@code FileHandler} is initialized using the following
 * {@code LogManager} configuration properties where {@code <handler-name>}
 * refers to the fully-qualified class name of the handler.
 * If properties are not defined
 * (or have invalid values) then the specified default values are used.
 * <ul>
 * <li>   &lt;handler-name&gt;.level
 *        specifies the default level for the {@code Handler}
 *        (defaults to {@code Level.ALL}). </li>
 * <li>   &lt;handler-name&gt;.filter
 *        specifies the name of a {@code Filter} class to use
 *        (defaults to no {@code Filter}). </li>
 * <li>   &lt;handler-name&gt;.formatter
 *        specifies the name of a {@code Formatter} class to use
 *        (defaults to {@code java.util.logging.XMLFormatter}) </li>
 * <li>   &lt;handler-name&gt;.encoding
 *        the name of the character set encoding to use (defaults to
 *        the default platform encoding). </li>
 * <li>   &lt;handler-name&gt;.limit
 *        specifies an approximate maximum amount to write (in bytes)
 *        to any one file.  If this is zero, then there is no limit.
 *        (Defaults to no limit). </li>
 * <li>   &lt;handler-name&gt;.count
 *        specifies how many output files to cycle through (defaults to 1). </li>
 * <li>   &lt;handler-name&gt;.pattern
 *        specifies a pattern for generating the output file name.  See
 *        below for details. (Defaults to "%h/java%u.log"). </li>
 * <li>   &lt;handler-name&gt;.append
 *        specifies whether the FileHandler should append onto
 *        any existing files (defaults to false). </li>
 * <li>   &lt;handler-name&gt;.maxLocks
 *        specifies the maximum number of concurrent locks held by
 *        FileHandler (defaults to 100). </li>
 * </ul>
 * <p>
 * For example, the properties for {@code FileHandler} would be:
 * <ul>
 * <li>   java.util.logging.FileHandler.level=INFO </li>
 * <li>   java.util.logging.FileHandler.formatter=java.util.logging.SimpleFormatter </li>
 * </ul>
 * <p>
 * For a custom handler, e.g. com.foo.MyHandler, the properties would be:
 * <ul>
 * <li>   com.foo.MyHandler.level=INFO </li>
 * <li>   com.foo.MyHandler.formatter=java.util.logging.SimpleFormatter </li>
 * </ul>
 * <p>
 * A pattern consists of a string that includes the following special
 * components that will be replaced at runtime:
 * <ul>
 * <li>    "/"    the local pathname separator </li>
 * <li>     "%t"   the system temporary directory </li>
 * <li>     "%h"   the value of the "user.home" system property </li>
 * <li>     "%g"   the generation number to distinguish rotated logs </li>
 * <li>     "%u"   a unique number to resolve conflicts </li>
 * <li>     "%%"   translates to a single percent sign "%" </li>
 * </ul>
 * If no "%g" field has been specified and the file count is greater
 * than one, then the generation number will be added to the end of
 * the generated filename, after a dot.
 * <p>
 * Thus for example a pattern of "%t/java%g.log" with a count of 2
 * would typically cause log files to be written on Solaris to
 * /var/tmp/java0.log and /var/tmp/java1.log whereas on Windows 95 they
 * would be typically written to C:\TEMP\java0.log and C:\TEMP\java1.log
 * <p>
 * Generation numbers follow the sequence 0, 1, 2, etc.
 * <p>
 * Normally the "%u" unique field is set to 0.  However, if the {@code FileHandler}
 * tries to open the filename and finds the file is currently in use by
 * another process it will increment the unique number field and try
 * again.  This will be repeated until {@code FileHandler} finds a file name that
 * is  not currently in use. If there is a conflict and no "%u" field has
 * been specified, it will be added at the end of the filename after a dot.
 * (This will be after any automatically added generation number.)
 * <p>
 * Thus if three processes were all trying to log to fred%u.%g.txt then
 * they  might end up using fred0.0.txt, fred1.0.txt, fred2.0.txt as
 * the first file in their rotating sequences.
 * <p>
 * Note that the use of unique ids to avoid conflicts is only guaranteed
 * to work reliably when using a local disk file system.
 *
 * @since 1.4
 */

public class FileHandler extends StreamHandler {
    private MeteredStream meter;
    private boolean append;
    private long limit;       // zero => no limit.
    private int count;
    private String pattern;
    private String lockFileName;
    private FileChannel lockFileChannel;
    private File files[];
    private static final int MAX_LOCKS = 100;
    private int maxLocks = MAX_LOCKS;
    private static final Set<String> locks = new HashSet<>();

    /**
     * A metered stream is a subclass of OutputStream that
     * (a) forwards all its output to a target stream
     * (b) keeps track of how many bytes have been written
     */
    private static final class MeteredStream extends OutputStream {
        final OutputStream out;
        long written;

        MeteredStream(OutputStream out, long written) {
            this.out = out;
            this.written = written;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            written++;
        }

        @Override
        public void write(byte buff[]) throws IOException {
            out.write(buff);
            written += buff.length;
        }

        @Override
        public void write(byte buff[], int off, int len) throws IOException {
            out.write(buff,off,len);
            written += len;
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }

    private void open(File fname, boolean append) throws IOException {
        long len = 0;
        if (append) {
            len = fname.length();
        }
        FileOutputStream fout = new FileOutputStream(fname.toString(), append);
        BufferedOutputStream bout = new BufferedOutputStream(fout);
        meter = new MeteredStream(bout, len);
        setOutputStream(meter);
    }

    /**
     * Configure a FileHandler from LogManager properties and/or default values
     * as specified in the class javadoc.
     */
    private void configure() {
        LogManager manager = LogManager.getLogManager();

        String cname = getClass().getName();

        pattern = manager.getStringProperty(cname + ".pattern", "%h/java%u.log");
        limit = manager.getLongProperty(cname + ".limit", 0);
        if (limit < 0) {
            limit = 0;
        }
        count = manager.getIntProperty(cname + ".count", 1);
        if (count <= 0) {
            count = 1;
        }
        append = manager.getBooleanProperty(cname + ".append", false);
        setLevel(manager.getLevelProperty(cname + ".level", Level.ALL));
        setFilter(manager.getFilterProperty(cname + ".filter", null));
        setFormatter(manager.getFormatterProperty(cname + ".formatter", new XMLFormatter()));
        // Initialize maxLocks from the logging.properties file.
        // If invalid/no property is provided 100 will be used as a default value.
        maxLocks = manager.getIntProperty(cname + ".maxLocks", MAX_LOCKS);
        if(maxLocks <= 0) {
            maxLocks = MAX_LOCKS;
        }
        try {
            setEncoding(manager.getStringProperty(cname +".encoding", null));
        } catch (Exception ex) {
            try {
                setEncoding(null);
            } catch (Exception ex2) {
                // doing a setEncoding with null should always work.
                // assert false;
            }
        }
    }


    /**
     * Construct a default {@code FileHandler}.  This will be configured
     * entirely from {@code LogManager} properties (or their default values).
     *
     * @throws  IOException if there are IO problems opening the files.
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control"))}.
     * @throws  NullPointerException if pattern property is an empty String.
     */
    public FileHandler() throws IOException, SecurityException {
        checkPermission();
        configure();
        // pattern will have been set by configure. check that it's not
        // empty.
        if (pattern.isEmpty()) {
            throw new NullPointerException();
        }
        openFiles();
    }

    /**
     * Initialize a {@code FileHandler} to write to the given filename.
     * <p>
     * The {@code FileHandler} is configured based on {@code LogManager}
     * properties (or their default values) except that the given pattern
     * argument is used as the filename pattern, the file limit is
     * set to no limit, and the file count is set to one.
     * <p>
     * There is no limit on the amount of data that may be written,
     * so use this with care.
     *
     * @param pattern  the name of the output file
     * @throws  IOException if there are IO problems opening the files.
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     * @throws  IllegalArgumentException if pattern is an empty string
     */
    public FileHandler(String pattern) throws IOException, SecurityException {
        if (pattern.length() < 1 ) {
            throw new IllegalArgumentException();
        }
        checkPermission();
        configure();
        this.pattern = pattern;
        this.limit = 0;
        this.count = 1;
        openFiles();
    }

    /**
     * Initialize a {@code FileHandler} to write to the given filename,
     * with optional append.
     * <p>
     * The {@code FileHandler} is configured based on {@code LogManager}
     * properties (or their default values) except that the given pattern
     * argument is used as the filename pattern, the file limit is
     * set to no limit, the file count is set to one, and the append
     * mode is set to the given {@code append} argument.
     * <p>
     * There is no limit on the amount of data that may be written,
     * so use this with care.
     *
     * @param pattern  the name of the output file
     * @param append  specifies append mode
     * @throws  IOException if there are IO problems opening the files.
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     * @throws  IllegalArgumentException if pattern is an empty string
     */
    public FileHandler(String pattern, boolean append) throws IOException,
            SecurityException {
        if (pattern.length() < 1 ) {
            throw new IllegalArgumentException();
        }
        checkPermission();
        configure();
        this.pattern = pattern;
        this.limit = 0;
        this.count = 1;
        this.append = append;
        openFiles();
    }

    /**
     * Initialize a {@code FileHandler} to write to a set of files.  When
     * (approximately) the given limit has been written to one file,
     * another file will be opened.  The output will cycle through a set
     * of count files.
     * <p>
     * The {@code FileHandler} is configured based on {@code LogManager}
     * properties (or their default values) except that the given pattern
     * argument is used as the filename pattern, the file limit is
     * set to the limit argument, and the file count is set to the
     * given count argument.
     * <p>
     * The count must be at least 1.
     *
     * @param pattern  the pattern for naming the output file
     * @param limit  the maximum number of bytes to write to any one file
     * @param count  the number of files to use
     * @throws  IOException if there are IO problems opening the files.
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     * @throws  IllegalArgumentException if {@code limit < 0}, or {@code count < 1}.
     * @throws  IllegalArgumentException if pattern is an empty string
     */
    public FileHandler(String pattern, int limit, int count)
                                        throws IOException, SecurityException {
        if (limit < 0 || count < 1 || pattern.length() < 1) {
            throw new IllegalArgumentException();
        }
        checkPermission();
        configure();
        this.pattern = pattern;
        this.limit = limit;
        this.count = count;
        openFiles();
    }

    /**
     * Initialize a {@code FileHandler} to write to a set of files
     * with optional append.  When (approximately) the given limit has
     * been written to one file, another file will be opened.  The
     * output will cycle through a set of count files.
     * <p>
     * The {@code FileHandler} is configured based on {@code LogManager}
     * properties (or their default values) except that the given pattern
     * argument is used as the filename pattern, the file limit is
     * set to the limit argument, and the file count is set to the
     * given count argument, and the append mode is set to the given
     * {@code append} argument.
     * <p>
     * The count must be at least 1.
     *
     * @param pattern  the pattern for naming the output file
     * @param limit  the maximum number of bytes to write to any one file
     * @param count  the number of files to use
     * @param append  specifies append mode
     * @throws  IOException if there are IO problems opening the files.
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     * @throws  IllegalArgumentException if {@code limit < 0}, or {@code count < 1}.
     * @throws  IllegalArgumentException if pattern is an empty string
     *
     */
    public FileHandler(String pattern, int limit, int count, boolean append)
                                        throws IOException, SecurityException {
        this(pattern, (long)limit, count, append);
    }

    /**
     * Initialize a {@code FileHandler} to write to a set of files
     * with optional append.  When (approximately) the given limit has
     * been written to one file, another file will be opened.  The
     * output will cycle through a set of count files.
     * <p>
     * The {@code FileHandler} is configured based on {@code LogManager}
     * properties (or their default values) except that the given pattern
     * argument is used as the filename pattern, the file limit is
     * set to the limit argument, and the file count is set to the
     * given count argument, and the append mode is set to the given
     * {@code append} argument.
     * <p>
     * The count must be at least 1.
     *
     * @param pattern  the pattern for naming the output file
     * @param limit  the maximum number of bytes to write to any one file
     * @param count  the number of files to use
     * @param append  specifies append mode
     * @throws  IOException if there are IO problems opening the files.
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     * @throws  IllegalArgumentException if {@code limit < 0}, or {@code count < 1}.
     * @throws  IllegalArgumentException if pattern is an empty string
     *
     * @since 9
     *
     */
    public FileHandler(String pattern, long limit, int count, boolean append)
                                        throws IOException {
        if (limit < 0 || count < 1 || pattern.length() < 1) {
            throw new IllegalArgumentException();
        }
        checkPermission();
        configure();
        this.pattern = pattern;
        this.limit = limit;
        this.count = count;
        this.append = append;
        openFiles();
    }

    private  boolean isParentWritable(Path path) {
        Path parent = path.getParent();
        if (parent == null) {
            parent = path.toAbsolutePath().getParent();
        }
        return parent != null && Files.isWritable(parent);
    }

    /**
     * Open the set of output files, based on the configured
     * instance variables.
     */
    private void openFiles() throws IOException {
        LogManager manager = LogManager.getLogManager();
        manager.checkPermission();
        if (count < 1) {
           throw new IllegalArgumentException("file count = " + count);
        }
        if (limit < 0) {
            limit = 0;
        }

        // All constructors check that pattern is neither null nor empty.
        assert pattern != null : "pattern should not be null";
        assert !pattern.isEmpty() : "pattern should not be empty";

        // We register our own ErrorManager during initialization
        // so we can record exceptions.
        InitializationErrorManager em = new InitializationErrorManager();
        setErrorManager(em);

        // Create a lock file.  This grants us exclusive access
        // to our set of output files, as long as we are alive.
        int unique = -1;
        for (;;) {
            unique++;
            if (unique > maxLocks) {
                throw new IOException("Couldn't get lock for " + pattern);
            }
            // Generate a lock file name from the "unique" int.
            lockFileName = generate(pattern, 0, unique).toString() + ".lck";
            // Now try to lock that filename.
            // Because some systems (e.g., Solaris) can only do file locks
            // between processes (and not within a process), we first check
            // if we ourself already have the file locked.
            synchronized(locks) {
                if (locks.contains(lockFileName)) {
                    // We already own this lock, for a different FileHandler
                    // object.  Try again.
                    continue;
                }

                final Path lockFilePath = Paths.get(lockFileName);
                FileChannel channel = null;
                int retries = -1;
                boolean fileCreated = false;
                while (channel == null && retries++ < 1) {
                    try {
                        channel = FileChannel.open(lockFilePath,
                                CREATE_NEW, WRITE);
                        fileCreated = true;
                    } catch (AccessDeniedException ade) {
                        // This can be either a temporary, or a more permanent issue.
                        // The lock file might be still pending deletion from a previous run
                        // (temporary), or the parent directory might not be accessible,
                        // not writable, etc..
                        // If we can write to the current directory, and this is a regular file,
                        // let's try again.
                        if (Files.isRegularFile(lockFilePath, LinkOption.NOFOLLOW_LINKS)
                            && isParentWritable(lockFilePath)) {
                            // Try again. If it doesn't work, then this will
                            // eventually ensure that we increment "unique" and
                            // use another file name.
                            continue;
                        } else {
                            throw ade; // no need to retry
                        }
                    } catch (FileAlreadyExistsException ix) {
                        // This may be a zombie file left over by a previous
                        // execution. Reuse it - but only if we can actually
                        // write to its directory.
                        // Note that this is a situation that may happen,
                        // but not too frequently.
                        if (Files.isRegularFile(lockFilePath, LinkOption.NOFOLLOW_LINKS)
                            && isParentWritable(lockFilePath)) {
                            try {
                                channel = FileChannel.open(lockFilePath,
                                    WRITE, APPEND);
                            } catch (NoSuchFileException x) {
                                // Race condition - retry once, and if that
                                // fails again just try the next name in
                                // the sequence.
                                continue;
                            } catch(IOException x) {
                                // the file may not be writable for us.
                                // try the next name in the sequence
                                break;
                            }
                        } else {
                            // at this point channel should still be null.
                            // break and try the next name in the sequence.
                            break;
                        }
                    }
                }

                if (channel == null) continue; // try the next name;
                lockFileChannel = channel;

                boolean available;
                try {
                    available = lockFileChannel.tryLock() != null;
                    // We got the lock OK.
                    // At this point we could call File.deleteOnExit().
                    // However, this could have undesirable side effects
                    // as indicated by JDK-4872014. So we will instead
                    // rely on the fact that close() will remove the lock
                    // file and that whoever is creating FileHandlers should
                    // be responsible for closing them.
                } catch (IOException ix) {
                    // We got an IOException while trying to get the lock.
                    // This normally indicates that locking is not supported
                    // on the target directory.  We have to proceed without
                    // getting a lock.   Drop through, but only if we did
                    // create the file...
                    available = fileCreated;
                } catch (OverlappingFileLockException x) {
                    // someone already locked this file in this VM, through
                    // some other channel - that is - using something else
                    // than new FileHandler(...);
                    // continue searching for an available lock.
                    available = false;
                }
                if (available) {
                    // We got the lock.  Remember it.
                    locks.add(lockFileName);
                    break;
                }

                // We failed to get the lock.  Try next file.
                lockFileChannel.close();
            }
        }

        files = new File[count];
        for (int i = 0; i < count; i++) {
            files[i] = generate(pattern, i, unique);
        }

        // Create the initial log file.
        if (append) {
            open(files[0], true);
        } else {
            rotate();
        }

        // Did we detect any exceptions during initialization?
        Exception ex = em.lastException;
        if (ex != null) {
            if (ex instanceof IOException) {
                throw (IOException) ex;
            } else if (ex instanceof SecurityException) {
                throw (SecurityException) ex;
            } else {
                throw new IOException("Exception: " + ex);
            }
        }

        // Install the normal default ErrorManager.
        setErrorManager(new ErrorManager());
    }

    /**
     * Generate a file based on a user-supplied pattern, generation number,
     * and an integer uniqueness suffix
     * @param pattern the pattern for naming the output file
     * @param generation the generation number to distinguish rotated logs
     * @param unique a unique number to resolve conflicts
     * @return the generated File
     * @throws IOException
     */
    private File generate(String pattern, int generation, int unique)
            throws IOException
    {
        return generate(pattern, count, generation, unique);
    }

    // The static method here is provided for whitebox testing of the algorithm.
    static File generate(String pat, int count, int generation, int unique)
            throws IOException
    {
        Path path = Paths.get(pat);
        Path result = null;
        boolean sawg = false;
        boolean sawu = false;
        StringBuilder word = new StringBuilder();
        Path prev = null;
        for (Path elem : path) {
            if (prev != null) {
                prev = prev.resolveSibling(word.toString());
                result = result == null ? prev : result.resolve(prev);
            }
            String pattern = elem.toString();
            int ix = 0;
            word.setLength(0);
            while (ix < pattern.length()) {
                char ch = pattern.charAt(ix);
                ix++;
                char ch2 = 0;
                if (ix < pattern.length()) {
                    ch2 = Character.toLowerCase(pattern.charAt(ix));
                }
                if (ch == '%') {
                    if (ch2 == 't') {
                        String tmpDir = System.getProperty("java.io.tmpdir");
                        if (tmpDir == null) {
                            tmpDir = System.getProperty("user.home");
                        }
                        result = Paths.get(tmpDir);
                        ix++;
                        word.setLength(0);
                        continue;
                    } else if (ch2 == 'h') {
                        result = Paths.get(System.getProperty("user.home"));
                        if (jdk.internal.misc.VM.isSetUID()) {
                            // Ok, we are in a set UID program.  For safety's sake
                            // we disallow attempts to open files relative to %h.
                            throw new IOException("can't use %h in set UID program");
                        }
                        ix++;
                        word.setLength(0);
                        continue;
                    } else if (ch2 == 'g') {
                        word = word.append(generation);
                        sawg = true;
                        ix++;
                        continue;
                    } else if (ch2 == 'u') {
                        word = word.append(unique);
                        sawu = true;
                        ix++;
                        continue;
                    } else if (ch2 == '%') {
                        word = word.append('%');
                        ix++;
                        continue;
                    }
                }
                word = word.append(ch);
            }
            prev = elem;
        }

        if (count > 1 && !sawg) {
            word = word.append('.').append(generation);
        }
        if (unique > 0 && !sawu) {
            word = word.append('.').append(unique);
        }
        if (word.length() > 0) {
            String n = word.toString();
            Path p = prev == null ? Paths.get(n) : prev.resolveSibling(n);
            result = result == null ? p : result.resolve(p);
        } else if (result == null) {
            result = Paths.get("");
        }

        if (path.getRoot() == null) {
            return result.toFile();
        } else {
            return path.getRoot().resolve(result).toFile();
        }
    }

    /**
     * Rotate the set of output files
     */
    private synchronized void rotate() {
        Level oldLevel = getLevel();
        setLevel(Level.OFF);

        super.close();
        for (int i = count-2; i >= 0; i--) {
            File f1 = files[i];
            File f2 = files[i+1];
            if (f1.exists()) {
                if (f2.exists()) {
                    f2.delete();
                }
                f1.renameTo(f2);
            }
        }
        try {
            open(files[0], false);
        } catch (IOException ix) {
            // We don't want to throw an exception here, but we
            // report the exception to any registered ErrorManager.
            reportError(null, ix, ErrorManager.OPEN_FAILURE);

        }
        setLevel(oldLevel);
    }

    /**
     * Format and publish a {@code LogRecord}.
     *
     * @param  record  description of the log event. A null record is
     *                 silently ignored and is not published
     */
    @SuppressWarnings("removal")
    @Override
    public synchronized void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        super.publish(record);
        flush();
        if (limit > 0 && (meter.written >= limit || meter.written < 0)) {
            // We performed access checks in the "init" method to make sure
            // we are only initialized from trusted code.  So we assume
            // it is OK to write the target files, even if we are
            // currently being called from untrusted code.
            // So it is safe to raise privilege here.
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    rotate();
                    return null;
                }
            });
        }
    }

    /**
     * Close all the files.
     *
     * @throws  SecurityException  if a security manager exists and if
     *             the caller does not have {@code LoggingPermission("control")}.
     */
    @Override
    public synchronized void close() throws SecurityException {
        super.close();
        // Unlock any lock file.
        if (lockFileName == null) {
            return;
        }
        try {
            // Close the lock file channel (which also will free any locks)
            lockFileChannel.close();
        } catch (Exception ex) {
            // Problems closing the stream.  Punt.
        }
        synchronized(locks) {
            locks.remove(lockFileName);
        }
        new File(lockFileName).delete();
        lockFileName = null;
        lockFileChannel = null;
    }

    private static class InitializationErrorManager extends ErrorManager {
        Exception lastException;
        @Override
        public void error(String msg, Exception ex, int code) {
            lastException = ex;
        }
    }
}
