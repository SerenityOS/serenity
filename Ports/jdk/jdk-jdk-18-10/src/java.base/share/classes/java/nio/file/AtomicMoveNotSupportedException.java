/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Checked exception thrown when a file cannot be moved as an atomic file system
 * operation.
 *
 * @since 1.7
 */

public class AtomicMoveNotSupportedException
    extends FileSystemException
{
    @java.io.Serial
    static final long serialVersionUID = 5402760225333135579L;

    /**
     * Constructs an instance of this class.
     *
     * @param   source
     *          a string identifying the source file or {@code null} if not known
     * @param   target
     *          a string identifying the target file or {@code null} if not known
     * @param   reason
     *          a reason message with additional information or {@code null}
     */
    public AtomicMoveNotSupportedException(String source,
                                           String target,
                                           String reason)
    {
        super(source, target, reason);
    }
}
