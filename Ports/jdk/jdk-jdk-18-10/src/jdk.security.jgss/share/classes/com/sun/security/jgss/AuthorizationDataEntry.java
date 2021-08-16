/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.security.jgss;

/**
 * Kerberos 5 AuthorizationData entry.
 */
public final class AuthorizationDataEntry {

    private final int type;
    private final byte[] data;

    /**
     * Create an AuthorizationDataEntry object.
     * @param type the ad-type
     * @param data the ad-data, a copy of the data will be saved
     * inside the object.
     */
    public AuthorizationDataEntry(int type, byte[] data) {
        this.type = type;
        this.data = data.clone();
    }

    /**
     * Get the ad-type field.
     * @return ad-type
     */
    public int getType() {
        return type;
    }

    /**
     * Get a copy of the ad-data field.
     * @return ad-data
     */
    public byte[] getData() {
        return data.clone();
    }

    public String toString() {
        return "AuthorizationDataEntry: type="+type+", data=" +
                data.length + " bytes:\n" +
                new sun.security.util.HexDumpEncoder().encodeBuffer(data);
    }
}
