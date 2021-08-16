/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.font;

/**
 * The {@code OpenType} interface represents OpenType and
 * TrueType fonts.  This interface makes it possible to obtain
 * <i>sfnt</i> tables from the font.  A particular
 * {@code Font} object can implement this interface.
 * <p>
 * For more information on TrueType and OpenType fonts, see the
 * OpenType specification.
 * ( <a href="http://www.microsoft.com/typography/otspec/">http://www.microsoft.com/typography/otspec/</a> ).
 */
public interface OpenType {

  /* 51 tag types so far */

  /**
   * Character to glyph mapping.  Table tag "cmap" in the Open
   * Type Specification.
   */
  public static final int       TAG_CMAP        = 0x636d6170;

  /**
   * Font header.  Table tag "head" in the Open
   * Type Specification.
   */
  public static final int       TAG_HEAD        = 0x68656164;

  /**
   * Naming table.  Table tag "name" in the Open
   * Type Specification.
   */
  public static final int       TAG_NAME        = 0x6e616d65;

  /**
   * Glyph data.  Table tag "glyf" in the Open
   * Type Specification.
   */
  public static final int       TAG_GLYF        = 0x676c7966;

  /**
   * Maximum profile.  Table tag "maxp" in the Open
   * Type Specification.
   */
  public static final int       TAG_MAXP        = 0x6d617870;

  /**
   * CVT preprogram.  Table tag "prep" in the Open
   * Type Specification.
   */
  public static final int       TAG_PREP        = 0x70726570;

  /**
   * Horizontal metrics.  Table tag "hmtx" in the Open
   * Type Specification.
   */
  public static final int       TAG_HMTX        = 0x686d7478;

  /**
   * Kerning.  Table tag "kern" in the Open
   * Type Specification.
   */
  public static final int       TAG_KERN        = 0x6b65726e;

  /**
   * Horizontal device metrics.  Table tag "hdmx" in the Open
   * Type Specification.
   */
  public static final int       TAG_HDMX        = 0x68646d78;

  /**
   * Index to location.  Table tag "loca" in the Open
   * Type Specification.
   */
  public static final int       TAG_LOCA        = 0x6c6f6361;

  /**
   * PostScript Information.  Table tag "post" in the Open
   * Type Specification.
   */
  public static final int       TAG_POST        = 0x706f7374;

  /**
   * OS/2 and Windows specific metrics.  Table tag "OS/2"
   * in the Open Type Specification.
   */
  public static final int       TAG_OS2 = 0x4f532f32;

  /**
   * Control value table.  Table tag "cvt "
   * in the Open Type Specification.
   */
  public static final int       TAG_CVT = 0x63767420;

  /**
   * Grid-fitting and scan conversion procedure.  Table tag
   * "gasp" in the Open Type Specification.
   */
  public static final int       TAG_GASP        = 0x67617370;

  /**
   * Vertical device metrics.  Table tag "VDMX" in the Open
   * Type Specification.
   */
  public static final int       TAG_VDMX        = 0x56444d58;

  /**
   * Vertical metrics.  Table tag "vmtx" in the Open
   * Type Specification.
   */
  public static final int       TAG_VMTX        = 0x766d7478;

  /**
   * Vertical metrics header.  Table tag "vhea" in the Open
   * Type Specification.
   */
  public static final int       TAG_VHEA        = 0x76686561;

  /**
   * Horizontal metrics header.  Table tag "hhea" in the Open
   * Type Specification.
   */
  public static final int       TAG_HHEA        = 0x68686561;

  /**
   * Adobe Type 1 font data.  Table tag "typ1" in the Open
   * Type Specification.
   */
  public static final int       TAG_TYP1        = 0x74797031;

  /**
   * Baseline table.  Table tag "bsln" in the Open
   * Type Specification.
   */
  public static final int       TAG_BSLN        = 0x62736c6e;

  /**
   * Glyph substitution.  Table tag "GSUB" in the Open
   * Type Specification.
   */
  public static final int       TAG_GSUB        = 0x47535542;

