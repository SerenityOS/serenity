/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright IBM Corp. 2003 - All Rights Reserved
 *
 * The original version of this source code and documentation is
 * copyrighted and owned by IBM. These materials are provided
 * under terms of a License Agreement between IBM and Sun.
 * This technology is protected by multiple US and International
 * patents. This notice and attribution to IBM may not be removed.
 */

package sun.font;

import java.text.CharacterIterator;

public abstract class CodePointIterator {
    public static final int DONE = -1;

    public abstract void setToStart();
    public abstract void setToLimit();

    public abstract int next();
    public abstract int prev();

    public abstract int charIndex();

    public static CodePointIterator create(char[] text) {
        return new CharArrayCodePointIterator(text);
    }

    public static CodePointIterator create(char[] text, int start, int limit) {
        return new CharArrayCodePointIterator(text, start, limit);
    }

    public static CodePointIterator create(CharSequence text) {
        return new CharSequenceCodePointIterator(text);
    }

    public static CodePointIterator create(CharacterIterator iter) {
        return new CharacterIteratorCodePointIterator(iter);
    }
}

final class CharArrayCodePointIterator extends CodePointIterator {
    private char[] text;
    private int start;
    private int limit;
    private int index;

    public CharArrayCodePointIterator(char[] text) {
        this.text = text;
        this.limit = text.length;
    }

    public CharArrayCodePointIterator(char[] text, int start, int limit) {
        if (start < 0 || limit < start || limit > text.length) {
            throw new IllegalArgumentException();
        }

        this.text = text;
        this.start = this.index = start;
        this.limit = limit;
    }

    public void setToStart() {
        index = start;
    }

    public void setToLimit() {
        index = limit;
    }

    public int next() {
        if (index < limit) {
            char cp1 = text[index++];
            if (Character.isHighSurrogate(cp1) && index < limit) {
                char cp2 = text[index];
                if (Character.isLowSurrogate(cp2)) {
                    ++index;
                    return Character.toCodePoint(cp1, cp2);
                }
            }
            return cp1;
        }
        return DONE;
    }

    public int prev() {
        if (index > start) {
            char cp2 = text[--index];
            if (Character.isLowSurrogate(cp2) && index > start) {
                char cp1 = text[index - 1];
                if (Character.isHighSurrogate(cp1)) {
                    --index;
                    return Character.toCodePoint(cp1, cp2);
                }
            }
            return cp2;
        }
        return DONE;
    }

    public int charIndex() {
        return index;
    }
}

final class CharSequenceCodePointIterator extends CodePointIterator {
    private CharSequence text;
    private int index;

    public CharSequenceCodePointIterator(CharSequence text) {
        this.text = text;
    }

    public void setToStart() {
        index = 0;
    }

    public void setToLimit() {
        index = text.length();
    }

    public int next() {
        if (index < text.length()) {
            char cp1 = text.charAt(index++);
            if (Character.isHighSurrogate(cp1) && index < text.length()) {
                char cp2 = text.charAt(index+1);
                if (Character.isLowSurrogate(cp2)) {
                    ++index;
                    return Character.toCodePoint(cp1, cp2);
                }
            }
            return cp1;
        }
        return DONE;
    }

    public int prev() {
        if (index > 0) {
            char cp2 = text.charAt(--index);
            if (Character.isLowSurrogate(cp2) && index > 0) {
                char cp1 = text.charAt(index - 1);
                if (Character.isHighSurrogate(cp1)) {
                    --index;
                    return Character.toCodePoint(cp1, cp2);
                }
            }
            return cp2;
        }
        return DONE;
    }

    public int charIndex() {
        return index;
    }
}

// note this has different iteration semantics than CharacterIterator
final class CharacterIteratorCodePointIterator extends CodePointIterator {
    private CharacterIterator iter;

    public CharacterIteratorCodePointIterator(CharacterIterator iter) {
        this.iter = iter;
    }

    public void setToStart() {
        iter.setIndex(iter.getBeginIndex());
    }

    public void setToLimit() {
        iter.setIndex(iter.getEndIndex());
    }

    public int next() {
        char cp1 = iter.current();
        if (cp1 != CharacterIterator.DONE) {
            char cp2 = iter.next();
            if (Character.isHighSurrogate(cp1) && cp2 != CharacterIterator.DONE) {
                if (Character.isLowSurrogate(cp2)) {
                    iter.next();
                    return Character.toCodePoint(cp1, cp2);
                }
            }
            return cp1;
        }
        return DONE;
    }

    public int prev() {
        char cp2 = iter.previous();
        if (cp2 != CharacterIterator.DONE) {
            if (Character.isLowSurrogate(cp2)) {
                char cp1 = iter.previous();
                if (Character.isHighSurrogate(cp1)) {
                    return Character.toCodePoint(cp1, cp2);
                }
                iter.next();
            }
            return cp2;
        }
        return DONE;
    }

    public int charIndex() {
        return iter.getIndex();
    }
}
