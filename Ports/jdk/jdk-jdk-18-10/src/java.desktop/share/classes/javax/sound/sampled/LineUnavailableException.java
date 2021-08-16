/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.sound.sampled;

import java.io.Serial;

/**
 * A {@code LineUnavailableException} is an exception indicating that a line
 * cannot be opened because it is unavailable. This situation arises most
 * commonly when a requested line is already in use by another application.
 *
 * @author Kara Kytle
 * @since 1.3
 */
public class LineUnavailableException extends Exception {

    /**
     * Use serialVersionUID from JDK 1.3 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -2046718279487432130L;

    /**
     * Constructs a {@code LineUnavailableException} that has {@code null} as
     * its error detail message.
     */
    public LineUnavailableException() {
        super();
    }

    /**
     * Constructs a {@code LineUnavailableException} that has the specified
     * detail message.
     *
     * @param  message a string containing the error detail message
     */
    public LineUnavailableException(final String message) {
        super(message);
    }
}
