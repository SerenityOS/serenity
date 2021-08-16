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
import java.util.*;

import javax.sql.rowset.spi.*;

/**
 * The interface that all standard implementations of
 * <code>CachedRowSet</code> must implement.
 * <P>
 * The reference implementation of the <code>CachedRowSet</code> interface provided
 * by Oracle Corporation is a standard implementation. Developers may use this implementation
 * just as it is, they may extend it, or they may choose to write their own implementations
 * of this interface.
 * <P>
 * A <code>CachedRowSet</code> object is a container for rows of data
 * that caches its rows in memory, which makes it possible to operate without always being
 * connected to its data source. Further, it is a
 * JavaBeans component and is scrollable,
 * updatable, and serializable. A <code>CachedRowSet</code> object typically
 * contains rows from a result set, but it can also contain rows from any file
 * with a tabular format, such as a spread sheet.  The reference implementation
 * supports getting data only from a <code>ResultSet</code> object, but
 * developers can extend the <code>SyncProvider</code> implementations to provide
 * access to other tabular data sources.
 * <P>
 * An application can modify the data in a <code>CachedRowSet</code> object, and
 * those modifications can then be propagated back to the source of the data.
 * <P>
 * A <code>CachedRowSet</code> object is a <i>disconnected</i> rowset, which means
 * that it makes use of a connection to its data source only briefly. It connects to its
 * data source while it is reading data to populate itself with rows and again
 * while it is propagating changes back to its underlying data source. The rest
 * of the time, a <code>CachedRowSet</code> object is disconnected, including
 * while its data is being modified. Being disconnected makes a <code>RowSet</code>
 * object much leaner and therefore much easier to pass to another component.  For
 * example, a disconnected <code>RowSet</code> object can be serialized and passed
 * over the wire to a thin client such as a personal digital assistant (PDA).
 *
 *
 * <h2>1.0 Creating a <code>CachedRowSet</code> Object</h2>
 * The following line of code uses the default constructor for
 * <code>CachedRowSet</code>
 * supplied in the reference implementation (RI) to create a default
 * <code>CachedRowSet</code> object.
 * <PRE>
 *     CachedRowSetImpl crs = new CachedRowSetImpl();
 * </PRE>
 * This new <code>CachedRowSet</code> object will have its properties set to the
 * default properties of a <code>BaseRowSet</code> object, and, in addition, it will
 * have an <code>RIOptimisticProvider</code> object as its synchronization provider.
 * <code>RIOptimisticProvider</code>, one of two <code>SyncProvider</code>
 * implementations included in the RI, is the default provider that the
 * <code>SyncFactory</code> singleton will supply when no synchronization
 * provider is specified.
 * <P>
 * A <code>SyncProvider</code> object provides a <code>CachedRowSet</code> object
 * with a reader (a <code>RowSetReader</code> object) for reading data from a
 * data source to populate itself with data. A reader can be implemented to read
 * data from a <code>ResultSet</code> object or from a file with a tabular format.
 * A <code>SyncProvider</code> object also provides
 * a writer (a <code>RowSetWriter</code> object) for synchronizing any
 * modifications to the <code>CachedRowSet</code> object's data made while it was
 * disconnected with the data in the underlying data source.
 * <P>
 * A writer can be implemented to exercise various degrees of care in checking
 * for conflicts and in avoiding them.
 * (A conflict occurs when a value in the data source has been changed after
 * the rowset populated itself with that value.)
 * The <code>RIOptimisticProvider</code> implementation assumes there will be
 * few or no conflicts and therefore sets no locks. It updates the data source
 * with values from the <code>CachedRowSet</code> object only if there are no
 * conflicts.
 * Other writers can be implemented so that they always write modified data to
 * the data source, which can be accomplished either by not checking for conflicts
 * or, on the other end of the spectrum, by setting locks sufficient to prevent data
 * in the data source from being changed. Still other writer implementations can be
 * somewhere in between.
 * <P>
 * A <code>CachedRowSet</code> object may use any
 * <code>SyncProvider</code> implementation that has been registered
 * with the <code>SyncFactory</code> singleton. An application
 * can find out which <code>SyncProvider</code> implementations have been
 * registered by calling the following line of code.
 * <PRE>
 *      java.util.Enumeration providers = SyncFactory.getRegisteredProviders();
 * </PRE>
 * <P>
 * There are two ways for a <code>CachedRowSet</code> object to specify which
 * <code>SyncProvider</code> object it will use.
 * <UL>
 *     <LI>Supplying the name of the implementation to the constructor<BR>
 *     The following line of code creates the <code>CachedRowSet</code>
 *     object <i>crs2</i> that is initialized with default values except that its
 *     <code>SyncProvider</code> object is the one specified.
 *     <PRE>
 *          CachedRowSetImpl crs2 = new CachedRowSetImpl(
 *                                 "com.fred.providers.HighAvailabilityProvider");
 *     </PRE>
 *     <LI>Setting the <code>SyncProvider</code> using the <code>CachedRowSet</code>
 *         method <code>setSyncProvider</code><BR>
 *      The following line of code resets the <code>SyncProvider</code> object
 *      for <i>crs</i>, the <code>CachedRowSet</code> object created with the
 *      default constructor.
 *      <PRE>
 *           crs.setSyncProvider("com.fred.providers.HighAvailabilityProvider");
 *      </PRE>
 * </UL>
 * See the comments for <code>SyncFactory</code> and <code>SyncProvider</code> for
 * more details.
 *
 *
 * <h2>2.0 Retrieving Data from a <code>CachedRowSet</code> Object</h2>
 * Data is retrieved from a <code>CachedRowSet</code> object by using the
 * getter methods inherited from the <code>ResultSet</code>
 * interface.  The following examples, in which <code>crs</code> is a
 * <code>CachedRowSet</code>
 * object, demonstrate how to iterate through the rows, retrieving the column
 * values in each row.  The first example uses the version of the
 * getter methods that take a column number; the second example
 * uses the version that takes a column name. Column numbers are generally
 * used when the <code>RowSet</code> object's command
 * is of the form <code>SELECT * FROM TABLENAME</code>; column names are most
 * commonly used when the command specifies columns by name.
 * <PRE>
 *    while (crs.next()) {
 *        String name = crs.getString(1);
 *        int id = crs.getInt(2);
 *        Clob comment = crs.getClob(3);
 *        short dept = crs.getShort(4);
 *        System.out.println(name + "  " + id + "  " + comment + "  " + dept);
 *    }
 * </PRE>
 *
 * <PRE>
 *    while (crs.next()) {
 *        String name = crs.getString("NAME");
 *        int id = crs.getInt("ID");
 *        Clob comment = crs.getClob("COM");
 *        short dept = crs.getShort("DEPT");
 *        System.out.println(name + "  " + id + "  " + comment + "  " + dept);
 *    }
 * </PRE>
 * <h3>2.1 Retrieving <code>RowSetMetaData</code></h3>
 * An application can get information about the columns in a <code>CachedRowSet</code>
 * object by calling <code>ResultSetMetaData</code> and <code>RowSetMetaData</code>
 * methods on a <code>RowSetMetaData</code> object. The following code fragment,
 * in which <i>crs</i> is a <code>CachedRowSet</code> object, illustrates the process.
 * The first line creates a <code>RowSetMetaData</code> object with information
 * about the columns in <i>crs</i>.  The method <code>getMetaData</code>,
 * inherited from the <code>ResultSet</code> interface, returns a
 * <code>ResultSetMetaData</code> object, which is cast to a
 * <code>RowSetMetaData</code> object before being assigned to the variable
 * <i>rsmd</i>.  The second line finds out how many columns <i>jrs</i> has, and
 * the third line gets the JDBC type of values stored in the second column of
 * <code>jrs</code>.
 * <PRE>
 *     RowSetMetaData rsmd = (RowSetMetaData)crs.getMetaData();
 *     int count = rsmd.getColumnCount();
 *     int type = rsmd.getColumnType(2);
 * </PRE>
 * The <code>RowSetMetaData</code> interface differs from the
 * <code>ResultSetMetaData</code> interface in two ways.
 * <UL>
 *   <LI><i>It includes <code>setter</code> methods:</i> A <code>RowSet</code>
 *   object uses these methods internally when it is populated with data from a
 *   different <code>ResultSet</code> object.
 *
 *   <LI><i>It contains fewer <code>getter</code> methods:</i> Some
 *   <code>ResultSetMetaData</code> methods to not apply to a <code>RowSet</code>
 *   object. For example, methods retrieving whether a column value is writable
 *   or read only do not apply because all of a <code>RowSet</code> object's
 *   columns will be writable or read only, depending on whether the rowset is
 *   updatable or not.
 * </UL>
 * NOTE: In order to return a <code>RowSetMetaData</code> object, implementations must
 * override the <code>getMetaData()</code> method defined in
 * <code>java.sql.ResultSet</code> and return a <code>RowSetMetaData</code> object.
 *
 * <h2>3.0 Updating a <code>CachedRowSet</code> Object</h2>
 * Updating a <code>CachedRowSet</code> object is similar to updating a
 * <code>ResultSet</code> object, but because the rowset is not connected to
 * its data source while it is being updated, it must take an additional step
 * to effect changes in its underlying data source. After calling the method
 * <code>updateRow</code> or <code>insertRow</code>, a
 * <code>CachedRowSet</code>
 * object must also call the method <code>acceptChanges</code> to have updates
 * written to the data source. The following example, in which the cursor is
 * on a row in the <code>CachedRowSet</code> object <i>crs</i>, shows
 * the code required to update two column values in the current row and also
 * update the <code>RowSet</code> object's underlying data source.
 * <PRE>
 *     crs.updateShort(3, 58);
 *     crs.updateInt(4, 150000);
 *     crs.updateRow();
 *     crs.acceptChanges();
 * </PRE>
 * <P>
 * The next example demonstrates moving to the insert row, building a new
 * row on the insert row, inserting it into the rowset, and then calling the
 * method <code>acceptChanges</code> to add the new row to the underlying data
 * source.  Note that as with the getter methods, the  updater methods may take
 * either a column index or a column name to designate the column being acted upon.
 * <PRE>
 *     crs.moveToInsertRow();
 *     crs.updateString("Name", "Shakespeare");
 *     crs.updateInt("ID", 10098347);
 *     crs.updateShort("Age", 58);
 *     crs.updateInt("Sal", 150000);
 *     crs.insertRow();
 *     crs.moveToCurrentRow();
 *     crs.acceptChanges();
 * </PRE>
 * <P>
 * NOTE: Where the <code>insertRow()</code> method inserts the contents of a
 * <code>CachedRowSet</code> object's insert row is implementation-defined.
 * The reference implementation for the <code>CachedRowSet</code> interface
 * inserts a new row immediately following the current row, but it could be
 * implemented to insert new rows in any number of other places.
 * <P>
 * Another thing to note about these examples is how they use the method
 * <code>acceptChanges</code>.  It is this method that propagates changes in
 * a <code>CachedRowSet</code> object back to the underlying data source,
 * calling on the <code>RowSet</code> object's writer internally to write
 * changes to the data source. To do this, the writer has to incur the expense
 * of establishing a connection with that data source. The
 * preceding two code fragments call the method <code>acceptChanges</code>
 * immediately after calling <code>updateRow</code> or <code>insertRow</code>.
 * However, when there are multiple rows being changed, it is more efficient to call
 * <code>acceptChanges</code> after all calls to <code>updateRow</code>
 * and <code>insertRow</code> have been made.  If <code>acceptChanges</code>
 * is called only once, only one connection needs to be established.
 *
 * <h2>4.0 Updating the Underlying Data Source</h2>
 * When the method <code>acceptChanges</code> is executed, the
 * <code>CachedRowSet</code> object's writer, a <code>RowSetWriterImpl</code>
 * object, is called behind the scenes to write the changes made to the
 * rowset to the underlying data source. The writer is implemented to make a
 * connection to the data source and write updates to it.
 * <P>
 * A writer is made available through an implementation of the
 * <code>SyncProvider</code> interface, as discussed in section 1,
 * "Creating a <code>CachedRowSet</code> Object."
 * The default reference implementation provider, <code>RIOptimisticProvider</code>,
 * has its writer implemented to use an optimistic concurrency control
 * mechanism. That is, it maintains no locks in the underlying database while
 * the rowset is disconnected from the database and simply checks to see if there
 * are any conflicts before writing data to the data source.  If there are any
 * conflicts, it does not write anything to the data source.
 * <P>
 * The reader/writer facility
 * provided by the <code>SyncProvider</code> class is pluggable, allowing for the
 * customization of data retrieval and updating. If a different concurrency
 * control mechanism is desired, a different implementation of
 * <code>SyncProvider</code> can be plugged in using the method
 * <code>setSyncProvider</code>.
 * <P>
 * In order to use the optimistic concurrency control routine, the
 * <code>RIOptimisticProvider</code> maintains both its current
 * value and its original value (the value it had immediately preceding the
 * current value). Note that if no changes have been made to the data in a
 * <code>RowSet</code> object, its current values and its original values are the same,
 * both being the values with which the <code>RowSet</code> object was initially
 * populated.  However, once any values in the <code>RowSet</code> object have been
 * changed, the current values and the original values will be different, though at
 * this stage, the original values are still the initial values. With any subsequent
 * changes to data in a <code>RowSet</code> object, its original values and current
 * values will still differ, but its original values will be the values that
 * were previously the current values.
 * <P>
 * Keeping track of original values allows the writer to compare the <code>RowSet</code>
 * object's original value with the value in the database. If the values in
 * the database differ from the <code>RowSet</code> object's original values, which means that
 * the values in the database have been changed, there is a conflict.
 * Whether a writer checks for conflicts, what degree of checking it does, and how
 * it handles conflicts all depend on how it is implemented.
 *
 * <h2>5.0 Registering and Notifying Listeners</h2>
 * Being JavaBeans components, all rowsets participate in the JavaBeans event
 * model, inheriting methods for registering listeners and notifying them of
 * changes from the <code>BaseRowSet</code> class.  A listener for a
 * <code>CachedRowSet</code> object is a component that wants to be notified
 * whenever there is a change in the rowset.  For example, if a
 * <code>CachedRowSet</code> object contains the results of a query and
 * those
 * results are being displayed in, say, a table and a bar graph, the table and
 * bar graph could be registered as listeners with the rowset so that they can
 * update themselves to reflect changes. To become listeners, the table and
 * bar graph classes must implement the <code>RowSetListener</code> interface.
 * Then they can be added to the <Code>CachedRowSet</code> object's list of
 * listeners, as is illustrated in the following lines of code.
 * <PRE>
 *    crs.addRowSetListener(table);
 *    crs.addRowSetListener(barGraph);
 * </PRE>
 * Each <code>CachedRowSet</code> method that moves the cursor or changes
 * data also notifies registered listeners of the changes, so
 * <code>table</code> and <code>barGraph</code> will be notified when there is
 * a change in <code>crs</code>.
 *
 * <h2>6.0 Passing Data to Thin Clients</h2>
 * One of the main reasons to use a <code>CachedRowSet</code> object is to
 * pass data between different components of an application. Because it is
 * serializable, a <code>CachedRowSet</code> object can be used, for example,
 * to send the result of a query executed by an enterprise JavaBeans component
 * running in a server environment over a network to a client running in a
 * web browser.
 * <P>
 * While a <code>CachedRowSet</code> object is disconnected, it can be much
 * leaner than a <code>ResultSet</code> object with the same data.
 * As a result, it can be especially suitable for sending data to a thin client
 * such as a PDA, where it would be inappropriate to use a JDBC driver
 * due to resource limitations or security considerations.
 * Thus, a <code>CachedRowSet</code> object provides a means to "get rows in"
 * without the need to implement the full JDBC API.
 *
 * <h2>7.0 Scrolling and Updating</h2>
 * A second major use for <code>CachedRowSet</code> objects is to provide
 * scrolling and updating for <code>ResultSet</code> objects that
 * do not provide these capabilities themselves.  In other words, a
 * <code>CachedRowSet</code> object can be used to augment the
 * capabilities of a JDBC technology-enabled driver (hereafter called a
 * "JDBC driver") when the DBMS does not provide full support for scrolling and
 * updating. To achieve the effect of making a non-scrollable and read-only
 * <code>ResultSet</code> object scrollable and updatable, a programmer
 * simply needs to create a <code>CachedRowSet</code> object populated
 * with that <code>ResultSet</code> object's data.  This is demonstrated
 * in the following code fragment, where <code>stmt</code> is a
 * <code>Statement</code> object.
 * <PRE>
 *    ResultSet rs = stmt.executeQuery("SELECT * FROM EMPLOYEES");
 *    CachedRowSetImpl crs = new CachedRowSetImpl();
 *    crs.populate(rs);
 * </PRE>
 * <P>
 * The object <code>crs</code> now contains the data from the table
 * <code>EMPLOYEES</code>, just as the object <code>rs</code> does.
 * The difference is that the cursor for <code>crs</code> can be moved
 * forward, backward, or to a particular row even if the cursor for
 * <code>rs</code> can move only forward.  In addition, <code>crs</code> is
 * updatable even if <code>rs</code> is not because by default, a
 * <code>CachedRowSet</code> object is both scrollable and updatable.
 * <P>
 * In summary, a <code>CachedRowSet</code> object can be thought of as simply
 * a disconnected set of rows that are being cached outside of a data source.
 * Being thin and serializable, it can easily be sent across a wire,
 * and it is well suited to sending data to a thin client. However, a
 * <code>CachedRowSet</code> object does have a limitation: It is limited in
 * size by the amount of data it can store in memory at one time.
 *
 * <h2>8.0 Getting Universal Data Access</h2>
 * Another advantage of the <code>CachedRowSet</code> class is that it makes it
 * possible to retrieve and store data from sources other than a relational
 * database. The reader for a rowset can be implemented to read and populate
 * its rowset with data from any tabular data source, including a spreadsheet
 * or flat file.
 * Because both a <code>CachedRowSet</code> object and its metadata can be
 * created from scratch, a component that acts as a factory for rowsets
 * can use this capability to create a rowset containing data from
 * non-SQL data sources. Nevertheless, it is expected that most of the time,
 * <code>CachedRowSet</code> objects will contain data that was fetched
 * from an SQL database using the JDBC API.
 *
 * <h2>9.0 Setting Properties</h2>
 * All rowsets maintain a set of properties, which will usually be set using
 * a tool.  The number and kinds of properties a rowset has will vary,
 * depending on what the rowset does and how it gets its data.  For example,
 * rowsets that get their data from a <code>ResultSet</code> object need to
 * set the properties that are required for making a database connection.
 * If a rowset uses the <code>DriverManager</code> facility to make a
 * connection, it needs to set a property for the JDBC URL that identifies
 * the appropriate driver, and it needs to set the properties that give the
 * user name and password.
 * If, on the other hand, the rowset uses a <code>DataSource</code> object
 * to make the connection, which is the preferred method, it does not need to
 * set the property for the JDBC URL.  Instead, it needs to set
 * properties for the logical name of the data source, for the user name,
 * and for the password.
 * <P>
 * NOTE:  In order to use a <code>DataSource</code> object for making a
 * connection, the <code>DataSource</code> object must have been registered
 * with a naming service that uses the Java Naming and Directory
 * Interface (JNDI) API.  This registration
 * is usually done by a person acting in the capacity of a system
 * administrator.
 * <P>
 * In order to be able to populate itself with data from a database, a rowset
 * needs to set a command property.  This property is a query that is a
 * <code>PreparedStatement</code> object, which allows the query to have
 * parameter placeholders that are set at run time, as opposed to design time.
 * To set these placeholder parameters with values, a rowset provides
 * setter methods for setting values of each data type,
 * similar to the setter methods provided by the <code>PreparedStatement</code>
 * interface.
 * <P>
 * The following code fragment illustrates how the <code>CachedRowSet</code>
 * object <code>crs</code> might have its command property set.  Note that if a
 * tool is used to set properties, this is the code that the tool would use.
 * <PRE>{@code
 *    crs.setCommand("SELECT FIRST_NAME, LAST_NAME, ADDRESS FROM CUSTOMERS " +
 *                   "WHERE CREDIT_LIMIT > ? AND REGION = ?");
 * } </PRE>
 * <P>
 * The values that will be used to set the command's placeholder parameters are
 * contained in the <code>RowSet</code> object's <code>params</code> field, which is a
 * <code>Vector</code> object.
 * The <code>CachedRowSet</code> class provides a set of setter
 * methods for setting the elements in its <code>params</code> field.  The
 * following code fragment demonstrates setting the two parameters in the
 * query from the previous example.
 * <PRE>
 *    crs.setInt(1, 5000);
 *    crs.setString(2, "West");
 * </PRE>
 * <P>
 * The <code>params</code> field now contains two elements, each of which is
 * an array two elements long.  The first element is the parameter number;
 * the second is the value to be set.
 * In this case, the first element of <code>params</code> is
 * <code>1</code>, <code>5000</code>, and the second element is <code>2</code>,
 * <code>"West"</code>.  When an application calls the method
 * <code>execute</code>, it will in turn call on this <code>RowSet</code> object's reader,
 * which will in turn invoke its <code>readData</code> method. As part of
 * its implementation, <code>readData</code> will get the values in
 * <code>params</code> and use them to set the command's placeholder
 * parameters.
 * The following code fragment gives an idea of how the reader
 * does this, after obtaining the <code>Connection</code> object
 * <code>con</code>.
 * <PRE>{@code
 *    PreparedStatement pstmt = con.prepareStatement(crs.getCommand());
 *    reader.decodeParams();
 *    // decodeParams figures out which setter methods to use and does something
 *    // like the following:
 *    //    for (i = 0; i < params.length; i++) {
 *    //        pstmt.setObject(i + 1, params[i]);
 *    //    }
 * }</PRE>
 * <P>
 * At this point, the command for <code>crs</code> is the query {@code "SELECT
 * FIRST_NAME, LAST_NAME, ADDRESS FROM CUSTOMERS WHERE CREDIT_LIMIT > 5000
 * AND REGION = "West"}.  After the <code>readData</code> method executes
 * this command with the following line of code, it will have the data from
 * <code>rs</code> with which to populate <code>crs</code>.
 * <PRE>{@code
 *     ResultSet rs = pstmt.executeQuery();
 * }</PRE>
 * <P>
 * The preceding code fragments give an idea of what goes on behind the
 * scenes; they would not appear in an application, which would not invoke
 * methods like <code>readData</code> and <code>decodeParams</code>.
 * In contrast, the following code fragment shows what an application might do.
 * It sets the rowset's command, sets the command's parameters, and executes
 * the command. Simply by calling the <code>execute</code> method,
 * <code>crs</code> populates itself with the requested data from the
 * table <code>CUSTOMERS</code>.
 * <PRE>{@code
 *    crs.setCommand("SELECT FIRST_NAME, LAST_NAME, ADDRESS FROM CUSTOMERS" +
 *                   "WHERE CREDIT_LIMIT > ? AND REGION = ?");
 *    crs.setInt(1, 5000);
 *    crs.setString(2, "West");
 *    crs.execute();
 * }</PRE>
 *
 * <h2>10.0 Paging Data</h2>
 * Because a <code>CachedRowSet</code> object stores data in memory,
 * the amount of data that it can contain at any one
 * time is determined by the amount of memory available. To get around this limitation,
 * a <code>CachedRowSet</code> object can retrieve data from a <code>ResultSet</code>
 * object in chunks of data, called <i>pages</i>. To take advantage of this mechanism,
 * an application sets the number of rows to be included in a page using the method
 * <code>setPageSize</code>. In other words, if the page size is set to five, a chunk
 * of five rows of
 * data will be fetched from the data source at one time. An application can also
 * optionally set the maximum number of rows that may be fetched at one time.  If the
 * maximum number of rows is set to zero, or no maximum number of rows is set, there is
 * no limit to the number of rows that may be fetched at a time.
 * <P>
 * After properties have been set,
 * the <code>CachedRowSet</code> object must be populated with data
 * using either the method <code>populate</code> or the method <code>execute</code>.
 * The following lines of code demonstrate using the method <code>populate</code>.
 * Note that this version of the method takes two parameters, a <code>ResultSet</code>
 * handle and the row in the <code>ResultSet</code> object from which to start
 * retrieving rows.
 * <PRE>
 *     CachedRowSet crs = new CachedRowSetImpl();
 *     crs.setMaxRows(20);
 *     crs.setPageSize(4);
 *     crs.populate(rsHandle, 10);
 * </PRE>
 * When this code runs, <i>crs</i> will be populated with four rows from
 * <i>rsHandle</i> starting with the tenth row.
 * <P>
 * The next code fragment shows populating a <code>CachedRowSet</code> object using the
 * method <code>execute</code>, which may or may not take a <code>Connection</code>
 * object as a parameter.  This code passes <code>execute</code> the <code>Connection</code>
 * object <i>conHandle</i>.
 * <P>
 * Note that there are two differences between the following code
 * fragment and the previous one. First, the method <code>setMaxRows</code> is not
 * called, so there is no limit set for the number of rows that <i>crs</i> may contain.
 * (Remember that <i>crs</i> always has the overriding limit of how much data it can
 * store in memory.) The second difference is that the you cannot pass the method
 * <code>execute</code> the number of the row in the <code>ResultSet</code> object
 * from which to start retrieving rows. This method always starts with the first row.
 * <PRE>
 *     CachedRowSet crs = new CachedRowSetImpl();
 *     crs.setPageSize(5);
 *     crs.execute(conHandle);
 * </PRE>
 * After this code has run, <i>crs</i> will contain five rows of data from the
 * <code>ResultSet</code> object produced by the command for <i>crs</i>. The writer
 * for <i>crs</i> will use <i>conHandle</i> to connect to the data source and
 * execute the command for <i>crs</i>. An application is then able to operate on the
 * data in <i>crs</i> in the same way that it would operate on data in any other
 * <code>CachedRowSet</code> object.
 * <P>
 * To access the next page (chunk of data), an application calls the method
 * <code>nextPage</code>.  This method creates a new <code>CachedRowSet</code> object
 * and fills it with the next page of data.  For example, assume that the
 * <code>CachedRowSet</code> object's command returns a <code>ResultSet</code> object
 * <i>rs</i> with 1000 rows of data.  If the page size has been set to 100, the first
 *  call to the method <code>nextPage</code> will create a <code>CachedRowSet</code> object
 * containing the first 100 rows of <i>rs</i>. After doing what it needs to do with the
 * data in these first 100 rows, the application can again call the method
 * <code>nextPage</code> to create another <code>CachedRowSet</code> object
 * with the second 100 rows from <i>rs</i>. The data from the first <code>CachedRowSet</code>
 * object will no longer be in memory because it is replaced with the data from the
 * second <code>CachedRowSet</code> object. After the tenth call to the method <code>nextPage</code>,
 * the tenth <code>CachedRowSet</code> object will contain the last 100 rows of data from
 * <i>rs</i>, which are stored in memory. At any given time, the data from only one
 * <code>CachedRowSet</code> object is stored in memory.
 * <P>
 * The method <code>nextPage</code> returns <code>true</code> as long as the current
 * page is not the last page of rows and <code>false</code> when there are no more pages.
 * It can therefore be used in a <code>while</code> loop to retrieve all of the pages,
 * as is demonstrated in the following lines of code.
 * <PRE>
 *     CachedRowSet crs = CachedRowSetImpl();
 *     crs.setPageSize(100);
 *     crs.execute(conHandle);
 *
 *     while(crs.nextPage()) {
 *         while(crs.next()) {
 *             . . . // operate on chunks (of 100 rows each) in crs,
 *                   // row by row
 *         }
 *     }
 * </PRE>
 * After this code fragment has been run, the application will have traversed all
 * 1000 rows, but it will have had no more than 100 rows in memory at a time.
 * <P>
 * The <code>CachedRowSet</code> interface also defines the method <code>previousPage</code>.
 * Just as the method <code>nextPage</code> is analogous to the <code>ResultSet</code>
 * method <code>next</code>, the method <code>previousPage</code> is analogous to
 * the <code>ResultSet</code> method <code>previous</code>.  Similar to the method
 * <code>nextPage</code>, <code>previousPage</code> creates a <code>CachedRowSet</code>
 * object containing the number of rows set as the page size.  So, for instance, the
 * method <code>previousPage</code> could be used in a <code>while</code> loop at
 * the end of the preceding code fragment to navigate back through the pages from the last
 * page to the first page.
 * The method <code>previousPage</code> is also similar to <code>nextPage</code>
 * in that it can be used in a <code>while</code>
 * loop, except that it returns <code>true</code> as long as there is another page
 * preceding it and <code>false</code> when there are no more pages ahead of it.
 * <P>
 * By positioning the cursor after the last row for each page,
 * as is done in the following code fragment, the method <code>previous</code>
 * navigates from the last row to the first row in each page.
 * The code could also have left the cursor before the first row on each page and then
 * used the method <code>next</code> in a <code>while</code> loop to navigate each page
 * from the first row to the last row.
 * <P>
 * The following code fragment assumes a continuation from the previous code fragment,
 * meaning that the cursor for the tenth <code>CachedRowSet</code> object is on the
 * last row.  The code moves the cursor to after the last row so that the first
 * call to the method <code>previous</code> will put the cursor back on the last row.
 * After going through all of the rows in the last page (the <code>CachedRowSet</code>
 * object <i>crs</i>), the code then enters
 * the <code>while</code> loop to get to the ninth page, go through the rows backwards,
 * go to the eighth page, go through the rows backwards, and so on to the first row
 * of the first page.
 *
 * <PRE>
 *     crs.afterLast();
 *     while(crs.previous())  {
 *         . . . // navigate through the rows, last to first
 *     {
 *     while(crs.previousPage())  {
 *         crs.afterLast();
 *         while(crs.previous())  {
 *             . . . // go from the last row to the first row of each page
 *         }
 *     }
 * </PRE>
 *
 * @author Jonathan Bruce
 * @since 1.5
 */

