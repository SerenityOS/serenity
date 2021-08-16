/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8168518 8194486
 * @summary  testing jdk.krb5.rcache.useMD5. This action is put in a separate
 *           test so that ReplayCacheTestProc.java can be launched with special
 *           test.* system properties easily.
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm/timeout=300 -Djdk.krb5.rcache.useMD5=true
 *           -Djdk.net.hosts.file=TestHosts
 *           -Dtest.service=host
 *           -Dtest.libs=J ReplayCacheTestProc
 */
