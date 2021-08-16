/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4347132 8136799
 * @key randomness
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @build Tests
 * @build FdlibmTranslit
 * @build CubeRootTests
 * @run main CubeRootTests
 * @summary Tests specifically for StrictMath.cbrt
 * @author Joseph D. Darcy
 */

import jdk.test.lib.RandomFactory;

/**
 * The tests in ../Math/CubeRootTests.java test properties that should
 * hold for any cube root implementation, including the FDLIBM-based
 * one required for StrictMath.cbrt.  Therefore, the test cases in
 * ../Math/CubeRootTests.java are run against both the Math and
 * StrictMath versions of cube root.  The role of this test is to
 * verify that the FDLIBM cbrt algorithm is being used by running
 * golden file tests on values that may vary from one conforming cube
 * root implementation to another.
 */

public class CubeRootTests {
    private CubeRootTests(){}

    public static void main(String [] argv) {
        int failures = 0;

        failures += testCubeRoot();
        failures += testAgainstTranslit();

        if (failures > 0) {
            System.err.println("Testing the cube root incurred "
                               + failures + " failures.");
            throw new RuntimeException();
        }
    }

    static int testCubeRootCase(double input, double expected) {
        int failures=0;

        double minus_input = -input;
        double minus_expected = -expected;

        failures+=Tests.test("StrictMath.cbrt(double)", input,
                             StrictMath.cbrt(input), expected);
        failures+=Tests.test("StrictMath.cbrt(double)", minus_input,
                             StrictMath.cbrt(minus_input), minus_expected);
        return failures;
    }

