/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6898462 8198826
 * @summary failed reallocations of scalar replaced objects during deoptimization causes crash
 *
 * @requires !vm.graal.enabled
 * @run main/othervm -XX:-BackgroundCompilation -Xmx128M -XX:+IgnoreUnrecognizedVMOptions -XX:+VerifyStack
 *      -XX:CompileCommand=exclude,compiler.uncommontrap.TestDeoptOOM::main
 *      -XX:CompileCommand=exclude,compiler.uncommontrap.TestDeoptOOM::m9_1
 *      compiler.uncommontrap.TestDeoptOOM
 */

package compiler.uncommontrap;

public class TestDeoptOOM {

    long f1;
    long f2;
    long f3;
    long f4;
    long f5;

    static class LinkedList {
        LinkedList l;
        long[] array;
        LinkedList(LinkedList l, int size) {
            array = new long[size];
            this.l = l;
        }
    }

    static LinkedList ll;

    static void consume_all_memory() {
        int size = 128 * 1024 * 1024;
        while(size > 0) {
            try {
                while(true) {
                    ll = new LinkedList(ll, size);
                }
            } catch(OutOfMemoryError oom) {
            }
            size = size / 2;
        }
    }

    static void free_memory() {
        ll = null;
    }

    static TestDeoptOOM m1(boolean deopt) {
        try {
            TestDeoptOOM tdoom = new TestDeoptOOM();
            if (deopt) {
                return tdoom;
            }
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in m1");
        }
        return null;
    }

    static TestDeoptOOM m2_1(boolean deopt) {
        try {
            TestDeoptOOM tdoom = new TestDeoptOOM();
            if (deopt) {
                return tdoom;
            }
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in m2_1");
        }
        return null;
    }

    static TestDeoptOOM m2(boolean deopt) {
        try {
            return m2_1(deopt);
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in m2");
        }
        return null;
    }

    static TestDeoptOOM m3_3(boolean deopt) {
        try {
            TestDeoptOOM tdoom = new TestDeoptOOM();
            if (deopt) {
                return tdoom;
            }
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in m3_3");
        }
        return null;
    }

    static boolean m3_2(boolean deopt) {
        try {
            return m3_3(deopt) != null;
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in m3_2");
        }
        return false;
    }

    static TestDeoptOOM m3_1(boolean deopt) {
        try {
            TestDeoptOOM tdoom = new TestDeoptOOM();
            if (m3_2(deopt)) {
                return tdoom;
            }
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in m3_1");
        }
        return null;
    }

    static TestDeoptOOM m3(boolean deopt) {
        try {
            return m3_1(deopt);
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in m3");
        }
        return null;
    }

    static TestDeoptOOM m4(boolean deopt) {
        try {
            TestDeoptOOM tdoom = new TestDeoptOOM();
            if (deopt) {
                tdoom.f1 = 1l;
                tdoom.f2 = 2l;
                tdoom.f3 = 3l;
                return tdoom;
            }
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in m4");
        }
        return null;
    }

    static TestDeoptOOM m5(boolean deopt) {
        try {
            TestDeoptOOM tdoom = new TestDeoptOOM();
            synchronized(tdoom) {
                if (deopt) {
                    return tdoom;
                }
            }
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in m5");
        }
        return null;
    }

    synchronized TestDeoptOOM m6_1(boolean deopt) {
        if (deopt) {
            return this;
        }
        return null;
    }

    static TestDeoptOOM m6(boolean deopt) {
        try {
            TestDeoptOOM tdoom = new TestDeoptOOM();
            return tdoom.m6_1(deopt);
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in m6");
        }
        return null;
    }

    static TestDeoptOOM m7_1(boolean deopt, Object lock) {
        try {
            synchronized(lock) {
                TestDeoptOOM tdoom = new TestDeoptOOM();
                if (deopt) {
                    return tdoom;
                }
            }
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in m7_1");
        }
        return null;
    }

    static TestDeoptOOM m7(boolean deopt, Object lock) {
        try {
            return m7_1(deopt, lock);
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in m7");
        }
        return null;
    }

    static class A {
        long f1;
        long f2;
        long f3;
        long f4;
        long f5;
    }

    static class B {
        long f1;
        long f2;
        long f3;
        long f4;
        long f5;

        A a;
    }

    static B m8(boolean deopt) {
        try {
            A a = new A();
            B b = new B();
            b.a = a;
            if (deopt) {
                return b;
            }
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in m8");
        }
        return null;
    }

    static void m9_1(int i) {
        if (i > 90000) {
            consume_all_memory();
        }
    }

    static TestDeoptOOM m9() {
        try {
            for (int i = 0; i < 100000; i++) {
                TestDeoptOOM tdoom = new TestDeoptOOM();
                m9_1(i);
                if (i > 90000) {
                    return tdoom;
                }
            }
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in m1");
        }
        return null;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 20000; i++) {
            m1(false);
        }

        consume_all_memory();

        try {
            m1(true);
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in main " + oom.getMessage());
        }

        free_memory();

        for (int i = 0; i < 20000; i++) {
            m2(false);
        }

        consume_all_memory();

        try {
            m2(true);
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in main");
        }

        free_memory();

        for (int i = 0; i < 20000; i++) {
            m3(false);
        }

        consume_all_memory();

        try {
            m3(true);
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in main");
        }

        free_memory();

        for (int i = 0; i < 20000; i++) {
            m4(false);
        }

        consume_all_memory();

        try {
            m4(true);
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in main");
        }

        free_memory();

        for (int i = 0; i < 20000; i++) {
            m5(false);
        }

        consume_all_memory();

        try {
            m5(true);
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in main");
        }

        free_memory();

        for (int i = 0; i < 20000; i++) {
            m6(false);
        }

        consume_all_memory();

        try {
            m6(true);
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in main");
        }

        free_memory();

        final Object lock = new Object();

        for (int i = 0; i < 20000; i++) {
            m7(false, lock);
        }

        consume_all_memory();

        try {
            m7(true, lock);
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in main");
        }

        free_memory();

        Thread thread = new Thread() {
                public void run() {
                    System.out.println("Acquiring lock");
                    synchronized(lock) {
                        System.out.println("Lock acquired");
                    }
                    System.out.println("Lock released");
                }
            };
        thread.start();
        try {
            thread.join();
        } catch(InterruptedException ie) {
        }

        for (int i = 0; i < 20000; i++) {
            m8(false);
        }

        consume_all_memory();

        try {
            m8(true);
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in main");
        }

        free_memory();

        try {
            m9();
        } catch(OutOfMemoryError oom) {
            free_memory();
            System.out.println("OOM caught in main");
        }

        free_memory();
    }
}
