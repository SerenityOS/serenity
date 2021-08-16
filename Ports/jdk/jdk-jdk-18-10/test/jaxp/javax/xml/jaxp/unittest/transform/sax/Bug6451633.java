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

package transform.sax;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.helpers.AttributesImpl;

/*
 * @test
 * @bug 6451633
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow transform.sax.Bug6451633
 * @run testng/othervm transform.sax.Bug6451633
 * @summary Test TransformerHandler ignores empty text node.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class Bug6451633 {

    @Test
    public void test() throws Exception {
        TransformerHandler th = ((SAXTransformerFactory) TransformerFactory.newInstance()).newTransformerHandler();

        DOMResult result = new DOMResult();
        th.setResult(result);

        th.startDocument();
        th.startElement("", "root", "root", new AttributesImpl());
        th.characters(new char[0], 0, 0);
        th.endElement("", "root", "root");
        th.endDocument();

        // there's no point in having empty text --- we should remove it
        Assert.assertEquals(0, ((Document) result.getNode()).getDocumentElement().getChildNodes().getLength());
    }
}
