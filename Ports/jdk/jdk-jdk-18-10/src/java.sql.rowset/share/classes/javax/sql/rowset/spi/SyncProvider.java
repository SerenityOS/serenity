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

package javax.sql.rowset.spi;

import javax.sql.*;

/**
 * The synchronization mechanism that provides reader/writer capabilities for
 * disconnected <code>RowSet</code> objects.
 * A <code>SyncProvider</code> implementation is a class that extends the
 * <code>SyncProvider</code> abstract class.
 * <P>
 * A <code>SyncProvider</code> implementation is
 * identified by a unique ID, which is its fully qualified class name.
 * This name must be registered with the
 * <code>SyncFactory</code> SPI, thus making the implementation available to
 * all <code>RowSet</code> implementations.
 * The factory mechanism in the reference implementation uses this name to instantiate
 * the implementation, which can then provide a <code>RowSet</code> object with its
 * reader (a <code>javax.sql.RowSetReader</code> object) and its writer (a
 * <code>javax.sql.RowSetWriter</code> object).
 * <P>
 * The Jdbc <code>RowSet</code> Implementations specification provides two
 * reference implementations of the <code>SyncProvider</code> abstract class:
 * <code>RIOptimisticProvider</code> and <code>RIXMLProvider</code>.
 * The <code>RIOptimisticProvider</code> can set any <code>RowSet</code>
 * implementation with a <code>RowSetReader</code> object and a
 * <code>RowSetWriter</code> object.  However, only the <code>RIXMLProvider</code>
 * implementation can set an <code>XmlReader</code> object and an
 * <code>XmlWriter</code> object. A <code>WebRowSet</code> object uses the
 * <code>XmlReader</code> object to read data in XML format to populate itself with that
 * data.  It uses the <code>XmlWriter</code> object to write itself to a stream or
 * <code>java.io.Writer</code> object in XML format.
 *
 * <h2>1.0 Naming Convention for Implementations</h2>
 * As a guide  to naming <code>SyncProvider</code>
 * implementations, the following should be noted:
 * <UL>
 * <li>The name for a <code>SyncProvider</code> implementation
 * is its fully qualified class name.
 * <li>It is recommended that vendors supply a
 * <code>SyncProvider</code> implementation in a package named <code>providers</code>.
 * </UL>
 * <p>
 * For instance, if a vendor named Fred, Inc. offered a
 * <code>SyncProvider</code> implementation, you could have the following:
 * <PRE>
 *     Vendor name:  Fred, Inc.
 *     Domain name of vendor:  com.fred
 *     Package name:  com.fred.providers
 *     SyncProvider implementation class name:  HighAvailabilityProvider
 *
 *     Fully qualified class name of SyncProvider implementation:
 *                        com.fred.providers.HighAvailabilityProvider
 * </PRE>
 * <P>
 * The following line of code uses the fully qualified name to register
 * this implementation with the <code>SyncFactory</code> static instance.
 * <PRE>
 *     SyncFactory.registerProvider(
 *                          "com.fred.providers.HighAvailabilityProvider");
 * </PRE>
 * <P>
 * The default <code>SyncProvider</code> object provided with the reference
 * implementation uses the following name:
 * <pre>
 *     com.sun.rowset.providers.RIOptimisticProvider
 * </pre>
 * <p>
 * Vendors should refer to the reference implementation synchronization
 * providers for additional guidance on how to implement a new
 * <code>SyncProvider</code> implementation.
 *
 * <h2>2.0 How a <code>RowSet</code> Object Gets Its Provider</h2>
 *
 * A disconnected <code>Rowset</code> object may get access to a
 * <code>SyncProvider</code> object in one of the following two ways:
 * <UL>
 *  <LI>Using a constructor<BR>
 *      <PRE>
 *       CachedRowSet crs = new CachedRowSet(
 *                  "com.fred.providers.HighAvailabilitySyncProvider");
 *      </PRE>
 *  <LI>Using the <code>setSyncProvider</code> method
 *      <PRE>
 *       CachedRowSet crs = new CachedRowSet();
 *       crs.setSyncProvider("com.fred.providers.HighAvailabilitySyncProvider");
 *      </PRE>

 * </UL>
 * <p>
 * By default, the reference implementations of the <code>RowSet</code> synchronization
 * providers are always available to the Java platform.
 * If no other pluggable synchronization providers have been correctly
 * registered, the <code>SyncFactory</code> will automatically generate
 * an instance of the default <code>SyncProvider</code> reference implementation.
 * Thus, in the preceding code fragment, if no implementation named
 * <code>com.fred.providers.HighAvailabilitySyncProvider</code> has been
 * registered with the <code>SyncFactory</code> instance, <i>crs</i> will be
 * assigned the default provider in the reference implementation, which is
 * <code>com.sun.rowset.providers.RIOptimisticProvider</code>.
 *
 * <h2>3.0 Violations and Synchronization Issues</h2>
 * If an update between a disconnected <code>RowSet</code> object
 * and a data source violates
 * the original query or the underlying data source constraints, this will
 * result in undefined behavior for all disconnected <code>RowSet</code> implementations
 * and their designated <code>SyncProvider</code> implementations.
 * Not defining the behavior when such violations occur offers greater flexibility
 * for a <code>SyncProvider</code>
 * implementation to determine its own best course of action.
 * <p>
 * A <code>SyncProvider</code> implementation
 * may choose to implement a specific handler to
 * handle a subset of query violations.
 * However if an original query violation or a more general data source constraint
 * violation is not handled by the <code>SyncProvider</code> implementation,
 * all <code>SyncProvider</code>
 * objects must throw a <code>SyncProviderException</code>.
 *
 * <h2>4.0 Updatable SQL VIEWs</h2>
 * It is possible for any disconnected or connected <code>RowSet</code> object to be populated
 * from an SQL query that is formulated originally from an SQL <code>VIEW</code>.
 * While in many cases it is possible for an update to be performed to an
 * underlying view, such an update requires additional metadata, which may vary.
 * The <code>SyncProvider</code> class provides two constants to indicate whether
 * an implementation supports updating an SQL <code>VIEW</code>.
 * <ul>
 * <li><code><b>NONUPDATABLE_VIEW_SYNC</b></code> - Indicates that a <code>SyncProvider</code>
 * implementation does not support synchronization with an SQL <code>VIEW</code> as the
 * underlying source of data for the <code>RowSet</code> object.
 * <li><code><b>UPDATABLE_VIEW_SYNC</b></code> - Indicates that a
 * <code>SyncProvider</code> implementation
 * supports synchronization with an SQL <code>VIEW</code> as the underlying source
 * of data.
 * </ul>
 * <P>
 * The default is for a <code>RowSet</code> object not to be updatable if it was
 * populated with data from an SQL <code>VIEW</code>.
 *
 * <h2>5.0 <code>SyncProvider</code> Constants</h2>
 * The <code>SyncProvider</code> class provides three sets of constants that
 * are used as return values or parameters for <code>SyncProvider</code> methods.
 * <code>SyncProvider</code> objects may be implemented to perform synchronization
 * between a <code>RowSet</code> object and its underlying data source with varying
 * degrees of care. The first group of constants indicate how synchronization
 * is handled. For example, <code>GRADE_NONE</code> indicates that a
 * <code>SyncProvider</code> object will not take any care to see what data is
 * valid and will simply write the <code>RowSet</code> data to the data source.
 * <code>GRADE_MODIFIED_AT_COMMIT</code> indicates that the provider will check
 * only modified data for validity.  Other grades check all data for validity
 * or set locks when data is modified or loaded.
 * <OL>
 *  <LI>Constants to indicate the synchronization grade of a
 *     <code>SyncProvider</code> object
 *   <UL>
 *    <LI>SyncProvider.GRADE_NONE
 *    <LI>SyncProvider.GRADE_MODIFIED_AT_COMMIT
 *    <LI>SyncProvider.GRADE_CHECK_ALL_AT_COMMIT
 *    <LI>SyncProvider.GRADE_LOCK_WHEN_MODIFIED
 *    <LI>SyncProvider.GRADE_LOCK_WHEN_LOADED
 *   </UL>
 *  <LI>Constants to indicate what locks are set on the data source
 *   <UL>
 *     <LI>SyncProvider.DATASOURCE_NO_LOCK
 *     <LI>SyncProvider.DATASOURCE_ROW_LOCK
 *     <LI>SyncProvider.DATASOURCE_TABLE_LOCK
 *     <LI>SyncProvider.DATASOURCE_DB_LOCK
 *   </UL>
 *  <LI>Constants to indicate whether a <code>SyncProvider</code> object can
 *       perform updates to an SQL <code>VIEW</code> <BR>
 *       These constants are explained in the preceding section (4.0).
 *   <UL>
 *     <LI>SyncProvider.UPDATABLE_VIEW_SYNC
 *     <LI>SyncProvider.NONUPDATABLE_VIEW_SYNC
 *   </UL>
 * </OL>
 *
 * @author Jonathan Bruce
 * @see javax.sql.rowset.spi.SyncFactory
 * @see javax.sql.rowset.spi.SyncFactoryException
 * @since 1.5
 */
