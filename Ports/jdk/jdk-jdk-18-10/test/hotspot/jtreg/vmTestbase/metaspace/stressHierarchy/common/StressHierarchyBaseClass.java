/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.net.MalformedURLException;

import vm.share.gc.HeapOOMEException;
import vm.share.gc.TriggerUnloadingByFillingMetaspace;
import vm.share.gc.TriggerUnloadingHelper;
import vm.share.gc.TriggerUnloadingWithWhiteBox;

import metaspace.stressHierarchy.common.classloader.tree.Node;
import metaspace.stressHierarchy.common.classloader.tree.Tree;
import metaspace.stressHierarchy.common.exceptions.TimeIsOverException;
import metaspace.stressHierarchy.common.generateHierarchy.GenerateHierarchyHelper;
import metaspace.stressHierarchy.common.generateHierarchy.GenerateHierarchyHelper.Type;
import metaspace.stressHierarchy.common.generateHierarchy.NodeDescriptor;
import metaspace.stressHierarchy.common.generateHierarchy.TreeDescriptor;
import nsk.share.test.ExecutionController;
import nsk.share.test.Stresser;
import nsk.share.test.TestBase;


/**
 * Superclass for StressHierarchy* tests. It provides util methods to create and load
 * classes hierarchy and perform checks.
 */
abstract public class StressHierarchyBaseClass extends TestBase {

    protected static String[] args;

    protected TriggerUnloadingHelper triggerUnloadingHelper = new TriggerUnloadingWithWhiteBox(); //default helper

    protected PerformChecksHelper performChecksHelper = null;

    private int treeDepth;

    private int minLevelSize;

    private int maxLevelSize;

    private Type hierarchyType;

    public void run() {
        try {
            int attemptsLimit = -1; // -1 means using default value defined in PerformChecksHelper
            long unloadingPause = -1; // -1 means the same
            int pausesLimit = -1; // -1 means the same

            for (int ind = 0; ind < args.length; ind++ ) {
                if ("-triggerUnloadingByFillingMetaspace".equals(args[ind])) {
                    log.info("using TriggerUnloadingByFillingMetaspace");
                    triggerUnloadingHelper = new TriggerUnloadingByFillingMetaspace();
                } else if ("-treeDepth".equals(args[ind])) {
                    this.treeDepth = Integer.parseInt(args[ind + 1]);
                } else if ("-minLevelSize".equals(args[ind])) {
                    this.minLevelSize = Integer.parseInt(args[ind + 1]);
                } else if ("-maxLevelSize".equals(args[ind])) {
                    this.maxLevelSize = Integer.parseInt(args[ind + 1]);
                } else if ("-attemptsLimit".equals(args[ind])) {
                        attemptsLimit = Integer.valueOf(args[ind + 1]);
                } else if ("-unloadingPause".equals(args[ind])) {
                        unloadingPause = Long.valueOf(args[ind + 1]);
                } else if ("-pausesLimit".equals(args[ind])) {
                        pausesLimit = Integer.valueOf(args[ind + 1]);
                } else if ("-hierarchyType".equals(args[ind])) {
                    String s = args[ind + 1];
                    hierarchyType = Type.CLASSES.toString().equals(s) ? Type.CLASSES :
                            (Type.INTERFACES.toString().equals(s) ? Type.INTERFACES : Type.MIXED);
                    System.out.println("hierarchyType = " + hierarchyType);
                } else if (args[ind].startsWith("-") && !args[ind].equals("-stressTime")) {
                    throw new RuntimeException("Unknown option " + args[ind]);
                }
            }
            performChecksHelper = new PerformChecksHelper(triggerUnloadingHelper, attemptsLimit, unloadingPause, pausesLimit);
            log.info("treeDepth=" + treeDepth + ", minLevelSize=" + minLevelSize + ", maxLevelSize=" + maxLevelSize + ", hierarchyType=" + hierarchyType +
                    ", triggerUnloadingHelper.getClass().getName()=" + triggerUnloadingHelper.getClass().getName());

            long startTimeStamp = System.currentTimeMillis();
            ExecutionController stresser = new Stresser(args);
            stresser.start(1);
            TreeDescriptor treeDescriptor = GenerateHierarchyHelper.generateHierarchy(treeDepth, minLevelSize, maxLevelSize, hierarchyType);
            Tree tree = buildTree(treeDescriptor);
            System.out.println("Generating took " + ((System.currentTimeMillis() - startTimeStamp)/1000) +" sec");

            performChecksHelper.setStresser(stresser);

            runTestLogic(tree, stresser);

            System.out.println("Whole test took " + ((System.currentTimeMillis() - startTimeStamp)/1000/60.0) +" min");
            log.info("Test PASSED");
        } catch (HeapOOMEException e) {
            log.info("HeapOOMEException: " + e.getMessage());
            log.info("Got wrong type of OOME. We are passing test as it breaks test logic. We have dedicated test configurations" +
            " for each OOME type provoking class unloading, that's why we are not missing test coverage here.");
        } catch (OutOfMemoryError e) {
            log.info("Got OOME.");
        } catch (TimeIsOverException e) {
            log.info("Time is over. That's okay. Passing test");
        } catch (Throwable throwable) {
            //Throw runtime exception. nsk framework will catch it, log and set appropriate exit code
            log.error("Test failed. Exception catched.");
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
    }

    abstract protected void runTestLogic(Tree tree, ExecutionController stresser) throws Throwable;

    private Tree buildTree(TreeDescriptor treeDescriptor) throws MalformedURLException,
            ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        log.info("Create tree");
        Tree tree = new Tree();
        for (NodeDescriptor nodeDescriptor : treeDescriptor.nodeDescriptorList) {
            tree.addNode(nodeDescriptor);
        }

        log.info("Load classes and instantiate objects");
        for (Node node : tree.getNodes()) {
            node.loadClasses();
            node.instantiateObjects();
        }
        return tree;
    }

}
