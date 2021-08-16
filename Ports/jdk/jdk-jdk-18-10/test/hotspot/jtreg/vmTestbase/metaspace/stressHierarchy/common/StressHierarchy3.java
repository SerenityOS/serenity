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
import nsk.share.test.ExecutionController;
import nsk.share.test.Tests;

/**
 * 1. Test case cleans up all levels except bottom, then checks that bottom level is alive (and whole tree).
 * 2. Then test cleans up whole tree and checks that it is reclaimed.
 */
public class StressHierarchy3 extends StressHierarchyBaseClass {

    public static void main(String[] args) {
        try {
            StressHierarchyBaseClass.args = args;
            Tests.runTest(new StressHierarchy3(), args);
        } catch (OutOfMemoryError error) {
            System.out.print("Got OOME: " + error.getMessage());
        }
    }

    @Override
    protected void runTestLogic(Tree tree, ExecutionController stresser)
            throws Throwable {

        for (int cleanupLevel = tree.getMaxLevel() - 1; cleanupLevel >= 0; cleanupLevel--) {
            if (! stresser.continueExecution()) { return; }
            tree.cleanupLevel(cleanupLevel);
            log.info("cleanupLevel=" + cleanupLevel);
        }

        triggerUnloadingHelper.triggerUnloading(stresser);

        if (! stresser.continueExecution()) { return; }

        log.info("Check bottom level alive ");
        performChecksHelper.checkLevelAlive(tree, tree.getMaxLevel());

        if (! stresser.continueExecution()) { return; }

        log.info("Cleanup all");
        tree.cleanupLevel(tree.getMaxLevel());

        triggerUnloadingHelper.triggerUnloading(stresser);

        log.info("Check bottom level reclaimed");
        performChecksHelper.checkLevelReclaimed(tree, tree.getMaxLevel());
    }

}
