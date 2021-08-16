/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.util.xml.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import jdk.internal.org.xml.sax.InputSource;
import jdk.internal.org.xml.sax.SAXException;

/**
 * XML non-validating parser engine.
 */
public abstract class Parser {

    public static final String FAULT = "";
    protected static final int BUFFSIZE_READER = 512;
    protected static final int BUFFSIZE_PARSER = 128;
    /**
     * The end of stream character.
     */
    public static final char EOS = 0xffff;
    private Pair mNoNS; // there is no namespace
    private Pair mXml;  // the xml namespace
    private Map<String, Input> mEnt;  // the entities look up table
    private Map<String, Input> mPEnt; // the parmeter entities look up table
    protected boolean mIsSAlone;     // xml decl standalone flag
    protected boolean mIsSAloneSet;  // standalone is explicitely set
    protected boolean mIsNSAware;    // if true - namespace aware mode
    protected int mPh;  // current phase of document processing
    protected static final int PH_BEFORE_DOC = -1;  // before parsing
    protected static final int PH_DOC_START = 0;   // document start
    protected static final int PH_MISC_DTD = 1;   // misc before DTD
    protected static final int PH_DTD = 2;   // DTD
    protected static final int PH_DTD_MISC = 3;   // misc after DTD
    protected static final int PH_DOCELM = 4;   // document's element
    protected static final int PH_DOCELM_MISC = 5;   // misc after element
    protected static final int PH_AFTER_DOC = 6;   // after parsing
    protected int mEvt;  // current event type
    protected static final int EV_NULL = 0;   // unknown
    protected static final int EV_ELM = 1;   // empty element
    protected static final int EV_ELMS = 2;   // start element
    protected static final int EV_ELME = 3;   // end element
    protected static final int EV_TEXT = 4;   // textual content
    protected static final int EV_WSPC = 5;   // white space content
    protected static final int EV_PI = 6;   // processing instruction
    protected static final int EV_CDAT = 7;   // character data
    protected static final int EV_COMM = 8;   // comment
    protected static final int EV_DTD = 9;   // document type definition
    protected static final int EV_ENT = 10;  // skipped entity
    private char mESt; // built-in entity recognizer state
    // mESt values:
    //   0x100   : the initial state
    //   > 0x100 : unrecognized name
    //   < 0x100 : replacement character
    protected char[] mBuff;       // parser buffer
    protected int mBuffIdx;    // index of the last char
    protected Pair mPref;       // stack of prefixes
    protected Pair mElm;        // stack of elements
    // mAttL.chars - element qname
    // mAttL.next  - next element
    // mAttL.list  - list of attributes defined on this element
    // mAttL.list.chars - attribute qname
    // mAttL.list.id    - a char representing attribute's type see below
    // mAttL.list.next  - next attribute defined on the element
    // mAttL.list.list  - devault value structure or null
    // mAttL.list.list.chars - "name='value' " chars array for Input
    //
    // Attribute type character values:
    // 'i' - "ID"
    // 'r' - "IDREF"
    // 'R' - "IDREFS"
    // 'n' - "ENTITY"
    // 'N' - "ENTITIES"
    // 't' - "NMTOKEN"
    // 'T' - "NMTOKENS"
    // 'u' - enumeration type
    // 'o' - "NOTATION"
    // 'c' - "CDATA"
    // see also: bkeyword() and atype()
    //
    protected Pair mAttL;       // list of defined attrs by element name
    protected Input mDoc;        // document entity
    protected Input mInp;        // stack of entities
    private char[] mChars;      // reading buffer
    private int mChLen;      // current capacity
    private int mChIdx;      // index to the next char
    protected Attrs mAttrs;      // attributes of the curr. element
    private String[] mItems;      // attributes array of the curr. element
    private char mAttrIdx;    // attributes counter/index
    private String mUnent;  // unresolved entity name
    private Pair mDltd;   // deleted objects for reuse
    /**
     * Default prefixes
     */
    private static final char NONS[];
    private static final char XML[];
    private static final char XMLNS[];

    static {
        NONS = new char[1];
        NONS[0] = (char) 0;

        XML = new char[4];
        XML[0] = (char) 4;
        XML[1] = 'x';
        XML[2] = 'm';
        XML[3] = 'l';

        XMLNS = new char[6];
        XMLNS[0] = (char) 6;
        XMLNS[1] = 'x';
        XMLNS[2] = 'm';
        XMLNS[3] = 'l';
        XMLNS[4] = 'n';
        XMLNS[5] = 's';
    }
    /**
     * ASCII character type array.
     *
     * This array maps an ASCII (7 bit) character to the character type.<br>
     * Possible character type values are:<br> - ' ' for any kind of white
     * space character;<br> - 'a' for any lower case alphabetical character
     * value;<br> - 'A' for any upper case alphabetical character value;<br>
     * - 'd' for any decimal digit character value;<br> - 'z' for any
     * character less than ' ' except '\t', '\n', '\r';<br> An ASCII (7 bit)
     * character which does not fall in any category listed above is mapped to
     * it self.
     */
    private static final byte asctyp[];
    /**
     * NMTOKEN character type array.
     *
     * This array maps an ASCII (7 bit) character to the character type.<br>
     * Possible character type values are:<br> - 0 for underscore ('_') or any
     * lower and upper case alphabetical character value;<br> - 1 for colon
     * (':') character;<br> - 2 for dash ('-') and dot ('.') or any decimal
     * digit character value;<br> - 3 for any kind of white space character<br>
     * An ASCII (7 bit) character which does not fall in any category listed
     * above is mapped to 0xff.
     */
    private static final byte nmttyp[];

    /**
     * Static constructor.
     *
     * Sets up the ASCII character type array which is used by
     * {@link #asctyp asctyp} method and NMTOKEN character type array.
     */
    static {
        short i = 0;

        asctyp = new byte[0x80];
        while (i < ' ') {
            asctyp[i++] = (byte) 'z';
        }
        asctyp['\t'] = (byte) ' ';
        asctyp['\r'] = (byte) ' ';
        asctyp['\n'] = (byte) ' ';
        while (i < '0') {
            asctyp[i] = (byte) i++;
        }
        while (i <= '9') {
            asctyp[i++] = (byte) 'd';
        }
        while (i < 'A') {
            asctyp[i] = (byte) i++;
        }
        while (i <= 'Z') {
            asctyp[i++] = (byte) 'A';
        }
        while (i < 'a') {
            asctyp[i] = (byte) i++;
        }
        while (i <= 'z') {
            asctyp[i++] = (byte) 'a';
        }
        while (i < 0x80) {
            asctyp[i] = (byte) i++;
        }

        nmttyp = new byte[0x80];
        for (i = 0; i < '0'; i++) {
            nmttyp[i] = (byte) 0xff;
        }
        while (i <= '9') {
            nmttyp[i++] = (byte) 2;  // digits
        }
        while (i < 'A') {
            nmttyp[i++] = (byte) 0xff;
        }
        // skiped upper case alphabetical character are already 0
        for (i = '['; i < 'a'; i++) {
            nmttyp[i] = (byte) 0xff;
        }
        // skiped lower case alphabetical character are already 0
        for (i = '{'; i < 0x80; i++) {
            nmttyp[i] = (byte) 0xff;
        }
        nmttyp['_'] = 0;
        nmttyp[':'] = 1;
        nmttyp['.'] = 2;
        nmttyp['-'] = 2;
        nmttyp[' '] = 3;
        nmttyp['\t'] = 3;
        nmttyp['\r'] = 3;
        nmttyp['\n'] = 3;
    }

    /**
     * Constructor.
     */
    protected Parser() {
        mPh = PH_BEFORE_DOC;  // before parsing

        //              Initialize the parser
        mBuff = new char[BUFFSIZE_PARSER];
        mAttrs = new Attrs();

        //              Default namespace
        mPref = pair(mPref);
        mPref.name = "";
        mPref.value = "";
        mPref.chars = NONS;
        mNoNS = mPref;  // no namespace
        //              XML namespace
        mPref = pair(mPref);
        mPref.name = "xml";
        mPref.value = "http://www.w3.org/XML/1998/namespace";
        mPref.chars = XML;
        mXml = mPref;  // XML namespace
    }

    /**
     * Initializes parser's internals. Note, current input has to be set before
     * this method is called.
     */
    protected void init() {
        mUnent = null;
        mElm = null;
        mPref = mXml;
        mAttL = null;
        mPEnt = new HashMap<>();
        mEnt = new HashMap<>();
        mDoc = mInp;          // current input is document entity
        mChars = mInp.chars;    // use document entity buffer
        mPh = PH_DOC_START;  // the begining of the document
    }

    /**
     * Cleans up parser internal resources.
     */
    protected void cleanup() {
        //              Default attributes
        while (mAttL != null) {
            while (mAttL.list != null) {
                if (mAttL.list.list != null) {
                    del(mAttL.list.list);
                }
                mAttL.list = del(mAttL.list);
            }
            mAttL = del(mAttL);
        }
        //              Element stack
        while (mElm != null) {
            mElm = del(mElm);
        }
        //              Namespace prefixes
        while (mPref != mXml) {
            mPref = del(mPref);
        }
        //              Inputs
        while (mInp != null) {
            pop();
        }
        //              Document reader
        if ((mDoc != null) && (mDoc.src != null)) {
            try {
                mDoc.src.close();
            } catch (IOException ioe) {
            }
        }
        mPEnt = null;
        mEnt = null;
        mDoc = null;
        mPh = PH_AFTER_DOC;  // before documnet processing
    }

