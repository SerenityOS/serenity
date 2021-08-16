/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.html;

import java.awt.font.TextAttribute;
import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import sun.swing.SwingUtilities2;
import static sun.swing.SwingUtilities2.IMPLIED_CR;

/**
 * A document that models HTML.  The purpose of this model is to
 * support both browsing and editing.  As a result, the structure
 * described by an HTML document is not exactly replicated by default.
 * The element structure that is modeled by default, is built by the
 * class <code>HTMLDocument.HTMLReader</code>, which implements the
 * <code>HTMLEditorKit.ParserCallback</code> protocol that the parser
 * expects.  To change the structure one can subclass
 * <code>HTMLReader</code>, and reimplement the method {@link
 * #getReader(int)} to return the new reader implementation.  The
 * documentation for <code>HTMLReader</code> should be consulted for
 * the details of the default structure created.  The intent is that
 * the document be non-lossy (although reproducing the HTML format may
 * result in a different format).
 *
 * <p>The document models only HTML, and makes no attempt to store
 * view attributes in it.  The elements are identified by the
 * <code>StyleContext.NameAttribute</code> attribute, which should
 * always have a value of type <code>HTML.Tag</code> that identifies
 * the kind of element.  Some of the elements (such as comments) are
 * synthesized.  The <code>HTMLFactory</code> uses this attribute to
 * determine what kind of view to build.</p>
 *
 * <p>This document supports incremental loading.  The
 * <code>TokenThreshold</code> property controls how much of the parse
 * is buffered before trying to update the element structure of the
 * document.  This property is set by the <code>EditorKit</code> so
 * that subclasses can disable it.</p>
 *
 * <p>The <code>Base</code> property determines the URL against which
 * relative URLs are resolved.  By default, this will be the
 * <code>Document.StreamDescriptionProperty</code> if the value of the
 * property is a URL.  If a &lt;BASE&gt; tag is encountered, the base
 * will become the URL specified by that tag.  Because the base URL is
 * a property, it can of course be set directly.</p>
 *
 * <p>The default content storage mechanism for this document is a gap
 * buffer (<code>GapContent</code>).  Alternatives can be supplied by
 * using the constructor that takes a <code>Content</code>
 * implementation.</p>
 *
 * <h2>Modifying HTMLDocument</h2>
 *
 * <p>In addition to the methods provided by Document and
 * StyledDocument for mutating an HTMLDocument, HTMLDocument provides
 * a number of convenience methods.  The following methods can be used
 * to insert HTML content into an existing document.</p>
 *
 * <ul>
 *   <li>{@link #setInnerHTML(Element, String)}</li>
 *   <li>{@link #setOuterHTML(Element, String)}</li>
 *   <li>{@link #insertBeforeStart(Element, String)}</li>
 *   <li>{@link #insertAfterStart(Element, String)}</li>
 *   <li>{@link #insertBeforeEnd(Element, String)}</li>
 *   <li>{@link #insertAfterEnd(Element, String)}</li>
 * </ul>
 *
 * <p>The following examples illustrate using these methods.  Each
 * example assumes the HTML document is initialized in the following
 * way:</p>
 *
 * <pre>
 * JEditorPane p = new JEditorPane();
 * p.setContentType("text/html");
 * p.setText("..."); // Document text is provided below.
 * HTMLDocument d = (HTMLDocument) p.getDocument();
 * </pre>
 *
 * <p>With the following HTML content:</p>
 *
 * <pre>
 * &lt;html&gt;
 *   &lt;head&gt;
 *     &lt;title&gt;An example HTMLDocument&lt;/title&gt;
 *     &lt;style type="text/css"&gt;
 *       div { background-color: silver; }
 *       ul { color: blue; }
 *     &lt;/style&gt;
 *   &lt;/head&gt;
 *   &lt;body&gt;
 *     &lt;div id="BOX"&gt;
 *       &lt;p&gt;Paragraph 1&lt;/p&gt;
 *       &lt;p&gt;Paragraph 2&lt;/p&gt;
 *     &lt;/div&gt;
 *   &lt;/body&gt;
 * &lt;/html&gt;
 * </pre>
 *
 * <p>All the methods for modifying an HTML document require an {@link
 * Element}.  Elements can be obtained from an HTML document by using
 * the method {@link #getElement(Element e, Object attribute, Object
 * value)}.  It returns the first descendant element that contains the
 * specified attribute with the given value, in depth-first order.
 * For example, <code>d.getElement(d.getDefaultRootElement(),
 * StyleConstants.NameAttribute, HTML.Tag.P)</code> returns the first
 * paragraph element.</p>
 *
 * <p>A convenient shortcut for locating elements is the method {@link
 * #getElement(String)}; returns an element whose <code>ID</code>
 * attribute matches the specified value.  For example,
 * <code>d.getElement("BOX")</code> returns the <code>DIV</code>
 * element.</p>
 *
 * <p>The {@link #getIterator(HTML.Tag t)} method can also be used for
 * finding all occurrences of the specified HTML tag in the
 * document.</p>
 *
 * <h3>Inserting elements</h3>
 *
 * <p>Elements can be inserted before or after the existing children
 * of any non-leaf element by using the methods
 * <code>insertAfterStart</code> and <code>insertBeforeEnd</code>.
 * For example, if <code>e</code> is the <code>DIV</code> element,
 * <code>d.insertAfterStart(e, "&lt;ul&gt;&lt;li&gt;List
 * Item&lt;/li&gt;&lt;/ul&gt;")</code> inserts the list before the first
 * paragraph, and <code>d.insertBeforeEnd(e, "&lt;ul&gt;&lt;li&gt;List
 * Item&lt;/li&gt;&lt;/ul&gt;")</code> inserts the list after the last
 * paragraph.  The <code>DIV</code> block becomes the parent of the
 * newly inserted elements.</p>
 *
 * <p>Sibling elements can be inserted before or after any element by
 * using the methods <code>insertBeforeStart</code> and
 * <code>insertAfterEnd</code>.  For example, if <code>e</code> is the
 * <code>DIV</code> element, <code>d.insertBeforeStart(e,
 * "&lt;ul&gt;&lt;li&gt;List Item&lt;/li&gt;&lt;/ul&gt;")</code> inserts the list
 * before the <code>DIV</code> element, and <code>d.insertAfterEnd(e,
 * "&lt;ul&gt;&lt;li&gt;List Item&lt;/li&gt;&lt;/ul&gt;")</code> inserts the list
 * after the <code>DIV</code> element.  The newly inserted elements
 * become siblings of the <code>DIV</code> element.</p>
 *
 * <h3>Replacing elements</h3>
 *
 * <p>Elements and all their descendants can be replaced by using the
 * methods <code>setInnerHTML</code> and <code>setOuterHTML</code>.
 * For example, if <code>e</code> is the <code>DIV</code> element,
 * <code>d.setInnerHTML(e, "&lt;ul&gt;&lt;li&gt;List
 * Item&lt;/li&gt;&lt;/ul&gt;")</code> replaces all children paragraphs with
 * the list, and <code>d.setOuterHTML(e, "&lt;ul&gt;&lt;li&gt;List
 * Item&lt;/li&gt;&lt;/ul&gt;")</code> replaces the <code>DIV</code> element
 * itself.  In latter case the parent of the list is the
 * <code>BODY</code> element.
 *
 * <h3>Summary</h3>
 *
 * <p>The following table shows the example document and the results
 * of various methods described above.</p>
 *
 * <table class="plain">
 * <caption>HTML Content of example above</caption>
 *   <tr>
 *     <th>Example</th>
 *     <th><code>insertAfterStart</code></th>
 *     <th><code>insertBeforeEnd</code></th>
 *     <th><code>insertBeforeStart</code></th>
 *     <th><code>insertAfterEnd</code></th>
 *     <th><code>setInnerHTML</code></th>
 *     <th><code>setOuterHTML</code></th>
 *   </tr>
 *   <tr style="vertical-align:top">
 *     <td style="white-space:nowrap">
 *       <div style="background-color: silver;">
 *         <p>Paragraph 1</p>
 *         <p>Paragraph 2</p>
 *       </div>
 *     </td>
 * <!--insertAfterStart-->
 *     <td style="white-space:nowrap">
 *       <div style="background-color: silver;">
 *         <ul style="color: blue;">
 *           <li>List Item</li>
 *         </ul>
 *         <p>Paragraph 1</p>
 *         <p>Paragraph 2</p>
 *       </div>
 *     </td>
 * <!--insertBeforeEnd-->
 *     <td style="white-space:nowrap">
 *       <div style="background-color: silver;">
 *         <p>Paragraph 1</p>
 *         <p>Paragraph 2</p>
 *         <ul style="color: blue;">
 *           <li>List Item</li>
 *         </ul>
 *       </div>
 *     </td>
 * <!--insertBeforeStart-->
 *     <td style="white-space:nowrap">
 *       <ul style="color: blue;">
 *         <li>List Item</li>
 *       </ul>
 *       <div style="background-color: silver;">
 *         <p>Paragraph 1</p>
 *         <p>Paragraph 2</p>
 *       </div>
 *     </td>
 * <!--insertAfterEnd-->
 *     <td style="white-space:nowrap">
 *       <div style="background-color: silver;">
 *         <p>Paragraph 1</p>
 *         <p>Paragraph 2</p>
 *       </div>
 *       <ul style="color: blue;">
 *         <li>List Item</li>
 *       </ul>
 *     </td>
 * <!--setInnerHTML-->
 *     <td style="white-space:nowrap">
 *       <div style="background-color: silver;">
 *         <ul style="color: blue;">
 *           <li>List Item</li>
 *         </ul>
 *       </div>
 *     </td>
 * <!--setOuterHTML-->
 *     <td style="white-space:nowrap">
 *       <ul style="color: blue;">
 *         <li>List Item</li>
 *       </ul>
 *     </td>
 *   </tr>
 * </table>
 *
 * <p><strong>Warning:</strong> Serialized objects of this class will
 * not be compatible with future Swing releases. The current
 * serialization support is appropriate for short term storage or RMI
 * between applications running the same version of Swing.  As of 1.4,
 * support for long term storage of all JavaBeans
 * has been added to the
 * <code>java.beans</code> package.  Please see {@link
 * java.beans.XMLEncoder}.</p>
 *
 * @author  Timothy Prinzing
 * @author  Scott Violet
 * @author  Sunita Mani
 */
@SuppressWarnings("serial") // Same-version serialization only
public class HTMLDocument extends DefaultStyledDocument {
    /**
     * Constructs an HTML document using the default buffer size
     * and a default <code>StyleSheet</code>.  This is a convenience
     * method for the constructor
     * <code>HTMLDocument(Content, StyleSheet)</code>.
     */
    public HTMLDocument() {
        this(new GapContent(BUFFER_SIZE_DEFAULT), new StyleSheet());
    }

    /**
     * Constructs an HTML document with the default content
     * storage implementation and the specified style/attribute
     * storage mechanism.  This is a convenience method for the
     * constructor
     * <code>HTMLDocument(Content, StyleSheet)</code>.
     *
     * @param styles  the styles
     */
    public HTMLDocument(StyleSheet styles) {
        this(new GapContent(BUFFER_SIZE_DEFAULT), styles);
    }

    /**
     * Constructs an HTML document with the given content
     * storage implementation and the given style/attribute
     * storage mechanism.
     *
     * @param c  the container for the content
     * @param styles the styles
     */
    public HTMLDocument(Content c, StyleSheet styles) {
        super(c, styles);
    }

    /**
     * Fetches the reader for the parser to use when loading the document
     * with HTML.  This is implemented to return an instance of
     * <code>HTMLDocument.HTMLReader</code>.
     * Subclasses can reimplement this
     * method to change how the document gets structured if desired.
     * (For example, to handle custom tags, or structurally represent character
     * style elements.)
     *
     * @param pos the starting position
     * @return the reader used by the parser to load the document
     */
    public HTMLEditorKit.ParserCallback getReader(int pos) {
        Object desc = getProperty(Document.StreamDescriptionProperty);
        if (desc instanceof URL) {
            setBase((URL)desc);
        }
        HTMLReader reader = new HTMLReader(pos);
        return reader;
    }

    /**
     * Returns the reader for the parser to use to load the document
     * with HTML.  This is implemented to return an instance of
     * <code>HTMLDocument.HTMLReader</code>.
     * Subclasses can reimplement this
     * method to change how the document gets structured if desired.
     * (For example, to handle custom tags, or structurally represent character
     * style elements.)
     * <p>This is a convenience method for
     * <code>getReader(int, int, int, HTML.Tag, TRUE)</code>.
     *
     * @param pos the starting position
     * @param popDepth   the number of <code>ElementSpec.EndTagTypes</code>
     *          to generate before inserting
     * @param pushDepth  the number of <code>ElementSpec.StartTagTypes</code>
     *          with a direction of <code>ElementSpec.JoinNextDirection</code>
     *          that should be generated before inserting,
     *          but after the end tags have been generated
     * @param insertTag  the first tag to start inserting into document
     * @return the reader used by the parser to load the document
     */
    public HTMLEditorKit.ParserCallback getReader(int pos, int popDepth,
                                                  int pushDepth,
                                                  HTML.Tag insertTag) {
        return getReader(pos, popDepth, pushDepth, insertTag, true);
    }

    /**
     * Fetches the reader for the parser to use to load the document
     * with HTML.  This is implemented to return an instance of
     * HTMLDocument.HTMLReader.  Subclasses can reimplement this
     * method to change how the document get structured if desired
     * (e.g. to handle custom tags, structurally represent character
     * style elements, etc.).
     *
     * @param popDepth   the number of <code>ElementSpec.EndTagTypes</code>
     *          to generate before inserting
     * @param pushDepth  the number of <code>ElementSpec.StartTagTypes</code>
     *          with a direction of <code>ElementSpec.JoinNextDirection</code>
     *          that should be generated before inserting,
     *          but after the end tags have been generated
     * @param insertTag  the first tag to start inserting into document
     * @param insertInsertTag  false if all the Elements after insertTag should
     *        be inserted; otherwise insertTag will be inserted
     * @return the reader used by the parser to load the document
     */
    HTMLEditorKit.ParserCallback getReader(int pos, int popDepth,
                                           int pushDepth,
                                           HTML.Tag insertTag,
                                           boolean insertInsertTag) {
        Object desc = getProperty(Document.StreamDescriptionProperty);
        if (desc instanceof URL) {
            setBase((URL)desc);
        }
        HTMLReader reader = new HTMLReader(pos, popDepth, pushDepth,
                                           insertTag, insertInsertTag, false,
                                           true);
        return reader;
    }

    /**
     * Returns the location to resolve relative URLs against.  By
     * default this will be the document's URL if the document
     * was loaded from a URL.  If a base tag is found and
     * can be parsed, it will be used as the base location.
     *
     * @return the base location
     */
    public URL getBase() {
        return base;
    }

    /**
     * Sets the location to resolve relative URLs against.  By
     * default this will be the document's URL if the document
     * was loaded from a URL.  If a base tag is found and
     * can be parsed, it will be used as the base location.
     * <p>This also sets the base of the <code>StyleSheet</code>
     * to be <code>u</code> as well as the base of the document.
     *
     * @param u  the desired base URL
     */
    public void setBase(URL u) {
        base = u;
        getStyleSheet().setBase(u);
    }

    /**
     * Inserts new elements in bulk.  This is how elements get created
     * in the document.  The parsing determines what structure is needed
     * and creates the specification as a set of tokens that describe the
     * edit while leaving the document free of a write-lock.  This method
     * can then be called in bursts by the reader to acquire a write-lock
     * for a shorter duration (i.e. while the document is actually being
     * altered).
     *
     * @param offset the starting offset
     * @param data the element data
     * @exception BadLocationException  if the given position does not
     *   represent a valid location in the associated document.
     */
    protected void insert(int offset, ElementSpec[] data) throws BadLocationException {
        super.insert(offset, data);
    }

    /**
     * Updates document structure as a result of text insertion.  This
     * will happen within a write lock.  This implementation simply
     * parses the inserted content for line breaks and builds up a set
     * of instructions for the element buffer.
     *
     * @param chng a description of the document change
     * @param attr the attributes
     */
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
        if(attr == null) {
            attr = contentAttributeSet;
        }

        // If this is the composed text element, merge the content attribute to it
        else if (attr.isDefined(StyleConstants.ComposedTextAttribute)) {
            ((MutableAttributeSet)attr).addAttributes(contentAttributeSet);
        }

        if (attr.isDefined(IMPLIED_CR)) {
            ((MutableAttributeSet)attr).removeAttribute(IMPLIED_CR);
        }

