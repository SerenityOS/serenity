/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4189641
 * @summary Wrapping a null collection/array should blow up sooner
 *          rather than later
 */

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class WrappedNull {
    public static void main(String[] args) throws Exception {
        boolean testSucceeded = false;
        try {
            List l = Arrays.asList(null);
        }
        catch (NullPointerException e) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("Arrays.asList");

        testSucceeded = false;
        try {
            Collection c = Collections.unmodifiableCollection(null);
        }
        catch (NullPointerException e) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("unmodifiableCollection");

        testSucceeded = false;
        try {
            Set c = Collections.unmodifiableSet(null);
        }
        catch (NullPointerException e) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("unmodifiableSet");

        testSucceeded = false;
        try {
            List c = Collections.unmodifiableList(null);
        }
        catch (NullPointerException e) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("unmodifiableList");

        testSucceeded = false;
        try {
            Map c = Collections.unmodifiableMap(null);
        }
        catch (NullPointerException e) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("unmodifiableMap");

        testSucceeded = false;
        try {
            SortedSet c = Collections.unmodifiableSortedSet(null);
        }
        catch (NullPointerException e) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("unmodifiableSortedSet");

        testSucceeded = false;
        try {
            SortedMap c = Collections.unmodifiableSortedMap(null);
        }
        catch (NullPointerException e) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("unmodifiableSortedMap");

        testSucceeded = false;
        try {
            Collection c = Collections.synchronizedCollection(null);
        }
        catch (NullPointerException e) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("synchronizedCollection");

        testSucceeded = false;
        try {
            Set c = Collections.synchronizedSet(null);
        }
        catch (NullPointerException e) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("synchronizedSet");

        testSucceeded = false;
        try {
            List c = Collections.synchronizedList(null);
        }
        catch (NullPointerException e) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("synchronizedList");

        testSucceeded = false;
        try {
            Map c = Collections.synchronizedMap(null);
        }
        catch (NullPointerException e) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("synchronizedMap");

        testSucceeded = false;
        try {
            SortedSet c = Collections.synchronizedSortedSet(null);
        }
        catch (NullPointerException e) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("synchronizedSortedSet");

        testSucceeded = false;
        try {
            SortedMap c = Collections.synchronizedSortedMap(null);
        }
        catch (NullPointerException e) {
            testSucceeded = true;
        }
        if (!testSucceeded)
            throw new Exception("synchronizedSortedMap");

        // Make sure that non-null arguments don't throw exc.
        List l = Arrays.asList(new Object[0]);
        Collection c = Collections.unmodifiableCollection(
                Collections.EMPTY_SET);
        Set s = Collections.unmodifiableSet(Collections.EMPTY_SET);
        l = Collections.unmodifiableList(Collections.EMPTY_LIST);
        Map m = Collections.unmodifiableMap(Collections.EMPTY_MAP);
        SortedSet ss = Collections.unmodifiableSortedSet(new TreeSet());
        SortedMap sm = Collections.unmodifiableSortedMap(new TreeMap());

        c = Collections.synchronizedCollection(Collections.EMPTY_SET);
        s = Collections.synchronizedSet(Collections.EMPTY_SET);
        l = Collections.synchronizedList(Collections.EMPTY_LIST);
        m = Collections.synchronizedMap(Collections.EMPTY_MAP);
        ss = Collections.synchronizedSortedSet(new TreeSet());
        sm = Collections.synchronizedSortedMap(new TreeMap());
    }
}
