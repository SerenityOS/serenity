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

package javax.xml.transform;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Objects;

/**
 * This class specifies an exceptional condition that occurred
 * during the transformation process.
 *
 * @since 1.4
 */
public class TransformerException extends Exception {

    private static final long serialVersionUID = 975798773772956428L;

    /** Field locator specifies where the error occurred */
    SourceLocator locator;

    /**
     * Method getLocator retrieves an instance of a SourceLocator
     * object that specifies where an error occurred.
     *
     * @return A SourceLocator object, or null if none was specified.
     */
    public SourceLocator getLocator() {
        return this.locator;
    }

    /**
     * Method setLocator sets an instance of a SourceLocator
     * object that specifies where an error occurred.
     *
     * @param location A SourceLocator object, or null to clear the location.
     */
    public void setLocator(SourceLocator location) {
        this.locator = location;
    }

    /** Field containedException specifies a wrapped exception.  May be null. */
    Throwable containedException;

    /**
     * This method retrieves an exception that this exception wraps.
     *
     * @return An Throwable object, or null.
     * @see #getCause
     */
    public Throwable getException() {
        return containedException;
    }

    /**
     * Returns the cause of this throwable or <code>null</code> if the
     * cause is nonexistent or unknown.  (The cause is the throwable that
     * caused this throwable to get thrown.)
     * @return the cause, or null if unknown
     */
    @Override
    public Throwable getCause() {

        return ((containedException == this)
                ? null
                : containedException);
    }

    /**
     * Initializes the <i>cause</i> of this throwable to the specified value.
     * (The cause is the throwable that caused this throwable to get thrown.)
     *
     * <p>This method can be called at most once.  It is generally called from
     * within the constructor, or immediately after creating the
     * throwable.  If this throwable was created
     * with {@link #TransformerException(Throwable)} or
     * {@link #TransformerException(String,Throwable)}, this method cannot be called
     * even once.
     *
     * @param  cause the cause (which is saved for later retrieval by the
     *         {@link #getCause()} method).  (A <code>null</code> value is
     *         permitted, and indicates that the cause is nonexistent or
     *         unknown.)
     * @return  a reference to this <code>Throwable</code> instance.
     * @throws IllegalArgumentException if <code>cause</code> is this
     *         throwable.  (A throwable cannot
     *         be its own cause.)
     * @throws IllegalStateException if this throwable was
     *         created with {@link #TransformerException(Throwable)} or
     *         {@link #TransformerException(String,Throwable)}, or this method has already
     *         been called on this throwable.
     */
    @Override
    public synchronized Throwable initCause(Throwable cause) {

        // TransformerException doesn't set its cause (probably
        // because it predates initCause()) - and we may not want
        // to change this since Exceptions are serializable...
        // But this also leads to the broken code in printStackTrace
        // below...

        if (this.containedException != null) {
            throw new IllegalStateException("Can't overwrite cause");
        }

        if (cause == this) {
            throw new IllegalArgumentException(
                "Self-causation not permitted");
        }

        this.containedException = cause;

        return this;
    }

    /**
     * Create a new TransformerException.
     *
     * @param message The error or warning message.
     */
    public TransformerException(String message) {
        this(message, null, null);
    }

    /**
     * Create a new TransformerException wrapping an existing exception.
     *
     * @param e The exception to be wrapped.
     */
    public TransformerException(Throwable e) {
        this(null, null, e);
    }

    /**
     * Wrap an existing exception in a TransformerException.
     *
     * <p>This is used for throwing processor exceptions before
     * the processing has started.</p>
     *
     * @param message The error or warning message, or null to
     *                use the message from the embedded exception.
     * @param e Any exception
     */
    public TransformerException(String message, Throwable e) {
        this(message, null, e);
    }

    /**
     * Create a new TransformerException from a message and a Locator.
     *
     * <p>This constructor is especially useful when an application is
     * creating its own exception from within a DocumentHandler
     * callback.</p>
     *
     * @param message The error or warning message.
     * @param locator The locator object for the error or warning.
     */
    public TransformerException(String message, SourceLocator locator) {
        this(message, locator, null);
    }

