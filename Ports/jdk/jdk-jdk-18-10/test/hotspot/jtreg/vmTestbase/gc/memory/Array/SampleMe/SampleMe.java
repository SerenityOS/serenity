/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase gc/memory/Array/SampleMe.
 * VM Testbase keywords: [gc]
 * VM Testbase readme:
 * ## This test is a basic implementation of the thoughts below.
 * Create an object: Cell
 * which contains an array of bytes, the size of
 * which is determined in the ctor call
 * You could then have varying sizes of these objects.
 * You could then store them in an array. Then delete
 * a subset and garbage collect:
 * Basically, you are looking at the holes and the
 * remaining cells, their numbers and their locations:
 * small/medium/large sized chunks
 * small/medium/large sized holes
 * contiguous/mixed/discontiguous
 * none, few, some, many, all
 * The key here is to provide a broad spectrum of
 * various GC scenarios. eventually, you will want
 * to take into account generational effects by making
 * sure that various portions of the chunks and holes
 * are created before or after certain generational
 * boundaries (1 Megabyte of allocs, for example).
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.memory.Array.SampleMe.SampleMe
 */

package gc.memory.Array.SampleMe;

//SampleMe.java
// import SampleClass;

public class SampleMe
{
        public static void main(String args[])
        {
                int arraySize = 2000;
                long before;
                long after;

                SampleClass array[] = new SampleClass[arraySize];
                for (int i= 0; i < arraySize; i++)
                {
                        array[i] = new SampleClass();
                        if ((i % (arraySize / 10)) == 0)
                        {
                                System.out.println(i);
                        }
                }
                System.gc();
                System.out.println("Erasing objects");
                for (int i = 0; i < arraySize /2; i ++)
                {
                        array[i*2] = null;
                }
                System.out.println("ready to GC");
                before = System.currentTimeMillis();
                System.gc();
                after = System.currentTimeMillis();
                System.out.println(after - before);

                for (int i = 0; i < arraySize /3; i ++)
                {
                        array[i*2 + 1] = null;
                }
                before = System.currentTimeMillis();
                System.gc();
                after = System.currentTimeMillis();
                System.out.println(after - before);

                for (int i = 0; i < arraySize; i ++)
                {
                        array[i] = null;
                }
        }
}
