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
 * Class {@code ResolutionSyntax} is an abstract base class providing the common
 * implementation of all attributes denoting a printer resolution.
 * <p>
 * A resolution attribute's value consists of two items, the cross feed
 * direction resolution and the feed direction resolution. A resolution
 * attribute may be constructed by supplying the two values and indicating the
 * units in which the values are measured. Methods are provided to return a
 * resolution attribute's values, indicating the units in which the values are
 * to be returned. The two most common resolution units are dots per inch (dpi)
 * and dots per centimeter (dpcm), and exported constants {@link #DPI DPI} and
 * {@link #DPCM DPCM} are provided for indicating those units.
 * <p>
 * Once constructed, a resolution attribute's value is immutable.
 * <p>
 * <b>Design</b>
 * <p>
 * A resolution attribute's cross feed direction resolution and feed direction
 * resolution values are stored internally using units of dots per 100 inches
 * (dphi). Storing the values in dphi rather than, say, metric units allows
 * precise integer arithmetic conversions between dpi and dphi and between dpcm
 * and dphi: 1 dpi = 100 dphi, 1 dpcm = 254 dphi. Thus, the values can be stored
 * into and retrieved back from a resolution attribute in either units with no
 * loss of precision. This would not be guaranteed if a floating point
 * representation were used. However, roundoff error will in general occur if a
 * resolution attribute's values are created in one units and retrieved in
 * different units; for example, 600 dpi will be rounded to 236 dpcm, whereas
 * the true value (to five figures) is 236.22 dpcm.
 * <p>
 * Storing the values internally in common units of dphi lets two resolution
 * attributes be compared without regard to the units in which they were
 * created; for example, 300 dpcm will compare equal to 762 dpi, as they both
 * are stored as 76200 dphi. In particular, a lookup service can match
 * resolution attributes based on equality of their serialized representations
 * regardless of the units in which they were created. Again, using integers for
 * internal storage allows precise equality comparisons to be done, which would
 * not be guaranteed if a floating point representation were used.
 * <p>
 * The exported constant {@link #DPI DPI} is actually the conversion factor by
 * which to multiply a value in dpi to get the value in dphi. Likewise, the
 * exported constant {@link #DPCM DPCM} is the conversion factor by which to
 * multiply a value in dpcm to get the value in dphi. A client can specify a
 * resolution value in units other than dpi or dpcm by supplying its own
 * conversion factor. However, since the internal units of dphi was chosen with
 * supporting only the external units of dpi and dpcm in mind, there is no
 * guarantee that the conversion factor for the client's units will be an exact
 * integer. If the conversion factor isn't an exact integer, resolution values
 * in the client's units won't be stored precisely.
 *
 * @author David Mendenhall
 * @author Alan Kaminsky
 */
public abstract class ResolutionSyntax implements Serializable, Cloneable {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = 2706743076526672017L;

    /**
     * Cross feed direction resolution in units of dots per 100 inches (dphi).
     *
     * @serial
     */
    private int crossFeedResolution;

    /**
     * Feed direction resolution in units of dots per 100 inches (dphi).
     *
     * @serial
     */
    private int feedResolution;

    /**
     * Value to indicate units of dots per inch (dpi). It is actually the
     * conversion factor by which to multiply dpi to yield dphi (100).
     */
    public static final int DPI = 100;

    /**
     * Value to indicate units of dots per centimeter (dpcm). It is actually the
     * conversion factor by which to multiply dpcm to yield dphi (254).
     */
    public static final int DPCM = 254;

    /**
     * Construct a new resolution attribute from the given items.
     *
     * @param  crossFeedResolution cross feed direction resolution
     * @param  feedResolution feed direction resolution
     * @param  units unit conversion factor, e.g. {@link #DPI DPI} or
     *         {@link #DPCM DPCM}
     * @throws IllegalArgumentException if {@code crossFeedResolution < 1} or
     *         {@code feedResolution < 1} or {@code units < 1}
     */
    public ResolutionSyntax(int crossFeedResolution, int feedResolution,
                            int units) {

        if (crossFeedResolution < 1) {
            throw new IllegalArgumentException("crossFeedResolution is < 1");
        }
        if (feedResolution < 1) {
                throw new IllegalArgumentException("feedResolution is < 1");
        }
        if (units < 1) {
                throw new IllegalArgumentException("units is < 1");
        }

        this.crossFeedResolution = crossFeedResolution * units;
        this.feedResolution = feedResolution * units;
    }

    /**
     * Convert a value from dphi to some other units. The result is rounded to
     * the nearest integer.
     *
     * @param  dphi value (dphi) to convert
     * @param  units unit conversion factor, e.g. {@link #DPI DPI} or
     *        {@link #DPCM DPCM}
     * @return the value of {@code dphi} converted to the desired units
     * @throws IllegalArgumentException if {@code units < 1}
     */
    private static int convertFromDphi(int dphi, int units) {
        if (units < 1) {
            throw new IllegalArgumentException(": units is < 1");
        }
        int round = units / 2;
        return (dphi + round) / units;
    }

    /**
     * Get this resolution attribute's resolution values in the given units. The
     * values are rounded to the nearest integer.
     *
     * @param  units unit conversion factor, e.g. {@link #DPI DPI} or
     *         {@link #DPCM DPCM}
     * @return a two-element array with the cross feed direction resolution at
     *         index 0 and the feed direction resolution at index 1
     * @throws IllegalArgumentException if {@code units < 1}
     */
    public int[] getResolution(int units) {
        return new int[] { getCrossFeedResolution(units),
                               getFeedResolution(units)
                               };
    }

    /**
     * Returns this resolution attribute's cross feed direction resolution in
     * the given units. The value is rounded to the nearest integer.
     *
     * @param  units unit conversion factor, e.g. {@link #DPI DPI} or
     *         {@link #DPCM DPCM}
     * @return cross feed direction resolution
     * @throws IllegalArgumentException if {@code units < 1}
     */
    public int getCrossFeedResolution(int units) {
        return convertFromDphi (crossFeedResolution, units);
    }

    /**
     * Returns this resolution attribute's feed direction resolution in the
     * given units. The value is rounded to the nearest integer.
     *
     * @param  units unit conversion factor, e.g. {@link #DPI DPI} or
     *         {@link #DPCM DPCM}
     * @return feed direction resolution
     * @throws IllegalArgumentException if {@code units < 1}
     */
    public int getFeedResolution(int units) {
        return convertFromDphi (feedResolution, units);
    }

    /**
     * Returns a string version of this resolution attribute in the given units.
     * The string takes the form <code>"<i>C</i>x<i>F</i> <i>U</i>"</code>,
     * where <i>C</i> is the cross feed direction resolution, <i>F</i> is the
     * feed direction resolution, and <i>U</i> is the units name. The values are
     * rounded to the nearest integer.
     *
     * @param  units unit conversion factor, e.g. {@link #DPI CODE>DPI} or
     *         {@link #DPCM DPCM}
     * @param  unitsName units name string, e.g. {@code "dpi"} or
     *         {@code "dpcm"}. If {@code null}, no units name is appended to the
     *         result.
     * @return string version of this resolution attribute
     * @throws IllegalArgumentException if {@code units < 1}
     */
    public String toString(int units, String unitsName) {
        StringBuilder result = new StringBuilder();
        result.append(getCrossFeedResolution (units));
        result.append('x');
        result.append(getFeedResolution (units));
        if (unitsName != null) {
            result.append (' ');
            result.append (unitsName);
        }
        return result.toString();
    }

    /**
     * Determine whether this resolution attribute's value is less than or equal
     * to the given resolution attribute's value. This is true if all of the
     * following conditions are true:
     * <ul>
     *   <li>This attribute's cross feed direction resolution is less than or
     *   equal to the {@code other} attribute's cross feed direction resolution.
     *   <li>This attribute's feed direction resolution is less than or equal to
     *   the {@code other} attribute's feed direction resolution.
     * </ul>
     *
     * @param  other resolution attribute to compare with
     * @return {@code true} if this resolution attribute is less than or equal
     *         to the {@code other} resolution attribute, {@code false}
     *         otherwise
     * @throws NullPointerException if {@code other} is {@code null}
     */
    public boolean lessThanOrEquals(ResolutionSyntax other) {
        return (this.crossFeedResolution <= other.crossFeedResolution &&
                this.feedResolution <= other.feedResolution);
    }

    /**
     * Returns whether this resolution attribute is equivalent to the passed in
     * object. To be equivalent, all of the following conditions must be true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class {@code ResolutionSyntax}.
     *   <li>This attribute's cross feed direction resolution is equal to
     *   {@code object}'s cross feed direction resolution.
     *   <li>This attribute's feed direction resolution is equal to
     *   {@code object}'s feed direction resolution.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this resolution
     *         attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {

        return(object != null &&
               object instanceof ResolutionSyntax &&
               this.crossFeedResolution ==
               ((ResolutionSyntax) object).crossFeedResolution &&
               this.feedResolution ==
               ((ResolutionSyntax) object).feedResolution);
    }

    /**
     * Returns a hash code value for this resolution attribute.
     */
    public int hashCode() {
        return(((crossFeedResolution & 0x0000FFFF)) |
               ((feedResolution      & 0x0000FFFF) << 16));
    }

    /**
     * Returns a string version of this resolution attribute. The string takes
     * the form <code>"<i>C</i>x<i>F</i> dphi"</code>, where <i>C</i> is the
     * cross feed direction resolution and <i>F</i> is the feed direction
     * resolution. The values are reported in the internal units of dphi.
     */
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(crossFeedResolution);
        result.append('x');
        result.append(feedResolution);
        result.append(" dphi");
        return result.toString();
    }

    /**
     * Returns this resolution attribute's cross feed direction resolution in
     * units of dphi. (For use in a subclass.)
     *
     * @return cross feed direction resolution
     */
    protected int getCrossFeedResolutionDphi() {
        return crossFeedResolution;
    }

    /**
     * Returns this resolution attribute's feed direction resolution in units of
     * dphi. (For use in a subclass.)
     *
     * @return feed direction resolution
     */
    protected int getFeedResolutionDphi() {
        return feedResolution;
    }
}
