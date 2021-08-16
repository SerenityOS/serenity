/*
 * Copyright (c) 1997, 1998, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4093817 4189594
   @summary Test for an illegalargumentexception on loadFactor
*/

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * This class tests to see if creating a hash table with an
 * illegal value of loadFactor results in an IllegalArgumentException
 */
public class IllegalLoadFactor {

    public static void main(String[] args) throws Exception {
        boolean testSucceeded = false;
        try {
            // this should generate an IllegalArgumentException
            Hashtable bad1 = new Hashtable(100, -3);
        }
        catch (IllegalArgumentException e1) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("Hashtable, negative load factor");

        testSucceeded = false;
        try {
            // this should generate an IllegalArgumentException
            Hashtable bad1 = new Hashtable(100, Float.NaN);
        }
        catch (IllegalArgumentException e1) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("Hashtable, NaN load factor");

        testSucceeded = false;
        try {
            // this should generate an IllegalArgumentException
            HashMap bad1 = new HashMap(100, -3);
        }
        catch (IllegalArgumentException e1) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("HashMap, negative load factor");

        testSucceeded = false;
        try {
            // this should generate an IllegalArgumentException
            HashMap bad1 = new HashMap(100, Float.NaN);
        }
        catch (IllegalArgumentException e1) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("HashMap, NaN load factor");


        testSucceeded = false;
        try {
            // this should generate an IllegalArgumentException
            HashSet bad1 = new HashSet(100, -3);
        }
        catch (IllegalArgumentException e1) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("HashSet, negative load factor");

        testSucceeded = false;
        try {
            // this should generate an IllegalArgumentException
            HashSet bad1 = new HashSet(100, Float.NaN);
        }
        catch (IllegalArgumentException e1) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("HashSet, NaN load factor");

        testSucceeded = false;
        try {
            // this should generate an IllegalArgumentException
            WeakHashMap bad1 = new WeakHashMap(100, -3);
        }
        catch (IllegalArgumentException e1) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("WeakHashMap, negative load factor");

        testSucceeded = false;
        try {
            // this should generate an IllegalArgumentException
            WeakHashMap bad1 = new WeakHashMap(100, Float.NaN);
        }
        catch (IllegalArgumentException e1) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("WeakHashMap, NaN load factor");

        // Make sure that legal creates don't throw exceptions
        Map goodMap = new Hashtable(100, .69f);
        goodMap = new HashMap(100, .69f);
        Set goodSet = new HashSet(100, .69f);
        goodMap = new WeakHashMap(100, .69f);
    }
}
