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
package test.rowset.cachedrowset;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Date;
import java.sql.JDBCType;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import javax.sql.RowSet;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.serial.SerialRef;
import javax.sql.rowset.spi.SyncFactory;
import javax.sql.rowset.spi.SyncProvider;
import javax.sql.rowset.spi.SyncProviderException;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import test.rowset.CommonRowSetTests;
import util.StubArray;
import util.StubRef;
import util.StubSyncProvider;
import util.TestRowSetListener;

public abstract class CommonCachedRowSetTests extends CommonRowSetTests {

    /*
     * DATATYPES Table column names
     */
    private final String[] DATATYPES_COLUMN_NAMES = {"AINTEGER", "ACHAR",
        "AVARCHAR", "ALONG", "ABOOLEAN", "ASHORT", "ADOUBLE", "ABIGDECIMAL",
        "AREAL", "ABYTE", "ADATE", "ATIME", "ATIMESTAMP", "ABYTES", "ARRAY",
        "AREF", "AFLOAT"};

    /*
     * Initializes a RowSet containing the DATAYPES data
     */
    protected <T extends RowSet> T createDataTypesRowSet() throws SQLException {
        T rs = (T) newInstance();
        initDataTypesMetaData((CachedRowSet) rs);
        createDataTypesRows(rs);
        // Make sure you are not on the insertRow
        rs.moveToCurrentRow();
        return rs;
    }

    //DataProviders to use for common tests

    /*
     * DataProvider that uses a RowSet with the COFFEE_HOUSES Table
     */
    @DataProvider(name = "rowsetUsingCoffeeHouses")
    protected Object[][] rowsetUsingCoffeeHouses() throws Exception {
        RowSet rs = createCoffeeHousesRowSet();
        return new Object[][]{
            {rs}
        };
    }

    /*
     * DataProvider that uses a RowSet with the COFFEES Table
     */
    @DataProvider(name = "rowsetUsingCoffees")
    protected Object[][] rowsetUsingCoffees() throws Exception {
        RowSet rs = createCoffeesRowSet();
        return new Object[][]{
            {rs}
        };
    }

    /*
     * DataProvider that uses a RowSet with the DATAYPES Table and
     * used to validate the various supported data types
     */
    @DataProvider(name = "rowsetUsingDataTypes")
    protected Object[][] rowsetUsingDataTypes() throws Exception {

        CachedRowSet rs = createDataTypesRowSet();
        return new Object[][]{
            {rs, JDBCType.INTEGER},
            {rs, JDBCType.CHAR},
            {rs, JDBCType.VARCHAR},
            {rs, JDBCType.BIGINT},
            {rs, JDBCType.BOOLEAN},
            {rs, JDBCType.SMALLINT},
            {rs, JDBCType.DOUBLE},
            {rs, JDBCType.DECIMAL},
            {rs, JDBCType.REAL},
            {rs, JDBCType.TINYINT},
            {rs, JDBCType.DATE},
            {rs, JDBCType.TIME},
            {rs, JDBCType.TIMESTAMP},
            {rs, JDBCType.VARBINARY},
            {rs, JDBCType.ARRAY},
            {rs, JDBCType.REF},
            {rs, JDBCType.FLOAT}
        };
    }

    /*
     * Initializes the DATAYPES table metadata
     */
    protected void initDataTypesMetaData(CachedRowSet crs) throws SQLException {
        RowSetMetaDataImpl rsmd = new RowSetMetaDataImpl();
        crs.setType(RowSet.TYPE_SCROLL_INSENSITIVE);

        rsmd.setColumnCount(DATATYPES_COLUMN_NAMES.length);

        for (int i = 1; i <= DATATYPES_COLUMN_NAMES.length; i++) {
            rsmd.setColumnName(i, DATATYPES_COLUMN_NAMES[i - 1]);
            rsmd.setColumnLabel(i, rsmd.getColumnName(i));
        }

        rsmd.setColumnType(1, Types.INTEGER);
        rsmd.setColumnType(2, Types.CHAR);
        rsmd.setColumnType(3, Types.VARCHAR);
        rsmd.setColumnType(4, Types.BIGINT);
        rsmd.setColumnType(5, Types.BOOLEAN);
        rsmd.setColumnType(6, Types.SMALLINT);
        rsmd.setColumnType(7, Types.DOUBLE);
        rsmd.setColumnType(8, Types.DECIMAL);
        rsmd.setColumnType(9, Types.REAL);
        rsmd.setColumnType(10, Types.TINYINT);
        rsmd.setColumnType(11, Types.DATE);
        rsmd.setColumnType(12, Types.TIME);
        rsmd.setColumnType(13, Types.TIMESTAMP);
        rsmd.setColumnType(14, Types.VARBINARY);
        rsmd.setColumnType(15, Types.ARRAY);
        rsmd.setColumnType(16, Types.REF);
        rsmd.setColumnType(17, Types.FLOAT);
        crs.setMetaData(rsmd);

    }

    /*
     * Add rows to DATAYPES table
     */
    protected void createDataTypesRows(RowSet crs) throws SQLException {

        Integer aInteger = 100;
        String aChar = "Oswald Cobblepot";
        Long aLong = Long.MAX_VALUE;
        Short aShort = Short.MAX_VALUE;
        Double aDouble = Double.MAX_VALUE;
        BigDecimal aBigDecimal = BigDecimal.ONE;
        Boolean aBoolean = false;
        Float aFloat = Float.MAX_VALUE;
        Byte aByte = Byte.MAX_VALUE;
        Date aDate = Date.valueOf(LocalDate.now());
        Time aTime = Time.valueOf(LocalTime.now());
        Timestamp aTimeStamp = Timestamp.valueOf(LocalDateTime.now());
        Array aArray = new StubArray("INTEGER", new Object[1]);
        Ref aRef = new SerialRef(new StubRef("INTEGER", query));
        byte[] bytes = new byte[10];
        crs.moveToInsertRow();
        crs.updateInt(1, aInteger);
        crs.updateString(2, aChar);
        crs.updateString(3, aChar);
        crs.updateLong(4, aLong);
        crs.updateBoolean(5, aBoolean);
        crs.updateShort(6, aShort);
        crs.updateDouble(7, aDouble);
        crs.updateBigDecimal(8, aBigDecimal);
        crs.updateFloat(9, aFloat);
        crs.updateByte(10, aByte);
        crs.updateDate(11, aDate);
        crs.updateTime(12, aTime);
        crs.updateTimestamp(13, aTimeStamp);
        crs.updateBytes(14, bytes);
        crs.updateArray(15, aArray);
        crs.updateRef(16, aRef);
        crs.updateDouble(17, aDouble);
        crs.insertRow();
        crs.moveToCurrentRow();

    }

