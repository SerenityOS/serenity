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

package sun.text;

import jdk.internal.icu.lang.UCharacter;
import jdk.internal.icu.text.NormalizerBase;

/**
 * This Normalizer is for Unicode 3.2 support for IDNA only.
 * Developers should not use this class.
 *
 * @since 1.6
 */
public final class Normalizer {

    private Normalizer() {};

    /**
     * Option to select Unicode 3.2 (without corrigendum 4 corrections) for
     * normalization.
     */
    public static final int UNICODE_3_2 = NormalizerBase.UNICODE_3_2_0_ORIGINAL;

    /**
     * Normalize a sequence of char values.
     * The sequence will be normalized according to the specified normalization
     * from.
     * @param src        The sequence of char values to normalize.
     * @param form       The normalization form; one of
     *                   {@link java.text.Normalizer.Form#NFC},
     *                   {@link java.text.Normalizer.Form#NFD},
     *                   {@link java.text.Normalizer.Form#NFKC},
     *                   {@link java.text.Normalizer.Form#NFKD}
     * @param option     The normalization option;
     *                   {@link sun.text.Normalizer#UNICODE_3_2}
     * @return The normalized String
     * @throws NullPointerException If <code>src</code> or <code>form</code>
     * is null.
     */
    public static String normalize(CharSequence src,
                                   java.text.Normalizer.Form form,
                                   int option) {
        return NormalizerBase.normalize(src.toString(), form, option);
    };

    /**
     * Determines if the given sequence of char values is normalized.
     * @param src        The sequence of char values to be checked.
     * @param form       The normalization form; one of
     *                   {@link java.text.Normalizer.Form#NFC},
     *                   {@link java.text.Normalizer.Form#NFD},
     *                   {@link java.text.Normalizer.Form#NFKC},
     *                   {@link java.text.Normalizer.Form#NFKD}
     * @param option     The normalization option;
     *                   {@link sun.text.Normalizer#UNICODE_3_2}
     * @return true if the sequence of char values is normalized;
     * false otherwise.
     * @throws NullPointerException If <code>src</code> or <code>form</code>
     * is null.
     */
    public static boolean isNormalized(CharSequence src,
                                       java.text.Normalizer.Form form,
                                       int option) {
        return NormalizerBase.isNormalized(src.toString(), form, option);
    }

    /**
     * Returns the combining class of the given character
     * @param ch character to retrieve combining class of
     * @return combining class of the given character
     */
    public static final int getCombiningClass(int ch) {
        return UCharacter.getCombiningClass(ch);
    }
}
