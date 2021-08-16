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
 * The Chinese resource bundle.
 * @xsl.usage internal
 */
public class XResources_zh_CN extends XResourceBundle
{
  private static final Object[][] _contents = new Object[][]
  {
    { "ui_language", "zh" }, { "help_language", "zh" }, { "language", "zh" },
    { "alphabet", new CharArrayWrapper(
      new char[]{ 0xff21, 0xff22, 0xff23, 0xff24, 0xff25, 0xff26, 0xff27,
                  0xff28, 0xff29, 0xff2a, 0xff2b, 0xff2c, 0xff2d, 0xff2e,
                  0xff2f, 0xff30, 0xff31, 0xff32, 0xff33, 0xff34, 0xff35,
                  0xff36, 0xff37, 0xff38, 0xff39, 0xff3a }) },
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
    //{"MaxNumericalValue", new Integer(100000000)},
    //These would not be used for EN. Only used for traditional numbering
    { "numberGroups", new IntArrayWrapper(new int[]{ 1 }) },

    // simplified chinese
    { "zero", new CharArrayWrapper(new char[]{ 0x96f6 }) },

    //These only used for mutiplicative-additive numbering
    { "multiplier", new LongArrayWrapper(new long[]{ 100000000, 10000, 1000,
        100, 10 }) },
    { "multiplierChar", new CharArrayWrapper(
      new char[]{ 0x4ebf, 0x4e07, 0x5343, 0x767e, 0x5341 }) },
    { "digits", new CharArrayWrapper(
      new char[]{ 0x4e00, 0x4e8c, 0x4e09, 0x56db, 0x4e94, 0x516d, 0x4e03,
                  0x516b, 0x4e5d }) }, { "tables", new StringArrayWrapper(
                      new String[]{ "digits" }) }
  };

  /**
   * Get the association list.
   *
   * @return The association list.
   */
  public Object[][] getContents()
  {
    return _contents;
  }
}
