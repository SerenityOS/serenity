/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.*;
import java.util.*;
import java.awt.Color;
import java.awt.Font;
import java.io.OutputStream;
import java.io.IOException;

import javax.swing.text.*;

/**
 * Generates an RTF output stream (java.io.OutputStream) from rich text
 * (handed off through a series of LTTextAcceptor calls).  Can be used to
 * generate RTF from any object which knows how to write to a text acceptor
 * (e.g., LTAttributedText and LTRTFFilter).
 *
 * <p>Note that this is a lossy conversion since RTF's model of
 * text does not exactly correspond with LightText's.
 *
 * @see java.io.OutputStream
 */
class RTFGenerator extends Object
{
    /* These dictionaries map Colors, font names, or Style objects
       to Integers */
    Dictionary<Object, Integer> colorTable;
    int colorCount;
    Dictionary<String, Integer> fontTable;
    int fontCount;
    Dictionary<AttributeSet, Integer> styleTable;
    int styleCount;

    /* where all the text is going */
    OutputStream outputStream;

    boolean afterKeyword;

    MutableAttributeSet outputAttributes;

    /* the value of the last \\ucN keyword emitted */
    int unicodeCount;

    /* for efficiency's sake (ha) */
    private Segment workingSegment;

    int[] outputConversion;

    /** The default color, used for text without an explicit color
     *  attribute. */
    public static final Color defaultRTFColor = Color.black;

    public static final float defaultFontSize = 12f;

    public static final String defaultFontFamily = "Helvetica";

    /* constants so we can avoid allocating objects in inner loops */
    private static final Object MagicToken;

    /* An array of character-keyword pairs. This could be done
       as a dictionary (and lookup would be quicker), but that
       would require allocating an object for every character
       written (slow!). */
    static class CharacterKeywordPair
      { public char character; public String keyword; }
    protected static CharacterKeywordPair[] textKeywords;

    static {
        MagicToken = new Object();

        Dictionary<String, String> textKeywordDictionary = RTFReader.textKeywords;
        Enumeration<String> keys = textKeywordDictionary.keys();
        Vector<CharacterKeywordPair> tempPairs = new Vector<CharacterKeywordPair>();
        while(keys.hasMoreElements()) {
            CharacterKeywordPair pair = new CharacterKeywordPair();
            pair.keyword = keys.nextElement();
            pair.character = textKeywordDictionary.get(pair.keyword).charAt(0);
            tempPairs.addElement(pair);
        }
        textKeywords = new CharacterKeywordPair[tempPairs.size()];
        tempPairs.copyInto(textKeywords);
    }

