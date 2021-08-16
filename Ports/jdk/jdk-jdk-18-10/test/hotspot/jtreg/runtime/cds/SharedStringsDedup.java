/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test SharedStringsDedup
 * @summary Test -Xshare:auto with shared strings and -XX:+UseStringDeduplication
 * @requires vm.cds.archived.java.heap
 * @library /test/lib
 * @run driver SharedStringsDedup
 */

import jdk.test.lib.cds.CDSTestUtils;

// The main purpose is to test the interaction between shared strings
// and -XX:+UseStringDeduplication. We run in -Xshare:auto mode so
// we don't need to worry about CDS archive mapping failure (which
// doesn't happen often so it won't impact coverage).
public class SharedStringsDedup {
    public static void main(String[] args) throws Exception {
        CDSTestUtils.createArchiveAndCheck()
            .shouldContain("Shared string table stats");
        CDSTestUtils.runWithArchiveAndCheck("-XX:+UseStringDeduplication");
    }
}
