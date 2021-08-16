/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.rtf;

import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.Action;
import javax.swing.text.*;
import javax.swing.*;

/**
 * This is the default implementation of RTF editing
 * functionality.  The RTF support was not written by the
 * Swing team.  In the future we hope to improve the support
 * provided.
 *
 * @author  Timothy Prinzing (of this class, not the package!)
 */
@SuppressWarnings("serial") // Same-version serialization only
public class RTFEditorKit extends StyledEditorKit {

    /**
     * Constructs an RTFEditorKit.
     */
    public RTFEditorKit() {
        super();
    }

    /**
     * Get the MIME type of the data that this
     * kit represents support for.  This kit supports
     * the type <code>text/rtf</code>.
     *
     * @return the type
     */
    public String getContentType() {
        return "text/rtf";
    }

    /**
     * Insert content from the given stream which is expected
     * to be in a format appropriate for this kind of content
     * handler.
     *
     * @param in  The stream to read from
     * @param doc The destination for the insertion.
     * @param pos The location in the document to place the
     *   content.
     * @exception IOException on any I/O error
     * @exception BadLocationException if pos represents an invalid
     *   location within the document.
     */
    public void read(InputStream in, Document doc, int pos) throws IOException, BadLocationException {

        if (doc instanceof StyledDocument) {
            // PENDING(prinz) this needs to be fixed to
            // insert to the given position.
            RTFReader rdr = new RTFReader((StyledDocument) doc);
            rdr.readFromStream(in);
            rdr.close();
        } else {
            // treat as text/plain
            super.read(in, doc, pos);
        }
    }

    /**
     * Write content from a document to the given stream
     * in a format appropriate for this kind of content handler.
     *
     * @param out  The stream to write to
     * @param doc The source for the write.
     * @param pos The location in the document to fetch the
     *   content.
     * @param len The amount to write out.
     * @exception IOException on any I/O error
     * @exception BadLocationException if pos represents an invalid
     *   location within the document.
     */
    public void write(OutputStream out, Document doc, int pos, int len)
        throws IOException, BadLocationException {

            // PENDING(prinz) this needs to be fixed to
            // use the given document range.
            RTFGenerator.writeDocument(doc, out);
    }

    /**
     * Insert content from the given stream, which will be
     * treated as plain text.
     *
     * @param in  The stream to read from
     * @param doc The destination for the insertion.
     * @param pos The location in the document to place the
     *   content.
     * @exception IOException on any I/O error
     * @exception BadLocationException if pos represents an invalid
     *   location within the document.
     */
    public void read(Reader in, Document doc, int pos)
        throws IOException, BadLocationException {

        if (doc instanceof StyledDocument) {
            RTFReader rdr = new RTFReader((StyledDocument) doc);
            rdr.readFromReader(in);
            rdr.close();
        } else {
            // treat as text/plain
            super.read(in, doc, pos);
        }
    }

    /**
     * Write content from a document to the given stream
     * as plain text.
     *
     * @param out  The stream to write to
     * @param doc The source for the write.
     * @param pos The location in the document to fetch the
     *   content.
     * @param len The amount to write out.
     * @exception IOException on any I/O error
     * @exception BadLocationException if pos represents an invalid
     *   location within the document.
     */
    public void write(Writer out, Document doc, int pos, int len)
        throws IOException, BadLocationException {

        throw new IOException("RTF is an 8-bit format");
    }

}
