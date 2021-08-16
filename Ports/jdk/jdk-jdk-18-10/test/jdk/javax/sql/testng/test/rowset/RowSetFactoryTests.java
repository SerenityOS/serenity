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
package test.rowset;

import java.sql.SQLException;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import util.BaseTest;

public class RowSetFactoryTests extends BaseTest {

    // RowSet implementations that we are testing for
    private final String DEFAULT_CACHEDROWSET_CLASSNAME = "com.sun.rowset.CachedRowSetImpl";
    private final String DEFAULT_FILTEREDROWSET_CLASSNAME = "com.sun.rowset.FileteredRowSetImpl";
    private final String DEFAULT_JDBCROWSET_CLASSNAME = "com.sun.rowset.JdbcRowSetImpl";
    private final String DEFAULT_JOINROWSET_CLASSNAME = "com.sun.rowset.JoinRowSetImpl";
    private final String DEFAULT_WEBROWSET_CLASSNAME = "com.sun.rowset.WebRowSetImpl";
    private final String STUB_FACTORY_CLASSNAME = "util.StubRowSetFactory";
    private final String STUB_CACHEDROWSET_CLASSNAME = "util.StubCachedRowSetImpl";
    private final String STUB_FILTEREDROWSET_CLASSNAME = "util.StubFilteredRowSetImpl";
    private final String STUB_JDBCROWSET_CLASSNAME = "util.StubJdbcRowSetImpl";
    private final String STUB_JOINROWSET_CLASSNAME = "util.StubJoinRowSetImpl";
    private final String STUB_WEBROWSET_CLASSNAME = "util.StubWebRowSetImpl";


    /*
     * Validate that the RowSetFactory returned by RowSetProvider.newFactory()
     * returns the correct RowSet implementations
     */
    @Test(dataProvider = "RowSetValues", enabled = true)
    public void test(RowSetFactory rsf, String impl) throws SQLException {
        validateRowSetImpl(rsf, impl);
    }

    /*
     * Utility Method to validate the RowsetFactory returns the correct
     * RowSet implementation
     */
    private void validateRowSetImpl(RowSetFactory rsf, String implName)
            throws SQLException {
        assertNotNull(rsf, "RowSetFactory should not be null");
        switch (implName) {
            case DEFAULT_CACHEDROWSET_CLASSNAME:
                assertTrue(rsf.createCachedRowSet() instanceof com.sun.rowset.CachedRowSetImpl);
                break;
            case DEFAULT_FILTEREDROWSET_CLASSNAME:
                assertTrue(rsf.createFilteredRowSet() instanceof com.sun.rowset.FilteredRowSetImpl);
                break;
            case DEFAULT_JDBCROWSET_CLASSNAME:
                assertTrue(rsf.createJdbcRowSet() instanceof com.sun.rowset.JdbcRowSetImpl);
                break;
            case DEFAULT_JOINROWSET_CLASSNAME:
                assertTrue(rsf.createJoinRowSet() instanceof com.sun.rowset.JoinRowSetImpl);
                break;
            case DEFAULT_WEBROWSET_CLASSNAME:
                assertTrue(rsf.createWebRowSet() instanceof com.sun.rowset.WebRowSetImpl);
                break;
            case STUB_CACHEDROWSET_CLASSNAME:
                assertTrue(rsf.createCachedRowSet() instanceof util.StubCachedRowSetImpl);
                break;
            case STUB_FILTEREDROWSET_CLASSNAME:
                assertTrue(rsf.createFilteredRowSet() instanceof util.StubFilteredRowSetImpl);
                break;
            case STUB_JDBCROWSET_CLASSNAME:
                assertTrue(rsf.createJdbcRowSet() instanceof util.StubJdbcRowSetImpl);
                break;
            case STUB_WEBROWSET_CLASSNAME:
                assertTrue(rsf.createWebRowSet() instanceof util.StubWebRowSetImpl);
                break;
        }

    }

    /*
     * DataProvider used to provide the RowSetFactory and the RowSet
     * implementation that should be returned
     */
    @DataProvider(name = "RowSetValues")
    private Object[][] RowSetValues() throws SQLException {
        RowSetFactory rsf = RowSetProvider.newFactory();
        RowSetFactory rsf1 = RowSetProvider.newFactory(STUB_FACTORY_CLASSNAME, null);
        return new Object[][]{
            {rsf, DEFAULT_CACHEDROWSET_CLASSNAME},
            {rsf, DEFAULT_FILTEREDROWSET_CLASSNAME},
            {rsf, DEFAULT_JDBCROWSET_CLASSNAME},
            {rsf, DEFAULT_JOINROWSET_CLASSNAME},
            {rsf, DEFAULT_WEBROWSET_CLASSNAME},
            {rsf1, STUB_CACHEDROWSET_CLASSNAME},
            {rsf1, STUB_FILTEREDROWSET_CLASSNAME},
            {rsf1, STUB_JDBCROWSET_CLASSNAME},
            {rsf1, STUB_JOINROWSET_CLASSNAME},
            {rsf1, STUB_WEBROWSET_CLASSNAME}

        };
    }
}
