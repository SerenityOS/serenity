/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.relation;

/**
 * This class describes the various problems which can be encountered when
 * accessing a role.
 *
 * @since 1.5
 */
public class RoleStatus {

    /**
     * Do not call.
     */
    @Deprecated(since="16", forRemoval=true)
    public RoleStatus() {}

    //
    // Possible problems
    //

    /**
     * Problem type when trying to access an unknown role.
     */
    public static final int NO_ROLE_WITH_NAME = 1;
    /**
     * Problem type when trying to read a non-readable attribute.
     */
    public static final int ROLE_NOT_READABLE = 2;
    /**
     * Problem type when trying to update a non-writable attribute.
     */
    public static final int ROLE_NOT_WRITABLE = 3;
    /**
     * Problem type when trying to set a role value with less ObjectNames than
     * the minimum expected cardinality.
     */
    public static final int LESS_THAN_MIN_ROLE_DEGREE = 4;
    /**
     * Problem type when trying to set a role value with more ObjectNames than
     * the maximum expected cardinality.
     */
    public static final int MORE_THAN_MAX_ROLE_DEGREE = 5;
    /**
     * Problem type when trying to set a role value including the ObjectName of
     * a MBean not of the class expected for that role.
     */
    public static final int REF_MBEAN_OF_INCORRECT_CLASS = 6;
    /**
     * Problem type when trying to set a role value including the ObjectName of
     * a MBean not registered in the MBean Server.
     */
    public static final int REF_MBEAN_NOT_REGISTERED = 7;

    /**
     * Returns true if given value corresponds to a known role status, false
     * otherwise.
     *
     * @param status a status code.
     *
     * @return true if this value is a known role status.
     */
    public static boolean isRoleStatus(int status) {
        if (status != NO_ROLE_WITH_NAME &&
            status != ROLE_NOT_READABLE &&
            status != ROLE_NOT_WRITABLE &&
            status != LESS_THAN_MIN_ROLE_DEGREE &&
            status != MORE_THAN_MAX_ROLE_DEGREE &&
            status != REF_MBEAN_OF_INCORRECT_CLASS &&
            status != REF_MBEAN_NOT_REGISTERED) {
            return false;
        }
        return true;
    }
}
