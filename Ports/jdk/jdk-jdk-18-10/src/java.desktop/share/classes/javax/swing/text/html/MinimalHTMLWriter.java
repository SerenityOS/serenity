/*
 * Copyright (c) 1998, 2010, Oracle and/or its affiliates. All rights reserved.
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

import java.io.Writer;
import java.io.IOException;
import java.util.*;
import java.awt.Color;
import javax.swing.text.*;

/**
 * MinimalHTMLWriter is a fallback writer used by the
 * HTMLEditorKit to write out HTML for a document that
 * is a not produced by the EditorKit.
 *
 * The format for the document is:
 * <pre>
 * &lt;html&gt;
 *   &lt;head&gt;
 *     &lt;style&gt;
 *        &lt;!-- list of named styles
 *         p.normal {
 *            font-family: SansSerif;
 *            margin-height: 0;
 *            font-size: 14
 *         }
 *        --&gt;
 *      &lt;/style&gt;
 *   &lt;/head&gt;
 *   &lt;body&gt;
 *    &lt;p style=normal&gt;
 *        <b>Bold, italic, and underline attributes
 *        of the run are emitted as HTML tags.
 *        The remaining attributes are emitted as
 *        part of the style attribute of a &lt;span&gt; tag.
 *        The syntax is similar to inline styles.</b>
 *    &lt;/p&gt;
 *   &lt;/body&gt;
 * &lt;/html&gt;
 * </pre>
 *
 * @author Sunita Mani
 */

public class MinimalHTMLWriter extends AbstractWriter {

    /**
     * These static finals are used to
     * tweak and query the fontMask about which
     * of these tags need to be generated or
     * terminated.
     */
    private static final int BOLD = 0x01;
    private static final int ITALIC = 0x02;
    private static final int UNDERLINE = 0x04;

    // Used to map StyleConstants to CSS.
    private static final CSS css = new CSS();

    private int fontMask = 0;

    int startOffset = 0;
    int endOffset = 0;

    /**
     * Stores the attributes of the previous run.
     * Used to compare with the current run's
     * attributeset.  If identical, then a
     * &lt;span&gt; tag is not emitted.
     */
    private AttributeSet fontAttributes;

    /**
     * Maps from style name as held by the Document, to the archived
     * style name (style name written out). These may differ.
     */
    private Hashtable<String, String> styleNameMapping;

    /**
     * Creates a new MinimalHTMLWriter.
     *
     * @param w  Writer
     * @param doc StyledDocument
     *
     */
    public MinimalHTMLWriter(Writer w, StyledDocument doc) {
        super(w, doc);
    }

    /**
     * Creates a new MinimalHTMLWriter.
     *
     * @param w  Writer
     * @param doc StyledDocument
     * @param pos The location in the document to fetch the
     *   content.
     * @param len The amount to write out.
     *
     */
    public MinimalHTMLWriter(Writer w, StyledDocument doc, int pos, int len) {
        super(w, doc, pos, len);
    }

    /**
     * Generates HTML output
     * from a StyledDocument.
     *
     * @exception IOException on any I/O error
     * @exception BadLocationException if pos represents an invalid
     *            location within the document.
     *
     */
    public void write() throws IOException, BadLocationException {
        styleNameMapping = new Hashtable<String, String>();
        writeStartTag("<html>");
        writeHeader();
        writeBody();
        writeEndTag("</html>");
    }


