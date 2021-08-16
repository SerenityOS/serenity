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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import javax.sql.RowSetMetaData;
import javax.sql.rowset.RowSetMetaDataImpl;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import util.BaseTest;

public class RowSetMetaDataTests extends BaseTest {

    // Max columns used in the tests
    private final int MAX_COLUMNS = 5;
    // Instance to be used within the tests
    private RowSetMetaDataImpl rsmd;

    @BeforeMethod
    public void setUpMethod() throws Exception {
        rsmd = new RowSetMetaDataImpl();
        rsmd.setColumnCount(MAX_COLUMNS);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test(Integer col) throws Exception {
        rsmd.getCatalogName(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test01(Integer col) throws Exception {
        rsmd.getColumnClassName(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test02(Integer col) throws Exception {
        rsmd.getColumnDisplaySize(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test03(Integer col) throws Exception {
        rsmd.getColumnLabel(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test04(Integer col) throws Exception {
        rsmd.getColumnName(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test05(Integer col) throws Exception {
        rsmd.getColumnType(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test06(Integer col) throws Exception {
        rsmd.getColumnTypeName(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test07(Integer col) throws Exception {
        rsmd.getPrecision(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test08(Integer col) throws Exception {
        rsmd.getScale(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test09(Integer col) throws Exception {
        rsmd.getSchemaName(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test10(Integer col) throws Exception {
        rsmd.getTableName(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test11(Integer col) throws Exception {
        rsmd.isAutoIncrement(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test12(Integer col) throws Exception {
        rsmd.isCaseSensitive(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test13(Integer col) throws Exception {
        rsmd.isCurrency(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test14(Integer col) throws Exception {
        rsmd.isDefinitelyWritable(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test15(Integer col) throws Exception {
        rsmd.isNullable(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test16(Integer col) throws Exception {
        rsmd.isReadOnly(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test17(Integer col) throws Exception {
        rsmd.isSearchable(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test18(Integer col) throws Exception {
        rsmd.isSigned(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test19(Integer col) throws Exception {
        rsmd.isWritable(col);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test20(Integer col) throws Exception {
        rsmd.setAutoIncrement(col, true);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test21(Integer col) throws Exception {
        rsmd.setCaseSensitive(col, true);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test22(Integer col) throws Exception {
        rsmd.setCatalogName(col, null);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test23(Integer col) throws Exception {
        rsmd.setColumnDisplaySize(col, 5);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test24(Integer col) throws Exception {
        rsmd.setColumnLabel(col, "label");
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test25(Integer col) throws Exception {
        rsmd.setColumnName(col, "F1");
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test26(Integer col) throws Exception {
        rsmd.setColumnType(col, Types.CHAR);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test27(Integer col) throws Exception {
        rsmd.setColumnTypeName(col, "F1");
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test28(Integer col) throws Exception {
        rsmd.setCurrency(col, true);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test29(Integer col) throws Exception {
        rsmd.setNullable(col, ResultSetMetaData.columnNoNulls);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test30(Integer col) throws Exception {
        rsmd.setPrecision(col, 2);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test31(Integer col) throws Exception {
        rsmd.setScale(col, 2);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test32(Integer col) throws Exception {
        rsmd.setSchemaName(col, "Gotham");
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test33(Integer col) throws Exception {
        rsmd.setSearchable(col, false);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test34(Integer col) throws Exception {
        rsmd.setSigned(col, false);
    }

    /*
     * Validate a SQLException is thrown for an invalid column index
     */
    @Test(dataProvider = "invalidColumnRanges",
            expectedExceptions = SQLException.class)
    public void test35(Integer col) throws Exception {
        rsmd.setTableName(col, "SUPERHEROS");
    }

    /*
     * Validate that the correct class name is returned for the column
     * Note:  Once setColumnClassName is added to RowSetMetaData, this
     * method will need to change.
     */
    @Test(dataProvider = "columnClassNames")
    public void test36(Integer type, String name) throws Exception {
        rsmd.setColumnType(1, type);
        assertTrue(rsmd.getColumnClassName(1).equals(name));
    }

    /*
     * Validate that all of the methods are accessible and the correct value
     * is returned for each column
     */
    @Test(dataProvider = "columnRanges")
    public void test37(Integer col) throws Exception {
        rsmd.setAutoIncrement(col, true);
        assertTrue(rsmd.isAutoIncrement(col));
        rsmd.setCaseSensitive(col, true);
        assertTrue(rsmd.isCaseSensitive(col));
        rsmd.setCatalogName(col, "Gotham");
        assertTrue(rsmd.getCatalogName(col).equals("Gotham"));
        rsmd.setColumnDisplaySize(col, 20);
        assertTrue(rsmd.getColumnDisplaySize(col) == 20);
        rsmd.setColumnLabel(col, "F1");
        assertTrue(rsmd.getColumnLabel(col).equals("F1"));
        rsmd.setColumnName(col, "F1");
        assertTrue(rsmd.getColumnName(col).equals("F1"));
        rsmd.setColumnType(col, Types.INTEGER);
        assertTrue(rsmd.getColumnType(col) == Types.INTEGER);
        assertTrue(rsmd.getColumnClassName(col).equals(Integer.class.getName()));
        rsmd.setColumnTypeName(col, "INTEGER");
        assertTrue(rsmd.getColumnTypeName(col).equals("INTEGER"));
        rsmd.setCurrency(col, true);
        assertTrue(rsmd.isCurrency(col));
        rsmd.setNullable(col, ResultSetMetaData.columnNoNulls);
        assertTrue(rsmd.isNullable(col) == ResultSetMetaData.columnNoNulls);
        rsmd.setPrecision(col, 2);
        assertTrue(rsmd.getPrecision(col) == 2);
        rsmd.setScale(col, 2);
        assertTrue(rsmd.getScale(col) == 2);
        rsmd.setSchemaName(col, "GOTHAM");
        assertTrue(rsmd.getSchemaName(col).equals("GOTHAM"));
        rsmd.setSearchable(col, false);
        assertFalse(rsmd.isSearchable(col));
        rsmd.setSigned(col, false);
        assertFalse(rsmd.isSigned(col));
        rsmd.setTableName(col, "SUPERHEROS");
        assertTrue(rsmd.getTableName(col).equals("SUPERHEROS"));
        rsmd.isReadOnly(col);
        rsmd.isDefinitelyWritable(col);
        rsmd.isWritable(col);

    }

    /*
     * Validate that the proper values are accepted by setNullable
     */
    @Test(dataProvider = "validSetNullableValues")
    public void test38(Integer val) throws Exception {
        rsmd.setNullable(1, val);
    }

    /*
     * Validate that the correct type is returned for the column
     */
    @Test(dataProvider = "jdbcTypes")
    public void test39(Integer type) throws Exception {
        rsmd.setColumnType(1, type);
        assertTrue(type == rsmd.getColumnType(1));
    }

    /*
     * Validate that the correct value is returned from the isXXX methods
     */
    @Test(dataProvider = "trueFalse")
    public void test40(Boolean b) throws Exception {
        rsmd.setAutoIncrement(1, b);
        rsmd.setCaseSensitive(1, b);
        rsmd.setCurrency(1, b);
        rsmd.setSearchable(1, b);
        rsmd.setSigned(1, b);
        assertTrue(rsmd.isAutoIncrement(1) == b);
        assertTrue(rsmd.isCaseSensitive(1) == b);
        assertTrue(rsmd.isCurrency(1) == b);
        assertTrue(rsmd.isSearchable(1) == b);
        assertTrue(rsmd.isSigned(1) == b);
    }

    /*
     * Validate isWrapperFor and unwrap work correctly
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test99() throws Exception {
        RowSetMetaData rsmd1 = rsmd;
        ResultSetMetaData rsmd2 = rsmd;
        Class clzz = rsmd.getClass();
        assertTrue(rsmd1.isWrapperFor(clzz));
        assertTrue(rsmd2.isWrapperFor(clzz));
        RowSetMetaDataImpl rsmdi = (RowSetMetaDataImpl) rsmd2.unwrap(clzz);

        // False should be returned
        assertFalse(rsmd1.isWrapperFor(this.getClass()));
        assertFalse(rsmd2.isWrapperFor(this.getClass()));
    }

    /*
     * DataProvider used to provide Date which are not valid and are used
     * to validate that an IllegalArgumentException will be thrown from the
     * valueOf method
     */
    @DataProvider(name = "validSetNullableValues")
    private Object[][] validSetNullableValues() {
        return new Object[][]{
            {ResultSetMetaData.columnNoNulls},
            {ResultSetMetaData.columnNullable},
            {ResultSetMetaData.columnNullableUnknown}
        };
    }

    /*
     * DataProvider used to provide column indexes that are out of range so that
     * SQLException is thrown
     */
    @DataProvider(name = "invalidColumnRanges")
    private Object[][] invalidColumnRanges() {
        return new Object[][]{
            {-1},
            {0},
            {MAX_COLUMNS + 1}
        };
    }

    /*
     * DataProvider used to provide the valid column ranges for the
     * RowSetMetaDataImpl object
     */
    @DataProvider(name = "columnRanges")
    private Object[][] columnRanges() {
        Object[][] o = new Object[MAX_COLUMNS][1];
        for (int i = 1; i <= MAX_COLUMNS; i++) {
            o[i - 1][0] = i;
        }
        return o;
    }

    /*
     * DataProvider used to specify the value to set via setColumnType and
     * the expected value to be returned from getColumnClassName
     */
    @DataProvider(name = "columnClassNames")
    private Object[][] columnClassNames() {
        return new Object[][]{
            {Types.CHAR, "java.lang.String"},
            {Types.NCHAR, "java.lang.String"},
            {Types.VARCHAR, "java.lang.String"},
            {Types.NVARCHAR, "java.lang.String"},
            {Types.LONGVARCHAR, "java.lang.String"},
            {Types.LONGNVARCHAR, "java.lang.String"},
            {Types.NUMERIC, "java.math.BigDecimal"},
            {Types.DECIMAL, "java.math.BigDecimal"},
            {Types.BIT, "java.lang.Boolean"},
            {Types.TINYINT, "java.lang.Byte"},
            {Types.SMALLINT, "java.lang.Short"},
            {Types.INTEGER, "java.lang.Integer"},
            {Types.FLOAT, "java.lang.Double"},
            {Types.DOUBLE, "java.lang.Double"},
            {Types.BINARY, "byte[]"},
            {Types.VARBINARY, "byte[]"},
            {Types.LONGVARBINARY, "byte[]"},
            {Types.DATE, "java.sql.Date"},
            {Types.TIME, "java.sql.Time"},
            {Types.TIMESTAMP, "java.sql.Timestamp"},
            {Types.CLOB, "java.sql.Clob"},
            {Types.BLOB, "java.sql.Blob"}

        };

    }

}