    /*
     * Dermine if a Row exists in a ResultSet by its primary key
     * If the parameter deleteRow is true, delete the row and validate
     * the RowSet indicates it is deleted
     */
    protected boolean findRowByPrimaryKey(RowSet rs, int id, int idPos,
            boolean deleteRow) throws Exception {
        boolean foundRow = false;
        rs.beforeFirst();
        while (rs.next()) {
            if (rs.getInt(idPos) == id) {
                foundRow = true;
                if (deleteRow) {
                    rs.deleteRow();
                    // validate row is marked as deleted
                    assertTrue(rs.rowDeleted());
                }
                break;
            }
        }
        return foundRow;
    }

    /*
     * Wrapper method to find if a row exists within a RowSet by its primary key
     */
    protected boolean findRowByPrimaryKey(RowSet rs, int id, int idPos) throws Exception {
        return findRowByPrimaryKey(rs, id, idPos, false);
    }

    /*
     * Wrapper method to find if a row exists within a RowSet by its primary key
     * and delete it
     */
    protected boolean deleteRowByPrimaryKey(RowSet rs, int id, int idPos) throws Exception {
        return findRowByPrimaryKey(rs, id, idPos, true);
    }

    /*
     * Utility method that compares two ResultSetMetaDataImpls for containing
     * the same values
     */
    private void compareMetaData(ResultSetMetaData rsmd,
            ResultSetMetaData rsmd1) throws SQLException {

        assertEquals(rsmd1.getColumnCount(), rsmd.getColumnCount());
        int cols = rsmd.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            assertTrue(rsmd1.getCatalogName(i).equals(rsmd.getCatalogName(i)));
            assertTrue(rsmd1.getColumnClassName(i).equals(rsmd.getColumnClassName(i)));
            assertTrue(rsmd1.getColumnDisplaySize(i) == rsmd.getColumnDisplaySize(i));
            assertTrue(rsmd1.getColumnLabel(i).equals(rsmd.getColumnLabel(i)));
            assertTrue(rsmd1.getColumnName(i).equals(rsmd.getColumnName(i)));
            assertTrue(rsmd1.getColumnType(i) == rsmd.getColumnType(i));
            assertTrue(rsmd1.getPrecision(i) == rsmd.getPrecision(i));
            assertTrue(rsmd1.getScale(i) == rsmd.getScale(i));
            assertTrue(rsmd1.getSchemaName(i).equals(rsmd.getSchemaName(i)));
            assertTrue(rsmd1.getTableName(i).equals(rsmd.getTableName(i)));
            assertTrue(rsmd1.isAutoIncrement(i) == rsmd.isAutoIncrement(i));
            assertTrue(rsmd1.isCaseSensitive(i) == rsmd.isCaseSensitive(i));
            assertTrue(rsmd1.isCurrency(i) == rsmd.isCurrency(i));
            assertTrue(rsmd1.isDefinitelyWritable(i) == rsmd.isDefinitelyWritable(i));
            assertTrue(rsmd1.isNullable(i) == rsmd.isNullable(i));
            assertTrue(rsmd1.isReadOnly(i) == rsmd.isReadOnly(i));
            assertTrue(rsmd1.isSearchable(i) == rsmd.isSearchable(i));
            assertTrue(rsmd1.isSigned(i) == rsmd.isSigned(i));
            assertTrue(rsmd1.isWritable(i) == rsmd.isWritable(i));

        }
    }

    /*
     * Utility method to compare two rowsets
     */
    private void compareRowSets(CachedRowSet crs, CachedRowSet crs1) throws Exception {

        int rows = crs.size();
        assertTrue(rows == crs1.size());

        ResultSetMetaData rsmd = crs.getMetaData();

        compareMetaData(rsmd, crs1.getMetaData());
        int cols = rsmd.getColumnCount();

        for (int row = 1; row <= rows; row++) {
            crs.absolute((row));
            crs1.absolute(row);
            for (int col = 1; col <= cols; col++) {
                compareColumnValue(JDBCType.valueOf(rsmd.getColumnType(col)),
                        crs, crs1, col);
            }
        }

    }

    /*
     * Utility method to compare two columns
     */
    private void compareColumnValue(JDBCType type, ResultSet rs, ResultSet rs1,
            int col) throws SQLException {

        switch (type) {
            case INTEGER:
                assertTrue(rs.getInt(col) == rs1.getInt(col));
                break;
            case CHAR:
            case VARCHAR:
                assertTrue(rs.getString(col).equals(rs1.getString(col)));
                break;
            case BIGINT:
                assertTrue(rs.getLong(col) == rs1.getLong(col));
                break;
            case BOOLEAN:
                assertTrue(rs.getBoolean(col) == rs1.getBoolean(col));
                break;
            case SMALLINT:
                assertTrue(rs.getShort(col) == rs1.getShort(col));
                break;
            case DOUBLE:
            case FLOAT:
                assertTrue(rs.getDouble(col) == rs1.getDouble(col));
                break;
            case DECIMAL:
                assertTrue(rs.getBigDecimal(col).equals(rs1.getBigDecimal(col)));
                break;
            case REAL:
                assertTrue(rs.getFloat(col) == rs1.getFloat(col));
                break;
            case TINYINT:
                assertTrue(rs.getByte(col) == rs1.getByte(col));
                break;
            case DATE:
                assertTrue(rs.getDate(col).equals(rs1.getDate(col)));
                break;
            case TIME:
                assertTrue(rs.getTime(col).equals(rs1.getTime(col)));
                break;
            case TIMESTAMP:
                assertTrue(rs.getTimestamp(col).equals(rs1.getTimestamp(col)));
                break;
        }
    }

    /*
     * Validate SyncProviderException is thrown when acceptChanges is called
     * but there is not a way to make a connection to the datasource
     */
    @Test(dataProvider = "rowSetType", expectedExceptions = SyncProviderException.class)
    public void commonCachedRowSetTest0000(CachedRowSet rs) throws Exception {
        rs.acceptChanges();
        rs.close();
    }

    /*
     * Validate SyncProviderException is thrown when acceptChanges is called
     * when null is passed as the datasource
     */
    @Test(dataProvider = "rowSetType", expectedExceptions = SyncProviderException.class)
    public void commonCachedRowSetTest0001(CachedRowSet rs) throws Exception {
        rs.acceptChanges(null);
        rs.close();
    }

    /*
     * Validate that that RIOPtimsticProvider is the default SyncProvider
     */
    @Test(dataProvider = "rowSetType")
    public void commonCachedRowSetTest0002(CachedRowSet rs) throws SQLException {
        SyncProvider sp = rs.getSyncProvider();
        assertTrue(sp instanceof com.sun.rowset.providers.RIOptimisticProvider);
        rs.close();
    }

    /*
     * Validate that you can specify a SyncProvider
     */
    @Test(dataProvider = "rowSetType")
    public void commonCachedRowSetTest0003(CachedRowSet rs) throws SQLException {

        // Register a provider and make sure it is avaiable
        SyncFactory.registerProvider(stubProvider);
        rs.setSyncProvider(stubProvider);
        SyncProvider sp = rs.getSyncProvider();
        assertTrue(sp instanceof StubSyncProvider);
        SyncFactory.unregisterProvider(stubProvider);
        rs.close();
    }

    /*
     * Create a RowSetListener and validate that notifyRowSetChanged is called
     */
    @Test(dataProvider = "rowSetType")
    public void commonCachedRowSetTest0004(CachedRowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        rs.addRowSetListener(rsl);
        rs.release();
        assertTrue(rsl.isNotified(TestRowSetListener.ROWSET_CHANGED));
        rs.close();
    }

    /*
     * Create a RowSetListener and validate that notifyRowSetChanged is called
     */
    @Test(dataProvider = "rowSetType")
    public void commonCachedRowSetTest0005(CachedRowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        rs.addRowSetListener(rsl);
        rs.restoreOriginal();
        assertTrue(rsl.isNotified(TestRowSetListener.ROWSET_CHANGED));
        rs.close();
    }

    /*
     * Create a RowSetListener and validate that notifyRowChanged is called
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0006(RowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        rs.addRowSetListener(rsl);
        rs.moveToInsertRow();
        rs.updateInt(1, 10024);
        rs.updateString(2, "Sacramento");
        rs.updateInt(3, 1987);
        rs.updateInt(4, 2341);
        rs.updateInt(5, 4328);
        rs.insertRow();
        assertTrue(rsl.isNotified(TestRowSetListener.ROW_CHANGED));
        rs.close();
    }

    /*
     * Create a multiple RowSetListeners and validate that notifyRowChanged,
     * notifiyMoved is called on all listners
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0007(RowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        TestRowSetListener rsl2 = new TestRowSetListener();
        rs.addRowSetListener(rsl);
        rs.addRowSetListener(rsl2);
        rs.first();
        rs.updateInt(1, 1961);
        rs.updateString(2, "Pittsburgh");
        rs.updateInt(3, 1987);
        rs.updateInt(4, 2341);
        rs.updateInt(5, 6689);
        rs.updateRow();
        assertTrue(rsl.isNotified(TestRowSetListener.CURSOR_MOVED
                | TestRowSetListener.ROW_CHANGED));
        assertTrue(rsl2.isNotified(TestRowSetListener.CURSOR_MOVED
                | TestRowSetListener.ROW_CHANGED));
        rs.close();
    }

    /*
     * Create a RowSetListener and validate that notifyRowChanged  and
     * notifyCursorMoved are  called
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0008(CachedRowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        rs.addRowSetListener(rsl);

        rs.first();
        assertTrue(rsl.isNotified(TestRowSetListener.CURSOR_MOVED));
        rs.deleteRow();
        assertTrue(
                rsl.isNotified(TestRowSetListener.ROW_CHANGED | TestRowSetListener.CURSOR_MOVED));
        rsl.resetFlag();
        rs.setShowDeleted(true);
        rs.undoDelete();
        assertTrue(rsl.isNotified(TestRowSetListener.ROW_CHANGED));
        rs.close();
    }

    /*
     * Create a RowSetListener and validate that notifyCursorMoved is called
     */
    @Test(dataProvider = "rowSetType")
    public void commonCachedRowSetTest0009(RowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        rs.addRowSetListener(rsl);
        rs.beforeFirst();
        assertTrue(rsl.isNotified(TestRowSetListener.CURSOR_MOVED));
        rs.close();
    }

    /*
     * Validate that getTableName() returns the proper values
     */
    @Test(dataProvider = "rowSetType")
    public void commonCachedRowSetTest0010(CachedRowSet rs) throws Exception {
        assertNull(rs.getTableName());
        rs.setTableName(COFFEE_HOUSES_TABLE);
        assertTrue(rs.getTableName().equals(COFFEE_HOUSES_TABLE));
        rs.close();
    }

    /*
     * Validate that getKeyColumns() returns the proper values
     */
    @Test(dataProvider = "rowSetType")
    public void commonCachedRowSetTest0011(CachedRowSet rs) throws Exception {
        int[] pkeys = {1, 3};
        assertNull(rs.getKeyColumns());
        rs.setKeyColumns(pkeys);
        assertEquals(rs.getKeyColumns(), pkeys);
        rs.close();
    }

    /*
     * Validate that setMatchColumn throws a SQLException if the column
     * index specified is out of range
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0012(CachedRowSet rs) throws Exception {
        rs.setMatchColumn(-1);
        rs.close();
    }

    /*
     * Validate that setMatchColumn throws a SQLException if the column
     * index specified is out of range
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0013(CachedRowSet rs) throws Exception {
        int[] cols = {1, -1};
        rs.setMatchColumn(cols);
        rs.close();
    }

    /*
     * Validate that setMatchColumn throws a SQLException if the column
     * index specified is out of range
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0014(CachedRowSet rs) throws Exception {
        rs.setMatchColumn((String) null);
        rs.close();
    }

    /*
     * Validate that setMatchColumn throws a SQLException if the column
     * index specified is out of range
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0015(CachedRowSet rs) throws Exception {
        String[] cols = {"ID", null};
        rs.setMatchColumn(cols);
    }

    /*
     * Validate that getMatchColumn returns the same value specified by
     * setMatchColumn
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses", enabled = false)
    public void commonCachedRowSetTest0016(CachedRowSet rs) throws Exception {
        int[] expectedCols = {1};
        String[] expectedColNames = {"ID"};
        rs.setMatchColumn(1);
        int[] actualCols = rs.getMatchColumnIndexes();
        String[] actualColNames = rs.getMatchColumnNames();
        for (int i = 0; i < actualCols.length; i++) {
            System.out.println(actualCols[i]);
        }
        assertEquals(actualCols, expectedCols);
        assertEquals(actualColNames, expectedColNames);
        rs.close();
    }

    /*
     * Validate that getMatchColumn returns the same value specified by
     * setMatchColumn
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses", enabled = false)
    public void commonCachedRowSetTest0017(CachedRowSet rs) throws Exception {
        int[] expectedCols = {1};
        String[] expectedColNames = {"ID"};
        rs.setMatchColumn(expectedColNames[0]);
        int[] actualCols = rs.getMatchColumnIndexes();
        String[] actualColNames = rs.getMatchColumnNames();
        assertEquals(actualCols, expectedCols);
        assertEquals(actualColNames, expectedColNames);
        rs.close();
    }

    /*
     * Validate that getMatchColumn returns the same valid value specified by
     * setMatchColumn
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses", enabled = false)
    public void commonCachedRowSetTest0018(CachedRowSet rs) throws Exception {
        int[] expectedCols = {1, 3};
        String[] expectedColNames = {"COF_ID", "SUP_ID"};
        rs.setMatchColumn(expectedCols);
        int[] actualCols = rs.getMatchColumnIndexes();
        String[] actualColNames = rs.getMatchColumnNames();
        assertEquals(actualCols, expectedCols);
        assertEquals(actualColNames, expectedColNames);
        assertEquals(actualCols, expectedCols);
        rs.close();
    }

    /*
     * Validate that getMatchColumn returns the same valid value specified by
     * setMatchColumn
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses", enabled = false)
    public void commonCachedRowSetTest0019(CachedRowSet rs) throws Exception {
        int[] expectedCols = {1, 3};
        String[] expectedColNames = {"COF_ID", "SUP_ID"};
        rs.setMatchColumn(expectedColNames);
        int[] actualCols = rs.getMatchColumnIndexes();
        String[] actualColNames = rs.getMatchColumnNames();
        assertEquals(actualCols, expectedCols);
        assertEquals(actualColNames, expectedColNames);
        rs.close();
    }

    /*
     * Validate that getMatchColumnIndexes throws a SQLException if
     * unsetMatchColumn has been called
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0020(CachedRowSet rs) throws Exception {
        rs.setMatchColumn(1);
        int[] actualCols = rs.getMatchColumnIndexes();
        assertTrue(actualCols != null);
        rs.unsetMatchColumn(1);
        actualCols = rs.getMatchColumnIndexes();
        rs.close();
    }

    /*
     * Validate that getMatchColumnNames throws a SQLException if
     * unsetMatchColumn has been called
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0021(CachedRowSet rs) throws Exception {
        String matchColumn = "ID";
        rs.setMatchColumn(matchColumn);
        String[] actualColNames = rs.getMatchColumnNames();
        assertTrue(actualColNames != null);
        rs.unsetMatchColumn(matchColumn);
        actualColNames = rs.getMatchColumnNames();
        rs.close();
    }

    /*
     * Validate that getMatchColumnIndexes throws a SQLException if
     * unsetMatchColumn has been called
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0022(CachedRowSet rs) throws Exception {
        int[] expectedCols = {1, 3};
        rs.setMatchColumn(expectedCols);
        int[] actualCols = rs.getMatchColumnIndexes();
        assertTrue(actualCols != null);
        rs.unsetMatchColumn(expectedCols);
        actualCols = rs.getMatchColumnIndexes();
        rs.close();
    }

    /*
     * Validate that getMatchColumnNames throws a SQLException if
     * unsetMatchColumn has been called
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0023(CachedRowSet rs) throws Exception {
        String[] expectedColNames = {"COF_ID", "SUP_ID"};
        rs.setMatchColumn(expectedColNames);
        String[] actualColNames = rs.getMatchColumnNames();
        assertTrue(actualColNames != null);
        rs.unsetMatchColumn(expectedColNames);
        actualColNames = rs.getMatchColumnNames();
        rs.close();
    }

    /*
     * Validate size() returns the correct number of rows
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0024(CachedRowSet rs) throws Exception {
        assertTrue(rs.size() == COFFEE_HOUSES_ROWS);
        rs.close();
    }

    /*
     * Validate that the correct rows are returned comparing the primary
     * keys
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0025(RowSet rs) throws SQLException {
        assertEquals(getPrimaryKeys(rs), COFFEE_HOUSES_PRIMARY_KEYS);
        rs.close();
    }

    /*
     * Delete a row within the RowSet using its primary key
     * Validate the visibility of the row depending on the value of
     * setShowdelete
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0026(CachedRowSet rs) throws Exception {
        Object[] afterDelete = {
            10023, 33002, 10040, 32001, 10042, 10024, 10039, 10041,
            33005, 33010, 10037, 10034, 32004
        };
        int rowToDelete = 10035;
        // All rows should be found
        assertEquals(getPrimaryKeys(rs), COFFEE_HOUSES_PRIMARY_KEYS);
        // Delete the row
        assertTrue(deleteRowByPrimaryKey(rs, rowToDelete, 1));
        // With setShowDeleted(false) which is the default,
        // the deleted row should not be visible
        assertFalse(findRowByPrimaryKey(rs, rowToDelete, 1));
        assertEquals(getPrimaryKeys(rs), afterDelete);
        assertTrue(rs.size() == COFFEE_HOUSES_ROWS);
        // With setShowDeleted(true), the deleted row should be visible
        rs.setShowDeleted(true);
        assertTrue(findRowByPrimaryKey(rs, rowToDelete, 1));
        rs.close();
    }

    /*
     * Validate that there is no page size by default
     */
    @Test(dataProvider = "rowSetType")
    public void commonCachedRowSetTest0027(CachedRowSet rs) throws Exception {
        assertTrue(rs.getPageSize() == 0);
        rs.close();
    }

    /*
     * Validate the value you set via setPageSize is returned by getPageSize
     * then reset to having no limit
     */
    @Test(dataProvider = "rowSetType")
    public void commonCachedRowSetTest0028(CachedRowSet rs) throws Exception {
        int rows = 100;
        rs.setPageSize(rows);
        assertTrue(rows == rs.getPageSize());
        rs.setPageSize(0);
        assertTrue(rs.getPageSize() == 0);
        rs.close();
    }

    /*
     * Validate SQLException is thrown when an invalid value is specified
     * for setPageSize
     */
    @Test(dataProvider = "rowSetType", expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0029(CachedRowSet rs) throws Exception {
        rs.setPageSize(-1);
        rs.close();
    }

    /*
     * Validate SQLException is thrown when nextPage is called without a
     * call to populate or execute
     */
    @Test(dataProvider = "rowSetType", expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0030(CachedRowSet rs) throws Exception {
        rs.nextPage();
        rs.close();
    }

    /*
     * Validate SQLException is thrown when previousPage is called without a
     * call to populate or execute
     */
    @Test(dataProvider = "rowSetType", expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0031(CachedRowSet rs) throws Exception {
        rs.previousPage();
        rs.close();
    }


    /*
     * Validate SQLException is thrown when execute is called
     * but there is not a way to make a connection to the datasource
     */
    @Test(dataProvider = "rowSetType", expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0032(CachedRowSet rs) throws Exception {
        rs.execute(null);
        rs.close();
    }

    /*
     * Validate SQLException is thrown when execute is called
     * but there is not a way to make a connection to the datasource
     */
    @Test(dataProvider = "rowSetType", expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0033(CachedRowSet rs) throws Exception {
        rs.execute();
        rs.close();
    }

    /*
     * Validate that toCollection(<column>) returns the proper values
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0034(CachedRowSet rs) throws Exception {
        Object[] cities = {"Mendocino", "Seattle", "SF", "Portland", "SF",
            "Sacramento", "Carmel", "LA", "Olympia", "Seattle", "SF",
            "LA", "San Jose", "Eugene"};
        rs.beforeFirst();
        assertEquals(rs.toCollection(2).toArray(), cities);
        assertEquals(rs.toCollection("CITY").toArray(), cities);
        rs.close();
    }

    /*
     * Validate that toCollection() returns the proper values
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0035(CachedRowSet rs) throws Exception {
        Collection<?> col = rs.toCollection();
        assertTrue(rs.size() == col.size());
        assertTrue(rs.toCollection().containsAll(col)
                && col.containsAll(rs.toCollection()));
        try ( // Validate that False is returned when compared to a different RowSet;
                CachedRowSet crs1 = createCoffeesRowSet()) {
            assertFalse(crs1.toCollection().containsAll(col)
                    && col.containsAll(crs1.toCollection()));
        }
        rs.close();

    }

    /*
     * Validate that createCopy() returns the proper values
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0036(CachedRowSet rs) throws Exception {
        try (CachedRowSet crs1 = rs.createCopy()) {
            compareRowSets(rs, crs1);
        }
        rs.close();
    }

    /*
     * Validate that createCopySchema() returns the proper values
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0037(CachedRowSet rs) throws Exception {
        try (CachedRowSet crs1 = rs.createCopySchema()) {
            assertTrue(crs1.size() == 0);
            compareMetaData(crs1.getMetaData(), rs.getMetaData());
        }
        rs.close();
    }

    /*
     * Validate that createCopyNoConstraints() returns the proper values
     * and getMatchColumnIndexes should throw a SQLException. This test
     * specifies setMatchColumn(int)
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0038(CachedRowSet rs) throws Exception {
        rs.setMatchColumn(1);
        try (CachedRowSet crs1 = rs.createCopyNoConstraints()) {
            assertTrue(crs1.size() == COFFEE_HOUSES_ROWS);
            compareRowSets(rs, crs1);
            boolean recievedSQE = false;
            try {
                int[] indexes = crs1.getMatchColumnIndexes();
            } catch (SQLException e) {
                recievedSQE = true;
            }
            assertTrue(recievedSQE);
            recievedSQE = false;
            try {
                String[] colNames = crs1.getMatchColumnNames();
            } catch (SQLException e) {
                recievedSQE = true;
            }
            assertTrue(recievedSQE);
        }
        rs.close();
    }

    /*
     * Validate that createCopyNoConstraints() returns the proper values
     * and getMatchColumnIndexes should throw a SQLException. This test
     * specifies setMatchColumn(String)
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0039(CachedRowSet rs) throws Exception {
        rs.setMatchColumn("ID");
        try (CachedRowSet crs1 = rs.createCopyNoConstraints()) {
            assertTrue(crs1.size() == COFFEE_HOUSES_ROWS);
            compareRowSets(rs, crs1);
            boolean recievedSQE = false;
            try {
                int[] indexes = crs1.getMatchColumnIndexes();
            } catch (SQLException e) {
                recievedSQE = true;
            }
            assertTrue(recievedSQE);
            recievedSQE = false;
            try {
                String[] colNames = crs1.getMatchColumnNames();
            } catch (SQLException e) {
                recievedSQE = true;
            }
            assertTrue(recievedSQE);
        }
        rs.close();
    }

    /*
     * Validate that columnUpdated works with the various datatypes specifying
     * the column index
     */
    @Test(dataProvider = "rowsetUsingDataTypes")
    public void commonCachedRowSetTest0040(CachedRowSet rs, JDBCType type) throws Exception {
        rs.beforeFirst();
        assertTrue(rs.next());
        switch (type) {
            case INTEGER:
                assertFalse(rs.columnUpdated(1));
                rs.updateInt(1, Integer.MIN_VALUE);
                assertTrue(rs.columnUpdated(1));
                break;
            case CHAR:
                assertFalse(rs.columnUpdated(2));
                rs.updateString(2, "foo");
                assertTrue(rs.columnUpdated(2));
                break;
            case VARCHAR:
                assertFalse(rs.columnUpdated(3));
                rs.updateString(3, "foo");
                assertTrue(rs.columnUpdated(3));
                break;
            case BIGINT:
                assertFalse(rs.columnUpdated(4));
                rs.updateLong(4, Long.MIN_VALUE);
                assertTrue(rs.columnUpdated(4));
                break;
            case BOOLEAN:
                assertFalse(rs.columnUpdated(5));
                rs.updateBoolean(5, false);
                assertTrue(rs.columnUpdated(5));
                break;
            case SMALLINT:
                assertFalse(rs.columnUpdated(6));
                rs.updateShort(6, Short.MIN_VALUE);
                assertTrue(rs.columnUpdated(6));
                break;
            case DOUBLE:
                assertFalse(rs.columnUpdated(7));
                rs.updateDouble(7, Double.MIN_VALUE);
                assertTrue(rs.columnUpdated(7));
                break;
            case DECIMAL:
                assertFalse(rs.columnUpdated(8));
                rs.updateBigDecimal(8, BigDecimal.TEN);
                assertTrue(rs.columnUpdated(8));
                break;
            case REAL:
                assertFalse(rs.columnUpdated(9));
                rs.updateFloat(9, Float.MIN_VALUE);
                assertTrue(rs.columnUpdated(9));
                break;
            case TINYINT:
                assertFalse(rs.columnUpdated(10));
                rs.updateByte(10, Byte.MIN_VALUE);
                assertTrue(rs.columnUpdated(10));
                break;
            case DATE:
                assertFalse(rs.columnUpdated(11));
                rs.updateDate(11, Date.valueOf(LocalDate.now()));
                assertTrue(rs.columnUpdated(11));
                break;
            case TIME:
                assertFalse(rs.columnUpdated(12));
                rs.updateTime(12, Time.valueOf(LocalTime.now()));
                assertTrue(rs.columnUpdated(12));
                break;
            case TIMESTAMP:
                assertFalse(rs.columnUpdated(13));
                rs.updateTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
                assertTrue(rs.columnUpdated(13));
                break;
            case VARBINARY:
                assertFalse(rs.columnUpdated(14));
                rs.updateBytes(14, new byte[1]);
                assertTrue(rs.columnUpdated(14));
                break;
            case ARRAY:
                assertFalse(rs.columnUpdated(15));
                rs.updateArray(15, new StubArray("VARCHAR", new Object[10]));
                assertTrue(rs.columnUpdated(15));
                break;
            case REF:
                assertFalse(rs.columnUpdated(16));
                rs.updateRef(16, new StubRef("INTEGER", query));
                assertTrue(rs.columnUpdated(16));
                break;
            case FLOAT:
                assertFalse(rs.columnUpdated(17));
                rs.updateDouble(17, Double.MIN_NORMAL);
                assertTrue(rs.columnUpdated(17));
        }

    }

    /*
     * Validate that columnUpdated works with the various datatypes specifying
     * the column name
     */
    @Test(dataProvider = "rowsetUsingDataTypes")
    public void commonCachedRowSetTest0041(CachedRowSet rs, JDBCType type) throws Exception {
        rs.beforeFirst();
        assertTrue(rs.next());
        switch (type) {
            case INTEGER:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[0]));
                rs.updateInt(DATATYPES_COLUMN_NAMES[0], Integer.MIN_VALUE);
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[0]));
                break;
            case CHAR:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[1]));
                rs.updateString(DATATYPES_COLUMN_NAMES[1], "foo");
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[1]));
                break;
            case VARCHAR:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[2]));
                rs.updateString(DATATYPES_COLUMN_NAMES[2], "foo");
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[2]));
                break;
            case BIGINT:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[3]));
                rs.updateLong(DATATYPES_COLUMN_NAMES[3], Long.MIN_VALUE);
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[3]));
                break;
            case BOOLEAN:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[4]));
                rs.updateBoolean(DATATYPES_COLUMN_NAMES[4], false);
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[4]));
                break;
            case SMALLINT:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[5]));
                rs.updateShort(DATATYPES_COLUMN_NAMES[5], Short.MIN_VALUE);
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[5]));
                break;
            case DOUBLE:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[6]));
                rs.updateDouble(DATATYPES_COLUMN_NAMES[6], Double.MIN_VALUE);
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[6]));
                break;
            case DECIMAL:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[7]));
                rs.updateBigDecimal(DATATYPES_COLUMN_NAMES[7], BigDecimal.TEN);
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[7]));
                break;
            case REAL:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[8]));
                rs.updateFloat(DATATYPES_COLUMN_NAMES[8], Float.MIN_VALUE);
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[8]));
                break;
            case TINYINT:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[9]));
                rs.updateByte(DATATYPES_COLUMN_NAMES[9], Byte.MIN_VALUE);
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[9]));
                break;
            case DATE:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[10]));
                rs.updateDate(DATATYPES_COLUMN_NAMES[10], Date.valueOf(LocalDate.now()));
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[10]));
                break;
            case TIME:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[11]));
                rs.updateTime(DATATYPES_COLUMN_NAMES[11], Time.valueOf(LocalTime.now()));
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[11]));
                break;
            case TIMESTAMP:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[12]));
                rs.updateTimestamp(DATATYPES_COLUMN_NAMES[12], Timestamp.valueOf(LocalDateTime.now()));
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[12]));
                break;
            case VARBINARY:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[13]));
                rs.updateBytes(DATATYPES_COLUMN_NAMES[13], new byte[1]);
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[13]));
                break;
            case ARRAY:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[14]));
                rs.updateArray(DATATYPES_COLUMN_NAMES[14], new StubArray("VARCHAR", new Object[10]));
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[14]));
                break;
            case REF:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[15]));
                rs.updateRef(DATATYPES_COLUMN_NAMES[15], new StubRef("INTEGER", query));
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[15]));
                break;
            case FLOAT:
                assertFalse(rs.columnUpdated(DATATYPES_COLUMN_NAMES[16]));
                rs.updateDouble(DATATYPES_COLUMN_NAMES[16], Double.MIN_NORMAL);
                assertTrue(rs.columnUpdated(DATATYPES_COLUMN_NAMES[16]));
                break;
        }

    }

    /*
     * Validate isBeforeFirst(), isFirst() and first() return the correct
     * results
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0042(RowSet rs) throws Exception {
        assertFalse(rs.isBeforeFirst());
        assertFalse(rs.isFirst());
        rs.beforeFirst();
        assertTrue(rs.isBeforeFirst());
        assertFalse(rs.isFirst());
        rs.next();
        assertFalse(rs.isBeforeFirst());
        assertTrue(rs.isFirst());
        rs.next();
        assertFalse(rs.isBeforeFirst());
        assertFalse(rs.isFirst());
        rs.first();
        assertFalse(rs.isBeforeFirst());
        assertTrue(rs.isFirst());
        rs.close();
    }

    /*
     * Validate isAfterLast(), isLast() and last() return the correct
     * results
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0043(RowSet rs) throws Exception {
        assertFalse(rs.isAfterLast());
        assertFalse(rs.isLast());
        rs.afterLast();
        assertTrue(rs.isAfterLast());
        assertFalse(rs.isLast());
        rs.previous();
        assertFalse(rs.isAfterLast());
        assertTrue(rs.isLast());
        rs.previous();
        assertFalse(rs.isAfterLast());
        assertFalse(rs.isLast());
        rs.last();
        assertFalse(rs.isAfterLast());
        assertTrue(rs.isLast());
        rs.close();
    }

    /*
     * Validate a SQLException is thrown when undoDelete is called on the
     * insertRow
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0044(CachedRowSet rs) throws Exception {
        rs.insertRow();
        rs.undoDelete();
        rs.close();
    }

    /*
     * Validate a SQLException is thrown when undoDelete is called when
     * cursor is before the first row
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0045(CachedRowSet rs) throws Exception {
        rs.setShowDeleted(true);
        rs.beforeFirst();
        rs.undoDelete();
        rs.close();
    }

    /*
     * Validate a SQLException is thrown when undoDelete is called when
     * cursor is after the last row
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0046(CachedRowSet rs) throws Exception {
        rs.setShowDeleted(true);
        rs.afterLast();
        rs.undoDelete();
        rs.close();
    }

    /*
     * Validate a SQLException is thrown when undoUpdate is called on the
     * insertRow
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0047(CachedRowSet rs) throws Exception {
        rs.insertRow();
        rs.undoUpdate();
        rs.close();
    }

    /*
     * Validate a SQLException is thrown when undoUpdate is called when
     * cursor is before the first row
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0048(CachedRowSet rs) throws Exception {
        rs.setShowDeleted(true);
        rs.beforeFirst();
        rs.undoUpdate();
        rs.close();
    }

    /*
     * Validate a SQLException is thrown when undoUpdate is called when
     * cursor is after the last row
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0049(CachedRowSet rs) throws Exception {
        rs.setShowDeleted(true);
        rs.afterLast();
        rs.undoUpdate();
        rs.close();
    }

    /*
     * Validate a SQLException is thrown when undoInsert is called on the
     * insertRow
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0050(CachedRowSet rs) throws Exception {
        rs.insertRow();
        rs.undoInsert();
        rs.close();
    }

    /*
     * Validate a SQLException is thrown when undoInsert is called when
     * cursor is before the first row
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0051(CachedRowSet rs) throws Exception {
        rs.setShowDeleted(true);
        rs.beforeFirst();
        rs.undoInsert();
        rs.close();
    }

    /*
     * Validate a SQLException is thrown when undoInsert is called when
     * cursor is after the last row
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses",
            expectedExceptions = SQLException.class)
    public void commonCachedRowSetTest0052(CachedRowSet rs) throws Exception {
        rs.setShowDeleted(true);
        rs.afterLast();
        rs.undoInsert();
        rs.close();
    }

    /*
     * Insert a row, then call undoInsert to roll back the insert and validate
     * the row is not there
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0053(CachedRowSet rs) throws Exception {
        int rowToInsert = 1961;
        assertTrue(rs.size() == COFFEE_HOUSES_ROWS);
        // Add new row
        rs.moveToInsertRow();
        rs.updateInt(1, rowToInsert);
        rs.updateString(2, "GOTHAM");
        rs.updateInt(3, 3450);
        rs.updateInt(4, 2005);
        rs.updateInt(5, 5455);
        rs.insertRow();
        rs.moveToCurrentRow();
        // check that the number of rows has increased
        assertTrue(rs.size() == COFFEE_HOUSES_ROWS + 1);
        assertTrue(findRowByPrimaryKey(rs, rowToInsert, 1));
        rs.undoInsert();
        // Check to make sure the row is no longer there
        assertTrue(rs.size() == COFFEE_HOUSES_ROWS);
        assertFalse(findRowByPrimaryKey(rs, rowToInsert, 1));
        rs.close();
    }

    /*
     * Insert a row, delete the row and then call undoDelete to make sure it
     * is comes back
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0054(CachedRowSet rs) throws Exception {
        int rowToDelete = 1961;
        assertTrue(rs.size() == COFFEE_HOUSES_ROWS);
        // Add new row
        rs.moveToInsertRow();
        rs.updateInt(1, rowToDelete);
        rs.updateString(2, "GOTHAM");
        rs.updateInt(3, 3450);
        rs.updateInt(4, 2005);
        rs.updateInt(5, 5455);
        rs.insertRow();
        rs.moveToCurrentRow();
        // check that the number of rows has increased
        assertTrue(rs.size() == COFFEE_HOUSES_ROWS + 1);
        assertTrue(findRowByPrimaryKey(rs, rowToDelete, 1));
        rs.absolute(COFFEE_HOUSES_ROWS + 1);
        rs.deleteRow();
        // Check to make sure the row is no longer there
        //assertTrue(rs.size() ==  COFFEE_HOUSES_ROWS);
        assertFalse(findRowByPrimaryKey(rs, rowToDelete, 1));
        rs.setShowDeleted(true);
        rs.absolute(COFFEE_HOUSES_ROWS + 1);
        rs.undoDelete();
        // check that the row is back
        assertTrue(rs.size() == COFFEE_HOUSES_ROWS + 1);
        assertTrue(findRowByPrimaryKey(rs, rowToDelete, 1));
        rs.close();
    }

    /*
     * Insert a row, modify a field and then call undoUpdate to revert the
     * insert
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0055(CachedRowSet rs) throws Exception {
        int rowToInsert = 1961;
        assertTrue(rs.size() == COFFEE_HOUSES_ROWS);
        // Add new row
        rs.moveToInsertRow();
        rs.updateInt(1, rowToInsert);
        rs.updateString(2, "GOTHAM");
        rs.updateInt(3, 3450);
        rs.updateInt(4, 2005);
        rs.updateInt(5, 5455);
        rs.insertRow();
        rs.moveToCurrentRow();
        // check that the number of rows has increased
        assertTrue(rs.size() == COFFEE_HOUSES_ROWS + 1);
        assertTrue(findRowByPrimaryKey(rs, rowToInsert, 1));
        rs.absolute(COFFEE_HOUSES_ROWS + 1);
        // Save off the original column values
        String f2 = rs.getString(2);
        int f3 = rs.getInt(3);
        rs.updateString(2, "SMALLVILLE");
        rs.updateInt(3, 500);
        // Validate the columns have been updated
        assertTrue(rs.columnUpdated(2));
        assertTrue(rs.columnUpdated(3));
        // Undo the update and validate it has taken place
        rs.absolute(COFFEE_HOUSES_ROWS + 1);
        rs.undoUpdate();
        assertTrue(rs.size() == COFFEE_HOUSES_ROWS);
        assertFalse(findRowByPrimaryKey(rs, rowToInsert, 1));
        rs.close();
    }

    /*
     * Validate getOriginal returns a ResultSet which is a copy of the original
     * RowSet
     */
    @Test(dataProvider = "rowsetUsingCoffees")
    public void commonCachedRowSetTest0056(CachedRowSet rs) throws Exception {
        String coffee = "Hazelnut";
        int sales = 100;
        int id = 200;
        Object[] updatedPkeys = {1, id, 3, 4, 5};
        // Change the coffee name and sales total for row 2 and save the
        // previous values
        rs.absolute(2);
        int origId = rs.getInt(1);
        String origCoffee = rs.getString(2);
        int origSales = rs.getInt(5);
        rs.updateInt(1, id);
        rs.updateString(2, coffee);
        rs.updateInt(5, sales);
        // MetaData should match
        try ( // Get the original original RowSet and validate that the changes
                // are only made to the current, not the original
                ResultSet rs1 = rs.getOriginal()) {
            // MetaData should match
            compareMetaData(rs.getMetaData(), rs1.getMetaData());
            assertTrue(rs1.isBeforeFirst());
            assertTrue(rs1.getConcurrency() == ResultSet.CONCUR_UPDATABLE);
            assertTrue(rs1.getType() == ResultSet.TYPE_SCROLL_INSENSITIVE);
            rs1.absolute(2);
            // Check original rowset is not changed
            assertTrue(rs1.getInt(1) == origId);
            assertTrue(rs1.getString(2).equals(origCoffee));
            assertTrue(rs1.getInt(5) == origSales);
            assertEquals(getPrimaryKeys(rs1), COFFEES_PRIMARY_KEYS);
            // Check current rowset
            assertTrue(rs.getInt(1) == id);
            assertTrue(rs.getString(2).equals(coffee));
            assertTrue(rs.getInt(5) == sales);
            assertEquals(getPrimaryKeys(rs), updatedPkeys);
        }
        rs.close();
    }

    /*
     * Validate getOriginalRow returns a ResultSet which is a copy of the
     * original row that was modified
     */
    @Test(dataProvider = "rowsetUsingCoffees")
    public void commonCachedRowSetTest0057(CachedRowSet rs) throws Exception {
        String coffee = "Hazelnut";
        int sales = 100;
        int id = 200;
        Object[] updatedPkeys = {1, id, 3, 4, 5};
        // Change the coffee name and sales total for row 2 and save the
        // previous values
        rs.absolute(2);
        int origId = rs.getInt(1);
        String origCoffee = rs.getString(2);
        int origSales = rs.getInt(5);
        rs.updateInt(1, id);
        rs.updateString(2, coffee);
        rs.updateInt(5, sales);
        // MetaData should match
        try ( // Get the original original row and validate that the changes
                // are only made to the current, not the original
                ResultSet rs1 = rs.getOriginalRow()) {
            // MetaData should match
            compareMetaData(rs.getMetaData(), rs1.getMetaData());
            assertTrue(rs1.isBeforeFirst());
            assertTrue(rs1.getConcurrency() == ResultSet.CONCUR_UPDATABLE);
            assertTrue(rs1.getType() == ResultSet.TYPE_SCROLL_INSENSITIVE);
            rs1.next();
            assertTrue(rs1.isFirst() && rs1.isLast());
            assertTrue(rs1.getRow() == 1);
            // Check original row is not changed
            assertTrue(rs1.getInt(1) == origId);
            assertTrue(rs1.getString(2).equals(origCoffee));
            assertTrue(rs1.getInt(5) == origSales);
            // Check current row
            assertTrue(rs.getInt(1) == id);
            assertTrue(rs.getString(2).equals(coffee));
            assertTrue(rs.getInt(5) == sales);
            assertEquals(getPrimaryKeys(rs), updatedPkeys);
        }
        rs.close();
    }

    /*
     * Validate that restoreOrginal will restore the RowSet to its
     * state prior to the insert of a row
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0058(CachedRowSet rs) throws Exception {
        int rowToInsert = 1961;
        assertTrue(rs.size() == COFFEE_HOUSES_ROWS);
        try ( // Add new row
                CachedRowSet crs1 = rsf.createCachedRowSet()) {
            rs.beforeFirst();
            crs1.populate(rs);
            TestRowSetListener rsl = new TestRowSetListener();
            crs1.addRowSetListener(rsl);
            crs1.moveToInsertRow();
            crs1.updateInt(1, rowToInsert);
            crs1.updateString(2, "GOTHAM");
            crs1.updateInt(3, 3450);
            crs1.updateInt(4, 2005);
            crs1.updateInt(5, 5455);
            crs1.insertRow();
            assertTrue(rsl.isNotified(TestRowSetListener.ROW_CHANGED));
            crs1.moveToCurrentRow();
            assertTrue(findRowByPrimaryKey(crs1, rowToInsert, 1));
            // Restore back to our original state and the
            // previously inserted row should not be there
            rsl.resetFlag();
            crs1.restoreOriginal();
            assertTrue(rsl.isNotified(TestRowSetListener.ROWSET_CHANGED));
            assertTrue(crs1.isBeforeFirst());
            crs1.last();
            assertFalse(crs1.rowInserted());
            assertFalse(findRowByPrimaryKey(crs1, rowToInsert, 1));
        }
        rs.close();
    }

    /*
     * Validate that restoreOrginal will restore the RowSet to its
     * state prior to deleting a row
     */
    @Test(dataProvider = "rowsetUsingCoffees", enabled = true)
    public void commonCachedRowSetTest0059(CachedRowSet rs) throws Exception {
        int rowToDelete = 2;
        try (CachedRowSet crs1 = rsf.createCachedRowSet()) {
            rs.beforeFirst();
            crs1.populate(rs);
            TestRowSetListener rsl = new TestRowSetListener();
            crs1.addRowSetListener(rsl);
            // Delete a row, the PK is also the absolute position as a List
            // backs the RowSet
            crs1.absolute(rowToDelete);
            crs1.deleteRow();
            assertTrue(crs1.rowDeleted());
            assertFalse(findRowByPrimaryKey(crs1, rowToDelete, 1));
            // Restore back to our original state and the
            // previously deleted row should be there
            rsl.resetFlag();
            crs1.restoreOriginal();
            assertTrue(rsl.isNotified(TestRowSetListener.ROWSET_CHANGED));
            assertTrue(crs1.isBeforeFirst());
            crs1.absolute(rowToDelete);
            assertFalse(crs1.rowDeleted());
            assertTrue(findRowByPrimaryKey(crs1, rowToDelete, 1));
        }
        rs.close();
    }

    /*
     * Validate that restoreOrginal will restore the RowSet to its
     * state prior to updating a row
     */
    @Test(dataProvider = "rowsetUsingCoffees", enabled = true)
    public void commonCachedRowSetTest0060(CachedRowSet rs) throws Exception {
        int rowToUpdate = 2;
        String coffee = "Hazelnut";
        try (CachedRowSet crs1 = rsf.createCachedRowSet()) {
            rs.beforeFirst();
            crs1.populate(rs);
            TestRowSetListener rsl = new TestRowSetListener();
            crs1.addRowSetListener(rsl);
            // Delete a row, the PK is also the absolute position as a List
            // backs the RowSet
            crs1.absolute(rowToUpdate);
            String origCoffee = crs1.getString(2);
            crs1.updateString(2, coffee);
            assertTrue(crs1.columnUpdated(2));
            crs1.updateRow();
            assertTrue(crs1.rowUpdated());
            assertFalse(origCoffee.equals(crs1.getString(2)));
            // Restore back to our original state and the
            // previous value for the column within the row should be there
            rsl.resetFlag();
            crs1.restoreOriginal();
            assertTrue(rsl.isNotified(TestRowSetListener.ROWSET_CHANGED));
            assertTrue(crs1.isBeforeFirst());
            // absolute() is failing for some reason so need to look at this later
            crs1.next();
            crs1.next();
            assertFalse(crs1.columnUpdated(2));
            assertFalse(crs1.rowUpdated());
            assertTrue(origCoffee.equals(crs1.getString(2)));
        }
        rs.close();
    }

    /*
     * Initialize a RowSet via the populate method. Validate it matches
     * the original ResultSet
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0061(CachedRowSet rs) throws Exception {
        try (CachedRowSet crs1 = rsf.createCachedRowSet()) {
            rs.beforeFirst();
            crs1.populate(rs);
            compareRowSets(rs, crs1);
        }
        rs.close();
    }

    /*
     * Initialize a RowSet via the populate method specifying a starting row.
     * Validate it matches the original ResultSet starting for the specofied
     * offset
     */
    @Test(dataProvider = "rowsetUsingCoffeeHouses")
    public void commonCachedRowSetTest0062(CachedRowSet rs) throws Exception {
        Object[] expectedRows = {
            32001, 10042, 10024, 10039, 10041, 33005, 33010, 10035, 10037,
            10034, 32004
        };
        int startingRow = 4;
        try (CachedRowSet crs1 = rsf.createCachedRowSet()) {
            rs.beforeFirst();
            crs1.populate(rs, startingRow);
            assertEquals(crs1.size(), COFFEE_HOUSES_ROWS - startingRow + 1);
            assertEquals(getPrimaryKeys(crs1), expectedRows);
        }
        rs.close();
    }

}
