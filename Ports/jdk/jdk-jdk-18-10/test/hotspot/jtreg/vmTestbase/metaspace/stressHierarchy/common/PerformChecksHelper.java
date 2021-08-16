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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import metaspace.stressHierarchy.common.classloader.tree.Node;
import metaspace.stressHierarchy.common.classloader.tree.Tree;
import metaspace.stressHierarchy.common.exceptions.ClassNotUnloadedException;
import metaspace.stressHierarchy.common.exceptions.TimeIsOverException;
import nsk.share.test.ExecutionController;
import sun.hotspot.WhiteBox;
import vm.share.gc.TriggerUnloadingHelper;

public class PerformChecksHelper {

    private static final int NUMBER_OF_HOT_METHOD_CALLS = 100;

    private static WhiteBox wb = WhiteBox.getWhiteBox();

    // This is the number of failed attempts required to deem class unloading failed
    private int attemptsLimit = 50;

    // This is the pause between unloading attempts in milliseconds
    private long unloadingPause = 1000;

    // This is the number of failed attempts after that pauses will be involved
    private int pausesLimit = 5;

    private TriggerUnloadingHelper triggerUnloadingHelper = null;

    private ExecutionController stresser;

    public PerformChecksHelper(TriggerUnloadingHelper triggerUnloadingHelper, int attemptsLimit, long unloadingPause, int pausesLimit) {
        this.triggerUnloadingHelper = triggerUnloadingHelper;
        if (attemptsLimit != -1) {
                this.attemptsLimit = attemptsLimit;
        }
        if (unloadingPause != -1) {
                this.unloadingPause = unloadingPause;
        }
        if (pausesLimit != -1) {
                this.pausesLimit = pausesLimit;
        }
        System.out.println("attemptsLimit = " + this.attemptsLimit);
        System.out.println("unloadingPause = " + this.unloadingPause);
        System.out.println("pausesLimit = " + this.pausesLimit);
    }

    public void checkLevelReclaimed(Tree tree, int level)
            throws IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotUnloadedException, TimeIsOverException {
        long attempsCounter = 0;
        boolean checkPassed = false;
        ClassNotUnloadedException classNotUnloadedException = null;
        while (!checkPassed && attempsCounter++ < attemptsLimit) {
            if (attempsCounter > pausesLimit && unloadingPause > 0) {
                try {
                    Thread.sleep(unloadingPause);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Somebody dared to interrupt thread while we were waiting after gc provoke");
                }
            }
            try {
                checkLevel(tree, level, false);
                checkPassed = true;
            } catch (ClassNotUnloadedException exception) {
                checkPassed = false;
                classNotUnloadedException = exception;
                triggerUnloadingHelper.triggerUnloading(stresser);
            }
        }
        if (!checkPassed) {
            System.out.println("Going to throw classNotUnloadedException. attempsCounter = " + attempsCounter);
            throw classNotUnloadedException;
        }

    }

    public void checkLevelAlive(Tree tree, int level) throws IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotUnloadedException, TimeIsOverException {
        checkLevel(tree, level, true);
    }

    private void checkLevel(Tree tree, int level, boolean shouldBeAlive)
            throws IllegalAccessException, InvocationTargetException, InstantiationException, ClassNotUnloadedException, TimeIsOverException {
        for (Node node : tree.getNodesInLevel(level)) {
            for (String className : node.getLoadedClassesNames()) {
                checkStresser();
                boolean isClassAlive = wb.isClassAlive(className);
                if (isClassAlive != shouldBeAlive) {
                    throw new ClassNotUnloadedException("Failing test! Class: " + className + " shouldBeAlive: " + shouldBeAlive
                            + " isClassAlive: " + isClassAlive);
                }
            }
        }
        if (shouldBeAlive) {
            checkAncestorsAlive(tree, level);
        }
    }

    private void callMethods(Class<?> clazz)
            throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, InstantiationException {
        try {
            for (Method m : clazz.getMethods()) {
                for (int j = 0; j < NUMBER_OF_HOT_METHOD_CALLS; j++) {
                    if (m.getName().equals("composeString")) {
                        m.invoke(clazz.newInstance());
                    } else if (m.getName().equals("calculate")) {
                        m.invoke(clazz.newInstance());
                    } else if (m.getName().equals("calculate2")) {
                        m.invoke(clazz.newInstance());
                    }
                }
            }
        } catch (OutOfMemoryError e) {
            if (e.getMessage().trim().toLowerCase().contains("metaspace")) {
                // avoid string concatenation, which may create more classes.
                System.out.println("Got OOME in metaspace in PerformChecksHelper.callMethods(Class clazz). ");
                System.out.println("This is possible with -triggerUnloadingByFillingMetaspace");
            } else {
                throw e;
            }
        }
    }

    private void checkAncestors(Class<?> clazz) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException, TimeIsOverException {
        for (; clazz != null; clazz = clazz.getSuperclass()) {
            checkStresser();
            if (!clazz.isInterface()) {
                //check class
                callMethods(clazz);
            } else {
                //check interface by implementing it
                InvocationHandler handler = new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        return Integer.MIN_VALUE;
                    }
                };
                Object instance = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] {clazz}, handler);
                instance.hashCode();
            }
            if (!wb.isClassAlive(clazz.getName())) {
                throw new RuntimeException("Test failed in method checkAncestors: class "
                        + clazz.getName() + " should be alive");
            }
        }
    }

    private void checkAncestorsAlive(Tree tree, int level)
            throws IllegalAccessException, InvocationTargetException,
            InstantiationException, IllegalArgumentException, TimeIsOverException {
        List<Node> bottomLevel = tree.getNodesInLevel(level);
        if (bottomLevel.isEmpty()) {
            throw new RuntimeException("Failing test because of test bug: no nodes in bottom level");
        }
        for (Node node : bottomLevel) {
            if (node.getLoadedClasses() == null || node.getLoadedClasses().isEmpty()) {
                throw new RuntimeException("Failing test because of test bug: no classes loaded by node " + node);
            }
            for (Class<?> clazz : node.getLoadedClasses()) {
                checkAncestors(clazz);
            }
        }
    }

    public void setStresser(ExecutionController stresser) {
        this.stresser = stresser;
    }

    private void checkStresser() throws TimeIsOverException {
        if (stresser == null) {
            throw new RuntimeException("Test bug. Wrong usage of PerformChecksHelper. Stresser was not set.");
        }
        if (!stresser.continueExecution()) {
            throw new TimeIsOverException();
        }
    }


}