  /**
   * Digital signature.  Table tag "DSIG" in the Open
   * Type Specification.
   */
  public static final int       TAG_DSIG        = 0x44534947;

  /**
   * Font program.   Table tag "fpgm" in the Open
   * Type Specification.
   */
  public static final int       TAG_FPGM        = 0x6670676d;

  /**
   * Font variation.   Table tag "fvar" in the Open
   * Type Specification.
   */
  public static final int       TAG_FVAR        = 0x66766172;

  /**
   * Glyph variation.  Table tag "gvar" in the Open
   * Type Specification.
   */
  public static final int       TAG_GVAR        = 0x67766172;

  /**
   * Compact font format (Type1 font).  Table tag
   * "CFF " in the Open Type Specification.
   */
  public static final int       TAG_CFF = 0x43464620;

  /**
   * Multiple master supplementary data.  Table tag
   * "MMSD" in the Open Type Specification.
   */
  public static final int       TAG_MMSD        = 0x4d4d5344;

  /**
   * Multiple master font metrics.  Table tag
   * "MMFX" in the Open Type Specification.
   */
  public static final int       TAG_MMFX        = 0x4d4d4658;

  /**
   * Baseline data.  Table tag "BASE" in the Open
   * Type Specification.
   */
  public static final int       TAG_BASE        = 0x42415345;

  /**
   * Glyph definition.  Table tag "GDEF" in the Open
   * Type Specification.
   */
  public static final int       TAG_GDEF        = 0x47444546;

  /**
   * Glyph positioning.  Table tag "GPOS" in the Open
   * Type Specification.
   */
  public static final int       TAG_GPOS        = 0x47504f53;

  /**
   * Justification.  Table tag "JSTF" in the Open
   * Type Specification.
   */
  public static final int       TAG_JSTF        = 0x4a535446;

  /**
   * Embedded bitmap data.  Table tag "EBDT" in the Open
   * Type Specification.
   */
  public static final int       TAG_EBDT        = 0x45424454;

  /**
   * Embedded bitmap location.  Table tag "EBLC" in the Open
   * Type Specification.
   */
  public static final int       TAG_EBLC        = 0x45424c43;

  /**
   * Embedded bitmap scaling.  Table tag "EBSC" in the Open
   * Type Specification.
   */
  public static final int       TAG_EBSC        = 0x45425343;

  /**
   * Linear threshold.  Table tag "LTSH" in the Open
   * Type Specification.
   */
  public static final int       TAG_LTSH        = 0x4c545348;

  /**
   * PCL 5 data.  Table tag "PCLT" in the Open
   * Type Specification.
   */
  public static final int       TAG_PCLT        = 0x50434c54;

  /**
   * Accent attachment.  Table tag "acnt" in the Open
   * Type Specification.
   */
  public static final int       TAG_ACNT        = 0x61636e74;

  /**
   * Axis variation.  Table tag "avar" in the Open
   * Type Specification.
   */
  public static final int       TAG_AVAR        = 0x61766172;

  /**
   * Bitmap data.  Table tag "bdat" in the Open
   * Type Specification.
   */
  public static final int       TAG_BDAT        = 0x62646174;

  /**
   * Bitmap location.  Table tag "bloc" in the Open
   * Type Specification.
   */
  public static final int       TAG_BLOC        = 0x626c6f63;

   /**
    * CVT variation.  Table tag "cvar" in the Open
    * Type Specification.
    */
  public static final int       TAG_CVAR        = 0x63766172;

  /**
   * Feature name.  Table tag "feat" in the Open
    * Type Specification.
   */
  public static final int       TAG_FEAT        = 0x66656174;

  /**
   * Font descriptors.  Table tag "fdsc" in the Open
   * Type Specification.
   */
  public static final int       TAG_FDSC        = 0x66647363;

  /**
   * Font metrics.  Table tag "fmtx" in the Open
   * Type Specification.
   */
  public static final int       TAG_FMTX        = 0x666d7478;

