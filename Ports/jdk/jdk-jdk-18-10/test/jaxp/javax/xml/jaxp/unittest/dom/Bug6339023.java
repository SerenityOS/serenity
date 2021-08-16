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

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DOMStringList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;

/*
 * @test
 * @bug 6339023
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.Bug6339023
 * @run testng/othervm dom.Bug6339023
 * @summary Test normalize-characters.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6339023 {

    /*
     * This test checks DOMConfiguration for DOM Level3 Load and Save
     * implementation.
     */
    @Test
    public void testLSSerializer() {
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            DOMImplementation impln = parser.getDOMImplementation();
            DOMImplementationLS lsImpln = (DOMImplementationLS) impln.getFeature("LS", "3.0");
            LSSerializer serializer = lsImpln.createLSSerializer();
            DOMConfiguration domConfig = serializer.getDomConfig();
            System.out.println("DOMConfig: " + domConfig.toString());
            Assert.assertTrue(domConfig.getParameter("normalize-characters") == null);
            System.out.println("value: " + domConfig.getParameter("normalize-characters"));

            DOMStringList list = domConfig.getParameterNames();
            for (int i = 0; i < list.getLength(); i++) {
                System.out.println("Param Name: " + list.item(i));
                Assert.assertFalse(list.item(i).equals("normalize-characters"));
            }

            Assert.assertFalse(domConfig.canSetParameter("normalize-characters", Boolean.FALSE));
            Assert.assertFalse(domConfig.canSetParameter("normalize-characters", Boolean.TRUE));

            try {
                domConfig.setParameter("normalize-characters", Boolean.TRUE);
                Assert.fail("Exception expected as 'normalize-characters' is not supported");
            } catch (Exception e) {
            }

            try {
                domConfig.setParameter("normalize-characters", Boolean.FALSE);
                Assert.fail("Exception expected as 'normalize-characters' is not supported");
            } catch (Exception e) {
            }

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception: " + e.getMessage());
        }
    }

    /*
     * This test checks DOMConfiguration for DOM Level3 Core implementation.
     */
    @Test
    public void testLSParser() {
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            DOMImplementation impln = parser.getDOMImplementation();
            DOMImplementationLS lsImpln = (DOMImplementationLS) impln.getFeature("Core", "3.0");
            LSParser lsparser = lsImpln.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, "http://www.w3.org/2001/XMLSchema");
            DOMConfiguration domConfig = lsparser.getDomConfig();
            System.out.println("DOMConfig: " + domConfig.toString());
            Assert.assertTrue(domConfig.getParameter("normalize-characters").toString().equalsIgnoreCase("false"));
            System.out.println("value: " + domConfig.getParameter("normalize-characters"));

            DOMStringList list = domConfig.getParameterNames();
            boolean flag = false;
            for (int i = 0; i < list.getLength(); i++) {
                System.out.println("Param Name: " + list.item(i));
                if (list.item(i).equals("normalize-characters")) {
                    flag = true;
                    break;
                }
            }
            Assert.assertTrue(flag, "'normalize-characters' doesnot exist in the list returned by domConfig.getParameterNames()");

            Assert.assertTrue(domConfig.canSetParameter("normalize-characters", Boolean.FALSE));
            Assert.assertFalse(domConfig.canSetParameter("normalize-characters", Boolean.TRUE));

            try {
                domConfig.setParameter("normalize-characters", Boolean.TRUE);
                Assert.fail("Exception expected as 'normalize-characters' is not supported");
            } catch (Exception e) {
            }

            try {
                domConfig.setParameter("normalize-characters", Boolean.FALSE);
            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail("Exception expected as 'normalize-characters' is not supported");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception: " + e.getMessage());
        }
    }

}
