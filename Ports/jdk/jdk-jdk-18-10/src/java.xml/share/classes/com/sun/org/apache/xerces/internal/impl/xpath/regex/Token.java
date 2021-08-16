/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl.xpath.regex;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * This class represents a node in parse tree.
 *
 * @xerces.internal
 * @LastModified: May 2018
 */
class Token implements java.io.Serializable {

    private static final long serialVersionUID = 8484976002585487481L;

    static final boolean COUNTTOKENS = true;
    static int tokens = 0;

    static final int CHAR = 0;                  // Literal char
    static final int DOT = 11;                  // .
    static final int CONCAT = 1;                // XY
    static final int UNION = 2;                 // X|Y|Z
    static final int CLOSURE = 3;               // X*
    static final int RANGE = 4;                 // [a-zA-Z] etc.
    static final int NRANGE = 5;                // [^a-zA-Z] etc.
    static final int PAREN = 6;                 // (X) or (?:X)
    static final int EMPTY = 7;                 //
    static final int ANCHOR = 8;                // ^ $ \b \B \< \> \A \Z \z
    static final int NONGREEDYCLOSURE = 9;      // *? +?
    static final int STRING = 10;               // strings
    static final int BACKREFERENCE = 12;        // back references
    static final int LOOKAHEAD = 20;            // (?=...)
    static final int NEGATIVELOOKAHEAD = 21;    // (?!...)
    static final int LOOKBEHIND = 22;           // (?<=...)
    static final int NEGATIVELOOKBEHIND = 23;   // (?<!...)
    static final int INDEPENDENT = 24;          // (?>...)
    static final int MODIFIERGROUP = 25;        // (?ims-ims:...)
    static final int CONDITION = 26;            // (?(...)yes|no)

    static final int UTF16_MAX = 0x10ffff;

    final int type;

    static Token token_dot;
    static Token token_0to9;
    static Token token_wordchars;
    static Token token_not_0to9;
    static Token token_not_wordchars;
    static Token token_spaces;
    static Token token_not_spaces;
    static Token token_empty;
    static Token token_linebeginning;
    static Token token_linebeginning2;
    static Token token_lineend;
    static Token token_stringbeginning;
    static Token token_stringend;
    static Token token_stringend2;
    static Token token_wordedge;
    static Token token_not_wordedge;
    static Token token_wordbeginning;
    static Token token_wordend;
    static {
        Token.token_empty = new Token(Token.EMPTY);

        Token.token_linebeginning = Token.createAnchor('^');
        Token.token_linebeginning2 = Token.createAnchor('@');
        Token.token_lineend = Token.createAnchor('$');
        Token.token_stringbeginning = Token.createAnchor('A');
        Token.token_stringend = Token.createAnchor('z');
        Token.token_stringend2 = Token.createAnchor('Z');
        Token.token_wordedge = Token.createAnchor('b');
        Token.token_not_wordedge = Token.createAnchor('B');
        Token.token_wordbeginning = Token.createAnchor('<');
        Token.token_wordend = Token.createAnchor('>');

        Token.token_dot = new Token(Token.DOT);

        Token.token_0to9 = Token.createRange();
        Token.token_0to9.addRange('0', '9');
        Token.token_wordchars = Token.createRange();
        Token.token_wordchars.addRange('0', '9');
        Token.token_wordchars.addRange('A', 'Z');
        Token.token_wordchars.addRange('_', '_');
        Token.token_wordchars.addRange('a', 'z');
        Token.token_spaces = Token.createRange();
        Token.token_spaces.addRange('\t', '\t');
        Token.token_spaces.addRange('\n', '\n');
        Token.token_spaces.addRange('\f', '\f');
        Token.token_spaces.addRange('\r', '\r');
        Token.token_spaces.addRange(' ', ' ');

        Token.token_not_0to9 = Token.complementRanges(Token.token_0to9);
        Token.token_not_wordchars = Token.complementRanges(Token.token_wordchars);
        Token.token_not_spaces = Token.complementRanges(Token.token_spaces);
    }

    static Token.ParenToken createLook(int type, Token child) {
        if (COUNTTOKENS)  Token.tokens ++;
        return new Token.ParenToken(type, child, 0);
    }
    static Token.ParenToken createParen(Token child, int pnumber) {
        if (COUNTTOKENS)  Token.tokens ++;
        return new Token.ParenToken(Token.PAREN, child, pnumber);
    }
    static Token.ClosureToken createClosure(Token tok) {
        if (COUNTTOKENS)  Token.tokens ++;
        return new Token.ClosureToken(Token.CLOSURE, tok);
    }
    static Token.ClosureToken createNGClosure(Token tok) {
        if (COUNTTOKENS)  Token.tokens ++;
        return new Token.ClosureToken(Token.NONGREEDYCLOSURE, tok);
    }
    static Token.ConcatToken createConcat(Token tok1, Token tok2) {
        if (COUNTTOKENS)  Token.tokens ++;
        return new Token.ConcatToken(tok1, tok2);
    }
    static Token.UnionToken createConcat() {
        if (COUNTTOKENS)  Token.tokens ++;
        return new Token.UnionToken(Token.CONCAT); // *** It is not a bug.
    }
    static Token.UnionToken createUnion() {
        if (COUNTTOKENS)  Token.tokens ++;
        return new Token.UnionToken(Token.UNION);
    }
    static Token createEmpty() {
        return Token.token_empty;
    }
    static RangeToken createRange() {
        if (COUNTTOKENS)  Token.tokens ++;
        return new RangeToken(Token.RANGE);
    }
    static RangeToken createNRange() {
        if (COUNTTOKENS)  Token.tokens ++;
        return new RangeToken(Token.NRANGE);
    }
    static Token.CharToken createChar(int ch) {
        if (COUNTTOKENS)  Token.tokens ++;
        return new Token.CharToken(Token.CHAR, ch);
    }
    static private Token.CharToken createAnchor(int ch) {
        if (COUNTTOKENS)  Token.tokens ++;
        return new Token.CharToken(Token.ANCHOR, ch);
    }
    static Token.StringToken createBackReference(int refno) {
        if (COUNTTOKENS)  Token.tokens ++;
        return new Token.StringToken(Token.BACKREFERENCE, null, refno);
    }
    static Token.StringToken createString(String str) {
        if (COUNTTOKENS)  Token.tokens ++;
        return new Token.StringToken(Token.STRING, str, 0);
    }
    static Token.ModifierToken createModifierGroup(Token child, int add, int mask) {
        if (COUNTTOKENS)  Token.tokens ++;
        return new Token.ModifierToken(child, add, mask);
    }
    static Token.ConditionToken createCondition(int refno, Token condition,
                                                Token yespat, Token nopat) {
        if (COUNTTOKENS)  Token.tokens ++;
        return new Token.ConditionToken(refno, condition, yespat, nopat);
    }

    protected Token(int type) {
        this.type = type;
    }

    /**
     * A number of children.
     */
    int size() {
        return 0;
    }
    Token getChild(int index) {
        return null;
    }
    void addChild(Token tok) {
        throw new RuntimeException("Not supported.");
    }

