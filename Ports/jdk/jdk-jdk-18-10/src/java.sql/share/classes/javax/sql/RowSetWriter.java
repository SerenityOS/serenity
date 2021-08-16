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
 * An object that implements the {@code RowSetWriter} interface,
 * called a <i>writer</i>. A writer may be registered with a {@code RowSet}
 * object that supports the reader/writer paradigm.
 * <P>
 * If a disconnected {@code RowSet} object modifies some of its data,
 * and it has a writer associated with it, it may be implemented so that it
 * calls on the writer's {@code writeData} method internally
 * to write the updates back to the data source. In order to do this, the writer
 * must first establish a connection with the rowset's data source.
 * <P>
 * If the data to be updated has already been changed in the data source, there
 * is a conflict, in which case the writer will not write
 * the changes to the data source.  The algorithm the writer uses for preventing
 * or limiting conflicts depends entirely on its implementation.
 *
 * @since 1.4
 */

public interface RowSetWriter {

  /**
   * Writes the changes in this {@code RowSetWriter} object's
   * rowset back to the data source from which it got its data.
   *
   * @param caller the {@code RowSet} object (1) that has implemented the
   *         {@code RowSetInternal} interface, (2) with which this writer is
   *        registered, and (3) that called this method internally
   * @return {@code true} if the modified data was written; {@code false}
   *          if not, which will be the case if there is a conflict
   * @throws SQLException if a database access error occurs
   */
  boolean writeData(RowSetInternal caller) throws SQLException;

}
