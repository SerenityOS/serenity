/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4904135 4923181
 * @summary Unit test for EnumSet
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @author  Yo Ma Ma
 * @key randomness
 */

import java.util.*;
import java.io.*;

public class EnumSetBash {
    static Random rnd = new Random();

    public static void main(String[] args) {
        bash(Silly0.class);
        bash(Silly1.class);
        bash(Silly31.class);
        bash(Silly32.class);
        bash(Silly33.class);
        bash(Silly63.class);
        bash(Silly64.class);
        bash(Silly65.class);
        bash(Silly127.class);
        bash(Silly128.class);
        bash(Silly129.class);
        bash(Silly500.class);
    }

    static <T extends Enum<T>> void bash(Class<T> enumClass) {
        Enum[] universe = EnumSet.allOf(enumClass).toArray(new Enum[0]);
        int numItr = 1000;

        for (int i=0; i<numItr; i++) {
            EnumSet<T> s1 = EnumSet.noneOf(enumClass);
            EnumSet<T> s2 = clone(s1, enumClass);
            AddRandoms(s1, universe);
            AddRandoms(s2, universe);

            EnumSet<T> intersection = clone(s1, enumClass);
            intersection.retainAll(s2);
            EnumSet<T> diff1 = clone(s1, enumClass); diff1.removeAll(s2);
            EnumSet<T> diff2 = clone(s2, enumClass); diff2.removeAll(s1);
            EnumSet<T> union = clone(s1, enumClass); union.addAll(s2);

            if (diff1.removeAll(diff2))
                fail("Set algebra identity 2 failed");
            if (diff1.removeAll(intersection))
                fail("Set algebra identity 3 failed");
            if (diff2.removeAll(diff1))
                fail("Set algebra identity 4 failed");
            if (diff2.removeAll(intersection))
                fail("Set algebra identity 5 failed");
            if (intersection.removeAll(diff1))
                fail("Set algebra identity 6 failed");
            if (intersection.removeAll(diff1))
                fail("Set algebra identity 7 failed");

            intersection.addAll(diff1); intersection.addAll(diff2);
            if (!intersection.equals(union))
                fail("Set algebra identity 1 failed");

            if (new HashSet<T>(union).hashCode() != union.hashCode())
                fail("Incorrect hashCode computation.");

            Iterator e = union.iterator();
            while (e.hasNext())
                if (!intersection.remove(e.next()))
                    fail("Couldn't remove element from copy.");
            if (!intersection.isEmpty())
                fail("Copy nonempty after deleting all elements.");

            e = union.iterator();
            while (e.hasNext()) {
                Object o = e.next();
                if (!union.contains(o))
                    fail("Set doesn't contain one of its elements.");
                e.remove();
                if (union.contains(o))
                    fail("Set contains element after deletion.");
            }
            if (!union.isEmpty())
                fail("Set nonempty after deleting all elements.");

            s1.clear();
            if (!s1.isEmpty())
                fail("Set nonempty after clear.");
        }
    }

    // Done inefficiently so as to exercise various functions
    static <E extends Enum<E>> EnumSet<E> clone(EnumSet<E> s, Class<E> cl) {
        EnumSet<E> clone = null;
        int method = rnd.nextInt(6);
        switch(method) {
            case 0:
                clone = s.clone();
                break;
            case 1:
                clone = EnumSet.noneOf(cl);
                Collection arrayList = (Collection)Arrays.asList(s.toArray());
                clone.addAll((Collection<E>)arrayList);
                break;
            case 2:
                clone = EnumSet.copyOf(s);
                break;
            case 3:
                clone = EnumSet.copyOf((Collection<E>)s);
                break;
            case 4:
                if (s.isEmpty())
                    clone = EnumSet.copyOf((Collection<E>)s);
                else
                    clone = EnumSet.copyOf((Collection<E>)(Collection)
                                           Arrays.asList(s.toArray()));
                break;
            case 5:
                clone = (EnumSet<E>) deepCopy(s);
        }
        if (!s.equals(clone))
            fail("Set not equal to copy. " + method);
        if (!s.containsAll(clone))
            fail("Set does not contain copy. " + method);
        if (!clone.containsAll(s))
            fail("Copy does not contain set. " + method);
        return clone;
    }

