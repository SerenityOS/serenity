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
 * @summary converted from VM Testbase gc/gctests/ClassDeallocGC.
 * VM Testbase keywords: [gc]
 * VM Testbase readme:
 * This test is a slightly modified version of a test taken from a research paper
 * by CE McDowell of UCSC titled 'No so static "static fields"'.
 * To quote from the paper :
 * " In this paper we have identified a problem with the Java Language
 * Specification with regard to class unloading and static fields. We have
 * provided an example that demonstrates how the current implementation of
 * class unloading can result in a static field being reinitialized resulting
 * in a program that changes its behavior depending upon when garbage collection occurs."
 * In this test, the creation of the object first assigned to
 * class_one_object (actually an instance of ClassOne) also creates an instance
 * of ClassTwo which contains a static field. Once the references to the ClassOne
 * object and the Class object are set to null, a call to the garbage
 * collector results in both ClassOne and ClassTwo being unloaded. When
 * the final assignment to class_one_object occurs, creating a new instance of ClassOne
 * and a new instance of ClassTwo, the program should print out the value of counter in
 * ClassTwo as 2.  This would mean that class was not unloaded and the static field
 * was not reinitialized. If the test prints out 1, the test has failed as  the static
 * field was reinitilized.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.gctests.ClassDeallocGC.ClassDeallocGC
 */

package gc.gctests.ClassDeallocGC;

import nsk.share.TestFailure;

public class ClassDeallocGC
{
    public static void main(String[] args)
    throws java.io.IOException, ClassNotFoundException,
    IllegalAccessException, InstantiationException
    {
        int count = 0, counter;
        ClassOne class_one_object;

        /* load the class ClassOne and instantiate an instance of it */
        /* ClassOne uses ClassTwo which will thus get loaded also */
        Class theClass = Class.forName("gc.gctests.ClassDeallocGC.ClassOne");
        class_one_object = (ClassOne)theClass.newInstance();

        /* remove all references to the class and the instance */
        class_one_object = null;
        theClass = null;

        System.gc(); /* force garbage collection which also unloads classes */

        /*loads and instantiates ClassOne again. ClassTwo also gets reloaded*/
        class_one_object = (ClassOne) new ClassOne();
        if ( (counter = class_one_object.getCounter()) == 2 ) {
           System.out.println("Test Passed.");
        } else {
           throw new TestFailure("Test failed. counter = " + counter + ", should be 2.");
        }
    }
}

class ClassOne {
    ClassTwo class_two;
    public ClassOne(){
      class_two = new ClassTwo();
    }
    public int getCounter() {
      return class_two.getCounter();
    }
}


class ClassTwo {
    static int counter = 0;
    public ClassTwo(){
        counter++;
    }
    public int getCounter() {
      return counter;
    }
}