    static final char[] hexdigits = { '0', '1', '2', '3', '4', '5', '6', '7',
                                      '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

public static void writeDocument(Document d, OutputStream to)
    throws IOException
{
    RTFGenerator gen = new RTFGenerator(to);
    Element root = d.getDefaultRootElement();

    gen.examineElement(root);
    gen.writeRTFHeader();
    gen.writeDocumentProperties(d);

    /* TODO this assumes a particular element structure; is there
       a way to iterate more generically ? */
    int max = root.getElementCount();
    for(int idx = 0; idx < max; idx++)
        gen.writeParagraphElement(root.getElement(idx));

    gen.writeRTFTrailer();
}

public RTFGenerator(OutputStream to)
{
    colorTable = new Hashtable<Object, Integer>();
    colorTable.put(defaultRTFColor, Integer.valueOf(0));
    colorCount = 1;

    fontTable = new Hashtable<String, Integer>();
    fontCount = 0;

    styleTable = new Hashtable<AttributeSet, Integer>();
    /* TODO: put default style in style table */
    styleCount = 0;

    workingSegment = new Segment();

    outputStream = to;

    unicodeCount = 1;
}

public void examineElement(Element el)
{
    AttributeSet a = el.getAttributes();
    String fontName;
    Object foregroundColor, backgroundColor;

    tallyStyles(a);

    if (a != null) {
        /* TODO: default color must be color 0! */

        foregroundColor = StyleConstants.getForeground(a);
        if (foregroundColor != null &&
            colorTable.get(foregroundColor) == null) {
            colorTable.put(foregroundColor, colorCount);
            colorCount ++;
        }

        backgroundColor = a.getAttribute(StyleConstants.Background);
        if (backgroundColor != null &&
            colorTable.get(backgroundColor) == null) {
            colorTable.put(backgroundColor, colorCount);
            colorCount ++;
        }

        fontName = StyleConstants.getFontFamily(a);

        if (fontName == null)
            fontName = defaultFontFamily;

        if (fontName != null &&
            fontTable.get(fontName) == null) {
            fontTable.put(fontName, fontCount);
            fontCount ++;
        }
    }

    int el_count = el.getElementCount();
    for(int el_idx = 0; el_idx < el_count; el_idx ++) {
        examineElement(el.getElement(el_idx));
    }
}

private void tallyStyles(AttributeSet a) {
    while (a != null) {
        if (a instanceof Style) {
            Integer aNum = styleTable.get(a);
            if (aNum == null) {
                styleCount = styleCount + 1;
                aNum = styleCount;
                styleTable.put(a, aNum);
            }
        }
        a = a.getResolveParent();
    }
}

private Style findStyle(AttributeSet a)
{
    while(a != null) {
        if (a instanceof Style) {
            Object aNum = styleTable.get(a);
            if (aNum != null)
                return (Style)a;
        }
        a = a.getResolveParent();
    }
    return null;
}

private Integer findStyleNumber(AttributeSet a, String domain)
{
    while(a != null) {
        if (a instanceof Style) {
            Integer aNum = styleTable.get(a);
            if (aNum != null) {
                if (domain == null ||
                    domain.equals(a.getAttribute(Constants.StyleType)))
                    return aNum;
            }

        }
        a = a.getResolveParent();
    }
    return null;
}

private static Object attrDiff(MutableAttributeSet oldAttrs,
                               AttributeSet newAttrs,
                               Object key,
                               Object dfl)
{
    Object oldValue, newValue;

    oldValue = oldAttrs.getAttribute(key);
    newValue = newAttrs.getAttribute(key);

    if (newValue == oldValue)
        return null;
    if (newValue == null) {
        oldAttrs.removeAttribute(key);
        if (dfl != null && !dfl.equals(oldValue))
            return dfl;
        else
            return null;
    }
    if (oldValue == null ||
        !equalArraysOK(oldValue, newValue)) {
        oldAttrs.addAttribute(key, newValue);
        return newValue;
    }
    return null;
}

private static boolean equalArraysOK(Object a, Object b)
{
    Object[] aa, bb;
    if (a == b)
        return true;
    if (a == null || b == null)
        return false;
    if (a.equals(b))
        return true;
    if (!(a.getClass().isArray() && b.getClass().isArray()))
        return false;
    aa = (Object[])a;
    bb = (Object[])b;
    if (aa.length != bb.length)
        return false;

    int i;
    int l = aa.length;
    for(i = 0; i < l; i++) {
        if (!equalArraysOK(aa[i], bb[i]))
            return false;
    }

    return true;
}

/* Writes a line break to the output file, for ease in debugging */
public void writeLineBreak()
    throws IOException
{
    writeRawString("\n");
    afterKeyword = false;
}


public void writeRTFHeader()
    throws IOException
{
    int index;

    /* TODO: Should the writer attempt to examine the text it's writing
       and pick a character set which will most compactly represent the
       document? (currently the writer always uses the ansi character
       set, which is roughly ISO-8859 Latin-1, and uses Unicode escapes
       for all other characters. However Unicode is a relatively
       recent addition to RTF, and not all readers will understand it.) */
    writeBegingroup();
    writeControlWord("rtf", 1);
    writeControlWord("ansi");
    outputConversion = outputConversionForName("ansi");
    writeLineBreak();

    /* write font table */
    String[] sortedFontTable = new String[fontCount];
    Enumeration<String> fonts = fontTable.keys();
    String font;
    while(fonts.hasMoreElements()) {
        font = fonts.nextElement();
        Integer num = fontTable.get(font);
        sortedFontTable[num.intValue()] = font;
    }
    writeBegingroup();
    writeControlWord("fonttbl");
    for(index = 0; index < fontCount; index ++) {
        writeControlWord("f", index);
        writeControlWord("fnil");  /* TODO: supply correct font style */
        writeText(sortedFontTable[index]);
        writeText(";");
    }
    writeEndgroup();
    writeLineBreak();

    /* write color table */
    if (colorCount > 1) {
        Color[] sortedColorTable = new Color[colorCount];
        Enumeration<Object> colors = colorTable.keys();
        Color color;
        while(colors.hasMoreElements()) {
            color = (Color)colors.nextElement();
            Integer num = colorTable.get(color);
            sortedColorTable[num.intValue()] = color;
        }
        writeBegingroup();
        writeControlWord("colortbl");
        for(index = 0; index < colorCount; index ++) {
            color = sortedColorTable[index];
            if (color != null) {
                writeControlWord("red", color.getRed());
                writeControlWord("green", color.getGreen());
                writeControlWord("blue", color.getBlue());
            }
            writeRawString(";");
        }
        writeEndgroup();
        writeLineBreak();
    }

    /* write the style sheet */
    if (styleCount > 1) {
        writeBegingroup();
        writeControlWord("stylesheet");
        Enumeration<AttributeSet> styles = styleTable.keys();
        while(styles.hasMoreElements()) {
            Style style = (Style)styles.nextElement();
            int styleNumber = styleTable.get(style).intValue();
            writeBegingroup();
            String styleType = (String)style.getAttribute(Constants.StyleType);
            if (styleType == null)
                styleType = Constants.STParagraph;
            if (styleType.equals(Constants.STCharacter)) {
                writeControlWord("*");
                writeControlWord("cs", styleNumber);
            } else if(styleType.equals(Constants.STSection)) {
                writeControlWord("*");
                writeControlWord("ds", styleNumber);
            } else {
                writeControlWord("s", styleNumber);
            }

            AttributeSet basis = style.getResolveParent();
            MutableAttributeSet goat;
            if (basis == null) {
                goat = new SimpleAttributeSet();
            } else {
                goat = new SimpleAttributeSet(basis);
            }

            updateSectionAttributes(goat, style, false);
            updateParagraphAttributes(goat, style, false);
            updateCharacterAttributes(goat, style, false);

            basis = style.getResolveParent();
            if (basis != null && basis instanceof Style) {
                Integer basedOn = styleTable.get(basis);
                if (basedOn != null) {
                    writeControlWord("sbasedon", basedOn.intValue());
                }
            }

            Style nextStyle = (Style)style.getAttribute(Constants.StyleNext);
            if (nextStyle != null) {
                Integer nextNum = styleTable.get(nextStyle);
                if (nextNum != null) {
                    writeControlWord("snext", nextNum.intValue());
                }
            }

            Boolean hidden = (Boolean)style.getAttribute(Constants.StyleHidden);
            if (hidden != null && hidden.booleanValue())
                writeControlWord("shidden");

            Boolean additive = (Boolean)style.getAttribute(Constants.StyleAdditive);
            if (additive != null && additive.booleanValue())
                writeControlWord("additive");


            writeText(style.getName());
            writeText(";");
            writeEndgroup();
        }
        writeEndgroup();
        writeLineBreak();
    }

    outputAttributes = new SimpleAttributeSet();
}

void writeDocumentProperties(Document doc)
    throws IOException
{
    /* Write the document properties */
    int i;
    boolean wroteSomething = false;

    for(i = 0; i < RTFAttributes.attributes.length; i++) {
        RTFAttribute attr = RTFAttributes.attributes[i];
        if (attr.domain() != RTFAttribute.D_DOCUMENT)
            continue;
        Object prop = doc.getProperty(attr.swingName());
        boolean ok = attr.writeValue(prop, this, false);
        if (ok)
            wroteSomething = true;
    }

    if (wroteSomething)
        writeLineBreak();
}

public void writeRTFTrailer()
    throws IOException
{
    writeEndgroup();
    writeLineBreak();
}

protected void checkNumericControlWord(MutableAttributeSet currentAttributes,
                                       AttributeSet newAttributes,
                                       Object attrName,
                                       String controlWord,
                                       float dflt, float scale)
    throws IOException
{
    Object parm;

    if ((parm = attrDiff(currentAttributes, newAttributes,
                         attrName, MagicToken)) != null) {
        float targ;
        if (parm == MagicToken)
            targ = dflt;
        else
            targ = ((Number)parm).floatValue();
        writeControlWord(controlWord, Math.round(targ * scale));
    }
}

protected void checkControlWord(MutableAttributeSet currentAttributes,
                                AttributeSet newAttributes,
                                RTFAttribute word)
    throws IOException
{
    Object parm;

    if ((parm = attrDiff(currentAttributes, newAttributes,
                         word.swingName(), MagicToken)) != null) {
        if (parm == MagicToken)
            parm = null;
        if (!word.writeValue(parm, this, true) &&
                word instanceof RTFAttributes.AssertiveAttribute ) {
            currentAttributes.removeAttribute(word.swingName());
        }
    }
}

protected void checkControlWords(MutableAttributeSet currentAttributes,
                                 AttributeSet newAttributes,
                                 RTFAttribute[] words,
                                 int domain)
    throws IOException
{
    int wordIndex;
    int wordCount = words.length;
    for(wordIndex = 0; wordIndex < wordCount; wordIndex++) {
        RTFAttribute attr = words[wordIndex];
        if (attr.domain() == domain)
            checkControlWord(currentAttributes, newAttributes, attr);
    }
}

void updateSectionAttributes(MutableAttributeSet current,
                             AttributeSet newAttributes,
                             boolean emitStyleChanges)
    throws IOException
{
    if (emitStyleChanges) {
        Object oldStyle = current.getAttribute("sectionStyle");
        Object newStyle = findStyleNumber(newAttributes, Constants.STSection);
        if (oldStyle != newStyle) {
            if (oldStyle != null) {
                resetSectionAttributes(current);
            }
            if (newStyle != null) {
                writeControlWord("ds", ((Integer)newStyle).intValue());
                current.addAttribute("sectionStyle", newStyle);
            } else {
                current.removeAttribute("sectionStyle");
            }
        }
    }

    checkControlWords(current, newAttributes,
                      RTFAttributes.attributes, RTFAttribute.D_SECTION);
}

protected void resetSectionAttributes(MutableAttributeSet currentAttributes)
    throws IOException
{
    writeControlWord("sectd");

    int wordIndex;
    int wordCount = RTFAttributes.attributes.length;
    for(wordIndex = 0; wordIndex < wordCount; wordIndex++) {
        RTFAttribute attr = RTFAttributes.attributes[wordIndex];
        if (attr.domain() == RTFAttribute.D_SECTION)
            attr.setDefault(currentAttributes);
    }

    currentAttributes.removeAttribute("sectionStyle");
}

void updateParagraphAttributes(MutableAttributeSet current,
                               AttributeSet newAttributes,
                               boolean emitStyleChanges)
    throws IOException
{
    Object parm;
    Object oldStyle, newStyle;

    /* The only way to get rid of tabs or styles is with the \pard keyword,
       emitted by resetParagraphAttributes(). Ideally we should avoid
       emitting \pard if the new paragraph's tabs are a superset of the old
       paragraph's tabs. */

    if (emitStyleChanges) {
        oldStyle = current.getAttribute("paragraphStyle");
        newStyle = findStyleNumber(newAttributes, Constants.STParagraph);
        if (oldStyle != newStyle) {
            if (oldStyle != null) {
                resetParagraphAttributes(current);
                oldStyle = null;
            }
        }
    } else {
        oldStyle = null;
        newStyle = null;
    }

    Object oldTabs = current.getAttribute(Constants.Tabs);
    Object newTabs = newAttributes.getAttribute(Constants.Tabs);
    if (oldTabs != newTabs) {
        if (oldTabs != null) {
            resetParagraphAttributes(current);
            oldTabs = null;
            oldStyle = null;
        }
    }

    if (oldStyle != newStyle && newStyle != null) {
        writeControlWord("s", ((Integer)newStyle).intValue());
        current.addAttribute("paragraphStyle", newStyle);
    }

    checkControlWords(current, newAttributes,
                      RTFAttributes.attributes, RTFAttribute.D_PARAGRAPH);

    if (oldTabs != newTabs && newTabs != null) {
        TabStop[] tabs = (TabStop[])newTabs;
        int index;
        for(index = 0; index < tabs.length; index ++) {
            TabStop tab = tabs[index];
            switch (tab.getAlignment()) {
              case TabStop.ALIGN_LEFT:
              case TabStop.ALIGN_BAR:
                break;
              case TabStop.ALIGN_RIGHT:
                writeControlWord("tqr");
                break;
              case TabStop.ALIGN_CENTER:
                writeControlWord("tqc");
                break;
              case TabStop.ALIGN_DECIMAL:
                writeControlWord("tqdec");
                break;
            }
            switch (tab.getLeader()) {
              case TabStop.LEAD_NONE:
                break;
              case TabStop.LEAD_DOTS:
                writeControlWord("tldot");
                break;
              case TabStop.LEAD_HYPHENS:
                writeControlWord("tlhyph");
                break;
              case TabStop.LEAD_UNDERLINE:
                writeControlWord("tlul");
                break;
              case TabStop.LEAD_THICKLINE:
                writeControlWord("tlth");
                break;
              case TabStop.LEAD_EQUALS:
                writeControlWord("tleq");
                break;
            }
            int twips = Math.round(20f * tab.getPosition());
            if (tab.getAlignment() == TabStop.ALIGN_BAR) {
                writeControlWord("tb", twips);
            } else {
                writeControlWord("tx", twips);
            }
        }
        current.addAttribute(Constants.Tabs, tabs);
    }
}

public void writeParagraphElement(Element el)
    throws IOException
{
    updateParagraphAttributes(outputAttributes, el.getAttributes(), true);

    int sub_count = el.getElementCount();
    for(int idx = 0; idx < sub_count; idx ++) {
        writeTextElement(el.getElement(idx));
    }

    writeControlWord("par");
    writeLineBreak();  /* makes the raw file more readable */
}

/* debugging. TODO: remove.
private static String tabdump(Object tso)
{
    String buf;
    int i;

    if (tso == null)
        return "[none]";

    TabStop[] ts = (TabStop[])tso;

    buf = "[";
    for(i = 0; i < ts.length; i++) {
        buf = buf + ts[i].toString();
        if ((i+1) < ts.length)
            buf = buf + ",";
    }
    return buf + "]";
}
*/

protected void resetParagraphAttributes(MutableAttributeSet currentAttributes)
    throws IOException
{
    writeControlWord("pard");

    currentAttributes.addAttribute(StyleConstants.Alignment, Integer.valueOf(0));

    int wordIndex;
    int wordCount = RTFAttributes.attributes.length;
    for(wordIndex = 0; wordIndex < wordCount; wordIndex++) {
        RTFAttribute attr = RTFAttributes.attributes[wordIndex];
        if (attr.domain() == RTFAttribute.D_PARAGRAPH)
            attr.setDefault(currentAttributes);
    }

    currentAttributes.removeAttribute("paragraphStyle");
    currentAttributes.removeAttribute(Constants.Tabs);
}

void updateCharacterAttributes(MutableAttributeSet current,
                               AttributeSet newAttributes,
                               boolean updateStyleChanges)
    throws IOException
{
    Object parm;

    if (updateStyleChanges) {
        Object oldStyle = current.getAttribute("characterStyle");
        Object newStyle = findStyleNumber(newAttributes,
                                          Constants.STCharacter);
        if (oldStyle != newStyle) {
            if (oldStyle != null) {
                resetCharacterAttributes(current);
            }
            if (newStyle != null) {
                writeControlWord("cs", ((Integer)newStyle).intValue());
                current.addAttribute("characterStyle", newStyle);
            } else {
                current.removeAttribute("characterStyle");
            }
        }
    }

    if ((parm = attrDiff(current, newAttributes,
                         StyleConstants.FontFamily, null)) != null) {
        Integer fontNum = fontTable.get(parm);
        writeControlWord("f", fontNum.intValue());
    }

    checkNumericControlWord(current, newAttributes,
                            StyleConstants.FontSize, "fs",
                            defaultFontSize, 2f);

    checkControlWords(current, newAttributes,
                      RTFAttributes.attributes, RTFAttribute.D_CHARACTER);

    checkNumericControlWord(current, newAttributes,
                            StyleConstants.LineSpacing, "sl",
                            0, 20f); /* TODO: sl wackiness */

    if ((parm = attrDiff(current, newAttributes,
                         StyleConstants.Background, MagicToken)) != null) {
        int colorNum;
        if (parm == MagicToken)
            colorNum = 0;
        else
            colorNum = colorTable.get(parm).intValue();
        writeControlWord("cb", colorNum);
    }

    if ((parm = attrDiff(current, newAttributes,
                         StyleConstants.Foreground, null)) != null) {
        int colorNum;
        if (parm == MagicToken)
            colorNum = 0;
        else
            colorNum = colorTable.get(parm).intValue();
        writeControlWord("cf", colorNum);
    }
}

protected void resetCharacterAttributes(MutableAttributeSet currentAttributes)
    throws IOException
{
    writeControlWord("plain");

    int wordIndex;
    int wordCount = RTFAttributes.attributes.length;
    for(wordIndex = 0; wordIndex < wordCount; wordIndex++) {
        RTFAttribute attr = RTFAttributes.attributes[wordIndex];
        if (attr.domain() == RTFAttribute.D_CHARACTER)
            attr.setDefault(currentAttributes);
    }

    StyleConstants.setFontFamily(currentAttributes, defaultFontFamily);
    currentAttributes.removeAttribute(StyleConstants.FontSize); /* =default */
    currentAttributes.removeAttribute(StyleConstants.Background);
    currentAttributes.removeAttribute(StyleConstants.Foreground);
    currentAttributes.removeAttribute(StyleConstants.LineSpacing);
    currentAttributes.removeAttribute("characterStyle");
}

public void writeTextElement(Element el)
    throws IOException
{
    updateCharacterAttributes(outputAttributes, el.getAttributes(), true);

    if (el.isLeaf()) {
        try {
            el.getDocument().getText(el.getStartOffset(),
                                     el.getEndOffset() - el.getStartOffset(),
                                     this.workingSegment);
        } catch (BadLocationException ble) {
            /* TODO is this the correct error to raise? */
            ble.printStackTrace();
            throw new InternalError(ble.getMessage());
        }
        writeText(this.workingSegment);
    } else {
        int sub_count = el.getElementCount();
        for(int idx = 0; idx < sub_count; idx ++)
            writeTextElement(el.getElement(idx));
    }
}

public void writeText(Segment s)
    throws IOException
{
    int pos, end;
    char[] array;

    pos = s.offset;
    end = pos + s.count;
    array = s.array;
    for( ; pos < end; pos ++)
        writeCharacter(array[pos]);
}

public void writeText(String s)
    throws IOException
{
    int pos, end;

    pos = 0;
    end = s.length();
    for( ; pos < end; pos ++)
        writeCharacter(s.charAt(pos));
}

public void writeRawString(String str)
    throws IOException
{
    int strlen = str.length();
    for (int offset = 0; offset < strlen; offset ++)
        outputStream.write((int)str.charAt(offset));
}

public void writeControlWord(String keyword)
    throws IOException
{
    outputStream.write('\\');
    writeRawString(keyword);
    afterKeyword = true;
}

public void writeControlWord(String keyword, int arg)
    throws IOException
{
    outputStream.write('\\');
    writeRawString(keyword);
    writeRawString(String.valueOf(arg)); /* TODO: correct in all cases? */
    afterKeyword = true;
}

public void writeBegingroup()
    throws IOException
{
    outputStream.write('{');
    afterKeyword = false;
}

public void writeEndgroup()
    throws IOException
{
    outputStream.write('}');
    afterKeyword = false;
}

@SuppressWarnings("fallthrough")
public void writeCharacter(char ch)
    throws IOException
{
    /* Nonbreaking space is in most RTF encodings, but the keyword is
       preferable; same goes for tabs */
    if (ch == 0xA0) { /* nonbreaking space */
        outputStream.write(0x5C);  /* backslash */
        outputStream.write(0x7E);  /* tilde */
        afterKeyword = false; /* non-alpha keywords are self-terminating */
        return;
    }

    if (ch == 0x09) { /* horizontal tab */
        writeControlWord("tab");
        return;
    }

    if (ch == 10 || ch == 13) { /* newline / paragraph */
        /* ignore CRs, we'll write a paragraph element soon enough */
        return;
    }

    int b = convertCharacter(outputConversion, ch);
    if (b == 0) {
        /* Unicode characters which have corresponding RTF keywords */
        int i;
        for(i = 0; i < textKeywords.length; i++) {
            if (textKeywords[i].character == ch) {
                writeControlWord(textKeywords[i].keyword);
                return;
            }
        }
        /* In some cases it would be reasonable to check to see if the
           glyph being written out is in the Symbol encoding, and if so,
           to switch to the Symbol font for this character. TODO. */
        /* Currently all unrepresentable characters are written as
           Unicode escapes. */
        String approximation = approximationForUnicode(ch);
        if (approximation.length() != unicodeCount) {
            unicodeCount = approximation.length();
            writeControlWord("uc", unicodeCount);
        }
        writeControlWord("u", (int)ch);
        writeRawString(" ");
        writeRawString(approximation);
        afterKeyword = false;
        return;
    }

    if (b > 127) {
        int nybble;
        outputStream.write('\\');
        outputStream.write('\'');
        nybble = ( b & 0xF0 ) >>> 4;
        outputStream.write(hexdigits[nybble]);
        nybble = ( b & 0x0F );
        outputStream.write(hexdigits[nybble]);
        afterKeyword = false;
        return;
    }

    switch (b) {
    case '}':
    case '{':
    case '\\':
        outputStream.write(0x5C);  /* backslash */
        afterKeyword = false;  /* in a keyword, actually ... */
        /* fall through */
    default:
        if (afterKeyword) {
            outputStream.write(0x20);  /* space */
            afterKeyword = false;
        }
        outputStream.write(b);
        break;
    }
}

String approximationForUnicode(char ch)
{
    /* TODO: Find reasonable approximations for all Unicode characters
       in all RTF code pages... heh, heh... */
    return "?";
}

/** Takes a translation table (a 256-element array of characters)
 * and creates an output conversion table for use by
 * convertCharacter(). */
    /* Not very efficient at all. Could be changed to sort the table
       for binary search. TODO. (Even though this is inefficient however,
       writing RTF is still much faster than reading it.) */
static int[] outputConversionFromTranslationTable(char[] table)
{
    int[] conversion = new int[2 * table.length];

    int index;

    for(index = 0; index < table.length; index ++) {
        conversion[index * 2] = table[index];
        conversion[(index * 2) + 1] = index;
    }

    return conversion;
}

static int[] outputConversionForName(String name)
    throws IOException
{
    char[] table = (char[])RTFReader.getCharacterSet(name);
    return outputConversionFromTranslationTable(table);
}

/** Takes a char and a conversion table (an int[] in the current
 * implementation, but conversion tables should be treated as an opaque
 * type) and returns the
 * corresponding byte value (as an int, since bytes are signed).
 */
    /* Not very efficient. TODO. */
protected static int convertCharacter(int[] conversion, char ch)
{
   int index;

   for(index = 0; index < conversion.length; index += 2) {
       if(conversion[index] == ch)
           return conversion[index + 1];
   }

   return 0;  /* 0 indicates an unrepresentable character */
}

}