public interface CachedRowSet extends RowSet, Joinable {

   /**
    * Populates this <code>CachedRowSet</code> object with data from
    * the given <code>ResultSet</code> object.
    * <P>
    * This method can be used as an alternative to the <code>execute</code> method when an
    * application has a connection to an open <code>ResultSet</code> object.
    * Using the method <code>populate</code> can be more efficient than using
    * the version of the <code>execute</code> method that takes no parameters
    * because it does not open a new connection and re-execute this
    * <code>CachedRowSet</code> object's command. Using the <code>populate</code>
    * method is more a matter of convenience when compared to using the version
    * of <code>execute</code> that takes a <code>ResultSet</code> object.
    *
    * @param data the <code>ResultSet</code> object containing the data
    * to be read into this <code>CachedRowSet</code> object
    * @throws SQLException if a null <code>ResultSet</code> object is supplied
    * or this <code>CachedRowSet</code> object cannot
    * retrieve the associated <code>ResultSetMetaData</code> object
    * @see #execute
    * @see java.sql.ResultSet
    * @see java.sql.ResultSetMetaData
    */
    public void populate(ResultSet data) throws SQLException;

   /**
    * Populates this <code>CachedRowSet</code> object with data, using the
    * given connection to produce the result set from which the data will be read.
    * This method should close any database connections that it creates to
    * ensure that this <code>CachedRowSet</code> object is disconnected except when
    * it is reading data from its data source or writing data to its data source.
    * <P>
    * The reader for this <code>CachedRowSet</code> object
    * will use <i>conn</i> to establish a connection to the data source
    * so that it can execute the rowset's command and read data from the
    * the resulting <code>ResultSet</code> object into this
    * <code>CachedRowSet</code> object. This method also closes <i>conn</i>
    * after it has populated this <code>CachedRowSet</code> object.
    * <P>
    * If this method is called when an implementation has already been
    * populated, the contents and the metadata are (re)set. Also, if this method is
    * called before the method <code>acceptChanges</code> has been called
    * to commit outstanding updates, those updates are lost.
    *
    * @param conn a standard JDBC <code>Connection</code> object with valid
    * properties
    * @throws SQLException if an invalid <code>Connection</code> object is supplied
    * or an error occurs in establishing the connection to the
    * data source
    * @see #populate
    * @see java.sql.Connection
    */
    public void execute(Connection conn) throws SQLException;

