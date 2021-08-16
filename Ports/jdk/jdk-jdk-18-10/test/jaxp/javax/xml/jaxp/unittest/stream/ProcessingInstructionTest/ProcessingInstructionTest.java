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

package stream.ProcessingInstructionTest;

import java.io.InputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow stream.ProcessingInstructionTest.ProcessingInstructionTest
 * @run testng/othervm stream.ProcessingInstructionTest.ProcessingInstructionTest
 * @summary Test XMLStreamReader parses Processing Instruction.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class ProcessingInstructionTest {

    @Test
    public void testPITargetAndData() {
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            String PITarget = "soffice";
            String PIData = "WebservicesArchitecture";
            String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<?" + PITarget + " " + PIData + "?>" + "<foo></foo>";
            // System.out.println("XML = " + xml) ;
            InputStream is = new java.io.ByteArrayInputStream(xml.getBytes());
            XMLStreamReader sr = xif.createXMLStreamReader(is);
            while (sr.hasNext()) {
                int eventType = sr.next();
                if (eventType == XMLStreamConstants.PROCESSING_INSTRUCTION) {
                    String target = sr.getPITarget();
                    String data = sr.getPIData();
                    Assert.assertTrue(target.equals(PITarget) && data.equals(PIData));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
