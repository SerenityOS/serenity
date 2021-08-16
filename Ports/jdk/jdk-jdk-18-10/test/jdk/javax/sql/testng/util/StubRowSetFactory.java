/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package util;

import java.sql.SQLException;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.FilteredRowSet;
import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.JoinRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.WebRowSet;

public class StubRowSetFactory implements RowSetFactory {

    @Override
    public CachedRowSet createCachedRowSet() throws SQLException {
        return new StubCachedRowSetImpl();
    }

    @Override
    public FilteredRowSet createFilteredRowSet() throws SQLException {
        return new StubFilteredRowSetImpl();
    }

    @Override
    public JdbcRowSet createJdbcRowSet() throws SQLException {
        return new StubJdbcRowSetImpl();
    }

    @Override
    public JoinRowSet createJoinRowSet() throws SQLException {
        return new StubJoinRowSetImpl();
    }

    @Override
    public WebRowSet createWebRowSet() throws SQLException {
        return new StubWebRowSetImpl();
    }

}
