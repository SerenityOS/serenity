/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package transform;

import java.util.Properties;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.XMLFilter;

/*
 * @test
 * @bug 8206164
 * @modules java.xml
 * @run testng transform.SAXTFactoryTest
 * @summary Tests SAXTransformerFactory.
 */
public class SAXTFactoryTest {

    /*
     * Verifies that the default ErrorListener throws a TransformerException
     * when a fatal error is encountered. It is then wrapped and thrown again in
     * a TransformerConfigurationException.
    */
    @Test
    public void testErrorListener() throws Exception {
        try {
            SAXTransformerFactory saxTFactory =
                    (SAXTransformerFactory)TransformerFactory.newInstance();
            XMLFilter filter = saxTFactory.newXMLFilter(new ATemplatesImpl());
        } catch (TransformerConfigurationException tce) {
            Throwable cause = tce.getCause();
            Assert.assertTrue((cause != null && cause instanceof TransformerException),
                    "The TransformerFactoryImpl terminates upon a fatal error "
                            + "by throwing a TransformerException.");
        }

    }

    class ATemplatesImpl implements Templates {

        @Override
        public Transformer newTransformer() throws TransformerConfigurationException {
            throw new TransformerConfigurationException("TCE from ATemplatesImpl");
        }

        @Override
        public Properties getOutputProperties() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
}
