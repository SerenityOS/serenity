/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jstat;

import java.util.*;

/**
 * A typesafe enumeration for describing data scaling semantics
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class Scale {
    private static int nextOrdinal = 0;
    private static HashMap<String, Scale> map = new HashMap<String, Scale>();

    private final String name;
    private final int ordinal = nextOrdinal++;
    private final double factor;

    private Scale(String name, double factor) {
        this.name = name;
        this.factor = factor;
        assert !map.containsKey(name);
        map.put(name, this);
    }

    /**
     * Scale representing a no scaling
     */
    public static final Scale RAW = new Scale("raw", 1);

    /**
     * Scale representing a percent scaling
     */
    public static final Scale PERCENT = new Scale("percent", 1/100);

    /**
     * Scale representing a kilo scaling
     */
    public static final Scale KILO = new Scale("K", 1024);

    /**
     * Scale representing a mega scaling
     */
    public static final Scale MEGA = new Scale("M", 1024*1024);

    /**
     * Scale representing a giga scaling
     */
    public static final Scale GIGA = new Scale("G", 1024*1024*1024);

    /**
     * Scale representing a tera scaling
     */
    public static final Scale TERA = new Scale("T", 1024*1024*1024*1024);

    /**
     * Scale representing a tera scaling
     */
    public static final Scale PETA = new Scale("P", 1024*1024*1024*1024*1024);

    /**
     * Scale representing a pico scaling
     */
    public static final Scale PICO = new Scale("p", 10.0E-12);

    /**
     * Scale representing a nano scaling
     */
    public static final Scale NANO = new Scale("n", 10.0E-9);

    /**
     * Scale representing a micro scaling
     */
    public static final Scale MICRO = new Scale("u", 10.0E-6);

    /**
     * Scale representing a milli scaling
     */
    public static final Scale MILLI = new Scale("m", 10.0E-3);

    /**
     * Scale representing a picosecond scaling
     */
    public static final Scale PSEC = new Scale("ps", 10.0E-12);

    /**
     * Scale representing a nanosecond scaling
     */
    public static final Scale NSEC = new Scale("ns", 10.0E-9);

    /**
     * Scale representing a microsecond scaling
     */
    public static final Scale USEC = new Scale("us", 10.0E-6);

    /**
     * Scale representing a millisecond scaling
     */
    public static final Scale MSEC = new Scale("ms", 10.0E-3);

    /**
     * Scale representing a second scaling
     */
    public static final Scale SEC = new Scale("s", 1);
    public static final Scale SEC2 = new Scale("sec", 1);

    /**
     * Scale representing a minutes scaling
     */
    public static final Scale MINUTES = new Scale("min", 1/60.0);

    /**
     * Scale representing a hours scaling
     */
    public static final Scale HOUR = new Scale("h", 1/(60.0*60.0));
    public static final Scale HOUR2 = new Scale("hour", 1/(60.0*60.0));

    /**
     * Returns the scaling factor of this Scale object
     *
     * @return  the scaling factor of this Scale object
     */
    public double getFactor() {
        return factor;
    }

    /**
     * Returns the string representation of this Scale object.
     * The string representation is the name of the Scale object.
     *
     * @return  the string representation of this Scale object
     */
    public String toString() {
        return name;
    }

    /**
     * Maps a string to its corresponding Scale object.
     *
     * @param   s  a string to match against Scale objects.
     * @return     The Scale object matching the given string.
     */
    public static Scale toScale(String s) {
        return map.get(s);
    }

    /**
     * Returns an enumeration of the keys for this enumerated type
     *
     * @param   s  an string to match against Scale objects.
     * @return     The Scale object matching the given string.
     */
    protected static Set<String> keySet() {
        return map.keySet();
    }

    protected double scale(double value) {
        return value/factor;
    }
}
