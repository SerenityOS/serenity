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

package javax.sql.rowset;

import java.sql.*;
import javax.sql.*;
import javax.naming.*;
import java.io.*;
import java.math.*;
import org.xml.sax.*;

/**
 * The standard interface that all implementations of a {@code WebRowSet}
 * must implement.
 *
 * <h2>1.0 Overview</h2>
 * The {@code WebRowSetImpl} provides the standard
 * reference implementation, which may be extended if required.
 * <P>
 * The standard WebRowSet XML Schema definition is available at the following
 * URI:
 * <ul>
 * <li>
 * <a href="http://xmlns.jcp.org/xml/ns//jdbc/webrowset.xsd">http://xmlns.jcp.org/xml/ns//jdbc/webrowset.xsd</a>
 * </li>
 * </ul>
 * It describes the standard XML document format required when describing a
 * {@code RowSet} object in XML and must be used be all standard implementations
 * of the {@code WebRowSet} interface to ensure interoperability. In addition,
 * the {@code WebRowSet} schema uses specific SQL/XML Schema annotations,
 * thus ensuring greater cross
 * platform interoperability. This is an effort currently under way at the ISO
 * organization. The SQL/XML definition is available at the following URI:
 * <ul>
 * <li>
 * <a href="http://standards.iso.org/iso/9075/2002/12/sqlxml.xsd">http://standards.iso.org/iso/9075/2002/12/sqlxml.xsd</a>
 * </li>
 * </ul>
 * The schema definition describes the internal data of a {@code RowSet} object
 * in three distinct areas:
 * <UL>
 * <li>properties - These properties describe the standard synchronization
 * provider properties in addition to the more general {@code RowSet} properties.
 * </li>
 * <li>metadata - This describes the metadata associated with the tabular structure governed by a
 * {@code WebRowSet} object. The metadata described is closely aligned with the
 * metadata accessible in the underlying {@code java.sql.ResultSet} interface.
 * </li>
 * <li>data - This describes the original data (the state of data since the
 * last population
 * or last synchronization of the {@code WebRowSet} object) and the current
 * data. By keeping track of the delta between the original data and the current data,
 * a {@code WebRowSet} maintains the ability to synchronize changes
 * in its data back to the originating data source.
 * </li>
 * </ul>
 *
 * <h2>2.0 WebRowSet States</h2>
 * The following sections demonstrates how a {@code WebRowSet} implementation
 * should use the XML Schema to describe update, insert, and delete operations
 * and to describe the state of a {@code WebRowSet} object in XML.
 *
 * <h2>2.1 State 1 - Outputting a {@code WebRowSet} Object to XML</h2>
 * In this example, a {@code WebRowSet} object is created and populated with a simple 2 column,
 * 5 row table from a data source. Having the 5 rows in a {@code WebRowSet} object
 * makes it possible to describe them in XML. The
 * metadata describing the various standard JavaBeans properties as defined
 * in the RowSet interface plus the standard properties defined in
 * the {@code CachedRowSet} interface
 * provide key details that describe WebRowSet
 * properties. Outputting the WebRowSet object to XML using the standard
 * {@code writeXml} methods describes the internal properties as follows:
 * <PRE>
 * {@code
 * <properties>
 *       <command>select co1, col2 from test_table</command>
 *      <concurrency>1</concurrency>
 *      <datasource/>
 *      <escape-processing>true</escape-processing>
 *      <fetch-direction>0</fetch-direction>
 *      <fetch-size>0</fetch-size>
 *      <isolation-level>1</isolation-level>
 *      <key-columns/>
 *      <map/>
 *      <max-field-size>0</max-field-size>
 *      <max-rows>0</max-rows>
 *      <query-timeout>0</query-timeout>
 *      <read-only>false</read-only>
 *      <rowset-type>TRANSACTION_READ_UNCOMMITTED</rowset-type>
 *      <show-deleted>false</show-deleted>
 *      <table-name/>
 *      <url>jdbc:thin:oracle</url>
 *      <sync-provider>
 *              <sync-provider-name>.com.rowset.provider.RIOptimisticProvider</sync-provider-name>
 *              <sync-provider-vendor>Oracle Corporation</sync-provider-vendor>
 *              <sync-provider-version>1.0</sync-provider-name>
 *              <sync-provider-grade>LOW</sync-provider-grade>
 *              <data-source-lock>NONE</data-source-lock>
 *      </sync-provider>
 * </properties>
 * } </PRE>
 * The meta-data describing the make up of the WebRowSet is described
 * in XML as detailed below. Note both columns are described between the
 * {@code column-definition} tags.
 * <PRE>
 * {@code
 * <metadata>
 *      <column-count>2</column-count>
 *      <column-definition>
 *              <column-index>1</column-index>
 *              <auto-increment>false</auto-increment>
 *              <case-sensitive>true</case-sensitive>
 *              <currency>false</currency>
 *              <nullable>1</nullable>
 *              <signed>false</signed>
 *              <searchable>true</searchable>
 *              <column-display-size>10</column-display-size>
 *              <column-label>COL1</column-label>
 *              <column-name>COL1</column-name>
 *              <schema-name/>
 *              <column-precision>10</column-precision>
 *              <column-scale>0</column-scale>
 *              <table-name/>
 *              <catalog-name/>
 *              <column-type>1</column-type>
 *              <column-type-name>CHAR</column-type-name>
 *      </column-definition>
 *      <column-definition>
 *              <column-index>2</column-index>
 *              <auto-increment>false</auto-increment>
 *              <case-sensitive>false</case-sensitive>
 *              <currency>false</currency>
 *              <nullable>1</nullable>
 *              <signed>true</signed>
 *              <searchable>true</searchable>
 *              <column-display-size>39</column-display-size>
 *              <column-label>COL2</column-label>
 *              <column-name>COL2</column-name>
 *              <schema-name/>
 *              <column-precision>38</column-precision>
 *              <column-scale>0</column-scale>
 *              <table-name/>
 *              <catalog-name/>
 *              <column-type>3</column-type>
 *              <column-type-name>NUMBER</column-type-name>
 *      </column-definition>
 * </metadata>
 * }</PRE>
 * Having detailed how the properties and metadata are described, the following details
 * how the contents of a {@code WebRowSet} object is described in XML. Note, that
 * this describes a {@code WebRowSet} object that has not undergone any
 * modifications since its instantiation.
 * A {@code currentRow} tag is mapped to each row of the table structure that the
 * {@code WebRowSet} object provides. A {@code columnValue} tag may contain
 * either the {@code stringData} or {@code binaryData} tag, according to
 * the SQL type that
 * the XML value is mapping back to. The {@code binaryData} tag contains data in the
 * Base64 encoding and is typically used for {@code BLOB} and {@code CLOB} type data.
 * <PRE>
 * {@code
 * <data>
 *      <currentRow>
 *              <columnValue>
 *                      firstrow
 *              </columnValue>
 *              <columnValue>
 *                      1
 *              </columnValue>
 *      </currentRow>
 *      <currentRow>
 *              <columnValue>
 *                      secondrow
 *              </columnValue>
 *              <columnValue>
 *                      2
 *              </columnValue>
 *      </currentRow>
 *      <currentRow>
 *              <columnValue>
 *                      thirdrow
 *              </columnValue>
 *              <columnValue>
 *                      3
 *              </columnValue>
 *      </currentRow>
 *      <currentRow>
 *              <columnValue>
 *                      fourthrow
 *              </columnValue>
 *              <columnValue>
 *                      4
 *              </columnValue>
 *      </currentRow>
 * </data>
 * }</PRE>
 * <h2>2.2 State 2 - Deleting a Row</h2>
 * Deleting a row in a {@code WebRowSet} object involves simply moving to the row
 * to be deleted and then calling the method {@code deleteRow}, as in any other
 * {@code RowSet} object.  The following
 * two lines of code, in which <i>wrs</i> is a {@code WebRowSet} object, delete
 * the third row.
 * <PRE>
 *     wrs.absolute(3);
 *     wrs.deleteRow();
 * </PRE>
 * The XML description shows the third row is marked as a {@code deleteRow},
 *  which eliminates the third row in the {@code WebRowSet} object.
 * <PRE>
 * {@code
 * <data>
 *      <currentRow>
 *              <columnValue>
 *                      firstrow
 *              </columnValue>
 *              <columnValue>
 *                      1
 *              </columnValue>
 *      </currentRow>
 *      <currentRow>
 *              <columnValue>
 *                      secondrow
 *              </columnValue>
 *              <columnValue>
 *                      2
 *              </columnValue>
 *      </currentRow>
 *      <deleteRow>
 *              <columnValue>
 *                      thirdrow
 *              </columnValue>
 *              <columnValue>
 *                      3
 *              </columnValue>
 *      </deleteRow>
 *      <currentRow>
 *              <columnValue>
 *                      fourthrow
 *              </columnValue>
 *              <columnValue>
 *                      4
 *              </columnValue>
 *      </currentRow>
 * </data>
 *} </PRE>
 * <h2>2.3 State 3 - Inserting a Row</h2>
 * A {@code WebRowSet} object can insert a new row by moving to the insert row,
 * calling the appropriate updater methods for each column in the row, and then
 * calling the method {@code insertRow}.
 * <PRE>
 * {@code
 * wrs.moveToInsertRow();
 * wrs.updateString(1, "fifththrow");
 * wrs.updateString(2, "5");
 * wrs.insertRow();
 * }</PRE>
 * The following code fragment changes the second column value in the row just inserted.
 * Note that this code applies when new rows are inserted right after the current row,
 * which is why the method {@code next} moves the cursor to the correct row.
 * Calling the method {@code acceptChanges} writes the change to the data source.
 *
 * <PRE>
 * {@code wrs.moveToCurrentRow();
 * wrs.next();
 * wrs.updateString(2, "V");
 * wrs.acceptChanges();
 * }</PRE>
 * Describing this in XML demonstrates where the Java code inserts a new row and then
 * performs an update on the newly inserted row on an individual field.
 * <PRE>
 * {@code
 * <data>
 *      <currentRow>
 *              <columnValue>
 *                      firstrow
 *              </columnValue>
 *              <columnValue>
 *                      1
 *              </columnValue>
 *      </currentRow>
 *      <currentRow>
 *              <columnValue>
 *                      secondrow
 *              </columnValue>
 *              <columnValue>
 *                      2
 *              </columnValue>
 *      </currentRow>
 *      <currentRow>
 *              <columnValue>
 *                      newthirdrow
 *              </columnValue>
 *              <columnValue>
 *                      III
 *              </columnValue>
 *      </currentRow>
 *      <insertRow>
 *              <columnValue>
 *                      fifthrow
 *              </columnValue>
 *              <columnValue>
 *                      5
 *              </columnValue>
 *              <updateValue>
 *                      V
 *              </updateValue>
 *      </insertRow>
 *      <currentRow>
 *              <columnValue>
 *                      fourthrow
 *              </columnValue>
 *              <columnValue>
 *                      4
 *              </columnValue>
 *      </currentRow>
 * </date>
 *} </PRE>
 * <h2>2.4 State 4 - Modifying a Row</h2>
 * Modifying a row produces specific XML that records both the new value and the
 * value that was replaced.  The value that was replaced becomes the original value,
 * and the new value becomes the current value. The following
 * code moves the cursor to a specific row, performs some modifications, and updates
 * the row when complete.
 * <PRE>
 *{@code
 * wrs.absolute(5);
 * wrs.updateString(1, "new4thRow");
 * wrs.updateString(2, "IV");
 * wrs.updateRow();
 * }</PRE>
 * In XML, this is described by the {@code modifyRow} tag. Both the original and new
 * values are contained within the tag for original row tracking purposes.
 * <PRE>
 * {@code
 * <data>
 *      <currentRow>
 *              <columnValue>
 *                      firstrow
 *              </columnValue>
 *              <columnValue>
 *                      1
 *              </columnValue>
 *      </currentRow>
 *      <currentRow>
 *              <columnValue>
 *                      secondrow
 *              </columnValue>
 *              <columnValue>
 *                      2
 *              </columnValue>
 *      </currentRow>
 *      <currentRow>
 *              <columnValue>
 *                      newthirdrow
 *              </columnValue>
 *              <columnValue>
 *                      III
 *              </columnValue>
 *      </currentRow>
 *      <currentRow>
 *              <columnValue>
 *                      fifthrow
 *              </columnValue>
 *              <columnValue>
 *                      5
 *              </columnValue>
 *      </currentRow>
 *      <modifyRow>
 *              <columnValue>
 *                      fourthrow
 *              </columnValue>
 *              <updateValue>
 *                      new4thRow
 *              </updateValue>
 *              <columnValue>
 *                      4
 *              </columnValue>
 *              <updateValue>
 *                      IV
 *              </updateValue>
 *      </modifyRow>
 * </data>
 * }</PRE>
 *
 * @see javax.sql.rowset.JdbcRowSet
 * @see javax.sql.rowset.CachedRowSet
 * @see javax.sql.rowset.FilteredRowSet
 * @see javax.sql.rowset.JoinRowSet
 * @since 1.5
 */

