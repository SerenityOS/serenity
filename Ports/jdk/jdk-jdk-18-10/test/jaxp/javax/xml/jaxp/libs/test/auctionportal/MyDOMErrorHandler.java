/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
package test.auctionportal;

import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMError;

/**
 * Error handler for recording DOM processing error.
 */
public class MyDOMErrorHandler implements DOMErrorHandler {
    /**
     * flag shows if there is any error.
     */
    private volatile boolean errorOccured = false;

    /**
     * Set errorOcurred to true when an error occurs.
     * @param error The error object that describes the error. This object
     * may be reused by the DOM implementation across multiple calls to
     * the handleError method.
     * @return true that processing may continue depending on.
     */
    @Override
    public boolean handleError (DOMError error) {
        System.err.println( "ERROR" + error.getMessage());
        System.err.println( "ERROR" + error.getRelatedData());
        errorOccured = true;
        return true;
    }

    /**
     * Showing if any error was handled.
     * @return true if there is one or more error.
     *         false no error occurs.
     */
    public boolean isError() {
        return errorOccured;
    }
}
