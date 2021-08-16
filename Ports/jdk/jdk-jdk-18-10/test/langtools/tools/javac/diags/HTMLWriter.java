/*
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * A class to facilitate writing HTML via a stream.
 */
public class HTMLWriter
{
    /**
     * Create an HTMLWriter object, using a default doctype for HTML 3.2.
     * @param out a Writer to which to write the generated HTML
     * @throws IOException if there is a problem writing to the underlying stream
     */
    public HTMLWriter(Writer out) throws IOException {
        this(out, "<!DOCTYPE html\">");
    }

    /**
     * Create an HTMLWriter object, using a specifed doctype header.
     * @param out a Writer to which to write the generated HTML
     * @param docType a string containing a doctype header for the HTML to be generetaed
     * @throws IOException if there is a problem writing to the underlying stream
     */
    public HTMLWriter(Writer out, String docType) throws IOException {
        if (out instanceof BufferedWriter)
            this.out = (BufferedWriter) out;
        else
            this.out = new BufferedWriter(out);
        this.out.write(docType);
        this.out.newLine();
    }

    /**
     * Create an HTMLWriter object, using a specified bundle for localizing messages.
     * @param out a Writer to which to write the generated HTML
     * @param i18n a resource bundle to use to localize messages
     * @throws IOException if there is a problem writing to the underlying stream
     */
    public HTMLWriter(Writer out, ResourceBundle i18n) throws IOException {
        this(out);
        this.i18n = i18n;
    }


    /**
     * Create an HTMLWriter object, using a specifed doctype header and
     * using a specified bundle for l0calizing messages.
     * @param out a Writer to which to write the generated HTML
     * @param docType a string containing a doctype header for the HTML to be generetaed
     * @param i18n a resource bundle to use to localize messages
     * @throws IOException if there is a problem writing to the underlying stream
     */
    public HTMLWriter(Writer out, String docType, ResourceBundle i18n) throws IOException {
        this(out, docType);
        this.i18n = i18n;
    }

    /**
     * Set the reource bundle to be used for localizing messages.
     * @param i18n the resource bundle to be used for localizing messages
     */
    public void setResourceBundle(ResourceBundle i18n) {
        this.i18n = i18n;
    }

    /**
     * Flush the stream, and the underlying output stream.
     * @throws IOException if there is a problem writing to the underlying stream
     */
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Close the stream, and the underlying output stream.
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void close() throws IOException {
        out.close();
    }

    /**
     * Write a newline to the underlying output stream.
     * @throws IOException if there is a problem writing to the underlying stream
     */
    public void newLine() throws IOException {
        out.newLine();
    }

    /**
     * Start an HTML tag.  If a prior tag has been started, it will
     * be closed first. Once a tag has been opened, attributes for the
     * tag may be written out, followed by body content before finally
     * ending the tag.
     * @param tag the tag to be started
     * @throws IOException if there is a problem writing to the underlying stream
     * @see #writeAttr
     * @see #write
     * @see #endTag
     */
    public void startTag(String tag) throws IOException {
        if (state == IN_TAG) {
            out.write(">");
            state = IN_BODY;
        }
        //newLine();
        out.write("<");
        out.write(tag);
        state = IN_TAG;
    }

    /**
     * Finish an HTML tag. It is expected that a call to endTag will match
     * a corresponding earlier call to startTag, but there is no formal check
     * for this.
     * @param tag the tag to be closed.
     * @throws IOException if there is a problem writing to the underlying stream
     */
    public void endTag(String tag) throws IOException {
        if (state == IN_TAG) {
            out.write(">");
            state = IN_BODY;
            out.newLine();
        }
        out.write("</");
        out.write(tag);
        out.write(">");
        //out.newLine();   // PATCHED, jjg
        state = IN_BODY;
    }

