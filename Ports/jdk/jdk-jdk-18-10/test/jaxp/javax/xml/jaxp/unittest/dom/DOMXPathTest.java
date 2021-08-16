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
package dom;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.DOMImplementation;

/*
 * @test
 * @bug 8042244
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow dom.DOMXPathTest
 * @run testng/othervm dom.DOMXPathTest
 * @summary Verifies that the experimental DOM L3 XPath implementation is no longer available.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class DOMXPathTest {
    /*
       Verifies that DOMImplementation::hasFeature returns false and getFeature
       returns null for DOM L3 XPath.
     */
    @Test
    public void test() throws ParserConfigurationException {
        DOMImplementation domImpl = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .getDOMImplementation();

        Assert.assertFalse(domImpl.hasFeature("+XPath", "3.0"));
        Assert.assertEquals(domImpl.getFeature("+XPath", "3.0"), null);
    }
}
