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
 * @run testng WhiteBox
 * @summary White box tests of implementation details
 */

import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import static java.util.stream.Collectors.toList;
import java.util.function.Consumer;

@Test
public class WhiteBox {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final VarHandle HEAD, TAIL, ITEM, NEXT;

    WhiteBox() throws ReflectiveOperationException {
        Class<?> qClass = ConcurrentLinkedQueue.class;
        Class<?> nodeClass = Class.forName(qClass.getName() + "$Node");
        MethodHandles.Lookup lookup
            = MethodHandles.privateLookupIn(qClass, MethodHandles.lookup());
        HEAD = lookup.findVarHandle(qClass, "head", nodeClass);
        TAIL = lookup.findVarHandle(qClass, "tail", nodeClass);
        NEXT = lookup.findVarHandle(nodeClass, "next", nodeClass);
        ITEM = lookup.findVarHandle(nodeClass, "item", Object.class);
    }

    Object head(ConcurrentLinkedQueue q) { return HEAD.getVolatile(q); }
    Object tail(ConcurrentLinkedQueue q) { return TAIL.getVolatile(q); }
    Object item(Object node)             { return ITEM.getVolatile(node); }
    Object next(Object node)             { return NEXT.getVolatile(node); }

    int nodeCount(ConcurrentLinkedQueue q) {
        int i = 0;
        for (Object p = head(q); p != null; ) {
            i++;
            if (p == (p = next(p))) p = head(q);
        }
        return i;
    }

    void assertIsSelfLinked(Object node) {
        assertSame(next(node), node);
        assertNull(item(node));
    }

    void assertIsNotSelfLinked(Object node) {
        assertNotSame(node, next(node));
    }

    @Test
    public void addRemove() {
        ConcurrentLinkedQueue q = new ConcurrentLinkedQueue();
        assertInvariants(q);
        assertNull(item(head(q)));
        assertEquals(nodeCount(q), 1);
        q.add(1);
        assertEquals(nodeCount(q), 2);
        assertInvariants(q);
        q.remove(1);
        assertEquals(nodeCount(q), 1);
        assertInvariants(q);
    }

