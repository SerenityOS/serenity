/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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
 *
 */

/*
 * This runs the parts of the gtest which make sense in large-page scenarios
 *  (mainly os*).
 * Note that if these tests run on a system whose kernel supports large pages
 *   but where no huge pages are configured, these tests are still useful. They
 *   will test correct initialization. Later reserve calls will fail and fall
 *   back to small pages (while complaining loudly) but this should not affect
 *   the gtests. When tests complain, they would spew a lot of warning messages,
 *   which could blow out the test runner Java heaps. This is why we are running
 *   with -XX:-PrintWarnings.
 */

/* @test id=use-large-pages
 * @summary Run metaspace-related gtests for reclaim policy none (with verifications)
 * @requires os.family == "linux" | os.family == "windows"
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.xml
 * @requires vm.flagless
 * @run main/native GTestWrapper --gtest_filter=os* -XX:-PrintWarnings -XX:+UseLargePages
 */

/* @test id=use-large-pages-1G
 * @summary Run metaspace-related gtests for reclaim policy none (with verifications)
 * @requires os.family == "linux"
 * @requires vm.bits == "64"
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.xml
 * @requires vm.flagless
 * @run main/native GTestWrapper --gtest_filter=os* -XX:-PrintWarnings -XX:+UseLargePages -XX:LargePageSizeInBytes=1G
 */

/* @test id=use-large-pages-sysV
 * @summary Run metaspace-related gtests for reclaim policy none (with verifications)
 * @requires os.family == "linux"
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.xml
 * @requires vm.flagless
 * @run main/native GTestWrapper --gtest_filter=os* -XX:-PrintWarnings -XX:+UseLargePages -XX:+UseSHM
 */