public interface WebRowSet extends CachedRowSet {

   /**
    * Reads a {@code WebRowSet} object in its XML format from the given
    * {@code Reader} object.
    *
    * @param reader the {@code java.io.Reader} stream from which this
    *        {@code WebRowSet} object will be populated

    * @throws SQLException if a database access error occurs
    */
    public void readXml(java.io.Reader reader) throws SQLException;

    /**
     * Reads a stream based XML input to populate this {@code WebRowSet}
     * object.
     *
     * @param iStream the {@code java.io.InputStream} from which this
     *        {@code WebRowSet} object will be populated
     * @throws SQLException if a data source access error occurs
     * @throws IOException if an IO exception occurs
     */
    public void readXml(java.io.InputStream iStream) throws SQLException, IOException;

   /**
    * Populates this {@code WebRowSet} object with
    * the contents of the given {@code ResultSet} object and writes its
    * data, properties, and metadata
    * to the given {@code Writer} object in XML format.
    * <p>
    * NOTE: The {@code WebRowSet} cursor may be moved to write out the
    * contents to the XML data source. If implemented in this way, the cursor <b>must</b>
    * be returned to its position just prior to the {@code writeXml()} call.
    *
    * @param rs the {@code ResultSet} object with which to populate this
    *        {@code WebRowSet} object
    * @param writer the {@code java.io.Writer} object to write to.
    * @throws SQLException if an error occurs writing out the rowset
    *          contents in XML format
    */
    public void writeXml(ResultSet rs, java.io.Writer writer) throws SQLException;

