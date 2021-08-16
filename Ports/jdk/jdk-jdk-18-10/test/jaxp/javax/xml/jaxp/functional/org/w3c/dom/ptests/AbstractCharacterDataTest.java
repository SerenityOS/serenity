/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package org.w3c.dom.ptests;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import static org.w3c.dom.DOMException.INDEX_SIZE_ERR;
import static org.w3c.dom.ptests.DOMTestUtil.DOMEXCEPTION_EXPECTED;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

/*
 * @summary common test for the CharacterData Interface
 */
public abstract class AbstractCharacterDataTest {
    @DataProvider(name = "data-for-length")
    public Object[][] getDataForTestLength() {
        return new Object[][] {
                { "", 0 },
                { "test", 4 } };
    }

    /*
     * Verify getLength method works as the spec, for an empty string, should
     * return zero
     */
    @Test(dataProvider = "data-for-length")
    public void testGetLength(String text, int length) throws Exception {
        CharacterData cd = createCharacterData(text);
        assertEquals(cd.getLength(), length);

    }

    /*
     * Test appendData method and verify by getData method.
     */
    @Test
    public void testAppendData() throws Exception {
        CharacterData cd = createCharacterData("DOM");
        cd.appendData("2");
        assertEquals(cd.getData(), "DOM2");

    }

    @DataProvider(name = "data-for-delete")
    public Object[][] getDataForTestDelete() {
        return new Object[][] {
                { "DOM", 2, 1, "DO" },
                { "DOM", 0, 2, "M" },
                { "DOM", 2, 3, "DO" } };
    }

    /*
     * Verify deleteData method works as the spec.
     */
    @Test(dataProvider = "data-for-delete")
    public void testDeleteData(String text, int offset, int count, String result) throws Exception {
        CharacterData cd = createCharacterData(text);
        cd.deleteData(offset, count);
        assertEquals(cd.getData(), result);
    }

    @DataProvider(name = "data-for-replace")
    public Object[][] getDataForTestReplace() {
        return new Object[][] {
                { "DOM", 0, 3, "SAX", "SAX" },
                { "DOM", 1, 1, "AA", "DAAM" },
                { "DOM", 1, 2, "A", "DA" },
                { "DOM", 2, 2, "SAX", "DOSAX" } };
    }

    /*
     * Verify replaceData method works as the spec.
     */
    @Test(dataProvider = "data-for-replace")
    public void testReplaceData(String text, int offset, int count, String arg, String result) throws Exception {
        CharacterData cd = createCharacterData(text);
        cd.replaceData(offset, count, arg);
        assertEquals(cd.getData(), result);
    }

    @DataProvider(name = "data-for-replace-neg")
    public Object[][] getDataForTestReplaceNeg() {
        return new Object[][] {
                { "DOM", -1, 3, "SAX" }, //offset if neg
                { "DOM", 0, -1, "SAX" }, //count is neg
                { "DOM", 4, 1, "SAX" } };//offset is greater than length
    }

    /*
     * Test for replaceData method: verifies that DOMException with
     * INDEX_SIZE_ERR is thrown if offset or count is out of the bound.
     */
    @Test(dataProvider = "data-for-replace-neg")
    public void testReplaceDataNeg(String text, int offset, int count, String arg) throws Exception {
        CharacterData cd = createCharacterData(text);
        try {
            cd.replaceData(offset, count, arg);
            fail(DOMEXCEPTION_EXPECTED);
        } catch (DOMException e) {
            assertEquals(e.code, INDEX_SIZE_ERR);
        }
    }

    @DataProvider(name = "data-for-insert")
    public Object[][] getDataForTestInsert() {
        return new Object[][] {
                { "DOM", 0, "SAX", "SAXDOM" },
                { "DOM", 3, "SAX", "DOMSAX" } };
    }

    /*
     * Verify insertData method works as the spec.
     */
    @Test(dataProvider = "data-for-insert")
    public void testInsertData(String text, int offset, String arg, String result) throws Exception {
        CharacterData cd = createCharacterData(text);
        cd.insertData(offset, arg);
        assertEquals(cd.getData(), result);
    }

    @DataProvider(name = "data-for-insert-neg")
    public Object[][] getDataForTestInsertNeg() {
        return new Object[][] {
                { "DOM", -1 }, //offset is neg
                { "DOM", 4 } };//offset is greater than length
    }

    /*
     * Test for insertData method: verifies that DOMException with
     * INDEX_SIZE_ERR is thrown if offset is out of the bound.
     */
    @Test(dataProvider = "data-for-insert-neg")
    public void testInsertDataNeg(String text, int offset) throws Exception {
        CharacterData cd = createCharacterData(text);
        try {
            cd.insertData(offset, "TEST");
            fail(DOMEXCEPTION_EXPECTED);
        } catch (DOMException e) {
            assertEquals(e.code, INDEX_SIZE_ERR);
        }
    }

    /*
     * Test setData method and verify by getData method.
     */
    @Test
    public void testSetData() throws Exception {
        CharacterData cd = createCharacterData("DOM");
        cd.setData("SAX");
        assertEquals(cd.getData(), "SAX");
    }

    @DataProvider(name = "data-for-substring")
    public Object[][] getDataForTestSubstring() {
        return new Object[][] {
                { "DOM Level 2", 0, 3, "DOM" },
                { "DOM", 0, 3, "DOM" },
                { "DOM", 2, 5, "M" } };
    }

    /*
     * Verify substringData method works as the spec.
     */
    @Test(dataProvider = "data-for-substring")
    public void testSubstringData(String text, int offset, int count, String result) throws Exception {
        CharacterData cd = createCharacterData(text);
        String retStr = cd.substringData(offset, count);
        assertEquals(retStr, result);
    }

    @DataProvider(name = "data-for-substring-neg")
    public Object[][] getDataForTestSubstringNeg() {
        return new Object[][] {
                { "DOM Level 2", -1, 3 }, //offset is neg
                { "DOM", 0, -1 }, //count is neg
                { "DOM", 3, 1 } }; //offset exceeds length
    }

    /*
     * Test for substringData method: verifies that DOMException with
     * INDEX_SIZE_ERR is thrown if offset or count is out of the bound.
     */
    @Test(dataProvider = "data-for-substring-neg")
    public void testSubstringDataNeg(String text, int offset, int count) throws Exception {
        CharacterData cd = createCharacterData(text);
        try {
            cd.substringData(offset, count);
            fail(DOMEXCEPTION_EXPECTED);
        } catch (DOMException e) {
            assertEquals(e.code, INDEX_SIZE_ERR);
        }

    }

    /*
     * Return a concrete CharacterData instance.
     */
    abstract protected CharacterData createCharacterData(String text) throws IOException, SAXException, ParserConfigurationException;

}
