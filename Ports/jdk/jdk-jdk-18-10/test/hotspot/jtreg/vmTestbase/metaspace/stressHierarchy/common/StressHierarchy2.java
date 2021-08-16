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
package metaspace.stressHierarchy.common;

import metaspace.stressHierarchy.common.classloader.tree.Tree;
import metaspace.stressHierarchy.common.exceptions.ClassNotUnloadedException;
import nsk.share.test.ExecutionController;
import nsk.share.test.Tests;

/**
 * Test case checks that presence of ancestors does not prevent descenders of being reclaimed.
 * We create hierarchy of classes, each loaded by dedicated classloader. Then we start the following
 * routine in loop: release references to classes in bottom level, provoke unloading and check
 *      1) that classes in bottom level were unloaded and
 *      2) that next to bottom level was NOT unloaded.
 */
public class StressHierarchy2 extends StressHierarchyBaseClass {

    public static void main(String[] args) {
        try {
            StressHierarchyBaseClass.args = args;
            Tests.runTest(new StressHierarchy2(), args);
        } catch (OutOfMemoryError error) {
            System.out.print("Got OOME: " + error.getMessage());
        }
    }

    @Override
    protected void runTestLogic(Tree tree, ExecutionController stresser)
            throws Throwable {

        for (int cleanupLevel = tree.getMaxLevel(); cleanupLevel >= 0; cleanupLevel-- ) {
            tree.cleanupLevel(cleanupLevel);
            log.info("cleanupLevel=" + cleanupLevel);
            triggerUnloadingHelper.triggerUnloading(stresser);
            if (!stresser.continueExecution()) {
                return;
            }
            if (cleanupLevel > 0) {
                performChecksHelper.checkLevelAlive(tree, cleanupLevel - 1);
            }
            if (!stresser.continueExecution()) {
                return;
            }

            try {
                performChecksHelper.checkLevelReclaimed(tree, cleanupLevel);
            } catch (ClassNotUnloadedException exception) {
                log.error("Class was not unloaded." + exception.toString());
                setFailed(true);
                throw exception;
            }
        }
    }

}
