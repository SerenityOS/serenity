/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.rowset.providers;

import com.sun.rowset.JdbcRowSetResourceBundle;
import javax.sql.*;
import java.io.*;

import javax.sql.rowset.spi.*;
import com.sun.rowset.internal.*;

/**
 * The reference implementation of a JDBC Rowset synchronization provider
 * providing optimistic synchronization with a relational datastore
 * using any JDBC technology-enabled driver.
 *
 * <h2>1.0 Backgroud</h2>
 * This synchronization provider is registered with the
 * <code>SyncFactory</code> by default as the
 * <code>com.sun.rowset.providers.RIOptimisticProvider</code>.
 * As an extension of the <code>SyncProvider</code> abstract
 * class, it provides the reader and writer classes required by disconnected
 * rowsets as <code>javax.sql.RowSetReader</code> and <code>javax.sql.RowSetWriter</code>
 * interface implementations. As a reference implementation,
 * <code>RIOptimisticProvider</code> provides a
 * fully functional implementation offering a medium grade classification of
 * syncrhonization, namely GRADE_CHECK_MODIFIED_AT_COMMIT. A
 * disconnected <code>RowSet</code> implementation using the
 * <code>RIOptimisticProvider</code> can expect the writer to
 * check only rows that have been modified in the <code>RowSet</code> against
 * the values in the data source.  If there is a conflict, that is, if a value
 * in the data source has been changed by another party, the
 * <code>RIOptimisticProvider</code> will not write any of the changes to the data
 * source and  will throw a <code>SyncProviderException</code> object.
 *
 * <h2>2.0 Usage</h2>
 * Standard disconnected <code>RowSet</code> implementations may opt to use this
 * <code>SyncProvider</code> implementation in one of two ways:
 * <OL>
 *  <LI>By specifically calling the <code>setSyncProvider</code> method
    defined in the <code>CachedRowSet</code> interface
 * <pre>
 *     CachedRowset crs = new FooCachedRowSetImpl();
 *     crs.setSyncProvider("com.sun.rowset.providers.RIOptimisticProvider");
 * </pre>
 *  <LI>By specifying it in the constructor of the <code>RowSet</code>
 *      implementation
 * <pre>
 *     CachedRowset crs = new FooCachedRowSetImpl(
 *                         "com.sun.rowset.providers.RIOptimisticProvider");
 * </pre>
 * </OL>
 * Note that because the <code>RIOptimisticProvider</code> implementation is
 * the default provider, it will always be the provider when no provider ID is
 * specified to the constructor.
 * <P>
 * See the standard <code>RowSet</code> reference implementations in the
 * <code>com.sun.rowset</code> package for more details.
 *
 * @author  Jonathan Bruce
 * @see javax.sql.rowset.spi.SyncProvider
 * @see javax.sql.rowset.spi.SyncProviderException
 * @see javax.sql.rowset.spi.SyncFactory
 * @see javax.sql.rowset.spi.SyncFactoryException
 *
 */
public final class RIOptimisticProvider extends SyncProvider implements Serializable {

    private CachedRowSetReader reader;
    private CachedRowSetWriter writer;

    /**
     * The unique provider identifier.
     */
    private String providerID = "com.sun.rowset.providers.RIOptimisticProvider";

    /**
     * The vendor name of this SyncProvider implementation
     */
    private String vendorName = "Oracle Corporation";

    /**
     * The version number of this SyncProvider implementation
     */
    private String versionNumber = "1.0";

    /**
     * ResourceBundle
     */
    private JdbcRowSetResourceBundle resBundle;

