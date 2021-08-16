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

package java.lang;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;

/**
 * Signals that an unexpected exception has occurred in a static initializer.
 * An {@code ExceptionInInitializerError} is thrown to indicate that an
 * exception occurred during evaluation of a static initializer or the
 * initializer for a static variable.
 *
 * @author  Frank Yellin
 * @since   1.1
 */
public class ExceptionInInitializerError extends LinkageError {
    /**
     * Use serialVersionUID from JDK 1.1.X for interoperability
     */
    @java.io.Serial
    private static final long serialVersionUID = 1521711792217232256L;

    /**
     * Constructs an {@code ExceptionInInitializerError} with
     * {@code null} as its detail message string and with no saved
     * throwable object.
     * A detail message is a String that describes this particular exception.
     */
    public ExceptionInInitializerError() {
        initCause(null); // Disallow subsequent initCause
    }

    /**
     * Constructs a new {@code ExceptionInInitializerError} class by
     * saving a reference to the {@code Throwable} object thrown for
     * later retrieval by the {@link #getException()} method. The detail
     * message string is set to {@code null}.
     *
     * @param thrown The exception thrown
     */
    public ExceptionInInitializerError(Throwable thrown) {
        super(null, thrown); // Disallow subsequent initCause
    }

    /**
     * Constructs an {@code ExceptionInInitializerError} with the specified detail
     * message string.  A detail message is a String that describes this
     * particular exception. The detail message string is saved for later
     * retrieval by the {@link Throwable#getMessage()} method. There is no
     * saved throwable object.
     *
     * @param s the detail message
     */
    public ExceptionInInitializerError(String s) {
        super(s, null);  // Disallow subsequent initCause
    }

    /**
     * Returns the exception that occurred during a static initialization that
     * caused this error to be created.
     *
     * @apiNote
     * This method predates the general-purpose exception chaining facility.
     * The {@link Throwable#getCause()} method is now the preferred means of
     * obtaining this information.
     *
     * @return the saved throwable object of this
     *         {@code ExceptionInInitializerError}, or {@code null}
     *         if this {@code ExceptionInInitializerError} has no saved
     *         throwable object.
     */
    public Throwable getException() {
        return super.getCause();
    }

    /**
     * Serializable fields for ExceptionInInitializerError.
     *
     * @serialField exception Throwable the exception
     */
    @java.io.Serial
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("exception", Throwable.class)
    };

    /**
     * Reconstitutes the ExceptionInInitializerError instance from a stream
     * and initialize the cause properly when deserializing from an older
     * version.
     *
     * The getException and getCause method returns the private "exception"
     * field in the older implementation and ExceptionInInitializerError::cause
     * was set to null.
     *
     * @param  s the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        ObjectInputStream.GetField fields = s.readFields();
        Throwable exception = (Throwable) fields.get("exception", null);
        if (exception != null) {
            setCause(exception);
        }
    }

    /**
     * To maintain compatibility with older implementation, write a serial
     * "exception" field with the cause as the value.
     *
     * @param  out the {@code ObjectOutputStream} to which data is written
     * @throws IOException if an I/O error occurs
     */
    @java.io.Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        ObjectOutputStream.PutField fields = out.putFields();
        fields.put("exception", super.getCause());
        out.writeFields();
    }

}