    /**
     * Processes a portion of document. This method returns one of EV_*
     * constants as an identifier of the portion of document have been read.
     *
     * @return Identifier of processed document portion.
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    @SuppressWarnings("fallthrough")
    protected int step() throws Exception {
        mEvt = EV_NULL;
        int st = 0;
        while (mEvt == EV_NULL) {
            char ch = (mChIdx < mChLen) ? mChars[mChIdx++] : getch();
            switch (st) {
                case 0:     // all sorts of markup (dispetcher)
                    if (ch != '<') {
                        bkch();
                        mBuffIdx = -1;  // clean parser buffer
                        st = 1;
                        break;
                    }
                    switch (getch()) {
                        case '/':  // the end of the element content
                            mEvt = EV_ELME;
                            if (mElm == null) {
                                panic(FAULT);
                            }
                            //          Check element's open/close tags balance
                            mBuffIdx = -1;  // clean parser buffer
                            bname(mIsNSAware);
                            char[] chars = mElm.chars;
                            if (chars.length == (mBuffIdx + 1)) {
                                for (char i = 1; i <= mBuffIdx; i += 1) {
                                    if (chars[i] != mBuff[i]) {
                                        panic(FAULT);
                                    }
                                }
                            } else {
                                panic(FAULT);
                            }
                            //          Skip white spaces before '>'
                            if (wsskip() != '>') {
                                panic(FAULT);
                            }
                            getch();  // read '>'
                            break;

                        case '!':  // a comment or a CDATA
                            ch = getch();
                            bkch();
                            switch (ch) {
                                case '-':  // must be a comment
                                    mEvt = EV_COMM;
                                    comm();
                                    break;

                                case '[':  // must be a CDATA section
                                    mEvt = EV_CDAT;
                                    cdat();
                                    break;

                                default:   // must be 'DOCTYPE'
                                    mEvt = EV_DTD;
                                    dtd();
                                    break;
                            }
                            break;

                        case '?':  // processing instruction
                            mEvt = EV_PI;
                            pi();
                            break;

                        default:  // must be the first char of an xml name
                            bkch();
                            //          Read an element name and put it on top of the
                            //          element stack
                            mElm = pair(mElm);  // add new element to the stack
                            mElm.chars = qname(mIsNSAware);
                            mElm.name = mElm.local();
                            mElm.id = (mElm.next != null) ? mElm.next.id : 0;  // flags
                            mElm.num = 0;     // namespace counter
                            //          Find the list of defined attributs of the current
                            //          element
                            Pair elm = find(mAttL, mElm.chars);
                            mElm.list = (elm != null) ? elm.list : null;
                            //          Read attributes till the end of the element tag
                            mAttrIdx = 0;
                            Pair att = pair(null);
                            att.num = 0;  // clear attribute's flags
                            attr(att);     // get all attributes inc. defaults
                            del(att);
                            mElm.value = (mIsNSAware) ? rslv(mElm.chars) : null;
                            //          Skip white spaces before '>'
                            switch (wsskip()) {
                                case '>':
                                    getch();  // read '>'
                                    mEvt = EV_ELMS;
                                    break;

                                case '/':
                                    getch();  // read '/'
                                    if (getch() != '>') // read '>'
                                    {
                                        panic(FAULT);
                                    }
                                    mEvt = EV_ELM;
                                    break;

                                default:
                                    panic(FAULT);
                            }
                            break;
                    }
                    break;

                case 1:     // read white space
                    switch (ch) {
                        case ' ':
                        case '\t':
                        case '\n':
                            bappend(ch);
                            break;

                        case '\r':              // EOL processing [#2.11]
                            if (getch() != '\n') {
                                bkch();
                            }
                            bappend('\n');
                            break;

                        case '<':
                            mEvt = EV_WSPC;
                            bkch();
                            bflash_ws();
                            break;

                        default:
                            bkch();
                            st = 2;
                            break;
                    }
                    break;

                case 2:     // read the text content of the element
                    switch (ch) {
                        case '&':
                            if (mUnent == null) {
                                //              There was no unresolved entity on previous step.
                                if ((mUnent = ent('x')) != null) {
                                    mEvt = EV_TEXT;
                                    bkch();      // move back to ';' after entity name
                                    setch('&');  // parser must be back on next step
                                    bflash();
                                }
                            } else {
                                //              There was unresolved entity on previous step.
                                mEvt = EV_ENT;
                                skippedEnt(mUnent);
                                mUnent = null;
                            }
                            break;

                        case '<':
                            mEvt = EV_TEXT;
                            bkch();
                            bflash();
                            break;

                        case '\r':  // EOL processing [#2.11]
                            if (getch() != '\n') {
                                bkch();
                            }
                            bappend('\n');
                            break;

                        case EOS:
                            panic(FAULT);

                        default:
                            bappend(ch);
                            break;
                    }
                    break;

                default:
                    panic(FAULT);
            }
        }

        return mEvt;
    }

    /**
     * Parses the document type declaration.
     *
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    private void dtd() throws Exception {
        char ch;
        String str = null;
        String name = null;
        Pair psid = null;
        // read 'DOCTYPE'
        if ("DOCTYPE".equals(name(false)) != true) {
            panic(FAULT);
        }
        mPh = PH_DTD;  // DTD
        for (short st = 0; st >= 0;) {
            ch = getch();
            switch (st) {
                case 0:     // read the document type name
                    if (chtyp(ch) != ' ') {
                        bkch();
                        name = name(mIsNSAware);
                        wsskip();
                        st = 1;  // read 'PUPLIC' or 'SYSTEM'
                    }
                    break;

                case 1:     // read 'PUPLIC' or 'SYSTEM'
                    switch (chtyp(ch)) {
                        case 'A':
                            bkch();
                            psid = pubsys(' ');
                            st = 2;  // skip spaces before internal subset
                            docType(name, psid.name, psid.value);
                            break;

                        case '[':
                            bkch();
                            st = 2;    // skip spaces before internal subset
                            docType(name, null, null);
                            break;

                        case '>':
                            bkch();
                            st = 3;    // skip spaces after internal subset
                            docType(name, null, null);
                            break;

                        default:
                            panic(FAULT);
                    }
                    break;

                case 2:     // skip spaces before internal subset
                    switch (chtyp(ch)) {
                        case '[':
                            //          Process internal subset
                            dtdsub();
                            st = 3;  // skip spaces after internal subset
                            break;

                        case '>':
                            //          There is no internal subset
                            bkch();
                            st = 3;  // skip spaces after internal subset
                            break;

                        case ' ':
                            // skip white spaces
                            break;

                        default:
                            panic(FAULT);
                    }
                    break;

                case 3:     // skip spaces after internal subset
                    switch (chtyp(ch)) {
                        case '>':
                            if (psid != null) {
                                //              Report the DTD external subset
                                InputSource is = resolveEnt(name, psid.name, psid.value);
                                if (is != null) {
                                    if (mIsSAlone == false) {
                                        //              Set the end of DTD external subset char
                                        bkch();
                                        setch(']');
                                        //              Set the DTD external subset InputSource
                                        push(new Input(BUFFSIZE_READER));
                                        setinp(is);
                                        mInp.pubid = psid.name;
                                        mInp.sysid = psid.value;
                                        //              Parse the DTD external subset
                                        dtdsub();
                                    } else {
                                        //              Unresolved DTD external subset
                                        skippedEnt("[dtd]");
                                        //              Release reader and stream
                                        if (is.getCharacterStream() != null) {
                                            try {
                                                is.getCharacterStream().close();
                                            } catch (IOException ioe) {
                                            }
                                        }
                                        if (is.getByteStream() != null) {
                                            try {
                                                is.getByteStream().close();
                                            } catch (IOException ioe) {
                                            }
                                        }
                                    }
                                } else {
                                    //          Unresolved DTD external subset
                                    skippedEnt("[dtd]");
                                }
                                del(psid);
                            }
                            st = -1;  // end of DTD
                            break;

                        case ' ':
                            // skip white spaces
                            break;

                        default:
                            panic(FAULT);
                    }
                    break;

                default:
                    panic(FAULT);
            }
        }
    }

    /**
     * Parses the document type declaration subset.
     *
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    private void dtdsub() throws Exception {
        startInternalSub(); // reports the event before parsing the subset

        char ch;
        for (short st = 0; st >= 0;) {
            ch = getch();
            switch (st) {
                case 0:     // skip white spaces before a declaration
                    switch (chtyp(ch)) {
                        case '<':
                            ch = getch();
                            switch (ch) {
                                case '?':
                                    pi();
                                    break;

                                case '!':
                                    ch = getch();
                                    bkch();
                                    if (ch == '-') {
                                        comm();
                                        break;
                                    }
                                    //          A markup or an entity declaration
                                    bntok();
                                    switch (bkeyword()) {
                                        case 'n':
                                            dtdent();
                                            break;

                                        case 'a':
                                            dtdattl();    // parse attributes declaration
                                            break;

                                        case 'e':
                                            dtdelm();     // parse element declaration
                                            break;

                                        case 'o':
                                            dtdnot();     // parse notation declaration
                                            break;

                                        default:
                                            panic(FAULT); // unsupported markup declaration
                                            break;
                                    }
                                    st = 1;  // read the end of declaration
                                    break;

                                default:
                                    panic(FAULT);
                                    break;
                            }
                            break;

                        case '%':
                            //          A parameter entity reference
                            pent(' ');
                            break;

                        case ']':
                            //          End of DTD subset
                            st = -1;
                            break;

                        case ' ':
                            //          Skip white spaces
                            break;

                        case 'Z':
                            //          End of stream
                            if (getch() != ']') {
                                panic(FAULT);
                            }
                            st = -1;
                            break;

                        default:
                            panic(FAULT);
                    }
                    break;

                case 1:     // read the end of declaration
                    switch (ch) {
                        case '>':   // there is no notation
                            st = 0; // skip white spaces before a declaration
                            break;

                        case ' ':
                        case '\n':
                        case '\r':
                        case '\t':
                            //          Skip white spaces
                            break;

                        default:
                            panic(FAULT);
                            break;
                    }
                    break;

                default:
                    panic(FAULT);
            }
        }
    }

    /**
     * Parses an entity declaration. This method fills the general (
     * <code>mEnt</code>) and parameter
     * (
     * <code>mPEnt</code>) entity look up table.
     *
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    @SuppressWarnings("fallthrough")
    private void dtdent() throws Exception {
        String str = null;
        char[] val = null;
        Input inp = null;
        Pair ids = null;
        char ch;
        for (short st = 0; st >= 0;) {
            ch = getch();
            switch (st) {
                case 0:     // skip white spaces before entity name
                    switch (chtyp(ch)) {
                        case ' ':
                            //          Skip white spaces
                            break;

                        case '%':
                            //          Parameter entity or parameter entity declaration.
                            ch = getch();
                            bkch();
                            if (chtyp(ch) == ' ') {
                                //              Parameter entity declaration.
                                wsskip();
                                str = name(false);
                                switch (chtyp(wsskip())) {
                                    case 'A':
                                        //              Read the external identifier
                                        ids = pubsys(' ');
                                        if (wsskip() == '>') {
                                            //          External parsed entity
                                            if (mPEnt.containsKey(str) == false) {      // [#4.2]
                                                inp = new Input();
                                                inp.pubid = ids.name;
                                                inp.sysid = ids.value;
                                                mPEnt.put(str, inp);
                                            }
                                        } else {
                                            panic(FAULT);
                                        }
                                        del(ids);
                                        st = -1;  // the end of declaration
                                        break;

                                    case '\"':
                                    case '\'':
                                        //              Read the parameter entity value
                                        bqstr('d');
                                        //              Create the parameter entity value
                                        val = new char[mBuffIdx + 1];
                                        System.arraycopy(mBuff, 1, val, 1, val.length - 1);
                                        //              Add surrounding spaces [#4.4.8]
                                        val[0] = ' ';
                                        //              Add the entity to the entity look up table
                                        if (mPEnt.containsKey(str) == false) {  // [#4.2]
                                            inp = new Input(val);
                                            inp.pubid = mInp.pubid;
                                            inp.sysid = mInp.sysid;
                                            inp.xmlenc = mInp.xmlenc;
                                            inp.xmlver = mInp.xmlver;
                                            mPEnt.put(str, inp);
                                        }
                                        st = -1;  // the end of declaration
                                        break;

                                    default:
                                        panic(FAULT);
                                        break;
                                }
                            } else {
                                //              Parameter entity reference.
                                pent(' ');
                            }
                            break;

                        default:
                            bkch();
                            str = name(false);
                            st = 1;  // read entity declaration value
                            break;
                    }
                    break;

                case 1:     // read entity declaration value
                    switch (chtyp(ch)) {
                        case '\"':  // internal entity
                        case '\'':
                            bkch();
                            bqstr('d');  // read a string into the buffer
                            if (mEnt.get(str) == null) {
                                //              Create general entity value
                                val = new char[mBuffIdx];
                                System.arraycopy(mBuff, 1, val, 0, val.length);
                                //              Add the entity to the entity look up table
                                if (mEnt.containsKey(str) == false) {   // [#4.2]
                                    inp = new Input(val);
                                    inp.pubid = mInp.pubid;
                                    inp.sysid = mInp.sysid;
                                    inp.xmlenc = mInp.xmlenc;
                                    inp.xmlver = mInp.xmlver;
                                    mEnt.put(str, inp);
                                }
                            }
                            st = -1;  // the end of declaration
                            break;

                        case 'A':  // external entity
                            bkch();
                            ids = pubsys(' ');
                            switch (wsskip()) {
                                case '>':  // external parsed entity
                                    if (mEnt.containsKey(str) == false) {  // [#4.2]
                                        inp = new Input();
                                        inp.pubid = ids.name;
                                        inp.sysid = ids.value;
                                        mEnt.put(str, inp);
                                    }
                                    break;

                                case 'N':  // external general unparsed entity
                                    if ("NDATA".equals(name(false)) == true) {
                                        wsskip();
                                        unparsedEntDecl(str, ids.name, ids.value, name(false));
                                        break;
                                    }
                                default:
                                    panic(FAULT);
                                    break;
                            }
                            del(ids);
                            st = -1;  // the end of declaration
                            break;

                        case ' ':
                            //          Skip white spaces
                            break;

                        default:
                            panic(FAULT);
                            break;
                    }
                    break;

                default:
                    panic(FAULT);
            }
        }
    }

    /**
     * Parses an element declaration.
     *
     * This method parses the declaration up to the closing angle bracket.
     *
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    @SuppressWarnings("fallthrough")
    private void dtdelm() throws Exception {
        //              This is stub implementation which skips an element
        //              declaration.
        wsskip();
        name(mIsNSAware);

        char ch;
        while (true) {
            ch = getch();
            switch (ch) {
                case '>':
                    bkch();
                    return;

                case EOS:
                    panic(FAULT);

                default:
                    break;
            }
        }
    }

    /**
     * Parses an attribute list declaration.
     *
     * This method parses the declaration up to the closing angle bracket.
     *
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    private void dtdattl() throws Exception {
        char elmqn[] = null;
        Pair elm = null;
        char ch;
        for (short st = 0; st >= 0;) {
            ch = getch();
            switch (st) {
                case 0:     // read the element name
                    switch (chtyp(ch)) {
                        case 'a':
                        case 'A':
                        case '_':
                        case 'X':
                        case ':':
                            bkch();
                            //          Get the element from the list or add a new one.
                            elmqn = qname(mIsNSAware);
                            elm = find(mAttL, elmqn);
                            if (elm == null) {
                                elm = pair(mAttL);
                                elm.chars = elmqn;
                                mAttL = elm;
                            }
                            st = 1;  // read an attribute declaration
                            break;

                        case ' ':
                            break;

                        case '%':
                            pent(' ');
                            break;

                        default:
                            panic(FAULT);
                            break;
                    }
                    break;

                case 1:     // read an attribute declaration
                    switch (chtyp(ch)) {
                        case 'a':
                        case 'A':
                        case '_':
                        case 'X':
                        case ':':
                            bkch();
                            dtdatt(elm);
                            if (wsskip() == '>') {
                                return;
                            }
                            break;

                        case ' ':
                            break;

                        case '%':
                            pent(' ');
                            break;

                        default:
                            panic(FAULT);
                            break;
                    }
                    break;

                default:
                    panic(FAULT);
                    break;
            }
        }
    }

    /**
     * Parses an attribute declaration.
     *
     * The attribute uses the following fields of Pair object: chars - characters
     * of qualified name id - the type identifier of the attribute list - a pair
     * which holds the default value (chars field)
     *
     * @param elm An object which represents all defined attributes on an
     * element.
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    @SuppressWarnings("fallthrough")
    private void dtdatt(Pair elm) throws Exception {
        char attqn[] = null;
        Pair att = null;
        char ch;
        for (short st = 0; st >= 0;) {
            ch = getch();
            switch (st) {
                case 0:     // the attribute name
                    switch (chtyp(ch)) {
                        case 'a':
                        case 'A':
                        case '_':
                        case 'X':
                        case ':':
                            bkch();
                            //          Get the attribute from the list or add a new one.
                            attqn = qname(mIsNSAware);
                            att = find(elm.list, attqn);
                            if (att == null) {
                                //              New attribute declaration
                                att = pair(elm.list);
                                att.chars = attqn;
                                elm.list = att;
                            } else {
                                //              Do not override the attribute declaration [#3.3]
                                att = pair(null);
                                att.chars = attqn;
                                att.id = 'c';
                            }
                            wsskip();
                            st = 1;
                            break;

                        case '%':
                            pent(' ');
                            break;

                        case ' ':
                            break;

                        default:
                            panic(FAULT);
                            break;
                    }
                    break;

                case 1:     // the attribute type
                    switch (chtyp(ch)) {
                        case '(':
                            att.id = 'u';  // enumeration type
                            st = 2;        // read the first element of the list
                            break;

                        case '%':
                            pent(' ');
                            break;

                        case ' ':
                            break;

                        default:
                            bkch();
                            bntok();  // read type id
                            att.id = bkeyword();
                            switch (att.id) {
                                case 'o':   // NOTATION
                                    if (wsskip() != '(') {
                                        panic(FAULT);
                                    }
                                    ch = getch();
                                    st = 2;  // read the first element of the list
                                    break;

                                case 'i':     // ID
                                case 'r':     // IDREF
                                case 'R':     // IDREFS
                                case 'n':     // ENTITY
                                case 'N':     // ENTITIES
                                case 't':     // NMTOKEN
                                case 'T':     // NMTOKENS
                                case 'c':     // CDATA
                                    wsskip();
                                    st = 4;  // read default declaration
                                    break;

                                default:
                                    panic(FAULT);
                                    break;
                            }
                            break;
                    }
                    break;

                case 2:     // read the first element of the list
                    switch (chtyp(ch)) {
                        case 'a':
                        case 'A':
                        case 'd':
                        case '.':
                        case ':':
                        case '-':
                        case '_':
                        case 'X':
                            bkch();
                            switch (att.id) {
                                case 'u':  // enumeration type
                                    bntok();
                                    break;

                                case 'o':  // NOTATION
                                    mBuffIdx = -1;
                                    bname(false);
                                    break;

                                default:
                                    panic(FAULT);
                                    break;
                            }
                            wsskip();
                            st = 3;  // read next element of the list
                            break;

                        case '%':
                            pent(' ');
                            break;

                        case ' ':
                            break;

                        default:
                            panic(FAULT);
                            break;
                    }
                    break;

                case 3:     // read next element of the list
                    switch (ch) {
                        case ')':
                            wsskip();
                            st = 4;  // read default declaration
                            break;

                        case '|':
                            wsskip();
                            switch (att.id) {
                                case 'u':  // enumeration type
                                    bntok();
                                    break;

                                case 'o':  // NOTATION
                                    mBuffIdx = -1;
                                    bname(false);
                                    break;

                                default:
                                    panic(FAULT);
                                    break;
                            }
                            wsskip();
                            break;

                        case '%':
                            pent(' ');
                            break;

                        default:
                            panic(FAULT);
                            break;
                    }
                    break;

                case 4:     // read default declaration
                    switch (ch) {
                        case '#':
                            bntok();
                            switch (bkeyword()) {
                                case 'F':  // FIXED
                                    switch (wsskip()) {
                                        case '\"':
                                        case '\'':
                                            st = 5;  // read the default value
                                            break;

                                        case EOS:
                                            panic(FAULT);

                                        default:
                                            st = -1;
                                            break;
                                    }
                                    break;

                                case 'Q':  // REQUIRED
                                case 'I':  // IMPLIED
                                    st = -1;
                                    break;

                                default:
                                    panic(FAULT);
                                    break;
                            }
                            break;

                        case '\"':
                        case '\'':
                            bkch();
                            st = 5;  // read the default value
                            break;

                        case ' ':
                        case '\n':
                        case '\r':
                        case '\t':
                            break;

                        case '%':
                            pent(' ');
                            break;

                        default:
                            bkch();
                            st = -1;
                            break;
                    }
                    break;

                case 5:     // read the default value
                    switch (ch) {
                        case '\"':
                        case '\'':
                            bkch();
                            bqstr('d');  // the value in the mBuff now
                            att.list = pair(null);
                            //          Create a string like "attqname='value' "
                            att.list.chars = new char[att.chars.length + mBuffIdx + 3];
                            System.arraycopy(
                                    att.chars, 1, att.list.chars, 0, att.chars.length - 1);
                            att.list.chars[att.chars.length - 1] = '=';
                            att.list.chars[att.chars.length] = ch;
                            System.arraycopy(
                                    mBuff, 1, att.list.chars, att.chars.length + 1, mBuffIdx);
                            att.list.chars[att.chars.length + mBuffIdx + 1] = ch;
                            att.list.chars[att.chars.length + mBuffIdx + 2] = ' ';
                            st = -1;
                            break;

                        default:
                            panic(FAULT);
                            break;
                    }
                    break;

                default:
                    panic(FAULT);
                    break;
            }
        }
    }

    /**
     * Parses a notation declaration.
     *
     * This method parses the declaration up to the closing angle bracket.
     *
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    private void dtdnot() throws Exception {
        wsskip();
        String name = name(false);
        wsskip();
        Pair ids = pubsys('N');
        notDecl(name, ids.name, ids.value);
        del(ids);
    }

    /**
     * Parses an attribute.
     *
     * This recursive method is responsible for prefix addition
     * (
     * <code>mPref</code>) on the way down. The element's start tag end triggers
     * the return process. The method then on it's way back resolves prefixes
     * and accumulates attributes.
     *
     * <p><code>att.num</code> carries attribute flags where: 0x1 - attribute is
     * declared in DTD (attribute decalration had been read); 0x2 - attribute's
     * default value is used.</p>
     *
     * @param att An object which reprecents current attribute.
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    @SuppressWarnings("fallthrough")
    private void attr(Pair att) throws Exception {
        switch (wsskip()) {
            case '/':
            case '>':
                if ((att.num & 0x2) == 0) {  // all attributes have been read
                    att.num |= 0x2;  // set default attribute flag
                    Input inp = mInp;
                    //          Go through all attributes defined on current element.
                    for (Pair def = mElm.list; def != null; def = def.next) {
                        if (def.list == null) // no default value
                        {
                            continue;
                        }
                        //              Go through all attributes defined on current
                        //              element and add defaults.
                        Pair act = find(att.next, def.chars);
                        if (act == null) {
                            push(new Input(def.list.chars));
                        }
                    }
                    if (mInp != inp) {  // defaults have been added
                        attr(att);
                        return;
                    }
                }
                //              Ensure the attribute string array capacity
                mAttrs.setLength(mAttrIdx);
                mItems = mAttrs.mItems;
                return;

            case EOS:
                panic(FAULT);

            default:
                //              Read the attribute name and value
                att.chars = qname(mIsNSAware);
                att.name = att.local();
                String type = atype(att);  // sets attribute's type on att.id
                wsskip();
                if (getch() != '=') {
                    panic(FAULT);
                }
                bqstr((char) att.id);   // read the value with normalization.
                String val = new String(mBuff, 1, mBuffIdx);
                Pair next = pair(att);
                next.num = (att.num & ~0x1);  // inherit attribute flags
                //              Put a namespace declaration on top of the prefix stack
                if ((mIsNSAware == false) || (isdecl(att, val) == false)) {
                    //          An ordinary attribute
                    mAttrIdx++;
                    attr(next);     // recursive call to parse the next attribute
                    mAttrIdx--;
                    //          Add the attribute to the attributes string array
                    char idx = (char) (mAttrIdx << 3);
                    mItems[idx + 1] = att.qname();  // attr qname
                    mItems[idx + 2] = (mIsNSAware) ? att.name : ""; // attr local name
                    mItems[idx + 3] = val;          // attr value
                    mItems[idx + 4] = type;         // attr type
                    switch (att.num & 0x3) {
                        case 0x0:
                            mItems[idx + 5] = null;
                            break;

                        case 0x1:  // declared attribute
                            mItems[idx + 5] = "d";
                            break;

                        default:  // 0x2, 0x3 - default attribute always declared
                            mItems[idx + 5] = "D";
                            break;
                    }
                    //          Resolve the prefix if any and report the attribute
                    //          NOTE: The attribute does not accept the default namespace.
                    mItems[idx + 0] = (att.chars[0] != 0) ? rslv(att.chars) : "";
                } else {
                    //          A namespace declaration. mPref.name contains prefix and
                    //          mPref.value contains namespace URI set by isdecl method.
                    //          Report a start of the new mapping
                    newPrefix();
                    //          Recursive call to parse the next attribute
                    attr(next);
                    //          NOTE: The namespace declaration is not reported.
                }
                del(next);
                break;
        }
    }

    /**
     * Retrieves attribute type.
     *
     * This method sets the type of normalization in the attribute
     * <code>id</code> field and returns the name of attribute type.
     *
     * @param att An object which represents current attribute.
     * @return The name of the attribute type.
     * @exception Exception is parser specific exception form panic method.
     */
    private String atype(Pair att)
            throws Exception {
        Pair attr;

        // CDATA-type normalization by default [#3.3.3]
        att.id = 'c';
        if (mElm.list == null || (attr = find(mElm.list, att.chars)) == null) {
            return "CDATA";
        }

        att.num |= 0x1;  // attribute is declared

        // Non-CDATA normalization except when the attribute type is CDATA.
        att.id = 'i';
        switch (attr.id) {
            case 'i':
                return "ID";

            case 'r':
                return "IDREF";

            case 'R':
                return "IDREFS";

            case 'n':
                return "ENTITY";

            case 'N':
                return "ENTITIES";

            case 't':
                return "NMTOKEN";

            case 'T':
                return "NMTOKENS";

            case 'u':
                return "NMTOKEN";

            case 'o':
                return "NOTATION";

            case 'c':
                att.id = 'c';
                return "CDATA";

            default:
                panic(FAULT);
        }
        return null;
    }

