/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 *
 * Parent of all Jemmy exceptions. Exception can be throught from inside jemmy
 * methods, if some exception occurs from code invoked from jemmy.
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class JemmyException extends RuntimeException {

    private static final long serialVersionUID = 42L;
    private Throwable innerException = null;
    private Object object = null;

    /**
     * Constructor.
     *
     * @param description An exception description.
     */
    public JemmyException(String description) {
        super(description);
    }

    /**
     * Constructor.
     *
     * @param description An exception description.
     * @param innerException Exception from code invoked from jemmy.
     */
    public JemmyException(String description, Throwable innerException) {
        this(description);
        this.innerException = innerException;
    }

    /**
     * Constructor.
     *
     * @param description An exception description.
     * @param object Object regarding which exception is thrown.
     */
    public JemmyException(String description, Object object) {
        this(description);
        this.object = object;
    }

    /**
     * Constructor.
     *
     * @param description An exception description.
     * @param innerException Exception from code invoked from jemmy.
     * @param object Object regarding which exception is thrown.
     */
    public JemmyException(String description, Throwable innerException, Object object) {
        this(description, innerException);
        this.object = object;
    }

    /**
     * Returns "object" constructor parameter.
     *
     * @return the Object value associated with the exception.
     */
    public Object getObject() {
        return object;
    }

    /**
     * Returns inner exception.
     *
     * @return An inner exception.
     * @deprecated Use getInnerThrowable()
     */
    @Deprecated
    public Exception getInnerException() {
        if (innerException instanceof Exception) {
            return (Exception) innerException;
        } else {
            return null;
        }
    }

    /**
     * Returns inner throwable.
     *
     * @return An inner throwable.
     */
    public Throwable getInnerThrowable() {
        return innerException;
    }

    /**
     * Prints stack trace into System.out.
     */
    @Override
    public void printStackTrace() {
        printStackTrace(System.out);
    }

    /**
     * Prints stack trace.
     *
     * @param ps PrintStream to print stack trace into.
     */
    @Override
    public void printStackTrace(PrintStream ps) {
        super.printStackTrace(ps);
        if (innerException != null) {
            ps.println("Inner exception:");
            innerException.printStackTrace(ps);
        }
        if (object != null) {
            ps.println("Object:");
            ps.println(object.toString());
        }
    }

    /**
     * Prints stack trace.
     *
     * @param pw PrintWriter to print stack trace into.
     *
     */
    @Override
    public void printStackTrace(PrintWriter pw) {
        super.printStackTrace(pw);
        if (innerException != null) {
            pw.println("Inner exception:");
            innerException.printStackTrace(pw);
        }
        if (object != null) {
            pw.println("Object:");
            pw.println(object.toString());
        }
    }
}
