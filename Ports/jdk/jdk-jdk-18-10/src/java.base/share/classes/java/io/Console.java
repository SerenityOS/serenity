/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.util.*;
import java.nio.charset.Charset;
import jdk.internal.access.JavaIOAccess;
import jdk.internal.access.SharedSecrets;
import sun.nio.cs.StreamDecoder;
import sun.nio.cs.StreamEncoder;
import sun.security.action.GetPropertyAction;

/**
 * Methods to access the character-based console device, if any, associated
 * with the current Java virtual machine.
 *
 * <p> Whether a virtual machine has a console is dependent upon the
 * underlying platform and also upon the manner in which the virtual
 * machine is invoked.  If the virtual machine is started from an
 * interactive command line without redirecting the standard input and
 * output streams then its console will exist and will typically be
 * connected to the keyboard and display from which the virtual machine
 * was launched.  If the virtual machine is started automatically, for
 * example by a background job scheduler, then it will typically not
 * have a console.
 * <p>
 * If this virtual machine has a console then it is represented by a
 * unique instance of this class which can be obtained by invoking the
 * {@link java.lang.System#console()} method.  If no console device is
 * available then an invocation of that method will return {@code null}.
 * <p>
 * Read and write operations are synchronized to guarantee the atomic
 * completion of critical operations; therefore invoking methods
 * {@link #readLine()}, {@link #readPassword()}, {@link #format format()},
 * {@link #printf printf()} as well as the read, format and write operations
 * on the objects returned by {@link #reader()} and {@link #writer()} may
 * block in multithreaded scenarios.
 * <p>
 * Invoking {@code close()} on the objects returned by the {@link #reader()}
 * and the {@link #writer()} will not close the underlying stream of those
 * objects.
 * <p>
 * The console-read methods return {@code null} when the end of the
 * console input stream is reached, for example by typing control-D on
 * Unix or control-Z on Windows.  Subsequent read operations will succeed
 * if additional characters are later entered on the console's input
 * device.
 * <p>
 * Unless otherwise specified, passing a {@code null} argument to any method
 * in this class will cause a {@link NullPointerException} to be thrown.
 * <p>
 * <b>Security note:</b>
 * If an application needs to read a password or other secure data, it should
 * use {@link #readPassword()} or {@link #readPassword(String, Object...)} and
 * manually zero the returned character array after processing to minimize the
 * lifetime of sensitive data in memory.
 *
 * <blockquote><pre>{@code
 * Console cons;
 * char[] passwd;
 * if ((cons = System.console()) != null &&
 *     (passwd = cons.readPassword("[%s]", "Password:")) != null) {
 *     ...
 *     java.util.Arrays.fill(passwd, ' ');
 * }
 * }</pre></blockquote>
 *
 * @author  Xueming Shen
 * @since   1.6
 */

public final class Console implements Flushable
{
   /**
    * Retrieves the unique {@link java.io.PrintWriter PrintWriter} object
    * associated with this console.
    *
    * @return  The printwriter associated with this console
    */
    public PrintWriter writer() {
        return pw;
    }

   /**
    * Retrieves the unique {@link java.io.Reader Reader} object associated
    * with this console.
    * <p>
    * This method is intended to be used by sophisticated applications, for
    * example, a {@link java.util.Scanner} object which utilizes the rich
    * parsing/scanning functionality provided by the {@code Scanner}:
    * <blockquote><pre>
    * Console con = System.console();
    * if (con != null) {
    *     Scanner sc = new Scanner(con.reader());
    *     ...
    * }
    * </pre></blockquote>
    * <p>
    * For simple applications requiring only line-oriented reading, use
    * {@link #readLine}.
    * <p>
    * The bulk read operations {@link java.io.Reader#read(char[]) read(char[]) },
    * {@link java.io.Reader#read(char[], int, int) read(char[], int, int) } and
    * {@link java.io.Reader#read(java.nio.CharBuffer) read(java.nio.CharBuffer)}
    * on the returned object will not read in characters beyond the line
    * bound for each invocation, even if the destination buffer has space for
    * more characters. The {@code Reader}'s {@code read} methods may block if a
    * line bound has not been entered or reached on the console's input device.
    * A line bound is considered to be any one of a line feed ({@code '\n'}),
    * a carriage return ({@code '\r'}), a carriage return followed immediately
    * by a linefeed, or an end of stream.
    *
    * @return  The reader associated with this console
    */
    public Reader reader() {
        return reader;
    }

