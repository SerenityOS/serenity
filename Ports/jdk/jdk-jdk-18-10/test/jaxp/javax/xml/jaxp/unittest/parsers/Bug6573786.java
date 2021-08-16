/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.InputStream;
import java.io.StringBufferInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6573786
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow parsers.Bug6573786
 * @run testng/othervm parsers.Bug6573786
 * @summary Test parser error messages are formatted.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6573786 {
    String _cache = "";

    @Test
    public void test() {
        final String XML = "" + "<?xml version='1.0' encoding='UTF-8' standalone='bad_value' ?>" + "<root />";

        runTest(XML);

    }

    @Test
    public void test1() {
        final String XML = "" + "<?xml version='1.0' standalone='bad_value' encoding='UTF-8' ?>" + "<root />";
        runTest(XML);

    }

    void runTest(String xmlString) {
        Bug6573786ErrorHandler handler = new Bug6573786ErrorHandler();
        try {
            InputStream is = new StringBufferInputStream(xmlString);
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            parser.parse(is, handler);
        } catch (Exception e) {
            if (handler.fail) {
                Assert.fail("The value of standalone attribute should be merged into the error message.");
            }
        }

    }
}
