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

import java.io.*;
import java.lang.*;

/**
 * <b>RTFParser</b> is a subclass of <b>AbstractFilter</b> which understands basic RTF syntax
 * and passes a stream of control words, text, and begin/end group
 * indications to its subclass.
 *
 * Normally programmers will only use <b>RTFReader</b>, a subclass of this class that knows what to
 * do with the tokens this class parses.
 *
 * @see AbstractFilter
 * @see RTFReader
 */
abstract class RTFParser extends AbstractFilter
{
  /** The current RTF group nesting level. */
  public int level;

  private int state;
  private StringBuffer currentCharacters;
  private String pendingKeyword;                // where keywords go while we
                                                // read their parameters
  private int pendingCharacter;                 // for the \'xx construct

  private long binaryBytesLeft;                  // in a \bin blob?
  ByteArrayOutputStream binaryBuf;
  private boolean[] savedSpecials;

  /** A stream to which to write warnings and debugging information
   *  while parsing. This is set to <code>System.out</code> to log
   *  any anomalous information to stdout. */
  protected PrintStream warnings;

  // value for the 'state' variable
  private final int S_text = 0;          // reading random text
  private final int S_backslashed = 1;   // read a backslash, waiting for next
  private final int S_token = 2;         // reading a multicharacter token
  private final int S_parameter = 3;     // reading a token's parameter

  private final int S_aftertick = 4;     // after reading \'
  private final int S_aftertickc = 5;    // after reading \'x

  private final int S_inblob = 6;        // in a \bin blob

  /** Implemented by subclasses to interpret a parameter-less RTF keyword.
   *  The keyword is passed without the leading '/' or any delimiting
   *  whitespace. */
  public abstract boolean handleKeyword(String keyword);
  /** Implemented by subclasses to interpret a keyword with a parameter.
   *  @param keyword   The keyword, as with <code>handleKeyword(String)</code>.
   *  @param parameter The parameter following the keyword. */
  public abstract boolean handleKeyword(String keyword, int parameter);
  /** Implemented by subclasses to interpret text from the RTF stream. */
  public abstract void handleText(String text);
  public void handleText(char ch)
  { handleText(String.valueOf(ch)); }
  /** Implemented by subclasses to handle the contents of the \bin keyword. */
  public abstract void handleBinaryBlob(byte[] data);
  /** Implemented by subclasses to react to an increase
   *  in the nesting level. */
  public abstract void begingroup();
  /** Implemented by subclasses to react to the end of a group. */
  public abstract void endgroup();

  // table of non-text characters in rtf
  static final boolean[] rtfSpecialsTable;
  static {
    rtfSpecialsTable = noSpecialsTable.clone();
    rtfSpecialsTable['\n'] = true;
    rtfSpecialsTable['\r'] = true;
    rtfSpecialsTable['{'] = true;
    rtfSpecialsTable['}'] = true;
    rtfSpecialsTable['\\'] = true;
  }

  public RTFParser()
  {
    currentCharacters = new StringBuffer();
    state = S_text;
    pendingKeyword = null;
    level = 0;
    //warnings = System.out;

    specialsTable = rtfSpecialsTable;
  }

  // TODO: Handle wrapup at end of file correctly.

  public void writeSpecial(int b)
    throws IOException
  {
    write((char)b);
  }

    protected void warning(String s) {
        if (warnings != null) {
            warnings.println(s);
        }
    }

  public void write(String s)
    throws IOException
  {
    if (state != S_text) {
      int index = 0;
      int length = s.length();
      while(index < length && state != S_text) {
        write(s.charAt(index));
        index ++;
      }

      if(index >= length)
        return;

      s = s.substring(index);
    }

    if (currentCharacters.length() > 0)
      currentCharacters.append(s);
    else
      handleText(s);
  }