   /**
    * Propagates row update, insert and delete changes made to this
    * <code>CachedRowSet</code> object to the underlying data source.
    * <P>
    * This method calls on this <code>CachedRowSet</code> object's writer
    * to do the work behind the scenes.
    * Standard <code>CachedRowSet</code> implementations should use the
    * <code>SyncFactory</code> singleton
    * to obtain a <code>SyncProvider</code> instance providing a
    * <code>RowSetWriter</code> object (writer).  The writer will attempt
    * to propagate changes made in this <code>CachedRowSet</code> object
    * back to the data source.
    * <P>
    * When the method <code>acceptChanges</code> executes successfully, in
    * addition to writing changes to the data source, it
    * makes the values in the current row be the values in the original row.
    * <P>
    * Depending on the synchronization level of the <code>SyncProvider</code>
    * implementation being used, the writer will compare the original values
    * with those in the data source to check for conflicts. When there is a conflict,
    * the <code>RIOptimisticProvider</code> implementation, for example, throws a
    * <code>SyncProviderException</code> and does not write anything to the
    * data source.
    * <P>
    * An application may choose to catch the <code>SyncProviderException</code>
    * object and retrieve the <code>SyncResolver</code> object it contains.
    * The <code>SyncResolver</code> object lists the conflicts row by row and
    * sets a lock on the data source to avoid further conflicts while the
    * current conflicts are being resolved.
    * Further, for each conflict, it provides methods for examining the conflict
    * and setting the value that should be persisted in the data source.
    * After all conflicts have been resolved, an application must call the
    * <code>acceptChanges</code> method again to write resolved values to the
    * data source.  If all of the values in the data source are already the
    * values to be persisted, the method <code>acceptChanges</code> does nothing.
    * <P>
    * Some provider implementations may use locks to ensure that there are no
    * conflicts.  In such cases, it is guaranteed that the writer will succeed in
    * writing changes to the data source when the method <code>acceptChanges</code>
    * is called.  This method may be called immediately after the methods
    * <code>updateRow</code>, <code>insertRow</code>, or <code>deleteRow</code>
    * have been called, but it is more efficient to call it only once after
    * all changes have been made so that only one connection needs to be
    * established.
    * <P>
    * Note: The <code>acceptChanges()</code> method will determine if the
    * <code>COMMIT_ON_ACCEPT_CHANGES</code> is set to true or not. If it is set
    * to true, all updates in the synchronization are committed to the data
    * source. Otherwise, the application <b>must</b> explicitly call the
    * <code>commit()</code> or <code>rollback()</code> methods as appropriate.
    *
    * @throws SyncProviderException if the underlying
    * synchronization provider's writer fails to write the updates
    * back to the data source
    * @see #acceptChanges(java.sql.Connection)
    * @see javax.sql.RowSetWriter
    * @see javax.sql.rowset.spi.SyncFactory
    * @see javax.sql.rowset.spi.SyncProvider
    * @see javax.sql.rowset.spi.SyncProviderException
    * @see javax.sql.rowset.spi.SyncResolver
    */
    public void acceptChanges() throws SyncProviderException;

