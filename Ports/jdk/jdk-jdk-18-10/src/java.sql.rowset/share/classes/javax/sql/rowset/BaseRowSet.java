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
import java.util.*;
import java.io.*;
import java.math.*;
import java.io.Serializable;

import javax.sql.rowset.serial.*;

/**
 * An abstract class providing a <code>RowSet</code> object with its basic functionality.
 * The basic functions include having properties and sending event notifications,
 * which all JavaBeans components must implement.
 *
 * <h2>1.0 Overview</h2>
 * The <code>BaseRowSet</code> class provides the core functionality
 * for all <code>RowSet</code> implementations,
 * and all standard implementations <b>may</b> use this class in combination with
 * one or more <code>RowSet</code> interfaces in order to provide a standard
 * vendor-specific implementation.  To clarify, all implementations must implement
 * at least one of the <code>RowSet</code> interfaces (<code>JdbcRowSet</code>,
 * <code>CachedRowSet</code>, <code>JoinRowSet</code>, <code>FilteredRowSet</code>,
 * or <code>WebRowSet</code>). This means that any implementation that extends
 * the <code>BaseRowSet</code> class must also implement one of the <code>RowSet</code>
 * interfaces.
 * <p>
 * The <code>BaseRowSet</code> class provides the following:
 *
 * <UL>
 * <LI><b>Properties</b>
 *     <ul>
 *     <li>Fields for storing current properties
 *     <li>Methods for getting and setting properties
 *     </ul>
 *
 * <LI><b>Event notification</b>
 *
 * <LI><b>A complete set of setter methods</b> for setting the parameters in a
 *      <code>RowSet</code> object's command
 *
 * <LI> <b>Streams</b>
 *  <ul>
 *  <li>Fields for storing stream instances
 *  <li>Constants for indicating the type of a stream
 *  </ul>
 * </UL>
 *
 * <h2>2.0 Setting Properties</h2>
 * All rowsets maintain a set of properties, which will usually be set using
 * a tool.  The number and kinds of properties a rowset has will vary,
 * depending on what the <code>RowSet</code> implementation does and how it gets
 * its data.  For example,
 * rowsets that get their data from a <code>ResultSet</code> object need to
 * set the properties that are required for making a database connection.
 * If a <code>RowSet</code> object uses the <code>DriverManager</code> facility to make a
 * connection, it needs to set a property for the JDBC URL that identifies the
 * appropriate driver, and it needs to set the properties that give the
 * user name and password.
 * If, on the other hand, the rowset uses a <code>DataSource</code> object
 * to make the connection, which is the preferred method, it does not need to
 * set the property for the JDBC URL.  Instead, it needs to set the property
 * for the logical name of the data source along with the properties for
 * the user name and password.
 * <P>
 * NOTE:  In order to use a <code>DataSource</code> object for making a
 * connection, the <code>DataSource</code> object must have been registered
 * with a naming service that uses the Java Naming and Directory
 * Interface (JNDI) API.  This registration
 * is usually done by a person acting in the capacity of a system administrator.
 *
 * <h2>3.0 Setting the Command and Its Parameters</h2>
 * When a rowset gets its data from a relational database, it executes a command (a query)
 * that produces a <code>ResultSet</code> object.  This query is the command that is set
 * for the <code>RowSet</code> object's command property.  The rowset populates itself with data by reading the
 * data from the <code>ResultSet</code> object into itself. If the query
 * contains placeholders for values to be set, the <code>BaseRowSet</code> setter methods
 * are used to set these values. All setter methods allow these values to be set
 * to <code>null</code> if required.
 * <P>
 * The following code fragment illustrates how the
 * <code>CachedRowSet</code>
 * object <code>crs</code> might have its command property set.  Note that if a
 * tool is used to set properties, this is the code that the tool would use.
 * <PRE>{@code
 *    crs.setCommand("SELECT FIRST_NAME, LAST_NAME, ADDRESS FROM CUSTOMERS" +
 *                   "WHERE CREDIT_LIMIT > ? AND REGION = ?");
 * }</PRE>
 * <P>
 * In this example, the values for <code>CREDIT_LIMIT</code> and
 * <code>REGION</code> are placeholder parameters, which are indicated with a
 * question mark (?).  The first question mark is placeholder parameter number
 * <code>1</code>, the second question mark is placeholder parameter number
 * <code>2</code>, and so on.  Any placeholder parameters must be set with
 * values before the query can be executed. To set these
 * placeholder parameters, the <code>BaseRowSet</code> class provides a set of setter
 * methods, similar to those provided by the <code>PreparedStatement</code>
 * interface, for setting values of each data type.  A <code>RowSet</code> object stores the
 * parameter values internally, and its <code>execute</code> method uses them internally
 * to set values for the placeholder parameters
 * before it sends the command to the DBMS to be executed.
 * <P>
 * The following code fragment demonstrates
 * setting the two parameters in the query from the previous example.
 * <PRE>{@code
 *    crs.setInt(1, 5000);
 *    crs.setString(2, "West");
 * }</PRE>
 * If the <code>execute</code> method is called at this point, the query
 * sent to the DBMS will be:
 * <PRE>{@code
 *    "SELECT FIRST_NAME, LAST_NAME, ADDRESS FROM CUSTOMERS" +
 *                   "WHERE CREDIT_LIMIT > 5000 AND REGION = 'West'"
 * }</PRE>
 * NOTE: Setting <code>Array</code>, <code>Clob</code>, <code>Blob</code> and
 * <code>Ref</code> objects as a command parameter, stores these values as
 * <code>SerialArray</code>, <code>SerialClob</code>, <code>SerialBlob</code>
 * and <code>SerialRef</code> objects respectively.
 *
 * <h2>4.0 Handling of Parameters Behind the Scenes</h2>
 *
 * NOTE: The <code>BaseRowSet</code> class provides two kinds of setter methods,
 * those that set properties and those that set placeholder parameters. The setter
 * methods discussed in this section are those that set placeholder parameters.
 * <P>
 * The placeholder parameters set with the <code>BaseRowSet</code> setter methods
 * are stored as objects in an internal <code>Hashtable</code> object.
 * Primitives are stored as their <code>Object</code> type. For example, <code>byte</code>
 * is stored as <code>Byte</code> object, and <code>int</code> is stored as
 * an <code>Integer</code> object.
 * When the method <code>execute</code> is called, the values in the
 * <code>Hashtable</code> object are substituted for the appropriate placeholder
 * parameters in the command.
 * <P>
 * A call to the method <code>getParams</code> returns the values stored in the
 * <code>Hashtable</code> object as an array of <code>Object</code> instances.
 * An element in this array may be a simple <code>Object</code> instance or an
 * array (which is a type of <code>Object</code>). The particular setter method used
 * determines whether an element in this array is an <code>Object</code> or an array.
 * <P>
 * The majority of methods for setting placeholder parameters take two parameters,
 *  with the first parameter
 * indicating which placeholder parameter is to be set, and the second parameter
 * giving the value to be set.  Methods such as <code>setInt</code>,
 * <code>setString</code>, <code>setBoolean</code>, and <code>setLong</code> fall into
 * this category.  After these methods have been called, a call to the method
 * <code>getParams</code> will return an array with the values that have been set. Each
 * element in the array is an <code>Object</code> instance representing the
 * values that have been set. The order of these values in the array is determined by the
 * <code>int</code> (the first parameter) passed to the setter method. The values in the
 * array are the values (the second parameter) passed to the setter method.
 * In other words, the first element in the array is the value
 * to be set for the first placeholder parameter in the <code>RowSet</code> object's
 * command. The second element is the value to
 * be set for the second placeholder parameter, and so on.
 * <P>
 * Several setter methods send the driver and DBMS information beyond the value to be set.
 * When the method <code>getParams</code> is called after one of these setter methods has
 * been used, the elements in the array will themselves be arrays to accommodate the
 * additional information. In this category, the method <code>setNull</code> is a special case
 * because one version takes only
 * two parameters (<code>setNull(int parameterIndex, int SqlType)</code>). Nevertheless,
 * it requires
 * an array to contain the information that will be passed to the driver and DBMS.  The first
 * element in this array is the value to be set, which is <code>null</code>, and the
 * second element is the <code>int</code> supplied for <i>sqlType</i>, which
 * indicates the type of SQL value that is being set to <code>null</code>. This information
 * is needed by some DBMSs and is therefore required in order to ensure that applications
 * are portable.
 * The other version is intended to be used when the value to be set to <code>null</code>
 * is a user-defined type. It takes three parameters
 * (<code>setNull(int parameterIndex, int sqlType, String typeName)</code>) and also
 * requires an array to contain the information to be passed to the driver and DBMS.
 * The first two elements in this array are the same as for the first version of
 * <code>setNull</code>.  The third element, <i>typeName</i>, gives the SQL name of
 * the user-defined type. As is true with the other setter methods, the number of the
 * placeholder parameter to be set is indicated by an element's position in the array
 * returned by <code>getParams</code>.  So, for example, if the parameter
 * supplied to <code>setNull</code> is <code>2</code>, the second element in the array
 * returned by <code>getParams</code> will be an array of two or three elements.
 * <P>
 * Some methods, such as <code>setObject</code> and <code>setDate</code> have versions
 * that take more than two parameters, with the extra parameters giving information
 * to the driver or the DBMS. For example, the methods <code>setDate</code>,
 * <code>setTime</code>, and <code>setTimestamp</code> can take a <code>Calendar</code>
 * object as their third parameter.  If the DBMS does not store time zone information,
 * the driver uses the <code>Calendar</code> object to construct the <code>Date</code>,
 * <code>Time</code>, or <code>Timestamp</code> object being set. As is true with other
 * methods that provide additional information, the element in the array returned
 * by <code>getParams</code> is an array instead of a simple <code>Object</code> instance.
 * <P>
 * The methods <code>setAsciiStream</code>, <code>setBinaryStream</code>,
 * <code>setCharacterStream</code>, and <code>setUnicodeStream</code> (which is
 * deprecated, so applications should use <code>getCharacterStream</code> instead)
 * take three parameters, so for them, the element in the array returned by
 * <code>getParams</code> is also an array.  What is different about these setter
 * methods is that in addition to the information provided by parameters, the array contains
 * one of the <code>BaseRowSet</code> constants indicating the type of stream being set.
* <p>
* NOTE: The method <code>getParams</code> is called internally by
* <code>RowSet</code> implementations extending this class; it is not normally called by an
* application programmer directly.
*
* <h2>5.0 Event Notification</h2>
* The <code>BaseRowSet</code> class provides the event notification
* mechanism for rowsets.  It contains the field
* <code>listeners</code>, methods for adding and removing listeners, and
* methods for notifying listeners of changes.
* <P>
* A listener is an object that has implemented the <code>RowSetListener</code> interface.
* If it has been added to a <code>RowSet</code> object's list of listeners, it will be notified
*  when an event occurs on that <code>RowSet</code> object.  Each listener's
* implementation of the <code>RowSetListener</code> methods defines what that object
* will do when it is notified that an event has occurred.
* <P>
* There are three possible events for a <code>RowSet</code> object:
* <OL>
* <LI>the cursor moves
* <LI>an individual row is changed (updated, deleted, or inserted)
* <LI>the contents of the entire <code>RowSet</code> object  are changed
* </OL>
* <P>
* The <code>BaseRowSet</code> method used for the notification indicates the
* type of event that has occurred.  For example, the method
* <code>notifyRowChanged</code> indicates that a row has been updated,
* deleted, or inserted.  Each of the notification methods creates a
* <code>RowSetEvent</code> object, which is supplied to the listener in order to
* identify the <code>RowSet</code> object on which the event occurred.
* What the listener does with this information, which may be nothing, depends on how it was
* implemented.
*
* <h2>6.0 Default Behavior</h2>
* A default <code>BaseRowSet</code> object is initialized with many starting values.
*
* The following is true of a default <code>RowSet</code> instance that extends
* the <code>BaseRowSet</code> class:
* <UL>
*   <LI>Has a scrollable cursor and does not show changes
*       made by others.
*   <LI>Is updatable.
*   <LI>Does not show rows that have been deleted.
*   <LI>Has no time limit for how long a driver may take to
*       execute the <code>RowSet</code> object's command.
*   <LI>Has no limit for the number of rows it may contain.
*   <LI>Has no limit for the number of bytes a column may contain. NOTE: This
*   limit applies only to columns that hold values of the
*   following types:  <code>BINARY</code>, <code>VARBINARY</code>,
*   <code>LONGVARBINARY</code>, <code>CHAR</code>, <code>VARCHAR</code>,
*   and <code>LONGVARCHAR</code>.
*   <LI>Will not see uncommitted data (make "dirty" reads).
*   <LI>Has escape processing turned on.
*   <LI>Has its connection's type map set to <code>null</code>.
*   <LI>Has an empty <code>Vector</code> object for storing the values set
*       for the placeholder parameters in the <code>RowSet</code> object's command.
* </UL>
* <p>
* If other values are desired, an application must set the property values
* explicitly. For example, the following line of code sets the maximum number
* of rows for the <code>CachedRowSet</code> object <i>crs</i> to 500.
* <PRE>
*    crs.setMaxRows(500);
* </PRE>
* Methods implemented in extensions of this <code>BaseRowSet</code> class <b>must</b> throw an
* <code>SQLException</code> object for any violation of the defined assertions.  Also, if the
* extending class overrides and reimplements any <code>BaseRowSet</code> method and encounters
* connectivity or underlying data source issues, that method <b>may</b> in addition throw an
* <code>SQLException</code> object for that reason.
*
* @since 1.5
*/

public abstract class BaseRowSet implements Serializable, Cloneable {

    /**
     * A constant indicating to a <code>RowSetReaderImpl</code> object
     * that a given parameter is a Unicode stream. This
     * <code>RowSetReaderImpl</code> object is provided as an extension of the
     * <code>SyncProvider</code> abstract class defined in the
     * <code>SyncFactory</code> static factory SPI mechanism.
     */
    public static final int UNICODE_STREAM_PARAM = 0;

    /**
     * A constant indicating to a <code>RowSetReaderImpl</code> object
     * that a given parameter is a binary stream. A
     * <code>RowSetReaderImpl</code> object is provided as an extension of the
     * <code>SyncProvider</code> abstract class defined in the
     * <code>SyncFactory</code> static factory SPI mechanism.
     */
    public static final int BINARY_STREAM_PARAM = 1;

    /**
     * A constant indicating to a <code>RowSetReaderImpl</code> object
     * that a given parameter is an ASCII stream. A
     * <code>RowSetReaderImpl</code> object is provided as an extension of the
     * <code>SyncProvider</code> abstract class defined in the
     * <code>SyncFactory</code> static factory SPI mechanism.
     */
    public static final int ASCII_STREAM_PARAM = 2;

    /**
     * The <code>InputStream</code> object that will be
     * returned by the method <code>getBinaryStream</code>, which is
     * specified in the <code>ResultSet</code> interface.
     * @serial
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    protected java.io.InputStream binaryStream;

    /**
     * The <code>InputStream</code> object that will be
     * returned by the method <code>getUnicodeStream</code>,
     * which is specified in the <code>ResultSet</code> interface.
     * @serial
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    protected java.io.InputStream unicodeStream;

    /**
     * The <code>InputStream</code> object that will be
     * returned by the method <code>getAsciiStream</code>,
     * which is specified in the <code>ResultSet</code> interface.
     * @serial
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    protected java.io.InputStream asciiStream;

    /**
     * The <code>Reader</code> object that will be
     * returned by the method <code>getCharacterStream</code>,
     * which is specified in the <code>ResultSet</code> interface.
     * @serial
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    protected java.io.Reader charStream;

    /**
     * The query that will be sent to the DBMS for execution when the
     * method <code>execute</code> is called.
     * @serial
     */
    private String command;

    /**
     * The JDBC URL the reader, writer, or both supply to the method
     * <code>DriverManager.getConnection</code> when the
     * <code>DriverManager</code> is used to get a connection.
     * <P>
     * The JDBC URL identifies the driver to be used to make the connection.
     * This URL can be found in the documentation supplied by the driver
     * vendor.
     * @serial
     */
    private String URL;

    /**
     * The logical name of the data source that the reader/writer should use
     * in order to retrieve a <code>DataSource</code> object from a Java
     * Directory and Naming Interface (JNDI) naming service.
     * @serial
     */
    private String dataSource;

    /**
     * The user name the reader, writer, or both supply to the method
     * <code>DriverManager.getConnection</code> when the
     * <code>DriverManager</code> is used to get a connection.
     * @serial
     */
    private transient String username;

    /**
     * The password the reader, writer, or both supply to the method
     * <code>DriverManager.getConnection</code> when the
     * <code>DriverManager</code> is used to get a connection.
     * @serial
     */
    private transient String password;

    /**
     * A constant indicating the type of this JDBC <code>RowSet</code>
     * object. It must be one of the following <code>ResultSet</code>
     * constants:  <code>TYPE_FORWARD_ONLY</code>,
     * <code>TYPE_SCROLL_INSENSITIVE</code>, or
     * <code>TYPE_SCROLL_SENSITIVE</code>.
     * @serial
     */
    private int rowSetType = ResultSet.TYPE_SCROLL_INSENSITIVE;

