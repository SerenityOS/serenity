/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.jvmti.unit;

public class Heap {

    // --- Errors ---

    public static final int JVMTI_ERROR_NONE = 0;

    // --- Filter types ---

    public static final int JVMTI_HEAP_OBJECT_TAGGED = 1;
    public static final int JVMTI_HEAP_OBJECT_UNTAGGED = 2;
    public static final int JVMTI_HEAP_OBJECT_EITHER = 3;

    // --- Root types ---

    public static final int JVMTI_HEAP_ROOT_JNI_GLOBAL = 1;
    public static final int JVMTI_HEAP_ROOT_SYSTEM_CLASS = 2;
    public static final int JVMTI_HEAP_ROOT_MONITOR = 3;
    public static final int JVMTI_HEAP_ROOT_STACK_LOCAL = 4;
    public static final int JVMTI_HEAP_ROOT_JNI_LOCAL = 5;
    public static final int JVMTI_HEAP_ROOT_THREAD = 6;
    public static final int JVMTI_HEAP_ROOT_OTHER = 7;

    // --- Heap Functions ---

    public static int setTag(Object o, long tag) {
        return setTag0(o, tag);
    }

    public static long getTag(Object o) {
        return getTag0(o);
    }

    public static int iterateOverHeap(int filter_kind) {
        return iterateOverHeap0(filter_kind);
    }

    public static int iterateOverInstancesOfClass(Class c, int filter_kind) {
        return iterateOverInstancesOfClass0(c, filter_kind);
    }

    public static int iterateOverReachableObjects() {
        return iterateOverReachableObjects0();
    }

    public static int iterateOverObjectsReachableFromObject(Object o) {
        return iterateOverObjectsReachableFromObject0(o);
    }

    public static native long getObjectSize(Object o);

    // --- GetObjectsWithTags ---

    public static native int getObjectsWithTags(int count, long array[]);

    public static native long[] tagResults();

    public static native Object[] objectResults();


    // --- Functions used by basic iteration tests

    public static native void setTaggedObjectCountCallback();
    public static native void setTotalObjectCountCallback();

    public static native void zeroObjectCount();
    public static native int getObjectCount();

    public static native void setKlassTagTestCallback();

    // --- Used by Heap Walking tests

    public static native Object newGlobalRef(Object o);

    public static native void setHeapRootCallback();
    public static native void setStackRefCallback();
    public static native void setObjectRefCallback();

    // --- Functions used by object free tests

    public static native void setObjectFreeCallback();
    public static native void zeroObjectFreeCount();
    public static native int getObjectFreeCount();

    // --- Native methods ---

    private static native int setTag0(Object o, long tag);
    private static native long getTag0(Object o);

    private static native int iterateOverHeap0(int filter_kind);
    private static native int iterateOverInstancesOfClass0(Class c, int filter_kind);

    private static native int iterateOverReachableObjects0();
    private static native int iterateOverObjectsReachableFromObject0(Object o);

    static {
        System.loadLibrary("Heap");
    }
}
