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

package javax.sql;

import java.sql.*;

/**
 * The interface that a {@code RowSet} object implements in order to
 * present itself to a {@code RowSetReader} or {@code RowSetWriter}
 * object. The {@code RowSetInternal} interface contains
 * methods that let the reader or writer access and modify the internal
 * state of the rowset.
 *
 * @since 1.4
 */

public interface RowSetInternal {

  /**
   * Retrieves the parameters that have been set for this
   * {@code RowSet} object's command.
   *
   * @return an array of the current parameter values for this {@code RowSet}
   *         object's command
   * @throws SQLException if a database access error occurs
   */
  Object[] getParams() throws SQLException;

  /**
   * Retrieves the {@code Connection} object that was passed to this
   * {@code RowSet} object.
   *
   * @return the {@code Connection} object passed to the rowset
   *      or {@code null} if none was passed
   * @throws SQLException if a database access error occurs
   */
  Connection getConnection() throws SQLException;

  /**
   * Sets the given {@code RowSetMetaData} object as the
   * {@code RowSetMetaData} object for this {@code RowSet}
   * object. The {@code RowSetReader} object associated with the rowset
   * will use {@code RowSetMetaData} methods to set the values giving
   * information about the rowset's columns.
   *
   * @param md the {@code RowSetMetaData} object that will be set with
   *        information about the rowset's columns
   *
   * @throws SQLException if a database access error occurs
   */
  void setMetaData(RowSetMetaData md) throws SQLException;

  /**
   * Retrieves a {@code ResultSet} object containing the original
   * value of this {@code RowSet} object.
   * <P>
   * The cursor is positioned before the first row in the result set.
   * Only rows contained in the result set returned by the method
   * {@code getOriginal} are said to have an original value.
   *
   * @return the original value of the rowset
   * @throws SQLException if a database access error occurs
   */
  public ResultSet getOriginal() throws SQLException;

  /**
   * Retrieves a {@code ResultSet} object containing the original value
   * of the current row only.  If the current row has no original value,
   * an empty result set is returned. If there is no current row,
   * an exception is thrown.
   *
   * @return the original value of the current row as a {@code ResultSet}
   *         object
   * @throws SQLException if a database access error occurs or this method
   *         is called while the cursor is on the insert row, before the
   *         first row, or after the last row
   */
  public ResultSet getOriginalRow() throws SQLException;

}