public abstract class SyncProvider {

   /**
    * Creates a default <code>SyncProvider</code> object.
    */
    public SyncProvider() {
    }

    /**
     * Returns the unique identifier for this <code>SyncProvider</code> object.
     *
     * @return a <code>String</code> object with the fully qualified class name of
     *         this <code>SyncProvider</code> object
     */
    public abstract String getProviderID();

    /**
     * Returns a <code>javax.sql.RowSetReader</code> object, which can be used to
     * populate a <code>RowSet</code> object with data.
     *
     * @return a <code>javax.sql.RowSetReader</code> object
     */
    public abstract RowSetReader getRowSetReader();

    /**
     * Returns a <code>javax.sql.RowSetWriter</code> object, which can be
     * used to write a <code>RowSet</code> object's data back to the
     * underlying data source.
     *
     * @return a <code>javax.sql.RowSetWriter</code> object
     */
    public abstract RowSetWriter getRowSetWriter();

    /**
     * Returns a constant indicating the
     * grade of synchronization a <code>RowSet</code> object can expect from
     * this <code>SyncProvider</code> object.
     *
     * @return an int that is one of the following constants:
     *           SyncProvider.GRADE_NONE,
     *           SyncProvider.GRADE_CHECK_MODIFIED_AT_COMMIT,
     *           SyncProvider.GRADE_CHECK_ALL_AT_COMMIT,
     *           SyncProvider.GRADE_LOCK_WHEN_MODIFIED,
     *           SyncProvider.GRADE_LOCK_WHEN_LOADED
     */
    public abstract int getProviderGrade();


