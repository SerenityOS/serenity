/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StreamTokenizer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabStop;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * Takes a sequence of RTF tokens and text and appends the text
 * described by the RTF to a <code>StyledDocument</code> (the <em>target</em>).
 * The RTF is lexed
 * from the character stream by the <code>RTFParser</code> which is this class's
 * superclass.
 *
 * This class is an indirect subclass of OutputStream. It must be closed
 * in order to guarantee that all of the text has been sent to
 * the text acceptor.
 *
 *   @see RTFParser
 *   @see java.io.OutputStream
 */
class RTFReader extends RTFParser
{
  /** The object to which the parsed text is sent. */
  StyledDocument target;

  /** Miscellaneous information about the parser's state. This
   *  dictionary is saved and restored when an RTF group begins
   *  or ends. */
  Dictionary<Object, Object> parserState;   /* Current parser state */
  /** This is the "dst" item from parserState. rtfDestination
   *  is the current rtf destination. It is cached in an instance
   *  variable for speed. */
  Destination rtfDestination;
  /** This holds the current document attributes. */
  MutableAttributeSet documentAttributes;

  /** This Dictionary maps Integer font numbers to String font names. */
  Dictionary<Integer, String> fontTable;
  /** This array maps color indices to Color objects. */
  Color[] colorTable;
  /** This array maps character style numbers to Style objects. */
  Style[] characterStyles;
  /** This array maps paragraph style numbers to Style objects. */
  Style[] paragraphStyles;
  /** This array maps section style numbers to Style objects. */
  Style[] sectionStyles;

  /** This is the RTF version number, extracted from the \rtf keyword.
   *  The version information is currently not used. */
  int rtfversion;

  /** <code>true</code> to indicate that if the next keyword is unknown,
   *  the containing group should be ignored. */
  boolean ignoreGroupIfUnknownKeyword;

  /** The parameter of the most recently parsed \\ucN keyword,
   *  used for skipping alternative representations after a
   *  Unicode character. */
  int skippingCharacters;

  private static Dictionary<String, RTFAttribute> straightforwardAttributes;
  static {
      straightforwardAttributes = RTFAttributes.attributesByKeyword();
  }

  private MockAttributeSet mockery;

  /* this should be final, but there's a bug in javac... */
  /** textKeywords maps RTF keywords to single-character strings,
   *  for those keywords which simply insert some text. */
  static Dictionary<String, String> textKeywords = null;
  static {
      textKeywords = new Hashtable<String, String>();
      textKeywords.put("\\",         "\\");
      textKeywords.put("{",          "{");
      textKeywords.put("}",          "}");
      textKeywords.put(" ",          "\u00A0");  /* not in the spec... */
      textKeywords.put("~",          "\u00A0");  /* nonbreaking space */
      textKeywords.put("_",          "\u2011");  /* nonbreaking hyphen */
      textKeywords.put("bullet",     "\u2022");
      textKeywords.put("emdash",     "\u2014");
      textKeywords.put("emspace",    "\u2003");
      textKeywords.put("endash",     "\u2013");
      textKeywords.put("enspace",    "\u2002");
      textKeywords.put("ldblquote",  "\u201C");
      textKeywords.put("lquote",     "\u2018");
      textKeywords.put("ltrmark",    "\u200E");
      textKeywords.put("rdblquote",  "\u201D");
      textKeywords.put("rquote",     "\u2019");
      textKeywords.put("rtlmark",    "\u200F");
      textKeywords.put("tab",        "\u0009");
      textKeywords.put("zwj",        "\u200D");
      textKeywords.put("zwnj",       "\u200C");

      /* There is no Unicode equivalent to an optional hyphen, as far as
         I can tell. */
      textKeywords.put("-",          "\u2027");  /* TODO: optional hyphen */
  }

  /* some entries in parserState */
  static final String TabAlignmentKey = "tab_alignment";
  static final String TabLeaderKey = "tab_leader";