   /**
    * Propagates all row update, insert and delete changes to the
    * data source backing this <code>CachedRowSet</code> object
    * using the specified <code>Connection</code> object to establish a
    * connection to the data source.
    * <P>
    * The other version of the <code>acceptChanges</code> method is not passed
    * a connection because it uses
    * the <code>Connection</code> object already defined within the <code>RowSet</code>
    * object, which is the connection used for populating it initially.
    * <P>
    * This form of the method <code>acceptChanges</code> is similar to the
    * form that takes no arguments; however, unlike the other form, this form
    * can be used only when the underlying data source is a JDBC data source.
    * The updated <code>Connection</code> properties must be used by the
    * <code>SyncProvider</code> to reset the <code>RowSetWriter</code>
    * configuration to ensure that the contents of the <code>CachedRowSet</code>
    * object are synchronized correctly.
    * <P>
    * When the method <code>acceptChanges</code> executes successfully, in
    * addition to writing changes to the data source, it
    * makes the values in the current row be the values in the original row.
    * <P>
    * Depending on the synchronization level of the <code>SyncProvider</code>
    * implementation being used, the writer will compare the original values
    * with those in the data source to check for conflicts. When there is a conflict,
    * the <code>RIOptimisticProvider</code> implementation, for example, throws a
    * <code>SyncProviderException</code> and does not write anything to the
    * data source.
    * <P>
    * An application may choose to catch the <code>SyncProviderException</code>
    * object and retrieve the <code>SyncResolver</code> object it contains.
    * The <code>SyncResolver</code> object lists the conflicts row by row and
    * sets a lock on the data source to avoid further conflicts while the
    * current conflicts are being resolved.
    * Further, for each conflict, it provides methods for examining the conflict
    * and setting the value that should be persisted in the data source.
    * After all conflicts have been resolved, an application must call the
    * <code>acceptChanges</code> method again to write resolved values to the
    * data source.  If all of the values in the data source are already the
    * values to be persisted, the method <code>acceptChanges</code> does nothing.
    * <P>
    * Some provider implementations may use locks to ensure that there are no
    * conflicts.  In such cases, it is guaranteed that the writer will succeed in
    * writing changes to the data source when the method <code>acceptChanges</code>
    * is called.  This method may be called immediately after the methods
    * <code>updateRow</code>, <code>insertRow</code>, or <code>deleteRow</code>
    * have been called, but it is more efficient to call it only once after
    * all changes have been made so that only one connection needs to be
    * established.
    * <P>
    * Note: The <code>acceptChanges()</code> method will determine if the
    * <code>COMMIT_ON_ACCEPT_CHANGES</code> is set to true or not. If it is set
    * to true, all updates in the synchronization are committed to the data
    * source. Otherwise, the application <b>must</b> explicitly call the
    * <code>commit</code> or <code>rollback</code> methods as appropriate.
    *
    * @param con a standard JDBC <code>Connection</code> object
    * @throws SyncProviderException if the underlying
    * synchronization provider's writer fails to write the updates
    * back to the data source
    * @see #acceptChanges()
    * @see javax.sql.RowSetWriter
    * @see javax.sql.rowset.spi.SyncFactory
    * @see javax.sql.rowset.spi.SyncProvider
    * @see javax.sql.rowset.spi.SyncProviderException
    * @see javax.sql.rowset.spi.SyncResolver
    */
    public void acceptChanges(Connection con) throws SyncProviderException;

