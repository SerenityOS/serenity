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

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;

/**
 * Class {@code MediaPrintableArea} is a printing attribute used to distinguish
 * the printable and non-printable areas of media.
 * <p>
 * The printable area is specified to be a rectangle, within the overall
 * dimensions of a media.
 * <p>
 * Most printers cannot print on the entire surface of the media, due to printer
 * hardware limitations. This class can be used to query the acceptable values
 * for a supposed print job, and to request an area within the constraints of
 * the printable area to be used in a print job.
 * <p>
 * To query for the printable area, a client must supply a suitable context.
 * Without specifying at the very least the size of the media being used no
 * meaningful value for printable area can be obtained.
 * <p>
 * The attribute is not described in terms of the distance from the edge of the
 * paper, in part to emphasise that this attribute is not independent of a
 * particular media, but must be described within the context of a choice of
 * other attributes. Additionally it is usually more convenient for a client to
 * use the printable area.
 * <p>
 * The hardware's minimum margins is not just a property of the printer, but may
 * be a function of the media size, orientation, media type, and any specified
 * finishings. {@code PrintService} provides the method to query the supported
 * values of an attribute in a suitable context : See
 * {@link PrintService#getSupportedAttributeValues(Class, DocFlavor, AttributeSet)
 * PrintService.getSupportedAttributeValues()}
 * <p>
 * The rectangular printable area is defined thus: The (x,y) origin is
 * positioned at the top-left of the paper in portrait mode regardless of the
 * orientation specified in the requesting context. For example a printable area
 * for A4 paper in portrait or landscape orientation will have height
 * {@literal >} width.
 * <p>
 * A printable area attribute's values are stored internally as integers in
 * units of micrometers (&#181;m), where 1 micrometer = 10<SUP>-6</SUP> meter =
 * 1/1000 millimeter = 1/25400 inch. This permits dimensions to be represented
 * exactly to a precision of 1/1000 mm (= 1 &#181;m) or 1/100 inch (= 254
 * &#181;m). If fractional inches are expressed in negative powers of two, this
 * permits dimensions to be represented exactly to a precision of 1/8 inch
 * (= 3175 &#181;m) but not 1/16 inch (because 1/16 inch does not equal an
 * integral number of &#181;m).
 * <p>
 * <b>IPP Compatibility:</b> MediaPrintableArea is not an IPP attribute.
 */
