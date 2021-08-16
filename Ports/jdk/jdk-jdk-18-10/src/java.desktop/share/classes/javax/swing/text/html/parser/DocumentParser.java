/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.text.html.parser;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTML;
import javax.swing.text.ChangedCharSetException;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * A Parser for HTML Documents (actually, you can specify a DTD, but
 * you should really only use this class with the html dtd in swing).
 * Reads an InputStream of HTML and
 * invokes the appropriate methods in the ParserCallback class. This
 * is the default parser used by HTMLEditorKit to parse HTML url's.
 * <p>This will message the callback for all valid tags, as well as
 * tags that are implied but not explicitly specified. For example, the
 * html string (&lt;p&gt;blah) only has a p tag defined. The callback
 * will see the following methods:
 * <ol><li><i>handleStartTag(html, ...)</i></li>
 *     <li><i>handleStartTag(head, ...)</i></li>
 *     <li><i>handleEndTag(head)</i></li>
 *     <li><i>handleStartTag(body, ...)</i></li>
 *     <li><i>handleStartTag(p, ...)</i></li>
 *     <li><i>handleText(...)</i></li>
 *     <li><i>handleEndTag(p)</i></li>
 *     <li><i>handleEndTag(body)</i></li>
 *     <li><i>handleEndTag(html)</i></li>
 * </ol>
 * The items in <i>italic</i> are implied, that is, although they were not
 * explicitly specified, to be correct html they should have been present
 * (head isn't necessary, but it is still generated). For tags that
 * are implied, the AttributeSet argument will have a value of
 * <code>Boolean.TRUE</code> for the key
 * <code>HTMLEditorKit.ParserCallback.IMPLIED</code>.
 * <p>HTML.Attributes defines a type safe enumeration of html attributes.
 * If an attribute key of a tag is defined in HTML.Attribute, the
 * HTML.Attribute will be used as the key, otherwise a String will be used.
 * For example &lt;p foo=bar class=neat&gt; has two attributes. foo is
 * not defined in HTML.Attribute, where as class is, therefore the
 * AttributeSet will have two values in it, HTML.Attribute.CLASS with
 * a String value of 'neat' and the String key 'foo' with a String value of
 * 'bar'.
 * <p>The position argument will indicate the start of the tag, comment
 * or text. Similar to arrays, the first character in the stream has a
 * position of 0. For tags that are
 * implied the position will indicate
 * the location of the next encountered tag. In the first example,
 * the implied start body and html tags will have the same position as the
 * p tag, and the implied end p, html and body tags will all have the same
 * position.
 * <p>As html skips whitespace the position for text will be the position
 * of the first valid character, eg in the string '\n\n\nblah'
 * the text 'blah' will have a position of 3, the newlines are skipped.
 * <p>
 * For attributes that do not have a value, eg in the html
 * string <code>&lt;foo blah&gt;</code> the attribute <code>blah</code>
 * does not have a value, there are two possible values that will be
 * placed in the AttributeSet's value:
 * <ul>
 * <li>If the DTD does not contain an definition for the element, or the
 *     definition does not have an explicit value then the value in the
 *     AttributeSet will be <code>HTML.NULL_ATTRIBUTE_VALUE</code>.
 * <li>If the DTD contains an explicit value, as in:
 *     <code>&lt;!ATTLIST OPTION selected (selected) #IMPLIED&gt;</code>
 *     this value from the dtd (in this case selected) will be used.
 * </ul>
 * <p>
 * Once the stream has been parsed, the callback is notified of the most
 * likely end of line string. The end of line string will be one of
 * \n, \r or \r\n, which ever is encountered the most in parsing the
 * stream.
 *
 * @author      Sunita Mani
 */
public class DocumentParser extends javax.swing.text.html.parser.Parser {

    private int inbody;
    private int intitle;
    private int inhead;
    private int instyle;
    private int inscript;
    private boolean seentitle;
    private HTMLEditorKit.ParserCallback callback = null;
    private boolean ignoreCharSet = false;
    private static final boolean debugFlag = false;

    /**
     * Creates document parser with the specified {@code dtd}.
     *
     * @param dtd the dtd.
     */
    public DocumentParser(DTD dtd) {
        super(dtd);
    }

    /**
     * Parse an HTML stream, given a DTD.
     *
     * @param in the reader to read the source from
     * @param callback the callback
     * @param ignoreCharSet if {@code true} the charset is ignored
     * @throws IOException if an I/O error occurs
     */
    public void parse(Reader in, HTMLEditorKit.ParserCallback callback, boolean ignoreCharSet) throws IOException {
        this.ignoreCharSet = ignoreCharSet;
        this.callback = callback;
        parse(in);
        // end of line
        callback.handleEndOfLineString(getEndOfLineString());
    }

    /**
     * Handle Start Tag.
     */
    protected void handleStartTag(TagElement tag) {

        Element elem = tag.getElement();
        if (elem == dtd.body) {
            inbody++;
        } else if (elem == dtd.html) {
        } else if (elem == dtd.head) {
            inhead++;
        } else if (elem == dtd.title) {
            intitle++;
        } else if (elem == dtd.style) {
            instyle++;
        } else if (elem == dtd.script) {
            inscript++;
        }
        if (debugFlag) {
            if (tag.fictional()) {
                debug("Start Tag: " + tag.getHTMLTag() + " pos: " + getCurrentPos());
            } else {
                debug("Start Tag: " + tag.getHTMLTag() + " attributes: " +
                      getAttributes() + " pos: " + getCurrentPos());
            }
        }
        if (tag.fictional()) {
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            attrs.addAttribute(HTMLEditorKit.ParserCallback.IMPLIED,
                               Boolean.TRUE);
            callback.handleStartTag(tag.getHTMLTag(), attrs,
                                    getBlockStartPosition());
        } else {
            callback.handleStartTag(tag.getHTMLTag(), getAttributes(),
                                    getBlockStartPosition());
            flushAttributes();
        }
    }


    protected void handleComment(char[] text) {
        if (debugFlag) {
            debug("comment: ->" + new String(text) + "<-"
                  + " pos: " + getCurrentPos());
        }
        callback.handleComment(text, getBlockStartPosition());
    }

    /**
     * Handle Empty Tag.
     */
    protected void handleEmptyTag(TagElement tag) throws ChangedCharSetException {

        Element elem = tag.getElement();
        if (elem == dtd.meta && !ignoreCharSet) {
            SimpleAttributeSet atts = getAttributes();
            if (atts != null) {
                String content = (String)atts.getAttribute(HTML.Attribute.CONTENT);
                if (content != null) {
                    if ("content-type".equalsIgnoreCase((String)atts.getAttribute(HTML.Attribute.HTTPEQUIV))) {
                        if (!content.equalsIgnoreCase("text/html") &&
                                !content.equalsIgnoreCase("text/plain")) {
                            throw new ChangedCharSetException(content, false);
                        }
                    } else if ("charset" .equalsIgnoreCase((String)atts.getAttribute(HTML.Attribute.HTTPEQUIV))) {
                        throw new ChangedCharSetException(content, true);
                    }
                }
            }
        }
        if (inbody != 0 || elem == dtd.meta || elem == dtd.base || elem == dtd.isindex || elem == dtd.style || elem == dtd.link) {
            if (debugFlag) {
                if (tag.fictional()) {
                    debug("Empty Tag: " + tag.getHTMLTag() + " pos: " + getCurrentPos());
                } else {
                    debug("Empty Tag: " + tag.getHTMLTag() + " attributes: "
                          + getAttributes() + " pos: " + getCurrentPos());
                }
            }
            if (tag.fictional()) {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                attrs.addAttribute(HTMLEditorKit.ParserCallback.IMPLIED,
                                   Boolean.TRUE);
                callback.handleSimpleTag(tag.getHTMLTag(), attrs,
                                         getBlockStartPosition());
            } else {
                callback.handleSimpleTag(tag.getHTMLTag(), getAttributes(),
                                         getBlockStartPosition());
                flushAttributes();
            }
        }
    }

    /**
     * Handle End Tag.
     */
    protected void handleEndTag(TagElement tag) {
        Element elem = tag.getElement();
        if (elem == dtd.body) {
            inbody--;
        } else if (elem == dtd.title) {
            intitle--;
            seentitle = true;
        } else if (elem == dtd.head) {
            inhead--;
        } else if (elem == dtd.style) {
            instyle--;
        } else if (elem == dtd.script) {
            inscript--;
        }
        if (debugFlag) {
            debug("End Tag: " + tag.getHTMLTag() + " pos: " + getCurrentPos());
        }
        callback.handleEndTag(tag.getHTMLTag(), getBlockStartPosition());

    }

    /**
     * Handle Text.
     */
    protected void handleText(char[] data) {
        if (data != null) {
            if (inscript != 0) {
                callback.handleComment(data, getBlockStartPosition());
                return;
            }
            if (inbody != 0 || ((instyle != 0) ||
                                ((intitle != 0) && !seentitle))) {
                if (debugFlag) {
                    debug("text:  ->" + new String(data) + "<-" + " pos: " + getCurrentPos());
                }
                callback.handleText(data, getBlockStartPosition());
            }
        }
    }

    /*
     * Error handling.
     */
    protected void handleError(int ln, String errorMsg) {
        if (debugFlag) {
            debug("Error: ->" + errorMsg + "<-" + " pos: " + getCurrentPos());
        }
        /* PENDING: need to improve the error string. */
        callback.handleError(errorMsg, getCurrentPos());
    }


    /*
     * debug messages
     */
    private void debug(String msg) {
        System.out.println(msg);
    }
}
