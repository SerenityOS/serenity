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
package test.rowset.joinrowset;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import javax.sql.RowSet;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.JoinRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.WebRowSet;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import test.rowset.webrowset.CommonWebRowSetTests;

public class JoinRowSetTests extends CommonWebRowSetTests {

    private final String SUPPLIERS_TABLE = "SUPPLIERS";
    // Expected COF_IDs to be found
    private final Object[] EXPECTED = {4, 1};
    // SUPPLIERS Primary Key to use to validate the joins
    private final int SUP_ID = 101;
    // Join Column between the SUPPLIERS and COFFEES table
    private final String JOIN_COLNAME = "SUP_ID";
    // Column index in COFFEES table which contains SUP_ID
    private final int COFFEES_JOIN_COLUMN_INDEX = 3;
    // Column index in SUPPLIERS table which contains SUP_ID
    private final int SUPPLIERS_JOIN_COLUMN_INDEX = 1;

    @Override
    protected JoinRowSet newInstance() throws SQLException {
        return rsf.createJoinRowSet();
    }

    /*
     * Initializes the SUPPLIERS metadata
     */
    private void initSuppliersMetaData(CachedRowSet crs) throws SQLException {
        RowSetMetaDataImpl rsmd = new RowSetMetaDataImpl();

        /*
         *  CREATE TABLE SUPPLIERS (
         *   SUP_ID INTEGER NOT NULL,
         *   SUP_NAME VARCHAR(32) NOT NULL,
         *   STREET VARCHAR(32) NOT NULL,
         *   CITY VARCHAR(32) NOT NULL,
         *   STATE CHAR(2) NOT NULL,
         *   ZIP CHAR(5) NOT NULL,
         *   PRIMARY KEY (SUP_ID))
         */
        rsmd.setColumnCount(6);
        rsmd.setColumnName(1, "SUP_ID");
        rsmd.setColumnName(2, "SUP_NAME");
        rsmd.setColumnName(3, "STREET");
        rsmd.setColumnName(4, "CITY");
        rsmd.setColumnName(5, "STATE");
        rsmd.setColumnName(6, "ZIP");

        rsmd.setColumnType(1, Types.INTEGER);
        rsmd.setColumnType(2, Types.VARCHAR);
        rsmd.setColumnType(3, Types.VARCHAR);
        rsmd.setColumnType(4, Types.VARCHAR);
        rsmd.setColumnType(5, Types.CHAR);
        rsmd.setColumnType(6, Types.CHAR);
        crs.setMetaData(rsmd);
        crs.setTableName(SUPPLIERS_TABLE);
    }

    /*
     * Add rows to SUPPLIERS table
     */
    protected void createSuppiersRows(RowSet rs) throws SQLException {

        // insert into SUPPLIERS values(49, 'Superior Coffee', '1 Party Place',
        // 'Mendocino', 'CA', '95460')
        rs.moveToInsertRow();
        rs.updateInt(1, 49);
        rs.updateString(2, "Superior Coffee");
        rs.updateString(3, "1 Party Place");
        rs.updateString(4, "Mendocino");
        rs.updateString(5, "CA");
        rs.updateString(6, "95460");
        rs.insertRow();

        // insert into SUPPLIERS values(101, 'Acme, Inc.', '99 Market Street',
        // 'Groundsville', 'CA', '95199')
        rs.moveToInsertRow();
        rs.updateInt(1, 101);
        rs.updateString(2, "Acme, Inc.");
        rs.updateString(3, "99 Market Street");
        rs.updateString(4, "Groundsville");
        rs.updateString(5, "CA");
        rs.updateString(6, "95199");
        rs.insertRow();
        // insert into SUPPLIERS values(150, 'The High Ground',
        // '100 Coffee Lane', 'Meadows', 'CA', '93966')
        rs.moveToInsertRow();
        rs.updateInt(1, 150);
        rs.updateString(2, "The High Ground");
        rs.updateString(3, "100 Coffee Lane");
        rs.updateString(4, "Meadows");
        rs.updateString(5, "CA");
        rs.updateString(6, "93966");
        rs.insertRow();
        // insert into SUPPLIERS values(456," 'Restaurant Supplies, Inc.',
        // '200 Magnolia Street', 'Meadows', 'CA', '93966')
        rs.moveToInsertRow();
        rs.updateInt(1, 456);
        rs.updateString(2, "Restaurant Supplies, Inc.");
        rs.updateString(3, "200 Magnolia Stree");
        rs.updateString(4, "Meadows");
        rs.updateString(5, "CA");
        rs.updateString(6, "93966");
        rs.insertRow();
        // insert into SUPPLIERS values(927, 'Professional Kitchen',
        // '300 Daisy Avenue', 'Groundsville'," 'CA', '95199')
        rs.moveToInsertRow();
        rs.updateInt(1, 927);
        rs.updateString(2, "Professional Kitchen");
        rs.updateString(3, "300 Daisy Avenue");
        rs.updateString(4, "Groundsville");
        rs.updateString(5, "CA");
        rs.updateString(6, "95199");
        rs.insertRow();
    }

    /*
     * DataProvider used to set parameters for basic types that are supported
     */
    @DataProvider(name = "createCachedRowSetsToUse")
    private Object[][] createCachedRowSetsToUse() throws SQLException {
        CachedRowSet crs = rsf.createCachedRowSet();
        initCoffeesMetaData(crs);
        createCoffeesRows(crs);
        // Make sure you are not on the insertRow
        crs.moveToCurrentRow();
        CachedRowSet crs1 = rsf.createCachedRowSet();
        initSuppliersMetaData(crs1);
        createSuppiersRows(crs1);
        // Make sure you are not on the insertRow
        crs1.moveToCurrentRow();
        return new Object[][]{
            {crs, crs1}
        };
    }

