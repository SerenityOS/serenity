/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.charset;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * A description of the result state of a coder.
 *
 * <p> A charset coder, that is, either a decoder or an encoder, consumes bytes
 * (or characters) from an input buffer, translates them, and writes the
 * resulting characters (or bytes) to an output buffer.  A coding process
 * terminates for one of four categories of reasons, which are described by
 * instances of this class:
 *
 * <ul>
 *
 *   <li><p> <i>Underflow</i> is reported when there is no more input to be
 *   processed, or there is insufficient input and additional input is
 *   required.  This condition is represented by the unique result object
 *   {@link #UNDERFLOW}, whose {@link #isUnderflow() isUnderflow} method
 *   returns {@code true}.  </p></li>
 *
 *   <li><p> <i>Overflow</i> is reported when there is insufficient room
 *   remaining in the output buffer.  This condition is represented by the
 *   unique result object {@link #OVERFLOW}, whose {@link #isOverflow()
 *   isOverflow} method returns {@code true}.  </p></li>
 *
 *   <li><p> A <i>malformed-input error</i> is reported when a sequence of
 *   input units is not well-formed.  Such errors are described by instances of
 *   this class whose {@link #isMalformed() isMalformed} method returns
 *   {@code true} and whose {@link #length() length} method returns the length
 *   of the malformed sequence.  There is one unique instance of this class for
 *   all malformed-input errors of a given length.  </p></li>
 *
 *   <li><p> An <i>unmappable-character error</i> is reported when a sequence
 *   of input units denotes a character that cannot be represented in the
 *   output charset.  Such errors are described by instances of this class
 *   whose {@link #isUnmappable() isUnmappable} method returns {@code true} and
 *   whose {@link #length() length} method returns the length of the input
 *   sequence denoting the unmappable character.  There is one unique instance
 *   of this class for all unmappable-character errors of a given length.
 *   </p></li>
 *
 * </ul>
 *
 * <p> For convenience, the {@link #isError() isError} method returns {@code true}
 * for result objects that describe malformed-input and unmappable-character
 * errors but {@code false} for those that describe underflow or overflow
 * conditions.  </p>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 */

public class CoderResult {

    private static final int CR_UNDERFLOW  = 0;
    private static final int CR_OVERFLOW   = 1;
    private static final int CR_ERROR_MIN  = 2;
    private static final int CR_MALFORMED  = 2;
    private static final int CR_UNMAPPABLE = 3;

    private static final String[] names
        = { "UNDERFLOW", "OVERFLOW", "MALFORMED", "UNMAPPABLE" };

    private final int type;
    private final int length;

    private CoderResult(int type, int length) {
        this.type = type;
        this.length = length;
    }

    /**
     * Returns a string describing this coder result.
     *
     * @return  A descriptive string
     */
    public String toString() {
        String nm = names[type];
        return isError() ? nm + "[" + length + "]" : nm;
    }

    /**
     * Tells whether or not this object describes an underflow condition.
     *
     * @return  {@code true} if, and only if, this object denotes underflow
     */
    public boolean isUnderflow() {
        return (type == CR_UNDERFLOW);
    }

    /**
     * Tells whether or not this object describes an overflow condition.
     *
     * @return  {@code true} if, and only if, this object denotes overflow
     */
    public boolean isOverflow() {
        return (type == CR_OVERFLOW);
    }

    /**
     * Tells whether or not this object describes an error condition.
     *
     * @return  {@code true} if, and only if, this object denotes either a
     *          malformed-input error or an unmappable-character error
     */
    public boolean isError() {
        return (type >= CR_ERROR_MIN);
    }

    /**
     * Tells whether or not this object describes a malformed-input error.
     *
     * @return  {@code true} if, and only if, this object denotes a
     *          malformed-input error
     */
    public boolean isMalformed() {
        return (type == CR_MALFORMED);
    }

    /**
     * Tells whether or not this object describes an unmappable-character
     * error.
     *
     * @return  {@code true} if, and only if, this object denotes an
     *          unmappable-character error
     */
    public boolean isUnmappable() {
        return (type == CR_UNMAPPABLE);
    }

    /**
     * Returns the length of the erroneous input described by this
     * object&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * @return  The length of the erroneous input, a positive integer
     *
     * @throws  UnsupportedOperationException
     *          If this object does not describe an error condition, that is,
     *          if the {@link #isError() isError} does not return {@code true}
     */
    public int length() {
        if (!isError())
            throw new UnsupportedOperationException();
        return length;
    }

    /**
     * Result object indicating underflow, meaning that either the input buffer
     * has been completely consumed or, if the input buffer is not yet empty,
     * that additional input is required.
     */
    public static final CoderResult UNDERFLOW
        = new CoderResult(CR_UNDERFLOW, 0);

    /**
     * Result object indicating overflow, meaning that there is insufficient
     * room in the output buffer.
     */
    public static final CoderResult OVERFLOW
        = new CoderResult(CR_OVERFLOW, 0);

    private static final class Cache {
        static final Cache INSTANCE = new Cache();
        private Cache() {}

        final Map<Integer, CoderResult> unmappable = new ConcurrentHashMap<>();
        final Map<Integer, CoderResult> malformed  = new ConcurrentHashMap<>();
    }

    private static final CoderResult[] malformed4 = new CoderResult[] {
        new CoderResult(CR_MALFORMED, 1),
        new CoderResult(CR_MALFORMED, 2),
        new CoderResult(CR_MALFORMED, 3),
        new CoderResult(CR_MALFORMED, 4),
    };

    /**
     * Static factory method that returns the unique object describing a
     * malformed-input error of the given length.
     *
     * @param   length
     *          The given length
     *
     * @return  The requested coder-result object
     */
    public static CoderResult malformedForLength(int length) {
        if (length <= 0)
            throw new IllegalArgumentException("Non-positive length");
        if (length <= 4)
            return malformed4[length - 1];
        return Cache.INSTANCE.malformed.computeIfAbsent(length,
                n -> new CoderResult(CR_MALFORMED, n));
    }

    private static final CoderResult[] unmappable4 = new CoderResult[] {
        new CoderResult(CR_UNMAPPABLE, 1),
        new CoderResult(CR_UNMAPPABLE, 2),
        new CoderResult(CR_UNMAPPABLE, 3),
        new CoderResult(CR_UNMAPPABLE, 4),
    };

    /**
     * Static factory method that returns the unique result object describing
     * an unmappable-character error of the given length.
     *
     * @param   length
     *          The given length
     *
     * @return  The requested coder-result object
     */
    public static CoderResult unmappableForLength(int length) {
        if (length <= 0)
            throw new IllegalArgumentException("Non-positive length");
        if (length <= 4)
            return unmappable4[length - 1];
        return Cache.INSTANCE.unmappable.computeIfAbsent(length,
                n -> new CoderResult(CR_UNMAPPABLE, n));
    }

    /**
     * Throws an exception appropriate to the result described by this object.
     *
     * @throws  BufferUnderflowException
     *          If this object is {@link #UNDERFLOW}
     *
     * @throws  BufferOverflowException
     *          If this object is {@link #OVERFLOW}
     *
     * @throws  MalformedInputException
     *          If this object represents a malformed-input error; the
     *          exception's length value will be that of this object
     *
     * @throws  UnmappableCharacterException
     *          If this object represents an unmappable-character error; the
     *          exceptions length value will be that of this object
     */
    public void throwException()
        throws CharacterCodingException
    {
        switch (type) {
        case CR_UNDERFLOW:   throw new BufferUnderflowException();
        case CR_OVERFLOW:    throw new BufferOverflowException();
        case CR_MALFORMED:   throw new MalformedInputException(length);
        case CR_UNMAPPABLE:  throw new UnmappableCharacterException(length);
        default:
            assert false;
        }
    }

}
