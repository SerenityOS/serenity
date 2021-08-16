/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

import java.sql.SQLException;

/**
 * An interface that defines the implementation of a factory that is used
 * to obtain different types of {@code RowSet} implementations.
 *
 * @author Lance Andersen
 * @since 1.7
 */
public interface RowSetFactory{

    /**
     * <p>Creates a new instance of a CachedRowSet.</p>
     *
     * @return A new instance of a CachedRowSet.
     *
     * @throws SQLException if a CachedRowSet cannot
     *   be created.
     *
     * @since 1.7
     */
    public CachedRowSet createCachedRowSet() throws SQLException;

    /**
     * <p>Creates a new instance of a FilteredRowSet.</p>
     *
     * @return A new instance of a FilteredRowSet.
     *
     * @throws SQLException if a FilteredRowSet cannot
     *   be created.
     *
     * @since 1.7
     */
    public FilteredRowSet createFilteredRowSet() throws SQLException;

    /**
     * <p>Creates a new instance of a JdbcRowSet.</p>
     *
     * @return A new instance of a JdbcRowSet.
     *
     * @throws SQLException if a JdbcRowSet cannot
     *   be created.
     *
     * @since 1.7
     */
    public  JdbcRowSet createJdbcRowSet() throws SQLException;

    /**
     * <p>Creates a new instance of a JoinRowSet.</p>
     *
     * @return A new instance of a JoinRowSet.
     *
     * @throws SQLException if a JoinRowSet cannot
     *   be created.
     *
     * @since 1.7
     */
    public  JoinRowSet createJoinRowSet() throws SQLException;

    /**
     * <p>Creates a new instance of a WebRowSet.</p>
     *
     * @return A new instance of a WebRowSet.
     *
     * @throws SQLException if a WebRowSet cannot
     *   be created.
     *
     * @since 1.7
     */
    public  WebRowSet createWebRowSet() throws SQLException;

}
