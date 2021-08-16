/*
 * Copyright (c) 1998, 2016, Oracle and/or its affiliates. All rights reserved.
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

package build.tools.dtdbuilder;

import javax.swing.text.html.parser.*;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import java.util.BitSet;
import java.text.MessageFormat;

/**
 * A parser for DTDs. This parser roughly corresponds to the
 * rules specified in "The SGML Handbook" by Charles F. Goldfarb.
 * The end result of parsing the stream is a DTD object.
 *
 *
 * @see DTD
 * @see DTDInputStream
 * @author Arthur van Hoff
 */
final
class DTDParser implements DTDConstants {
    DTDBuilder dtd;
    DTDInputStream in;
    int ch;
    char str[] = new char[128];
    int strpos = 0;
    int nerrors = 0;

    /**
     * Report an error.
     */
    void error(String err, String arg1, String arg2, String arg3) {
        nerrors++;

        String msgParams[] = {arg1, arg2, arg3};

        String str = getSubstProp("dtderr." + err, msgParams);
        if (str == null) {
            str = err + "[" + arg1 + "," + arg2 + "," + arg3 + "]";
        }
        System.err.println("line " + in.ln + ", dtd " + dtd + ": " + str);
    }
    void error(String err, String arg1, String arg2) {
        error(err, arg1, arg2, "?");
    }
    void error(String err, String arg1) {
        error(err, arg1, "?", "?");
    }
    void error(String err) {
        error(err, "?", "?", "?");
    }

    private String getSubstProp(String propName, String args[]) {
        String prop = System.getProperty(propName);

        if (prop == null) {
            return null;
        }

        return MessageFormat.format(prop, (Object[])args);
    }

    /**
     * Expect a character.
     */
    boolean expect(int c) throws IOException {
        if (ch != c) {
            char str[] = {(char)c};
            error("expected", "'" + new String(str) + "'");
            return false;
        }
        ch = in.read();
        return true;
    }

    /**
     * Add a char to the string buffer.
     */
    void addString(int c) {
        if (strpos == str.length) {
            char newstr[] = new char[str.length * 2];
            System.arraycopy(str, 0, newstr, 0, str.length);
            str = newstr;
        }
        str[strpos++] = (char)c;
    }

    /**
     * Get the string which was accumulated in the buffer.
     * Pos is the starting position of the string.
     */
    String getString(int pos) {
        char newstr[] = new char[strpos - pos];
        System.arraycopy(str, pos, newstr, 0, strpos - pos);
        strpos = pos;
        return new String(newstr);
    }

    /**
     * Get the chars which were accumulated in the buffer.
     * Pos is the starting position of the string.
     */
    char[] getChars(int pos) {
        char newstr[] = new char[strpos - pos];
        System.arraycopy(str, pos, newstr, 0, strpos - pos);
        strpos = pos;
        return newstr;
    }

    /**
     * Skip spaces. [5] 297:23
     */
    void skipSpace() throws IOException {
        while (true) {
            switch (ch) {
              case '\n':
              case ' ':
              case '\t':
                ch = in.read();
                break;

              default:
                return;
            }
        }
    }

    /**
     * Skip tag spaces (includes comments). [65] 372:1
     */
    void skipParameterSpace() throws IOException {
        while (true) {
            switch (ch) {
              case '\n':
              case ' ':
              case '\t':
                ch = in.read();
                break;
              case '-':
                if ((ch = in.read()) != '-') {
                    in.push(ch);
                    ch = '-';
                    return;
                }

                in.replace++;
                while (true) {
                    switch (ch = in.read()) {
                      case '-':
                        if ((ch = in.read()) == '-') {
                            ch = in.read();
                            in.replace--;
                            skipParameterSpace();
                            return;
                        }
                        break;

                      case -1:
                        error("eof.arg", "comment");
                        in.replace--;
                        return;
                    }
                }
              default:
                return;
            }
        }
    }