                                                // for RANGE or NRANGE
    protected void addRange(int start, int end) {
        throw new RuntimeException("Not supported.");
    }
    protected void sortRanges() {
        throw new RuntimeException("Not supported.");
    }
    protected void compactRanges() {
        throw new RuntimeException("Not supported.");
    }
    protected void mergeRanges(Token tok) {
        throw new RuntimeException("Not supported.");
    }
    protected void subtractRanges(Token tok) {
        throw new RuntimeException("Not supported.");
    }
    protected void intersectRanges(Token tok) {
        throw new RuntimeException("Not supported.");
    }
    static Token complementRanges(Token tok) {
        return RangeToken.complementRanges(tok);
    }


    void setMin(int min) {                      // for CLOSURE
    }
    void setMax(int max) {                      // for CLOSURE
    }
    int getMin() {                              // for CLOSURE
        return -1;
    }
    int getMax() {                              // for CLOSURE
        return -1;
    }
    int getReferenceNumber() {                  // for STRING
        return 0;
    }
    String getString() {                        // for STRING
        return null;
    }

    int getParenNumber() {
        return 0;
    }
    int getChar() {
        return -1;
    }

    public String toString() {
        return this.toString(0);
    }
    public String toString(int options) {
        return this.type == Token.DOT ? "." : "";
    }

    /**
     * How many characters are needed?
     */
    final int getMinLength() {
        switch (this.type) {
          case CONCAT:
            int sum = 0;
            for (int i = 0;  i < this.size();  i ++)
                sum += this.getChild(i).getMinLength();
            return sum;

          case CONDITION:
          case UNION:
            if (this.size() == 0)
                return 0;
            int ret = this.getChild(0).getMinLength();
            for (int i = 1;  i < this.size();  i ++) {
                int min = this.getChild(i).getMinLength();
                if (min < ret)  ret = min;
            }
            return ret;

          case CLOSURE:
          case NONGREEDYCLOSURE:
            if (this.getMin() >= 0)
                return this.getMin() * this.getChild(0).getMinLength();
            return 0;

          case EMPTY:
          case ANCHOR:
            return 0;

          case DOT:
          case CHAR:
          case RANGE:
          case NRANGE:
            return 1;

          case INDEPENDENT:
          case PAREN:
          case MODIFIERGROUP:
            return this.getChild(0).getMinLength();

          case BACKREFERENCE:
            return 0;                           // *******

          case STRING:
            return this.getString().length();

          case LOOKAHEAD:
          case NEGATIVELOOKAHEAD:
          case LOOKBEHIND:
          case NEGATIVELOOKBEHIND:
            return 0;                           // ***** Really?

          default:
            throw new RuntimeException("Token#getMinLength(): Invalid Type: "+this.type);
        }
    }

    final int getMaxLength() {
        switch (this.type) {
          case CONCAT:
            int sum = 0;
            for (int i = 0;  i < this.size();  i ++) {
                int d = this.getChild(i).getMaxLength();
                if (d < 0)  return -1;
                sum += d;
            }
            return sum;

          case CONDITION:
          case UNION:
            if (this.size() == 0)
                return 0;
            int ret = this.getChild(0).getMaxLength();
            for (int i = 1;  ret >= 0 && i < this.size();  i ++) {
                int max = this.getChild(i).getMaxLength();
                if (max < 0) {                  // infinity
                    ret = -1;
                    break;
                }
                if (max > ret)  ret = max;
            }
            return ret;

          case CLOSURE:
          case NONGREEDYCLOSURE:
            if (this.getMax() >= 0)
                                                // When this.child.getMaxLength() < 0,
                                                // this returns minus value
                return this.getMax() * this.getChild(0).getMaxLength();
            return -1;

          case EMPTY:
          case ANCHOR:
            return 0;

          case CHAR:
            return 1;
          case DOT:
          case RANGE:
          case NRANGE:
            return 2;

          case INDEPENDENT:
          case PAREN:
          case MODIFIERGROUP:
            return this.getChild(0).getMaxLength();

          case BACKREFERENCE:
            return -1;                          // ******

          case STRING:
            return this.getString().length();

          case LOOKAHEAD:
          case NEGATIVELOOKAHEAD:
          case LOOKBEHIND:
          case NEGATIVELOOKBEHIND:
            return 0;                           // ***** Really?

          default:
            throw new RuntimeException("Token#getMaxLength(): Invalid Type: "+this.type);
        }
    }

    static final int FC_CONTINUE = 0;
    static final int FC_TERMINAL = 1;
    static final int FC_ANY = 2;
    private static final boolean isSet(int options, int flag) {
        return (options & flag) == flag;
    }
    final int analyzeFirstCharacter(RangeToken result, int options) {
        switch (this.type) {
          case CONCAT:
            int ret = FC_CONTINUE;
            for (int i = 0;  i < this.size();  i ++)
                if ((ret = this.getChild(i).analyzeFirstCharacter(result, options)) != FC_CONTINUE)
                    break;
            return ret;

          case UNION:
            if (this.size() == 0)
                return FC_CONTINUE;
            /*
             *  a|b|c -> FC_TERMINAL
             *  a|.|c -> FC_ANY
             *  a|b|  -> FC_CONTINUE
             */
            int ret2 = FC_CONTINUE;
            boolean hasEmpty = false;
            for (int i = 0;  i < this.size();  i ++) {
                ret2 = this.getChild(i).analyzeFirstCharacter(result, options);
                if (ret2 == FC_ANY)
                    break;
                else if (ret2 == FC_CONTINUE)
                    hasEmpty = true;
            }
            return hasEmpty ? FC_CONTINUE : ret2;

          case CONDITION:
            int ret3 = this.getChild(0).analyzeFirstCharacter(result, options);
            if (this.size() == 1)  return FC_CONTINUE;
            if (ret3 == FC_ANY)  return ret3;
            int ret4 = this.getChild(1).analyzeFirstCharacter(result, options);
            if (ret4 == FC_ANY)  return ret4;
            return ret3 == FC_CONTINUE || ret4 == FC_CONTINUE ? FC_CONTINUE : FC_TERMINAL;

          case CLOSURE:
          case NONGREEDYCLOSURE:
            this.getChild(0).analyzeFirstCharacter(result, options);
            return FC_CONTINUE;

          case EMPTY:
          case ANCHOR:
            return FC_CONTINUE;

          case CHAR:
            int ch = this.getChar();
            result.addRange(ch, ch);
            if (ch < 0x10000 && isSet(options, RegularExpression.IGNORE_CASE)) {
                ch = Character.toUpperCase((char)ch);
                result.addRange(ch, ch);
                ch = Character.toLowerCase((char)ch);
                result.addRange(ch, ch);
            }
            return FC_TERMINAL;

          case DOT:
              return FC_ANY;

          case RANGE:
            result.mergeRanges(this);
            return FC_TERMINAL;

          case NRANGE:                          // ****
            result.mergeRanges(Token.complementRanges(this));
            return FC_TERMINAL;

          case INDEPENDENT:
          case PAREN:
            return this.getChild(0).analyzeFirstCharacter(result, options);

          case MODIFIERGROUP:
            options |= ((ModifierToken)this).getOptions();
            options &= ~((ModifierToken)this).getOptionsMask();
            return this.getChild(0).analyzeFirstCharacter(result, options);

          case BACKREFERENCE:
            result.addRange(0, UTF16_MAX);  // **** We can not optimize.
            return FC_ANY;

          case STRING:
            int cha = this.getString().charAt(0);
            int ch2;
            if (REUtil.isHighSurrogate(cha)
                && this.getString().length() >= 2
                && REUtil.isLowSurrogate((ch2 = this.getString().charAt(1))))
                cha = REUtil.composeFromSurrogates(cha, ch2);
            result.addRange(cha, cha);
            if (cha < 0x10000 && isSet(options, RegularExpression.IGNORE_CASE)) {
                cha = Character.toUpperCase((char)cha);
                result.addRange(cha, cha);
                cha = Character.toLowerCase((char)cha);
                result.addRange(cha, cha);
            }
            return FC_TERMINAL;

          case LOOKAHEAD:
          case NEGATIVELOOKAHEAD:
          case LOOKBEHIND:
          case NEGATIVELOOKBEHIND:
            return FC_CONTINUE;

          default:
            throw new RuntimeException("Token#analyzeHeadCharacter(): Invalid Type: "+this.type);
        }
    }

