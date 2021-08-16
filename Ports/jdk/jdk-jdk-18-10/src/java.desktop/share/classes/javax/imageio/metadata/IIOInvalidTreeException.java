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

package javax.imageio.metadata;

import java.io.Serial;

import javax.imageio.IIOException;

import org.w3c.dom.Node;

/**
 * An {@code IIOInvalidTreeException} is thrown when an attempt
 * by an {@code IIOMetadata} object to parse a tree of
 * {@code IIOMetadataNode}s fails.  The node that led to the
 * parsing error may be stored.  As with any parsing error, the actual
 * error may occur at a different point that that where it is
 * detected.  The node returned by {@code getOffendingNode}
 * should merely be considered as a clue to the actual nature of the
 * problem.
 *
 * @see IIOMetadata#setFromTree
 * @see IIOMetadata#mergeTree
 * @see IIOMetadataNode
 *
 */
public class IIOInvalidTreeException extends IIOException {

    /**
     * Use serialVersionUID from JDK 9 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -1314083172544132777L;

    /**
     * The {@code Node} that led to the parsing error, or
     * {@code null}.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    protected Node offendingNode = null;

    /**
     * Constructs an {@code IIOInvalidTreeException} with a
     * message string and a reference to the {@code Node} that
     * caused the parsing error.
     *
     * @param message a {@code String} containing the reason for
     * the parsing failure.
     * @param offendingNode the DOM {@code Node} that caused the
     * exception, or {@code null}.
     */
    public IIOInvalidTreeException(String message, Node offendingNode) {
        super(message);
        this.offendingNode = offendingNode;
    }

    /**
     * Constructs an {@code IIOInvalidTreeException} with a
     * message string, a reference to an exception that caused this
     * exception, and a reference to the {@code Node} that caused
     * the parsing error.
     *
     * @param message a {@code String} containing the reason for
     * the parsing failure.
     * @param cause the {@code Throwable} ({@code Error} or
     * {@code Exception}) that caused this exception to occur,
     * or {@code null}.
     * @param offendingNode the DOM {@code Node} that caused the
     * exception, or {@code null}.
     */
    public IIOInvalidTreeException(String message, Throwable cause,
                                   Node offendingNode) {
        super(message, cause);
        this.offendingNode = offendingNode;
    }

    /**
     * Returns the {@code Node} that caused the error in parsing.
     *
     * @return the offending {@code Node}.
     */
    public Node getOffendingNode() {
        return offendingNode;
    }
}