    /**
     * Parse identifier. Uppercase characters are automatically
     * folded to lowercase. Returns falsed if no identifier is found.
     */
    @SuppressWarnings("fallthrough")
    boolean parseIdentifier(boolean lower) throws IOException {
        switch (ch) {
          case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
          case 'G': case 'H': case 'I': case 'J': case 'K': case 'L':
          case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R':
          case 'S': case 'T': case 'U': case 'V': case 'W': case 'X':
          case 'Y': case 'Z':
            if (lower) {
                ch = 'a' + (ch - 'A');
            }
            /* fall through */

          case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
          case 'g': case 'h': case 'i': case 'j': case 'k': case 'l':
          case 'm': case 'n': case 'o': case 'p': case 'q': case 'r':
          case 's': case 't': case 'u': case 'v': case 'w': case 'x':
          case 'y': case 'z':
            break;

          default:
            return false;
        }

        addString(ch);
        ch = in.read();
        parseNameToken(lower);
        return true;
    }

    /**
     * Parses name token. If <code>lower</code> is true, upper case letters
     * are folded to lower case. Returns falsed if no token is found.
     */
    @SuppressWarnings("fallthrough")
    boolean parseNameToken(boolean lower) throws IOException {
        boolean first = true;

        while (true) {
            switch (ch) {
              case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
              case 'G': case 'H': case 'I': case 'J': case 'K': case 'L':
              case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R':
              case 'S': case 'T': case 'U': case 'V': case 'W': case 'X':
              case 'Y': case 'Z':
                if (lower) {
                    ch = 'a' + (ch - 'A');
                }
                /* fall through */

              case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
              case 'g': case 'h': case 'i': case 'j': case 'k': case 'l':
              case 'm': case 'n': case 'o': case 'p': case 'q': case 'r':
              case 's': case 't': case 'u': case 'v': case 'w': case 'x':
              case 'y': case 'z':

              case '0': case '1': case '2': case '3': case '4':
              case '5': case '6': case '7': case '8': case '9':

              case '.': case '-':
                addString(ch);
                ch = in.read();
                first = false;
                break;

              default:
                return !first;
            }
        }
    }

    /**
     * Parse a list of identifiers.
     */
    Vector<String> parseIdentifierList(boolean lower) throws IOException {
        Vector<String> elems = new Vector<>();
        skipSpace();
        switch (ch) {
          case '(':
            ch = in.read();
            skipParameterSpace();
            while (parseNameToken(lower)) {
                elems.addElement(getString(0));
                skipParameterSpace();
                if (ch == '|') {
                    ch = in.read();
                    skipParameterSpace();
                }
            }
            expect(')');
            skipParameterSpace();
            break;

          default:
            if (!parseIdentifier(lower)) {
                error("expected", "identifier");
                break;
            }
            elems.addElement(getString(0));
            skipParameterSpace();
            break;
        }
        return elems;
    }

    /**
     * Parse and Entity reference. Should be called when
     * a &amp; is encountered. The data is put in the string buffer.
     * [59] 350:17
     */
    private void parseEntityReference() throws IOException {
        int pos = strpos;

        if ((ch = in.read()) == '#') {
            int n = 0;
            ch = in.read();
            if (((ch >= 'a') && (ch <= 'z')) || ((ch >= 'A') && (ch <= 'Z'))) {
                addString('#');
            } else {
                while ((ch >= '0') && (ch <= '9')) {
                    n = (n * 10) + ch - '0';
                    ch = in.read();
                }
                if ((ch == ';') || (ch == '\n')) {
                    ch = in.read();
                }
                addString(n);
                return;
            }
        }

        while (true) {
            switch (ch) {
              case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
              case 'G': case 'H': case 'I': case 'J': case 'K': case 'L':
              case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R':
              case 'S': case 'T': case 'U': case 'V': case 'W': case 'X':
              case 'Y': case 'Z':

              case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
              case 'g': case 'h': case 'i': case 'j': case 'k': case 'l':
              case 'm': case 'n': case 'o': case 'p': case 'q': case 'r':
              case 's': case 't': case 'u': case 'v': case 'w': case 'x':
              case 'y': case 'z':

              case '0': case '1': case '2': case '3': case '4':
              case '5': case '6': case '7': case '8': case '9':

              case '.': case '-':
                addString(ch);
                ch = in.read();
                break;

              default:
                if (strpos == pos) {
                    addString('&');
                    return;
                }
                String nm = getString(pos);
                Entity ent = dtd.getEntity(nm);
                if (ent == null) {
                    error("undef.entref" + nm);
                    return;
                }
                if ((ch == ';') || (ch == '\n')) {
                    ch = in.read();
                }
                char data[] = ent.getData();
                for (int i = 0 ; i < data.length ; i++) {
                    addString(data[i]);
                }
                return;
            }
        }
    }

