/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package common;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;

import static jaxp.library.JAXPTestUtilities.runWithAllPerm;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 6350682
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow common.Bug6350682
 * @run testng/othervm common.Bug6350682
 * @summary Test SAXParserFactory and TransformerFactory can newInstance when setContextClassLoader(null).
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6350682 {

    @Test
    public void testSAXParserFactory() {
        // This test run in othervm so change in environment need not to be recovered at the end of test.
        runWithAllPerm(() -> Thread.currentThread().setContextClassLoader(null));
        if (Bug6350682.class.getClassLoader() == null) {
            System.out.println("this class loader is NULL");
        } else {
            System.out.println("this class loader is NOT NULL");
        }
        SAXParserFactory factory = SAXParserFactory.newInstance();
        Assert.assertNotNull(factory, "Failed to get an instance of a SAXParserFactory");
    }

    @Test
    public void testTransformerFactory() {
        // This test run in othervm so change in environment need not to be recovered at the end of test.
        runWithAllPerm(() -> Thread.currentThread().setContextClassLoader(null));
        TransformerFactory factory = TransformerFactory.newInstance();
        Assert.assertNotNull(factory, "Failed to get an instance of a TransformerFactory");
    }
}