   /**
    * Writes a formatted string to this console's output stream using
    * the specified format string and arguments.
    *
    * @param  fmt
    *         A format string as described in <a
    *         href="../util/Formatter.html#syntax">Format string syntax</a>
    *
    * @param  args
    *         Arguments referenced by the format specifiers in the format
    *         string.  If there are more arguments than format specifiers, the
    *         extra arguments are ignored.  The number of arguments is
    *         variable and may be zero.  The maximum number of arguments is
    *         limited by the maximum dimension of a Java array as defined by
    *         <cite>The Java Virtual Machine Specification</cite>.
    *         The behaviour on a
    *         {@code null} argument depends on the <a
    *         href="../util/Formatter.html#syntax">conversion</a>.
    *
    * @throws  IllegalFormatException
    *          If a format string contains an illegal syntax, a format
    *          specifier that is incompatible with the given arguments,
    *          insufficient arguments given the format string, or other
    *          illegal conditions.  For specification of all possible
    *          formatting errors, see the <a
    *          href="../util/Formatter.html#detail">Details</a> section
    *          of the formatter class specification.
    *
    * @return  This console
    */
    public Console format(String fmt, Object ...args) {
        formatter.format(fmt, args).flush();
        return this;
    }

   /**
    * A convenience method to write a formatted string to this console's
    * output stream using the specified format string and arguments.
    *
    * <p> An invocation of this method of the form
    * {@code con.printf(format, args)} behaves in exactly the same way
    * as the invocation of
    * <pre>con.format(format, args)</pre>.
    *
    * @param  format
    *         A format string as described in <a
    *         href="../util/Formatter.html#syntax">Format string syntax</a>.
    *
    * @param  args
    *         Arguments referenced by the format specifiers in the format
    *         string.  If there are more arguments than format specifiers, the
    *         extra arguments are ignored.  The number of arguments is
    *         variable and may be zero.  The maximum number of arguments is
    *         limited by the maximum dimension of a Java array as defined by
    *         <cite>The Java Virtual Machine Specification</cite>.
    *         The behaviour on a
    *         {@code null} argument depends on the <a
    *         href="../util/Formatter.html#syntax">conversion</a>.
    *
    * @throws  IllegalFormatException
    *          If a format string contains an illegal syntax, a format
    *          specifier that is incompatible with the given arguments,
    *          insufficient arguments given the format string, or other
    *          illegal conditions.  For specification of all possible
    *          formatting errors, see the <a
    *          href="../util/Formatter.html#detail">Details</a> section of the
    *          formatter class specification.
    *
    * @return  This console
    */
    public Console printf(String format, Object ... args) {
        return format(format, args);
    }

   /**
    * Provides a formatted prompt, then reads a single line of text from the
    * console.
    *
    * @param  fmt
    *         A format string as described in <a
    *         href="../util/Formatter.html#syntax">Format string syntax</a>.
    *
    * @param  args
    *         Arguments referenced by the format specifiers in the format
    *         string.  If there are more arguments than format specifiers, the
    *         extra arguments are ignored.  The maximum number of arguments is
    *         limited by the maximum dimension of a Java array as defined by
    *         <cite>The Java Virtual Machine Specification</cite>.
    *
    * @throws  IllegalFormatException
    *          If a format string contains an illegal syntax, a format
    *          specifier that is incompatible with the given arguments,
    *          insufficient arguments given the format string, or other
    *          illegal conditions.  For specification of all possible
    *          formatting errors, see the <a
    *          href="../util/Formatter.html#detail">Details</a> section
    *          of the formatter class specification.
    *
    * @throws IOError
    *         If an I/O error occurs.
    *
    * @return  A string containing the line read from the console, not
    *          including any line-termination characters, or {@code null}
    *          if an end of stream has been reached.
    */
    public String readLine(String fmt, Object ... args) {
        String line = null;
        synchronized (writeLock) {
            synchronized(readLock) {
                if (!fmt.isEmpty())
                    pw.format(fmt, args);
                try {
                    char[] ca = readline(false);
                    if (ca != null)
                        line = new String(ca);
                } catch (IOException x) {
                    throw new IOError(x);
                }
            }
        }
        return line;
    }

   /**
    * Reads a single line of text from the console.
    *
    * @throws IOError
    *         If an I/O error occurs.
    *
    * @return  A string containing the line read from the console, not
    *          including any line-termination characters, or {@code null}
    *          if an end of stream has been reached.
    */
    public String readLine() {
        return readLine("");
    }

