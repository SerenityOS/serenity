/*
 * Copyright (c) 2006, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Enumeration for status of the reason that a property could not be set
 * via a call to {@code Connection.setClientInfo}
 * @since 1.6
 */

public enum ClientInfoStatus {

    /**
     * The client info property could not be set for some unknown reason
     * @since 1.6
     */
    REASON_UNKNOWN,

    /**
     * The client info property name specified was not a recognized property
     * name.
     * @since 1.6
     */
    REASON_UNKNOWN_PROPERTY,

    /**
     * The value specified for the client info property was not valid.
     * @since 1.6
     */
    REASON_VALUE_INVALID,

    /**
     * The value specified for the client info property was too large.
     * @since 1.6
     */
    REASON_VALUE_TRUNCATED
}