  /**
   * Justification.  Table tag "just" in the Open
   * Type Specification.
   */
  public static final int       TAG_JUST        = 0x6a757374;

  /**
   * Ligature caret.   Table tag "lcar" in the Open
   * Type Specification.
   */
  public static final int       TAG_LCAR        = 0x6c636172;

  /**
   * Glyph metamorphosis.  Table tag "mort" in the Open
   * Type Specification.
   */
  public static final int       TAG_MORT        = 0x6d6f7274;

  /**
   * Optical bounds.  Table tag "opbd" in the Open
   * Type Specification.
   */
  public static final int       TAG_OPBD        = 0x6F706264;

  /**
   * Glyph properties.  Table tag "prop" in the Open
   * Type Specification.
   */
  public static final int       TAG_PROP        = 0x70726f70;

  /**
   * Tracking.  Table tag "trak" in the Open
   * Type Specification.
   */
  public static final int       TAG_TRAK        = 0x7472616b;

  /**
   * Returns the version of the {@code OpenType} font.
   * 1.0 is represented as 0x00010000.
   * @return the version of the {@code OpenType} font.
   */
  public int getVersion();

  /**
   * Returns the table as an array of bytes for a specified tag.
   * Tags for sfnt tables include items like <i>cmap</i>,
   * <i>name</i> and <i>head</i>.  The {@code byte} array
   * returned is a copy of the font data in memory.
   * @param     sfntTag a four-character code as a 32-bit integer
   * @return a {@code byte} array that is the table that
   * contains the font data corresponding to the specified
   * tag.
   */
  public byte[] getFontTable(int sfntTag);

  /**
   * Returns the table as an array of bytes for a specified tag.
   * Tags for sfnt tables include items like <i>cmap</i>,
   * <i>name</i> and <i>head</i>.  The byte array returned is a
   * copy of the font data in memory.
   * @param     strSfntTag a four-character code as a
   *            {@code String}
   * @return a {@code byte} array that is the table that
   * contains the font data corresponding to the specified
   * tag.
   */
  public byte[] getFontTable(String strSfntTag);

  /**
   * Returns a subset of the table as an array of bytes
   * for a specified tag.  Tags for sfnt tables include
   * items like <i>cmap</i>, <i>name</i> and <i>head</i>.
   * The byte array returned is a copy of the font data in
   * memory.
   * @param     sfntTag a four-character code as a 32-bit integer
   * @param     offset index of first byte to return from table
   * @param     count number of bytes to return from table
   * @return a subset of the table corresponding to
   *            {@code sfntTag} and containing the bytes
   *            starting at {@code offset} byte and including
   *            {@code count} bytes.
   */
  public byte[] getFontTable(int sfntTag, int offset, int count);

  /**
   * Returns a subset of the table as an array of bytes
   * for a specified tag.  Tags for sfnt tables include items
   * like <i>cmap</i>, <i>name</i> and <i>head</i>. The
   * {@code byte} array returned is a copy of the font
   * data in memory.
   * @param     strSfntTag a four-character code as a
   * {@code String}
   * @param     offset index of first byte to return from table
   * @param     count  number of bytes to return from table
   * @return a subset of the table corresponding to
   *            {@code strSfntTag} and containing the bytes
   *            starting at {@code offset} byte and including
   *            {@code count} bytes.
   */
  public byte[] getFontTable(String strSfntTag, int offset, int count);

  /**
   * Returns the size of the table for a specified tag. Tags for sfnt
   * tables include items like <i>cmap</i>, <i>name</i> and <i>head</i>.
   * @param     sfntTag a four-character code as a 32-bit integer
   * @return the size of the table corresponding to the specified
   * tag.
   */
  public int getFontTableSize(int sfntTag);

  /**
   * Returns the size of the table for a specified tag. Tags for sfnt
   * tables include items like <i>cmap</i>, <i>name</i> and <i>head</i>.
   * @param     strSfntTag a four-character code as a
   * {@code String}
   * @return the size of the table corresponding to the specified tag.
   */
  public int getFontTableSize(String strSfntTag);


}