public final class MediaPrintableArea
      implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {

    /**
     * Printable {@code x}, {@code y}, {@code width} and {@code height}.
     */
    private int x, y, w, h;

    /**
     * The units in which the values are expressed.
     */
    private int units;

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -1597171464050795793L;

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
     * Constructs a {@code MediaPrintableArea} object from floating point
     * values.
     *
     * @param  x printable x
     * @param  y printable y
     * @param  w printable width
     * @param  h printable height
     * @param  units in which the values are expressed
     * @throws IllegalArgumentException if {@code x < 0} or {@code y < 0} or
     *         {@code w <= 0} or {@code h <= 0} or {@code units < 1}
     */
    public MediaPrintableArea(float x, float y, float w, float h, int units) {
        if ((x < 0.0) || (y < 0.0) || (w <= 0.0) || (h <= 0.0) ||
            (units < 1)) {
            throw new IllegalArgumentException("0 or negative value argument");
        }

        this.x = (int) (x * units + 0.5f);
        this.y = (int) (y * units + 0.5f);
        this.w = (int) (w * units + 0.5f);
        this.h = (int) (h * units + 0.5f);

    }

    /**
     * Constructs a {@code MediaPrintableArea} object from integer values.
     *
     * @param  x printable x
     * @param  y printable y
     * @param  w printable width
     * @param  h printable height
     * @param  units in which the values are expressed
     * @throws IllegalArgumentException if {@code x < 0} or {@code y < 0} or
     *         {@code w <= 0} or {@code h <= 0} or {@code units < 1}
     */
    public MediaPrintableArea(int x, int y, int w, int h, int units) {
        if ((x < 0) || (y < 0) || (w <= 0) || (h <= 0) ||
            (units < 1)) {
            throw new IllegalArgumentException("0 or negative value argument");
        }
        this.x = x * units;
        this.y = y * units;
        this.w = w * units;
        this.h = h * units;

    }

    /**
     * Get the printable area as an array of 4 values in the order
     * {@code x, y, w, h}. The values returned are in the given units.
     *
     * @param  units unit conversion factor, e.g. {@link #INCH INCH} or
     *         {@link #MM MM}
     * @return printable area as array of {@code x, y, w, h} in the specified
     *         units
     * @throws IllegalArgumentException if {@code units < 1}
     */
    public float[] getPrintableArea(int units) {
        return new float[] { getX(units), getY(units),
                             getWidth(units), getHeight(units) };
    }

    /**
     * Get the {@code x} location of the origin of the printable area in the
     * specified units.
     *
     * @param  units unit conversion factor, e.g. {@link #INCH INCH} or
     *         {@link #MM MM}
     * @return {@code x} location of the origin of the printable area in the
     *         specified units
     * @throws IllegalArgumentException if {@code units < 1}
     */
    public float getX(int units) {
        return convertFromMicrometers(x, units);
    }

    /**
     * Get the {@code y} location of the origin of the printable area in the
     * specified units.
     *
     * @param  units unit conversion factor, e.g. {@link #INCH INCH} or
     *         {@link #MM MM}
     * @return {@code y} location of the origin of the printable area in the
     *         specified units
     * @throws IllegalArgumentException if {@code units < 1}
     */
    public float getY(int units) {
        return convertFromMicrometers(y, units);
    }

    /**
     * Get the {@code width} of the printable area in the specified units.
     *
     * @param  units unit conversion factor, e.g. {@link #INCH INCH} or
     *         {@link #MM MM}
     * @return {@code width} of the printable area in the specified units
     * @throws IllegalArgumentException if {@code units < 1}
     */
    public float getWidth(int units) {
        return convertFromMicrometers(w, units);
    }

    /**
     * Get the {@code height} of the printable area in the specified units.
     *
     * @param  units unit conversion factor, e.g. {@link #INCH INCH} or
     *        {@link #MM MM}
     * @return {@code height} of the printable area in the specified units
     * @throws IllegalArgumentException if {@code units < 1}
     */
    public float getHeight(int units) {
        return convertFromMicrometers(h, units);
    }

    /**
     * Returns whether this media margins attribute is equivalent to the passed
     * in object. To be equivalent, all of the following conditions must be
     * true:
     * <ol type=1>
     *   <li>{@code object} is not {@code null}.
     *   <li>{@code object} is an instance of class {@code MediaPrintableArea}.
     *   <li>The origin and dimensions are the same.
     * </ol>
     *
     * @param  object {@code Object} to compare to
     * @return {@code true} if {@code object} is equivalent to this media
     *         margins attribute, {@code false} otherwise
     */
    public boolean equals(Object object) {
        boolean ret = false;
        if (object instanceof MediaPrintableArea) {
           MediaPrintableArea mm = (MediaPrintableArea)object;
           if (x == mm.x &&  y == mm.y && w == mm.w && h == mm.h) {
               ret = true;
           }
        }
        return ret;
    }

    /**
     * Get the printing attribute class which is to be used as the "category"
     * for this printing attribute value.
     * <p>
     * For class {@code MediaPrintableArea}, the category is class
     * {@code MediaPrintableArea} itself.
     *
     * @return printing attribute class (category), an instance of class
     *         {@link Class java.lang.Class}
     */
    public final Class<? extends Attribute> getCategory() {
        return MediaPrintableArea.class;
    }

    /**
     * Get the name of the category of which this attribute value is an
     * instance.
     * <p>
     * For class {@code MediaPrintableArea}, the category name is
     * {@code "media-printable-area"}.
     * <p>
     * This is not an IPP V1.1 attribute.
     *
     * @return attribute category name
     */
    public final String getName() {
        return "media-printable-area";
    }

    /**
     * Returns a string version of this rectangular size attribute in the given
     * units.
     *
     * @param  units unit conversion factor, e.g. {@link #INCH INCH} or
     *         {@link #MM MM}
     * @param  unitsName units name string, e.g. {@code "in"} or {@code "mm"}.
     *         If {@code null}, no units name is appended to the result
     * @return string version of this two-dimensional size attribute
     * @throws IllegalArgumentException if {@code units < 1}
     */
    public String toString(int units, String unitsName) {
        if (unitsName == null) {
            unitsName = "";
        }
        float []vals = getPrintableArea(units);
        String str = "("+vals[0]+","+vals[1]+")->("+vals[2]+","+vals[3]+")";
        return str + unitsName;
    }

    /**
     * Returns a string version of this rectangular size attribute in mm.
     */
    public String toString() {
        return(toString(MM, "mm"));
    }

    /**
     * Returns a hash code value for this attribute.
     */
    public int hashCode() {
        return x + 37*y + 43*w + 47*h;
    }

    /**
     * Converts the {@code x} from micrometers to {@code units}.
     *
     * @param  x the value
     * @param  units unit conversion factor, e.g. {@link #INCH INCH} or
     *        {@link #MM MM}
     * @return the value of {@code x} in the specified units
     */
    private static float convertFromMicrometers(int x, int units) {
        if (units < 1) {
            throw new IllegalArgumentException("units is < 1");
        }
        return ((float)x) / ((float)units);
    }
}
