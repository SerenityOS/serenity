/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.security.auth.module;

/**
 * This class implementation retrieves and makes available NT
 * security information for the current user.
 *
 */
public class NTSystem {

    private native void getCurrent(boolean debug);
    private native long getImpersonationToken0();

    // Warning: the next 6 fields are used by nt.c
    private String userName;
    private String domain;
    private String domainSID;
    private String userSID;
    private String[] groupIDs;
    private String primaryGroupID;

    private long   impersonationToken;

    /**
     * Instantiate an {@code NTSystem} and load
     * the native library to access the underlying system information.
     */
    public NTSystem() {
        this(false);
    }

    /**
     * Instantiate an {@code NTSystem} and load
     * the native library to access the underlying system information.
     */
    NTSystem(boolean debug) {
        loadNative();
        getCurrent(debug);
    }

    /**
     * Get the username for the current NT user.
     *
     * @return the username for the current NT user.
     */
    public String getName() {
        return userName;
    }

    /**
     * Get the domain for the current NT user.
     *
     * @return the domain for the current NT user.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Get a printable SID for the current NT user's domain.
     *
     * @return a printable SID for the current NT user's domain.
     */
    public String getDomainSID() {
        return domainSID;
    }

    /**
     * Get a printable SID for the current NT user.
     *
     * @return a printable SID for the current NT user.
     */
    public String getUserSID() {
        return userSID;
    }

    /**
     * Get a printable primary group SID for the current NT user.
     *
     * @return the primary group SID for the current NT user.
     */
    public String getPrimaryGroupID() {
        return primaryGroupID;
    }

    /**
     * Get the printable group SIDs for the current NT user.
     *
     * @return the group SIDs for the current NT user.
     */
    public String[] getGroupIDs() {
        return groupIDs == null ? null : groupIDs.clone();
    }

    /**
     * Get an impersonation token for the current NT user.
     *
     * @return an impersonation token for the current NT user.
     */
    public synchronized long getImpersonationToken() {
        if (impersonationToken == 0) {
            impersonationToken = getImpersonationToken0();
        }
        return impersonationToken;
    }


    private void loadNative() {
        System.loadLibrary("jaas");
    }
}
