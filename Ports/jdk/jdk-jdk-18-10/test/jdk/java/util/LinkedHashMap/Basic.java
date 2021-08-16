/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4245809 8029795
 * @summary Basic test for LinkedHashMap.  (Based on MapBash)
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;

public class Basic {
    static final Random rnd = new Random(666);
    static final Integer nil = new Integer(0);

    public static void main(String[] args)  throws Exception {
        int numItr =  500;
        int mapSize = 500;

        // Linked List testk
        for (int i=0; i<numItr; i++) {
            Map<Integer,Integer> m = new LinkedHashMap();
            Integer head = nil;

            for (int j=0; j<mapSize; j++) {
                Integer newHead;
                do {
                    newHead = new Integer(rnd.nextInt());
                } while (m.containsKey(newHead));
                m.put(newHead, head);
                head = newHead;
            }
            if (m.size() != mapSize)
                throw new Exception("Size not as expected.");

            if (new HashMap(m).hashCode() != m.hashCode())
                throw new Exception("Incorrect hashCode computation.");

            Map<Integer,Integer> m2 = new LinkedHashMap(); m2.putAll(m);
            m2.values().removeAll(m.keySet());
            if (m2.size()!= 1 || !m2.containsValue(nil))
                throw new Exception("Collection views test failed.");

            int j=0;
            while (head != nil) {
                if (!m.containsKey(head))
                    throw new Exception("Linked list doesn't contain a link.");
                Integer newHead = m.get(head);
                if (newHead == null)
                    throw new Exception("Could not retrieve a link.");
                m.remove(head);
                head = newHead;
                j++;
            }
            if (!m.isEmpty())
                throw new Exception("Map nonempty after removing all links.");
            if (j != mapSize)
                throw new Exception("Linked list size not as expected.");
        }

        Map<Integer,Integer> m = new LinkedHashMap();
        for (int i=0; i<mapSize; i++)
            if (m.put(new Integer(i), new Integer(2*i)) != null)
                throw new Exception("put returns non-null value erroneously.");
        for (int i=0; i<2*mapSize; i++)
            if (m.containsValue(new Integer(i)) != (i%2==0))
                throw new Exception("contains value "+i);
        if (m.put(nil, nil) == null)
            throw new Exception("put returns a null value erroneously.");
        Map<Integer,Integer> m2 = new LinkedHashMap(); m2.putAll(m);
        if (!m.equals(m2))
            throw new Exception("Clone not equal to original. (1)");
        if (!m2.equals(m))
            throw new Exception("Clone not equal to original. (2)");
        Set<Map.Entry<Integer,Integer>> s = m.entrySet(), s2 = m2.entrySet();
        if (!s.equals(s2))
            throw new Exception("Clone not equal to original. (3)");
        if (!s2.equals(s))
            throw new Exception("Clone not equal to original. (4)");
        if (!s.containsAll(s2))
            throw new Exception("Original doesn't contain clone!");
        if (!s2.containsAll(s))
            throw new Exception("Clone doesn't contain original!");

        m2 = serClone(m);
        if (!m.equals(m2))
            throw new Exception("Serialize Clone not equal to original. (1)");
        if (!m2.equals(m))
            throw new Exception("Serialize Clone not equal to original. (2)");
        s = m.entrySet(); s2 = m2.entrySet();
        if (!s.equals(s2))
            throw new Exception("Serialize Clone not equal to original. (3)");
        if (!s2.equals(s))
            throw new Exception("Serialize Clone not equal to original. (4)");
        if (!s.containsAll(s2))
            throw new Exception("Original doesn't contain Serialize clone!");
        if (!s2.containsAll(s))
            throw new Exception("Serialize Clone doesn't contain original!");

        s2.removeAll(s);
        if (!m2.isEmpty())
            throw new Exception("entrySet().removeAll failed.");

        m2.putAll(m);
        m2.clear();
        if (!m2.isEmpty())
            throw new Exception("clear failed.");

        Iterator it = m.entrySet().iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        if (!m.isEmpty())
            throw new Exception("Iterator.remove() failed");

        // Test ordering properties with insert order
        m = new LinkedHashMap();
        List<Integer> l = new ArrayList(mapSize);
        for (int i=0; i<mapSize; i++) {
            Integer x = new Integer(i);
            m.put(x, x);
            l.add(x);
        }
        if (!new ArrayList(m.keySet()).equals(l))
            throw new Exception("Insertion order not preserved.");
        for (int i=mapSize-1; i>=0; i--) {
            Integer x = (Integer) l.get(i);
            if (!m.get(x).equals(x))
                throw new Exception("Wrong value: "+i+", "+m.get(x)+", "+x);
        }
        if (!new ArrayList(m.keySet()).equals(l))
            throw new Exception("Insertion order not preserved after read.");

        for (int i=mapSize-1; i>=0; i--) {
            Integer x = (Integer) l.get(i);
            m.put(x, x);
        }
        if (!new ArrayList(m.keySet()).equals(l))
            throw new Exception("Insert order not preserved after reinsert.");

        m2 = (Map) ((LinkedHashMap)m).clone();
        if (!m.equals(m2))
            throw new Exception("Insert-order Map != clone.");

        List<Integer> l2 = new ArrayList(l);
        Collections.shuffle(l2);
        for (int i=0; i<mapSize; i++) {
            Integer x = (Integer) l2.get(i);
            if (!m2.get(x).equals(x))
              throw new Exception("Clone: Wrong val: "+i+", "+m.get(x)+", "+x);
        }
        if (!new ArrayList(m2.keySet()).equals(l))
            throw new Exception("Clone: altered by read.");

        // Test ordering properties with access order
        m = new LinkedHashMap(2*mapSize, .75f, true);
        for (int i=0; i<mapSize; i++) {
            Integer x = new Integer(i);
            m.put(x, x);
        }
        if (!new ArrayList(m.keySet()).equals(l))
            throw new Exception("Insertion order not properly preserved.");

        for (int i=0; i<mapSize; i++) {
            Integer x = (Integer) l2.get(i);
            if (!m.get(x).equals(x))
                throw new Exception("Wrong value: "+i+", "+m.get(x)+", "+x);
        }
        if (!new ArrayList(m.keySet()).equals(l2))
            throw new Exception("Insert order not properly altered by read.");

        for (int i=0; i<mapSize; i++) {
            Integer x = (Integer) l2.get(i);
            if (!m.getOrDefault(x, new Integer(i + 1000)).equals(x))
                throw new Exception("Wrong value: "+i+", "+m.get(x)+", "+x);
        }
        if (!new ArrayList(m.keySet()).equals(l2))
            throw new Exception("Insert order not properly altered by read.");

        for (int i=0; i<mapSize; i++) {
            Integer x = (Integer) l2.get(i);
            if (!m.replace(x, x).equals(x))
                throw new Exception("Wrong value: "+i+", "+m.get(x)+", "+x);
        }
        if (!new ArrayList(m.keySet()).equals(l2))
            throw new Exception("Insert order not properly altered by replace.");

        for (int i=0; i<mapSize; i++) {
            Integer x = (Integer) l2.get(i);
            if (!m.replace(x, x, x))
                throw new Exception("Wrong value: "+i+", "+m.get(x)+", "+x);
        }
        if (!new ArrayList(m.keySet()).equals(l2))
            throw new Exception("Insert order not properly altered by replace.");

        BiFunction<Integer,Integer,Integer> f = (Integer y, Integer z) -> {
            if (!Objects.equals(y,z))
                throw new RuntimeException("unequal " + y + "," + z);
            return new Integer(z);
        };

        for (int i=0; i<mapSize; i++) {
            Integer x = (Integer) l2.get(i);
            if (!x.equals(m.merge(x, x, f)))
                throw new Exception("Wrong value: "+i+", "+m.get(x)+", "+x);
        }
        if (!new ArrayList(m.keySet()).equals(l2))
            throw new Exception("Insert order not properly altered by replace.");

        for (int i=0; i<mapSize; i++) {
            Integer x = (Integer) l2.get(i);
            if (!x.equals(m.compute(x, f)))
                throw new Exception("Wrong value: "+i+", "+m.get(x)+", "+x);
        }
        if (!new ArrayList(m.keySet()).equals(l2))
            throw new Exception("Insert order not properly altered by replace.");

        for (int i=0; i<mapSize; i++) {
            Integer x = (Integer) l2.get(i);
            if (!x.equals(m.remove(x)))
                throw new Exception("Missing key: "+i+", "+x);
            if (!x.equals(m.computeIfAbsent(x, Integer::valueOf)))
                throw new Exception("Wrong value: "+i+", "+m.get(x)+", "+x);
        }
        if (!new ArrayList(m.keySet()).equals(l2))
            throw new Exception("Insert order not properly altered by replace.");

        for (int i=0; i<mapSize; i++) {
            Integer x = (Integer) l2.get(i);
            if (!x.equals(m.computeIfPresent(x, f)))
                throw new Exception("Wrong value: "+i+", "+m.get(x)+", "+x);
        }
        if (!new ArrayList(m.keySet()).equals(l2))
            throw new Exception("Insert order not properly altered by replace.");

        for (int i=0; i<mapSize; i++) {
            Integer x = new Integer(i);
            m.put(x, x);
        }
        if (!new ArrayList(m.keySet()).equals(l))
            throw new Exception("Insertion order not altered by reinsert.");

        m2 = (Map) ((LinkedHashMap)m).clone();
        if (!m.equals(m2))
            throw new Exception("Access-order Map != clone.");
        for (int i=0; i<mapSize; i++) {
            Integer x = (Integer) l.get(i);
            if (!m2.get(x).equals(x))
              throw new Exception("Clone: Wrong val: "+i+", "+m.get(x)+", "+x);
        }
        if (!new ArrayList(m2.keySet()).equals(l))
            throw new Exception("Clone: order not properly altered by read.");

        System.err.println("Success.");
    }

    private static Map serClone(Map m) {
        Map result = null;
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
            result = (Map)in.readObject();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