    /**
     * Parse an entity declaration.
     * [101] 394:18
     * REMIND: external entity type
     */
    private void parseEntityDeclaration() throws IOException {
        int type = GENERAL;

        skipSpace();
        if (ch == '%') {
            ch = in.read();
            type = PARAMETER;
            skipSpace();
        }
        if (ch == '#') {
            addString('#');
            ch = in.read();
        }
        if (!parseIdentifier(false)) {
            error("expected", "identifier");
            return;
        }
        String nm = getString(0);
        skipParameterSpace();
        if (parseIdentifier(false)) {
            String tnm = getString(0);
            int t = Entity.name2type(tnm);
            if (t == 0) {
                error("invalid.arg", "entity type", tnm);
            } else {
                type |= t;
            }
            skipParameterSpace();
        }

        if ((ch != '"') && (ch != '\'')) {
            error("expected", "entity value");
            skipParameterSpace();
            if (ch == '>') {
                ch = in.read();
            }
            return;
        }

        int term = ch;
        ch = in.read();
        while ((ch != -1) && (ch != term)) {
            if (ch == '&') {
                parseEntityReference();
            } else {
                addString(ch & 0xFF);
                ch = in.read();
            }
        }
        if (ch == term) {
            ch = in.read();
        }
        if (in.replace == 0) {
            char data[] = getChars(0);
            dtd.defineEntity(nm, type, data);
        } else {
            strpos = 0;
        }
        skipParameterSpace();
        expect('>');
    }

    /**
     * Parse content model.
     * [126] 410:1
     * REMIND: data tag group
     */
    ContentModel parseContentModel() throws IOException {
        ContentModel m = null;

        switch (ch) {
          case '(':
            ch = in.read();
            skipParameterSpace();
            ContentModel e = parseContentModel();

            if (ch != ')') {
                m = new ContentModel(ch, e);
                do {
                    ch = in.read();
                    skipParameterSpace();
                    e.next = parseContentModel();
                    if (e.next.type == m.type) {
                        e.next = (ContentModel)e.next.content;
                    }
                    for (; e.next != null ; e = e.next);
                } while (ch == m.type);
            } else {
                m = new ContentModel(',', e);
            }
            expect(')');
            break;

          case '#':
            ch = in.read();
            if (parseIdentifier(true)) {
                m = new ContentModel('*', new ContentModel(dtd.getElement("#" + getString(0))));
            } else {
                error("invalid", "content model");
            }
            break;

          default:
            if (parseIdentifier(true)) {
                m = new ContentModel(dtd.getElement(getString(0)));
            } else {
                error("invalid", "content model");
            }
            break;
        }

        switch (ch) {
          case '?':
          case '*':
          case '+':
            m = new ContentModel(ch, m);
            ch = in.read();
            break;
        }
        skipParameterSpace();

        return m;
    }

    /**
     * Parse element declaration.
     * [116] 405:6
     */
    void parseElementDeclaration() throws IOException {
        Vector<String> elems = parseIdentifierList(true);
        BitSet inclusions = null;
        BitSet exclusions = null;
        boolean omitStart = false;
        boolean omitEnd = false;

        if ((ch == '-') || (ch == 'O')) {
            omitStart = ch == 'O';
            ch = in.read();
            skipParameterSpace();

            if ((ch == '-') || (ch == 'O')) {
                omitEnd = ch == 'O';
                ch = in.read();
                skipParameterSpace();
            } else {
                expect('-');
            }
        }

        int type = MODEL;
        ContentModel content = null;
        if (parseIdentifier(false)) {
            String nm = getString(0);
            type = Element.name2type(nm);
            if (type == 0) {
                error("invalid.arg", "content type", nm);
                type = EMPTY;
            }
            skipParameterSpace();
        } else {
            content = parseContentModel();
        }

        if ((type == MODEL) || (type == ANY)) {
            if (ch == '-') {
                ch = in.read();
                Vector<String> v = parseIdentifierList(true);
                exclusions = new BitSet();
                for (Enumeration<String> e = v.elements() ; e.hasMoreElements() ;) {
                    exclusions.set(dtd.getElement(e.nextElement()).getIndex());
                }
            }
            if (ch == '+') {
                ch = in.read();
                Vector<String> v = parseIdentifierList(true);
                inclusions = new BitSet();
                for (Enumeration<String> e = v.elements() ; e.hasMoreElements() ;) {
                    inclusions.set(dtd.getElement(e.nextElement()).getIndex());
                }
            }
        }
        expect('>');

        if (in.replace == 0) {
            for (Enumeration<String> e = elems.elements() ; e.hasMoreElements() ;) {
                dtd.defineElement(e.nextElement(), type, omitStart, omitEnd, content, exclusions, inclusions, null);
            }
        }
    }

