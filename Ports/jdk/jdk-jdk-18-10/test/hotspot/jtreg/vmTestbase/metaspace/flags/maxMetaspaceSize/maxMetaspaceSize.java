/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * This test set up -XX:MaxMetaspaceSize option and try to to generate OOME
 * in metaspace. It also could crash with OOM. This is expected behavior.
 */
package metaspace.flags.maxMetaspaceSize;

import nsk.share.gc.GC;
import nsk.share.gc.GCTestBase;
import nsk.share.gc.gp.*;
import nsk.share.gc.gp.classload.GeneratedClassProducer;
import nsk.share.test.Stresser;
import nsk.share.TestFailure;

public class maxMetaspaceSize extends GCTestBase {
    @Override
    public void run() {
        Stresser stresser = new Stresser(runParams.getStressOptions());
        GeneratedClassProducer garbageProducer = new GeneratedClassProducer();
        int numOOM = 0;
        stresser.start(0);
        numOOM = GarbageUtils.eatMemory(stresser, garbageProducer, 50, 100, 2, GarbageUtils.OOM_TYPE.METASPACE);
        if ((numOOM == 0) && (stresser.continueExecution())) {
            throw new TestFailure("java.lang.OutOfMemoryError: Metadata space. Exception was not thrown.");
        } else {
            log.info("java.lang.OutOfMemoryError: Metadata space. Was thrown test passed.");
        }
    }

    public static void main (String[] args) {
        GC.runTest(new maxMetaspaceSize(), args);
    }
}
