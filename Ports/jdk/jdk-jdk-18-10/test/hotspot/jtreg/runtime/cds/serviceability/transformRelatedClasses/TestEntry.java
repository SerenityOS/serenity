/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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


// Test Entry - a single entry in a test table
// that defines a test case
// See TransformRelatedClasses.java for more details
public class TestEntry {
    int testCaseId;
    boolean transformParent;
    boolean transformChild;
    boolean isParentExpectedShared;
    boolean isChildExpectedShared;

    public TestEntry(int testCaseId,
                     boolean transformParent, boolean transformChild,
                     boolean isParentExpectedShared, boolean isChildExpectedShared) {
        this.testCaseId = testCaseId;
        this.transformParent = transformParent;
        this.transformChild = transformChild;
        this.isParentExpectedShared = isParentExpectedShared;
        this.isChildExpectedShared = isChildExpectedShared;
    }
}