    /**
     * Parses a comment.
     *
     * The &apos;&lt;!&apos; part is read in dispatcher so the method starts
     * with first &apos;-&apos; after &apos;&lt;!&apos;.
     *
     * @exception Exception is parser specific exception form panic method.
     */
    @SuppressWarnings("fallthrough")
    private void comm() throws Exception {
        if (mPh == PH_DOC_START) {
            mPh = PH_MISC_DTD;  // misc before DTD
        }               // '<!' has been already read by dispetcher.
        char ch;
        mBuffIdx = -1;
        for (short st = 0; st >= 0;) {
            ch = (mChIdx < mChLen) ? mChars[mChIdx++] : getch();
            if (ch == EOS) {
                panic(FAULT);
            }
            switch (st) {
                case 0:     // first '-' of the comment open
                    if (ch == '-') {
                        st = 1;
                    } else {
                        panic(FAULT);
                    }
                    break;

                case 1:     // secind '-' of the comment open
                    if (ch == '-') {
                        st = 2;
                    } else {
                        panic(FAULT);
                    }
                    break;

                case 2:     // skip the comment body
                    switch (ch) {
                        case '-':
                            st = 3;
                            break;

                        default:
                            bappend(ch);
                            break;
                    }
                    break;

                case 3:     // second '-' of the comment close
                    switch (ch) {
                        case '-':
                            st = 4;
                            break;

                        default:
                            bappend('-');
                            bappend(ch);
                            st = 2;
                            break;
                    }
                    break;

                case 4:     // '>' of the comment close
                    if (ch == '>') {
                        comm(mBuff, mBuffIdx + 1);
                        st = -1;
                        break;
                    }
                // else - panic [#2.5 compatibility note]

                default:
                    panic(FAULT);
            }
        }
    }