    /**
     * Finish an empty element tag, such as a META, BASE or LINK tag.
     * This is expected to correspond with a startTag.
     * @param tag the tag which is being closed.  this is only useful for
     *        validation, it is not written out
     * @throws IllegalStateException if this call does not follow startTag
     *         (stream is not currently inside a tag)
     * @throws IOException if there is a problem writing to the underlying stream
     */
    public void endEmptyTag(String tag) throws IOException {
        if (state != IN_TAG)
            throw new IllegalStateException();

        out.write(">");
        state = IN_BODY;
        out.newLine();
    }

    /**
     * Write an attribute for a tag. A tag must previously have been started.
     * All tag attributes must be written before any body text is written.
     * The value will be quoted if necessary when writing it to the underlying
     * stream. No check is made that the attribute is valid for the current tag.
     * @param name the name of the attribute to be written
     * @param value the value of the attribute to be written
     * @throws IllegalStateException if the stream is not in a state to
     * write attributes -- e.g. if this call does not follow startTag or other
     * calls of writteAttr
     * @throws IOException if there is a problem writing to the underlying stream
     */
    public void writeAttr(String name, String value) throws IOException {
        if (state != IN_TAG)
            throw new IllegalStateException();

        out.write(" ");
        out.write(name);
        out.write("=");
        boolean alpha = true;
        for (int i = 0; i < value.length() && alpha; i++)
            alpha = Character.isLetter(value.charAt(i));
        if (!alpha)
            out.write("\"");
        out.write(value);
        if (!alpha)
            out.write("\"");
    }

    /**
     * Write an attribute for a tag. A tag must previously have been started.
     * All tag attributes must be written before any body text is written.
     * The value will be quoted if necessary when writing it to the underlying
     * stream. No check is made that the attribute is valid for the current tag.
     * @param name the name of the attribute to be written
     * @param value the value of the attribute to be written
     * @throws IllegalStateException if the stream is not in a state to
     * write attributes -- e.g. if this call does not follow startTag or other
     * calls of writteAttr
     * @throws IOException if there is a problem writing to the underlying stream
     */
    public void writeAttr(String name, int value) throws IOException {
        writeAttr(name, Integer.toString(value));
    }