  static Dictionary<String, char[]> characterSets;
  static boolean useNeXTForAnsi = false;
  static {
      characterSets = new Hashtable<String, char[]>();
      defineCharacterSet("ansicpg", latin1TranslationTable);
  }

/* TODO: per-font font encodings ( \fcharset control word ) ? */

/**
 * Creates a new RTFReader instance. Text will be sent to
 * the specified TextAcceptor.
 *
 * @param destination The TextAcceptor which is to receive the text.
 */
public RTFReader(StyledDocument destination)
{
    int i;

    target = destination;
    parserState = new Hashtable<Object, Object>();
    fontTable = new Hashtable<Integer, String>();

    rtfversion = -1;

    mockery = new MockAttributeSet();
    documentAttributes = new SimpleAttributeSet();
}

/** Called when the RTFParser encounters a bin keyword in the
 *  RTF stream.
 *
 *  @see RTFParser
 */
public void handleBinaryBlob(byte[] data)
{
    if (skippingCharacters > 0) {
        /* a blob only counts as one character for skipping purposes */
        skippingCharacters --;
        return;
    }

    /* someday, someone will want to do something with blobs */
}


/**
 * Handles any pure text (containing no control characters) in the input
 * stream. Called by the superclass. */
public void handleText(String text)
{
    if (skippingCharacters > 0) {
        if (skippingCharacters >= text.length()) {
            skippingCharacters -= text.length();
            return;
        } else {
            text = text.substring(skippingCharacters);
            skippingCharacters = 0;
        }
    }

    if (rtfDestination != null) {
        rtfDestination.handleText(text);
        return;
    }

    warning("Text with no destination. oops.");
}

/** The default color for text which has no specified color. */
Color defaultColor()
{
    return Color.black;
}

/** Called by the superclass when a new RTF group is begun.
 *  This implementation saves the current <code>parserState</code>, and gives
 *  the current destination a chance to save its own state.
 * @see RTFParser#begingroup
 */
public void begingroup()
{
    if (skippingCharacters > 0) {
        /* TODO this indicates an error in the RTF. Log it? */
        skippingCharacters = 0;
    }

    /* we do this little dance to avoid cloning the entire state stack and
       immediately throwing it away. */
    Object oldSaveState = parserState.get("_savedState");
    if (oldSaveState != null)
        parserState.remove("_savedState");
    @SuppressWarnings("unchecked")
    Dictionary<String, Object> saveState = (Dictionary<String, Object>)((Hashtable)parserState).clone();
    if (oldSaveState != null)
        saveState.put("_savedState", oldSaveState);
    parserState.put("_savedState", saveState);

    if (rtfDestination != null)
        rtfDestination.begingroup();
}

/** Called by the superclass when the current RTF group is closed.
 *  This restores the parserState saved by <code>begingroup()</code>
 *  as well as invoking the endgroup method of the current
 *  destination.
 * @see RTFParser#endgroup
 */
public void endgroup()
{
    if (skippingCharacters > 0) {
        /* NB this indicates an error in the RTF. Log it? */
        skippingCharacters = 0;
    }

    @SuppressWarnings("unchecked")
    Dictionary<Object, Object> restoredState = (Dictionary<Object, Object>)parserState.get("_savedState");
    Destination restoredDestination = (Destination)restoredState.get("dst");
    if (restoredDestination != rtfDestination) {
        rtfDestination.close(); /* allow the destination to clean up */
        rtfDestination = restoredDestination;
    }
    Dictionary<Object, Object> oldParserState = parserState;
    parserState = restoredState;
    if (rtfDestination != null)
        rtfDestination.endgroup(oldParserState);
}

protected void setRTFDestination(Destination newDestination)
{
    /* Check that setting the destination won't close the
       current destination (should never happen) */
    @SuppressWarnings("unchecked")
    Dictionary<Object, Object> previousState = (Dictionary)parserState.get("_savedState");
    if (previousState != null) {
        if (rtfDestination != previousState.get("dst")) {
            warning("Warning, RTF destination overridden, invalid RTF.");
            rtfDestination.close();
        }
    }
    rtfDestination = newDestination;
    parserState.put("dst", rtfDestination);
}

/** Called by the user when there is no more input (<i>i.e.</i>,
 * at the end of the RTF file.)
 *
 * @see OutputStream#close
 */
public void close()
    throws IOException
{
    Enumeration<?> docProps = documentAttributes.getAttributeNames();
    while(docProps.hasMoreElements()) {
        Object propName = docProps.nextElement();
        target.putProperty(propName,
                           documentAttributes.getAttribute(propName));
    }

    /* RTFParser should have ensured that all our groups are closed */

    warning("RTF filter done.");

    super.close();
}

/**
 * Handles a parameterless RTF keyword. This is called by the superclass
 * (RTFParser) when a keyword is found in the input stream.
 *
 * @return true if the keyword is recognized and handled;
 *         false otherwise
 * @see RTFParser#handleKeyword
 */
public boolean handleKeyword(String keyword)
{
    String item;
    boolean ignoreGroupIfUnknownKeywordSave = ignoreGroupIfUnknownKeyword;

    if (skippingCharacters > 0) {
        skippingCharacters --;
        return true;
    }

    ignoreGroupIfUnknownKeyword = false;

    if ((item = textKeywords.get(keyword)) != null) {
        handleText(item);
        return true;
    }

    if (keyword.equals("fonttbl")) {
        setRTFDestination(new FonttblDestination());
        return true;
    }

    if (keyword.equals("colortbl")) {
        setRTFDestination(new ColortblDestination());
        return true;
    }

    if (keyword.equals("stylesheet")) {
        setRTFDestination(new StylesheetDestination());
        return true;
    }

    if (keyword.equals("info")) {
        setRTFDestination(new InfoDestination());
        return false;
    }

    if (keyword.equals("mac")) {
        setCharacterSet("mac");
        return true;
    }

    if (keyword.equals("ansi")) {
        if (useNeXTForAnsi)
            setCharacterSet("NeXT");
        else
            setCharacterSet("ansi");
        return true;
    }

    if (keyword.equals("next")) {
        setCharacterSet("NeXT");
        return true;
    }

    if (keyword.equals("pc")) {
        setCharacterSet("cpg437"); /* IBM Code Page 437 */
        return true;
    }

    if (keyword.equals("pca")) {
        setCharacterSet("cpg850"); /* IBM Code Page 850 */
        return true;
    }

    if (keyword.equals("*")) {
        ignoreGroupIfUnknownKeyword = true;
        return true;
    }

    if (rtfDestination != null) {
        if(rtfDestination.handleKeyword(keyword))
            return true;
    }

    /* this point is reached only if the keyword is unrecognized */

    /* other destinations we don't understand and therefore ignore */
    if (keyword.equals("aftncn") ||
        keyword.equals("aftnsep") ||
        keyword.equals("aftnsepc") ||
        keyword.equals("annotation") ||
        keyword.equals("atnauthor") ||
        keyword.equals("atnicn") ||
        keyword.equals("atnid") ||
        keyword.equals("atnref") ||
        keyword.equals("atntime") ||
        keyword.equals("atrfend") ||
        keyword.equals("atrfstart") ||
        keyword.equals("bkmkend") ||
        keyword.equals("bkmkstart") ||
        keyword.equals("datafield") ||
        keyword.equals("do") ||
        keyword.equals("dptxbxtext") ||
        keyword.equals("falt") ||
        keyword.equals("field") ||
        keyword.equals("file") ||
        keyword.equals("filetbl") ||
        keyword.equals("fname") ||
        keyword.equals("fontemb") ||
        keyword.equals("fontfile") ||
        keyword.equals("footer") ||
        keyword.equals("footerf") ||
        keyword.equals("footerl") ||
        keyword.equals("footerr") ||
        keyword.equals("footnote") ||
        keyword.equals("ftncn") ||
        keyword.equals("ftnsep") ||
        keyword.equals("ftnsepc") ||
        keyword.equals("header") ||
        keyword.equals("headerf") ||
        keyword.equals("headerl") ||
        keyword.equals("headerr") ||
        keyword.equals("keycode") ||
        keyword.equals("nextfile") ||
        keyword.equals("object") ||
        keyword.equals("pict") ||
        keyword.equals("pn") ||
        keyword.equals("pnseclvl") ||
        keyword.equals("pntxtb") ||
        keyword.equals("pntxta") ||
        keyword.equals("revtbl") ||
        keyword.equals("rxe") ||
        keyword.equals("tc") ||
        keyword.equals("template") ||
        keyword.equals("txe") ||
        keyword.equals("xe")) {
        ignoreGroupIfUnknownKeywordSave = true;
    }

    if (ignoreGroupIfUnknownKeywordSave) {
        setRTFDestination(new DiscardingDestination());
    }

    return false;
}

/**
 * Handles an RTF keyword and its integer parameter.
 * This is called by the superclass
 * (RTFParser) when a keyword is found in the input stream.
 *
 * @return true if the keyword is recognized and handled;
 *         false otherwise
 * @see RTFParser#handleKeyword
 */
public boolean handleKeyword(String keyword, int parameter)
{
    boolean ignoreGroupIfUnknownKeywordSave = ignoreGroupIfUnknownKeyword;

    if (skippingCharacters > 0) {
        skippingCharacters --;
        return true;
    }

    ignoreGroupIfUnknownKeyword = false;

    if (keyword.equals("uc")) {
        /* count of characters to skip after a unicode character */
        parserState.put("UnicodeSkip", Integer.valueOf(parameter));
        return true;
    }
    if (keyword.equals("u")) {
        if (parameter < 0)
            parameter = parameter + 65536;
        handleText((char)parameter);
        Number skip = (Number)(parserState.get("UnicodeSkip"));
        if (skip != null) {
            skippingCharacters = skip.intValue();
        } else {
            skippingCharacters = 1;
        }
        return true;
    }

    if (keyword.equals("rtf")) {
        rtfversion = parameter;
        setRTFDestination(new DocumentDestination());
        return true;
    }

    if (keyword.startsWith("NeXT") ||
        keyword.equals("private"))
        ignoreGroupIfUnknownKeywordSave = true;

     if (keyword.contains("ansicpg")) {
         setCharacterSet("ansicpg");
         return true;
     }

    if (rtfDestination != null) {
        if(rtfDestination.handleKeyword(keyword, parameter))
            return true;
    }

    /* this point is reached only if the keyword is unrecognized */

    if (ignoreGroupIfUnknownKeywordSave) {
        setRTFDestination(new DiscardingDestination());
    }

    return false;
}

private void setTargetAttribute(String name, Object value)
{
//    target.changeAttributes(new LFDictionary(LFArray.arrayWithObject(value), LFArray.arrayWithObject(name)));
}

/**
 * setCharacterSet sets the current translation table to correspond with
 * the named character set. The character set is loaded if necessary.
 *
 * @see AbstractFilter
 */
public void setCharacterSet(String name)
{
    Object set;

    try {
        set = getCharacterSet(name);
    } catch (Exception e) {
        warning("Exception loading RTF character set \"" + name + "\": " + e);
        set = null;
    }

    if (set != null) {
        translationTable = (char[])set;
    } else {
        warning("Unknown RTF character set \"" + name + "\"");
        if (!name.equals("ansi")) {
            try {
                translationTable = (char[])getCharacterSet("ansi");
            } catch (IOException e) {
                throw new InternalError("RTFReader: Unable to find character set resources (" + e + ")", e);
            }
        }
    }

    setTargetAttribute(Constants.RTFCharacterSet, name);
}

/** Adds a character set to the RTFReader's list
 *  of known character sets */
public static void
defineCharacterSet(String name, char[] table)
{
    if (table.length < 256)
        throw new IllegalArgumentException("Translation table must have 256 entries.");
    characterSets.put(name, table);
}

/** Looks up a named character set. A character set is a 256-entry
 *  array of characters, mapping unsigned byte values to their Unicode
 *  equivalents. The character set is loaded if necessary.
 *
 *  @return the character set
 */
public static Object
getCharacterSet(final String name)
    throws IOException
{
    char[] set = characterSets.get(name);
    if (set == null) {
        @SuppressWarnings("removal")
        InputStream charsetStream = AccessController.doPrivileged(
                new PrivilegedAction<InputStream>() {
                    public InputStream run() {
                        return RTFReader.class.getResourceAsStream("charsets/" + name + ".txt");
                    }
                });
        set = readCharset(charsetStream);
        defineCharacterSet(name, set);
    }
    return set;
}

/** Parses a character set from an InputStream. The character set
 * must contain 256 decimal integers, separated by whitespace, with
 * no punctuation. B- and C- style comments are allowed.
 *
 * @return the newly read character set
 */
static char[] readCharset(InputStream strm)
     throws IOException
{
    char[] values = new char[256];
    int i;
    StreamTokenizer in = new StreamTokenizer(new BufferedReader(
            new InputStreamReader(strm, ISO_8859_1)));

    in.eolIsSignificant(false);
    in.commentChar('#');
    in.slashSlashComments(true);
    in.slashStarComments(true);

    i = 0;
    while (i < 256) {
        int ttype;
        try {
            ttype = in.nextToken();
        } catch (Exception e) {
            throw new IOException("Unable to read from character set file (" + e + ")");
        }
        if (ttype != StreamTokenizer.TT_NUMBER) {
//          System.out.println("Bad token: type=" + ttype + " tok=" + in.sval);
            throw new IOException("Unexpected token in character set file");
//          continue;
        }
        values[i] = (char)(in.nval);
        i++;
    }

    return values;
}

static char[] readCharset(java.net.URL href)
     throws IOException
{
    return readCharset(href.openStream());
}

/** An interface (could be an entirely abstract class) describing
 *  a destination. The RTF reader always has a current destination
 *  which is where text is sent.
 *
 *  @see RTFReader
 */
interface Destination {
    void handleBinaryBlob(byte[] data);
    void handleText(String text);
    boolean handleKeyword(String keyword);
    boolean handleKeyword(String keyword, int parameter);

