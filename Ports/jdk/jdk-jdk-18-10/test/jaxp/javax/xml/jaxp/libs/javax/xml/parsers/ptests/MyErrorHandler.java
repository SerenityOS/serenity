/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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
package javax.xml.parsers.ptests;

import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Customized DefaultHandler used for SAXParseException testing.
 */
class MyErrorHandler extends DefaultHandler {
    /**
     * Flag whether any event was received.
     */
    private volatile boolean errorOccured;

    /**
     * Set no event received on constructor.
     */
    private MyErrorHandler() {
        errorOccured = false;
    }

    /**
     * Factory method to create a MyErrorHandler instance.
     * @return a MyErrorHandler instance.
     */
    public static MyErrorHandler newInstance() {
        return new MyErrorHandler();
    }

    /**
     * Receive notification of a recoverable error.
     * @param e a recoverable parser exception error.
     */
    @Override
    public void error(SAXParseException e) {
        errorOccured = true;
    }

    /**
     * Receive notification of a parser warning.
     * @param e a parser warning  event.
     */
    @Override
    public void warning(SAXParseException e) {
        errorOccured = true;
    }

    /**
     * Report a fatal XML parsing error.
     * @param e The error information encoded as an exception.
     */
    @Override
    public void fatalError(SAXParseException e) {
        errorOccured = true;
    }

    /**
     * Has any event been received.
     *
     * @return true if any event has been received.
     *         false if no event has been received.
     */
    public boolean isErrorOccured() {
        return errorOccured;
    }
}