   /**
    * Provides a formatted prompt, then reads a password or passphrase from
    * the console with echoing disabled.
    *
    * @param  fmt
    *         A format string as described in <a
    *         href="../util/Formatter.html#syntax">Format string syntax</a>
    *         for the prompt text.
    *
    * @param  args
    *         Arguments referenced by the format specifiers in the format
    *         string.  If there are more arguments than format specifiers, the
    *         extra arguments are ignored.  The maximum number of arguments is
    *         limited by the maximum dimension of a Java array as defined by
    *         <cite>The Java Virtual Machine Specification</cite>.
    *
    * @throws  IllegalFormatException
    *          If a format string contains an illegal syntax, a format
    *          specifier that is incompatible with the given arguments,
    *          insufficient arguments given the format string, or other
    *          illegal conditions.  For specification of all possible
    *          formatting errors, see the <a
    *          href="../util/Formatter.html#detail">Details</a>
    *          section of the formatter class specification.
    *
    * @throws IOError
    *         If an I/O error occurs.
    *
    * @return  A character array containing the password or passphrase read
    *          from the console, not including any line-termination characters,
    *          or {@code null} if an end of stream has been reached.
    */
    public char[] readPassword(String fmt, Object ... args) {
        char[] passwd = null;
        synchronized (writeLock) {
            synchronized(readLock) {
                installShutdownHook();
                try {
                    restoreEcho = echo(false);
                } catch (IOException x) {
                    throw new IOError(x);
                }
                IOError ioe = null;
                try {
                    if (!fmt.isEmpty())
                        pw.format(fmt, args);
                    passwd = readline(true);
                } catch (IOException x) {
                    ioe = new IOError(x);
                } finally {
                    try {
                        if (restoreEcho)
                            restoreEcho = echo(true);
                    } catch (IOException x) {
                        if (ioe == null)
                            ioe = new IOError(x);
                        else
                            ioe.addSuppressed(x);
                    }
                    if (ioe != null)
                        throw ioe;
                }
                pw.println();
            }
        }
        return passwd;
    }

    private void installShutdownHook() {
        if (shutdownHookInstalled)
            return;
        try {
            // Add a shutdown hook to restore console's echo state should
            // it be necessary.
            SharedSecrets.getJavaLangAccess()
                .registerShutdownHook(0 /* shutdown hook invocation order */,
                    false /* only register if shutdown is not in progress */,
                    new Runnable() {
                        public void run() {
                            try {
                                if (restoreEcho) {
                                    echo(true);
                                }
                            } catch (IOException x) { }
                        }
                    });
        } catch (IllegalStateException e) {
            // shutdown is already in progress and readPassword is first used
            // by a shutdown hook
        }
        shutdownHookInstalled = true;
    }

   /**
    * Reads a password or passphrase from the console with echoing disabled
    *
    * @throws IOError
    *         If an I/O error occurs.
    *
    * @return  A character array containing the password or passphrase read
    *          from the console, not including any line-termination characters,
    *          or {@code null} if an end of stream has been reached.
    */
    public char[] readPassword() {
        return readPassword("");
    }

    /**
     * Flushes the console and forces any buffered output to be written
     * immediately .
     */
    public void flush() {
        pw.flush();
    }


    /**
     * Returns the {@link java.nio.charset.Charset Charset} object used for
     * the {@code Console}.
     * <p>
     * The returned charset corresponds to the input and output source
     * (e.g., keyboard and/or display) specified by the host environment or user.
     * It may not necessarily be the same as the default charset returned from
     * {@link java.nio.charset.Charset#defaultCharset() Charset.defaultCharset()}.
     *
     * @return a {@link java.nio.charset.Charset Charset} object used for the
     *          {@code Console}
     * @since 17
     */
    public Charset charset() {
        assert CHARSET != null : "charset() should not return null";
        return CHARSET;
    }

    private Object readLock;
    private Object writeLock;
    private Reader reader;
    private Writer out;
    private PrintWriter pw;
    private Formatter formatter;
    private char[] rcb;
    private boolean restoreEcho;
    private boolean shutdownHookInstalled;
    private static native String encoding();
    /*
     * Sets the console echo status to {@code on} and returns the previous
     * console on/off status.
     * @param on    the echo status to set to. {@code true} for echo on and
     *              {@code false} for echo off
     * @return true if the previous console echo status is on
     */
    private static native boolean echo(boolean on) throws IOException;

    private char[] readline(boolean zeroOut) throws IOException {
        int len = reader.read(rcb, 0, rcb.length);
        if (len < 0)
            return null;  //EOL
        if (rcb[len-1] == '\r')
            len--;        //remove CR at end;
        else if (rcb[len-1] == '\n') {
            len--;        //remove LF at end;
            if (len > 0 && rcb[len-1] == '\r')
                len--;    //remove the CR, if there is one
        }
        char[] b = new char[len];
        if (len > 0) {
            System.arraycopy(rcb, 0, b, 0, len);
            if (zeroOut) {
                Arrays.fill(rcb, 0, len, ' ');
            }
        }
        return b;
    }

