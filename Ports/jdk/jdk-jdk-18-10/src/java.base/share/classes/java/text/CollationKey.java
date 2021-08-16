/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * (C) Copyright Taligent, Inc. 1996 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - All Rights Reserved
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 */

package java.text;

/**
 * A {@code CollationKey} represents a {@code String} under the
 * rules of a specific {@code Collator} object. Comparing two
 * {@code CollationKey}s returns the relative order of the
 * {@code String}s they represent. Using {@code CollationKey}s
 * to compare {@code String}s is generally faster than using
 * {@code Collator.compare}. Thus, when the {@code String}s
 * must be compared multiple times, for example when sorting a list
 * of {@code String}s. It's more efficient to use {@code CollationKey}s.
 *
 * <p>
 * You can not create {@code CollationKey}s directly. Rather,
 * generate them by calling {@code Collator.getCollationKey}.
 * You can only compare {@code CollationKey}s generated from
 * the same {@code Collator} object.
 *
 * <p>
 * Generating a {@code CollationKey} for a {@code String}
 * involves examining the entire {@code String}
 * and converting it to series of bits that can be compared bitwise. This
 * allows fast comparisons once the keys are generated. The cost of generating
 * keys is recouped in faster comparisons when {@code String}s need
 * to be compared many times. On the other hand, the result of a comparison
 * is often determined by the first couple of characters of each {@code String}.
 * {@code Collator.compare} examines only as many characters as it needs which
 * allows it to be faster when doing single comparisons.
 * <p>
 * The following example shows how {@code CollationKey}s might be used
 * to sort a list of {@code String}s.
 * <blockquote>
 * <pre>{@code
 * // Create an array of CollationKeys for the Strings to be sorted.
 * Collator myCollator = Collator.getInstance();
 * CollationKey[] keys = new CollationKey[3];
 * keys[0] = myCollator.getCollationKey("Tom");
 * keys[1] = myCollator.getCollationKey("Dick");
 * keys[2] = myCollator.getCollationKey("Harry");
 * sort(keys);
 *
 * //...
 *
 * // Inside body of sort routine, compare keys this way
 * if (keys[i].compareTo(keys[j]) > 0)
 *    // swap keys[i] and keys[j]
 *
 * //...
 *
 * // Finally, when we've returned from sort.
 * System.out.println(keys[0].getSourceString());
 * System.out.println(keys[1].getSourceString());
 * System.out.println(keys[2].getSourceString());
 * }</pre>
 * </blockquote>
 *
 * @see          Collator
 * @see          RuleBasedCollator
 * @author       Helena Shih
 * @since 1.1
 */

public abstract class CollationKey implements Comparable<CollationKey> {
    /**
     * Compare this CollationKey to the target CollationKey. The collation rules of the
     * Collator object which created these keys are applied. <strong>Note:</strong>
     * CollationKeys created by different Collators can not be compared.
     * @param target target CollationKey
     * @return Returns an integer value. Value is less than zero if this is less
     * than target, value is zero if this and target are equal and value is greater than
     * zero if this is greater than target.
     * @see java.text.Collator#compare
     */
    public abstract int compareTo(CollationKey target);

    /**
     * Returns the String that this CollationKey represents.
     *
     * @return the source string of this CollationKey
     */
    public String getSourceString() {
        return source;
    }


    /**
     * Converts the CollationKey to a sequence of bits. If two CollationKeys
     * could be legitimately compared, then one could compare the byte arrays
     * for each of those keys to obtain the same result.  Byte arrays are
     * organized most significant byte first.
     *
     * @return a byte array representation of the CollationKey
     */
    public abstract byte[] toByteArray();


  /**
   * CollationKey constructor.
   *
   * @param source the source string
   * @throws    NullPointerException if {@code source} is null
   * @since 1.6
   */
    protected CollationKey(String source) {
        if (source==null){
            throw new NullPointerException();
        }
        this.source = source;
    }

    private final String source;
}
