/*
 * Copyright (c) 2000, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.print;

import java.net.URI;

/**
 * Interface {@code URIException} is a mixin interface which a subclass of
 * {@link PrintException PrintException} can implement to report an error
 * condition involving a {@code URI} address. The Print Service API does not
 * define any print exception classes that implement interface
 * {@code URIException}, that being left to the Print Service implementor's
 * discretion.
 */
public interface URIException {

    /**
     * Indicates that the printer cannot access the {@code URI} address. For
     * example, the printer might report this error if it goes to get the print
     * data and cannot even establish a connection to the {@code URI} address.
     */
    public static final int URIInaccessible = 1;

    /**
     * Indicates that the printer does not support the {@code URI} scheme
     * ("http", "ftp", etc.) in the {@code URI} address.
     */
    public static final int URISchemeNotSupported = 2;

    /**
     * Indicates any kind of problem not specifically identified by the other
     * reasons.
     */
    public static final int URIOtherProblem = -1;

    /**
     * Returns the {@code URI}.
     *
     * @return the {@code URI} that is the cause of this exception
     */
    public URI getUnsupportedURI();

    /**
     * Returns the reason of this exception.
     *
     * @return one of the predefined reasons enumerated in this interface
     */
    public int getReason();
}
