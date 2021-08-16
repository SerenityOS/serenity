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
 * An implementor determines if requests to quit this application should proceed
 * or cancel.
 *
 * @see java.awt.Desktop#setQuitHandler(QuitHandler)
 * @see java.awt.Desktop#setQuitStrategy(QuitStrategy)
 * @since 9
 */
public interface QuitHandler {

    /**
     * Invoked when the application is asked to quit.
     * <p>
     * Implementors must call either {@link QuitResponse#cancelQuit()},
     * {@link QuitResponse#performQuit()}, or ensure the application terminates.
     * The process (or log-out) requesting this app to quit will be blocked
     * until the {@link QuitResponse} is handled. Apps that require complex UI
     * to shutdown may call the {@link QuitResponse} from any thread. Your app
     * may be asked to quit multiple times before you have responded to the
     * initial request. This handler is called each time a quit is requested,
     * and the same {@link QuitResponse} object is passed until it is handled.
     * Once used, the {@link QuitResponse} cannot be used again to change the
     * decision.
     *
     * @param  e the request to quit this application
     * @param  response the one-shot response object used to cancel or proceed
     *         with the quit action
     */
    public void handleQuitRequestWith(QuitEvent e, QuitResponse response);
}
