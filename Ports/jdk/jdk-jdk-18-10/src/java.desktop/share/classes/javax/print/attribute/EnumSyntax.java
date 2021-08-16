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

package javax.print.attribute;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;

/**
 * Class {@code EnumSyntax} is an abstract base class providing the common
 * implementation of all "type safe enumeration" objects. An enumeration class
 * (which extends class {@code EnumSyntax}) provides a group of enumeration
 * values (objects) that are singleton instances of the enumeration class; for
 * example:
 *
 * <pre>
 *     public class Bach extends EnumSyntax {
 *         public static final Bach JOHANN_SEBASTIAN     = new Bach(0);
 *         public static final Bach WILHELM_FRIEDEMANN   = new Bach(1);
 *         public static final Bach CARL_PHILIP_EMMANUEL = new Bach(2);
 *         public static final Bach JOHANN_CHRISTIAN     = new Bach(3);
 *         public static final Bach P_D_Q                = new Bach(4);
 *
 *         private static final String[] stringTable = {
 *             "Johann Sebastian Bach",
 *              "Wilhelm Friedemann Bach",
 *              "Carl Philip Emmanuel Bach",
 *              "Johann Christian Bach",
 *              "P.D.Q. Bach"
 *         };
 *
 *         protected String[] getStringTable() {
 *             return stringTable;
 *         }
 *
 *         private static final Bach[] enumValueTable = {
 *             JOHANN_SEBASTIAN,
 *              WILHELM_FRIEDEMANN,
 *              CARL_PHILIP_EMMANUEL,
 *              JOHANN_CHRISTIAN,
 *              P_D_Q
 *         };
 *
 *         protected EnumSyntax[] getEnumValueTable() {
 *             return enumValueTable;
 *         }
 *     }
 * </pre>
 * You can then write code that uses the {@code ==} and {@code !=} operators to
 * test enumeration values; for example:
 * <pre>
 *     Bach theComposer;
 *     . . .
 *     if (theComposer == Bach.JOHANN_SEBASTIAN) {
 *         System.out.println ("The greatest composer of all time!");
 *     }
 * </pre>
 * The {@code equals()} method for an enumeration class just does a test for
 * identical objects ({@code ==}).
 * <p>
 * You can convert an enumeration value to a string by calling
 * {@link #toString() toString()}. The string is obtained from a table supplied
 * by the enumeration class.
 * <p>
 * Under the hood, an enumeration value is just an integer, a different integer
 * for each enumeration value within an enumeration class. You can get an
 * enumeration value's integer value by calling {@link #getValue() getValue()}.
 * An enumeration value's integer value is established when it is constructed
 * (see {@link #EnumSyntax(int) EnumSyntax(int)}). Since the constructor is
 * protected, the only possible enumeration values are the singleton objects
 * declared in the enumeration class; additional enumeration values cannot be
 * created at run time.
 * <p>
 * You can define a subclass of an enumeration class that extends it with
 * additional enumeration values. The subclass's enumeration values' integer
 * values need not be distinct from the superclass's enumeration values' integer
 * values; the {@code ==}, {@code !=}, {@code equals()}, and {@code toString()}
 * methods will still work properly even if the subclass uses some of the same
 * integer values as the superclass. However, the application in which the
 * enumeration class and subclass are used may need to have distinct integer
 * values in the superclass and subclass.
 *
 * @author David Mendenhall
 * @author Alan Kaminsky
 */
public abstract class EnumSyntax implements Serializable, Cloneable {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -2739521845085831642L;

    /**
     * This enumeration value's integer value.
     *
     * @serial
     */
    private int value;

    /**
     * Construct a new enumeration value with the given integer value.
     *
     * @param  value Integer value
     */
    protected EnumSyntax(int value) {
        this.value = value;
    }

    /**
     * Returns this enumeration value's integer value.
     *
     * @return the value
     */
    public int getValue() {
        return value;
    }

    /**
     * Returns a clone of this enumeration value, which to preserve the
     * semantics of enumeration values is the same object as this enumeration
     * value.
     */
    public Object clone() {
        return this;
    }

    /**
     * Returns a hash code value for this enumeration value. The hash code is
     * just this enumeration value's integer value.
     */
    public int hashCode() {
        return value;
    }

    /**
     * Returns a string value corresponding to this enumeration value.
     */
    public String toString() {

        String[] theTable = getStringTable();
        int theIndex = value - getOffset();
        return
            theTable != null && theIndex >= 0 && theIndex < theTable.length ?
            theTable[theIndex] :
            Integer.toString (value);
    }

