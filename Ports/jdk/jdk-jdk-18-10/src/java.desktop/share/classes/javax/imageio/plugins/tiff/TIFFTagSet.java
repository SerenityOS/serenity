/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
package javax.imageio.plugins.tiff;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A class representing a set of TIFF tags.  Each tag in the set must have
 * a unique number (this is a limitation of the TIFF specification itself).
 *
 * <p> This class and its subclasses are responsible for mapping
 * between raw tag numbers and {@code TIFFTag} objects, which
 * contain additional information about each tag, such as the tag's
 * name, legal data types, and mnemonic names for some or all of its
 * data values.
 *
 * @since 9
 * @see   TIFFTag
 */
public class TIFFTagSet {

    private SortedMap<Integer,TIFFTag> allowedTagsByNumber = new TreeMap<Integer,TIFFTag>();

    private SortedMap<String,TIFFTag> allowedTagsByName = new TreeMap<String,TIFFTag>();

    /**
     * Constructs a TIFFTagSet.
     */
    private TIFFTagSet() {}

    /**
     * Constructs a {@code TIFFTagSet}, given a {@code List}
     * of {@code TIFFTag} objects.
     *
     * @param tags a {@code List} object containing
     * {@code TIFFTag} objects to be added to this tag set.
     *
     * @throws IllegalArgumentException if {@code tags} is
     * {@code null}, or contains objects that are not instances
     * of the {@code TIFFTag} class.
     */
    public TIFFTagSet(List<TIFFTag> tags) {
        if (tags == null) {
            throw new IllegalArgumentException("tags == null!");
        }
        for (Object o : tags) {
            if (!(o instanceof TIFFTag)) {
                throw new IllegalArgumentException(
                                               "tags contains a non-TIFFTag!");
            }
            TIFFTag tag = (TIFFTag)o;

            allowedTagsByNumber.put(Integer.valueOf(tag.getNumber()), tag);
            allowedTagsByName.put(tag.getName(), tag);
        }
    }

    /**
     * Returns the {@code TIFFTag} from this set that is
     * associated with the given tag number, or {@code null} if
     * no tag exists for that number.
     *
     * @param tagNumber the number of the tag to be retrieved.
     *
     * @return the numbered {@code TIFFTag}, or {@code null}.
     */
    public TIFFTag getTag(int tagNumber) {
        return allowedTagsByNumber.get(Integer.valueOf(tagNumber));
    }

    /**
     * Returns the {@code TIFFTag} having the given tag name, or
     * {@code null} if the named tag does not belong to this tag set.
     *
     * @param tagName the name of the tag to be retrieved, as a
     * {@code String}.
     *
     * @return the named {@code TIFFTag}, or {@code null}.
     *
     * @throws IllegalArgumentException if {@code tagName} is
     * {@code null}.
     */
    public TIFFTag getTag(String tagName) {
        if (tagName == null) {
            throw new IllegalArgumentException("tagName == null!");
        }
        return allowedTagsByName.get(tagName);
    }

    /**
     * Retrieves an unmodifiable numerically increasing set of tag numbers.
     *
     * <p>The returned object is unmodifiable and contains the tag
     * numbers of all {@code TIFFTag}s in this {@code TIFFTagSet}
     * sorted into ascending order according to
     * {@link Integer#compareTo(Object)}.</p>
     *
     * @return All tag numbers in this set.
     */
    public SortedSet<Integer> getTagNumbers() {
        Set<Integer> tagNumbers = allowedTagsByNumber.keySet();
        SortedSet<Integer> sortedTagNumbers;
        if(tagNumbers instanceof SortedSet) {
            sortedTagNumbers = (SortedSet<Integer>)tagNumbers;
        } else {
            sortedTagNumbers = new TreeSet<Integer>(tagNumbers);
        }

        return Collections.unmodifiableSortedSet(sortedTagNumbers);
    }

    /**
     * Retrieves an unmodifiable lexicographically increasing set of tag names.
     *
     * <p>The returned object is unmodifiable and contains the tag
     * names of all {@code TIFFTag}s in this {@code TIFFTagSet}
     * sorted into ascending order according to
     * {@link String#compareTo(Object)}.</p>
     *
     * @return All tag names in this set.
     */
    public SortedSet<String> getTagNames() {
        Set<String> tagNames = allowedTagsByName.keySet();
        SortedSet<String> sortedTagNames;
        if(tagNames instanceof SortedSet) {
            sortedTagNames = (SortedSet<String>)tagNames;
        } else {
            sortedTagNames = new TreeSet<String>(tagNames);
        }

        return Collections.unmodifiableSortedSet(sortedTagNames);
    }
}