    /*
     * Validate that the correct coffees are returned for SUP_ID
     */
    private void validateResults(final JoinRowSet jrs) throws SQLException {
        List<Integer> results = new ArrayList<>();
        jrs.beforeFirst();
        while (jrs.next()) {
            if (jrs.getInt(JOIN_COLNAME) == SUP_ID) {
                results.add(jrs.getInt("COF_ID"));
            }
        }
        assertEquals(results.toArray(), EXPECTED);
    }

    /*
     * Join two CachedRowSets specifying a column name to join against
     */
    @Test(dataProvider = "createCachedRowSetsToUse")
    public void joinRowSetTests0000(CachedRowSet crs, CachedRowSet crs1)
            throws Exception {

        try (JoinRowSet jrs = newInstance()) {
            jrs.addRowSet(crs, JOIN_COLNAME);
            jrs.addRowSet(crs1, JOIN_COLNAME);
            validateResults(jrs);
            crs.close();
            crs1.close();
        }
    }

    /*
     * Join two CachedRowSets specifying a column index to join against
     */
    @Test(dataProvider = "createCachedRowSetsToUse")
    public void joinRowSetTests0001(CachedRowSet crs, CachedRowSet crs1)
            throws Exception {

        try (JoinRowSet jrs = newInstance()) {
            jrs.addRowSet(crs, COFFEES_JOIN_COLUMN_INDEX);
            jrs.addRowSet(crs1, SUPPLIERS_JOIN_COLUMN_INDEX);
            validateResults(jrs);
            crs.close();
            crs1.close();
        }
    }

    /*
     * Join two CachedRowSets specifying a column name to join against
     */
    @Test(dataProvider = "createCachedRowSetsToUse")
    public void joinRowSetTests0002(CachedRowSet crs, CachedRowSet crs1)
            throws Exception {

        try (JoinRowSet jrs = newInstance()) {
            RowSet[] rowsets = {crs, crs1};
            String[] joinCols = {JOIN_COLNAME, JOIN_COLNAME};
            jrs.addRowSet(rowsets, joinCols);
            validateResults(jrs);
            crs.close();
            crs1.close();
        }
    }

    /*
     * Join two CachedRowSets specifying a column index to join against
     */
    @Test(dataProvider = "createCachedRowSetsToUse")
    public void joinRowSetTests0003(CachedRowSet crs, CachedRowSet crs1)
            throws Exception {

        try (JoinRowSet jrs = newInstance()) {
            RowSet[] rowsets = {crs, crs1};
            int[] joinCols = {COFFEES_JOIN_COLUMN_INDEX,
                SUPPLIERS_JOIN_COLUMN_INDEX};
            jrs.addRowSet(rowsets, joinCols);
            validateResults(jrs);
            crs.close();
            crs1.close();
        }
    }

    /*
     * Join two CachedRowSets specifying a column name to join against
     */
    @Test(dataProvider = "createCachedRowSetsToUse")
    public void joinRowSetTests0005(CachedRowSet crs, CachedRowSet crs1)
            throws Exception {

        try (JoinRowSet jrs = newInstance()) {
            crs.setMatchColumn(JOIN_COLNAME);
            crs1.setMatchColumn(JOIN_COLNAME);
            jrs.addRowSet(crs);
            jrs.addRowSet(crs1);
            validateResults(jrs);
            crs.close();
            crs1.close();
        }
    }

    /*
     * Join two CachedRowSets specifying a column index to join against
     */
    @Test(dataProvider = "createCachedRowSetsToUse")
    public void joinRowSetTests0006(CachedRowSet crs, CachedRowSet crs1)
            throws Exception {

        try (JoinRowSet jrs = newInstance()) {
            crs.setMatchColumn(COFFEES_JOIN_COLUMN_INDEX);
            crs1.setMatchColumn(SUPPLIERS_JOIN_COLUMN_INDEX);

            jrs.addRowSet(crs);
            jrs.addRowSet(crs1);
            validateResults(jrs);
            crs.close();
            crs1.close();
        }
    }

    // Disabled tests due to bugs in JoinRowSet
    @Test(dataProvider = "rowSetType", enabled = false)
    public void commonCachedRowSetTest0004(CachedRowSet rs) throws Exception {
    }

    @Test(dataProvider = "rowSetType", enabled = false)
    public void commonCachedRowSetTest0005(CachedRowSet rs) throws Exception {
    }

    @Test(dataProvider = "rowSetType", enabled = false)
    public void commonCachedRowSetTest0008(CachedRowSet rs) throws Exception {
    }

    @Test(dataProvider = "rowSetType", enabled = false)
    public void commonCachedRowSetTest0026(CachedRowSet rs) throws Exception {
    }

    @Test(dataProvider = "rowSetType", enabled = false)
    public void commonCachedRowSetTest0027(CachedRowSet rs) throws Exception {
    }

    @Test(dataProvider = "rowSetType", enabled = false)
    public void commonCachedRowSetTest0053(CachedRowSet rs) throws Exception {
    }

    @Test(dataProvider = "rowSetType", enabled = false)
    public void commonCachedRowSetTest0054(CachedRowSet rs) throws Exception {
    }

    @Test(dataProvider = "rowSetType", enabled = false)
    public void commonCachedRowSetTest0055(CachedRowSet rs) throws Exception {
    }

    @Test(dataProvider = "rowSetType")
    public void WebRowSetTest0009(WebRowSet wrs1) throws Exception {
    }
}