    void begingroup();
    void endgroup(Dictionary<Object, Object> oldState);

    void close();
}

/** This data-sink class is used to implement ignored destinations
 *  (e.g. {\*\blegga blah blah blah} )
 *  It accepts all keywords and text but does nothing with them. */
class DiscardingDestination implements Destination
{
    public void handleBinaryBlob(byte[] data)
    {
        /* Discard binary blobs. */
    }

    public void handleText(String text)
    {
        /* Discard text. */
    }

    public boolean handleKeyword(String text)
    {
        /* Accept and discard keywords. */
        return true;
    }

    public boolean handleKeyword(String text, int parameter)
    {
        /* Accept and discard parameterized keywords. */
        return true;
    }

    public void begingroup()
    {
        /* Ignore groups --- the RTFReader will keep track of the
           current group level as necessary */
    }

    public void endgroup(Dictionary<Object, Object> oldState)
    {
        /* Ignore groups */
    }

    public void close()
    {
        /* No end-of-destination cleanup needed */
    }
}

/** Reads the fonttbl group, inserting fonts into the RTFReader's
 *  fontTable dictionary. */
class FonttblDestination implements Destination
{
    int nextFontNumber;
    Integer fontNumberKey = null;
    String nextFontFamily;

    public void handleBinaryBlob(byte[] data)
    { /* Discard binary blobs. */ }