    /**
     * Parse an attribute declared value.
     * [145] 422:6
     */
    void parseAttributeDeclaredValue(AttributeList atts) throws IOException {
        if (ch == '(') {
            atts.values = parseIdentifierList(true);
            atts.type = NMTOKEN;
            return;
        }
        if (!parseIdentifier(false)) {
            error("invalid", "attribute value");
            return;
        }
        atts.type = AttributeList.name2type(getString(0));
        skipParameterSpace();
        if (atts.type == NOTATION) {
            atts.values = parseIdentifierList(true);
        }
    }

    /**
     * Parse an attribute value specification.
     * [33] 331:1
     */
    @SuppressWarnings("fallthrough")
    String parseAttributeValueSpecification() throws IOException {
        int delim = -1;
        switch (ch) {
          case '\'':
          case '"':
            delim = ch;
            ch = in.read();
        }
        while (true) {
            switch (ch) {
              case -1:
                error("eof.arg", "attribute value");
                return getString(0);

              case '&':
                parseEntityReference();
                break;

              case ' ':
              case '\t':
              case '\n':
                if (delim == -1) {
                    return getString(0);
                }
                addString(' ');
                ch = in.read();
                break;

              case '\'':
              case '"':
                if (delim == ch) {
                    ch = in.read();
                    return getString(0);
                }
                /* fall through */

              default:
                addString(ch & 0xFF);
                ch = in.read();
                break;
            }
        }
    }

    /**
     * Parse an attribute default value.
     * [147] 425:1
     */
    void parseAttributeDefaultValue(AttributeList atts) throws IOException {
        if (ch == '#') {
            ch = in.read();
            if (!parseIdentifier(true)) {
                error("invalid", "attribute value");
                return;
            }
            skipParameterSpace();
            atts.modifier = AttributeList.name2type(getString(0));
            if (atts.modifier != FIXED) {
                return;
            }
        }
        atts.value = parseAttributeValueSpecification();
        skipParameterSpace();
    }

    /**
     * Parse an attribute definition list declaration.
     * [141] 420:15
     * REMIND: associated notation name
     */
    void parseAttlistDeclaration() throws IOException {
        Vector<String> elems = parseIdentifierList(true);
        AttributeList attlist = null, atts = null;

        while (parseIdentifier(true)) {
            if (atts == null) {
                attlist = atts = new AttributeList(getString(0));
            } else {
                atts.next = new AttributeList(getString(0));
                atts = atts.next;
            }
            skipParameterSpace();
            parseAttributeDeclaredValue(atts);
            parseAttributeDefaultValue(atts);

            if ((atts.modifier == IMPLIED) && (atts.values != null) && (atts.values.size() == 1)) {
                atts.value = (String)atts.values.elementAt(0);
            }
        }

        expect('>');

        if (in.replace == 0) {
            for (Enumeration<String> e = elems.elements() ; e.hasMoreElements() ;) {
                dtd.defineAttributes(e.nextElement(), attlist);
            }
        }
    }

    /**
     * Parse an ignored section until ]]> is encountered.
     */
    void parseIgnoredSection() throws IOException {
        int depth = 1;
        in.replace++;
        while (true) {
            switch (ch) {
              case '<':
                if ((ch = in.read()) == '!') {
                    if ((ch = in.read()) == '[') {
                        ch = in.read();
                        depth++;
                    }
                }
                break;
              case ']':
                if ((ch = in.read()) == ']') {
                    if ((ch = in.read()) == '>') {
                        ch = in.read();
                        if (--depth == 0) {
                            in.replace--;
                            return;
                        }
                    }
                }
                break;
              case -1:
                error("eof");
                in.replace--;
                return;

              default:
                ch = in.read();
                break;
            }
        }
    }