    static int testCubeRoot() {
        int failures = 0;
        double [][] testCases = {
            {0x1.ffffffffffffep-766,    0x1.fffffffffffffp-256},
            {0x1.ffffffffffffep-763,    0x1.fffffffffffffp-255},
            {0x1.ffffffffffffep-760,    0x1.fffffffffffffp-254},
            {0x1.ffffffffffffep-757,    0x1.fffffffffffffp-253},
            {0x1.ffffffffffffep-754,    0x1.fffffffffffffp-252},
            {0x1.ffffffffffffep-751,    0x1.fffffffffffffp-251},
            {0x1.ffffffffffffep-748,    0x1.fffffffffffffp-250},
            {0x1.ffffffffffffep-745,    0x1.fffffffffffffp-249},
            {0x1.ffffffffffffep-742,    0x1.fffffffffffffp-248},
            {0x1.ffffffffffffep-739,    0x1.fffffffffffffp-247},
            {0x1.ffffffffffffep-1006,   0x1.fffffffffffffp-336},
            {0x1.ffffffffffffep-736,    0x1.fffffffffffffp-246},
            {0x1.ffffffffffffep-733,    0x1.fffffffffffffp-245},
            {0x1.ffffffffffffep-730,    0x1.fffffffffffffp-244},
            {0x1.ffffffffffffep-727,    0x1.fffffffffffffp-243},
            {0x1.ffffffffffffep-724,    0x1.fffffffffffffp-242},
            {0x1.ffffffffffffep-721,    0x1.fffffffffffffp-241},
            {0x1.ffffffffffffep-718,    0x1.fffffffffffffp-240},
            {0x1.ffffffffffffep-715,    0x1.fffffffffffffp-239},
            {0x1.ffffffffffffep-712,    0x1.fffffffffffffp-238},
            {0x1.ffffffffffffep-709,    0x1.fffffffffffffp-237},
            {0x1.ffffffffffffep-706,    0x1.fffffffffffffp-236},
            {0x1.ffffffffffffep-703,    0x1.fffffffffffffp-235},
            {0x1.ffffffffffffep-700,    0x1.fffffffffffffp-234},
            {0x1.ffffffffffffep-697,    0x1.fffffffffffffp-233},
            {0x1.ffffffffffffep-694,    0x1.fffffffffffffp-232},
            {0x1.ffffffffffffep-691,    0x1.fffffffffffffp-231},
            {0x1.ffffffffffffep-1003,   0x1.fffffffffffffp-335},
            {0x1.ffffffffffffep-688,    0x1.fffffffffffffp-230},
            {0x1.ffffffffffffep-685,    0x1.fffffffffffffp-229},
            {0x1.ffffffffffffep-682,    0x1.fffffffffffffp-228},
            {0x1.ffffffffffffep-679,    0x1.fffffffffffffp-227},
            {0x1.ffffffffffffep-676,    0x1.fffffffffffffp-226},
            {0x1.ffffffffffffep-673,    0x1.fffffffffffffp-225},
            {0x1.ffffffffffffep-670,    0x1.fffffffffffffp-224},
            {0x1.ffffffffffffep-667,    0x1.fffffffffffffp-223},
            {0x1.ffffffffffffep-664,    0x1.fffffffffffffp-222},
            {0x1.ffffffffffffep-661,    0x1.fffffffffffffp-221},
            {0x1.ffffffffffffep-658,    0x1.fffffffffffffp-220},
            {0x1.ffffffffffffep-655,    0x1.fffffffffffffp-219},
            {0x1.ffffffffffffep-652,    0x1.fffffffffffffp-218},
            {0x1.ffffffffffffep-649,    0x1.fffffffffffffp-217},
            {0x1.ffffffffffffep-646,    0x1.fffffffffffffp-216},
            {0x1.ffffffffffffep-643,    0x1.fffffffffffffp-215},
            {0x1.ffffffffffffep-1000,   0x1.fffffffffffffp-334},
            {0x1.ffffffffffffep-640,    0x1.fffffffffffffp-214},
            {0x1.ffffffffffffep-637,    0x1.fffffffffffffp-213},
            {0x1.ffffffffffffep-634,    0x1.fffffffffffffp-212},
            {0x1.ffffffffffffep-631,    0x1.fffffffffffffp-211},
            {0x1.ffffffffffffep-628,    0x1.fffffffffffffp-210},
            {0x1.ffffffffffffep-625,    0x1.fffffffffffffp-209},
            {0x1.ffffffffffffep-622,    0x1.fffffffffffffp-208},
            {0x1.ffffffffffffep-619,    0x1.fffffffffffffp-207},
            {0x1.ffffffffffffep-616,    0x1.fffffffffffffp-206},
            {0x1.ffffffffffffep-613,    0x1.fffffffffffffp-205},
            {0x1.ffffffffffffep-610,    0x1.fffffffffffffp-204},
            {0x1.ffffffffffffep-607,    0x1.fffffffffffffp-203},
            {0x1.ffffffffffffep-604,    0x1.fffffffffffffp-202},
            {0x1.ffffffffffffep-601,    0x1.fffffffffffffp-201},
            {0x1.ffffffffffffep-598,    0x1.fffffffffffffp-200},
            {0x1.ffffffffffffep-595,    0x1.fffffffffffffp-199},
            {0x1.ffffffffffffep-997,    0x1.fffffffffffffp-333},
            {0x1.ffffffffffffep-592,    0x1.fffffffffffffp-198},
            {0x1.ffffffffffffep-589,    0x1.fffffffffffffp-197},
            {0x1.ffffffffffffep-586,    0x1.fffffffffffffp-196},
            {0x1.ffffffffffffep-583,    0x1.fffffffffffffp-195},
            {0x1.ffffffffffffep-580,    0x1.fffffffffffffp-194},
            {0x1.ffffffffffffep-577,    0x1.fffffffffffffp-193},
            {0x1.ffffffffffffep-574,    0x1.fffffffffffffp-192},
            {0x1.ffffffffffffep-571,    0x1.fffffffffffffp-191},
            {0x1.ffffffffffffep-568,    0x1.fffffffffffffp-190},
            {0x1.ffffffffffffep-565,    0x1.fffffffffffffp-189},
            {0x1.ffffffffffffep-562,    0x1.fffffffffffffp-188},
            {0x1.ffffffffffffep-559,    0x1.fffffffffffffp-187},
            {0x1.ffffffffffffep-556,    0x1.fffffffffffffp-186},
            {0x1.ffffffffffffep-553,    0x1.fffffffffffffp-185},
            {0x1.ffffffffffffep-550,    0x1.fffffffffffffp-184},
            {0x1.ffffffffffffep-547,    0x1.fffffffffffffp-183},
            {0x1.ffffffffffffep-994,    0x1.fffffffffffffp-332},
            {0x1.ffffffffffffep-544,    0x1.fffffffffffffp-182},
            {0x1.ffffffffffffep-541,    0x1.fffffffffffffp-181},
            {0x1.ffffffffffffep-538,    0x1.fffffffffffffp-180},
            {0x1.ffffffffffffep-535,    0x1.fffffffffffffp-179},
            {0x1.ffffffffffffep-532,    0x1.fffffffffffffp-178},
            {0x1.ffffffffffffep-529,    0x1.fffffffffffffp-177},
            {0x0.00000000001fp-1022,    0x1.fa9c313858568p-356},
            {0x1.ffffffffffffep-526,    0x1.fffffffffffffp-176},
            {0x1.ffffffffffffep-523,    0x1.fffffffffffffp-175},
            {0x1.ffffffffffffep-520,    0x1.fffffffffffffp-174},
            {0x1.ffffffffffffep-517,    0x1.fffffffffffffp-173},
            {0x0.00000000001fdp-1022,   0x1.feff7f94ea34dp-356},
            {0x1.ffffffffffffep-514,    0x1.fffffffffffffp-172},
            {0x0.00000001fffe7p-1022,   0x1.ffff7aaa87f1bp-352},
            {0x0.00000001fffffp-1022,   0x1.fffffaaaaa9c7p-352},
            {0x0.00001ffffff4p-1022,    0x1.ffffffcp-348},
            {0x0.00001ffffffffp-1022,   0x1.ffffffffaaaabp-348},
            {0x0.01ffffffffffcp-1022,   0x1.ffffffffffeabp-344},
            {0x1.ffffffffffffep-511,    0x1.fffffffffffffp-171},
            {0x1.ffffffffffffep-508,    0x1.fffffffffffffp-170},
            {0x1.ffffffffffffep-505,    0x1.fffffffffffffp-169},
            {0x1.ffffffffffffep-502,    0x1.fffffffffffffp-168},
            {0x1.ffffffffffffep-499,    0x1.fffffffffffffp-167},
            {0x1.ffffffffffffep-991,    0x1.fffffffffffffp-331},
            {0x1.ffffffffffffep-496,    0x1.fffffffffffffp-166},
            {0x1.ffffffffffffep-493,    0x1.fffffffffffffp-165},
            {0x1.ffffffffffffep-490,    0x1.fffffffffffffp-164},
            {0x1.ffffffffffffep-487,    0x1.fffffffffffffp-163},
            {0x1.ffffffffffffep-484,    0x1.fffffffffffffp-162},
            {0x1.ffffffffffffep-481,    0x1.fffffffffffffp-161},
            {0x1.ffffffffffffep-478,    0x1.fffffffffffffp-160},
            {0x1.ffffffffffffep-475,    0x1.fffffffffffffp-159},
            {0x1.ffffffffffffep-472,    0x1.fffffffffffffp-158},
            {0x1.ffffffffffffep-469,    0x1.fffffffffffffp-157},
            {0x1.ffffffffffffep-466,    0x1.fffffffffffffp-156},
            {0x1.ffffffffffffep-463,    0x1.fffffffffffffp-155},
            {0x1.ffffffffffffep-460,    0x1.fffffffffffffp-154},
            {0x1.ffffffffffffep-457,    0x1.fffffffffffffp-153},
            {0x1.ffffffffffffep-454,    0x1.fffffffffffffp-152},
            {0x1.ffffffffffffep-451,    0x1.fffffffffffffp-151},
            {0x1.ffffffffffffep-988,    0x1.fffffffffffffp-330},
            {0x1.ffffffffffffep-448,    0x1.fffffffffffffp-150},
            {0x1.ffffffffffffep-445,    0x1.fffffffffffffp-149},
            {0x1.ffffffffffffep-442,    0x1.fffffffffffffp-148},
            {0x1.ffffffffffffep-439,    0x1.fffffffffffffp-147},
            {0x1.ffffffffffffep-436,    0x1.fffffffffffffp-146},
            {0x1.ffffffffffffep-433,    0x1.fffffffffffffp-145},
            {0x1.ffffffffffffep-430,    0x1.fffffffffffffp-144},
            {0x1.ffffffffffffep-427,    0x1.fffffffffffffp-143},
            {0x1.ffffffffffffep-424,    0x1.fffffffffffffp-142},
            {0x1.ffffffffffffep-421,    0x1.fffffffffffffp-141},
            {0x1.ffffffffffffep-418,    0x1.fffffffffffffp-140},
            {0x1.ffffffffffffep-415,    0x1.fffffffffffffp-139},
            {0x1.ffffffffffffep-412,    0x1.fffffffffffffp-138},
            {0x1.ffffffffffffep-409,    0x1.fffffffffffffp-137},
            {0x1.ffffffffffffep-406,    0x1.fffffffffffffp-136},
            {0x1.ffffffffffffep-403,    0x1.fffffffffffffp-135},
            {0x1.ffffffffffffep-985,    0x1.fffffffffffffp-329},
            {0x1.ffffffffffffep-400,    0x1.fffffffffffffp-134},
            {0x1.ffffffffffffep-397,    0x1.fffffffffffffp-133},
            {0x1.ffffffffffffep-394,    0x1.fffffffffffffp-132},
            {0x1.ffffffffffffep-391,    0x1.fffffffffffffp-131},
            {0x1.ffffffffffffep-388,    0x1.fffffffffffffp-130},
            {0x1.ffffffffffffep-385,    0x1.fffffffffffffp-129},
            {0x1.ffffffffffffep-382,    0x1.fffffffffffffp-128},
            {0x1.ffffffffffffep-379,    0x1.fffffffffffffp-127},
            {0x1.ffffffffffffep-376,    0x1.fffffffffffffp-126},
            {0x1.ffffffffffffep-373,    0x1.fffffffffffffp-125},
            {0x1.ffffffffffffep-370,    0x1.fffffffffffffp-124},
            {0x1.ffffffffffffep-367,    0x1.fffffffffffffp-123},
            {0x1.ffffffffffffep-364,    0x1.fffffffffffffp-122},
            {0x1.ffffffffffffep-361,    0x1.fffffffffffffp-121},
            {0x1.ffffffffffffep-358,    0x1.fffffffffffffp-120},
            {0x1.ffffffffffffep-355,    0x1.fffffffffffffp-119},
            {0x1.ffffffffffffep-982,    0x1.fffffffffffffp-328},
            {0x1.ffffffffffffep-352,    0x1.fffffffffffffp-118},
            {0x1.ffffffffffffep-349,    0x1.fffffffffffffp-117},
            {0x1.ffffffffffffep-346,    0x1.fffffffffffffp-116},
            {0x1.ffffffffffffep-343,    0x1.fffffffffffffp-115},
            {0x1.ffffffffffffep-340,    0x1.fffffffffffffp-114},
            {0x1.ffffffffffffep-337,    0x1.fffffffffffffp-113},
            {0x1.ffffffffffffep-334,    0x1.fffffffffffffp-112},
            {0x1.ffffffffffffep-331,    0x1.fffffffffffffp-111},
            {0x1.ffffffffffffep-328,    0x1.fffffffffffffp-110},
            {0x1.ffffffffffffep-325,    0x1.fffffffffffffp-109},
            {0x1.ffffffffffffep-322,    0x1.fffffffffffffp-108},
            {0x1.ffffffffffffep-319,    0x1.fffffffffffffp-107},
            {0x1.ffffffffffffep-316,    0x1.fffffffffffffp-106},
            {0x1.ffffffffffffep-313,    0x1.fffffffffffffp-105},
            {0x1.ffffffffffffep-310,    0x1.fffffffffffffp-104},
            {0x1.ffffffffffffep-307,    0x1.fffffffffffffp-103},
            {0x1.ffffffffffffep-979,    0x1.fffffffffffffp-327},
            {0x1.ffffffffffffep-304,    0x1.fffffffffffffp-102},
            {0x1.ffffffffffffep-301,    0x1.fffffffffffffp-101},
            {0x1.ffffffffffffep-298,    0x1.fffffffffffffp-100},
            {0x1.ffffffffffffep-295,    0x1.fffffffffffffp-99},
            {0x1.ffffffffffffep-292,    0x1.fffffffffffffp-98},
            {0x1.ffffffffffffep-289,    0x1.fffffffffffffp-97},
            {0x1.ffffffffffffep-286,    0x1.fffffffffffffp-96},
            {0x1.ffffffffffffep-283,    0x1.fffffffffffffp-95},
            {0x1.ffffffffffffep-280,    0x1.fffffffffffffp-94},
            {0x1.ffffffffffffep-277,    0x1.fffffffffffffp-93},
            {0x1.ffffffffffffep-274,    0x1.fffffffffffffp-92},
            {0x1.ffffffffffffep-271,    0x1.fffffffffffffp-91},
            {0x1.ffffffffffffep-268,    0x1.fffffffffffffp-90},
            {0x1.ffffffffffffep-265,    0x1.fffffffffffffp-89},
            {0x1.ffffffffffffep-262,    0x1.fffffffffffffp-88},
            {0x1.ffffffffffffep-259,    0x1.fffffffffffffp-87},
            {0x1.ffffffffffffep-1021,   0x1.fffffffffffffp-341},
            {0x1.ffffffffffffep-976,    0x1.fffffffffffffp-326},
            {0x1.ffffffffffffep-256,    0x1.fffffffffffffp-86},
            {0x1.ffffffffffffep-253,    0x1.fffffffffffffp-85},
            {0x1.ffffffffffffep-250,    0x1.fffffffffffffp-84},
            {0x1.ffffffffffffep-247,    0x1.fffffffffffffp-83},
            {0x1.ffffffffffffep-244,    0x1.fffffffffffffp-82},
            {0x1.ffffffffffffep-241,    0x1.fffffffffffffp-81},
            {0x1.ffffffffffffep-238,    0x1.fffffffffffffp-80},
            {0x1.ffffffffffffep-235,    0x1.fffffffffffffp-79},
            {0x1.ffffffffffffep-232,    0x1.fffffffffffffp-78},
            {0x1.ffffffffffffep-229,    0x1.fffffffffffffp-77},
            {0x1.ffffffffffffep-226,    0x1.fffffffffffffp-76},
            {0x1.ffffffffffffep-223,    0x1.fffffffffffffp-75},
            {0x1.ffffffffffffep-220,    0x1.fffffffffffffp-74},
            {0x1.ffffffffffffep-217,    0x1.fffffffffffffp-73},
            {0x1.ffffffffffffep-214,    0x1.fffffffffffffp-72},
            {0x1.ffffffffffffep-211,    0x1.fffffffffffffp-71},
            {0x1.ffffffffffffep-973,    0x1.fffffffffffffp-325},
            {0x1.ffffffffffffep-208,    0x1.fffffffffffffp-70},
            {0x1.ffffffffffffep-205,    0x1.fffffffffffffp-69},
            {0x1.ffffffffffffep-202,    0x1.fffffffffffffp-68},
            {0x1.ffffffffffffep-199,    0x1.fffffffffffffp-67},
            {0x1.ffffffffffffep-196,    0x1.fffffffffffffp-66},
            {0x1.ffffffffffffep-193,    0x1.fffffffffffffp-65},
            {0x1.ffffffffffffep-190,    0x1.fffffffffffffp-64},
            {0x1.ffffffffffffep-187,    0x1.fffffffffffffp-63},
            {0x1.ffffffffffffep-184,    0x1.fffffffffffffp-62},
            {0x1.ffffffffffffep-181,    0x1.fffffffffffffp-61},
            {0x1.ffffffffffffep-178,    0x1.fffffffffffffp-60},
            {0x1.ffffffffffffep-175,    0x1.fffffffffffffp-59},
            {0x1.ffffffffffffep-172,    0x1.fffffffffffffp-58},
            {0x1.ffffffffffffep-169,    0x1.fffffffffffffp-57},
            {0x1.ffffffffffffep-166,    0x1.fffffffffffffp-56},
            {0x1.ffffffffffffep-163,    0x1.fffffffffffffp-55},
            {0x1.ffffffffffffep-970,    0x1.fffffffffffffp-324},
            {0x1.ffffffffffffep-160,    0x1.fffffffffffffp-54},
            {0x1.ffffffffffffep-157,    0x1.fffffffffffffp-53},
            {0x1.ffffffffffffep-154,    0x1.fffffffffffffp-52},
            {0x1.ffffffffffffep-151,    0x1.fffffffffffffp-51},
            {0x1.ffffffffffffep-148,    0x1.fffffffffffffp-50},
            {0x1.ffffffffffffep-145,    0x1.fffffffffffffp-49},
            {0x1.ffffffffffffep-142,    0x1.fffffffffffffp-48},
            {0x1.ffffffffffffep-139,    0x1.fffffffffffffp-47},
            {0x1.ffffffffffffep-136,    0x1.fffffffffffffp-46},
            {0x1.ffffffffffffep-133,    0x1.fffffffffffffp-45},
            {0x1.ffffffffffffep-130,    0x1.fffffffffffffp-44},
            {0x1.ffffffffffffep-127,    0x1.fffffffffffffp-43},
            {0x1.ffffffffffffep-124,    0x1.fffffffffffffp-42},
            {0x1.ffffffffffffep-121,    0x1.fffffffffffffp-41},
            {0x1.ffffffffffffep-118,    0x1.fffffffffffffp-40},
            {0x1.ffffffffffffep-115,    0x1.fffffffffffffp-39},
            {0x1.ffffffffffffep-967,    0x1.fffffffffffffp-323},
            {0x1.ffffffffffffep-112,    0x1.fffffffffffffp-38},
            {0x1.ffffffffffffep-109,    0x1.fffffffffffffp-37},
            {0x1.ffffffffffffep-106,    0x1.fffffffffffffp-36},
            {0x1.ffffffffffffep-103,    0x1.fffffffffffffp-35},
            {0x1.ffffffffffffep-100,    0x1.fffffffffffffp-34},
            {0x1.ffffffffffffep-97,     0x1.fffffffffffffp-33},
            {0x1.ffffffffffffep-94,     0x1.fffffffffffffp-32},
            {0x1.ffffffffffffep-91,     0x1.fffffffffffffp-31},
            {0x1.ffffffffffffep-88,     0x1.fffffffffffffp-30},
            {0x1.ffffffffffffep-85,     0x1.fffffffffffffp-29},
            {0x1.ffffffffffffep-82,     0x1.fffffffffffffp-28},
            {0x1.ffffffffffffep-79,     0x1.fffffffffffffp-27},
            {0x1.ffffffffffffep-76,     0x1.fffffffffffffp-26},
            {0x1.ffffffffffffep-73,     0x1.fffffffffffffp-25},
            {0x1.ffffffffffffep-70,     0x1.fffffffffffffp-24},
            {0x1.ffffffffffffep-67,     0x1.fffffffffffffp-23},
            {0x1.ffffffffffffep-964,    0x1.fffffffffffffp-322},
            {0x1.ffffffffffffep-64,     0x1.fffffffffffffp-22},
            {0x1.ffffffffffffep-61,     0x1.fffffffffffffp-21},
            {0x1.ffffffffffffep-58,     0x1.fffffffffffffp-20},
            {0x1.ffffffffffffep-55,     0x1.fffffffffffffp-19},
            {0x1.ffffffffffffep-52,     0x1.fffffffffffffp-18},
            {0x1.ffffffffffffep-49,     0x1.fffffffffffffp-17},
            {0x1.ffffffffffffep-46,     0x1.fffffffffffffp-16},
            {0x1.ffffffffffffep-43,     0x1.fffffffffffffp-15},
            {0x1.ffffffffffffep-40,     0x1.fffffffffffffp-14},
            {0x1.ffffffffffffep-37,     0x1.fffffffffffffp-13},
            {0x1.ffffffffffffep-34,     0x1.fffffffffffffp-12},
            {0x1.ffffffffffffep-31,     0x1.fffffffffffffp-11},
            {0x1.ffffffffffffep-28,     0x1.fffffffffffffp-10},
            {0x1.ffffffffffffep-25,     0x1.fffffffffffffp-9},
            {0x1.ffffffffffffep-22,     0x1.fffffffffffffp-8},
            {0x0.000000000003ep-1022,   0x1.fa9c313858568p-357},
            {0x1.ffffffffffffep-19,     0x1.fffffffffffffp-7},
            {0x1.ffffffffffffep-961,    0x1.fffffffffffffp-321},
            {0x1.ffffffffffffep-16,     0x1.fffffffffffffp-6},
            {0x1.ffffffffffffep-13,     0x1.fffffffffffffp-5},
            {0x1.ffffffffffffep-10,     0x1.fffffffffffffp-4},
            {0x1.ffffffffffffep-7,      0x1.fffffffffffffp-3},
            {0x0.000000000003fp-1022,   0x1.fd51bf2069fe6p-357},
            {0x1.ffffffffffffep-4,      0x1.fffffffffffffp-2},
            {0x1.ffffffffffffep-1,      0x1.fffffffffffffp-1},
            {0x0.000000003fffcp-1022,   0x1.ffff55551c71bp-353},
            {0x0.000003fffffe8p-1022,   0x1.ffffffcp-349},
            {0x0.000003ffffffcp-1022,   0x1.fffffff555555p-349},
            {0x0.003fffffffff9p-1022,   0x1.fffffffffed55p-345},
            {0x1.ffffffffffffep2,       0x1.fffffffffffffp0},
            {0x1.bp4,                   0x1.8p1},
            {0x1.ffffffffffffep5,       0x1.fffffffffffffp1},
            {0x1.f3ffffffffff4p6,       0x1.3fffffffffffep2},
            {0x1.f3ffffffffffcp6,       0x1.3ffffffffffffp2},
            {0x1.bp7,                   0x1.8p2},
            {0x1.56ffffffffffep8,       0x1.bffffffffffffp2},
            {0x1.ffffffffffffep8,       0x1.fffffffffffffp2},
            {0x1.6c8p9,                 0x1.2p3},
            {0x1.f3ffffffffff4p9,       0x1.3fffffffffffep3},
            {0x1.f3ffffffffffcp9,       0x1.3ffffffffffffp3},
            {0x1.4cbfffffffffcp10,      0x1.5fffffffffffep3},
            {0x1.4cbfffffffffep10,      0x1.5ffffffffffffp3},
            {0x1.bp10,                  0x1.8p3},
            {0x1.129ffffffffa4p11,      0x1.9ffffffffffd1p3},
            {0x1.129fffffffffep11,      0x1.9ffffffffffffp3},
            {0x1.56ffffffffffep11,      0x1.bffffffffffffp3},
            {0x1.a5ep11,                0x1.ep3},
            {0x1.ffffffffffffep11,      0x1.fffffffffffffp3},
            {0x1.330fffffffc1ep12,      0x1.0fffffffffedbp4},
            {0x1.331p12,                0x1.1p4},
            {0x1.6c8p12,                0x1.2p4},
            {0x1.acafffffffffap12,      0x1.2ffffffffffffp4},
            {0x1.acafffffffffep12,      0x1.2ffffffffffffp4},
            {0x1.ffffffffffffep-958,    0x1.fffffffffffffp-320},
            {0x1.ffffffffffffep-955,    0x1.fffffffffffffp-319},
            {0x1.ffffffffffffep-952,    0x1.fffffffffffffp-318},
            {0x1.ffffffffffffep-949,    0x1.fffffffffffffp-317},
            {0x1.ffffffffffffep-946,    0x1.fffffffffffffp-316},
            {0x1.ffffffffffffep-943,    0x1.fffffffffffffp-315},
            {0x1.ffffffffffffep-940,    0x1.fffffffffffffp-314},
            {0x1.ffffffffffffep-937,    0x1.fffffffffffffp-313},
            {0x1.ffffffffffffep-934,    0x1.fffffffffffffp-312},
            {0x1.ffffffffffffep-931,    0x1.fffffffffffffp-311},
            {0x1.ffffffffffffep-1018,   0x1.fffffffffffffp-340},
            {0x1.ffffffffffffep-928,    0x1.fffffffffffffp-310},
            {0x1.ffffffffffffep-925,    0x1.fffffffffffffp-309},
            {0x1.ffffffffffffep-922,    0x1.fffffffffffffp-308},
            {0x1.ffffffffffffep-919,    0x1.fffffffffffffp-307},
            {0x1.ffffffffffffep-916,    0x1.fffffffffffffp-306},
            {0x1.ffffffffffffep-913,    0x1.fffffffffffffp-305},
            {0x1.ffffffffffffep-910,    0x1.fffffffffffffp-304},
            {0x1.ffffffffffffep-907,    0x1.fffffffffffffp-303},
            {0x1.ffffffffffffep-904,    0x1.fffffffffffffp-302},
            {0x0.0000000000007p-1022,   0x1.e9b5dba58189ep-358},
            {0x1.ffffffffffffep-901,    0x1.fffffffffffffp-301},
            {0x1.ffffffffffffep-898,    0x1.fffffffffffffp-300},
            {0x0.0000000007ffp-1022,    0x1.ffeaa9c70ca31p-354},
            {0x0.0000000007ffep-1022,   0x1.fffd5551c7149p-354},
            {0x0.0000007fffffdp-1022,   0x1.ffffffcp-350},
            {0x0.0000007fffffep-1022,   0x1.ffffffd555555p-350},
            {0x0.0007ffffffffap-1022,   0x1.fffffffff8p-346},
            {0x0.7ffffffffffffp-1022,   0x1.fffffffffffffp-342},
            {0x1.ffffffffffffep-895,    0x1.fffffffffffffp-299},
            {0x1.ffffffffffffep-892,    0x1.fffffffffffffp-298},
            {0x1.ffffffffffffep-889,    0x1.fffffffffffffp-297},
            {0x1.ffffffffffffep-886,    0x1.fffffffffffffp-296},
            {0x1.ffffffffffffep-883,    0x1.fffffffffffffp-295},
            {0x1.ffffffffffffep-1015,   0x1.fffffffffffffp-339},
            {0x1.ffffffffffffep-880,    0x1.fffffffffffffp-294},
            {0x1.ffffffffffffep-877,    0x1.fffffffffffffp-293},
            {0x1.ffffffffffffep-874,    0x1.fffffffffffffp-292},
            {0x1.ffffffffffffep-871,    0x1.fffffffffffffp-291},
            {0x1.ffffffffffffep-868,    0x1.fffffffffffffp-290},
            {0x1.ffffffffffffep-865,    0x1.fffffffffffffp-289},
            {0x1.ffffffffffffep-862,    0x1.fffffffffffffp-288},
            {0x1.ffffffffffffep-859,    0x1.fffffffffffffp-287},
            {0x1.ffffffffffffep-856,    0x1.fffffffffffffp-286},
            {0x1.ffffffffffffep-853,    0x1.fffffffffffffp-285},
            {0x1.ffffffffffffep-850,    0x1.fffffffffffffp-284},
            {0x1.ffffffffffffep-847,    0x1.fffffffffffffp-283},
            {0x1.ffffffffffffep-844,    0x1.fffffffffffffp-282},
            {0x1.ffffffffffffep-841,    0x1.fffffffffffffp-281},
            {0x1.ffffffffffffep-838,    0x1.fffffffffffffp-280},
            {0x1.ffffffffffffep-835,    0x1.fffffffffffffp-279},
            {0x1.ffffffffffffep-1012,   0x1.fffffffffffffp-338},
            {0x1.ffffffffffffep-832,    0x1.fffffffffffffp-278},
            {0x1.ffffffffffffep-829,    0x1.fffffffffffffp-277},
            {0x1.ffffffffffffep-826,    0x1.fffffffffffffp-276},
            {0x1.ffffffffffffep-823,    0x1.fffffffffffffp-275},
            {0x1.ffffffffffffep-820,    0x1.fffffffffffffp-274},
            {0x1.ffffffffffffep-817,    0x1.fffffffffffffp-273},
            {0x1.ffffffffffffep-814,    0x1.fffffffffffffp-272},
            {0x1.ffffffffffffep-811,    0x1.fffffffffffffp-271},
            {0x1.ffffffffffffep-808,    0x1.fffffffffffffp-270},
            {0x1.ffffffffffffep-805,    0x1.fffffffffffffp-269},
            {0x1.ffffffffffffep-802,    0x1.fffffffffffffp-268},
            {0x1.ffffffffffffep-799,    0x1.fffffffffffffp-267},
            {0x1.ffffffffffffep-796,    0x1.fffffffffffffp-266},
            {0x1.ffffffffffffep-793,    0x1.fffffffffffffp-265},
            {0x1.ffffffffffffep-790,    0x1.fffffffffffffp-264},
            {0x1.ffffffffffffep-787,    0x1.fffffffffffffp-263},
            {0x1.ffffffffffffep-1009,   0x1.fffffffffffffp-337},
            {0x1.ffffffffffffep-784,    0x1.fffffffffffffp-262},
            {0x1.ffffffffffffep-781,    0x1.fffffffffffffp-261},
            {0x1.ffffffffffffep-778,    0x1.fffffffffffffp-260},
            {0x1.ffffffffffffep-775,    0x1.fffffffffffffp-259},
            {0x1.ffffffffffffep-772,    0x1.fffffffffffffp-258},
            {0x1.ffffffffffffep-769,    0x1.fffffffffffffp-257},
            {0x0.0000000000ffep-1022,   0x1.ffeaa9c70ca31p-355},
            {0x0.0000000000fffp-1022,   0x1.fff5551c6fcd6p-355},
            {0x0.0000000ffff86p-1022,   0x1.ffffaeaa9dbf1p-351},
            {0x0.0000000ffffffp-1022,   0x1.ffffff5555552p-351},
            {0x0.0000ffffffap-1022,     0x1.ffffffcp-347},
            {0x0.0000ffffffff8p-1022,   0x1.ffffffffaaaabp-347},
            {0x0.0fffffffffffbp-1022,   0x1.fffffffffffcbp-343}
        };

        for(double[] testCase: testCases)
            failures+=testCubeRootCase(testCase[0], testCase[1]);

        return failures;
    }

    // Initialize shared random number generator
    private static java.util.Random random = RandomFactory.getRandom();

    /**
     * Test StrictMath.cbrt against transliteration port of cbrt.
     */
    private static int testAgainstTranslit() {
        int failures = 0;
        double x;

        // Test just above subnormal threshold...
        x = Double.MIN_NORMAL;
        failures += testRange(x, Math.ulp(x), 1000);

        // ... and just below subnormal threshold ...
        x =  Math.nextDown(Double.MIN_NORMAL);
        failures += testRange(x, -Math.ulp(x), 1000);

        // ... and near zero.
        failures += testRange(0.0, Double.MIN_VALUE, 1000);

        x = Tests.createRandomDouble(random);

        // Make the increment twice the ulp value in case the random
        // value is near an exponent threshold. Don't worry about test
        // elements overflowing to infinity if the starting value is
        // near Double.MAX_VALUE.
        failures += testRange(x, 2.0 * Math.ulp(x), 1000);

        return failures;
    }

    private static int testRange(double start, double increment, int count) {
        int failures = 0;
        double x = start;
        for (int i = 0; i < count; i++, x += increment) {
            failures += testCubeRootCase(x, FdlibmTranslit.Cbrt.compute(x));
        }
        return failures;
    }
}