        super.insertUpdate(chng, attr);
    }

    /**
     * Replaces the contents of the document with the given
     * element specifications.  This is called before insert if
     * the loading is done in bursts.  This is the only method called
     * if loading the document entirely in one burst.
     *
     * @param data  the new contents of the document
     */
    protected void create(ElementSpec[] data) {
        super.create(data);
    }

    /**
     * Sets attributes for a paragraph.
     * <p>
     * This method is thread safe, although most Swing methods
     * are not. Please see
     * <A HREF="https://docs.oracle.com/javase/tutorial/uiswing/concurrency/index.html">Concurrency
     * in Swing</A> for more information.
     *
     * @param offset the offset into the paragraph (must be at least 0)
     * @param length the number of characters affected (must be at least 0)
     * @param s the attributes
     * @param replace whether to replace existing attributes, or merge them
     */
    public void setParagraphAttributes(int offset, int length, AttributeSet s,
                                       boolean replace) {
        try {
            writeLock();
            // Make sure we send out a change for the length of the paragraph.
            int end = Math.min(offset + length, getLength());
            Element e = getParagraphElement(offset);
            offset = e.getStartOffset();
            e = getParagraphElement(end);
            length = Math.max(0, e.getEndOffset() - offset);
            DefaultDocumentEvent changes =
                new DefaultDocumentEvent(offset, length,
                                         DocumentEvent.EventType.CHANGE);
            AttributeSet sCopy = s.copyAttributes();
            int lastEnd = Integer.MAX_VALUE;
            for (int pos = offset; pos <= end; pos = lastEnd) {
                Element paragraph = getParagraphElement(pos);
                if (lastEnd == paragraph.getEndOffset()) {
                    lastEnd++;
                }
                else {
                    lastEnd = paragraph.getEndOffset();
                }
                MutableAttributeSet attr =
                    (MutableAttributeSet) paragraph.getAttributes();
                changes.addEdit(new AttributeUndoableEdit(paragraph, sCopy, replace));
                if (replace) {
                    attr.removeAttributes(attr);
                }
                attr.addAttributes(s);
            }
            changes.end();
            fireChangedUpdate(changes);
            fireUndoableEditUpdate(new UndoableEditEvent(this, changes));
        } finally {
            writeUnlock();
        }
    }

    /**
     * Fetches the <code>StyleSheet</code> with the document-specific display
     * rules (CSS) that were specified in the HTML document itself.
     *
     * @return the <code>StyleSheet</code>
     */
    public StyleSheet getStyleSheet() {
        return (StyleSheet) getAttributeContext();
    }

    /**
     * Fetches an iterator for the specified HTML tag.
     * This can be used for things like iterating over the
     * set of anchors contained, or iterating over the input
     * elements.
     *
     * @param t the requested <code>HTML.Tag</code>
     * @return the <code>Iterator</code> for the given HTML tag
     * @see javax.swing.text.html.HTML.Tag
     */
    public Iterator getIterator(HTML.Tag t) {
        if (t.isBlock()) {
            // TBD
            return null;
        }
        return new LeafIterator(t, this);
    }

    /**
     * Creates a document leaf element that directly represents
     * text (doesn't have any children).  This is implemented
     * to return an element of type
     * <code>HTMLDocument.RunElement</code>.
     *
     * @param parent the parent element
     * @param a the attributes for the element
     * @param p0 the beginning of the range (must be at least 0)
     * @param p1 the end of the range (must be at least p0)
     * @return the new element
     */
    protected Element createLeafElement(Element parent, AttributeSet a, int p0, int p1) {
        return new RunElement(parent, a, p0, p1);
    }

    /**
     * Creates a document branch element, that can contain other elements.
     * This is implemented to return an element of type
     * <code>HTMLDocument.BlockElement</code>.
     *
     * @param parent the parent element
     * @param a the attributes
     * @return the element
     */
    protected Element createBranchElement(Element parent, AttributeSet a) {
        return new BlockElement(parent, a);
    }

    /**
     * Creates the root element to be used to represent the
     * default document structure.
     *
     * @return the element base
     */
    protected AbstractElement createDefaultRoot() {
        // grabs a write-lock for this initialization and
        // abandon it during initialization so in normal
        // operation we can detect an illegitimate attempt
        // to mutate attributes.
        writeLock();
        MutableAttributeSet a = new SimpleAttributeSet();
        a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.HTML);
        BlockElement html = new BlockElement(null, a.copyAttributes());
        a.removeAttributes(a);
        a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.BODY);
        BlockElement body = new BlockElement(html, a.copyAttributes());
        a.removeAttributes(a);
        a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.P);
        getStyleSheet().addCSSAttributeFromHTML(a, CSS.Attribute.MARGIN_TOP, "0");
        BlockElement paragraph = new BlockElement(body, a.copyAttributes());
        a.removeAttributes(a);
        a.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
        RunElement brk = new RunElement(paragraph, a, 0, 1);
        Element[] buff = new Element[1];
        buff[0] = brk;
        paragraph.replace(0, 0, buff);
        buff[0] = paragraph;
        body.replace(0, 0, buff);
        buff[0] = body;
        html.replace(0, 0, buff);
        writeUnlock();
        return html;
    }

    /**
     * Sets the number of tokens to buffer before trying to update
     * the documents element structure.
     *
     * @param n  the number of tokens to buffer
     */
    public void setTokenThreshold(int n) {
        putProperty(TokenThreshold, n);
    }

    /**
     * Gets the number of tokens to buffer before trying to update
     * the documents element structure.  The default value is
     * <code>Integer.MAX_VALUE</code>.
     *
     * @return the number of tokens to buffer
     */
    public int getTokenThreshold() {
        Integer i = (Integer) getProperty(TokenThreshold);
        if (i != null) {
            return i.intValue();
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Determines how unknown tags are handled by the parser.
     * If set to true, unknown
     * tags are put in the model, otherwise they are dropped.
     *
     * @param preservesTags  true if unknown tags should be
     *          saved in the model, otherwise tags are dropped
     * @see javax.swing.text.html.HTML.Tag
     */
    public void setPreservesUnknownTags(boolean preservesTags) {
        preservesUnknownTags = preservesTags;
    }

    /**
     * Returns the behavior the parser observes when encountering
     * unknown tags.
     *
     * @see javax.swing.text.html.HTML.Tag
     * @return true if unknown tags are to be preserved when parsing
     */
    public boolean getPreservesUnknownTags() {
        return preservesUnknownTags;
    }

    /**
     * Processes <code>HyperlinkEvents</code> that
     * are generated by documents in an HTML frame.
     * The <code>HyperlinkEvent</code> type, as the parameter suggests,
     * is <code>HTMLFrameHyperlinkEvent</code>.
     * In addition to the typical information contained in a
     * <code>HyperlinkEvent</code>,
     * this event contains the element that corresponds to the frame in
     * which the click happened (the source element) and the
     * target name.  The target name has 4 possible values:
     * <ul>
     * <li>  _self
     * <li>  _parent
     * <li>  _top
     * <li>  a named frame
     * </ul>
     *
     * If target is _self, the action is to change the value of the
     * <code>HTML.Attribute.SRC</code> attribute and fires a
     * <code>ChangedUpdate</code> event.
     *<p>
     * If the target is _parent, then it deletes the parent element,
     * which is a &lt;FRAMESET&gt; element, and inserts a new &lt;FRAME&gt;
     * element, and sets its <code>HTML.Attribute.SRC</code> attribute
     * to have a value equal to the destination URL and fire a
     * <code>RemovedUpdate</code> and <code>InsertUpdate</code>.
     *<p>
     * If the target is _top, this method does nothing. In the implementation
     * of the view for a frame, namely the <code>FrameView</code>,
     * the processing of _top is handled.  Given that _top implies
     * replacing the entire document, it made sense to handle this outside
     * of the document that it will replace.
     *<p>
     * If the target is a named frame, then the element hierarchy is searched
     * for an element with a name equal to the target, its
     * <code>HTML.Attribute.SRC</code> attribute is updated and a
     * <code>ChangedUpdate</code> event is fired.
     *
     * @param e the event
     */
    public void processHTMLFrameHyperlinkEvent(HTMLFrameHyperlinkEvent e) {
        String frameName = e.getTarget();
        Element element = e.getSourceElement();
        String urlStr = e.getURL().toString();

        if (frameName.equals("_self")) {
            /*
              The source and destination elements
              are the same.
            */
            updateFrame(element, urlStr);
        } else if (frameName.equals("_parent")) {
            /*
              The destination is the parent of the frame.
            */
            updateFrameSet(element.getParentElement(), urlStr);
        } else {
            /*
              locate a named frame
            */
            Element targetElement = findFrame(frameName);
            if (targetElement != null) {
                updateFrame(targetElement, urlStr);
            }
        }
    }


    /**
     * Searches the element hierarchy for an FRAME element
     * that has its name attribute equal to the <code>frameName</code>.
     *
     * @param frameName
     * @return the element whose NAME attribute has a value of
     *          <code>frameName</code>; returns <code>null</code>
     *          if not found
     */
    private Element findFrame(String frameName) {
        ElementIterator it = new ElementIterator(this);
        Element next;

        while ((next = it.next()) != null) {
            AttributeSet attr = next.getAttributes();
            if (matchNameAttribute(attr, HTML.Tag.FRAME)) {
                String frameTarget = (String)attr.getAttribute(HTML.Attribute.NAME);
                if (frameTarget != null && frameTarget.equals(frameName)) {
                    break;
                }
            }
        }
        return next;
    }

    /**
     * Returns true if <code>StyleConstants.NameAttribute</code> is
     * equal to the tag that is passed in as a parameter.
     *
     * @param attr the attributes to be matched
     * @param tag the value to be matched
     * @return true if there is a match, false otherwise
     * @see javax.swing.text.html.HTML.Attribute
     */
    static boolean matchNameAttribute(AttributeSet attr, HTML.Tag tag) {
        Object o = attr.getAttribute(StyleConstants.NameAttribute);
        if (o instanceof HTML.Tag) {
            HTML.Tag name = (HTML.Tag) o;
            if (name == tag) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces a frameset branch Element with a frame leaf element.
     *
     * @param element the frameset element to remove
     * @param url     the value for the SRC attribute for the
     *                new frame that will replace the frameset
     */
    private void updateFrameSet(Element element, String url) {
        try {
            int startOffset = element.getStartOffset();
            int endOffset = Math.min(getLength(), element.getEndOffset());
            String html = "<frame";
            if (url != null) {
                html += " src=\"" + url + "\"";
            }
            html += ">";
            installParserIfNecessary();
            setOuterHTML(element, html);
        } catch (BadLocationException e1) {
            // Should handle this better
        } catch (IOException ioe) {
            // Should handle this better
        }
    }


    /**
     * Updates the Frame elements <code>HTML.Attribute.SRC attribute</code>
     * and fires a <code>ChangedUpdate</code> event.
     *
     * @param element a FRAME element whose SRC attribute will be updated
     * @param url     a string specifying the new value for the SRC attribute
     */
    private void updateFrame(Element element, String url) {

        try {
            writeLock();
            DefaultDocumentEvent changes = new DefaultDocumentEvent(element.getStartOffset(),
                                                                    1,
                                                                    DocumentEvent.EventType.CHANGE);
            AttributeSet sCopy = element.getAttributes().copyAttributes();
            MutableAttributeSet attr = (MutableAttributeSet) element.getAttributes();
            changes.addEdit(new AttributeUndoableEdit(element, sCopy, false));
            attr.removeAttribute(HTML.Attribute.SRC);
            attr.addAttribute(HTML.Attribute.SRC, url);
            changes.end();
            fireChangedUpdate(changes);
            fireUndoableEditUpdate(new UndoableEditEvent(this, changes));
        } finally {
            writeUnlock();
        }
    }


    /**
     * Returns true if the document will be viewed in a frame.
     * @return true if document will be viewed in a frame, otherwise false
     */
    boolean isFrameDocument() {
        return frameDocument;
    }

    /**
     * Sets a boolean state about whether the document will be
     * viewed in a frame.
     * @param frameDoc  true if the document will be viewed in a frame,
     *          otherwise false
     */
    void setFrameDocumentState(boolean frameDoc) {
        this.frameDocument = frameDoc;
    }

    /**
     * Adds the specified map, this will remove a Map that has been
     * previously registered with the same name.
     *
     * @param map  the <code>Map</code> to be registered
     */
    void addMap(Map map) {
        String     name = map.getName();

        if (name != null) {
            Object     maps = getProperty(MAP_PROPERTY);

            if (maps == null) {
                maps = new Hashtable<>(11);
                putProperty(MAP_PROPERTY, maps);
            }
            if (maps instanceof Hashtable) {
                @SuppressWarnings("unchecked")
                Hashtable<Object, Object> tmp = (Hashtable)maps;
                tmp.put("#" + name, map);
            }
        }
    }

    /**
     * Removes a previously registered map.
     * @param map the <code>Map</code> to be removed
     */
    void removeMap(Map map) {
        String     name = map.getName();

        if (name != null) {
            Object     maps = getProperty(MAP_PROPERTY);

            if (maps instanceof Hashtable) {
                ((Hashtable)maps).remove("#" + name);
            }
        }
    }

    /**
     * Returns the Map associated with the given name.
     * @param name the name of the desired <code>Map</code>
     * @return the <code>Map</code> or <code>null</code> if it can't
     *          be found, or if <code>name</code> is <code>null</code>
     */
    Map getMap(String name) {
        if (name != null) {
            Object     maps = getProperty(MAP_PROPERTY);

            if (maps != null && (maps instanceof Hashtable)) {
                return (Map)((Hashtable)maps).get(name);
            }
        }
        return null;
    }

    /**
     * Returns an <code>Enumeration</code> of the possible Maps.
     * @return the enumerated list of maps, or <code>null</code>
     *          if the maps are not an instance of <code>Hashtable</code>
     */
    Enumeration<Object> getMaps() {
        Object     maps = getProperty(MAP_PROPERTY);

        if (maps instanceof Hashtable) {
            @SuppressWarnings("unchecked")
            Hashtable<Object, Object> tmp = (Hashtable) maps;
            return tmp.elements();
        }
        return null;
    }

    /**
     * Sets the content type language used for style sheets that do not
     * explicitly specify the type. The default is text/css.
     * @param contentType  the content type language for the style sheets
     */
    /* public */
    void setDefaultStyleSheetType(String contentType) {
        putProperty(StyleType, contentType);
    }

    /**
     * Returns the content type language used for style sheets. The default
     * is text/css.
     * @return the content type language used for the style sheets
     */
    /* public */
    String getDefaultStyleSheetType() {
        String retValue = (String)getProperty(StyleType);
        if (retValue == null) {
            return "text/css";
        }
        return retValue;
    }

    /**
     * Sets the parser that is used by the methods that insert html
     * into the existing document, such as <code>setInnerHTML</code>,
     * and <code>setOuterHTML</code>.
     * <p>
     * <code>HTMLEditorKit.createDefaultDocument</code> will set the parser
     * for you. If you create an <code>HTMLDocument</code> by hand,
     * be sure and set the parser accordingly.
     * @param parser the parser to be used for text insertion
     *
     * @since 1.3
     */
    public void setParser(HTMLEditorKit.Parser parser) {
        this.parser = parser;
        putProperty("__PARSER__", null);
    }

    /**
     * Returns the parser that is used when inserting HTML into the existing
     * document.
     * @return the parser used for text insertion
     *
     * @since 1.3
     */
    public HTMLEditorKit.Parser getParser() {
        Object p = getProperty("__PARSER__");

        if (p instanceof HTMLEditorKit.Parser) {
            return (HTMLEditorKit.Parser)p;
        }
        return parser;
    }

    /**
     * Replaces the children of the given element with the contents
     * specified as an HTML string.
     *
     * <p>This will be seen as at least two events, n inserts followed by
     * a remove.</p>
     *
     * <p>Consider the following structure (the <code>elem</code>
     * parameter is <b>in bold</b>).</p>
     *
     * <pre>
     *     &lt;body&gt;
     *       |
     *     <b>&lt;div&gt;</b>
     *      /  \
     *    &lt;p&gt;   &lt;p&gt;
     * </pre>
     *
     * <p>Invoking <code>setInnerHTML(elem, "&lt;ul&gt;&lt;li&gt;")</code>
     * results in the following structure (new elements are <span
     * style="color: blue;">in blue</span>).</p>
     *
     * <pre>
     *     &lt;body&gt;
     *       |
     *     <b>&lt;div&gt;</b>
     *         \
     *         <span style="color: blue;">&lt;ul&gt;</span>
     *           \
     *           <span style="color: blue;">&lt;li&gt;</span>
     * </pre>
     *
     * <p>Parameter <code>elem</code> must not be a leaf element,
     * otherwise an <code>IllegalArgumentException</code> is thrown.
     * If either <code>elem</code> or <code>htmlText</code> parameter
     * is <code>null</code>, no changes are made to the document.</p>
     *
     * <p>For this to work correctly, the document must have an
     * <code>HTMLEditorKit.Parser</code> set. This will be the case
     * if the document was created from an HTMLEditorKit via the
     * <code>createDefaultDocument</code> method.</p>
     *
     * @param elem the branch element whose children will be replaced
     * @param htmlText the string to be parsed and assigned to <code>elem</code>
     * @throws IllegalArgumentException if <code>elem</code> is a leaf
     * @throws IllegalStateException if an <code>HTMLEditorKit.Parser</code>
     *         has not been defined
     * @throws BadLocationException if replacement is impossible because of
     *         a structural issue
     * @throws IOException if an I/O exception occurs
     * @since 1.3
     */
    public void setInnerHTML(Element elem, String htmlText) throws
                             BadLocationException, IOException {
        verifyParser();
        if (elem != null && elem.isLeaf()) {
            throw new IllegalArgumentException
                ("Can not set inner HTML of a leaf");
        }
        if (elem != null && htmlText != null) {
            int oldCount = elem.getElementCount();
            int insertPosition = elem.getStartOffset();
            insertHTML(elem, elem.getStartOffset(), htmlText, true);
            if (elem.getElementCount() > oldCount) {
                // Elements were inserted, do the cleanup.
                removeElements(elem, elem.getElementCount() - oldCount,
                               oldCount);
            }
        }
    }

    /**
     * Replaces the given element in the parent with the contents
     * specified as an HTML string.
     *
     * <p>This will be seen as at least two events, n inserts followed by
     * a remove.</p>
     *
     * <p>When replacing a leaf this will attempt to make sure there is
     * a newline present if one is needed. This may result in an additional
     * element being inserted. Consider, if you were to replace a character
     * element that contained a newline with &lt;img&gt; this would create
     * two elements, one for the image, and one for the newline.</p>
     *
     * <p>If you try to replace the element at length you will most
     * likely end up with two elements, eg
     * <code>setOuterHTML(getCharacterElement (getLength()),
     * "blah")</code> will result in two leaf elements at the end, one
     * representing 'blah', and the other representing the end
     * element.</p>
     *
     * <p>Consider the following structure (the <code>elem</code>
     * parameter is <b>in bold</b>).</p>
     *
     * <pre>
     *     &lt;body&gt;
     *       |
     *     <b>&lt;div&gt;</b>
     *      /  \
     *    &lt;p&gt;   &lt;p&gt;
     * </pre>
     *
     * <p>Invoking <code>setOuterHTML(elem, "&lt;ul&gt;&lt;li&gt;")</code>
     * results in the following structure (new elements are <span
     * style="color: blue;">in blue</span>).</p>
     *
     * <pre>
     *    &lt;body&gt;
     *      |
     *     <span style="color: blue;">&lt;ul&gt;</span>
     *       \
     *       <span style="color: blue;">&lt;li&gt;</span>
     * </pre>
     *
     * <p>If either <code>elem</code> or <code>htmlText</code>
     * parameter is <code>null</code>, no changes are made to the
     * document.</p>
     *
     * <p>For this to work correctly, the document must have an
     * HTMLEditorKit.Parser set. This will be the case if the document
     * was created from an HTMLEditorKit via the
     * <code>createDefaultDocument</code> method.</p>
     *
     * @param elem the element to replace
     * @param htmlText the string to be parsed and inserted in place of <code>elem</code>
     * @throws IllegalStateException if an HTMLEditorKit.Parser has not
     *         been set
     * @throws BadLocationException if replacement is impossible because of
     *         a structural issue
     * @throws IOException if an I/O exception occurs
     * @since 1.3
     */
    public void setOuterHTML(Element elem, String htmlText) throws
                            BadLocationException, IOException {
        verifyParser();
        if (elem != null && elem.getParentElement() != null &&
            htmlText != null) {
            int start = elem.getStartOffset();
            int end = elem.getEndOffset();
            int startLength = getLength();
            // We don't want a newline if elem is a leaf, and doesn't contain
            // a newline.
            boolean wantsNewline = !elem.isLeaf();
            if (!wantsNewline && (end > startLength ||
                                 getText(end - 1, 1).charAt(0) == NEWLINE[0])){
                wantsNewline = true;
            }
            Element parent = elem.getParentElement();
            int oldCount = parent.getElementCount();
            insertHTML(parent, start, htmlText, wantsNewline);
            // Remove old.
            int newLength = getLength();
            if (oldCount != parent.getElementCount()) {
                int removeIndex = parent.getElementIndex(start + newLength -
                                                         startLength);
                removeElements(parent, removeIndex, 1);
            }
        }
    }

    /**
     * Inserts the HTML specified as a string at the start
     * of the element.
     *
     * <p>Consider the following structure (the <code>elem</code>
     * parameter is <b>in bold</b>).</p>
     *
     * <pre>
     *     &lt;body&gt;
     *       |
     *     <b>&lt;div&gt;</b>
     *      /  \
     *    &lt;p&gt;   &lt;p&gt;
     * </pre>
     *
     * <p>Invoking <code>insertAfterStart(elem,
     * "&lt;ul&gt;&lt;li&gt;")</code> results in the following structure
     * (new elements are <span style="color: blue;">in blue</span>).</p>
     *
     * <pre>
     *        &lt;body&gt;
     *          |
     *        <b>&lt;div&gt;</b>
     *       /  |  \
     *    <span style="color: blue;">&lt;ul&gt;</span> &lt;p&gt; &lt;p&gt;
     *     /
     *  <span style="color: blue;">&lt;li&gt;</span>
     * </pre>
     *
     * <p>Unlike the <code>insertBeforeStart</code> method, new
     *  elements become <em>children</em> of the specified element,
     *  not siblings.</p>
     *
     * <p>Parameter <code>elem</code> must not be a leaf element,
     * otherwise an <code>IllegalArgumentException</code> is thrown.
     * If either <code>elem</code> or <code>htmlText</code> parameter
     * is <code>null</code>, no changes are made to the document.</p>
     *
     * <p>For this to work correctly, the document must have an
     * <code>HTMLEditorKit.Parser</code> set. This will be the case
     * if the document was created from an HTMLEditorKit via the
     * <code>createDefaultDocument</code> method.</p>
     *
     * @param elem the branch element to be the root for the new text
     * @param htmlText the string to be parsed and assigned to <code>elem</code>
     * @throws IllegalArgumentException if <code>elem</code> is a leaf
     * @throws IllegalStateException if an HTMLEditorKit.Parser has not
     *         been set on the document
     * @throws BadLocationException if insertion is impossible because of
     *         a structural issue
     * @throws IOException if an I/O exception occurs
     * @since 1.3
     */
    public void insertAfterStart(Element elem, String htmlText) throws
                                 BadLocationException, IOException {
        verifyParser();

        if (elem == null || htmlText == null) {
            return;
        }

        if (elem.isLeaf()) {
            throw new IllegalArgumentException
                ("Can not insert HTML after start of a leaf");
        }
        insertHTML(elem, elem.getStartOffset(), htmlText, false);
    }

    /**
     * Inserts the HTML specified as a string at the end of
     * the element.
     *
     * <p> If <code>elem</code>'s children are leaves, and the
     * character at a <code>elem.getEndOffset() - 1</code> is a newline,
     * this will insert before the newline so that there isn't text after
     * the newline.</p>
     *
     * <p>Consider the following structure (the <code>elem</code>
     * parameter is <b>in bold</b>).</p>
     *
     * <pre>
     *     &lt;body&gt;
     *       |
     *     <b>&lt;div&gt;</b>
     *      /  \
     *    &lt;p&gt;   &lt;p&gt;
     * </pre>
     *
     * <p>Invoking <code>insertBeforeEnd(elem, "&lt;ul&gt;&lt;li&gt;")</code>
     * results in the following structure (new elements are <span
     * style="color: blue;">in blue</span>).</p>
     *
     * <pre>
     *        &lt;body&gt;
     *          |
     *        <b>&lt;div&gt;</b>
     *       /  |  \
     *     &lt;p&gt; &lt;p&gt; <span style="color: blue;">&lt;ul&gt;</span>
     *               \
     *               <span style="color: blue;">&lt;li&gt;</span>
     * </pre>
     *
     * <p>Unlike the <code>insertAfterEnd</code> method, new elements
     * become <em>children</em> of the specified element, not
     * siblings.</p>
     *
     * <p>Parameter <code>elem</code> must not be a leaf element,
     * otherwise an <code>IllegalArgumentException</code> is thrown.
     * If either <code>elem</code> or <code>htmlText</code> parameter
     * is <code>null</code>, no changes are made to the document.</p>
     *
     * <p>For this to work correctly, the document must have an
     * <code>HTMLEditorKit.Parser</code> set. This will be the case
     * if the document was created from an HTMLEditorKit via the
     * <code>createDefaultDocument</code> method.</p>
     *
     * @param elem the element to be the root for the new text
     * @param htmlText the string to be parsed and assigned to <code>elem</code>
     * @throws IllegalArgumentException if <code>elem</code> is a leaf
     * @throws IllegalStateException if an HTMLEditorKit.Parser has not
     *         been set on the document
     * @throws BadLocationException if insertion is impossible because of
     *         a structural issue
     * @throws IOException if an I/O exception occurs
     * @since 1.3
     */
    public void insertBeforeEnd(Element elem, String htmlText) throws
                                BadLocationException, IOException {
        verifyParser();
        if (elem != null && elem.isLeaf()) {
            throw new IllegalArgumentException
                ("Can not set inner HTML before end of leaf");
        }
        if (elem != null) {
            int offset = elem.getEndOffset();
            if (elem.getElement(elem.getElementIndex(offset - 1)).isLeaf() &&
                getText(offset - 1, 1).charAt(0) == NEWLINE[0]) {
                offset--;
            }
            insertHTML(elem, offset, htmlText, false);
        }
    }

    /**
     * Inserts the HTML specified as a string before the start of
     * the given element.
     *
     * <p>Consider the following structure (the <code>elem</code>
     * parameter is <b>in bold</b>).</p>
     *
     * <pre>
     *     &lt;body&gt;
     *       |
     *     <b>&lt;div&gt;</b>
     *      /  \
     *    &lt;p&gt;   &lt;p&gt;
     * </pre>
     *
     * <p>Invoking <code>insertBeforeStart(elem,
     * "&lt;ul&gt;&lt;li&gt;")</code> results in the following structure
     * (new elements are <span style="color: blue;">in blue</span>).</p>
     *
     * <pre>
     *        &lt;body&gt;
     *         /  \
     *      <span style="color: blue;">&lt;ul&gt;</span> <b>&lt;div&gt;</b>
     *       /    /  \
     *     <span style="color: blue;">&lt;li&gt;</span> &lt;p&gt;  &lt;p&gt;
     * </pre>
     *
     * <p>Unlike the <code>insertAfterStart</code> method, new
     * elements become <em>siblings</em> of the specified element, not
     * children.</p>
     *
     * <p>If either <code>elem</code> or <code>htmlText</code>
     * parameter is <code>null</code>, no changes are made to the
     * document.</p>
     *
     * <p>For this to work correctly, the document must have an
     * <code>HTMLEditorKit.Parser</code> set. This will be the case
     * if the document was created from an HTMLEditorKit via the
     * <code>createDefaultDocument</code> method.</p>
     *
     * @param elem the element the content is inserted before
     * @param htmlText the string to be parsed and inserted before <code>elem</code>
     * @throws IllegalStateException if an HTMLEditorKit.Parser has not
     *         been set on the document
     * @throws BadLocationException if insertion is impossible because of
     *         a structural issue
     * @throws IOException if an I/O exception occurs
     * @since 1.3
     */
    public void insertBeforeStart(Element elem, String htmlText) throws
                                  BadLocationException, IOException {
        verifyParser();
        if (elem != null) {
            Element parent = elem.getParentElement();

            if (parent != null) {
                insertHTML(parent, elem.getStartOffset(), htmlText, false);
            }
        }
    }

    /**
     * Inserts the HTML specified as a string after the end of the
     * given element.
     *
     * <p>Consider the following structure (the <code>elem</code>
     * parameter is <b>in bold</b>).</p>
     *
     * <pre>
     *     &lt;body&gt;
     *       |
     *     <b>&lt;div&gt;</b>
     *      /  \
     *    &lt;p&gt;   &lt;p&gt;
     * </pre>
     *
     * <p>Invoking <code>insertAfterEnd(elem, "&lt;ul&gt;&lt;li&gt;")</code>
     * results in the following structure (new elements are <span
     * style="color: blue;">in blue</span>).</p>
     *
     * <pre>
     *        &lt;body&gt;
     *         /  \
     *      <b>&lt;div&gt;</b> <span style="color: blue;">&lt;ul&gt;</span>
     *       / \    \
     *     &lt;p&gt; &lt;p&gt;  <span style="color: blue;">&lt;li&gt;</span>
     * </pre>
     *
     * <p>Unlike the <code>insertBeforeEnd</code> method, new elements
     * become <em>siblings</em> of the specified element, not
     * children.</p>
     *
     * <p>If either <code>elem</code> or <code>htmlText</code>
     * parameter is <code>null</code>, no changes are made to the
     * document.</p>
     *
     * <p>For this to work correctly, the document must have an
     * <code>HTMLEditorKit.Parser</code> set. This will be the case
     * if the document was created from an HTMLEditorKit via the
     * <code>createDefaultDocument</code> method.</p>
     *
     * @param elem the element the content is inserted after
     * @param htmlText the string to be parsed and inserted after <code>elem</code>
     * @throws IllegalStateException if an HTMLEditorKit.Parser has not
     *         been set on the document
     * @throws BadLocationException if insertion is impossible because of
     *         a structural issue
     * @throws IOException if an I/O exception occurs
     * @since 1.3
     */
    public void insertAfterEnd(Element elem, String htmlText) throws
                               BadLocationException, IOException {
        verifyParser();
        if (elem != null) {
            Element parent = elem.getParentElement();

            if (parent != null) {
                // If we are going to insert the string into the body
                // section, it is necessary to set the corrsponding flag.
                if (HTML.Tag.BODY.name.equals(parent.getName())) {
                    insertInBody = true;
                }
                int offset = elem.getEndOffset();
                if (offset > (getLength() + 1)) {
                    offset--;
                }
                else if (elem.isLeaf() && getText(offset - 1, 1).
                    charAt(0) == NEWLINE[0]) {
                    offset--;
                }
                insertHTML(parent, offset, htmlText, false);
                // Cleanup the flag, if any.
                if (insertInBody) {
                    insertInBody = false;
                }
            }
        }
    }

    /**
     * Returns the element that has the given id <code>Attribute</code>.
     * If the element can't be found, <code>null</code> is returned.
     * Note that this method works on an <code>Attribute</code>,
     * <i>not</i> a character tag.  In the following HTML snippet:
     * <code>&lt;a id="HelloThere"&gt;</code> the attribute is
     * 'id' and the character tag is 'a'.
     * This is a convenience method for
     * <code>getElement(RootElement, HTML.Attribute.id, id)</code>.
     * This is not thread-safe.
     *
     * @param id  the string representing the desired <code>Attribute</code>
     * @return the element with the specified <code>Attribute</code>
     *          or <code>null</code> if it can't be found,
     *          or <code>null</code> if <code>id</code> is <code>null</code>
     * @see javax.swing.text.html.HTML.Attribute
     * @since 1.3
     */
    public Element getElement(String id) {
        if (id == null) {
            return null;
        }
        return getElement(getDefaultRootElement(), HTML.Attribute.ID, id,
                          true);
    }

    /**
     * Returns the child element of <code>e</code> that contains the
     * attribute, <code>attribute</code> with value <code>value</code>, or
     * <code>null</code> if one isn't found. This is not thread-safe.
     *
     * @param e the root element where the search begins
     * @param attribute the desired <code>Attribute</code>
     * @param value the values for the specified <code>Attribute</code>
     * @return the element with the specified <code>Attribute</code>
     *          and the specified <code>value</code>, or <code>null</code>
     *          if it can't be found
     * @see javax.swing.text.html.HTML.Attribute
     * @since 1.3
     */
    public Element getElement(Element e, Object attribute, Object value) {
        return getElement(e, attribute, value, true);
    }

    /**
     * Returns the child element of <code>e</code> that contains the
     * attribute, <code>attribute</code> with value <code>value</code>, or
     * <code>null</code> if one isn't found. This is not thread-safe.
     * <p>
     * If <code>searchLeafAttributes</code> is true, and <code>e</code> is
     * a leaf, any attributes that are instances of <code>HTML.Tag</code>
     * with a value that is an <code>AttributeSet</code> will also be checked.
     *
     * @param e the root element where the search begins
     * @param attribute the desired <code>Attribute</code>
     * @param value the values for the specified <code>Attribute</code>
     * @return the element with the specified <code>Attribute</code>
     *          and the specified <code>value</code>, or <code>null</code>
     *          if it can't be found
     * @see javax.swing.text.html.HTML.Attribute
     */
    private Element getElement(Element e, Object attribute, Object value,
                               boolean searchLeafAttributes) {
        AttributeSet attr = e.getAttributes();

        if (attr != null && attr.isDefined(attribute)) {
            if (value.equals(attr.getAttribute(attribute))) {
                return e;
            }
        }
        if (!e.isLeaf()) {
            for (int counter = 0, maxCounter = e.getElementCount();
                 counter < maxCounter; counter++) {
                Element retValue = getElement(e.getElement(counter), attribute,
                                              value, searchLeafAttributes);

                if (retValue != null) {
                    return retValue;
                }
            }
        }
        else if (searchLeafAttributes && attr != null) {
            // For some leaf elements we store the actual attributes inside
            // the AttributeSet of the Element (such as anchors).
            Enumeration<?> names = attr.getAttributeNames();
            if (names != null) {
                while (names.hasMoreElements()) {
                    Object name = names.nextElement();
                    if ((name instanceof HTML.Tag) &&
                        (attr.getAttribute(name) instanceof AttributeSet)) {

                        AttributeSet check = (AttributeSet)attr.
                                             getAttribute(name);
                        if (check.isDefined(attribute) &&
                            value.equals(check.getAttribute(attribute))) {
                            return e;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Verifies the document has an <code>HTMLEditorKit.Parser</code> set.
     * If <code>getParser</code> returns <code>null</code>, this will throw an
     * IllegalStateException.
     *
     * @throws IllegalStateException if the document does not have a Parser
     */
    private void verifyParser() {
        if (getParser() == null) {
            throw new IllegalStateException("No HTMLEditorKit.Parser");
        }
    }

    /**
     * Installs a default Parser if one has not been installed yet.
     */
    private void installParserIfNecessary() {
        if (getParser() == null) {
            setParser(new HTMLEditorKit().getParser());
        }
    }

    /**
     * Inserts a string of HTML into the document at the given position.
     * <code>parent</code> is used to identify the location to insert the
     * <code>html</code>. If <code>parent</code> is a leaf this can have
     * unexpected results.
     */
    private void insertHTML(Element parent, int offset, String html,
                            boolean wantsTrailingNewline)
                 throws BadLocationException, IOException {
        if (parent != null && html != null) {
            HTMLEditorKit.Parser parser = getParser();
            if (parser != null) {
                int lastOffset = Math.max(0, offset - 1);
                Element charElement = getCharacterElement(lastOffset);
                Element commonParent = parent;
                int pop = 0;
                int push = 0;

                if (parent.getStartOffset() > lastOffset) {
                    while (commonParent != null &&
                           commonParent.getStartOffset() > lastOffset) {
                        commonParent = commonParent.getParentElement();
                        push++;
                    }
                    if (commonParent == null) {
                        throw new BadLocationException("No common parent",
                                                       offset);
                    }
                }
                while (charElement != null && charElement != commonParent) {
                    pop++;
                    charElement = charElement.getParentElement();
                }
                if (charElement != null) {
                    // Found it, do the insert.
                    HTMLReader reader = new HTMLReader(offset, pop - 1, push,
                                                       null, false, true,
                                                       wantsTrailingNewline);

                    parser.parse(new StringReader(html), reader, true);
                    reader.flush();
                }
            }
        }
    }

    /**
     * Removes child Elements of the passed in Element <code>e</code>. This
     * will do the necessary cleanup to ensure the element representing the
     * end character is correctly created.
     * <p>This is not a general purpose method, it assumes that <code>e</code>
     * will still have at least one child after the remove, and it assumes
     * the character at <code>e.getStartOffset() - 1</code> is a newline and
     * is of length 1.
     */
    private void removeElements(Element e, int index, int count) throws BadLocationException {
        writeLock();
        try {
            int start = e.getElement(index).getStartOffset();
            int end = e.getElement(index + count - 1).getEndOffset();
            if (end > getLength()) {
                removeElementsAtEnd(e, index, count, start, end);
            }
            else {
                removeElements(e, index, count, start, end);
            }
        } finally {
            writeUnlock();
        }
    }

    /**
     * Called to remove child elements of <code>e</code> when one of the
     * elements to remove is representing the end character.
     * <p>Since the Content will not allow a removal to the end character
     * this will do a remove from <code>start - 1</code> to <code>end</code>.
     * The end Element(s) will be removed, and the element representing
     * <code>start - 1</code> to <code>start</code> will be recreated. This
     * Element has to be recreated as after the content removal its offsets
     * become <code>start - 1</code> to <code>start - 1</code>.
     */
    private void removeElementsAtEnd(Element e, int index, int count,
                         int start, int end) throws BadLocationException {
        // index must be > 0 otherwise no insert would have happened.
        boolean isLeaf = (e.getElement(index - 1).isLeaf());
        DefaultDocumentEvent dde = new DefaultDocumentEvent(
                       start - 1, end - start + 1, DocumentEvent.
                       EventType.REMOVE);

        if (isLeaf) {
            Element endE = getCharacterElement(getLength());
            // e.getElement(index - 1) should represent the newline.
            index--;
            if (endE.getParentElement() != e) {
                // The hiearchies don't match, we'll have to manually
                // recreate the leaf at e.getElement(index - 1)
                replace(dde, e, index, ++count, start, end, true, true);
            }
            else {
                // The hierarchies for the end Element and
                // e.getElement(index - 1), match, we can safely remove
                // the Elements and the end content will be aligned
                // appropriately.
                replace(dde, e, index, count, start, end, true, false);
            }
        }
        else {
            // Not a leaf, descend until we find the leaf representing
            // start - 1 and remove it.
            Element newLineE = e.getElement(index - 1);
            while (!newLineE.isLeaf()) {
                newLineE = newLineE.getElement(newLineE.getElementCount() - 1);
            }
            newLineE = newLineE.getParentElement();
            replace(dde, e, index, count, start, end, false, false);
            replace(dde, newLineE, newLineE.getElementCount() - 1, 1, start,
                    end, true, true);
        }
        postRemoveUpdate(dde);
        dde.end();
        fireRemoveUpdate(dde);
        fireUndoableEditUpdate(new UndoableEditEvent(this, dde));
    }

    /**
     * This is used by <code>removeElementsAtEnd</code>, it removes
     * <code>count</code> elements starting at <code>start</code> from
     * <code>e</code>.  If <code>remove</code> is true text of length
     * <code>start - 1</code> to <code>end - 1</code> is removed.  If
     * <code>create</code> is true a new leaf is created of length 1.
     */
    private void replace(DefaultDocumentEvent dde, Element e, int index,
                         int count, int start, int end, boolean remove,
                         boolean create) throws BadLocationException {
        Element[] added;
        AttributeSet attrs = e.getElement(index).getAttributes();
        Element[] removed = new Element[count];

        for (int counter = 0; counter < count; counter++) {
            removed[counter] = e.getElement(counter + index);
        }
        if (remove) {
            UndoableEdit u = getContent().remove(start - 1, end - start);
            if (u != null) {
                dde.addEdit(u);
            }
        }
        if (create) {
            added = new Element[1];
            added[0] = createLeafElement(e, attrs, start - 1, start);
        }
        else {
            added = new Element[0];
        }
        dde.addEdit(new ElementEdit(e, index, removed, added));
        ((AbstractDocument.BranchElement)e).replace(
                                             index, removed.length, added);
    }

    /**
     * Called to remove child Elements when the end is not touched.
     */
    private void removeElements(Element e, int index, int count,
                             int start, int end) throws BadLocationException {
        Element[] removed = new Element[count];
        Element[] added = new Element[0];
        for (int counter = 0; counter < count; counter++) {
            removed[counter] = e.getElement(counter + index);
        }
        DefaultDocumentEvent dde = new DefaultDocumentEvent
                (start, end - start, DocumentEvent.EventType.REMOVE);
        ((AbstractDocument.BranchElement)e).replace(index, removed.length,
                                                    added);
        dde.addEdit(new ElementEdit(e, index, removed, added));
        UndoableEdit u = getContent().remove(start, end - start);
        if (u != null) {
            dde.addEdit(u);
        }
        postRemoveUpdate(dde);
        dde.end();
        fireRemoveUpdate(dde);
        if (u != null) {
            fireUndoableEditUpdate(new UndoableEditEvent(this, dde));
        }
    }


    // These two are provided for inner class access. The are named different
    // than the super class as the super class implementations are final.
    void obtainLock() {
        writeLock();
    }

    void releaseLock() {
        writeUnlock();
    }

    //
    // Provided for inner class access.
    //

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.
     *
     * @param e the event
     * @see EventListenerList
     */
    protected void fireChangedUpdate(DocumentEvent e) {
        super.fireChangedUpdate(e);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance
     * is lazily created using the parameters passed into
     * the fire method.
     *
     * @param e the event
     * @see EventListenerList
     */
    protected void fireUndoableEditUpdate(UndoableEditEvent e) {
        super.fireUndoableEditUpdate(e);
    }

    boolean hasBaseTag() {
        return hasBaseTag;
    }

    String getBaseTarget() {
        return baseTarget;
    }

    /*
     * state defines whether the document is a frame document
     * or not.
     */
    private boolean frameDocument = false;
    private boolean preservesUnknownTags = true;

    /*
     * Used to store button groups for radio buttons in
     * a form.
     */
    private HashMap<String, ButtonGroup> radioButtonGroupsMap;

    /**
     * Document property for the number of tokens to buffer
     * before building an element subtree to represent them.
     */
    static final String TokenThreshold = "token threshold";

    private static final int MaxThreshold = 10000;

    private static final int StepThreshold = 5;


    /**
     * Document property key value. The value for the key will be a Vector
     * of Strings that are comments not found in the body.
     */
    public static final String AdditionalComments = "AdditionalComments";

    /**
     * Document property key value. The value for the key will be a
     * String indicating the default type of stylesheet links.
     */
    /* public */ static final String StyleType = "StyleType";

    /**
     * The location to resolve relative URLs against.  By
     * default this will be the document's URL if the document
     * was loaded from a URL.  If a base tag is found and
     * can be parsed, it will be used as the base location.
     */
    URL base;

    /**
     * does the document have base tag
     */
    boolean hasBaseTag = false;

    /**
     * BASE tag's TARGET attribute value
     */
    private String baseTarget = null;

    /**
     * The parser that is used when inserting html into the existing
     * document.
     */
    private HTMLEditorKit.Parser parser;

    /**
     * Used for inserts when a null AttributeSet is supplied.
     */
    private static AttributeSet contentAttributeSet;

    /**
     * Property Maps are registered under, will be a Hashtable.
     */
    static String MAP_PROPERTY = "__MAP__";

    private static char[] NEWLINE;

    /**
     * Indicates that direct insertion to body section takes place.
     */
    private boolean insertInBody = false;

    /**
     * I18N property key.
     *
     * @see AbstractDocument#I18NProperty
     */
    private static final String I18NProperty = "i18n";

    static {
        contentAttributeSet = new SimpleAttributeSet();
        ((MutableAttributeSet)contentAttributeSet).
                        addAttribute(StyleConstants.NameAttribute,
                                     HTML.Tag.CONTENT);
        NEWLINE = new char[1];
        NEWLINE[0] = '\n';
    }


    /**
     * An iterator to iterate over a particular type of
     * tag.  The iterator is not thread safe.  If reliable
     * access to the document is not already ensured by
     * the context under which the iterator is being used,
     * its use should be performed under the protection of
     * Document.render.
     */
    public abstract static class Iterator {

        /**
         * Constructor for subclasses to call.
         */
        protected Iterator() {}

        /**
         * Return the attributes for this tag.
         * @return the <code>AttributeSet</code> for this tag, or
         *      <code>null</code> if none can be found
         */
        public abstract AttributeSet getAttributes();

        /**
         * Returns the start of the range for which the current occurrence of
         * the tag is defined and has the same attributes.
         *
         * @return the start of the range, or -1 if it can't be found
         */
        public abstract int getStartOffset();

        /**
         * Returns the end of the range for which the current occurrence of
         * the tag is defined and has the same attributes.
         *
         * @return the end of the range
         */
        public abstract int getEndOffset();

        /**
         * Move the iterator forward to the next occurrence
         * of the tag it represents.
         */
        public abstract void next();

        /**
         * Indicates if the iterator is currently
         * representing an occurrence of a tag.  If
         * false there are no more tags for this iterator.
         * @return true if the iterator is currently representing an
         *              occurrence of a tag, otherwise returns false
         */
        public abstract boolean isValid();

        /**
         * Type of tag this iterator represents.
         * @return the tag
         */
        public abstract HTML.Tag getTag();
    }

    /**
     * An iterator to iterate over a particular type of tag.
     */
    static class LeafIterator extends Iterator {

        LeafIterator(HTML.Tag t, Document doc) {
            tag = t;
            pos = new ElementIterator(doc);
            endOffset = 0;
            next();
        }

        /**
         * Returns the attributes for this tag.
         * @return the <code>AttributeSet</code> for this tag,
         *              or <code>null</code> if none can be found
         */
        public AttributeSet getAttributes() {
            Element elem = pos.current();
            if (elem != null) {
                AttributeSet a = (AttributeSet)
                    elem.getAttributes().getAttribute(tag);
                if (a == null) {
                    a = elem.getAttributes();
                }
                return a;
            }
            return null;
        }

        /**
         * Returns the start of the range for which the current occurrence of
         * the tag is defined and has the same attributes.
         *
         * @return the start of the range, or -1 if it can't be found
         */
        public int getStartOffset() {
            Element elem = pos.current();
            if (elem != null) {
                return elem.getStartOffset();
            }
            return -1;
        }

        /**
         * Returns the end of the range for which the current occurrence of
         * the tag is defined and has the same attributes.
         *
         * @return the end of the range
         */
        public int getEndOffset() {
            return endOffset;
        }

        /**
         * Moves the iterator forward to the next occurrence
         * of the tag it represents.
         */
        public void next() {
            for (nextLeaf(pos); isValid(); nextLeaf(pos)) {
                Element elem = pos.current();
                if (elem.getStartOffset() >= endOffset) {
                    AttributeSet a = pos.current().getAttributes();

                    if (a.isDefined(tag) ||
                        a.getAttribute(StyleConstants.NameAttribute) == tag) {

                        // we found the next one
                        setEndOffset();
                        break;
                    }
                }
            }
        }

        /**
         * Returns the type of tag this iterator represents.
         *
         * @return the <code>HTML.Tag</code> that this iterator represents.
         * @see javax.swing.text.html.HTML.Tag
         */
        public HTML.Tag getTag() {
            return tag;
        }

        /**
         * Returns true if the current position is not <code>null</code>.
         * @return true if current position is not <code>null</code>,
         *              otherwise returns false
         */
        public boolean isValid() {
            return (pos.current() != null);
        }

        /**
         * Moves the given iterator to the next leaf element.
         * @param iter  the iterator to be scanned
         */
        void nextLeaf(ElementIterator iter) {
            for (iter.next(); iter.current() != null; iter.next()) {
                Element e = iter.current();
                if (e.isLeaf()) {
                    break;
                }
            }
        }

        /**
         * Marches a cloned iterator forward to locate the end
         * of the run.  This sets the value of <code>endOffset</code>.
         */
        void setEndOffset() {
            AttributeSet a0 = getAttributes();
            endOffset = pos.current().getEndOffset();
            ElementIterator fwd = (ElementIterator) pos.clone();
            for (nextLeaf(fwd); fwd.current() != null; nextLeaf(fwd)) {
                Element e = fwd.current();
                AttributeSet a1 = (AttributeSet) e.getAttributes().getAttribute(tag);
                if ((a1 == null) || (! a1.equals(a0))) {
                    break;
                }
                endOffset = e.getEndOffset();
            }
        }

        private int endOffset;
        private HTML.Tag tag;
        private ElementIterator pos;

    }

    /**
     * An HTML reader to load an HTML document with an HTML
     * element structure.  This is a set of callbacks from
     * the parser, implemented to create a set of elements
     * tagged with attributes.  The parse builds up tokens
     * (ElementSpec) that describe the element subtree desired,
     * and burst it into the document under the protection of
     * a write lock using the insert method on the document
     * outer class.
     * <p>
     * The reader can be configured by registering actions
     * (of type <code>HTMLDocument.HTMLReader.TagAction</code>)
     * that describe how to handle the action.  The idea behind
     * the actions provided is that the most natural text editing
     * operations can be provided if the element structure boils
     * down to paragraphs with runs of some kind of style
     * in them.  Some things are more naturally specified
     * structurally, so arbitrary structure should be allowed
     * above the paragraphs, but will need to be edited with structural
     * actions.  The implication of this is that some of the
     * HTML elements specified in the stream being parsed will
     * be collapsed into attributes, and in some cases paragraphs
     * will be synthesized.  When HTML elements have been
     * converted to attributes, the attribute key will be of
     * type HTML.Tag, and the value will be of type AttributeSet
     * so that no information is lost.  This enables many of the
     * existing actions to work so that the user can type input,
     * hit the return key, backspace, delete, etc and have a
     * reasonable result.  Selections can be created, and attributes
     * applied or removed, etc.  With this in mind, the work done
     * by the reader can be categorized into the following kinds
     * of tasks:
     * <dl>
     * <dt>Block
     * <dd>Build the structure like it's specified in the stream.
     * This produces elements that contain other elements.
     * <dt>Paragraph
     * <dd>Like block except that it's expected that the element
     * will be used with a paragraph view so a paragraph element
     * won't need to be synthesized.
     * <dt>Character
     * <dd>Contribute the element as an attribute that will start
     * and stop at arbitrary text locations.  This will ultimately
     * be mixed into a run of text, with all of the currently
     * flattened HTML character elements.
     * <dt>Special
     * <dd>Produce an embedded graphical element.
     * <dt>Form
     * <dd>Produce an element that is like the embedded graphical
     * element, except that it also has a component model associated
     * with it.
     * <dt>Hidden
     * <dd>Create an element that is hidden from view when the
     * document is being viewed read-only, and visible when the
     * document is being edited.  This is useful to keep the
     * model from losing information, and used to store things
     * like comments and unrecognized tags.
     *
     * </dl>
     * <p>
     * Currently, &lt;APPLET&gt;, &lt;PARAM&gt;, &lt;MAP&gt;, &lt;AREA&gt;, &lt;LINK&gt;,
     * &lt;SCRIPT&gt; and &lt;STYLE&gt; are unsupported.
     *
     * <p>
     * The assignment of the actions described is shown in the
     * following table for the tags defined in <code>HTML.Tag</code>.
     *
     * <table class="striped">
     * <caption>HTML tags and assigned actions</caption>
     * <thead>
     *   <tr>
     *     <th scope="col">Tag
     *     <th scope="col">Action
     * </thead>
     * <tbody>
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.A}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.ADDRESS}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.APPLET}
     *     <td>HiddenAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.AREA}
     *     <td>AreaAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.B}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.BASE}
     *     <td>BaseAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.BASEFONT}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.BIG}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.BLOCKQUOTE}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.BODY}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.BR}
     *     <td>SpecialAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.CAPTION}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.CENTER}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.CITE}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.CODE}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.DD}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.DFN}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.DIR}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.DIV}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.DL}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.DT}
     *     <td>ParagraphAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.EM}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.FONT}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.FORM}
     *     <td>As of 1.4 a BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.FRAME}
     *     <td>SpecialAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.FRAMESET}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.H1}
     *     <td>ParagraphAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.H2}
     *     <td>ParagraphAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.H3}
     *     <td>ParagraphAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.H4}
     *     <td>ParagraphAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.H5}
     *     <td>ParagraphAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.H6}
     *     <td>ParagraphAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.HEAD}
     *     <td>HeadAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.HR}
     *     <td>SpecialAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.HTML}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.I}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.IMG}
     *     <td>SpecialAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.INPUT}
     *     <td>FormAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.ISINDEX}
     *     <td>IsndexAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.KBD}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.LI}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.LINK}
     *     <td>LinkAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.MAP}
     *     <td>MapAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.MENU}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.META}
     *     <td>MetaAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.NOFRAMES}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.OBJECT}
     *     <td>SpecialAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.OL}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.OPTION}
     *     <td>FormAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.P}
     *     <td>ParagraphAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.PARAM}
     *     <td>HiddenAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.PRE}
     *     <td>PreAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.SAMP}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.SCRIPT}
     *     <td>HiddenAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.SELECT}
     *     <td>FormAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.SMALL}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.STRIKE}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.S}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.STRONG}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.STYLE}
     *     <td>StyleAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.SUB}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.SUP}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.TABLE}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.TD}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.TEXTAREA}
     *     <td>FormAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.TH}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.TITLE}
     *     <td>TitleAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.TR}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.TT}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.U}
     *     <td>CharacterAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.UL}
     *     <td>BlockAction
     *   <tr>
     *     <th scope="row">{@code HTML.Tag.VAR}
     *     <td>CharacterAction
     * </tbody>
     * </table>
     * <p>
     * Once &lt;/html&gt; is encountered, the Actions are no longer notified.
     */
    public class HTMLReader extends HTMLEditorKit.ParserCallback {

        /**
         * Constructs an HTMLReader using default pop and push depth and no tag to insert.
         *
         * @param offset the starting offset
         */
        public HTMLReader(int offset) {
            this(offset, 0, 0, null);
        }

        /**
         * Constructs an HTMLReader.
         *
         * @param offset the starting offset
         * @param popDepth how many parents to ascend before insert new element
         * @param pushDepth how many parents to descend (relative to popDepth) before
         *                  inserting
         * @param insertTag a tag to insert (may be null)
         */
        public HTMLReader(int offset, int popDepth, int pushDepth,
                          HTML.Tag insertTag) {
            this(offset, popDepth, pushDepth, insertTag, true, false, true);
        }

        /**
         * Generates a RuntimeException (will eventually generate
         * a BadLocationException when API changes are alloced) if inserting
         * into non empty document, <code>insertTag</code> is
         * non-<code>null</code>, and <code>offset</code> is not in the body.
         */
        // PENDING(sky): Add throws BadLocationException and remove
        // RuntimeException
        HTMLReader(int offset, int popDepth, int pushDepth,
                   HTML.Tag insertTag, boolean insertInsertTag,
                   boolean insertAfterImplied, boolean wantsTrailingNewline) {
            emptyDocument = (getLength() == 0);
            isStyleCSS = "text/css".equals(getDefaultStyleSheetType());
            this.offset = offset;
            threshold = HTMLDocument.this.getTokenThreshold();
            tagMap = new Hashtable<HTML.Tag, TagAction>(57);
            TagAction na = new TagAction();
            TagAction ba = new BlockAction();
            TagAction pa = new ParagraphAction();
            TagAction ca = new CharacterAction();
            TagAction sa = new SpecialAction();
            TagAction fa = new FormAction();
            TagAction ha = new HiddenAction();
            TagAction conv = new ConvertAction();

            // register handlers for the well known tags
            tagMap.put(HTML.Tag.A, new AnchorAction());
            tagMap.put(HTML.Tag.ADDRESS, ca);
            tagMap.put(HTML.Tag.APPLET, ha);
            tagMap.put(HTML.Tag.AREA, new AreaAction());
            tagMap.put(HTML.Tag.B, conv);
            tagMap.put(HTML.Tag.BASE, new BaseAction());
            tagMap.put(HTML.Tag.BASEFONT, ca);
            tagMap.put(HTML.Tag.BIG, ca);
            tagMap.put(HTML.Tag.BLOCKQUOTE, ba);
            tagMap.put(HTML.Tag.BODY, ba);
            tagMap.put(HTML.Tag.BR, sa);
            tagMap.put(HTML.Tag.CAPTION, ba);
            tagMap.put(HTML.Tag.CENTER, ba);
            tagMap.put(HTML.Tag.CITE, ca);
            tagMap.put(HTML.Tag.CODE, ca);
            tagMap.put(HTML.Tag.DD, ba);
            tagMap.put(HTML.Tag.DFN, ca);
            tagMap.put(HTML.Tag.DIR, ba);
            tagMap.put(HTML.Tag.DIV, ba);
            tagMap.put(HTML.Tag.DL, ba);
            tagMap.put(HTML.Tag.DT, pa);
            tagMap.put(HTML.Tag.EM, ca);
            tagMap.put(HTML.Tag.FONT, conv);
            tagMap.put(HTML.Tag.FORM, new FormTagAction());
            tagMap.put(HTML.Tag.FRAME, sa);
            tagMap.put(HTML.Tag.FRAMESET, ba);
            tagMap.put(HTML.Tag.H1, pa);
            tagMap.put(HTML.Tag.H2, pa);
            tagMap.put(HTML.Tag.H3, pa);
            tagMap.put(HTML.Tag.H4, pa);
            tagMap.put(HTML.Tag.H5, pa);
            tagMap.put(HTML.Tag.H6, pa);
            tagMap.put(HTML.Tag.HEAD, new HeadAction());
            tagMap.put(HTML.Tag.HR, sa);
            tagMap.put(HTML.Tag.HTML, ba);
            tagMap.put(HTML.Tag.I, conv);
            tagMap.put(HTML.Tag.IMG, sa);
            tagMap.put(HTML.Tag.INPUT, fa);
            tagMap.put(HTML.Tag.ISINDEX, new IsindexAction());
            tagMap.put(HTML.Tag.KBD, ca);
            tagMap.put(HTML.Tag.LI, ba);
            tagMap.put(HTML.Tag.LINK, new LinkAction());
            tagMap.put(HTML.Tag.MAP, new MapAction());
            tagMap.put(HTML.Tag.MENU, ba);
            tagMap.put(HTML.Tag.META, new MetaAction());
            tagMap.put(HTML.Tag.NOBR, ca);
            tagMap.put(HTML.Tag.NOFRAMES, ba);
            tagMap.put(HTML.Tag.OBJECT, sa);
            tagMap.put(HTML.Tag.OL, ba);
            tagMap.put(HTML.Tag.OPTION, fa);
            tagMap.put(HTML.Tag.P, pa);
            tagMap.put(HTML.Tag.PARAM, new ObjectAction());
            tagMap.put(HTML.Tag.PRE, new PreAction());
            tagMap.put(HTML.Tag.SAMP, ca);
            tagMap.put(HTML.Tag.SCRIPT, ha);
            tagMap.put(HTML.Tag.SELECT, fa);
            tagMap.put(HTML.Tag.SMALL, ca);
            tagMap.put(HTML.Tag.SPAN, ca);
            tagMap.put(HTML.Tag.STRIKE, conv);
            tagMap.put(HTML.Tag.S, ca);
            tagMap.put(HTML.Tag.STRONG, ca);
            tagMap.put(HTML.Tag.STYLE, new StyleAction());
            tagMap.put(HTML.Tag.SUB, conv);
            tagMap.put(HTML.Tag.SUP, conv);
            tagMap.put(HTML.Tag.TABLE, ba);
            tagMap.put(HTML.Tag.TD, ba);
            tagMap.put(HTML.Tag.TEXTAREA, fa);
            tagMap.put(HTML.Tag.TH, ba);
            tagMap.put(HTML.Tag.TITLE, new TitleAction());
            tagMap.put(HTML.Tag.TR, ba);
            tagMap.put(HTML.Tag.TT, ca);
            tagMap.put(HTML.Tag.U, conv);
            tagMap.put(HTML.Tag.UL, ba);
            tagMap.put(HTML.Tag.VAR, ca);

            if (insertTag != null) {
                this.insertTag = insertTag;
                this.popDepth = popDepth;
                this.pushDepth = pushDepth;
                this.insertInsertTag = insertInsertTag;
                foundInsertTag = false;
            }
            else {
                foundInsertTag = true;
            }
            if (insertAfterImplied) {
                this.popDepth = popDepth;
                this.pushDepth = pushDepth;
                this.insertAfterImplied = true;
                foundInsertTag = false;
                midInsert = false;
                this.insertInsertTag = true;
                this.wantsTrailingNewline = wantsTrailingNewline;
            }
            else {
                midInsert = (!emptyDocument && insertTag == null);
                if (midInsert) {
                    generateEndsSpecsForMidInsert();
                }
            }

            /**
             * This block initializes the <code>inParagraph</code> flag.
             * It is left in <code>false</code> value automatically
             * if the target document is empty or future inserts
             * were positioned into the 'body' tag.
             */
            if (!emptyDocument && !midInsert) {
                int targetOffset = Math.max(this.offset - 1, 0);
                Element elem =
                        HTMLDocument.this.getCharacterElement(targetOffset);
                /* Going up by the left document structure path */
                for (int i = 0; i <= this.popDepth; i++) {
                    elem = elem.getParentElement();
                }
                /* Going down by the right document structure path */
                for (int i = 0; i < this.pushDepth; i++) {
                    int index = elem.getElementIndex(this.offset);
                    elem = elem.getElement(index);
                }
                AttributeSet attrs = elem.getAttributes();
                if (attrs != null) {
                    HTML.Tag tagToInsertInto =
                            (HTML.Tag) attrs.getAttribute(StyleConstants.NameAttribute);
                    if (tagToInsertInto != null) {
                        this.inParagraph = tagToInsertInto.isParagraph();
                    }
                }
            }
        }

        /**
         * Generates an initial batch of end <code>ElementSpecs</code>
         * in parseBuffer to position future inserts into the body.
         */
        private void generateEndsSpecsForMidInsert() {
            int           count = heightToElementWithName(HTML.Tag.BODY,
                                                   Math.max(0, offset - 1));
            boolean       joinNext = false;

            if (count == -1 && offset > 0) {
                count = heightToElementWithName(HTML.Tag.BODY, offset);
                if (count != -1) {
                    // Previous isn't in body, but current is. Have to
                    // do some end specs, followed by join next.
                    count = depthTo(offset - 1) - 1;
                    joinNext = true;
                }
            }
            if (count == -1) {
                throw new RuntimeException("Must insert new content into body element-");
            }
            if (count != -1) {
                // Insert a newline, if necessary.
                try {
                    if (!joinNext && offset > 0 &&
                        !getText(offset - 1, 1).equals("\n")) {
                        SimpleAttributeSet newAttrs = new SimpleAttributeSet();
                        newAttrs.addAttribute(StyleConstants.NameAttribute,
                                              HTML.Tag.CONTENT);
                        ElementSpec spec = new ElementSpec(newAttrs,
                                    ElementSpec.ContentType, NEWLINE, 0, 1);
                        parseBuffer.addElement(spec);
                    }
                    // Should never throw, but will catch anyway.
                } catch (BadLocationException ble) {}
                while (count-- > 0) {
                    parseBuffer.addElement(new ElementSpec
                                           (null, ElementSpec.EndTagType));
                }
                if (joinNext) {
                    ElementSpec spec = new ElementSpec(null, ElementSpec.
                                                       StartTagType);

                    spec.setDirection(ElementSpec.JoinNextDirection);
                    parseBuffer.addElement(spec);
                }
            }
            // We should probably throw an exception if (count == -1)
            // Or look for the body and reset the offset.
        }

        /**
         * @return number of parents to reach the child at offset.
         */
        private int depthTo(int offset) {
            Element       e = getDefaultRootElement();
            int           count = 0;

            while (!e.isLeaf()) {
                count++;
                e = e.getElement(e.getElementIndex(offset));
            }
            return count;
        }

        /**
         * @return number of parents of the leaf at <code>offset</code>
         *         until a parent with name, <code>name</code> has been
         *         found. -1 indicates no matching parent with
         *         <code>name</code>.
         */
        private int heightToElementWithName(Object name, int offset) {
            Element       e = getCharacterElement(offset).getParentElement();
            int           count = 0;

            while (e != null && e.getAttributes().getAttribute
                   (StyleConstants.NameAttribute) != name) {
                count++;
                e = e.getParentElement();
            }
            return (e == null) ? -1 : count;
        }

        /**
         * This will make sure there aren't two BODYs (the second is
         * typically created when you do a remove all, and then an insert).
         */
        private void adjustEndElement() {
            int length = getLength();
            if (length == 0) {
                return;
            }
            obtainLock();
            try {
                Element[] pPath = getPathTo(length - 1);
                int pLength = pPath.length;
                if (pLength > 1 && pPath[1].getAttributes().getAttribute
                         (StyleConstants.NameAttribute) == HTML.Tag.BODY &&
                         pPath[1].getEndOffset() == length) {
                    String lastText = getText(length - 1, 1);
                    DefaultDocumentEvent event;
                    Element[] added;
                    Element[] removed;
                    int index;
                    // Remove the fake second body.
                    added = new Element[0];
                    removed = new Element[1];
                    index = pPath[0].getElementIndex(length);
                    removed[0] = pPath[0].getElement(index);
                    ((BranchElement)pPath[0]).replace(index, 1, added);
                    ElementEdit firstEdit = new ElementEdit(pPath[0], index,
                                                            removed, added);

                    // Insert a new element to represent the end that the
                    // second body was representing.
                    SimpleAttributeSet sas = new SimpleAttributeSet();
                    sas.addAttribute(StyleConstants.NameAttribute,
                                         HTML.Tag.CONTENT);
                    sas.addAttribute(IMPLIED_CR, Boolean.TRUE);
                    added = new Element[1];
                    added[0] = createLeafElement(pPath[pLength - 1],
                                                     sas, length, length + 1);
                    index = pPath[pLength - 1].getElementCount();
                    ((BranchElement)pPath[pLength - 1]).replace(index, 0,
                                                                added);
                    event = new DefaultDocumentEvent(length, 1,
                                            DocumentEvent.EventType.CHANGE);
                    event.addEdit(new ElementEdit(pPath[pLength - 1],
                                         index, new Element[0], added));
                    event.addEdit(firstEdit);
                    event.end();
                    fireChangedUpdate(event);
                    fireUndoableEditUpdate(new UndoableEditEvent(this, event));

                    if (lastText.equals("\n")) {
                        // We now have two \n's, one part of the Document.
                        // We need to remove one
                        event = new DefaultDocumentEvent(length - 1, 1,
                                           DocumentEvent.EventType.REMOVE);
                        removeUpdate(event);
                        UndoableEdit u = getContent().remove(length - 1, 1);
                        if (u != null) {
                            event.addEdit(u);
                        }
                        postRemoveUpdate(event);
                        // Mark the edit as done.
                        event.end();
                        fireRemoveUpdate(event);
                        fireUndoableEditUpdate(new UndoableEditEvent(
                                               this, event));
                    }
                }
            }
            catch (BadLocationException ble) {
            }
            finally {
                releaseLock();
            }
        }

        private Element[] getPathTo(int offset) {
            Stack<Element> elements = new Stack<Element>();
            Element e = getDefaultRootElement();
            int index;
            while (!e.isLeaf()) {
                elements.push(e);
                e = e.getElement(e.getElementIndex(offset));
            }
            Element[] retValue = new Element[elements.size()];
            elements.copyInto(retValue);
            return retValue;
        }

        // -- HTMLEditorKit.ParserCallback methods --------------------

        /**
         * The last method called on the reader.  It allows
         * any pending changes to be flushed into the document.
         * Since this is currently loading synchronously, the entire
         * set of changes are pushed in at this point.
         */
        public void flush() throws BadLocationException {
            if (emptyDocument && !insertAfterImplied) {
                if (HTMLDocument.this.getLength() > 0 ||
                                      parseBuffer.size() > 0) {
                    flushBuffer(true);
                    adjustEndElement();
                }
                // We won't insert when
            }
            else {
                flushBuffer(true);
            }
        }

        /**
         * Called by the parser to indicate a block of text was
         * encountered.
         */
        public void handleText(char[] data, int pos) {
            if (receivedEndHTML || (midInsert && !inBody)) {
                return;
            }

            // see if complex glyph layout support is needed
            if(HTMLDocument.this.getProperty(I18NProperty).equals( Boolean.FALSE ) ) {
                // if a default direction of right-to-left has been specified,
                // we want complex layout even if the text is all left to right.
                Object d = getProperty(TextAttribute.RUN_DIRECTION);
                if ((d != null) && (d.equals(TextAttribute.RUN_DIRECTION_RTL))) {
                    HTMLDocument.this.putProperty( I18NProperty, Boolean.TRUE);
                } else {
                    if (SwingUtilities2.isComplexLayout(data, 0, data.length)) {
                        HTMLDocument.this.putProperty( I18NProperty, Boolean.TRUE);
                    }
                }
            }

            if (inTextArea) {
                textAreaContent(data);
            } else if (inPre) {
                preContent(data);
            } else if (inTitle) {
                putProperty(Document.TitleProperty, new String(data));
            } else if (option != null) {
                option.setLabel(new String(data));
            } else if (inStyle) {
                if (styles != null) {
                    styles.addElement(new String(data));
                }
            } else if (inBlock > 0) {
                if (!foundInsertTag && insertAfterImplied) {
                    // Assume content should be added.
                    foundInsertTag(false);
                    foundInsertTag = true;
                    // If content is added directly to the body, it should
                    // be wrapped by p-implied.
                    inParagraph = impliedP = !insertInBody;
                }
                if (data.length >= 1) {
                    addContent(data, 0, data.length);
                }
            }
        }

        /**
         * Callback from the parser.  Route to the appropriate
         * handler for the tag.
         */
        public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            if (receivedEndHTML) {
                return;
            }
            if (midInsert && !inBody) {
                if (t == HTML.Tag.BODY) {
                    inBody = true;
                    // Increment inBlock since we know we are in the body,
                    // this is needed incase an implied-p is needed. If
                    // inBlock isn't incremented, and an implied-p is
                    // encountered, addContent won't be called!
                    inBlock++;
                }
                return;
            }
            if (!inBody && t == HTML.Tag.BODY) {
                inBody = true;
            }
            if (isStyleCSS && a.isDefined(HTML.Attribute.STYLE)) {
                // Map the style attributes.
                String decl = (String)a.getAttribute(HTML.Attribute.STYLE);
                a.removeAttribute(HTML.Attribute.STYLE);
                styleAttributes = getStyleSheet().getDeclaration(decl);
                a.addAttributes(styleAttributes);
            }
            else {
                styleAttributes = null;
            }
            TagAction action = tagMap.get(t);

            if (action != null) {
                action.start(t, a);
            }
        }

        public void handleComment(char[] data, int pos) {
            if (receivedEndHTML) {
                addExternalComment(new String(data));
                return;
            }
            if (inStyle) {
                if (styles != null) {
                    styles.addElement(new String(data));
                }
            }
            else if (getPreservesUnknownTags()) {
                if (inBlock == 0 && (foundInsertTag ||
                                     insertTag != HTML.Tag.COMMENT)) {
                    // Comment outside of body, will not be able to show it,
                    // but can add it as a property on the Document.
                    addExternalComment(new String(data));
                    return;
                }
                SimpleAttributeSet sas = new SimpleAttributeSet();
                sas.addAttribute(HTML.Attribute.COMMENT, new String(data));
                addSpecialElement(HTML.Tag.COMMENT, sas);
            }

            TagAction action = tagMap.get(HTML.Tag.COMMENT);
            if (action != null) {
                action.start(HTML.Tag.COMMENT, new SimpleAttributeSet());
                action.end(HTML.Tag.COMMENT);
            }
        }

        /**
         * Adds the comment <code>comment</code> to the set of comments
         * maintained outside of the scope of elements.
         */
        private void addExternalComment(String comment) {
            Object comments = getProperty(AdditionalComments);
            if (comments != null && !(comments instanceof Vector)) {
                // No place to put comment.
                return;
            }
            if (comments == null) {
                comments = new Vector<>();
                putProperty(AdditionalComments, comments);
            }
            @SuppressWarnings("unchecked")
            Vector<Object> v = (Vector<Object>)comments;
            v.addElement(comment);
        }

        /**
         * Callback from the parser.  Route to the appropriate
         * handler for the tag.
         */
        public void handleEndTag(HTML.Tag t, int pos) {
            if (receivedEndHTML || (midInsert && !inBody)) {
                return;
            }
            if (t == HTML.Tag.HTML) {
                receivedEndHTML = true;
            }
            if (t == HTML.Tag.BODY) {
                inBody = false;
                if (midInsert) {
                    inBlock--;
                }
            }
            TagAction action = tagMap.get(t);
            if (action != null) {
                action.end(t);
            }
        }

        /**
         * Callback from the parser.  Route to the appropriate
         * handler for the tag.
         */
        public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
            if (receivedEndHTML || (midInsert && !inBody)) {
                return;
            }

            if (isStyleCSS && a.isDefined(HTML.Attribute.STYLE)) {
                // Map the style attributes.
                String decl = (String)a.getAttribute(HTML.Attribute.STYLE);
                a.removeAttribute(HTML.Attribute.STYLE);
                styleAttributes = getStyleSheet().getDeclaration(decl);
                a.addAttributes(styleAttributes);
            }
            else {
                styleAttributes = null;
            }

            TagAction action = tagMap.get(t);
            if (action != null) {
                action.start(t, a);
                action.end(t);
            }
            else if (getPreservesUnknownTags()) {
                // unknown tag, only add if should preserve it.
                addSpecialElement(t, a);
            }
        }

        /**
         * This is invoked after the stream has been parsed, but before
         * <code>flush</code>. <code>eol</code> will be one of \n, \r
         * or \r\n, which ever is encountered the most in parsing the
         * stream.
         *
         * @since 1.3
         */
        public void handleEndOfLineString(String eol) {
            if (emptyDocument && eol != null) {
                putProperty(DefaultEditorKit.EndOfLineStringProperty,
                            eol);
            }
        }

        // ---- tag handling support ------------------------------

        /**
         * Registers a handler for the given tag.  By default
         * all of the well-known tags will have been registered.
         * This can be used to change the handling of a particular
         * tag or to add support for custom tags.
         *
         * @param t an HTML tag
         * @param a tag action handler
         */
        protected void registerTag(HTML.Tag t, TagAction a) {
            tagMap.put(t, a);
        }

        /**
         * An action to be performed in response
         * to parsing a tag.  This allows customization
         * of how each tag is handled and avoids a large
         * switch statement.
         */
        public class TagAction {
            /**
             * Constructs a {@code TagAction}.
             */
            public TagAction() {}

            /**
             * Called when a start tag is seen for the
             * type of tag this action was registered
             * to.  The tag argument indicates the actual
             * tag for those actions that are shared across
             * many tags.  By default this does nothing and
             * completely ignores the tag.
             *
             * @param t the HTML tag
             * @param a the attributes
             */
            public void start(HTML.Tag t, MutableAttributeSet a) {
            }

            /**
             * Called when an end tag is seen for the
             * type of tag this action was registered
             * to.  The tag argument indicates the actual
             * tag for those actions that are shared across
             * many tags.  By default this does nothing and
             * completely ignores the tag.
             *
             * @param t the HTML tag
             */
            public void end(HTML.Tag t) {
            }

        }

        /**
         * Action assigned by default to handle the Block task of the reader.
         */
        public class BlockAction extends TagAction {
            /**
             * Constructs a {@code BlockAction}.
             */
            public BlockAction() {}

            public void start(HTML.Tag t, MutableAttributeSet attr) {
                blockOpen(t, attr);
            }

            public void end(HTML.Tag t) {
                blockClose(t);
            }
        }


        /**
         * Action used for the actual element form tag. This is named such
         * as there was already a public class named FormAction.
         */
        private class FormTagAction extends BlockAction {
            public void start(HTML.Tag t, MutableAttributeSet attr) {
                super.start(t, attr);
                // initialize a ButtonGroupsMap when
                // FORM tag is encountered.  This will
                // be used for any radio buttons that
                // might be defined in the FORM.
                // for new group new ButtonGroup will be created (fix for 4529702)
                // group name is a key in radioButtonGroupsMap
                radioButtonGroupsMap = new HashMap<String, ButtonGroup>();
            }

            public void end(HTML.Tag t) {
                super.end(t);
                // reset the button group to null since
                // the form has ended.
                radioButtonGroupsMap = null;
            }
        }


        /**
         * Action assigned by default to handle the Paragraph task of the reader.
         */
        public class ParagraphAction extends BlockAction {

            /**
             * Constructs a {@code ParagraphAction}.
             */
            public ParagraphAction() {}

            public void start(HTML.Tag t, MutableAttributeSet a) {
                super.start(t, a);
                inParagraph = true;
            }

            public void end(HTML.Tag t) {
                super.end(t);
                inParagraph = false;
            }
        }

        /**
         * Action assigned by default to handle the Special task of the reader.
         */
        public class SpecialAction extends TagAction {
            /**
             * Constructs a {@code SpecialAction}.
             */
            public SpecialAction() {}

            public void start(HTML.Tag t, MutableAttributeSet a) {
                addSpecialElement(t, a);
            }

        }

        /**
         * Action assigned by default to handle the Isindex task of the reader.
         */
        public class IsindexAction extends TagAction {

            /**
             * Constructs a {@code IsindexAction}.
             */
            public IsindexAction() {}

            public void start(HTML.Tag t, MutableAttributeSet a) {
                blockOpen(HTML.Tag.IMPLIED, new SimpleAttributeSet());
                addSpecialElement(t, a);
                blockClose(HTML.Tag.IMPLIED);
            }

        }


        /**
         * Action assigned by default to handle the Hidden task of the reader.
         */
        public class HiddenAction extends TagAction {

            /**
             * Constructs a {@code HiddenAction}.
             */
            public HiddenAction() {}

            public void start(HTML.Tag t, MutableAttributeSet a) {
                addSpecialElement(t, a);
            }

            public void end(HTML.Tag t) {
                if (!isEmpty(t)) {
                    MutableAttributeSet a = new SimpleAttributeSet();
                    a.addAttribute(HTML.Attribute.ENDTAG, "true");
                    addSpecialElement(t, a);
                }
            }

            boolean isEmpty(HTML.Tag t) {
                if (t == HTML.Tag.APPLET ||
                    t == HTML.Tag.SCRIPT) {
                    return false;
                }
                return true;
            }
        }


        /**
         * Subclass of HiddenAction to set the content type for style sheets,
         * and to set the name of the default style sheet.
         */
        class MetaAction extends HiddenAction {

            public void start(HTML.Tag t, MutableAttributeSet a) {
                Object equiv = a.getAttribute(HTML.Attribute.HTTPEQUIV);
                if (equiv != null) {
                    equiv = ((String)equiv).toLowerCase();
                    if (equiv.equals("content-style-type")) {
                        String value = (String)a.getAttribute
                                       (HTML.Attribute.CONTENT);
                        setDefaultStyleSheetType(value);
                        isStyleCSS = "text/css".equals
                                      (getDefaultStyleSheetType());
                    }
                    else if (equiv.equals("default-style")) {
                        defaultStyle = (String)a.getAttribute
                                       (HTML.Attribute.CONTENT);
                    }
                }
                super.start(t, a);
            }

            boolean isEmpty(HTML.Tag t) {
                return true;
            }
        }


        /**
         * End if overridden to create the necessary stylesheets that
         * are referenced via the link tag. It is done in this manner
         * as the meta tag can be used to specify an alternate style sheet,
         * and is not guaranteed to come before the link tags.
         */
        class HeadAction extends BlockAction {

            public void start(HTML.Tag t, MutableAttributeSet a) {
                inHead = true;
                // This check of the insertTag is put in to avoid considering
                // the implied-p that is generated for the head. This allows
                // inserts for HR to work correctly.
                if ((insertTag == null && !insertAfterImplied) ||
                    (insertTag == HTML.Tag.HEAD) ||
                    (insertAfterImplied &&
                     (foundInsertTag || !a.isDefined(IMPLIED)))) {
                    super.start(t, a);
                }
            }

            public void end(HTML.Tag t) {
                inHead = inStyle = false;
                // See if there is a StyleSheet to link to.
                if (styles != null) {
                    boolean isDefaultCSS = isStyleCSS;
                    for (int counter = 0, maxCounter = styles.size();
                         counter < maxCounter;) {
                        Object value = styles.elementAt(counter);
                        if (value == HTML.Tag.LINK) {
                            handleLink((AttributeSet)styles.
                                       elementAt(++counter));
                            counter++;
                        }
                        else {
                            // Rule.
                            // First element gives type.
                            String type = (String)styles.elementAt(++counter);
                            boolean isCSS = (type == null) ? isDefaultCSS :
                                            type.equals("text/css");
                            while (++counter < maxCounter &&
                                   (styles.elementAt(counter)
                                    instanceof String)) {
                                if (isCSS) {
                                    addCSSRules((String)styles.elementAt
                                                (counter));
                                }
                            }
                        }
                    }
                }
                if ((insertTag == null && !insertAfterImplied) ||
                    insertTag == HTML.Tag.HEAD ||
                    (insertAfterImplied && foundInsertTag)) {
                    super.end(t);
                }
            }

            boolean isEmpty(HTML.Tag t) {
                return false;
            }

            private void handleLink(AttributeSet attr) {
                // Link.
                String type = (String)attr.getAttribute(HTML.Attribute.TYPE);
                if (type == null) {
                    type = getDefaultStyleSheetType();
                }
                // Only choose if type==text/css
                // Select link if rel==stylesheet.
                // Otherwise if rel==alternate stylesheet and
                //   title matches default style.
                if (type.equals("text/css")) {
                    String rel = (String)attr.getAttribute(HTML.Attribute.REL);
                    String title = (String)attr.getAttribute
                                               (HTML.Attribute.TITLE);
                    String media = (String)attr.getAttribute
                                                   (HTML.Attribute.MEDIA);
                    if (media == null) {
                        media = "all";
                    }
                    else {
                        media = media.toLowerCase();
                    }
                    if (rel != null) {
                        rel = rel.toLowerCase();
                        if ((media.indexOf("all") != -1 ||
                             media.indexOf("screen") != -1) &&
                            (rel.equals("stylesheet") ||
                             (rel.equals("alternate stylesheet") &&
                              title.equals(defaultStyle)))) {
                            linkCSSStyleSheet((String)attr.getAttribute
                                              (HTML.Attribute.HREF));
                        }
                    }
                }
            }
        }


        /**
         * A subclass to add the AttributeSet to styles if the
         * attributes contains an attribute for 'rel' with value
         * 'stylesheet' or 'alternate stylesheet'.
         */
        class LinkAction extends HiddenAction {

            public void start(HTML.Tag t, MutableAttributeSet a) {
                String rel = (String)a.getAttribute(HTML.Attribute.REL);
                if (rel != null) {
                    rel = rel.toLowerCase();
                    if (rel.equals("stylesheet") ||
                        rel.equals("alternate stylesheet")) {
                        if (styles == null) {
                            styles = new Vector<Object>(3);
                        }
                        styles.addElement(t);
                        styles.addElement(a.copyAttributes());
                    }
                }
                super.start(t, a);
            }
        }

        class MapAction extends TagAction {

            public void start(HTML.Tag t, MutableAttributeSet a) {
                lastMap = new Map((String)a.getAttribute(HTML.Attribute.NAME));
                addMap(lastMap);
            }

            public void end(HTML.Tag t) {
            }
        }


        class AreaAction extends TagAction {

            public void start(HTML.Tag t, MutableAttributeSet a) {
                if (lastMap != null) {
                    lastMap.addArea(a.copyAttributes());
                }
            }

            public void end(HTML.Tag t) {
            }
        }


        class StyleAction extends TagAction {

            public void start(HTML.Tag t, MutableAttributeSet a) {
                if (inHead) {
                    if (styles == null) {
                        styles = new Vector<Object>(3);
                    }
                    styles.addElement(t);
                    styles.addElement(a.getAttribute(HTML.Attribute.TYPE));
                    inStyle = true;
                }
            }

            public void end(HTML.Tag t) {
                inStyle = false;
            }

            boolean isEmpty(HTML.Tag t) {
                return false;
            }
        }

        /**
         * Action assigned by default to handle the Pre block task of the reader.
         */
        public class PreAction extends BlockAction {

            /**
             * Constructs a {@code PreAction}.
             */
            public PreAction() {}

            public void start(HTML.Tag t, MutableAttributeSet attr) {
                inPre = true;
                blockOpen(t, attr);
                attr.addAttribute(CSS.Attribute.WHITE_SPACE, "pre");
                blockOpen(HTML.Tag.IMPLIED, attr);
            }

            public void end(HTML.Tag t) {
                blockClose(HTML.Tag.IMPLIED);
                // set inPre to false after closing, so that if a newline
                // is added it won't generate a blockOpen.
                inPre = false;
                blockClose(t);
            }
        }

        /**
         * Action assigned by default to handle the Character task of the reader.
         */
        public class CharacterAction extends TagAction {

            /**
             * Constructs a {@code CharacterAction}.
             */
            public CharacterAction() {}

            public void start(HTML.Tag t, MutableAttributeSet attr) {
                pushCharacterStyle();
                if (!foundInsertTag) {
                    // Note that the third argument should really be based off
                    // inParagraph and impliedP. If we're wrong (that is
                    // insertTagDepthDelta shouldn't be changed), we'll end up
                    // removing an extra EndSpec, which won't matter anyway.
                    boolean insert = canInsertTag(t, attr, false);
                    if (foundInsertTag) {
                        if (!inParagraph) {
                            inParagraph = impliedP = true;
                        }
                    }
                    if (!insert) {
                        return;
                    }
                }
                if (attr.isDefined(IMPLIED)) {
                    attr.removeAttribute(IMPLIED);
                }
                charAttr.addAttribute(t, attr.copyAttributes());
                if (styleAttributes != null) {
                    charAttr.addAttributes(styleAttributes);
                }
            }

            public void end(HTML.Tag t) {
                popCharacterStyle();
            }
        }

        /**
         * Provides conversion of HTML tag/attribute
         * mappings that have a corresponding StyleConstants
         * and CSS mapping.  The conversion is to CSS attributes.
         */
        class ConvertAction extends TagAction {

            public void start(HTML.Tag t, MutableAttributeSet attr) {
                pushCharacterStyle();
                if (!foundInsertTag) {
                    // Note that the third argument should really be based off
                    // inParagraph and impliedP. If we're wrong (that is
                    // insertTagDepthDelta shouldn't be changed), we'll end up
                    // removing an extra EndSpec, which won't matter anyway.
                    boolean insert = canInsertTag(t, attr, false);
                    if (foundInsertTag) {
                        if (!inParagraph) {
                            inParagraph = impliedP = true;
                        }
                    }
                    if (!insert) {
                        return;
                    }
                }
                if (attr.isDefined(IMPLIED)) {
                    attr.removeAttribute(IMPLIED);
                }
                if (styleAttributes != null) {
                    charAttr.addAttributes(styleAttributes);
                }
                // We also need to add attr, otherwise we lose custom
                // attributes, including class/id for style lookups, and
                // further confuse style lookup (doesn't have tag).
                charAttr.addAttribute(t, attr.copyAttributes());
                StyleSheet sheet = getStyleSheet();
                if (t == HTML.Tag.B) {
                    sheet.addCSSAttribute(charAttr, CSS.Attribute.FONT_WEIGHT, "bold");
                } else if (t == HTML.Tag.I) {
                    sheet.addCSSAttribute(charAttr, CSS.Attribute.FONT_STYLE, "italic");
                } else if (t == HTML.Tag.U) {
                    Object v = charAttr.getAttribute(CSS.Attribute.TEXT_DECORATION);
                    String value = "underline";
                    value = (v != null) ? value + "," + v.toString() : value;
                    sheet.addCSSAttribute(charAttr, CSS.Attribute.TEXT_DECORATION, value);
                } else if (t == HTML.Tag.STRIKE) {
                    Object v = charAttr.getAttribute(CSS.Attribute.TEXT_DECORATION);
                    String value = "line-through";
                    value = (v != null) ? value + "," + v.toString() : value;
                    sheet.addCSSAttribute(charAttr, CSS.Attribute.TEXT_DECORATION, value);
                } else if (t == HTML.Tag.SUP) {
                    Object v = charAttr.getAttribute(CSS.Attribute.VERTICAL_ALIGN);
                    String value = "sup";
                    value = (v != null) ? value + "," + v.toString() : value;
                    sheet.addCSSAttribute(charAttr, CSS.Attribute.VERTICAL_ALIGN, value);
                } else if (t == HTML.Tag.SUB) {
                    Object v = charAttr.getAttribute(CSS.Attribute.VERTICAL_ALIGN);
                    String value = "sub";
                    value = (v != null) ? value + "," + v.toString() : value;
                    sheet.addCSSAttribute(charAttr, CSS.Attribute.VERTICAL_ALIGN, value);
                } else if (t == HTML.Tag.FONT) {
                    String color = (String) attr.getAttribute(HTML.Attribute.COLOR);
                    if (color != null) {
                        sheet.addCSSAttribute(charAttr, CSS.Attribute.COLOR, color);
                    }
                    String face = (String) attr.getAttribute(HTML.Attribute.FACE);
                    if (face != null) {
                        sheet.addCSSAttribute(charAttr, CSS.Attribute.FONT_FAMILY, face);
                    }
                    String size = (String) attr.getAttribute(HTML.Attribute.SIZE);
                    if (size != null) {
                        sheet.addCSSAttributeFromHTML(charAttr, CSS.Attribute.FONT_SIZE, size);
                    }
                }
            }

            public void end(HTML.Tag t) {
                popCharacterStyle();
            }

        }

        class AnchorAction extends CharacterAction {

            public void start(HTML.Tag t, MutableAttributeSet attr) {
                // set flag to catch empty anchors
                emptyAnchor = true;
                super.start(t, attr);
            }

            public void end(HTML.Tag t) {
                if (emptyAnchor) {
                    // if the anchor was empty it was probably a
                    // named anchor point and we don't want to throw
                    // it away.
                    char[] one = new char[1];
                    one[0] = '\n';
                    addContent(one, 0, 1);
                }
                super.end(t);
            }
        }

        class TitleAction extends HiddenAction {

            public void start(HTML.Tag t, MutableAttributeSet attr) {
                inTitle = true;
                super.start(t, attr);
            }

            public void end(HTML.Tag t) {
                inTitle = false;
                super.end(t);
            }

            boolean isEmpty(HTML.Tag t) {
                return false;
            }
        }


        class BaseAction extends TagAction {

            public void start(HTML.Tag t, MutableAttributeSet attr) {
                String href = (String) attr.getAttribute(HTML.Attribute.HREF);
                if (href != null) {
                    try {
                        URL newBase = new URL(base, href);
                        setBase(newBase);
                        hasBaseTag = true;
                    } catch (MalformedURLException ex) {
                    }
                }
                baseTarget = (String) attr.getAttribute(HTML.Attribute.TARGET);
            }
        }

        class ObjectAction extends SpecialAction {

            public void start(HTML.Tag t, MutableAttributeSet a) {
                if (t == HTML.Tag.PARAM) {
                    addParameter(a);
                } else {
                    super.start(t, a);
                }
            }

            public void end(HTML.Tag t) {
                if (t != HTML.Tag.PARAM) {
                    super.end(t);
                }
            }

            void addParameter(AttributeSet a) {
                String name = (String) a.getAttribute(HTML.Attribute.NAME);
                String value = (String) a.getAttribute(HTML.Attribute.VALUE);
                if ((name != null) && (value != null)) {
                    ElementSpec objSpec = parseBuffer.lastElement();
                    MutableAttributeSet objAttr = (MutableAttributeSet) objSpec.getAttributes();
                    objAttr.addAttribute(name, value);
                }
            }
        }

        /**
         * Action to support forms by building all of the elements
         * used to represent form controls.  This will process
         * the &lt;INPUT&gt;, &lt;TEXTAREA&gt;, &lt;SELECT&gt;,
         * and &lt;OPTION&gt; tags.  The element created by
         * this action is expected to have the attribute
         * <code>StyleConstants.ModelAttribute</code> set to
         * the model that holds the state for the form control.
         * This enables multiple views, and allows document to
         * be iterated over picking up the data of the form.
         * The following are the model assignments for the
         * various type of form elements.
         *
         * <table class="striped">
         * <caption>Model assignments for the various types of form elements
         * </caption>
         * <thead>
         *   <tr>
         *     <th scope="col">Element Type
         *     <th scope="col">Model Type
         * </thead>
         * <tbody>
         *   <tr>
         *     <th scope="row">input, type button
         *     <td>{@link DefaultButtonModel}
         *   <tr>
         *     <th scope="row">input, type checkbox
         *     <td>{@link JToggleButton.ToggleButtonModel}
         *   <tr>
         *     <th scope="row">input, type image
         *     <td>{@link DefaultButtonModel}
         *   <tr>
         *     <th scope="row">input, type password
         *     <td>{@link PlainDocument}
         *   <tr>
         *     <th scope="row">input, type radio
         *     <td>{@link JToggleButton.ToggleButtonModel}
         *   <tr>
         *     <th scope="row">input, type reset
         *     <td>{@link DefaultButtonModel}
         *   <tr>
         *     <th scope="row">input, type submit
         *     <td>{@link DefaultButtonModel}
         *   <tr>
         *     <th scope="row">input, type text or type is null.
         *     <td>{@link PlainDocument}
         *   <tr>
         *     <th scope="row">select
         *     <td>{@link DefaultComboBoxModel} or an {@link DefaultListModel},
         *     with an item type of Option
         *   <tr>
         *     <th scope="row">textarea
         *     <td>{@link PlainDocument}
         * </tbody>
         * </table>
         */
        public class FormAction extends SpecialAction {

            /**
             * Constructs a {@code FormAction}.
             */
            public FormAction() {}

            public void start(HTML.Tag t, MutableAttributeSet attr) {
                if (t == HTML.Tag.INPUT) {
                    String type = (String)
                        attr.getAttribute(HTML.Attribute.TYPE);
                    /*
                     * if type is not defined the default is
                     * assumed to be text.
                     */
                    if (type == null) {
                        type = "text";
                        attr.addAttribute(HTML.Attribute.TYPE, "text");
                    }
                    setModel(type, attr);
                } else if (t == HTML.Tag.TEXTAREA) {
                    inTextArea = true;
                    textAreaDocument = new TextAreaDocument();
                    attr.addAttribute(StyleConstants.ModelAttribute,
                                      textAreaDocument);
                } else if (t == HTML.Tag.SELECT) {
                    int size = HTML.getIntegerAttributeValue(attr,
                                                             HTML.Attribute.SIZE,
                                                             1);
                    boolean multiple = attr.getAttribute(HTML.Attribute.MULTIPLE) != null;
                    if ((size > 1) || multiple) {
                        OptionListModel<Option> m = new OptionListModel<Option>();
                        if (multiple) {
                            m.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                        }
                        selectModel = m;
                    } else {
                        selectModel = new OptionComboBoxModel<Option>();
                    }
                    attr.addAttribute(StyleConstants.ModelAttribute,
                                      selectModel);

                }

                // build the element, unless this is an option.
                if (t == HTML.Tag.OPTION) {
                    option = new Option(attr);

                    if (selectModel instanceof OptionListModel) {
                        @SuppressWarnings("unchecked")
                        OptionListModel<Option> m = (OptionListModel<Option>) selectModel;
                        m.addElement(option);
                        if (option.isSelected()) {
                            m.addSelectionInterval(optionCount, optionCount);
                            m.setInitialSelection(optionCount);
                        }
                    } else if (selectModel instanceof OptionComboBoxModel) {
                        @SuppressWarnings("unchecked")
                        OptionComboBoxModel<Option> m = (OptionComboBoxModel<Option>) selectModel;
                        m.addElement(option);
                        if (option.isSelected()) {
                            m.setSelectedItem(option);
                            m.setInitialSelection(option);
                        }
                    }
                    optionCount++;
                } else {
                    super.start(t, attr);
                }
            }

            public void end(HTML.Tag t) {
                if (t == HTML.Tag.OPTION) {
                    option = null;
                } else {
                    if (t == HTML.Tag.SELECT) {
                        selectModel = null;
                        optionCount = 0;
                    } else if (t == HTML.Tag.TEXTAREA) {
                        inTextArea = false;

                        /* Now that the textarea has ended,
                         * store the entire initial text
                         * of the text area.  This will
                         * enable us to restore the initial
                         * state if a reset is requested.
                         */
                        textAreaDocument.storeInitialText();
                    }
                    super.end(t);
                }
            }

            void setModel(String type, MutableAttributeSet attr) {
                if (type.equals("submit") ||
                    type.equals("reset") ||
                    type.equals("image")) {

                    // button model
                    attr.addAttribute(StyleConstants.ModelAttribute,
                                      new DefaultButtonModel());
                } else if (type.equals("text") ||
                           type.equals("password")) {
                    // plain text model
                    int maxLength = HTML.getIntegerAttributeValue(
                                       attr, HTML.Attribute.MAXLENGTH, -1);
                    Document doc;

                    if (maxLength > 0) {
                        doc = new FixedLengthDocument(maxLength);
                    }
                    else {
                        doc = new PlainDocument();
                    }
                    String value = (String)
                        attr.getAttribute(HTML.Attribute.VALUE);
                    try {
                        doc.insertString(0, value, null);
                    } catch (BadLocationException e) {
                    }
                    attr.addAttribute(StyleConstants.ModelAttribute, doc);
                } else if (type.equals("file")) {
                    // plain text model
                    attr.addAttribute(StyleConstants.ModelAttribute,
                                      new PlainDocument());
                } else if (type.equals("checkbox") ||
                           type.equals("radio")) {
                    JToggleButton.ToggleButtonModel model = new JToggleButton.ToggleButtonModel();
                    if (type.equals("radio")) {
                        String name = (String) attr.getAttribute(HTML.Attribute.NAME);
                        if ( radioButtonGroupsMap == null ) { //fix for 4772743
                           radioButtonGroupsMap = new HashMap<String, ButtonGroup>();
                        }
                        ButtonGroup radioButtonGroup = radioButtonGroupsMap.get(name);
                        if (radioButtonGroup == null) {
                            radioButtonGroup = new ButtonGroup();
                            radioButtonGroupsMap.put(name,radioButtonGroup);
                        }
                        model.setGroup(radioButtonGroup);
                    }
                    boolean checked = (attr.getAttribute(HTML.Attribute.CHECKED) != null);
                    model.setSelected(checked);
                    attr.addAttribute(StyleConstants.ModelAttribute, model);
                }
            }

            /**
             * If a &lt;SELECT&gt; tag is being processed, this
             * model will be a reference to the model being filled
             * with the &lt;OPTION&gt; elements (which produce
             * objects of type <code>Option</code>.
             */
            Object selectModel;
            int optionCount;
        }


        // --- utility methods used by the reader ------------------

        /**
         * Pushes the current character style on a stack in preparation
         * for forming a new nested character style.
         */
        protected void pushCharacterStyle() {
            charAttrStack.push(charAttr.copyAttributes());
        }

        /**
         * Pops a previously pushed character style off the stack
         * to return to a previous style.
         */
        protected void popCharacterStyle() {
            if (!charAttrStack.empty()) {
                charAttr = (MutableAttributeSet) charAttrStack.peek();
                charAttrStack.pop();
            }
        }

        /**
         * Adds the given content to the textarea document.
         * This method gets called when we are in a textarea
         * context.  Therefore all text that is seen belongs
         * to the text area and is hence added to the
         * TextAreaDocument associated with the text area.
         *
         * @param data the given content
         */
        protected void textAreaContent(char[] data) {
            try {
                textAreaDocument.insertString(textAreaDocument.getLength(), new String(data), null);
            } catch (BadLocationException e) {
                // Should do something reasonable
            }
        }

        /**
         * Adds the given content that was encountered in a
         * PRE element.  This synthesizes lines to hold the
         * runs of text, and makes calls to addContent to
         * actually add the text.
         *
         * @param data the given content
         */
        protected void preContent(char[] data) {
            int last = 0;
            for (int i = 0; i < data.length; i++) {
                if (data[i] == '\n') {
                    addContent(data, last, i - last + 1);
                    blockClose(HTML.Tag.IMPLIED);
                    MutableAttributeSet a = new SimpleAttributeSet();
                    a.addAttribute(CSS.Attribute.WHITE_SPACE, "pre");
                    blockOpen(HTML.Tag.IMPLIED, a);
                    last = i + 1;
                }
            }
            if (last < data.length) {
                addContent(data, last, data.length - last);
            }
        }

        /**
         * Adds an instruction to the parse buffer to create a
         * block element with the given attributes.
         *
         * @param t an HTML tag
         * @param attr the attribute set
         */
        protected void blockOpen(HTML.Tag t, MutableAttributeSet attr) {
            if (impliedP) {
                blockClose(HTML.Tag.IMPLIED);
            }

            inBlock++;

            if (!canInsertTag(t, attr, true)) {
                return;
            }
            if (attr.isDefined(IMPLIED)) {
                attr.removeAttribute(IMPLIED);
            }
            lastWasNewline = false;
            attr.addAttribute(StyleConstants.NameAttribute, t);
            ElementSpec es = new ElementSpec(
                attr.copyAttributes(), ElementSpec.StartTagType);
            parseBuffer.addElement(es);
        }

        /**
         * Adds an instruction to the parse buffer to close out
         * a block element of the given type.
         *
         * @param t the HTML tag
         */
        protected void blockClose(HTML.Tag t) {
            inBlock--;

            if (!foundInsertTag) {
                return;
            }

            // Add a new line, if the last character wasn't one. This is
            // needed for proper positioning of the cursor. addContent
            // with true will force an implied paragraph to be generated if
            // there isn't one. This may result in a rather bogus structure
            // (perhaps a table with a child pargraph), but the paragraph
            // is needed for proper positioning and display.
            if(!lastWasNewline) {
                pushCharacterStyle();
                charAttr.addAttribute(IMPLIED_CR, Boolean.TRUE);
                addContent(NEWLINE, 0, 1, true);
                popCharacterStyle();
                lastWasNewline = true;
            }

            if (impliedP) {
                impliedP = false;
                inParagraph = false;
                if (t != HTML.Tag.IMPLIED) {
                    blockClose(HTML.Tag.IMPLIED);
                }
            }
            // an open/close with no content will be removed, so we
            // add a space of content to keep the element being formed.
            ElementSpec prev = (parseBuffer.size() > 0) ?
                parseBuffer.lastElement() : null;
            if (prev != null && prev.getType() == ElementSpec.StartTagType) {
                char[] one = new char[1];
                one[0] = ' ';
                addContent(one, 0, 1);
            }
            ElementSpec es = new ElementSpec(
                null, ElementSpec.EndTagType);
            parseBuffer.addElement(es);
        }

        /**
         * Adds some text with the current character attributes.
         *
         * @param data the content to add
         * @param offs the initial offset
         * @param length the length
         */
        protected void addContent(char[] data, int offs, int length) {
            addContent(data, offs, length, true);
        }

        /**
         * Adds some text with the current character attributes.
         *
         * @param data the content to add
         * @param offs the initial offset
         * @param length the length
         * @param generateImpliedPIfNecessary whether to generate implied
         * paragraphs
         */
        protected void addContent(char[] data, int offs, int length,
                                  boolean generateImpliedPIfNecessary) {
            if (!foundInsertTag) {
                return;
            }

            if (generateImpliedPIfNecessary && (! inParagraph) && (! inPre)) {
                blockOpen(HTML.Tag.IMPLIED, new SimpleAttributeSet());
                inParagraph = true;
                impliedP = true;
            }
            emptyAnchor = false;
            charAttr.addAttribute(StyleConstants.NameAttribute, HTML.Tag.CONTENT);
            AttributeSet a = charAttr.copyAttributes();
            ElementSpec es = new ElementSpec(
                a, ElementSpec.ContentType, data, offs, length);
            parseBuffer.addElement(es);

            if (parseBuffer.size() > threshold) {
                if ( threshold <= MaxThreshold ) {
                    threshold *= StepThreshold;
                }
                try {
                    flushBuffer(false);
                } catch (BadLocationException ble) {
                }
            }
            if(length > 0) {
                lastWasNewline = (data[offs + length - 1] == '\n');
            }
        }

        /**
         * Adds content that is basically specified entirely
         * in the attribute set.
         *
         * @param t an HTML tag
         * @param a the attribute set
         */
        protected void addSpecialElement(HTML.Tag t, MutableAttributeSet a) {
            if ((t != HTML.Tag.FRAME) && (! inParagraph) && (! inPre)) {
                nextTagAfterPImplied = t;
                blockOpen(HTML.Tag.IMPLIED, new SimpleAttributeSet());
                nextTagAfterPImplied = null;
                inParagraph = true;
                impliedP = true;
            }
            if (!canInsertTag(t, a, t.isBlock())) {
                return;
            }
            if (a.isDefined(IMPLIED)) {
                a.removeAttribute(IMPLIED);
            }
            emptyAnchor = false;
            a.addAttributes(charAttr);
            a.addAttribute(StyleConstants.NameAttribute, t);
            char[] one = new char[1];
            one[0] = ' ';
            ElementSpec es = new ElementSpec(
                a.copyAttributes(), ElementSpec.ContentType, one, 0, 1);
            parseBuffer.addElement(es);
            // Set this to avoid generating a newline for frames, frames
            // shouldn't have any content, and shouldn't need a newline.
            if (t == HTML.Tag.FRAME) {
                lastWasNewline = true;
            }
        }

        /**
         * Flushes the current parse buffer into the document.
         * @param endOfStream true if there is no more content to parser
         */
        void flushBuffer(boolean endOfStream) throws BadLocationException {
            int oldLength = HTMLDocument.this.getLength();
            int size = parseBuffer.size();
            if (endOfStream && (insertTag != null || insertAfterImplied) &&
                size > 0) {
                adjustEndSpecsForPartialInsert();
                size = parseBuffer.size();
            }
            ElementSpec[] spec = new ElementSpec[size];
            parseBuffer.copyInto(spec);

            if (oldLength == 0 && (insertTag == null && !insertAfterImplied)) {
                create(spec);
            } else {
                insert(offset, spec);
            }
            parseBuffer.removeAllElements();
            offset += HTMLDocument.this.getLength() - oldLength;
            flushCount++;
        }

        /**
         * This will be invoked for the last flush, if <code>insertTag</code>
         * is non null.
         */
        private void adjustEndSpecsForPartialInsert() {
            int size = parseBuffer.size();
            if (insertTagDepthDelta < 0) {
                // When inserting via an insertTag, the depths (of the tree
                // being read in, and existing hierarchy) may not match up.
                // This attemps to clean it up.
                int removeCounter = insertTagDepthDelta;
                while (removeCounter < 0 && size >= 0 &&
                        parseBuffer.elementAt(size - 1).
                       getType() == ElementSpec.EndTagType) {
                    parseBuffer.removeElementAt(--size);
                    removeCounter++;
                }
            }
            if (flushCount == 0 && (!insertAfterImplied ||
                                    !wantsTrailingNewline)) {
                // If this starts with content (or popDepth > 0 &&
                // pushDepth > 0) and ends with EndTagTypes, make sure
                // the last content isn't a \n, otherwise will end up with
                // an extra \n in the middle of content.
                int index = 0;
                if (pushDepth > 0) {
                    if (parseBuffer.elementAt(0).getType() ==
                        ElementSpec.ContentType) {
                        index++;
                    }
                }
                index += (popDepth + pushDepth);
                int cCount = 0;
                int cStart = index;
                while (index < size && parseBuffer.elementAt
                        (index).getType() == ElementSpec.ContentType) {
                    index++;
                    cCount++;
                }
                if (cCount > 1) {
                    while (index < size && parseBuffer.elementAt
                            (index).getType() == ElementSpec.EndTagType) {
                        index++;
                    }
                    if (index == size) {
                        char[] lastText = parseBuffer.elementAt
                                (cStart + cCount - 1).getArray();
                        if (lastText.length == 1 && lastText[0] == NEWLINE[0]){
                            index = cStart + cCount - 1;
                            while (size > index) {
                                parseBuffer.removeElementAt(--size);
                            }
                        }
                    }
                }
            }
            if (wantsTrailingNewline) {
                // Make sure there is in fact a newline
                for (int counter = parseBuffer.size() - 1; counter >= 0;
                                   counter--) {
                    ElementSpec spec = parseBuffer.elementAt(counter);
                    if (spec.getType() == ElementSpec.ContentType) {
                        if (spec.getArray()[spec.getLength() - 1] != '\n') {
                            SimpleAttributeSet attrs =new SimpleAttributeSet();

                            attrs.addAttribute(StyleConstants.NameAttribute,
                                               HTML.Tag.CONTENT);
                            parseBuffer.insertElementAt(new ElementSpec(
                                    attrs,
                                    ElementSpec.ContentType, NEWLINE, 0, 1),
                                    counter + 1);
                        }
                        break;
                    }
                }
            }
        }

        /**
         * Adds the CSS rules in <code>rules</code>.
         */
        void addCSSRules(String rules) {
            StyleSheet ss = getStyleSheet();
            ss.addRule(rules);
        }

        /**
         * Adds the CSS stylesheet at <code>href</code> to the known list
         * of stylesheets.
         */
        void linkCSSStyleSheet(String href) {
            URL url;
            try {
                url = new URL(base, href);
            } catch (MalformedURLException mfe) {
                try {
                    url = new URL(href);
                } catch (MalformedURLException mfe2) {
                    url = null;
                }
            }
            if (url != null) {
                getStyleSheet().importStyleSheet(url);
            }
        }

        /**
         * Returns true if can insert starting at <code>t</code>. This
         * will return false if the insert tag is set, and hasn't been found
         * yet.
         */
        private boolean canInsertTag(HTML.Tag t, AttributeSet attr,
                                     boolean isBlockTag) {
            if (!foundInsertTag) {
                boolean needPImplied = ((t == HTML.Tag.IMPLIED)
                                                          && (!inParagraph)
                                                          && (!inPre));
                if (needPImplied && (nextTagAfterPImplied != null)) {

                    /*
                     * If insertTag == null then just proceed to
                     * foundInsertTag() call below and return true.
                     */
                    if (insertTag != null) {
                        boolean nextTagIsInsertTag =
                                isInsertTag(nextTagAfterPImplied);
                        if ( (! nextTagIsInsertTag) || (! insertInsertTag) ) {
                            return false;
                        }
                    }
                    /*
                     *  Proceed to foundInsertTag() call...
                     */
                 } else if ((insertTag != null && !isInsertTag(t))
                               || (insertAfterImplied
                                    && (attr == null
                                        || attr.isDefined(IMPLIED)
                                        || t == HTML.Tag.IMPLIED
                                       )
                                   )
                           ) {
                    return false;
                }

                // Allow the insert if t matches the insert tag, or
                // insertAfterImplied is true and the element is implied.
                foundInsertTag(isBlockTag);
                if (!insertInsertTag) {
                    return false;
                }
            }
            return true;
        }

        private boolean isInsertTag(HTML.Tag tag) {
            return (insertTag == tag);
        }

        private void foundInsertTag(boolean isBlockTag) {
            foundInsertTag = true;
            if (!insertAfterImplied && (popDepth > 0 || pushDepth > 0)) {
                try {
                    if (offset == 0 || !getText(offset - 1, 1).equals("\n")) {
                        // Need to insert a newline.
                        AttributeSet newAttrs = null;
                        boolean joinP = true;

                        if (offset != 0) {
                            // Determine if we can use JoinPrevious, we can't
                            // if the Element has some attributes that are
                            // not meant to be duplicated.
                            Element charElement = getCharacterElement
                                                    (offset - 1);
                            AttributeSet attrs = charElement.getAttributes();

                            if (attrs.isDefined(StyleConstants.
                                                ComposedTextAttribute)) {
                                joinP = false;
                            }
                            else {
                                Object name = attrs.getAttribute
                                              (StyleConstants.NameAttribute);
                                if (name instanceof HTML.Tag) {
                                    HTML.Tag tag = (HTML.Tag)name;
                                    if (tag == HTML.Tag.IMG ||
                                        tag == HTML.Tag.HR ||
                                        tag == HTML.Tag.COMMENT ||
                                        (tag instanceof HTML.UnknownTag)) {
                                        joinP = false;
                                    }
                                }
                            }
                        }
                        if (!joinP) {
                            // If not joining with the previous element, be
                            // sure and set the name (otherwise it will be
                            // inherited).
                            newAttrs = new SimpleAttributeSet();
                            ((SimpleAttributeSet)newAttrs).addAttribute
                                              (StyleConstants.NameAttribute,
                                               HTML.Tag.CONTENT);
                        }
                        ElementSpec es = new ElementSpec(newAttrs,
                                     ElementSpec.ContentType, NEWLINE, 0,
                                     NEWLINE.length);
                        if (joinP) {
                            es.setDirection(ElementSpec.
                                            JoinPreviousDirection);
                        }
                        parseBuffer.addElement(es);
                    }
                } catch (BadLocationException ble) {}
            }
            // pops
            for (int counter = 0; counter < popDepth; counter++) {
                parseBuffer.addElement(new ElementSpec(null, ElementSpec.
                                                       EndTagType));
            }
            // pushes
            for (int counter = 0; counter < pushDepth; counter++) {
                ElementSpec es = new ElementSpec(null, ElementSpec.
                                                 StartTagType);
                es.setDirection(ElementSpec.JoinNextDirection);
                parseBuffer.addElement(es);
            }
            insertTagDepthDelta = depthTo(Math.max(0, offset - 1)) -
                                  popDepth + pushDepth - inBlock;
            if (isBlockTag) {
                // A start spec will be added (for this tag), so we account
                // for it here.
                insertTagDepthDelta++;
            }
            else {
                // An implied paragraph close (end spec) is going to be added,
                // so we account for it here.
                insertTagDepthDelta--;
                inParagraph = true;
                lastWasNewline = false;
            }
        }

        /**
         * This is set to true when and end is invoked for {@literal <html>}.
         */
        private boolean receivedEndHTML;
        /** Number of times <code>flushBuffer</code> has been invoked. */
        private int flushCount;
        /** If true, behavior is similar to insertTag, but instead of
         * waiting for insertTag will wait for first Element without
         * an 'implied' attribute and begin inserting then. */
        private boolean insertAfterImplied;
        /** This is only used if insertAfterImplied is true. If false, only
         * inserting content, and there is a trailing newline it is removed. */
        private boolean wantsTrailingNewline;
        int threshold;
        int offset;
        boolean inParagraph = false;
        boolean impliedP = false;
        boolean inPre = false;
        boolean inTextArea = false;
        TextAreaDocument textAreaDocument = null;
        boolean inTitle = false;
        boolean lastWasNewline = true;
        boolean emptyAnchor;
        /** True if (!emptyDocument &amp;&amp; insertTag == null), this is used so
         * much it is cached. */
        boolean midInsert;
        /** True when the body has been encountered. */
        boolean inBody;
        /** If non null, gives parent Tag that insert is to happen at. */
        HTML.Tag insertTag;
        /** If true, the insertTag is inserted, otherwise elements after
         * the insertTag is found are inserted. */
        boolean insertInsertTag;
        /** Set to true when insertTag has been found. */
        boolean foundInsertTag;
        /** When foundInsertTag is set to true, this will be updated to
         * reflect the delta between the two structures. That is, it
         * will be the depth the inserts are happening at minus the
         * depth of the tags being passed in. A value of 0 (the common
         * case) indicates the structures match, a value greater than 0 indicates
         * the insert is happening at a deeper depth than the stream is
         * parsing, and a value less than 0 indicates the insert is happening earlier
         * in the tree that the parser thinks and that we will need to remove
         * EndTagType specs in the flushBuffer method.
         */
        int insertTagDepthDelta;
        /** How many parents to ascend before insert new elements. */
        int popDepth;
        /** How many parents to descend (relative to popDepth) before
         * inserting. */
        int pushDepth;
        /** Last Map that was encountered. */
        Map lastMap;
        /** Set to true when a style element is encountered. */
        boolean inStyle = false;
        /** Name of style to use. Obtained from Meta tag. */
        String defaultStyle;
        /** Vector describing styles that should be include. Will consist
         * of a bunch of HTML.Tags, which will either be:
         * <p>LINK: in which case it is followed by an AttributeSet
         * <p>STYLE: in which case the following element is a String
         * indicating the type (may be null), and the elements following
         * it until the next HTML.Tag are the rules as Strings.
         */
        Vector<Object> styles;
        /** True if inside the head tag. */
        boolean inHead = false;
        /** Set to true if the style language is text/css. Since this is
         * used alot, it is cached. */
        boolean isStyleCSS;
        /** True if inserting into an empty document. */
        boolean emptyDocument;
        /** Attributes from a style Attribute. */
        AttributeSet styleAttributes;

        /**
         * Current option, if in an option element (needed to
         * load the label.
         */
        Option option;

        /**
         * Buffer to keep building elements.
         */
        protected Vector<ElementSpec> parseBuffer = new Vector<ElementSpec>();
        /**
         * Current character attribute set.
         */
        protected MutableAttributeSet charAttr = new TaggedAttributeSet();
        Stack<AttributeSet> charAttrStack = new Stack<AttributeSet>();
        Hashtable<HTML.Tag, TagAction> tagMap;
        int inBlock = 0;

        /**
         * This attribute is sometimes used to refer to next tag
         * to be handled after p-implied when the latter is
         * the current tag which is being handled.
         */
        private HTML.Tag nextTagAfterPImplied = null;
    }


    /**
     * Used by StyleSheet to determine when to avoid removing HTML.Tags
     * matching StyleConstants.
     */
    static class TaggedAttributeSet extends SimpleAttributeSet {
        TaggedAttributeSet() {
            super();
        }
    }


    /**
     * An element that represents a chunk of text that has
     * a set of HTML character level attributes assigned to
     * it.
     */
    public class RunElement extends LeafElement {

        /**
         * Constructs an element that represents content within the
         * document (has no children).
         *
         * @param parent  the parent element
         * @param a       the element attributes
         * @param offs0   the start offset (must be at least 0)
         * @param offs1   the end offset (must be at least offs0)
         * @since 1.4
         */
        public RunElement(Element parent, AttributeSet a, int offs0, int offs1) {
            super(parent, a, offs0, offs1);
        }

        /**
         * Gets the name of the element.
         *
         * @return the name, null if none
         */
        public String getName() {
            Object o = getAttribute(StyleConstants.NameAttribute);
            if (o != null) {
                return o.toString();
            }
            return super.getName();
        }

        /**
         * Gets the resolving parent.  HTML attributes are not inherited
         * at the model level so we override this to return null.
         *
         * @return null, there are none
         * @see AttributeSet#getResolveParent
         */
        public AttributeSet getResolveParent() {
            return null;
        }
    }

    /**
     * An element that represents a structural <em>block</em> of
     * HTML.
     */
    public class BlockElement extends BranchElement {

        /**
         * Constructs a composite element that initially contains
         * no children.
         *
         * @param parent  the parent element
         * @param a       the attributes for the element
         * @since 1.4
         */
        public BlockElement(Element parent, AttributeSet a) {
            super(parent, a);
        }

        /**
         * Gets the name of the element.
         *
         * @return the name, null if none
         */
        public String getName() {
            Object o = getAttribute(StyleConstants.NameAttribute);
            if (o != null) {
                return o.toString();
            }
            return super.getName();
        }

        /**
         * Gets the resolving parent.  HTML attributes are not inherited
         * at the model level so we override this to return null.
         *
         * @return null, there are none
         * @see AttributeSet#getResolveParent
         */
        public AttributeSet getResolveParent() {
            return null;
        }

    }


    /**
     * Document that allows you to set the maximum length of the text.
     */
    private static class FixedLengthDocument extends PlainDocument {
        private int maxLength;

        public FixedLengthDocument(int maxLength) {
            this.maxLength = maxLength;
        }

        public void insertString(int offset, String str, AttributeSet a)
            throws BadLocationException {
            if (str != null && str.length() + getLength() <= maxLength) {
                super.insertString(offset, str, a);
            }
        }
    }
}
