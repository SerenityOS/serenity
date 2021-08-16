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
package test.rowset;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.RowSet;
import javax.sql.rowset.BaseRowSet;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import org.testng.Assert;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import util.BaseTest;
import util.StubBlob;
import util.StubClob;
import util.StubNClob;
import util.StubSQLXML;

public abstract class CommonRowSetTests extends BaseTest {

    protected final String stubProvider = "util.StubSyncProvider";
    protected final String query = "SELECT * FROM SUPERHEROS";
    private final String url = "jdbc:derby://localhost:1527/myDB";
    private final String dsName = "jdbc/myDB";
    private final String user = "Bruce Wayne";
    private final String password = "The Dark Knight";
    protected final String COFFEE_HOUSES_TABLE = "COFFEE_HOUSES";
    protected final String COFFEES_TABLE = "COFFEES";
    protected final int COFFEE_HOUSES_ROWS = 14;
    protected final int COFFEES_ROWS = 5;
    protected final Object[] COFFEES_PRIMARY_KEYS = {1, 2, 3, 4, 5};
    protected final Object[] COFFEE_HOUSES_PRIMARY_KEYS = {
        10023, 33002, 10040, 32001, 10042, 10024, 10039, 10041,
        33005, 33010, 10035, 10037, 10034, 32004
    };

    /*
     * COFFEES_HOUSES Table column names
     */
    protected final String[] COFFEE_HOUSES_COLUMN_NAMES = {
        "STORE_ID", "CITY", "COFFEE", "MERCH", "TOTAL"
    };

    /*
     * COFFEES Table column names
     */
    protected final String[] COFFEES_COLUMN_NAMES = {
        "COF_ID", "COF_NAME", "SUP_ID", "PRICE", "SALES", "TOTAL"
    };

    protected RowSetFactory rsf;