   /**
    * Restores this <code>CachedRowSet</code> object to its original
    * value, that is, its value before the last set of changes. If there
    * have been no changes to the rowset or only one set of changes,
    * the original value is the value with which this <code>CachedRowSet</code> object
    * was populated; otherwise, the original value is
    * the value it had immediately before its current value.
    * <P>
    * When this method is called, a <code>CachedRowSet</code> implementation
    * must ensure that all updates, inserts, and deletes to the current
    * rowset instance are replaced by the previous values. In addition,
    * the cursor should be
    * reset to the first row and a <code>rowSetChanged</code> event
    * should be fired to notify all registered listeners.
    *
    * @throws SQLException if an error occurs rolling back the current value of
    *       this <code>CachedRowSet</code> object to its previous value
    * @see javax.sql.RowSetListener#rowSetChanged
    */
    public void restoreOriginal() throws SQLException;

   /**
    * Releases the current contents of this <code>CachedRowSet</code>
    * object and sends a <code>rowSetChanged</code> event to all
    * registered listeners. Any outstanding updates are discarded and
    * the rowset contains no rows after this method is called. There
    * are no interactions with the underlying data source, and any rowset
    * content, metadata, and content updates should be non-recoverable.
    * <P>
    * This <code>CachedRowSet</code> object should lock until its contents and
    * associated updates are fully cleared, thus preventing 'dirty' reads by
    * other components that hold a reference to this <code>RowSet</code> object.
    * In addition, the contents cannot be released
    * until all components reading this <code>CachedRowSet</code> object
    * have completed their reads. This <code>CachedRowSet</code> object
    * should be returned to normal behavior after firing the
    * <code>rowSetChanged</code> event.
    * <P>
    * The metadata, including JDBC properties and Synchronization SPI
    * properties, are maintained for future use. It is important that
    * properties such as the <code>command</code> property be
    * relevant to the originating data source from which this <code>CachedRowSet</code>
    * object was originally established.
    * <P>
    * This method empties a rowset, as opposed to the <code>close</code> method,
    * which marks the entire rowset as recoverable to allow the garbage collector
    * the rowset's Java VM resources.
    *
    * @throws SQLException if an error occurs flushing the contents of this
    * <code>CachedRowSet</code> object
    * @see javax.sql.RowSetListener#rowSetChanged
    * @see java.sql.ResultSet#close
    */
    public void release() throws SQLException;

   /**
    * Cancels the deletion of the current row and notifies listeners that
    * a row has changed. After this method is called, the current row is
    * no longer marked for deletion. This method can be called at any
    * time during the lifetime of the rowset.
    * <P>
    * In addition, multiple cancellations of row deletions can be made
    * by adjusting the position of the cursor using any of the cursor
    * position control methods such as:
    * <ul>
    * <li><code>CachedRowSet.absolute</code>
    * <li><code>CachedRowSet.first</code>
    * <li><code>CachedRowSet.last</code>
    * </ul>
    *
    * @throws SQLException if (1) the current row has not been deleted or
    * (2) the cursor is on the insert row, before the first row, or
    * after the last row
    * @see javax.sql.rowset.CachedRowSet#undoInsert
    * @see java.sql.ResultSet#cancelRowUpdates
    */
    public void undoDelete() throws SQLException;

   /**
    * Immediately removes the current row from this <code>CachedRowSet</code>
    * object if the row has been inserted, and also notifies listeners that a
    * row has changed. This method can be called at any time during the
    * lifetime of a rowset and assuming the current row is within
    * the exception limitations (see below), it cancels the row insertion
    * of the current row.
    * <P>
    * In addition, multiple cancellations of row insertions can be made
    * by adjusting the position of the cursor using any of the cursor
    * position control methods such as:
    * <ul>
    * <li><code>CachedRowSet.absolute</code>
    * <li><code>CachedRowSet.first</code>
    * <li><code>CachedRowSet.last</code>
    * </ul>
    *
    * @throws SQLException if (1) the current row has not been inserted or (2)
    * the cursor is before the first row, after the last row, or on the
    * insert row
    * @see javax.sql.rowset.CachedRowSet#undoDelete
    * @see java.sql.ResultSet#cancelRowUpdates
    */
    public void undoInsert() throws SQLException;


   /**
    * Immediately reverses the last update operation if the
    * row has been modified. This method can be
    * called to reverse updates on all columns until all updates in a row have
    * been rolled back to their state just prior to the last synchronization
    * (<code>acceptChanges</code>) or population. This method may also be called
    * while performing updates to the insert row.
    * <P>
    * <code>undoUpdate</code> may be called at any time during the lifetime of a
    * rowset; however, after a synchronization has occurred, this method has no
    * effect until further modification to the rowset data has occurred.
    *
    * @throws SQLException if the cursor is before the first row or after the last
    *     row in this <code>CachedRowSet</code> object
    * @see #undoDelete
    * @see #undoInsert
    * @see java.sql.ResultSet#cancelRowUpdates
    */
    public void undoUpdate() throws SQLException;

   /**
    * Indicates whether the designated column in the current row of this
    * <code>CachedRowSet</code> object has been updated.
    *
    * @param idx an <code>int</code> identifying the column to be checked for updates
    * @return <code>true</code> if the designated column has been visibly updated;
    * <code>false</code> otherwise
    * @throws SQLException if the cursor is on the insert row, before the first row,
    *     or after the last row
    * @see java.sql.DatabaseMetaData#updatesAreDetected
    */
    public boolean columnUpdated(int idx) throws SQLException;


   /**
    * Indicates whether the designated column in the current row of this
    * <code>CachedRowSet</code> object has been updated.
    *
    * @param columnName a <code>String</code> object giving the name of the
    *        column to be checked for updates
    * @return <code>true</code> if the column has been visibly updated;
    * <code>false</code> otherwise
    * @throws SQLException if the cursor is on the insert row, before the first row,
    *      or after the last row
    * @see java.sql.DatabaseMetaData#updatesAreDetected
    */
    public boolean columnUpdated(String columnName) throws SQLException;