    private final boolean isShorterThan(Token tok) {
        if (tok == null)  return false;
        /*
        int mylength;
        if (this.type == STRING)  mylength = this.getString().length();
        else if (this.type == CHAR)  mylength = this.getChar() >= 0x10000 ? 2 : 1;
        else throw new RuntimeException("Internal Error: Illegal type: "+this.type);
        int otherlength;
        if (tok.type == STRING)  otherlength = tok.getString().length();
        else if (tok.type == CHAR)  otherlength = tok.getChar() >= 0x10000 ? 2 : 1;
        else throw new RuntimeException("Internal Error: Illegal type: "+tok.type);
        */
        int mylength;
        if (this.type == STRING)  mylength = this.getString().length();
        else throw new RuntimeException("Internal Error: Illegal type: "+this.type);
        int otherlength;
        if (tok.type == STRING)  otherlength = tok.getString().length();
        else throw new RuntimeException("Internal Error: Illegal type: "+tok.type);
        return mylength < otherlength;
    }

    static class FixedStringContainer {
        Token token = null;
        int options = 0;
        FixedStringContainer() {
        }
    }

    final void findFixedString(FixedStringContainer container, int options) {
        switch (this.type) {
          case CONCAT:
            Token prevToken = null;
            int prevOptions = 0;
            for (int i = 0;  i < this.size();  i ++) {
                this.getChild(i).findFixedString(container, options);
                if (prevToken == null || prevToken.isShorterThan(container.token)) {
                    prevToken = container.token;
                    prevOptions = container.options;
                }
            }
            container.token = prevToken;
            container.options = prevOptions;
            return;

          case UNION:
          case CLOSURE:
          case NONGREEDYCLOSURE:
          case EMPTY:
          case ANCHOR:
          case RANGE:
          case DOT:
          case NRANGE:
          case BACKREFERENCE:
          case LOOKAHEAD:
          case NEGATIVELOOKAHEAD:
          case LOOKBEHIND:
          case NEGATIVELOOKBEHIND:
          case CONDITION:
            container.token = null;
            return;

          case CHAR:                            // Ignore CHAR tokens.
            container.token = null;             // **
            return;                             // **

          case STRING:
            container.token = this;
            container.options = options;
            return;

          case INDEPENDENT:
          case PAREN:
            this.getChild(0).findFixedString(container, options);
            return;

          case MODIFIERGROUP:
            options |= ((ModifierToken)this).getOptions();
            options &= ~((ModifierToken)this).getOptionsMask();
            this.getChild(0).findFixedString(container, options);
            return;

          default:
            throw new RuntimeException("Token#findFixedString(): Invalid Type: "+this.type);
        }
    }

    boolean match(int ch) {
        throw new RuntimeException("NFAArrow#match(): Internal error: "+this.type);
    }

    // ------------------------------------------------------
    private static volatile Map<String, Token> categories = null;
    private static volatile Map<String, Token> categories2 = null;
    private static final Object lock = new Object();
    private static final String[] categoryNames = {
        "Cn", "Lu", "Ll", "Lt", "Lm", "Lo", "Mn", "Me", "Mc", "Nd",
        "Nl", "No", "Zs", "Zl", "Zp", "Cc", "Cf", null, "Co", "Cs",
        "Pd", "Ps", "Pe", "Pc", "Po", "Sm", "Sc", "Sk", "So", // 28
        "Pi", "Pf",  // 29, 30
        "L", "M", "N", "Z", "C", "P", "S",      // 31-37
    };

    // Schema Rec. {Datatypes} - Punctuation
    static final int CHAR_INIT_QUOTE  = 29;     // Pi - initial quote
    static final int CHAR_FINAL_QUOTE = 30;     // Pf - final quote
    static final int CHAR_LETTER = 31;
    static final int CHAR_MARK = 32;
    static final int CHAR_NUMBER = 33;
    static final int CHAR_SEPARATOR = 34;
    static final int CHAR_OTHER = 35;
    static final int CHAR_PUNCTUATION = 36;
    static final int CHAR_SYMBOL = 37;

