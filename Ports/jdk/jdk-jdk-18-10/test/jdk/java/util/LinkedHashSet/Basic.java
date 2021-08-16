/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4245809
 * @summary Basic test for LinkedHashSet.  (Based on SetBash)
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public class Basic {
    static Random rnd = new Random(666);

    public static void main(String[] args) throws Exception {
        int numItr =  500;
        int setSize = 500;

        for (int i=0; i<numItr; i++) {
            Set s1 = new LinkedHashSet();
            AddRandoms(s1, setSize);

            Set s2 = new LinkedHashSet();
            AddRandoms(s2, setSize);

            Set intersection = clone(s1);
            intersection.retainAll(s2);
            Set diff1 = clone(s1); diff1.removeAll(s2);
            Set diff2 = clone(s2); diff2.removeAll(s1);
            Set union = clone(s1); union.addAll(s2);

            if (diff1.removeAll(diff2))
                throw new Exception("Set algebra identity 2 failed");
            if (diff1.removeAll(intersection))
                throw new Exception("Set algebra identity 3 failed");
            if (diff2.removeAll(diff1))
                throw new Exception("Set algebra identity 4 failed");
            if (diff2.removeAll(intersection))
                throw new Exception("Set algebra identity 5 failed");
            if (intersection.removeAll(diff1))
                throw new Exception("Set algebra identity 6 failed");
            if (intersection.removeAll(diff1))
                throw new Exception("Set algebra identity 7 failed");

            intersection.addAll(diff1); intersection.addAll(diff2);
            if (!intersection.equals(union))
                throw new Exception("Set algebra identity 1 failed");

            if (new LinkedHashSet(union).hashCode() != union.hashCode())
                throw new Exception("Incorrect hashCode computation.");

            Iterator e = union.iterator();
            while (e.hasNext())
                if (!intersection.remove(e.next()))
                    throw new Exception("Couldn't remove element from copy.");
            if (!intersection.isEmpty())
                throw new Exception("Copy nonempty after deleting all elements.");

            e = union.iterator();
            while (e.hasNext()) {
                Object o = e.next();
                if (!union.contains(o))
                    throw new Exception("Set doesn't contain one of its elements.");
                e.remove();
                if (union.contains(o))
                    throw new Exception("Set contains element after deletion.");
            }
            if (!union.isEmpty())
                throw new Exception("Set nonempty after deleting all elements.");

            s1.clear();
            if (!s1.isEmpty())
                throw new Exception("Set nonempty after clear.");
        }
        System.err.println("Success.");
    }

    static Set clone(Set s) throws Exception {
        Set clone;
        int method = rnd.nextInt(3);
        clone = (method==0 ? (Set) ((LinkedHashSet)s).clone() :
                 (method==1 ? new LinkedHashSet(Arrays.asList(s.toArray())) :
                  serClone(s)));
        if (!s.equals(clone))
            throw new Exception("Set not equal to copy: "+method);
        if (!s.containsAll(clone))
            throw new Exception("Set does not contain copy.");
        if (!clone.containsAll(s))
            throw new Exception("Copy does not contain set.");
        return clone;
    }

    private static Set serClone(Set m) {
        Set result = null;
        try {
            // Serialize
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(m);
            out.flush();

            // Deserialize
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            out.close();
            ObjectInputStream in = new ObjectInputStream(bis);
            result = (Set)in.readObject();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    static void AddRandoms(Set s, int n) throws Exception {
        for (int i = 0; i < n; i++) {
            Integer e = rnd.nextInt(n);

            int preSize = s.size();
            boolean prePresent = s.contains(e);
            boolean added = s.add(e);
            if (!s.contains(e))
                throw new Exception("Element not present after addition.");
            if (added == prePresent)
                throw new Exception("added == alreadyPresent");
            int postSize = s.size();
            if (added && preSize == postSize)
                throw new Exception("Add returned true, but size didn't change.");
            if (!added && preSize != postSize)
                throw new Exception("Add returned false, but size changed.");
        }
    }
}
