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

package transform;

import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 6883209
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.OpenJDK100017Test
 * @run testng/othervm transform.OpenJDK100017Test
 * @summary Test XSLT won't cause StackOverflow when it handle many characters.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class OpenJDK100017Test {

    @Test
    public final void testXMLStackOverflowBug() throws TransformerConfigurationException, IOException, SAXException {
        try {
            SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
            TransformerHandler ser = stf.newTransformerHandler();
            ser.setResult(new StreamResult(System.out));

            StringBuilder sb = new StringBuilder(4096);
            for (int x = 4096; x > 0; x--) {
                sb.append((char) x);
            }
            ser.characters(sb.toString().toCharArray(), 0, sb.toString().toCharArray().length);
            ser.endDocument();
        } catch (StackOverflowError se) {
            se.printStackTrace();
            Assert.fail("StackOverflow");
        }
    }
}
