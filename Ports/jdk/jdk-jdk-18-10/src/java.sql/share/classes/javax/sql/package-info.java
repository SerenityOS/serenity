/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides the API for server side data source access and processing from
 * the Java programming language.
 * This package supplements the {@code java.sql}
 * package and, as of the version 1.4 release, is included in the
 * Java Platform, Standard Edition (Java SE).
 * It remains an essential part of the Java Platform, Enterprise Edition
 * (Java EE).
 * <p>
 * The {@code javax.sql} package provides for the following:
 * <OL>
 * <LI>The {@code DataSource} interface as an alternative to the
 * {@code DriverManager} for establishing a
 * connection with a data source
 * <LI>Connection pooling and Statement pooling
 * <LI>Distributed transactions
 * <LI>Rowsets
 * </OL>
 * <p>
 * Applications use the {@code DataSource} and {@code RowSet}
 * APIs directly, but the connection pooling and distributed transaction
 * APIs are used internally by the middle-tier infrastructure.
 *
 * <H2>Using a {@code DataSource} Object to Make a Connection</H2>
 * <p>
 * The {@code javax.sql} package provides the preferred
 * way to make a connection with a data source.  The {@code DriverManager}
 * class, the original mechanism, is still valid, and code using it will
 * continue to run.  However, the newer {@code DataSource} mechanism
 * is preferred because it offers many advantages over the
 * {@code DriverManager} mechanism.
 * <p>
 * These are the main advantages of using a {@code DataSource} object to
 * make a connection:
 * <UL>
 *
 * <LI>Changes can be made to a data source's properties, which means
 * that it is not necessary to make changes in application code when
 * something about the data source or driver changes.
 * <LI>Connection  and Statement pooling and distributed transactions are available
 * through a {@code DataSource} object that is
 * implemented to work with the middle-tier infrastructure.
 * Connections made through the {@code DriverManager}
 * do not have connection and statement pooling or distributed transaction
 * capabilities.
 * </UL>
 * <p>
 * Driver vendors provide {@code DataSource} implementations. A
 * particular {@code DataSource} object represents a particular
 * physical data source, and each connection the {@code DataSource} object
 * creates is a connection to that physical data source.
 * <p>
 * A logical name for the data source is registered with a naming service that
 * uses the Java Naming and Directory Interface
 * (JNDI) API, usually by a system administrator or someone performing the
 * duties of a system administrator. An application can retrieve the
 * {@code DataSource} object it wants by doing a lookup on the logical
 * name that has been registered for it.  The application can then use the
 * {@code DataSource} object to create a connection to the physical data
 * source it represents.
 * <p>
 * A {@code DataSource} object can be implemented to work with the
 * middle tier infrastructure so that the connections it produces will be
 * pooled for reuse. An application that uses such a {@code DataSource}
 * implementation will automatically get a connection that participates in
 * connection pooling.
 * A {@code DataSource} object can also be implemented to work with the
 * middle tier infrastructure so that the connections it produces can be
 * used for distributed transactions without any special coding.
 *
 * <H2>Connection Pooling and Statement Pooling</H2>
 * <p>
 * Connections made via a {@code DataSource}
 * object that is implemented to work with a middle tier connection pool manager
 * will participate in connection pooling.  This can improve performance
 * dramatically because creating new connections is very expensive.
 * Connection pooling allows a connection to be used and reused,
 * thus cutting down substantially on the number of new connections
 * that need to be created.
 * <p>
 * Connection pooling is totally transparent.  It is done automatically
 * in the middle tier of a Java EE configuration, so from an application's
 * viewpoint, no change in code is required. An application simply uses
 * the {@code DataSource.getConnection} method to get the pooled
 * connection and uses it the same way it uses any {@code Connection}
 * object.
 * <p>
 * The classes and interfaces used for connection pooling are:
 * <UL>
 * <LI>{@code ConnectionPoolDataSource}
 * <LI>{@code PooledConnection}
 * <LI>{@code ConnectionEvent}
 * <LI>{@code ConnectionEventListener}
 * <LI>{@code StatementEvent}
 * <LI>{@code StatementEventListener}
 * </UL>
 * The connection pool manager, a facility in the middle tier of
 * a three-tier architecture, uses these classes and interfaces
 * behind the scenes.  When a {@code ConnectionPoolDataSource} object
 * is called on to create a {@code PooledConnection} object, the
 * connection pool manager will register as a {@code ConnectionEventListener}
 * object with the new {@code PooledConnection} object.  When the connection
 * is closed or there is an error, the connection pool manager (being a listener)
 * gets a notification that includes a {@code ConnectionEvent} object.
 * <p>
 * If the connection pool manager supports {@code Statement} pooling, for
 * {@code PreparedStatements}, which can be determined by invoking the method
 * {@code DatabaseMetaData.supportsStatementPooling},  the
 * connection pool manager will register as a {@code StatementEventListener}
 * object with the new {@code PooledConnection} object.  When the
 * {@code PreparedStatement} is closed or there is an error, the connection
 * pool manager (being a listener)
 * gets a notification that includes a {@code StatementEvent} object.
 *
 * <H2>Distributed Transactions</H2>
 * <p>
 * As with pooled connections, connections made via a {@code DataSource}
 * object that is implemented to work with the middle tier infrastructure
 * may participate in distributed transactions.  This gives an application
 * the ability to involve data sources on multiple servers in a single
 * transaction.
 * <p>
 * The classes and interfaces used for distributed transactions are:
 * <UL>
 * <LI>{@code XADataSource}
 * <LI>{@code XAConnection}
 * </UL>
 * These interfaces are used by the transaction manager; an application does
 * not use them directly.
 * <p>
 * The {@code XAConnection} interface is derived from the
 * {@code PooledConnection} interface, so what applies to a pooled connection
 * also applies to a connection that is part of a distributed transaction.
 * A transaction manager in the middle tier handles everything transparently.
 * The only change in application code is that an application cannot do anything
 * that would interfere with the transaction manager's handling of the transaction.
 * Specifically, an application cannot call the methods {@code Connection.commit}
 * or {@code Connection.rollback}, and it cannot set the connection to be in
 * auto-commit mode (that is, it cannot call
 * {@code Connection.setAutoCommit(true)}).
 * <p>
 * An application does not need to do anything special to participate in a
 * distributed transaction.
 * It simply creates connections to the data sources it wants to use via
 * the {@code DataSource.getConnection} method, just as it normally does.
 * The transaction manager manages the transaction behind the scenes.  The
 * {@code XADataSource} interface creates {@code XAConnection} objects, and
 * each {@code XAConnection} object creates an {@code XAResource} object
 * that the transaction manager uses to manage the connection.
 *
 *
 * <H2>Rowsets</H2>
 * The {@code RowSet} interface works with various other classes and
 * interfaces behind the scenes. These can be grouped into three categories.
 * <OL>
 * <LI>Event Notification
 * <UL>
 * <LI>{@code RowSetListener}<br>
 * A {@code RowSet} object is a JavaBeans
 * component because it has properties and participates in the JavaBeans
 * event notification mechanism. The {@code RowSetListener} interface
 * is implemented by a component that wants to be notified about events that
 * occur to a particular {@code RowSet} object.  Such a component registers
 * itself as a listener with a rowset via the {@code RowSet.addRowSetListener}
 * method.
 * <p>
 * When the {@code RowSet} object changes one of its rows, changes all of
 * it rows, or moves its cursor, it also notifies each listener that is registered
 * with it.  The listener reacts by carrying out its implementation of the
 * notification method called on it.
 * <LI>{@code RowSetEvent}<br>
 * As part of its internal notification process, a {@code RowSet} object
 * creates an instance of {@code RowSetEvent} and passes it to the listener.
 * The listener can use this {@code RowSetEvent} object to find out which rowset
 * had the event.
 * </UL>
 * <LI>Metadata
 * <UL>
 * <LI>{@code RowSetMetaData}<br>
 * This interface, derived from the
 * {@code ResultSetMetaData} interface, provides information about
 * the columns in a {@code RowSet} object.  An application can use
 * {@code RowSetMetaData} methods to find out how many columns the
 * rowset contains and what kind of data each column can contain.
 * <p>
 * The {@code RowSetMetaData} interface provides methods for
 * setting the information about columns, but an application would not
 * normally use these methods.  When an application calls the {@code RowSet}
 * method {@code execute}, the {@code RowSet} object will contain
 * a new set of rows, and its {@code RowSetMetaData} object will have been
 * internally updated to contain information about the new columns.
 * </UL>
 * <LI>The Reader/Writer Facility<br>
 * A {@code RowSet} object that implements the {@code RowSetInternal}
 * interface can call on the {@code RowSetReader} object associated with it
 * to populate itself with data.  It can also call on the {@code RowSetWriter}
 * object associated with it to write any changes to its rows back to the
 * data source from which it originally got the rows.
 * A rowset that remains connected to its data source does not need to use a
 * reader and writer because it can simply operate on the data source directly.
 *
 * <UL>
 * <LI>{@code RowSetInternal}<br>
 * By implementing the {@code RowSetInternal} interface, a
 * {@code RowSet} object gets access to
 * its internal state and is able to call on its reader and writer. A rowset
 * keeps track of the values in its current rows and of the values that immediately
 * preceded the current ones, referred to as the <i>original</i> values.  A rowset
 * also keeps track of (1) the parameters that have been set for its command and
 * (2) the connection that was passed to it, if any.  A rowset uses the
 * {@code RowSetInternal} methods behind the scenes to get access to
 * this information.  An application does not normally invoke these methods directly.
 *
 * <LI>{@code RowSetReader}<br>
 * A disconnected {@code RowSet} object that has implemented the
 * {@code RowSetInternal} interface can call on its reader (the
 * {@code RowSetReader} object associated with it) to populate it with
 * data.  When an application calls the {@code RowSet.execute} method,
 * that method calls on the rowset's reader to do much of the work. Implementations
 * can vary widely, but generally a reader makes a connection to the data source,
 * reads data from the data source and populates the rowset with it, and closes
 * the connection. A reader may also update the {@code RowSetMetaData} object
 * for its rowset.  The rowset's internal state is also updated, either by the
 * reader or directly by the method {@code RowSet.execute}.
 *
 *
 * <LI>{@code RowSetWriter}<br>
 * A disconnected {@code RowSet} object that has implemented the
 * {@code RowSetInternal} interface can call on its writer (the
 * {@code RowSetWriter} object associated with it) to write changes
 * back to the underlying data source.  Implementations may vary widely, but
 * generally, a writer will do the following:
 *
 * <UL>
 * <LI>Make a connection to the data source
 * <LI>Check to see whether there is a conflict, that is, whether
 * a value that has been changed in the rowset has also been changed
 * in the data source
 * <LI>Write the new values to the data source if there is no conflict
 * <LI>Close the connection
 * </UL>
 *
 *
 * </UL>
 * </OL>
 * <p>
 * The {@code RowSet} interface may be implemented in any number of
 * ways, and anyone may write an implementation. Developers are encouraged
 * to use their imaginations in coming up with new ways to use rowsets.
 *
 *
 * <h2>Package Specification</h2>
 *
 * <ul>
 * <li><a href="https://jcp.org/en/jsr/detail?id=221">JDBC 4.3 Specification</a>
 * </ul>
 *
 * <h2>Related Documentation</h2>
 * <p>
 * The Java Series book published by Addison-Wesley Longman provides detailed
 * information about the classes and interfaces in the {@code javax.sql}
 * package:
 *
 * <ul>
 * <li>&ldquo;<i>JDBC&#8482;API Tutorial and Reference, Third Edition</i>&rdquo;
 * </ul>
 */
package javax.sql;
