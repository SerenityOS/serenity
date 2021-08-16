/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Implementors receive notification when the user session changes.
 * <p>
 * This notification is useful for discontinuing a costly animation, or
 * indicating that the user is no longer present on a network service.
 * <p>
 * Some systems may provide a reason of the user session change.
 *
 * @see UserSessionEvent.Reason#UNSPECIFIED
 * @see UserSessionEvent.Reason#CONSOLE
 * @see UserSessionEvent.Reason#REMOTE
 * @see UserSessionEvent.Reason#LOCK
 * @since 9
 */
public interface UserSessionListener extends SystemEventListener {

    /**
     * Called when the user session has been switched away.
     *
     * @param  e the user session switch event
     */
    public void userSessionDeactivated(UserSessionEvent e);

    /**
     * Called when the user session has been switched to.
     *
     * @param  e the user session switch event
     */
    public void userSessionActivated(UserSessionEvent e);
}
