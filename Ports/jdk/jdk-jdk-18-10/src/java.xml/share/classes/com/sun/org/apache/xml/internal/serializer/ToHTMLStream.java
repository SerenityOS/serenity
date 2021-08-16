/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xml.internal.serializer;

import java.io.IOException;
import java.util.Properties;

import javax.xml.transform.Result;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.serializer.utils.MsgKey;
import com.sun.org.apache.xml.internal.serializer.utils.Utils;
import javax.xml.transform.ErrorListener;

/**
 * This serializer takes a series of SAX or
 * SAX-like events and writes its output
 * to the given stream.
 *
 * This class is not a public API, it is public
 * because it is used from another package.
 *
 * @xsl.usage internal
 * @LastModified: June 2021
 */
public final class ToHTMLStream extends ToStream
{

    /** This flag is set while receiving events from the DTD */
    protected boolean m_inDTD = false;

    /** True if the previous element is a block element. */
    private boolean m_isprevblock = false;

    /**
     * Map that tells which XML characters should have special treatment, and it
     *  provides character to entity name lookup.
     */
    private static final CharInfo m_htmlcharInfo =
//        new CharInfo(CharInfo.HTML_ENTITIES_RESOURCE);
        CharInfo.getCharInfoInternal(CharInfo.HTML_ENTITIES_RESOURCE, Method.HTML);

    /** A digital search trie for fast, case insensitive lookup of ElemDesc objects. */
    static final Trie m_elementFlags = new Trie();

