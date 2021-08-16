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
package test.rowset.webrowset;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Arrays;
import javax.sql.rowset.WebRowSet;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertEqualsNoOrder;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;
import test.rowset.cachedrowset.CommonCachedRowSetTests;

public abstract class CommonWebRowSetTests extends CommonCachedRowSetTests {

    protected final String XMLFILEPATH = System.getProperty("test.src", ".")
            + File.separatorChar + "xml" + File.separatorChar;
    protected final String COFFEE_ROWS_XML = XMLFILEPATH + "COFFEE_ROWS.xml";
    protected final String DELETED_COFFEE_ROWS_XML
            = XMLFILEPATH + "DELETED_COFFEE_ROWS.xml";
    protected final String MODFIED_DELETED_COFFEE_ROWS_XML
            = XMLFILEPATH + "MODFIED_DELETED_COFFEE_ROWS.xml";
    protected final String UPDATED_COFFEE_ROWS_XML
            = XMLFILEPATH + "UPDATED_COFFEE_ROWS.xml";
    protected final String INSERTED_COFFEE_ROWS_XML
            = XMLFILEPATH + "INSERTED_COFFEE_ROWS.xml";
    protected final String UPDATED_INSERTED_COFFEE_ROWS_XML
            = XMLFILEPATH + "UPDATED_INSERTED_COFFEE_ROWS.xml";