    public CommonRowSetTests() {
        try {
            rsf = RowSetProvider.newFactory();
        } catch (SQLException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    // Create an instance of the RowSet we are using
    protected abstract <T extends RowSet> T newInstance() throws SQLException;

    //DataProvider to use for common tests

    /*
     * DataProvider used to specify the value to set and check for the
     * methods for fetch direction
     */
    @DataProvider(name = "rowSetFetchDirection")
    protected Object[][] rowSetFetchDirection() throws Exception {
        RowSet rs = newInstance();
        return new Object[][]{
            {rs, ResultSet.FETCH_FORWARD},
            {rs, ResultSet.FETCH_REVERSE},
            {rs, ResultSet.FETCH_UNKNOWN}
        };
    }

    /*
     * DataProvider used to specify the value to set and check for the
     * methods for Cursor Scroll Type
     */
    @DataProvider(name = "rowSetScrollTypes")
    protected Object[][] rowSetScrollTypes() throws Exception {
        RowSet rs = newInstance();

        return new Object[][]{
            {rs, ResultSet.TYPE_FORWARD_ONLY},
            {rs, ResultSet.TYPE_SCROLL_INSENSITIVE},
            {rs, ResultSet.TYPE_SCROLL_SENSITIVE}
        };
    }

    /*
     * DataProvider used to specify the value to set and check for
     * methods using transaction isolation types
     */
    @DataProvider(name = "rowSetIsolationTypes")
    protected Object[][] rowSetIsolationTypes() throws Exception {
        RowSet rs = newInstance();

        return new Object[][]{
            {rs, Connection.TRANSACTION_NONE},
            {rs, Connection.TRANSACTION_READ_COMMITTED},
            {rs, Connection.TRANSACTION_READ_UNCOMMITTED},
            {rs, Connection.TRANSACTION_REPEATABLE_READ},
            {rs, Connection.TRANSACTION_SERIALIZABLE}
        };
    }

    /*
     * DataProvider used to specify the value to set and check for the
     * methods for Concurrency
     */
    @DataProvider(name = "rowSetConcurrencyTypes")
    protected Object[][] rowSetConcurrencyTypes() throws Exception {
        RowSet rs = newInstance();
        return new Object[][]{
            {rs, ResultSet.CONCUR_READ_ONLY},
            {rs, ResultSet.CONCUR_UPDATABLE}
        };
    }

    /*
     * DataProvider used to specify the value to set and check for
     * methods using boolean values
     */
    @DataProvider(name = "rowSetTrueFalse")
    protected Object[][] rowSetTrueFalse() throws Exception {
        RowSet rs = newInstance();
        return new Object[][]{
            {rs, true},
            {rs, false}
        };
    }
    /*
     * DataProvider used to specify the type of RowSet to use.  We also must
     * initialize the RowSet
     */
    @DataProvider(name = "rowSetType")
    protected Object[][] rowSetType() throws Exception {

        RowSet rs = newInstance();
        return new Object[][]{
            {rs}
        };
    }

    /*
     * Initializes a RowSet containing the COFFEE_HOUSES data
     */
    protected <T extends RowSet> T createCoffeeHousesRowSet() throws SQLException {
        T rs = (T) newInstance();
        initCoffeeHousesMetaData((CachedRowSet) rs);
        createCoffeeHouseRows(rs);
        // Make sure you are not on the insertRow
        rs.moveToCurrentRow();
        return rs;
    }

    /*
     * Initializes a RowSet containing the COFFEE_HOUSES data
     */
    protected <T extends RowSet> T createCoffeesRowSet() throws SQLException {
        T rs = (T) newInstance();
        initCoffeesMetaData((CachedRowSet) rs);
        createCoffeesRows(rs);
        // Make sure you are not on the insertRow
        rs.moveToCurrentRow();
        return rs;
    }

    /*
     * Initializes the COFFEE_HOUSES metadata
     */
    private void initCoffeeHousesMetaData(CachedRowSet crs) throws SQLException {
        RowSetMetaDataImpl rsmd = new RowSetMetaDataImpl();
        crs.setType(RowSet.TYPE_SCROLL_INSENSITIVE);

        /*
         *  CREATE TABLE COFFEE_HOUSES(
         *   STORE_ID Integer NOT NULL,
         *   CITY VARCHAR(32),
         *   COFFEE INTEGER NOT NULL,
         *   MERCH INTEGER NOT NULL,
         *   TOTAL INTEGER NOT NULL,
         *   PRIMARY KEY (STORE_ID))
         */
        rsmd.setColumnCount(COFFEE_HOUSES_COLUMN_NAMES.length);
        for(int i = 1; i <= COFFEE_HOUSES_COLUMN_NAMES.length; i++){
            rsmd.setColumnName(i, COFFEE_HOUSES_COLUMN_NAMES[i-1]);
            rsmd.setColumnLabel(i, rsmd.getColumnName(i));
        }

        rsmd.setColumnType(1, Types.INTEGER);
        rsmd.setColumnType(2, Types.VARCHAR);
        rsmd.setColumnType(3, Types.INTEGER);
        rsmd.setColumnType(4, Types.INTEGER);
        rsmd.setColumnType(5, Types.INTEGER);
        crs.setMetaData(rsmd);
        crs.setTableName(COFFEE_HOUSES_TABLE);

    }

    /*
     * Add rows to COFFEE_HOUSES table
     */
    protected void createCoffeeHouseRows(RowSet rs) throws SQLException {

        // insert into COFFEE_HOUSES values(10023, 'Mendocino', 3450, 2005, 5455)
        rs.moveToInsertRow();
        rs.updateInt(1, 10023);
        rs.updateString(2, "Mendocino");
        rs.updateInt(3, 3450);
        rs.updateInt(4, 2005);
        rs.updateInt(5, 5455);
        rs.insertRow();
        // insert into COFFEE_HOUSES values(33002, 'Seattle', 4699, 3109, 7808)
        rs.moveToInsertRow();
        rs.updateInt(1, 33002);
        rs.updateString(2, "Seattle");
        rs.updateInt(3, 4699);
        rs.updateInt(4, 3109);
        rs.updateInt(5, 7808);
        rs.insertRow();
        // insert into COFFEE_HOUSES values(10040, 'SF', 5386, 2841, 8227)
        rs.moveToInsertRow();
        rs.updateInt(1, 10040);
        rs.updateString(2, "SF");
        rs.updateInt(3, 5386);
        rs.updateInt(4, 2841);
        rs.updateInt(5, 8227);
        rs.insertRow();
        // insert into COFFEE_HOUSES values(32001, 'Portland', 3147, 3579, 6726)
        rs.moveToInsertRow();
        rs.updateInt(1, 32001);
        rs.updateString(2, "Portland");
        rs.updateInt(3, 3147);
        rs.updateInt(4, 3579);
        rs.updateInt(5, 6726);
        rs.insertRow();
        // insert into COFFEE_HOUSES values(10042, 'SF', 2863, 1874, 4710)
        rs.moveToInsertRow();
        rs.updateInt(1, 10042);
        rs.updateString(2, "SF");
        rs.updateInt(3, 2863);
        rs.updateInt(4, 1874);
        rs.updateInt(5, 4710);
        rs.insertRow();
        // insert into COFFEE_HOUSES values(10024, 'Sacramento', 1987, 2341, 4328)
        rs.moveToInsertRow();
        rs.updateInt(1, 10024);
        rs.updateString(2, "Sacramento");
        rs.updateInt(3, 1987);
        rs.updateInt(4, 2341);
        rs.updateInt(5, 4328);
        rs.insertRow();
        // insert into COFFEE_HOUSES values(10039, 'Carmel', 2691, 1121, 3812)
        rs.moveToInsertRow();
        rs.updateInt(1, 10039);
        rs.updateString(2, "Carmel");
        rs.updateInt(3, 2691);
        rs.updateInt(4, 1121);
        rs.updateInt(5, 3812);
        rs.insertRow();
        // insert into COFFEE_HOUSES values(10041, 'LA', 1533, 1007, 2540)
        rs.moveToInsertRow();
        rs.updateInt(1, 10041);
        rs.updateString(2, "LA");
        rs.updateInt(3, 1533);
        rs.updateInt(4, 1007);
        rs.updateInt(5, 2540);
        rs.insertRow();
        // insert into COFFEE_HOUSES values(33005, 'Olympia', 2733, 1550, 1550)
        rs.moveToInsertRow();
        rs.updateInt(1, 33005);
        rs.updateString(2, "Olympia");
        rs.updateInt(3, 2733);
        rs.updateInt(4, 1550);
        rs.updateInt(5, 1550);
        rs.insertRow();
        // insert into COFFEE_HOUSES values(33010, 'Seattle', 3210, 2177, 5387)
        rs.moveToInsertRow();
        rs.updateInt(1, 33010);
        rs.updateString(2, "Seattle");
        rs.updateInt(3, 3210);
        rs.updateInt(4, 2177);
        rs.updateInt(5, 5387);
        rs.insertRow();
        // insert into COFFEE_HOUSES values(10035, 'SF', 1922, 1056, 2978)
        rs.moveToInsertRow();
        rs.updateInt(1, 10035);
        rs.updateString(2, "SF");
        rs.updateInt(3, 1922);
        rs.updateInt(4, 1056);
        rs.updateInt(5, 2978);
        rs.insertRow();
        // insert into COFFEE_HOUSES values(10037, 'LA', 2143, 1876, 4019)
        rs.moveToInsertRow();
        rs.updateInt(1, 10037);
        rs.updateString(2, "LA");
        rs.updateInt(3, 2143);
        rs.updateInt(4, 1876);
        rs.updateInt(5, 4019);
        rs.insertRow();
        // insert into COFFEE_HOUSES values(10034, 'San_Jose', 1234, 1032, 2266)
        rs.moveToInsertRow();
        rs.updateInt(1, 10034);
        rs.updateString(2, "San Jose");
        rs.updateInt(3, 1234);
        rs.updateInt(4, 1032);
        rs.updateInt(5, 2266);
        rs.insertRow();
        // insert into COFFEE_HOUSES values(32004, 'Eugene', 1356, 1112, 2468)
        rs.moveToInsertRow();
        rs.updateInt(1, 32004);
        rs.updateString(2, "Eugene");
        rs.updateInt(3, 1356);
        rs.updateInt(4, 1112);
        rs.updateInt(5, 2468);
        rs.insertRow();
        rs.moveToCurrentRow();
    }

    /*
     * Initializes the COFFEES metadata
     */
    protected void initCoffeesMetaData(CachedRowSet crs) throws SQLException {
        RowSetMetaDataImpl rsmd = new RowSetMetaDataImpl();
        crs.setType(RowSet.TYPE_SCROLL_INSENSITIVE);

        /*
         *  CREATE TABLE COFFEES (
         *   COF_ID INTEGER NOT NULL,
         *   COF_NAME VARCHAR(32) NOT NULL,
         *   SUP_ID INTEGER NOT NULL,
         *   PRICE NUMBERIC(10,2 NOT NULL,
         *   SALES INTEGER NOT NULL,
         *   TOTAL INTEGER NOT NULL,
         *   PRIMARY KEY (COF_ID),
         *   FOREIGN KEY (SUP_ID) REFERENCES SUPPLIERS (SUP_ID) )
         */
        rsmd.setColumnCount(COFFEES_COLUMN_NAMES.length);
        for(int i = 1; i <= COFFEES_COLUMN_NAMES.length; i++){
            rsmd.setColumnName(i, COFFEES_COLUMN_NAMES[i-1]);
            rsmd.setColumnLabel(i, rsmd.getColumnName(i));
        }

        rsmd.setColumnType(1, Types.INTEGER);
        rsmd.setColumnType(2, Types.VARCHAR);
        rsmd.setColumnType(3, Types.INTEGER);
        rsmd.setColumnType(4, Types.NUMERIC);
        rsmd.setPrecision(4, 10);
        rsmd.setScale(4, 2);
        rsmd.setColumnType(5, Types.INTEGER);
        rsmd.setColumnType(6, Types.INTEGER);
        crs.setMetaData(rsmd);
        crs.setTableName(COFFEES_TABLE);

    }

    /*
     * Add rows to COFFEES table
     */
    protected void createCoffeesRows(RowSet rs) throws SQLException {

        // insert into COFFEES values(1, 'Colombian', 101, 7.99, 0, 0)
        rs.moveToInsertRow();
        rs.updateInt(1, 1);
        rs.updateString(2, "Colombian");
        rs.updateInt(3, 101);
        rs.updateBigDecimal(4, BigDecimal.valueOf(7.99));
        rs.updateInt(5, 0);
        rs.updateInt(6, 0);
        rs.insertRow();
        // insert into COFFEES values(2, 'French_Roast', 49, 8.99, 0, 0)
        rs.moveToInsertRow();
        rs.updateInt(1, 2);
        rs.updateString(2, "French_Roast");
        rs.updateInt(3, 49);
        rs.updateBigDecimal(4, BigDecimal.valueOf(8.99));
        rs.updateInt(5, 0);
        rs.updateInt(6, 0);
        rs.insertRow();
        // insert into COFFEES values(3, 'Espresso', 150, 9.99, 0, 0)
        rs.moveToInsertRow();
        rs.updateInt(1, 3);
        rs.updateString(2, "Espresso");
        rs.updateInt(3, 150);
        rs.updateBigDecimal(4, BigDecimal.valueOf(9.99));
        rs.updateInt(5, 0);
        rs.updateInt(6, 0);
        rs.insertRow();
        // insert into COFFEES values(4, 'Colombian_Decaf', 101, 8.99, 0, 0)
        rs.moveToInsertRow();
        rs.updateInt(1, 4);
        rs.updateString(2, "Colombian_Decaf");
        rs.updateInt(3, 101);
        rs.updateBigDecimal(4, BigDecimal.valueOf(8.99));
        rs.updateInt(5, 0);
        rs.updateInt(6, 0);
        rs.insertRow();
        // insert into COFFEES values(5, 'French_Roast_Decaf', 049, 9.99, 0, 0)
        rs.moveToInsertRow();
        rs.updateInt(1, 5);
        rs.updateString(2, "French_Roast_Decaf");
        rs.updateInt(3, 49);
        rs.updateBigDecimal(4, BigDecimal.valueOf(9.99));
        rs.updateInt(5, 0);
        rs.updateInt(6, 0);
        rs.insertRow();

    }


    /*
     * Utility method to return the Primary Keys for a RowSet.  The Primary
     * keys are assumed to be in the first column of the RowSet
     */
    protected Object[] getPrimaryKeys(ResultSet rs) throws SQLException {
        List<? super Object> result = new ArrayList<>();
        if (rs == null) {
            return null;
        }
        rs.beforeFirst();
        while (rs.next()) {
            result.add(rs.getInt(1));
        }
        return result.toArray();
    }

    /*
     * Utility method to display the RowSet and will return the row count
     * it found
     */
    protected int displayResults(ResultSet rs) throws SQLException {
        int rows = 0;
        ResultSetMetaData rsmd = rs.getMetaData();
        int cols = rsmd.getColumnCount();
        if (rs != null) {
            rs.beforeFirst();
            while (rs.next()) {
                rows++;

                for (int i = 0; i < cols; i++) {
                    System.out.print(rs.getString(i + 1) + " ");
                }
                System.out.println();
            }
        }

        return rows;
    }


     // Insert common tests here

    /*
     * Validate that getCommand() returns null by default
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0000(RowSet rs) {
        assertNull(rs.getCommand());
    }

    /*
     * Validate that getCommand() returns command specified to setCommand
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0001(RowSet rs) throws Exception {
        rs.setCommand(query);
        assertTrue(rs.getCommand().equals(query));
    }


    /*
     * Validate that getCurrency() returns the correct default value
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0002(RowSet rs) throws Exception {
        assertTrue(rs.getConcurrency() == ResultSet.CONCUR_UPDATABLE);
    }

    /*
     * Validate that getCurrency() returns the correct value
     * after a call to setConcurrency())
     */
    @Test(dataProvider = "rowSetConcurrencyTypes")
    public void commonRowSetTest0003(RowSet rs, int concurType) throws Exception {
        rs.setConcurrency(concurType);
        assertTrue(rs.getConcurrency() == concurType);
    }

    /*
     * Validate that getCurrency() throws a SQLException for an invalid value
     */
    @Test(dataProvider = "rowSetType", expectedExceptions = SQLException.class)
    public void commonRowSetTest0004(RowSet rs) throws Exception {
        rs.setConcurrency(ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    /*
     * Validate that getDataSourceName() returns null by default
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0005(RowSet rs) throws Exception {
        assertTrue(rs.getDataSourceName() == null);
    }

    /*
     * Validate that getDataSourceName() returns the value specified
     * by setDataSourceName() and getUrl() returns null
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0006(RowSet rs) throws Exception {
        rs.setUrl(url);
        rs.setDataSourceName(dsName);
        assertTrue(rs.getDataSourceName().equals(dsName));
        assertNull(rs.getUrl());
    }

    /*
     * Validate that setDataSourceName() throws a SQLException for an empty
     * String specified for the data source name
     */
    @Test(dataProvider = "rowSetType", expectedExceptions = SQLException.class)
    public void commonRowSetTest0007(RowSet rs) throws Exception {
        String dsname = "";
        rs.setDataSourceName(dsname);
    }

    /*
     * Validate that getEscapeProcessing() returns false by default
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0008(RowSet rs) throws Exception {
        assertTrue(rs.getEscapeProcessing());
    }

    /*
     * Validate that getEscapeProcessing() returns value set by
     * setEscapeProcessing()
     */
    @Test(dataProvider = "rowSetTrueFalse")
    public void commonRowSetTest0009(RowSet rs, boolean val) throws Exception {
        rs.setEscapeProcessing(val);
        assertTrue(rs.getEscapeProcessing() == val);
    }

    /*
     * Validate that getFetchDirection() returns the correct default value
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0010(RowSet rs) throws Exception {
        assertTrue(rs.getFetchDirection() == ResultSet.FETCH_FORWARD);
    }

    /*
     * Validate that getFetchDirection() returns the value set by
     * setFetchDirection()
     */
    @Test(dataProvider = "rowSetFetchDirection")
    public void commonRowSetTest0011(RowSet rs, int direction) throws Exception {
        rs.setFetchDirection(direction);
        assertTrue(rs.getFetchDirection() == direction);
    }

    /*
     * Validate that setFetchSize() throws a SQLException for an invalid value
     */
    @Test(dataProvider = "rowSetType", expectedExceptions = SQLException.class)
    public void commonRowSetTest0013(RowSet rs) throws Exception {
        rs.setFetchSize(-1);
    }

    /*
     * Validate that setFetchSize() throws a SQLException for a
     * value greater than getMaxRows()
     */
    @Test(dataProvider = "rowSetType", expectedExceptions = SQLException.class)
    public void commonRowSetTest0014(RowSet rs) throws Exception {
        rs.setMaxRows(5);
        rs.setFetchSize(rs.getMaxRows() + 1);
    }

    /*
     * Validate that getFetchSize() returns the correct value after
     * setFetchSize() has been called
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0015(RowSet rs) throws Exception {
        int maxRows = 150;
        rs.setFetchSize(0);
        assertTrue(rs.getFetchSize() == 0);
        rs.setFetchSize(100);
        assertTrue(rs.getFetchSize() == 100);
        rs.setMaxRows(maxRows);
        rs.setFetchSize(maxRows);
        assertTrue(rs.getFetchSize() == maxRows);
    }

    /*
     * Validate that setMaxFieldSize() throws a SQLException for an invalid value
     */
    @Test(dataProvider = "rowSetType", expectedExceptions = SQLException.class)
    public void commonRowSetTest0016(RowSet rs) throws Exception {
        rs.setMaxFieldSize(-1);
    }

    /*
     * Validate that getMaxFieldSize() returns the value set by
     * setMaxFieldSize()
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0017(RowSet rs) throws Exception {
        rs.setMaxFieldSize(0);
        assertTrue(rs.getMaxFieldSize() == 0);
        rs.setMaxFieldSize(100);
        assertTrue(rs.getMaxFieldSize() == 100);
        rs.setMaxFieldSize(50);
        assertTrue(rs.getMaxFieldSize() == 50);
    }

    /*
     * Validate that isReadOnly() returns value set by
     * setReadOnly()
     */
    @Test(dataProvider = "rowSetTrueFalse")
    public void commonRowSetTest0018(RowSet rs, boolean val) throws Exception {
        rs.setReadOnly(val);
        assertTrue(rs.isReadOnly() == val);
    }

    /*
     * Validate that getTransactionIsolation() returns value set by
     * setTransactionIsolation()
     */
    @Test(dataProvider = "rowSetIsolationTypes")
    public void commonRowSetTest0019(RowSet rs, int val) throws Exception {
        rs.setTransactionIsolation(val);
        assertTrue(rs.getTransactionIsolation() == val);
    }

    /*
     * Validate that getType() returns value set by setType()
     */
    @Test(dataProvider = "rowSetScrollTypes")
    public void commonRowSetTest0020(RowSet rs, int val) throws Exception {
        rs.setType(val);
        assertTrue(rs.getType() == val);
    }

    /*
     * Validate that getEscapeProcessing() returns value set by
     * setEscapeProcessing()
     */
    @Test(dataProvider = "rowSetTrueFalse")
    public void commonRowSetTest0021(BaseRowSet rs, boolean val) throws Exception {
        rs.setShowDeleted(val);
        assertTrue(rs.getShowDeleted() == val);
    }

    /*
     * Validate that getTypeMap() returns same value set by
     * setTypeMap()
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0022(RowSet rs) throws Exception {
        Map<String, Class<?>> map = new HashMap<>();
        map.put("SUPERHERO", Class.forName("util.SuperHero"));
        rs.setTypeMap(map);
        assertTrue(rs.getTypeMap().equals(map));
    }

    /*
     * Validate that getUsername() returns same value set by
     * setUsername()
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0023(RowSet rs) throws Exception {
        rs.setUsername(user);
        assertTrue(rs.getUsername().equals(user));
    }

    /*
     * Validate that getPassword() returns same password set by
     * setPassword()
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0024(RowSet rs) throws Exception {
        rs.setPassword(password);
        assertTrue(rs.getPassword().equals(password));
    }

    /*
     * Validate that getQueryTimeout() returns same value set by
     * setQueryTimeout() and that 0 is a valid timeout value
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0025(RowSet rs) throws Exception {
        int timeout = 0;
        rs.setQueryTimeout(timeout);
        assertTrue(rs.getQueryTimeout() == timeout);
    }

    /*
     * Validate that getQueryTimeout() returns same value set by
     * setQueryTimeout() and that 0 is a valid timeout value
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0026(RowSet rs) throws Exception {
        int timeout = 10000;
        rs.setQueryTimeout(timeout);
        assertTrue(rs.getQueryTimeout() == timeout);
    }

    /*
     * Validate that setQueryTimeout() throws a SQLException for a timeout
     * value < 0
     */
    @Test(dataProvider = "rowSetType", expectedExceptions = SQLException.class)
    public void commonRowSetTest0027(RowSet rs) throws Exception {
        rs.setQueryTimeout(-1);
    }


    /*
     * Validate addRowSetListener does not throw an Exception when null is
     * passed as the parameter
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0028(RowSet rs) throws Exception {
        rs.addRowSetListener(null);
    }

    /*
     * Validate removeRowSetListener does not throw an Exception when null is
     * passed as the parameter
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0029(RowSet rs) throws Exception {
        rs.removeRowSetListener(null);
    }

    /*
     * Set two parameters and then validate clearParameters() will clear them
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0030(BaseRowSet rs) throws Exception {
        rs.setInt(1, 1);
        rs.setString(2, query);
        assertTrue(rs.getParams().length == 2);
        rs.clearParameters();
        assertTrue(rs.getParams().length == 0);
    }

    /*
     * Validate that getURL() returns same value set by
     * setURL()
     */
    @Test(dataProvider = "rowSetType")
    public void commonRowSetTest0031(RowSet rs) throws Exception {
        rs.setUrl(url);
        assertTrue(rs.getUrl().equals(url));
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0100(RowSet rs) throws Exception {
        InputStream is = null;
        rs.setAsciiStream(1, is);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0101(RowSet rs) throws Exception {
        InputStream is = null;
        rs.setAsciiStream("one", is);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0102(RowSet rs) throws Exception {
        InputStream is = null;
        rs.setAsciiStream("one", is, query.length());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0103(RowSet rs) throws Exception {
        InputStream is = null;
        rs.setBinaryStream(1, is);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0104(RowSet rs) throws Exception {
        InputStream is = null;
        rs.setBinaryStream("one", is);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0105(RowSet rs) throws Exception {
        InputStream is = null;
        rs.setBinaryStream("one", is, query.length());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0106(RowSet rs) throws Exception {
        rs.setBigDecimal("one", BigDecimal.ONE);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0107(RowSet rs) throws Exception {
        InputStream is = null;
        rs.setBlob(1, is);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0108(RowSet rs) throws Exception {
        InputStream is = null;
        rs.setBlob("one", is);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0109(RowSet rs) throws Exception {
        InputStream is = null;
        rs.setBlob("one", is, query.length());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0110(RowSet rs) throws Exception {
        rs.setBlob("one", new StubBlob());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0111(RowSet rs) throws Exception {
        rs.setBoolean("one", true);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0112(RowSet rs) throws Exception {
        byte b = 1;
        rs.setByte("one", b);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0113(RowSet rs) throws Exception {
        byte b = 1;
        rs.setBytes("one", new byte[10]);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0114(RowSet rs) throws Exception {
        Reader rdr = null;
        rs.setCharacterStream("one", rdr, query.length());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0115(RowSet rs) throws Exception {
        Reader rdr = null;
        rs.setCharacterStream("one", rdr);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0116(RowSet rs) throws Exception {
        Reader rdr = null;
        rs.setCharacterStream(1, rdr);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0117(RowSet rs) throws Exception {
        Reader rdr = null;
        rs.setClob(1, rdr);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0118(RowSet rs) throws Exception {
        Reader rdr = null;
        rs.setClob("one", rdr);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0119(RowSet rs) throws Exception {
        Reader rdr = null;
        rs.setClob("one", rdr, query.length());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0120(RowSet rs) throws Exception {
        rs.setClob("one", new StubClob());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0121(RowSet rs) throws Exception {
        rs.setDate("one", Date.valueOf(LocalDate.now()));
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0122(RowSet rs) throws Exception {
        rs.setDate("one", Date.valueOf(LocalDate.now()),
                Calendar.getInstance());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0123(RowSet rs) throws Exception {
        rs.setTime("one", Time.valueOf(LocalTime.now()));
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0124(RowSet rs) throws Exception {
        rs.setTime("one", Time.valueOf(LocalTime.now()),
                Calendar.getInstance());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0125(RowSet rs) throws Exception {
        rs.setTimestamp("one", Timestamp.valueOf(LocalDateTime.now()));
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0126(RowSet rs) throws Exception {
        rs.setTimestamp("one", Timestamp.valueOf(LocalDateTime.now()),
                Calendar.getInstance());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0127(RowSet rs) throws Exception {
        rs.setDouble("one", 2.0d);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0128(RowSet rs) throws Exception {
        rs.setFloat("one", 2.0f);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0129(RowSet rs) throws Exception {
        rs.setInt("one", 21);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0130(RowSet rs) throws Exception {
        rs.setLong("one", 21l);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0131(RowSet rs) throws Exception {
        Reader rdr = null;
        rs.setNCharacterStream("one", rdr, query.length());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0132(RowSet rs) throws Exception {
        Reader rdr = null;
        rs.setNCharacterStream("one", rdr);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0133(RowSet rs) throws Exception {
        Reader rdr = null;
        rs.setNCharacterStream(1, rdr);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0134(RowSet rs) throws Exception {
        Reader rdr = null;
        rs.setNCharacterStream(1, rdr, query.length());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0135(RowSet rs) throws Exception {
        Reader rdr = null;
        rs.setClob("one", rdr);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0136(RowSet rs) throws Exception {
        Reader rdr = null;
        rs.setClob("one", rdr, query.length());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0137(RowSet rs) throws Exception {
        rs.setNClob("one", new StubNClob());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0138(RowSet rs) throws Exception {
        Reader rdr = null;
        rs.setNClob(1, rdr);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0139(RowSet rs) throws Exception {
        Reader rdr = null;
        rs.setNClob(1, rdr, query.length());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0140(RowSet rs) throws Exception {
        rs.setNClob(1, new StubNClob());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0141(RowSet rs) throws Exception {
        rs.setNString(1, query);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0142(RowSet rs) throws Exception {
        rs.setNull("one", Types.INTEGER);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0143(RowSet rs) throws Exception {
        rs.setNull("one", Types.INTEGER, "my.type");
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0144(RowSet rs) throws Exception {
        rs.setObject("one", query, Types.VARCHAR);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0145(RowSet rs) throws Exception {
        rs.setObject("one", query, Types.VARCHAR, 0);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0146(RowSet rs) throws Exception {
        rs.setObject("one", query);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0147(RowSet rs) throws Exception {
        RowId aRowid = null;
        rs.setRowId("one", aRowid);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0148(RowSet rs) throws Exception {
        rs.setSQLXML("one", new StubSQLXML());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0149(RowSet rs) throws Exception {
        rs.setSQLXML(1, new StubSQLXML());
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0150(RowSet rs) throws Exception {
        rs.setNString(1, query);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0151(RowSet rs) throws Exception {
        rs.setNString("one", query);
    }

    /*
     * This method is currently not implemented in BaseRowSet and will
     * throw a SQLFeatureNotSupportedException
     */
    @Test(dataProvider = "rowSetType",
            expectedExceptions = SQLFeatureNotSupportedException.class)
    public void commonRowSetTest0152(RowSet rs) throws Exception {
        short val = 21;
        rs.setShort("one", val);
    }

}
