/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xalan.internal.xsltc.runtime;

import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.serializer.EmptySerializer;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 */
public final class StringValueHandler extends EmptySerializer {

    private StringBuilder _buffer = new StringBuilder();
    private String _str = null;
    private static final String EMPTY_STR = "";
    private boolean m_escaping = false;
    private int _nestedLevel = 0;

    public void characters(char[] ch, int off, int len)
        throws SAXException
    {
        if (_nestedLevel > 0)
            return;

        if (_str != null) {
            _buffer.append(_str);
            _str = null;
        }
        _buffer.append(ch, off, len);
    }

    public String getValue() {
        if (_buffer.length() != 0) {
            String result = _buffer.toString();
            _buffer.setLength(0);
            return result;
        }
        else {
            String result = _str;
            _str = null;
            return (result != null) ? result : EMPTY_STR;
        }
    }

    public void characters(String characters) throws SAXException {
        if (_nestedLevel > 0)
            return;

        if (_str == null && _buffer.length() == 0) {
            _str = characters;
        }
        else {
            if (_str != null) {
                _buffer.append(_str);
                _str = null;
            }

            _buffer.append(characters);
        }
    }

    public void startElement(String qname) throws SAXException {
        _nestedLevel++;
    }

    public void endElement(String qname) throws SAXException {
        _nestedLevel--;
    }

    // Override the setEscaping method just to indicate that this class is
    // aware that that method might be called.
    public boolean setEscaping(boolean bool) {
        boolean oldEscaping = m_escaping;
        m_escaping = bool;

        return bool;
    }

    /**
     * The value of a PI must not contain the substring "?>". Should
     * that substring be present, replace it by "? >".
     */
    public String getValueOfPI() {
        final String value = getValue();

        if (value.indexOf("?>") > 0) {
            final int n = value.length();
            final StringBuilder valueOfPI = new StringBuilder();

            for (int i = 0; i < n;) {
                final char ch = value.charAt(i++);
                if (ch == '?' && value.charAt(i) == '>') {
                    valueOfPI.append("? >"); i++;
                }
                else {
                    valueOfPI.append(ch);
                }
            }
            return valueOfPI.toString();
        }
        return value;
    }
}