   /**
    * Populates this {@code WebRowSet} object with
    * the contents of the given {@code ResultSet} object and writes its
    * data, properties, and metadata
    * to the given {@code OutputStream} object in XML format.
    * <p>
    * NOTE: The {@code WebRowSet} cursor may be moved to write out the
    * contents to the XML data source. If implemented in this way, the cursor <b>must</b>
    * be returned to its position just prior to the {@code writeXml()} call.
    *
    * @param rs the {@code ResultSet} object with which to populate this
    *        {@code WebRowSet} object
    * @param oStream the {@code java.io.OutputStream} to write to
    * @throws SQLException if a data source access error occurs
    * @throws IOException if a IO exception occurs
    */
    public void writeXml(ResultSet rs, java.io.OutputStream oStream) throws SQLException, IOException;

   /**
    * Writes the data, properties, and metadata for this {@code WebRowSet} object
    * to the given {@code Writer} object in XML format.
    *
    * @param writer the {@code java.io.Writer} stream to write to
    * @throws SQLException if an error occurs writing out the rowset
    *          contents to XML
    */
    public void writeXml(java.io.Writer writer) throws SQLException;

    /**
     * Writes the data, properties, and metadata for this {@code WebRowSet} object
     * to the given {@code OutputStream} object in XML format.
     *
     * @param oStream the {@code java.io.OutputStream} stream to write to
     * @throws SQLException if a data source access error occurs
     * @throws IOException if a IO exception occurs
     */
    public void writeXml(java.io.OutputStream oStream) throws SQLException, IOException;

    /**
     * The public identifier for the XML Schema definition that defines the XML
     * tags and their valid values for a {@code WebRowSet} implementation.
     */
    public static String PUBLIC_XML_SCHEMA =
        "--//Oracle Corporation//XSD Schema//EN";

    /**
     * The URL for the XML Schema definition file that defines the XML tags and
     * their valid values for a {@code WebRowSet} implementation.
     */
    public static String SCHEMA_SYSTEM_ID = "http://java.sun.com/xml/ns/jdbc/webrowset.xsd";
}