    /**
     * Creates an <code>RIOptimisticProvider</code> object initialized with the
     * fully qualified class name of this <code>SyncProvider</code> implementation
     * and a default reader and writer.
     * <P>
     * This provider is available to all disconnected <code>RowSet</code> implementations
     *  as the default persistence provider.
     */
    public RIOptimisticProvider() {
        providerID = this.getClass().getName();
        reader = new CachedRowSetReader();
        writer = new CachedRowSetWriter();
        try {
           resBundle = JdbcRowSetResourceBundle.getJdbcRowSetResourceBundle();
        } catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Returns the <code>'javax.sql.rowset.providers.RIOptimisticProvider'</code>
     * provider identification string.
     *
     * @return String Provider ID of this persistence provider
     */
    public String getProviderID() {
        return providerID;
    }

    /**
     * Returns the <code>javax.sql.RowSetWriter</code> object for this
     * <code>RIOptimisticProvider</code> object.  This is the writer that will
     * write changes made to the <code>Rowset</code> object back to the data source.
     *
     * @return the <code>javax.sql.RowSetWriter</code> object for this
     *     <code>RIOptimisticProvider</code> object
     */
    public RowSetWriter getRowSetWriter() {
        try {
            writer.setReader(reader);
        } catch (java.sql.SQLException e) {}
        return writer;
    }

    /**
     * Returns the <code>javax.sql.RowSetReader</code> object for this
     * <code>RIOptimisticProvider</code> object.  This is the reader that will
     * populate a <code>RowSet</code> object using this <code>RIOptimisticProvider</code>.
     *
     * @return the <code>javax.sql.RowSetReader</code> object for this
     *     <code>RIOptimisticProvider</code> object
     */
    public RowSetReader getRowSetReader() {
        return reader;
    }

    /**
     * Returns the <code>SyncProvider</code> grade of synchronization that
     * <code>RowSet</code> objects can expect when using this
     * implementation. As an optimisic synchonization provider, the writer
     * will only check rows that have been modified in the <code>RowSet</code>
     * object.
     */
    public int getProviderGrade() {
        return SyncProvider.GRADE_CHECK_MODIFIED_AT_COMMIT;
    }

    /**
     * Modifies the data source lock severity according to the standard
     * <code>SyncProvider</code> classifications.
     *
     * @param datasource_lock An <code>int</code> indicating the level of locking to be
     *        set; must be one of the following constants:
     * <PRE>
     *       SyncProvider.DATASOURCE_NO_LOCK,
     *       SyncProvider.DATASOURCE_ROW_LOCK,
     *       SyncProvider.DATASOURCE_TABLE_LOCK,
     *       SyncProvider.DATASOURCE_DB_LOCk
     * </PRE>
     * @throws SyncProviderException if the parameter specified is not
     *           <code>SyncProvider.DATASOURCE_NO_LOCK</code>
     */
    public void setDataSourceLock(int datasource_lock) throws SyncProviderException {
        if(datasource_lock != SyncProvider.DATASOURCE_NO_LOCK ) {
          throw new SyncProviderException(resBundle.handleGetObject("riop.locking").toString());
        }
    }

    /**
     * Returns the active data source lock severity in this
     * reference implementation of the <code>SyncProvider</code>
     * abstract class.
     *
     * @return <code>SyncProvider.DATASOURCE_NO_LOCK</code>.
     *     The reference implementation does not support data source locks.
     */
    public int getDataSourceLock() throws SyncProviderException {
        return SyncProvider.DATASOURCE_NO_LOCK;
    }

    /**
     * Returns the supported updatable view abilities of the
     * reference implementation of the <code>SyncProvider</code>
     * abstract class.
     *
     * @return <code>SyncProvider.NONUPDATABLE_VIEW_SYNC</code>. The
     *     the reference implementation does not support updating tables
     *     that are the source of a view.
     */
    public int supportsUpdatableView() {
        return SyncProvider.NONUPDATABLE_VIEW_SYNC;
    }

    /**
     * Returns the release version ID of the Reference Implementation Optimistic
     * Synchronization Provider.
     *
     * @return the <code>String</code> detailing the version number of this SyncProvider
     */
    public String getVersion() {
        return this.versionNumber;
    }

    /**
     * Returns the vendor name of the Reference Implementation Optimistic
     * Synchronization Provider
     *
     * @return the <code>String</code> detailing the vendor name of this
     *      SyncProvider
     */
    public String getVendor() {
        return this.vendorName;
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Default state initialization happens here
        ois.defaultReadObject();
        // Initialization of transient Res Bundle happens here .
        try {
           resBundle = JdbcRowSetResourceBundle.getJdbcRowSetResourceBundle();
        } catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }
    static final long serialVersionUID =-3143367176751761936L;

}
