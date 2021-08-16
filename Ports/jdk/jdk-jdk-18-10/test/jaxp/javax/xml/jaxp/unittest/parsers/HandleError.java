/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.StringReader;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8157797
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.HandleError
 * @run testng/othervm parsers.HandleError
 * @summary Tests that the parser handles errors properly.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class HandleError {

    /*
     * Verifies that the parser returns with no unexpected "java.lang.InternalError"
     * when continue-after-fatal-error is requested.
    */
    @Test
    public void test() throws Exception {
        String invalidXml = "<a>";
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
        SAXParser parser = saxParserFactory.newSAXParser();
        parser.parse(new InputSource(new StringReader(invalidXml)), new DefaultHandler() {
            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                System.err.printf("%s%n", e.getMessage());
            }
        });
    }


    /*
     * Verifies that the parser throws SAXParseException when parsing error is
     * encountered when:
     * continue-after-fatal-error is not set, the default it false
     * continue-after-fatal-error is explicitly set to false
    */
    @Test(dataProvider = "setFeature", expectedExceptions = SAXParseException.class)
    public void test1(boolean setFeature, boolean value) throws Exception {
        String invalidXml = "<a>";
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        if (setFeature) {
            saxParserFactory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", value);
        }
        SAXParser parser = saxParserFactory.newSAXParser();
        parser.parse(new InputSource(new StringReader(invalidXml)), new DefaultHandler());
    }

    /*
       DataProvider: used to set feature "continue-after-fatal-error"
        Data columns:
        flag to indicate the feature is to be set, the value of the feature
     */
    @DataProvider(name = "setFeature")
    public Object[][] getFeatureSetting() {

        return new Object[][]{
            {false, false},
            {true, false},
         };
    }
}
