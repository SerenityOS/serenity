/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

package com.sun.org.apache.xml.internal.utils.res;

//
//  LangResources_en.properties
//

/**
 * The Japanese (Katakana) resource bundle.
 * @xsl.usage internal
 */
public class XResources_ja_JP_I extends XResourceBundle
{
  private static final Object[][] _contents = new Object[][]
  {
    { "ui_language", "ja" }, { "help_language", "ja" }, { "language", "ja" },
    { "alphabet", new CharArrayWrapper(
      new char[]{ 0x30a4, 0x30ed, 0x30cf, 0x30cb, 0x30db, 0x30d8, 0x30c8,
                  0x30c1, 0x30ea, 0x30cc, 0x30eb, 0x30f2, 0x30ef, 0x30ab,
                  0x30e8, 0x30bf, 0x30ec, 0x30bd, 0x30c4, 0x30cd, 0x30ca,
                  0x30e9, 0x30e0, 0x30a6, 0x30f0, 0x30ce, 0x30aa, 0x30af,
                  0x30e4, 0x30de, 0x30b1, 0x30d5, 0x30b3, 0x30a8, 0x30c6,
                  0x30a2, 0x30b5, 0x30ad, 0x30e6, 0x30e1, 0x30df, 0x30b7,
                  0x30f1, 0x30d2, 0x30e2, 0x30bb, 0x30b9 }) },
    { "tradAlphabet", new CharArrayWrapper(
      new char[]{ 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
                  'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
                  'Y', 'Z' }) },

    //language orientation
    { "orientation", "LeftToRight" },

    //language numbering
    { "numbering", "multiplicative-additive" },
    { "multiplierOrder", "follows" },

    // largest numerical value
    //{"MaxNumericalValue", new Integer(10000000000)},
    //These would not be used for EN. Only used for traditional numbering
    { "numberGroups", new IntArrayWrapper(new int[]{ 1 }) },

    //These only used for mutiplicative-additive numbering
    // Note that we are using longs and that the last two
    // multipliers are not supported. This is a known limitation.
    { "multiplier", new LongArrayWrapper(
      new long[]{ Long.MAX_VALUE, Long.MAX_VALUE, 100000000, 10000, 1000, 100, 10 }) },
    { "multiplierChar", new CharArrayWrapper(
      new char[]{ 0x4EAC, 0x5146, 0x5104, 0x4E07, 0x5343, 0x767e, 0x5341 }) },

    // chinese only??
    { "zero", new CharArrayWrapper(new char[0]) },
    { "digits", new CharArrayWrapper(
      new char[]{ 0x4E00, 0x4E8C, 0x4E09, 0x56DB, 0x4E94, 0x516D, 0x4E03,
                  0x516B, 0x4E5D }) }, { "tables", new StringArrayWrapper(
                      new String[]{ "digits" }) }
  };

  /**
   * Get the association table for this resource.
   *
   *
   * @return the association table for this resource.
   */
  public Object[][] getContents()
  {
    return _contents;
  }
}
