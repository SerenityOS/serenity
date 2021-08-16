/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
package java.text;

import java.util.ArrayList;

/**
 * CharacterIteratorFieldDelegate combines the notifications from a Format
 * into a resulting {@code AttributedCharacterIterator}. The resulting
 * {@code AttributedCharacterIterator} can be retrieved by way of
 * the {@code getIterator} method.
 *
 */
class CharacterIteratorFieldDelegate implements Format.FieldDelegate {
    /**
     * Array of AttributeStrings. Whenever {@code formatted} is invoked
     * for a region > size, a new instance of AttributedString is added to
     * attributedStrings. Subsequent invocations of {@code formatted}
     * for existing regions result in invoking addAttribute on the existing
     * AttributedStrings.
     */
    private ArrayList<AttributedString> attributedStrings;
    /**
     * Running count of the number of characters that have
     * been encountered.
     */
    private int size;


    CharacterIteratorFieldDelegate() {
        attributedStrings = new ArrayList<>();
    }

    public void formatted(Format.Field attr, Object value, int start, int end,
                          StringBuffer buffer) {
        if (start != end) {
            if (start < size) {
                // Adjust attributes of existing runs
                int index = size;
                int asIndex = attributedStrings.size() - 1;

                while (start < index) {
                    AttributedString as = attributedStrings.
                                           get(asIndex--);
                    int newIndex = index - as.length();
                    int aStart = Math.max(0, start - newIndex);

                    as.addAttribute(attr, value, aStart, Math.min(
                                    end - start, as.length() - aStart) +
                                    aStart);
                    index = newIndex;
                }
            }
            if (size < start) {
                // Pad attributes
                attributedStrings.add(new AttributedString(
                                          buffer.substring(size, start)));
                size = start;
            }
            if (size < end) {
                // Add new string
                int aStart = Math.max(start, size);
                AttributedString string = new AttributedString(
                                   buffer.substring(aStart, end));

                string.addAttribute(attr, value);
                attributedStrings.add(string);
                size = end;
            }
        }
    }

    public void formatted(int fieldID, Format.Field attr, Object value,
                          int start, int end, StringBuffer buffer) {
        formatted(attr, value, start, end, buffer);
    }

    /**
     * Returns an {@code AttributedCharacterIterator} that can be used
     * to iterate over the resulting formatted String.
     *
     * @pararm string Result of formatting.
     */
    public AttributedCharacterIterator getIterator(String string) {
        // Add the last AttributedCharacterIterator if necessary
        // assert(size <= string.length());
        if (string.length() > size) {
            attributedStrings.add(new AttributedString(
                                  string.substring(size)));
            size = string.length();
        }
        int iCount = attributedStrings.size();
        AttributedCharacterIterator iterators[] = new
                                    AttributedCharacterIterator[iCount];

        for (int counter = 0; counter < iCount; counter++) {
            iterators[counter] = attributedStrings.
                                  get(counter).getIterator();
        }
        return new AttributedString(iterators).getIterator();
    }
}