    /**
     * Write a line of text, followed by a newline.
     * The text will be escaped as necessary.
     * @param text the text to be written.
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void writeLine(String text) throws IOException {
        write(text);
        out.newLine();
    }

    /**
     * Write body text, escaping it as necessary.
     * If this call follows a call of startTag, the open tag will be
     * closed -- meaning that no more attributes can be written until another
     * tag is started.  If the text value is null, the current tag will still
     * be closed, but no other text will be written.
     * @param text the text to be written, may be null or zero length.
     * @throws IOException if there is a problem writing to the underlying stream
     */
    public void write(String text) throws IOException {
        if (state == IN_TAG) {
            out.write(">");
            state = IN_BODY;
        }

        if (text == null)
            return;

        // check to see if there are any special characters
        boolean specialChars = false;
        for (int i = 0; i < text.length() && !specialChars; i++) {
            switch (text.charAt(i)) {
            case '<': case '>': case '&':
                specialChars = true;
            }
        }

        // if there are special characters write the string character at a time;
        // otherwise, write it out as is
        if (specialChars) {
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                switch (c) {
                case '<': out.write("&lt;"); break;
                case '>': out.write("&gt;"); break;
                case '&': out.write("&amp;"); break;
                default: out.write(c);
                }
            }
        }
        else
            out.write(text);
    }

    /**
     * Write a basic HTML entity, such as &nbsp; or &#123; .
     * @param entity the entity to write
     * @throws IOException if there is a problem writing to the underlying stream
     */
    public void writeEntity(String entity) throws IOException {
        if (state == IN_TAG) {
            out.write(">");
            state = IN_BODY;
        }
        out.write(entity);
    }

    /**
     * Write an image tag, using a specified path for the image source attribute.
     * @param imagePath the path for the image source
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void writeImage(String imagePath) throws IOException {
        startTag(IMAGE);
        writeAttr(SRC, imagePath);
    }

    /**
     * Write an image tag, using a specified path for the image source attribute.
     * @param imageURL the url for the image source
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void writeImage(URL imageURL) throws IOException {
        writeImage(imageURL.toString());
    }

    /**
     * Write a hypertext link.
     * @param anchor the target for the link
     * @param body the body text for the link
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void writeLink(String anchor, String body) throws IOException {
        startTag(A);
        writeAttr(HREF, anchor);
        write(body);
        endTag(A);
    }

    /**
     * Write a hypertext link.
     * @param file the target for the link
     * @param body the body text for the link
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void writeLink(File file, String body) throws IOException {
        startTag(A);
        StringBuilder sb = new StringBuilder();
        String path = file.getPath().replace(File.separatorChar, '/');
        if (file.isAbsolute() && !path.startsWith("/"))
            sb.append('/');
        sb.append(path);
        writeAttr(HREF, sb.toString());
        write(body);
        endTag(A);
    }

    /**
     * Write a hypertext link.
     * @param file the target and body for the link
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void writeLink(File file) throws IOException {
        writeLink(file, file.getPath());
    }

    /**
     * Write a hypertext link.
     * @param url the target for the link
     * @param body the body text for the link
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void writeLink(URL url, String body) throws IOException {
        startTag(A);
        writeAttr(HREF, url.toString());
        write(body);
        endTag(A);
    }

    /**
     * Write the destination marker for a hypertext link.
     * @param anchor the destination marker for hypertext links
     * @param body the body text for the marker
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void writeLinkDestination(String anchor, String body) throws IOException {
        startTag(A);
        writeAttr(NAME, anchor);
        write(body);
        endTag(A);
    }

    /**
     * Write a parameter tag.
     * @param name the name of the parameter
     * @param value the value of the parameter
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void writeParam(String name, String value) throws IOException {
        startTag(PARAM);
        writeAttr(NAME, name);
        writeAttr(VALUE, value);
    }

    /**
     * Write a style attribute.
     * @param value the value for the style atrtribute
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void writeStyleAttr(String value) throws IOException {
        writeAttr(STYLE, value);
    }

    /**
     * Write a localized message, using a specified resource bundle.
     * @param i18n the resource bundle used to localize the message
     * @param key the key for the message to be localized
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void write(ResourceBundle i18n, String key) throws IOException {
        write(getString(i18n, key));
    }

    /**
     * Write a localized message, using a specified resource bundle.
     * @param i18n the resource bundle used to localize the message
     * @param key the key for the message to be localized
     * @param arg an argument to be formatted into the localized message
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void write(ResourceBundle i18n, String key, Object arg) throws IOException {
        write(getString(i18n, key, arg));
    }

    /**
     * Write a localized message, using a specified resource bundle.
     * @param i18n the resource bundle used to localize the message
     * @param key the key for the message to be localized
     * @param args arguments to be formatted into the localized message
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void write(ResourceBundle i18n, String key, Object[] args) throws IOException {
        write(getString(i18n, key, args));
    }

    /**
     * Write a localized message, using the default resource bundle.
     * @param key the key for the message to be localized
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void writeI18N(String key) throws IOException {
        write(getString(i18n, key));
    }

    /**
     * Write a localized message, using the default resource bundle.
     * @param key the key for the message to be localized
     * @param arg an argument to be formatted into the localized message
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void writeI18N(String key, Object arg) throws IOException {
        write(getString(i18n, key, arg));
    }

    /**
     * Write a localized message, using the default resource bundle.
     * @param key the key for the message to be localized
     * @param args arguments to be formatted into the localized message
     * @throws IOException if there is a problem closing the underlying stream
     */
    public void writeI18N(String key, Object[] args) throws IOException {
        write(getString(i18n, key, args));
    }

    private String getString(ResourceBundle rb, String key, Object... args) {
        String s = rb.getString(key);
        return MessageFormat.format(s, args);
    }

    /** The HTML "a" tag. */
    public static final String A = "a";
    /** The HTML "align" attribute. */
    public static final String ALIGN = "align";
    /** The HTML "b" tag. */
    public static final String B = "b";
    /** The HTML "body" tag. */
    public static final String BODY = "body";
    /** The HTML "border" attribute. */
    public static final String BORDER = "border";
    /** The HTML "br" tag. */
    public static final String BR = "br";
    /** The HTML "charset" attribute. */
    public static final String CHARSET  = "charset";
    /** The HTML "class" attribute. */
    public static final String CLASS  = "class";
    /** The HTML "classid" attribute. */
    public static final String CLASSID  = "classid";
    /** The HTML "code" tag. */
    public static final String CODE  = "code";
    /** The HTML "color" attribute. */
    public static final String COLOR  = "color";
    /** The HTML "col" attribute value. */
    public static final String COL = "col";
    /** The HTML "dd" tag. */
    public static final String DD = "dd";
    /** The HTML "div" tag. */
    public static final String DIV = "div";
    /** The HTML "dl" tag. */
    public static final String DL = "dl";
    /** The HTML "dt" tag. */
    public static final String DT = "dt";
    /** The HTML "font" tag. */
    public static final String FONT = "font";
    /** The HTML "h1" tag. */
    public static final String H1 = "h1";
    /** The HTML "h2" tag. */
    public static final String H2 = "h2";
    /** The HTML "h3" tag. */
    public static final String H3 = "h3";
    /** The HTML "h4" tag. */
    public static final String H4 = "h4";
    /** The HTML "h5" tag. */
    public static final String H5 = "h5";
    /** The HTML "head" tag. */
    public static final String HEAD = "head";
    /** The HTML "href" attribute. */
    public static final String HREF = "href";
    /** The HTML "html" tag. */
    public static final String HTML = "html";
    /** The HTML "hr" tag. */
    public static final String HR = "hr";
    /** The HTML "i" tag. */
    public static final String I = "i";
    /** The HTML "id" tag. */
    public static final String ID = "id";
    /** The HTML "image" tag. */
    public static final String IMAGE = "image";
    /** The HTML "left" attribute value. */
    public static final String LEFT = "left";
    /** The HTML "li" tag. */
    public static final String LI = "li";
    /** The HTML "link" tag. */
    public static final String LINK = "link";
    /** The HTML "meta" attribute. */
    public static final String META = "meta";
    /** The HTML "name" attribute. */
    public static final String NAME = "name";
    /** The HTML "object" tag. */
    public static final String OBJECT = "object";
    /** The HTML "p" tag. */
    public static final String PARAM = "param";
    /** The HTML "param" tag. */
    public static final String P = "p";
    /** The HTML "rel" attribute value. */
    public static final String REL = "rel";
    /** The HTML "right" attribute value. */
    public static final String RIGHT = "right";
    /** The HTML "row" attribute value. */
    public static final String ROW = "row";
    /** The HTML "script" tag. */
    public static final String SCRIPT = "script";
    /** The HTML "small" tag. */
    public static final String SMALL = "small";
    /** The HTML "span" tag. */
    public static final String SPAN = "span";
    /** The HTML "src" attribute. */
    public static final String SRC = "src";
    /** The HTML "scope" attribute. */
    public static final String SCOPE = "scope";
    /** The HTML "style" attribute. */
    public static final String STYLE = "style";
    /** The HTML "table" tag. */
    public static final String TABLE = "table";
    /** The HTML "td" tag. */
    public static final String TD = "td";
    /** The HTML type for JavaScript. */
    public static final String TEXT_JAVASCRIPT = "text/javascript";
    /** The HTML "title"attribute. */
    public static final String TITLE = "title";
    /** The HTML "th" tag. */
    public static final String TH = "th";
    /** The HTML "top" attribute value. */
    public static final String TOP = "top";
    /** The HTML "tr" tag. */
    public static final String TR = "tr";
    /** The HTML "type" attribute. */
    public static final String TYPE = "type";
    /** The HTML "ul" tag. */
    public static final String UL = "ul";
    /** The HTML "valign" attribute. */
    public static final String VALIGN = "valign";
    /** The HTML "value" attribute. */
    public static final String VALUE = "value";


    private BufferedWriter out;
    private int state;
    private ResourceBundle i18n;
    private static final int IN_TAG = 1;
    private static final int IN_BODY = 2;
}