    private char[] grow() {
        assert Thread.holdsLock(readLock);
        char[] t = new char[rcb.length * 2];
        System.arraycopy(rcb, 0, t, 0, rcb.length);
        rcb = t;
        return rcb;
    }

    class LineReader extends Reader {
        private Reader in;
        private char[] cb;
        private int nChars, nextChar;
        boolean leftoverLF;
        LineReader(Reader in) {
            this.in = in;
            cb = new char[1024];
            nextChar = nChars = 0;
            leftoverLF = false;
        }
        public void close () {}
        public boolean ready() throws IOException {
            //in.ready synchronizes on readLock already
            return in.ready();
        }

        public int read(char cbuf[], int offset, int length)
            throws IOException
        {
            int off = offset;
            int end = offset + length;
            if (offset < 0 || offset > cbuf.length || length < 0 ||
                end < 0 || end > cbuf.length) {
                throw new IndexOutOfBoundsException();
            }
            synchronized(readLock) {
                boolean eof = false;
                char c = 0;
                for (;;) {
                    if (nextChar >= nChars) {   //fill
                        int n = 0;
                        do {
                            n = in.read(cb, 0, cb.length);
                        } while (n == 0);
                        if (n > 0) {
                            nChars = n;
                            nextChar = 0;
                            if (n < cb.length &&
                                cb[n-1] != '\n' && cb[n-1] != '\r') {
                                /*
                                 * we're in canonical mode so each "fill" should
                                 * come back with an eol. if there no lf or nl at
                                 * the end of returned bytes we reached an eof.
                                 */
                                eof = true;
                            }
                        } else { /*EOF*/
                            if (off - offset == 0)
                                return -1;
                            return off - offset;
                        }
                    }
                    if (leftoverLF && cbuf == rcb && cb[nextChar] == '\n') {
                        /*
                         * if invoked by our readline, skip the leftover, otherwise
                         * return the LF.
                         */
                        nextChar++;
                    }
                    leftoverLF = false;
                    while (nextChar < nChars) {
                        c = cbuf[off++] = cb[nextChar];
                        cb[nextChar++] = 0;
                        if (c == '\n') {
                            return off - offset;
                        } else if (c == '\r') {
                            if (off == end) {
                                /* no space left even the next is LF, so return
                                 * whatever we have if the invoker is not our
                                 * readLine()
                                 */
                                if (cbuf == rcb) {
                                    cbuf = grow();
                                    end = cbuf.length;
                                } else {
                                    leftoverLF = true;
                                    return off - offset;
                                }
                            }
                            if (nextChar == nChars && in.ready()) {
                                /*
                                 * we have a CR and we reached the end of
                                 * the read in buffer, fill to make sure we
                                 * don't miss a LF, if there is one, it's possible
                                 * that it got cut off during last round reading
                                 * simply because the read in buffer was full.
                                 */
                                nChars = in.read(cb, 0, cb.length);
                                nextChar = 0;
                            }
                            if (nextChar < nChars && cb[nextChar] == '\n') {
                                cbuf[off++] = '\n';
                                nextChar++;
                            }
                            return off - offset;
                        } else if (off == end) {
                           if (cbuf == rcb) {
                                cbuf = grow();
                                end = cbuf.length;
                           } else {
                               return off - offset;
                           }
                        }
                    }
                    if (eof)
                        return off - offset;
                }
            }
        }
    }

    private static final Charset CHARSET;
    static {
        String csname = encoding();
        Charset cs = null;
        if (csname == null) {
            csname = GetPropertyAction.privilegedGetProperty("sun.stdout.encoding");
        }
        if (csname != null) {
            try {
                cs = Charset.forName(csname);
            } catch (Exception ignored) { }
        }
        CHARSET = cs == null ? Charset.defaultCharset() : cs;

        // Set up JavaIOAccess in SharedSecrets
        SharedSecrets.setJavaIOAccess(new JavaIOAccess() {
            public Console console() {
                if (istty()) {
                    if (cons == null)
                        cons = new Console();
                    return cons;
                }
                return null;
            }

            public Charset charset() {
                return CHARSET;
            }
        });
    }
    private static Console cons;
    private static native boolean istty();
    private Console() {
        readLock = new Object();
        writeLock = new Object();
        out = StreamEncoder.forOutputStreamWriter(
                  new FileOutputStream(FileDescriptor.out),
                  writeLock,
                  CHARSET);
        pw = new PrintWriter(out, true) { public void close() {} };
        formatter = new Formatter(out);
        reader = new LineReader(StreamDecoder.forInputStreamReader(
                     new FileInputStream(FileDescriptor.in),
                     readLock,
                     CHARSET));
        rcb = new char[1024];
    }
}
