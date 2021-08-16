/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

class Parent {
    int get() {return 1;}
}

class Child extends Parent {
    int get() {return 2;}
    int dummy() {
        // When Child is linked during dynamic dump (when the VM is shutting
        // down), it will be verified, which will recursively cause Child2/Parent2
        // to be loaded and verified.
        // We want to make sure that this is done at a point in the VM lifecycle where
        // it's still safe to do so.
        Parent2 x = new Child2();
        return x.get();
    }
}

class Parent2 {
    int get() {return 3;}
}

class Child2 extends Parent2 {
    int get() {return 4;}
}

class MyShutdown extends Thread{
    public void run(){
        System.out.println("shut down hook invoked...");
    }
}

class LinkClassApp {
    public static void main(String args[]) {
        Runtime r=Runtime.getRuntime();
        r.addShutdownHook(new MyShutdown());

        if (args.length > 0 && args[0].equals("run")) {
            System.out.println("test() = " + test());
        } else {
            // Executed during dynamic dumping.
            System.out.println("Test.class is initialized.");
            System.out.println("Parent.class and Child.class are loaded when Test.class is verified,");
            System.out.println("but these two classes are not linked");
        }

        if (args.length > 0 && args[0].equals("callExit")) {
            System.exit(0);
        }
    }

    static int test() {
        // Verification of Test.test() would load Child and Parent, and create a verification constraint that
        // Child must be a subtype of Parent.
        //
        // Child and Parent are not linked until Test.test() is actually executed.
        Parent x = new Child();
        return x.get();
    }
}