   /**
    * Converts this <code>CachedRowSet</code> object to a <code>Collection</code>
    * object that contains all of this <code>CachedRowSet</code> object's data.
    * Implementations have some latitude in
    * how they can represent this <code>Collection</code> object because of the
    * abstract nature of the <code>Collection</code> framework.
    * Each row must be fully represented in either a
    * general purpose <code>Collection</code> implementation or a specialized
    * <code>Collection</code> implementation, such as a <code>TreeMap</code>
    * object or a <code>Vector</code> object.
    * An SQL <code>NULL</code> column value must be represented as a <code>null</code>
    * in the Java programming language.
    * <P>
    * The standard reference implementation for the <code>CachedRowSet</code>
    * interface uses a <code>TreeMap</code> object for the rowset, with the
    * values in each row being contained in  <code>Vector</code> objects. It is
    * expected that most implementations will do the same.
    * <P>
    * The <code>TreeMap</code> type of collection guarantees that the map will be in
    * ascending key order, sorted according to the natural order for the
    * key's class.
    * Each key references a <code>Vector</code> object that corresponds to one
    * row of a <code>RowSet</code> object. Therefore, the size of each
    * <code>Vector</code> object  must be exactly equal to the number of
    * columns in the <code>RowSet</code> object.
    * The key used by the <code>TreeMap</code> collection is determined by the
    * implementation, which may choose to leverage a set key that is
    * available within the internal <code>RowSet</code> tabular structure by
    * virtue of a key already set either on the <code>RowSet</code> object
    * itself or on the underlying SQL data.
    *
    * @return a <code>Collection</code> object that contains the values in
    * each row in this <code>CachedRowSet</code> object
    * @throws SQLException if an error occurs generating the collection
    * @see #toCollection(int)
    * @see #toCollection(String)
    */
    public Collection<?> toCollection() throws SQLException;

   /**
    * Converts the designated column in this <code>CachedRowSet</code> object
    * to a <code>Collection</code> object. Implementations have some latitude in
    * how they can represent this <code>Collection</code> object because of the
    * abstract nature of the <code>Collection</code> framework.
    * Each column value should be fully represented in either a
    * general purpose <code>Collection</code> implementation or a specialized
    * <code>Collection</code> implementation, such as a <code>Vector</code> object.
    * An SQL <code>NULL</code> column value must be represented as a <code>null</code>
    * in the Java programming language.
    * <P>
    * The standard reference implementation uses a <code>Vector</code> object
    * to contain the column values, and it is expected
    * that most implementations will do the same. If a <code>Vector</code> object
    * is used, it size must be exactly equal to the number of rows
    * in this <code>CachedRowSet</code> object.
    *
    * @param column an <code>int</code> indicating the column whose values
    *        are to be represented in a <code>Collection</code> object
    * @return a <code>Collection</code> object that contains the values
    * stored in the specified column of this <code>CachedRowSet</code>
    * object
    * @throws SQLException if an error occurs generating the collection or
    * an invalid column id is provided
    * @see #toCollection
    * @see #toCollection(String)
    */
    public Collection<?> toCollection(int column) throws SQLException;

   /**
    * Converts the designated column in this <code>CachedRowSet</code> object
    * to a <code>Collection</code> object. Implementations have some latitude in
    * how they can represent this <code>Collection</code> object because of the
    * abstract nature of the <code>Collection</code> framework.
    * Each column value should be fully represented in either a
    * general purpose <code>Collection</code> implementation or a specialized
    * <code>Collection</code> implementation, such as a <code>Vector</code> object.
    * An SQL <code>NULL</code> column value must be represented as a <code>null</code>
    * in the Java programming language.
    * <P>
    * The standard reference implementation uses a <code>Vector</code> object
    * to contain the column values, and it is expected
    * that most implementations will do the same. If a <code>Vector</code> object
    * is used, it size must be exactly equal to the number of rows
    * in this <code>CachedRowSet</code> object.
    *
    * @param column a <code>String</code> object giving the name of the
    *        column whose values are to be represented in a collection
    * @return a <code>Collection</code> object that contains the values
    * stored in the specified column of this <code>CachedRowSet</code>
    * object
    * @throws SQLException if an error occurs generating the collection or
    * an invalid column id is provided
    * @see #toCollection
    * @see #toCollection(int)
    */
    public Collection<?> toCollection(String column) throws SQLException;

   /**
    * Retrieves the <code>SyncProvider</code> implementation for this
    * <code>CachedRowSet</code> object. Internally, this method is used by a rowset
    * to trigger read or write actions between the rowset
    * and the data source. For example, a rowset may need to get a handle
    * on the rowset reader (<code>RowSetReader</code> object) from the
    * <code>SyncProvider</code> to allow the rowset to be populated.
    * <pre>
    *     RowSetReader rowsetReader = null;
    *     SyncProvider provider =
    *         SyncFactory.getInstance("javax.sql.rowset.provider.RIOptimisticProvider");
    *         if (provider instanceof RIOptimisticProvider) {
    *             rowsetReader = provider.getRowSetReader();
    *         }
    * </pre>
    * Assuming <i>rowsetReader</i> is a private, accessible field within
    * the rowset implementation, when an application calls the <code>execute</code>
    * method, it in turn calls on the reader's <code>readData</code> method
    * to populate the <code>RowSet</code> object.
    *<pre>
    *     rowsetReader.readData((RowSetInternal)this);
    * </pre>
    * <P>
    * In addition, an application can use the <code>SyncProvider</code> object
    * returned by this method to call methods that return information about the
    * <code>SyncProvider</code> object, including information about the
    * vendor, version, provider identification, synchronization grade, and locks
    * it currently has set.
    *
    * @return the <code>SyncProvider</code> object that was set when the rowset
    *      was instantiated, or if none was set, the default provider
    * @throws SQLException if an error occurs while returning the
    * <code>SyncProvider</code> object
    * @see #setSyncProvider
    */
    public SyncProvider getSyncProvider() throws SQLException;

   /**
    * Sets the <code>SyncProvider</code> object for this <code>CachedRowSet</code>
    * object to the one specified.  This method
    * allows the <code>SyncProvider</code> object to be reset.
    * <P>
    * A <code>CachedRowSet</code> implementation should always be instantiated
    * with an available <code>SyncProvider</code> mechanism, but there are
    * cases where resetting the <code>SyncProvider</code> object is desirable
    * or necessary. For example, an application might want to use the default
    * <code>SyncProvider</code> object for a time and then choose to use a provider
    * that has more recently become available and better fits its needs.
    * <P>
    * Resetting the <code>SyncProvider</code> object causes the
    * <code>RowSet</code> object to request a new <code>SyncProvider</code> implementation
    * from the <code>SyncFactory</code>. This has the effect of resetting
    * all previous connections and relationships with the originating
    * data source and can potentially drastically change the synchronization
    * behavior of a disconnected rowset.
    *
    * @param provider a <code>String</code> object giving the fully qualified class
    *        name of a <code>SyncProvider</code> implementation
    * @throws SQLException if an error occurs while attempting to reset the
    * <code>SyncProvider</code> implementation
    * @see #getSyncProvider
    */
    public void setSyncProvider(String provider) throws SQLException;

   /**
    * Returns the number of rows in this <code>CachedRowSet</code>
    * object.
    *
    * @return number of rows in the rowset
    */
    public int size();

   /**
    * Sets the metadata for this <code>CachedRowSet</code> object with
    * the given <code>RowSetMetaData</code> object. When a
    * <code>RowSetReader</code> object is reading the contents of a rowset,
    * it creates a <code>RowSetMetaData</code> object and initializes
    * it using the methods in the <code>RowSetMetaData</code> implementation.
    * The reference implementation uses the <code>RowSetMetaDataImpl</code>
    * class. When the reader has completed reading the rowset contents,
    * this method is called internally to pass the <code>RowSetMetaData</code>
    * object to the rowset.
    *
    * @param md a <code>RowSetMetaData</code> object containing
    * metadata about the columns in this <code>CachedRowSet</code> object
    * @throws SQLException if invalid metadata is supplied to the
    * rowset
    */
    public void setMetaData(RowSetMetaData md) throws SQLException;

   /**
    * Returns a <code>ResultSet</code> object containing the original value of this
    * <code>CachedRowSet</code> object.
    * <P>
    * The cursor for the <code>ResultSet</code>
    * object should be positioned before the first row.
    * In addition, the returned <code>ResultSet</code> object should have the following
    * properties:
    * <UL>
    * <LI>ResultSet.TYPE_SCROLL_INSENSITIVE
    * <LI>ResultSet.CONCUR_UPDATABLE
    * </UL>
    * <P>
    * The original value for a <code>RowSet</code> object is the value it had before
    * the last synchronization with the underlying data source.  If there have been
    * no synchronizations, the original value will be the value with which the
    * <code>RowSet</code> object was populated.  This method is called internally
    * when an application calls the method <code>acceptChanges</code> and the
    * <code>SyncProvider</code> object has been implemented to check for conflicts.
    * If this is the case, the writer compares the original value with the value
    * currently in the data source to check for conflicts.
    *
    * @return a <code>ResultSet</code> object that contains the original value for
    *         this <code>CachedRowSet</code> object
    * @throws SQLException if an error occurs producing the
    * <code>ResultSet</code> object
    */
   public ResultSet getOriginal() throws SQLException;

