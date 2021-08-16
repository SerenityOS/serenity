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

package java.awt;

import java.io.Serial;

/**
 * Thrown when code that is dependent on a keyboard, display, or mouse
 * is called in an environment that does not support a keyboard, display,
 * or mouse. Any code that depends on any of those devices should firstly
 * ensure their availability using the {@code GraphicsEnvironment.isHeadless()}
 * method and throw {@code HeadlessException} if the latter returns
 * {@code true}.
 *
 * @since 1.4
 * @author  Michael Martak
 * @see GraphicsEnvironment#isHeadless
 */
public class HeadlessException extends UnsupportedOperationException {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 167183644944358563L;

    /**
     * Constructs new {@code HeadlessException} with empty message.
     * For such {@code HeadlessException} the default headless error message
     * may be auto-generated for some platforms.
     * The text of the default headless message may depend on
     * whether the GraphicsEnvironment is in fact headless.
     * That is, the default headless message is both system and environmentally
     * dependent.
     */
    public HeadlessException() {}

    /**
     * Create a new instance with the specified detailed error message.
     * For some platforms the default headless error message may be
     * added at the end of the specified message.
     * The text of the default headless message may depend on
     * whether the GraphicsEnvironment is in fact headless.
     * That is, the default headless message is both system and environmentally
     * dependent.
     *
     * @param  msg the error message
     */
    public HeadlessException(String msg) {
        super(msg);
    }

    /**
     * Returns the detail message string of this {@code HeadlessException}.
     * Depending on the platform the message specified in the constructor may
     * be followed by the default headless error message.
     * The text of the default headless message may depend on
     * whether the GraphicsEnvironment is in fact headless.
     * That is, the default headless message is both system and environmentally
     * dependent.
     *
     * @return  the detail message string of this {@code HeadlessException}
     *          instance (which may be {@code null}).
     */
    public String getMessage() {
        String superMessage = super.getMessage();
        String headlessMessage = GraphicsEnvironment.getHeadlessMessage();

        if (superMessage == null) {
            return headlessMessage;
        } else if (headlessMessage == null) {
            return superMessage;
        } else {
            return superMessage + headlessMessage;
        }
    }
}
