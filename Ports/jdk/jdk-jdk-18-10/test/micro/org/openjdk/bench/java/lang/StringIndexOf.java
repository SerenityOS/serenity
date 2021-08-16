/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class StringIndexOf {

    private String dataString;
    private String searchString;
    private String dataStringBig;
    private String searchStringBig;
    private String data;
    private String sub;
    private String shortSub1;
    private String data2;
    private String shortSub2;
    private String string16Short;
    private String string16Medium;
    private String string16Long;
    private char searchChar;
    private char searchChar16;
    private String searchString16;

    @Setup
    public void setup() {
        dataString = "ngdfilsoscargfdgf";
        searchString = "oscar";
        dataStringBig = "2937489745890797905764956790452976742965790437698498409583479067ngdcapaapapapasdkajdlkajskldjaslkjdlkasjdsalkjas";
        searchStringBig = "capaapapapasdkajdlkajskldjaslkjdlkasjdsalk";
        data = "0000100101010010110101010010101110101001110110101010010101010010000010111010101010101010100010010101110111010101101010100010010100001010111111100001010101001010100001010101001010101010111010010101010101010101010101010";
        sub = "10101010";
        shortSub1 = "1";
        data2 = "00001001010100a10110101010010101110101001110110101010010101010010000010111010101010101010a100010010101110111010101101010100010010a100a0010101111111000010101010010101000010101010010101010101110a10010101010101010101010101010";
        shortSub2 = "a";
        searchChar = 's';

        string16Short = "scar\u01fe1";
        string16Medium = "capaapapapasdkajdlkajskldjaslkjdlkasjdsalksca1r\u01fescar";
        string16Long = "2937489745890797905764956790452976742965790437698498409583479067ngdcapaapapapasdkajdlkajskldjaslkjdlkasjdsalkja1sscar\u01fescar";
        searchChar16 = 0x1fe;
        searchString16 = "\u01fe";
    }


    /** IndexOf Micros Chars */
    @Benchmark
    public int searchCharLongSuccess() {
        return dataStringBig.indexOf(searchChar);
    }

    @Benchmark
    public int searchCharMediumSuccess() {
        return searchStringBig.indexOf(searchChar);
    }

    @Benchmark
    public int searchCharShortSuccess() {
        return searchString.indexOf(searchChar);
    }

    @Benchmark
    public int searchChar16LongSuccess() {
        return string16Long.indexOf(searchChar16);
    }

    @Benchmark
    public int searchChar16MediumSuccess() {
        return string16Medium.indexOf(searchChar16);
    }

    @Benchmark
    public int searchChar16ShortSuccess() {
        return string16Short.indexOf(searchChar16);
    }

    @Benchmark
    public int searchChar16LongWithOffsetSuccess() {
        return string16Long.indexOf(searchChar16, 3);
    }

    @Benchmark
    public int searchChar16MediumWithOffsetSuccess() {
        return string16Medium.indexOf(searchChar16, 3);
    }

    @Benchmark
    public int searchChar16ShortWithOffsetSuccess() {
        return string16Short.indexOf(searchChar16, 2);
    }

    /** IndexOf Micros Strings */
    @Benchmark
    public int searchString16LongLatinSuccess() {
        return string16Long.indexOf(shortSub1);
    }

    @Benchmark
    public int searchString16MediumLatinSuccess() {
        return string16Medium.indexOf(shortSub1);
    }

    @Benchmark
    public int searchString16ShortLatinSuccess() {
        return string16Long.indexOf(shortSub2);
    }

    @Benchmark
    public int searchString16LongWithOffsetLatinSuccess() {
        return string16Long.indexOf(shortSub1, 3);
    }

    @Benchmark
    public int searchString16MediumWithOffsetLatinSuccess() {
        return string16Medium.indexOf(shortSub1, 3);
    }

    @Benchmark
    public int searchString16ShortWithOffsetLatinSuccess() {
        return string16Short.indexOf(shortSub2, 1);
    }

    @Benchmark
    public int searchString16LongWithOffsetSuccess() {
        return string16Long.indexOf(searchString16, 3);
    }

    @Benchmark
    public int searchString16MediumWithOffsetSuccess() {
        return string16Medium.indexOf(searchString16, 3);
    }

    @Benchmark
    public int searchString16ShortWithOffsetSuccess() {
        return string16Short.indexOf(searchString16, 2);
    }

    @Benchmark
    public int searchString16LongSuccess() {
        return string16Long.indexOf(searchString16);
    }

    @Benchmark
    public int searchString16MediumSuccess() {
        return string16Medium.indexOf(searchString16);
    }

    @Benchmark
    public int searchString16ShortSuccess() {
        return string16Short.indexOf(searchString16);
    }

    /**
     * Benchmarks String.indexOf with a rather small String to search and a rather small String to search for. The
     * searched string contains the string that is searched for.
     */
    @Benchmark
    public int success() {
        return dataString.indexOf(searchString, 2);
    }

    /**
     * Benchmarks String.indexOf with a rather big String to search and a rather big String to search for. The searched
     * string contains the string that is searched for.
     */
    @Benchmark
    public int successBig() {
        return dataStringBig.indexOf(searchStringBig, 2);
    }

    /**
     * Benchmarks String.indexOf with a rather big String. Search repeatedly for a matched that is 8 chars and most
     * oftenly will require a inner lopp match in String.indexOf with sse42.
     */
    @Benchmark
    public int advancedWithMediumSub() {
        int index = 0;
        int dummy = 0;
        while ((index = data.indexOf(sub, index)) > -1) {
            index++;
            dummy += index;
        }
        return dummy;
    }


    /**
     * Benchmarks String.indexOf with a rather big String. Search repeatedly for a matched that is 1 chars will find a
     * huge amount of matches
     */
    @Benchmark
    public int advancedWithShortSub1() {
        int dummy = 0;
        int index = 0;
        while ((index = data.indexOf(shortSub1, index)) > -1) {
            index++;
            dummy += index;
        }
        return dummy;
    }


    /**
     * Benchmarks String.indexOf with a rather big String. Search repeatedly for a matched that is 1 chars but only with
     * a few matches.
     */
    @Benchmark
    public int advancedWithShortSub2() {
        int dummy = 0;
        int index = 0;
        while ((index = data2.indexOf(shortSub2, index)) > -1) {
            index++;
            dummy += index;
        }
        return dummy;
    }

    @Benchmark
    public void constantPattern() {
        String tmp = "simple-hash:SHA-1/UTF-8";
        if (!tmp.contains("SHA-1")) {
            throw new RuntimeException("indexOf failed");
        }
    }

}