   /**
    * Returns a <code>ResultSet</code> object containing the original value for the
    * current row only of this <code>CachedRowSet</code> object.
    * <P>
    * The cursor for the <code>ResultSet</code>
    * object should be positioned before the first row.
    * In addition, the returned <code>ResultSet</code> object should have the following
    * properties:
    * <UL>
    * <LI>ResultSet.TYPE_SCROLL_INSENSITIVE
    * <LI>ResultSet.CONCUR_UPDATABLE
    * </UL>
    *
    * @return the original result set of the row
    * @throws SQLException if there is no current row
    * @see #setOriginalRow
    */
    public ResultSet getOriginalRow() throws SQLException;

   /**
    * Sets the current row in this <code>CachedRowSet</code> object as the original
    * row.
    * <P>
    * This method is called internally after the any modified values in the current
    * row have been synchronized with the data source. The current row must be tagged
    * as no longer inserted, deleted or updated.
    * <P>
    * A call to <code>setOriginalRow</code> is irreversible.
    *
    * @throws SQLException if there is no current row or an error is
    * encountered resetting the contents of the original row
    * @see #getOriginalRow
    */
    public void setOriginalRow() throws SQLException;

   /**
    * Returns an identifier for the object (table) that was used to
    * create this <code>CachedRowSet</code> object. This name may be set on multiple occasions,
    * and the specification imposes no limits on how many times this
    * may occur or whether standard implementations should keep track
    * of previous table names.
    *
    * @return a <code>String</code> object giving the name of the table that is the
    *         source of data for this <code>CachedRowSet</code> object or <code>null</code>
    *         if no name has been set for the table
    * @throws SQLException if an error is encountered returning the table name
    * @see javax.sql.RowSetMetaData#getTableName
    */
    public String getTableName() throws SQLException;

   /**
    * Sets the identifier for the table from which this <code>CachedRowSet</code>
    * object was derived to the given table name. The writer uses this name to
    * determine which table to use when comparing the values in the data source with the
    * <code>CachedRowSet</code> object's values during a synchronization attempt.
    * The table identifier also indicates where modified values from this
    * <code>CachedRowSet</code> object should be written.
    * <P>
    * The implementation of this <code>CachedRowSet</code> object may obtain the
    * the name internally from the <code>RowSetMetaDataImpl</code> object.
    *
    * @param tabName a <code>String</code> object identifying the table from which this
             <code>CachedRowSet</code> object was derived; cannot be <code>null</code>
    *         but may be an empty string
    * @throws SQLException if an error is encountered naming the table or
    *     <i>tabName</i> is <code>null</code>
    * @see javax.sql.RowSetMetaData#setTableName
    * @see javax.sql.RowSetWriter
    * @see javax.sql.rowset.spi.SyncProvider
    */
   public void setTableName(String tabName) throws SQLException;

   /**
    * Returns an array containing one or more column numbers indicating the columns
    * that form a key that uniquely
    * identifies a row in this <code>CachedRowSet</code> object.
    *
    * @return an array containing the column number or numbers that indicate which columns
    *       constitute a primary key
    *       for a row in this <code>CachedRowSet</code> object. This array should be
    *       empty if no columns are representative of a primary key.
    * @throws SQLException if this <code>CachedRowSet</code> object is empty
    * @see #setKeyColumns
    * @see Joinable#getMatchColumnIndexes
    * @see Joinable#getMatchColumnNames
    */
    public int[] getKeyColumns() throws SQLException;

   /**
    * Sets this <code>CachedRowSet</code> object's <code>keyCols</code>
    * field with the given array of column numbers, which forms a key
    * for uniquely identifying a row in this <code>CachedRowSet</code> object.
    * <p>
    * If a <code>CachedRowSet</code> object becomes part of a <code>JoinRowSet</code>
    * object, the keys defined by this method and the resulting constraints are
    * maintained if the columns designated as key columns also become match
    * columns.
    *
    * @param keys an array of <code>int</code> indicating the columns that form
    *        a primary key for this <code>CachedRowSet</code> object; every
    *        element in the array must be greater than <code>0</code> and
    *        less than or equal to the number of columns in this rowset
    * @throws SQLException if any of the numbers in the given array
    *            are not valid for this rowset
    * @see #getKeyColumns
    * @see Joinable#setMatchColumn(String)
    * @see Joinable#setMatchColumn(int)

    */
    public void setKeyColumns(int[] keys) throws SQLException;


   /**
    * Returns a new <code>RowSet</code> object backed by the same data as
    * that of this <code>CachedRowSet</code> object. In effect, both
    * <code>CachedRowSet</code> objects have a cursor over the same data.
    * As a result, any changes made by a duplicate are visible to the original
    * and to any other duplicates, just as a change made by the original is visible
    * to all of its duplicates. If a duplicate calls a method that changes the
    * underlying data, the method it calls notifies all registered listeners
    * just as it would when it is called by the original <code>CachedRowSet</code>
    * object.
    * <P>
    * In addition, any <code>RowSet</code> object
    * created by this method will have the same properties as this
    * <code>CachedRowSet</code> object. For example, if this <code>CachedRowSet</code>
    * object is read-only, all of its duplicates will also be read-only. If it is
    * changed to be updatable, the duplicates also become updatable.
    * <P>
    * NOTE: If multiple threads access <code>RowSet</code> objects created from
    * the <code>createShared()</code> method, the following behavior is specified
    * to preserve shared data integrity: reads and writes of all
    * shared <code>RowSet</code> objects should be made serially between each
    * object and the single underlying tabular structure.
    *
    * @return a new shared <code>RowSet</code> object that has the same properties
    *         as this <code>CachedRowSet</code> object and that has a cursor over
    *         the same data
    * @throws SQLException if an error occurs or cloning is not
    * supported in the underlying platform
    * @see javax.sql.RowSetEvent
    * @see javax.sql.RowSetListener
    */
    public RowSet createShared() throws SQLException;

   /**
    * Creates a <code>RowSet</code> object that is a deep copy of the data in
    * this <code>CachedRowSet</code> object. In contrast to
    * the <code>RowSet</code> object generated from a <code>createShared</code>
    * call, updates made to the copy of the original <code>RowSet</code> object
    * must not be visible to the original <code>RowSet</code> object. Also, any
    * event listeners that are registered with the original
    * <code>RowSet</code> must not have scope over the new
    * <code>RowSet</code> copies. In addition, any constraint restrictions
    * established must be maintained.
    *
    * @return a new <code>RowSet</code> object that is a deep copy
    * of this <code>CachedRowSet</code> object and is
    * completely independent of this <code>CachedRowSet</code> object
    * @throws SQLException if an error occurs in generating the copy of
    * the of this <code>CachedRowSet</code> object
    * @see #createShared
    * @see #createCopySchema
    * @see #createCopyNoConstraints
    * @see javax.sql.RowSetEvent
    * @see javax.sql.RowSetListener
    */
    public CachedRowSet createCopy() throws SQLException;

    /**
     * Creates a <code>CachedRowSet</code> object that is an empty copy of this
     * <code>CachedRowSet</code> object.  The copy
     * must not contain any contents but only represent the table
     * structure of the original <code>CachedRowSet</code> object. In addition, primary
     * or foreign key constraints set in the originating <code>CachedRowSet</code> object must
     * be equally enforced in the new empty <code>CachedRowSet</code> object.
     * In contrast to
     * the <code>RowSet</code> object generated from a <code>createShared</code> method
     * call, updates made to a copy of this <code>CachedRowSet</code> object with the
     * <code>createCopySchema</code> method must not be visible to it.
     * <P>
     * Applications can form a <code>WebRowSet</code> object from the <code>CachedRowSet</code>
     * object returned by this method in order
     * to export the <code>RowSet</code> schema definition to XML for future use.
     * @return An empty copy of this {@code CachedRowSet} object
     * @throws SQLException if an error occurs in cloning the structure of this
     *         <code>CachedRowSet</code> object
     * @see #createShared
     * @see #createCopySchema
     * @see #createCopyNoConstraints
     * @see javax.sql.RowSetEvent
     * @see javax.sql.RowSetListener
     */
    public CachedRowSet createCopySchema() throws SQLException;

    /**
     * Creates a <code>CachedRowSet</code> object that is a deep copy of
     * this <code>CachedRowSet</code> object's data but is independent of it.
     * In contrast to
     * the <code>RowSet</code> object generated from a <code>createShared</code>
     * method call, updates made to a copy of this <code>CachedRowSet</code> object
     * must not be visible to it. Also, any
     * event listeners that are registered with this
     * <code>CachedRowSet</code> object must not have scope over the new
     * <code>RowSet</code> object. In addition, any constraint restrictions
     * established for this <code>CachedRowSet</code> object must <b>not</b> be maintained
     * in the copy.
     *
     * @return a new <code>CachedRowSet</code> object that is a deep copy
     *     of this <code>CachedRowSet</code> object and is
     *     completely independent of this  <code>CachedRowSet</code> object
     * @throws SQLException if an error occurs in generating the copy of
     *     the of this <code>CachedRowSet</code> object
     * @see #createCopy
     * @see #createShared
     * @see #createCopySchema
     * @see javax.sql.RowSetEvent
     * @see javax.sql.RowSetListener
     */
    public CachedRowSet createCopyNoConstraints() throws SQLException;