    /**
     * Sets a lock on the underlying data source at the level indicated by
     * <i>datasource_lock</i>. This should cause the
     * <code>SyncProvider</code> to adjust its behavior by increasing or
     * decreasing the level of optimism it provides for a successful
     * synchronization.
     *
     * @param datasource_lock one of the following constants indicating the severity
     *           level of data source lock required:
     * <pre>
     *           SyncProvider.DATASOURCE_NO_LOCK,
     *           SyncProvider.DATASOURCE_ROW_LOCK,
     *           SyncProvider.DATASOURCE_TABLE_LOCK,
     *           SyncProvider.DATASOURCE_DB_LOCK,
     * </pre>
     * @throws SyncProviderException if an unsupported data source locking level
     *           is set.
     * @see #getDataSourceLock
     */
    public abstract void setDataSourceLock(int datasource_lock)
        throws SyncProviderException;

    /**
     * Returns the current data source lock severity level active in this
     * <code>SyncProvider</code> implementation.
     *
     * @return a constant indicating the current level of data source lock
     *        active in this <code>SyncProvider</code> object;
     *         one of the following:
     * <pre>
     *           SyncProvider.DATASOURCE_NO_LOCK,
     *           SyncProvider.DATASOURCE_ROW_LOCK,
     *           SyncProvider.DATASOURCE_TABLE_LOCK,
     *           SyncProvider.DATASOURCE_DB_LOCK
     * </pre>
     * @throws SyncProviderException if an error occurs determining the data
     *        source locking level.
     * @see #setDataSourceLock

     */
    public abstract int getDataSourceLock()
        throws SyncProviderException;

