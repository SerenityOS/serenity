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
 * The standard classes and interfaces that a third party vendor has to
 * use in its implementation of a synchronization provider. These classes and
 * interfaces are referred to as the Service Provider Interface (SPI).  To make it possible
 * for a {@code RowSet} object to use an implementation, the vendor must register
 * it with the {@code SyncFactory} singleton. (See the class comment for
 * {@code SyncProvider} for a full explanation of the registration process and
 * the naming convention to be used.)
 *
 * <h2>Table of Contents</h2>
 * <ul>
 * <li><a href="#pkgspec">1.0 Package Specification</a>
 * <li><a href="#arch">2.0 Service Provider Architecture</a>
 * <li><a href="#impl">3.0 Implementer's Guide</a>
 * <li><a href="#resolving">4.0 Resolving Synchronization Conflicts</a>
 * <li><a href="#relspec">5.0 Related Specifications</a>
 * <li><a href="#reldocs">6.0 Related Documentation</a>
 * </ul>
 *
 * <h3><a id="pkgspec">1.0 Package Specification</a></h3>
 * <P>
 * The following classes and interfaces make up the {@code javax.sql.rowset.spi}
 * package:
 * <UL>
 *  <LI>{@code SyncFactory}
 *  <LI>{@code SyncProvider}
 *  <LI>{@code SyncFactoryException}
 *  <LI>{@code SyncProviderException}
 *  <LI>{@code SyncResolver}
 *  <LI>{@code XmlReader}
 *  <LI>{@code XmlWriter}
 *  <LI>{@code TransactionalWriter}
 * </UL>
 * The following interfaces, in the {@code javax.sql} package, are also part of the SPI:
 * <UL>
 *  <LI>{@code RowSetReader}
 *  <LI>{@code RowSetWriter}
 * </UL>
 * <P>
 * A {@code SyncProvider} implementation provides a disconnected {@code RowSet}
 * object with the mechanisms for reading data into it and for writing data that has been
 * modified in it
 * back to the underlying data source.  A <i>reader</i>, a {@code RowSetReader} or
 * {@code XMLReader} object, reads data into a {@code RowSet} object when the
 * {@code CachedRowSet} methods {@code execute} or {@code populate}
 * are called.  A <i>writer</i>, a {@code RowSetWriter} or {@code XMLWriter}
 * object, writes changes back to the underlying data source when the
 * {@code CachedRowSet} method {@code acceptChanges} is called.
 * <P>
 * The process of writing changes in a {@code RowSet} object to its data source
 * is known as <i>synchronization</i>.  The {@code SyncProvider} implementation that a
 * {@code RowSet} object is using determines the level of synchronization that the
 * {@code RowSet} object's writer uses. The various levels of synchronization are
 * referred to as <i>grades</i>.
 * <P>
 * The lower grades of synchronization are
 * known as <i>optimistic</i> concurrency levels because they optimistically
 * assume that there will be no conflicts or very few conflicts.  A conflict exists when
 * the same data modified in the {@code RowSet} object has also been modified
 * in the data source. Using the optimistic concurrency model means that if there
 * is a conflict, modifications to either the data source or the {@code RowSet}
 * object will be lost.
 * <P>
 * Higher grades of synchronization are called <i>pessimistic</i> because they assume
 * that others will be accessing the data source and making modifications.  These
 * grades set varying levels of locks to increase the chances that no conflicts
 * occur.
 * <P>
 * The lowest level of synchronization is simply writing any changes made to the
 * {@code RowSet} object to its underlying data source.  The writer does
 * nothing to check for conflicts.
 * If there is a conflict and the data
 * source values are overwritten, the changes other parties have made by to the data
 * source are lost.
 * <P>
 * The {@code RIXMLProvider} implementation uses the lowest level
 * of synchronization and just writes {@code RowSet} changes to the data source.
 *
 * <P>
 * For the next level up, the
 * writer checks to see if there are any conflicts, and if there are,
 * it does not write anything to the data source.  The problem with this concurrency
 * level is that if another party has modified the corresponding data in the data source
 * since the {@code RowSet} object got its data,
 * the changes made to the {@code RowSet} object are lost. The
 * {@code RIOptimisticProvider} implementation uses this level of synchronization.
 * <P>
 * At higher levels of synchronization, referred to as pessimistic concurrency,
 * the writer take steps to avoid conflicts by setting locks. Setting locks
 * can vary from setting a lock on a single row to setting a lock on a table
 * or the entire data source. The level of synchronization is therefore a tradeoff
 * between the ability of users to access the data source concurrently and the  ability
 * of the writer to keep the data in the {@code RowSet} object and its data source
 * synchronized.
 * <P>
 * It is a requirement that all disconnected {@code RowSet} objects
 * ({@code CachedRowSet}, {@code FilteredRowSet}, {@code JoinRowSet},
 * and {@code WebRowSet} objects) obtain their {@code SyncProvider} objects
 * from the {@code SyncFactory} mechanism.
 * <P>
 * The reference implementation (RI) provides two synchronization providers.
 *    <UL>
 *       <LI><b>{@code RIOptimisticProvider}</b> <br>
 *            The default provider that the {@code SyncFactory} instance will
 *            supply to a disconnected {@code RowSet} object when no provider
 *            implementation is specified.<BR>
 *            This synchronization provider uses an optimistic concurrency model,
 *            assuming that there will be few conflicts among users
 *            who are accessing the same data in a database.  It avoids
 *            using locks; rather, it checks to see if there is a conflict
 *            before trying to synchronize the {@code RowSet} object and the
 *            data source. If there is a conflict, it does nothing, meaning that
 *            changes to the {@code RowSet} object are not persisted to the data
 *            source.
 *        <LI><B>{@code RIXMLProvider}</B> <BR>
 *             A synchronization provider that can be used with a
 *             {@code WebRowSet} object, which is a rowset that can be written
 *             in XML format or read from XML format. The
 *             {@code RIXMLProvider} implementation does no checking at all for
 *             conflicts and simply writes any updated data in the
 *             {@code WebRowSet} object to the underlying data source.
 *             {@code WebRowSet} objects use this provider when they are
 *             dealing with XML data.
 *     </UL>
 *
 *  These {@code SyncProvider} implementations
 *  are bundled with the reference implementation, which makes them always available to
 *  {@code RowSet} implementations.
 *  {@code SyncProvider} implementations make themselves available by being
 *  registered with the {@code SyncFactory} singleton.  When a {@code RowSet}
 *  object requests a provider, by specifying it in the constructor or as an argument to the
 *  {@code CachedRowSet} method {@code setSyncProvider},
 *  the {@code SyncFactory} singleton
 *  checks to see if the requested provider has been registered with it.
 *  If it has, the {@code SyncFactory} creates an instance of it and passes it to the
 *  requesting {@code RowSet} object.
 *  If the {@code SyncProvider} implementation that is specified has not been registered,
 *  the {@code SyncFactory} singleton causes a {@code SyncFactoryException} object
 *  to be thrown.  If no provider is specified,
 *  the {@code SyncFactory} singleton will create an instance of the default
 *  provider implementation, {@code RIOptimisticProvider},
 *  and pass it to the requesting {@code RowSet} object.
 *
 * <P>
 * If a {@code WebRowSet} object does not specify a provider in its constructor, the
 * {@code SyncFactory} will give it an instance of {@code RIOptimisticProvider}.
 * However, the constructor for {@code WebRowSet} is implemented to set the provider
 * to the {@code RIXMLProvider}, which reads and writes a {@code RowSet} object
 *  in XML format.
 *  <P>
 * See the <a href="SyncProvider.html">SyncProvider</a> class
 *  specification for further details.
 * <p>
 * Vendors may develop a {@code SyncProvider} implementation with any one of the possible
 * levels of synchronization, thus giving {@code RowSet} objects a choice of
 * synchronization mechanisms.
 *
 * <h3><a id="arch">2.0 Service Provider Interface Architecture</a></h3>
 * <b>2.1 Overview</b>
 * <p>
 * The Service Provider Interface provides a pluggable mechanism by which
 * {@code SyncProvider} implementations can be registered and then generated when
 * required. The lazy reference mechanism employed by the {@code SyncFactory} limits
 * unnecessary resource consumption by not creating an instance until it is
 * required by a disconnected
 * {@code RowSet} object. The {@code SyncFactory} class also provides
 * a standard API to configure logging options and streams that <b>may</b> be provided
 * by a particular {@code SyncProvider} implementation.
 * <p>
 * <b>2.2 Registering with the {@code SyncFactory}</b>
 * <p>
 * A third party {@code SyncProvider} implementation must be registered with the
 * {@code SyncFactory} in order for a disconnected {@code RowSet} object
 * to obtain it and thereby use its {@code javax.sql.RowSetReader} and
 * {@code javax.sql.RowSetWriter}
 * implementations. The following registration mechanisms are available to all
 * {@code SyncProvider} implementations:
 * <ul>
 * <li><b>System properties</b> - Properties set at the command line. These
 * properties are set at run time and apply system-wide per invocation of the Java
 * application. See the section <a href="#reldocs">"Related Documentation"</a>
 * further related information.
 *
 * <li><b>Property Files</b> - Properties specified in a standard property file.
 * This can be specified using a System Property or by modifying a standard
 * property file located in the platform run-time. The
 * reference implementation of this technology includes a standard property
 * file than can be edited to add additional {@code SyncProvider} objects.
 *
 * <li><b>JNDI Context</b> - Available providers can be registered on a JNDI
 * context. The {@code SyncFactory} will attempt to load {@code SyncProvider}
 * objects bound to the context and register them with the factory. This
 * context must be supplied to the {@code SyncFactory} for the mechanism to
 * function correctly.
 * </ul>
 * <p>
 * Details on how to specify the system properties or properties in a property file
 * and how to configure the JNDI Context are explained in detail in the
 * <a href="SyncFactory.html">{@code SyncFactory}</a> class description.
 * <p>
 * <b>2.3 SyncFactory Provider Instance Generation Policies</b>
 * <p>
 * The {@code SyncFactory} generates a requested {@code SyncProvider}
 * object if the provider has been correctly registered.  The
 * following policies are adhered to when either a disconnected {@code RowSet} object
 * is instantiated with a specified {@code SyncProvider} implementation or is
 * reconfigured at runtime with an alternative {@code SyncProvider} object.
 * <ul>
 * <li> If a {@code SyncProvider} object is specified and the {@code SyncFactory}
 * contains <i>no</i> reference to the provider, a {@code SyncFactoryException} is
 * thrown.
 *
 * <li> If a {@code SyncProvider} object is specified and the {@code SyncFactory}
 * contains a reference to the provider, the requested provider is supplied.
 *
 * <li> If no {@code SyncProvider} object is specified, the reference
 * implementation provider {@code RIOptimisticProvider} is supplied.
 * </ul>
 * <p>
 * These policies are explored in more detail in the <a href="SyncFactory.html">
 * {@code SyncFactory}</a> class.
 *
 * <h3><a id="impl">3.0 SyncProvider Implementer's Guide</a></h3>
 *
 * <b>3.1 Requirements</b>
 * <p>
 * A compliant {@code SyncProvider} implementation that is fully pluggable
 * into the {@code SyncFactory} <b>must</b> extend and implement all
 * abstract methods in the <a href="SyncProvider.html">{@code SyncProvider}</a>
 * class. In addition, an implementation <b>must</b> determine the
 * grade, locking and updatable view capabilities defined in the
 * {@code SyncProvider} class definition. One or more of the
 * {@code SyncProvider} description criteria <b>must</b> be supported. It
 * is expected that vendor implementations will offer a range of grade, locking, and
 * updatable view capabilities.
 * <p>
 * Furthermore, the {@code SyncProvider} naming convention <b>must</b> be followed as
 * detailed in the <a href="SyncProvider.html">{@code SyncProvider}</a> class
 * description.
 * <p>
 * <b>3.2 Grades</b>
 * <p>
 * JSR 114 defines a set of grades to describe the quality of synchronization
 * a {@code SyncProvider} object can offer a disconnected {@code RowSet}
 * object. These grades are listed from the lowest quality of service to the highest.
 * <ul>
 * <li><b>GRADE_NONE</b> - No synchronization with the originating data source is
 * provided. A {@code SyncProvider} implementation returning this grade will simply
 * attempt to write any data that has changed in the {@code RowSet} object to the
 *underlying data source, overwriting whatever is there. No attempt is made to compare
 * original values with current values to see if there is a conflict. The
 * {@code RIXMLProvider} is implemented with this grade.
 *
 * <li><b>GRADE_CHECK_MODIFIED_AT_COMMIT</b> - A low grade of optimistic synchronization.
 * A {@code SyncProvider} implementation returning this grade
 * will check for conflicts in rows that have changed between the last synchronization
 * and the current synchronization under way. Any changes in the originating data source
 * that have been modified will not be reflected in the disconnected {@code RowSet}
 * object. If there are no conflicts, changes in the {@code RowSet} object will be
 * written to the data source. If there are conflicts, no changes are written.
 * The {@code RIOptimisticProvider} implementation uses this grade.
 *
 * <li><b>GRADE_CHECK_ALL_AT_COMMIT</b> - A high grade of optimistic synchronization.
 * A {@code SyncProvider} implementation   returning this grade
 * will check all rows, including rows that have not changed in the disconnected
 * {@code RowSet} object. In this way, any changes to rows in the underlying
 * data source will be reflected in the disconnected {@code RowSet} object
 * when the synchronization finishes successfully.
 *
 * <li><b>GRADE_LOCK_WHEN_MODIFIED</b> - A pessimistic grade of synchronization.
 * {@code SyncProvider} implementations returning this grade will lock
 * the row in the originating  data source that corresponds to the row being changed
 * in the {@code RowSet} object to reduce the possibility of other
 * processes modifying the same data in the data source.
 *
 * <li><b>GRADE_LOCK_WHEN_LOADED</b> - A higher pessimistic synchronization grade.
 * A {@code SyncProvider} implementation returning this grade will lock
 * the entire view and/or  table affected by the original query used to
 * populate a {@code RowSet} object.
 * </ul>
 * <p>
 * <b>3.3 Locks</b>
 * <p>
 * JSR 114 defines a set of constants that specify whether any locks have been
 * placed on a {@code RowSet} object's underlying data source and, if so,
 * on which constructs the locks are placed.  These locks will remain on the data
 * source while the {@code RowSet} object is disconnected from the data source.
 * <P>
 * These constants <b>should</b> be considered complementary to the
 * grade constants. The default setting for the majority of grade settings requires
 * that no data source locks remain when a {@code RowSet} object is disconnected
 * from its data source.
 * The grades {@code GRADE_LOCK_WHEN_MODIFIED} and
 * {@code GRADE_LOCK_WHEN_LOADED} allow a disconnected {@code RowSet} object
 * to have a fine-grained control over the degree of locking.
 * <ul>
 * <li><b>DATASOURCE_NO_LOCK</b> - No locks remain on the originating data source.
 * This is the default lock setting for all {@code SyncProvider} implementations
 * unless otherwise directed by a {@code RowSet} object.
 *
 * <li><b>DATASOURCE_ROW_LOCK</b> - A lock is placed on the rows that are touched by
 * the original SQL query used to populate the {@code RowSet} object.
 *
 * <li><b>DATASOURCE_TABLE_LOCK</b> - A lock is placed on all tables that are touched
 * by the query that was used to populate the {@code RowSet} object.
 *
 * <li><b>DATASOURCE_DB_LOCK</b>
 * A lock is placed on the entire data source that is used by the {@code RowSet}
 * object.
 * </ul>
 * <p>
 * <b>3.4 Updatable Views</b>
 * <p>
 * A {@code RowSet} object may be populated with data from an SQL {@code VIEW}.
 * The following constants indicate whether a {@code SyncProvider} object can
 * update data in the table or tables from which the {@code VIEW} was derived.
 * <ul>
 * <li><b>UPDATABLE_VIEW_SYNC</b>
 * Indicates that a {@code SyncProvider} implementation  supports synchronization
 * to the table or tables from which the SQL {@code VIEW} used to populate
 * a {@code RowSet} object is derived.
 *
 * <li><b>NONUPDATABLE_VIEW_SYNC</b>
 * Indicates that a {@code SyncProvider} implementation  does <b>not</b> support
 * synchronization to the table or tables from which the SQL {@code VIEW}
 * used to populate  a {@code RowSet} object is derived.
 * </ul>
 * <p>
 * <b>3.5 Usage of {@code SyncProvider} Grading and Locking</b>
 * <p>
 * In the example below, the reference {@code CachedRowSetImpl} implementation
 * reconfigures its current {@code SyncProvider} object by calling the
 * {@code setSyncProvider} method.<br>
 *
 * <PRE>
 *   CachedRowSetImpl crs = new CachedRowSetImpl();
 *   crs.setSyncProvider("com.foo.bar.HASyncProvider");
 * </PRE>
 *   An application can retrieve the {@code SyncProvider} object currently in use
 * by a disconnected {@code RowSet} object. It can also retrieve the
 * grade of synchronization with which the provider was implemented and the degree of
 * locking currently in use.  In addition, an application has the flexibility to set
 * the degree of locking to be used, which can increase the possibilities for successful
 * synchronization.  These operation are shown in the following code fragment.
 * <PRE>
 *   SyncProvider sync = crs.getSyncProvider();
 *
 *   switch (sync.getProviderGrade()) {
 *   case: SyncProvider.GRADE_CHECK_ALL_AT_COMMIT
 *         //A high grade of optimistic synchronization
 *    break;
 *    case: SyncProvider.GRADE_CHECK_MODIFIED_AT_COMMIT
 *         //A low grade of optimistic synchronization
 *    break;
 *    case: SyncProvider.GRADE_LOCK_WHEN_LOADED
 *         // A pessimistic synchronization grade
 *    break;
 *    case: SyncProvider.GRADE_LOCK_WHEN_MODIFIED
 *         // A pessimistic synchronization grade
 *    break;
 *    case: SyncProvider.GRADE_NONE
 *      // No synchronization with the originating data source provided
 *    break;
 *    }
 *
 *    switch (sync.getDataSourceLock() {
 *      case: SyncProvider.DATASOURCE_DB_LOCK
 *       // A lock is placed on the entire datasource that is used by the
 *       // {@code RowSet} object
 *       break;
 *
 *      case: SyncProvider.DATASOURCE_NO_LOCK
 *       // No locks remain on the  originating data source.
 *      break;
 *
 *      case: SyncProvider.DATASOURCE_ROW_LOCK
 *       // A lock is placed on the rows that are  touched by the original
 *       // SQL statement used to populate
 *       // the RowSet object that is using the SyncProvider
 *       break;
 *
 *      case: DATASOURCE_TABLE_LOCK
 *       // A lock is placed on  all tables that are touched by the original
 *       // SQL statement used to populated
 *       // the RowSet object that is using the SyncProvider
 *      break;
 *
 * </PRE>
 *    It is also possible using the static utility method in the
 * {@code SyncFactory} class to determine the list of {@code SyncProvider}
 * implementations currently registered with the {@code SyncFactory}.
 *
 * <pre>
 *       Enumeration e = SyncFactory.getRegisteredProviders();
 * </pre>
 *
 *
 * <h3><a id="resolving">4.0 Resolving Synchronization Conflicts</a></h3>
 *
 * The interface {@code SyncResolver} provides a way for an application to
 * decide manually what to do when a conflict occurs. When the {@code CachedRowSet}
 * method {@code acceptChanges} finishes and has detected one or more conflicts,
 * it throws a {@code SyncProviderException} object.  An application can
 * catch the exception and
 * have it retrieve a {@code SyncResolver} object by calling the method
 * {@code SyncProviderException.getSyncResolver()}.
 * <P>
 * A {@code SyncResolver} object, which is a special kind of
 * {@code CachedRowSet} object or
 * a {@code JdbcRowSet} object that has implemented the {@code SyncResolver}
 * interface,  examines the conflicts row by row. It is a duplicate of the
 * {@code RowSet} object being synchronized except that it contains only the data
 * from the data source this is causing a conflict. All of the other column values are
 * set to {@code null}. To navigate from one conflict value to another, a
 * {@code SyncResolver} object provides the methods {@code nextConflict} and
 * {@code previousConflict}.
 * <P>
 * The {@code SyncResolver} interface also
 * provides methods for doing the following:
 * <UL>
 *  <LI>finding out whether the conflict involved an update, a delete, or an insert
 *  <LI>getting the value in the data source that caused the conflict
 *  <LI>setting the value that should be in the data source if it needs to be changed
 *      or setting the value that should be in the {@code RowSet} object if it needs
 *      to be changed
 * </UL>
 * <P>
 * When the {@code CachedRowSet} method {@code acceptChanges} is called, it
 * delegates to the {@code RowSet} object's  {@code SyncProvider} object.
 * How the writer provided by that {@code SyncProvider} object is implemented
 * determines what level (grade) of checking for conflicts will be done.  After all
 * checking for conflicts is completed and one or more conflicts has been found, the method
 * {@code acceptChanges} throws a {@code SyncProviderException} object. The
 * application can catch the exception and use it to obtain a {@code SyncResolver} object.
 * <P>
 * The application can then use {@code SyncResolver} methods to get information
 * about each conflict and decide what to do.  If the application logic or the user
 * decides that a value in the {@code RowSet} object should be the one to
 * persist, the application or user can overwrite the data source value with it.
 * <P>
 * The comment for the {@code SyncResolver} interface has more detail.
 *
 * <h3><a id="relspec">5.0 Related Specifications</a></h3>
 * <ul>
 * <li><a href="http://docs.oracle.com/javase/jndi/tutorial/index.html">JNDI</a>
 * <li><a href="{@docRoot}/java.logging/java/util/logging/package-summary.html">Java Logging
 * APIs</a>
 * </ul>
 * <h3><a id="reldocs">6.0 Related Documentation</a></h3>
 * <ul>
 * <li><a href="http://docs.oracle.com/javase/tutorial/jdbc/">DataSource for JDBC
 * Connections</a>
 * </ul>
 */
package javax.sql.rowset.spi;