    public void handleText(String text)
    {
        int semicolon = text.indexOf(';');
        String fontName;

        if (semicolon > -1)
            fontName = text.substring(0, semicolon);
        else
            fontName = text;


        /* TODO: do something with the font family. */

        if (nextFontNumber == -1
            && fontNumberKey != null) {
            //font name might be broken across multiple calls
            fontName = fontTable.get(fontNumberKey) + fontName;
        } else {
            fontNumberKey = Integer.valueOf(nextFontNumber);
        }
        fontTable.put(fontNumberKey, fontName);

        nextFontNumber = -1;
        nextFontFamily = null;
    }

    public boolean handleKeyword(String keyword)
    {
        if (keyword.charAt(0) == 'f') {
            nextFontFamily = keyword.substring(1);
            return true;
        }

        return false;
    }

    public boolean handleKeyword(String keyword, int parameter)
    {
        if (keyword.equals("f")) {
            nextFontNumber = parameter;
            return true;
        }

        return false;
    }

    /* Groups are irrelevant. */
    public void begingroup() {}
    public void endgroup(Dictionary<Object, Object> oldState) {}

    /* currently, the only thing we do when the font table ends is
       dump its contents to the debugging log. */
    public void close()
    {
        Enumeration<Integer> nums = fontTable.keys();
        warning("Done reading font table.");
        while(nums.hasMoreElements()) {
            Integer num = nums.nextElement();
            warning("Number " + num + ": " + fontTable.get(num));
        }
    }
}

/** Reads the colortbl group. Upon end-of-group, the RTFReader's
 *  color table is set to an array containing the read colors. */
class ColortblDestination implements Destination
{
    int red, green, blue;
    Vector<Color> proTemTable;

    public ColortblDestination()
    {
        red = 0;
        green = 0;
        blue = 0;
        proTemTable = new Vector<Color>();
    }

    public void handleText(String text)
    {
        int index;

        for (index = 0; index < text.length(); index ++) {
            if (text.charAt(index) == ';') {
                Color newColor;
                newColor = new Color(red, green, blue);
                proTemTable.addElement(newColor);
            }
        }
    }

    public void close()
    {
        int count = proTemTable.size();
        warning("Done reading color table, " + count + " entries.");
        colorTable = new Color[count];
        proTemTable.copyInto(colorTable);
    }

    public boolean handleKeyword(String keyword, int parameter)
    {
        if (keyword.equals("red"))
            red = parameter;
        else if (keyword.equals("green"))
            green = parameter;
        else if (keyword.equals("blue"))
            blue = parameter;
        else
            return false;

        return true;
    }

    /* Colortbls don't understand any parameterless keywords */
    public boolean handleKeyword(String keyword) { return false; }

    /* Groups are irrelevant. */
    public void begingroup() {}
    public void endgroup(Dictionary<Object, Object> oldState) {}

