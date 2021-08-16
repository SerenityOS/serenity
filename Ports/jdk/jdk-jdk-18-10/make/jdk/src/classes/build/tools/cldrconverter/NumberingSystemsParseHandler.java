/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package build.tools.cldrconverter;

import java.io.File;
import java.io.IOException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Handles parsing of files in Locale Data Markup Language for numberingSystems.xml
 * and produces a map that uses the keys and values of JRE locale data.
 */

class NumberingSystemsParseHandler extends AbstractLDMLHandler<String> {

    NumberingSystemsParseHandler() {
    }

    @Override
    public InputSource resolveEntity(String publicID, String systemID) throws IOException, SAXException {
        // avoid HTTP traffic to unicode.org
        if (systemID.startsWith(CLDRConverter.SPPL_LDML_DTD_SYSTEM_ID)) {
            return new InputSource((new File(CLDRConverter.LOCAL_SPPL_LDML_DTD)).toURI().toString());
        }
        return null;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        switch (qName) {
        case "numberingSystem":
            numberingSystem: {
                if ("numeric".equals(attributes.getValue("type"))) {
                    // eg, <numberingSystem id="latn" type="numeric" digits="0123456789"/>
                    String script = attributes.getValue("id");
                    String digits = attributes.getValue("digits");

                    if (Character.isSurrogate(digits.charAt(0))) {
                        // DecimalFormatSymbols doesn't support supplementary characters as digit zero.
                        // Replace supplementary digits with latin digits. This is a restriction till JDK-8204092 is resolved.
                        digits = "0123456789";
                        put(script, digits);
                        break numberingSystem;
                    }
                    // in case digits are in the reversed order, reverse back the order.
                    if (digits.charAt(0) > digits.charAt(digits.length() - 1)) {
                        StringBuilder sb = new StringBuilder(digits);
                        digits = sb.reverse().toString();
                    }
                    // Check if the order is sequential.
                    char c0 = digits.charAt(0);
                    for (int i = 1; i < digits.length(); i++) {
                        if (digits.charAt(i) != c0 + i) {
                            break numberingSystem;
                        }
                    }

                    // script/digits are acceptable.
                    put(script, digits);
                }
            }
            pushIgnoredContainer(qName);
            break;
        case "version":
        case "generation":
            pushIgnoredContainer(qName);
            break;
        default:
            // treat anything else as a container
            pushContainer(qName, attributes);
            break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        assert qName.equals(currentContainer.getqName()) : "current=" + currentContainer.getqName() + ", param=" + qName;
        currentContainer = currentContainer.getParent();
    }
}
