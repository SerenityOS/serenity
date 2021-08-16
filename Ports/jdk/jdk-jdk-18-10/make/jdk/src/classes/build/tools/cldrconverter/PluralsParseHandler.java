/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Handles parsing of files in Locale Data Markup Language for
 * plurals.xml
 */

class PluralsParseHandler extends AbstractLDMLHandler<Map<String, String>> {
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
        case "plurals":
            // Only deal with "cardinal" type for now.
            if (attributes.getValue("type").equals("cardinal")) {
                pushContainer(qName, attributes);
            } else {
                // ignore
                pushIgnoredContainer(qName);
            }
            break;
        case "pluralRules":
            // key: locales
            pushKeyContainer(qName, attributes, attributes.getValue("locales"));
            break;
        case "pluralRule":
            pushStringEntry(qName, attributes, attributes.getValue("count"));
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
        switch (qName) {
            case "pluralRule":
                assert !(currentContainer instanceof Entry);
                Entry<?> entry = (Entry<?>)currentContainer;
                final String count = entry.getKey();
                if (!count.equals("other")) {
                    final String rule = (String)entry.getValue();
                    String locales = ((KeyContainer)(currentContainer.getParent())).getKey();
                    Arrays.stream(locales.split("\\s"))
                        .forEach(loc -> {
                            Map<String, String> rules = get(loc);
                            if (rules == null) {
                                rules = new HashMap<>();
                                put(loc, rules);
                            }
                            rules.put(count, rule);
                        });
                }
                break;
        }

        currentContainer = currentContainer.getParent();
    }
}