    /**
     * Wrap an existing exception in a TransformerException.
     *
     * @param message The error or warning message, or null to
     *                use the message from the embedded exception.
     * @param locator The locator object for the error or warning.
     * @param e Any exception
     */
    public TransformerException(String message, SourceLocator locator,
                                Throwable e) {
        super(((message == null) || (message.length() == 0))
              ? ((e == null) ? "" : e.toString())
              : message);

        this.containedException = e;
        this.locator            = locator;
    }

    /**
     * Get the error message with location information
     * appended.
     *
     * @return A <code>String</code> representing the error message with
     *         location information appended.
     */
    public String getMessageAndLocation() {
        StringBuilder sbuffer = new StringBuilder();
        sbuffer.append(Objects.toString(super.getMessage(), ""));
        sbuffer.append(Objects.toString(getLocationAsString(), ""));

        return sbuffer.toString();
    }

    /**
     * Get the location information as a string.
     *
     * @return A string with location info, or null
     * if there is no location information.
     */
    @SuppressWarnings("removal")
    public String getLocationAsString() {
        if (locator == null) {
            return null;
        }

        if (System.getSecurityManager() == null) {
            return getLocationString();
        } else {
            return AccessController.doPrivileged((PrivilegedAction<String>) () ->
                getLocationString(),
                new AccessControlContext(new ProtectionDomain[] {getNonPrivDomain()}));
        }
    }

    /**
     * Constructs the location string.
     * @return the location string
     */
    private String getLocationString() {
        if (locator == null) {
            return null;
        }

        StringBuilder sbuffer  = new StringBuilder();
            String       systemID = locator.getSystemId();
            int          line     = locator.getLineNumber();
            int          column   = locator.getColumnNumber();

            if (null != systemID) {
                sbuffer.append("; SystemID: ");
                sbuffer.append(systemID);
            }

            if (0 != line) {
                sbuffer.append("; Line#: ");
                sbuffer.append(line);
            }

            if (0 != column) {
                sbuffer.append("; Column#: ");
                sbuffer.append(column);
            }

            return sbuffer.toString();
    }

    /**
     * Print the the trace of methods from where the error
     * originated.  This will trace all nested exception
     * objects, as well as this object.
     */
    @Override
    public void printStackTrace() {
        printStackTrace(new java.io.PrintWriter(System.err, true));
    }

    /**
     * Print the the trace of methods from where the error
     * originated.  This will trace all nested exception
     * objects, as well as this object.
     * @param s The stream where the dump will be sent to.
     */
    @Override
    public void printStackTrace(java.io.PrintStream s) {
        printStackTrace(new java.io.PrintWriter(s));
    }

    /**
     * Print the the trace of methods from where the error
     * originated.  This will trace all nested exception
     * objects, as well as this object.
     * @param s The writer where the dump will be sent to.
     */
    @Override
    public void printStackTrace(java.io.PrintWriter s) {

        if (s == null) {
            s = new java.io.PrintWriter(System.err, true);
        }

        try {
            try {
                String locInfo = getLocationAsString();

                if (null != locInfo) {
                    s.println(locInfo);
                }

                super.printStackTrace(s);
            } catch (Throwable e) {}

            Throwable exception = getException();

            for (int i = 0; (i < 10) && (null != exception); i++) {
                s.println("---------");

                try {
                    exception.printStackTrace(s);
                    // if exception is a TransformerException it will print
                    // its contained exception, so we don't need to redo it here,
                    // and we can exit the loop now.
                    if (exception instanceof TransformerException) break;
                } catch (Throwable e) {
                    s.println("Could not print stack trace...");
                }

                try {
                    // Is this still needed?
                    Method meth = exception.getClass().getMethod("getException");

                    if (null != meth) {
                        Throwable prev = exception;

                        exception = (Throwable) meth.invoke(exception, (Object[]) null);

                        if (prev == exception) {
                            break;
                        }
                    } else {
                        exception = null;
                    }
                } catch (InvocationTargetException | IllegalAccessException
                        | NoSuchMethodException e) {
                    exception = null;
                }
            }
        } finally {
            // ensure output is written
            s.flush();
        }
    }

    /**
     * Creates a ProtectionDomain that has no permission.
     * @return a ProtectionDomain
     */
    private ProtectionDomain getNonPrivDomain() {
        CodeSource nullSource = new CodeSource(null, (CodeSigner[]) null);
        PermissionCollection noPermission = new Permissions();
        return new ProtectionDomain(nullSource, noPermission);
}
}
