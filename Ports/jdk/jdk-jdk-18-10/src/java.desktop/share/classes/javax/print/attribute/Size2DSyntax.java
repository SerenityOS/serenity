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

import java.io.Serial;
import java.io.Serializable;

/**
 * Class {@code Size2DSyntax} is an abstract base class providing the common
 * implementation of all attributes denoting a size in two dimensions.
 * <p>
 * A two-dimensional size attribute's value consists of two items, the {@code X}
 * dimension and the {@code Y} dimension. A two-dimensional size attribute may
 * be constructed by supplying the two values and indicating the units in which
 * the values are measured. Methods are provided to return a two-dimensional
 * size attribute's values, indicating the units in which the values are to be
 * returned. The two most common size units are inches (in) and millimeters
 * (mm), and exported constants {@link #INCH INCH} and {@link #MM MM} are
 * provided for indicating those units.
 * <p>
 * Once constructed, a two-dimensional size attribute's value is immutable.
 * <p>
 * <b>Design</b>
 * <p>
 * A two-dimensional size attribute's {@code X} and {@code Y} dimension values
 * are stored internally as integers in units of micrometers (&#181;m), where 1
 * micrometer = 10<SUP>-6</SUP> meter = 1/1000 millimeter = 1/25400 inch. This
 * permits dimensions to be represented exactly to a precision of 1/1000 mm (= 1
 * &#181;m) or 1/100 inch (= 254 &#181;m). If fractional inches are expressed in
 * negative powers of two, this permits dimensions to be represented exactly to
 * a precision of 1/8 inch (= 3175 &#181;m) but not 1/16 inch (because 1/16 inch
 * does not equal an integral number of &#181;m).
 * <p>
 * Storing the dimensions internally in common units of &#181;m lets two size
 * attributes be compared without regard to the units in which they were
 * created; for example, 8.5 in will compare equal to 215.9 mm, as they both are
 * stored as 215900 &#181;m. For example, a lookup service can match resolution
 * attributes based on equality of their serialized representations regardless
 * of the units in which they were created. Using integers for internal storage
 * allows precise equality comparisons to be done, which would not be guaranteed
 * if an internal floating point representation were used. Note that if you're
 * looking for {@code U.S. letter} sized media in metric units, you have to
 * search for a media size of 215.9 x 279.4 mm; rounding off to an integral
 * 216 x 279 mm will not match.
 * <p>
 * The exported constant {@link #INCH INCH} is actually the conversion factor by
 * which to multiply a value in inches to get the value in &#181;m. Likewise,
 * the exported constant {@link #MM MM} is the conversion factor by which to
 * multiply a value in mm to get the value in &#181;m. A client can specify a
 * resolution value in units other than inches or mm by supplying its own
 * conversion factor. However, since the internal units of &#181;m was chosen
 * with supporting only the external units of inch and mm in mind, there is no
 * guarantee that the conversion factor for the client's units will be an exact
 * integer. If the conversion factor isn't an exact integer, resolution values
 * in the client's units won't be stored precisely.
 *
 * @author Alan Kaminsky
 */
public abstract class Size2DSyntax implements Serializable, Cloneable {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 5584439964938660530L;

    /**
     * {@code X} dimension in units of micrometers (&#181;m).
     *
     * @serial
     */
    private int x;

    /**
     * {@code Y} dimension in units of micrometers (&#181;m).
     *
     * @serial
     */
    private int y;

    /**
     * Value to indicate units of inches (in). It is actually the conversion
     * factor by which to multiply inches to yield &#181;m (25400).
     */
    public static final int INCH = 25400;

    /**
     * Value to indicate units of millimeters (mm). It is actually the
     * conversion factor by which to multiply mm to yield &#181;m (1000).
     */
    public static final int MM = 1000;

    /**
     * Construct a new two-dimensional size attribute from the given
     * floating-point values.
     *
     * @param  x {@code X} dimension
     * @param  y {@code Y} dimension
     * @param  units unit conversion factor, e.g. {@link #INCH INCH} or
     *         {@link #MM MM}
     * @throws IllegalArgumentException if {@code x < 0} or {@code y < 0} or
     *         {@code units < 1}
     */
    protected Size2DSyntax(float x, float y, int units) {
        if (x < 0.0f) {
            throw new IllegalArgumentException("x < 0");
        }
        if (y < 0.0f) {
            throw new IllegalArgumentException("y < 0");
        }
        if (units < 1) {
            throw new IllegalArgumentException("units < 1");
        }
        this.x = (int) (x * units + 0.5f);
        this.y = (int) (y * units + 0.5f);
    }

    /**
     * Construct a new two-dimensional size attribute from the given integer
     * values.
     *
     * @param  x {@code X} dimension
     * @param  y {@code Y} dimension
     * @param  units unit conversion factor, e.g. {@link #INCH INCH} or
     *         {@link #MM MM}
     * @throws IllegalArgumentException if {@code x < 0} or {@code y < 0} or
     *         {@code units < 1}
     */
    protected Size2DSyntax(int x, int y, int units) {
        if (x < 0) {
            throw new IllegalArgumentException("x < 0");
        }
        if (y < 0) {
            throw new IllegalArgumentException("y < 0");
        }
        if (units < 1) {
            throw new IllegalArgumentException("units < 1");
        }
        this.x = x * units;
        this.y = y * units;
    }