    //blockNames in UNICODE 3.1 that supported by XML Schema REC
    private static final String[] blockNames = {
        /*0000..007F;*/ "Basic Latin",
        /*0080..00FF;*/ "Latin-1 Supplement",
        /*0100..017F;*/ "Latin Extended-A",
        /*0180..024F;*/ "Latin Extended-B",
        /*0250..02AF;*/ "IPA Extensions",
        /*02B0..02FF;*/ "Spacing Modifier Letters",
        /*0300..036F;*/ "Combining Diacritical Marks",
        /*0370..03FF;*/ "Greek",
        /*0400..04FF;*/ "Cyrillic",
        /*0530..058F;*/ "Armenian",
        /*0590..05FF;*/ "Hebrew",
        /*0600..06FF;*/ "Arabic",
        /*0700..074F;*/ "Syriac",
        /*0780..07BF;*/ "Thaana",
        /*0900..097F;*/ "Devanagari",
        /*0980..09FF;*/ "Bengali",
        /*0A00..0A7F;*/ "Gurmukhi",
        /*0A80..0AFF;*/ "Gujarati",
        /*0B00..0B7F;*/ "Oriya",
        /*0B80..0BFF;*/ "Tamil",
        /*0C00..0C7F;*/ "Telugu",
        /*0C80..0CFF;*/ "Kannada",
        /*0D00..0D7F;*/ "Malayalam",
        /*0D80..0DFF;*/ "Sinhala",
        /*0E00..0E7F;*/ "Thai",
        /*0E80..0EFF;*/ "Lao",
        /*0F00..0FFF;*/ "Tibetan",
        /*1000..109F;*/ "Myanmar",
        /*10A0..10FF;*/ "Georgian",
        /*1100..11FF;*/ "Hangul Jamo",
        /*1200..137F;*/ "Ethiopic",
        /*13A0..13FF;*/ "Cherokee",
        /*1400..167F;*/ "Unified Canadian Aboriginal Syllabics",
        /*1680..169F;*/ "Ogham",
        /*16A0..16FF;*/ "Runic",
        /*1780..17FF;*/ "Khmer",
        /*1800..18AF;*/ "Mongolian",
        /*1E00..1EFF;*/ "Latin Extended Additional",
        /*1F00..1FFF;*/ "Greek Extended",
        /*2000..206F;*/ "General Punctuation",
        /*2070..209F;*/ "Superscripts and Subscripts",
        /*20A0..20CF;*/ "Currency Symbols",
        /*20D0..20FF;*/ "Combining Marks for Symbols",
        /*2100..214F;*/ "Letterlike Symbols",
        /*2150..218F;*/ "Number Forms",
        /*2190..21FF;*/ "Arrows",
        /*2200..22FF;*/ "Mathematical Operators",
        /*2300..23FF;*/ "Miscellaneous Technical",
        /*2400..243F;*/ "Control Pictures",
        /*2440..245F;*/ "Optical Character Recognition",
        /*2460..24FF;*/ "Enclosed Alphanumerics",
        /*2500..257F;*/ "Box Drawing",
        /*2580..259F;*/ "Block Elements",
        /*25A0..25FF;*/ "Geometric Shapes",
        /*2600..26FF;*/ "Miscellaneous Symbols",
        /*2700..27BF;*/ "Dingbats",
        /*2800..28FF;*/ "Braille Patterns",
        /*2E80..2EFF;*/ "CJK Radicals Supplement",
        /*2F00..2FDF;*/ "Kangxi Radicals",
        /*2FF0..2FFF;*/ "Ideographic Description Characters",
        /*3000..303F;*/ "CJK Symbols and Punctuation",
        /*3040..309F;*/ "Hiragana",
        /*30A0..30FF;*/ "Katakana",
        /*3100..312F;*/ "Bopomofo",
        /*3130..318F;*/ "Hangul Compatibility Jamo",
        /*3190..319F;*/ "Kanbun",
        /*31A0..31BF;*/ "Bopomofo Extended",
        /*3200..32FF;*/ "Enclosed CJK Letters and Months",
        /*3300..33FF;*/ "CJK Compatibility",
        /*3400..4DB5;*/ "CJK Unified Ideographs Extension A",
        /*4E00..9FFF;*/ "CJK Unified Ideographs",
        /*A000..A48F;*/ "Yi Syllables",
        /*A490..A4CF;*/ "Yi Radicals",
        /*AC00..D7A3;*/ "Hangul Syllables",
        /*E000..F8FF;*/ "Private Use",
        /*F900..FAFF;*/ "CJK Compatibility Ideographs",
        /*FB00..FB4F;*/ "Alphabetic Presentation Forms",
        /*FB50..FDFF;*/ "Arabic Presentation Forms-A",
        /*FE20..FE2F;*/ "Combining Half Marks",
        /*FE30..FE4F;*/ "CJK Compatibility Forms",
        /*FE50..FE6F;*/ "Small Form Variants",
        /*FE70..FEFE;*/ "Arabic Presentation Forms-B",
        /*FEFF..FEFF;*/ "Specials",
        /*FF00..FFEF;*/ "Halfwidth and Fullwidth Forms",
         //missing Specials add manually
        /*10300..1032F;*/ "Old Italic",         // 84
        /*10330..1034F;*/ "Gothic",
        /*10400..1044F;*/ "Deseret",
        /*1D000..1D0FF;*/ "Byzantine Musical Symbols",
        /*1D100..1D1FF;*/ "Musical Symbols",
        /*1D400..1D7FF;*/ "Mathematical Alphanumeric Symbols",
        /*20000..2A6D6;*/ "CJK Unified Ideographs Extension B",
        /*2F800..2FA1F;*/ "CJK Compatibility Ideographs Supplement",
        /*E0000..E007F;*/ "Tags",
        //missing 2 private use add manually

    };
    //ADD THOSE MANUALLY
    //F0000..FFFFD; "Private Use",
    //100000..10FFFD; "Private Use"
    //FFF0..FFFD; "Specials",
    static final String blockRanges =
       "\u0000\u007F\u0080\u00FF\u0100\u017F\u0180\u024F\u0250\u02AF\u02B0\u02FF\u0300\u036F"
        +"\u0370\u03FF\u0400\u04FF\u0530\u058F\u0590\u05FF\u0600\u06FF\u0700\u074F\u0780\u07BF"
        +"\u0900\u097F\u0980\u09FF\u0A00\u0A7F\u0A80\u0AFF\u0B00\u0B7F\u0B80\u0BFF\u0C00\u0C7F\u0C80\u0CFF"
        +"\u0D00\u0D7F\u0D80\u0DFF\u0E00\u0E7F\u0E80\u0EFF\u0F00\u0FFF\u1000\u109F\u10A0\u10FF\u1100\u11FF"
        +"\u1200\u137F\u13A0\u13FF\u1400\u167F\u1680\u169F\u16A0\u16FF\u1780\u17FF\u1800\u18AF\u1E00\u1EFF"
        +"\u1F00\u1FFF\u2000\u206F\u2070\u209F\u20A0\u20CF\u20D0\u20FF\u2100\u214F\u2150\u218F\u2190\u21FF\u2200\u22FF"
        +"\u2300\u23FF\u2400\u243F\u2440\u245F\u2460\u24FF\u2500\u257F\u2580\u259F\u25A0\u25FF\u2600\u26FF\u2700\u27BF"
        +"\u2800\u28FF\u2E80\u2EFF\u2F00\u2FDF\u2FF0\u2FFF\u3000\u303F\u3040\u309F\u30A0\u30FF\u3100\u312F\u3130\u318F"
        +"\u3190\u319F\u31A0\u31BF\u3200\u32FF\u3300\u33FF\u3400\u4DB5\u4E00\u9FFF\uA000\uA48F\uA490\uA4CF"
        +"\uAC00\uD7A3\uE000\uF8FF\uF900\uFAFF\uFB00\uFB4F\uFB50\uFDFF"
        +"\uFE20\uFE2F\uFE30\uFE4F\uFE50\uFE6F\uFE70\uFEFE\uFEFF\uFEFF\uFF00\uFFEF";
    static final int[] nonBMPBlockRanges = {
        0x10300, 0x1032F,       // 84
        0x10330, 0x1034F,
        0x10400, 0x1044F,
        0x1D000, 0x1D0FF,
        0x1D100, 0x1D1FF,
        0x1D400, 0x1D7FF,
        0x20000, 0x2A6D6,
        0x2F800, 0x2FA1F,
        0xE0000, 0xE007F
    };
    private static final int NONBMP_BLOCK_START = 84;

