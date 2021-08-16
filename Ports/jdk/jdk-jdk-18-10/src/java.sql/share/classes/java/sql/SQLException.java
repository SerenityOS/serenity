/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.sql;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * <P>An exception that provides information on a database access
 * error or other errors.
 *
 * <P>Each {@code SQLException} provides several kinds of information:
 * <UL>
 *   <LI> a string describing the error.  This is used as the Java Exception
 *       message, available via the method {@code getMessage}.
 *   <LI> a "SQLstate" string, which follows either the XOPEN SQLstate conventions
 *        or the SQL:2003 conventions.
 *       The values of the SQLState string are described in the appropriate spec.
 *       The {@code DatabaseMetaData} method {@code getSQLStateType}
 *       can be used to discover whether the driver returns the XOPEN type or
 *       the SQL:2003 type.
 *   <LI> an integer error code that is specific to each vendor.  Normally this will
 *       be the actual error code returned by the underlying database.
 *   <LI> a chain to a next Exception.  This can be used to provide additional
 *       error information.
 *   <LI> the causal relationship, if any for this {@code SQLException}.
 * </UL>
 *
 * @since 1.1
 */
public class SQLException extends java.lang.Exception
                          implements Iterable<Throwable> {

    /**
     *  Constructs a {@code SQLException} object with a given
     * {@code reason}, {@code SQLState}  and
     * {@code vendorCode}.
     *
     * The {@code cause} is not initialized, and may subsequently be
     * initialized by a call to the
     * {@link Throwable#initCause(java.lang.Throwable)} method.
     *
     * @param reason a description of the exception
     * @param SQLState an XOPEN or SQL:2003 code identifying the exception
     * @param vendorCode a database vendor-specific exception code
     */
    public SQLException(String reason, String SQLState, int vendorCode) {
        super(reason);
        this.SQLState = SQLState;
        this.vendorCode = vendorCode;
        if (!(this instanceof SQLWarning)) {
            if (DriverManager.getLogWriter() != null) {
                DriverManager.println("SQLState(" + SQLState +
                                                ") vendor code(" + vendorCode + ")");
                printStackTrace(DriverManager.getLogWriter());
            }
        }
    }


    /**
     * Constructs a {@code SQLException} object with a given
     * {@code reason} and {@code SQLState}.
     *
     * The {@code cause} is not initialized, and may subsequently be
     * initialized by a call to the
     * {@link Throwable#initCause(java.lang.Throwable)} method. The vendor code
     * is initialized to 0.
     *
     * @param reason a description of the exception
     * @param SQLState an XOPEN or SQL:2003 code identifying the exception
     */
    public SQLException(String reason, String SQLState) {
        super(reason);
        this.SQLState = SQLState;
        this.vendorCode = 0;
        if (!(this instanceof SQLWarning)) {
            if (DriverManager.getLogWriter() != null) {
                printStackTrace(DriverManager.getLogWriter());
                DriverManager.println("SQLException: SQLState(" + SQLState + ")");
            }
        }
    }

    /**
     *  Constructs a {@code SQLException} object with a given
     * {@code reason}. The  {@code SQLState}  is initialized to
     * {@code null} and the vendor code is initialized to 0.
     *
     * The {@code cause} is not initialized, and may subsequently be
     * initialized by a call to the
     * {@link Throwable#initCause(java.lang.Throwable)} method.
     *
     * @param reason a description of the exception
     */
    public SQLException(String reason) {
        super(reason);
        this.SQLState = null;
        this.vendorCode = 0;
        if (!(this instanceof SQLWarning)) {
            if (DriverManager.getLogWriter() != null) {
                printStackTrace(DriverManager.getLogWriter());
            }
        }
    }

    /**
     * Constructs a {@code SQLException} object.
     * The {@code reason}, {@code SQLState} are initialized
     * to {@code null} and the vendor code is initialized to 0.
     *
     * The {@code cause} is not initialized, and may subsequently be
     * initialized by a call to the
     * {@link Throwable#initCause(java.lang.Throwable)} method.
     *
     */
    public SQLException() {
        super();
        this.SQLState = null;
        this.vendorCode = 0;
        if (!(this instanceof SQLWarning)) {
            if (DriverManager.getLogWriter() != null) {
                printStackTrace(DriverManager.getLogWriter());
            }
        }
    }

    /**
     *  Constructs a {@code SQLException} object with a given
     * {@code cause}.
     * The {@code SQLState} is initialized
     * to {@code null} and the vendor code is initialized to 0.
     * The {@code reason}  is initialized to {@code null} if
     * {@code cause==null} or to {@code cause.toString()} if
     * {@code cause!=null}.
     *
     * @param cause the underlying reason for this {@code SQLException}
     * (which is saved for later retrieval by the {@code getCause()} method);
     * may be null indicating the cause is non-existent or unknown.
     * @since 1.6
     */
    public SQLException(Throwable cause) {
        super(cause);

        if (!(this instanceof SQLWarning)) {
            if (DriverManager.getLogWriter() != null) {
                printStackTrace(DriverManager.getLogWriter());
            }
        }
    }

    /**
     * Constructs a {@code SQLException} object with a given
     * {@code reason} and  {@code cause}.
     * The {@code SQLState} is  initialized to {@code null}
     * and the vendor code is initialized to 0.
     *
     * @param reason a description of the exception.
     * @param cause the underlying reason for this {@code SQLException}
     * (which is saved for later retrieval by the {@code getCause()} method);
     * may be null indicating the cause is non-existent or unknown.
     * @since 1.6
     */
    public SQLException(String reason, Throwable cause) {
        super(reason,cause);

        if (!(this instanceof SQLWarning)) {
            if (DriverManager.getLogWriter() != null) {
                    printStackTrace(DriverManager.getLogWriter());
            }
        }
    }

    /**
     * Constructs a {@code SQLException} object with a given
     * {@code reason}, {@code SQLState} and  {@code cause}.
     * The vendor code is initialized to 0.
     *
     * @param reason a description of the exception.
     * @param sqlState an XOPEN or SQL:2003 code identifying the exception
     * @param cause the underlying reason for this {@code SQLException}
     * (which is saved for later retrieval by the
     * {@code getCause()} method); may be null indicating
     *     the cause is non-existent or unknown.
     * @since 1.6
     */
    public SQLException(String reason, String sqlState, Throwable cause) {
        super(reason,cause);

        this.SQLState = sqlState;
        this.vendorCode = 0;
        if (!(this instanceof SQLWarning)) {
            if (DriverManager.getLogWriter() != null) {
                printStackTrace(DriverManager.getLogWriter());
                DriverManager.println("SQLState(" + SQLState + ")");
            }
        }
    }

    /**
     * Constructs a {@code SQLException} object with a given
     * {@code reason}, {@code SQLState}, {@code vendorCode}
     * and  {@code cause}.
     *
     * @param reason a description of the exception
     * @param sqlState an XOPEN or SQL:2003 code identifying the exception
     * @param vendorCode a database vendor-specific exception code
     * @param cause the underlying reason for this {@code SQLException}
     * (which is saved for later retrieval by the {@code getCause()} method);
     * may be null indicating the cause is non-existent or unknown.
     * @since 1.6
     */
    public SQLException(String reason, String sqlState, int vendorCode, Throwable cause) {
        super(reason,cause);

        this.SQLState = sqlState;
        this.vendorCode = vendorCode;
        if (!(this instanceof SQLWarning)) {
            if (DriverManager.getLogWriter() != null) {
                DriverManager.println("SQLState(" + SQLState +
                                                ") vendor code(" + vendorCode + ")");
                printStackTrace(DriverManager.getLogWriter());
            }
        }
    }

    /**
     * Retrieves the SQLState for this {@code SQLException} object.
     *
     * @return the SQLState value
     */
    public String getSQLState() {
        return (SQLState);
    }

    /**
     * Retrieves the vendor-specific exception code
     * for this {@code SQLException} object.
     *
     * @return the vendor's error code
     */
    public int getErrorCode() {
        return (vendorCode);
    }

    /**
     * Retrieves the exception chained to this
     * {@code SQLException} object by setNextException(SQLException ex).
     *
     * @return the next {@code SQLException} object in the chain;
     *         {@code null} if there are none
     * @see #setNextException
     */
    public SQLException getNextException() {
        return (next);
    }

    /**
     * Adds an {@code SQLException} object to the end of the chain.
     *
     * @param ex the new exception that will be added to the end of
     *            the {@code SQLException} chain
     * @see #getNextException
     */
    public void setNextException(SQLException ex) {

        SQLException current = this;
        for(;;) {
            SQLException next=current.next;
            if (next != null) {
                current = next;
                continue;
            }

            if (nextUpdater.compareAndSet(current,null,ex)) {
                return;
            }
            current=current.next;
        }
    }

    /**
     * Returns an iterator over the chained SQLExceptions.  The iterator will
     * be used to iterate over each SQLException and its underlying cause
     * (if any).
     *
     * @return an iterator over the chained SQLExceptions and causes in the proper
     * order
     *
     * @since 1.6
     */
    public Iterator<Throwable> iterator() {

       return new Iterator<Throwable>() {

           SQLException firstException = SQLException.this;
           SQLException nextException = firstException.getNextException();
           Throwable cause = firstException.getCause();

           public boolean hasNext() {
               if(firstException != null || nextException != null || cause != null)
                   return true;
               return false;
           }

           public Throwable next() {
               Throwable throwable = null;
               if(firstException != null){
                   throwable = firstException;
                   firstException = null;
               }
               else if(cause != null){
                   throwable = cause;
                   cause = cause.getCause();
               }
               else if(nextException != null){
                   throwable = nextException;
                   cause = nextException.getCause();
                   nextException = nextException.getNextException();
               }
               else
                   throw new NoSuchElementException();
               return throwable;
           }

           public void remove() {
               throw new UnsupportedOperationException();
           }

       };

    }

    /**
         * @serial
         */
    private String SQLState;

        /**
         * @serial
         */
    private int vendorCode;

        /**
         * @serial
         */
    private volatile SQLException next;

    private static final AtomicReferenceFieldUpdater<SQLException,SQLException> nextUpdater =
            AtomicReferenceFieldUpdater.newUpdater(SQLException.class,SQLException.class,"next");

    private static final long serialVersionUID = 2135244094396331484L;
}
