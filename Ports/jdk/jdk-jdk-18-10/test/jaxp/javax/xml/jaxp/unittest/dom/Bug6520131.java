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

package dom;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/*
 * @test
 * @bug 6520131
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.Bug6520131
 * @run testng/othervm dom.Bug6520131
 * @summary Test DOMErrorHandler reports an error for invalid character.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6520131 {

    @Test
    public void test() {
        String string = new String("\u0001");

        try {
            // create document
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            Document document = documentBuilder.newDocument();

            DOMConfiguration domConfig = document.getDomConfig();
            domConfig.setParameter("well-formed", Boolean.TRUE);
            domConfig.setParameter("error-handler", new DOMErrorHandler() {
                public boolean handleError(DOMError e) {
                    throw new RuntimeException(e.getMessage());
                }
            });

            // add text element
            Element textElement = document.createElementNS("", "Text");
            Text text = document.createTextNode(string);
            textElement.appendChild(text);
            document.appendChild(textElement);

            // normalize document
            document.normalizeDocument();

            Assert.fail("Invalid character exception not thrown");
        } catch (ParserConfigurationException e) {
            Assert.fail("Unable to configure parser");
        } catch (RuntimeException e) {
            // This exception is expected!
        }
    }
}
