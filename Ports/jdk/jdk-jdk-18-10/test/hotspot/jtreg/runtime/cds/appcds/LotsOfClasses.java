/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.cds.CDSOptions;

/*
 * @test
 * @summary Try to archive lots of classes by searching for classes from the jrt:/ file system. With JDK 12
 *          this will produce an archive with over 30,000 classes.
 * @requires vm.cds
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @run driver/timeout=500 LotsOfClasses
 */

public class LotsOfClasses {

    public static void main(String[] args) throws Exception {
        ArrayList<String> list = new ArrayList<>();
        TestCommon.findAllClasses(list);

        CDSOptions opts = new CDSOptions();
        opts.setClassList(list);
        opts.addSuffix("--add-modules");
        opts.addSuffix("ALL-SYSTEM");
        opts.addSuffix("-Xlog:hashtables");
        opts.addSuffix("-Xmx500m");
        opts.addSuffix("-Xlog:gc+region+cds");
        opts.addSuffix("-Xlog:cds=debug");  // test detailed metadata info printing

        CDSTestUtils.createArchiveAndCheck(opts);
    }
}
