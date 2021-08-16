/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4530538
 * @summary Basic unit test of ClassLoadingMXBean.getLoadedClassCount()
 *                             ClassLoadingMXBean.getTotalLoadedClassCount()
 *                             ClassLoadingMXBean.getUnloadedClassCount()
 * @author  Alexei Guibadoulline
 *
 * @run main/othervm LoadCounts
 */

import java.lang.management.*;

public class LoadCounts {
    private static ClassLoadingMXBean mbean
        = ManagementFactory.getClassLoadingMXBean();

    public static void main(String argv[]) throws Exception {
        // Get current count
        int classesNowPrev = mbean.getLoadedClassCount();
        long classesTotalPrev = mbean.getTotalLoadedClassCount();

        System.out.println("Loading 4 classes with the system class loader");

        new SimpleOne();
        new SimpleTwo();
        new Chain();

        int classesNow = mbean.getLoadedClassCount();
        long classesTotal = mbean.getTotalLoadedClassCount();

        if (classesNow > classesTotal)
            throw new RuntimeException("getLoadedClassCount() > "
                                     + "getTotalLoadedClassCount()");

        if (classesNowPrev + 4 > classesNow)
            throw new RuntimeException("Number of loaded classes is "
                                     + "expected to be at least "
                                     + (classesNowPrev + 4) + ", but "
                                     + "MBean.getLoadedClassCount() returned "
                                     + classesNow);
        if (classesTotalPrev + 4 > classesTotal)
            throw new RuntimeException("Total number of loaded classes is "
                                     + "expected to be at least "
                                     + (classesTotalPrev + 4) + ", but "
                                     + "MBean.getTotalLoadedClassCount() "
                                     + "returned " + classesTotal);

        System.out.println("Creating new class loader instances");

        LeftHand leftHand = new LeftHand();
        RightHand rightHand = new RightHand();
        LoaderForTwoInstances ins1 = new LoaderForTwoInstances();
        LoaderForTwoInstances ins2 = new LoaderForTwoInstances();

        // Load different type of classes with different
        // initiating classloaders but the same defining class loader.
        System.out.println("Loading 2 class instances; each by " +
                           "2 initiating class loaders.");

        classesNowPrev = mbean.getLoadedClassCount();
        classesTotalPrev = mbean.getTotalLoadedClassCount();
        try {
            Class.forName("Body", true, leftHand);
            Class.forName("Body", true, rightHand);
            Class.forName("TheSameClass", true, ins1);
            Class.forName("TheSameClass", true, ins2);
        } catch (ClassNotFoundException e) {
            System.out.println("Unexpected excetion " + e);
            e.printStackTrace(System.out);
            throw new RuntimeException();
        }
        classesNow = mbean.getLoadedClassCount();
        classesTotal = mbean.getTotalLoadedClassCount();

        // Expected 2 classes got loaded since they are loaded by
        // same defining class loader
        if (classesNowPrev + 2 > classesNow)
            throw new RuntimeException("Number of loaded classes is "
                                     + "expected to be at least "
                                     + (classesNowPrev + 4) + ", but "
                                     + "MBean.getLoadedClassCount() returned "
                                     + classesNow);
        if (classesTotalPrev + 2 > classesTotal)
            throw new RuntimeException("Total number of loaded classes is "
                                     + "expected to be at least "
                                     + (classesTotalPrev + 4) + ", but "
                                     + "MBean.getTotalLoadedClassCount() "
                                     + "returned " + classesTotal);

        System.out.println("Test passed.");
    }
}

class SimpleOne {}
class SimpleTwo {}

class Chain {
    Worker worker = new Worker();
}
class Worker {}

class LeftHand extends ClassLoader {
    public LeftHand() {
        super(LeftHand.class.getClassLoader());
    }
}
class RightHand extends ClassLoader {
    public RightHand() {
        super(RightHand.class.getClassLoader());
    }
}
class Body {}

class LoaderForTwoInstances extends ClassLoader {
    public LoaderForTwoInstances() {
        super(LoaderForTwoInstances.class.getClassLoader());
    }
}
class TheSameClass {}
