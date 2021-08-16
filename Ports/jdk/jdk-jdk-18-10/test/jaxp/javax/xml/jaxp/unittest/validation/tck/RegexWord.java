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
package validation.tck;

import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

/*
 * @test
 * @bug 8142900
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow validation.tck.RegexWord
 * @run testng/othervm validation.tck.RegexWord
 * @summary Verifies that all characters except the set of "punctuation",
 * "separator" and "other" characters are accepted by \w [#x0000-#x10FFFF]-[\p{P}\p{Z}\p{C}]
 * @author Joe Wang
 */
@Listeners({jaxp.library.FilePolicy.class})
public class RegexWord {
    static final String SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";

    /*
    The original reZ003v.xml contains a full list of word characters that \w should accept.
    However, U+2308..U+230B were changed from Sm to either Ps or Pe in Unicode 7.0.
    They are therefore excluded from the test.

    The test throws an Exception (and fails) if it fails to recognize any of characters.
    */
    @Test
    public void test() throws SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(new StreamSource(RegexWord.class.getResourceAsStream("reZ003.xsd")));
        Validator validator = schema.newValidator();

        validator.validate(new StreamSource(RegexWord.class.getResourceAsStream("reZ003vExc23082309.xml")));
    }
}
