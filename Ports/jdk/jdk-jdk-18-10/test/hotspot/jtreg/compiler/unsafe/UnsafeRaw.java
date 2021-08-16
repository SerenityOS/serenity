/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 * @bug 8058744
 * @summary Invalid pattern-matching of address computations in raw unsafe
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main/othervm -Xbatch compiler.unsafe.UnsafeRaw
 */

package compiler.unsafe;

import jdk.internal.misc.Unsafe;
import jdk.test.lib.Utils;

import java.util.Random;

public class UnsafeRaw {
  public static class Tests {
    public static int int_index(Unsafe unsafe, long base, int index) throws Exception {
      return unsafe.getInt(base + (index << 2));
    }
    public static int long_index(Unsafe unsafe, long base, long index) throws Exception {
      return unsafe.getInt(base + (index << 2));
    }
    public static int int_index_back_ashift(Unsafe unsafe, long base, int index) throws Exception {
      return unsafe.getInt(base + (index >> 2));
    }
    public static int int_index_back_lshift(Unsafe unsafe, long base, int index) throws Exception {
      return unsafe.getInt(base + (index >>> 2));
    }
    public static int long_index_back_ashift(Unsafe unsafe, long base, long index) throws Exception {
      return unsafe.getInt(base + (index >> 2));
    }
    public static int long_index_back_lshift(Unsafe unsafe, long base, long index) throws Exception {
      return unsafe.getInt(base + (index >>> 2));
    }
    public static int int_const_12345678_index(Unsafe unsafe, long base) throws Exception {
      int idx4 = 0x12345678;
      return unsafe.getInt(base + idx4);
    }
    public static int long_const_1234567890abcdef_index(Unsafe unsafe, long base) throws Exception {
      long idx5 = 0x1234567890abcdefL;
      return unsafe.getInt(base + idx5);
    }
    public static int int_index_mul(Unsafe unsafe, long base, int index) throws Exception {
      return unsafe.getInt(base + (index * 4));
    }
    public static int long_index_mul(Unsafe unsafe, long base, long index) throws Exception {
      return unsafe.getInt(base + (index * 4));
    }
    public static int int_index_mul_scale_16(Unsafe unsafe, long base, int index) throws Exception {
      return unsafe.getInt(base + (index * 16));
    }
    public static int long_index_mul_scale_16(Unsafe unsafe, long base, long index) throws Exception {
      return unsafe.getInt(base + (index * 16));
    }
  }

  public static void main(String[] args) throws Exception {
    Unsafe unsafe = Unsafe.getUnsafe();
    final int array_size = 128;
    final int element_size = 4;
    final int magic = 0x12345678;

    Random rnd = Utils.getRandomInstance();

    long array = unsafe.allocateMemory(array_size * element_size); // 128 ints
    long addr = array + array_size * element_size / 2; // something in the middle to work with
    unsafe.putInt(addr, magic);
    for (int j = 0; j < 100000; j++) {
       if (Tests.int_index(unsafe, addr, 0) != magic) throw new Exception();
       if (Tests.long_index(unsafe, addr, 0) != magic) throw new Exception();
       if (Tests.int_index_mul(unsafe, addr, 0) != magic) throw new Exception();
       if (Tests.long_index_mul(unsafe, addr, 0) != magic) throw new Exception();
       {
         long idx1 = rnd.nextLong();
         long addr1 = addr - (idx1 << 2);
         if (Tests.long_index(unsafe, addr1, idx1) != magic) throw new Exception();
       }
       {
         long idx2 = rnd.nextLong();
         long addr2 = addr - (idx2 >> 2);
         if (Tests.long_index_back_ashift(unsafe, addr2, idx2) != magic) throw new Exception();
       }
       {
         long idx3 = rnd.nextLong();
         long addr3 = addr - (idx3 >>> 2);
         if (Tests.long_index_back_lshift(unsafe, addr3, idx3) != magic) throw new Exception();
       }
       {
         long idx4 = 0x12345678;
         long addr4 = addr - idx4;
         if (Tests.int_const_12345678_index(unsafe, addr4) != magic) throw new Exception();
       }
       {
         long idx5 = 0x1234567890abcdefL;
         long addr5 = addr - idx5;
         if (Tests.long_const_1234567890abcdef_index(unsafe, addr5) != magic) throw new Exception();
       }
       {
         int idx6 = rnd.nextInt();
         long addr6 = addr - (idx6 >> 2);
         if (Tests.int_index_back_ashift(unsafe, addr6, idx6) != magic) throw new Exception();
       }
       {
         int idx7 = rnd.nextInt();
         long addr7 = addr - (idx7 >>> 2);
         if (Tests.int_index_back_lshift(unsafe, addr7, idx7) != magic) throw new Exception();
       }
       {
         int idx8 = rnd.nextInt();
         long addr8 = addr - (idx8 * 16);
         if (Tests.int_index_mul_scale_16(unsafe, addr8, idx8) != magic) throw new Exception();
       }
       {
         long idx9 = rnd.nextLong();
         long addr9 = addr - (idx9 * 16);
         if (Tests.long_index_mul_scale_16(unsafe, addr9, idx9) != magic) throw new Exception();
       }
    }
  }
}
