/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.print.attribute.standard;

import java.io.Serial;

import javax.print.attribute.Attribute;
import javax.print.attribute.SetOfIntegerSyntax;
import javax.print.attribute.SupportedValuesAttribute;

/**
 * Class {@code NumberUpSupported} is a printing attribute class, a set of
 * integers, that gives the supported values for a {@link NumberUp NumberUp}
 * attribute.
 * <p>
 * <b>IPP Compatibility:</b> The NumberUpSupported attribute's canonical array
 * form gives the lower and upper bound for each range of number-up to be
 * included in an IPP "number-up-supported" attribute. See class
 * {@link SetOfIntegerSyntax SetOfIntegerSyntax} for an explanation of canonical
 * array form. The category name returned by {@code getName()} gives the IPP
 * attribute name.
 *
 * @author Alan Kaminsky
 */
public final class NumberUpSupported    extends SetOfIntegerSyntax
        implements SupportedValuesAttribute {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -1041573395759141805L;

    /**
     * Construct a new number up supported attribute with the given members. The
     * supported values for NumberUp are specified in "array form;" see class
     * {@link SetOfIntegerSyntax SetOfIntegerSyntax} for
     * an explanation of array form.
     *
     * @param  members set members in array form
     * @throws NullPointerException if {@code members} is {@code null} or any
     *         element of {@code members} is {@code null}
     * @throws IllegalArgumentException if any element of {@code members} is not
     *         a length-one or length-two array. Also if {@code members} is a
     *         zero-length array or if any member of the set is less than 1.
     */
    public NumberUpSupported(int[][] members) {
        super (members);
        if (members == null) {
            throw new NullPointerException("members is null");
        }
        int[][] myMembers = getMembers();
        int n = myMembers.length;
        if (n == 0) {
            throw new IllegalArgumentException("members is zero-length");
        }
        int i;
        for (i = 0; i < n; ++ i) {
            if (myMembers[i][0] < 1) {
                throw new IllegalArgumentException
                    ("Number up value must be > 0");
            }
        }
    }

    /**
     * Construct a new number up supported attribute containing a single
     * integer. That is, only the one value of {@code NumberUp} is supported.
     *
     * @param  member set member
     * @throws IllegalArgumentException if {@code member < 1}
     */
    public NumberUpSupported(int member) {
        super (member);
        if (member < 1) {
            throw new IllegalArgumentException("Number up value must be > 0");
        }
    }

    /**
     * Construct a new number up supported attribute containing a single range
     * of integers. That is, only those values of {@code NumberUp} in the one
     * range are supported.
     *
     * @param  lowerBound lower bound of the range
     * @param  upperBound upper bound of the range
     * @throws IllegalArgumentException if a {@code null} range is specified or
     *         if a {@code non-null} range is specified with {@code lowerBound}
     *         less than 1
     */
    public NumberUpSupported(int lowerBound, int upperBound) {
        super (lowerBound, upperBound);
        if (lowerBound > upperBound) {
            throw new IllegalArgumentException("Null range specified");
        } else if (lowerBound < 1) {
            throw new IllegalArgumentException
                ("Number up value must be > 0");
        }
    }

    /**
     * Returns whether this number up supported attribute is equivalent to the
     * passed in object. To be equivalent, all of the following conditions must
     * be true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class {@code NumberUpSupported}.
     *   <li>This number up supported attribute's members and {@code object}'s
     *   members are the same.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this number up
     *         supported attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {
        return (super.equals (object) &&
                object instanceof NumberUpSupported);
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code NumberUpSupported}, the category is class
     * {@code NumberUpSupported} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return NumberUpSupported.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code NumberUpSupported}, the category name is
     * {@code "number-up-supported"}.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "number-up-supported";
    }
}