    /**
     * Returns whether this <code>SyncProvider</code> implementation
     * can perform synchronization between a <code>RowSet</code> object
     * and the SQL <code>VIEW</code> in the data source from which
     * the <code>RowSet</code> object got its data.
     *
     * @return an <code>int</code> saying whether this <code>SyncProvider</code>
     *         object supports updating an SQL <code>VIEW</code>; one of the
     *         following:
     *            SyncProvider.UPDATABLE_VIEW_SYNC,
     *            SyncProvider.NONUPDATABLE_VIEW_SYNC
     */
    public abstract int supportsUpdatableView();

    /**
     * Returns the release version of this <code>SyncProvider</code> instance.
     *
     * @return a <code>String</code> detailing the release version of the
     *     <code>SyncProvider</code> implementation
     */
    public abstract String getVersion();

    /**
     * Returns the vendor name of this <code>SyncProvider</code> instance
     *
     * @return a <code>String</code> detailing the vendor name of this
     *     <code>SyncProvider</code> implementation
     */
    public abstract String getVendor();

    /*
     * Standard description of synchronization grades that a SyncProvider
     * could provide.
     */

    /**
     * Indicates that no synchronization with the originating data source is
     * provided. A <code>SyncProvider</code>
     * implementation returning this grade will simply attempt to write
     * updates in the <code>RowSet</code> object to the underlying data
     * source without checking the validity of any data.
     *
     */
    public static final int GRADE_NONE = 1;

    /**
     * Indicates a low level optimistic synchronization grade with
     * respect to the originating data source.
     *
     * A <code>SyncProvider</code> implementation
     * returning this grade will check only rows that have changed.
     *
     */
    public static final int GRADE_CHECK_MODIFIED_AT_COMMIT = 2;

    /**
     * Indicates a high level optimistic synchronization grade with
     * respect to the originating data source.
     *
     * A <code>SyncProvider</code> implementation
     * returning this grade will check all rows, including rows that have not
     * changed.
     */
    public static final int GRADE_CHECK_ALL_AT_COMMIT = 3;

    /**
     * Indicates a pessimistic synchronization grade with
     * respect to the originating data source.
     *
     * A <code>SyncProvider</code>
     * implementation returning this grade will lock the row in the originating
     * data source.
     */
    public static final int GRADE_LOCK_WHEN_MODIFIED = 4;

    /**
     * Indicates the most pessimistic synchronization grade with
     * respect to the originating
     * data source. A <code>SyncProvider</code>
     * implementation returning this grade will lock the entire view and/or
     * table affected by the original statement used to populate a
     * <code>RowSet</code> object.
     */
    public static final int GRADE_LOCK_WHEN_LOADED = 5;

    /**
     * Indicates that no locks remain on the originating data source. This is the default
     * lock setting for all <code>SyncProvider</code> implementations unless
     * otherwise directed by a <code>RowSet</code> object.
     */
    public static final int DATASOURCE_NO_LOCK = 1;

    /**
     * Indicates that a lock is placed on the rows that are touched by the original
     * SQL statement used to populate the <code>RowSet</code> object
     * that is using this <code>SyncProvider</code> object.
     */
    public static final int DATASOURCE_ROW_LOCK = 2;

    /**
     * Indicates that a lock is placed on all tables that are touched by the original
     * SQL statement used to populate the <code>RowSet</code> object
     * that is using this <code>SyncProvider</code> object.
     */
    public static final int DATASOURCE_TABLE_LOCK = 3;

    /**
     * Indicates that a lock is placed on the entire data source that is the source of
     * data for the <code>RowSet</code> object
     * that is using this <code>SyncProvider</code> object.
     */
    public static final int DATASOURCE_DB_LOCK = 4;

    /**
     * Indicates that a <code>SyncProvider</code> implementation
     * supports synchronization between a <code>RowSet</code> object and
     * the SQL <code>VIEW</code> used to populate it.
     */
    public static final int UPDATABLE_VIEW_SYNC = 5;

    /**
     * Indicates that a <code>SyncProvider</code> implementation
     * does <B>not</B> support synchronization between a <code>RowSet</code>
     * object and the SQL <code>VIEW</code> used to populate it.
     */
    public static final int NONUPDATABLE_VIEW_SYNC = 6;
}