    /**
     * A <code>boolean</code> indicating whether deleted rows are visible in this
     * JDBC <code>RowSet</code> object .
     * @serial
     */
    private boolean showDeleted = false; // default is false

    /**
     * The maximum number of seconds the driver
     * will wait for a command to execute.  This limit applies while
     * this JDBC <code>RowSet</code> object is connected to its data
     * source, that is, while it is populating itself with
     * data and while it is writing data back to the data source.
     * @serial
     */
    private int queryTimeout = 0; // default is no timeout

    /**
     * The maximum number of rows the reader should read.
     * @serial
     */
    private int maxRows = 0; // default is no limit

    /**
     * The maximum field size the reader should read.
     * @serial
     */
    private int maxFieldSize = 0; // default is no limit

    /**
     * A constant indicating the concurrency of this JDBC <code>RowSet</code>
     * object. It must be one of the following <code>ResultSet</code>
     * constants: <code>CONCUR_READ_ONLY</code> or
     * <code>CONCUR_UPDATABLE</code>.
     * @serial
     */
    private int concurrency = ResultSet.CONCUR_UPDATABLE;

    /**
     * A <code>boolean</code> indicating whether this JDBC <code>RowSet</code>
     * object is read-only.  <code>true</code> indicates that it is read-only;
     * <code>false</code> that it is writable.
     * @serial
     */
    private boolean readOnly;

    /**
     * A <code>boolean</code> indicating whether the reader for this
     * JDBC <code>RowSet</code> object should perform escape processing.
     * <code>true</code> means that escape processing is turned on;
     * <code>false</code> that it is not. The default is <code>true</code>.
     * @serial
     */
    private boolean escapeProcessing = true;

    /**
     * A constant indicating the isolation level of the connection
     * for this JDBC <code>RowSet</code> object . It must be one of
     * the following <code>Connection</code> constants:
     * <code>TRANSACTION_NONE</code>,
     * <code>TRANSACTION_READ_UNCOMMITTED</code>,
     * <code>TRANSACTION_READ_COMMITTED</code>,
     * <code>TRANSACTION_REPEATABLE_READ</code> or
     * <code>TRANSACTION_SERIALIZABLE</code>.
     * @serial
     */
    private int isolation;

    /**
     * A constant used as a hint to the driver that indicates the direction in
     * which data from this JDBC <code>RowSet</code> object  is going
     * to be fetched. The following <code>ResultSet</code> constants are
     * possible values:
     * <code>FETCH_FORWARD</code>,
     * <code>FETCH_REVERSE</code>,
     * <code>FETCH_UNKNOWN</code>.
     * <P>
     * Unused at this time.
     * @serial
     */
    private int fetchDir = ResultSet.FETCH_FORWARD; // default fetch direction

    /**
     * A hint to the driver that indicates the expected number of rows
     * in this JDBC <code>RowSet</code> object .
     * <P>
     * Unused at this time.
     * @serial
     */
    private int fetchSize = 0; // default fetchSize

    /**
     * The <code>java.util.Map</code> object that contains entries mapping
     * SQL type names to classes in the Java programming language for the
     * custom mapping of user-defined types.
     * @serial
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private Map<String, Class<?>> map;

    /**
     * A <code>Vector</code> object that holds the list of listeners
     * that have registered with this <code>RowSet</code> object.
     * @serial
     */
    private Vector<RowSetListener> listeners;

    /**
     * A <code>Vector</code> object that holds the parameters set
     * for this <code>RowSet</code> object's current command.
     * @serial
     */
    private Hashtable<Integer, Object> params; // could be transient?

    /**
     * Constructs a new <code>BaseRowSet</code> object initialized with
     * a default <code>Vector</code> object for its <code>listeners</code>
     * field. The other default values with which it is initialized are listed
     * in Section 6.0 of the class comment for this class.
     */
    public BaseRowSet() {
        // allocate the listeners collection
        listeners = new Vector<RowSetListener>();
    }

    /**
     * Performs the necessary internal configurations and initializations
     * to allow any JDBC <code>RowSet</code> implementation to start using
     * the standard facilities provided by a <code>BaseRowSet</code>
     * instance. This method <b>should</b> be called after the <code>RowSet</code> object
     * has been instantiated to correctly initialize all parameters. This method
     * <b>should</b> never be called by an application, but is called from with
     * a <code>RowSet</code> implementation extending this class.
     */
    protected void initParams() {
        params = new Hashtable<Integer, Object>();
    }

    //--------------------------------------------------------------------
    // Events
    //--------------------------------------------------------------------

    /**
    * The listener will be notified whenever an event occurs on this <code>RowSet</code>
    * object.
    * <P>
    * A listener might, for example, be a table or graph that needs to
    * be updated in order to accurately reflect the current state of
    * the <code>RowSet</code> object.
    * <p>
    * <b>Note</b>: if the <code>RowSetListener</code> object is
    * <code>null</code>, this method silently discards the <code>null</code>
    * value and does not add a null reference to the set of listeners.
    * <p>
    * <b>Note</b>: if the listener is already set, and the new <code>RowSetListener</code>
    * instance is added to the set of listeners already registered to receive
    * event notifications from this <code>RowSet</code>.
    *
    * @param listener an object that has implemented the
    *     <code>javax.sql.RowSetListener</code> interface and wants to be notified
    *     of any events that occur on this <code>RowSet</code> object; May be
    *     null.
    * @see #removeRowSetListener
    */
    public void addRowSetListener(RowSetListener listener) {
        listeners.add(listener);
    }

    /**
    * Removes the designated object from this <code>RowSet</code> object's list of listeners.
    * If the given argument is not a registered listener, this method
    * does nothing.
    *
    *  <b>Note</b>: if the <code>RowSetListener</code> object is
    * <code>null</code>, this method silently discards the <code>null</code>
    * value.
    *
    * @param listener a <code>RowSetListener</code> object that is on the list
    *        of listeners for this <code>RowSet</code> object
    * @see #addRowSetListener
    */
    public void removeRowSetListener(RowSetListener listener) {
        listeners.remove(listener);
    }

    /**
     * Determine if instance of this class extends the RowSet interface.
     */
    private void checkforRowSetInterface() throws SQLException {
        if ((this instanceof javax.sql.RowSet) == false) {
            throw new SQLException("The class extending abstract class BaseRowSet " +
                "must implement javax.sql.RowSet or one of it's sub-interfaces.");
        }
    }

    /**
    * Notifies all of the listeners registered with this
    * <code>RowSet</code> object that its cursor has moved.
    * <P>
    * When an application calls a method to move the cursor,
    * that method moves the cursor and then calls this method
    * internally. An application <b>should</b> never invoke
    * this method directly.
    *
    * @throws SQLException if the class extending the <code>BaseRowSet</code>
    *     abstract class does not implement the <code>RowSet</code> interface or
    *     one of it's sub-interfaces.
    */
    protected void notifyCursorMoved() throws SQLException {
        checkforRowSetInterface();
        if (listeners.isEmpty() == false) {
            RowSetEvent event = new RowSetEvent((RowSet)this);
            for (RowSetListener rsl : listeners) {
                rsl.cursorMoved(event);
            }
        }
    }

    /**
    * Notifies all of the listeners registered with this <code>RowSet</code> object that
    * one of its rows has changed.
    * <P>
    * When an application calls a method that changes a row, such as
    * the <code>CachedRowSet</code> methods <code>insertRow</code>,
    * <code>updateRow</code>, or <code>deleteRow</code>,
    * that method calls <code>notifyRowChanged</code>
    * internally. An application <b>should</b> never invoke
    * this method directly.
    *
    * @throws SQLException if the class extending the <code>BaseRowSet</code>
    *     abstract class does not implement the <code>RowSet</code> interface or
    *     one of it's sub-interfaces.
    */
    protected void notifyRowChanged() throws SQLException {
        checkforRowSetInterface();
        if (listeners.isEmpty() == false) {
                RowSetEvent event = new RowSetEvent((RowSet)this);
                for (RowSetListener rsl : listeners) {
                    rsl.rowChanged(event);
                }
        }
    }

   /**
    * Notifies all of the listeners registered with this <code>RowSet</code>
    * object that its entire contents have changed.
    * <P>
    * When an application calls methods that change the entire contents
    * of the <code>RowSet</code> object, such as the <code>CachedRowSet</code> methods
    * <code>execute</code>, <code>populate</code>, <code>restoreOriginal</code>,
    * or <code>release</code>, that method calls <code>notifyRowSetChanged</code>
    * internally (either directly or indirectly). An application <b>should</b>
    * never invoke this method directly.
    *
    * @throws SQLException if the class extending the <code>BaseRowSet</code>
    *     abstract class does not implement the <code>RowSet</code> interface or
    *     one of it's sub-interfaces.
    */
    protected void notifyRowSetChanged() throws SQLException {
        checkforRowSetInterface();
        if (listeners.isEmpty() == false) {
                RowSetEvent event = new RowSetEvent((RowSet)this);
                for (RowSetListener rsl : listeners) {
                    rsl.rowSetChanged(event);
                }
        }
}

    /**
     * Retrieves the SQL query that is the command for this
     * <code>RowSet</code> object. The command property contains the query that
     * will be executed to populate this <code>RowSet</code> object.
     * <P>
     * The SQL query returned by this method is used by <code>RowSet</code> methods
     * such as <code>execute</code> and <code>populate</code>, which may be implemented
     * by any class that extends the <code>BaseRowSet</code> abstract class and
     * implements one or more of the standard JSR-114 <code>RowSet</code>
     * interfaces.
     * <P>
     * The command is used by the <code>RowSet</code> object's
     * reader to obtain a <code>ResultSet</code> object.  The reader then
     * reads the data from the <code>ResultSet</code> object and uses it to
     * to populate this <code>RowSet</code> object.
     * <P>
     * The default value for the <code>command</code> property is <code>null</code>.
     *
     * @return the <code>String</code> that is the value for this
     *         <code>RowSet</code> object's <code>command</code> property;
     *         may be <code>null</code>
     * @see #setCommand
     */
    public String getCommand() {
        return command;
    }

    /**
     * Sets this <code>RowSet</code> object's <code>command</code> property to
     * the given <code>String</code> object and clears the parameters, if any,
     * that were set for the previous command.
     * <P>
     * The <code>command</code> property may not be needed if the <code>RowSet</code>
     * object gets its data from a source that does not support commands,
     * such as a spreadsheet or other tabular file.
     * Thus, this property is optional and may be <code>null</code>.
     *
     * @param cmd a <code>String</code> object containing an SQL query
     *            that will be set as this <code>RowSet</code> object's command
     *            property; may be <code>null</code> but may not be an empty string
     * @throws SQLException if an empty string is provided as the command value
     * @see #getCommand
     */
    public void setCommand(String cmd) throws SQLException {
        // cmd equal to null or
        // cmd with length 0 (implies url =="")
        // are not independent events.

        if(cmd == null) {
           command = null;
        } else if (cmd.length() == 0) {
            throw new SQLException("Invalid command string detected. " +
            "Cannot be of length less than 0");
        } else {
            // "unbind" any parameters from any previous command.
            if(params == null){
                 throw new SQLException("Set initParams() before setCommand");
            }
            params.clear();
            command = cmd;
        }

    }

    /**
     * Retrieves the JDBC URL that this <code>RowSet</code> object's
     * <code>javax.sql.Reader</code> object uses to make a connection
     * with a relational database using a JDBC technology-enabled driver.
     *<P>
     * The <code>Url</code> property will be <code>null</code> if the underlying data
     * source is a non-SQL data source, such as a spreadsheet or an XML
     * data source.
     *
     * @return a <code>String</code> object that contains the JDBC URL
     *         used to establish the connection for this <code>RowSet</code>
     *         object; may be <code>null</code> (default value) if not set
     * @throws SQLException if an error occurs retrieving the URL value
     * @see #setUrl
     */
    public String getUrl() throws SQLException {
        return URL;
    }

    /**
     * Sets the Url property for this <code>RowSet</code> object
     * to the given <code>String</code> object and sets the dataSource name
     * property to <code>null</code>. The Url property is a
     * JDBC URL that is used when
     * the connection is created using a JDBC technology-enabled driver
     * ("JDBC driver") and the <code>DriverManager</code>.
     * The correct JDBC URL for the specific driver to be used can be found
     * in the driver documentation.  Although there are guidelines for how
     * a JDBC URL is formed,
     * a driver vendor can specify any <code>String</code> object except
     * one with a length of <code>0</code> (an empty string).
     * <P>
     * Setting the Url property is optional if connections are established using
     * a <code>DataSource</code> object instead of the <code>DriverManager</code>.
     * The driver will use either the URL property or the
     * dataSourceName property to create a connection, whichever was
     * specified most recently. If an application uses a JDBC URL, it
     * must load a JDBC driver that accepts the JDBC URL before it uses the
     * <code>RowSet</code> object to connect to a database.  The <code>RowSet</code>
     * object will use the URL internally to create a database connection in order
     * to read or write data.
     *
     * @param url a <code>String</code> object that contains the JDBC URL
     *     that will be used to establish the connection to a database for this
     *     <code>RowSet</code> object; may be <code>null</code> but must not
     *     be an empty string
     * @throws SQLException if an error occurs setting the Url property or the
     *     parameter supplied is a string with a length of <code>0</code> (an
     *     empty string)
     * @see #getUrl
     */
    public void setUrl(String url) throws SQLException {
        if(url == null) {
           url = null;
        } else if (url.length() < 1) {
            throw new SQLException("Invalid url string detected. " +
            "Cannot be of length less than 1");
        } else {
            URL = url;
        }

        dataSource = null;

    }

    /**
     * Returns the logical name that when supplied to a naming service
     * that uses the Java Naming and Directory Interface (JNDI) API, will
     * retrieve a <code>javax.sql.DataSource</code> object. This
     * <code>DataSource</code> object can be used to establish a connection
     * to the data source that it represents.
     * <P>
     * Users should set either the url or the data source name property.
     * The driver will use the property set most recently to establish a
     * connection.
     *
     * @return a <code>String</code> object that identifies the
     *         <code>DataSource</code> object to be used for making a
     *         connection; if no logical name has been set, <code>null</code>
     *         is returned.
     * @see #setDataSourceName
     */
    public String getDataSourceName() {
        return dataSource;
    }


    /**
     * Sets the <code>DataSource</code> name property for this <code>RowSet</code>
     * object to the given logical name and sets this <code>RowSet</code> object's
     * Url property to <code>null</code>. The name must have been bound to a
     * <code>DataSource</code> object in a JNDI naming service so that an
     * application can do a lookup using that name to retrieve the
     * <code>DataSource</code> object bound to it. The <code>DataSource</code>
     * object can then be used to establish a connection to the data source it
     * represents.
     * <P>
     * Users should set either the Url property or the dataSourceName property.
     * If both properties are set, the driver will use the property set most recently.
     *
     * @param name a <code>String</code> object with the name that can be supplied
     *     to a naming service based on JNDI technology to retrieve the
     *     <code>DataSource</code> object that can be used to get a connection;
     *     may be <code>null</code> but must not be an empty string
     * @throws SQLException if an empty string is provided as the <code>DataSource</code>
     *    name
     * @see #getDataSourceName
     */
    public void setDataSourceName(String name) throws SQLException {

        if (name == null) {
            dataSource = null;
        } else if (name.isEmpty()) {
           throw new SQLException("DataSource name cannot be empty string");
        } else {
           dataSource = name;
        }

        URL = null;
    }

    /**
     * Returns the user name used to create a database connection.  Because it
     * is not serialized, the username property is set at runtime before
     * calling the method <code>execute</code>.
     *
     * @return the <code>String</code> object containing the user name that
     *         is supplied to the data source to create a connection; may be
     *         <code>null</code> (default value) if not set
     * @see #setUsername
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username property for this <code>RowSet</code> object
     * to the given user name. Because it
     * is not serialized, the username property is set at run time before
     * calling the method <code>execute</code>.
     *
     * @param name the <code>String</code> object containing the user name that
     *     is supplied to the data source to create a connection. It may be null.
     * @see #getUsername
     */
    public void setUsername(String name) {
        if(name == null)
        {
           username = null;
        } else {
           username = name;
        }
    }

    /**
     * Returns the password used to create a database connection for this
     * <code>RowSet</code> object.  Because the password property is not
     * serialized, it is set at run time before calling the method
     * <code>execute</code>. The default value is <code>null</code>
     *
     * @return the <code>String</code> object that represents the password
     *         that must be supplied to the database to create a connection
     * @see #setPassword
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password used to create a database connection for this
     * <code>RowSet</code> object to the given <code>String</code>
     * object.  Because the password property is not
     * serialized, it is set at run time before calling the method
     * <code>execute</code>.
     *
     * @param pass the <code>String</code> object that represents the password
     *     that is supplied to the database to create a connection. It may be
     *     null.
     * @see #getPassword
     */
    public void setPassword(String pass) {
        if(pass == null)
        {
           password = null;
        } else {
           password = pass;
        }
    }

