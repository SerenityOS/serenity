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

package java.text;

import java.util.*;
import java.text.AttributedCharacterIterator.Attribute;

/**
 * An AttributedString holds text and related attribute information. It
 * may be used as the actual data storage in some cases where a text
 * reader wants to access attributed text through the AttributedCharacterIterator
 * interface.
 *
 * <p>
 * An attribute is a key/value pair, identified by the key.  No two
 * attributes on a given character can have the same key.
 *
 * <p>The values for an attribute are immutable, or must not be mutated
 * by clients or storage.  They are always passed by reference, and not
 * cloned.
 *
 * @see AttributedCharacterIterator
 * @see Annotation
 * @since 1.2
 */

public class AttributedString {
    // field holding the text
    String text;

    // Fields holding run attribute information.
    // Run attributes are organized by run.
    // Arrays are always of equal lengths (the current capacity).
    // Since there are no vectors of int, we have to use arrays.
    private static final int INITIAL_CAPACITY = 10;
    int runCount;                   // actual number of runs, <= current capacity
    int[] runStarts;                // start index for each run
    Vector<Attribute>[] runAttributes;   // vector of attribute keys for each run
    Vector<Object>[] runAttributeValues; // parallel vector of attribute values for each run

    /**
     * Constructs an AttributedString instance with the given
     * AttributedCharacterIterators.
     *
     * @param iterators AttributedCharacterIterators to construct
     * AttributedString from.
     * @throws NullPointerException if iterators is null
     */
    AttributedString(AttributedCharacterIterator[] iterators) {
        if (iterators == null) {
            throw new NullPointerException("Iterators must not be null");
        }
        if (iterators.length == 0) {
            text = "";
        }
        else {
            // Build the String contents
            StringBuffer buffer = new StringBuffer();
            for (int counter = 0; counter < iterators.length; counter++) {
                appendContents(buffer, iterators[counter]);
            }

            text = buffer.toString();

            if (!text.isEmpty()) {
                // Determine the runs, creating a new run when the attributes
                // differ.
                int offset = 0;
                Map<Attribute,Object> last = null;

                for (int counter = 0; counter < iterators.length; counter++) {
                    AttributedCharacterIterator iterator = iterators[counter];
                    int start = iterator.getBeginIndex();
                    int end = iterator.getEndIndex();
                    int index = start;

                    while (index < end) {
                        iterator.setIndex(index);

                        Map<Attribute,Object> attrs = iterator.getAttributes();

                        if (mapsDiffer(last, attrs)) {
                            setAttributes(attrs, index - start + offset);
                        }
                        last = attrs;
                        index = iterator.getRunLimit();
                    }
                    offset += (end - start);
                }
            }
        }
    }

    /**
     * Constructs an AttributedString instance with the given text.
     * @param text The text for this attributed string.
     * @throws    NullPointerException if {@code text} is null.
     */
    public AttributedString(String text) {
        if (text == null) {
            throw new NullPointerException();
        }
        this.text = text;
    }

