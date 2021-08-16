/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4248728
 * @summary Test ReferenceType.allLineLocations
 * @author Gordon Hirsch
 *
 * @run build TestScaffold VMConnection
 * @run compile -g RefTypes.java
 * @run build AllLineLocations
 *
 * @run driver AllLineLocations RefTypes
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.List;

public class AllLineLocations extends TestScaffold {

    public static void main(String args[]) throws Exception {
        new AllLineLocations(args).startTests();
    }

    AllLineLocations(String args[]) {
        super(args);
    }

    protected void runTests() throws Exception {

        /*
         * Get to a point where the classes are loaded.
         */
        BreakpointEvent bp = startTo("RefTypes", "loadClasses", "()V");
        stepOut(bp.thread());

        /*
         * These classes should have no line numbers, except for
         * one in the implicit constructor.
         */
        ReferenceType rt = findReferenceType("AllAbstract");
        if (rt == null) {
            throw new Exception("AllAbstract: not loaded");
        }
        List list = rt.allLineLocations();
        if (list.size() != 1) {
            throw new Exception("AllAbstract: incorrect number of line locations");
        }
        if (rt.locationsOfLine(5000).size() != 0) {
            throw new Exception("AllAbstract: incorrect locationsOfLine");
        }
        Method method = findMethod(rt, "<init>", "()V");
        if (method == null) {
            throw new Exception("AllAbstract.<init> not found");
        }
        List list2 = method.allLineLocations();
        if (!list2.equals(list)) {
            throw new Exception("AllAbstract: line locations in wrong method");
        }
        if (method.locationsOfLine(5000).size() != 0) {
            throw new Exception("AllAbstract: incorrect locationsOfLine");
        }
        System.out.println("AllAbstract: passed");

        rt = findReferenceType("AllNative");
        if (rt == null) {
            throw new Exception("AllNative: not loaded");
        }
        list = rt.allLineLocations();
        if (list.size() != 1) {
            throw new Exception("AllNative: incorrect number of line locations");
        }
        if (rt.locationsOfLine(5000).size() != 0) {
            throw new Exception("AllNative: incorrect locationsOfLine");
        }
        method = findMethod(rt, "<init>", "()V");
        if (method == null) {
            throw new Exception("AllNative.<init> not found");
        }
        list2 = method.allLineLocations();
        if (!list2.equals(list)) {
            throw new Exception("AllNative: line locations in wrong method");
        }
        if (method.locationsOfLine(5000).size() != 0) {
            throw new Exception("AllNative: incorrect locationsOfLine");
        }
        System.out.println("AllNative: passed");

        rt = findReferenceType("Interface");
        if (rt == null) {
            throw new Exception("Interface: not loaded");
        }
        list = rt.allLineLocations();
        if (list.size() != 0) {
            throw new Exception("Interface: locations reported for abstract methods");
        }
        System.out.println("Interface: passed");

        /*
         * These classes have line numbers in one method and
         * in the implicit constructor.
         */
        rt = findReferenceType("Abstract");
        if (rt == null) {
            throw new Exception("Abstract: not loaded");
        }
        list = rt.allLineLocations();
        if (list.size() != 5) {
            throw new Exception("Abstract: incorrect number of line locations");
        }
        method = findMethod(rt, "b", "()V");
        if (method == null) {
            throw new Exception("Abstract.b not found");
        }
        list2 = method.allLineLocations();
        list.removeAll(list2);

        // Remaining location should be in constructor
        if ((list.size() != 1) ||
            !(((Location)list.get(0)).method().name().equals("<init>"))) {
            throw new Exception("Abstract: line locations in wrong method");
        }
        if (method.locationsOfLine(20).size() != 1) {
            throw new Exception("Abstract method: incorrect locationsOfLine");
        }
        if (method.locationsOfLine(5000).size() != 0) {
            throw new Exception("Abstract method: incorrect locationsOfLine");
        }
        method = findMethod(rt, "a", "()V");
        if (method.locationsOfLine(5000).size() != 0) {
            throw new Exception("Abstract method: incorrect locationsOfLine");
        }
        System.out.println("Abstract: passed");

        rt = findReferenceType("Native");
        if (rt == null) {
            throw new Exception("Native: not loaded");
        }
        list = rt.allLineLocations();
        if (list.size() != 5) {
            throw new Exception("Native: incorrect number of line locations");
        }
        if (rt.locationsOfLine(5000).size() != 0) {
            throw new Exception("Native: incorrect locationsOfLine");
        }
        method = findMethod(rt, "b", "()V");
        if (method == null) {
            throw new Exception("Native.b not found");
        }
        list2 = method.allLineLocations();
        list.removeAll(list2);

        // Remaining location should be in constructor
        if ((list.size() != 1) ||
            !(((Location)list.get(0)).method().name().equals("<init>"))) {
            throw new Exception("Native: line locations in wrong method");
        }
        if (method.locationsOfLine(30).size() != 1) {
            throw new Exception("Native method: incorrect locationsOfLine");
        }
        if (method.locationsOfLine(5000).size() != 0) {
            throw new Exception("Native method: incorrect locationsOfLine");
        }
        method = findMethod(rt, "a", "()V");
        if (method.locationsOfLine(5000).size() != 0) {
            throw new Exception("Native method: incorrect locationsOfLine");
        }
        System.out.println("Native: passed");

        rt = findReferenceType("AbstractAndNative");
        if (rt == null) {
            throw new Exception("AbstractAndNative: not loaded");
        }
        list = rt.allLineLocations();
        if (list.size() != 5) {
            throw new Exception("AbstractAndNative: incorrect number of line locations");
        }
        if (rt.locationsOfLine(5000).size() != 0) {
            throw new Exception("AbstractAndNative: incorrect locationsOfLine");
        }
        method = findMethod(rt, "c", "()V");
        if (method == null) {
            throw new Exception("AbstractAndNative.c not found");
        }
        list2 = method.allLineLocations();
        list.removeAll(list2);

        // Remaining location should be in constructor
        if ((list.size() != 1) ||
            !(((Location)list.get(0)).method().name().equals("<init>"))) {
            throw new Exception("AbstractAndNative: line locations in wrong method");
        }
        System.out.println("AbstractAndNative: passed");

        // Allow application to complete
        resumeToVMDisconnect();
    }
}
