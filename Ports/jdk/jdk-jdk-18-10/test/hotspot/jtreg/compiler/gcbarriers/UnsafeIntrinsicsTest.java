/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test id=z
 * @key randomness
 * @bug 8059022
 * @modules java.base/jdk.internal.misc:+open
 * @summary Validate barriers after Unsafe getReference, CAS and swap (GetAndSet)
 * @requires vm.gc.Z
 * @library /test/lib
 * @run main/othervm -XX:+UseZGC
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+ZVerifyViews -XX:ZCollectionInterval=1
 *                   -XX:-CreateCoredumpOnCrash
 *                   -XX:CompileCommand=dontinline,*::mergeImpl*
 *                   compiler.gcbarriers.UnsafeIntrinsicsTest
 */

/*
 * @test id=shenandoah
 * @key randomness
 * @bug 8255401 8251944
 * @modules java.base/jdk.internal.misc:+open
 * @summary Validate barriers after Unsafe getReference, CAS and swap (GetAndSet)
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @run main/othervm -XX:+UseShenandoahGC
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -XX:-CreateCoredumpOnCrash
 *                   -XX:+ShenandoahVerify
 *                   -XX:+IgnoreUnrecognizedVMOptions -XX:+ShenandoahVerifyOptoBarriers
 *                   -XX:CompileCommand=dontinline,*::mergeImpl*
 *                   compiler.gcbarriers.UnsafeIntrinsicsTest
 */

package compiler.gcbarriers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Random;
import jdk.test.lib.Utils;
import sun.misc.Unsafe;

public class UnsafeIntrinsicsTest {

    /*
     * This test triggers the loadbarriers by allocating a lot, keeping the objects alive and then
     * letting them die in a way that maximizes fragmentation.
     *
     * All subtests (OperationType's) could run in parallel.
     */

    static int node_count = 133700;
    static int thread_count = 4;
    static int time = Integer.getInteger("time", 4); // seconds per subtest

    static Runner r = new Runner(null, 1, 1, Runner.OperationType.CAS);

    static Node first_node;
    int epoch = 0;

    public static void main(String[] args) {
        UnsafeIntrinsicsTest t = new UnsafeIntrinsicsTest();

        t.testWithLocalData(Runner.OperationType.CAS);
        t.testWithLocalData(Runner.OperationType.Weak_CAS);
        t.testWithLocalData(Runner.OperationType.CMPX);

        t.testWithSharedData(Runner.OperationType.Swap);
        t.testWithSharedData(Runner.OperationType.Load);
    }

    public UnsafeIntrinsicsTest() {

    }

    public void testWithLocalData(Runner.OperationType optype) {
        System.out.println("Testing " + optype.name() + " with " + thread_count +" thread and " + node_count + " nodes");

        // start mutator threads
        ArrayList<Thread> thread_list = new ArrayList<Thread>();
        Random r = Utils.getRandomInstance();
        for (int i = 0; i < thread_count; i++) {

            setup(); // each thread has its own circle of nodes
            Thread t = new Thread(new Runner(first_node, time, r.nextLong(), optype));
            t.start();
            thread_list.add(t);
        }

        waitForCompletion(thread_list);
        countNodes();
    }

    public void testWithSharedData(Runner.OperationType optype) {
        System.out.println("Testing " + optype.name() + " with " + thread_count +" thread and " + node_count + " nodes");

        setup(); // All nodes are shared between threads
        ArrayList<Thread> thread_list = new ArrayList<Thread>();
        Random r = Utils.getRandomInstance();
        for (int i = 0; i < thread_count; i++) {
            Thread t = new Thread(new Runner(first_node, time, r.nextLong(), optype));
            t.start();
            thread_list.add(t);
        }

        waitForCompletion(thread_list);
        countNodes();
    }

