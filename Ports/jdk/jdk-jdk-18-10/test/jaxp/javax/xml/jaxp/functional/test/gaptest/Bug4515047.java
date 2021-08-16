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

package test.gaptest;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 4515047
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow test.gaptest.Bug4515047
 * @run testng/othervm test.gaptest.Bug4515047
 * @summary test transform an empty dom source
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug4515047 {

    @Test
    public void testCreateTxDoc() throws TransformerException, ParserConfigurationException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();

        StreamResult result = new StreamResult(System.out);
        DOMSource source = new DOMSource();

        /* This should not throw an Illegal Argument Exception */
        //Test empty DOMSource
        transformer.transform(source, result);

        //Test DOMSource having only an empty node
        source.setNode(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
        transformer.transform(source, result);
    }

}
