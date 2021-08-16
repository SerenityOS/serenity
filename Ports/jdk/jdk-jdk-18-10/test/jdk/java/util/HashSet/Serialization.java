/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/*
 * @test
 * @bug 8016252
 * @summary Verify that a serialized HashSet may successfully be deserialized.
 * @key randomness
 */
public class Serialization {

    private static final int NUM_SETS = 43;
    private static final int MAX_CAPACITY = 257;
    private static final float MAX_LOAD_FACTOR = 100.0F;

    private static final Random rnd = ThreadLocalRandom.current();

    private static HashSet<Integer> createHashSet() {
        int capacity = rnd.nextInt(MAX_CAPACITY);
        float loadFactor = Float.MIN_VALUE + rnd.nextFloat()*MAX_LOAD_FACTOR;
        HashSet<Integer> hashSet = new HashSet<Integer>(capacity, loadFactor);
        float multiplier = 2*rnd.nextFloat(); // range [0,2]
        int size = (int)(capacity*loadFactor*multiplier);
        for (int i = 0; i < size; i++) {
            hashSet.add(rnd.nextInt());
        }
        return hashSet;
    }

    private static HashSet<Integer> serDeser(HashSet<Integer> hashSet) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(hashSet);
        oos.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        HashSet<Integer> result = (HashSet<Integer>)ois.readObject();

        oos.close();
        ois.close();

        return result;
    }

    private static void printHashSet(HashSet<Integer> hashSet) {
        System.err.println("Size: "+hashSet.size());
        for (Object o : hashSet) {
            System.err.println(o);
        }
    }

    public static void main(String[] args) {
        int failures = 0;

        for (int i = 0; i < NUM_SETS; i++) {
            HashSet<Integer> hashSet = createHashSet();

            HashSet<Integer> result = null;
            try {
                result = serDeser(hashSet);
            } catch (IOException ioe) {
                System.err.println(ioe);
                failures++;
            } catch (ClassNotFoundException cnfe) {
                System.err.println(cnfe);
                failures++;
            }

            if (!hashSet.equals(result)) {
                System.err.println("Unequal HashSets!");
                printHashSet(hashSet);
                System.err.println();
                failures++;
            }
        }

        if (failures != 0) {
            throw new RuntimeException("HashSet/Serialzation failed with "+
                    failures+" failures!");
        }
    }
}
