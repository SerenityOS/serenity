/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.xpath;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.InvalidClassException;

/**
 * {@code XPathException} represents a generic XPath exception.
 *
 * @author Norman Walsh
 * @author Jeff Suttor
 * @since 1.5
 */
public class XPathException extends Exception {
    /**
     * Serializable fields.
     */
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField( "cause", Throwable.class )
    };

    /**
     * Stream Unique Identifier.
     */
    private static final long serialVersionUID = -1837080260374986980L;

    /**
     * Constructs a new {@code XPathException}
     * with the specified detail {@code message}.
     *
     * <p>The {@code cause} is not initialized.
     *
     * <p>If {@code message} is {@code null},
     * then a {@code NullPointerException} is thrown.
     *
     * @param message The detail message.
     *
     * @throws NullPointerException When {@code message} is
     *   {@code null}.
     */
    public XPathException(String message) {
        super(message);
        if ( message == null ) {
            throw new NullPointerException ( "message can't be null");
        }
    }

    /**
     * Constructs a new {@code XPathException}
     * with the specified {@code cause}.
     *
     * <p>If {@code cause} is {@code null},
     * then a {@code NullPointerException} is thrown.
     *
     * @param cause The cause.
     *
     * @throws NullPointerException if {@code cause} is {@code null}.
     */
    public XPathException(Throwable cause) {
        super(cause);
        if ( cause == null ) {
            throw new NullPointerException ( "cause can't be null");
        }
    }

    /**
     * Get the cause of this XPathException.
     *
     * @return Cause of this XPathException.
     */
    public Throwable getCause() {
        return super.getCause();
    }

    /**
     * Writes "cause" field to the stream.
     * The cause is got from the parent class.
     *
     * @param out stream used for serialization.
     * @throws IOException thrown by {@code ObjectOutputStream}
     *
     */
    private void writeObject(ObjectOutputStream out)
            throws IOException
    {
        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("cause", super.getCause());
        out.writeFields();
    }

    /**
     * Reads the "cause" field from the stream.
     * And initializes the "cause" if it wasn't
     * done before.
     *
     * @param in stream used for deserialization
     * @throws IOException thrown by {@code ObjectInputStream}
     * @throws ClassNotFoundException  thrown by {@code ObjectInputStream}
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
    {
        ObjectInputStream.GetField fields = in.readFields();
        Throwable scause = (Throwable) fields.get("cause", null);

        if (super.getCause() == null && scause != null) {
            try {
                super.initCause(scause);
            } catch(IllegalStateException e) {
                throw new InvalidClassException("Inconsistent state: two causes");
            }
        }
    }

    /**
     * Print stack trace to specified {@code PrintStream}.
     *
     * @param s Print stack trace to this {@code PrintStream}.
     */
    public void printStackTrace(java.io.PrintStream s) {
        if (getCause() != null) {
            getCause().printStackTrace(s);
          s.println("--------------- linked to ------------------");
        }

        super.printStackTrace(s);
    }

    /**
     * Print stack trace to {@code System.err}.
     */
    public void printStackTrace() {
        printStackTrace(System.err);
    }

    /**
     * Print stack trace to specified {@code PrintWriter}.
     *
     * @param s Print stack trace to this {@code PrintWriter}.
     */
    public void printStackTrace(PrintWriter s) {

        if (getCause() != null) {
            getCause().printStackTrace(s);
          s.println("--------------- linked to ------------------");
        }

        super.printStackTrace(s);
    }
}
