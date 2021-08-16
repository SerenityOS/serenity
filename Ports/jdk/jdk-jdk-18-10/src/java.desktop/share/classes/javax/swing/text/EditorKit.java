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
package javax.swing.text;

import java.io.*;
import javax.swing.Action;
import javax.swing.JEditorPane;

/**
 * Establishes the set of things needed by a text component
 * to be a reasonably functioning editor for some <em>type</em>
 * of text content.  The EditorKit acts as a factory for some
 * kind of policy.  For example, an implementation
 * of html and rtf can be provided that is replaceable
 * with other implementations.
 * <p>
 * A kit can safely store editing state as an instance
 * of the kit will be dedicated to a text component.
 * New kits will normally be created by cloning a
 * prototype kit.  The kit will have its
 * <code>setComponent</code> method called to establish
 * its relationship with a JTextComponent.
 *
 * @author  Timothy Prinzing
 */
@SuppressWarnings("serial") // Same-version serialization only
public abstract class EditorKit implements Cloneable, Serializable {

    /**
     * Construct an EditorKit.
     */
    public EditorKit() {
    }

    /**
     * Creates a copy of the editor kit.  This is implemented
     * to use <code>Object.clone()</code>.  If the kit cannot be cloned,
     * null is returned.
     *
     * @return the copy
     */
    public Object clone() {
        Object o;
        try {
            o = super.clone();
        } catch (CloneNotSupportedException cnse) {
            o = null;
        }
        return o;
    }

    /**
     * Called when the kit is being installed into the
     * a JEditorPane.
     *
     * @param c the JEditorPane
     */
    public void install(JEditorPane c) {
    }

    /**
     * Called when the kit is being removed from the
     * JEditorPane.  This is used to unregister any
     * listeners that were attached.
     *
     * @param c the JEditorPane
     */
    public void deinstall(JEditorPane c) {
    }

    /**
     * Gets the MIME type of the data that this
     * kit represents support for.
     *
     * @return the type
     */
    public abstract String getContentType();

    /**
     * Fetches a factory that is suitable for producing
     * views of any models that are produced by this
     * kit.
     *
     * @return the factory
     */
    public abstract ViewFactory getViewFactory();

    /**
     * Fetches the set of commands that can be used
     * on a text component that is using a model and
     * view produced by this kit.
     *
     * @return the set of actions
     */
    public abstract Action[] getActions();

    /**
     * Fetches a caret that can navigate through views
     * produced by the associated ViewFactory.
     *
     * @return the caret
     */
    public abstract Caret createCaret();

    /**
     * Creates an uninitialized text storage model
     * that is appropriate for this type of editor.
     *
     * @return the model
     */
    public abstract Document createDefaultDocument();

    /**
     * Inserts content from the given stream which is expected
     * to be in a format appropriate for this kind of content
     * handler.
     *
     * @param in  The stream to read from
     * @param doc The destination for the insertion.
     * @param pos The location in the document to place the
     *   content &gt;= 0.
     * @exception IOException on any I/O error
     * @exception BadLocationException if pos represents an invalid
     *   location within the document.
     */
    public abstract void read(InputStream in, Document doc, int pos)
        throws IOException, BadLocationException;

    /**
     * Writes content from a document to the given stream
     * in a format appropriate for this kind of content handler.
     *
     * @param out  The stream to write to
     * @param doc The source for the write.
     * @param pos The location in the document to fetch the
     *   content from &gt;= 0.
     * @param len The amount to write out &gt;= 0.
     * @exception IOException on any I/O error
     * @exception BadLocationException if pos represents an invalid
     *   location within the document.
     */
    public abstract void write(OutputStream out, Document doc, int pos, int len)
        throws IOException, BadLocationException;

    /**
     * Inserts content from the given stream which is expected
     * to be in a format appropriate for this kind of content
     * handler.
     * <p>
     * Since actual text editing is unicode based, this would
     * generally be the preferred way to read in the data.
     * Some types of content are stored in an 8-bit form however,
     * and will favor the InputStream.
     *
     * @param in  The stream to read from
     * @param doc The destination for the insertion.
     * @param pos The location in the document to place the
     *   content &gt;= 0.
     * @exception IOException on any I/O error
     * @exception BadLocationException if pos represents an invalid
     *   location within the document.
     */
    public abstract void read(Reader in, Document doc, int pos)
        throws IOException, BadLocationException;

    /**
     * Writes content from a document to the given stream
     * in a format appropriate for this kind of content handler.
     * <p>
     * Since actual text editing is unicode based, this would
     * generally be the preferred way to write the data.
     * Some types of content are stored in an 8-bit form however,
     * and will favor the OutputStream.
     *
     * @param out  The stream to write to
     * @param doc The source for the write.
     * @param pos The location in the document to fetch the
     *   content &gt;= 0.
     * @param len The amount to write out &gt;= 0.
     * @exception IOException on any I/O error
     * @exception BadLocationException if pos represents an invalid
     *   location within the document.
     */
    public abstract void write(Writer out, Document doc, int pos, int len)
        throws IOException, BadLocationException;

}