    /* Shouldn't see any binary blobs ... */
    public void handleBinaryBlob(byte[] data) {}
}

/** Handles the stylesheet keyword. Styles are read and sorted
 *  into the three style arrays in the RTFReader. */
class StylesheetDestination
    extends DiscardingDestination
    implements Destination
{
    Dictionary<Integer, StyleDefiningDestination> definedStyles;

    public StylesheetDestination()
    {
        definedStyles = new Hashtable<Integer, StyleDefiningDestination>();
    }

    public void begingroup()
    {
        setRTFDestination(new StyleDefiningDestination());
    }

    public void close()
    {
        Vector<Style> chrStyles = new Vector<Style>();
        Vector<Style> pgfStyles = new Vector<Style>();
        Vector<Style> secStyles = new Vector<Style>();
        Enumeration<StyleDefiningDestination> styles = definedStyles.elements();
        while(styles.hasMoreElements()) {
            StyleDefiningDestination style;
            Style defined;
            style = styles.nextElement();
            defined = style.realize();
            warning("Style "+style.number+" ("+style.styleName+"): "+defined);
            String stype = (String)defined.getAttribute(Constants.StyleType);
            Vector<Style> toSet;
            if (stype.equals(Constants.STSection)) {
                toSet = secStyles;
            } else if (stype.equals(Constants.STCharacter)) {
                toSet = chrStyles;
            } else {
                toSet = pgfStyles;
            }
            if (toSet.size() <= style.number)
                toSet.setSize(style.number + 1);
            toSet.setElementAt(defined, style.number);
        }
        if (!(chrStyles.isEmpty())) {
            Style[] styleArray = new Style[chrStyles.size()];
            chrStyles.copyInto(styleArray);
            characterStyles = styleArray;
        }
        if (!(pgfStyles.isEmpty())) {
            Style[] styleArray = new Style[pgfStyles.size()];
            pgfStyles.copyInto(styleArray);
            paragraphStyles = styleArray;
        }
        if (!(secStyles.isEmpty())) {
            Style[] styleArray = new Style[secStyles.size()];
            secStyles.copyInto(styleArray);
            sectionStyles = styleArray;
        }

/* (old debugging code)
        int i, m;
        if (characterStyles != null) {
          m = characterStyles.length;
          for(i=0;i<m;i++)
            warnings.println("chrStyle["+i+"]="+characterStyles[i]);
        } else warnings.println("No character styles.");
        if (paragraphStyles != null) {
          m = paragraphStyles.length;
          for(i=0;i<m;i++)
            warnings.println("pgfStyle["+i+"]="+paragraphStyles[i]);
        } else warnings.println("No paragraph styles.");
        if (sectionStyles != null) {
          m = characterStyles.length;
          for(i=0;i<m;i++)
            warnings.println("secStyle["+i+"]="+sectionStyles[i]);
        } else warnings.println("No section styles.");
*/
    }

    /** This subclass handles an individual style */
    class StyleDefiningDestination
        extends AttributeTrackingDestination
        implements Destination
    {
        final int STYLENUMBER_NONE = 222;
        boolean additive;
        boolean characterStyle;
        boolean sectionStyle;
        public String styleName;
        public int number;
        int basedOn;
        int nextStyle;
        boolean hidden;

        Style realizedStyle;

        public StyleDefiningDestination()
        {
            additive = false;
            characterStyle = false;
            sectionStyle = false;
            styleName = null;
            number = 0;
            basedOn = STYLENUMBER_NONE;
            nextStyle = STYLENUMBER_NONE;
            hidden = false;
        }

        public void handleText(String text)
        {
            if (styleName != null)
                styleName = styleName + text;
            else
                styleName = text;
        }

        public void close() {
            int semicolon = (styleName == null) ? 0 : styleName.indexOf(';');
            if (semicolon > 0)
                styleName = styleName.substring(0, semicolon);
            definedStyles.put(Integer.valueOf(number), this);
            super.close();
        }

        public boolean handleKeyword(String keyword)
        {
            if (keyword.equals("additive")) {
                additive = true;
                return true;
            }
            if (keyword.equals("shidden")) {
                hidden = true;
                return true;
            }
            return super.handleKeyword(keyword);
        }

        public boolean handleKeyword(String keyword, int parameter)
        {
            if (keyword.equals("s")) {
                characterStyle = false;
                sectionStyle = false;
                number = parameter;
            } else if (keyword.equals("cs")) {
                characterStyle = true;
                sectionStyle = false;
                number = parameter;
            } else if (keyword.equals("ds")) {
                characterStyle = false;
                sectionStyle = true;
                number = parameter;
            } else if (keyword.equals("sbasedon")) {
                basedOn = parameter;
            } else if (keyword.equals("snext")) {
                nextStyle = parameter;
            } else {
                return super.handleKeyword(keyword, parameter);
            }
            return true;
        }

        public Style realize()
        {
            Style basis = null;
            Style next = null;

            if (realizedStyle != null)
                return realizedStyle;

            if (basedOn != STYLENUMBER_NONE) {
                StyleDefiningDestination styleDest;
                styleDest = definedStyles.get(Integer.valueOf(basedOn));
                if (styleDest != null && styleDest != this) {
                    basis = styleDest.realize();
                }
            }

            /* NB: Swing StyleContext doesn't allow distinct styles with
               the same name; RTF apparently does. This may confuse the
               user. */
            realizedStyle = target.addStyle(styleName, basis);

            if (characterStyle) {
                realizedStyle.addAttributes(currentTextAttributes());
                realizedStyle.addAttribute(Constants.StyleType,
                                           Constants.STCharacter);
            } else if (sectionStyle) {
                realizedStyle.addAttributes(currentSectionAttributes());
                realizedStyle.addAttribute(Constants.StyleType,
                                           Constants.STSection);
            } else { /* must be a paragraph style */
                realizedStyle.addAttributes(currentParagraphAttributes());
                realizedStyle.addAttribute(Constants.StyleType,
                                           Constants.STParagraph);
            }

            if (nextStyle != STYLENUMBER_NONE) {
                StyleDefiningDestination styleDest;
                styleDest = definedStyles.get(Integer.valueOf(nextStyle));
                if (styleDest != null) {
                    next = styleDest.realize();
                }
            }

            if (next != null)
                realizedStyle.addAttribute(Constants.StyleNext, next);
            realizedStyle.addAttribute(Constants.StyleAdditive,
                                       Boolean.valueOf(additive));
            realizedStyle.addAttribute(Constants.StyleHidden,
                                       Boolean.valueOf(hidden));

            return realizedStyle;
        }
    }
}

/** Handles the info group. Currently no info keywords are recognized
 *  so this is a subclass of DiscardingDestination. */
class InfoDestination
    extends DiscardingDestination
    implements Destination
{
}

/** RTFReader.TextHandlingDestination is an abstract RTF destination
 *  which simply tracks the attributes specified by the RTF control words
 *  in internal form and can produce acceptable AttributeSets for the
 *  current character, paragraph, and section attributes. It is up
 *  to the subclasses to determine what is done with the actual text. */
abstract class AttributeTrackingDestination implements Destination
{
    /** This is the "chr" element of parserState, cached for
     *  more efficient use */
    MutableAttributeSet characterAttributes;
    /** This is the "pgf" element of parserState, cached for
     *  more efficient use */
    MutableAttributeSet paragraphAttributes;
    /** This is the "sec" element of parserState, cached for
     *  more efficient use */
    MutableAttributeSet sectionAttributes;