    /**
     * Retrieves the first warning reported by calls on this <code>RowSet</code> object.
     * Subsequent warnings on this <code>RowSet</code> object will be chained to the
     * <code>RowSetWarning</code> object that this method returns.
     *
     * The warning chain is automatically cleared each time a new row is read.
     * This method may not be called on a RowSet object that has been closed;
     * doing so will cause a <code>SQLException</code> to be thrown.
     *
     * @return RowSetWarning the first <code>RowSetWarning</code>
     * object reported or null if there are none
     * @throws SQLException if this method is called on a closed RowSet
     * @see RowSetWarning
     */
    public RowSetWarning getRowSetWarnings() throws SQLException;

    /**
     * Retrieves a <code>boolean</code> indicating whether rows marked
     * for deletion appear in the set of current rows. If <code>true</code> is
     * returned, deleted rows are visible with the current rows. If
     * <code>false</code> is returned, rows are not visible with the set of
     * current rows. The default value is <code>false</code>.
     * <P>
     * Standard rowset implementations may choose to restrict this behavior
     * due to security considerations or to better fit certain deployment
     * scenarios. This is left as implementation defined and does not
     * represent standard behavior.
     * <P>
     * Note: Allowing deleted rows to remain visible complicates the behavior
     * of some standard JDBC <code>RowSet</code> Implementations methods.
     * However, most rowset users can simply ignore this extra detail because
     * only very specialized applications will likely want to take advantage of
     * this feature.
     *
     * @return <code>true</code> if deleted rows are visible;
     *         <code>false</code> otherwise
     * @throws SQLException if a rowset implementation is unable to
     * to determine whether rows marked for deletion are visible
     * @see #setShowDeleted
     */
    public boolean getShowDeleted() throws SQLException;

    /**
     * Sets the property <code>showDeleted</code> to the given
     * <code>boolean</code> value, which determines whether
     * rows marked for deletion appear in the set of current rows.
     * If the value is set to <code>true</code>, deleted rows are immediately
     * visible with the set of current rows. If the value is set to
     * <code>false</code>, the deleted rows are set as invisible with the
     * current set of rows.
     * <P>
     * Standard rowset implementations may choose to restrict this behavior
     * due to security considerations or to better fit certain deployment
     * scenarios. This is left as implementations defined and does not
     * represent standard behavior.
     *
     * @param b <code>true</code> if deleted rows should be shown;
     *              <code>false</code> otherwise
     * @exception SQLException if a rowset implementation is unable to
     * to reset whether deleted rows should be visible
     * @see #getShowDeleted
     */
    public void setShowDeleted(boolean b) throws SQLException;

    /**
     * Each <code>CachedRowSet</code> object's <code>SyncProvider</code> contains
     * a <code>Connection</code> object from the <code>ResultSet</code> or JDBC
     * properties passed to it's constructors. This method wraps the
     * <code>Connection</code> commit method to allow flexible
     * auto commit or non auto commit transactional control support.
     * <p>
     * Makes all changes that are performed by the <code>acceptChanges()</code>
     * method since the previous commit/rollback permanent. This method should
     * be used only when auto-commit mode has been disabled.
     *
     * @throws SQLException if a database access error occurs or this
     * Connection object within this <code>CachedRowSet</code> is in auto-commit mode
     * @see java.sql.Connection#setAutoCommit
     */
    public void commit() throws SQLException;

    /**
     * Each <code>CachedRowSet</code> object's <code>SyncProvider</code> contains
     * a <code>Connection</code> object from the original <code>ResultSet</code>
     * or JDBC properties passed to it.
     * <p>
     * Undoes all changes made in the current transaction.  This method
     * should be used only when auto-commit mode has been disabled.
     *
     * @throws SQLException if a database access error occurs or this Connection
     * object within this <code>CachedRowSet</code> is in auto-commit mode.
     */
    public void rollback() throws SQLException;

    /**
     * Each <code>CachedRowSet</code> object's <code>SyncProvider</code> contains
     * a <code>Connection</code> object from the original <code>ResultSet</code>
     * or JDBC properties passed to it.
     * <p>
     * Undoes all changes made in the current transaction back to the last
     * <code>Savepoint</code> transaction marker. This method should be used only
     * when auto-commit mode has been disabled.
     *
     * @param s A <code>Savepoint</code> transaction marker
     * @throws SQLException if a database access error occurs or this Connection
     * object within this <code>CachedRowSet</code> is in auto-commit mode.
     */
    public void rollback(Savepoint s) throws SQLException;

    /**
     * Causes the <code>CachedRowSet</code> object's <code>SyncProvider</code>
     * to commit the changes when <code>acceptChanges()</code> is called. If
     * set to false, the changes will <b>not</b> be committed until one of the
     * <code>CachedRowSet</code> interface transaction methods is called.
     *
     * @deprecated Because this field is final (it is part of an interface),
     *  its value cannot be changed.
     * @see #commit
     * @see #rollback
     */
    @Deprecated
    public static final boolean COMMIT_ON_ACCEPT_CHANGES = true;

    /**
     * Notifies registered listeners that a RowSet object in the given RowSetEvent
     * object has populated a number of additional rows. The <code>numRows</code> parameter
     * ensures that this event will only be fired every <code>numRow</code>.
     * <p>
     * The source of the event can be retrieved with the method event.getSource.
     *
     * @param event a <code>RowSetEvent</code> object that contains the
     *     <code>RowSet</code> object that is the source of the events
     * @param numRows when populating, the number of rows interval on which the
     *     <code>CachedRowSet</code> populated should fire; the default value
     *     is zero; cannot be less than <code>fetchSize</code> or zero
     * @throws SQLException {@code numRows < 0 or numRows < getFetchSize() }
     */
    public void rowSetPopulated(RowSetEvent event, int numRows) throws SQLException;

    /**
     * Populates this <code>CachedRowSet</code> object with data from
     * the given <code>ResultSet</code> object. While related to the <code>populate(ResultSet)</code>
     * method, an additional parameter is provided to allow starting position within
     * the <code>ResultSet</code> from where to populate the CachedRowSet
     * instance.
     * <P>
     * This method can be used as an alternative to the <code>execute</code> method when an
     * application has a connection to an open <code>ResultSet</code> object.
     * Using the method <code>populate</code> can be more efficient than using
     * the version of the <code>execute</code> method that takes no parameters
     * because it does not open a new connection and re-execute this
     * <code>CachedRowSet</code> object's command. Using the <code>populate</code>
     *  method is more a matter of convenience when compared to using the version
     * of <code>execute</code> that takes a <code>ResultSet</code> object.
     *
     * @param startRow the position in the <code>ResultSet</code> from where to start
     *                populating the records in this <code>CachedRowSet</code>
     * @param rs the <code>ResultSet</code> object containing the data
     * to be read into this <code>CachedRowSet</code> object
     * @throws SQLException if a null <code>ResultSet</code> object is supplied
     * or this <code>CachedRowSet</code> object cannot
     * retrieve the associated <code>ResultSetMetaData</code> object
     * @see #execute
     * @see #populate(ResultSet)
     * @see java.sql.ResultSet
     * @see java.sql.ResultSetMetaData
    */
    public void populate(ResultSet rs, int startRow) throws SQLException;

    /**
     * Sets the <code>CachedRowSet</code> object's page-size. A <code>CachedRowSet</code>
     * may be configured to populate itself in page-size sized batches of rows. When
     * either <code>populate()</code> or <code>execute()</code> are called, the
     * <code>CachedRowSet</code> fetches an additional page according to the
     * original SQL query used to populate the RowSet.
     *
     * @param size the page-size of the <code>CachedRowSet</code>
     * @throws SQLException if an error occurs setting the <code>CachedRowSet</code>
     *      page size or if the page size is less than 0.
     */
    public void setPageSize(int size) throws SQLException;

    /**
     * Returns the page-size for the <code>CachedRowSet</code> object
     *
     * @return an <code>int</code> page size
     */
    public int getPageSize();

    /**
     * Increments the current page of the <code>CachedRowSet</code>. This causes
     * the <code>CachedRowSet</code> implementation to fetch the next page-size
     * rows and populate the RowSet, if remaining rows remain within scope of the
     * original SQL query used to populated the RowSet.
     *
     * @return true if more pages exist; false if this is the last page
     * @throws SQLException if an error occurs fetching the next page, or if this
     *     method is called prematurely before populate or execute.
     */
    public boolean nextPage() throws SQLException;

    /**
     * Decrements the current page of the <code>CachedRowSet</code>. This causes
     * the <code>CachedRowSet</code> implementation to fetch the previous page-size
     * rows and populate the RowSet. The amount of rows returned in the previous
     * page must always remain within scope of the original SQL query used to
     * populate the RowSet.
     *
     * @return true if the previous page is successfully retrieved; false if this
     *     is the first page.
     * @throws SQLException if an error occurs fetching the previous page, or if
     *     this method is called prematurely before populate or execute.
     */
    public boolean previousPage() throws SQLException;

}
