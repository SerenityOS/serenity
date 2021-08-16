/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.net.SocketException;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.GroupPrincipal;
import java.security.AccessController;
import java.security.PrivilegedAction;
import jdk.net.ExtendedSocketOptions.PlatformSocketOptions;
import sun.nio.fs.UnixUserPrincipals;

@SuppressWarnings("removal")
class MacOSXSocketOptions extends PlatformSocketOptions {

    public MacOSXSocketOptions() {
    }

    @Override
    boolean keepAliveOptionsSupported() {
        return keepAliveOptionsSupported0();
    }

    @Override
    void setTcpkeepAliveProbes(int fd, final int value) throws SocketException {
        setTcpkeepAliveProbes0(fd, value);
    }

    @Override
    void setTcpKeepAliveTime(int fd, final int value) throws SocketException {
        setTcpKeepAliveTime0(fd, value);
    }

    @Override
    boolean peerCredentialsSupported() {
        return true;
    }

    @Override
    void setTcpKeepAliveIntvl(int fd, final int value) throws SocketException {
        setTcpKeepAliveIntvl0(fd, value);
    }

    @Override
    int getTcpkeepAliveProbes(int fd) throws SocketException {
        return getTcpkeepAliveProbes0(fd);
    }

    @Override
    int getTcpKeepAliveTime(int fd) throws SocketException {
        return getTcpKeepAliveTime0(fd);
    }

    @Override
    int getTcpKeepAliveIntvl(int fd) throws SocketException {
        return getTcpKeepAliveIntvl0(fd);
    }

    @Override
    UnixDomainPrincipal getSoPeerCred(int fd) throws SocketException {
        long l = getSoPeerCred0(fd);
        int uid = (int)(l >> 32);
        int gid = (int)l;
        UserPrincipal user = UnixUserPrincipals.fromUid(uid);
        GroupPrincipal group = UnixUserPrincipals.fromGid(gid);
        return new UnixDomainPrincipal(user, group);
    }

    private static native void setTcpkeepAliveProbes0(int fd, int value) throws SocketException;
    private static native void setTcpKeepAliveTime0(int fd, int value) throws SocketException;
    private static native void setTcpKeepAliveIntvl0(int fd, int value) throws SocketException;
    private static native int getTcpkeepAliveProbes0(int fd) throws SocketException;
    private static native int getTcpKeepAliveTime0(int fd) throws SocketException;
    private static native int getTcpKeepAliveIntvl0(int fd) throws SocketException;
    private static native long getSoPeerCred0(int fd) throws SocketException;
    private static native boolean keepAliveOptionsSupported0();
    static {
        if (System.getSecurityManager() == null) {
            System.loadLibrary("extnet");
        } else {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                System.loadLibrary("extnet");
                return null;
            });
        }
    }
}
