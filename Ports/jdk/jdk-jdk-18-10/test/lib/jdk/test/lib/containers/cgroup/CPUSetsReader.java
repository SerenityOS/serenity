/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.containers.cgroup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jdk.test.lib.Asserts;


// A simple CPU sets reader and parser
public class CPUSetsReader {
    public static String PROC_SELF_STATUS_PATH = "/proc/self/status";

    // Test the parser
    public static void test() {
        assertParse("0-7", "0,1,2,3,4,5,6,7");
        assertParse("1,3,6", "1,3,6");
        assertParse("0,2-4,6,10-11", "0,2,3,4,6,10,11");
        assertParse("0", "0");
    }


    private static void assertParse(String cpuSet, String expectedResult) {
        Asserts.assertEquals(listToString(parseCpuSet(cpuSet)), expectedResult);
    }

    public static int getNumCpus() {
        String path = "/proc/cpuinfo";
        try (Stream<String> stream = Files.lines(Paths.get(path))) {
            return (int) stream.filter(line -> line.startsWith("processor")).count();
        } catch (IOException e) {
            return 0;
        }
    }


    public static String readFromProcStatus(String setType) {
        String path = PROC_SELF_STATUS_PATH;
        Optional<String> o = Optional.empty();

        System.out.println("readFromProcStatus() entering for: " + setType);

        try (Stream<String> stream = Files.lines(Paths.get(path))) {
            o = stream
                    .filter(line -> line.contains(setType))
                    .findFirst();
        } catch (IOException e) {
            return null;
        }

        if (!o.isPresent()) {
            return null;    // entry not found
        }

        String[] parts = o.get().replaceAll("\\s", "").split(":");

        // Should be 2 parts, before and after ":"
        Asserts.assertEquals(parts.length, 2);

        String result = parts[1];
        System.out.println("readFromProcStatus() returning: " + result);
        return result;
    }


    public static List<Integer> parseCpuSet(String value) {
        ArrayList<Integer> result = new ArrayList<Integer>();

        try {
            String[] commaSeparated = value.split(",");

            for (String item : commaSeparated) {
                if (item.contains("-")) {
                    addRange(result, item);
                } else {
                    result.add(Integer.parseInt(item));
                }
            }
        } catch (Exception e) {
            System.err.println("Exception in getMaxCpuSets(): " + e);
            return null;
        }

        return result;
    }

    private static void addRange(ArrayList<Integer> list, String s) {
        String[] range = s.split("-");
        if (range.length != 2) {
            throw new RuntimeException("Range should only contain two items, but contains "
                    + range.length + " items");
        }

        int min = Integer.parseInt(range[0]);
        int max = Integer.parseInt(range[1]);

        if (min >= max) {
            String msg = String.format("min is greater or equals to max, min = %d, max = %d",
                    min, max);
            throw new RuntimeException(msg);
        }

        for (int i = min; i <= max; i++) {
            list.add(i);
        }
    }


    // Convert list of integers to string with comma-separated values
    public static String listToString(List<Integer> list) {
        return listToString(list, Integer.MAX_VALUE);
    }

    // Convert list of integers to a string with comma-separated values;
    // include up to maxCount.
    public static String listToString(List<Integer> list, int maxCount) {
        return list.stream()
                .limit(maxCount)
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    public static String numberToString(int num) {
        return IntStream.range(0, num).boxed().map(Object::toString).collect(Collectors.joining(","));
    }
}