    public AttributeTrackingDestination()
    {
        characterAttributes = rootCharacterAttributes();
        parserState.put("chr", characterAttributes);
        paragraphAttributes = rootParagraphAttributes();
        parserState.put("pgf", paragraphAttributes);
        sectionAttributes = rootSectionAttributes();
        parserState.put("sec", sectionAttributes);
    }

    public abstract void handleText(String text);

    public void handleBinaryBlob(byte[] data)
    {
        /* This should really be in TextHandlingDestination, but
         * since *nobody* does anything with binary blobs, this
         * is more convenient. */
        warning("Unexpected binary data in RTF file.");
    }

    public void begingroup()
    {
        AttributeSet characterParent = currentTextAttributes();
        AttributeSet paragraphParent = currentParagraphAttributes();
        AttributeSet sectionParent = currentSectionAttributes();

        /* It would probably be more efficient to use the
         * resolver property of the attributes set for
         * implementing rtf groups,
         * but that's needed for styles. */

        /* update the cached attribute dictionaries */
        characterAttributes = new SimpleAttributeSet();
        characterAttributes.addAttributes(characterParent);
        parserState.put("chr", characterAttributes);

        paragraphAttributes = new SimpleAttributeSet();
        paragraphAttributes.addAttributes(paragraphParent);
        parserState.put("pgf", paragraphAttributes);

        sectionAttributes = new SimpleAttributeSet();
        sectionAttributes.addAttributes(sectionParent);
        parserState.put("sec", sectionAttributes);
    }

    public void endgroup(Dictionary<Object, Object> oldState)
    {
        characterAttributes = (MutableAttributeSet)parserState.get("chr");
        paragraphAttributes = (MutableAttributeSet)parserState.get("pgf");
        sectionAttributes   = (MutableAttributeSet)parserState.get("sec");
    }

    public void close()
    {
    }

    public boolean handleKeyword(String keyword)
    {
        if (keyword.equals("ulnone")) {
            return handleKeyword("ul", 0);
        }

        {
            RTFAttribute attr = straightforwardAttributes.get(keyword);
            if (attr != null) {
                boolean ok;

                switch(attr.domain()) {
                  case RTFAttribute.D_CHARACTER:
                    ok = attr.set(characterAttributes);
                    break;
                  case RTFAttribute.D_PARAGRAPH:
                    ok = attr.set(paragraphAttributes);
                    break;
                  case RTFAttribute.D_SECTION:
                    ok = attr.set(sectionAttributes);
                    break;
                  case RTFAttribute.D_META:
                    mockery.backing = parserState;
                    ok = attr.set(mockery);
                    mockery.backing = null;
                    break;
                  case RTFAttribute.D_DOCUMENT:
                    ok = attr.set(documentAttributes);
                    break;
                  default:
                    /* should never happen */
                    ok = false;
                    break;
                }
                if (ok)
                    return true;
            }
        }


        if (keyword.equals("plain")) {
            resetCharacterAttributes();
            return true;
        }

        if (keyword.equals("pard")) {
            resetParagraphAttributes();
            return true;
        }

        if (keyword.equals("sectd")) {
            resetSectionAttributes();
            return true;
        }

        return false;
    }

    public boolean handleKeyword(String keyword, int parameter)
    {
        boolean booleanParameter = (parameter != 0);

        if (keyword.equals("fc"))
            keyword = "cf"; /* whatEVER, dude. */

        if (keyword.equals("f")) {
            parserState.put(keyword, Integer.valueOf(parameter));
            return true;
        }
        if (keyword.equals("cf")) {
            parserState.put(keyword, Integer.valueOf(parameter));
            return true;
        }
        if (keyword.equals("cb")) {
            parserState.put(keyword, Integer.valueOf(parameter));
            return true;
        }

        {
            RTFAttribute attr = straightforwardAttributes.get(keyword);
            if (attr != null) {
                boolean ok;

                switch(attr.domain()) {
                  case RTFAttribute.D_CHARACTER:
                    ok = attr.set(characterAttributes, parameter);
                    break;
                  case RTFAttribute.D_PARAGRAPH:
                    ok = attr.set(paragraphAttributes, parameter);
                    break;
                  case RTFAttribute.D_SECTION:
                    ok = attr.set(sectionAttributes, parameter);
                    break;
                  case RTFAttribute.D_META:
                    mockery.backing = parserState;
                    ok = attr.set(mockery, parameter);
                    mockery.backing = null;
                    break;
                  case RTFAttribute.D_DOCUMENT:
                    ok = attr.set(documentAttributes, parameter);
                    break;
                  default:
                    /* should never happen */
                    ok = false;
                    break;
                }
                if (ok)
                    return true;
            }
        }

        if (keyword.equals("fs")) {
            StyleConstants.setFontSize(characterAttributes, (parameter / 2));
            return true;
        }

        /* TODO: superscript/subscript */

        if (keyword.equals("sl")) {
            if (parameter == 1000) {  /* magic value! */
                characterAttributes.removeAttribute(StyleConstants.LineSpacing);
            } else {
                /* TODO: The RTF sl attribute has special meaning if it's
                   negative. Make sure that SwingText has the same special
                   meaning, or find a way to imitate that. When SwingText
                   handles this, also recognize the slmult keyword. */
                StyleConstants.setLineSpacing(characterAttributes,
                                              parameter / 20f);
            }
            return true;
        }

        /* TODO: Other kinds of underlining */

        if (keyword.equals("tx") || keyword.equals("tb")) {
            float tabPosition = parameter / 20f;
            int tabAlignment, tabLeader;
            Number item;

            tabAlignment = TabStop.ALIGN_LEFT;
            item = (Number)(parserState.get("tab_alignment"));
            if (item != null)
                tabAlignment = item.intValue();
            tabLeader = TabStop.LEAD_NONE;
            item = (Number)(parserState.get("tab_leader"));
            if (item != null)
                tabLeader = item.intValue();
            if (keyword.equals("tb"))
                tabAlignment = TabStop.ALIGN_BAR;

            parserState.remove("tab_alignment");
            parserState.remove("tab_leader");

            TabStop newStop = new TabStop(tabPosition, tabAlignment, tabLeader);
            Dictionary<Object, Object> tabs;
            Integer stopCount;

            @SuppressWarnings("unchecked")
            Dictionary<Object, Object>tmp = (Dictionary)parserState.get("_tabs");
            tabs = tmp;
            if (tabs == null) {
                tabs = new Hashtable<Object, Object>();
                parserState.put("_tabs", tabs);
                stopCount = Integer.valueOf(1);
            } else {
                stopCount = (Integer)tabs.get("stop count");
                stopCount = Integer.valueOf(1 + stopCount.intValue());
            }
            tabs.put(stopCount, newStop);
            tabs.put("stop count", stopCount);
            parserState.remove("_tabs_immutable");

            return true;
        }

        if (keyword.equals("s") &&
            paragraphStyles != null) {
            parserState.put("paragraphStyle", paragraphStyles[parameter]);
            return true;
        }

        if (keyword.equals("cs") &&
            characterStyles != null) {
            parserState.put("characterStyle", characterStyles[parameter]);
            return true;
        }

        if (keyword.equals("ds") &&
            sectionStyles != null) {
            parserState.put("sectionStyle", sectionStyles[parameter]);
            return true;
        }

        return false;
    }

