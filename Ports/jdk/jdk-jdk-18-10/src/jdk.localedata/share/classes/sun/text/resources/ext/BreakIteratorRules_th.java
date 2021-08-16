/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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
 */

/*
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 */

/*
 * Since JDK 1.5.0, this file no longer goes to runtime and is used at J2SE
 * build phase in order to create [Word|Line]BreakIteratorData_th files which
 * are used on runtime instead.
 */

package sun.text.resources.ext;

import java.util.ListResourceBundle;
import java.util.MissingResourceException;
import java.net.URL;

public class BreakIteratorRules_th extends ListResourceBundle {
    protected final Object[][] getContents() {
        return new Object[][] {
            { "WordBreakRules",
              // this rule breaks the iterator with mixed Thai and English
                "<dictionary>=[\u0e01-\u0e2e\u0e30-\u0e3a\u0e40-\u0e44\u0e47-\u0e4e];"

                + "<ignore>=[:Mn::Me::Cf:^<dictionary>];"
                + "<paiyannoi>=[\u0e2f];"
                + "<maiyamok>=[\u0e46];"
                + "<danda>=[\u0964\u0965];"
                + "<kanji>=[\u3005\u4e00-\u9fa5\uf900-\ufa2d];"
                + "<kata>=[\u30a1-\u30fa];"
                + "<hira>=[\u3041-\u3094];"
                + "<cjk-diacrit>=[\u3099-\u309c];"
                + "<let>=[:L::Mc:^[<kanji><kata><hira><cjk-diacrit><dictionary>]];"
                + "<dgt>=[:N:];"
                + "<mid-word>=[:Pd:\u00ad\u2027\\\"\\\'\\.];"
                + "<mid-num>=[\\\"\\\'\\,\u066b\\.];"
                + "<pre-num>=[:Sc:\\#\\.^\u00a2];"
                + "<post-num>=[\\%\\&\u00a2\u066a\u2030\u2031];"
                + "<ls>=[\n\u000c\u2028\u2029];"
                + "<ws>=[:Zs:\t];"
                + "<word>=((<let><let>*(<mid-word><let><let>*)*){<danda>});"
                + "<number>=(<dgt><dgt>*(<mid-num><dgt><dgt>*)*);"
                + "<thai-etc>=<paiyannoi>\u0e25<paiyannoi>;"
                + ".;"
                + "{<word>}(<number><word>)*{<number>{<post-num>}};"
                + "<pre-num>(<number><word>)*{<number>{<post-num>}};"
                + "<dictionary><dictionary>*{{<paiyannoi>}<maiyamok>};"
                + "<dictionary><dictionary>*<paiyannoi>/([^[\u0e25<ignore>]]"
                        + "|\u0e25[^[<paiyannoi><ignore>]]);"
                + "<thai-etc>;"
                + "<ws>*{\r}{<ls>};"
                + "[<kata><cjk-diacrit>]*;"
                + "[<hira><cjk-diacrit>]*;"
                + "<kanji>*;"
            },

            { "LineBreakRules",
                "<dictionary>=[\u0e01-\u0e2e\u0e30-\u0e3a\u0e40-\u0e44\u0e47-\u0e4e];" // this rule breaks the iterator with mixed Thai and English
                + "<ignore>=[:Mn::Me::Cf:^[<dictionary>]];"
                + "<danda>=[\u0964\u0965];"
                + "<break>=[\u0003\t\n\f\u2028\u2029];"
                + "<nbsp>=[\u00a0\u0f0c\u2007\u2011\u202f\ufeff];"
                + "<space>=[:Zs::Cc:^[<nbsp><break>\r]];"
                + "<dash>=[:Pd:\u00ad^<nbsp>];"
                + "<paiyannoi>=[\u0e2f];"
                + "<maiyamok>=[\u0e46];"
                + "<thai-etc>=(<paiyannoi>\u0e25<paiyannoi>);"
                + "<pre-word>=[:Sc::Ps::Pi:^\u00a2\\\"];"
                + "<post-word>=[:Pe::Pf:\\!\\%\\.\\,\\:\\;\\?\\\"\u00a2\u00b0\u066a\u2030-\u2034\u2103"
                        + "\u2105\u2109\u3001\u3002\u3005\u3041\u3043\u3045\u3047\u3049\u3063"
                        + "\u3083\u3085\u3087\u308e\u3099-\u309e\u30a1\u30a3\u30a5\u30a7\u30a9"
                        + "\u30c3\u30e3\u30e5\u30e7\u30ee\u30f5\u30f6\u30fc-\u30fe\uff01\uff0e"
                        + "\uff1f<maiyamok>];"
                + "<kanji>=[\u4e00-\u9fa5\uf900-\ufa2d\u3041-\u3094\u30a1-\u30fa^[<post-word><ignore>]];"
                + "<digit>=[:Nd::No:];"
                + "<mid-num>=[\\.\\,];"
                + "<char>=[^[<break><space><dash><kanji><nbsp><ignore><pre-word><post-word>"
                        + "<mid-num>\r<danda><dictionary><paiyannoi><maiyamok>]];"
                + "<number>=([<pre-word><dash>]*<digit><digit>*(<mid-num><digit><digit>*)*);"
                + "<word-core>=(<char>*|<kanji>|<number>|<dictionary><dictionary>*|<thai-etc>);"
                + "<word-suffix>=((<dash><dash>*|<post-word>*)<space>*);"
                + "<word>=(<pre-word>*<word-core><word-suffix>);"
                + "<word>(<nbsp><nbsp>*<word>)*{({\r}{<break>}|<paiyannoi>\r{break}|<paiyannoi><break>)};"
                + "<word>(<nbsp><nbsp>*<word>)*<paiyannoi>/([^[\u0e25<ignore>]]|"
                        + "\u0e25[^[<paiyannoi><ignore>]]);"
            }
        };
    }
}
