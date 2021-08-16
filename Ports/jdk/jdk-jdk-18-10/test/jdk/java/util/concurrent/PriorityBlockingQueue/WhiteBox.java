/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Martin Buchholz with assistance from members of JCP
 * JSR-166 Expert Group and released to the public domain, as
 * explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @modules java.base/java.util.concurrent:open
 *          java.base/java.util:open
 * @run testng WhiteBox
 * @summary White box tests of implementation details
 */

import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Test
public class WhiteBox {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final VarHandle PQ_QUEUE = queueVarHandle(PriorityQueue.class);
    final VarHandle PBQ_QUEUE = queueVarHandle(PriorityBlockingQueue.class);

    VarHandle queueVarHandle(Class klazz) {
        return findVarHandle(klazz, "queue", Object[].class);
    }

    VarHandle findVarHandle(Class klazz, String fieldName, Class type) {
        try {
            return MethodHandles.privateLookupIn(klazz, MethodHandles.lookup())
                .findVarHandle(klazz, fieldName, type);
        } catch (ReflectiveOperationException ex) { throw new Error(ex); }
    }

    Object[] queue(PriorityQueue q) {
        return (Object[]) PQ_QUEUE.getOpaque(q);
    }

    Object[] queue(PriorityBlockingQueue q) {
        return (Object[]) PBQ_QUEUE.getOpaque(q);
    }

    /** PriorityQueue and PriorityBlockingQueue have identical heap management. */
    @Test
    public void randomLockstep() {
        PriorityBlockingQueue pbq = new PriorityBlockingQueue();
        PriorityQueue pq = new PriorityQueue();
        List<Consumer<Queue>> frobbers = List.of(
            q -> q.add(42),
            q -> assertTrue(q.offer(86)),
            q -> q.poll(),
            q -> q.peek(),
            q -> {
                Iterator it = q.iterator();
                if (it.hasNext()) {
                    it.next();
                    it.remove();
                }});
        for (int i = 0; i < 100; i++) {
            Consumer<Queue> frobber = frobbers.get(rnd.nextInt(frobbers.size()));
            frobber.accept(pq);
            frobber.accept(pbq);
            assertInvariants(pbq);
            assertInvariants(pq);
            assertTrue(Arrays.equals(pq.toArray(), pbq.toArray()));
        }
    }

    @Test
    public void forgetMeNot_PriorityQueue() {
        forgetMeNot(() -> new PriorityQueue());
        forgetMeNot(() -> new PriorityQueue(rnd.nextInt(1, 100)));
        forgetMeNot(() -> new PriorityQueue(rnd.nextInt(1, 100), Collections.reverseOrder()));
    }

    @Test
    public void forgetMeNot_PriorityBlockingQueue() {
        forgetMeNot(() -> new PriorityBlockingQueue());
        forgetMeNot(() -> new PriorityBlockingQueue(rnd.nextInt(1, 100)));
        forgetMeNot(() -> new PriorityBlockingQueue(rnd.nextInt(1, 100), Collections.reverseOrder()));
    }

    void forgetMeNot(Supplier<Queue> qMaker) {
        Queue q = qMaker.get();
        Set replay = Collections.newSetFromMap(new IdentityHashMap());
        int size = rnd.nextInt(64);
        for (int i = 0; i < size; i++) {
            Object e = new Integer(rnd.nextInt());
            q.add(e);
            replay.add(e);
        }
        Iterator it = q.iterator();
        while (it.hasNext()) {
            Object e = it.next();
            assertTrue(replay.contains(e));
            if (rnd.nextBoolean()) {
                it.remove();
                if (rnd.nextBoolean())
                    assertThrows(IllegalStateException.class,
                                 () -> it.remove());
                assertTrue(replay.remove(e));
            }
            assertInvariants(q);
        }
        for (Object e; (e = q.poll()) != null; )
            assertTrue(replay.remove(e));
        assertTrue(replay.isEmpty());
    }

