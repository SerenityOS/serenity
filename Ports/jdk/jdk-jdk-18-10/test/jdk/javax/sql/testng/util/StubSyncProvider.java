/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package util;

import javax.sql.RowSetReader;
import javax.sql.RowSetWriter;
import javax.sql.rowset.spi.SyncProvider;
import javax.sql.rowset.spi.SyncProviderException;

public class StubSyncProvider extends SyncProvider {

    /**
     * The unique provider identifier.
     */
    private String providerID = "util.StubSyncProvider";

    /**
     * The vendor name of this SyncProvider implementation
     */
    private String vendorName = "Oracle Corporation";

    /**
     * The version number of this SyncProvider implementation
     */
    private String versionNumber = "1.0";

    @Override
    public String getProviderID() {
        return providerID;
    }

    @Override
    public RowSetReader getRowSetReader() {
        return null;
    }

    @Override
    public RowSetWriter getRowSetWriter() {
        return null;
    }

    @Override
    public int getProviderGrade() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDataSourceLock(int datasource_lock) throws SyncProviderException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getDataSourceLock() throws SyncProviderException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int supportsUpdatableView() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getVersion() {
        return versionNumber;
    }

    @Override
    public String getVendor() {
        return vendorName;
    }

}
