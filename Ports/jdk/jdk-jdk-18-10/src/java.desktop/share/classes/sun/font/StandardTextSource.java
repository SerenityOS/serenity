/*
 * Copyright (c) 1998, 2015, Oracle and/or its affiliates. All rights reserved.
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
/*
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 */

package sun.font;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;

final class StandardTextSource extends TextSource {

    private final char[] chars;
    private final int start;
    private final int len;
    private final int cstart;
    private final int clen;
    private final int level; // assumed all uniform
    private final int flags; // see GlyphVector.java
    private final Font font;
    private final FontRenderContext frc;
    private final CoreMetrics cm;

    /**
     * Create a simple implementation of a TextSource.
     *
     * Chars is an array containing clen chars in the context, in
     * logical order, contiguously starting at cstart.  Start and len
     * represent that portion of the context representing the true
     * source; start, like cstart, is relative to the start of the
     * character array.
     *
     * Level is the bidi level (0-63 for the entire context. Flags is
     * the layout flags. Font is the font, frc is the render context,
     * and lm is the line metrics for the entire source text, but not
     * necessarily the context.
     */
    StandardTextSource(char[] chars,
                       int start,
                       int len,
                       int cstart,
                       int clen,
                       int level,
                       int flags,
                       Font font,
                       FontRenderContext frc,
                       CoreMetrics cm) {
    if (chars == null) {
      throw new IllegalArgumentException("bad chars: null");
    }
    if (cstart < 0) {
      throw new IllegalArgumentException("bad cstart: " + cstart);
    }
    if (start < cstart) {
      throw new IllegalArgumentException("bad start: " + start + " for cstart: " + cstart);
    }
    if (clen < 0) {
      throw new IllegalArgumentException("bad clen: " + clen);
    }
    if (cstart + clen > chars.length) {
      throw new IllegalArgumentException("bad clen: " + clen + " cstart: " + cstart + " for array len: " + chars.length);
    }
    if (len < 0) {
      throw new IllegalArgumentException("bad len: " + len);
    }
    if ((start + len) > (cstart + clen)) {
      throw new IllegalArgumentException("bad len: " + len + " start: " + start + " for cstart: " + cstart + " clen: " + clen);
    }
    if (font == null) {
      throw new IllegalArgumentException("bad font: null");
    }
    if (frc == null) {
      throw new IllegalArgumentException("bad frc: null");
    }

    this.chars = chars;
    this.start = start;
    this.len = len;
    this.cstart = cstart;
    this.clen = clen;
    this.level = level;
    this.flags = flags;
    this.font = font;
    this.frc = frc;

    if (cm != null) {
        this.cm = cm;
    } else {
        LineMetrics metrics = font.getLineMetrics(chars, cstart, clen, frc);
        this.cm = ((FontLineMetrics)metrics).cm;
    }
  }

  // TextSource API

  public char[] getChars() {
    return chars;
  }

  public int getStart() {
    return start;
  }

  public int getLength() {
    return len;
  }

  public int getContextStart() {
    return cstart;
  }

  public int getContextLength() {
    return clen;
  }

  public int getLayoutFlags() {
    return flags;
  }

  public int getBidiLevel() {
    return level;
  }

  public Font getFont() {
    return font;
  }

  public FontRenderContext getFRC() {
    return frc;
  }

  public CoreMetrics getCoreMetrics() {
    return cm;
  }

  public TextSource getSubSource(int start, int length, int dir) {
    if (start < 0 || length < 0 || (start + length) > len) {
      throw new IllegalArgumentException("bad start (" + start + ") or length (" + length + ")");
    }

    int level = this.level;
    if (dir != TextLineComponent.UNCHANGED) {
        boolean ltr = (flags & 0x8) == 0;
        if (!(dir == TextLineComponent.LEFT_TO_RIGHT && ltr) &&
                !(dir == TextLineComponent.RIGHT_TO_LEFT && !ltr)) {
            throw new IllegalArgumentException("direction flag is invalid");
        }
        level = ltr? 0 : 1;
    }

    return new StandardTextSource(chars, this.start + start, length, cstart, clen, level, flags, font, frc, cm);
  }

  public String toString() {
    return toString(WITH_CONTEXT);
  }

  public String toString(boolean withContext) {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.append("[start:");
    sb.append(start);
    sb.append(", len:" );
    sb.append(len);
    sb.append(", cstart:");
    sb.append(cstart);
    sb.append(", clen:" );
    sb.append(clen);
    sb.append(", chars:\"");
    int chStart, chLimit;
    if (withContext == WITH_CONTEXT) {
        chStart = cstart;
        chLimit = cstart + clen;
    }
    else {
        chStart = start;
        chLimit = start + len;
    }
    for (int i = chStart; i < chLimit; ++i) {
      if (i > chStart) {
        sb.append(" ");
      }
      sb.append(Integer.toHexString(chars[i]));
    }
    sb.append("\"");
    sb.append(", level:");
    sb.append(level);
    sb.append(", flags:");
    sb.append(flags);
    sb.append(", font:");
    sb.append(font);
    sb.append(", frc:");
    sb.append(frc);
    sb.append(", cm:");
    sb.append(cm);
    sb.append("]");

    return sb.toString();
  }
}
