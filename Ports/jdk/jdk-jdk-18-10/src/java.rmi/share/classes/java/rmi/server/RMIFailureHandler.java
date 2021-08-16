/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.rmi.server;

/**
 * An {@code RMIFailureHandler} can be registered via the
 * {@code RMISocketFactory.setFailureHandler} call. The
 * {@code failure} method of the handler is invoked when the RMI
 * runtime is unable to create a {@code ServerSocket} to listen
 * for incoming calls. The {@code failure} method returns a boolean
 * indicating whether the runtime should attempt to re-create the
 * {@code ServerSocket}.
 *
 * @author      Ann Wollrath
 * @since       1.1
 */
public interface RMIFailureHandler {

    /**
     * The {@code failure} callback is invoked when the RMI
     * runtime is unable to create a {@code ServerSocket} via the
     * {@code RMISocketFactory}. An {@code RMIFailureHandler}
     * is registered via a call to
     * {@code RMISocketFactory.setFailureHandler}.  If no failure
     * handler is installed, the default behavior is to attempt to
     * re-create the ServerSocket.
     *
     * @param ex the exception that occurred during {@code ServerSocket}
     *           creation
     * @return if true, the RMI runtime attempts to retry
     * {@code ServerSocket} creation
     * @see java.rmi.server.RMISocketFactory#setFailureHandler(RMIFailureHandler)
     * @since 1.1
     */
    public boolean failure(Exception ex);

}
