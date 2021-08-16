/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4593209 8001667
 * @summary Reverse comparator was subtly broken
 * @author Josh Bloch
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReverseOrder {
    static byte[] serialBytes(Object o) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(o);
            oos.flush();
            oos.close();
            return bos.toByteArray();
        } catch (Throwable t) {
            throw new Error(t);
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T serialClone(T o) {
        try {
            ObjectInputStream ois = new ObjectInputStream
                (new ByteArrayInputStream(serialBytes(o)));
            T clone = (T) ois.readObject();
            return clone;
        } catch (Throwable t) {
            throw new Error(t);
        }
    }

    public static void main(String[] args) throws Exception {
        Foo[] a = { new Foo(2), new Foo(3), new Foo(1) };
        List list = Arrays.asList(a);
        Comparator cmp = Collections.reverseOrder();
        Collections.sort(list, cmp);

        Foo[] golden = { new Foo(3), new Foo(2), new Foo(1) };
        List goldenList = Arrays.asList(golden);
        if (!list.equals(goldenList))
            throw new Exception(list.toString());

        Comparator clone = serialClone(cmp);
        List list2 = Arrays.asList(a);
        Collections.sort(list2, clone);
        if (!list2.equals(goldenList))
            throw new Exception(list.toString());
    }
}

class Foo implements Comparable {
    int val;
    Foo(int i) { val = i; }

    public int compareTo(Object o) {
        Foo f = (Foo)o;
        return (val < f.val ? Integer.MIN_VALUE : (val == f.val ? 0 : 1));
    }

    public boolean equals(Object o) {
        return o instanceof Foo && ((Foo)o).val == val;
    }

    public int hashCode()    { return val; }

    public String toString() { return Integer.toString(val); }
}
