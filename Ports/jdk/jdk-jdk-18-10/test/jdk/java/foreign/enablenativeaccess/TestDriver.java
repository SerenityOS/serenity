/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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


/**
 * @test id=panama_enable_native_access
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @build panama_module/*
 * @run main/othervm --enable-native-access=panama_module panama_module/org.openjdk.foreigntest.PanamaMain
 * @summary with --enable-native-access access to specific module Panama unsafe API succeeds
 */

/**
 * @test id=panama_enable_native_access_reflection
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @build panama_module/*
 * @run main/othervm --enable-native-access=panama_module panama_module/org.openjdk.foreigntest.PanamaMainReflection
 * @summary with --enable-native-access access to specific module Panama unsafe API succeeds
 */

/**
 * @test id=panama_enable_native_access_invoke
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @build panama_module/*
 * @run main/othervm --enable-native-access=panama_module panama_module/org.openjdk.foreigntest.PanamaMainInvoke
 * @summary with --enable-native-access access to specific module Panama unsafe API succeeds
 */

/**
 * @test id=panama_comma_separated_enable
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @build panama_module/*
 * @run main/othervm --enable-native-access=com.acme,panama_module panama_module/org.openjdk.foreigntest.PanamaMain
 * @summary with --enable-native-access access to comma separated list of modules
 */

/**
 * @test id=panama_comma_separated_enable_reflection
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @build panama_module/*
 * @run main/othervm --enable-native-access=com.acme,panama_module panama_module/org.openjdk.foreigntest.PanamaMainReflection
 * @summary with --enable-native-access access to comma separated list of modules
 */

/**
 * @test id=panama_comma_separated_enable_invoke
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @build panama_module/*
 * @run main/othervm --enable-native-access=com.acme,panama_module panama_module/org.openjdk.foreigntest.PanamaMainInvoke
 * @summary with --enable-native-access access to comma separated list of modules
 */

/**
 * @test id=panama_no_enable_native_access_fail
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @build panama_module/*
 * @run main/othervm/fail panama_module/org.openjdk.foreigntest.PanamaMain
 * @summary without --enable-native-access access to Panama unsafe API fails
 */

/**
 * @test id=panama_no_enable_native_access_fail_reflection
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @build panama_module/*
 * @run main/othervm/fail panama_module/org.openjdk.foreigntest.PanamaMainReflection
 * @summary without --enable-native-access access to Panama unsafe API fails
 */

/**
 * @test id=panama_no_enable_native_access_fail_invoke
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @build panama_module/*
 * @run main/othervm/fail panama_module/org.openjdk.foreigntest.PanamaMainInvoke
 * @summary without --enable-native-access access to Panama unsafe API fails
 */

/**
 * @test id=panama_no_all_module_path_blanket_native_access
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @build panama_module/*
 * @run main/othervm/fail --enable-native-access=ALL-MODULE-PATH panama_module/org.openjdk.foreigntest.PanamaMain
 * @summary --enable-native-access does not work with ALL-MODULE-PATH
 */

/**
 * @test id=panama_no_unnamed_module_native_access
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @build org.openjdk.foreigntest.PanamaMainUnnamedModule
 * @run testng/othervm/fail org.openjdk.foreigntest.PanamaMainUnnamedModule
 * @summary --enable-native-access does not work without ALL-UNNAMED
 */

/**
 * @test id=panama_all_unnamed_module_native_access
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @build org.openjdk.foreigntest.PanamaMainUnnamedModule
 * @run testng/othervm --enable-native-access=ALL-UNNAMED org.openjdk.foreigntest.PanamaMainUnnamedModule
 * @summary --enable-native-access ALL-UNNAMED works
 */
