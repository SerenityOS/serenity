/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8068399 8069545
 * @summary structural most specific and stuckness
 */

import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class T8068399 {

    public static class Spectrum {
        public double[] getEnergy() {
            return new double[0];
        }
    }

    protected Spectrum spectrum;

    public static class Ref<T> {

        T value;

        public Ref() {
        }

        public Ref(T value) {
            this.value = value;
        }

        public boolean isNull() {
            return value == null;
        }

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }
    }

    public static <T>T maxKey(Stream<T> stream, Function<T, Double> function) {
        Ref<Double> max = new Ref<>();
        Ref<T> index = new Ref<>();
        stream.forEach(v -> {
            Double value = function.apply(v);

            if (max.isNull() || value > max.get()) {
                max.set(value);
                index.set(v);
            }
        });

        return index.get();
    }

    public static int interpolate(int x, int x0, int x1, int y0, int y1) {
        return y0 + (x - x0) * (y1 - y0) / (x1 - x0);
    }

    public static double interpolate(double x, double x0, double x1, double y0, double y1) {
        return y0 + (x - x0) * (y1 - y0) / (x1 - x0);
    }

    protected int getXByFrequency(double frequency) {
        return (int) Math.round(interpolate(frequency,
                                            getMinSpectrumCoord(),
                                            getMaxSpectrumCoord(),
                                            0, getWidth()));
    }

    private int getWidth() {
        return 0;
    }

    private double getMaxSpectrumCoord() {
        return 0;
    }

    private double getMinSpectrumCoord() {
        return 0;
    }

    void foo() {
        int maxBpmIndex = 0;
        int xcur = getXByFrequency(maxKey(IntStream.range(0, maxBpmIndex).boxed(),
                                          i -> Math.abs(spectrum.getEnergy()[i])));
    }

    public static void main(String [] args) {
    }
}