    /**
     * Constructs an AttributedString instance with the given text and attributes.
     * @param text The text for this attributed string.
     * @param attributes The attributes that apply to the entire string.
     * @throws    NullPointerException if {@code text} or
     *            {@code attributes} is null.
     * @throws    IllegalArgumentException if the text has length 0
     * and the attributes parameter is not an empty Map (attributes
     * cannot be applied to a 0-length range).
     */
    public AttributedString(String text,
                            Map<? extends Attribute, ?> attributes)
    {
        if (text == null || attributes == null) {
            throw new NullPointerException();
        }
        this.text = text;

        if (text.isEmpty()) {
            if (attributes.isEmpty())
                return;
            throw new IllegalArgumentException("Can't add attribute to 0-length text");
        }

        int attributeCount = attributes.size();
        if (attributeCount > 0) {
            createRunAttributeDataVectors();
            Vector<Attribute> newRunAttributes = new Vector<>(attributeCount);
            Vector<Object> newRunAttributeValues = new Vector<>(attributeCount);
            runAttributes[0] = newRunAttributes;
            runAttributeValues[0] = newRunAttributeValues;

            Iterator<? extends Map.Entry<? extends Attribute, ?>> iterator = attributes.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<? extends Attribute, ?> entry = iterator.next();
                newRunAttributes.addElement(entry.getKey());
                newRunAttributeValues.addElement(entry.getValue());
            }
        }
    }

    /**
     * Constructs an AttributedString instance with the given attributed
     * text represented by AttributedCharacterIterator.
     * @param text The text for this attributed string.
     * @throws    NullPointerException if {@code text} is null.
     */
    public AttributedString(AttributedCharacterIterator text) {
        // If performance is critical, this constructor should be
        // implemented here rather than invoking the constructor for a
        // subrange. We can avoid some range checking in the loops.
        this(text, text.getBeginIndex(), text.getEndIndex(), null);
    }

    /**
     * Constructs an AttributedString instance with the subrange of
     * the given attributed text represented by
     * AttributedCharacterIterator. If the given range produces an
     * empty text, all attributes will be discarded.  Note that any
     * attributes wrapped by an Annotation object are discarded for a
     * subrange of the original attribute range.
     *
     * @param text The text for this attributed string.
     * @param beginIndex Index of the first character of the range.
     * @param endIndex Index of the character following the last character
     * of the range.
     * @throws    NullPointerException if {@code text} is null.
     * @throws    IllegalArgumentException if the subrange given by
     * beginIndex and endIndex is out of the text range.
     * @see java.text.Annotation
     */
    public AttributedString(AttributedCharacterIterator text,
                            int beginIndex,
                            int endIndex) {
        this(text, beginIndex, endIndex, null);
    }

    /**
     * Constructs an AttributedString instance with the subrange of
     * the given attributed text represented by
     * AttributedCharacterIterator.  Only attributes that match the
     * given attributes will be incorporated into the instance. If the
     * given range produces an empty text, all attributes will be
     * discarded. Note that any attributes wrapped by an Annotation
     * object are discarded for a subrange of the original attribute
     * range.
     *
     * @param text The text for this attributed string.
     * @param beginIndex Index of the first character of the range.
     * @param endIndex Index of the character following the last character
     * of the range.
     * @param attributes Specifies attributes to be extracted
     * from the text. If null is specified, all available attributes will
     * be used.
     * @throws    NullPointerException if {@code text} is null.
     * @throws    IllegalArgumentException if the subrange given by
     * beginIndex and endIndex is out of the text range.
     * @see java.text.Annotation
     */
    public AttributedString(AttributedCharacterIterator text,
                            int beginIndex,
                            int endIndex,
                            Attribute[] attributes) {
        if (text == null) {
            throw new NullPointerException();
        }

        // Validate the given subrange
        int textBeginIndex = text.getBeginIndex();
        int textEndIndex = text.getEndIndex();
        if (beginIndex < textBeginIndex || endIndex > textEndIndex || beginIndex > endIndex)
            throw new IllegalArgumentException("Invalid substring range");

        // Copy the given string
        StringBuilder textBuilder = new StringBuilder();
        text.setIndex(beginIndex);
        for (char c = text.current(); text.getIndex() < endIndex; c = text.next())
            textBuilder.append(c);
        this.text = textBuilder.toString();

        if (beginIndex == endIndex)
            return;

        // Select attribute keys to be taken care of
        HashSet<Attribute> keys = new HashSet<>();
        if (attributes == null) {
            keys.addAll(text.getAllAttributeKeys());
        } else {
            for (int i = 0; i < attributes.length; i++)
                keys.add(attributes[i]);
            keys.retainAll(text.getAllAttributeKeys());
        }
        if (keys.isEmpty())
            return;

        // Get and set attribute runs for each attribute name. Need to
        // scan from the top of the text so that we can discard any
        // Annotation that is no longer applied to a subset text segment.
        Iterator<Attribute> itr = keys.iterator();
        while (itr.hasNext()) {
            Attribute attributeKey = itr.next();
            text.setIndex(textBeginIndex);
            while (text.getIndex() < endIndex) {
                int start = text.getRunStart(attributeKey);
                int limit = text.getRunLimit(attributeKey);
                Object value = text.getAttribute(attributeKey);

                if (value != null) {
                    if (value instanceof Annotation) {
                        if (start >= beginIndex && limit <= endIndex) {
                            addAttribute(attributeKey, value, start - beginIndex, limit - beginIndex);
                        } else {
                            if (limit > endIndex)
                                break;
                        }
                    } else {
                        // if the run is beyond the given (subset) range, we
                        // don't need to process further.
                        if (start >= endIndex)
                            break;
                        if (limit > beginIndex) {
                            // attribute is applied to any subrange
                            if (start < beginIndex)
                                start = beginIndex;
                            if (limit > endIndex)
                                limit = endIndex;
                            if (start != limit) {
                                addAttribute(attributeKey, value, start - beginIndex, limit - beginIndex);
                            }
                        }
                    }
                }
                text.setIndex(limit);
            }
        }
    }

    /**
     * Adds an attribute to the entire string.
     * @param attribute the attribute key
     * @param value the value of the attribute; may be null
     * @throws    NullPointerException if {@code attribute} is null.
     * @throws    IllegalArgumentException if the AttributedString has length 0
     * (attributes cannot be applied to a 0-length range).
     */
    public void addAttribute(Attribute attribute, Object value) {

        if (attribute == null) {
            throw new NullPointerException();
        }

        int len = length();
        if (len == 0) {
            throw new IllegalArgumentException("Can't add attribute to 0-length text");
        }

        addAttributeImpl(attribute, value, 0, len);
    }

    /**
     * Adds an attribute to a subrange of the string.
     * @param attribute the attribute key
     * @param value The value of the attribute. May be null.
     * @param beginIndex Index of the first character of the range.
     * @param endIndex Index of the character following the last character of the range.
     * @throws    NullPointerException if {@code attribute} is null.
     * @throws    IllegalArgumentException if beginIndex is less than 0, endIndex is
     * greater than the length of the string, or beginIndex and endIndex together don't
     * define a non-empty subrange of the string.
     */
    public void addAttribute(Attribute attribute, Object value,
            int beginIndex, int endIndex) {

        if (attribute == null) {
            throw new NullPointerException();
        }

        if (beginIndex < 0 || endIndex > length() || beginIndex >= endIndex) {
            throw new IllegalArgumentException("Invalid substring range");
        }

        addAttributeImpl(attribute, value, beginIndex, endIndex);
    }

    /**
     * Adds a set of attributes to a subrange of the string.
     * @param attributes The attributes to be added to the string.
     * @param beginIndex Index of the first character of the range.
     * @param endIndex Index of the character following the last
     * character of the range.
     * @throws    NullPointerException if {@code attributes} is null.
     * @throws    IllegalArgumentException if beginIndex is less than
     * 0, endIndex is greater than the length of the string, or
     * beginIndex and endIndex together don't define a non-empty
     * subrange of the string and the attributes parameter is not an
     * empty Map.
     */
    public void addAttributes(Map<? extends Attribute, ?> attributes,
                              int beginIndex, int endIndex)
    {
        if (attributes == null) {
            throw new NullPointerException();
        }

        if (beginIndex < 0 || endIndex > length() || beginIndex > endIndex) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        if (beginIndex == endIndex) {
            if (attributes.isEmpty())
                return;
            throw new IllegalArgumentException("Can't add attribute to 0-length text");
        }

        // make sure we have run attribute data vectors
        if (runCount == 0) {
            createRunAttributeDataVectors();
        }

        // break up runs if necessary
        int beginRunIndex = ensureRunBreak(beginIndex);
        int endRunIndex = ensureRunBreak(endIndex);

        Iterator<? extends Map.Entry<? extends Attribute, ?>> iterator =
            attributes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<? extends Attribute, ?> entry = iterator.next();
            addAttributeRunData(entry.getKey(), entry.getValue(), beginRunIndex, endRunIndex);
        }
    }

    private synchronized void addAttributeImpl(Attribute attribute, Object value,
            int beginIndex, int endIndex) {

        // make sure we have run attribute data vectors
        if (runCount == 0) {
            createRunAttributeDataVectors();
        }

        // break up runs if necessary
        int beginRunIndex = ensureRunBreak(beginIndex);
        int endRunIndex = ensureRunBreak(endIndex);

        addAttributeRunData(attribute, value, beginRunIndex, endRunIndex);
    }

    private final void createRunAttributeDataVectors() {
        // use temporary variables so things remain consistent in case of an exception
        int[] newRunStarts = new int[INITIAL_CAPACITY];

        @SuppressWarnings("unchecked")
        Vector<Attribute>[] newRunAttributes = (Vector<Attribute>[]) new Vector<?>[INITIAL_CAPACITY];

        @SuppressWarnings("unchecked")
        Vector<Object>[] newRunAttributeValues = (Vector<Object>[]) new Vector<?>[INITIAL_CAPACITY];

        runStarts = newRunStarts;
        runAttributes = newRunAttributes;
        runAttributeValues = newRunAttributeValues;
        runCount = 1; // assume initial run starting at index 0
    }

    // ensure there's a run break at offset, return the index of the run
    private final int ensureRunBreak(int offset) {
        return ensureRunBreak(offset, true);
    }

    /**
     * Ensures there is a run break at offset, returning the index of
     * the run. If this results in splitting a run, two things can happen:
     * <ul>
     * <li>If copyAttrs is true, the attributes from the existing run
     *     will be placed in both of the newly created runs.
     * <li>If copyAttrs is false, the attributes from the existing run
     * will NOT be copied to the run to the right (>= offset) of the break,
     * but will exist on the run to the left (< offset).
     * </ul>
     */
    private final int ensureRunBreak(int offset, boolean copyAttrs) {
        if (offset == length()) {
            return runCount;
        }

        // search for the run index where this offset should be
        int runIndex = 0;
        while (runIndex < runCount && runStarts[runIndex] < offset) {
            runIndex++;
        }

        // if the offset is at a run start already, we're done
        if (runIndex < runCount && runStarts[runIndex] == offset) {
            return runIndex;
        }

        // we'll have to break up a run
        // first, make sure we have enough space in our arrays
        int currentCapacity = runStarts.length;
        if (runCount == currentCapacity) {
            // We need to resize - we grow capacity by 25%.
            int newCapacity = currentCapacity + (currentCapacity >> 2);

            // use temporary variables so things remain consistent in case of an exception
            int[] newRunStarts =
                Arrays.copyOf(runStarts, newCapacity);
            Vector<Attribute>[] newRunAttributes =
                Arrays.copyOf(runAttributes, newCapacity);
            Vector<Object>[] newRunAttributeValues =
                Arrays.copyOf(runAttributeValues, newCapacity);

            runStarts = newRunStarts;
            runAttributes = newRunAttributes;
            runAttributeValues = newRunAttributeValues;
        }

        // make copies of the attribute information of the old run that the new one used to be part of
        // use temporary variables so things remain consistent in case of an exception
        Vector<Attribute> newRunAttributes = null;
        Vector<Object> newRunAttributeValues = null;

        if (copyAttrs) {
            Vector<Attribute> oldRunAttributes = runAttributes[runIndex - 1];
            Vector<Object> oldRunAttributeValues = runAttributeValues[runIndex - 1];
            if (oldRunAttributes != null) {
                newRunAttributes = new Vector<>(oldRunAttributes);
            }
            if (oldRunAttributeValues != null) {
                newRunAttributeValues =  new Vector<>(oldRunAttributeValues);
            }
        }

        // now actually break up the run
        runCount++;
        for (int i = runCount - 1; i > runIndex; i--) {
            runStarts[i] = runStarts[i - 1];
            runAttributes[i] = runAttributes[i - 1];
            runAttributeValues[i] = runAttributeValues[i - 1];
        }
        runStarts[runIndex] = offset;
        runAttributes[runIndex] = newRunAttributes;
        runAttributeValues[runIndex] = newRunAttributeValues;

        return runIndex;
    }

    // add the attribute attribute/value to all runs where beginRunIndex <= runIndex < endRunIndex
    private void addAttributeRunData(Attribute attribute, Object value,
            int beginRunIndex, int endRunIndex) {

        for (int i = beginRunIndex; i < endRunIndex; i++) {
            int keyValueIndex = -1; // index of key and value in our vectors; assume we don't have an entry yet
            if (runAttributes[i] == null) {
                Vector<Attribute> newRunAttributes = new Vector<>();
                Vector<Object> newRunAttributeValues = new Vector<>();
                runAttributes[i] = newRunAttributes;
                runAttributeValues[i] = newRunAttributeValues;
            } else {
                // check whether we have an entry already
                keyValueIndex = runAttributes[i].indexOf(attribute);
            }

            if (keyValueIndex == -1) {
                // create new entry
                int oldSize = runAttributes[i].size();
                runAttributes[i].addElement(attribute);
                try {
                    runAttributeValues[i].addElement(value);
                }
                catch (Exception e) {
                    runAttributes[i].setSize(oldSize);
                    runAttributeValues[i].setSize(oldSize);
                }
            } else {
                // update existing entry
                runAttributeValues[i].set(keyValueIndex, value);
            }
        }
    }

    /**
     * Creates an AttributedCharacterIterator instance that provides access to the entire contents of
     * this string.
     *
     * @return An iterator providing access to the text and its attributes.
     */
    public AttributedCharacterIterator getIterator() {
        return getIterator(null, 0, length());
    }

    /**
     * Creates an AttributedCharacterIterator instance that provides access to
     * selected contents of this string.
     * Information about attributes not listed in attributes that the
     * implementor may have need not be made accessible through the iterator.
     * If the list is null, all available attribute information should be made
     * accessible.
     *
     * @param attributes a list of attributes that the client is interested in
     * @return an iterator providing access to the entire text and its selected attributes
     */
    public AttributedCharacterIterator getIterator(Attribute[] attributes) {
        return getIterator(attributes, 0, length());
    }

    /**
     * Creates an AttributedCharacterIterator instance that provides access to
     * selected contents of this string.
     * Information about attributes not listed in attributes that the
     * implementor may have need not be made accessible through the iterator.
     * If the list is null, all available attribute information should be made
     * accessible.
     *
     * @param attributes a list of attributes that the client is interested in
     * @param beginIndex the index of the first character
     * @param endIndex the index of the character following the last character
     * @return an iterator providing access to the text and its attributes
     * @throws    IllegalArgumentException if beginIndex is less than 0,
     * endIndex is greater than the length of the string, or beginIndex is
     * greater than endIndex.
     */
    public AttributedCharacterIterator getIterator(Attribute[] attributes, int beginIndex, int endIndex) {
        return new AttributedStringIterator(attributes, beginIndex, endIndex);
    }

    // all (with the exception of length) reading operations are private,
    // since AttributedString instances are accessed through iterators.

    // length is package private so that CharacterIteratorFieldDelegate can
    // access it without creating an AttributedCharacterIterator.
    int length() {
        return text.length();
    }

    private char charAt(int index) {
        return text.charAt(index);
    }

    private synchronized Object getAttribute(Attribute attribute, int runIndex) {
        Vector<Attribute> currentRunAttributes = runAttributes[runIndex];
        Vector<Object> currentRunAttributeValues = runAttributeValues[runIndex];
        if (currentRunAttributes == null) {
            return null;
        }
        int attributeIndex = currentRunAttributes.indexOf(attribute);
        if (attributeIndex != -1) {
            return currentRunAttributeValues.elementAt(attributeIndex);
        }
        else {
            return null;
        }
    }

    // gets an attribute value, but returns an annotation only if it's range does not extend outside the range beginIndex..endIndex
    private Object getAttributeCheckRange(Attribute attribute, int runIndex, int beginIndex, int endIndex) {
        Object value = getAttribute(attribute, runIndex);
        if (value instanceof Annotation) {
            // need to check whether the annotation's range extends outside the iterator's range
            if (beginIndex > 0) {
                int currIndex = runIndex;
                int runStart = runStarts[currIndex];
                while (runStart >= beginIndex &&
                        valuesMatch(value, getAttribute(attribute, currIndex - 1))) {
                    currIndex--;
                    runStart = runStarts[currIndex];
                }
                if (runStart < beginIndex) {
                    // annotation's range starts before iterator's range
                    return null;
                }
            }
            int textLength = length();
            if (endIndex < textLength) {
                int currIndex = runIndex;
                int runLimit = (currIndex < runCount - 1) ? runStarts[currIndex + 1] : textLength;
                while (runLimit <= endIndex &&
                        valuesMatch(value, getAttribute(attribute, currIndex + 1))) {
                    currIndex++;
                    runLimit = (currIndex < runCount - 1) ? runStarts[currIndex + 1] : textLength;
                }
                if (runLimit > endIndex) {
                    // annotation's range ends after iterator's range
                    return null;
                }
            }
            // annotation's range is subrange of iterator's range,
            // so we can return the value
        }
        return value;
    }

    // returns whether all specified attributes have equal values in the runs with the given indices
    private boolean attributeValuesMatch(Set<? extends Attribute> attributes, int runIndex1, int runIndex2) {
        Iterator<? extends Attribute> iterator = attributes.iterator();
        while (iterator.hasNext()) {
            Attribute key = iterator.next();
           if (!valuesMatch(getAttribute(key, runIndex1), getAttribute(key, runIndex2))) {
                return false;
            }
        }
        return true;
    }

    // returns whether the two objects are either both null or equal
    private static final boolean valuesMatch(Object value1, Object value2) {
        if (value1 == null) {
            return value2 == null;
        } else {
            return value1.equals(value2);
        }
    }

    /**
     * Appends the contents of the CharacterIterator iterator into the
     * StringBuffer buf.
     */
    private final void appendContents(StringBuffer buf,
                                      CharacterIterator iterator) {
        int index = iterator.getBeginIndex();
        int end = iterator.getEndIndex();

        while (index < end) {
            iterator.setIndex(index++);
            buf.append(iterator.current());
        }
    }

    /**
     * Sets the attributes for the range from offset to the next run break
     * (typically the end of the text) to the ones specified in attrs.
     * This is only meant to be called from the constructor!
     */
    private void setAttributes(Map<Attribute, Object> attrs, int offset) {
        if (runCount == 0) {
            createRunAttributeDataVectors();
        }

        int index = ensureRunBreak(offset, false);
        int size;

        if (attrs != null && (size = attrs.size()) > 0) {
            Vector<Attribute> runAttrs = new Vector<>(size);
            Vector<Object> runValues = new Vector<>(size);
            Iterator<Map.Entry<Attribute, Object>> iterator = attrs.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Attribute, Object> entry = iterator.next();

                runAttrs.add(entry.getKey());
                runValues.add(entry.getValue());
            }
            runAttributes[index] = runAttrs;
            runAttributeValues[index] = runValues;
        }
    }

    /**
     * Returns true if the attributes specified in last and attrs differ.
     */
    private static <K,V> boolean mapsDiffer(Map<K, V> last, Map<K, V> attrs) {
        if (last == null) {
            return (attrs != null && attrs.size() > 0);
        }
        return (!last.equals(attrs));
    }


    // the iterator class associated with this string class

    private final class AttributedStringIterator implements AttributedCharacterIterator {

        // note on synchronization:
        // we don't synchronize on the iterator, assuming that an iterator is only used in one thread.
        // we do synchronize access to the AttributedString however, since it's more likely to be shared between threads.

        // start and end index for our iteration
        private int beginIndex;
        private int endIndex;

        // attributes that our client is interested in
        private Attribute[] relevantAttributes;

        // the current index for our iteration
        // invariant: beginIndex <= currentIndex <= endIndex
        private int currentIndex;

        // information about the run that includes currentIndex
        private int currentRunIndex;
        private int currentRunStart;
        private int currentRunLimit;

        // constructor
        AttributedStringIterator(Attribute[] attributes, int beginIndex, int endIndex) {

            if (beginIndex < 0 || beginIndex > endIndex || endIndex > length()) {
                throw new IllegalArgumentException("Invalid substring range");
            }

            this.beginIndex = beginIndex;
            this.endIndex = endIndex;
            this.currentIndex = beginIndex;
            updateRunInfo();
            if (attributes != null) {
                relevantAttributes = attributes.clone();
            }
        }

        // Object methods. See documentation in that class.

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AttributedStringIterator that)) {
                return false;
            }

            if (AttributedString.this != that.getString())
                return false;
            if (currentIndex != that.currentIndex || beginIndex != that.beginIndex || endIndex != that.endIndex)
                return false;
            return true;
        }

        public int hashCode() {
            return text.hashCode() ^ currentIndex ^ beginIndex ^ endIndex;
        }

        public Object clone() {
            try {
                AttributedStringIterator other = (AttributedStringIterator) super.clone();
                return other;
            }
            catch (CloneNotSupportedException e) {
                throw new InternalError(e);
            }
        }

        // CharacterIterator methods. See documentation in that interface.

        public char first() {
            return internalSetIndex(beginIndex);
        }

        public char last() {
            if (endIndex == beginIndex) {
                return internalSetIndex(endIndex);
            } else {
                return internalSetIndex(endIndex - 1);
            }
        }

        public char current() {
            if (currentIndex == endIndex) {
                return DONE;
            } else {
                return charAt(currentIndex);
            }
        }

        public char next() {
            if (currentIndex < endIndex) {
                return internalSetIndex(currentIndex + 1);
            }
            else {
                return DONE;
            }
        }

        public char previous() {
            if (currentIndex > beginIndex) {
                return internalSetIndex(currentIndex - 1);
            }
            else {
                return DONE;
            }
        }

        public char setIndex(int position) {
            if (position < beginIndex || position > endIndex)
                throw new IllegalArgumentException("Invalid index");
            return internalSetIndex(position);
        }

        public int getBeginIndex() {
            return beginIndex;
        }

        public int getEndIndex() {
            return endIndex;
        }

        public int getIndex() {
            return currentIndex;
        }

        // AttributedCharacterIterator methods. See documentation in that interface.

        public int getRunStart() {
            return currentRunStart;
        }

        public int getRunStart(Attribute attribute) {
            if (currentRunStart == beginIndex || currentRunIndex == -1) {
                return currentRunStart;
            } else {
                Object value = getAttribute(attribute);
                int runStart = currentRunStart;
                int runIndex = currentRunIndex;
                while (runStart > beginIndex &&
                        valuesMatch(value, AttributedString.this.getAttribute(attribute, runIndex - 1))) {
                    runIndex--;
                    runStart = runStarts[runIndex];
                }
                if (runStart < beginIndex) {
                    runStart = beginIndex;
                }
                return runStart;
            }
        }

        public int getRunStart(Set<? extends Attribute> attributes) {
            if (currentRunStart == beginIndex || currentRunIndex == -1) {
                return currentRunStart;
            } else {
                int runStart = currentRunStart;
                int runIndex = currentRunIndex;
                while (runStart > beginIndex &&
                        AttributedString.this.attributeValuesMatch(attributes, currentRunIndex, runIndex - 1)) {
                    runIndex--;
                    runStart = runStarts[runIndex];
                }
                if (runStart < beginIndex) {
                    runStart = beginIndex;
                }
                return runStart;
            }
        }

        public int getRunLimit() {
            return currentRunLimit;
        }

        public int getRunLimit(Attribute attribute) {
            if (currentRunLimit == endIndex || currentRunIndex == -1) {
                return currentRunLimit;
            } else {
                Object value = getAttribute(attribute);
                int runLimit = currentRunLimit;
                int runIndex = currentRunIndex;
                while (runLimit < endIndex &&
                        valuesMatch(value, AttributedString.this.getAttribute(attribute, runIndex + 1))) {
                    runIndex++;
                    runLimit = runIndex < runCount - 1 ? runStarts[runIndex + 1] : endIndex;
                }
                if (runLimit > endIndex) {
                    runLimit = endIndex;
                }
                return runLimit;
            }
        }

        public int getRunLimit(Set<? extends Attribute> attributes) {
            if (currentRunLimit == endIndex || currentRunIndex == -1) {
                return currentRunLimit;
            } else {
                int runLimit = currentRunLimit;
                int runIndex = currentRunIndex;
                while (runLimit < endIndex &&
                        AttributedString.this.attributeValuesMatch(attributes, currentRunIndex, runIndex + 1)) {
                    runIndex++;
                    runLimit = runIndex < runCount - 1 ? runStarts[runIndex + 1] : endIndex;
                }
                if (runLimit > endIndex) {
                    runLimit = endIndex;
                }
                return runLimit;
            }
        }

        public Map<Attribute,Object> getAttributes() {
            if (runAttributes == null || currentRunIndex == -1 || runAttributes[currentRunIndex] == null) {
                // ??? would be nice to return null, but current spec doesn't allow it
                // returning Hashtable saves AttributeMap from dealing with emptiness
                return new Hashtable<>();
            }
            return new AttributeMap(currentRunIndex, beginIndex, endIndex);
        }

        public Set<Attribute> getAllAttributeKeys() {
            // ??? This should screen out attribute keys that aren't relevant to the client
            if (runAttributes == null) {
                // ??? would be nice to return null, but current spec doesn't allow it
                // returning HashSet saves us from dealing with emptiness
                return new HashSet<>();
            }
            synchronized (AttributedString.this) {
                // ??? should try to create this only once, then update if necessary,
                // and give callers read-only view
                Set<Attribute> keys = new HashSet<>();
                int i = 0;
                while (i < runCount) {
                    if (runStarts[i] < endIndex && (i == runCount - 1 || runStarts[i + 1] > beginIndex)) {
                        Vector<Attribute> currentRunAttributes = runAttributes[i];
                        if (currentRunAttributes != null) {
                            int j = currentRunAttributes.size();
                            while (j-- > 0) {
                                keys.add(currentRunAttributes.get(j));
                            }
                        }
                    }
                    i++;
                }
                return keys;
            }
        }

        public Object getAttribute(Attribute attribute) {
            int runIndex = currentRunIndex;
            if (runIndex < 0) {
                return null;
            }
            return AttributedString.this.getAttributeCheckRange(attribute, runIndex, beginIndex, endIndex);
        }

        // internally used methods

        private AttributedString getString() {
            return AttributedString.this;
        }

        // set the current index, update information about the current run if necessary,
        // return the character at the current index
        private char internalSetIndex(int position) {
            currentIndex = position;
            if (position < currentRunStart || position >= currentRunLimit) {
                updateRunInfo();
            }
            if (currentIndex == endIndex) {
                return DONE;
            } else {
                return charAt(position);
            }
        }

        // update the information about the current run
        private void updateRunInfo() {
            if (currentIndex == endIndex) {
                currentRunStart = currentRunLimit = endIndex;
                currentRunIndex = -1;
            } else {
                synchronized (AttributedString.this) {
                    int runIndex = -1;
                    while (runIndex < runCount - 1 && runStarts[runIndex + 1] <= currentIndex)
                        runIndex++;
                    currentRunIndex = runIndex;
                    if (runIndex >= 0) {
                        currentRunStart = runStarts[runIndex];
                        if (currentRunStart < beginIndex)
                            currentRunStart = beginIndex;
                    }
                    else {
                        currentRunStart = beginIndex;
                    }
                    if (runIndex < runCount - 1) {
                        currentRunLimit = runStarts[runIndex + 1];
                        if (currentRunLimit > endIndex)
                            currentRunLimit = endIndex;
                    }
                    else {
                        currentRunLimit = endIndex;
                    }
                }
            }
        }

    }

    // the map class associated with this string class, giving access to the attributes of one run

    private final class AttributeMap extends AbstractMap<Attribute,Object> {

        int runIndex;
        int beginIndex;
        int endIndex;

        AttributeMap(int runIndex, int beginIndex, int endIndex) {
            this.runIndex = runIndex;
            this.beginIndex = beginIndex;
            this.endIndex = endIndex;
        }

        public Set<Map.Entry<Attribute, Object>> entrySet() {
            HashSet<Map.Entry<Attribute, Object>> set = new HashSet<>();
            synchronized (AttributedString.this) {
                int size = runAttributes[runIndex].size();
                for (int i = 0; i < size; i++) {
                    Attribute key = runAttributes[runIndex].get(i);
                    Object value = runAttributeValues[runIndex].get(i);
                    if (value instanceof Annotation) {
                        value = AttributedString.this.getAttributeCheckRange(key,
                                                             runIndex, beginIndex, endIndex);
                        if (value == null) {
                            continue;
                        }
                    }

                    Map.Entry<Attribute, Object> entry = new AttributeEntry(key, value);
                    set.add(entry);
                }
            }
            return set;
        }

        public Object get(Object key) {
            return AttributedString.this.getAttributeCheckRange((Attribute) key, runIndex, beginIndex, endIndex);
        }
    }
}

class AttributeEntry implements Map.Entry<Attribute,Object> {

    private Attribute key;
    private Object value;

    AttributeEntry(Attribute key, Object value) {
        this.key = key;
        this.value = value;
    }

    public boolean equals(Object o) {
        if (!(o instanceof AttributeEntry other)) {
            return false;
        }
        return other.key.equals(key) && Objects.equals(other.value, value);
    }

    public Attribute getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public Object setValue(Object newValue) {
        throw new UnsupportedOperationException();
    }

    public int hashCode() {
        return key.hashCode() ^ (value==null ? 0 : value.hashCode());
    }

    public String toString() {
        return key.toString()+"="+value.toString();
    }
}