    /**
     * Parses a processing instruction.
     *
     * The &apos;&lt;?&apos; is read in dispatcher so the method starts with
     * first character of PI target name after &apos;&lt;?&apos;.
     *
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    private void pi() throws Exception {
        // '<?' has been already read by dispetcher.
        char ch;
        String str = null;
        mBuffIdx = -1;
        for (short st = 0; st >= 0;) {
            ch = getch();
            if (ch == EOS) {
                panic(FAULT);
            }
            switch (st) {
                case 0:     // read the PI target name
                    switch (chtyp(ch)) {
                        case 'a':
                        case 'A':
                        case '_':
                        case ':':
                        case 'X':
                            bkch();
                            str = name(false);
                            //          PI target name may not be empty string [#2.6]
                            //          PI target name 'XML' is reserved [#2.6]
                            if ((str.isEmpty())
                                    || (mXml.name.equals(str.toLowerCase()) == true)) {
                                panic(FAULT);
                            }
                            //          This is processing instruction
                            if (mPh == PH_DOC_START) // the begining of the document
                            {
                                mPh = PH_MISC_DTD;    // misc before DTD
                            }
                            wsskip();  // skip spaces after the PI target name
                            st = 1;    // accumulate the PI body
                            mBuffIdx = -1;
                            break;

                        default:
                            panic(FAULT);
                    }
                    break;

                case 1:     // accumulate the PI body
                    switch (ch) {
                        case '?':
                            st = 2;  // end of the PI body
                            break;

                        default:
                            bappend(ch);
                            break;
                    }
                    break;

                case 2:     // end of the PI body
                    switch (ch) {
                        case '>':
                            //          PI has been read.
                            pi(str, new String(mBuff, 0, mBuffIdx + 1));
                            st = -1;
                            break;

                        case '?':
                            bappend('?');
                            break;

                        default:
                            bappend('?');
                            bappend(ch);
                            st = 1;  // accumulate the PI body
                            break;
                    }
                    break;

                default:
                    panic(FAULT);
            }
        }
    }

    /**
     * Parses a character data.
     *
     * The &apos;&lt;!&apos; part is read in dispatcher so the method starts
     * with first &apos;[&apos; after &apos;&lt;!&apos;.
     *
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    private void cdat()
            throws Exception {
        // '<!' has been already read by dispetcher.
        char ch;
        mBuffIdx = -1;
        for (short st = 0; st >= 0;) {
            ch = getch();
            switch (st) {
                case 0:     // the first '[' of the CDATA open
                    if (ch == '[') {
                        st = 1;
                    } else {
                        panic(FAULT);
                    }
                    break;

                case 1:     // read "CDATA"
                    if (chtyp(ch) == 'A') {
                        bappend(ch);
                    } else {
                        if ("CDATA".equals(
                                new String(mBuff, 0, mBuffIdx + 1)) != true) {
                            panic(FAULT);
                        }
                        bkch();
                        st = 2;
                    }
                    break;

                case 2:     // the second '[' of the CDATA open
                    if (ch != '[') {
                        panic(FAULT);
                    }
                    mBuffIdx = -1;
                    st = 3;
                    break;

                case 3:     // read data before the first ']'
                    if (ch != ']') {
                        bappend(ch);
                    } else {
                        st = 4;
                    }
                    break;

                case 4:     // read the second ']' or continue to read the data
                    if (ch != ']') {
                        bappend(']');
                        bappend(ch);
                        st = 3;
                    } else {
                        st = 5;
                    }
                    break;

                case 5:     // read '>' or continue to read the data
                    switch (ch) {
                        case ']':
                            bappend(']');
                            break;

                        case '>':
                            bflash();
                            st = -1;
                            break;

                        default:
                            bappend(']');
                            bappend(']');
                            bappend(ch);
                            st = 3;
                            break;
                    }
                    break;

                default:
                    panic(FAULT);
            }
        }
    }

    /**
     * Reads a xml name.
     *
     * The xml name must conform "Namespaces in XML" specification. Therefore
     * the ':' character is not allowed in the name. This method should be used
     * for PI and entity names which may not have a namespace according to the
     * specification mentioned above.
     *
     * @param ns The true value turns namespace conformance on.
     * @return The name has been read.
     * @exception Exception When incorrect character appear in the name.
     * @exception IOException
     */
    protected String name(boolean ns)
            throws Exception {
        mBuffIdx = -1;
        bname(ns);
        return new String(mBuff, 1, mBuffIdx);
    }

    /**
     * Reads a qualified xml name.
     *
     * The characters of a qualified name is an array of characters. The first
     * (chars[0]) character is the index of the colon character which separates
     * the prefix from the local name. If the index is zero, the name does not
     * contain separator or the parser works in the namespace unaware mode. The
     * length of qualified name is the length of the array minus one.
     *
     * @param ns The true value turns namespace conformance on.
     * @return The characters of a qualified name.
     * @exception Exception When incorrect character appear in the name.
     * @exception IOException
     */
    protected char[] qname(boolean ns)
            throws Exception {
        mBuffIdx = -1;
        bname(ns);
        char chars[] = new char[mBuffIdx + 1];
        System.arraycopy(mBuff, 0, chars, 0, mBuffIdx + 1);
        return chars;
    }