    /**
     * Parse a marked section declaration.
     * [93] 391:13
     * REMIND: deal with all status keywords
     */
    void parseMarkedSectionDeclaration() throws IOException {
        ch = in.read();
        skipSpace();
        if (!parseIdentifier(true)) {
            error("expected", "section status keyword");
            return;
        }
        String str = getString(0);
        skipSpace();
        expect('[');
        if ("ignore".equals(str)) {
            parseIgnoredSection();
        } else {
            if (!"include".equals(str)) {
                error("invalid.arg", "section status keyword", str);
            }
            parseSection();
            expect(']');
            expect(']');
            expect('>');
        }
    }

    /**
     * Parse an external identifier
     * [73] 379:1
     */
    void parseExternalIdentifier() throws IOException {
        if (parseIdentifier(false)) {
            String id = getString(0);
            skipParameterSpace();

            if (id.equals("PUBLIC")) {
                if ((ch == '\'') || (ch == '"')) {
                    parseAttributeValueSpecification();
                } else {
                    error("expected", "public identifier");
                }
                skipParameterSpace();
            } else if (!id.equals("SYSTEM")) {
                error("invalid", "external identifier");
            }
            if ((ch == '\'') || (ch == '"')) {
                parseAttributeValueSpecification();
            }
            skipParameterSpace();
        }
    }

    /**
     * Parse document type declaration.
     * [110] 403:1
     */
    void parseDocumentTypeDeclaration() throws IOException {
        skipParameterSpace();
        if (!parseIdentifier(true)) {
            error("expected", "identifier");
        } else {
            skipParameterSpace();
        }
        strpos = 0;
        parseExternalIdentifier();

        if (ch == '[') {
            ch = in.read();
            parseSection();
            expect(']');
            skipParameterSpace();
        }
        expect('>');
    }

    /**
     * Parse a section of the input upto EOF or ']'.
     */
    @SuppressWarnings("fallthrough")
    void parseSection() throws IOException {
        while (true) {
            switch (ch) {
              case ']':
                return;

              case '<':
                switch (ch = in.read()) {
                  case '!':
                    switch (ch = in.read()) {
                      case '[':
                        parseMarkedSectionDeclaration();
                        break;

                      case '-':
                        skipParameterSpace();
                        expect('>');
                        break;

                      default:
                        if (parseIdentifier(true)) {
                            String str = getString(0);

                            if (str.equals("element")) {
                                parseElementDeclaration();

                            } else if (str.equals("entity")) {
                                parseEntityDeclaration();

                            } else if (str.equals("attlist")) {
                                parseAttlistDeclaration();

                            } else if (str.equals("doctype")) {
                                parseDocumentTypeDeclaration();

                            } else if (str.equals("usemap")) {
                                error("ignoring", "usemap");
                                while ((ch != -1) && (ch != '>')) {
                                    ch = in.read();
                                }
                                expect('>');
                            } else if (str.equals("shortref")) {
                                error("ignoring", "shortref");
                                while ((ch != -1) && (ch != '>')) {
                                    ch = in.read();
                                }
                                expect('>');
                            } else if (str.equals("notation")) {
                                error("ignoring", "notation");
                                while ((ch != -1) && (ch != '>')) {
                                    ch = in.read();
                                }
                                expect('>');
                            } else {
                                error("markup");
                            }
                        } else {
                            error("markup");
                            while ((ch != -1) && (ch != '>')) {
                                ch = in.read();
                            }
                            expect('>');
                        }
                    }
                }
                break;

              case -1:
                return;

              default:
                char str[] = {(char)ch};
                error("invalid.arg", "character", "'" + new String(str) + "' / " + ch);
                /* fall through */

              case ' ':
              case '\t':
              case '\n':
                ch = in.read();
                break;
            }
        }
    }

    /**
     * Parse a DTD.
     * @return the dtd or null if an error occurred.
     */
    DTD parse(InputStream in, DTDBuilder dtd) {
        try {
            this.dtd = dtd;
            this.in = new DTDInputStream(in, dtd);

            ch = this.in.read();
            parseSection();

            if (ch != -1) {
                error("premature");
            }
        } catch (IOException e) {
            error("ioexception");
        } catch (Exception e) {
            error("exception", e.getClass().getName(), e.getMessage());
            e.printStackTrace();
        } catch (ThreadDeath e) {
            error("terminated");
        }
        return (nerrors > 0) ? null : dtd;
    }
}
