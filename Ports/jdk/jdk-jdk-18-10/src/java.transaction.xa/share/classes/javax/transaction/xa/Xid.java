/*
 * Copyright (c) 2000, 2002, Oracle and/or its affiliates. All rights reserved.
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

package javax.transaction.xa;

/**
 * The Xid interface is a Java mapping of the X/Open transaction identifier
 * XID structure. This interface specifies three accessor methods to
 * retrieve a global transaction format ID, global transaction ID,
 * and branch qualifier. The Xid interface is used by the transaction
 * manager and the resource managers. This interface is not visible to
 * the application programs.
 *
 * @since 1.4
 */
public interface Xid {

    /**
     * Maximum number of bytes returned by {@link #getGlobalTransactionId }.
     */
    static final int MAXGTRIDSIZE = 64;

    /**
     * Maximum number of bytes returned by {@link #getBranchQualifier }.
     */
    static final int MAXBQUALSIZE = 64;

    /**
     * Obtain the format identifier part of the XID.
     *
     * @return Format identifier. O means the OSI CCR format.
     */
    int getFormatId();

    /**
     * Obtain the global transaction identifier part of XID as an array
     * of bytes.
     *
     * @return Global transaction identifier.
     */
    byte[] getGlobalTransactionId();

    /**
     * Obtain the transaction branch identifier part of XID as an array
     * of bytes.
     *
     * @return Global transaction identifier.
     */
    byte[] getBranchQualifier();
}
