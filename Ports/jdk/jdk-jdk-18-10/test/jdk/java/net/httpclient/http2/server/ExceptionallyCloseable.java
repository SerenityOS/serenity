/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.io.Closeable;
import java.io.IOException;

/**
 * Implemented by closeable objects which might be able to report
 * an error when closed due to exceptional conditions.
 */
public interface ExceptionallyCloseable extends Closeable {

    /**
     * Called when an instance of {@code ExceptionallyCloseable} is closed
     * due to some exceptional condition revealed by {@code cause}.
     *
     * @implSpec The default implementation of this method simply calls
     *           {@link #close()}. Implementation of this interface are
     *           suppose to override this method in order to ensure that
     *           the cause is properly reported.
     *
     * @param cause The reason for which the object is closed.
     * @throws IOException if {@link #close()} fails.
     */
    public default void closeExceptionally(Throwable cause) throws IOException {
        close();
    }

    public static void close(Throwable t, Closeable c) throws IOException {
        if (c instanceof ExceptionallyCloseable) {
            ((ExceptionallyCloseable)c).closeExceptionally(t);
        } else {
            c.close();
        }
    }
}
