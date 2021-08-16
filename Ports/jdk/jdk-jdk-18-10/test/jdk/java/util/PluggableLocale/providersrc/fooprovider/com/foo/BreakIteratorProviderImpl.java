/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 */

package com.foo;

import java.text.*;
import java.text.spi.*;
import java.util.*;
import com.foobar.Utils;

public class BreakIteratorProviderImpl extends BreakIteratorProvider {

    static Locale[] avail = {
        Locale.JAPAN,
        new Locale("ja", "JP", "osaka"),
        new Locale("ja", "JP", "kyoto"),
        new Locale("xx", "YY")};

    static String[] dialect = {
        "\u3067\u3059\u3002",
        "\u3084\u3002",
        "\u3069\u3059\u3002",
        "-xx-YY"
    };

    static enum Type {CHARACTER, LINE, SENTENCE, WORD};

    public Locale[] getAvailableLocales() {
        return avail;
    }

    public BreakIterator getCharacterInstance(Locale locale) {
        for (int i = 0; i < avail.length; i ++) {
            if (Utils.supportsLocale(avail[i], locale)) {
                return new FooBreakIterator(Type.CHARACTER, i);
            }
        }
        throw new IllegalArgumentException("locale is not supported: "+locale);
    }

    public BreakIterator getLineInstance(Locale locale) {
        for (int i = 0; i < avail.length; i ++) {
            if (Utils.supportsLocale(avail[i], locale)) {
                return new FooBreakIterator(Type.LINE, i);
            }
        }
        throw new IllegalArgumentException("locale is not supported: "+locale);
    }

    public BreakIterator getSentenceInstance(Locale locale) {
        for (int i = 0; i < avail.length; i ++) {
            if (Utils.supportsLocale(avail[i], locale)) {
                return new FooBreakIterator(Type.SENTENCE, i);
            }
        }
        throw new IllegalArgumentException("locale is not supported: "+locale);
    }

    public BreakIterator getWordInstance(Locale locale) {
        for (int i = 0; i < avail.length; i ++) {
            if (Utils.supportsLocale(avail[i], locale)) {
                return new FooBreakIterator(Type.WORD, i);
            }
        }
        throw new IllegalArgumentException("locale is not supported: "+locale);
    }

    // dummy implementation
    class FooBreakIterator extends BreakIterator {
        public FooBreakIterator(Type t, int index) {
            super();
        }

        public int current() {
            return 0;
        }

        public int first() {
            return 0;
        }

        public int following(int offset) {
            return 0;
        }

        public CharacterIterator getText() {
            return null;
        }

        public boolean isBoundary(int offset) {
            return true;
        }

        public int last() {
            return 0;
        }

        public int next() {
            return 0;
        }

        public int next(int n) {
            return 0;
        }

        public int preceding(int offset) {
            return 0;
        }

        public int previous() {
            return 0;
        }

        public void setText(CharacterIterator ci) {
        }
    }
}
