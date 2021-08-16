/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package bench;

/**
 * Information about a benchmark: its name, how long it took to run, and the
 * weight associated with it (for calculating the overall score).
 */
public class BenchInfo {

    Benchmark benchmark;
    String name;
    long time;
    float weight;
    String[] args;

    /**
     * Construct benchmark info.
     */
    BenchInfo(Benchmark benchmark, String name, float weight, String[] args) {
        this.benchmark = benchmark;
        this.name = name;
        this.weight = weight;
        this.args = args;
        this.time = -1;
    }

    /**
     * Run benchmark with specified args.  Called only by the harness.
     */
    void runBenchmark() throws Exception {
        time = benchmark.run(args);
    }

    /**
     * Return the benchmark for this benchmark info.
     */
    public Benchmark getBenchmark() {
        return benchmark;
    }

    /**
     * Return the name of this benchmark.
     */
    public String getName() {
        return name;
    }

    /**
     * Return the execution time for benchmark, or -1 if benchmark hasn't been
     * run to completion.
     */
    public long getTime() {
        return time;
    }

    /**
     * Return weight associated with benchmark.
     */
    public float getWeight() {
        return weight;
    }
}
