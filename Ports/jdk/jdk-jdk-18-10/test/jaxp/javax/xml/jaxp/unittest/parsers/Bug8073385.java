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

package parsers;

import static jaxp.library.JAXPTestUtilities.runWithAllPerm;
import static org.testng.Assert.assertTrue;

import java.io.StringReader;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @test
 * @bug 8073385
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug8073385
 * @run testng/othervm parsers.Bug8073385
 * @summary test that invalid XML character exception string contains
 *     information about character value, element and attribute names
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug8073385 {

    private Locale defLoc;

    @BeforeClass
    public void setup() {
        defLoc = Locale.getDefault();
        runWithAllPerm(() -> Locale.setDefault(Locale.ENGLISH));
    }

    @AfterClass
    public void cleanup() {
        runWithAllPerm(() -> Locale.setDefault(defLoc));
    }

    @DataProvider(name = "illegalCharactersData")
    public static Object[][] illegalCharactersData() {
        return new Object[][]{
            {0x00},
            {0xFFFE},
            {0xFFFF}
        };
    }

    @Test(dataProvider = "illegalCharactersData")
    public void test(int character) throws Exception {
        // Construct the XML document as a String
        int[] cps = new int[]{character};
        String txt = new String(cps, 0, cps.length);
        String inxml = "<topElement attTest=\'" + txt + "\'/>";
        String exceptionText = "NO EXCEPTION OBSERVED";
        String hexString = "0x" + Integer.toHexString(character);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource isrc = new InputSource(new StringReader(inxml));

        try {
            db.parse(isrc);
        } catch (SAXException e) {
            exceptionText = e.toString();
        }
        System.out.println("Got Exception:" + exceptionText);
        assertTrue(exceptionText.contains("attribute \"attTest\""));
        assertTrue(exceptionText.contains("element is \"topElement\""));
        assertTrue(exceptionText.contains("Unicode: " + hexString));
    }
}
