/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6851282
 * @summary JIT miscompilation results in null entry in array when using CompressedOops
 *
 * @run main/othervm/timeout=600 -Xmx256m -XX:+IgnoreUnrecognizedVMOptions -XX:+UseCompressedOops
 *    compiler.c2.Test6851282
 */

package compiler.c2;

import java.util.ArrayList;
import java.util.List;

public class Test6851282 {
  void foo(A a, A[] as) {
    for (A a1 : as) {
      B[] filtered = a.c(a1);
      for (B b : filtered) {
        if (b == null) {
          System.out.println("bug: b == null");
          System.exit(97);
        }
      }
    }
  }

  public static void main(String[] args) {
    List<A> as = new ArrayList<A>();
    for (int i = 0; i < 5000; i++) {
      List<B> bs = new ArrayList<B>();
      for (int j = i; j < i + 1000; j++)
        bs.add(new B(j));
      as.add(new A(bs.toArray(new B[0])));
    }
    new Test6851282().foo(as.get(0), as.subList(1, as.size()).toArray(new A[0]));
  }

  static class A {
    final B[] bs;

    public A(B[] bs) {
      this.bs = bs;
    }

    final B[] c(final A a) {
      return new BoxedArray<B>(bs).filter(new Function<B, Boolean>() {
        public Boolean apply(B arg) {
          for (B b : a.bs) {
            if (b.d == arg.d)
              return true;
          }
          return false;
        }
      });
    }
  }

  static class BoxedArray<T> {

    private final T[] array;

    BoxedArray(T[] array) {
      this.array = array;
    }

    public T[] filter(Function<T, Boolean> function) {
      boolean[] include = new boolean[array.length];
      int len = 0;
      int i = 0;
      while (i < array.length) {
        if (function.apply(array[i])) {
          include[i] = true;
          len += 1;
        }
        i += 1;
      }
      T[] result = (T[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), len);
      len = 0;
      i = 0;
      while (len < result.length) {
        if (include[i]) {
          result[len] = array[i];
          len += 1;
        }
        i += 1;
      }
      return result;
    }
  }

  static interface Function<T, R> {
    R apply(T arg);
  }

  static class B {
    final int d;

    public B(int d) {
      this.d = d;
    }
  }
}

