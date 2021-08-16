/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.net;

import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.GroupPrincipal;
import java.util.Objects;

/**
 * Represents the credentials of a peer connected to a
 * <a href="../../../java.base/java/nio/channels/package-summary.html#unixdomain">
 * Unix domain</a> socket.
 *
 * @param user the user identity
 * @param group the group identity
 *
 * @since 16
 */
public record UnixDomainPrincipal(UserPrincipal user, GroupPrincipal group) {

    /**
     * Creates a UnixDomainPrincipal.
     *
     * @param user the user identity
     * @param group the group identity
     *
     * @throws NullPointerException if {@code user} or {@code group} are {@code null}.
     */
    public UnixDomainPrincipal {
        Objects.requireNonNull(user);
        Objects.requireNonNull(group);
    }
}
