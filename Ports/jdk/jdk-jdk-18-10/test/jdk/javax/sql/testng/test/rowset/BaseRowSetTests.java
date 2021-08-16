/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import javax.sql.RowSet;
import javax.sql.rowset.serial.SerialArray;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialRef;
import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import util.StubArray;
import util.StubBaseRowSet;
import util.StubBlob;
import util.StubClob;
import util.StubRef;
import util.TestRowSetListener;

public class BaseRowSetTests extends CommonRowSetTests {

    private StubBaseRowSet brs;

    @Override
    protected RowSet newInstance() throws SQLException {
        return new StubBaseRowSet();
    }

    /*
     * Create a RowSetListener and validate that notifyCursorMoved is called
     */
    @Test(dataProvider = "rowSetType")
    public void baseRowSetTest0000(StubBaseRowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        rs.addRowSetListener(rsl);
        rs.notifyCursorMoved();
        assertTrue(rsl.isNotified(TestRowSetListener.CURSOR_MOVED));
    }

    /*
     * Create a RowSetListener and validate that notifyRowChanged is called
     */
    @Test(dataProvider = "rowSetType")
    public void baseRowSetTest0001(StubBaseRowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        rs.addRowSetListener(rsl);
        rs.notifyRowChanged();
        assertTrue(rsl.isNotified(TestRowSetListener.ROW_CHANGED));
    }

    /*
     * Create a RowSetListener and validate that notifyRowSetChanged is called
     */
    @Test(dataProvider = "rowSetType")
    public void baseRowSetTest0002(StubBaseRowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        rs.addRowSetListener(rsl);
        rs.notifyRowSetChanged();
        assertTrue(rsl.isNotified(TestRowSetListener.ROWSET_CHANGED));
    }

    /*
     * Create multiple RowSetListeners and validate that notifyRowSetChanged
     * is called on all listeners
     */
    @Test(dataProvider = "rowSetType")
    public void baseRowSetTest0003(StubBaseRowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        TestRowSetListener rsl2 = new TestRowSetListener();
        rs.addRowSetListener(rsl);
        rs.addRowSetListener(rsl2);
        rs.notifyRowSetChanged();
        assertTrue(rsl.isNotified(TestRowSetListener.ROWSET_CHANGED));
        assertTrue(rsl2.isNotified(TestRowSetListener.ROWSET_CHANGED));
    }

    /*
     * Create multiple RowSetListeners and validate that notifyRowChanged
     * is called on all listeners
     */
    @Test(dataProvider = "rowSetType")
    public void baseRowSetTest0004(StubBaseRowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        TestRowSetListener rsl2 = new TestRowSetListener();
        rs.addRowSetListener(rsl);
        rs.addRowSetListener(rsl2);
        rs.notifyRowChanged();
        assertTrue(rsl.isNotified(TestRowSetListener.ROW_CHANGED));
        assertTrue(rsl2.isNotified(TestRowSetListener.ROW_CHANGED));
    }

    /*
     * Create multiple RowSetListeners and validate that notifyCursorMoved
     * is called on all listeners
     */
    @Test(dataProvider = "rowSetType")
    public void baseRowSetTest0005(StubBaseRowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        TestRowSetListener rsl2 = new TestRowSetListener();
        rs.addRowSetListener(rsl);
        rs.addRowSetListener(rsl2);
        rs.notifyCursorMoved();
        assertTrue(rsl.isNotified(TestRowSetListener.CURSOR_MOVED));
        assertTrue(rsl2.isNotified(TestRowSetListener.CURSOR_MOVED));
    }

    /*
     * Create a RowSetListener and validate that notifyRowSetChanged,
     * notifyRowChanged() and notifyCursorMoved are called
     */
    @Test(dataProvider = "rowSetType")
    public void baseRowSetTest0006(StubBaseRowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        rs.addRowSetListener(rsl);
        rs.notifyRowSetChanged();
        rs.notifyRowChanged();
        rs.notifyCursorMoved();
        assertTrue(rsl.isNotified(
                TestRowSetListener.CURSOR_MOVED | TestRowSetListener.ROWSET_CHANGED
                | TestRowSetListener.ROW_CHANGED));
    }