    static protected RangeToken getRange(String name, boolean positive) {
        // use local variable for better performance
        Map<String, Token> localCat = Token.categories;
        if (localCat == null) {
            synchronized (lock) {
                localCat = Token.categories;
                if (localCat == null) {
                    Map<String, Token> tmpCat = new HashMap<>();
                    Map<String, Token> tmpCat2 = new HashMap<>();

                    Token[] ranges = new Token[Token.categoryNames.length];
                    for (int i = 0;  i < ranges.length;  i ++) {
                        ranges[i] = Token.createRange();
                    }
                    int type;
                    for (int i = 0;  i < 0x10000;  i ++) {
                        type = Character.getType((char)i);
                        if (type == Character.START_PUNCTUATION ||
                            type == Character.END_PUNCTUATION) {
                            //build table of Pi values
                            if (i == 0x00AB || i == 0x2018 || i == 0x201B || i == 0x201C ||
                                i == 0x201F || i == 0x2039) {
                                type = CHAR_INIT_QUOTE;
                            }
                            //build table of Pf values
                            if (i == 0x00BB || i == 0x2019 || i == 0x201D || i == 0x203A ) {
                                type = CHAR_FINAL_QUOTE;
                            }
                        }
                        ranges[type].addRange(i, i);
                        switch (type) {
                          case Character.UPPERCASE_LETTER:
                          case Character.LOWERCASE_LETTER:
                          case Character.TITLECASE_LETTER:
                          case Character.MODIFIER_LETTER:
                          case Character.OTHER_LETTER:
                            type = CHAR_LETTER;
                            break;
                          case Character.NON_SPACING_MARK:
                          case Character.COMBINING_SPACING_MARK:
                          case Character.ENCLOSING_MARK:
                            type = CHAR_MARK;
                            break;
                          case Character.DECIMAL_DIGIT_NUMBER:
                          case Character.LETTER_NUMBER:
                          case Character.OTHER_NUMBER:
                            type = CHAR_NUMBER;
                            break;
                          case Character.SPACE_SEPARATOR:
                          case Character.LINE_SEPARATOR:
                          case Character.PARAGRAPH_SEPARATOR:
                            type = CHAR_SEPARATOR;
                            break;
                          case Character.CONTROL:
                          case Character.FORMAT:
                          case Character.SURROGATE:
                          case Character.PRIVATE_USE:
                          case Character.UNASSIGNED:
                            type = CHAR_OTHER;
                            break;
                          case Character.CONNECTOR_PUNCTUATION:
                          case Character.DASH_PUNCTUATION:
                          case Character.START_PUNCTUATION:
                          case Character.END_PUNCTUATION:
                          case CHAR_INIT_QUOTE:
                          case CHAR_FINAL_QUOTE:
                          case Character.OTHER_PUNCTUATION:
                            type = CHAR_PUNCTUATION;
                            break;
                          case Character.MATH_SYMBOL:
                          case Character.CURRENCY_SYMBOL:
                          case Character.MODIFIER_SYMBOL:
                          case Character.OTHER_SYMBOL:
                            type = CHAR_SYMBOL;
                            break;
                          default:
                            throw new RuntimeException("org.apache.xerces.utils.regex.Token#getRange(): Unknown Unicode category: "+type);
                        }
                        ranges[type].addRange(i, i);
                    } // for all characters
                    ranges[Character.UNASSIGNED].addRange(0x10000, Token.UTF16_MAX);

                    for (int i = 0;  i < ranges.length;  i ++) {
                        if (Token.categoryNames[i] != null) {
                            if (i == Character.UNASSIGNED) { // Unassigned
                                ranges[i].addRange(0x10000, Token.UTF16_MAX);
                            }
                            tmpCat.put(Token.categoryNames[i], ranges[i]);
                            tmpCat2.put(Token.categoryNames[i],
                                                  Token.complementRanges(ranges[i]));
                        }
                    }
                    //REVISIT: do we really need to support block names as in Unicode 3.1
                    //         or we can just create all the names in IsBLOCKNAME format (XML Schema REC)?
                    //
                    StringBuilder buffer = new StringBuilder(50);
                    for (int i = 0;  i < Token.blockNames.length;  i ++) {
                        Token r1 = Token.createRange();
                        int location;
                        if (i < NONBMP_BLOCK_START) {
                            location = i*2;
                            int rstart = Token.blockRanges.charAt(location);
                            int rend = Token.blockRanges.charAt(location+1);
                            //DEBUGING
                            //System.out.println(n+" " +Integer.toHexString(rstart)
                            //                     +"-"+ Integer.toHexString(rend));
                            r1.addRange(rstart, rend);
                        } else {
                            location = (i - NONBMP_BLOCK_START) * 2;
                            r1.addRange(Token.nonBMPBlockRanges[location],
                                        Token.nonBMPBlockRanges[location + 1]);
                        }
                        String n = Token.blockNames[i];
                        if (n.equals("Specials"))
                            r1.addRange(0xfff0, 0xfffd);
                        if (n.equals("Private Use")) {
                            r1.addRange(0xF0000,0xFFFFD);
                            r1.addRange(0x100000,0x10FFFD);
                        }
                        tmpCat.put(n, r1);
                        tmpCat2.put(n, Token.complementRanges(r1));
                        buffer.setLength(0);
                        buffer.append("Is");
                        if (n.indexOf(' ') >= 0) {
                            for (int ci = 0;  ci < n.length();  ci ++)
                                if (n.charAt(ci) != ' ')  buffer.append(n.charAt(ci));
                        }
                        else {
                            buffer.append(n);
                        }
                        Token.setAlias(tmpCat, tmpCat2, buffer.toString(), n, true);
                    }

                    // TR#18 1.2
                    Token.setAlias(tmpCat, tmpCat2, "ASSIGNED", "Cn", false);
                    Token.setAlias(tmpCat, tmpCat2, "UNASSIGNED", "Cn", true);
                    Token all = Token.createRange();
                    all.addRange(0, Token.UTF16_MAX);
                    tmpCat.put("ALL", all);
                    tmpCat2.put("ALL", Token.complementRanges(all));
                    Token.registerNonXS("ASSIGNED");
                    Token.registerNonXS("UNASSIGNED");
                    Token.registerNonXS("ALL");

                    Token isalpha = Token.createRange();
                    isalpha.mergeRanges(ranges[Character.UPPERCASE_LETTER]); // Lu
                    isalpha.mergeRanges(ranges[Character.LOWERCASE_LETTER]); // Ll
                    isalpha.mergeRanges(ranges[Character.OTHER_LETTER]); // Lo
                    tmpCat.put("IsAlpha", isalpha);
                    tmpCat2.put("IsAlpha", Token.complementRanges(isalpha));
                    Token.registerNonXS("IsAlpha");

                    Token isalnum = Token.createRange();
                    isalnum.mergeRanges(isalpha);   // Lu Ll Lo
                    isalnum.mergeRanges(ranges[Character.DECIMAL_DIGIT_NUMBER]); // Nd
                    tmpCat.put("IsAlnum", isalnum);
                    tmpCat2.put("IsAlnum", Token.complementRanges(isalnum));
                    Token.registerNonXS("IsAlnum");

                    Token isspace = Token.createRange();
                    isspace.mergeRanges(Token.token_spaces);
                    isspace.mergeRanges(ranges[CHAR_SEPARATOR]); // Z
                    tmpCat.put("IsSpace", isspace);
                    tmpCat2.put("IsSpace", Token.complementRanges(isspace));
                    Token.registerNonXS("IsSpace");

                    Token isword = Token.createRange();
                    isword.mergeRanges(isalnum);     // Lu Ll Lo Nd
                    isword.addRange('_', '_');
                    tmpCat.put("IsWord", isword);
                    tmpCat2.put("IsWord", Token.complementRanges(isword));
                    Token.registerNonXS("IsWord");

                    Token isascii = Token.createRange();
                    isascii.addRange(0, 127);
                    tmpCat.put("IsASCII", isascii);
                    tmpCat2.put("IsASCII", Token.complementRanges(isascii));
                    Token.registerNonXS("IsASCII");

                    Token isnotgraph = Token.createRange();
                    isnotgraph.mergeRanges(ranges[CHAR_OTHER]);
                    isnotgraph.addRange(' ', ' ');
                    tmpCat.put("IsGraph", Token.complementRanges(isnotgraph));
                    tmpCat2.put("IsGraph", isnotgraph);
                    Token.registerNonXS("IsGraph");

                    Token isxdigit = Token.createRange();
                    isxdigit.addRange('0', '9');
                    isxdigit.addRange('A', 'F');
                    isxdigit.addRange('a', 'f');
                    tmpCat.put("IsXDigit", Token.complementRanges(isxdigit));
                    tmpCat2.put("IsXDigit", isxdigit);
                    Token.registerNonXS("IsXDigit");

                    Token.setAlias(tmpCat, tmpCat2, "IsDigit", "Nd", true);
                    Token.setAlias(tmpCat, tmpCat2, "IsUpper", "Lu", true);
                    Token.setAlias(tmpCat, tmpCat2, "IsLower", "Ll", true);
                    Token.setAlias(tmpCat, tmpCat2, "IsCntrl", "C", true);
                    Token.setAlias(tmpCat, tmpCat2, "IsPrint", "C", false);
                    Token.setAlias(tmpCat, tmpCat2, "IsPunct", "P", true);
                    Token.registerNonXS("IsDigit");
                    Token.registerNonXS("IsUpper");
                    Token.registerNonXS("IsLower");
                    Token.registerNonXS("IsCntrl");
                    Token.registerNonXS("IsPrint");
                    Token.registerNonXS("IsPunct");

                    Token.setAlias(tmpCat, tmpCat2, "alpha", "IsAlpha", true);
                    Token.setAlias(tmpCat, tmpCat2, "alnum", "IsAlnum", true);
                    Token.setAlias(tmpCat, tmpCat2, "ascii", "IsASCII", true);
                    Token.setAlias(tmpCat, tmpCat2, "cntrl", "IsCntrl", true);
                    Token.setAlias(tmpCat, tmpCat2, "digit", "IsDigit", true);
                    Token.setAlias(tmpCat, tmpCat2, "graph", "IsGraph", true);
                    Token.setAlias(tmpCat, tmpCat2, "lower", "IsLower", true);
                    Token.setAlias(tmpCat, tmpCat2, "print", "IsPrint", true);
                    Token.setAlias(tmpCat, tmpCat2, "punct", "IsPunct", true);
                    Token.setAlias(tmpCat, tmpCat2, "space", "IsSpace", true);
                    Token.setAlias(tmpCat, tmpCat2, "upper", "IsUpper", true);
                    Token.setAlias(tmpCat, tmpCat2, "word", "IsWord", true); // Perl extension
                    Token.setAlias(tmpCat, tmpCat2, "xdigit", "IsXDigit", true);
                    Token.registerNonXS("alpha");
                    Token.registerNonXS("alnum");
                    Token.registerNonXS("ascii");
                    Token.registerNonXS("cntrl");
                    Token.registerNonXS("digit");
                    Token.registerNonXS("graph");
                    Token.registerNonXS("lower");
                    Token.registerNonXS("print");
                    Token.registerNonXS("punct");
                    Token.registerNonXS("space");
                    Token.registerNonXS("upper");
                    Token.registerNonXS("word");
                    Token.registerNonXS("xdigit");
                    Token.categories = localCat = Collections.unmodifiableMap(tmpCat);
                    Token.categories2 = Collections.unmodifiableMap(tmpCat2);
                } // localCat == null
            } // synchronized
        } // if null
        return positive ? (RangeToken)localCat.get(name)
            : (RangeToken)Token.categories2.get(name);
    }
    static protected RangeToken getRange(String name, boolean positive, boolean xs) {
        RangeToken range = Token.getRange(name, positive);
        if (xs && range != null && Token.isRegisterNonXS(name))
            range = null;
        return range;
    }