    @Test
    public void testRemoveIf_PriorityQueue() {
        testRemoveIf(() -> new PriorityQueue());
        testRemoveIf(() -> new PriorityQueue(rnd.nextInt(1, 100)));
        testRemoveIf(() -> new PriorityQueue(rnd.nextInt(1, 100), Collections.reverseOrder()));
    }

    @Test
    public void testRemoveIf_PriorityBlockingQueue() {
        testRemoveIf(() -> new PriorityBlockingQueue());
        testRemoveIf(() -> new PriorityBlockingQueue(rnd.nextInt(1, 100)));
        testRemoveIf(() -> new PriorityBlockingQueue(rnd.nextInt(1, 100), Collections.reverseOrder()));
    }

    void testRemoveIf(Supplier<Queue> qMaker) {
        Queue q = qMaker.get();
        Set replay = Collections.newSetFromMap(new IdentityHashMap());
        int size = rnd.nextInt(64);
        for (int i = 0; i < size; i++) {
            Object e = new Integer(rnd.nextInt());
            q.add(e);
            replay.add(e);
        }
        q.removeIf(
            e -> {
                if (rnd.nextBoolean())
                    return false;
                assertTrue(replay.remove(e));
                return true;
            });
        assertInvariants(q);
        for (Object e; (e = q.poll()) != null; )
            assertTrue(replay.remove(e));
        assertTrue(replay.isEmpty());
    }

    byte[] serialBytes(Object o) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(o);
            oos.flush();
            oos.close();
            return bos.toByteArray();
        } catch (Exception fail) {
            throw new AssertionError(fail);
        }
    }

    @SuppressWarnings("unchecked")
    <T> T serialClone(T o) {
        try {
            ObjectInputStream ois = new ObjectInputStream
                (new ByteArrayInputStream(serialBytes(o)));
            T clone = (T) ois.readObject();
            assertNotSame(o, clone);
            assertSame(o.getClass(), clone.getClass());
            return clone;
        } catch (Exception fail) {
            throw new AssertionError(fail);
        }
    }

    @Test
    public void testSerialization() {
        assertInvariants(serialClone(new PriorityQueue()));
        assertInvariants(serialClone(new PriorityQueue(1)));
        assertInvariants(serialClone(new PriorityQueue(new ArrayList())));
        assertInvariants(serialClone(new PriorityBlockingQueue()));
        assertInvariants(serialClone(new PriorityBlockingQueue(1)));
        assertInvariants(serialClone(new PriorityBlockingQueue(new ArrayList())));
    }

    void assertInvariants(Queue q) {
        if (q instanceof PriorityQueue)
            assertInvariants((PriorityQueue) q);
        else
            assertInvariants((PriorityBlockingQueue) q);
    }

    void assertInvariants(PriorityBlockingQueue q) {
        assertHeap(queue(q), q.size(), q.comparator());
    }

    void assertInvariants(PriorityQueue q) {
        assertHeap(queue(q), q.size(), q.comparator());
    }

    void assertHeap(Object[] es, int size, Comparator cmp) {
        assertTrue(es.length > 0);
        assertTrue(size >= 0 && size <= es.length);
        if (size < es.length)
            assertNull(es[size]);
        if (size > 0)
            assertNotNull(es[size - 1]);
        for (int i = 0; i <= size / 2; i++) {
            int leftChild = 2 * i + 1;
            int rightChild = leftChild + 1;
            if (leftChild < size)
                assertTrue(cmp(es[i], es[leftChild], cmp) <= 0);
            if (rightChild < size)
                assertTrue(cmp(es[i], es[rightChild], cmp) <= 0);
        }
    }

    int cmp(Object x, Object y, Comparator cmp) {
        return (cmp == null)
            ? ((Comparable) x).compareTo(y)
            : cmp.compare(x, y);
    }

}
