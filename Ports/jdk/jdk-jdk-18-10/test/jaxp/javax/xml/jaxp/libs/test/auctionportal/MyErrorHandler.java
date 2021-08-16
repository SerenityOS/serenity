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

import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * ErrorHandler for error handling. Set state if any method in error, warning
 * or fatalError was called.
 */
public final class MyErrorHandler extends DefaultHandler {
    /**
     * Enumeration for ErrorHandler's state.
     */
    private enum STATE { ERROR, FATAL, WARNING, NORMAL};

    /**
     * Set state as normal by default.
     */
    private volatile STATE state = STATE.NORMAL;

    /**
     * Keep exception for further investigation.
     */
    private volatile SAXParseException exception;

    /**
     * Save exception and set state to ERROR.
     * @param e exception wrap error.
     */
    @Override
    public void error (SAXParseException e) {
        state = STATE.ERROR;
        exception = e;
    }

    /**
     * Save exception and set state to FATAL.
     * @param e exception wrap error.
     */
    @Override
    public void fatalError (SAXParseException e) {
        state = STATE.FATAL;
        exception = e;
    }

    /**
     * Save exception and set state to WARNING.
     * @param e exception wrap error.
     */
    @Override
    public void warning (SAXParseException e) {
        state = STATE.WARNING;
        exception = e;
    }

    /**
     * return ErrorHandle's state .
     * @return true No error, fatalError and warning.
     *         false there is any error, fatalError or warning in processing.
     */
    public boolean isAnyError() {
        if (state != STATE.NORMAL)
            System.out.println(exception);
        return state != STATE.NORMAL;
    }

    /**
     * return whether fatalError is the only error.
     * @return true fatalError is the only error.
     *         false there is no error, or other error besides fatalError.
     */
    public boolean isFatalError() {
        if (state == STATE.FATAL)
            System.out.println(exception);
        return state == STATE.FATAL;
    }

}