    /**
     * Convert a value from micrometers to some other units. The result is
     * returned as a floating-point number.
     *
     * @param  x value (micrometers) to convert
     * @param  units unit conversion factor, e.g. {@link #INCH INCH} or
     *         {@link #MM MM}
     * @return the value of {@code x} converted to the desired units
     * @throws IllegalArgumentException if {@code units < 1}
     */
    private static float convertFromMicrometers(int x, int units) {
        if (units < 1) {
            throw new IllegalArgumentException("units is < 1");
        }
        return ((float)x) / ((float)units);
    }

    /**
     * Get this two-dimensional size attribute's dimensions in the given units
     * as floating-point values.
     *
     * @param  units unit conversion factor, e.g. {@link #INCH INCH} or
     *         {@link #MM MM}
     * @return a two-element array with the {@code X} dimension at index 0 and
     *         the {@code Y} dimension at index 1
     * @throws IllegalArgumentException if {@code units < 1}
     */
    public float[] getSize(int units) {
        return new float[] {getX(units), getY(units)};
    }

    /**
     * Returns this two-dimensional size attribute's {@code X} dimension in the
     * given units as a floating-point value.
     *
     * @param  units unit conversion factor, e.g. {@link #INCH INCH} or
     *         {@link #MM MM}
     * @return {@code X} dimension
     * @throws IllegalArgumentException if {@code units < 1}
     */
    public float getX(int units) {
        return convertFromMicrometers(x, units);
    }

    /**
     * Returns this two-dimensional size attribute's {@code Y} dimension in the
     * given units as a floating-point value.
     *
     * @param  units unit conversion factor, e.g. {@link #INCH INCH} or
     *         {@link #MM MM}
     * @return {@code Y} dimension
     * @throws IllegalArgumentException if {@code units < 1}
     */
    public float getY(int units) {
        return convertFromMicrometers(y, units);
    }

    /**
     * Returns a string version of this two-dimensional size attribute in the
     * given units. The string takes the form <code>"<i>X</i>x<i>Y</i>
     * <i>U</i>"</code>, where <i>X</i> is the {@code X} dimension, <i>Y</i> is
     * the {@code Y} dimension, and <i>U</i> is the units name. The values are
     * displayed in floating point.
     *
     * @param  units unit conversion factor, e.g. {@link #INCH INCH} or
     *         {@link #MM MM}
     * @param  unitsName units name string, e.g. {@code in} or {@code mm}. If
     *         {@code null}, no units name is appended to the result
     * @return {@code String} version of this two-dimensional size attribute
     * @throws IllegalArgumentException if {@code units < 1}
     */
    public String toString(int units, String unitsName) {
        StringBuilder result = new StringBuilder();
        result.append(getX (units));
        result.append('x');
        result.append(getY (units));
        if (unitsName != null) {
            result.append(' ');
            result.append(unitsName);
        }
        return result.toString();
    }

    /**
     * Returns whether this two-dimensional size attribute is equivalent to the
     * passed in object. To be equivalent, all of the following conditions must
     * be true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class {@code Size2DSyntax}
     *   <li>This attribute's {@code X} dimension is equal to {@code object}'s
     *   {@code X} dimension.
     *   <li>This attribute's {@code Y} dimension is equal to {@code object}'s
     *   {@code Y} dimension.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this
     *         two-dimensional size attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {
        return(object != null &&
               object instanceof Size2DSyntax &&
               this.x == ((Size2DSyntax) object).x &&
               this.y == ((Size2DSyntax) object).y);
    }

    /**
     * Returns a hash code value for this two-dimensional size attribute.
     */
    public int hashCode() {
        return (((x & 0x0000FFFF)      ) |
                ((y & 0x0000FFFF) << 16));
    }

    /**
     * Returns a string version of this two-dimensional size attribute. The
     * string takes the form <code>"<i>X</i>x<i>Y</i> um"</code>, where <i>X</i>
     * is the {@code X} dimension and <i>Y</i> is the {@code Y} dimension. The
     * values are reported in the internal units of micrometers.
     */
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(x);
        result.append('x');
        result.append(y);
        result.append(" um");
        return result.toString();
    }

    /**
     * Returns this two-dimensional size attribute's {@code X} dimension in
     * units of micrometers (&#181;m). (For use in a subclass.)
     *
     * @return {@code X} dimension (&#181;m)
     */
    protected int getXMicrometers(){
        return x;
    }

    /**
     * Returns this two-dimensional size attribute's {@code Y} dimension in
     * units of micrometers (&#181;m). (For use in a subclass.)
     *
     * @return {@code Y} dimension (&#181;m)
     */
    protected int getYMicrometers() {
        return y;
    }
}
