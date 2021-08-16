/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package examples;

/**
 * Two-dimensional coordinates that can be represented
 * in either Cartesian or polar form.
 */
public interface Coords {

    /**
     * Returns the Cartesian x-coordinate.
     *
     * @return the Cartesian x-coordinate
     */
    public double x();

    /**
     * Returns the Cartesian y-coordinate.
     *
     * @return the Cartesian y-coordinate
     */
    public double y();

    /**
     * Returns the polar r-coordinate, or radius.
     *
     * @return the polar r-coordinate
     */
    public double r();

    /**
     * Returns the polar theta-coordinate, or angle.
     *
     * @return the polar theta-coordinate
     */
    public double theta();

    /**
      * Cartesian coordinates.
      *
      * @param x the x-coordinate
      * @param y the y-coordinate
      */
    public record Cartesian(double x, double y) implements Coords {
        @Override
        public double r() {
            return Math.sqrt(x * x + y * y);
        }

        @Override
        public double theta() {
            return (x == 0) ? Math.PI / 2.d : Math.atan(y / x);
        }
    }

    /**
      * Polar coordinates.
      *
      * @param r     the radius
      * @param theta the angle
      */
    public record Polar(double r, double theta) implements Coords {
        @Override
        public double x() {
          return r * Math.cos(theta);
        }

        @Override
        public double y() {
          return r * Math.sin(theta);
        }
    }
}
