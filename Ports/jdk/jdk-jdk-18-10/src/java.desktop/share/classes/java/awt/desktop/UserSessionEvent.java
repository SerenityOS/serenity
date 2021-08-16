/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.desktop;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.Serial;

/**
 * Event sent when the user session has been changed. Some systems may provide a
 * reason of a user session change.
 *
 * @see UserSessionListener#userSessionActivated(UserSessionEvent)
 * @see UserSessionListener#userSessionDeactivated(UserSessionEvent)
 * @since 9
 */
public final class UserSessionEvent extends AppEvent {

    /**
     * Use serialVersionUID from JDK 9 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 6747138462796569055L;

    /**
     * The reason of the user session change.
     */
    private final Reason reason;

    /**
     * Kinds of available reasons of user session change.
     */
    public static enum Reason {
        /**
         * The system does not provide a reason for a session change.
         */
        UNSPECIFIED,

        /**
         * The session was connected/disconnected to the console terminal.
         */
        CONSOLE,

        /**
         * The session was connected/disconnected to the remote terminal.
         */
        REMOTE,

        /**
         * The session has been locked/unlocked.
         */
        LOCK
    }

    /**
     * Constructs a {@code UserSessionEvent}.
     *
     * @param  reason the reason of the user session change
     * @throws HeadlessException if {@link GraphicsEnvironment#isHeadless()}
     *         returns {@code true}
     * @throws UnsupportedOperationException if Desktop API is not supported on
     *         the current platform
     * @see Desktop#isDesktopSupported()
     * @see java.awt.GraphicsEnvironment#isHeadless
     */
    public UserSessionEvent(Reason reason) {
        this.reason = reason;
    }

    /**
     * Gets a reason of the user session change.
     *
     * @return reason a reason
     * @see Reason#UNSPECIFIED
     * @see Reason#CONSOLE
     * @see Reason#REMOTE
     * @see Reason#LOCK
     */
    public Reason getReason() {
        return reason;
    }
}
