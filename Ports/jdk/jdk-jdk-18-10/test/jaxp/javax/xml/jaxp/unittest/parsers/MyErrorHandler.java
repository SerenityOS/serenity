/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package parsers;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class MyErrorHandler extends DefaultHandler {

    public boolean errorOccured = false;

    public void error(SAXParseException e) throws SAXException {

        System.err.println("Error: " + "[[" + e.getPublicId() + "]" + "[" + e.getSystemId() + "]]" + "[[" + e.getLineNumber() + "]" + "[" + e.getColumnNumber()
                + "]] " + e);

        errorOccured = true;
    }

    public void fatalError(SAXParseException e) throws SAXException {

        System.err.println("Fatal Error: " + e);

        errorOccured = true;
    }

    public void warning(SAXParseException e) throws SAXException {

        System.err.println("Warning: " + e);

        errorOccured = true;
    }
}
