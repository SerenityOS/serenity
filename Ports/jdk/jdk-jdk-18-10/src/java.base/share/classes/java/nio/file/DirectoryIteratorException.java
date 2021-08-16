/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.InvalidObjectException;

/**
 * Runtime exception thrown if an I/O error is encountered when iterating over
 * the entries in a directory. The I/O error is retrieved as an {@link
 * IOException} using the {@link #getCause() getCause()} method.
 *
 * @since 1.7
 * @see DirectoryStream
 */

public final class DirectoryIteratorException
    extends ConcurrentModificationException
{
    @java.io.Serial
    private static final long serialVersionUID = -6012699886086212874L;

    /**
     * Constructs an instance of this class.
     *
     * @param   cause
     *          the {@code IOException} that caused the directory iteration
     *          to fail
     *
     * @throws  NullPointerException
     *          if the cause is {@code null}
     */
    public DirectoryIteratorException(IOException cause) {
        super(Objects.requireNonNull(cause));
    }

    /**
     * Returns the cause of this exception.
     *
     * @return  the cause
     */
    @Override
    public IOException getCause() {
        return (IOException)super.getCause();
    }

    /**
     * Called to read the object from a stream.
     *
     * @param   s
     *          the {@code ObjectInputStream} to read
     *
     * @throws  InvalidObjectException
     *          if the object is invalid or has a cause that is not
     *          an {@code IOException}
     *
     * @throws  IOException
     *          if an I/O error occurs
     *
     * @throws  ClassNotFoundException
     *          if the class of a serialized object could not be
     *          found
     */
    @java.io.Serial
    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException
    {
        s.defaultReadObject();
        Throwable cause = super.getCause();
        if (!(cause instanceof IOException))
            throw new InvalidObjectException("Cause must be an IOException");
    }
}
