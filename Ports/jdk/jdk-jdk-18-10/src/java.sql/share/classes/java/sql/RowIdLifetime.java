/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

package java.sql;

import java.util.*;

/**
 * Enumeration for RowId life-time values.
 *
 * @since 1.6
 */

public enum RowIdLifetime {

    /**
     * Indicates that this data source does not support the ROWID type.
     */
    ROWID_UNSUPPORTED,

    /**
     * Indicates that the lifetime of a RowId from this data source is indeterminate;
     * but not one of ROWID_VALID_TRANSACTION, ROWID_VALID_SESSION, or,
     * ROWID_VALID_FOREVER.
     */
    ROWID_VALID_OTHER,

    /**
     * Indicates that the lifetime of a RowId from this data source is at least the
     * containing session.
     */
    ROWID_VALID_SESSION,

    /**
     * Indicates that the lifetime of a RowId from this data source is at least the
     * containing transaction.
     */
    ROWID_VALID_TRANSACTION,

    /**
     * Indicates that the lifetime of a RowId from this data source is, effectively,
     * unlimited.
     */
    ROWID_VALID_FOREVER
}