  @SuppressWarnings("fallthrough")
  public void write(char ch)
    throws IOException
  {
    boolean ok;

    switch (state)
    {
      case S_text:
        if (ch == '\n' || ch == '\r') {
          break;  // unadorned newlines are ignored
        } else if (ch == '{') {
          if (currentCharacters.length() > 0) {
            handleText(currentCharacters.toString());
            currentCharacters = new StringBuffer();
          }
          level ++;
          begingroup();
        } else if(ch == '}') {
          if (currentCharacters.length() > 0) {
            handleText(currentCharacters.toString());
            currentCharacters = new StringBuffer();
          }
          if (level == 0)
            throw new IOException("Too many close-groups in RTF text");
          endgroup();
          level --;
        } else if(ch == '\\') {
          if (currentCharacters.length() > 0) {
            handleText(currentCharacters.toString());
            currentCharacters = new StringBuffer();
          }
          state = S_backslashed;
        } else {
          currentCharacters.append(ch);
        }
        break;
      case S_backslashed:
        if (ch == '\'') {
          state = S_aftertick;
          break;
        }
        if (!Character.isLetter(ch)) {
          char[] newstring = new char[1];
          newstring[0] = ch;
          if (!handleKeyword(new String(newstring))) {
            warning("Unknown keyword: " + newstring + " (" + (byte)ch + ")");
          }
          state = S_text;
          pendingKeyword = null;
          /* currentCharacters is already an empty stringBuffer */
          break;
        }

        state = S_token;
        /* FALL THROUGH */
      case S_token:
        if (Character.isLetter(ch)) {
          currentCharacters.append(ch);
        } else {
          pendingKeyword = currentCharacters.toString();
          currentCharacters = new StringBuffer();

          // Parameter following?
          if (Character.isDigit(ch) || (ch == '-')) {
            state = S_parameter;
            currentCharacters.append(ch);
          } else {
            ok = handleKeyword(pendingKeyword);
            if (!ok)
              warning("Unknown keyword: " + pendingKeyword);
            pendingKeyword = null;
            state = S_text;

            // Non-space delimiters get included in the text
            if (!Character.isWhitespace(ch))
              write(ch);
          }
        }
        break;
      case S_parameter:
        if (Character.isDigit(ch)) {
          currentCharacters.append(ch);
        } else {
          /* TODO: Test correct behavior of \bin keyword */
          if (pendingKeyword.equals("bin")) {  /* magic layer-breaking kwd */
            long parameter = Long.parseLong(currentCharacters.toString());
            pendingKeyword = null;
            state = S_inblob;
            binaryBytesLeft = parameter;
            if (binaryBytesLeft > Integer.MAX_VALUE)
                binaryBuf = new ByteArrayOutputStream(Integer.MAX_VALUE);
            else
                binaryBuf = new ByteArrayOutputStream((int)binaryBytesLeft);
            savedSpecials = specialsTable;
            specialsTable = allSpecialsTable;
            break;
          }

          int parameter = Integer.parseInt(currentCharacters.toString());
          ok = handleKeyword(pendingKeyword, parameter);
          if (!ok)
            warning("Unknown keyword: " + pendingKeyword +
                    " (param " + currentCharacters + ")");
          pendingKeyword = null;
          currentCharacters = new StringBuffer();
          state = S_text;

          // Delimiters here are interpreted as text too
          if (!Character.isWhitespace(ch))
            write(ch);
        }
        break;
      case S_aftertick:
        if (Character.digit(ch, 16) == -1)
          state = S_text;
        else {
          pendingCharacter = Character.digit(ch, 16);
          state = S_aftertickc;
        }
        break;
      case S_aftertickc:
        state = S_text;
        if (Character.digit(ch, 16) != -1)
        {
          pendingCharacter = pendingCharacter * 16 + Character.digit(ch, 16);
          ch = translationTable[pendingCharacter];
          if (ch != 0)
              handleText(ch);
        }
        break;
      case S_inblob:
        binaryBuf.write(ch);
        binaryBytesLeft --;
        if (binaryBytesLeft == 0) {
            state = S_text;
            specialsTable = savedSpecials;
            savedSpecials = null;
            handleBinaryBlob(binaryBuf.toByteArray());
            binaryBuf = null;
        }
      }
  }

  /** Flushes any buffered but not yet written characters.
   *  Subclasses which override this method should call this
   *  method <em>before</em> flushing
   *  any of their own buffers. */
  public void flush()
    throws IOException
  {
    super.flush();

    if (state == S_text && currentCharacters.length() > 0) {
      handleText(currentCharacters.toString());
      currentCharacters = new StringBuffer();
    }
  }

  /** Closes the parser. Currently, this simply does a <code>flush()</code>,
   *  followed by some minimal consistency checks. */
  public void close()
    throws IOException
  {
    flush();

    if (state != S_text || level > 0) {
      warning("Truncated RTF file.");

      /* TODO: any sane way to handle termination in a non-S_text state? */
      /* probably not */

      /* this will cause subclasses to behave more reasonably
         some of the time */
      while (level > 0) {
          endgroup();
          level --;
      }
    }

    super.close();
  }

}
