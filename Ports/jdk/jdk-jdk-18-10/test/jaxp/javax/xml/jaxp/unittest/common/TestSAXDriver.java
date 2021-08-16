/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.xerces.internal.jaxp.SAXParserImpl;
import javax.xml.XMLConstants;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/*
 * Test implementation of SAXParser. It is extended from JDK parser and two methods
 * are overriden to disable support of specific features and properties.
 * This class is used in ValidationWarningsTest and TransformationWarningsTest
 * to generate multiple warnings during xml validation and transformation processes.
*/
public class TestSAXDriver extends SAXParserImpl.JAXPSAXParser {

    @Override
    public synchronized void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (XMLConstants.FEATURE_SECURE_PROCESSING.equals(name)) {
            throw new SAXNotRecognizedException(name+" feature is not recognised by test SAX parser intentionally.");
        } else {
            super.setFeature(name, value);
        }
    }

    @Override
    public synchronized void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (XMLConstants.ACCESS_EXTERNAL_DTD.equals(name) || ENT_EXP_LIMIT_PROP.equals(name)) {
            throw new SAXNotRecognizedException(name+" property is not recognised by test SAX parser intentionally.");
        } else {
            super.setProperty(name, value);
        }
    }

    private static final String ENT_EXP_LIMIT_PROP = "http://www.oracle.com/xml/jaxp/properties/entityExpansionLimit";
}