    /**
     * Sets the type for this <code>RowSet</code> object to the specified type.
     * The default type is <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>.
     *
     * @param type one of the following constants:
     *             <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *             <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *             <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @throws SQLException if the parameter supplied is not one of the
     *         following constants:
     *          <code>ResultSet.TYPE_FORWARD_ONLY</code> or
     *          <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>
     *          <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @see #getConcurrency
     * @see #getType
     */
    public void setType(int type) throws SQLException {

        if ((type != ResultSet.TYPE_FORWARD_ONLY) &&
           (type != ResultSet.TYPE_SCROLL_INSENSITIVE) &&
           (type != ResultSet.TYPE_SCROLL_SENSITIVE)) {
                throw new SQLException("Invalid type of RowSet set. Must be either " +
                "ResultSet.TYPE_FORWARD_ONLY or ResultSet.TYPE_SCROLL_INSENSITIVE " +
                "or ResultSet.TYPE_SCROLL_SENSITIVE.");
        }
        this.rowSetType = type;
    }

    /**
     * Returns the type of this <code>RowSet</code> object. The type is initially
     * determined by the statement that created the <code>RowSet</code> object.
     * The <code>RowSet</code> object can call the method
     * <code>setType</code> at any time to change its
     * type.  The default is <code>TYPE_SCROLL_INSENSITIVE</code>.
     *
     * @return the type of this JDBC <code>RowSet</code>
     *         object, which must be one of the following:
     *         <code>ResultSet.TYPE_FORWARD_ONLY</code>,
     *         <code>ResultSet.TYPE_SCROLL_INSENSITIVE</code>, or
     *         <code>ResultSet.TYPE_SCROLL_SENSITIVE</code>
     * @throws SQLException if an error occurs getting the type of
     *     of this <code>RowSet</code> object
     * @see #setType
     */
    public int getType() throws SQLException {
        return rowSetType;
    }

    /**
     * Sets the concurrency for this <code>RowSet</code> object to
     * the specified concurrency. The default concurrency for any <code>RowSet</code>
     * object (connected or disconnected) is <code>ResultSet.CONCUR_UPDATABLE</code>,
     * but this method may be called at any time to change the concurrency.
     *
     * @param concurrency one of the following constants:
     *                    <code>ResultSet.CONCUR_READ_ONLY</code> or
     *                    <code>ResultSet.CONCUR_UPDATABLE</code>
     * @throws SQLException if the parameter supplied is not one of the
     *         following constants:
     *          <code>ResultSet.CONCUR_UPDATABLE</code> or
     *          <code>ResultSet.CONCUR_READ_ONLY</code>
     * @see #getConcurrency
     * @see #isReadOnly
     */
    public void setConcurrency(int concurrency) throws SQLException {

        if((concurrency != ResultSet.CONCUR_READ_ONLY) &&
           (concurrency != ResultSet.CONCUR_UPDATABLE)) {
                throw new SQLException("Invalid concurrency set. Must be either " +
                "ResultSet.CONCUR_READ_ONLY or ResultSet.CONCUR_UPDATABLE.");
        }
        this.concurrency = concurrency;
    }

    /**
     * Returns a <code>boolean</code> indicating whether this
     * <code>RowSet</code> object is read-only.
     * Any attempts to update a read-only <code>RowSet</code> object will result in an
     * <code>SQLException</code> being thrown. By default,
     * rowsets are updatable if updates are possible.
     *
     * @return <code>true</code> if this <code>RowSet</code> object
     *         cannot be updated; <code>false</code> otherwise
     * @see #setConcurrency
     * @see #setReadOnly
     */
    public boolean isReadOnly() {
        return readOnly;
    };

    /**
     * Sets this <code>RowSet</code> object's readOnly  property to the given <code>boolean</code>.
     *
     * @param value <code>true</code> to indicate that this
     *              <code>RowSet</code> object is read-only;
     *              <code>false</code> to indicate that it is updatable
     */
    public void setReadOnly(boolean value) {
        readOnly = value;
    }

    /**
     * Returns the transaction isolation property for this
     * <code>RowSet</code> object's connection. This property represents
     * the transaction isolation level requested for use in transactions.
     * <P>
     * For <code>RowSet</code> implementations such as
     * the <code>CachedRowSet</code> that operate in a disconnected environment,
     * the <code>SyncProvider</code> object
     * offers complementary locking and data integrity options. The
     * options described below are pertinent only to connected <code>RowSet</code>
     * objects (<code>JdbcRowSet</code> objects).
     *
     * @return one of the following constants:
     *         <code>Connection.TRANSACTION_NONE</code>,
     *         <code>Connection.TRANSACTION_READ_UNCOMMITTED</code>,
     *         <code>Connection.TRANSACTION_READ_COMMITTED</code>,
     *         <code>Connection.TRANSACTION_REPEATABLE_READ</code>, or
     *         <code>Connection.TRANSACTION_SERIALIZABLE</code>
     * @see javax.sql.rowset.spi.SyncFactory
     * @see javax.sql.rowset.spi.SyncProvider
     * @see #setTransactionIsolation

     */
    public int getTransactionIsolation() {
        return isolation;
    };

    /**
     * Sets the transaction isolation property for this JDBC <code>RowSet</code> object to the given
     * constant. The DBMS will use this transaction isolation level for
     * transactions if it can.
     * <p>
     * For <code>RowSet</code> implementations such as
     * the <code>CachedRowSet</code> that operate in a disconnected environment,
     * the <code>SyncProvider</code> object being used
     * offers complementary locking and data integrity options. The
     * options described below are pertinent only to connected <code>RowSet</code>
     * objects (<code>JdbcRowSet</code> objects).
     *
     * @param level one of the following constants, listed in ascending order:
     *              <code>Connection.TRANSACTION_NONE</code>,
     *              <code>Connection.TRANSACTION_READ_UNCOMMITTED</code>,
     *              <code>Connection.TRANSACTION_READ_COMMITTED</code>,
     *              <code>Connection.TRANSACTION_REPEATABLE_READ</code>, or
     *              <code>Connection.TRANSACTION_SERIALIZABLE</code>
     * @throws SQLException if the given parameter is not one of the Connection
     *          constants
     * @see javax.sql.rowset.spi.SyncFactory
     * @see javax.sql.rowset.spi.SyncProvider
     * @see #getTransactionIsolation
     */
    public void setTransactionIsolation(int level) throws SQLException {
        if ((level != Connection.TRANSACTION_NONE) &&
           (level != Connection.TRANSACTION_READ_COMMITTED) &&
           (level != Connection.TRANSACTION_READ_UNCOMMITTED) &&
           (level != Connection.TRANSACTION_REPEATABLE_READ) &&
           (level != Connection.TRANSACTION_SERIALIZABLE))
            {
                throw new SQLException("Invalid transaction isolation set. Must " +
                "be either " +
                "Connection.TRANSACTION_NONE or " +
                "Connection.TRANSACTION_READ_UNCOMMITTED or " +
                "Connection.TRANSACTION_READ_COMMITTED or " +
                "Connection.TRANSACTION_REPEATABLE_READ or " +
                "Connection.TRANSACTION_SERIALIZABLE");
            }
        this.isolation = level;
    }

    /**
     * Retrieves the type map associated with the <code>Connection</code>
     * object for this <code>RowSet</code> object.
     * <P>
     * Drivers that support the JDBC 3.0 API will create
     * <code>Connection</code> objects with an associated type map.
     * This type map, which is initially empty, can contain one or more
     * fully-qualified SQL names and <code>Class</code> objects indicating
     * the class to which the named SQL value will be mapped. The type mapping
     * specified in the connection's type map is used for custom type mapping
     * when no other type map supersedes it.
     * <p>
     * If a type map is explicitly supplied to a method that can perform
     * custom mapping, that type map supersedes the connection's type map.
     *
     * @return the <code>java.util.Map</code> object that is the type map
     *         for this <code>RowSet</code> object's connection
     */
    public java.util.Map<String,Class<?>> getTypeMap() {
        return map;
    }

    /**
     * Installs the given <code>java.util.Map</code> object as the type map
     * associated with the <code>Connection</code> object for this
     * <code>RowSet</code> object.  The custom mapping indicated in
     * this type map will be used unless a different type map is explicitly
     * supplied to a method, in which case the type map supplied will be used.
     *
     * @param map a <code>java.util.Map</code> object that contains the
     *     mapping from SQL type names for user defined types (UDT) to classes in
     *     the Java programming language.  Each entry in the <code>Map</code>
     *     object consists of the fully qualified SQL name of a UDT and the
     *     <code>Class</code> object for the <code>SQLData</code> implementation
     *     of that UDT. May be <code>null</code>.
     */
    public void setTypeMap(java.util.Map<String,Class<?>> map) {
        this.map = map;
    }

    /**
     * Retrieves the maximum number of bytes that can be used for a column
     * value in this <code>RowSet</code> object.
     * This limit applies only to columns that hold values of the
     * following types:  <code>BINARY</code>, <code>VARBINARY</code>,
     * <code>LONGVARBINARY</code>, <code>CHAR</code>, <code>VARCHAR</code>,
     * and <code>LONGVARCHAR</code>.  If the limit is exceeded, the excess
     * data is silently discarded.
     *
     * @return an <code>int</code> indicating the current maximum column size
     *     limit; zero means that there is no limit
     * @throws SQLException if an error occurs internally determining the
     *    maximum limit of the column size
     */
    public int getMaxFieldSize() throws SQLException {
        return maxFieldSize;
    }

    /**
     * Sets the maximum number of bytes that can be used for a column
     * value in this <code>RowSet</code> object to the given number.
     * This limit applies only to columns that hold values of the
     * following types:  <code>BINARY</code>, <code>VARBINARY</code>,
     * <code>LONGVARBINARY</code>, <code>CHAR</code>, <code>VARCHAR</code>,
     * and <code>LONGVARCHAR</code>.  If the limit is exceeded, the excess
     * data is silently discarded. For maximum portability, it is advisable to
     * use values greater than 256.
     *
     * @param max an <code>int</code> indicating the new maximum column size
     *     limit; zero means that there is no limit
     * @throws SQLException if (1) an error occurs internally setting the
     *     maximum limit of the column size or (2) a size of less than 0 is set
     */
    public void setMaxFieldSize(int max) throws SQLException {
        if (max < 0) {
            throw new SQLException("Invalid max field size set. Cannot be of " +
            "value: " + max);
        }
        maxFieldSize = max;
    }

    /**
     * Retrieves the maximum number of rows that this <code>RowSet</code> object may contain. If
     * this limit is exceeded, the excess rows are silently dropped.
     *
     * @return an <code>int</code> indicating the current maximum number of
     *     rows; zero means that there is no limit
     * @throws SQLException if an error occurs internally determining the
     *     maximum limit of rows that a <code>Rowset</code> object can contain
     */
    public int getMaxRows() throws SQLException {
        return maxRows;
    }

    /**
     * Sets the maximum number of rows that this <code>RowSet</code> object may contain to
     * the given number. If this limit is exceeded, the excess rows are
     * silently dropped.
     *
     * @param max an <code>int</code> indicating the current maximum number
     *     of rows; zero means that there is no limit
     * @throws SQLException if an error occurs internally setting the
     *     maximum limit on the number of rows that a JDBC <code>RowSet</code> object
     *     can contain; or if <i>max</i> is less than <code>0</code>; or
     *     if <i>max</i> is less than the <code>fetchSize</code> of the
     *     <code>RowSet</code>
     */
    public void setMaxRows(int max) throws SQLException {
        if (max < 0) {
            throw new SQLException("Invalid max row size set. Cannot be of " +
                "value: " + max);
        } else if (max < this.getFetchSize()) {
            throw new SQLException("Invalid max row size set. Cannot be less " +
                "than the fetchSize.");
        }
        this.maxRows = max;
    }

    /**
     * Sets to the given <code>boolean</code> whether or not the driver will
     * scan for escape syntax and do escape substitution before sending SQL
     * statements to the database. The default is for the driver to do escape
     * processing.
     * <P>
     * Note: Since <code>PreparedStatement</code> objects have usually been
     * parsed prior to making this call, disabling escape processing for
     * prepared statements will likely have no effect.
     *
     * @param enable <code>true</code> to enable escape processing;
     *     <code>false</code> to disable it
     * @throws SQLException if an error occurs setting the underlying JDBC
     * technology-enabled driver to process the escape syntax
     */
    public void setEscapeProcessing(boolean enable) throws SQLException {
        escapeProcessing = enable;
    }

    /**
     * Retrieves the maximum number of seconds the driver will wait for a
     * query to execute. If the limit is exceeded, an <code>SQLException</code>
     * is thrown.
     *
     * @return the current query timeout limit in seconds; zero means that
     *     there is no limit
     * @throws SQLException if an error occurs in determining the query
     *     time-out value
     */
    public int getQueryTimeout() throws SQLException {
        return queryTimeout;
    }

    /**
     * Sets to the given number the maximum number of seconds the driver will
     * wait for a query to execute. If the limit is exceeded, an
     * <code>SQLException</code> is thrown.
     *
     * @param seconds the new query time-out limit in seconds; zero means that
     *     there is no limit; must not be less than zero
     * @throws SQLException if an error occurs setting the query
     *     time-out or if the query time-out value is less than 0
     */
    public void setQueryTimeout(int seconds) throws SQLException {
        if (seconds < 0) {
            throw new SQLException("Invalid query timeout value set. Cannot be " +
            "of value: " + seconds);
        }
        this.queryTimeout = seconds;
    }

    /**
     * Retrieves a <code>boolean</code> indicating whether rows marked
     * for deletion appear in the set of current rows.
     * The default value is <code>false</code>.
     * <P>
     * Note: Allowing deleted rows to remain visible complicates the behavior
     * of some of the methods.  However, most <code>RowSet</code> object users
     * can simply ignore this extra detail because only sophisticated
     * applications will likely want to take advantage of this feature.
     *
     * @return <code>true</code> if deleted rows are visible;
     *         <code>false</code> otherwise
     * @throws SQLException if an error occurs determining if deleted rows
     * are visible or not
     * @see #setShowDeleted
     */
    public boolean getShowDeleted() throws SQLException {
        return showDeleted;
    }

    /**
     * Sets the property <code>showDeleted</code> to the given
     * <code>boolean</code> value, which determines whether
     * rows marked for deletion appear in the set of current rows.
     *
     * @param value <code>true</code> if deleted rows should be shown;
     *     <code>false</code> otherwise
     * @throws SQLException if an error occurs setting whether deleted
     *     rows are visible or not
     * @see #getShowDeleted
     */
    public void setShowDeleted(boolean value) throws SQLException {
        showDeleted = value;
    }

    /**
     * Ascertains whether escape processing is enabled for this
     * <code>RowSet</code> object.
     *
     * @return <code>true</code> if escape processing is turned on;
     *         <code>false</code> otherwise
     * @throws SQLException if an error occurs determining if escape
     *     processing is enabled or not or if the internal escape
     *     processing trigger has not been enabled
     */
    public boolean getEscapeProcessing() throws SQLException {
        return escapeProcessing;
    }

    /**
     * Gives the driver a performance hint as to the direction in
     * which the rows in this <code>RowSet</code> object will be
     * processed.  The driver may ignore this hint.
     * <P>
     * A <code>RowSet</code> object inherits the default properties of the
     * <code>ResultSet</code> object from which it got its data.  That
     * <code>ResultSet</code> object's default fetch direction is set by
     * the <code>Statement</code> object that created it.
     * <P>
     * This method applies to a <code>RowSet</code> object only while it is
     * connected to a database using a JDBC driver.
     * <p>
     * A <code>RowSet</code> object may use this method at any time to change
     * its setting for the fetch direction.
     *
     * @param direction one of <code>ResultSet.FETCH_FORWARD</code>,
     *                  <code>ResultSet.FETCH_REVERSE</code>, or
     *                  <code>ResultSet.FETCH_UNKNOWN</code>
     * @throws SQLException if (1) the <code>RowSet</code> type is
     *     <code>TYPE_FORWARD_ONLY</code> and the given fetch direction is not
     *     <code>FETCH_FORWARD</code> or (2) the given fetch direction is not
     *     one of the following:
     *        ResultSet.FETCH_FORWARD,
     *        ResultSet.FETCH_REVERSE, or
     *        ResultSet.FETCH_UNKNOWN
     * @see #getFetchDirection
     */
    public void setFetchDirection(int direction) throws SQLException {
        // Changed the condition checking to the below as there were two
        // conditions that had to be checked
        // 1. RowSet is TYPE_FORWARD_ONLY and direction is not FETCH_FORWARD
        // 2. Direction is not one of the valid values

        if (((getType() == ResultSet.TYPE_FORWARD_ONLY) && (direction != ResultSet.FETCH_FORWARD)) ||
            ((direction != ResultSet.FETCH_FORWARD) &&
            (direction != ResultSet.FETCH_REVERSE) &&
            (direction != ResultSet.FETCH_UNKNOWN))) {
            throw new SQLException("Invalid Fetch Direction");
        }
        fetchDir = direction;
    }

