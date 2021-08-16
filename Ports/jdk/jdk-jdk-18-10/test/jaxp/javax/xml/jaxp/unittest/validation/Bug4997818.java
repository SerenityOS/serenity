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

package validation;

import java.io.StringReader;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/*
 * @test
 * @bug 4997818
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.Bug4997818
 * @run testng/othervm validation.Bug4997818
 * @summary Test SchemaFactory.newSchema(...) throws an exception, which is thrown from LSResourceResolver.
 */

@Listeners({jaxp.library.BasePolicy.class})
public class Bug4997818 {

    @Test
    public void test1() throws Exception {
        String xsd1 = "<?xml version='1.0'?>\n" + "<schema xmlns='http://www.w3.org/2001/XMLSchema'\n" + "        xmlns:test='jaxp13_test1'\n"
                + "        targetNamespace='jaxp13_test1'\n" + "        elementFormDefault='qualified'>\n" + "    <import namespace='jaxp13_test2'/>\n"
                + "    <element name='test'/>\n" + "    <element name='child1'/>\n" + "</schema>\n";

        final NullPointerException EUREKA = new NullPointerException("NewSchema015");

        SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        StringReader reader = new StringReader(xsd1);
        StreamSource source = new StreamSource(reader);
        LSResourceResolver resolver = new LSResourceResolver() {
            public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                LSInput input;
                if (namespaceURI != null && namespaceURI.endsWith("jaxp13_test2")) {
                    throw EUREKA;
                } else {
                    input = null;
                }

                return input;
            }
        };
        schemaFactory.setResourceResolver(resolver);

        try {
            schemaFactory.newSchema(new Source[] { source });
            Assert.fail("NullPointerException was not thrown.");
        } catch (RuntimeException e) {
            if (e != EUREKA)
                throw e;
        }
    }
}