    /** Returns a new MutableAttributeSet containing the
     *  default character attributes */
    protected MutableAttributeSet rootCharacterAttributes()
    {
        MutableAttributeSet set = new SimpleAttributeSet();

        /* TODO: default font */

        StyleConstants.setItalic(set, false);
        StyleConstants.setBold(set, false);
        StyleConstants.setUnderline(set, false);
        StyleConstants.setForeground(set, defaultColor());

        return set;
    }

    /** Returns a new MutableAttributeSet containing the
     *  default paragraph attributes */
    protected MutableAttributeSet rootParagraphAttributes()
    {
        MutableAttributeSet set = new SimpleAttributeSet();

        StyleConstants.setLeftIndent(set, 0f);
        StyleConstants.setRightIndent(set, 0f);
        StyleConstants.setFirstLineIndent(set, 0f);

        /* TODO: what should this be, really? */
        set.setResolveParent(target.getStyle(StyleContext.DEFAULT_STYLE));

        return set;
    }

    /** Returns a new MutableAttributeSet containing the
     *  default section attributes */
    protected MutableAttributeSet rootSectionAttributes()
    {
        MutableAttributeSet set = new SimpleAttributeSet();

        return set;
    }

    /**
     * Calculates the current text (character) attributes in a form suitable
     * for SwingText from the current parser state.
     *
     * @return a new MutableAttributeSet containing the text attributes.
     */
    MutableAttributeSet currentTextAttributes()
    {
        MutableAttributeSet attributes =
            new SimpleAttributeSet(characterAttributes);
        Integer fontnum;
        Integer stateItem;

        /* figure out the font name */
        /* TODO: catch exceptions for undefined attributes,
           bad font indices, etc.? (as it stands, it is the caller's
           job to clean up after corrupt RTF) */
        fontnum = (Integer)parserState.get("f");
        /* note setFontFamily() can not handle a null font */
        String fontFamily;
        if (fontnum != null)
            fontFamily = fontTable.get(fontnum);
        else
            fontFamily = null;
        if (fontFamily != null)
            StyleConstants.setFontFamily(attributes, fontFamily);
        else
            attributes.removeAttribute(StyleConstants.FontFamily);

        if (colorTable != null) {
            stateItem = (Integer)parserState.get("cf");
            if (stateItem != null) {
                Color fg = colorTable[stateItem.intValue()];
                StyleConstants.setForeground(attributes, fg);
            } else {
                /* AttributeSet dies if you set a value to null */
                attributes.removeAttribute(StyleConstants.Foreground);
            }
        }

        if (colorTable != null) {
            stateItem = (Integer)parserState.get("cb");
            if (stateItem != null) {
                Color bg = colorTable[stateItem.intValue()];
                attributes.addAttribute(StyleConstants.Background,
                                        bg);
            } else {
                /* AttributeSet dies if you set a value to null */
                attributes.removeAttribute(StyleConstants.Background);
            }
        }

        Style characterStyle = (Style)parserState.get("characterStyle");
        if (characterStyle != null)
            attributes.setResolveParent(characterStyle);

        /* Other attributes are maintained directly in "attributes" */

        return attributes;
    }

    /**
     * Calculates the current paragraph attributes (with keys
     * as given in StyleConstants) from the current parser state.
     *
     * @return a newly created MutableAttributeSet.
     * @see StyleConstants
     */
    MutableAttributeSet currentParagraphAttributes()
    {
        /* NB if there were a mutableCopy() method we should use it */
        MutableAttributeSet bld = new SimpleAttributeSet(paragraphAttributes);

        Integer stateItem;

        /*** Tab stops ***/
        TabStop[] tabs;

        tabs = (TabStop[])parserState.get("_tabs_immutable");
        if (tabs == null) {
            @SuppressWarnings("unchecked")
            Dictionary<Object, Object> workingTabs = (Dictionary)parserState.get("_tabs");
            if (workingTabs != null) {
                int count = ((Integer)workingTabs.get("stop count")).intValue();
                tabs = new TabStop[count];
                for (int ix = 1; ix <= count; ix ++)
                    tabs[ix-1] = (TabStop)workingTabs.get(Integer.valueOf(ix));
                parserState.put("_tabs_immutable", tabs);
            }
        }
        if (tabs != null)
            bld.addAttribute(Constants.Tabs, tabs);

        Style paragraphStyle = (Style)parserState.get("paragraphStyle");
        if (paragraphStyle != null)
            bld.setResolveParent(paragraphStyle);

        return bld;
    }