    /*
     * Create multiple RowSetListeners and validate that notifyRowSetChanged,
     * notifyRowChanged() and notifyCursorMoved are called on all listeners
     */
    @Test(dataProvider = "rowSetType")
    public void baseRowSetTest0007(StubBaseRowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        TestRowSetListener rsl2 = new TestRowSetListener();
        rs.addRowSetListener(rsl);
        rs.addRowSetListener(rsl2);
        rs.notifyRowSetChanged();
        rs.notifyRowChanged();
        rs.notifyCursorMoved();
        assertTrue(rsl.isNotified(
                TestRowSetListener.CURSOR_MOVED | TestRowSetListener.ROWSET_CHANGED
                | TestRowSetListener.ROW_CHANGED));
        assertTrue(rsl2.isNotified(
                TestRowSetListener.CURSOR_MOVED | TestRowSetListener.ROWSET_CHANGED
                | TestRowSetListener.ROW_CHANGED));
    }

    /*
     * Create a RowSetListener and validate that notifyRowSetChanged is called,
     * remove the listener, invoke notifyRowSetChanged again and verify the
     * listner is not called
     */
    @Test(dataProvider = "rowSetType")
    public void baseRowSetTest0008(StubBaseRowSet rs) throws Exception {
        TestRowSetListener rsl = new TestRowSetListener();
        rs.addRowSetListener(rsl);
        rs.notifyRowSetChanged();
        assertTrue(rsl.isNotified(TestRowSetListener.ROWSET_CHANGED));
        // Clear the flag indicating the listener has been called
        rsl.resetFlag();
        rs.removeRowSetListener(rsl);
        rs.notifyRowSetChanged();
        assertFalse(rsl.isNotified(TestRowSetListener.ROWSET_CHANGED));
    }

    /*
     * Set the base parameters and validate that the value set is
     * the correct type and value
     */
    @Test(dataProvider = "testBaseParameters")
    public void baseRowSetTest0009(int pos, Object o) throws Exception {
        assertTrue(getParam(pos, o).getClass().isInstance(o));
        assertTrue(o.equals(getParam(pos, o)));
    }

    /*
     * Set the complex parameters and validate that the value set is
     * the correct type
     */
    @Test(dataProvider = "testAdvancedParameters")
    public void baseRowSetTest0010(int pos, Object o) throws Exception {
        assertTrue(getParam(pos, o).getClass().isInstance(o));
    }

    /*
     * Validate setNull specifying the supported type values
     */
    @Test(dataProvider = "jdbcTypes")
    public void baseRowSetTest0011(Integer type) throws Exception {
        brs = new StubBaseRowSet();
        brs.setNull(1, type);
        assertTrue(checkNullParam(1, type, null));
    }

    /*
     * Validate setNull specifying the supported type values and that
     * typeName is set internally
     */
    @Test(dataProvider = "jdbcTypes")
    public void baseRowSetTest0012(Integer type) throws Exception {
        brs = new StubBaseRowSet();
        brs.setNull(1, type, "SUPERHERO");
        assertTrue(checkNullParam(1, type, "SUPERHERO"));
    }

    /*
     *  Validate that setDate sets the specified Calendar internally
     */
    @Test()
    public void baseRowSetTest0013() throws Exception {
        Calendar cal = Calendar.getInstance();
        brs = new StubBaseRowSet();
        brs.setDate(1, Date.valueOf(LocalDate.now()), cal);
        assertTrue(checkCalendarParam(1, cal));
    }

    /*
     *  Validate that setTime sets the specified Calendar internally
     */
    @Test()
    public void baseRowSetTest0014() throws Exception {
        Calendar cal = Calendar.getInstance();
        brs = new StubBaseRowSet();
        brs.setTime(1, Time.valueOf(LocalTime.now()), cal);
        assertTrue(checkCalendarParam(1, cal));
    }

