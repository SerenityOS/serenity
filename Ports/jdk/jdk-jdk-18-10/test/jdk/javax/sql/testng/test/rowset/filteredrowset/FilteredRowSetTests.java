/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package test.rowset.filteredrowset;

import java.sql.SQLException;
import javax.sql.RowSet;
import javax.sql.rowset.FilteredRowSet;
import javax.sql.rowset.Predicate;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import test.rowset.webrowset.CommonWebRowSetTests;

public class FilteredRowSetTests extends CommonWebRowSetTests {

    private FilteredRowSet frs;

    @BeforeMethod
    public void setUpMethod() throws Exception {
        frs = createCoffeeHousesRowSet();
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
        frs.close();
    }

    protected FilteredRowSet newInstance() throws SQLException {
        return rsf.createFilteredRowSet();
    }

    /*
     * Validate getFilter returns null if setFilter has not been called
     */
    @Test
    public void FilteredRowSetTest0000() throws SQLException {
        assertNull(frs.getFilter());
    }

    /*
     * Call setFilter to set a Predicate and validate that getFilter
     * returns the correct Predicate
     */
    @Test
    public void FilteredRowSetTest0001() throws SQLException {
        Predicate p = new PrimaryKeyFilter(0, 100030, 1);
        frs.setFilter(p);
        assertTrue(frs.getFilter().equals(p));
        frs.setFilter(null);
        assertNull(frs.getFilter());
    }

    /*
     * Validate that the correct rows are returned when a Predicate using
     * a column index is used
     */
    @Test
    public void FilteredRowSetTest0002() throws SQLException {
        Object[] expectedKeys = {
            10023, 10040, 10042, 10024, 10039, 10041, 10035, 10037, 10034
        };
        frs.setFilter(new PrimaryKeyFilter(10000, 10999, 1));
        assertEquals(getPrimaryKeys(frs), expectedKeys);
    }

    /*
     * Validate that the correct rows are returned when a Predicate using
     * a column Label is used
     */
    @Test
    public void FilteredRowSetTest0003() throws SQLException {
        Object[] expectedKeys = {
            10023, 10040, 10042, 10024, 10039, 10041, 10035, 10037, 10034
        };
        frs.setFilter(new PrimaryKeyFilter(10000, 10999, "STORE_ID"));
        assertEquals(getPrimaryKeys(frs), expectedKeys);

    }

    /*
     * Validate that the correct rows are returned when a Predicate using
     * a column index is used
     */
    @Test
    public void FilteredRowSetTest0004() throws SQLException {
        Object[] expectedKeys = {
            10040, 10042, 10041, 10035, 10037
        };
        String[] cityArray = {"SF", "LA"};
        frs.setFilter(new CityFilter(cityArray, 2));
        assertEquals(getPrimaryKeys(frs), expectedKeys);
    }

    /*
     * Validate that the correct rows are returned when a Predicate using
     * a column Label is used
     */
    @Test
    public void FilteredRowSetTest0005() throws SQLException {
        Object[] expectedKeys = {
            10040, 10042, 10041, 10035, 10037
        };
        String[] cityArray = {"SF", "LA"};
        frs.setFilter(new CityFilter(cityArray, "CITY"));
        assertEquals(getPrimaryKeys(frs), expectedKeys);
    }


    // Tests that are common but need to be disabled due to an implementation bug


    @Test(dataProvider = "rowSetType", enabled = false)
    public void commonCachedRowSetTest0043(RowSet rs) throws Exception {
        // Need to fix bug in FilteredRowSets
    }

}
