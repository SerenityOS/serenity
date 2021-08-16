/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5015676 4987888 4997464
 * @summary Testing upper bounds and availability of toString methods
 * @author Joseph D. Darcy
 */

import java.lang.reflect.*;
import java.util.List;
import java.util.Collection;

class A<T> {
    class B<U> {}
}

class Test<T> {
    static class Inner1<U> {
        void bar(U[] array1) { return;}
    }

    class Inner2<V> {
        List<?> foo2(List<? extends V> t) {
            return null;
        }
    }

    static <S extends Object & Comparable<? super S> > S max(Collection<? extends S> coll) {
        return null;
    }

    List<? extends T> foo(List<? super T> t) {
        return null;
    }
}

public class StringsAndBounds {
    public void f(A<String>.B<Integer> x) {
    }

    public <T>  void g(T a) {return ;}

    static void scanner(Class clazz) {
        System.out.println("\n\nScanning " + clazz.getName());

        for(Class c: clazz.getDeclaredClasses()) {
            scanner(c);
        }

        for(Method m: clazz.getDeclaredMethods()) {
            System.out.println("\nMethod:\t" + m.toString()); // Need toGenericString?
            System.out.println("\tReturn Type: " + m.getGenericReturnType().toString() );
            for(Type p: m.getGenericParameterTypes()) {
                if (p instanceof WildcardType) { // Check upper bounds
                    Type[] upperBounds = ((WildcardType)p).getUpperBounds();
                    if (upperBounds.length < 1 ||
                        upperBounds[0] == null)
                        throw new RuntimeException("Malformed upper bounds: " + p);

                }

                System.out.println("\tParameter: " + p.toString());
            }
        }
    }

    public static void main(String[] argv) throws Exception {
        scanner(StringsAndBounds.class);
        scanner(A.B.class);
        scanner(Test.class);
    }
}
