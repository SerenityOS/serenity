/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * Created on Apr 28, 2005
 */
package javax.sql;

/**
 * An object that registers to be notified of events that occur on PreparedStatements
 * that are in the Statement pool.
 * <p>
 * The JDBC 3.0 specification added the maxStatements
 * {@code ConnectionPooledDataSource} property to provide a standard mechanism for
 * enabling the pooling of {@code PreparedStatements}
 * and to specify the size of the statement
 * pool.  However, there was no way for a driver to notify an external
 * statement pool when a {@code PreparedStatement} becomes invalid.  For some databases, a
 * statement becomes invalid if a DDL operation is performed that affects the
 * table.  For example an application may create a temporary table to do some work
 * on the table and then destroy it.  It may later recreate the same table when
 * it is needed again.  Some databases will invalidate any prepared statements
 * that reference the temporary table when the table is dropped.
 * <p>
 * Similar to the methods defined in the {@code ConnectionEventListener} interface,
 * the driver will call the {@code StatementEventListener.statementErrorOccurred}
 * method prior to throwing any exceptions when it detects a statement is invalid.
 * The driver will also call the {@code StatementEventListener.statementClosed}
 * method when a {@code PreparedStatement} is closed.
 * <p>
 * Methods which allow a component to register a StatementEventListener with a
 * {@code PooledConnection} have been added to the {@code PooledConnection} interface.
 *
 * @since 1.6
 */
public interface StatementEventListener  extends java.util.EventListener{
  /**
   * The driver calls this method on all {@code StatementEventListener}s registered on the connection when it detects that a
   * {@code PreparedStatement} is closed.
   *
   * @param event an event object describing the source of
   * the event and that the {@code PreparedStatement} was closed.
   * @since 1.6
   */
  void statementClosed(StatementEvent event);

        /**
         * The driver calls this method on all {@code StatementEventListener}s
         * registered on the connection when it detects that a
         * {@code PreparedStatement} is invalid. The driver calls this method
         * just before it throws the {@code SQLException},
         * contained in the given event, to the application.
         *
         * @param event    an event object describing the source of the event,
         *                 the statement that is invalid and the exception the
         *                 driver is about to throw.  The source of the event is
         *                 the {@code PooledConnection} which the invalid {@code PreparedStatement}
         *                 is associated with.
         *
         * @since 1.6
         */
        void statementErrorOccurred(StatementEvent event);

}