    /*
     * Utility method to write a WebRowSet XML file via an OutputStream
     */
    protected ByteArrayOutputStream writeWebRowSetWithOutputStream(WebRowSet rs) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            rs.writeXml(oos);
        }
        return baos;
    }

    /*
     * Utility method to write a WebRowSet XML file via an OutputStream
     * and populating the WebRowSet via a ResultSet
     */
    protected ByteArrayOutputStream writeWebRowSetWithOutputStream(ResultSet rs) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            WebRowSet wrs = rsf.createWebRowSet();
            wrs.writeXml(rs, oos);
        }
        return baos;
    }


    /*
     * Utility method to popoulate a WebRowSet via a InputStream
     */
    protected WebRowSet readWebRowSetWithOInputStream(ByteArrayOutputStream baos) throws Exception {
        WebRowSet wrs1 = rsf.createWebRowSet();
        try (ObjectInputStream ois
                = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            wrs1.readXml(ois);
        }
        return wrs1;
    }

    /*
     * Utility method to write a WebRowSet XML file via an Writer
     */
    protected ByteArrayOutputStream writeWebRowSetWithOutputStreamWithWriter(WebRowSet rs) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(baos);
        rs.writeXml(osw);
        return baos;
    }

    /*
     * Utility method to write a WebRowSet XML file via an Writer and populating
     * the WebRowSet via a ResultSet
     */
    protected ByteArrayOutputStream writeWebRowSetWithOutputStreamWithWriter(ResultSet rs) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(baos);
        WebRowSet wrs = rsf.createWebRowSet();
        wrs.writeXml(rs, osw);
        return baos;
    }

    /*
     * Utility method to popoulate a WebRowSet via a Readar
     */
    protected WebRowSet readWebRowSetWithOInputStreamWithReader(ByteArrayOutputStream baos) throws Exception {
        WebRowSet wrs1 = rsf.createWebRowSet();
        InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
        wrs1.readXml(isr);
        return wrs1;
    }

    /*
     * Validate the expected Rows are contained within the RowSet
     */
    @Test(dataProvider = "rowsetUsingCoffees")
    public void WebRowSetTest0000(WebRowSet wrs) throws Exception {
        assertEquals(getPrimaryKeys(wrs), COFFEES_PRIMARY_KEYS);
        assertEquals(wrs.size(), COFFEES_ROWS);
        wrs.close();
    }

    /*
     * Validate the expected Rows are contained within the RowSet
     * populated by readXML(Reader)
     */
    @Test(dataProvider = "rowSetType")
    public void WebRowSetTest0001(WebRowSet wrs1) throws Exception {

        try (FileReader fr = new FileReader(COFFEE_ROWS_XML)) {
            wrs1.readXml(fr);
        }
        assertEquals(getPrimaryKeys(wrs1), COFFEES_PRIMARY_KEYS);
        assertEquals(wrs1.size(), COFFEES_ROWS);
        wrs1.close();

    }

    /*
     * Validate the expected Rows are contained within the RowSet
     * populated by readXML(InputStream)
     */
    @Test(dataProvider = "rowSetType")
    public void WebRowSetTest0002(WebRowSet wrs1) throws Exception {
        try (FileInputStream fis = new FileInputStream(COFFEE_ROWS_XML)) {
            wrs1.readXml(fis);
        }
        assertEquals(getPrimaryKeys(wrs1), COFFEES_PRIMARY_KEYS);
        assertEquals(wrs1.size(), COFFEES_ROWS);
        wrs1.close();
    }

    /*
     * Write a WebRowSet via writeXML(OutputStream), read it
     * back via readXML(InputStream) and validate the primary  keys
     * are the same
     */
    @Test(dataProvider = "rowsetUsingCoffees")
    public void WebRowSetTest0003(WebRowSet wrs) throws Exception {
        ByteArrayOutputStream baos = writeWebRowSetWithOutputStream(wrs);
        try (WebRowSet wrs1 = readWebRowSetWithOInputStream(baos)) {
            assertEquals(getPrimaryKeys(wrs1), COFFEES_PRIMARY_KEYS);
            assertEquals(wrs1.size(), COFFEES_ROWS);
        }
    }

    /*
     * Write a ResultSet via writeXML(OutputStream), read it
     * back via readXML(InputStream) and validate the primary  keys
     * are the same
     */
    @Test(dataProvider = "rowsetUsingCoffees")
    public void WebRowSetTest0004(WebRowSet wrs) throws Exception {
        ResultSet rs = wrs;
        rs.beforeFirst();
        ByteArrayOutputStream baos = writeWebRowSetWithOutputStream(rs);
        try (WebRowSet wrs1 = readWebRowSetWithOInputStream(baos)) {
            assertEquals(getPrimaryKeys(wrs1), COFFEES_PRIMARY_KEYS);
            assertEquals(wrs1.size(), COFFEES_ROWS);
        }
    }

    /*
     * Write a WebRowSet via writeXML(Writer), read it
     * back via readXML(Reader) and validate the primary  keys
     * are the same
     */
    @Test(dataProvider = "rowsetUsingCoffees")
    public void WebRowSetTest0005(WebRowSet wrs) throws Exception {
        ByteArrayOutputStream baos = writeWebRowSetWithOutputStreamWithWriter(wrs);
        try (WebRowSet wrs1 = readWebRowSetWithOInputStreamWithReader(baos)) {
            assertEquals(getPrimaryKeys(wrs1), COFFEES_PRIMARY_KEYS);
            assertEquals(wrs1.size(), COFFEES_ROWS);
        }
    }

    /*
     * Write a WebRowSet via writeXML(Writer), read it
     * back via readXML(Reader) and validate the primary  keys
     * are the same
     */
    @Test(dataProvider = "rowsetUsingCoffees")
    public void WebRowSetTest0006(WebRowSet wrs) throws Exception {
        ResultSet rs = wrs;
        rs.beforeFirst();
        ByteArrayOutputStream baos = writeWebRowSetWithOutputStreamWithWriter(rs);
        try (WebRowSet wrs1 = readWebRowSetWithOInputStreamWithReader(baos)) {
            assertEquals(getPrimaryKeys(wrs1), COFFEES_PRIMARY_KEYS);
            assertEquals(wrs1.size(), COFFEES_ROWS);
        }
    }

    /*
     * Validate the expected Rows are contained within the RowSet
     * after deleting the specified rows
     */
    @Test(dataProvider = "rowsetUsingCoffees", enabled = false)
    public void WebRowSetTest0007(WebRowSet wrs) throws Exception {
        assertEquals(getPrimaryKeys(wrs), COFFEES_PRIMARY_KEYS);
        int[] rowsToDelete = {2, 4};
        assertEquals(getPrimaryKeys(wrs), COFFEES_PRIMARY_KEYS);
        for (int row : rowsToDelete) {
            assertTrue(deleteRowByPrimaryKey(wrs, row, 1));
        }

        FileInputStream fis = new FileInputStream(MODFIED_DELETED_COFFEE_ROWS_XML);
        try (WebRowSet wrs1 = rsf.createWebRowSet()) {
            wrs1.readXml(fis);
            // With setShowDeleted(false) which is the default,
            // the deleted row should not be visible
            for (int row : rowsToDelete) {
                assertTrue(findRowByPrimaryKey(wrs1, row, 1));
            }
            assertTrue(wrs.size() == COFFEES_ROWS);
            // With setShowDeleted(true), the deleted row should be visible
            for (int row : rowsToDelete) {
                assertTrue(findRowByPrimaryKey(wrs, row, 1));
            }
        }
    }

    /*
     * Validate the expected Rows are contained within the RowSet
     * that was populated by reading an xml file with all rows
     * marked as a currentRow
     */
    @Test(dataProvider = "rowSetType")
    public void WebRowSetTest0008(WebRowSet wrs1) throws Exception {
        FileInputStream fis = new FileInputStream(COFFEE_ROWS_XML);
        wrs1.readXml(fis);
        assertTrue(wrs1.size() == COFFEES_ROWS);
        assertEquals(getPrimaryKeys(wrs1), COFFEES_PRIMARY_KEYS);
        // Validate that the rows are not marked as deleted, inserted or updated
        wrs1.beforeFirst();
        while (wrs1.next()) {
            assertFalse(wrs1.rowDeleted());
            assertFalse(wrs1.rowInserted());
            assertFalse(wrs1.rowUpdated());
        }
        wrs1.close();
    }

    /*
     * Read an XML file to populate a WebRowSet and validate that the rows
     * that are marked as deleted are marked as such in the WebRowSet
     * Also validate that they are or are not visible based on the
     * setShowDeleted value
     */
    @Test(dataProvider = "rowSetType")
    public void WebRowSetTest0009(WebRowSet wrs1) throws Exception {
        int[] rowsToDelete = {2, 4};
        Object[] expectedRows = {1, 3, 5};
        FileInputStream fis = new FileInputStream(DELETED_COFFEE_ROWS_XML);
        wrs1.readXml(fis);
        assertTrue(wrs1.size() == COFFEES_ROWS);
        assertEquals(getPrimaryKeys(wrs1), expectedRows);
        // With setShowDeleted(false) which is the default,
        // the deleted row should not be visible
        for (int row : rowsToDelete) {
            assertFalse(findRowByPrimaryKey(wrs1, row, 1));
        }
        // With setShowDeleted(true), the deleted row should be visible
        wrs1.setShowDeleted(true);
        for (int row : rowsToDelete) {
            assertTrue(findRowByPrimaryKey(wrs1, row, 1));
        }
        assertEquals(getPrimaryKeys(wrs1), COFFEES_PRIMARY_KEYS);
        wrs1.close();

    }

    /*
     * Validate that the correct row in the WebRowSet that had been created
     * from an xml file is marked as updated and contains the correct values
     */
    @Test(dataProvider = "rowSetType")
    public void WebRowSetTest0010(WebRowSet wrs1) throws Exception {
        FileInputStream fis = new FileInputStream(UPDATED_COFFEE_ROWS_XML);
        wrs1.readXml(fis);
        assertTrue(wrs1.size() == COFFEES_ROWS);
        assertEquals(getPrimaryKeys(wrs1), COFFEES_PRIMARY_KEYS);
        wrs1.beforeFirst();
        while (wrs1.next()) {
            if (wrs1.getInt(1) == 3) {
                assertTrue(wrs1.rowUpdated());
                assertTrue(wrs1.getInt(5) == 21 && wrs1.getInt(6) == 69);
                assertFalse(wrs1.rowDeleted());
                assertFalse(wrs1.rowInserted());
            } else {
                assertFalse(wrs1.rowUpdated());
                assertFalse(wrs1.rowDeleted());
                assertFalse(wrs1.rowInserted());
            }
        }
        wrs1.close();
    }

    /*
     * Validate the correct row is marked as inserted in a WebRowSet
     * that is read from an xml file
     */
    @Test(dataProvider = "rowSetType")
    public void WebRowSetTest0011(WebRowSet wrs1) throws Exception {
        int expectedSize = COFFEES_ROWS + 2;
        int addedRowPK = 15;
        int addedRowPK2 = 20;
        Object[] expected = Arrays.copyOf(COFFEES_PRIMARY_KEYS, expectedSize);
        expected[expectedSize - 2] = addedRowPK;
        expected[expectedSize - 1] = addedRowPK2;
        FileInputStream fis = new FileInputStream(INSERTED_COFFEE_ROWS_XML);
        wrs1.readXml(fis);
        assertTrue(wrs1.size() == expectedSize);
        assertEqualsNoOrder(getPrimaryKeys(wrs1), expected);
        wrs1.beforeFirst();
        while (wrs1.next()) {
            if (wrs1.getInt(1) == 15 || wrs1.getInt(1) == 20) {
                assertTrue(wrs1.rowInserted());
                assertFalse(wrs1.rowDeleted());
                assertFalse(wrs1.rowUpdated());
            } else {
                assertFalse(wrs1.rowInserted());
                assertFalse(wrs1.rowDeleted());
                assertFalse(wrs1.rowUpdated());
            }
        }
        wrs1.close();
    }

    /*
     * Read an xml file which contains a row that was inserted and updated
     */
    @Test(dataProvider = "rowSetType")
    public void WebRowSetTest0012(WebRowSet wrs1) throws Exception {
        int expectedSize = COFFEES_ROWS + 1;
        int addedRowPK = 100;
        Object[] expected = Arrays.copyOf(COFFEES_PRIMARY_KEYS, expectedSize);
        expected[expectedSize - 1] = addedRowPK;
        FileInputStream fis = new FileInputStream(UPDATED_INSERTED_COFFEE_ROWS_XML);
        wrs1.readXml(fis);
        assertTrue(wrs1.size() == expectedSize);
        assertEquals(getPrimaryKeys(wrs1), expected);
        wrs1.beforeFirst();
        while (wrs1.next()) {
            if (wrs1.getInt(1) == addedRowPK) {
                // Row that was inserted and updated
                assertTrue(wrs1.rowUpdated());
                assertTrue(
                        wrs1.getBigDecimal(4).equals(BigDecimal.valueOf(12.99))
                        && wrs1.getInt(6) == 125);
                assertFalse(wrs1.rowDeleted());
                assertTrue(wrs1.rowInserted());
            } else {
                // Remaining rows should only be inserted
                assertFalse(wrs1.rowUpdated());
                assertFalse(wrs1.rowDeleted());
                assertTrue(wrs1.rowInserted());
            }
        }
        wrs1.close();
    }

}