    /**
     * During object input, convert this deserialized enumeration instance to
     * the proper enumeration value defined in the enumeration attribute class.
     *
     * @return The enumeration singleton value stored at index <i>i</i>-<i>L</i>
     *         in the enumeration value table returned by
     *         {@link #getEnumValueTable() getEnumValueTable()}, where <i>i</i>
     *         is this enumeration value's integer value and <i>L</i> is the
     *         value returned by {@link #getOffset() getOffset()}
     * @throws ObjectStreamException if the stream can't be deserialised
     * @throws InvalidObjectException if the enumeration value table is
     *         {@code null}, this enumeration value's integer value does not
     *         correspond to an element in the enumeration value table, or the
     *         corresponding element in the enumeration value table is
     *         {@code null}. (Note:
     *         {@link InvalidObjectException InvalidObjectException} is a
     *         subclass of {@link ObjectStreamException ObjectStreamException},
     *         which {@code readResolve()} is declared to throw.)
     */
    @Serial
    protected Object readResolve() throws ObjectStreamException {

        EnumSyntax[] theTable = getEnumValueTable();

        if (theTable == null) {
            throw new InvalidObjectException(
                                "Null enumeration value table for class " +
                                getClass());
        }

        int theOffset = getOffset();
        int theIndex = value - theOffset;

        if (0 > theIndex || theIndex >= theTable.length) {
            throw new InvalidObjectException
                ("Integer value = " +  value + " not in valid range " +
                 theOffset + ".." + (theOffset + theTable.length - 1) +
                 "for class " + getClass());
        }

        EnumSyntax result = theTable[theIndex];
        if (result == null) {
            throw new InvalidObjectException
                ("No enumeration value for integer value = " +
                 value + "for class " + getClass());
        }
        return result;
    }

    // Hidden operations to be implemented in a subclass.

    /**
     * Returns the string table for this enumeration value's enumeration class.
     * The enumeration class's integer values are assumed to lie in the range
     * <i>L</i>..<i>L</i>+<i>N</i>-1, where <i>L</i> is the value returned by
     * {@link #getOffset() getOffset()} and <i>N</i> is the length of the string
     * table. The element in the string table at index <i>i</i>-<i>L</i> is the
     * value returned by {@link #toString() toString()} for the enumeration
     * value whose integer value is <i>i</i>. If an integer within the above
     * range is not used by any enumeration value, leave the corresponding table
     * element {@code null}.
     * <p>
     * The default implementation returns {@code null}. If the enumeration class
     * (a subclass of class {@code EnumSyntax}) does not override this method to
     * return a {@code non-null} string table, and the subclass does not
     * override the {@link #toString() toString()} method, the base class
     * {@link #toString() toString()} method will return just a string
     * representation of this enumeration value's integer value.
     *
     * @return the string table
     */
    protected String[] getStringTable() {
        return null;
    }

    /**
     * Returns the enumeration value table for this enumeration value's
     * enumeration class. The enumeration class's integer values are assumed to
     * lie in the range <i>L</i>..<i>L</i>+<i>N</i>-1, where <i>L</i> is the
     * value returned by {@link #getOffset() getOffset()} and <i>N</i> is the
     * length of the enumeration value table. The element in the enumeration
     * value table at index <i>i</i>-<i>L</i> is the enumeration value object
     * whose integer value is <i>i</i>; the {@link #readResolve() readResolve()}
     * method needs this to preserve singleton semantics during deserialization
     * of an enumeration instance. If an integer within the above range is not
     * used by any enumeration value, leave the corresponding table element
     * {@code null}.
     * <p>
     * The default implementation returns {@code null}. If the enumeration class
     * (a subclass of class EnumSyntax) does not override this method to return
     * a {@code non-null} enumeration value table, and the subclass does not
     * override the {@link #readResolve() readResolve()} method, the base class
     * {@link #readResolve() readResolve()} method will throw an exception
     * whenever an enumeration instance is deserialized from an object input
     * stream.
     *
     * @return the value table
     */
    protected EnumSyntax[] getEnumValueTable() {
        return null;
    }

    /**
     * Returns the lowest integer value used by this enumeration value's
     * enumeration class.
     * <p>
     * The default implementation returns 0. If the enumeration class (a
     * subclass of class {@code EnumSyntax}) uses integer values starting at
     * other than 0, override this method in the subclass.
     *
     * @return the offset of the lowest enumeration value
     */
    protected int getOffset() {
        return 0;
    }
}
