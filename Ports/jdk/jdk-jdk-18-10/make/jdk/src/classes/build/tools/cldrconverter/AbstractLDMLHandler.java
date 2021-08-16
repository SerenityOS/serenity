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

import build.tools.cldrconverter.CLDRConverter.DraftType;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This is an abstract class for general LDML parsing purpose.
 * LDMLParseHandler, SupplementLDMLParseHandler, and NumberingLDMLParseHandler
 * are the subclasses of this class.
 */

abstract class AbstractLDMLHandler<V> extends DefaultHandler {
    static final Map<String, String> DAY_OF_WEEK_MAP = new HashMap<>();
    static {
        DAY_OF_WEEK_MAP.put("sun", "1");
        DAY_OF_WEEK_MAP.put("mon", "2");
        DAY_OF_WEEK_MAP.put("tue", "3");
        DAY_OF_WEEK_MAP.put("wed", "4");
        DAY_OF_WEEK_MAP.put("thu", "5");
        DAY_OF_WEEK_MAP.put("fri", "6");
        DAY_OF_WEEK_MAP.put("sat", "7");
    }
    // Collected data in JRE locale data format.
    private Map<String, V> data = new HashMap<>();

    // The root Container
    Container currentContainer = new Container("$ROOT", null);

    AbstractLDMLHandler() {
    }

    Map<String, V> getData() {
        return data;
    }

    V put(String key, V value) {
        return data.put(key, value);
    }

    V get(String key) {
        return data.get(key);
    }

    Set<String> keySet() {
        return data.keySet();
    }

    /*
     * It returns true if the data should be ignored based on the user
     * defined acceptance level, which is listed with draft attribute in
     * the cldr locale xml files.
     * When the alt attribute is present, the data is always ignored since
     * we always use the primary data
     */
    boolean isIgnored(Attributes attributes) {
        if (attributes.getValue("alt") != null) {
            return true;
        }
        String draftValue = attributes.getValue("draft");
        if (draftValue != null) {
            return DraftType.getDefault().ordinal() > DraftType.forKeyword(draftValue).ordinal();
        }
        return false;
    }

    void pushContainer(String qName, Attributes attributes) {
        if (isIgnored(attributes) || currentContainer instanceof IgnoredContainer) {
            currentContainer = new IgnoredContainer(qName, currentContainer);
        } else {
            currentContainer = new Container(qName, currentContainer);
        }
    }

    void pushIgnoredContainer(String qName) {
        currentContainer = new IgnoredContainer(qName, currentContainer);
    }

    void pushKeyContainer(String qName, Attributes attributes, String key) {
        if (!pushIfIgnored(qName, attributes)) {
            currentContainer = new KeyContainer(qName, currentContainer, key);
        }
    }

    /**
     * start an element that defines a string entry, with the value provided by the element's text.
     */
    void pushStringEntry(String qName, Attributes attributes, String key) {
        if (!pushIfIgnored(qName, attributes)) {
            currentContainer = new StringEntry(qName, currentContainer, key);
        }
    }

    /**
     * start an element that defines an alias entry, with the value provided by the element's alias.
     */
    void pushAliasEntry(String qName, Attributes attributes, String key) {
        if (!pushIfIgnored(qName, attributes)) {
            currentContainer = new AliasEntry(qName, currentContainer, key);
        }
    }

    /**
     * start an element that defines a string entry, with the value provided by an attribute value.
     */
    void pushStringEntry(String qName, Attributes attributes, String key, String value) {
        if (!pushIfIgnored(qName, attributes)) {
            currentContainer = new StringEntry(qName, currentContainer, key, value);
        }
    }

    void pushStringArrayEntry(String qName, Attributes attributes, String key, int length) {
        if (!pushIfIgnored(qName, attributes)) {
            currentContainer = new StringArrayEntry(qName, currentContainer, key, length);
        }
    }

    void pushStringArrayElement(String qName, Attributes attributes, int index) {
        if (!pushIfIgnored(qName, attributes)) {
            currentContainer = new StringArrayElement(qName, currentContainer, index);
        }
    }

    void pushStringListEntry(String qName, Attributes attributes, String key) {
        if (!pushIfIgnored(qName, attributes)) {
            currentContainer = new StringListEntry(qName, currentContainer, key);
        }
    }

    void pushStringListElement(String qName, Attributes attributes, int index, String count) {
        if (!pushIfIgnored(qName, attributes)) {
            currentContainer = new StringListElement(qName, currentContainer, index, count);
        }
    }


    private boolean pushIfIgnored(String qName, Attributes attributes) {
        if (isIgnored(attributes) || currentContainer instanceof IgnoredContainer) {
            pushIgnoredContainer(qName);
            return true;
        }
        return false;
    }

    /**
     * Obtains the key from the innermost containing container that provides one.
     */
    String getContainerKey() {
        Container current = currentContainer;
        while (current != null) {
            if (current instanceof KeyContainer) {
                return ((KeyContainer) current).getKey();
            }
            current = current.getParent();
        }
        return null;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        currentContainer.addCharacters(ch, start, length);
    }

    @SuppressWarnings(value = "CallToThreadDumpStack")
    @Override
    public void warning(SAXParseException e) throws SAXException {
        e.printStackTrace();
    }

    @SuppressWarnings(value = "CallToThreadDumpStack")
    @Override
    public void error(SAXParseException e) throws SAXException {
        e.printStackTrace();
    }

    @SuppressWarnings(value = "CallToThreadDumpStack")
    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        e.printStackTrace();
        super.fatalError(e);
    }
}