    public void waitForCompletion(ArrayList<Thread> thread_list) {
        // do some waiting
        try {
            Thread.sleep(time*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // wait for all thread to terminate
        for (int i = 0; i < thread_count; i++) {
            try {
                thread_list.get(i).join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void countNodes() {
        epoch++;
        int count = 0;
        Node node = first_node;
        while (node.number() < epoch) {
            node.setNumber(epoch);
            count++;
            node = node.next();
        }
        System.out.println("Program end, found " + count + " nodes");
    }

    // Create a circular linked list
    public void setup() {
        first_node = new Node();
        Node last_node = first_node;
        for (int i = 0; i < node_count; i++) {
            last_node = new Node(last_node);
        }
        first_node.setNext(last_node);
    }
}

class Runner implements Runnable {

    OperationType type;
    Node current;
    Random r;
    long time;
    long seed;

    long milage = 0;
    long created = 0;
    long skipped = 0;
    int iterations = 0;

    static final jdk.internal.misc.Unsafe UNSAFE;
    static final long offset;

    public enum OperationType {
        Load("Load"),
        Swap("Swap"),
        CAS("CAS"),
        Weak_CAS("Weak-CAS"),
        CMPX("CMPX");

        private String name;
        private OperationType(String name) { this.name = name; }
    }

    static {
        try {
            Field f = jdk.internal.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (jdk.internal.misc.Unsafe) f.get(null);
            offset = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("next"));
        } catch (Exception e) {
            throw new RuntimeException("Unable to get Unsafe instance.", e);
        }
    }

    public Runner(Node start, int testtime, long seed, OperationType type) {
        current = start;
        time = testtime*1000000000L;
        r = new Random(seed);
        this.type = type;
    }

    @Override
    public void run() {
        long starttime = System.nanoTime();
        while((System.nanoTime() - starttime) < time) {
            iterations++;
            // Run a bit
            int run_length = r.nextInt() & 0xfff;
            for (int i = 0; i < run_length; i++) {
                current = current.next();
                milage++;
            }
            // find a start node
            Node startNode = current;
            Node expectedNext = startNode.next;

            // Run a bit more
            int skip_length = (r.nextInt() & 0xff) + 1;
            for (int i = 0; i < skip_length; i++) {
                current = current.next();
                skipped++;
            }

            // create a branch
            int branch_length = (r.nextInt() & 0xff) + 1;
            created += branch_length;
            Node head = makeBranch(current, branch_length);

            // complete circle, but continue to run on old path
            boolean test_fail = ((iterations & 0x1) == 0);
            Node current = merge(startNode, expectedNext, head, test_fail);
        }
        System.out.println("Milage: " + milage + " Skipped: " + skipped + " Created: " + created + " iterations: " + iterations);
    }

    /*
     *  The reason for the duplicated code that is wrapping the unsafe operations is that we want
     *  to test the operations individually. They must not interfere with each other - checking a field
     *  will heal that reference and no operation after can trigger the barrier.
     *
     *  All mergeImpl*-method are prevented from being inlined.
     */

    private Node merge(Node startNode, Node expectedNext, Node head, boolean test_fail) {
        switch (type) {
            case Load:
                return mergeImplLoad(startNode, expectedNext, head);
            case Swap:
                return mergeImplSwap(startNode, expectedNext, head);
            case CAS:
                if (test_fail) {
                    return mergeImplCASFail(startNode, expectedNext, head);
                } else {
                    return mergeImplCAS(startNode, expectedNext, head);
                }
            case Weak_CAS:
                if (test_fail) {
                    return mergeImplWeakCASFail(startNode, expectedNext, head);
                } else {
                    return mergeImplWeakCAS(startNode, expectedNext, head);
                }
            case CMPX:
                if (test_fail) {
                    return mergeImplCMPXFail(startNode, expectedNext, head);
                } else {
                    return mergeImplCMPX(startNode, expectedNext, head);
                }
            default:
            throw new Error("Unimplemented");
        }
    }

    private Node mergeImplLoad(Node startNode, Node expectedNext, Node head) {
        // Atomic load version
        Node temp = (Node) UNSAFE.getReference(startNode, offset);
        startNode.setNext(head);
        return temp;
    }

    private Node mergeImplSwap(Node startNode, Node expectedNext, Node head) {
        // Swap version
        return (Node) UNSAFE.getAndSetReference(startNode, offset, head);
    }

    private Node mergeImplCAS(Node startNode, Node expectedNext, Node head) {
        // CAS - should always be true within a single thread - no other thread can have overwritten
        if (!UNSAFE.compareAndSetReference(startNode, offset, expectedNext, head)) {
            throw new Error("CAS should always succeed on thread local objects, check you barrier implementation");
        }
        return expectedNext; // continue on old circle
    }

    private Node mergeImplCASFail(Node startNode, Node expectedNext, Node head) {
        // Force a fail
        if (UNSAFE.compareAndSetReference(startNode, offset, "fail", head)) {
            throw new Error("This CAS should always fail, check you barrier implementation");
        }
        if (startNode.next() != expectedNext) {
            throw new Error("Shouldn't have changed");
        }
        return current;
    }

    private Node mergeImplWeakCAS(Node startNode, Node expectedNext, Node head) {
        // Weak CAS - should always be true within a single thread - no other thread can have overwritten
        if (!UNSAFE.weakCompareAndSetReference(startNode, offset, expectedNext, head)) {
            throw new Error("Weak CAS should always succeed on thread local objects, check you barrier implementation");
        }
        return expectedNext; // continue on old circle
    }

    private Node mergeImplWeakCASFail(Node startNode, Node expectedNext, Node head) {
        // Force a fail
        if (UNSAFE.weakCompareAndSetReference(startNode, offset, "fail", head)) {
            throw new Error("This weak CAS should always fail, check you barrier implementation");
        }
        if (startNode.next() != expectedNext) {
            throw new Error("Shouldn't have changed");
        }
        return current;
    }

    private Node mergeImplCMPX(Node startNode, Node expectedNext, Node head) {
        // CmpX - should always be true within a single thread - no other thread can have overwritten
        Object res = UNSAFE.compareAndExchangeReference(startNode, offset, expectedNext, head);
        if (!res.equals(expectedNext)) {
            throw new Error("Fail CmpX should always succeed on thread local objects, check you barrier implementation");
        }
        return expectedNext; // continue on old circle
    }

    private Node mergeImplCMPXFail(Node startNode, Node expectedNext, Node head) {
        Object res = UNSAFE.compareAndExchangeReference(startNode, offset, head, head);
        if (startNode.next() != expectedNext) {
            throw new Error("Shouldn't have changed");
        }
        if (head == expectedNext) {
            throw new Error("Test malfunction");
        }
        if (!res.equals(expectedNext)) {
            throw new Error("This CmpX should have returned 'expectedNext' when it failed");
        }
        if (res.equals(head)) {
            throw new Error("This CmpX shouldn't have returned head when it failed. count: "+ iterations);
        }

        return current;
    }

    // Create a new branch that will replace a part of the circle
    public Node makeBranch(Node end_node, int count) {
        Node head = end_node;
        for (int i = 0; i < count; i++) {
            head = new Node(head);
        }
        return head;
    }
}

class Node {
    Node next;
    int number = 0;

    public int number() {
        return number;
    }

    public void setNumber(int v) {
        number = v;
    }

    public Node() {
    }

    public Node(Node link) {
        next = link;
    }

    public void setNext(Node next) {
        this.next = next;
    }
    public Node next() {
        return next;
    }
}
