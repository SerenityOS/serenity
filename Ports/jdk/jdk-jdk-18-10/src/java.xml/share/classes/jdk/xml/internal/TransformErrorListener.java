/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.xml.internal;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

/**
 * Implements an ErrorListener for use by the JDK as the default ErrorListener.
 * For compatibility, this implementation retains the behavior as was implemented
 * by TransformerFactoryImpl and TransformerImpl where both the error and
 * fatalError methods re-throw the exception.
 */
public class TransformErrorListener implements ErrorListener {
    /**
     * Receives notification of a warning.
     *
     * @param e The warning information encapsulated in a TransformerException.
     * @throws TransformerException not thrown in this implementation
     */
    @Override
    public void warning(TransformerException e)
        throws TransformerException
    {
        // no op
    }

    /**
     * Receives notification of an error.
     * The transformer may continue the process if the error is recoverable.
     * It may decide not to if it can not continue after the error.
     *
     * @param e The error information encapsulated in a TransformerException.
     * @throws TransformerException re-throws the exception.
     */
    @Override
    public void error(TransformerException e)
        throws TransformerException
    {
        throw e;
    }

    /**
     * Receives notification of a fatal error.
     *
     * @param e The error information encapsulated in a TransformerException.
     * @throws TransformerException re-throws the exception.
     */
    @Override
    public void fatalError(TransformerException e)
        throws TransformerException
    {
        throw e;
    }
}
