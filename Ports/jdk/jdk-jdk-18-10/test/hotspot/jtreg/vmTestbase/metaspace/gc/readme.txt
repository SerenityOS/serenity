 Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.

 This code is free software; you can redistribute it and/or modify it
 under the terms of the GNU General Public License version 2 only, as
 published by the Free Software Foundation.

 This code is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 version 2 for more details (a copy is included in the LICENSE file that
 accompanied this code).

 You should have received a copy of the GNU General Public License version
 2 along with this work; if not, write to the Free Software Foundation,
 Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.

 Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 or visit www.oracle.com if you need additional information or have any
 questions.



metaspace/gc tests - are the test for the Metaspace GC tuning, which is described in the
Java SE 8 HotSpot[tm] Virtual Machine Garbage  Collection Tuning

Tests load classes and monitor the used/committed amounts of metaspace.
There are three types of tests all extending base class - MetaspaceBaseGC

MemoryUsageTest -
  trivial test to check memory dynamic (loading classes should lead to growth
  of used memory, gc to reduce)

FirstGCTest -
  loads classes until the GC has happened and check the GC has happened at the
  right moment (as stated in the Spec)

HighWaterMarkTest
  The test loads classes until the committed metaspace achieves the certain
  level between MetaspaceSize and MaxMetaspaceSize.
  Then it counts how many times GC has been induced.
  Test verifies that MinMetaspaceFreeRatio/MaxMetaspaceFreeRatio settings
  affect the frequency of GC. (High-water mark)

Note: GC could be caused not by Metaspace. When GC happens, the tests check
gc.log file to find the reason of GC. Tests will pass if there is no guarantee
that GC was induced by metaspace.