    /**
     * Calculates the current section attributes
     * from the current parser state.
     *
     * @return a newly created MutableAttributeSet.
     */
    public AttributeSet currentSectionAttributes()
    {
        MutableAttributeSet attributes = new SimpleAttributeSet(sectionAttributes);

        Style sectionStyle = (Style)parserState.get("sectionStyle");
        if (sectionStyle != null)
            attributes.setResolveParent(sectionStyle);

        return attributes;
    }

    /** Resets the filter's internal notion of the current character
     *  attributes to their default values. Invoked to handle the
     *  \plain keyword. */
    protected void resetCharacterAttributes()
    {
        handleKeyword("f", 0);
        handleKeyword("cf", 0);

        handleKeyword("fs", 24);  /* 12 pt. */

        Enumeration<RTFAttribute> attributes = straightforwardAttributes.elements();
        while(attributes.hasMoreElements()) {
            RTFAttribute attr = attributes.nextElement();
            if (attr.domain() == RTFAttribute.D_CHARACTER)
                attr.setDefault(characterAttributes);
        }

        handleKeyword("sl", 1000);

        parserState.remove("characterStyle");
    }

    /** Resets the filter's internal notion of the current paragraph's
     *  attributes to their default values. Invoked to handle the
     *  \pard keyword. */
    protected void resetParagraphAttributes()
    {
        parserState.remove("_tabs");
        parserState.remove("_tabs_immutable");
        parserState.remove("paragraphStyle");

        StyleConstants.setAlignment(paragraphAttributes,
                                    StyleConstants.ALIGN_LEFT);

        Enumeration<RTFAttribute> attributes = straightforwardAttributes.elements();
        while(attributes.hasMoreElements()) {
            RTFAttribute attr = attributes.nextElement();
            if (attr.domain() == RTFAttribute.D_PARAGRAPH)
                attr.setDefault(characterAttributes);
        }
    }

    /** Resets the filter's internal notion of the current section's
     *  attributes to their default values. Invoked to handle the
     *  \sectd keyword. */
    protected void resetSectionAttributes()
    {
        Enumeration<RTFAttribute> attributes = straightforwardAttributes.elements();
        while(attributes.hasMoreElements()) {
            RTFAttribute attr = attributes.nextElement();
            if (attr.domain() == RTFAttribute.D_SECTION)
                attr.setDefault(characterAttributes);
        }

        parserState.remove("sectionStyle");
    }
}

/** RTFReader.TextHandlingDestination provides basic text handling
 *  functionality. Subclasses must implement: <dl>
 *  <dt>deliverText()<dd>to handle a run of text with the same
 *                       attributes
 *  <dt>finishParagraph()<dd>to end the current paragraph and
 *                           set the paragraph's attributes
 *  <dt>endSection()<dd>to end the current section
 *  </dl>
 */
abstract class TextHandlingDestination
    extends AttributeTrackingDestination
    implements Destination
{
    /** <code>true</code> if the reader has not just finished
     *  a paragraph; false upon startup */
    boolean inParagraph;

    public TextHandlingDestination()
    {
        super();
        inParagraph = false;
    }

    public void handleText(String text)
    {
        if (! inParagraph)
            beginParagraph();

        deliverText(text, currentTextAttributes());
    }

    abstract void deliverText(String text, AttributeSet characterAttributes);

    public void close()
    {
        if (inParagraph)
            endParagraph();

        super.close();
    }

    public boolean handleKeyword(String keyword)
    {
        if (keyword.equals("\r") || keyword.equals("\n")) {
            keyword = "par";
        }

        if (keyword.equals("par")) {
//          warnings.println("Ending paragraph.");
            endParagraph();
            return true;
        }

        if (keyword.equals("sect")) {
//          warnings.println("Ending section.");
            endSection();
            return true;
        }

        return super.handleKeyword(keyword);
    }

    protected void beginParagraph()
    {
        inParagraph = true;
    }

    protected void endParagraph()
    {
        AttributeSet pgfAttributes = currentParagraphAttributes();
        AttributeSet chrAttributes = currentTextAttributes();
        finishParagraph(pgfAttributes, chrAttributes);
        inParagraph = false;
    }

    abstract void finishParagraph(AttributeSet pgfA, AttributeSet chrA);

    abstract void endSection();
}

/** RTFReader.DocumentDestination is a concrete subclass of
 *  TextHandlingDestination which appends the text to the
 *  StyledDocument given by the <code>target</code> ivar of the
 *  containing RTFReader.
 */
class DocumentDestination
    extends TextHandlingDestination
    implements Destination
{
    public void deliverText(String text, AttributeSet characterAttributes)
    {
        try {
            target.insertString(target.getLength(),
                                text,
                                currentTextAttributes());
        } catch (BadLocationException ble) {
            /* This shouldn't be able to happen, of course */
            /* TODO is InternalError the correct error to throw? */
            throw new InternalError(ble.getMessage(), ble);
        }
    }

    public void finishParagraph(AttributeSet pgfAttributes,
                                AttributeSet chrAttributes)
    {
        int pgfEndPosition = target.getLength();
        try {
            target.insertString(pgfEndPosition, "\n", chrAttributes);
            target.setParagraphAttributes(pgfEndPosition, 1, pgfAttributes, true);
        } catch (BadLocationException ble) {
            /* This shouldn't be able to happen, of course */
            /* TODO is InternalError the correct error to throw? */
            throw new InternalError(ble.getMessage(), ble);
        }
    }

    public void endSection()
    {
        /* If we implemented sections, we'd end 'em here */
    }
}

}