    /**
     * Retrieves this <code>RowSet</code> object's current setting for the
     * fetch direction. The default type is <code>ResultSet.FETCH_FORWARD</code>
     *
     * @return one of <code>ResultSet.FETCH_FORWARD</code>,
     *                  <code>ResultSet.FETCH_REVERSE</code>, or
     *                  <code>ResultSet.FETCH_UNKNOWN</code>
     * @throws SQLException if an error occurs in determining the
     *     current fetch direction for fetching rows
     * @see #setFetchDirection
     */
    public int getFetchDirection() throws SQLException {

        //Added the following code to throw a
        //SQL Exception if the fetchDir is not
        //set properly.Bug id:4914155

        // This checking is not necessary!

        /*
         if((fetchDir != ResultSet.FETCH_FORWARD) &&
           (fetchDir != ResultSet.FETCH_REVERSE) &&
           (fetchDir != ResultSet.FETCH_UNKNOWN)) {
            throw new SQLException("Fetch Direction Invalid");
         }
         */
        return (fetchDir);
    }

    /**
     * Sets the fetch size for this <code>RowSet</code> object to the given number of
     * rows.  The fetch size gives a JDBC technology-enabled driver ("JDBC driver")
     * a hint as to the
     * number of rows that should be fetched from the database when more rows
     * are needed for this <code>RowSet</code> object. If the fetch size specified
     * is zero, the driver ignores the value and is free to make its own best guess
     * as to what the fetch size should be.
     * <P>
     * A <code>RowSet</code> object inherits the default properties of the
     * <code>ResultSet</code> object from which it got its data.  That
     * <code>ResultSet</code> object's default fetch size is set by
     * the <code>Statement</code> object that created it.
     * <P>
     * This method applies to a <code>RowSet</code> object only while it is
     * connected to a database using a JDBC driver.
     * For connected <code>RowSet</code> implementations such as
     * <code>JdbcRowSet</code>, this method has a direct and immediate effect
     * on the underlying JDBC driver.
     * <P>
     * A <code>RowSet</code> object may use this method at any time to change
     * its setting for the fetch size.
     * <p>
     * For <code>RowSet</code> implementations such as
     * <code>CachedRowSet</code>, which operate in a disconnected environment,
     * the <code>SyncProvider</code> object being used
     * may leverage the fetch size to poll the data source and
     * retrieve a number of rows that do not exceed the fetch size and that may
     * form a subset of the actual rows returned by the original query. This is
     * an implementation variance determined by the specific <code>SyncProvider</code>
     * object employed by the disconnected <code>RowSet</code> object.
     *
     * @param rows the number of rows to fetch; <code>0</code> to let the
     *        driver decide what the best fetch size is; must not be less
     *        than <code>0</code> or more than the maximum number of rows
     *        allowed for this <code>RowSet</code> object (the number returned
     *        by a call to the method {@link #getMaxRows})
     * @throws SQLException if the specified fetch size is less than <code>0</code>
     *        or more than the limit for the maximum number of rows
     * @see #getFetchSize
     */
    public void setFetchSize(int rows) throws SQLException {
        //Added this checking as maxRows can be 0 when this function is called
        //maxRows = 0 means rowset can hold any number of rows, os this checking
        // is needed to take care of this condition.
        if (getMaxRows() == 0 && rows >= 0)  {
            fetchSize = rows;
            return;
        }
        if ((rows < 0) || (rows > getMaxRows())) {
            throw new SQLException("Invalid fetch size set. Cannot be of " +
            "value: " + rows);
        }
        fetchSize = rows;
    }

    /**
     * Returns the fetch size for this <code>RowSet</code> object. The default
     * value is zero.
     *
     * @return the number of rows suggested as the fetch size when this <code>RowSet</code> object
     *     needs more rows from the database
     * @throws SQLException if an error occurs determining the number of rows in the
     *     current fetch size
     * @see #setFetchSize
     */
    public int getFetchSize() throws SQLException {
        return fetchSize;
    }

    /**
     * Returns the concurrency for this <code>RowSet</code> object.
     * The default is <code>CONCUR_UPDATABLE</code> for both connected and
     * disconnected <code>RowSet</code> objects.
     * <P>
     * An application can call the method <code>setConcurrency</code> at any time
     * to change a <code>RowSet</code> object's concurrency.
     *
     * @return the concurrency type for this <code>RowSet</code>
     *     object, which must be one of the following:
     *     <code>ResultSet.CONCUR_READ_ONLY</code> or
     *     <code>ResultSet.CONCUR_UPDATABLE</code>
     * @throws SQLException if an error occurs getting the concurrency
     *     of this <code>RowSet</code> object
     * @see #setConcurrency
     * @see #isReadOnly
     */
    public int getConcurrency() throws SQLException {
        return concurrency;
    }

    //-----------------------------------------------------------------------
    // Parameters
    //-----------------------------------------------------------------------

    /**
     * Checks the given index to see whether it is less than <code>1</code> and
     * throws an <code>SQLException</code> object if it is.
     * <P>
     * This method is called by many methods internally; it is never
     * called by an application directly.
     *
     * @param idx an <code>int</code> indicating which parameter is to be
     *     checked; the first parameter is <code>1</code>
     * @throws SQLException if the parameter is less than <code>1</code>
     */
    private void checkParamIndex(int idx) throws SQLException {
        if ((idx < 1)) {
            throw new SQLException("Invalid Parameter Index");
        }
    }

    //---------------------------------------------------------------------
    // setter methods for setting the parameters in a <code>RowSet</code> object's command
    //---------------------------------------------------------------------

    /**
     * Sets the designated parameter to SQL <code>NULL</code>.
     * Note that the parameter's SQL type must be specified using one of the
         * type codes defined in <code>java.sql.Types</code>.  This SQL type is
     * specified in the second parameter.
     * <p>
     * Note that the second parameter tells the DBMS the data type of the value being
     * set to <code>NULL</code>. Some DBMSs require this information, so it is required
     * in order to make code more portable.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <P>
     * Calls made to the method <code>getParams</code> after this version of
     * <code>setNull</code>
     * has been called will return an <code>Object</code> array containing the parameter values that
     * have been set.  In that array, the element that represents the values
     * set with this method will itself be an array. The first element of that array
     * is <code>null</code>.
     * The second element is the value set for <i>sqlType</i>.
     * The parameter number is indicated by an element's position in the array
     * returned by the method <code>getParams</code>,
     * with the first element being the value for the first placeholder parameter, the
     * second element being the value for the second placeholder parameter, and so on.
     * In other words, if the second placeholder parameter is being set to
     * <code>null</code>, the array containing it will be the second element in
     * the array returned by <code>getParams</code>.
     * <P>
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param sqlType an <code>int</code> that is one of the SQL type codes
     *        defined in the class {@link java.sql.Types}. If a non-standard
     *        <i>sqlType</i> is supplied, this method will not throw a
     *        <code>SQLException</code>. This allows implicit support for
     *        non-standard SQL types.
     * @throws SQLException if a database access error occurs or the given
     *        parameter index is out of bounds
     * @see #getParams
     */
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        Object nullVal[];
        checkParamIndex(parameterIndex);

        nullVal = new Object[2];
        nullVal[0] = null;
        nullVal[1] = Integer.valueOf(sqlType);

       if (params == null){
            throw new SQLException("Set initParams() before setNull");
       }