    static final Set<String> nonxs = Collections.synchronizedSet(new HashSet<>());
    /**
     * This method is called by only getRange().
     * So this method need not MT-safe.
     */
    static protected void registerNonXS(String name) {
        Token.nonxs.add(name);
    }

    static protected boolean isRegisterNonXS(String name) {
        return Token.nonxs.contains(name);
    }

    private static void setAlias(Map<String, Token> tmpCat, Map<String, Token> tmpCat2,
            String newName, String name, boolean positive) {
        Token t1 = tmpCat.get(name);
        Token t2 = tmpCat2.get(name);
        if (positive) {
            tmpCat.put(newName, t1);
            tmpCat2.put(newName, t2);
        } else {
            tmpCat2.put(newName, t1);
            tmpCat.put(newName, t2);
        }
    }

    // ------------------------------------------------------

    static final String viramaString =
    "\u094D"// ;DEVANAGARI SIGN VIRAMA;Mn;9;ON;;;;;N;;;;;
    +"\u09CD"//;BENGALI SIGN VIRAMA;Mn;9;ON;;;;;N;;;;;
    +"\u0A4D"//;GURMUKHI SIGN VIRAMA;Mn;9;ON;;;;;N;;;;;
    +"\u0ACD"//;GUJARATI SIGN VIRAMA;Mn;9;ON;;;;;N;;;;;
    +"\u0B4D"//;ORIYA SIGN VIRAMA;Mn;9;ON;;;;;N;;;;;
    +"\u0BCD"//;TAMIL SIGN VIRAMA;Mn;9;ON;;;;;N;;;;;
    +"\u0C4D"//;TELUGU SIGN VIRAMA;Mn;9;ON;;;;;N;;;;;
    +"\u0CCD"//;KANNADA SIGN VIRAMA;Mn;9;ON;;;;;N;;;;;
    +"\u0D4D"//;MALAYALAM SIGN VIRAMA;Mn;9;ON;;;;;N;;;;;
    +"\u0E3A"//;THAI CHARACTER PHINTHU;Mn;9;ON;;;;;N;THAI VOWEL SIGN PHINTHU;;;;
    +"\u0F84";//;TIBETAN MARK HALANTA;Mn;9;ON;;;;;N;TIBETAN VIRAMA;;;;

    static private Token token_grapheme = null;
    static synchronized Token getGraphemePattern() {
        if (Token.token_grapheme != null)
            return Token.token_grapheme;

        Token base_char = Token.createRange();  // [{ASSIGNED}]-[{M},{C}]
        base_char.mergeRanges(Token.getRange("ASSIGNED", true));
        base_char.subtractRanges(Token.getRange("M", true));
        base_char.subtractRanges(Token.getRange("C", true));

        Token virama = Token.createRange();
        for (int i = 0;  i < Token.viramaString.length(); i++) {
            virama.addRange(i, i);
        }

        Token combiner_wo_virama = Token.createRange();
        combiner_wo_virama.mergeRanges(Token.getRange("M", true));
        combiner_wo_virama.addRange(0x1160, 0x11ff); // hangul_medial and hangul_final
        combiner_wo_virama.addRange(0xff9e, 0xff9f); // extras

        Token left = Token.createUnion();       // base_char?
        left.addChild(base_char);
        left.addChild(Token.token_empty);

        Token foo = Token.createUnion();
        foo.addChild(Token.createConcat(virama, Token.getRange("L", true)));
        foo.addChild(combiner_wo_virama);

        foo = Token.createClosure(foo);

        foo = Token.createConcat(left, foo);

        Token.token_grapheme = foo;
        return Token.token_grapheme;
    }