    /**
     * Traversal actions that visit every node and do nothing, but
     * have side effect of squeezing out dead nodes.
     */
    @DataProvider
    public Object[][] traversalActions() {
        return List.<Consumer<ConcurrentLinkedQueue>>of(
            q -> q.forEach(e -> {}),
            q -> assertFalse(q.contains(new Object())),
            q -> assertFalse(q.remove(new Object())),
            q -> q.spliterator().forEachRemaining(e -> {}),
            q -> q.stream().collect(toList()),
            q -> assertFalse(q.removeIf(e -> false)),
            q -> assertFalse(q.removeAll(List.of())))
            .stream().map(x -> new Object[]{ x }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "traversalActions")
    public void traversalOperationsCollapseNodes(
        Consumer<ConcurrentLinkedQueue> traversalAction) {
        ConcurrentLinkedQueue q = new ConcurrentLinkedQueue();
        Object oldHead;
        int n = 1 + rnd.nextInt(5);
        for (int i = 0; i < n; i++) q.add(i);
        assertInvariants(q);
        assertEquals(nodeCount(q), n + 1);
        oldHead = head(q);
        traversalAction.accept(q); // collapses head node
        assertIsSelfLinked(oldHead);
        assertInvariants(q);
        assertEquals(nodeCount(q), n);
        // Iterator.remove does not currently try to collapse dead nodes
        for (Iterator it = q.iterator(); it.hasNext(); ) {
            it.next();
            it.remove();
        }
        assertEquals(nodeCount(q), n);
        assertInvariants(q);
        oldHead = head(q);
        traversalAction.accept(q); // collapses all nodes
        if (n > 1) assertIsSelfLinked(oldHead);
        assertEquals(nodeCount(q), 1);
        assertInvariants(q);

        for (int i = 0; i < n + 1; i++) q.add(i);
        assertEquals(nodeCount(q), n + 2);
        oldHead = head(q);
        assertEquals(0, q.poll()); // 2 leading nodes collapsed
        assertIsSelfLinked(oldHead);
        assertEquals(nodeCount(q), n);
        assertTrue(q.remove(n));
        assertEquals(nodeCount(q), n);
        traversalAction.accept(q); // trailing node is never collapsed
    }

    @Test(dataProvider = "traversalActions")
    public void traversalOperationsCollapseLeadingNodes(
        Consumer<ConcurrentLinkedQueue> traversalAction) {
        ConcurrentLinkedQueue q = new ConcurrentLinkedQueue();
        Object oldHead;
        int n = 1 + rnd.nextInt(5);
        for (int i = 0; i < n; i++) q.add(i);
        assertEquals(nodeCount(q), n + 1);
        oldHead = head(q);
        traversalAction.accept(q);
        assertInvariants(q);
        assertEquals(nodeCount(q), n);
        assertIsSelfLinked(oldHead);
    }

    @Test(dataProvider = "traversalActions")
    public void traversalOperationsDoNotSelfLinkInteriorNodes(
        Consumer<ConcurrentLinkedQueue> traversalAction) {
        ConcurrentLinkedQueue q = new ConcurrentLinkedQueue();
        int c;
        int n = 3 + rnd.nextInt(3);
        for (int i = 0; i < n; i++) q.add(i);
        Object oneNode;
        for (oneNode = head(q);
             ! (item(oneNode) != null && item(oneNode).equals(1));
             oneNode = next(oneNode))
            ;
        Object next = next(oneNode);
        c = nodeCount(q);
        for (Iterator it = q.iterator(); it.hasNext(); )
            if (it.next().equals(1)) it.remove();
        assertEquals(nodeCount(q), c - 1); // iterator detached head!
        assertNull(item(oneNode));
        assertSame(next, next(oneNode));
        assertInvariants(q);
        c = nodeCount(q);
        traversalAction.accept(q);
        assertEquals(nodeCount(q), c - 1);
        assertSame(next, next(oneNode)); // un-linked, but not self-linked
    }

    /**
     * Checks that traversal operations collapse a random pattern of
     * dead nodes as could normally only occur with a race.
     */
    @Test(dataProvider = "traversalActions")
    public void traversalOperationsCollapseRandomNodes(
        Consumer<ConcurrentLinkedQueue> traversalAction) {
        ConcurrentLinkedQueue q = new ConcurrentLinkedQueue();
        int n = rnd.nextInt(6);
        for (int i = 0; i < n; i++) q.add(i);
        ArrayList nulledOut = new ArrayList();
        for (Object p = head(q); p != null; p = next(p))
            if (item(p) != null && rnd.nextBoolean()) {
                nulledOut.add(item(p));
                ITEM.setVolatile(p, null);
            }
        traversalAction.accept(q);
        int c = nodeCount(q);
        assertEquals(q.size(), c - (q.contains(n - 1) ? 0 : 1));
        for (int i = 0; i < n; i++)
            assertTrue(nulledOut.contains(i) ^ q.contains(i));
    }

    /**
     * Traversal actions that remove every element, and are also
     * expected to squeeze out dead nodes.
     */
    @DataProvider
    public Object[][] bulkRemovalActions() {
        return List.<Consumer<ConcurrentLinkedQueue>>of(
            q -> q.clear(),
            q -> assertTrue(q.removeIf(e -> true)),
            q -> assertTrue(q.retainAll(List.of())))
            .stream().map(x -> new Object[]{ x }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "bulkRemovalActions")
    public void bulkRemovalOperationsCollapseNodes(
        Consumer<ConcurrentLinkedQueue> bulkRemovalAction) {
        ConcurrentLinkedQueue q = new ConcurrentLinkedQueue();
        int n = 1 + rnd.nextInt(5);
        for (int i = 0; i < n; i++) q.add(i);
        bulkRemovalAction.accept(q);
        assertEquals(nodeCount(q), 1);
        assertInvariants(q);
    }

    /**
     * Actions that remove the first element, and are expected to
     * leave at most one slack dead node at head.
     */
    @DataProvider
    public Object[][] pollActions() {
        return List.<Consumer<ConcurrentLinkedQueue>>of(
            q -> assertNotNull(q.poll()),
            q -> assertNotNull(q.remove()))
            .stream().map(x -> new Object[]{ x }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "pollActions")
    public void pollActionsOneNodeSlack(
        Consumer<ConcurrentLinkedQueue> pollAction) {
        ConcurrentLinkedQueue q = new ConcurrentLinkedQueue();
        int n = 1 + rnd.nextInt(5);
        for (int i = 0; i < n; i++) q.add(i);
        assertEquals(nodeCount(q), n + 1);
        for (int i = 0; i < n; i++) {
            int c = nodeCount(q);
            boolean slack = item(head(q)) == null;
            if (slack) assertNotNull(item(next(head(q))));
            pollAction.accept(q);
            assertEquals(nodeCount(q), q.isEmpty() ? 1 : c - (slack ? 2 : 0));
        }
        assertInvariants(q);
    }

    /**
     * Actions that append an element, and are expected to
     * leave at most one slack node at tail.
     */
    @DataProvider
    public Object[][] addActions() {
        return List.<Consumer<ConcurrentLinkedQueue>>of(
            q -> q.add(1),
            q -> q.offer(1))
            .stream().map(x -> new Object[]{ x }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "addActions")
    public void addActionsOneNodeSlack(
        Consumer<ConcurrentLinkedQueue> addAction) {
        ConcurrentLinkedQueue q = new ConcurrentLinkedQueue();
        int n = 1 + rnd.nextInt(5);
        for (int i = 0; i < n; i++) {
            boolean slack = next(tail(q)) != null;
            addAction.accept(q);
            if (slack)
                assertNull(next(tail(q)));
            else {
                assertNotNull(next(tail(q)));
                assertNull(next(next(tail(q))));
            }
            assertInvariants(q);
        }
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
        ConcurrentLinkedQueue q = serialClone(new ConcurrentLinkedQueue());
        assertInvariants(q);
    }

    /** Checks conditions which should always be true. */
    void assertInvariants(ConcurrentLinkedQueue q) {
        assertNotNull(head(q));
        assertNotNull(tail(q));
        // head is never self-linked (but tail may!)
        for (Object h; next(h = head(q)) == h; )
            assertNotSame(h, head(q)); // must be update race
    }
}