    /**
     * Writes out all the attributes for the
     * following types:
     *  StyleConstants.ParagraphConstants,
     *  StyleConstants.CharacterConstants,
     *  StyleConstants.FontConstants,
     *  StyleConstants.ColorConstants.
     * The attribute name and value are separated by a colon.
     * Each pair is separated by a semicolon.
     *
     * @exception IOException on any I/O error
     */
    protected void writeAttributes(AttributeSet attr) throws IOException {
        Enumeration<?> attributeNames = attr.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            Object name = attributeNames.nextElement();
            if ((name instanceof StyleConstants.ParagraphConstants) ||
                (name instanceof StyleConstants.CharacterConstants) ||
                (name instanceof StyleConstants.FontConstants) ||
                (name instanceof StyleConstants.ColorConstants)) {
                indent();
                write(name.toString());
                write(':');
                write(css.styleConstantsValueToCSSValue
                      ((StyleConstants)name, attr.getAttribute(name)).
                      toString());
                write(';');
                write(NEWLINE);
            }
        }
    }


    /**
     * Writes out text.
     *
     * @exception IOException on any I/O error
     */
    protected void text(Element elem) throws IOException, BadLocationException {
        String contentStr = getText(elem);
        if ((contentStr.length() > 0) &&
            (contentStr.charAt(contentStr.length()-1) == NEWLINE)) {
            contentStr = contentStr.substring(0, contentStr.length()-1);
        }
        if (contentStr.length() > 0) {
            write(contentStr);
        }
    }

    /**
     * Writes out a start tag appropriately
     * indented.  Also increments the indent level.
     *
     * @param tag a start tag
     * @exception IOException on any I/O error
     */
    protected void writeStartTag(String tag) throws IOException {
        indent();
        write(tag);
        write(NEWLINE);
        incrIndent();
    }


    /**
     * Writes out an end tag appropriately
     * indented.  Also decrements the indent level.
     *
     * @param endTag an end tag
     * @exception IOException on any I/O error
     */
    protected void writeEndTag(String endTag) throws IOException {
        decrIndent();
        indent();
        write(endTag);
        write(NEWLINE);
    }


    /**
     * Writes out the &lt;head&gt; and &lt;style&gt;
     * tags, and then invokes writeStyles() to write
     * out all the named styles as the content of the
     * &lt;style&gt; tag.  The content is surrounded by
     * valid HTML comment markers to ensure that the
     * document is viewable in applications/browsers
     * that do not support the tag.
     *
     * @exception IOException on any I/O error
     */
    protected void writeHeader() throws IOException {
        writeStartTag("<head>");
        writeStartTag("<style>");
        writeStartTag("<!--");
        writeStyles();
        writeEndTag("-->");
        writeEndTag("</style>");
        writeEndTag("</head>");
    }



    /**
     * Writes out all the named styles as the
     * content of the &lt;style&gt; tag.
     *
     * @exception IOException on any I/O error
     */
    protected void writeStyles() throws IOException {
        /*
         *  Access to DefaultStyledDocument done to workaround
         *  a missing API in styled document to access the
         *  stylenames.
         */
        DefaultStyledDocument styledDoc =  ((DefaultStyledDocument)getDocument());
        Enumeration<?> styleNames = styledDoc.getStyleNames();

        while (styleNames.hasMoreElements()) {
            Style s = styledDoc.getStyle((String)styleNames.nextElement());

            /** PENDING: Once the name attribute is removed
                from the list we check check for 0. **/
            if (s.getAttributeCount() == 1 &&
                s.isDefined(StyleConstants.NameAttribute)) {
                continue;
            }
            indent();
            write("p." + addStyleName(s.getName()));
            write(" {\n");
            incrIndent();
            writeAttributes(s);
            decrIndent();
            indent();
            write("}\n");
        }
    }


    /**
     * Iterates over the elements in the document
     * and processes elements based on whether they are
     * branch elements or leaf elements.  This method specially handles
     * leaf elements that are text.
     *
     * @throws IOException on any I/O error
     * @throws BadLocationException if we are in an invalid
     *            location within the document.
     */
    protected void writeBody() throws IOException, BadLocationException {
        ElementIterator it = getElementIterator();

        /*
          This will be a section element for a styled document.
          We represent this element in HTML as the body tags.
          Therefore we ignore it.
         */
        it.current();

        Element next;

        writeStartTag("<body>");

        boolean inContent = false;

        while((next = it.next()) != null) {
            if (!inRange(next)) {
                continue;
            }
            if (next instanceof AbstractDocument.BranchElement) {
                if (inContent) {
                    writeEndParagraph();
                    inContent = false;
                    fontMask = 0;
                }
                writeStartParagraph(next);
            } else if (isText(next)) {
                writeContent(next, !inContent);
                inContent = true;
            } else {
                writeLeaf(next);
                inContent = true;
            }
        }
        if (inContent) {
            writeEndParagraph();
        }
        writeEndTag("</body>");
    }


    /**
     * Emits an end tag for a &lt;p&gt;
     * tag.  Before writing out the tag, this method ensures
     * that all other tags that have been opened are
     * appropriately closed off.
     *
     * @exception IOException on any I/O error
     */
    protected void writeEndParagraph() throws IOException {
        writeEndMask(fontMask);
        if (inFontTag()) {
            endSpanTag();
        } else {
            write(NEWLINE);
        }
        writeEndTag("</p>");
    }


    /**
     * Emits the start tag for a paragraph. If
     * the paragraph has a named style associated with it,
     * then this method also generates a class attribute for the
     * &lt;p&gt; tag and sets its value to be the name of the
     * style.
     *
     * @param elem an element
     * @exception IOException on any I/O error
     */
    protected void writeStartParagraph(Element elem) throws IOException {
        AttributeSet attr = elem.getAttributes();
        Object resolveAttr = attr.getAttribute(StyleConstants.ResolveAttribute);
        if (resolveAttr instanceof StyleContext.NamedStyle) {
            writeStartTag("<p class=" + mapStyleName(((StyleContext.NamedStyle)resolveAttr).getName()) + ">");
        } else {
            writeStartTag("<p>");
        }
    }


    /**
     * Responsible for writing out other non-text leaf
     * elements.
     *
     * @param elem an element
     * @exception IOException on any I/O error
     */
    protected void writeLeaf(Element elem) throws IOException {
        indent();
        if (elem.getName() == StyleConstants.IconElementName) {
            writeImage(elem);
        } else if (elem.getName() == StyleConstants.ComponentElementName) {
            writeComponent(elem);
        }
    }


    /**
     * Responsible for handling Icon Elements;
     * deliberately unimplemented.  How to implement this method is
     * an issue of policy.  For example, if you're generating
     * an &lt;img&gt; tag, how should you
     * represent the src attribute (the location of the image)?
     * In certain cases it could be a URL, in others it could
     * be read from a stream.
     *
     * @param elem an element of type StyleConstants.IconElementName
     * @throws IOException if I/O error occured.
     */
    protected void writeImage(Element elem) throws IOException {
    }


    /**
     * Responsible for handling Component Elements;
     * deliberately unimplemented.
     * How this method is implemented is a matter of policy.
     *
     * @param elem an element of type StyleConstants.ComponentElementName
     * @throws IOException if I/O error occured.
     */
    protected void writeComponent(Element elem) throws IOException {
    }


    /**
     * Returns true if the element is a text element.
     *
     * @param elem an element
     * @return {@code true} if the element is a text element.
     */
    protected boolean isText(Element elem) {
        return (elem.getName() == AbstractDocument.ContentElementName);
    }


    /**
     * Writes out the attribute set
     * in an HTML-compliant manner.
     *
     * @param elem an element
     * @param needsIndenting indention will be added if {@code needsIndenting} is {@code true}
     * @exception IOException on any I/O error
     * @exception BadLocationException if pos represents an invalid
     *            location within the document.
     */
    protected void writeContent(Element elem,  boolean needsIndenting)
        throws IOException, BadLocationException {

        AttributeSet attr = elem.getAttributes();
        writeNonHTMLAttributes(attr);
        if (needsIndenting) {
            indent();
        }
        writeHTMLTags(attr);
        text(elem);
    }


    /**
     * Generates
     * bold &lt;b&gt;, italic &lt;i&gt;, and &lt;u&gt; tags for the
     * text based on its attribute settings.
     *
     * @param attr a set of attributes
     * @exception IOException on any I/O error
     */

    protected void writeHTMLTags(AttributeSet attr) throws IOException {

        int oldMask = fontMask;
        setFontMask(attr);

        int endMask = 0;
        int startMask = 0;
        if ((oldMask & BOLD) != 0) {
            if ((fontMask & BOLD) == 0) {
                endMask |= BOLD;
            }
        } else if ((fontMask & BOLD) != 0) {
            startMask |= BOLD;
        }

        if ((oldMask & ITALIC) != 0) {
            if ((fontMask & ITALIC) == 0) {
                endMask |= ITALIC;
            }
        } else if ((fontMask & ITALIC) != 0) {
            startMask |= ITALIC;
        }

        if ((oldMask & UNDERLINE) != 0) {
            if ((fontMask & UNDERLINE) == 0) {
                endMask |= UNDERLINE;
            }
        } else if ((fontMask & UNDERLINE) != 0) {
            startMask |= UNDERLINE;
        }
        writeEndMask(endMask);
        writeStartMask(startMask);
    }


    /**
     * Tweaks the appropriate bits of fontMask
     * to reflect whether the text is to be displayed in
     * bold, italic, and/or with an underline.
     *
     */
    private void setFontMask(AttributeSet attr) {
        if (StyleConstants.isBold(attr)) {
            fontMask |= BOLD;
        }

        if (StyleConstants.isItalic(attr)) {
            fontMask |= ITALIC;
        }

        if (StyleConstants.isUnderline(attr)) {
            fontMask |= UNDERLINE;
        }
    }




    /**
     * Writes out start tags &lt;u&gt;, &lt;i&gt;, and &lt;b&gt; based on
     * the mask settings.
     *
     * @exception IOException on any I/O error
     */
    private void writeStartMask(int mask) throws IOException  {
        if (mask != 0) {
            if ((mask & UNDERLINE) != 0) {
                write("<u>");
            }
            if ((mask & ITALIC) != 0) {
                write("<i>");
            }
            if ((mask & BOLD) != 0) {
                write("<b>");
            }
        }
    }

    /**
     * Writes out end tags for &lt;u&gt;, &lt;i&gt;, and &lt;b&gt; based on
     * the mask settings.
     *
     * @exception IOException on any I/O error
     */
    private void writeEndMask(int mask) throws IOException {
        if (mask != 0) {
            if ((mask & BOLD) != 0) {
                write("</b>");
            }
            if ((mask & ITALIC) != 0) {
                write("</i>");
            }
            if ((mask & UNDERLINE) != 0) {
                write("</u>");
            }
        }
    }


    /**
     * Writes out the remaining
     * character-level attributes (attributes other than bold,
     * italic, and underline) in an HTML-compliant way.  Given that
     * attributes such as font family and font size have no direct
     * mapping to HTML tags, a &lt;span&gt; tag is generated and its
     * style attribute is set to contain the list of remaining
     * attributes just like inline styles.
     *
     * @param attr a set of attributes
     * @exception IOException on any I/O error
     */
    protected void writeNonHTMLAttributes(AttributeSet attr) throws IOException {

        String style = "";
        String separator = "; ";

        if (inFontTag() && fontAttributes.isEqual(attr)) {
            return;
        }

        boolean first = true;
        Color color = (Color)attr.getAttribute(StyleConstants.Foreground);
        if (color != null) {
            style += "color: " + css.styleConstantsValueToCSSValue
                                    ((StyleConstants)StyleConstants.Foreground,
                                     color);
            first = false;
        }
        Integer size = (Integer)attr.getAttribute(StyleConstants.FontSize);
        if (size != null) {
            if (!first) {
                style += separator;
            }
            style += "font-size: " + size.intValue() + "pt";
            first = false;
        }

        String family = (String)attr.getAttribute(StyleConstants.FontFamily);
        if (family != null) {
            if (!first) {
                style += separator;
            }
            style += "font-family: " + family;
            first = false;
        }

        if (style.length() > 0) {
            if (fontMask != 0) {
                writeEndMask(fontMask);
                fontMask = 0;
            }
            startSpanTag(style);
            fontAttributes = attr;
        }
        else if (fontAttributes != null) {
            writeEndMask(fontMask);
            fontMask = 0;
            endSpanTag();
        }
    }


    /**
     * Returns true if we are currently in a &lt;font&gt; tag.
     *
     * @return {@code true} if we are currently in a &lt;font&gt; tag.
     */
    protected boolean inFontTag() {
        return (fontAttributes != null);
    }

    /**
     * This is no longer used, instead &lt;span&gt; will be written out.
     * <p>
     * Writes out an end tag for the &lt;font&gt; tag.
     *
     * @exception IOException on any I/O error
     */
    protected void endFontTag() throws IOException {
        write(NEWLINE);
        writeEndTag("</font>");
        fontAttributes = null;
    }


    /**
     * This is no longer used, instead &lt;span&gt; will be written out.
     * <p>
     * Writes out a start tag for the &lt;font&gt; tag.
     * Because font tags cannot be nested,
     * this method closes out
     * any enclosing font tag before writing out a
     * new start tag.
     *
     * @param style a font style
     * @exception IOException on any I/O error
     */
    protected void startFontTag(String style) throws IOException {
        boolean callIndent = false;
        if (inFontTag()) {
            endFontTag();
            callIndent = true;
        }
        writeStartTag("<font style=\"" + style + "\">");
        if (callIndent) {
            indent();
        }
    }

    /**
     * Writes out a start tag for the &lt;font&gt; tag.
     * Because font tags cannot be nested,
     * this method closes out
     * any enclosing font tag before writing out a
     * new start tag.
     *
     * @exception IOException on any I/O error
     */
    private void startSpanTag(String style) throws IOException {
        boolean callIndent = false;
        if (inFontTag()) {
            endSpanTag();
            callIndent = true;
        }
        writeStartTag("<span style=\"" + style + "\">");
        if (callIndent) {
            indent();
        }
    }

    /**
     * Writes out an end tag for the &lt;span&gt; tag.
     *
     * @exception IOException on any I/O error
     */
    private void endSpanTag() throws IOException {
        write(NEWLINE);
        writeEndTag("</span>");
        fontAttributes = null;
    }

    /**
     * Adds the style named <code>style</code> to the style mapping. This
     * returns the name that should be used when outputting. CSS does not
     * allow the full Unicode set to be used as a style name.
     */
    private String addStyleName(String style) {
        if (styleNameMapping == null) {
            return style;
        }
        StringBuilder sb = null;
        for (int counter = style.length() - 1; counter >= 0; counter--) {
            if (!isValidCharacter(style.charAt(counter))) {
                if (sb == null) {
                    sb = new StringBuilder(style);
                }
                sb.setCharAt(counter, 'a');
            }
        }
        String mappedName = (sb != null) ? sb.toString() : style;
        while (styleNameMapping.get(mappedName) != null) {
            mappedName = mappedName + 'x';
        }
        styleNameMapping.put(style, mappedName);
        return mappedName;
    }

    /**
     * Returns the mapped style name corresponding to <code>style</code>.
     */
    private String mapStyleName(String style) {
        if (styleNameMapping == null) {
            return style;
        }
        String retValue = styleNameMapping.get(style);
        return (retValue == null) ? style : retValue;
    }

    private boolean isValidCharacter(char character) {
        return ((character >= 'a' && character <= 'z') ||
                (character >= 'A' && character <= 'Z'));
    }
}