    /**
     * Combing Character Sequence in Perl 5.6.
     */
    static private Token token_ccs = null;
    static synchronized Token getCombiningCharacterSequence() {
        if (Token.token_ccs != null)
            return Token.token_ccs;

        Token foo = Token.createClosure(Token.getRange("M", true)); // \pM*
        foo = Token.createConcat(Token.getRange("M", false), foo); // \PM + \pM*
        Token.token_ccs = foo;
        return Token.token_ccs;
    }

    // ------------------------------------------------------

    // ------------------------------------------------------
    /**
     * This class represents a node in parse tree.
     */
    static class StringToken extends Token implements java.io.Serializable {

        private static final long serialVersionUID = -4614366944218504172L;

        String string;
        final int refNumber;

        StringToken(int type, String str, int n) {
            super(type);
            this.string = str;
            this.refNumber = n;
        }

        int getReferenceNumber() {              // for STRING
            return this.refNumber;
        }
        String getString() {                    // for STRING
            return this.string;
        }

        public String toString(int options) {
            if (this.type == BACKREFERENCE)
                return "\\"+this.refNumber;
            else
                return REUtil.quoteMeta(this.string);
        }
    }

    /**
     * This class represents a node in parse tree.
     */
    static class ConcatToken extends Token implements java.io.Serializable {

        private static final long serialVersionUID = 8717321425541346381L;

        final Token child;
        final Token child2;

        ConcatToken(Token t1, Token t2) {
            super(Token.CONCAT);
            this.child = t1;
            this.child2 = t2;
        }

        int size() {
            return 2;
        }
        Token getChild(int index) {
            return index == 0 ? this.child : this.child2;
        }

        public String toString(int options) {
            String ret;
            if (this.child2.type == CLOSURE && this.child2.getChild(0) == this.child) {
                ret = this.child.toString(options)+"+";
            } else if (this.child2.type == NONGREEDYCLOSURE && this.child2.getChild(0) == this.child) {
                ret = this.child.toString(options)+"+?";
            } else
                ret = this.child.toString(options)+this.child2.toString(options);
            return ret;
        }
    }

    /**
     * This class represents a node in parse tree.
     */
    static class CharToken extends Token implements java.io.Serializable {

        private static final long serialVersionUID = -4394272816279496989L;

        final int chardata;

        CharToken(int type, int ch) {
            super(type);
            this.chardata = ch;
        }

        int getChar() {
            return this.chardata;
        }

        public String toString(int options) {
            String ret;
            switch (this.type) {
              case CHAR:
                switch (this.chardata) {
                  case '|':  case '*':  case '+':  case '?':
                  case '(':  case ')':  case '.':  case '[':
                  case '{':  case '\\':
                    ret = "\\"+(char)this.chardata;
                    break;
                  case '\f':  ret = "\\f";  break;
                  case '\n':  ret = "\\n";  break;
                  case '\r':  ret = "\\r";  break;
                  case '\t':  ret = "\\t";  break;
                  case 0x1b:  ret = "\\e";  break;
                    //case 0x0b:  ret = "\\v";  break;
                  default:
                    if (this.chardata >= 0x10000) {
                        String pre = "0"+Integer.toHexString(this.chardata);
                        ret = "\\v"+pre.substring(pre.length()-6, pre.length());
                    } else
                        ret = ""+(char)this.chardata;
                }
                break;

              case ANCHOR:
                if (this == Token.token_linebeginning || this == Token.token_lineend)
                    ret = ""+(char)this.chardata;
                else
                    ret = "\\"+(char)this.chardata;
                break;

              default:
                ret = null;
            }
            return ret;
        }

        boolean match(int ch) {
            if (this.type == CHAR) {
                return ch == this.chardata;
            } else
                throw new RuntimeException("NFAArrow#match(): Internal error: "+this.type);
        }
    }

    /**
     * This class represents a node in parse tree.
     */
    static class ClosureToken extends Token implements java.io.Serializable {

        private static final long serialVersionUID = 1308971930673997452L;

        int min;
        int max;
        final Token child;

        ClosureToken(int type, Token tok) {
            super(type);
            this.child = tok;
            this.setMin(-1);
            this.setMax(-1);
        }

        int size() {
            return 1;
        }
        Token getChild(int index) {
            return this.child;
        }

        final void setMin(int min) {
            this.min = min;
        }
        final void setMax(int max) {
            this.max = max;
        }
        final int getMin() {
            return this.min;
        }
        final int getMax() {
            return this.max;
        }

        public String toString(int options) {
            String ret;
            if (this.type == CLOSURE) {
                if (this.getMin() < 0 && this.getMax() < 0) {
                    ret = this.child.toString(options)+"*";
                } else if (this.getMin() == this.getMax()) {
                    ret = this.child.toString(options)+"{"+this.getMin()+"}";
                } else if (this.getMin() >= 0 && this.getMax() >= 0) {
                    ret = this.child.toString(options)+"{"+this.getMin()+","+this.getMax()+"}";
                } else if (this.getMin() >= 0 && this.getMax() < 0) {
                    ret = this.child.toString(options)+"{"+this.getMin()+",}";
                } else
                    throw new RuntimeException("Token#toString(): CLOSURE "
                                               +this.getMin()+", "+this.getMax());
            } else {
                if (this.getMin() < 0 && this.getMax() < 0) {
                    ret = this.child.toString(options)+"*?";
                } else if (this.getMin() == this.getMax()) {
                    ret = this.child.toString(options)+"{"+this.getMin()+"}?";
                } else if (this.getMin() >= 0 && this.getMax() >= 0) {
                    ret = this.child.toString(options)+"{"+this.getMin()+","+this.getMax()+"}?";
                } else if (this.getMin() >= 0 && this.getMax() < 0) {
                    ret = this.child.toString(options)+"{"+this.getMin()+",}?";
                } else
                    throw new RuntimeException("Token#toString(): NONGREEDYCLOSURE "
                                               +this.getMin()+", "+this.getMax());
            }
            return ret;
        }
    }

    /**
     * This class represents a node in parse tree.
     */
    static class ParenToken extends Token implements java.io.Serializable {

        private static final long serialVersionUID = -5938014719827987704L;

        final Token child;
        final int parennumber;

        ParenToken(int type, Token tok, int paren) {
            super(type);
            this.child = tok;
            this.parennumber = paren;
        }

        int size() {
            return 1;
        }
        Token getChild(int index) {
            return this.child;
        }

        int getParenNumber() {
            return this.parennumber;
        }

