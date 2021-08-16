/*
 * Copyright (c) 2000, 2005, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.transform.sax;

import javax.xml.transform.Result;

import org.xml.sax.ContentHandler;
import org.xml.sax.ext.LexicalHandler;

/**
 * <p>Acts as an holder for a transformation Result.</p>
 *
 * @author Jeff Suttor
 * @since 1.4
 */
public class SAXResult implements Result {

    /**
     * If {@link javax.xml.transform.TransformerFactory#getFeature}
     * returns true when passed this value as an argument,
     * the Transformer supports Result output of this type.
     */
    public static final String FEATURE =
        "http://javax.xml.transform.sax.SAXResult/feature";

    /**
     * Zero-argument default constructor.
     */
    public SAXResult() {
    }

    /**
     * Create a SAXResult that targets a SAX2 {@link org.xml.sax.ContentHandler}.
     *
     * @param handler Must be a non-null ContentHandler reference.
     */
    public SAXResult(ContentHandler handler) {
        setHandler(handler);
    }

    /**
     * Set the target to be a SAX2 {@link org.xml.sax.ContentHandler}.
     *
     * @param handler Must be a non-null ContentHandler reference.
     */
    public void setHandler(ContentHandler handler) {
        this.handler = handler;
    }

    /**
     * Get the {@link org.xml.sax.ContentHandler} that is the Result.
     *
     * @return The ContentHandler that is to be transformation output.
     */
    public ContentHandler getHandler() {
        return handler;
    }

    /**
     * Set the SAX2 {@link org.xml.sax.ext.LexicalHandler} for the output.
     *
     * <p>This is needed to handle XML comments and the like.  If the
     * lexical handler is not set, an attempt should be made by the
     * transformer to cast the {@link org.xml.sax.ContentHandler} to a
     * <code>LexicalHandler</code>.</p>
     *
     * @param handler A non-null <code>LexicalHandler</code> for
     * handling lexical parse events.
     */
    public void setLexicalHandler(LexicalHandler handler) {
        this.lexhandler = handler;
    }

    /**
     * Get a SAX2 {@link org.xml.sax.ext.LexicalHandler} for the output.
     *
     * @return A <code>LexicalHandler</code>, or null.
     */
    public LexicalHandler getLexicalHandler() {
        return lexhandler;
    }

    /**
     * Method setSystemId Set the systemID that may be used in association
     * with the {@link org.xml.sax.ContentHandler}.
     *
     * @param systemId The system identifier as a URI string.
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Get the system identifier that was set with setSystemId.
     *
     * @return The system identifier that was set with setSystemId, or null
     * if setSystemId was not called.
     */
    public String getSystemId() {
        return systemId;
    }

    //////////////////////////////////////////////////////////////////////
    // Internal state.
    //////////////////////////////////////////////////////////////////////

    /**
     * The handler for parse events.
     */
    private ContentHandler handler;

    /**
     * The handler for lexical events.
     */
    private LexicalHandler lexhandler;

    /**
     * The systemID that may be used in association
     * with the node.
     */
    private String systemId;
}
