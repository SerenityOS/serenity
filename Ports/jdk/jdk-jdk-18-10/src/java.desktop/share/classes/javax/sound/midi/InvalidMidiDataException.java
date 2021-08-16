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

package javax.sound.midi;

import java.io.Serial;

/**
 * An {@code InvalidMidiDataException} indicates that inappropriate MIDI data
 * was encountered. This often means that the data is invalid in and of itself,
 * from the perspective of the MIDI specification. An example would be an
 * undefined status byte. However, the exception might simply mean that the data
 * was invalid in the context it was used, or that the object to which the data
 * was given was unable to parse or use it. For example, a file reader might not
 * be able to parse a Type 2 MIDI file, even though that format is defined in
 * the MIDI specification.
 *
 * @author Kara Kytle
 */
public class InvalidMidiDataException extends Exception {

    /**
     * Use serialVersionUID from JDK 1.3 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 2780771756789932067L;

    /**
     * Constructs an {@code InvalidMidiDataException} with {@code null} for its
     * error detail message.
     */
    public InvalidMidiDataException() {
        super();
    }

    /**
     * Constructs an {@code InvalidMidiDataException} with the specified detail
     * message.
     *
     * @param  message the string to display as an error detail message
     */
    public InvalidMidiDataException(final String message) {
        super(message);
    }
}
