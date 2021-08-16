/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8020430
 * @summary test that setProperty for XMLOutputFactory works properly
 * @run main/othervm JAXP15RegTest
 */
import java.security.Policy;
import javax.xml.stream.XMLOutputFactory;

/**
 * @author huizhe.wang@oracle.com
 */
public class JAXP15RegTest extends TestBase {

    public JAXP15RegTest(String name) {
        super(name);
    }

    private boolean hasSM;
    private Policy _orig;


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JAXP15RegTest test = new JAXP15RegTest("JAXP 1.5 regression");
        test.setUp();
        test.testXMLOutputFactory();
        test.tearDown();
    }


    public void testXMLOutputFactory() {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
        success("testXMLOutputFactory passed");
    }

}
