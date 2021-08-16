/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

/**
 * A socket option associated with a socket.
 *
 * <p> In the {@link java.nio.channels channels} package, the {@link
 * java.nio.channels.NetworkChannel} interface defines the {@link
 * java.nio.channels.NetworkChannel#setOption(SocketOption,Object) setOption}
 * and {@link java.nio.channels.NetworkChannel#getOption(SocketOption) getOption}
 * methods to set and query the channel's socket options.
 *
 * @param   <T>     The type of the socket option value.
 *
 * @since 1.7
 *
 * @see StandardSocketOptions
 */

public interface SocketOption<T> {

    /**
     * Returns the name of the socket option.
     *
     * @return the name of the socket option
     */
    String name();

    /**
     * Returns the type of the socket option value.
     *
     * @return the type of the socket option value
     */
    Class<T> type();
}
