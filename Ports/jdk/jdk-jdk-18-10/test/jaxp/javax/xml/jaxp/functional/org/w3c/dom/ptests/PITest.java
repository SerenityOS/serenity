/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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
package org.w3c.dom.ptests;

import static org.testng.Assert.assertEquals;
import static org.w3c.dom.ptests.DOMTestUtil.createDOMWithNS;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.ProcessingInstruction;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow org.w3c.dom.ptests.PITest
 * @run testng/othervm org.w3c.dom.ptests.PITest
 * @summary Test for the methods of Processing Instruction
 */
@Listeners({jaxp.library.FilePolicy.class})
public class PITest {
    /*
     * Test getData, setData and getTarget methods
     */
    @Test
    public void test() throws Exception {
        Document document = createDOMWithNS("PITest01.xml");
        ProcessingInstruction pi = document.createProcessingInstruction("PI", "processing");
        assertEquals(pi.getData(), "processing");
        assertEquals(pi.getTarget(), "PI");

        pi.setData("newProcessing");
        assertEquals(pi.getData(), "newProcessing");
    }

}