    // Utility method to do a deep copy of an object *very slowly* using
    // serialization/deserialization
    static <T> T deepCopy(T oldObj) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(oldObj);
            oos.flush();
            ByteArrayInputStream bin = new ByteArrayInputStream(
                bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bin);
            return (T) ois.readObject();
        } catch(Exception e) {
            throw new IllegalArgumentException(e.toString());
        }
    }

    static <T extends Enum<T>> void AddRandoms(EnumSet<T> s, Enum[] universe) {
        for (int i=0; i < universe.length * 2 / 3; i++) {
            T e = (T) universe[rnd.nextInt(universe.length)];

            boolean prePresent = s.contains(e);
            int preSize = s.size();
            boolean added = s.add(e);
            if (!s.contains(e))
                fail ("Element not present after addition.");
            if (added == prePresent)
                fail ("added == alreadyPresent");
            int postSize = s.size();
            if (added && preSize == postSize)
                fail ("Add returned true, but size didn't change.");
            if (!added && preSize != postSize)
                fail ("Add returned false, but size changed.");
        }
    }

    static void fail(String s) {
        throw new RuntimeException(s);
    }

    public enum Silly0 { };

    public enum Silly1 { e1 }

    public enum Silly31 {
        e0, e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, e13, e14, e15, e16,
        e17, e18, e19, e20, e21, e22, e23, e24, e25, e26, e27, e28, e29, e30
    }

    public enum Silly32 {
        e0, e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, e13, e14, e15, e16,
        e17, e18, e19, e20, e21, e22, e23, e24, e25, e26, e27, e28, e29, e30, e31
    }

    public enum Silly33 {
        e0, e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, e13, e14, e15, e16,
        e17, e18, e19, e20, e21, e22, e23, e24, e25, e26, e27, e28, e29, e30, e31,
        e32
    }

    public enum Silly63 {
        e0, e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, e13, e14, e15, e16,
        e17, e18, e19, e20, e21, e22, e23, e24, e25, e26, e27, e28, e29, e30, e31,
        e32, e33, e34, e35, e36, e37, e38, e39, e40, e41, e42, e43, e44, e45, e46,
        e47, e48, e49, e50, e51, e52, e53, e54, e55, e56, e57, e58, e59, e60, e61,
        e62
    }

    public enum Silly64 {
        e0, e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, e13, e14, e15, e16,
        e17, e18, e19, e20, e21, e22, e23, e24, e25, e26, e27, e28, e29, e30, e31,
        e32, e33, e34, e35, e36, e37, e38, e39, e40, e41, e42, e43, e44, e45, e46,
        e47, e48, e49, e50, e51, e52, e53, e54, e55, e56, e57, e58, e59, e60, e61,
        e62, e63
    }

    public enum Silly65 {
        e0, e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, e13, e14, e15, e16,
        e17, e18, e19, e20, e21, e22, e23, e24, e25, e26, e27, e28, e29, e30, e31,
        e32, e33, e34, e35, e36, e37, e38, e39, e40, e41, e42, e43, e44, e45, e46,
        e47, e48, e49, e50, e51, e52, e53, e54, e55, e56, e57, e58, e59, e60, e61,
        e62, e63, e64
    }

    public enum Silly127 {
        e0, e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, e13, e14, e15, e16,
        e17, e18, e19, e20, e21, e22, e23, e24, e25, e26, e27, e28, e29, e30, e31,
        e32, e33, e34, e35, e36, e37, e38, e39, e40, e41, e42, e43, e44, e45, e46,
        e47, e48, e49, e50, e51, e52, e53, e54, e55, e56, e57, e58, e59, e60, e61,
        e62, e63, e64, e65, e66, e67, e68, e69, e70, e71, e72, e73, e74, e75, e76,
        e77, e78, e79, e80, e81, e82, e83, e84, e85, e86, e87, e88, e89, e90, e91,
        e92, e93, e94, e95, e96, e97, e98, e99, e100, e101, e102, e103, e104, e105,
        e106, e107, e108, e109, e110, e111, e112, e113, e114, e115, e116, e117,
        e118, e119, e120, e121, e122, e123, e124, e125, e126
    }

    public enum Silly128 {
        e0, e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, e13, e14, e15, e16,
        e17, e18, e19, e20, e21, e22, e23, e24, e25, e26, e27, e28, e29, e30, e31,
        e32, e33, e34, e35, e36, e37, e38, e39, e40, e41, e42, e43, e44, e45, e46,
        e47, e48, e49, e50, e51, e52, e53, e54, e55, e56, e57, e58, e59, e60, e61,
        e62, e63, e64, e65, e66, e67, e68, e69, e70, e71, e72, e73, e74, e75, e76,
        e77, e78, e79, e80, e81, e82, e83, e84, e85, e86, e87, e88, e89, e90, e91,
        e92, e93, e94, e95, e96, e97, e98, e99, e100, e101, e102, e103, e104, e105,
        e106, e107, e108, e109, e110, e111, e112, e113, e114, e115, e116, e117,
        e118, e119, e120, e121, e122, e123, e124, e125, e126, e127
    }

    public enum Silly129 {
        e0, e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, e13, e14, e15, e16,
        e17, e18, e19, e20, e21, e22, e23, e24, e25, e26, e27, e28, e29, e30, e31,
        e32, e33, e34, e35, e36, e37, e38, e39, e40, e41, e42, e43, e44, e45, e46,
        e47, e48, e49, e50, e51, e52, e53, e54, e55, e56, e57, e58, e59, e60, e61,
        e62, e63, e64, e65, e66, e67, e68, e69, e70, e71, e72, e73, e74, e75, e76,
        e77, e78, e79, e80, e81, e82, e83, e84, e85, e86, e87, e88, e89, e90, e91,
        e92, e93, e94, e95, e96, e97, e98, e99, e100, e101, e102, e103, e104, e105,
        e106, e107, e108, e109, e110, e111, e112, e113, e114, e115, e116, e117,
        e118, e119, e120, e121, e122, e123, e124, e125, e126, e127, e128
    }

    public enum Silly500 {
        e0, e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12, e13, e14, e15, e16,
        e17, e18, e19, e20, e21, e22, e23, e24, e25, e26, e27, e28, e29, e30, e31,
        e32, e33, e34, e35, e36, e37, e38, e39, e40, e41, e42, e43, e44, e45, e46,
        e47, e48, e49, e50, e51, e52, e53, e54, e55, e56, e57, e58, e59, e60, e61,
        e62, e63, e64, e65, e66, e67, e68, e69, e70, e71, e72, e73, e74, e75, e76,
        e77, e78, e79, e80, e81, e82, e83, e84, e85, e86, e87, e88, e89, e90, e91,
        e92, e93, e94, e95, e96, e97, e98, e99, e100, e101, e102, e103, e104, e105,
        e106, e107, e108, e109, e110, e111, e112, e113, e114, e115, e116, e117,
        e118, e119, e120, e121, e122, e123, e124, e125, e126, e127, e128, e129,
        e130, e131, e132, e133, e134, e135, e136, e137, e138, e139, e140, e141,
        e142, e143, e144, e145, e146, e147, e148, e149, e150, e151, e152, e153,
        e154, e155, e156, e157, e158, e159, e160, e161, e162, e163, e164, e165,
        e166, e167, e168, e169, e170, e171, e172, e173, e174, e175, e176, e177,
        e178, e179, e180, e181, e182, e183, e184, e185, e186, e187, e188, e189,
        e190, e191, e192, e193, e194, e195, e196, e197, e198, e199, e200, e201,
        e202, e203, e204, e205, e206, e207, e208, e209, e210, e211, e212, e213,
        e214, e215, e216, e217, e218, e219, e220, e221, e222, e223, e224, e225,
        e226, e227, e228, e229, e230, e231, e232, e233, e234, e235, e236, e237,
        e238, e239, e240, e241, e242, e243, e244, e245, e246, e247, e248, e249,
        e250, e251, e252, e253, e254, e255, e256, e257, e258, e259, e260, e261,
        e262, e263, e264, e265, e266, e267, e268, e269, e270, e271, e272, e273,
        e274, e275, e276, e277, e278, e279, e280, e281, e282, e283, e284, e285,
        e286, e287, e288, e289, e290, e291, e292, e293, e294, e295, e296, e297,
        e298, e299, e300, e301, e302, e303, e304, e305, e306, e307, e308, e309,
        e310, e311, e312, e313, e314, e315, e316, e317, e318, e319, e320, e321,
        e322, e323, e324, e325, e326, e327, e328, e329, e330, e331, e332, e333,
        e334, e335, e336, e337, e338, e339, e340, e341, e342, e343, e344, e345,
        e346, e347, e348, e349, e350, e351, e352, e353, e354, e355, e356, e357,
        e358, e359, e360, e361, e362, e363, e364, e365, e366, e367, e368, e369,
        e370, e371, e372, e373, e374, e375, e376, e377, e378, e379, e380, e381,
        e382, e383, e384, e385, e386, e387, e388, e389, e390, e391, e392, e393,
        e394, e395, e396, e397, e398, e399, e400, e401, e402, e403, e404, e405,
        e406, e407, e408, e409, e410, e411, e412, e413, e414, e415, e416, e417,
        e418, e419, e420, e421, e422, e423, e424, e425, e426, e427, e428, e429,
        e430, e431, e432, e433, e434, e435, e436, e437, e438, e439, e440, e441,
        e442, e443, e444, e445, e446, e447, e448, e449, e450, e451, e452, e453,
        e454, e455, e456, e457, e458, e459, e460, e461, e462, e463, e464, e465,
        e466, e467, e468, e469, e470, e471, e472, e473, e474, e475, e476, e477,
        e478, e479, e480, e481, e482, e483, e484, e485, e486, e487, e488, e489,
        e490, e491, e492, e493, e494, e495, e496, e497, e498, e499
    }

}