        params.put(Integer.valueOf(parameterIndex - 1), nullVal);
    }

    /**
     * Sets the designated parameter to SQL <code>NULL</code>.
     *
     * Although this version of the  method <code>setNull</code> is intended
     * for user-defined
     * and <code>REF</code> parameters, this method may be used to set a null
     * parameter for any JDBC type. The following are user-defined types:
     * <code>STRUCT</code>, <code>DISTINCT</code>, and <code>JAVA_OBJECT</code>,
     * and named array types.
     *
     * <P><B>Note:</B> To be portable, applications must give the
     * SQL type code and the fully qualified SQL type name when specifying
     * a <code>NULL</code> user-defined or <code>REF</code> parameter.
     * In the case of a user-defined type, the name is the type name of
     * the parameter itself.  For a <code>REF</code> parameter, the name is
     * the type name of the referenced type.  If a JDBC technology-enabled
     * driver does not need the type code or type name information,
     * it may ignore it.
     * <P>
     * If the parameter does not have a user-defined or <code>REF</code> type,
     * the given <code>typeName</code> parameter is ignored.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <P>
     * Calls made to the method <code>getParams</code> after this version of
     * <code>setNull</code>
     * has been called will return an <code>Object</code> array containing the parameter values that
     * have been set.  In that array, the element that represents the values
     * set with this method will itself be an array. The first element of that array
     * is <code>null</code>.
     * The second element is the value set for <i>sqlType</i>, and the third
     * element is the value set for <i>typeName</i>.
     * The parameter number is indicated by an element's position in the array
     * returned by the method <code>getParams</code>,
     * with the first element being the value for the first placeholder parameter, the
     * second element being the value for the second placeholder parameter, and so on.
     * In other words, if the second placeholder parameter is being set to
     * <code>null</code>, the array containing it will be the second element in
     * the array returned by <code>getParams</code>.
     * <P>
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param sqlType a value from <code>java.sql.Types</code>
     * @param typeName the fully qualified name of an SQL user-defined type,
     *                 which is ignored if the parameter is not a user-defined
     *                 type or <code>REF</code> value
     * @throws SQLException if an error occurs or the given parameter index
     *            is out of bounds
     * @see #getParams
     */
    public void setNull(int parameterIndex, int sqlType, String typeName)
        throws SQLException {

        Object nullVal[];
        checkParamIndex(parameterIndex);

        nullVal = new Object[3];
        nullVal[0] = null;
        nullVal[1] = Integer.valueOf(sqlType);
        nullVal[2] = typeName;

       if(params == null){
            throw new SQLException("Set initParams() before setNull");
       }

        params.put(Integer.valueOf(parameterIndex - 1), nullVal);
    }


    /**
     * Sets the designated parameter to the given <code>boolean</code> in the
     * Java programming language.  The driver converts this to an SQL
     * <code>BIT</code> value when it sends it to the database.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code>, <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <p>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the parameter value
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        checkParamIndex(parameterIndex);

       if(params == null){
            throw new SQLException("Set initParams() before setNull");
       }

        params.put(Integer.valueOf(parameterIndex - 1), Boolean.valueOf(x));
    }

    /**
     * Sets the designated parameter to the given <code>byte</code> in the Java
     * programming language.  The driver converts this to an SQL
     * <code>TINYINT</code> value when it sends it to the database.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <p>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the parameter value
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setByte(int parameterIndex, byte x) throws SQLException {
        checkParamIndex(parameterIndex);

       if(params == null){
            throw new SQLException("Set initParams() before setByte");
       }

        params.put(Integer.valueOf(parameterIndex - 1), Byte.valueOf(x));
    }

    /**
     * Sets the designated parameter to the given <code>short</code> in the
     * Java programming language.  The driver converts this to an SQL
     * <code>SMALLINT</code> value when it sends it to the database.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <p>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the parameter value
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setShort(int parameterIndex, short x) throws SQLException {
        checkParamIndex(parameterIndex);

        if(params == null){
             throw new SQLException("Set initParams() before setShort");
        }

        params.put(Integer.valueOf(parameterIndex - 1), Short.valueOf(x));
    }

    /**
     * Sets the designated parameter to an <code>int</code> in the Java
     * programming language.  The driver converts this to an SQL
     * <code>INTEGER</code> value when it sends it to the database.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the parameter value
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setInt(int parameterIndex, int x) throws SQLException {
        checkParamIndex(parameterIndex);
        if(params == null){
             throw new SQLException("Set initParams() before setInt");
        }
        params.put(Integer.valueOf(parameterIndex - 1), Integer.valueOf(x));
    }

    /**
     * Sets the designated parameter to the given <code>long</code> in the Java
     * programming language.  The driver converts this to an SQL
     * <code>BIGINT</code> value when it sends it to the database.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the parameter value
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setLong(int parameterIndex, long x) throws SQLException {
        checkParamIndex(parameterIndex);
        if(params == null){
             throw new SQLException("Set initParams() before setLong");
        }
        params.put(Integer.valueOf(parameterIndex - 1), Long.valueOf(x));
    }

    /**
     * Sets the designated parameter to the given <code>float</code> in the
     * Java programming language.  The driver converts this to an SQL
     * <code>FLOAT</code> value when it sends it to the database.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the parameter value
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setFloat(int parameterIndex, float x) throws SQLException {
        checkParamIndex(parameterIndex);
        if(params == null){
             throw new SQLException("Set initParams() before setFloat");
        }
        params.put(Integer.valueOf(parameterIndex - 1), Float.valueOf(x));
    }

    /**
     * Sets the designated parameter to the given <code>double</code> in the
     * Java programming language.  The driver converts this to an SQL
     * <code>DOUBLE</code> value when it sends it to the database.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the parameter value
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setDouble(int parameterIndex, double x) throws SQLException {
        checkParamIndex(parameterIndex);
        if(params == null){
             throw new SQLException("Set initParams() before setDouble");
        }
        params.put(Integer.valueOf(parameterIndex - 1), Double.valueOf(x));
    }

    /**
     * Sets the designated parameter to the given
     * <code>java.lang.BigDecimal</code> value.  The driver converts this to
     * an SQL <code>NUMERIC</code> value when it sends it to the database.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * Note: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the parameter value
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setBigDecimal(int parameterIndex, java.math.BigDecimal x) throws SQLException {
        checkParamIndex(parameterIndex);
        if(params == null){
             throw new SQLException("Set initParams() before setBigDecimal");
        }
        params.put(Integer.valueOf(parameterIndex - 1), x);
    }

    /**
     * Sets the designated parameter to the given <code>String</code>
     * value.  The driver converts this to an SQL
     * <code>VARCHAR</code> or <code>LONGVARCHAR</code> value
     * (depending on the argument's size relative to the driver's limits
     * on <code>VARCHAR</code> values) when it sends it to the database.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <p>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the parameter value
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setString(int parameterIndex, String x) throws SQLException {
        checkParamIndex(parameterIndex);
        if(params == null){
             throw new SQLException("Set initParams() before setString");
        }
        params.put(Integer.valueOf(parameterIndex - 1), x);
    }

    /**
     * Sets the designated parameter to the given array of bytes.
     * The driver converts this to an SQL
     * <code>VARBINARY</code> or <code>LONGVARBINARY</code> value
     * (depending on the argument's size relative to the driver's limits
     * on <code>VARBINARY</code> values) when it sends it to the database.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <p>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the parameter value
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setBytes(int parameterIndex, byte x[]) throws SQLException {
        checkParamIndex(parameterIndex);
        if(params == null){
             throw new SQLException("Set initParams() before setBytes");
        }
        params.put(Integer.valueOf(parameterIndex - 1), x);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Date</code>
     * value. The driver converts this to an SQL
     * <code>DATE</code> value when it sends it to the database.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <P>
     * Calls made to the method <code>getParams</code> after this version
     * of <code>setDate</code>
     * has been called will return an array with the value to be set for
     * placeholder parameter number <i>parameterIndex</i> being the <code>Date</code>
     * object supplied as the second parameter.
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the parameter value
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setDate(int parameterIndex, java.sql.Date x) throws SQLException {
        checkParamIndex(parameterIndex);

        if(params == null){
             throw new SQLException("Set initParams() before setDate");
        }
        params.put(Integer.valueOf(parameterIndex - 1), x);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Time</code>
     * value.  The driver converts this to an SQL <code>TIME</code> value
     * when it sends it to the database.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <P>
     * Calls made to the method <code>getParams</code> after this version
     * of the method <code>setTime</code>
     * has been called will return an array of the parameters that have been set.
     * The parameter to be set for parameter placeholder number <i>parameterIndex</i>
     * will be the <code>Time</code> object that was set as the second parameter
     * to this method.
     * <P>
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x a <code>java.sql.Time</code> object, which is to be set as the value
     *              for placeholder parameter <i>parameterIndex</i>
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setTime(int parameterIndex, java.sql.Time x) throws SQLException {
        checkParamIndex(parameterIndex);
        if(params == null){
             throw new SQLException("Set initParams() before setTime");
        }

        params.put(Integer.valueOf(parameterIndex - 1), x);
    }

    /**
     * Sets the designated parameter to the given
     * <code>java.sql.Timestamp</code> value.
     * The driver converts this to an SQL <code>TIMESTAMP</code> value when it
     * sends it to the database.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <P>
     * Calls made to the method <code>getParams</code> after this version of
     * <code>setTimestamp</code>
     * has been called will return an array with the value for parameter placeholder
     * number <i>parameterIndex</i> being the <code>Timestamp</code> object that was
     * supplied as the second parameter to this method.
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x a <code>java.sql.Timestamp</code> object
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setTimestamp(int parameterIndex, java.sql.Timestamp x) throws SQLException {
        checkParamIndex(parameterIndex);
        if(params == null){
             throw new SQLException("Set initParams() before setTimestamp");
        }

        params.put(Integer.valueOf(parameterIndex - 1), x);
    }

    /**
     * Sets the designated parameter to the given
     * <code>java.io.InputStream</code> object,
     * which will have the specified number of bytes.
     * The contents of the stream will be read and sent to the database.
     * This method throws an <code>SQLException</code> object if the number of bytes
     * read and sent to the database is not equal to <i>length</i>.
     * <P>
     * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
     * parameter, it may be more practical to send it via a
     * <code>java.io.InputStream</code> object. A JDBC technology-enabled
     * driver will read the data from the stream as needed until it reaches
     * end-of-file. The driver will do any necessary conversion from ASCII to
     * the database <code>CHAR</code> format.
     *
     * <P><B>Note:</B> This stream object can be either a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * Note: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <P>
     * Calls made to the method <code>getParams</code> after <code>setAsciiStream</code>
     * has been called will return an array containing the parameter values that
     * have been set.  The element in the array that represents the values
     * set with this method will itself be an array. The first element of that array
     * is the given <code>java.io.InputStream</code> object.
     * The second element is the value set for <i>length</i>.
     * The third element is an internal <code>BaseRowSet</code> constant
     * specifying that the stream passed to this method is an ASCII stream.
     * The parameter number is indicated by an element's position in the array
     * returned by the method <code>getParams</code>,
     * with the first element being the value for the first placeholder parameter, the
     * second element being the value for the second placeholder parameter, and so on.
     * In other words, if the input stream being set is the value for the second
     * placeholder parameter, the array containing it will be the second element in
     * the array returned by <code>getParams</code>.
     * <P>
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is element number <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the Java input stream that contains the ASCII parameter value
     * @param length the number of bytes in the stream. This is the number of bytes
     *       the driver will send to the DBMS; lengths of 0 or less are
     *       are undefined but will cause an invalid length exception to be
     *       thrown in the underlying JDBC driver.
     * @throws SQLException if an error occurs, the parameter index is out of bounds,
     *       or when connected to a data source, the number of bytes the driver reads
     *       and sends to the database is not equal to the number of bytes specified
     *       in <i>length</i>
     * @see #getParams
     */
    public void setAsciiStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {
        Object asciiStream[];
        checkParamIndex(parameterIndex);

        asciiStream = new Object[3];
        asciiStream[0] = x;
        asciiStream[1] = Integer.valueOf(length);
        asciiStream[2] = Integer.valueOf(ASCII_STREAM_PARAM);

        if(params == null){
             throw new SQLException("Set initParams() before setAsciiStream");
        }

        params.put(Integer.valueOf(parameterIndex - 1), asciiStream);
    }

  /**
   * Sets the designated parameter in this <code>RowSet</code> object's command
   * to the given input stream.
   * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
   * parameter, it may be more practical to send it via a
   * <code>java.io.InputStream</code>. Data will be read from the stream
   * as needed until end-of-file is reached.  The JDBC driver will
   * do any necessary conversion from ASCII to the database char format.
   *
   * <P><B>Note:</B> This stream object can either be a standard
   * Java stream object or your own subclass that implements the
   * standard interface.
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>setAsciiStream</code> which takes a length parameter.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the Java input stream that contains the ASCII parameter value
   * @exception SQLException if a database access error occurs or
   * this method is called on a closed <code>PreparedStatement</code>
   * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
   * @since 1.6
   */
  public void setAsciiStream(int parameterIndex, java.io.InputStream x)
                      throws SQLException {
      throw new SQLFeatureNotSupportedException("Feature not supported");
  }

    /**
     * Sets the designated parameter to the given <code>java.io.InputStream</code>
     * object, which will have the specified number of bytes.
     * The contents of the stream will be read and sent to the database.
     * This method throws an <code>SQLException</code> object if the number of bytes
     * read and sent to the database is not equal to <i>length</i>.
     * <P>
     * When a very large binary value is input to a
     * <code>LONGVARBINARY</code> parameter, it may be more practical
     * to send it via a <code>java.io.InputStream</code> object.
     * A JDBC technology-enabled driver will read the data from the
     * stream as needed until it reaches end-of-file.
     *
     * <P><B>Note:</B> This stream object can be either a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     *<P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <P>
     * Calls made to the method <code>getParams</code> after <code>setBinaryStream</code>
     * has been called will return an array containing the parameter values that
     * have been set.  In that array, the element that represents the values
     * set with this method will itself be an array. The first element of that array
     * is the given <code>java.io.InputStream</code> object.
     * The second element is the value set for <i>length</i>.
     * The third element is an internal <code>BaseRowSet</code> constant
     * specifying that the stream passed to this method is a binary stream.
     * The parameter number is indicated by an element's position in the array
     * returned by the method <code>getParams</code>,
     * with the first element being the value for the first placeholder parameter, the
     * second element being the value for the second placeholder parameter, and so on.
     * In other words, if the input stream being set is the value for the second
     * placeholder parameter, the array containing it will be the second element in
     * the array returned by <code>getParams</code>.
     * <P>
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is element number <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the input stream that contains the binary value to be set
     * @param length the number of bytes in the stream; lengths of 0 or less are
     *         are undefined but will cause an invalid length exception to be
     *         thrown in the underlying JDBC driver.
     * @throws SQLException if an error occurs, the parameter index is out of bounds,
     *         or when connected to a data source, the number of bytes the driver
     *         reads and sends to the database is not equal to the number of bytes
     *         specified in <i>length</i>
     * @see #getParams
     */
    public void setBinaryStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {
        Object binaryStream[];
        checkParamIndex(parameterIndex);

        binaryStream = new Object[3];
        binaryStream[0] = x;
        binaryStream[1] = Integer.valueOf(length);
        binaryStream[2] = Integer.valueOf(BINARY_STREAM_PARAM);
        if(params == null){
             throw new SQLException("Set initParams() before setBinaryStream");
        }

        params.put(Integer.valueOf(parameterIndex - 1), binaryStream);
    }


   /**
   * Sets the designated parameter in this <code>RowSet</code> object's command
   * to the given input stream.
   * When a very large binary value is input to a <code>LONGVARBINARY</code>
   * parameter, it may be more practical to send it via a
   * <code>java.io.InputStream</code> object. The data will be read from the
   * stream as needed until end-of-file is reached.
   *
   * <P><B>Note:</B> This stream object can either be a standard
   * Java stream object or your own subclass that implements the
   * standard interface.
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>setBinaryStream</code> which takes a length parameter.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the java input stream which contains the binary parameter value
   * @exception SQLException if a database access error occurs or
   * this method is called on a closed <code>PreparedStatement</code>
   * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
   * @since 1.6
   */
  public void setBinaryStream(int parameterIndex, java.io.InputStream x)
                              throws SQLException {
      throw new SQLFeatureNotSupportedException("Feature not supported");
  }


    /**
     * Sets the designated parameter to the given
     * <code>java.io.InputStream</code> object, which will have the specified
     * number of bytes. The contents of the stream will be read and sent
     * to the database.
     * This method throws an <code>SQLException</code> if the number of bytes
     * read and sent to the database is not equal to <i>length</i>.
     * <P>
     * When a very large Unicode value is input to a
     * <code>LONGVARCHAR</code> parameter, it may be more practical
     * to send it via a <code>java.io.InputStream</code> object.
     * A JDBC technology-enabled driver will read the data from the
     * stream as needed, until it reaches end-of-file.
     * The driver will do any necessary conversion from Unicode to the
     * database <code>CHAR</code> format.
     * The byte format of the Unicode stream must be Java UTF-8, as
     * defined in the Java Virtual Machine Specification.
     *
     * <P><B>Note:</B> This stream object can be either a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P>
     * This method is deprecated; the method <code>getCharacterStream</code>
     * should be used in its place.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Calls made to the method <code>getParams</code> after <code>setUnicodeStream</code>
     * has been called will return an array containing the parameter values that
     * have been set.  In that array, the element that represents the values
     * set with this method will itself be an array. The first element of that array
     * is the given <code>java.io.InputStream</code> object.
     * The second element is the value set for <i>length</i>.
     * The third element is an internal <code>BaseRowSet</code> constant
     * specifying that the stream passed to this method is a Unicode stream.
     * The parameter number is indicated by an element's position in the array
     * returned by the method <code>getParams</code>,
     * with the first element being the value for the first placeholder parameter, the
     * second element being the value for the second placeholder parameter, and so on.
     * In other words, if the input stream being set is the value for the second
     * placeholder parameter, the array containing it will be the second element in
     * the array returned by <code>getParams</code>.
     * <P>
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is element number <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the <code>java.io.InputStream</code> object that contains the
     *          UNICODE parameter value
     * @param length the number of bytes in the input stream
     * @throws SQLException if an error occurs, the parameter index is out of bounds,
     *         or the number of bytes the driver reads and sends to the database is
     *         not equal to the number of bytes specified in <i>length</i>
     * @deprecated getCharacterStream should be used in its place
     * @see #getParams
     */
    @Deprecated
    public void setUnicodeStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {
        Object unicodeStream[];
        checkParamIndex(parameterIndex);

        unicodeStream = new Object[3];
        unicodeStream[0] = x;
        unicodeStream[1] = Integer.valueOf(length);
        unicodeStream[2] = Integer.valueOf(UNICODE_STREAM_PARAM);
        if(params == null){
             throw new SQLException("Set initParams() before setUnicodeStream");
        }
        params.put(Integer.valueOf(parameterIndex - 1), unicodeStream);
    }

    /**
     * Sets the designated parameter to the given <code>java.io.Reader</code>
     * object, which will have the specified number of characters. The
     * contents of the reader will be read and sent to the database.
     * This method throws an <code>SQLException</code> if the number of bytes
     * read and sent to the database is not equal to <i>length</i>.
     * <P>
     * When a very large Unicode value is input to a
     * <code>LONGVARCHAR</code> parameter, it may be more practical
     * to send it via a <code>Reader</code> object.
     * A JDBC technology-enabled driver will read the data from the
     * stream as needed until it reaches end-of-file.
     * The driver will do any necessary conversion from Unicode to the
     * database <code>CHAR</code> format.
     * The byte format of the Unicode stream must be Java UTF-8, as
     * defined in the Java Virtual Machine Specification.
     *
     * <P><B>Note:</B> This stream object can be either a standard
     * Java stream object or your own subclass that implements the
     * standard interface.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <P>
     * Calls made to the method <code>getParams</code> after
     * <code>setCharacterStream</code>
     * has been called will return an array containing the parameter values that
     * have been set.  In that array, the element that represents the values
     * set with this method will itself be an array. The first element of that array
     * is the given <code>java.io.Reader</code> object.
     * The second element is the value set for <i>length</i>.
     * The parameter number is indicated by an element's position in the array
     * returned by the method <code>getParams</code>,
     * with the first element being the value for the first placeholder parameter, the
     * second element being the value for the second placeholder parameter, and so on.
     * In other words, if the reader being set is the value for the second
     * placeholder parameter, the array containing it will be the second element in
     * the array returned by <code>getParams</code>.
     * <P>
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is element number <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param reader the <code>Reader</code> object that contains the
     *        Unicode data
     * @param length the number of characters in the stream; lengths of 0 or
     *        less are undefined but will cause an invalid length exception to
     *        be thrown in the underlying JDBC driver.
     * @throws SQLException if an error occurs, the parameter index is out of bounds,
     *        or when connected to a data source, the number of bytes the driver
     *        reads and sends to the database is not equal to the number of bytes
     *        specified in <i>length</i>
     * @see #getParams
     */
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        Object charStream[];
        checkParamIndex(parameterIndex);

        charStream = new Object[2];
        charStream[0] = reader;
        charStream[1] = Integer.valueOf(length);
        if(params == null){
             throw new SQLException("Set initParams() before setCharacterStream");
        }
        params.put(Integer.valueOf(parameterIndex - 1), charStream);
    }

   /**
   * Sets the designated parameter in this <code>RowSet</code> object's command
   * to the given <code>Reader</code>
   * object.
   * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
   * parameter, it may be more practical to send it via a
   * <code>java.io.Reader</code> object. The data will be read from the stream
   * as needed until end-of-file is reached.  The JDBC driver will
   * do any necessary conversion from UNICODE to the database char format.
   *
   * <P><B>Note:</B> This stream object can either be a standard
   * Java stream object or your own subclass that implements the
   * standard interface.
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>setCharacterStream</code> which takes a length parameter.
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param reader the <code>java.io.Reader</code> object that contains the
   *        Unicode data
   * @exception SQLException if a database access error occurs or
   * this method is called on a closed <code>PreparedStatement</code>
   * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
   * @since 1.6
   */
  public void setCharacterStream(int parameterIndex,
                          java.io.Reader reader) throws SQLException {
      throw new SQLFeatureNotSupportedException("Feature not supported");
  }

    /**
     * Sets the designated parameter to an <code>Object</code> in the Java
     * programming language. The second parameter must be an
     * <code>Object</code> type.  For integral values, the
     * <code>java.lang</code> equivalent
     * objects should be used. For example, use the class <code>Integer</code>
     * for an <code>int</code>.
     * <P>
     * The driver converts this object to the specified
     * target SQL type before sending it to the database.
     * If the object has a custom mapping (is of a class implementing
     * <code>SQLData</code>), the driver should call the method
     * <code>SQLData.writeSQL</code> to write the object to the SQL
     * data stream. If, on the other hand, the object is of a class
     * implementing <code>Ref</code>, <code>Blob</code>, <code>Clob</code>,
     * <code>Struct</code>, or <code>Array</code>,
     * the driver should pass it to the database as a value of the
     * corresponding SQL type.
     *
     * <p>Note that this method may be used to pass database-
     * specific abstract data types.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <P>
     * Calls made to the method <code>getParams</code> after this version of
     * <code>setObject</code>
     * has been called will return an array containing the parameter values that
     * have been set.  In that array, the element that represents the values
     * set with this method will itself be an array. The first element of that array
     * is the given <code>Object</code> instance, and the
     * second element is the value set for <i>targetSqlType</i>.  The
     * third element is the value set for <i>scale</i>, which the driver will
     * ignore if the type of the object being set is not
     * <code>java.sql.Types.NUMERIC</code> or <code>java.sql.Types.DECIMAL</code>.
     * The parameter number is indicated by an element's position in the array
     * returned by the method <code>getParams</code>,
     * with the first element being the value for the first placeholder parameter, the
     * second element being the value for the second placeholder parameter, and so on.
     * In other words, if the object being set is the value for the second
     * placeholder parameter, the array containing it will be the second element in
     * the array returned by <code>getParams</code>.
     *<P>
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is element number <i>parameterIndex</i> -1.
     *
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the <code>Object</code> containing the input parameter value;
     *        must be an <code>Object</code> type
     * @param targetSqlType the SQL type (as defined in <code>java.sql.Types</code>)
     *        to be sent to the database. The <code>scale</code> argument may
     *        further qualify this type. If a non-standard <i>targetSqlType</i>
     *        is supplied, this method will not throw a <code>SQLException</code>.
     *        This allows implicit support for non-standard SQL types.
     * @param scale for the types <code>java.sql.Types.DECIMAL</code> and
     *        <code>java.sql.Types.NUMERIC</code>, this is the number
     *        of digits after the decimal point.  For all other types, this
     *        value will be ignored.
     * @throws SQLException if an error occurs or the parameter index is out of bounds
     * @see #getParams
     */
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException {
        Object obj[];
        checkParamIndex(parameterIndex);

        obj = new Object[3];
        obj[0] = x;
        obj[1] = Integer.valueOf(targetSqlType);
        obj[2] = Integer.valueOf(scale);
        if(params == null){
             throw new SQLException("Set initParams() before setObject");
        }
        params.put(Integer.valueOf(parameterIndex - 1), obj);
    }

    /**
     * Sets the value of the designated parameter with the given
     * <code>Object</code> value.
     * This method is like <code>setObject(int parameterIndex, Object x, int
     * targetSqlType, int scale)</code> except that it assumes a scale of zero.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <P>
     * Calls made to the method <code>getParams</code> after this version of
     * <code>setObject</code>
     * has been called will return an array containing the parameter values that
     * have been set.  In that array, the element that represents the values
     * set with this method will itself be an array. The first element of that array
     * is the given <code>Object</code> instance.
     * The second element is the value set for <i>targetSqlType</i>.
     * The parameter number is indicated by an element's position in the array
     * returned by the method <code>getParams</code>,
     * with the first element being the value for the first placeholder parameter, the
     * second element being the value for the second placeholder parameter, and so on.
     * In other words, if the object being set is the value for the second
     * placeholder parameter, the array containing it will be the second element in
     * the array returned by <code>getParams</code>.
     * <P>
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is element number <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the <code>Object</code> containing the input parameter value;
     *        must be an <code>Object</code> type
     * @param targetSqlType the SQL type (as defined in <code>java.sql.Types</code>)
     *        to be sent to the database. If a non-standard <i>targetSqlType</i>
     *        is supplied, this method will not throw a <code>SQLException</code>.
     *        This allows implicit support for non-standard SQL types.
     * @throws SQLException if an error occurs or the parameter index
     *        is out of bounds
     * @see #getParams
     */
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        Object obj[];
        checkParamIndex(parameterIndex);

        obj = new Object[2];
        obj[0] = x;
        obj[1] = Integer.valueOf(targetSqlType);
        if (params == null){
             throw new SQLException("Set initParams() before setObject");
        }
        params.put(Integer.valueOf(parameterIndex - 1), obj);
    }

    /**
     * Sets the designated parameter to an <code>Object</code> in the Java
     * programming language. The second parameter must be an
     * <code>Object</code>
     * type.  For integral values, the <code>java.lang</code> equivalent
     * objects should be used. For example, use the class <code>Integer</code>
     * for an <code>int</code>.
     * <P>
     * The JDBC specification defines a standard mapping from
     * Java <code>Object</code> types to SQL types.  The driver will
     * use this standard mapping to  convert the given object
     * to its corresponding SQL type before sending it to the database.
     * If the object has a custom mapping (is of a class implementing
     * <code>SQLData</code>), the driver should call the method
     * <code>SQLData.writeSQL</code> to write the object to the SQL
     * data stream.
     * <P>
     * If, on the other hand, the object is of a class
     * implementing <code>Ref</code>, <code>Blob</code>, <code>Clob</code>,
     * <code>Struct</code>, or <code>Array</code>,
     * the driver should pass it to the database as a value of the
     * corresponding SQL type.
     * <P>
     * This method throws an exception if there
     * is an ambiguity, for example, if the object is of a class
     * implementing more than one interface.
     * <P>
     * Note that this method may be used to pass database-specific
     * abstract data types.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <p>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <P>
     * After this method has been called, a call to the
     * method <code>getParams</code>
     * will return an object array of the current command parameters, which will
     * include the <code>Object</code> set for placeholder parameter number
     * <code>parameterIndex</code>.
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is element number <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x the object containing the input parameter value
     * @throws SQLException if an error occurs the
     *                         parameter index is out of bounds, or there
     *                         is ambiguity in the implementation of the
     *                         object being set
     * @see #getParams
     */
    public void setObject(int parameterIndex, Object x) throws SQLException {
        checkParamIndex(parameterIndex);
        if (params == null) {
             throw new SQLException("Set initParams() before setObject");
        }
        params.put(Integer.valueOf(parameterIndex - 1), x);
    }

    /**
     * Sets the designated parameter to the given <code>Ref</code> object in
     * the Java programming language.  The driver converts this to an SQL
     * <code>REF</code> value when it sends it to the database. Internally, the
     * <code>Ref</code> is represented as a <code>SerialRef</code> to ensure
     * serializability.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <p>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <p>
     * After this method has been called, a call to the
     * method <code>getParams</code>
     * will return an object array of the current command parameters, which will
     * include the <code>Ref</code> object set for placeholder parameter number
     * <code>parameterIndex</code>.
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is element number <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param ref a <code>Ref</code> object representing an SQL <code>REF</code>
     *         value; cannot be null
     * @throws SQLException if an error occurs; the parameter index is out of
     *         bounds or the <code>Ref</code> object is <code>null</code>; or
     *         the <code>Ref</code> object returns a <code>null</code> base type
     *         name.
     * @see #getParams
     * @see javax.sql.rowset.serial.SerialRef
     */
    public void setRef (int parameterIndex, Ref ref) throws SQLException {
        checkParamIndex(parameterIndex);
        if (params == null) {
             throw new SQLException("Set initParams() before setRef");
        }
        params.put(Integer.valueOf(parameterIndex - 1), new SerialRef(ref));
    }

    /**
     * Sets the designated parameter to the given <code>Blob</code> object in
     * the Java programming language.  The driver converts this to an SQL
     * <code>BLOB</code> value when it sends it to the database. Internally,
     * the <code>Blob</code> is represented as a <code>SerialBlob</code>
     * to ensure serializability.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <p>
     * After this method has been called, a call to the
     * method <code>getParams</code>
     * will return an object array of the current command parameters, which will
     * include the <code>Blob</code> object set for placeholder parameter number
     * <code>parameterIndex</code>.
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is element number <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x a <code>Blob</code> object representing an SQL
     *          <code>BLOB</code> value
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     * @see javax.sql.rowset.serial.SerialBlob
     */
    public void setBlob (int parameterIndex, Blob x) throws SQLException {
        checkParamIndex(parameterIndex);
        if(params == null){
             throw new SQLException("Set initParams() before setBlob");
        }
        params.put(Integer.valueOf(parameterIndex - 1), new SerialBlob(x));
    }

    /**
     * Sets the designated parameter to the given <code>Clob</code> object in
     * the Java programming language.  The driver converts this to an SQL
     * <code>CLOB</code> value when it sends it to the database. Internally, the
     * <code>Clob</code> is represented as a <code>SerialClob</code> to ensure
     * serializability.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <p>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <p>
     * After this method has been called, a call to the
     * method <code>getParams</code>
     * will return an object array of the current command parameters, which will
     * include the <code>Clob</code> object set for placeholder parameter number
     * <code>parameterIndex</code>.
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is element number <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *     in this <code>RowSet</code> object's command that is to be set.
     *     The first parameter is 1, the second is 2, and so on; must be
     *     <code>1</code> or greater
     * @param x a <code>Clob</code> object representing an SQL
     *     <code>CLOB</code> value; cannot be null
     * @throws SQLException if an error occurs; the parameter index is out of
     *     bounds or the <code>Clob</code> is null
     * @see #getParams
     * @see javax.sql.rowset.serial.SerialBlob
     */
    public void setClob (int parameterIndex, Clob x) throws SQLException {
        checkParamIndex(parameterIndex);
        if(params == null){
             throw new SQLException("Set initParams() before setClob");
        }
        params.put(Integer.valueOf(parameterIndex - 1), new SerialClob(x));
    }

    /**
     * Sets the designated parameter to an <code>Array</code> object in the
     * Java programming language.  The driver converts this to an SQL
     * <code>ARRAY</code> value when it sends it to the database. Internally,
     * the <code>Array</code> is represented as a <code>SerialArray</code>
     * to ensure serializability.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * Note: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <p>
     * After this method has been called, a call to the
     * method <code>getParams</code>
     * will return an object array of the current command parameters, which will
     * include the <code>Array</code> object set for placeholder parameter number
     * <code>parameterIndex</code>.
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is element number <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param array an <code>Array</code> object representing an SQL
     *        <code>ARRAY</code> value; cannot be null. The <code>Array</code> object
     *        passed to this method must return a non-null Object for all
     *        <code>getArray()</code> method calls. A null value will cause a
     *        <code>SQLException</code> to be thrown.
     * @throws SQLException if an error occurs; the parameter index is out of
     *        bounds or the <code>ARRAY</code> is null
     * @see #getParams
     * @see javax.sql.rowset.serial.SerialArray
     */
    public void setArray (int parameterIndex, Array array) throws SQLException {
        checkParamIndex(parameterIndex);
        if (params == null){
             throw new SQLException("Set initParams() before setArray");
        }
        params.put(Integer.valueOf(parameterIndex - 1), new SerialArray(array));
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Date</code>
     * object.
     * When the DBMS does not store time zone information, the driver will use
     * the given <code>Calendar</code> object to construct the SQL <code>DATE</code>
     * value to send to the database. With a
     * <code>Calendar</code> object, the driver can calculate the date
     * taking into account a custom time zone.  If no <code>Calendar</code>
     * object is specified, the driver uses the time zone of the Virtual Machine
     * that is running the application.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <P>
     * Calls made to the method <code>getParams</code> after this version of
     * <code>setDate</code>
     * has been called will return an array containing the parameter values that
     * have been set.  In that array, the element that represents the values
     * set with this method will itself be an array. The first element of that array
     * is the given <code>java.sql.Date</code> object.
     * The second element is the value set for <i>cal</i>.
     * The parameter number is indicated by an element's position in the array
     * returned by the method <code>getParams</code>,
     * with the first element being the value for the first placeholder parameter, the
     * second element being the value for the second placeholder parameter, and so on.
     * In other words, if the date being set is the value for the second
     * placeholder parameter, the array containing it will be the second element in
     * the array returned by <code>getParams</code>.
     * <P>
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x a <code>java.sql.Date</code> object representing an SQL
     *        <code>DATE</code> value
     * @param cal a <code>java.util.Calendar</code> object to use when
     *        when constructing the date
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setDate(int parameterIndex, java.sql.Date x, Calendar cal) throws SQLException {
        Object date[];
        checkParamIndex(parameterIndex);

        date = new Object[2];
        date[0] = x;
        date[1] = cal;
        if(params == null){
             throw new SQLException("Set initParams() before setDate");
        }
        params.put(Integer.valueOf(parameterIndex - 1), date);
    }

    /**
     * Sets the designated parameter to the given <code>java.sql.Time</code>
     * object.  The driver converts this
     * to an SQL <code>TIME</code> value when it sends it to the database.
     * <P>
     * When the DBMS does not store time zone information, the driver will use
     * the given <code>Calendar</code> object to construct the SQL <code>TIME</code>
     * value to send to the database. With a
     * <code>Calendar</code> object, the driver can calculate the date
     * taking into account a custom time zone.  If no <code>Calendar</code>
     * object is specified, the driver uses the time zone of the Virtual Machine
     * that is running the application.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <P>
     * Calls made to the method <code>getParams</code> after this version of
     * <code>setTime</code>
     * has been called will return an array containing the parameter values that
     * have been set.  In that array, the element that represents the values
     * set with this method will itself be an array. The first element of that array
     * is the given <code>java.sql.Time</code> object.
     * The second element is the value set for <i>cal</i>.
     * The parameter number is indicated by an element's position in the array
     * returned by the method <code>getParams</code>,
     * with the first element being the value for the first placeholder parameter, the
     * second element being the value for the second placeholder parameter, and so on.
     * In other words, if the time being set is the value for the second
     * placeholder parameter, the array containing it will be the second element in
     * the array returned by <code>getParams</code>.
     * <P>
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x a <code>java.sql.Time</code> object
     * @param cal the <code>java.util.Calendar</code> object the driver can use to
     *         construct the time
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setTime(int parameterIndex, java.sql.Time x, Calendar cal) throws SQLException {
        Object time[];
        checkParamIndex(parameterIndex);

        time = new Object[2];
        time[0] = x;
        time[1] = cal;
        if(params == null){
             throw new SQLException("Set initParams() before setTime");
        }
        params.put(Integer.valueOf(parameterIndex - 1), time);
    }

    /**
     * Sets the designated parameter to the given
     * <code>java.sql.Timestamp</code> object.  The driver converts this
     * to an SQL <code>TIMESTAMP</code> value when it sends it to the database.
     * <P>
     * When the DBMS does not store time zone information, the driver will use
     * the given <code>Calendar</code> object to construct the SQL <code>TIMESTAMP</code>
     * value to send to the database. With a
     * <code>Calendar</code> object, the driver can calculate the timestamp
     * taking into account a custom time zone.  If no <code>Calendar</code>
     * object is specified, the driver uses the time zone of the Virtual Machine
     * that is running the application.
     * <P>
     * The parameter value set by this method is stored internally and
     * will be supplied as the appropriate parameter in this <code>RowSet</code>
     * object's command when the method <code>execute</code> is called.
     * Methods such as <code>execute</code> and <code>populate</code> must be
     * provided in any class that extends this class and implements one or
     * more of the standard JSR-114 <code>RowSet</code> interfaces.
     * <P>
     * NOTE: <code>JdbcRowSet</code> does not require the <code>populate</code> method
     * as it is undefined in this class.
     * <P>
     * Calls made to the method <code>getParams</code> after this version of
     * <code>setTimestamp</code>
     * has been called will return an array containing the parameter values that
     * have been set.  In that array, the element that represents the values
     * set with this method will itself be an array. The first element of that array
     * is the given <code>java.sql.Timestamp</code> object.
     * The second element is the value set for <i>cal</i>.
     * The parameter number is indicated by an element's position in the array
     * returned by the method <code>getParams</code>,
     * with the first element being the value for the first placeholder parameter, the
     * second element being the value for the second placeholder parameter, and so on.
     * In other words, if the timestamp being set is the value for the second
     * placeholder parameter, the array containing it will be the second element in
     * the array returned by <code>getParams</code>.
     * <P>
     * Note that because the numbering of elements in an array starts at zero,
     * the array element that corresponds to placeholder parameter number
     * <i>parameterIndex</i> is <i>parameterIndex</i> -1.
     *
     * @param parameterIndex the ordinal number of the placeholder parameter
     *        in this <code>RowSet</code> object's command that is to be set.
     *        The first parameter is 1, the second is 2, and so on; must be
     *        <code>1</code> or greater
     * @param x a <code>java.sql.Timestamp</code> object
     * @param cal the <code>java.util.Calendar</code> object the driver can use to
     *         construct the timestamp
     * @throws SQLException if an error occurs or the
     *                         parameter index is out of bounds
     * @see #getParams
     */
    public void setTimestamp(int parameterIndex, java.sql.Timestamp x, Calendar cal) throws SQLException {
        Object timestamp[];
        checkParamIndex(parameterIndex);

        timestamp = new Object[2];
        timestamp[0] = x;
        timestamp[1] = cal;
        if(params == null){
             throw new SQLException("Set initParams() before setTimestamp");
        }
        params.put(Integer.valueOf(parameterIndex - 1), timestamp);
    }

    /**
     * Clears all of the current parameter values in this <code>RowSet</code>
     * object's internal representation of the parameters to be set in
     * this <code>RowSet</code> object's command when it is executed.
     * <P>
     * In general, parameter values remain in force for repeated use in
     * this <code>RowSet</code> object's command. Setting a parameter value with the
     * setter methods automatically clears the value of the
     * designated parameter and replaces it with the new specified value.
     * <P>
     * This method is called internally by the <code>setCommand</code>
     * method to clear all of the parameters set for the previous command.
     * <P>
     * Furthermore, this method differs from the <code>initParams</code>
     * method in that it maintains the schema of the <code>RowSet</code> object.
     *
     * @throws SQLException if an error occurs clearing the parameters
     */
    public void clearParameters() throws SQLException {
        params.clear();
    }

    /**
     * Retrieves an array containing the parameter values (both Objects and
     * primitives) that have been set for this
     * <code>RowSet</code> object's command and throws an <code>SQLException</code> object
     * if all parameters have not been set.   Before the command is sent to the
     * DBMS to be executed, these parameters will be substituted
     * for placeholder parameters in the  <code>PreparedStatement</code> object
     * that is the command for a <code>RowSet</code> implementation extending
     * the <code>BaseRowSet</code> class.
     * <P>
     * Each element in the array that is returned is an <code>Object</code> instance
     * that contains the values of the parameters supplied to a setter method.
     * The order of the elements is determined by the value supplied for
     * <i>parameterIndex</i>.  If the setter method takes only the parameter index
     * and the value to be set (possibly null), the array element will contain the value to be set
     * (which will be expressed as an <code>Object</code>).  If there are additional
     * parameters, the array element will itself be an array containing the value to be set
     * plus any additional parameter values supplied to the setter method. If the method
     * sets a stream, the array element includes the type of stream being supplied to the
     * method. These additional parameters are for the use of the driver or the DBMS and may or
     * may not be used.
     * <P>
     * NOTE: Stored parameter values of types <code>Array</code>, <code>Blob</code>,
     * <code>Clob</code> and <code>Ref</code> are returned as <code>SerialArray</code>,
     * <code>SerialBlob</code>, <code>SerialClob</code> and <code>SerialRef</code>
     * respectively.
     *
     * @return an array of <code>Object</code> instances that includes the
     *         parameter values that may be set in this <code>RowSet</code> object's
     *         command; an empty array if no parameters have been set
     * @throws SQLException if an error occurs retrieving the object array of
     *         parameters of this <code>RowSet</code> object or if not all parameters have
     *         been set
     */
    public Object[] getParams() throws SQLException {
        if (params == null) {

            initParams();
            Object [] paramsArray = new Object[params.size()];
            return paramsArray;

        } else {
            // The parameters may be set in random order
            // but all must be set, check to verify all
            // have been set till the last parameter
            // else throw exception.

            Object[] paramsArray = new Object[params.size()];
            for (int i = 0; i < params.size(); i++) {
               paramsArray[i] = params.get(Integer.valueOf(i));
               if (paramsArray[i] == null) {
                 throw new SQLException("missing parameter: " + (i + 1));
               } //end if
            } //end for
            return paramsArray;

        } //end if

    } //end getParams


   /**
    * Sets the designated parameter to SQL <code>NULL</code>.
    *
    * <P><B>Note:</B> You must specify the parameter's SQL type.
    *
    * @param parameterName the name of the parameter
    * @param sqlType the SQL type code defined in <code>java.sql.Types</code>
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    */
   public void setNull(String parameterName, int sqlType) throws SQLException {
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to SQL <code>NULL</code>.
    * This version of the method <code>setNull</code> should
    * be used for user-defined types and REF type parameters.  Examples
    * of user-defined types include: STRUCT, DISTINCT, JAVA_OBJECT, and
    * named array types.
    *
    * <P><B>Note:</B> To be portable, applications must give the
    * SQL type code and the fully-qualified SQL type name when specifying
    * a NULL user-defined or REF parameter.  In the case of a user-defined type
    * the name is the type name of the parameter itself.  For a REF
    * parameter, the name is the type name of the referenced type.  If
    * a JDBC driver does not need the type code or type name information,
    * it may ignore it.
    *
    * Although it is intended for user-defined and Ref parameters,
    * this method may be used to set a null parameter of any JDBC type.
    * If the parameter does not have a user-defined or REF type, the given
    * typeName is ignored.
    *
    *
    * @param parameterName the name of the parameter
    * @param sqlType a value from <code>java.sql.Types</code>
    * @param typeName the fully-qualified name of an SQL user-defined type;
    *        ignored if the parameter is not a user-defined type or
    *        SQL <code>REF</code> value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    */
   public void setNull (String parameterName, int sqlType, String typeName)
       throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given Java <code>boolean</code> value.
    * The driver converts this
    * to an SQL <code>BIT</code> or <code>BOOLEAN</code> value when it sends it to the database.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setBoolean(String parameterName, boolean x) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given Java <code>byte</code> value.
    * The driver converts this
    * to an SQL <code>TINYINT</code> value when it sends it to the database.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setByte(String parameterName, byte x) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given Java <code>short</code> value.
    * The driver converts this
    * to an SQL <code>SMALLINT</code> value when it sends it to the database.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setShort(String parameterName, short x) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given Java <code>int</code> value.
    * The driver converts this
    * to an SQL <code>INTEGER</code> value when it sends it to the database.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setInt(String parameterName, int x) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }


   /**
    * Sets the designated parameter to the given Java <code>long</code> value.
    * The driver converts this
    * to an SQL <code>BIGINT</code> value when it sends it to the database.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setLong(String parameterName, long x) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given Java <code>float</code> value.
    * The driver converts this
    * to an SQL <code>FLOAT</code> value when it sends it to the database.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setFloat(String parameterName, float x) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given Java <code>double</code> value.
    * The driver converts this
    * to an SQL <code>DOUBLE</code> value when it sends it to the database.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setDouble(String parameterName, double x) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given
    * <code>java.math.BigDecimal</code> value.
    * The driver converts this to an SQL <code>NUMERIC</code> value when
    * it sends it to the database.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given Java <code>String</code> value.
    * The driver converts this
    * to an SQL <code>VARCHAR</code> or <code>LONGVARCHAR</code> value
    * (depending on the argument's
    * size relative to the driver's limits on <code>VARCHAR</code> values)
    * when it sends it to the database.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setString(String parameterName, String x) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given Java array of bytes.
    * The driver converts this to an SQL <code>VARBINARY</code> or
    * <code>LONGVARBINARY</code> (depending on the argument's size relative
    * to the driver's limits on <code>VARBINARY</code> values) when it sends
    * it to the database.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setBytes(String parameterName, byte x[]) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value.
    * The driver
    * converts this to an SQL <code>TIMESTAMP</code> value when it sends it to the
    * database.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setTimestamp(String parameterName, java.sql.Timestamp x)
       throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given input stream, which will have
    * the specified number of bytes.
    * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
    * parameter, it may be more practical to send it via a
    * <code>java.io.InputStream</code>. Data will be read from the stream
    * as needed until end-of-file is reached.  The JDBC driver will
    * do any necessary conversion from ASCII to the database char format.
    *
    * <P><B>Note:</B> This stream object can either be a standard
    * Java stream object or your own subclass that implements the
    * standard interface.
    *
    * @param parameterName the name of the parameter
    * @param x the Java input stream that contains the ASCII parameter value
    * @param length the number of bytes in the stream
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    */
   public void setAsciiStream(String parameterName, java.io.InputStream x, int length)
       throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given input stream, which will have
    * the specified number of bytes.
    * When a very large binary value is input to a <code>LONGVARBINARY</code>
    * parameter, it may be more practical to send it via a
    * <code>java.io.InputStream</code> object. The data will be read from the stream
    * as needed until end-of-file is reached.
    *
    * <P><B>Note:</B> This stream object can either be a standard
    * Java stream object or your own subclass that implements the
    * standard interface.
    *
    * @param parameterName the name of the parameter
    * @param x the java input stream which contains the binary parameter value
    * @param length the number of bytes in the stream
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    */
   public void setBinaryStream(String parameterName, java.io.InputStream x,
                        int length) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given <code>Reader</code>
    * object, which is the given number of characters long.
    * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
    * parameter, it may be more practical to send it via a
    * <code>java.io.Reader</code> object. The data will be read from the stream
    * as needed until end-of-file is reached.  The JDBC driver will
    * do any necessary conversion from UNICODE to the database char format.
    *
    * <P><B>Note:</B> This stream object can either be a standard
    * Java stream object or your own subclass that implements the
    * standard interface.
    *
    * @param parameterName the name of the parameter
    * @param reader the <code>java.io.Reader</code> object that
    *        contains the UNICODE data used as the designated parameter
    * @param length the number of characters in the stream
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    */
   public void setCharacterStream(String parameterName,
                           java.io.Reader reader,
                           int length) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

  /**
   * Sets the designated parameter to the given input stream.
   * When a very large ASCII value is input to a <code>LONGVARCHAR</code>
   * parameter, it may be more practical to send it via a
   * <code>java.io.InputStream</code>. Data will be read from the stream
   * as needed until end-of-file is reached.  The JDBC driver will
   * do any necessary conversion from ASCII to the database char format.
   *
   * <P><B>Note:</B> This stream object can either be a standard
   * Java stream object or your own subclass that implements the
   * standard interface.
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>setAsciiStream</code> which takes a length parameter.
   *
   * @param parameterName the name of the parameter
   * @param x the Java input stream that contains the ASCII parameter value
   * @exception SQLException if a database access error occurs or
   * this method is called on a closed <code>CallableStatement</code>
   * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
   * @since 1.6
  */
  public void setAsciiStream(String parameterName, java.io.InputStream x)
          throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given input stream.
    * When a very large binary value is input to a <code>LONGVARBINARY</code>
    * parameter, it may be more practical to send it via a
    * <code>java.io.InputStream</code> object. The data will be read from the
    * stream as needed until end-of-file is reached.
    *
    * <P><B>Note:</B> This stream object can either be a standard
    * Java stream object or your own subclass that implements the
    * standard interface.
    * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
    * it might be more efficient to use a version of
    * <code>setBinaryStream</code> which takes a length parameter.
    *
    * @param parameterName the name of the parameter
    * @param x the java input stream which contains the binary parameter value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
    * @since 1.6
    */
   public void setBinaryStream(String parameterName, java.io.InputStream x)
   throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given <code>Reader</code>
    * object.
    * When a very large UNICODE value is input to a <code>LONGVARCHAR</code>
    * parameter, it may be more practical to send it via a
    * <code>java.io.Reader</code> object. The data will be read from the stream
    * as needed until end-of-file is reached.  The JDBC driver will
    * do any necessary conversion from UNICODE to the database char format.
    *
    * <P><B>Note:</B> This stream object can either be a standard
    * Java stream object or your own subclass that implements the
    * standard interface.
    * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
    * it might be more efficient to use a version of
    * <code>setCharacterStream</code> which takes a length parameter.
    *
    * @param parameterName the name of the parameter
    * @param reader the <code>java.io.Reader</code> object that contains the
    *        Unicode data
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
    * @since 1.6
    */
   public void setCharacterStream(String parameterName,
                         java.io.Reader reader) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

 /**
  * Sets the designated parameter in this <code>RowSet</code> object's command
  * to a <code>Reader</code> object. The
  * <code>Reader</code> reads the data till end-of-file is reached. The
  * driver does the necessary conversion from Java character format to
  * the national character set in the database.
  *
  * <P><B>Note:</B> This stream object can either be a standard
  * Java stream object or your own subclass that implements the
  * standard interface.
  * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
  * it might be more efficient to use a version of
  * <code>setNCharacterStream</code> which takes a length parameter.
  *
  * @param parameterIndex of the first parameter is 1, the second is 2, ...
  * @param value the parameter value
  * @throws SQLException if the driver does not support national
  *         character sets;  if the driver can detect that a data conversion
  *  error could occur ; if a database access error occurs; or
  * this method is called on a closed <code>PreparedStatement</code>
  * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
  * @since 1.6
  */
  public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the value of the designated parameter with the given object. The second
    * argument must be an object type; for integral values, the
    * <code>java.lang</code> equivalent objects should be used.
    *
    * <p>The given Java object will be converted to the given targetSqlType
    * before being sent to the database.
    *
    * If the object has a custom mapping (is of a class implementing the
    * interface <code>SQLData</code>),
    * the JDBC driver should call the method <code>SQLData.writeSQL</code> to write it
    * to the SQL data stream.
    * If, on the other hand, the object is of a class implementing
    * <code>Ref</code>, <code>Blob</code>, <code>Clob</code>,  <code>NClob</code>,
    *  <code>Struct</code>, <code>java.net.URL</code>,
    * or <code>Array</code>, the driver should pass it to the database as a
    * value of the corresponding SQL type.
    * <P>
    * Note that this method may be used to pass database-
    * specific abstract data types.
    *
    * @param parameterName the name of the parameter
    * @param x the object containing the input parameter value
    * @param targetSqlType the SQL type (as defined in java.sql.Types) to be
    * sent to the database. The scale argument may further qualify this type.
    * @param scale for java.sql.Types.DECIMAL or java.sql.Types.NUMERIC types,
    *          this is the number of digits after the decimal point.  For all other
    *          types, this value will be ignored.
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if <code>targetSqlType</code> is
    * a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>,
    * <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>,
    * <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
    *  <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
    * or  <code>STRUCT</code> data type and the JDBC driver does not support
    * this data type
    * @see Types
    * @see #getParams
    */
   public void setObject(String parameterName, Object x, int targetSqlType, int scale)
       throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the value of the designated parameter with the given object.
    * This method is like the method <code>setObject</code>
    * above, except that it assumes a scale of zero.
    *
    * @param parameterName the name of the parameter
    * @param x the object containing the input parameter value
    * @param targetSqlType the SQL type (as defined in java.sql.Types) to be
    *                      sent to the database
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if <code>targetSqlType</code> is
    * a <code>ARRAY</code>, <code>BLOB</code>, <code>CLOB</code>,
    * <code>DATALINK</code>, <code>JAVA_OBJECT</code>, <code>NCHAR</code>,
    * <code>NCLOB</code>, <code>NVARCHAR</code>, <code>LONGNVARCHAR</code>,
    *  <code>REF</code>, <code>ROWID</code>, <code>SQLXML</code>
    * or  <code>STRUCT</code> data type and the JDBC driver does not support
    * this data type
    * @see #getParams
    */
   public void setObject(String parameterName, Object x, int targetSqlType)
       throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

  /**
   * Sets the value of the designated parameter with the given object.
   * The second parameter must be of type <code>Object</code>; therefore, the
   * <code>java.lang</code> equivalent objects should be used for built-in types.
   *
   * <p>The JDBC specification specifies a standard mapping from
   * Java <code>Object</code> types to SQL types.  The given argument
   * will be converted to the corresponding SQL type before being
   * sent to the database.
   *
   * <p>Note that this method may be used to pass database-
   * specific abstract data types, by using a driver-specific Java
   * type.
   *
   * If the object is of a class implementing the interface <code>SQLData</code>,
   * the JDBC driver should call the method <code>SQLData.writeSQL</code>
   * to write it to the SQL data stream.
   * If, on the other hand, the object is of a class implementing
   * <code>Ref</code>, <code>Blob</code>, <code>Clob</code>,  <code>NClob</code>,
   *  <code>Struct</code>, <code>java.net.URL</code>,
   * or <code>Array</code>, the driver should pass it to the database as a
   * value of the corresponding SQL type.
   * <P>
   * This method throws an exception if there is an ambiguity, for example, if the
   * object is of a class implementing more than one of the interfaces named above.
   *
   * @param parameterName the name of the parameter
   * @param x the object containing the input parameter value
   * @exception SQLException if a database access error occurs,
   * this method is called on a closed <code>CallableStatement</code> or if the given
   *            <code>Object</code> parameter is ambiguous
   * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
   * this method
   * @see #getParams
   */
  public void setObject(String parameterName, Object x) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to a <code>InputStream</code> object.
    * The <code>InputStream</code> must contain  the number
    * of characters specified by length otherwise a <code>SQLException</code> will be
    * generated when the <code>PreparedStatement</code> is executed.
    * This method differs from the <code>setBinaryStream (int, InputStream, int)</code>
    * method because it informs the driver that the parameter value should be
    * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
    * the driver may have to do extra work to determine whether the parameter
    * data should be sent to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
    * @param parameterIndex index of the first parameter is 1,
    * the second is 2, ...
    * @param inputStream An object that contains the data to set the parameter
    * value to.
    * @param length the number of bytes in the parameter data.
    * @throws SQLException if a database access error occurs,
    * this method is called on a closed <code>PreparedStatement</code>,
    * if parameterIndex does not correspond
    * to a parameter marker in the SQL statement,  if the length specified
    * is less than zero or if the number of bytes in the
    * <code>InputStream</code> does not match the specified length.
    * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
    *
    * @since 1.6
    */
    public void setBlob(int parameterIndex, InputStream inputStream, long length)
       throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to a <code>InputStream</code> object.
    * This method differs from the <code>setBinaryStream (int, InputStream)</code>
    * method because it informs the driver that the parameter value should be
    * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
    * the driver may have to do extra work to determine whether the parameter
    * data should be sent to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
    *
    * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
    * it might be more efficient to use a version of
    * <code>setBlob</code> which takes a length parameter.
    *
    * @param parameterIndex index of the first parameter is 1,
    * the second is 2, ...
    * @param inputStream An object that contains the data to set the parameter
    * value to.
    * @throws SQLException if a database access error occurs,
    * this method is called on a closed <code>PreparedStatement</code> or
    * if parameterIndex does not correspond
    * to a parameter marker in the SQL statement,
    * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
    *
    * @since 1.6
    */
    public void setBlob(int parameterIndex, InputStream inputStream)
       throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
    }

    /**
     * Sets the designated parameter to a <code>InputStream</code> object.
     * The <code>Inputstream</code> must contain  the number
     * of characters specified by length, otherwise a <code>SQLException</code> will be
     * generated when the <code>CallableStatement</code> is executed.
     * This method differs from the <code>setBinaryStream (int, InputStream, int)</code>
     * method because it informs the driver that the parameter value should be
     * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
     * the driver may have to do extra work to determine whether the parameter
     * data should be sent to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
     *
     * @param parameterName the name of the parameter to be set
     * the second is 2, ...
     *
     * @param inputStream An object that contains the data to set the parameter
     * value to.
     * @param length the number of bytes in the parameter data.
     * @throws SQLException  if parameterIndex does not correspond
     * to a parameter marker in the SQL statement,  or if the length specified
     * is less than zero; if the number of bytes in the <code>InputStream</code> does not match
     * the specified length; if a database access error occurs or
     * this method is called on a closed <code>CallableStatement</code>
     * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
     * this method
     *
     * @since 1.6
     */
     public void setBlob(String parameterName, InputStream inputStream, long length)
        throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given <code>java.sql.Blob</code> object.
    * The driver converts this to an SQL <code>BLOB</code> value when it
    * sends it to the database.
    *
    * @param parameterName the name of the parameter
    * @param x a <code>Blob</code> object that maps an SQL <code>BLOB</code> value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @since 1.6
    */
   public void setBlob (String parameterName, Blob x) throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to a <code>InputStream</code> object.
    * This method differs from the <code>setBinaryStream (int, InputStream)</code>
    * method because it informs the driver that the parameter value should be
    * sent to the server as a <code>BLOB</code>.  When the <code>setBinaryStream</code> method is used,
    * the driver may have to do extra work to determine whether the parameter
    * data should be send to the server as a <code>LONGVARBINARY</code> or a <code>BLOB</code>
    *
    * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
    * it might be more efficient to use a version of
    * <code>setBlob</code> which takes a length parameter.
    *
    * @param parameterName the name of the parameter
    * @param inputStream An object that contains the data to set the parameter
    * value to.
    * @throws SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
    *
    * @since 1.6
    */
    public void setBlob(String parameterName, InputStream inputStream)
       throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
    }

   /**
    * Sets the designated parameter to a <code>Reader</code> object.
    * The reader must contain  the number
    * of characters specified by length otherwise a <code>SQLException</code> will be
    * generated when the <code>PreparedStatement</code> is executed.
    * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
    * because it informs the driver that the parameter value should be sent to
    * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
    * driver may have to do extra work to determine whether the parameter
    * data should be sent to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
    * @param parameterIndex index of the first parameter is 1, the second is 2, ...
    * @param reader An object that contains the data to set the parameter value to.
    * @param length the number of characters in the parameter data.
    * @throws SQLException if a database access error occurs, this method is called on
    * a closed <code>PreparedStatement</code>, if parameterIndex does not correspond to a parameter
    * marker in the SQL statement, or if the length specified is less than zero.
    *
    * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
    * @since 1.6
    */
   public void setClob(int parameterIndex, Reader reader, long length)
     throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

  /**
   * Sets the designated parameter to a <code>Reader</code> object.
   * This method differs from the <code>setCharacterStream (int, Reader)</code> method
   * because it informs the driver that the parameter value should be sent to
   * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
   * driver may have to do extra work to determine whether the parameter
   * data should be sent to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
   *
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>setClob</code> which takes a length parameter.
   *
   * @param parameterIndex index of the first parameter is 1, the second is 2, ...
   * @param reader An object that contains the data to set the parameter value to.
   * @throws SQLException if a database access error occurs, this method is called on
   * a closed <code>PreparedStatement</code>or if parameterIndex does not correspond to a parameter
   * marker in the SQL statement
   *
   * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
   * @since 1.6
   */
   public void setClob(int parameterIndex, Reader reader)
     throws SQLException{
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to a <code>Reader</code> object.
    * The <code>reader</code> must contain  the number
    * of characters specified by length otherwise a <code>SQLException</code> will be
    * generated when the <code>CallableStatement</code> is executed.
    * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
    * because it informs the driver that the parameter value should be sent to
    * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
    * driver may have to do extra work to determine whether the parameter
    * data should be send to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
    * @param parameterName the name of the parameter to be set
    * @param reader An object that contains the data to set the parameter value to.
    * @param length the number of characters in the parameter data.
    * @throws SQLException if parameterIndex does not correspond to a parameter
    * marker in the SQL statement; if the length specified is less than zero;
    * a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    *
    * @since 1.6
    */
   public void setClob(String parameterName, Reader reader, long length)
      throws SQLException {
       throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given <code>java.sql.Clob</code> object.
    * The driver converts this to an SQL <code>CLOB</code> value when it
    * sends it to the database.
    *
    * @param parameterName the name of the parameter
    * @param x a <code>Clob</code> object that maps an SQL <code>CLOB</code> value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @since 1.6
    */
   public void setClob (String parameterName, Clob x) throws SQLException {
       throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to a <code>Reader</code> object.
    * This method differs from the <code>setCharacterStream (int, Reader)</code> method
    * because it informs the driver that the parameter value should be sent to
    * the server as a <code>CLOB</code>.  When the <code>setCharacterStream</code> method is used, the
    * driver may have to do extra work to determine whether the parameter
    * data should be send to the server as a <code>LONGVARCHAR</code> or a <code>CLOB</code>
    *
    * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
    * it might be more efficient to use a version of
    * <code>setClob</code> which takes a length parameter.
    *
    * @param parameterName the name of the parameter
    * @param reader An object that contains the data to set the parameter value to.
    * @throws SQLException if a database access error occurs or this method is called on
    * a closed <code>CallableStatement</code>
    *
    * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
    * @since 1.6
    */
    public void setClob(String parameterName, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("Feature not supported");
    }

   /**
    * Sets the designated parameter to the given <code>java.sql.Date</code> value
    * using the default time zone of the virtual machine that is running
    * the application.
    * The driver converts this
    * to an SQL <code>DATE</code> value when it sends it to the database.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setDate(String parameterName, java.sql.Date x)
           throws SQLException {
       throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given <code>java.sql.Date</code> value,
    * using the given <code>Calendar</code> object.  The driver uses
    * the <code>Calendar</code> object to construct an SQL <code>DATE</code> value,
    * which the driver then sends to the database.  With a
    * a <code>Calendar</code> object, the driver can calculate the date
    * taking into account a custom timezone.  If no
    * <code>Calendar</code> object is specified, the driver uses the default
    * timezone, which is that of the virtual machine running the application.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @param cal the <code>Calendar</code> object the driver will use
    *            to construct the date
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setDate(String parameterName, java.sql.Date x, Calendar cal)
           throws SQLException {
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given <code>java.sql.Time</code> value.
    * The driver converts this
    * to an SQL <code>TIME</code> value when it sends it to the database.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setTime(String parameterName, java.sql.Time x)
           throws SQLException {
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given <code>java.sql.Time</code> value,
    * using the given <code>Calendar</code> object.  The driver uses
    * the <code>Calendar</code> object to construct an SQL <code>TIME</code> value,
    * which the driver then sends to the database.  With a
    * a <code>Calendar</code> object, the driver can calculate the time
    * taking into account a custom timezone.  If no
    * <code>Calendar</code> object is specified, the driver uses the default
    * timezone, which is that of the virtual machine running the application.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @param cal the <code>Calendar</code> object the driver will use
    *            to construct the time
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setTime(String parameterName, java.sql.Time x, Calendar cal)
           throws SQLException {
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given <code>java.sql.Timestamp</code> value,
    * using the given <code>Calendar</code> object.  The driver uses
    * the <code>Calendar</code> object to construct an SQL <code>TIMESTAMP</code> value,
    * which the driver then sends to the database.  With a
    * a <code>Calendar</code> object, the driver can calculate the timestamp
    * taking into account a custom timezone.  If no
    * <code>Calendar</code> object is specified, the driver uses the default
    * timezone, which is that of the virtual machine running the application.
    *
    * @param parameterName the name of the parameter
    * @param x the parameter value
    * @param cal the <code>Calendar</code> object the driver will use
    *            to construct the timestamp
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @see #getParams
    */
   public void setTimestamp(String parameterName, java.sql.Timestamp x, Calendar cal)
           throws SQLException {
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given <code>java.sql.SQLXML</code> object. The driver converts this to an
    * SQL <code>XML</code> value when it sends it to the database.
    * @param parameterIndex index of the first parameter is 1, the second is 2, ...
    * @param xmlObject a <code>SQLXML</code> object that maps an SQL <code>XML</code> value
    * @throws SQLException if a database access error occurs, this method
    *  is called on a closed result set,
    * the <code>java.xml.transform.Result</code>,
    *  <code>Writer</code> or <code>OutputStream</code> has not been closed
    * for the <code>SQLXML</code> object  or
    *  if there is an error processing the XML value.  The <code>getCause</code> method
    *  of the exception may provide a more detailed exception, for example, if the
    *  stream does not contain valid XML.
    * @throws SQLFeatureNotSupportedException if the JDBC driver does not
    * support this method
    * @since 1.6
    */
   public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
       throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given <code>java.sql.SQLXML</code> object. The driver converts this to an
    * <code>SQL XML</code> value when it sends it to the database.
    * @param parameterName the name of the parameter
    * @param xmlObject a <code>SQLXML</code> object that maps an <code>SQL XML</code> value
    * @throws SQLException if a database access error occurs, this method
    *  is called on a closed result set,
    * the <code>java.xml.transform.Result</code>,
    *  <code>Writer</code> or <code>OutputStream</code> has not been closed
    * for the <code>SQLXML</code> object  or
    *  if there is an error processing the XML value.  The <code>getCause</code> method
    *  of the exception may provide a more detailed exception, for example, if the
    *  stream does not contain valid XML.
    * @throws SQLFeatureNotSupportedException if the JDBC driver does not
    * support this method
    * @since 1.6
    */
   public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
       throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
   * Sets the designated parameter to the given <code>java.sql.RowId</code> object. The
   * driver converts this to a SQL <code>ROWID</code> value when it sends it
   * to the database
   *
   * @param parameterIndex the first parameter is 1, the second is 2, ...
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not
   * support this method
   *
   * @since 1.6
   */
  public void setRowId(int parameterIndex, RowId x) throws SQLException {
      throw new SQLFeatureNotSupportedException("Feature not supported");
  }

  /**
   * Sets the designated parameter to the given <code>java.sql.RowId</code> object. The
   * driver converts this to a SQL <code>ROWID</code> when it sends it to the
   * database.
   *
   * @param parameterName the name of the parameter
   * @param x the parameter value
   * @throws SQLException if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not
   * support this method
   * @since 1.6
   */
  public void setRowId(String parameterName, RowId x) throws SQLException {
      throw new SQLFeatureNotSupportedException("Feature not supported");
  }

  /**
   * Sets the designated parameter to the given <code>String</code> object.
   * The driver converts this to a SQL <code>NCHAR</code> or
   * <code>NVARCHAR</code> or <code>LONGNVARCHAR</code> value
   * (depending on the argument's
   * size relative to the driver's limits on <code>NVARCHAR</code> values)
   * when it sends it to the database.
   *
   * @param parameterIndex of the first parameter is 1, the second is 2, ...
   * @param value the parameter value
   * @throws SQLException if the driver does not support national
   * character sets;  if the driver can detect that a data conversion
   * error could occur ; or if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not
   * support this method
   * @since 1.6
   */
  public void setNString(int parameterIndex, String value) throws SQLException {
      throw new SQLFeatureNotSupportedException("Feature not supported");
  }

  /**
   * Sets the designated parameter to the given <code>String</code> object.
   * The driver converts this to a SQL <code>NCHAR</code> or
   * <code>NVARCHAR</code> or <code>LONGNVARCHAR</code>
   * @param parameterName the name of the column to be set
   * @param value the parameter value
   * @throws SQLException if the driver does not support national
   * character sets;  if the driver can detect that a data conversion
   * error could occur; or if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not
   * support this method
   * @since 1.6
   */
  public void setNString(String parameterName, String value) throws SQLException {
     throw new SQLFeatureNotSupportedException("Feature not supported");
  }

  /**
   * Sets the designated parameter to a <code>Reader</code> object. The
   * <code>Reader</code> reads the data till end-of-file is reached. The
   * driver does the necessary conversion from Java character format to
   * the national character set in the database.
   * @param parameterIndex of the first parameter is 1, the second is 2, ...
   * @param value the parameter value
   * @param length the number of characters in the parameter data.
   * @throws SQLException if the driver does not support national
   *         character sets;  if the driver can detect that a data conversion
   *  error could occur ; or if a database access error occurs
   * @throws SQLFeatureNotSupportedException if the JDBC driver does not
   * support this method
   * @since 1.6
   */
  public void setNCharacterStream(int parameterIndex, Reader value, long length)
          throws SQLException {
      throw new SQLFeatureNotSupportedException("Feature not supported");
  }

  /**
   * Sets the designated parameter to a <code>Reader</code> object. The
   * <code>Reader</code> reads the data till end-of-file is reached. The
   * driver does the necessary conversion from Java character format to
   * the national character set in the database.
   * @param parameterName the name of the column to be set
   * @param value the parameter value
   * @param length the number of characters in the parameter data.
   * @throws SQLException if the driver does not support national
   *         character sets;  if the driver can detect that a data conversion
   *  error could occur; or if a database access error occurs
   * @throws SQLFeatureNotSupportedException  if the JDBC driver does not
   * support this method
   * @since 1.6
   */
  public void setNCharacterStream(String parameterName, Reader value, long length)
          throws SQLException {
      throw new SQLFeatureNotSupportedException("Feature not supported");
  }

  /**
   * Sets the designated parameter to a <code>Reader</code> object. The
   * <code>Reader</code> reads the data till end-of-file is reached. The
   * driver does the necessary conversion from Java character format to
   * the national character set in the database.
   * <P><B>Note:</B> This stream object can either be a standard
   * Java stream object or your own subclass that implements the
   * standard interface.
   * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
   * it might be more efficient to use a version of
   * <code>setNCharacterStream</code> which takes a length parameter.
   *
   * @param parameterName the name of the parameter
   * @param value the parameter value
   * @throws SQLException if the driver does not support national
   *         character sets;  if the driver can detect that a data conversion
   *  error could occur ; if a database access error occurs; or
   * this method is called on a closed <code>CallableStatement</code>
   * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
   * @since 1.6
   */
  public void setNCharacterStream(String parameterName, Reader value)
          throws SQLException {
      throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to a <code>java.sql.NClob</code> object. The object
    * implements the <code>java.sql.NClob</code> interface. This <code>NClob</code>
    * object maps to a SQL <code>NCLOB</code>.
    * @param parameterName the name of the column to be set
    * @param value the parameter value
    * @throws SQLException if the driver does not support national
    *         character sets;  if the driver can detect that a data conversion
    *  error could occur; or if a database access error occurs
    * @throws SQLFeatureNotSupportedException  if the JDBC driver does not
    * support this method
    * @since 1.6
    */
   public void setNClob(String parameterName, NClob value) throws SQLException {
       throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to a <code>Reader</code> object.  The <code>reader</code> must contain
    * the number
    * of characters specified by length otherwise a <code>SQLException</code> will be
    * generated when the <code>CallableStatement</code> is executed.
    * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
    * because it informs the driver that the parameter value should be sent to
    * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
    * driver may have to do extra work to determine whether the parameter
    * data should be send to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
    *
    * @param parameterName the name of the parameter to be set
    * @param reader An object that contains the data to set the parameter value to.
    * @param length the number of characters in the parameter data.
    * @throws SQLException if parameterIndex does not correspond to a parameter
    * marker in the SQL statement; if the length specified is less than zero;
    * if the driver does not support national
    *         character sets;  if the driver can detect that a data conversion
    *  error could occur; if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @exception SQLFeatureNotSupportedException if the JDBC driver does not support
    * this method
    * @since 1.6
    */
   public void setNClob(String parameterName, Reader reader, long length)
           throws SQLException {
       throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to a <code>Reader</code> object.
    * This method differs from the <code>setCharacterStream (int, Reader)</code> method
    * because it informs the driver that the parameter value should be sent to
    * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
    * driver may have to do extra work to determine whether the parameter
    * data should be send to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
    * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
    * it might be more efficient to use a version of
    * <code>setNClob</code> which takes a length parameter.
    *
    * @param parameterName the name of the parameter
    * @param reader An object that contains the data to set the parameter value to.
    * @throws SQLException if the driver does not support national character sets;
    * if the driver can detect that a data conversion
    *  error could occur;  if a database access error occurs or
    * this method is called on a closed <code>CallableStatement</code>
    * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
    *
    * @since 1.6
    */
   public void setNClob(String parameterName, Reader reader) throws SQLException {
       throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to a <code>Reader</code> object.  The reader must contain  the number
    * of characters specified by length otherwise a <code>SQLException</code> will be
    * generated when the <code>PreparedStatement</code> is executed.
    * This method differs from the <code>setCharacterStream (int, Reader, int)</code> method
    * because it informs the driver that the parameter value should be sent to
    * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
    * driver may have to do extra work to determine whether the parameter
    * data should be sent to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
    * @param parameterIndex index of the first parameter is 1, the second is 2, ...
    * @param reader An object that contains the data to set the parameter value to.
    * @param length the number of characters in the parameter data.
    * @throws SQLException if parameterIndex does not correspond to a parameter
    * marker in the SQL statement; if the length specified is less than zero;
    * if the driver does not support national character sets;
    * if the driver can detect that a data conversion
    *  error could occur;  if a database access error occurs or
    * this method is called on a closed <code>PreparedStatement</code>
    * @throws SQLFeatureNotSupportedException  if the JDBC driver does not
    * support this method
    *
    * @since 1.6
    */
   public void setNClob(int parameterIndex, Reader reader, long length)
           throws SQLException {
       throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to a <code>java.sql.NClob</code> object. The driver converts this oa
    * SQL <code>NCLOB</code> value when it sends it to the database.
    * @param parameterIndex of the first parameter is 1, the second is 2, ...
    * @param value the parameter value
    * @throws SQLException if the driver does not support national
    *         character sets;  if the driver can detect that a data conversion
    *  error could occur ; or if a database access error occurs
    * @throws SQLFeatureNotSupportedException  if the JDBC driver does not
    * support this method
    * @since 1.6
    */
   public void setNClob(int parameterIndex, NClob value) throws SQLException {
        throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to a <code>Reader</code> object.
    * This method differs from the <code>setCharacterStream (int, Reader)</code> method
    * because it informs the driver that the parameter value should be sent to
    * the server as a <code>NCLOB</code>.  When the <code>setCharacterStream</code> method is used, the
    * driver may have to do extra work to determine whether the parameter
    * data should be sent to the server as a <code>LONGNVARCHAR</code> or a <code>NCLOB</code>
    * <P><B>Note:</B> Consult your JDBC driver documentation to determine if
    * it might be more efficient to use a version of
    * <code>setNClob</code> which takes a length parameter.
    *
    * @param parameterIndex index of the first parameter is 1, the second is 2, ...
    * @param reader An object that contains the data to set the parameter value to.
    * @throws SQLException if parameterIndex does not correspond to a parameter
    * marker in the SQL statement;
    * if the driver does not support national character sets;
    * if the driver can detect that a data conversion
    *  error could occur;  if a database access error occurs or
    * this method is called on a closed <code>PreparedStatement</code>
    * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
    *
    * @since 1.6
    */
   public void setNClob(int parameterIndex, Reader reader)throws SQLException {
       throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   /**
    * Sets the designated parameter to the given <code>java.net.URL</code> value.
    * The driver converts this to an SQL <code>DATALINK</code> value
    * when it sends it to the database.
    *
    * @param parameterIndex the first parameter is 1, the second is 2, ...
    * @param x the <code>java.net.URL</code> object to be set
    * @exception SQLException if a database access error occurs or
    * this method is called on a closed <code>PreparedStatement</code>
    * @throws SQLFeatureNotSupportedException  if the JDBC driver does not support this method
    */
   public void setURL(int parameterIndex, java.net.URL x) throws SQLException {
       throw new SQLFeatureNotSupportedException("Feature not supported");
   }

   static final long serialVersionUID = 4886719666485113312L;

} //end class