    /*
     *  Validate that setTimestamp sets the specified Calendar internally
     */
    @Test()
    public void baseRowSetTest0015() throws Exception {
        Calendar cal = Calendar.getInstance();
        brs = new StubBaseRowSet();
        brs.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()), cal);
        assertTrue(checkCalendarParam(1, cal));
    }

    /*
     * Validate that initParams() initializes the parameters
     */
    @Test(dataProvider = "rowSetType")
    public void baseRowSetTest0016(StubBaseRowSet rs) throws Exception {
        rs.setInt(1, 1);
        rs.initParams();
        assertTrue(rs.getParams().length == 0);
    }


    /*
     * DataProvider used to set parameters for basic types that are supported
     */
    @DataProvider(name = "testBaseParameters")
    private Object[][] testBaseParameters() throws SQLException {
        Integer aInt = 1;
        Long aLong = Long.MAX_VALUE;
        Short aShort = Short.MIN_VALUE;
        BigDecimal bd = BigDecimal.ONE;
        Double aDouble = Double.MAX_VALUE;
        Date aDate = Date.valueOf(LocalDate.now());
        Time aTime = Time.valueOf(LocalTime.now());
        Timestamp aTimeStamp = Timestamp.valueOf(LocalDateTime.now());
        Calendar cal = Calendar.getInstance();
        Boolean aBoolean = true;
        Float aFloat = 1.5f;
        Byte aByte = 1;
        brs = new StubBaseRowSet();

        brs.setInt(1, aInt);
        brs.setString(2, query);
        brs.setLong(3, aLong);
        brs.setBoolean(4, aBoolean);
        brs.setShort(5, aShort);
        brs.setDouble(6, aDouble);
        brs.setBigDecimal(7, bd);
        brs.setFloat(8, aFloat);
        brs.setByte(9, aByte);
        brs.setDate(10, aDate);
        brs.setTime(11, aTime);
        brs.setTimestamp(12, aTimeStamp);
        brs.setDate(13, aDate, cal);
        brs.setTime(14, aTime, cal);
        brs.setTimestamp(15, aTimeStamp);
        brs.setObject(16, query);
        brs.setObject(17, query, Types.CHAR);
        brs.setObject(18, query, Types.CHAR, 0);

        return new Object[][]{
            {1, aInt},
            {2, query},
            {3, aLong},
            {4, aBoolean},
            {5, aShort},
            {6, aDouble},
            {7, bd},
            {8, aFloat},
            {9, aByte},
            {10, aDate},
            {11, aTime},
            {12, aTimeStamp},
            {13, aDate},
            {14, aTime},
            {15, aTimeStamp},
            {16, query},
            {17, query},
            {18, query}

        };
    }

    /*
     * DataProvider used to set advanced parameters for types that are supported
     */
    @DataProvider(name = "testAdvancedParameters")
    private Object[][] testAdvancedParameters() throws SQLException {

        byte[] bytes = new byte[10];
        Ref aRef = new SerialRef(new StubRef("INTEGER", query));
        Array aArray = new SerialArray(new StubArray("INTEGER", new Object[1]));
        Blob aBlob = new SerialBlob(new StubBlob());
        Clob aClob = new SerialClob(new StubClob());
        Reader rdr = new StringReader(query);
        InputStream is = new StringBufferInputStream(query);;
        brs = new StubBaseRowSet();
        brs.setBytes(1, bytes);
        brs.setAsciiStream(2, is, query.length());
        brs.setRef(3, aRef);
        brs.setArray(4, aArray);
        brs.setBlob(5, aBlob);
        brs.setClob(6, aClob);
        brs.setBinaryStream(7, is, query.length());
        brs.setUnicodeStream(8, is, query.length());
        brs.setCharacterStream(9, rdr, query.length());

        return new Object[][]{
            {1, bytes},
            {2, is},
            {3, aRef},
            {4, aArray},
            {5, aBlob},
            {6, aClob},
            {7, is},
            {8, is},
            {9, rdr}
        };
    }

    /*
     *  Method that returns the specified parameter instance that was set via setXXX
     *  Note non-basic types are stored as an Object[] where the 1st element
     *  is the object instnace
     */
    @SuppressWarnings("unchecked")
    private <T> T getParam(int pos, T o) throws SQLException {
        Object[] params = brs.getParams();
        if (params[pos - 1] instanceof Object[]) {
            Object[] param = (Object[]) params[pos - 1];
            return (T) param[0];
        } else {
            return (T) params[pos - 1];
        }
    }

    /*
     * Utility method to validate parameters when the param is an Object[]
     */
    private boolean checkParam(int pos, int type, Object val) throws SQLException {
        boolean result = false;
        Object[] params = brs.getParams();
        if (params[pos - 1] instanceof Object[]) {
            Object[] param = (Object[]) params[pos - 1];

            if (param[0] == null) {
                // setNull was used
                if (param.length == 2 && (Integer) param[1] == type) {
                    result = true;
                } else {
                    if (param.length == 3 && (Integer) param[1] == type
                            && val.equals(param[2])) {
                        result = true;
                    }
                }

            } else if (param[0] instanceof java.util.Date) {
                // setDate/Time/Timestamp with a Calendar object
                if (param[1] instanceof Calendar && val.equals(param[1])) {
                    result = true;
                }
            }
        }
        return result;
    }

    /*
     * Wrapper method for validating that a null was set and the appropriate
     * type and typeName if applicable
     */
    private boolean checkNullParam(int pos, int type, String typeName) throws SQLException {
        return checkParam(pos, type, typeName);
    }

    /*
     *  Wrapper method for validating that a Calander was set
     */
    private boolean checkCalendarParam(int pos, Calendar cal) throws SQLException {
        // 2nd param is ignored when instanceof java.util.Date
        return checkParam(pos, Types.DATE, cal);
    }
}
