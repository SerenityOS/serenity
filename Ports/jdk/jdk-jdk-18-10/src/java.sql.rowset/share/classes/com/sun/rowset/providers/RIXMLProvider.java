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
import java.io.IOException;
import java.sql.*;
import javax.sql.*;

import javax.sql.rowset.spi.*;

/**
 * A reference implementation of a JDBC RowSet synchronization provider
 * with the ability to read and write rowsets in well formed XML using the
 * standard WebRowSet schema.
 *
 * <h2>1.0 Background</h2>
 * This synchronization provider is registered with the
 * <code>SyncFactory</code> by default as the
 * <code>com.sun.rowset.providers.RIXMLProvider</code>.
 * <P>
 * A <code>WebRowSet</code> object uses an <code>RIXMLProvider</code> implementation
 * to read an XML data source or to write itself in XML format using the
 * <code>WebRowSet</code> XML schema definition available at
 * <pre>
 *     <a href="http://xmlns.jcp.org/xml/ns//jdbc/webrowset.xsd">http://xmlns.jcp.org/xml/ns//jdbc/webrowset.xsd</a>
 * </pre>
 * The <code>RIXMLProvider</code> implementation has a synchronization level of
 * GRADE_NONE, which means that it does no checking at all for conflicts.  It
 * simply writes a <code>WebRowSet</code> object to a file.
 * <h2>2.0 Usage</h2>
 * A <code>WebRowSet</code> implementation is created with an <code>RIXMLProvider</code>
 * by default.
 * <pre>
 *     WebRowSet wrs = new FooWebRowSetImpl();
 * </pre>
 * The <code>SyncFactory</code> always provides an instance of
 * <code>RIOptimisticProvider</code> when no provider is specified,
 * but the implementation of the default constructor for <code>WebRowSet</code> sets the
 * provider to be the <code>RIXMLProvider</code> implementation.  Therefore,
 * the following line of code is executed behind the scenes as part of the
 * implementation of the default constructor.
 * <pre>
 *     wrs.setSyncProvider("com.sun.rowset.providers.RIXMLProvider");
 * </pre>
 * See the standard <code>RowSet</code> reference implementations in the
 * <code>com.sun.rowset</code> package for more details.
 *
 * @author  Jonathan Bruce
 * @see javax.sql.rowset.spi.SyncProvider
 * @see javax.sql.rowset.spi.SyncProviderException
 * @see javax.sql.rowset.spi.SyncFactory
 * @see javax.sql.rowset.spi.SyncFactoryException
 */
public final class RIXMLProvider extends SyncProvider {

    /**
     * The unique provider identifier.
     */
    private String providerID = "com.sun.rowset.providers.RIXMLProvider";

    /**
     * The vendor name of this SyncProvider implementation.
     */
    private String vendorName = "Oracle Corporation";

    /**
     * The version number of this SyncProvider implementation.
     */
    private String versionNumber = "1.0";

    private JdbcRowSetResourceBundle resBundle;

    private XmlReader xmlReader;
    private XmlWriter xmlWriter;

    /**
     * This provider is available to all JDBC <code>RowSet</code> implementations as the
     * default persistence provider.
     */
    public RIXMLProvider() {
        providerID = this.getClass().getName();
        try {
           resBundle = JdbcRowSetResourceBundle.getJdbcRowSetResourceBundle();
        } catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Returns <code>"javax.sql.rowset.providers.RIXMLProvider"</code>, which is
     * the fully qualified class name of this provider implementation.
     *
     * @return a <code>String</code> object with the fully specified class name of
     *           this <code>RIOptimisticProvider</code> implementation
     */
    public String getProviderID() {
        return providerID;
    }

    // additional methods that sit on top of reader/writer methods back to
    // original datasource. Allow XML state to be written out and in

    /**
     * Sets this <code>WebRowSet</code> object's reader to the given
     * <code>XmlReader</code> object.
     *
     * @throws SQLException if a database access error occurs
     */
    public void setXmlReader(XmlReader reader) throws SQLException {
        xmlReader = reader;
    }

    /**
     * Sets this <code>WebRowSet</code> object's writer to the given
     * <code>XmlWriter</code> object.
     *
     * @throws SQLException if a database access error occurs
     */
    public void setXmlWriter(XmlWriter writer) throws SQLException {
        xmlWriter = writer;
    }

    /**
     * Retrieves the reader that this <code>WebRowSet</code> object
     * will call when its <code>readXml</code> method is called.
     *
     * @return the <code>XmlReader</code> object for this SyncProvider
     * @throws SQLException if a database access error occurs
     */
    public XmlReader getXmlReader() throws SQLException {
        return xmlReader;
    }

    /**
     * Retrieves the writer that this <code>WebRowSet</code> object
     * will call when its <code>writeXml</code> method is called.
     *
     * @return the <code>XmlWriter</code> for this SyncProvider
     * @throws SQLException if a database access error occurs
     */
    public XmlWriter getXmlWriter() throws SQLException {
        return xmlWriter;
    }

    /**
     * Returns the <code>SyncProvider</code> grade of syncrhonization that
     * <code>RowSet</code> object instances can expect when using this
     * implementation. As this implementation provides no synchonization
     * facilities to the XML data source, the lowest grade is returned.
     *
     * @return the <code>SyncProvider</code> syncronization grade of this
     *     provider; must be one of the following constants:
     *       <PRE>
     *          SyncProvider.GRADE_NONE,
     *          SyncProvider.GRADE_MODIFIED_AT_COMMIT,
     *          SyncProvider.GRADE_CHECK_ALL_AT_COMMIT,
     *          SyncProvider.GRADE_LOCK_WHEN_MODIFIED,
     *          SyncProvider.GRADE_LOCK_WHEN_LOADED
     *       </PRE>
     *
     */
    public int getProviderGrade() {
        return SyncProvider.GRADE_NONE;
    }

    /**
     * Returns the default UPDATABLE_VIEW behavior of this reader
     *
     */
    public int supportsUpdatableView() {
        return SyncProvider.NONUPDATABLE_VIEW_SYNC;
    }

    /**
     * Returns the default DATASOURCE_LOCK behavior of this reader
     */
    public int getDataSourceLock() throws SyncProviderException {
        return SyncProvider.DATASOURCE_NO_LOCK;
    }

    /**
     * Throws an unsupported operation exception as this method does
     * function with non-locking XML data sources.
     */
    public void setDataSourceLock(int lock) throws SyncProviderException {
        throw new UnsupportedOperationException(resBundle.handleGetObject("rixml.unsupp").toString());
    }

    /**
     * Returns a null object as RowSetWriters are not returned by this SyncProvider
     */
    public RowSetWriter getRowSetWriter() {
        return null;
    }

    /**
     * Returns a null object as RowSetWriter objects are not returned by this
     * SyncProvider
     */
    public RowSetReader getRowSetReader() {
        return null;
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
     * Returns the vendor name of the Reference Implemntation Optimistic
     * Syncchronication Provider
     *
     * @return the <code>String</code> detailing the vendor name of this
     *      SyncProvider
     */
    public String getVendor() {
        return this.vendorName;
    }
}
