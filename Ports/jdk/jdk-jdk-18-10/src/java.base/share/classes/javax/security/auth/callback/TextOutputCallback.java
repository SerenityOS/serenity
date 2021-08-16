/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.security.auth.callback;

/**
 * <p> Underlying security services instantiate and pass a
 * {@code TextOutputCallback} to the {@code handle}
 * method of a {@code CallbackHandler} to display information messages,
 * warning messages and error messages.
 *
 * @since 1.4
 * @see javax.security.auth.callback.CallbackHandler
 */
public class TextOutputCallback implements Callback, java.io.Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 1689502495511663102L;

    /** Information message. */
    public static final int INFORMATION         = 0;
    /** Warning message. */
    public static final int WARNING             = 1;
    /** Error message. */
    public static final int ERROR               = 2;

    /**
     * @serial
     * @since 1.4
     */
    private int messageType;
    /**
     * @serial
     * @since 1.4
     */
    private String message;

    /**
     * Construct a TextOutputCallback with a message type and message
     * to be displayed.
     *
     * @param messageType the message type ({@code INFORMATION},
     *                  {@code WARNING} or {@code ERROR}).
     *
     * @param message the message to be displayed.
     *
     * @exception IllegalArgumentException if {@code messageType}
     *                  is not either {@code INFORMATION},
     *                  {@code WARNING} or {@code ERROR},
     *                  if {@code message} is null,
     *                  or if {@code message} has a length of 0.
     */
    public TextOutputCallback(int messageType, String message) {
        if ((messageType != INFORMATION &&
                messageType != WARNING && messageType != ERROR) ||
            message == null || message.isEmpty())
            throw new IllegalArgumentException();

        this.messageType = messageType;
        this.message = message;
    }

    /**
     * Get the message type.
     *
     * @return the message type ({@code INFORMATION},
     *                  {@code WARNING} or {@code ERROR}).
     */
    public int getMessageType() {
        return messageType;
    }

    /**
     * Get the message to be displayed.
     *
     * @return the message to be displayed.
     */
    public String getMessage() {
        return message;
    }
}