        public String toString(int options) {
            String ret = null;
            switch (this.type) {
              case PAREN:
                if (this.parennumber == 0) {
                    ret = "(?:"+this.child.toString(options)+")";
                } else {
                    ret = "("+this.child.toString(options)+")";
                }
                break;

              case LOOKAHEAD:
                ret = "(?="+this.child.toString(options)+")";
                break;
              case NEGATIVELOOKAHEAD:
                ret = "(?!"+this.child.toString(options)+")";
                break;
              case LOOKBEHIND:
                ret = "(?<="+this.child.toString(options)+")";
                break;
              case NEGATIVELOOKBEHIND:
                ret = "(?<!"+this.child.toString(options)+")";
                break;
              case INDEPENDENT:
                ret = "(?>"+this.child.toString(options)+")";
                break;
            }
            return ret;
        }
    }

    /**
     * (?(condition)yes-pattern|no-pattern)
     */
    static class ConditionToken extends Token implements java.io.Serializable {

        private static final long serialVersionUID = 4353765277910594411L;

        final int refNumber;
        final Token condition;
        final Token yes;
        final Token no;
        ConditionToken(int refno, Token cond, Token yespat, Token nopat) {
            super(Token.CONDITION);
            this.refNumber = refno;
            this.condition = cond;
            this.yes = yespat;
            this.no = nopat;
        }
        int size() {
            return this.no == null ? 1 : 2;
        }
        Token getChild(int index) {
            if (index == 0)  return this.yes;
            if (index == 1)  return this.no;
            throw new RuntimeException("Internal Error: "+index);
        }

        public String toString(int options) {
            String ret;
            if (refNumber > 0) {
                ret = "(?("+refNumber+")";
            } else if (this.condition.type == Token.ANCHOR) {
                ret = "(?("+this.condition+")";
            } else {
                ret = "(?"+this.condition;
            }

            if (this.no == null) {
                ret += this.yes+")";
            } else {
                ret += this.yes+"|"+this.no+")";
            }
            return ret;
        }
    }

    /**
     * (ims-ims: .... )
     */
    static class ModifierToken extends Token implements java.io.Serializable {

        private static final long serialVersionUID = -9114536559696480356L;

        final Token child;
        final int add;
        final int mask;

        ModifierToken(Token tok, int add, int mask) {
            super(Token.MODIFIERGROUP);
            this.child = tok;
            this.add = add;
            this.mask = mask;
        }

        int size() {
            return 1;
        }
        Token getChild(int index) {
            return this.child;
        }

        int getOptions() {
            return this.add;
        }
        int getOptionsMask() {
            return this.mask;
        }

        public String toString(int options) {
            return "(?"
                +(this.add == 0 ? "" : REUtil.createOptionString(this.add))
                +(this.mask == 0 ? "" : REUtil.createOptionString(this.mask))
                +":"
                +this.child.toString(options)
                +")";
        }
    }

    /**
     * This class represents a node in parse tree.
     * for UNION or CONCAT.
     */
    static class UnionToken extends Token implements java.io.Serializable {

        private static final long serialVersionUID = -2568843945989489861L;

        List<Token> children;

        /**
         * @serialField children Vector children
         */
        private static final ObjectStreamField[] serialPersistentFields =
            new ObjectStreamField[] {
                new ObjectStreamField("children", Vector.class),
            };

        UnionToken(int type) {
            super(type);
        }

        @Override
        void addChild(Token tok) {
            if (tok == null)  return;
            if (this.children == null)  this.children = new ArrayList<>();
            if (this.type == UNION) {
                this.children.add(tok);
                return;
            }
                                                // This is CONCAT, and new child is CONCAT.
            if (tok.type == CONCAT) {
                for (int i = 0;  i < tok.size();  i ++)
                    this.addChild(tok.getChild(i)); // Recursion
                return;
            }
            int size = this.children.size();
            if (size == 0) {
                this.children.add(tok);
                return;
            }
            Token previous = this.children.get(size - 1);
            if (!((previous.type == CHAR || previous.type == STRING)
                  && (tok.type == CHAR || tok.type == STRING))) {
                this.children.add(tok);
                return;
            }

            //System.err.println("Merge '"+previous+"' and '"+tok+"'.");

            StringBuilder buffer;
            int nextMaxLength = (tok.type == CHAR ? 2 : tok.getString().length());
            if (previous.type == CHAR) {        // Replace previous token by STRING
                buffer = new StringBuilder(2 + nextMaxLength);
                int ch = previous.getChar();
                if (ch >= 0x10000)
                    buffer.append(REUtil.decomposeToSurrogates(ch));
                else
                    buffer.append((char)ch);
                previous = Token.createString(null);
                this.children.set(size - 1, previous);
            } else {                            // STRING
                buffer = new StringBuilder(previous.getString().length() + nextMaxLength);
                buffer.append(previous.getString());
            }

            if (tok.type == CHAR) {
                int ch = tok.getChar();
                if (ch >= 0x10000)
                    buffer.append(REUtil.decomposeToSurrogates(ch));
                else
                    buffer.append((char)ch);
            } else {
                buffer.append(tok.getString());
            }

            ((StringToken)previous).string = new String(buffer);
        }

        @Override
        int size() {
            return this.children == null ? 0 : this.children.size();
        }
        @Override
        Token getChild(int index) {
            return this.children.get(index);
        }

        @Override
        public String toString(int options) {
            String ret;
            if (this.type == CONCAT) {
                if (this.children.size() == 2) {
                    Token ch = this.getChild(0);
                    Token ch2 = this.getChild(1);
                    if (ch2.type == CLOSURE && ch2.getChild(0) == ch) {
                        ret = ch.toString(options)+"+";
                    } else if (ch2.type == NONGREEDYCLOSURE && ch2.getChild(0) == ch) {
                        ret = ch.toString(options)+"+?";
                    } else
                        ret = ch.toString(options)+ch2.toString(options);
                } else {
                    StringBuilder sb = new StringBuilder();
                    this.children.stream().forEach((children1) -> {
                        sb.append((children1).toString(options));
                    });
                    ret = sb.toString();
                }
                return ret;
            }
            if (this.children.size() == 2 && this.getChild(1).type == EMPTY) {
                ret = this.getChild(0).toString(options)+"?";
            } else if (this.children.size() == 2
                       && this.getChild(0).type == EMPTY) {
                ret = this.getChild(1).toString(options)+"??";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append((this.children.get(0)).toString(options));
                for (int i = 1;  i < this.children.size();  i ++) {
                    sb.append('|');
                    sb.append((this.children.get(i)).toString(options));
                }
                ret = sb.toString();
            }
            return ret;
        }

        /**
         * @serialData Serialized fields. Convert the List to Vector for backward compatibility.
         */
        private void writeObject(ObjectOutputStream out) throws IOException {
            // Convert List to Vector
            Vector<Token> vChildren = (children == null)? null : new Vector<>(children);

            // Write serialized fields
            ObjectOutputStream.PutField pf = out.putFields();
            pf.put("children", vChildren);
            out.writeFields();
    }

        @SuppressWarnings("unchecked")
        private void readObject(ObjectInputStream in)
                            throws IOException, ClassNotFoundException {
            // We have to read serialized fields first.
            ObjectInputStream.GetField gf = in.readFields();
            Vector<Token> vChildren = (Vector<Token>)gf.get("children", null);

            //convert Vector back to List
            if (vChildren != null) children = new ArrayList<>(vChildren);
        }
    }
}