    /**
     * Reads the public or/and system identifiers.
     *
     * @param inp The input object.
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    private void pubsys(Input inp)
            throws Exception {
        Pair pair = pubsys(' ');
        inp.pubid = pair.name;
        inp.sysid = pair.value;
        del(pair);
    }

    /**
     * Reads the public or/and system identifiers.
     *
     * @param flag The 'N' allows public id be without system id.
     * @return The public or/and system identifiers pair.
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    @SuppressWarnings("fallthrough")
    private Pair pubsys(char flag) throws Exception {
        Pair ids = pair(null);
        String str = name(false);
        if ("PUBLIC".equals(str) == true) {
            bqstr('i');  // non-CDATA normalization [#4.2.2]
            ids.name = new String(mBuff, 1, mBuffIdx);
            switch (wsskip()) {
                case '\"':
                case '\'':
                    bqstr(' ');
                    ids.value = new String(mBuff, 1, mBuffIdx);
                    break;

                case EOS:
                    panic(FAULT);

                default:
                    if (flag != 'N') // [#4.7]
                    {
                        panic(FAULT);
                    }
                    ids.value = null;
                    break;
            }
            return ids;
        } else if ("SYSTEM".equals(str) == true) {
            ids.name = null;
            bqstr(' ');
            ids.value = new String(mBuff, 1, mBuffIdx);
            return ids;
        }
        panic(FAULT);
        return null;
    }

    /**
     * Reads an attribute value.
     *
     * The grammar this method can read is:
     * <pre>{@code
     * eqstr := S "=" qstr
     * qstr  := S ("'" string "'") | ('"' string '"')
     * }</pre>
     * This method resolves entities
     * inside a string unless the parser parses DTD.
     *
     * @param flag The '=' character forces the method to accept the '='
     * character before quoted string and read the following string as not an
     * attribute ('-'), 'c' - CDATA, 'i' - non CDATA, ' ' - no normalization;
     * '-' - not an attribute value; 'd' - in DTD context.
     * @return The content of the quoted strign as a string.
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    protected String eqstr(char flag) throws Exception {
        if (flag == '=') {
            wsskip();
            if (getch() != '=') {
                panic(FAULT);
            }
        }
        bqstr((flag == '=') ? '-' : flag);
        return new String(mBuff, 1, mBuffIdx);
    }

    /**
     * Resoves an entity.
     *
     * This method resolves built-in and character entity references. It is also
     * reports external entities to the application.
     *
     * @param flag The 'x' character forces the method to report a skipped
     * entity; 'i' character - indicates non-CDATA normalization.
     * @return Name of unresolved entity or <code>null</code> if entity had been
     * resolved successfully.
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    @SuppressWarnings("fallthrough")
    private String ent(char flag) throws Exception {
        char ch;
        int idx = mBuffIdx + 1;
        Input inp = null;
        String str = null;
        mESt = 0x100;  // reset the built-in entity recognizer
        bappend('&');
        for (short st = 0; st >= 0;) {
            ch = (mChIdx < mChLen) ? mChars[mChIdx++] : getch();
            switch (st) {
                case 0:     // the first character of the entity name
                case 1:     // read built-in entity name
                    switch (chtyp(ch)) {
                        case 'd':
                        case '.':
                        case '-':
                            if (st != 1) {
                                panic(FAULT);
                            }
                        case 'a':
                        case 'A':
                        case '_':
                        case 'X':
                            bappend(ch);
                            eappend(ch);
                            st = 1;
                            break;

                        case ':':
                            if (mIsNSAware != false) {
                                panic(FAULT);
                            }
                            bappend(ch);
                            eappend(ch);
                            st = 1;
                            break;

                        case ';':
                            if (mESt < 0x100) {
                                //              The entity is a built-in entity
                                mBuffIdx = idx - 1;
                                bappend(mESt);
                                st = -1;
                                break;
                            } else if (mPh == PH_DTD) {
                                //              In DTD entity declaration has to resolve character
                                //              entities and include "as is" others. [#4.4.7]
                                bappend(';');
                                st = -1;
                                break;
                            }
                            //          Convert an entity name to a string
                            str = new String(mBuff, idx + 1, mBuffIdx - idx);
                            inp = mEnt.get(str);
                            //          Restore the buffer offset
                            mBuffIdx = idx - 1;
                            if (inp != null) {
                                if (inp.chars == null) {
                                    //          External entity
                                    InputSource is = resolveEnt(str, inp.pubid, inp.sysid);
                                    if (is != null) {
                                        push(new Input(BUFFSIZE_READER));
                                        setinp(is);
                                        mInp.pubid = inp.pubid;
                                        mInp.sysid = inp.sysid;
                                        str = null;  // the entity is resolved
                                    } else {
                                        //              Unresolved external entity
                                        if (flag != 'x') {
                                            panic(FAULT);  // unknown entity within marckup
                                        }                                                               //              str is name of unresolved entity
                                    }
                                } else {
                                    //          Internal entity
                                    push(inp);
                                    str = null;  // the entity is resolved
                                }
                            } else {
                                //              Unknown or general unparsed entity
                                if (flag != 'x') {
                                    panic(FAULT);  // unknown entity within marckup
                                }                                               //              str is name of unresolved entity
                            }
                            st = -1;
                            break;

                        case '#':
                            if (st != 0) {
                                panic(FAULT);
                            }
                            st = 2;
                            break;

                        default:
                            panic(FAULT);
                    }
                    break;

                case 2:     // read character entity
                    switch (chtyp(ch)) {
                        case 'd':
                            bappend(ch);
                            break;

                        case ';':
                            //          Convert the character entity to a character
                            try {
                                int i = Integer.parseInt(
                                        new String(mBuff, idx + 1, mBuffIdx - idx), 10);
                                if (i >= 0xffff) {
                                    panic(FAULT);
                                }
                                ch = (char) i;
                            } catch (NumberFormatException nfe) {
                                panic(FAULT);
                            }
                            //          Restore the buffer offset
                            mBuffIdx = idx - 1;
                            if (ch == ' ' || mInp.next != null) {
                                bappend(ch, flag);
                            } else {
                                bappend(ch);
                            }
                            st = -1;
                            break;

                        case 'a':
                            //          If the entity buffer is empty and ch == 'x'
                            if ((mBuffIdx == idx) && (ch == 'x')) {
                                st = 3;
                                break;
                            }
                        default:
                            panic(FAULT);
                    }
                    break;

                case 3:     // read hex character entity
                    switch (chtyp(ch)) {
                        case 'A':
                        case 'a':
                        case 'd':
                            bappend(ch);
                            break;

                        case ';':
                            //          Convert the character entity to a character
                            try {
                                int i = Integer.parseInt(
                                        new String(mBuff, idx + 1, mBuffIdx - idx), 16);
                                if (i >= 0xffff) {
                                    panic(FAULT);
                                }
                                ch = (char) i;
                            } catch (NumberFormatException nfe) {
                                panic(FAULT);
                            }
                            //          Restore the buffer offset
                            mBuffIdx = idx - 1;
                            if (ch == ' ' || mInp.next != null) {
                                bappend(ch, flag);
                            } else {
                                bappend(ch);
                            }
                            st = -1;
                            break;

                        default:
                            panic(FAULT);
                    }
                    break;

                default:
                    panic(FAULT);
            }
        }

        return str;
    }

    /**
     * Resoves a parameter entity.
     *
     * This method resolves a parameter entity references. It is also reports
     * external entities to the application.
     *
     * @param flag The '-' instruct the method to do not set up surrounding
     * spaces [#4.4.8].
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    @SuppressWarnings("fallthrough")
    private void pent(char flag) throws Exception {
        char ch;
        int idx = mBuffIdx + 1;
        Input inp = null;
        String str = null;
        bappend('%');
        if (mPh != PH_DTD) // the DTD internal subset
        {
            return;         // Not Recognized [#4.4.1]
        }               //              Read entity name
        bname(false);
        str = new String(mBuff, idx + 2, mBuffIdx - idx - 1);
        if (getch() != ';') {
            panic(FAULT);
        }
        inp = mPEnt.get(str);
        //              Restore the buffer offset
        mBuffIdx = idx - 1;
        if (inp != null) {
            if (inp.chars == null) {
                //              External parameter entity
                InputSource is = resolveEnt(str, inp.pubid, inp.sysid);
                if (is != null) {
                    if (flag != '-') {
                        bappend(' ');  // tail space
                    }
                    push(new Input(BUFFSIZE_READER));
                    // BUG: there is no leading space! [#4.4.8]
                    setinp(is);
                    mInp.pubid = inp.pubid;
                    mInp.sysid = inp.sysid;
                } else {
                    //          Unresolved external parameter entity
                    skippedEnt("%" + str);
                }
            } else {
                //              Internal parameter entity
                if (flag == '-') {
                    //          No surrounding spaces
                    inp.chIdx = 1;
                } else {
                    //          Insert surrounding spaces
                    bappend(' ');  // tail space
                    inp.chIdx = 0;
                }
                push(inp);
            }
        } else {
            //          Unknown parameter entity
            skippedEnt("%" + str);
        }
    }

    /**
     * Recognizes and handles a namespace declaration.
     *
     * This method identifies a type of namespace declaration if any and puts
     * new mapping on top of prefix stack.
     *
     * @param name The attribute qualified name (<code>name.value</code> is a
     * <code>String</code> object which represents the attribute prefix).
     * @param value The attribute value.
     * @return <code>true</code> if a namespace declaration is recognized.
     */
    private boolean isdecl(Pair name, String value) {
        if (name.chars[0] == 0) {
            if ("xmlns".equals(name.name) == true) {
                //              New default namespace declaration
                mPref = pair(mPref);
                mPref.list = mElm;  // prefix owner element
                mPref.value = value;
                mPref.name = "";
                mPref.chars = NONS;
                mElm.num++;  // namespace counter
                return true;
            }
        } else {
            if (name.eqpref(XMLNS) == true) {
                //              New prefix declaration
                int len = name.name.length();
                mPref = pair(mPref);
                mPref.list = mElm;  // prefix owner element
                mPref.value = value;
                mPref.name = name.name;
                mPref.chars = new char[len + 1];
                mPref.chars[0] = (char) (len + 1);
                name.name.getChars(0, len, mPref.chars, 1);
                mElm.num++;  // namespace counter
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves a prefix.
     *
     * @return The namespace assigned to the prefix.
     * @exception Exception When mapping for specified prefix is not found.
     */
    private String rslv(char[] qname)
            throws Exception {
        for (Pair pref = mPref; pref != null; pref = pref.next) {
            if (pref.eqpref(qname) == true) {
                return pref.value;
            }
        }
        if (qname[0] == 1) {  // QNames like ':local'
            for (Pair pref = mPref; pref != null; pref = pref.next) {
                if (pref.chars[0] == 0) {
                    return pref.value;
                }
            }
        }
        panic(FAULT);
        return null;
    }

    /**
     * Skips xml white space characters.
     *
     * This method skips white space characters (' ', '\t', '\n', '\r') and
     * looks ahead not white space character.
     *
     * @return The first not white space look ahead character.
     * @exception IOException
     */
    protected char wsskip()
            throws IOException {
        char ch;
        while (true) {
            //          Read next character
            ch = (mChIdx < mChLen) ? mChars[mChIdx++] : getch();
            if (ch < 0x80) {
                if (nmttyp[ch] != 3) // [ \t\n\r]
                {
                    break;
                }
            } else {
                break;
            }
        }
        mChIdx--;  // bkch();
        return ch;
    }

    /**
     * Reports document type.
     *
     * @param name The name of the entity.
     * @param pubid The public identifier of the entity or <code>null</code>.
     * @param sysid The system identifier of the entity or <code>null</code>.
     */
    protected abstract void docType(String name, String pubid, String sysid)
            throws SAXException;

    /**
     * Reports the start of DTD internal subset.
     *
     * @throws SAXException if the receiver throws SAXException
     */
    public abstract void startInternalSub () throws SAXException;

    /**
     * Reports a comment.
     *
     * @param text The comment text starting from first charcater.
     * @param length The number of characters in comment.
     */
    protected abstract void comm(char[] text, int length);

    /**
     * Reports a processing instruction.
     *
     * @param target The processing instruction target name.
     * @param body The processing instruction body text.
     */
    protected abstract void pi(String target, String body)
            throws Exception;

    /**
     * Reports new namespace prefix. The Namespace prefix (
     * <code>mPref.name</code>) being declared and the Namespace URI (
     * <code>mPref.value</code>) the prefix is mapped to. An empty string is
     * used for the default element namespace, which has no prefix.
     */
    protected abstract void newPrefix()
            throws Exception;

    /**
     * Reports skipped entity name.
     *
     * @param name The entity name.
     */
    protected abstract void skippedEnt(String name)
            throws Exception;

    /**
     * Returns an
     * <code>InputSource</code> for specified entity or
     * <code>null</code>.
     *
     * @param name The name of the entity.
     * @param pubid The public identifier of the entity.
     * @param sysid The system identifier of the entity.
     */
    protected abstract InputSource resolveEnt(
            String name, String pubid, String sysid)
            throws Exception;

    /**
     * Reports notation declaration.
     *
     * @param name The notation's name.
     * @param pubid The notation's public identifier, or null if none was given.
     * @param sysid The notation's system identifier, or null if none was given.
     */
    protected abstract void notDecl(String name, String pubid, String sysid)
            throws Exception;

    /**
     * Reports unparsed entity name.
     *
     * @param name The unparsed entity's name.
     * @param pubid The entity's public identifier, or null if none was given.
     * @param sysid The entity's system identifier.
     * @param notation The name of the associated notation.
     */
    protected abstract void unparsedEntDecl(
            String name, String pubid, String sysid, String notation)
            throws Exception;

    /**
     * Notifies the handler about fatal parsing error.
     *
     * @param msg The problem description message.
     */
    protected abstract void panic(String msg)
            throws Exception;

    /**
     * Reads a qualified xml name.
     *
     * This is low level routine which leaves a qName in the buffer. The
     * characters of a qualified name is an array of characters. The first
     * (chars[0]) character is the index of the colon character which separates
     * the prefix from the local name. If the index is zero, the name does not
     * contain separator or the parser works in the namespace unaware mode. The
     * length of qualified name is the length of the array minus one.
     *
     * @param ns The true value turns namespace conformance on.
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    private void bname(boolean ns)
            throws Exception {
        char ch;
        char type;
        mBuffIdx++;  // allocate a char for colon offset
        int bqname = mBuffIdx;
        int bcolon = bqname;
        int bchidx = bqname + 1;
        int bstart = bchidx;
        int cstart = mChIdx;
        short st = (short) ((ns == true) ? 0 : 2);
        while (true) {
            //          Read next character
            if (mChIdx >= mChLen) {
                bcopy(cstart, bstart);
                getch();
                mChIdx--;  // bkch();
                cstart = mChIdx;
                bstart = bchidx;
            }
            ch = mChars[mChIdx++];
            type = (char) 0;  // [X]
            if (ch < 0x80) {
                type = (char) nmttyp[ch];
            } else if (ch == EOS) {
                panic(FAULT);
            }
            //          Parse QName
            switch (st) {
                case 0:     // read the first char of the prefix
                case 2:     // read the first char of the suffix
                    switch (type) {
                        case 0:  // [aA_X]
                            bchidx++;  // append char to the buffer
                            st++;      // (st == 0)? 1: 3;
                            break;

                        case 1:  // [:]
                            mChIdx--;  // bkch();
                            st++;      // (st == 0)? 1: 3;
                            break;

                        default:
                            panic(FAULT);
                    }
                    break;

                case 1:     // read the prefix
                case 3:     // read the suffix
                    switch (type) {
                        case 0:  // [aA_X]
                        case 2:  // [.-d]
                            bchidx++;  // append char to the buffer
                            break;

                        case 1:  // [:]
                            bchidx++;  // append char to the buffer
                            if (ns == true) {
                                if (bcolon != bqname) {
                                    panic(FAULT);  // it must be only one colon
                                }
                                bcolon = bchidx - 1;
                                if (st == 1) {
                                    st = 2;
                                }
                            }
                            break;

                        default:
                            mChIdx--;  // bkch();
                            bcopy(cstart, bstart);
                            mBuff[bqname] = (char) (bcolon - bqname);
                            return;
                    }
                    break;

                default:
                    panic(FAULT);
            }
        }
    }

    /**
     * Reads a nmtoken.
     *
     * This is low level routine which leaves a nmtoken in the buffer.
     *
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    @SuppressWarnings("fallthrough")
    private void bntok() throws Exception {
        char ch;
        mBuffIdx = -1;
        bappend((char) 0);  // default offset to the colon char
        while (true) {
            ch = getch();
            switch (chtyp(ch)) {
                case 'a':
                case 'A':
                case 'd':
                case '.':
                case ':':
                case '-':
                case '_':
                case 'X':
                    bappend(ch);
                    break;

                case 'Z':
                    panic(FAULT);

                default:
                    bkch();
                    return;
            }
        }
    }

    /**
     * Recognizes a keyword.
     *
     * This is low level routine which recognizes one of keywords in the buffer.
     * Keyword Id ID - i IDREF - r IDREFS - R ENTITY - n ENTITIES - N NMTOKEN -
     * t NMTOKENS - T ELEMENT - e ATTLIST - a NOTATION - o CDATA - c REQUIRED -
     * Q IMPLIED - I FIXED - F
     *
     * @return an id of a keyword or '?'.
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    private char bkeyword()
            throws Exception {
        String str = new String(mBuff, 1, mBuffIdx);
        switch (str.length()) {
            case 2:  // ID
                return ("ID".equals(str) == true) ? 'i' : '?';

            case 5:  // IDREF, CDATA, FIXED
                switch (mBuff[1]) {
                    case 'I':
                        return ("IDREF".equals(str) == true) ? 'r' : '?';
                    case 'C':
                        return ("CDATA".equals(str) == true) ? 'c' : '?';
                    case 'F':
                        return ("FIXED".equals(str) == true) ? 'F' : '?';
                    default:
                        break;
                }
                break;

            case 6:  // IDREFS, ENTITY
                switch (mBuff[1]) {
                    case 'I':
                        return ("IDREFS".equals(str) == true) ? 'R' : '?';
                    case 'E':
                        return ("ENTITY".equals(str) == true) ? 'n' : '?';
                    default:
                        break;
                }
                break;

            case 7:  // NMTOKEN, IMPLIED, ATTLIST, ELEMENT
                switch (mBuff[1]) {
                    case 'I':
                        return ("IMPLIED".equals(str) == true) ? 'I' : '?';
                    case 'N':
                        return ("NMTOKEN".equals(str) == true) ? 't' : '?';
                    case 'A':
                        return ("ATTLIST".equals(str) == true) ? 'a' : '?';
                    case 'E':
                        return ("ELEMENT".equals(str) == true) ? 'e' : '?';
                    default:
                        break;
                }
                break;

            case 8:  // ENTITIES, NMTOKENS, NOTATION, REQUIRED
                switch (mBuff[2]) {
                    case 'N':
                        return ("ENTITIES".equals(str) == true) ? 'N' : '?';
                    case 'M':
                        return ("NMTOKENS".equals(str) == true) ? 'T' : '?';
                    case 'O':
                        return ("NOTATION".equals(str) == true) ? 'o' : '?';
                    case 'E':
                        return ("REQUIRED".equals(str) == true) ? 'Q' : '?';
                    default:
                        break;
                }
                break;

            default:
                break;
        }
        return '?';
    }

    /**
     * Reads a single or double quotted string in to the buffer.
     *
     * This method resolves entities inside a string unless the parser parses
     * DTD.
     *
     * @param flag 'c' - CDATA, 'i' - non CDATA, ' ' - no normalization; '-' -
     * not an attribute value; 'd' - in DTD context.
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    @SuppressWarnings("fallthrough")
    private void bqstr(char flag) throws Exception {
        Input inp = mInp;  // remember the original input
        mBuffIdx = -1;
        bappend((char) 0);  // default offset to the colon char
        char ch;
        for (short st = 0; st >= 0;) {
            ch = (mChIdx < mChLen) ? mChars[mChIdx++] : getch();
            switch (st) {
                case 0:     // read a single or double quote
                    switch (ch) {
                        case ' ':
                        case '\n':
                        case '\r':
                        case '\t':
                            break;

                        case '\'':
                            st = 2;  // read a single quoted string
                            break;

                        case '\"':
                            st = 3;  // read a double quoted string
                            break;

                        default:
                            panic(FAULT);
                            break;
                    }
                    break;

                case 2:     // read a single quoted string
                case 3:     // read a double quoted string
                    switch (ch) {
                        case '\'':
                            if ((st == 2) && (mInp == inp)) {
                                st = -1;
                            } else {
                                bappend(ch);
                            }
                            break;

                        case '\"':
                            if ((st == 3) && (mInp == inp)) {
                                st = -1;
                            } else {
                                bappend(ch);
                            }
                            break;

                        case '&':
                            if (flag != 'd') {
                                ent(flag);
                            } else {
                                bappend(ch);
                            }
                            break;

                        case '%':
                            if (flag == 'd') {
                                pent('-');
                            } else {
                                bappend(ch);
                            }
                            break;

                        case '<':
                            if ((flag == '-') || (flag == 'd')) {
                                bappend(ch);
                            } else {
                                panic(FAULT);
                            }
                            break;

                        case EOS:               // EOS before single/double quote
                            panic(FAULT);

                        case '\r':     // EOL processing [#2.11 & #3.3.3]
                            if (flag != ' ' && mInp.next == null) {
                                if (getch() != '\n') {
                                    bkch();
                                }
                                ch = '\n';
                            }
                        default:
                            bappend(ch, flag);
                            break;
                    }
                    break;

                default:
                    panic(FAULT);
            }
        }
        //              There is maximum one space at the end of the string in
        //              i-mode (non CDATA normalization) and it has to be removed.
        if ((flag == 'i') && (mBuff[mBuffIdx] == ' ')) {
            mBuffIdx -= 1;
        }
    }

    /**
     * Reports characters and empties the parser's buffer. This method is called
     * only if parser is going to return control to the main loop. This means
     * that this method may use parser buffer to report white space without
     * copying characters to temporary buffer.
     */
    protected abstract void bflash()
            throws Exception;

    /**
     * Reports white space characters and empties the parser's buffer. This
     * method is called only if parser is going to return control to the main
     * loop. This means that this method may use parser buffer to report white
     * space without copying characters to temporary buffer.
     */
    protected abstract void bflash_ws()
            throws Exception;

    /**
     * Appends a character to parser's buffer with normalization.
     *
     * @param ch The character to append to the buffer.
     * @param mode The normalization mode.
     */
    private void bappend(char ch, char mode) {
        //              This implements attribute value normalization as
        //              described in the XML specification [#3.3.3].
        switch (mode) {
            case 'i':  // non CDATA normalization
                switch (ch) {
                    case ' ':
                    case '\n':
                    case '\r':
                    case '\t':
                        if ((mBuffIdx > 0) && (mBuff[mBuffIdx] != ' ')) {
                            bappend(' ');
                        }
                        return;

                    default:
                        break;
                }
                break;

            case 'c':  // CDATA normalization
                switch (ch) {
                    case '\n':
                    case '\r':
                    case '\t':
                        ch = ' ';
                        break;

                    default:
                        break;
                }
                break;

            default:  // no normalization
                break;
        }
        mBuffIdx++;
        if (mBuffIdx < mBuff.length) {
            mBuff[mBuffIdx] = ch;
        } else {
            mBuffIdx--;
            bappend(ch);
        }
    }

    /**
     * Appends a character to parser's buffer.
     *
     * @param ch The character to append to the buffer.
     */
    private void bappend(char ch) {
        try {
            mBuff[++mBuffIdx] = ch;
        } catch (Exception exp) {
            //          Double the buffer size
            char buff[] = new char[mBuff.length << 1];
            System.arraycopy(mBuff, 0, buff, 0, mBuff.length);
            mBuff = buff;
            mBuff[mBuffIdx] = ch;
        }
    }

    /**
     * Appends (mChIdx - cidx) characters from character buffer (mChars) to
     * parser's buffer (mBuff).
     *
     * @param cidx The character buffer (mChars) start index.
     * @param bidx The parser buffer (mBuff) start index.
     */
    private void bcopy(int cidx, int bidx) {
        int length = mChIdx - cidx;
        if ((bidx + length + 1) >= mBuff.length) {
            //          Expand the buffer
            char buff[] = new char[mBuff.length + length];
            System.arraycopy(mBuff, 0, buff, 0, mBuff.length);
            mBuff = buff;
        }
        System.arraycopy(mChars, cidx, mBuff, bidx, length);
        mBuffIdx += length;
    }

    /**
     * Recognizes the built-in entities <i>lt</i>, <i>gt</i>, <i>amp</i>,
     * <i>apos</i>, <i>quot</i>. The initial state is 0x100. Any state belowe
     * 0x100 is a built-in entity replacement character.
     *
     * @param ch the next character of an entity name.
     */
    @SuppressWarnings("fallthrough")
    private void eappend(char ch) {
        switch (mESt) {
            case 0x100:  // "l" or "g" or "a" or "q"
                switch (ch) {
                    case 'l':
                        mESt = 0x101;
                        break;
                    case 'g':
                        mESt = 0x102;
                        break;
                    case 'a':
                        mESt = 0x103;
                        break;
                    case 'q':
                        mESt = 0x107;
                        break;
                    default:
                        mESt = 0x200;
                        break;
                }
                break;

            case 0x101:  // "lt"
                mESt = (ch == 't') ? '<' : (char) 0x200;
                break;

            case 0x102:  // "gt"
                mESt = (ch == 't') ? '>' : (char) 0x200;
                break;

            case 0x103:  // "am" or "ap"
                switch (ch) {
                    case 'm':
                        mESt = 0x104;
                        break;
                    case 'p':
                        mESt = 0x105;
                        break;
                    default:
                        mESt = 0x200;
                        break;
                }
                break;

            case 0x104:  // "amp"
                mESt = (ch == 'p') ? '&' : (char) 0x200;
                break;

            case 0x105:  // "apo"
                mESt = (ch == 'o') ? (char) 0x106 : (char) 0x200;
                break;

            case 0x106:  // "apos"
                mESt = (ch == 's') ? '\'' : (char) 0x200;
                break;

            case 0x107:  // "qu"
                mESt = (ch == 'u') ? (char) 0x108 : (char) 0x200;
                break;

            case 0x108:  // "quo"
                mESt = (ch == 'o') ? (char) 0x109 : (char) 0x200;
                break;

            case 0x109:  // "quot"
                mESt = (ch == 't') ? '\"' : (char) 0x200;
                break;

            case '<':   // "lt"
            case '>':   // "gt"
            case '&':   // "amp"
            case '\'':  // "apos"
            case '\"':  // "quot"
                mESt = 0x200;
            default:
                break;
        }
    }

    /**
     * Sets up a new input source on the top of the input stack. Note, the first
     * byte returned by the entity's byte stream has to be the first byte in the
     * entity. However, the parser does not expect the byte order mask in both
     * cases when encoding is provided by the input source.
     *
     * @param is A new input source to set up.
     * @exception IOException If any IO errors occur.
     * @exception Exception is parser specific exception form panic method.
     */
    protected void setinp(InputSource is)
            throws Exception {
        Reader reader = null;
        mChIdx = 0;
        mChLen = 0;
        mChars = mInp.chars;
        mInp.src = null;
        if (mPh < PH_DOC_START) {
            mIsSAlone = false;  // default [#2.9]
        }
        mIsSAloneSet = false;
        if (is.getCharacterStream() != null) {
            //          Ignore encoding in the xml text decl.
            reader = is.getCharacterStream();
            xml(reader);
        } else if (is.getByteStream() != null) {
            String expenc;
            if (is.getEncoding() != null) {
                //              Ignore encoding in the xml text decl.
                expenc = is.getEncoding().toUpperCase();
                if (expenc.equals("UTF-16")) {
                    reader = bom(is.getByteStream(), 'U');  // UTF-16 [#4.3.3]
                } else {
                    reader = enc(expenc, is.getByteStream());
                }
                xml(reader);
            } else {
                //              Get encoding from BOM or the xml text decl.
                reader = bom(is.getByteStream(), ' ');
                /**
                 * [#4.3.3] requires BOM for UTF-16, however, it's not uncommon
                 * that it may be missing. A mature technique exists in Xerces
                 * to further check for possible UTF-16 encoding
                 */
                if (reader == null) {
                    reader = utf16(is.getByteStream());
                }

                if (reader == null) {
                    //          Encoding is defined by the xml text decl.
                    reader = enc("UTF-8", is.getByteStream());
                    expenc = xml(reader);
                    if (!expenc.equals("UTF-8")) {
                        if (expenc.startsWith("UTF-16")) {
                            panic(FAULT);  // UTF-16 must have BOM [#4.3.3]
                        }
                        reader = enc(expenc, is.getByteStream());
                    }
                } else {
                    //          Encoding is defined by the BOM.
                    xml(reader);
                }
            }
        } else {
            //          There is no support for public/system identifiers.
            panic(FAULT);
        }
        mInp.src = reader;
        mInp.pubid = is.getPublicId();
        mInp.sysid = is.getSystemId();
    }

    /**
     * Determines the entity encoding.
     *
     * This method gets encoding from Byte Order Mask [#4.3.3] if any. Note, the
     * first byte returned by the entity's byte stream has to be the first byte
     * in the entity. Also, there is no support for UCS-4.
     *
     * @param is A byte stream of the entity.
     * @param hint An encoding hint, character U means UTF-16.
     * @return a reader constructed from the BOM or UTF-8 by default.
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    private Reader bom(InputStream is, char hint)
            throws Exception {
        int val = is.read();
        switch (val) {
            case 0xef:     // UTF-8
                if (hint == 'U') // must be UTF-16
                {
                    panic(FAULT);
                }
                if (is.read() != 0xbb) {
                    panic(FAULT);
                }
                if (is.read() != 0xbf) {
                    panic(FAULT);
                }
                return new ReaderUTF8(is);

            case 0xfe:     // UTF-16, big-endian
                if (is.read() != 0xff) {
                    panic(FAULT);
                }
                return new ReaderUTF16(is, 'b');

            case 0xff:     // UTF-16, little-endian
                if (is.read() != 0xfe) {
                    panic(FAULT);
                }
                return new ReaderUTF16(is, 'l');

            case -1:
                mChars[mChIdx++] = EOS;
                return new ReaderUTF8(is);

            default:
                if (hint == 'U') // must be UTF-16
                {
                    panic(FAULT);
                }
                //              Read the rest of UTF-8 character
                switch (val & 0xf0) {
                    case 0xc0:
                    case 0xd0:
                        mChars[mChIdx++] = (char) (((val & 0x1f) << 6) | (is.read() & 0x3f));
                        break;

                    case 0xe0:
                        mChars[mChIdx++] = (char) (((val & 0x0f) << 12)
                                | ((is.read() & 0x3f) << 6) | (is.read() & 0x3f));
                        break;

                    case 0xf0:  // UCS-4 character
                        throw new UnsupportedEncodingException();

                    default:
                        mChars[mChIdx++] = (char) val;
                        break;
                }
                return null;
        }
    }


    /**
     * Using a mature technique from Xerces, this method checks further after
     * the bom method above to see if the encoding is UTF-16
     *
     * @param is A byte stream of the entity.
     * @return a reader, may be null
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    private Reader utf16(InputStream is)
            throws Exception {
        if (mChIdx != 0) {
            // The bom method has read ONE byte into the buffer.
            byte b0 = (byte)mChars[0];
            if (b0 == 0x00 || b0 == 0x3C) {
                int b1 = is.read();
                int b2 = is.read();
                int b3 = is.read();
                if (b0 == 0x00 && b1 == 0x3C && b2 == 0x00 && b3 == 0x3F) {
                    // UTF-16, big-endian, no BOM
                    mChars[0] = (char)(b1);
                    mChars[mChIdx++] = (char)(b3);
                    return new ReaderUTF16(is, 'b');
                } else if (b0 == 0x3C && b1 == 0x00 && b2 == 0x3F && b3 == 0x00) {
                    // UTF-16, little-endian, no BOM
                    mChars[0] = (char)(b0);
                    mChars[mChIdx++] = (char)(b2);
                    return new ReaderUTF16(is, 'l');
                } else {
                    /* not every InputStream supports reset, so we have to remember
                     * the state for further parsing
                     */
                    mChars[0] = (char)(b0);
                    mChars[mChIdx++] = (char)(b1);
                    mChars[mChIdx++] = (char)(b2);
                    mChars[mChIdx++] = (char)(b3);
                }

            }
        }
        return null;
    }
    /**
     * Parses the xml text declaration.
     *
     * This method gets encoding from the xml text declaration [#4.3.1] if any.
     * The method assumes the buffer (mChars) is big enough to accommodate whole
     * xml text declaration.
     *
     * @param reader is entity reader.
     * @return The xml text declaration encoding or default UTF-8 encoding.
     * @exception Exception is parser specific exception form panic method.
     * @exception IOException
     */
    private String xml(Reader reader)
            throws Exception {
        String str = null;
        String enc = "UTF-8";
        char ch;
        int val;
        short st = 0;
        int byteRead =  mChIdx; //number of bytes read prior to entering this method

        while (st >= 0 && mChIdx < mChars.length) {
            if (st < byteRead) {
                ch = mChars[st];
            } else {
                ch = ((val = reader.read()) >= 0) ? (char) val : EOS;
                mChars[mChIdx++] = ch;
            }

            switch (st) {
                case 0:     // read '<' of xml declaration
                    switch (ch) {
                        case '<':
                            st = 1;
                            break;

                        case 0xfeff:    // the byte order mask
                            ch = ((val = reader.read()) >= 0) ? (char) val : EOS;
                            mChars[mChIdx - 1] = ch;
                            st = (short) ((ch == '<') ? 1 : -1);
                            break;

                        default:
                            st = -1;
                            break;
                    }
                    break;

                case 1:     // read '?' of xml declaration [#4.3.1]
                    st = (short) ((ch == '?') ? 2 : -1);
                    break;

                case 2:     // read 'x' of xml declaration [#4.3.1]
                    st = (short) ((ch == 'x') ? 3 : -1);
                    break;

                case 3:     // read 'm' of xml declaration [#4.3.1]
                    st = (short) ((ch == 'm') ? 4 : -1);
                    break;

                case 4:     // read 'l' of xml declaration [#4.3.1]
                    st = (short) ((ch == 'l') ? 5 : -1);
                    break;

                case 5:     // read white space after 'xml'
                    switch (ch) {
                        case ' ':
                        case '\t':
                        case '\r':
                        case '\n':
                            st = 6;
                            break;

                        default:
                            st = -1;
                            break;
                    }
                    break;

                case 6:     // read content of xml declaration
                    switch (ch) {
                        case '?':
                            st = 7;
                            break;

                        case EOS:
                            st = -2;
                            break;

                        default:
                            break;
                    }
                    break;

                case 7:     // read '>' after '?' of xml declaration
                    switch (ch) {
                        case '>':
                        case EOS:
                            st = -2;
                            break;

                        default:
                            st = 6;
                            break;
                    }
                    break;

                default:
                    panic(FAULT);
                    break;
            }
        }
        mChLen = mChIdx;
        mChIdx = 0;
        //              If there is no xml text declaration, the encoding is default.
        if (st == -1) {
            return enc;
        }
        mChIdx = 5;  // the first white space after "<?xml"
        //              Parse the xml text declaration
        for (st = 0; st >= 0;) {
            ch = getch();
            switch (st) {
                case 0:     // skip spaces after the xml declaration name
                    if (chtyp(ch) != ' ') {
                        bkch();
                        st = 1;
                    }
                    break;

                case 1:     // read xml declaration version
                case 2:     // read xml declaration encoding or standalone
                case 3:     // read xml declaration standalone
                    switch (chtyp(ch)) {
                        case 'a':
                        case 'A':
                        case '_':
                            bkch();
                            str = name(false).toLowerCase();
                            if ("version".equals(str) == true) {
                                if (st != 1) {
                                    panic(FAULT);
                                }
                                if ("1.0".equals(eqstr('=')) != true) {
                                    panic(FAULT);
                                }
                                mInp.xmlver = 0x0100;
                                st = 2;
                            } else if ("encoding".equals(str) == true) {
                                if (st != 2) {
                                    panic(FAULT);
                                }
                                mInp.xmlenc = eqstr('=').toUpperCase();
                                enc = mInp.xmlenc;
                                st = 3;
                            } else if ("standalone".equals(str) == true) {
                                if ((st == 1) || (mPh >= PH_DOC_START)) // [#4.3.1]
                                {
                                    panic(FAULT);
                                }
                                str = eqstr('=').toLowerCase();
                                //              Check the 'standalone' value and use it [#5.1]
                                if (str.equals("yes") == true) {
                                    mIsSAlone = true;
                                } else if (str.equals("no") == true) {
                                    mIsSAlone = false;
                                } else {
                                    panic(FAULT);
                                }
                                mIsSAloneSet = true;
                                st = 4;
                            } else {
                                panic(FAULT);
                            }
                            break;

                        case ' ':
                            break;

                        case '?':
                            if (st == 1) {
                                panic(FAULT);
                            }
                            bkch();
                            st = 4;
                            break;

                        default:
                            panic(FAULT);
                    }
                    break;

                case 4:     // end of xml declaration
                    switch (chtyp(ch)) {
                        case '?':
                            if (getch() != '>') {
                                panic(FAULT);
                            }
                            if (mPh <= PH_DOC_START) {
                                mPh = PH_MISC_DTD;  // misc before DTD
                            }
                            st = -1;
                            break;

                        case ' ':
                            break;

                        default:
                            panic(FAULT);
                    }
                    break;

                default:
                    panic(FAULT);
            }
        }
        return enc;
    }

    /**
     * Sets up the document reader.
     *
     * @param name an encoding name.
     * @param is the document byte input stream.
     * @return a reader constructed from encoding name and input stream.
     * @exception UnsupportedEncodingException
     */
    private Reader enc(String name, InputStream is)
            throws UnsupportedEncodingException {
        //              DO NOT CLOSE current reader if any!
        if (name.equals("UTF-8")) {
            return new ReaderUTF8(is);
        } else if (name.equals("UTF-16LE")) {
            return new ReaderUTF16(is, 'l');
        } else if (name.equals("UTF-16BE")) {
            return new ReaderUTF16(is, 'b');
        } else {
            return new InputStreamReader(is, name);
        }
    }

    /**
     * Sets up current input on the top of the input stack.
     *
     * @param inp A new input to set up.
     */
    protected void push(Input inp) {
        mInp.chLen = mChLen;
        mInp.chIdx = mChIdx;
        inp.next = mInp;
        mInp = inp;
        mChars = inp.chars;
        mChLen = inp.chLen;
        mChIdx = inp.chIdx;
    }

    /**
     * Restores previous input on the top of the input stack.
     */
    protected void pop() {
        if (mInp.src != null) {
            try {
                mInp.src.close();
            } catch (IOException ioe) {
            }
            mInp.src = null;
        }
        mInp = mInp.next;
        if (mInp != null) {
            mChars = mInp.chars;
            mChLen = mInp.chLen;
            mChIdx = mInp.chIdx;
        } else {
            mChars = null;
            mChLen = 0;
            mChIdx = 0;
        }
    }

    /**
     * Maps a character to its type.
     *
     * Possible character type values are:
     * <ul>
     * <li>' ' - for any kind of whitespace character;</li>
     * <li>'a' - for any lower case alphabetical character value;</li>
     * <li>'A' - for any upper case alphabetical character value;</li>
     * <li>'d' - for any decimal digit character value;</li>
     * <li>'z' - for any character less than ' ' except '\t', '\n', '\r';</li>
     * <li>'X' - for any not ASCII character;</li>
     * <li>'Z' - for EOS character.</li>
     * </ul>
     * An ASCII (7 bit) character which does not fall in any category
     * listed above is mapped to itself.
     *
     * @param ch The character to map.
     * @return The type of character.
     */
    protected char chtyp(char ch) {
        if (ch < 0x80) {
            return (char) asctyp[ch];
        }
        return (ch != EOS) ? 'X' : 'Z';
    }

    /**
     * Retrives the next character in the document.
     *
     * @return The next character in the document.
     */
    protected char getch()
            throws IOException {
        if (mChIdx >= mChLen) {
            if (mInp.src == null) {
                pop();  // remove internal entity
                return getch();
            }
            //          Read new portion of the document characters
            int Num = mInp.src.read(mChars, 0, mChars.length);
            if (Num < 0) {
                if (mInp != mDoc) {
                    pop();  // restore the previous input
                    return getch();
                } else {
                    mChars[0] = EOS;
                    mChLen = 1;
                }
            } else {
                mChLen = Num;
            }
            mChIdx = 0;
        }
        return mChars[mChIdx++];
    }

    /**
     * Puts back the last read character.
     *
     * This method <strong>MUST NOT</strong> be called more then once after each
     * call of {@link #getch getch} method.
     */
    protected void bkch()
            throws Exception {
        if (mChIdx <= 0) {
            panic(FAULT);
        }
        mChIdx--;
    }

    /**
     * Sets the current character.
     *
     * @param ch The character to set.
     */
    protected void setch(char ch) {
        mChars[mChIdx] = ch;
    }

    /**
     * Finds a pair in the pair chain by a qualified name.
     *
     * @param chain The first element of the chain of pairs.
     * @param qname The qualified name.
     * @return A pair with the specified qualified name or null.
     */
    protected Pair find(Pair chain, char[] qname) {
        for (Pair pair = chain; pair != null; pair = pair.next) {
            if (pair.eqname(qname) == true) {
                return pair;
            }
        }
        return null;
    }

    /**
     * Provedes an instance of a pair.
     *
     * @param next The reference to a next pair.
     * @return An instance of a pair.
     */
    protected Pair pair(Pair next) {
        Pair pair;

        if (mDltd != null) {
            pair = mDltd;
            mDltd = pair.next;
        } else {
            pair = new Pair();
        }
        pair.next = next;

        return pair;
    }

    /**
     * Deletes an instance of a pair.
     *
     * @param pair The pair to delete.
     * @return A reference to the next pair in a chain.
     */
    protected Pair del(Pair pair) {
        Pair next = pair.next;

        pair.name = null;
        pair.value = null;
        pair.chars = null;
        pair.list = null;
        pair.next = mDltd;
        mDltd = pair;

        return next;
    }
}