    static {
        initTagReference(m_elementFlags);
    }
    static void initTagReference(Trie m_elementFlags) {

        // HTML 4.0 loose DTD
        m_elementFlags.put("BASEFONT", new ElemDesc(0 | ElemDesc.EMPTY));
        m_elementFlags.put(
            "FRAME",
            new ElemDesc(0 | ElemDesc.EMPTY | ElemDesc.BLOCK));
        m_elementFlags.put("FRAMESET", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put("NOFRAMES", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put(
            "ISINDEX",
            new ElemDesc(0 | ElemDesc.EMPTY | ElemDesc.BLOCK));
        m_elementFlags.put(
            "APPLET",
            new ElemDesc(0 | ElemDesc.WHITESPACESENSITIVE));
        m_elementFlags.put("CENTER", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put("DIR", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put("MENU", new ElemDesc(0 | ElemDesc.BLOCK));

        // HTML 4.0 strict DTD
        m_elementFlags.put("TT", new ElemDesc(0 | ElemDesc.FONTSTYLE));
        m_elementFlags.put("I", new ElemDesc(0 | ElemDesc.FONTSTYLE));
        m_elementFlags.put("B", new ElemDesc(0 | ElemDesc.FONTSTYLE));
        m_elementFlags.put("BIG", new ElemDesc(0 | ElemDesc.FONTSTYLE));
        m_elementFlags.put("SMALL", new ElemDesc(0 | ElemDesc.FONTSTYLE));
        m_elementFlags.put("EM", new ElemDesc(0 | ElemDesc.PHRASE));
        m_elementFlags.put("STRONG", new ElemDesc(0 | ElemDesc.PHRASE));
        m_elementFlags.put("DFN", new ElemDesc(0 | ElemDesc.PHRASE));
        m_elementFlags.put("CODE", new ElemDesc(0 | ElemDesc.PHRASE));
        m_elementFlags.put("SAMP", new ElemDesc(0 | ElemDesc.PHRASE));
        m_elementFlags.put("KBD", new ElemDesc(0 | ElemDesc.PHRASE));
        m_elementFlags.put("VAR", new ElemDesc(0 | ElemDesc.PHRASE));
        m_elementFlags.put("CITE", new ElemDesc(0 | ElemDesc.PHRASE));
        m_elementFlags.put("ABBR", new ElemDesc(0 | ElemDesc.PHRASE));
        m_elementFlags.put("ACRONYM", new ElemDesc(0 | ElemDesc.PHRASE));
        m_elementFlags.put(
            "SUP",
            new ElemDesc(0 | ElemDesc.SPECIAL | ElemDesc.ASPECIAL));
        m_elementFlags.put(
            "SUB",
            new ElemDesc(0 | ElemDesc.SPECIAL | ElemDesc.ASPECIAL));
        m_elementFlags.put(
            "SPAN",
            new ElemDesc(0 | ElemDesc.SPECIAL | ElemDesc.ASPECIAL));
        m_elementFlags.put(
            "BDO",
            new ElemDesc(0 | ElemDesc.SPECIAL | ElemDesc.ASPECIAL));
        m_elementFlags.put(
            "BR",
            new ElemDesc(
                0
                    | ElemDesc.SPECIAL
                    | ElemDesc.ASPECIAL
                    | ElemDesc.EMPTY
                    | ElemDesc.BLOCK));
        m_elementFlags.put("BODY", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put(
            "ADDRESS",
            new ElemDesc(
                0
                    | ElemDesc.BLOCK
                    | ElemDesc.BLOCKFORM
                    | ElemDesc.BLOCKFORMFIELDSET));
        m_elementFlags.put(
            "DIV",
            new ElemDesc(
                0
                    | ElemDesc.BLOCK
                    | ElemDesc.BLOCKFORM
                    | ElemDesc.BLOCKFORMFIELDSET));
        m_elementFlags.put("A", new ElemDesc(0 | ElemDesc.SPECIAL));
        m_elementFlags.put(
            "MAP",
            new ElemDesc(
                0 | ElemDesc.SPECIAL | ElemDesc.ASPECIAL | ElemDesc.BLOCK));
        m_elementFlags.put(
            "AREA",
            new ElemDesc(0 | ElemDesc.EMPTY | ElemDesc.BLOCK));
        m_elementFlags.put(
            "LINK",
            new ElemDesc(
                0 | ElemDesc.HEADMISC | ElemDesc.EMPTY | ElemDesc.BLOCK));
        m_elementFlags.put(
            "IMG",
            new ElemDesc(
                0
                    | ElemDesc.SPECIAL
                    | ElemDesc.ASPECIAL
                    | ElemDesc.EMPTY
                    | ElemDesc.WHITESPACESENSITIVE));
        m_elementFlags.put(
            "OBJECT",
            new ElemDesc(
                0
                    | ElemDesc.SPECIAL
                    | ElemDesc.ASPECIAL
                    | ElemDesc.HEADMISC
                    | ElemDesc.WHITESPACESENSITIVE));
        m_elementFlags.put("PARAM", new ElemDesc(0 | ElemDesc.EMPTY));
        m_elementFlags.put(
            "HR",
            new ElemDesc(
                0
                    | ElemDesc.BLOCK
                    | ElemDesc.BLOCKFORM
                    | ElemDesc.BLOCKFORMFIELDSET
                    | ElemDesc.EMPTY));
        m_elementFlags.put(
            "P",
            new ElemDesc(
                0
                    | ElemDesc.BLOCK
                    | ElemDesc.BLOCKFORM
                    | ElemDesc.BLOCKFORMFIELDSET));
        m_elementFlags.put(
            "H1",
            new ElemDesc(0 | ElemDesc.HEAD | ElemDesc.BLOCK));
        m_elementFlags.put(
            "H2",
            new ElemDesc(0 | ElemDesc.HEAD | ElemDesc.BLOCK));
        m_elementFlags.put(
            "H3",
            new ElemDesc(0 | ElemDesc.HEAD | ElemDesc.BLOCK));
        m_elementFlags.put(
            "H4",
            new ElemDesc(0 | ElemDesc.HEAD | ElemDesc.BLOCK));
        m_elementFlags.put(
            "H5",
            new ElemDesc(0 | ElemDesc.HEAD | ElemDesc.BLOCK));
        m_elementFlags.put(
            "H6",
            new ElemDesc(0 | ElemDesc.HEAD | ElemDesc.BLOCK));
        m_elementFlags.put(
            "PRE",
            new ElemDesc(0 | ElemDesc.PREFORMATTED | ElemDesc.BLOCK));
        m_elementFlags.put(
            "Q",
            new ElemDesc(0 | ElemDesc.SPECIAL | ElemDesc.ASPECIAL));
        m_elementFlags.put(
            "BLOCKQUOTE",
            new ElemDesc(
                0
                    | ElemDesc.BLOCK
                    | ElemDesc.BLOCKFORM
                    | ElemDesc.BLOCKFORMFIELDSET));
        m_elementFlags.put("INS", new ElemDesc(0));
        m_elementFlags.put("DEL", new ElemDesc(0));
        m_elementFlags.put(
            "DL",
            new ElemDesc(
                0
                    | ElemDesc.BLOCK
                    | ElemDesc.BLOCKFORM
                    | ElemDesc.BLOCKFORMFIELDSET));
        m_elementFlags.put("DT", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put("DD", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put(
            "OL",
            new ElemDesc(0 | ElemDesc.LIST | ElemDesc.BLOCK));
        m_elementFlags.put(
            "UL",
            new ElemDesc(0 | ElemDesc.LIST | ElemDesc.BLOCK));
        m_elementFlags.put("LI", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put("FORM", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put("LABEL", new ElemDesc(0 | ElemDesc.FORMCTRL));
        m_elementFlags.put(
            "INPUT",
            new ElemDesc(
                0 | ElemDesc.FORMCTRL | ElemDesc.INLINELABEL | ElemDesc.EMPTY));
        m_elementFlags.put(
            "SELECT",
            new ElemDesc(0 | ElemDesc.FORMCTRL | ElemDesc.INLINELABEL));
        m_elementFlags.put("OPTGROUP", new ElemDesc(0));
        m_elementFlags.put("OPTION", new ElemDesc(0));
        m_elementFlags.put(
            "TEXTAREA",
            new ElemDesc(0 | ElemDesc.FORMCTRL | ElemDesc.INLINELABEL));
        m_elementFlags.put(
            "FIELDSET",
            new ElemDesc(0 | ElemDesc.BLOCK | ElemDesc.BLOCKFORM));
        m_elementFlags.put("LEGEND", new ElemDesc(0));
        m_elementFlags.put(
            "BUTTON",
            new ElemDesc(0 | ElemDesc.FORMCTRL | ElemDesc.INLINELABEL));
        m_elementFlags.put(
            "TABLE",
            new ElemDesc(
                0
                    | ElemDesc.BLOCK
                    | ElemDesc.BLOCKFORM
                    | ElemDesc.BLOCKFORMFIELDSET));
        m_elementFlags.put("CAPTION", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put("THEAD", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put("TFOOT", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put("TBODY", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put("COLGROUP", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put(
            "COL",
            new ElemDesc(0 | ElemDesc.EMPTY | ElemDesc.BLOCK));
        m_elementFlags.put("TR", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put("TH", new ElemDesc(0));
        m_elementFlags.put("TD", new ElemDesc(0));
        m_elementFlags.put(
            "HEAD",
            new ElemDesc(0 | ElemDesc.BLOCK | ElemDesc.HEADELEM));
        m_elementFlags.put("TITLE", new ElemDesc(0 | ElemDesc.BLOCK));
        m_elementFlags.put(
            "BASE",
            new ElemDesc(0 | ElemDesc.EMPTY | ElemDesc.BLOCK));
        m_elementFlags.put(
            "META",
            new ElemDesc(
                0 | ElemDesc.HEADMISC | ElemDesc.EMPTY | ElemDesc.BLOCK));
        m_elementFlags.put(
            "STYLE",
            new ElemDesc(
                0 | ElemDesc.HEADMISC | ElemDesc.RAW | ElemDesc.BLOCK));
        m_elementFlags.put(
            "SCRIPT",
            new ElemDesc(
                0
                    | ElemDesc.SPECIAL
                    | ElemDesc.ASPECIAL
                    | ElemDesc.HEADMISC
                    | ElemDesc.RAW));
        m_elementFlags.put(
            "NOSCRIPT",
            new ElemDesc(
                0
                    | ElemDesc.BLOCK
                    | ElemDesc.BLOCKFORM
                    | ElemDesc.BLOCKFORMFIELDSET));
        m_elementFlags.put("HTML", new ElemDesc(0 | ElemDesc.BLOCK));

        // From "John Ky" <hand@syd.speednet.com.au
        // Transitional Document Type Definition ()
        // file:///C:/Documents%20and%20Settings/sboag.BOAG600E/My%20Documents/html/sgml/loosedtd.html#basefont
        m_elementFlags.put("FONT", new ElemDesc(0 | ElemDesc.FONTSTYLE));

        // file:///C:/Documents%20and%20Settings/sboag.BOAG600E/My%20Documents/html/present/graphics.html#edef-STRIKE
        m_elementFlags.put("S", new ElemDesc(0 | ElemDesc.FONTSTYLE));
        m_elementFlags.put("STRIKE", new ElemDesc(0 | ElemDesc.FONTSTYLE));

        // file:///C:/Documents%20and%20Settings/sboag.BOAG600E/My%20Documents/html/present/graphics.html#edef-U
        m_elementFlags.put("U", new ElemDesc(0 | ElemDesc.FONTSTYLE));

        // From "John Ky" <hand@syd.speednet.com.au
        m_elementFlags.put("NOBR", new ElemDesc(0 | ElemDesc.FONTSTYLE));

        // HTML 4.0, section 16.5
        m_elementFlags.put(
            "IFRAME",
            new ElemDesc(
                0
                    | ElemDesc.BLOCK
                    | ElemDesc.BLOCKFORM
                    | ElemDesc.BLOCKFORMFIELDSET));

        // Netscape 4 extension
        m_elementFlags.put(
            "LAYER",
            new ElemDesc(
                0
                    | ElemDesc.BLOCK
                    | ElemDesc.BLOCKFORM
                    | ElemDesc.BLOCKFORMFIELDSET));
        // Netscape 4 extension
        m_elementFlags.put(
            "ILAYER",
            new ElemDesc(
                0
                    | ElemDesc.BLOCK
                    | ElemDesc.BLOCKFORM
                    | ElemDesc.BLOCKFORMFIELDSET));


        // NOW FOR ATTRIBUTE INFORMATION . . .
        ElemDesc elemDesc;


        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("a");
        elemDesc.setAttr("HREF", ElemDesc.ATTRURL);
        elemDesc.setAttr("NAME", ElemDesc.ATTRURL);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("area");
        elemDesc.setAttr("HREF", ElemDesc.ATTRURL);
        elemDesc.setAttr("NOHREF", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("base");
        elemDesc.setAttr("HREF", ElemDesc.ATTRURL);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("button");
        elemDesc.setAttr("DISABLED", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("blockquote");
        elemDesc.setAttr("CITE", ElemDesc.ATTRURL);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("del");
        elemDesc.setAttr("CITE", ElemDesc.ATTRURL);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("dir");
        elemDesc.setAttr("COMPACT", ElemDesc.ATTREMPTY);

        // ----------------------------------------------

        elemDesc = (ElemDesc) m_elementFlags.get("div");
        elemDesc.setAttr("SRC", ElemDesc.ATTRURL); // Netscape 4 extension
        elemDesc.setAttr("NOWRAP", ElemDesc.ATTREMPTY); // Internet-Explorer extension

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("dl");
        elemDesc.setAttr("COMPACT", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("form");
        elemDesc.setAttr("ACTION", ElemDesc.ATTRURL);

        // ----------------------------------------------
        // Attribution to: "Voytenko, Dimitry" <DVoytenko@SECTORBASE.COM>
        elemDesc = (ElemDesc) m_elementFlags.get("frame");
        elemDesc.setAttr("SRC", ElemDesc.ATTRURL);
        elemDesc.setAttr("LONGDESC", ElemDesc.ATTRURL);
        elemDesc.setAttr("NORESIZE",ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("head");
        elemDesc.setAttr("PROFILE", ElemDesc.ATTRURL);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("hr");
        elemDesc.setAttr("NOSHADE", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        // HTML 4.0, section 16.5
        elemDesc = (ElemDesc) m_elementFlags.get("iframe");
        elemDesc.setAttr("SRC", ElemDesc.ATTRURL);
        elemDesc.setAttr("LONGDESC", ElemDesc.ATTRURL);

        // ----------------------------------------------
        // Netscape 4 extension
        elemDesc = (ElemDesc) m_elementFlags.get("ilayer");
        elemDesc.setAttr("SRC", ElemDesc.ATTRURL);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("img");
        elemDesc.setAttr("SRC", ElemDesc.ATTRURL);
        elemDesc.setAttr("LONGDESC", ElemDesc.ATTRURL);
        elemDesc.setAttr("USEMAP", ElemDesc.ATTRURL);
        elemDesc.setAttr("ISMAP", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("input");
        elemDesc.setAttr("SRC", ElemDesc.ATTRURL);
        elemDesc.setAttr("USEMAP", ElemDesc.ATTRURL);
        elemDesc.setAttr("CHECKED", ElemDesc.ATTREMPTY);
        elemDesc.setAttr("DISABLED", ElemDesc.ATTREMPTY);
        elemDesc.setAttr("ISMAP", ElemDesc.ATTREMPTY);
        elemDesc.setAttr("READONLY", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("ins");
        elemDesc.setAttr("CITE", ElemDesc.ATTRURL);

        // ----------------------------------------------
        // Netscape 4 extension
        elemDesc = (ElemDesc) m_elementFlags.get("layer");
        elemDesc.setAttr("SRC", ElemDesc.ATTRURL);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("link");
        elemDesc.setAttr("HREF", ElemDesc.ATTRURL);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("menu");
        elemDesc.setAttr("COMPACT", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("object");
        elemDesc.setAttr("CLASSID", ElemDesc.ATTRURL);
        elemDesc.setAttr("CODEBASE", ElemDesc.ATTRURL);
        elemDesc.setAttr("DATA", ElemDesc.ATTRURL);
        elemDesc.setAttr("ARCHIVE", ElemDesc.ATTRURL);
        elemDesc.setAttr("USEMAP", ElemDesc.ATTRURL);
        elemDesc.setAttr("DECLARE", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("ol");
        elemDesc.setAttr("COMPACT", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("optgroup");
        elemDesc.setAttr("DISABLED", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("option");
        elemDesc.setAttr("SELECTED", ElemDesc.ATTREMPTY);
        elemDesc.setAttr("DISABLED", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("q");
        elemDesc.setAttr("CITE", ElemDesc.ATTRURL);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("script");
        elemDesc.setAttr("SRC", ElemDesc.ATTRURL);
        elemDesc.setAttr("FOR", ElemDesc.ATTRURL);
        elemDesc.setAttr("DEFER", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("select");
        elemDesc.setAttr("DISABLED", ElemDesc.ATTREMPTY);
        elemDesc.setAttr("MULTIPLE", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("table");
        elemDesc.setAttr("NOWRAP", ElemDesc.ATTREMPTY); // Internet-Explorer extension

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("td");
        elemDesc.setAttr("NOWRAP", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("textarea");
        elemDesc.setAttr("DISABLED", ElemDesc.ATTREMPTY);
        elemDesc.setAttr("READONLY", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("th");
        elemDesc.setAttr("NOWRAP", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        // The nowrap attribute of a tr element is both
        // a Netscape and Internet-Explorer extension
        elemDesc = (ElemDesc) m_elementFlags.get("tr");
        elemDesc.setAttr("NOWRAP", ElemDesc.ATTREMPTY);

        // ----------------------------------------------
        elemDesc = (ElemDesc) m_elementFlags.get("ul");
        elemDesc.setAttr("COMPACT", ElemDesc.ATTREMPTY);
    }

    /**
     * Dummy element for elements not found.
     */
    static private final ElemDesc m_dummy = new ElemDesc(0 | ElemDesc.BLOCK);

    /** True if URLs should be specially escaped with the %xx form. */
    private boolean m_specialEscapeURLs = true;

    /** True if the META tag should be omitted. */
    private boolean m_omitMetaTag = false;

    /**
     * Tells if the formatter should use special URL escaping.
     *
     * @param bool True if URLs should be specially escaped with the %xx form.
     */
    public void setSpecialEscapeURLs(boolean bool)
    {
        m_specialEscapeURLs = bool;
    }

    /**
     * Tells if the formatter should omit the META tag.
     *
     * @param bool True if the META tag should be omitted.
     */
    public void setOmitMetaTag(boolean bool)
    {
        m_omitMetaTag = bool;
    }

    /**
     * Specifies an output format for this serializer. It the
     * serializer has already been associated with an output format,
     * it will switch to the new format. This method should not be
     * called while the serializer is in the process of serializing
     * a document.
     *
     * This method can be called multiple times before starting
     * the serialization of a particular result-tree. In principle
     * all serialization parameters can be changed, with the exception
     * of method="html" (it must be method="html" otherwise we
     * shouldn't even have a ToHTMLStream object here!)
     *
     * @param format The output format or serialzation parameters
     * to use.
     */
    public void setOutputFormat(Properties format)
    {

        m_specialEscapeURLs =
            OutputPropertyUtils.getBooleanProperty(
                OutputPropertiesFactory.S_USE_URL_ESCAPING,
                format);

        m_omitMetaTag =
            OutputPropertyUtils.getBooleanProperty(
                OutputPropertiesFactory.S_OMIT_META_TAG,
                format);

        super.setOutputFormat(format);
    }

    /**
     * Tells if the formatter should use special URL escaping.
     *
     * @return True if URLs should be specially escaped with the %xx form.
     */
    private final boolean getSpecialEscapeURLs()
    {
        return m_specialEscapeURLs;
    }

    /**
     * Tells if the formatter should omit the META tag.
     *
     * @return True if the META tag should be omitted.
     */
    private final boolean getOmitMetaTag()
    {
        return m_omitMetaTag;
    }

    /**
     * Get a description of the given element.
     *
     * @param name non-null name of element, case insensitive.
     *
     * @return non-null reference to ElemDesc, which may be m_dummy if no
     *         element description matches the given name.
     */
    public static final ElemDesc getElemDesc(String name)
    {
        /* this method used to return m_dummy  when name was null
         * but now it doesn't check and and requires non-null name.
         */
        Object obj = m_elementFlags.get(name);
        if (null != obj)
            return (ElemDesc)obj;
        return m_dummy;
    }

    /**
     * A Trie that is just a copy of the "static" one.
     * We need this one to be able to use the faster, but not thread-safe
     * method Trie.get2(name)
     */
    private Trie m_htmlInfo = new Trie(m_elementFlags);
    /**
     * Calls to this method could be replaced with calls to
     * getElemDesc(name), but this one should be faster.
     */
    private ElemDesc getElemDesc2(String name)
    {
        Object obj = m_htmlInfo.get2(name);
        if (null != obj)
            return (ElemDesc)obj;
        return m_dummy;
    }

    /**
     * Default constructor.
     */
    public ToHTMLStream()
    {
        this(null);
    }

    public ToHTMLStream(ErrorListener l)
    {
        super(l);
        m_charInfo = m_htmlcharInfo;
        // initialize namespaces
        m_prefixMap = new NamespaceMappings();
    }

    /** The name of the current element. */
//    private String m_currentElementName = null;

    /**
     * Receive notification of the beginning of a document.
     *
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     *
     * @throws org.xml.sax.SAXException
     */
    protected void startDocumentInternal() throws org.xml.sax.SAXException
    {
        super.startDocumentInternal();

        m_needToCallStartDocument = false;
        m_needToOutputDocTypeDecl = true;
        m_startNewLine = false;
        setOmitXMLDeclaration(true);

        if (true == m_needToOutputDocTypeDecl)
        {
            String doctypeSystem = getDoctypeSystem();
            String doctypePublic = getDoctypePublic();
            if ((null != doctypeSystem) || (null != doctypePublic))
            {
                final java.io.Writer writer = m_writer;
                try
                {
                writer.write("<!DOCTYPE html");

                if (null != doctypePublic)
                {
                    writer.write(" PUBLIC \"");
                    writer.write(doctypePublic);
                    writer.write('"');
                }

                if (null != doctypeSystem)
                {
                    if (null == doctypePublic)
                        writer.write(" SYSTEM \"");
                    else
                        writer.write(" \"");

                    writer.write(doctypeSystem);
                    writer.write('"');
                }

                writer.write('>');
                outputLineSep();
                }
                catch(IOException e)
                {
                    throw new SAXException(e);
                }
            }
        }

        m_needToOutputDocTypeDecl = false;
    }

    /**
     * Receive notification of the end of a document.
     *
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     *
     * @throws org.xml.sax.SAXException
     */
    public final void endDocument() throws org.xml.sax.SAXException
    {
        if (m_doIndent) {
            flushCharactersBuffer(false);
        }
        flushPending();
        if (m_doIndent && !m_isprevtext)
        {
            try
            {
            outputLineSep();
            }
            catch(IOException e)
            {
                throw new SAXException(e);
            }
        }

        flushWriter();
        if (m_tracer != null)
            super.fireEndDoc();
    }

    /**
     * If the previous is an inline element, won't insert a new line before the
     * text.
     *
     */
    protected boolean shouldIndentForText() {
        return super.shouldIndentForText() && m_isprevblock;
    }

    /**
     * Only check m_doIndent, disregard m_ispreserveSpace.
     *
     * @return True if the content should be formatted.
     */
    protected boolean shouldFormatOutput() {
        return m_doIndent;
    }

    /**
     * Receive notification of the beginning of an element.
     *
     *
     * @param namespaceURI
     * @param localName
     * @param name
     *            The element type name.
     * @param atts
     *            The attributes attached to the element, if any.
     * @throws org.xml.sax.SAXException
     *             Any SAX exception, possibly wrapping another exception.
     * @see #endElement
     * @see org.xml.sax.AttributeList
     */
    public void startElement(
        String namespaceURI,
        String localName,
        String name,
        Attributes atts)
        throws SAXException
    {
        if (m_doIndent) {
            // will add extra one if having namespace but no matter
            m_childNodeNum++;
            flushCharactersBuffer(false);
        }
        ElemContext elemContext = m_elemContext;

        // clean up any pending things first
        if (elemContext.m_startTagOpen)
        {
            closeStartTag();
            elemContext.m_startTagOpen = false;
        }
        else if (m_cdataTagOpen)
        {
            closeCDATA();
            m_cdataTagOpen = false;
        }
        else if (m_needToCallStartDocument)
        {
            startDocumentInternal();
            m_needToCallStartDocument = false;
        }


        // if this element has a namespace then treat it like XML
        if (null != namespaceURI && namespaceURI.length() > 0)
        {
            super.startElement(namespaceURI, localName, name, atts);

            return;
        }

        try
        {
            // getElemDesc2(name) is faster than getElemDesc(name)
            ElemDesc elemDesc = getElemDesc2(name);
            int elemFlags = elemDesc.getFlags();

            // deal with indentation issues first
            if (m_doIndent)
            {
                boolean isBlockElement = (elemFlags & ElemDesc.BLOCK) != 0;
                if ((elemContext.m_elementName != null)
                        // If this element is a block element,
                        // or if this is not a block element, then if the
                        // previous is neither a text nor an inline
                        && (isBlockElement || (!(m_isprevtext || !m_isprevblock))))
                {
                    m_startNewLine = true;

                    indent();
                }
                m_isprevblock = isBlockElement;
            }

            // save any attributes for later processing
            if (atts != null)
                addAttributes(atts);

            m_isprevtext = false;
            final java.io.Writer writer = m_writer;
            writer.write('<');
            writer.write(name);

            if (m_doIndent) {
                m_childNodeNumStack.add(m_childNodeNum);
                m_childNodeNum = 0;
            }

            if (m_tracer != null)
                firePseudoAttributes();

            if ((elemFlags & ElemDesc.EMPTY) != 0)
            {
                // an optimization for elements which are expected
                // to be empty.
                m_elemContext = elemContext.push();
                /* XSLTC sometimes calls namespaceAfterStartElement()
                 * so we need to remember the name
                 */
                m_elemContext.m_elementName = name;
                m_elemContext.m_elementDesc = elemDesc;
                return;
            }
            else
            {
                elemContext = elemContext.push(namespaceURI,localName,name);
                m_elemContext = elemContext;
                elemContext.m_elementDesc = elemDesc;
                elemContext.m_isRaw = (elemFlags & ElemDesc.RAW) != 0;

                // set m_startNewLine for the next element
                if (m_doIndent) {
                    // elemFlags is equivalent to m_elemContext.m_elementDesc.getFlags(),
                    // in this branch m_elemContext.m_elementName is not null
                    boolean isBlockElement = (elemFlags & ElemDesc.BLOCK) != 0;
                    if (isBlockElement)
                        m_startNewLine = true;
                }
            }


            if ((elemFlags & ElemDesc.HEADELEM) != 0)
            {
                // This is the <HEAD> element, do some special processing
                closeStartTag();
                elemContext.m_startTagOpen = false;
                if (!m_omitMetaTag)
                {
                    if (m_doIndent)
                        indent();
                    writer.write(
                        "<META http-equiv=\"Content-Type\" content=\"text/html; charset=");
                    String encoding = getEncoding();
                    String encode = Encodings.getMimeEncoding(encoding);
                    writer.write(encode);
                    writer.write("\">");
                }
            }
        }
        catch (IOException e)
        {
            throw new SAXException(e);
        }
    }

    /**
     *  Receive notification of the end of an element.
     *
     *
     *  @param namespaceURI
     *  @param localName
     *  @param name The element type name
     *  @throws org.xml.sax.SAXException Any SAX exception, possibly
     *             wrapping another exception.
     */
    public final void endElement(
        final String namespaceURI,
        final String localName,
        final String name)
        throws org.xml.sax.SAXException
    {
        if (m_doIndent) {
            flushCharactersBuffer(false);
        }
        // deal with any pending issues
        if (m_cdataTagOpen)
            closeCDATA();

        // if the element has a namespace, treat it like XML, not HTML
        if (null != namespaceURI && namespaceURI.length() > 0)
        {
            super.endElement(namespaceURI, localName, name);

            return;
        }

        try
        {

            ElemContext elemContext = m_elemContext;
            final ElemDesc elemDesc = elemContext.m_elementDesc;
            final int elemFlags = elemDesc.getFlags();
            final boolean elemEmpty = (elemFlags & ElemDesc.EMPTY) != 0;

            // deal with any indentation issues
            if (m_doIndent)
            {
                final boolean isBlockElement = (elemFlags&ElemDesc.BLOCK) != 0;
                boolean shouldIndent = false;

                // If this element is a block element,
                // or if this is not a block element, then if the previous is
                // neither a text nor an inline
                if (isBlockElement || (!(m_isprevtext || !m_isprevblock)))
                {
                    m_startNewLine = true;
                    shouldIndent = true;
                }
                if (!elemContext.m_startTagOpen && shouldIndent && (m_childNodeNum > 1 || !m_isprevtext))
                    indent(elemContext.m_currentElemDepth - 1);

                m_isprevblock = isBlockElement;
            }

            final java.io.Writer writer = m_writer;
            if (!elemContext.m_startTagOpen)
            {
                writer.write("</");
                writer.write(name);
                writer.write('>');
            }
            else
            {
                // the start-tag open when this method was called,
                // so we need to process it now.

                if (m_tracer != null)
                    super.fireStartElem(name);

                // the starting tag was still open when we received this endElement() call
                // so we need to process any gathered attributes NOW, before they go away.
                int nAttrs = m_attributes.getLength();
                if (nAttrs > 0)
                {
                    processAttributes(m_writer, nAttrs);
                    // clear attributes object for re-use with next element
                    m_attributes.clear();
                }
                if (!elemEmpty)
                {
                    // As per Dave/Paul recommendation 12/06/2000
                    // if (shouldIndent)
                    // writer.write('>');
                    //  indent(m_currentIndent);

                    writer.write("></");
                    writer.write(name);
                    writer.write('>');
                }
                else
                {
                    writer.write('>');
                }
            }

            if (m_doIndent) {
                m_childNodeNum = m_childNodeNumStack.remove(m_childNodeNumStack.size() - 1);
                // clean up because the element has ended
                m_isprevtext = false;
            }
            // fire off the end element event
            if (m_tracer != null)
                super.fireEndElem(name);

            // OPTIMIZE-EMPTY
            if (elemEmpty)
            {
                // a quick exit if the HTML element had no children.
                // This block of code can be removed if the corresponding block of code
                // in startElement() also labeled with "OPTIMIZE-EMPTY" is also removed
                m_elemContext = elemContext.m_prev;
                return;
            }

            // some more clean because the element has ended.
            m_elemContext = elemContext.m_prev;
//            m_isRawStack.pop();
        }
        catch (IOException e)
        {
            throw new SAXException(e);
        }
    }

    /**
     * Process an attribute.
     * @param   writer The writer to write the processed output to.
     * @param   name   The name of the attribute.
     * @param   value   The value of the attribute.
     * @param   elemDesc The description of the HTML element
     *           that has this attribute.
     *
     * @throws org.xml.sax.SAXException
     */
    protected void processAttribute(
        java.io.Writer writer,
        String name,
        String value,
        ElemDesc elemDesc)
        throws IOException, SAXException
    {
        writer.write(' ');

        if (   ((value.length() == 0) || value.equalsIgnoreCase(name))
            && elemDesc != null
            && elemDesc.isAttrFlagSet(name, ElemDesc.ATTREMPTY))
        {
            writer.write(name);
        }
        else
        {
            // %REVIEW% %OPT%
            // Two calls to single-char write may NOT
            // be more efficient than one to string-write...
            writer.write(name);
            writer.write("=\"");
            if (   elemDesc != null
                && elemDesc.isAttrFlagSet(name, ElemDesc.ATTRURL))
                writeAttrURI(writer, value, m_specialEscapeURLs);
            else
                writeAttrString(writer, value, this.getEncoding());
            writer.write('"');

        }
    }

    /**
     * Tell if a character is an ASCII digit.
     */
    private boolean isASCIIDigit(char c)
    {
        return (c >= '0' && c <= '9');
    }

    /**
     * Make an integer into an HH hex value.
     * Does no checking on the size of the input, since this
     * is only meant to be used locally by writeAttrURI.
     *
     * @param i must be a value less than 255.
     *
     * @return should be a two character string.
     */
    private static String makeHHString(int i)
    {
        String s = Integer.toHexString(i).toUpperCase();
        if (s.length() == 1)
        {
            s = "0" + s;
        }
        return s;
    }

    /**
    * Dmitri Ilyin: Makes sure if the String is HH encoded sign.
    * @param str must be 2 characters long
    *
    * @return true or false
    */
    private boolean isHHSign(String str)
    {
        boolean sign = true;
        try
        {
            char r = (char) Integer.parseInt(str, 16);
        }
        catch (NumberFormatException e)
        {
            sign = false;
        }
        return sign;
    }

    /**
     * Write the specified <var>string</var> after substituting non ASCII characters,
     * with <CODE>%HH</CODE>, where HH is the hex of the byte value.
     *
     * @param   string      String to convert to XML format.
     * @param doURLEscaping True if we should try to encode as
     *                      per http://www.ietf.org/rfc/rfc2396.txt.
     *
     * @throws org.xml.sax.SAXException if a bad surrogate pair is detected.
     */
    public void writeAttrURI(
        final java.io.Writer writer, String string, boolean doURLEscaping)
        throws IOException
    {
        // http://www.ietf.org/rfc/rfc2396.txt says:
        // A URI is always in an "escaped" form, since escaping or unescaping a
        // completed URI might change its semantics.  Normally, the only time
        // escape encodings can safely be made is when the URI is being created
        // from its component parts; each component may have its own set of
        // characters that are reserved, so only the mechanism responsible for
        // generating or interpreting that component can determine whether or
        // not escaping a character will change its semantics. Likewise, a URI
        // must be separated into its components before the escaped characters
        // within those components can be safely decoded.
        //
        // ...So we do our best to do limited escaping of the URL, without
        // causing damage.  If the URL is already properly escaped, in theory, this
        // function should not change the string value.

        final int end = string.length();
        if (end > m_attrBuff.length)
        {
           m_attrBuff = new char[end*2 + 1];
        }
        string.getChars(0,end, m_attrBuff, 0);
        final char[] chars = m_attrBuff;

        int cleanStart = 0;
        int cleanLength = 0;


        char ch = 0;
        for (int i = 0; i < end; i++)
        {
            ch = chars[i];

            if ((ch < 32) || (ch > 126))
            {
                if (cleanLength > 0)
                {
                    writer.write(chars, cleanStart, cleanLength);
                    cleanLength = 0;
                }
                if (doURLEscaping)
                {
                    // Encode UTF16 to UTF8.
                    // Reference is Unicode, A Primer, by Tony Graham.
                    // Page 92.

                    // Note that Kay doesn't escape 0x20...
                    //  if(ch == 0x20) // Not sure about this... -sb
                    //  {
                    //    writer.write(ch);
                    //  }
                    //  else
                    if (ch <= 0x7F)
                    {
                        writer.write('%');
                        writer.write(makeHHString(ch));
                    }
                    else if (ch <= 0x7FF)
                    {
                        // Clear low 6 bits before rotate, put high 4 bits in low byte,
                        // and set two high bits.
                        int high = (ch >> 6) | 0xC0;
                        int low = (ch & 0x3F) | 0x80;
                        // First 6 bits, + high bit
                        writer.write('%');
                        writer.write(makeHHString(high));
                        writer.write('%');
                        writer.write(makeHHString(low));
                    }
                    else if (Encodings.isHighUTF16Surrogate(ch)) // high surrogate
                    {
                        // I'm sure this can be done in 3 instructions, but I choose
                        // to try and do it exactly like it is done in the book, at least
                        // until we are sure this is totally clean.  I don't think performance
                        // is a big issue with this particular function, though I could be
                        // wrong.  Also, the stuff below clearly does more masking than
                        // it needs to do.

                        // Clear high 6 bits.
                        int highSurrogate = ((int) ch) & 0x03FF;

                        // Middle 4 bits (wwww) + 1
                        // "Note that the value of wwww from the high surrogate bit pattern
                        // is incremented to make the uuuuu bit pattern in the scalar value
                        // so the surrogate pair don't address the BMP."
                        int wwww = ((highSurrogate & 0x03C0) >> 6);
                        int uuuuu = wwww + 1;

                        // next 4 bits
                        int zzzz = (highSurrogate & 0x003C) >> 2;

                        // low 2 bits
                        int yyyyyy = ((highSurrogate & 0x0003) << 4) & 0x30;

                        // Get low surrogate character.
                        ch = chars[++i];

                        // Clear high 6 bits.
                        int lowSurrogate = ((int) ch) & 0x03FF;

                        // put the middle 4 bits into the bottom of yyyyyy (byte 3)
                        yyyyyy = yyyyyy | ((lowSurrogate & 0x03C0) >> 6);

                        // bottom 6 bits.
                        int xxxxxx = (lowSurrogate & 0x003F);

                        int byte1 = 0xF0 | (uuuuu >> 2); // top 3 bits of uuuuu
                        int byte2 =
                            0x80 | (((uuuuu & 0x03) << 4) & 0x30) | zzzz;
                        int byte3 = 0x80 | yyyyyy;
                        int byte4 = 0x80 | xxxxxx;

                        writer.write('%');
                        writer.write(makeHHString(byte1));
                        writer.write('%');
                        writer.write(makeHHString(byte2));
                        writer.write('%');
                        writer.write(makeHHString(byte3));
                        writer.write('%');
                        writer.write(makeHHString(byte4));
                    }
                    else
                    {
                        int high = (ch >> 12) | 0xE0; // top 4 bits
                        int middle = ((ch & 0x0FC0) >> 6) | 0x80;
                        // middle 6 bits
                        int low = (ch & 0x3F) | 0x80;
                        // First 6 bits, + high bit
                        writer.write('%');
                        writer.write(makeHHString(high));
                        writer.write('%');
                        writer.write(makeHHString(middle));
                        writer.write('%');
                        writer.write(makeHHString(low));
                    }

                }
                else if (escapingNotNeeded(ch))
                {
                    writer.write(ch);
                }
                else
                {
                    writer.write("&#");
                    writer.write(Integer.toString(ch));
                    writer.write(';');
                }
                // In this character range we have first written out any previously accumulated
                // "clean" characters, then processed the current more complicated character,
                // which may have incremented "i".
                // We now we reset the next possible clean character.
                cleanStart = i + 1;
            }
            // Since http://www.ietf.org/rfc/rfc2396.txt refers to the URI grammar as
            // not allowing quotes in the URI proper syntax, nor in the fragment
            // identifier, we believe that it's OK to double escape quotes.
            else if (ch == '"')
            {
                // If the character is a '%' number number, try to avoid double-escaping.
                // There is a question if this is legal behavior.

                // Dmitri Ilyin: to check if '%' number number is invalid. It must be checked if %xx is a sign, that would be encoded
                // The encoded signes are in Hex form. So %xx my be in form %3C that is "<" sign. I will try to change here a little.

                //        if( ((i+2) < len) && isASCIIDigit(stringArray[i+1]) && isASCIIDigit(stringArray[i+2]) )

                // We are no longer escaping '%'

                if (cleanLength > 0)
                {
                    writer.write(chars, cleanStart, cleanLength);
                    cleanLength = 0;
                }


                // Mike Kay encodes this as &#34;, so he may know something I don't?
                if (doURLEscaping)
                    writer.write("%22");
                else
                    writer.write("&quot;"); // we have to escape this, I guess.

                // We have written out any clean characters, then the escaped '%' and now we
                // We now we reset the next possible clean character.
                cleanStart = i + 1;
            }
            else if (ch == '&')
            {
                // HTML 4.01 reads, "Authors should use "&amp;" (ASCII decimal 38)
                // instead of "&" to avoid confusion with the beginning of a character
                // reference (entity reference open delimiter).
                if (cleanLength > 0)
                {
                    writer.write(chars, cleanStart, cleanLength);
                    cleanLength = 0;
                }
                writer.write("&amp;");
                cleanStart = i + 1;
            }
            else
            {
                // no processing for this character, just count how
                // many characters in a row that we have that need no processing
                cleanLength++;
            }
        }

        // are there any clean characters at the end of the array
        // that we haven't processed yet?
        if (cleanLength > 1)
        {
            // if the whole string can be written out as-is do so
            // otherwise write out the clean chars at the end of the
            // array
            if (cleanStart == 0)
                writer.write(string);
            else
                writer.write(chars, cleanStart, cleanLength);
        }
        else if (cleanLength == 1)
        {
            // a little optimization for 1 clean character
            // (we could have let the previous if(...) handle them all)
            writer.write(ch);
        }
    }

    /**
     * Writes the specified <var>string</var> after substituting <VAR>specials</VAR>,
     * and UTF-16 surrogates for character references <CODE>&amp;#xnn</CODE>.
     *
     * @param   string      String to convert to XML format.
     * @param   encoding    CURRENTLY NOT IMPLEMENTED.
     *
     * @throws org.xml.sax.SAXException
     */
    public void writeAttrString(
        final java.io.Writer writer, String string, String encoding)
        throws IOException, SAXException
    {
        final int end = string.length();
        if (end > m_attrBuff.length)
        {
            m_attrBuff = new char[end * 2 + 1];
        }
        string.getChars(0, end, m_attrBuff, 0);
        final char[] chars = m_attrBuff;



        int cleanStart = 0;
        int cleanLength = 0;

        char ch = 0;
        for (int i = 0; i < end; i++)
        {
            ch = chars[i];

            // System.out.println("SPECIALSSIZE: "+SPECIALSSIZE);
            // System.out.println("ch: "+(int)ch);
            // System.out.println("m_maxCharacter: "+(int)m_maxCharacter);
            // System.out.println("m_attrCharsMap[ch]: "+(int)m_attrCharsMap[ch]);
            if (escapingNotNeeded(ch) && (!m_charInfo.isSpecialAttrChar(ch)))
            {
                cleanLength++;
            }
            else if ('<' == ch || '>' == ch)
            {
                cleanLength++; // no escaping in this case, as specified in 15.2
            }
            else if (
                ('&' == ch) && ((i + 1) < end) && ('{' == chars[i + 1]))
            {
                cleanLength++; // no escaping in this case, as specified in 15.2
            }
            else
            {
                if (cleanLength > 0)
                {
                    writer.write(chars,cleanStart,cleanLength);
                    cleanLength = 0;
                }
                int pos = accumDefaultEntity(writer, ch, i, chars, end, false, true);

                if (i != pos)
                {
                    i = pos - 1;
                }
                else
                {
                    if (Encodings.isHighUTF16Surrogate(ch) ||
                            Encodings.isLowUTF16Surrogate(ch))
                    {
                        if (writeUTF16Surrogate(ch, chars, i, end) >= 0) {
                            // move the index if the low surrogate is consumed
                            // as writeUTF16Surrogate has written the pair
                            if (Encodings.isHighUTF16Surrogate(ch)) {
                                i++;
                            }
                        }
                    }
                    else
                    {
                        String outputStringForChar = m_charInfo.getOutputStringForChar(ch);
                        if (null != outputStringForChar)
                        {
                            writer.write(outputStringForChar);
                        }
                        else if (escapingNotNeeded(ch))
                        {
                            writer.write(ch); // no escaping in this case
                        }
                        else
                        {
                            writer.write("&#");
                            writer.write(Integer.toString(ch));
                            writer.write(';');
                        }
                    }
                }
                cleanStart = i + 1;
            }
        } // end of for()

        // are there any clean characters at the end of the array
        // that we haven't processed yet?
        if (cleanLength > 1)
        {
            // if the whole string can be written out as-is do so
            // otherwise write out the clean chars at the end of the
            // array
            if (cleanStart == 0)
                writer.write(string);
            else
                writer.write(chars, cleanStart, cleanLength);
        }
        else if (cleanLength == 1)
        {
            // a little optimization for 1 clean character
            // (we could have let the previous if(...) handle them all)
            writer.write(ch);
        }
    }



    /**
     * Receive notification of character data.
     *
     * <p>The Parser will call this method to report each chunk of
     * character data.  SAX parsers may return all contiguous character
     * data in a single chunk, or they may split it into several
     * chunks; however, all of the characters in any single event
     * must come from the same external entity, so that the Locator
     * provides useful information.</p>
     *
     * <p>The application must not attempt to read from the array
     * outside of the specified range.</p>
     *
     * <p>Note that some parsers will report whitespace using the
     * ignorableWhitespace() method rather than this one (validating
     * parsers must do so).</p>
     *
     * @param chars The characters from the XML document.
     * @param start The start position in the array.
     * @param length The number of characters to read from the array.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *            wrapping another exception.
     * @see #ignorableWhitespace
     * @see org.xml.sax.Locator
     *
     * @throws org.xml.sax.SAXException
     */
    public final void characters(char chars[], int start, int length)
        throws org.xml.sax.SAXException
    {

        if (m_elemContext.m_isRaw)
        {
            try
            {
                if (m_elemContext.m_startTagOpen)
                {
                    closeStartTag();
                    m_elemContext.m_startTagOpen = false;
                }

//              With m_ispreserve just set true it looks like shouldIndent()
//              will always return false, so drop any possible indentation.
//              if (shouldIndent())
//                  indent();

                // writer.write("<![CDATA[");
                // writer.write(chars, start, length);
                writeNormalizedChars(chars, start, length, false, m_lineSepUse);
                m_isprevtext = true;
                // writer.write("]]>");

                // time to generate characters event
                if (m_tracer != null)
                    super.fireCharEvent(chars, start, length);

                return;
            }
            catch (IOException ioe)
            {
                throw new org.xml.sax.SAXException(
                    Utils.messages.createMessage(
                        MsgKey.ER_OIERROR,
                        null),
                    ioe);
                //"IO error", ioe);
            }
        }
        else
        {
            super.characters(chars, start, length);
        }
    }

    /**
     *  Receive notification of cdata.
     *
     *  <p>The Parser will call this method to report each chunk of
     *  character data.  SAX parsers may return all contiguous character
     *  data in a single chunk, or they may split it into several
     *  chunks; however, all of the characters in any single event
     *  must come from the same external entity, so that the Locator
     *  provides useful information.</p>
     *
     *  <p>The application must not attempt to read from the array
     *  outside of the specified range.</p>
     *
     *  <p>Note that some parsers will report whitespace using the
     *  ignorableWhitespace() method rather than this one (validating
     *  parsers must do so).</p>
     *
     *  @param ch The characters from the XML document.
     *  @param start The start position in the array.
     *  @param length The number of characters to read from the array.
     *  @throws org.xml.sax.SAXException Any SAX exception, possibly
     *             wrapping another exception.
     *  @see #ignorableWhitespace
     *  @see org.xml.sax.Locator
     *
     * @throws org.xml.sax.SAXException
     */
    public final void cdata(char ch[], int start, int length)
        throws org.xml.sax.SAXException
    {
        if ((null != m_elemContext.m_elementName)
            && (m_elemContext.m_elementName.equalsIgnoreCase("SCRIPT")
                || m_elemContext.m_elementName.equalsIgnoreCase("STYLE")))
        {
            try
            {
                if (m_elemContext.m_startTagOpen)
                {
                    closeStartTag();
                    m_elemContext.m_startTagOpen = false;
                }

                if (shouldIndent())
                    indent();

                // writer.write(ch, start, length);
                writeNormalizedChars(ch, start, length, true, m_lineSepUse);
            }
            catch (IOException ioe)
            {
                throw new org.xml.sax.SAXException(
                    Utils.messages.createMessage(
                        MsgKey.ER_OIERROR,
                        null),
                    ioe);
                //"IO error", ioe);
            }
        }
        else
        {
            super.cdata(ch, start, length);
        }
    }

    /**
     *  Receive notification of a processing instruction.
     *
     *  @param target The processing instruction target.
     *  @param data The processing instruction data, or null if
     *         none was supplied.
     *  @throws org.xml.sax.SAXException Any SAX exception, possibly
     *             wrapping another exception.
     *
     * @throws org.xml.sax.SAXException
     */
    public void processingInstruction(String target, String data)
        throws org.xml.sax.SAXException
    {
        if (m_doIndent) {
            m_childNodeNum++;
            flushCharactersBuffer(false);
        }
        // Process any pending starDocument and startElement first.
        flushPending();

        // Use a fairly nasty hack to tell if the next node is supposed to be
        // unescaped text.
        if (target.equals(Result.PI_DISABLE_OUTPUT_ESCAPING))
        {
            startNonEscaping();
        }
        else if (target.equals(Result.PI_ENABLE_OUTPUT_ESCAPING))
        {
            endNonEscaping();
        }
        else
        {
            try
            {
            if (m_elemContext.m_startTagOpen)
            {
                closeStartTag();
                m_elemContext.m_startTagOpen = false;
            }
            else if (m_needToCallStartDocument)
                startDocumentInternal();

            if (shouldIndent())
                indent();

            final java.io.Writer writer = m_writer;
            //writer.write("<?" + target);
            writer.write("<?");
            writer.write(target);

            if (data.length() > 0 && !Character.isSpaceChar(data.charAt(0)))
                writer.write(' ');

            //writer.write(data + ">"); // different from XML
            writer.write(data); // different from XML
            writer.write('>'); // different from XML

            // Always output a newline char if not inside of an
            // element. The whitespace is not significant in that
            // case.
            if (m_elemContext.m_currentElemDepth <= 0)
                outputLineSep();

            m_startNewLine = true;
            }
            catch(IOException e)
            {
                throw new SAXException(e);
            }
        }

        // now generate the PI event
        if (m_tracer != null)
            super.fireEscapingEvent(target, data);
     }

    /**
     * Receive notivication of a entityReference.
     *
     * @param name non-null reference to entity name string.
     *
     * @throws org.xml.sax.SAXException
     */
    public final void entityReference(String name)
        throws org.xml.sax.SAXException
    {
        try
        {

        final java.io.Writer writer = m_writer;
        writer.write('&');
        writer.write(name);
        writer.write(';');

        } catch(IOException e)
        {
            throw new SAXException(e);
        }
    }
    /**
     * @see ExtendedContentHandler#endElement(String)
     */
    public final void endElement(String elemName) throws SAXException
    {
        endElement(null, null, elemName);
    }

    /**
     * Process the attributes, which means to write out the currently
     * collected attributes to the writer. The attributes are not
     * cleared by this method
     *
     * @param writer the writer to write processed attributes to.
     * @param nAttrs the number of attributes in m_attributes
     * to be processed
     *
     * @throws org.xml.sax.SAXException
     */
    public void processAttributes(java.io.Writer writer, int nAttrs)
        throws IOException,SAXException
    {
            /*
             * process the collected attributes
             */
            for (int i = 0; i < nAttrs; i++)
            {
                processAttribute(
                    writer,
                    m_attributes.getQName(i),
                    m_attributes.getValue(i),
                    m_elemContext.m_elementDesc);
            }
    }

    /**
     * For the enclosing elements starting tag write out out any attributes
     * followed by ">"
     *
     *@throws org.xml.sax.SAXException
     */
    protected void closeStartTag() throws SAXException
    {
            try
            {

            // finish processing attributes, time to fire off the start element event
            if (m_tracer != null)
                super.fireStartElem(m_elemContext.m_elementName);

            int nAttrs = m_attributes.getLength();
            if (nAttrs>0)
            {
                processAttributes(m_writer, nAttrs);
                // clear attributes object for re-use with next element
                m_attributes.clear();
            }

            m_writer.write('>');

            /* whether Xalan or XSLTC, we have the prefix mappings now, so
             * lets determine if the current element is specified in the cdata-
             * section-elements list.
             */
            if (m_StringOfCDATASections != null)
                m_elemContext.m_isCdataSection = isCdataSection();

            }
            catch(IOException e)
            {
                throw new SAXException(e);
            }
    }

        /**
         * This method is used when a prefix/uri namespace mapping
         * is indicated after the element was started with a
         * startElement() and before and endElement().
         * startPrefixMapping(prefix,uri) would be used before the
         * startElement() call.
         * @param uri the URI of the namespace
         * @param prefix the prefix associated with the given URI.
         *
         * @see ExtendedContentHandler#namespaceAfterStartElement(String, String)
         */
        public void namespaceAfterStartElement(String prefix, String uri)
            throws SAXException
        {
            // hack for XSLTC with finding URI for default namespace
            if (m_elemContext.m_elementURI == null)
            {
                String prefix1 = getPrefixPart(m_elemContext.m_elementName);
                if (prefix1 == null && EMPTYSTRING.equals(prefix))
                {
                    // the elements URI is not known yet, and it
                    // doesn't have a prefix, and we are currently
                    // setting the uri for prefix "", so we have
                    // the uri for the element... lets remember it
                    m_elemContext.m_elementURI = uri;
                }
            }
            startPrefixMapping(prefix,uri,false);
        }

    public void startDTD(String name, String publicId, String systemId)
        throws SAXException
    {
        m_inDTD = true;
        super.startDTD(name, publicId, systemId);
    }

    /**
     * Report the end of DTD declarations.
     * @throws org.xml.sax.SAXException The application may raise an exception.
     * @see #startDTD
     */
    public void endDTD() throws org.xml.sax.SAXException
    {
        m_inDTD = false;
        /* for ToHTMLStream the DOCTYPE is entirely output in the
         * startDocumentInternal() method, so don't do anything here
         */
    }
    /**
     * This method does nothing.
     */
    public void attributeDecl(
        String eName,
        String aName,
        String type,
        String valueDefault,
        String value)
        throws SAXException
    {
        // The internal DTD subset is not serialized by the ToHTMLStream serializer
    }

    /**
     * This method does nothing.
     */
    public void elementDecl(String name, String model) throws SAXException
    {
        // The internal DTD subset is not serialized by the ToHTMLStream serializer
    }
    /**
     * This method does nothing.
     */
    public void internalEntityDecl(String name, String value)
        throws SAXException
    {
        // The internal DTD subset is not serialized by the ToHTMLStream serializer
    }
    /**
     * This method does nothing.
     */
    public void externalEntityDecl(
        String name,
        String publicId,
        String systemId)
        throws SAXException
    {
        // The internal DTD subset is not serialized by the ToHTMLStream serializer
    }

    /**
     * This method is used to add an attribute to the currently open element.
     * The caller has guaranted that this attribute is unique, which means that it
     * not been seen before and will not be seen again.
     *
     * @param name the qualified name of the attribute
     * @param value the value of the attribute which can contain only
     * ASCII printable characters characters in the range 32 to 127 inclusive.
     * @param flags the bit values of this integer give optimization information.
     */
    public void addUniqueAttribute(String name, String value, int flags)
        throws SAXException
    {
        try
        {
            final java.io.Writer writer = m_writer;
            if ((flags & NO_BAD_CHARS) > 0 && m_htmlcharInfo.onlyQuotAmpLtGt)
            {
                // "flags" has indicated that the characters
                // '>'  '<'   '&'  and '"' are not in the value and
                // m_htmlcharInfo has recorded that there are no other
                // entities in the range 0 to 127 so we write out the
                // value directly
                writer.write(' ');
                writer.write(name);
                writer.write("=\"");
                writer.write(value);
                writer.write('"');
            }
            else if (
                (flags & HTML_ATTREMPTY) > 0
                    && (value.length() == 0 || value.equalsIgnoreCase(name)))
            {
                writer.write(' ');
                writer.write(name);
            }
            else
            {
                writer.write(' ');
                writer.write(name);
                writer.write("=\"");
                if ((flags & HTML_ATTRURL) > 0)
                {
                    writeAttrURI(writer, value, m_specialEscapeURLs);
                }
                else
                {
                    writeAttrString(writer, value, this.getEncoding());
                }
                writer.write('"');
            }
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void comment(char ch[], int start, int length)
            throws SAXException
    {
        // The internal DTD subset is not serialized by the ToHTMLStream serializer
        if (m_inDTD)
            return;
        super.comment(ch, start, length);
    }

    public boolean reset()
    {
        boolean ret = super.reset();
        if (!ret)
            return false;
        initToHTMLStream();
        return true;
    }

    private void initToHTMLStream()
    {
        m_isprevblock = false;
        m_inDTD = false;
        m_omitMetaTag = false;
        m_specialEscapeURLs = true;
    }

    static class Trie
    {
        /**
         * A digital search trie for 7-bit ASCII text
         * The API is a subset of java.util.Hashtable
         * The key must be a 7-bit ASCII string
         * The value may be any Java Object
         * One can get an object stored in a trie from its key,
         * but the search is either case sensitive or case
         * insensitive to the characters in the key, and this
         * choice of sensitivity or insensitivity is made when
         * the Trie is created, before any objects are put in it.
         *
         * This class is a copy of the one in com.sun.org.apache.xml.internal.utils.
         * It exists to cut the serializers dependancy on that package.
         *
         * @xsl.usage internal
         */

        /** Size of the m_nextChar array.  */
        public static final int ALPHA_SIZE = 128;

        /** The root node of the tree.    */
        final Node m_Root;

        /** helper buffer to convert Strings to char arrays */
        private char[] m_charBuffer = new char[0];

        /** true if the search for an object is lower case only with the key */
        private final boolean m_lowerCaseOnly;

        /**
         * Construct the trie that has a case insensitive search.
         */
        public Trie()
        {
            m_Root = new Node();
            m_lowerCaseOnly = false;
        }

        /**
         * Construct the trie given the desired case sensitivity with the key.
         * @param lowerCaseOnly true if the search keys are to be loser case only,
         * not case insensitive.
         */
        public Trie(boolean lowerCaseOnly)
        {
            m_Root = new Node();
            m_lowerCaseOnly = lowerCaseOnly;
        }

        /**
         * Put an object into the trie for lookup.
         *
         * @param key must be a 7-bit ASCII string
         * @param value any java object.
         *
         * @return The old object that matched key, or null.
         */
        public Object put(String key, Object value)
        {

            final int len = key.length();
            if (len > m_charBuffer.length)
            {
                // make the biggest buffer ever needed in get(String)
                m_charBuffer = new char[len];
            }

            Node node = m_Root;

            for (int i = 0; i < len; i++)
            {
                Node nextNode =
                    node.m_nextChar[Character.toLowerCase(key.charAt(i))];

                if (nextNode != null)
                {
                    node = nextNode;
                }
                else
                {
                    for (; i < len; i++)
                    {
                        Node newNode = new Node();
                        if (m_lowerCaseOnly)
                        {
                            // put this value into the tree only with a lower case key
                            node.m_nextChar[Character.toLowerCase(
                                key.charAt(i))] =
                                newNode;
                        }
                        else
                        {
                            // put this value into the tree with a case insensitive key
                            node.m_nextChar[Character.toUpperCase(
                                key.charAt(i))] =
                                newNode;
                            node.m_nextChar[Character.toLowerCase(
                                key.charAt(i))] =
                                newNode;
                        }
                        node = newNode;
                    }
                    break;
                }
            }

            Object ret = node.m_Value;

            node.m_Value = value;

            return ret;
        }

        /**
         * Get an object that matches the key.
         *
         * @param key must be a 7-bit ASCII string
         *
         * @return The object that matches the key, or null.
         */
        public Object get(final String key)
        {

            final int len = key.length();

            /* If the name is too long, we won't find it, this also keeps us
             * from overflowing m_charBuffer
             */
            if (m_charBuffer.length < len)
                return null;

            Node node = m_Root;
            switch (len) // optimize the look up based on the number of chars
            {
                // case 0 looks silly, but the generated bytecode runs
                // faster for lookup of elements of length 2 with this in
                // and a fair bit faster.  Don't know why.
                case 0 :
                    {
                        return null;
                    }

                case 1 :
                    {
                        final char ch = key.charAt(0);
                        if (ch < ALPHA_SIZE)
                        {
                            node = node.m_nextChar[ch];
                            if (node != null)
                                return node.m_Value;
                        }
                        return null;
                    }
                    //                comment out case 2 because the default is faster
                    //                case 2 :
                    //                    {
                    //                        final char ch0 = key.charAt(0);
                    //                        final char ch1 = key.charAt(1);
                    //                        if (ch0 < ALPHA_SIZE && ch1 < ALPHA_SIZE)
                    //                        {
                    //                            node = node.m_nextChar[ch0];
                    //                            if (node != null)
                    //                            {
                    //
                    //                                if (ch1 < ALPHA_SIZE)
                    //                                {
                    //                                    node = node.m_nextChar[ch1];
                    //                                    if (node != null)
                    //                                        return node.m_Value;
                    //                                }
                    //                            }
                    //                        }
                    //                        return null;
                    //                   }
                default :
                    {
                        for (int i = 0; i < len; i++)
                        {
                            // A thread-safe way to loop over the characters
                            final char ch = key.charAt(i);
                            if (ALPHA_SIZE <= ch)
                            {
                                // the key is not 7-bit ASCII so we won't find it here
                                return null;
                            }

                            node = node.m_nextChar[ch];
                            if (node == null)
                                return null;
                        }

                        return node.m_Value;
                    }
            }
        }

        /**
         * The node representation for the trie.
         * @xsl.usage internal
         */
        private class Node
        {

            /**
             * Constructor, creates a Node[ALPHA_SIZE].
             */
            Node()
            {
                m_nextChar = new Node[ALPHA_SIZE];
                m_Value = null;
            }

            /** The next nodes.   */
            final Node m_nextChar[];

            /** The value.   */
            Object m_Value;
        }
        /**
         * Construct the trie from another Trie.
         * Both the existing Trie and this new one share the same table for
         * lookup, and it is assumed that the table is fully populated and
         * not changing anymore.
         *
         * @param existingTrie the Trie that this one is a copy of.
         */
        public Trie(Trie existingTrie)
        {
            // copy some fields from the existing Trie into this one.
            m_Root = existingTrie.m_Root;
            m_lowerCaseOnly = existingTrie.m_lowerCaseOnly;

            // get a buffer just big enough to hold the longest key in the table.
            int max = existingTrie.getLongestKeyLength();
            m_charBuffer = new char[max];
        }

        /**
         * Get an object that matches the key.
         * This method is faster than get(), but is not thread-safe.
         *
         * @param key must be a 7-bit ASCII string
         *
         * @return The object that matches the key, or null.
         */
        public Object get2(final String key)
        {

            final int len = key.length();

            /* If the name is too long, we won't find it, this also keeps us
             * from overflowing m_charBuffer
             */
            if (m_charBuffer.length < len)
                return null;

            Node node = m_Root;
            switch (len) // optimize the look up based on the number of chars
            {
                // case 0 looks silly, but the generated bytecode runs
                // faster for lookup of elements of length 2 with this in
                // and a fair bit faster.  Don't know why.
                case 0 :
                    {
                        return null;
                    }

                case 1 :
                    {
                        final char ch = key.charAt(0);
                        if (ch < ALPHA_SIZE)
                        {
                            node = node.m_nextChar[ch];
                            if (node != null)
                                return node.m_Value;
                        }
                        return null;
                    }
                default :
                    {
                        /* Copy string into array. This is not thread-safe because
                         * it modifies the contents of m_charBuffer. If multiple
                         * threads were to use this Trie they all would be
                         * using this same array (not good). So this
                         * method is not thread-safe, but it is faster because
                         * converting to a char[] and looping over elements of
                         * the array is faster than a String's charAt(i).
                         */
                        key.getChars(0, len, m_charBuffer, 0);

                        for (int i = 0; i < len; i++)
                        {
                            final char ch = m_charBuffer[i];
                            if (ALPHA_SIZE <= ch)
                            {
                                // the key is not 7-bit ASCII so we won't find it here
                                return null;
                            }

                            node = node.m_nextChar[ch];
                            if (node == null)
                                return null;
                        }

                        return node.m_Value;
                    }
            }
        }

        /**
         * Get the length of the longest key used in the table.
         */
        public int getLongestKeyLength()
        {
            return m_charBuffer.length;
        }
    }
}
