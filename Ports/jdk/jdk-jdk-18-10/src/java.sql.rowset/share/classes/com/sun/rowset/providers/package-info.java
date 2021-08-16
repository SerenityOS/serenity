/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * Repository for the {@code RowSet} reference implementations of the
 * {@code SyncProvider} abstract class. These implementations provide a
 * disconnected {@code RowSet}
 * object with the ability to synchronize the data in the underlying data
 * source with its data.  These implementations are provided as
 * the default {@code SyncProvider} implementations and are accessible via the
 * {@code SyncProvider} SPI managed by the {@code SyncFactory}.
 *
 * <h2>1.0 {@code SyncProvider} Reference Implementations</h2>
 *   The main job of a {@code SyncProvider} implementation is to manage
 * the reader and writer mechanisms.
 *  The {@code SyncProvider} SPI, as specified in the {@code javax.sql.rowset.spi}
 * package, provides a pluggable mechanism by which {@code javax.sql.RowSetReader}
 * and {@code javax.sql.RowSetWriter} implementations can be supplied to a disconnected
 * {@code RowSet} object.
 * <P>
 *  A reader, a {@code javax.sql.RowSetReader}
 * object, does the work necessary to populate a {@code RowSet} object with data.
 * A writer, a {@code javax.sql.RowSetWriter} object, does the work necessary for
 * synchronizing a {@code RowSet} object's data with the data in the originating
 * source of data. Put another way, a writer writes a {@code RowSet}
 * object's data back to the data source.
 * <P>
 * Generally speaking, the course of events is this.  The reader makes a connection to
 * the data source and reads the data from a {@code ResultSet} object into its
 * {@code RowSet} object.  Then it closes the connection.  While
 * the {@code RowSet} object is disconnected, an application makes some modifications
 * to the data and calls the method {@code acceptChanges}. At this point, the
 * writer is called to write the changes back to the database table or view
 * from which the original data came. This is called <i>synchronization</i>.
 * <P>
 * If the data in the originating data source has not changed, there is no problem
 * with just writing the {@code RowSet} object's new data to the data source.
 * If it has changed, however, there is a conflict that needs to be resolved. One
 * way to solve the problem is not to let the data in the data source be changed in
 * the first place, which can be done by setting locks on a row, a table, or the
 * whole data source.  Setting locks is a way to avoid conflicts, but it can be
 * very expensive. Another approach, which is at the other end of the spectrum,
 *  is simply to assume that no conflicts will occur and thus do nothing to avoid
 * conflicts.
 * Different {@code SyncProvider} implementations may handle synchronization in
 * any of these ways, varying from doing no checking for
 * conflicts, to doing various levels of checking, to guaranteeing that there are no
 * conflicts.
 * <P>
 * The {@code SyncProvider} class offers methods to help a {@code RowSet}
 * object discover and manage how a provider handles synchronization.
 * The method {@code getProviderGrade} returns the
 * grade of synchronization a provider offers. An application can
 * direct the provider to use a particular level of locking by calling
 * the method {@code setDataSourceLock} and specifying the level of locking desired.
 * If a {@code RowSet} object's data came from an SQL {@code VIEW}, an
 * application may call the method {@code supportsUpdatableView} to
 * find out whether the {@code VIEW} can be updated.
 * <P>
 * Synchronization is done completely behind the scenes, so it is third party vendors of
 * synchronization provider implementations who have to take care of this complex task.
 * Application programmers can decide which provider to use and the level of locking to
 * be done, but they are free from having to worry about the implementation details.
 * <P>
 * The JDBC {@code RowSet} Implementations reference implementation provides two
 * implementations of the {@code SyncProvider} class:
 *
 * <UL>
 * <LI>
 * <b>{@code RIOptimisticProvider}</b> - provides the {@code javax.sql.RowSetReader}
 * and {@code javax.sql.RowSetWriter} interface implementations and provides
 * an optimistic concurrency model for synchronization. This model assumes that there
 * will be few conflicts and therefore uses a relatively low grade of synchronization.
 * If no other provider is available, this is the default provider that the
 * {@code SyncFactory} will supply to a {@code RowSet} object.
 *     <br>
 * <LI>
 * <b>{@code RIXMLProvider}</b> - provides the {@code XmlReader} (an extension
 * of the {@code javax.sql.RowSetReader} interface) and the {@code XmlWriter}
 * (an extension of the {@code javax.sql.RowSetWriter} interface) to enable
 * {@code WebRowSet} objects to write their state to a
 * well formed XML document according to the {@code WebRowSet} XML schema
 * definition.<br>
 * </UL>
 *
 * <h2>2.0 Basics in RowSet Population &amp; Synchronization</h2>
 * A rowset's first task is to populate itself with rows of column values.
 * Generally,   these rows will come from a relational database, so a rowset
 * has properties   that supply what is necessary for making a connection to
 * a database and executing  a query. A rowset that does not need to establish
 * a connection and execute  a command, such as one that gets its data from
 * a tabular file instead of a relational database, does not need to have these
 * properties set. The vast  majority of RowSets, however, do need to set these
 * properties. The general  rule is that a RowSet is required to set only the
 * properties that it uses.<br>
 *     <br>
 * The {@code command} property contains the query that determines what
 * data  a {@code RowSet} will contain. Rowsets have methods for setting a query's
 * parameter(s),  which means that a query can be executed multiple times with
 * different parameters  to produce different result sets. Or the query can be
 * changed to something  completely new to get a new result set.
 * <p>Once a rowset contains the rows from a {@code ResultSet} object or some
 * other data source, its column values can be updated, and its rows can be
 * inserted or deleted. Any method that causes a change in the rowset's values
 * or cursor position also notifies any object that has been registered as
 * a listener with the rowset. So, for example, a table that displays the rowset's
 * data in an applet can be notified of changes and make updates as they
 * occur.<br>
 *     <br>
 * The changes made to a rowset can be propagated back to the original data
 * source to keep the rowset and its data source synchronized. Although this
 * involves many operations behind the scenes, it is completely transparent
 * to the application programmer and remains the concern of the RowSet provider
 * developer. All an application has to do is invoke the method {@code acceptChanges},
 * and the data source backing the rowset will be updated to match the current
 * values in the rowset. </p>
 *
 * <p>A disconnected rowset, such as a {@code CachedRowSet} or {@code WebRowSet}
 *  object, establishes a connection to populate itself with data from a database
 *  and then closes the connection. The {@code RowSet} object will remain
 *  disconnected until it wants to propagate changes back to its database table,
 *  which is optional. To write its changes back to the database (synchronize with
 *  the database), the rowset establishes a connection, write the changes, and then
 *  once again disconnects itself.<br>
 *   </p>
 *
 * <h2> 3.0 Other Possible Implementations</h2>
 *  There are many other possible implementations of the {@code SyncProvider} abstract
 *  class. One possibility is to employ a more robust synchronization model, which
 *  would give a {@code RowSet} object increased trust in the provider's
 *  ability to get any updates back to the original data source. Another possibility
 *  is a more formal synchronization mechanism such as SyncML
 *  (<a href="http://www.syncml.org/">http://www.syncml.org/</a>)   <br>
 */
package com.sun.rowset.providers;
